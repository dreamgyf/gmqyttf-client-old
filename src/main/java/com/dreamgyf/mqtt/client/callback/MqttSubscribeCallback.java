package com.dreamgyf.mqtt.client.callback;

public interface MqttSubscribeCallback extends MqttCallback {

    void onSuccess(String topic, int returnCode);

    void onFailure(String topic);
    
}
