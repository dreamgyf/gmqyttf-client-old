package com.dreamgyf.utils;

import java.nio.charset.StandardCharsets;

public class MqttBuildUtils {

    public static byte[] utf8EncodedStrings(String str) {
        byte[] strBytes = str.getBytes(StandardCharsets.UTF_8);
        byte[] strBytesLength = ByteUtils.shortToByte2((short) strBytes.length);
        return combineBytes(strBytesLength,strBytes);
    }

    public static byte[] combineBytes(byte[] ...bytes) {
        int pos = 0;
        int len = 0;
        for(byte[] byteArray : bytes){
            len += byteArray.length;
        }
        byte[] res = new byte[len];
        for(byte[] byteArray : bytes){
            for(byte b : byteArray){
                res[pos++] = b;
            }
        }
        return res;
    }

    public static int getRemainingLength(byte packet[]) {
        if(packet.length < 2)
            return 0;
        int bits = 0;
        int pos = 1;
        int length = 0;
        do {
            length += (packet[pos] & 0x7f) << bits;
            bits += 7;
            pos++;
        } while(pos < packet.length && (packet[pos - 1] & 0x80) != 0);
        return length;
    }

    public static byte[] buildRemainingLength(int length) {
        byte[] res = new byte[4];
        int pos = 0;
        do {
            int digit = length % 128;
            length /= 128;
            if (length > 0)
                digit |= 0x80;
            res[pos++] = (byte) digit;
        } while (length > 0);
        return ByteUtils.getSection(res, 0, pos);
    }
}
