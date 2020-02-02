package com.dreamgyf.mqtt.packet;

import com.dreamgyf.utils.ByteUtils;

public class MqttPubrecPacket extends MqttPacket {
    public MqttPubrecPacket() {
        super();
    }

    public MqttPubrecPacket(byte[] packet) {
        super(packet);
    }

    public byte[] getPacketId() {
        return ByteUtils.getSection(packet, getLength() - getRemainingLength(), 2);
    }
}
