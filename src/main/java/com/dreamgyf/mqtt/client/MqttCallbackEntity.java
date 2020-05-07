package com.dreamgyf.mqtt.client;

import com.dreamgyf.mqtt.client.callback.MqttCallback;

import java.util.HashMap;
import java.util.LinkedHashMap;

class MqttCallbackEntity extends HashMap<String,Object> {

    private MqttCallback callback;

    private int state;

    public MqttCallbackEntity(MqttCallback callback, int state) {
        super();
        this.callback = callback;
        this.state = state;
    }

    public MqttCallback getCallback() {
        return callback;
    }

    public void setCallback(MqttCallback callback) {
        this.callback = callback;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }
}
