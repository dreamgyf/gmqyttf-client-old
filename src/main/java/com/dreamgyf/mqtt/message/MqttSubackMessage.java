package com.dreamgyf.mqtt.message;

public class MqttSubackMessage extends MqttMessage {
    public MqttSubackMessage() {
        super();
    }

    public MqttSubackMessage(byte[] message) {
        super(message);
    }

    public byte[] getPacketId() {
        byte[] res = new byte[2];
        res[0] = message[2];
        res[1] = message[3];
        return res;
    }

    public byte[] getReturnCodeList() {
        byte[] res = new byte[message.length - 4];
        for(int i = 4;i < message.length;i++) {
            res[i - 4] = message[i];
        }
        return res;
    }
}
