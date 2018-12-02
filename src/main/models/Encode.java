package main.models;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class Encode {

    //Tracks how often a appears in our map
    private HashMap<String, Integer> freqMap = new HashMap<String, Integer>();

    private HashMap<String, String> compressedResult = new HashMap<String, String>();

    private String fileExtension;
    private int wordLength;
    private String decodedTextRoot; //Saved first
    private String encodedTextRoot; //Saved last


    public Encode(int wordLength) {
        this.wordLength = wordLength;
        this.fileExtension = ".txt";
        this.decodedTextRoot = "";
        this.encodedTextRoot = "";
    }

    public Encode(int wordLength, String fileExtension) {
        this.wordLength = wordLength;
        this.fileExtension = fileExtension;
        this.decodedTextRoot = "";
        this.encodedTextRoot = "";
    }

    public void encode(URL filepath)
    {
        try {
            byte[] fileContents = Files.readAllBytes(Paths.get(filepath.toURI()));

            //Converting bits to string
            String fileBufferStr = "";
            for (byte b : fileContents) {
                fileBufferStr += String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0');
            }
            System.out.println(fileBufferStr);

            getFreqTable(fileBufferStr, wordLength);
            System.out.println(freqMap);

            ArrayList<Map.Entry<String, Integer>> sortedFreqList = sortedByFreq();

            System.out.println(sortedFreqList);
            compressString(sortedFreqList);
            System.out.println(compressedResult);
            saveEncodedFile(getFileName(filepath),fileBufferStr, wordLength);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

    }

    private String getFileName(URL filepath) throws URISyntaxException {
        File file = new File(filepath.toURI());
        String fileName = file.getName();

        return fileName.substring(0, fileName.lastIndexOf('.')) + "Encoded" + this.fileExtension;
    }

    private void getFreqTable(String buffer, int wordLen) {
        for (int i = 0; i < buffer.length(); i += wordLen) {
            if (buffer.length() < i+wordLen)
            {
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

    private  ArrayList<Map.Entry<String, Integer>> sortedByFreq() {
        ArrayList<Map.Entry<String, Integer>> list = new ArrayList<Map.Entry<String, Integer>>(freqMap.entrySet());
        list.sort(Map.Entry.comparingByValue());
        Collections.reverse(list);
        return list;
    }

    private  void compressString(ArrayList<Map.Entry<String, Integer>> sortedFreqList) {
        List<String> bitList = new ArrayList<String>();

        for (Map.Entry<String, Integer> entry : sortedFreqList) {
            bitList.add(entry.getKey());
        }

        appendBit(compressedResult, bitList, true);
    }

    private  void appendBit(HashMap<String, String> result, List<String> wordList, boolean up) {

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

    private  void saveEncodedFile(String fileName, String input, int wordLen) throws IOException {
        String output="";

        for (int i = 0; i < input.length(); i += wordLen) {
            if (input.length() < i+wordLen)
                break;

            String word = input.substring(i, i + wordLen);
            output += compressedResult.get(word);
        }

        ArrayList<Byte> bytes = new ArrayList<Byte>();
        for (int i = 0; i < output.length(); i+=8)
        {
            //If string % 8 != 0 we have to take the remainder
            if (output.length() < i+8)
            {
                encodedTextRoot = output.substring(i);
                break;
            }

            String bitString = output.substring(i, i+8);
            Byte b =(byte)Integer.parseInt(bitString, 2); //Byte.parseByte("01111111", 2);
            bytes.add(b);
        }

        DataOutputStream writer = new DataOutputStream(new
                FileOutputStream(fileName));

        //Constructing header
        output = encodedTextRoot + "," + decodedTextRoot + compressedResult.toString().trim().replace(" ", "");
        writer.writeBytes(output);

        //Writing encoded bytes to file
        for (Byte b : bytes)
            writer.writeByte(b);

        System.out.println(output);
        writer.close();
    }
}
