package com.dreamgyf.mqtt.packet;

import com.dreamgyf.utils.ByteUtils;

public class MqttSubackPacket extends MqttPacket {
    public MqttSubackPacket() {
        super();
    }

    public MqttSubackPacket(byte[] packet) {
        super(packet);
    }

    public byte[] getPacketId() {
        return ByteUtils.getSection(packet, getLength() - getRemainingLength(), 2);
    }

    public byte[] getReturnCodeList() {
        return ByteUtils.getSection(packet, getLength() - getRemainingLength() + 2, getRemainingLength() - 2);
    }
}
