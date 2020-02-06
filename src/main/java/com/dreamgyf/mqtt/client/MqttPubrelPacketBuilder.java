package com.dreamgyf.mqtt.client;

import com.dreamgyf.mqtt.MqttPacketType;
import com.dreamgyf.mqtt.client.callback.MqttPublishCallback;
import com.dreamgyf.utils.MqttBuildUtils;

class MqttPubrelPacketBuilder {

    private byte[] id;

    private String topic;

    private String message;

    private MqttPublishCallback callback;

    public MqttPubrelPacketBuilder(byte[] id, String topic, String message, MqttPublishCallback callback) {
        this.id = id;
        this.topic = topic;
        this.message = message;
        this.callback = callback;
    }

    protected byte[] getPacketId() {
        return id;
    }

    protected byte[] build() {
        byte[] fixedHeader = new byte[2];
        fixedHeader[0] = MqttPacketType.PUBREL.getCode();
        fixedHeader[0] <<= 4;
        fixedHeader[0] |= 0b00000010;
        fixedHeader[1] = 0b00000010;
        byte[] variableHeader = id;
        byte[] packet = MqttBuildUtils.combineBytes(fixedHeader,variableHeader);
        return packet;
    }
    

    public byte[] getId() {
        return this.id;
    }

    public String getTopic() {
        return this.topic;
    }

    public String getMessage() {
        return this.message;
    }

    public MqttPublishCallback getCallback() {
        return this.callback;
    }

}