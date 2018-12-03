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
    private boolean isLastBufferToRead = false;

    private String remainderFromWord = "";
    private String keyRemainder = "";
    private String valueRemainder = "";
    private String codeWordRemainder = "";

    public void decode(URL filepath){
        File file;
        resetDefaults();
        try (FileInputStream fs = new FileInputStream(file = new File(filepath.toURI()));
        ) {
            String outputFileName = file.getName();
            outputFileName = outputFileName.toLowerCase();
            if (outputFileName.contains("encoded"))
                outputFileName = outputFileName.replace("encoded","Decoded");
            else
                outputFileName = "Decoded" + outputFileName;

            DataOutputStream writer = new DataOutputStream(new
                    FileOutputStream(outputFileName));

            boolean metaDataRead = false;
            byte[] fileBuffer = new byte[bufferSize];
            int readBytes = 0;
            String remainder = "";
            int encodedDataPosition = 0; //position where encoded data starts in the buffer
            while (-1 != (readBytes = fs.read(fileBuffer))) {
                if(fs.available() == 0)
                    isLastBufferToRead = true;

                //first read metaData
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
            fs.close();
            //add remainder
            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

    }

    private void resetDefaults(){
        codeTable = new HashMap<String, String>();
        isRemainderFromWordRead = false;
        isReadingValueFromMetaData = true; //if false - we are reading key, not value
        isLastBufferToRead = false;
        remainderFromWord = "";
        keyRemainder = "";
        valueRemainder = "";
        codeWordRemainder = "";
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
                //System.out.println(codeTable);

                return ++i;
            }

            if (fileBuffer[i] == ',')
            {
                isReadingValueFromMetaData = true;
                //have a pair
                codeTable.put(key, value);
                //System.out.println(codeTable);
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
            remainderFromWord+=(char)fileBuffer[i];
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
        if (isLastBufferToRead)
        {
            //get last 8bits - they tell how many zeros were added to the last byte of encoded stream
            String lastByteString = bitString.substring(bitString.length() - 8);
            Byte lastByte = (byte) Integer.parseInt(lastByteString, 2);
            bitString = bitString.substring(0, bitString.length() - 8 - (int)lastByte);
        }
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

        //if it was last buffer add remainder of the original(decoded) string to result and then convert to bytes
        if(isLastBufferToRead)
            bitResult += remainderFromWord;

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