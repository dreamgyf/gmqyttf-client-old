package com.dreamgyf.mqtt.client.callback;

import java.util.Collection;

import com.dreamgyf.mqtt.client.MqttTopic;

public interface MqttUnsubscribeCallback extends MqttCallback {

    void onSuccess(Collection<MqttTopic> topics);
    
}
