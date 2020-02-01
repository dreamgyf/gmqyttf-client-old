package com.dreamgyf.mqtt.message;

public class MqttSubackPacket extends MqttPacket {
    public MqttSubackPacket() {
        super();
    }

    public MqttSubackPacket(byte[] packet) {
        super(packet);
    }

    public byte[] getPacketId() {
        byte[] res = new byte[2];
        res[0] = packet[2];
        res[1] = packet[3];
        return res;
    }

    public byte[] getReturnCodeList() {
        byte[] res = new byte[packet.length - 4];
        for(int i = 4;i < packet.length;i++) {
            res[i - 4] = packet[i];
        }
        return res;
    }
}
