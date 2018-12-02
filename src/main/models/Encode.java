package main.models;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

public class Encode {

    //Tracks how often a appears in our map
    private HashMap<String, Integer> freqMap = new HashMap<String, Integer>();

    private HashMap<String, String> compressedResult = new HashMap<String, String>();

    private String fileExtension;
    private int wordLength;
    private String decodedTextRoot; //Saved first
    private int bufferSize = 1024;

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

    public void encode(URL filepath) {
        try (FileInputStream fs = new FileInputStream(new File(filepath.toURI()));
        ) {
            byte[] fileBuffer = new byte[bufferSize];
            int readBytes = 0;
            //fill freq map iterating through whole file using buffers
            while (-1 != (readBytes = fs.read(fileBuffer))) {
                //convert bits to string
                String bitString = convertBitsToBitString(fileBuffer, readBytes);
                getFreqTable(bitString, wordLength);
                //System.out.println(freqMap);
            }
            fs.close();

            ArrayList<Map.Entry<String, Integer>> sortedFreqList = sortedByFreq();
            System.out.println(sortedFreqList);
            compressString(sortedFreqList);
            System.out.println(compressedResult);

            saveEncodedFile(filepath);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    private String convertBitsToBitString(byte[] byteBuffer, int size) {
        String bitBufferStr = "";
        for (int i = 0; i < size; ++i) {
            bitBufferStr += String.format("%8s", Integer.toBinaryString(byteBuffer[i] & 0xFF)).replace(' ', '0');
        }
        return bitBufferStr;
    }

    private String getFileName(URL filepath) throws URISyntaxException {
        File file = new File(filepath.toURI());
        String fileName = file.getName();

        return fileName.substring(0, fileName.lastIndexOf('.')) + "Encoded" + this.fileExtension;
    }

    private void getFreqTable(String buffer, int wordLen) {
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

    private void compressString(ArrayList<Map.Entry<String, Integer>> sortedFreqList) {
        List<String> bitList = new ArrayList<String>();

        for (Map.Entry<String, Integer> entry : sortedFreqList) {
            bitList.add(entry.getKey());
        }

        appendBit(compressedResult, bitList, true);
    }

    private void appendBit(HashMap<String, String> result, List<String> wordList, boolean up) {

        String bit = "";
        if (!result.isEmpty()) {
            bit = (up) ? "0" : "1";
        }

        for (String word : wordList) {
            String s = (result.get(word) == null) ? "" : result.get(word);
            result.put(word, s + bit);
        }

        if (wordList.size() >= 2) {
            int separator = (int) Math.floor((float) wordList.size() / 2.0);

            List<String> upList = wordList.subList(0, separator);
            appendBit(result, upList, true);
            List<String> downList = wordList.subList(separator, wordList.size());
            appendBit(result, downList, false);
        }
    }

    private void saveEncodedFile(URL sourceFilepath) throws IOException, URISyntaxException {
        DataOutputStream writer = new DataOutputStream(new
                FileOutputStream(getFileName(sourceFilepath)));

        //Construct and write header
        String metaData = decodedTextRoot + compressedResult.toString().trim().replace(" ", "");
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
        if (!remainder.equals("")) {
            while (8 != remainder.length())
                remainder += '0';

            Byte b = (byte) Integer.parseInt(remainder, 2); //Byte.parseByte("01111111", 2);
            writer.writeByte(b);
        }
        writer.close();
    }

    //returns remainder
    private String writeEncodedBytesBuffer(String input, DataOutputStream writer, String remainder) throws IOException {
        String output = remainder;
        // encode the input string
        for (int i = 0; i < input.length(); i += wordLength) {
            if (input.length() < i + wordLength)
                break;

            String word = input.substring(i, i + wordLength);
            output += compressedResult.get(word);
        }
        // convert encoded string symbols to bytes and write them to stream
        for (int i = 0; i < output.length(); i += 8) {
            //If string % 8 != 0 we have to take the remainder
            if (output.length() < i + 8) {
                return output.substring(i);
            }
            String bitString = output.substring(i, i + 8);
            Byte b = (byte) Integer.parseInt(bitString, 2);
            writer.writeByte(b);
        }
        return ""; //no reminder
    }
}
