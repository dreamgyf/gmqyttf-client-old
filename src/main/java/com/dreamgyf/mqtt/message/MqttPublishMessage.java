package com.dreamgyf.mqtt.message;

public class MqttPublishMessage extends MqttMessage {
    public MqttPublishMessage() {
        super();
    }

    public MqttPublishMessage(byte[] message) {
        super(message);
    }
}
