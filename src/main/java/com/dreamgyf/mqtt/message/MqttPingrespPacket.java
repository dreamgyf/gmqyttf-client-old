package com.dreamgyf.mqtt.message;

public class MqttPingrespPacket extends MqttPacket {
    public MqttPingrespPacket() {
        super();
    }

    public MqttPingrespPacket(byte[] packet) {
        super(packet);
    }
}
