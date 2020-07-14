package com.dreamgyf.mqtt.client;

import com.dreamgyf.utils.MqttBuildUtils;

public class MqttTopic {

    private String topic;

    private byte QoS = 0;

    public MqttTopic(String topic) {
        this.topic = topic;
    }
    
    public String getTopic() {
        return this.topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public int getQoS() {
        return this.QoS;
    }

    public MqttTopic setQoS(int QoS) {
        if(QoS < 0 || QoS > 2)
            throw new IllegalArgumentException("The value of QoS must be between 0 and 2.");
        this.QoS = (byte) QoS;
        return this;
    }

    protected byte[] buildSubscribePayLoadPacket() {
        byte[] topicByte = MqttBuildUtils.utf8EncodedStrings(topic);
        byte[] res = new byte[topicByte.length + 1];
        for(int i = 0;i < topicByte.length;i++) {
            res[i] = topicByte[i];
        }
        res[res.length - 1] = QoS;
        return res;
    }

    protected byte[] buildUnsubscribePayLoadPacket() {
        return MqttBuildUtils.utf8EncodedStrings(topic);
    }

}