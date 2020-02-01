package com.dreamgyf.mqtt.message;

public class MqttConnackPacket extends MqttPacket {

    public MqttConnackPacket() {
        super();
    }

    public MqttConnackPacket(byte[] packet) {
        super(packet);
    }

    public int getReturnCode(){
        return packet[3];
    }
}
