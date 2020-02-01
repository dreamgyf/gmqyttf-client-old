package com.dreamgyf.mqtt.client.callback;

public interface MqttMessageCallback extends MqttCallback {
    void messageArrived(String topic, String message);
}