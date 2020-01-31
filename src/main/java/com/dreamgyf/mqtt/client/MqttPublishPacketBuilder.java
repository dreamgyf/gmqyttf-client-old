package com.dreamgyf.mqtt.client;

import java.nio.charset.StandardCharsets;

import com.dreamgyf.mqtt.MqttPacketType;
import com.dreamgyf.mqtt.client.callback.MqttPublishCallback;
import com.dreamgyf.utils.MqttBuildUtils;

class MqttPublishPacketBuilder {

    private byte[] id;

    private String topic;

    private String message;

    private MqttPublishOptions options;

    private MqttPublishCallback callback;

    public MqttPublishPacketBuilder(byte[] id, String topic, String message, MqttPublishOptions options, MqttPublishCallback callback) {
        this.id = id;
        this.topic = topic;
        this.message = message;
        this.options = options;
        this.callback = callback;
    }

    protected byte[] getPacketId() {
        return id;
    }

    protected byte[] build() {
        byte[] fixedHeader = new byte[2];
        fixedHeader[0] = MqttPacketType.PUBLISH.getCode();
        fixedHeader[0] <<= 4;
        if(options.getDUP())
            fixedHeader[0] |= 0b00001000;
        fixedHeader[0] |= options.getQoS() << 1;
        if(options.getRETAIN())
            fixedHeader[0] |= 0b00000001;
        byte[] variableHeader;
        byte[] topicByte = MqttBuildUtils.utf8EncodedStrings(topic);
        //构建报文标识符 Packet Identifier
        byte[] packetIdentifier = new byte[0];
        if(options.getQoS() != 0) {
            packetIdentifier = id;
        }
        variableHeader = MqttBuildUtils.combineBytes(topicByte,packetIdentifier);
        byte[] payLoad = message.getBytes(StandardCharsets.UTF_8);
        byte[] packet = MqttBuildUtils.combineBytes(fixedHeader,variableHeader,payLoad);
        //设置报文长度
        packet[1] = (byte) (packet.length - 2);
        return packet;
    }
    

}