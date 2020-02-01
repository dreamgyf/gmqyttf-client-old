package com.dreamgyf.mqtt.client;

import com.dreamgyf.mqtt.client.callback.MqttMessageCallback;
import com.dreamgyf.mqtt.message.*;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MqttMessageHandler implements Runnable {
    
    private ExecutorService executorService = Executors.newFixedThreadPool(10);

    private List<MqttPacket> packetList;

    private final Object packetListLock;

    private MqttMessageCallback callback;

    private boolean isRunning = true;
    

    public MqttMessageHandler(List<MqttPacket> packetList, Object packetListLock, MqttMessageCallback callback) {
        this.packetList = packetList;
        this.packetListLock = packetListLock;
        this.callback = callback;
    }

    @Override
    public void run() {
        Thread.currentThread().setName("Thread-MqttMessageHandler");
        while (isRunning){
            synchronized (packetListLock) {
                Iterator<MqttPacket> iterator = packetList.iterator();
                while(iterator.hasNext()){
                    MqttPacket mqttMessage = iterator.next();
                    if(mqttMessage instanceof MqttPublishPacket) {
                        String topic = ((MqttPublishPacket) mqttMessage).getTopic();
                        String message = ((MqttPublishPacket) mqttMessage).getMessage();
                        if(((MqttPublishPacket) mqttMessage).getQoS() == 0) {
                            if(callback != null) {
                                executorService.execute(new Runnable(){
                                    @Override
                                    public void run() {
                                        callback.messageArrived(topic, message);
                                    }
                                });
                            }
                            iterator.remove();
                        }
                    }
                }
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }        
    }

    protected void stop() {
        isRunning = false;
    }
}
