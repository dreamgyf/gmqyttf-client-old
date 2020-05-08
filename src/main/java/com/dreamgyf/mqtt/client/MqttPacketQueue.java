package com.dreamgyf.mqtt.client;

import com.dreamgyf.mqtt.packet.*;

import java.util.concurrent.LinkedBlockingQueue;

class MqttPacketQueue {

    protected final LinkedBlockingQueue<MqttConnackPacket> connack;

    protected final LinkedBlockingQueue<MqttPublishPacket> publish;

    protected final LinkedBlockingQueue<MqttPubackPacket> puback;

    protected final LinkedBlockingQueue<MqttPubrecPacket> pubrec;

    protected final LinkedBlockingQueue<MqttPubrelPacket> pubrel;

    protected final LinkedBlockingQueue<MqttPubcompPacket> pubcomp;

    protected final LinkedBlockingQueue<MqttSubackPacket> suback;

    protected final LinkedBlockingQueue<MqttUnsubackPacket> unsuback;

    protected final LinkedBlockingQueue<MqttPingrespPacket> pingresp;

    protected MqttPacketQueue() {
        this(1000);
    }

    protected MqttPacketQueue(int capacity) {
        connack = new LinkedBlockingQueue<>(capacity);
        publish = new LinkedBlockingQueue<>(capacity);
        puback = new LinkedBlockingQueue<>(capacity);
        pubrec = new LinkedBlockingQueue<>(capacity);
        pubrel = new LinkedBlockingQueue<>(capacity);
        pubcomp = new LinkedBlockingQueue<>(capacity);
        suback = new LinkedBlockingQueue<>(capacity);
        unsuback = new LinkedBlockingQueue<>(capacity);
        pingresp = new LinkedBlockingQueue<>(capacity);
    }

    protected void clear() {
        connack.clear();
        publish.clear();
        puback.clear();
        pubrec.clear();
        pubrel.clear();
        pubcomp.clear();
        suback.clear();
        unsuback.clear();
        pingresp.clear();
    }

}
