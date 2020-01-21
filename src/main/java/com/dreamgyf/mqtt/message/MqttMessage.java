package com.dreamgyf.mqtt.message;

public class MqttMessage {

    protected byte[] message;

    public MqttMessage() {
    }

    public MqttMessage(byte[] message) {
        this.message = message;
    }

    public byte[] getMessage() {
        return message;
    }

    public void setMessage(byte[] message) {
        this.message = message;
    }
}
