package com.dreamgyf.mqtt.packet;

import com.dreamgyf.utils.MqttBuildUtils;

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

    public int getRemainingLength() {
        return MqttBuildUtils.getRemainingLength(packet);
    }
}
