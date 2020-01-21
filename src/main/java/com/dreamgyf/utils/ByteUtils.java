package com.dreamgyf.utils;

public class ByteUtils {

    public static byte string2byte(String str) throws Exception {
        if(str.length() > 8)
            throw new Exception("Value is out of range");
        byte res = 0;
        byte temp = 1;
        for(int i = str.length() - 1;i >= 0;i--) {
            if(str.charAt(i) < '0' || str.charAt(i) > '9')
                throw new Exception("Has illegal character in the String");
            res += char2int(str.charAt(i)) * temp;
            temp <<= 1;
//            res += char2int(str.charAt(i)) * Math.pow(2,str.length() - 1 - i);
        }
        return res;
    }

    public static int char2int(char c) {
        return c - '0';
    }

    public static byte[] shortToByte2(short n) {
        byte[] b = new byte[2];
        b[1] = (byte) (n & 0xff);
        b[0] = (byte) (n >> 8 & 0xff);
        return b;
    }

    public static short byte2ToShort(byte[] bytes) {
        int b0 = bytes[0] & 0xFF;
        int b1 = bytes[1] & 0xFF;
        return (short) ((b0 << 8) | b1);
    }

    public static byte[] intToByte4(int n) {
        byte[] b = new byte[4];
        b[3] = (byte) (n & 0xff);
        b[2] = (byte) (n >> 8 & 0xff);
        b[1] = (byte) (n >> 16 & 0xff);
        b[0] = (byte) (n >> 24 & 0xff);
        return b;
    }

    public static int byte4ToInt(byte[] bytes) {
        int b0 = bytes[0] & 0xFF;
        int b1 = bytes[1] & 0xFF;
        int b2 = bytes[2] & 0xFF;
        int b3 = bytes[3] & 0xFF;
        return (b0 << 24) | (b1 << 16) | (b2 << 8) | b3;
    }
}
