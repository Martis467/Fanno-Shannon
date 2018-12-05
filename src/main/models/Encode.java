package main.models;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

public class Encode {

    //Tracks how often a appears in our map
    private HashMap<String, Integer> freqMap = new HashMap<String, Integer>();

    private HashMap<String, String> codeTable = new HashMap<String, String>();

    private String fileExtension;
    private int wordLength;
    private String decodedTextRoot; //Saved first
    private int bufferSize = 1024;
    private long fileSize;

    public Encode(int wordLength) {
        this(wordLength, ".txt");
    }

    public Encode(int wordLength, String fileExtension) {
        this.wordLength = wordLength;
        this.fileExtension = fileExtension;
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
            //System.out.println(freqMap);

            ArrayList<Map.Entry<String, Integer>> sortedFreqList = sortedByFreq();
            //System.out.println(sortedFreqList);
            getCodeTable(sortedFreqList);
            //System.out.println(codeTable);

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
        if (bufferSize % wordLength != 0)
            bufferSize += (wordLength - (bufferSize % wordLength));
    }

    private String convertBitsToBitString(byte[] byteBuffer, int size) {
        StringBuilder bitBufferStr = new StringBuilder();
        for (int i = 0; i < size; ++i) {
            bitBufferStr.append(String.format("%8s", Integer.toBinaryString(byteBuffer[i] & 0xFF)).replace(' ', '0'));
        }
        return bitBufferStr.toString();
    }

    private String getFileName(URL filepath) throws URISyntaxException {
        File file = new File(filepath.toURI());
        String fileName = file.getName();

        return fileName.substring(0, fileName.lastIndexOf('.')) + "Encoded" + this.fileExtension;
    }

    private void getFreqTable(String buffer, int wordLen) {
        buffer = decodedTextRoot + buffer;
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
        appendBit(codeTable, sortedFreqList, true);
    }

    private void appendBit(HashMap<String, String> result, List<Map.Entry<String, Integer>> wordList, boolean up) {

        String bit = "";
        if (!result.isEmpty()) {
            bit = (up) ? "0" : "1";
        }

        for (Map.Entry<String, Integer> word : wordList) {
            String s = (result.get(word.getKey()) == null) ? "" : result.get(word.getKey());
            result.put(word.getKey(), s + bit);
        }

        if (wordList.size() >= 2) {
            int separator = getSeparator(wordList);

            List<Map.Entry<String, Integer>> upList = wordList.subList(0, separator);
            appendBit(result, upList, true);
            List<Map.Entry<String, Integer>> downList = wordList.subList(separator, wordList.size());
            appendBit(result, downList, false);
        }
    }

    private int getSeparator(List<Map.Entry<String, Integer>> list)
    {
        int sum = 0;
        for (Map.Entry<String, Integer> word : list)
            sum+=word.getValue();

        int halfSum = 0;
        int idx = 0;
        while(halfSum < sum/2)
        {
            halfSum += list.get(idx).getValue();
            idx++;
        }
        return idx;
    }


    private void saveEncodedFile(URL sourceFilepath) throws IOException, URISyntaxException {
        File file = new File(getFileName(sourceFilepath));
        DataOutputStream writer = new DataOutputStream(new
                FileOutputStream(getFileName(sourceFilepath)));

        //Construct and write header
        String metaData = decodedTextRoot + codeTable.toString().trim().replace(" ", "");
        writer.writeBytes(metaData);

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

    public long getFileSize() { return fileSize; }
}
