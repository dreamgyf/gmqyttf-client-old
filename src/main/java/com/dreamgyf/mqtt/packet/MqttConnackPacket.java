package com.dreamgyf.mqtt.packet;

public class MqttConnackPacket extends MqttPacket {

    public MqttConnackPacket() {
        super();
    }

    public MqttConnackPacket(byte[] packet) {
        super(packet);
    }

    public int getReturnCode(){
        return packet[getLength() - getRemainingLength() + 1];
    }
}
