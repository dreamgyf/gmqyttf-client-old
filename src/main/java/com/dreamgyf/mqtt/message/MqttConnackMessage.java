package com.dreamgyf.mqtt.message;

public class MqttConnackMessage extends MqttMessage {

    public MqttConnackMessage() {
        super();
    }

    public MqttConnackMessage(byte[] message) {
        super(message);
    }

    public int getReturnCode(){
        return message[3];
    }
}
