package com.dreamgyf.mqtt.client.callback;

public interface MqttConnectCallback extends MqttCallback {

    void onSuccess();

    void onFailure();
    
}
