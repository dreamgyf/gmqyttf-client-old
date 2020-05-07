package com.dreamgyf.mqtt.client;

import com.dreamgyf.mqtt.client.callback.MqttCallback;
import com.dreamgyf.mqtt.client.callback.MqttMessageCallback;
import com.dreamgyf.mqtt.client.callback.MqttPublishCallback;

import java.util.concurrent.LinkedBlockingQueue;

class MqttCallbackQueueManger implements Runnable {

    private final LinkedBlockingQueue<MqttCallbackEntity> callbackQueue;

    private boolean isRunning = true;

    public MqttCallbackQueueManger(final LinkedBlockingQueue<MqttCallbackEntity> callbackQueue) {
        this.callbackQueue = callbackQueue;
    }

    @Override
    public void run() {
        while(isRunning) {
            try {
                MqttCallbackEntity callbackEntity = callbackQueue.take();
                MqttCallback callback = callbackEntity.getCallback();
                if(callback instanceof MqttMessageCallback) {
                    if(callbackEntity.getState() == 0) {
                        ((MqttMessageCallback) callback).messageArrived((String) callbackEntity.get("topic"),(String) callbackEntity.get("message"));
                    }
                } else if(callback instanceof MqttPublishCallback) {
                    if(callbackEntity.getState() == 0) {
                        ((MqttMessageCallback) callback).messageArrived((String) callbackEntity.get("topic"),(String) callbackEntity.get("message"));
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void stop() {
        isRunning = false;
    }
}
