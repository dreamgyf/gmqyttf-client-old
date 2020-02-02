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
        byte[] header = new byte[1];
        header[0] = MqttPacketType.PUBLISH.getCode();
        header[0] <<= 4;
        if(options.getDUP())
            header[0] |= 0b00001000;
        header[0] |= options.getQoS() << 1;
        if(options.getRETAIN())
            header[0] |= 0b00000001;
        byte[] variableHeader;
        byte[] topicByte = MqttBuildUtils.utf8EncodedStrings(topic);
        //构建报文标识符 Packet Identifier
        byte[] packetIdentifier = new byte[0];
        if(options.getQoS() != 0) {
            packetIdentifier = id;
        }
        variableHeader = MqttBuildUtils.combineBytes(topicByte,packetIdentifier);
        byte[] payLoad = message.getBytes(StandardCharsets.UTF_8);
        //构建固定报头 Fixed header
        byte[] remainingLength = MqttBuildUtils.buildRemainingLength(variableHeader.length + payLoad.length);
        byte[] fixedHeader = MqttBuildUtils.combineBytes(header,remainingLength);
        //构建整个报文
        byte[] packet = MqttBuildUtils.combineBytes(fixedHeader,variableHeader,payLoad);
        return packet;
    }
    

}