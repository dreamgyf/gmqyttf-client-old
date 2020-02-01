package com.dreamgyf.mqtt.client.callback;

public interface MqttConnectStateCallback extends MqttCallback {
    void onDisconnected();
}