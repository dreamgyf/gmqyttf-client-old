package com.dreamgyf.mqtt.packet;

import com.dreamgyf.utils.ByteUtils;

public class MqttUnsubackPacket extends MqttPacket {
    public MqttUnsubackPacket() {
        super();
    }

    public MqttUnsubackPacket(byte[] packet) {
        super(packet);
    }

    public byte[] getPacketId() {
        return ByteUtils.getSection(packet, getLength() - getRemainingLength(), 2);
    }
}
