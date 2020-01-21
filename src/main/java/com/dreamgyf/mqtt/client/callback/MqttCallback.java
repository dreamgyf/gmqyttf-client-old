package com.dreamgyf.mqtt.client.callback;

public interface MqttCallback {

    void onSuccess();

    void onFailure();
}
