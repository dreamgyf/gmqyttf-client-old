package com.dreamgyf.mqtt.packet;

import com.dreamgyf.utils.ByteUtils;

public class MqttPubcompPacket extends MqttPacket {
    public MqttPubcompPacket() {
        super();
    }

    public MqttPubcompPacket(byte[] packet) {
        super(packet);
    }

    public byte[] getPacketId() {
        return ByteUtils.getSection(packet, getLength() - getRemainingLength(), 2);
    }
}
