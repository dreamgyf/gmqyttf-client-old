package com.dreamgyf.mqtt.packet;

import com.dreamgyf.utils.ByteUtils;

public class MqttPubackPacket extends MqttPacket {
    public MqttPubackPacket() {
        super();
    }

    public MqttPubackPacket(byte[] packet) {
        super(packet);
    }

    public byte[] getPacketId() {
        return ByteUtils.getSection(packet, getLength() - getRemainingLength(), 2);
    }
}
