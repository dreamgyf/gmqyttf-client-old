package com.dreamgyf.utils;

import java.nio.charset.StandardCharsets;

public class MqttBuildUtils {

    public static byte[] utf8EncodedStrings(String str) {
        byte[] strBytes = str.getBytes(StandardCharsets.UTF_8);
        byte[] strBytesLength = ByteUtils.shortToByte2((short) strBytes.length);
        return combineBytes(strBytesLength,strBytes);
    }

    public static byte[] combineBytes(byte[] ...bytes){
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
}
