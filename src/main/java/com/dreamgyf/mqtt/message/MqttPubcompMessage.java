package com.dreamgyf.mqtt.message;

public class MqttPubcompMessage extends MqttMessage {
    public MqttPubcompMessage() {
        super();
    }

    public MqttPubcompMessage(byte[] message) {
        super(message);
    }

    public byte[] getPacketId() {
        byte[] res = new byte[2];
        res[0] = message[2];
        res[1] = message[3];
        return res;
    }
}
