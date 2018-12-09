package main.models;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;

public class Decode {

    private HashMap<String, String> codeTable = new HashMap<String, String>();
    private int bufferSize = 4096;

    private boolean isLastBufferToRead = false;

    private String remainderFromWord = "";
    private String codeWordRemainder = "";
    private String workingString = ""; // this is string buffer we work with, converted from bytes to bitString
    private String currentWord = "";   // for tree decoding in recursion

    private long fileSize;
    private int wordLength = 0;
    private int codeCount = 0;

    public void decode(URL filepath){
        decode(filepath, "");
    }

    public void decode(URL filepath, String outputFileName) {
        File file;
        resetDefaults();
        try (FileInputStream fs = new FileInputStream(file = new File(filepath.toURI()))) {

            if(outputFileName.isEmpty()){
                outputFileName = file.getName();

                if (outputFileName.contains(".encoded"))
                    outputFileName = outputFileName.replace(".encoded", "");
            }
            File decodedFile = new File(outputFileName);
            DataOutputStream writer = new DataOutputStream(new
                    FileOutputStream(decodedFile));

            readMetaData(fs);
            writeDecodedBytes(writer, fs);

            fs.close();
            writer.close();
            fileSize = decodedFile.length();

        } catch (IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

    }

    private void resetDefaults() {
        codeTable = new HashMap<String, String>();
        isLastBufferToRead = false;
        remainderFromWord = "";
        codeWordRemainder = "";
        workingString = "";
        currentWord = "";
    }

    /**
     * @param fs
     * @return <b>n</b> where meta data ends
     */
    private void readMetaData(FileInputStream fs) throws IOException {

        byte[] fileBuffer = new byte[bufferSize];
        int readBytes = 0;
        if (-1 != (readBytes = fs.read(fileBuffer))) {
            if (fs.available() == 0) {
                isLastBufferToRead = true;
            }
        }

        int pos = readRemainder(fileBuffer, readBytes);
        wordLength = fileBuffer[pos];
        pos++;

        //4bytes for int - code count
        byte[] codeCountBytes = Arrays.copyOfRange(fileBuffer, pos, pos + 4);
        codeCount = fromByteArray(codeCountBytes);
        pos += 4;
        //now the code tree
        //convert remaining buffer to string
        byte[] fileRemainingBuffer = Arrays.copyOfRange(fileBuffer, pos, readBytes);
        workingString = convertBitsToBitString(fileRemainingBuffer, readBytes - pos);
        DecodeTree(fs);
        //byte were filled with ending zeros if tree was not %8 in encoding
        workingString = workingString.substring(workingString.length() % 8);
    }

    private void DecodeTree(FileInputStream fs) throws IOException //not encoded word
    {
        if (codeCount == 0)
            return;

        if (workingString.isEmpty() || workingString.length() < wordLength + 1)
            workingString += readInputAsBitString(fs);

        if (workingString.charAt(0) == '1') {
            if (workingString.length() < wordLength + 1)
                workingString += readInputAsBitString(fs);

            String value = workingString.substring(1, wordLength + 1);
            workingString = workingString.substring(1 + wordLength);

            codeTable.put(currentWord, value);
            codeCount--;
        } else {
            workingString = workingString.substring(1);
            currentWord += "0";
            DecodeTree(fs);
            currentWord += "1";
            DecodeTree(fs);
        }
        if (currentWord.length() > 0)
            currentWord = currentWord.substring(0, currentWord.length() - 1);

        return;
    }

    private String readInputAsBitString(FileInputStream fs) throws IOException {
        byte[] fileBuffer = new byte[bufferSize];
        int readBytes = 0;
        String s = "";
        if (-1 != (readBytes = fs.read(fileBuffer))) {
            if (fs.available() == 0)
                isLastBufferToRead = true;
            else if (fs.available() == 1)  {
                // quick hack - if there is only one byte left to read (the one that tells how many zeros were added at the end),
                // it means we did not know that last zeros were code words or added as tail
                byte[] tempArray = Arrays.copyOf(fileBuffer, bufferSize+1);
                tempArray[bufferSize] = (byte)fs.read();
                assert (fs.available() == 0);
                isLastBufferToRead = true;
                s = convertBitsToBitString(tempArray, readBytes+1);
                return s;
            }
            s = convertBitsToBitString(fileBuffer, readBytes);
        }
        return s;
    }

    //return position after remainder
    private int readRemainder(byte[] fileBuffer, int size) {
        for (int i = 0; i < size; ++i) {
            if (fileBuffer[i] == '}')
                return ++i; //done with remainder

            remainderFromWord += (char) fileBuffer[i];
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

    private void writeDecodedBytes(DataOutputStream writer, FileInputStream fs) throws IOException {
        String remainder = "";
        if (workingString.isEmpty())
            workingString = readInputAsBitString(fs);

        remainder = writeDecodedBytesBuffer(writer, remainder);
        while (!workingString.isEmpty()) {
            workingString = readInputAsBitString(fs);
            if (workingString.isEmpty())
                break;
            remainder = writeDecodedBytesBuffer(writer, remainder);
        }
    }

    private String writeDecodedBytesBuffer(DataOutputStream writer, String remainder) throws IOException {
        String bitString = workingString;//convertBitsToBitString(fileBuffer, readBytes);
        //System.out.println("bitStringas: " + bitString);
        if (isLastBufferToRead) {
            //get last 8bits - they tell how many zeros were added to the last byte of encoded stream
            String lastByteString = bitString.substring(bitString.length() - 8);
            Byte lastByte = (byte) Integer.parseInt(lastByteString, 2);
            bitString = bitString.substring(0, bitString.length() - 8 - (int) lastByte);
        }

        String codeWord = codeWordRemainder;
        String bitResult = remainder;
        for (int i = 0; i < bitString.length(); ++i) {
            codeWord += bitString.charAt(i);
            if (codeTable.containsKey(codeWord)) {
                bitResult += codeTable.get(codeWord);
                codeWord = "";
            }
        }
        codeWordRemainder = codeWord;
        //if at this point we have codeWord not empty, it means buffer ended , so save it as a remainder

        //if it was last buffer add remainder of the original(decoded) string to result and then convert to bytes
        if (isLastBufferToRead)
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

    private int fromByteArray(byte[] bytes) {
        return bytes[0] << 24 | (bytes[1] & 0xFF) << 16 | (bytes[2] & 0xFF) << 8 | (bytes[3] & 0xFF);
    }

    public long getFileSize() {
        return fileSize;
    }
}