package com.dreamgyf.mqtt.message;

import com.dreamgyf.utils.ByteUtils;

public class MqttPublishPacket extends MqttPacket {
    public MqttPublishPacket() {
        super();
    }

    public MqttPublishPacket(byte[] packet) {
        super(packet);
    }

    public boolean getDUP() {
        return (((packet[0] & 0xff) >> 3) & 1) == 1;
    }

    public int getQoS() {
        return ((packet[0] & 0xff) >> 1) & 0b00000011;
    }

    public boolean getRETAIN() {
        return ((packet[0] & 0xff) & 1) == 1;
    }

    public int getTopicLength() {
        byte[] lengthByte = ByteUtils.getSection(packet, getLength() - getRemainingLength(), 2);
        return ByteUtils.byte2ToShort(lengthByte);
    }

    public String getTopic() {
        return new String(packet,4,getTopicLength());
    }

    public byte[] getPacketId() {
        return getQoS() != 0 ? ByteUtils.getSection(packet, getLength() - getRemainingLength() + 2 + getTopicLength(), 2) : null;
    }

    public int getMessageLength() {
        return getQoS() != 0 ? (getRemainingLength() - 2 - getTopicLength() - 2) : (getRemainingLength() - 2 - getTopicLength());
    }

    public String getMessage() {
        return new String(packet, getQoS() != 0 ? (getLength() - getRemainingLength() + 2 + getTopicLength() + 2) : (getLength() - getRemainingLength() + 2 + getTopicLength()),getMessageLength());
    }
}
