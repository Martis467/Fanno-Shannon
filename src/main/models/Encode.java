package main.models;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

public class Encode {

    //Tracks how often a appears in our map
    private HashMap<String, Integer> freqMap = new HashMap<String, Integer>();

    //<word,code>
    private HashMap<String, String> codeTable = new HashMap<String, String>();

    private int wordLength;
    private String decodedTextRoot; //Saved first
    private int bufferSize = 4096;
    private long fileSize;
    private StringBuilder compressedTree = new StringBuilder();

    public Encode(int wordLength) {
        this(wordLength, ".txt");
    }

    public Encode(int wordLength, String fileExtension) {
        this.wordLength = wordLength;
        this.decodedTextRoot = "";
        if (bufferSize % wordLength != 0)
            bufferSize += (wordLength - (bufferSize % wordLength));
    }

    public void setWordLength (int wordLen){
        wordLength = wordLen;
        if (bufferSize % wordLength != 0)
            bufferSize += (wordLength - (bufferSize % wordLength));
    }

    public void encode(URL filepath) {
        try (FileInputStream fs = new FileInputStream(new File(filepath.toURI()))) {
            resetDefaults();
            byte[] fileBuffer = new byte[bufferSize];
            int readBytes = 0;
            //fill freq map iterating through whole file using buffers
            while (-1 != (readBytes = fs.read(fileBuffer))) {
                //convert bits to string
                String bitString = convertBitsToBitString(fileBuffer, readBytes);
                getFreqTable(bitString, wordLength);
            }
            fs.close();

            ArrayList<Map.Entry<String, Integer>> sortedFreqList = sortedByFreq();
            getCodeTable(sortedFreqList);
            compressCodeTree();

            saveEncodedFile(filepath);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    private void resetDefaults (){
        freqMap = new HashMap<String, Integer>();
        codeTable = new HashMap<String, String>();
        decodedTextRoot = "";
        compressedTree = new StringBuilder();
        if (bufferSize % wordLength != 0)
            bufferSize += (wordLength - (bufferSize % wordLength));
    }

    private String convertBitsToBitString(byte[] byteBuffer, int size) {
        StringBuilder bitBufferStr = new StringBuilder();
        for (int i = 0; i < size; ++i) {
            bitBufferStr.append(convertByteToBitString(byteBuffer[i]));
        }
        return bitBufferStr.toString();
    }

    private String convertByteToBitString(byte b){
        return String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0');
    }

    private String getFileName(URL filepath) throws URISyntaxException {
        File file = new File(filepath.toURI());
        String fileName = file.getName();

        return fileName +"." + "encoded";
    }

    private void getFreqTable(String buffer, int wordLen) {
        buffer = decodedTextRoot + buffer;  // decodedTextRoot should only appear in the end of file, because we adjusted buffer
        for (int i = 0; i < buffer.length(); i += wordLen) {
            if (buffer.length() < i + wordLen) {
                decodedTextRoot = buffer.substring(i);
                break;
            }
            String word = buffer.substring(i, i + wordLen);

            if (freqMap.containsKey(word))
                freqMap.put(word, freqMap.get(word) + 1);
            else
                freqMap.put(word, 1);
        }
    }
    private ArrayList<Map.Entry<String, Integer>> sortedByFreq() {
        ArrayList<Map.Entry<String, Integer>> list = new ArrayList<Map.Entry<String, Integer>>(freqMap.entrySet());
        list.sort(Map.Entry.comparingByValue());
        Collections.reverse(list);
        return list;
    }

    private void getCodeTable(ArrayList<Map.Entry<String, Integer>> sortedFreqList) {
        if(sortedFreqList.size() == 0)
            return;

        if(sortedFreqList.size() == 1){
            codeTable.put(sortedFreqList.get(0).getKey(), "0");
            return;
        }
        appendBit(sortedFreqList, true);
    }

    private void appendBit(List<Map.Entry<String, Integer>> wordList, boolean up) {
        String bit = "";
        if (!codeTable.isEmpty()) {
            bit = (up) ? "0" : "1";
        }

        for (Map.Entry<String, Integer> word : wordList) {
            String s = (codeTable.get(word.getKey()) == null) ? "" : codeTable.get(word.getKey());
            codeTable.put(word.getKey(), s + bit);
        }

        if (wordList.size() >= 2) {
            int separator = getSeparator(wordList);

            List<Map.Entry<String, Integer>> upList = wordList.subList(0, separator);
            appendBit(upList, true);
            List<Map.Entry<String,  Integer>> downList = wordList.subList(separator, wordList.size());
            appendBit(downList, false);
        }
    }

    private int getSeparator(List<Map.Entry<String, Integer>> list)
    {
        int sum = 0;
        for (Map.Entry<String, Integer> word : list)
            sum+=word.getValue();

        int lhsHalfSum = 0;
        int idx = 0;
        while(lhsHalfSum < sum/2)
        {
            lhsHalfSum += list.get(idx).getValue();
            idx++;
        }
        // did we hit too hard? if so, go back by one
        int rhsHalfSum = sum - lhsHalfSum;
        int lastValue = list.get(idx).getValue();
        if (Math.abs(lhsHalfSum - rhsHalfSum) > Math.abs((lhsHalfSum - lastValue) - (rhsHalfSum + lastValue)) && idx > 1)
            idx--;

        return idx;
    }

    private void saveEncodedFile(URL sourceFilepath) throws IOException, URISyntaxException {
        File file = new File(getFileName(sourceFilepath));
        DataOutputStream writer = new DataOutputStream(new
                FileOutputStream(getFileName(sourceFilepath)));

        // Construct and write header
        // metadata: decodedTextRootFromWord} wordLength(1byte) codeCount - 4bytes
        writeMetaDataHeader(writer);
        //iterate through whole input and encode it
        FileInputStream fs = new FileInputStream(new File(sourceFilepath.toURI()));
        byte[] fileBuffer = new byte[bufferSize];
        String remainder = "";
        int readBytes = 0;
        while (-1 != (readBytes = fs.read(fileBuffer))) {
            //convert bits to string
            String bitString = convertBitsToBitString(fileBuffer, readBytes);
            remainder = writeEncodedBytesBuffer(bitString, writer, remainder);
        }
        //last remainder should be parsed here
        byte lastByteZeroCount = 0;
        if (!remainder.equals("")) {
            while (8 != remainder.length())
            {
                lastByteZeroCount++;
                remainder += '0';
            }

            byte b = (byte) Integer.parseInt(remainder, 2); //Byte.parseByte("01111111", 2);
            writer.writeByte(b);
        }
        writer.writeByte(lastByteZeroCount);
        fs.close();
        writer.close();
        this.fileSize = file.length();
    }

    private void writeMetaDataHeader(DataOutputStream writer) throws IOException {
        writer.writeBytes(decodedTextRoot +"}" );
        writer.writeByte(wordLength);
        writer.writeInt(codeTable.size());

        //save tree and fill zeros till the end of the byte, next byte after the tree will be code
        while (compressedTree.length() % 8 != 0 )
            compressedTree.append("0");

        // convert encoded string symbols to bytes and write them to stream
        for (int i = 0; i < compressedTree.length(); i += 8) {

            String bitString = compressedTree.substring(i, i + 8);
            byte b = (byte) Integer.parseInt(bitString, 2);
            writer.writeByte(b);
        }
    }

    //returns remainder
    private String writeEncodedBytesBuffer(String input, DataOutputStream writer, String remainder) throws IOException {
        StringBuilder outputSb = new StringBuilder(remainder);
        // encode the input string
        for (int i = 0; i < input.length(); i += wordLength) {
            if (input.length() < i + wordLength)
            {   //should only happen at last buffer iteration
                break;
            }
            String word = input.substring(i, i + wordLength);
            outputSb.append(codeTable.get(word));
        }
        String output = outputSb.toString();

        // convert encoded string symbols to bytes and write them to stream
        for (int i = 0; i < output.length(); i += 8) {
            //If string % 8 != 0 we have to take the remainder
            if (output.length() < i + 8) {
                return output.substring(i);
            }
            String bitString = output.substring(i, i + 8);
            byte b = (byte) Integer.parseInt(bitString, 2);
            writer.writeByte(b);
        }
        return ""; //no reminder
    }

    private void compressCodeTree(){
        if(codeTable.size() == 0)
            return;

        Map<String, String> swappedCodeTable = codeTable.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
        if (swappedCodeTable.size() == 1){
            compressedTree.append("01" + swappedCodeTable.get("0"));
            return;
        }
        EncodeWord("", swappedCodeTable);
    }


private void EncodeWord(String word, Map <String, String> wordTable) //not encoded word
    {
        if (codeTable.containsValue(word)){
            compressedTree.append("1");
            compressedTree.append( wordTable.get(word));
        }
        else{
            compressedTree.append("0");
            EncodeWord(word + "0", wordTable);
            EncodeWord(word + "1", wordTable);
        }
    }

/*    private <T, E>  T getKeyByValue(Map<T, E> map, E value) {
        for (Map.Entry<T, E> entry : map.entrySet()) {
            if (Objects.equals(value, entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }*/

    public long getFileSize() { return fileSize; }
}
