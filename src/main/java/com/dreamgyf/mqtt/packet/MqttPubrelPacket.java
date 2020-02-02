package com.dreamgyf.mqtt.packet;

import com.dreamgyf.utils.ByteUtils;

public class MqttPubrelPacket extends MqttPacket {
    public MqttPubrelPacket() {
        super();
    }

    public MqttPubrelPacket(byte[] packet) {
        super(packet);
    }

    public byte[] getPacketId() {
        return ByteUtils.getSection(packet, getLength() - getRemainingLength(), 2);
    }
}
