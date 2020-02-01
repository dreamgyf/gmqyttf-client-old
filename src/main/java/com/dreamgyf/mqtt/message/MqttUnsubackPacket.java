package com.dreamgyf.mqtt.message;

public class MqttUnsubackPacket extends MqttPacket {
    public MqttUnsubackPacket() {
        super();
    }

    public MqttUnsubackPacket(byte[] packet) {
        super(packet);
    }

    public byte[] getPacketId() {
        byte[] res = new byte[2];
        res[0] = packet[2];
        res[1] = packet[3];
        return res;
    }
}
