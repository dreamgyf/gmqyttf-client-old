package com.dreamgyf.mqtt;

public enum MqttPacketType {

    CONNECT((byte) 1),
    CONNACK((byte) 2),
    PUBLISH((byte) 3),
    PUBACK((byte) 4),
    PUBREC((byte) 5),
    PUBREL((byte) 6),
    PUBCOMP((byte) 7),
    SUBSCRIBE((byte) 8),
    SUBACK((byte) 9),
    UNSUBSCRIBE((byte) 10),
    UNSUBACK((byte) 11),
    PINGREQ((byte) 12),
    PINGRESP((byte) 13),
    DISCONNECT((byte) 14);


    private byte type;

    private MqttPacketType(byte type) {
        this.type = type;
    }

    public byte getCode() {
        return type;
    }
}
