package com.dreamgyf.mqtt.client.callback;

public interface MqttPublishCallback extends MqttCallback {

    void messageArrived(String topic, String message);
    
}
