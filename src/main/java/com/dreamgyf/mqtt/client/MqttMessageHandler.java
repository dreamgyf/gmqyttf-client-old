package com.dreamgyf.mqtt.client;

import com.dreamgyf.mqtt.MqttPacketType;
import com.dreamgyf.mqtt.client.callback.MqttMessageCallback;
import com.dreamgyf.mqtt.client.callback.MqttPublishCallback;
import com.dreamgyf.mqtt.packet.*;
import com.dreamgyf.utils.ByteUtils;
import com.dreamgyf.utils.MqttBuildUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

class MqttMessageHandler {
    
    private final ExecutorService coreExecutorService;

    private final Socket socket;

    private final Object socketLock;

    private final LinkedBlockingQueue<MqttPublishPacket> publishQueue;

    private final LinkedBlockingQueue<MqttPubrelPacket> pubrelQueue;

    private final LinkedBlockingQueue<MqttCallbackEntity> callbackQueue;

    private MqttMessageCallback callback;

    private boolean isRunning = false;

    private final Set<Short> packetIdSet;

    private final MqttPublishMessageHandler mqttPublishMessageHandler = new MqttPublishMessageHandler();

    private final MqttPubrelMessageHandler mqttPubrelMessageHandler = new MqttPubrelMessageHandler();

    public MqttMessageHandler(final ExecutorService coreExecutorService,
                              final Socket socket, final Object socketLock,
                              final LinkedBlockingQueue<MqttPublishPacket> publishQueue,
                              final LinkedBlockingQueue<MqttPubrelPacket> pubrelQueue,
                              final Set<Short> packetIdSet,
                              final LinkedBlockingQueue<MqttCallbackEntity> callbackQueue, MqttMessageCallback callback) {
        this.coreExecutorService = coreExecutorService;
        this.socket = socket;
        this.socketLock = socketLock;
        this.publishQueue = publishQueue;
        this.pubrelQueue = pubrelQueue;
        this.packetIdSet = packetIdSet;
        this.callbackQueue = callbackQueue;
        this.callback = callback;
    }

    private class MqttPublishMessageHandler implements Runnable {
        @Override
        public void run() {
            Thread.currentThread().setName("Thread-MqttPublishMessageHandler");
            while (isRunning){
                //只有此线程一个线程使用publishQueue，所以不用加锁
                MqttPublishPacket publishPacket = null;
                try {
                    publishPacket = publishQueue.take();
                    String topic = publishPacket.getTopic();
                    String message = publishPacket.getMessage();
                    if(publishPacket.getQoS() == 0) {
                        if(callback != null) {
                            MqttCallbackEntity callbackEntity = new MqttCallbackEntity(callback,0);
                            callbackEntity.put("topic",topic);
                            callbackEntity.put("message",message);
                            callbackQueue.put(callbackEntity);
                        }
                    }
                    else if(publishPacket.getQoS() == 1) {
                        byte[] packetId = publishPacket.getPacketId();
                        byte[] fixedHeader = new byte[2];
                        fixedHeader[0] = MqttPacketType.PUBACK.getCode();
                        fixedHeader[0] <<= 4;
                        fixedHeader[1] = 0b00000010;
                        byte[] variableHeader = packetId;
                        byte[] packet = MqttBuildUtils.combineBytes(fixedHeader,variableHeader);
                        synchronized (socketLock) {
                            if(socket.isConnected()) {
                                try {
                                    OutputStream os = socket.getOutputStream();
                                    os.write(packet);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        if(callback != null) {
                            MqttCallbackEntity callbackEntity = new MqttCallbackEntity(callback,0);
                            callbackEntity.put("topic",topic);
                            callbackEntity.put("message",message);
                            callbackQueue.put(callbackEntity);
                        }
                    }
                    else if(publishPacket.getQoS() == 2) {
                        byte[] packetId = publishPacket.getPacketId();
                        short packageIdShort = ByteUtils.byte2ToShort(packetId);
                        //这里不关心是否是来自服务端的重发消息
                        if(!packetIdSet.contains(packageIdShort)) {
                            packetIdSet.add(packageIdShort);
                            byte[] fixedHeader = new byte[2];
                            fixedHeader[0] = MqttPacketType.PUBREC.getCode();
                            fixedHeader[0] <<= 4;
                            fixedHeader[1] = 0b00000010;
                            byte[] variableHeader = packetId;
                            byte[] packet = MqttBuildUtils.combineBytes(fixedHeader,variableHeader);
                            synchronized (socketLock) {
                                if(socket.isConnected()) {
                                    try {
                                        OutputStream os = socket.getOutputStream();
                                        os.write(packet);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                            if(callback != null) {
                                MqttCallbackEntity callbackEntity = new MqttCallbackEntity(callback,0);
                                callbackEntity.put("topic",topic);
                                callbackEntity.put("message",message);
                                callbackQueue.put(callbackEntity);
                            }
                        }

                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    private class MqttPubrelMessageHandler implements Runnable {

        @Override
        public void run() {
            Thread.currentThread().setName("Thread-MqttPubrelMessageHandler");
            while(isRunning) {
                //pubrelQueue只被此线程使用，线程安全，不加锁
                try {
                    MqttPubrelPacket pubrelPacket = pubrelQueue.take();
                    byte[] packetId = pubrelPacket.getPacketId();
                    byte[] fixedHeader = new byte[2];
                    fixedHeader[0] = MqttPacketType.PUBCOMP.getCode();
                    fixedHeader[0] <<= 4;
                    fixedHeader[1] = 0b00000010;
                    byte[] variableHeader = packetId;
                    byte[] packet = MqttBuildUtils.combineBytes(fixedHeader,variableHeader);
                    packetIdSet.remove(ByteUtils.byte2ToShort(packetId));
                    synchronized (socketLock) {
                        if(socket.isConnected()) {
                            try {
                                OutputStream os = socket.getOutputStream();
                                os.write(packet);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    protected void setCallback(MqttMessageCallback callback) {
        this.callback = callback;
    }

    protected void run() {
        if(!isRunning) {
            isRunning = true;
            coreExecutorService.execute(mqttPublishMessageHandler);
            coreExecutorService.execute(mqttPubrelMessageHandler);
        }
    }

    protected void stop() {
        isRunning = false;
    }
}
