package main.models;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;

public class Decode {

    private HashMap<String, String> codeTable = new HashMap<String, String>();
    private int bufferSize = 1024;
    private boolean isRemainderFromWordRead = false;
    private boolean isReadingValueFromMetaData = true; //if false - we are reading key, not value
    private String remainderFromWord = "";
    private String keyRemainder = "";
    private String valueRemainder = "";
    private String codeWordRemainder = "";
    public void decode(URL filepath){
        try (FileInputStream fs = new FileInputStream(new File(filepath.toURI()));
        ) {
            File file = new File(filepath.toURI());
            String fileName = file.getName();
            DataOutputStream writer = new DataOutputStream(new
                    FileOutputStream(fileName));

            boolean metaDataRead = false;
            byte[] fileBuffer = new byte[bufferSize];
            int readBytes = 0;
            String remainder = "";
            int encodedDataPosition = 0; //position where encoded data starts in the buffer
            while (-1 != (readBytes = fs.read(fileBuffer))) {
                //first read metaData
                //System.out.println("readBYtes: " + readBytes);
                if (!metaDataRead){
                    if (-1 == (encodedDataPosition = readMetaData(fileBuffer, readBytes))){
                        // did not finished reading meta data - get another buffer
                        continue;
                    }
                    readBytes = readBytes - encodedDataPosition;
                    //get rid of metadata by "moving" buffer
                    System.arraycopy(fileBuffer, encodedDataPosition, fileBuffer,0, readBytes);
                }
                metaDataRead = true;
                //once we done with metadata, do decoding
                remainder = writeDecodedBytesBuffer(fileBuffer, readBytes, writer, remainder);
            }
            //add remainder
            writer.writeBytes(remainderFromWord);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

    }
    //return -1 - did not finished (buffer did not contained '}'. Can happen if buffer is too small)
    //return n - position where metaData ends
    private int readMetaData(byte[] fileBuffer, int size ){
        int pos = 0;
        if (!isRemainderFromWordRead)
            pos = readRemainder(fileBuffer, size);

        isRemainderFromWordRead = true;

        String value = valueRemainder;
        String key = keyRemainder;

        for(int i = pos; i < size; ++i) {

            if (fileBuffer[i] == '}') {
                //finished reading metadata
                codeTable.put(key, value);
                System.out.println(codeTable);

                return ++i;
            }

            if (fileBuffer[i] == ',')
            {
                isReadingValueFromMetaData = true;
                //have a pair
                codeTable.put(key, value);
                System.out.println(codeTable);
                key = "";
                value = "";
                keyRemainder = "";
                valueRemainder = "";
                continue;
            }

            if (fileBuffer[i] == '=')
            {
                isReadingValueFromMetaData = false;
                continue;
            }

            if (isReadingValueFromMetaData)
                value += (char)fileBuffer[i];
            else
                key += (char)fileBuffer[i];

        }
        keyRemainder = key;
        valueRemainder = value;

        return -1;
    }
    //return position after remainder
    private int readRemainder(byte[] fileBuffer, int size){
        for(int i = 0; i < size; ++i){
            if (fileBuffer[i] == '{')
                return ++i; //skip {
            remainderFromWord+=fileBuffer[i];
        }
        return -1;
    }

    private String convertBitsToBitString(byte[] byteBuffer, int size) {
        String bitBufferStr = "";
        for (int i = 0; i < size; ++i) {
            bitBufferStr += String.format("%8s", Integer.toBinaryString(byteBuffer[i] & 0xFF)).replace(' ', '0');
        }
        return bitBufferStr;
    }
    private String writeDecodedBytesBuffer (byte[] fileBuffer, int readBytes, DataOutputStream writer, String remainder) throws IOException {
        String bitString = convertBitsToBitString(fileBuffer, readBytes);
        String codeWord = codeWordRemainder;
        String bitResult = remainder;
        for(int i = 0; i < bitString.length(); ++i){
            codeWord+=bitString.charAt(i);
            if(codeTable.containsKey(codeWord)){
                bitResult += codeTable.get(codeWord);
                codeWord = "";
            }
        }
        codeWordRemainder = codeWord;
        //if at this point we have codeWord not empty, it means buffer ended , so save it as a remainder
        for (int i = 0; i < bitResult.length(); i += 8) {
            //If string % 8 != 0 we have to take the remainder
            if (bitResult.length() < i + 8) {
                return bitResult.substring(i); //return remainder
            }
            String bitStringToParse = bitResult.substring(i, i + 8);
            Byte b = (byte) Integer.parseInt(bitStringToParse, 2);
            writer.writeByte(b);
        }
    return ""; // no remainder
    }

}