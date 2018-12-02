package sample;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Encode {
    private static HashMap<String, Integer> freqMap = new HashMap<String, Integer>();
    private static HashMap<String, String> compressedResult = new HashMap<String, String>();

    public static void encode(String input, String output, int wordLen)
    {
        HashMap<Integer, String> a = new HashMap<Integer, String>();

        Path path = Paths.get(input);
        try {
            byte[] fileContents = Files.readAllBytes(path);
            // TODO: bufferiais imt faila

            String fileBufferStr = "";
            for (byte b : fileContents) {
                fileBufferStr += String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0');
            }
            System.out.println(fileBufferStr);
            getFreqTable(fileBufferStr, wordLen);
            System.out.println(freqMap);
            ArrayList<Map.Entry<String, Integer>> sortedFreqList = sortedByFreq();
            System.out.println(sortedFreqList);
            compressString(sortedFreqList);
            System.out.println(compressedResult);
            saveEncodedFile(output, fileBufferStr, wordLen);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    private static void getFreqTable(String buffer, int wordLen) {
        for (int i = 0; i < buffer.length(); i += wordLen) {
            //TODO get the rest of the string and save it as root
            if (buffer.length() < i+wordLen)
                break;

            String word = buffer.substring(i, i + wordLen);
            // we have bitset here

            if (freqMap.containsKey(word))
                freqMap.put(word, freqMap.get(word) + 1);
            else
                freqMap.put(word, 1);
        }

    }

    private static ArrayList<Map.Entry<String, Integer>> sortedByFreq() {
        ArrayList<Map.Entry<String, Integer>> list = new ArrayList<Map.Entry<String, Integer>>(freqMap.entrySet());
        list.sort(Map.Entry.comparingByValue());
        Collections.reverse(list);
        return list;
    }

    private static void compressString(ArrayList<Map.Entry<String, Integer>> sortedFreqList) {
        List<String> bitList = new ArrayList<String>();

        for (Map.Entry<String, Integer> entry : sortedFreqList) {
            bitList.add(entry.getKey());
        }

        appendBit(compressedResult, bitList, true);
    }

    private static void appendBit(HashMap<String, String> result, List<String> wordList, boolean up) {

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

    private static void saveEncodedFile(String output, String input, int wordLen) throws IOException
    {
        //save metadata
        DataOutputStream writer = new DataOutputStream(new
                FileOutputStream(output));

        output = "";
        for (int i = 0; i < input.length(); i += wordLen) {
            if (input.length() < i+wordLen)
                break;

            String word = input.substring(i, i + wordLen);
            output += compressedResult.get(word);
        }
        String remainder = "";
        ArrayList<Byte> bytes = new ArrayList<Byte>();
        for (int i = 0; i < output.length(); i+=8)
        {
            //TODO gala pasiimt
            if (output.length() < i+8)
            {
                remainder = output.substring(i, output.length());
                break;
            }

            String bitString = output.substring(i, i+8);
            Byte b =(byte)Integer.parseInt(bitString, 2); //Byte.parseByte("01111111", 2);
            bytes.add(b);
        }
        writer.writeBytes(remainder);

        writer.writeBytes(compressedResult.toString());
        for (Byte b : bytes)
            writer.writeByte(b);

        System.out.println(output);
        writer.close();
    }

}
