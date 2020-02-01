package com.dreamgyf.mqtt.message;

public class MqttPacket {

    protected byte[] packet;

    public MqttPacket() {
    }

    public MqttPacket(byte[] packet) {
        this.packet = packet;
    }

    public byte[] getPacket() {
        return packet;
    }

    public void setPacket(byte[] packet) {
        this.packet = packet;
    }

    public int getLength() {
        return packet.length;
    }
}
