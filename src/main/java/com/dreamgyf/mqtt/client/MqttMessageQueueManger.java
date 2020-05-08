package com.dreamgyf.mqtt.client;

import com.dreamgyf.mqtt.MqttPacketType;
import com.dreamgyf.mqtt.client.callback.MqttPublishCallback;
import com.dreamgyf.mqtt.packet.MqttPubackPacket;
import com.dreamgyf.mqtt.packet.MqttPubcompPacket;
import com.dreamgyf.mqtt.packet.MqttPubrecPacket;
import com.dreamgyf.utils.ByteUtils;
import com.dreamgyf.utils.MqttBuildUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

class MqttMessageQueueManger {

    private final ExecutorService coreExecutorService;

    private final Socket socket;

    private final Object socketLock;

    private final LinkedBlockingQueue<MqttPubackPacket> pubackQueue;

    private final LinkedBlockingQueue<MqttPubrecPacket> pubrecQueue;

    private final LinkedBlockingQueue<MqttPubcompPacket> pubcompQueue;

    private final LinkedBlockingQueue<MqttCallbackEntity> callbackQueue;

    private final Set<Short> packetIdSet;

    private final MqttPing mqttPing;

    private final LinkedBlockingQueue<MqttPublishPacketBuilder> publishQueue;

    private final ConcurrentHashMap<Short,MqttPublishPacketBuilder> waitPublishCompleteMap = new ConcurrentHashMap<>();

    private boolean isRunning = false;

    private final MqttPublishQueueManger publishQueueManger = new MqttPublishQueueManger();

    private final MqttPubackQueueManger pubackQueueManger = new MqttPubackQueueManger();

    private final MqttPubrecQueueManger pubrecQueueManger = new MqttPubrecQueueManger();

    private final MqttPubcompQueueManger pubcompQueueManger = new MqttPubcompQueueManger();

    protected MqttMessageQueueManger(final ExecutorService coreExecutorService,
                                     final Socket socket, final Object socketLock,
                                     final LinkedBlockingQueue<MqttPubackPacket> pubackQueue,
                                     final LinkedBlockingQueue<MqttPubrecPacket> pubrecQueue,
                                     final LinkedBlockingQueue<MqttPubcompPacket> pubcompQueue,
                                     final LinkedBlockingQueue<MqttCallbackEntity> callbackQueue,
                                     final Set<Short> packetIdSet, final MqttPing mqttPing,
                                     final LinkedBlockingQueue<MqttPublishPacketBuilder> publishQueue) {
        this.coreExecutorService = coreExecutorService;
        this.socket = socket;
        this.socketLock = socketLock;
        this.pubackQueue = pubackQueue;
        this.pubrecQueue = pubrecQueue;
        this.pubcompQueue = pubcompQueue;
        this.callbackQueue = callbackQueue;
        this.packetIdSet = packetIdSet;
        this.mqttPing = mqttPing;
        this.publishQueue = publishQueue;
    }

    private class MqttPublishQueueManger implements Runnable {

        @Override
        public void run() {
            Thread.currentThread().setName("Thread-MqttPublishQueueManger");
            while(isRunning) {
                //publishQueue线程安全且只有这一个线程使用，不加锁
                try {
                    MqttPublishPacketBuilder publishPacketBuilder = publishQueue.take();
                    byte[] id = publishPacketBuilder.getId();
                    String topic = publishPacketBuilder.getTopic();
                    String message = publishPacketBuilder.getMessage();
                    MqttPublishCallback callback = publishPacketBuilder.getCallback();
                    MqttPublishOptions options = publishPacketBuilder.getOptions();
                    byte[] packet = publishPacketBuilder.build();
                    //发送消息
                    synchronized (socketLock) {
                        if(socket.isConnected()) {
                            OutputStream os;
                            try {
                                os = socket.getOutputStream();
                                os.write(packet);
                                mqttPing.updateLastReqTime();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    if(options.getQoS() == 0) {
                        if(callback != null) {
                            MqttCallbackEntity callbackEntity = new MqttCallbackEntity(callback,0);
                            callbackEntity.put("topic",topic);
                            callbackEntity.put("message",message);
                            callbackQueue.put(callbackEntity);
                        }
                    }
                    else if(options.getQoS() == 1) {
                        Short packetIdShort = ByteUtils.byte2ToShort(id);
                        waitPublishCompleteMap.put(packetIdShort,publishPacketBuilder);
                    }
                    else if(options.getQoS() == 2) {
                        Short packetIdShort = ByteUtils.byte2ToShort(id);
                        waitPublishCompleteMap.put(packetIdShort,publishPacketBuilder);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class MqttPubackQueueManger implements Runnable {
        @Override
        public void run() {
            Thread.currentThread().setName("Thread-MqttPubackQueueManger");
            while(isRunning) {
                try {
                    MqttPubackPacket pubackPacket = pubackQueue.take();
                    byte[] packetId = pubackPacket.getPacketId();
                    Short packetIdShort = ByteUtils.byte2ToShort(packetId);
                    packetIdSet.remove(packetIdShort);
                    MqttPublishPacketBuilder publishPacketBuilder = waitPublishCompleteMap.get(packetIdShort);
                    String topic = publishPacketBuilder.getTopic();
                    String message = publishPacketBuilder.getMessage();
                    MqttPublishCallback callback = publishPacketBuilder.getCallback();
                    if(callback != null) {
                        MqttCallbackEntity callbackEntity = new MqttCallbackEntity(callback,0);
                        callbackEntity.put("topic",topic);
                        callbackEntity.put("message",message);
                        callbackQueue.put(callbackEntity);
                    }
                    waitPublishCompleteMap.remove(packetIdShort);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class MqttPubrecQueueManger implements Runnable {
        @Override
        public void run() {
            Thread.currentThread().setName("Thread-MqttPubrecQueueManger");
            while(isRunning) {
                try {
                    MqttPubrecPacket pubrecPacket = pubrecQueue.take();
                    byte[] packetId = pubrecPacket.getPacketId();
                    Short packetIdShort = ByteUtils.byte2ToShort(packetId);
                    byte[] fixedHeader = new byte[2];
                    fixedHeader[0] = MqttPacketType.PUBREL.getCode();
                    fixedHeader[0] <<= 4;
                    fixedHeader[0] |= 0b00000010;
                    fixedHeader[1] = 0b00000010;
                    byte[] variableHeader = packetId;
                    byte[] packet = MqttBuildUtils.combineBytes(fixedHeader,variableHeader);
                    //发送消息
                    synchronized (socketLock) {
                        if(socket.isConnected()) {
                            OutputStream os;
                            try {
                                os = socket.getOutputStream();
                                os.write(packet);
                                mqttPing.updateLastReqTime();
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

    private class MqttPubcompQueueManger implements Runnable {
        @Override
        public void run() {
            Thread.currentThread().setName("Thread-MqttPubcompQueueManger");
            while(isRunning) {
                try {
                    MqttPubcompPacket pubcompPacket = pubcompQueue.take();
                    byte[] packetId = pubcompPacket.getPacketId();
                    Short packetIdShort = ByteUtils.byte2ToShort(packetId);
                    packetIdSet.remove(packetIdShort);
                    MqttPublishPacketBuilder publishPacketBuilder = waitPublishCompleteMap.get(packetIdShort);
                    String topic = publishPacketBuilder.getTopic();
                    String message = publishPacketBuilder.getMessage();
                    MqttPublishCallback callback = publishPacketBuilder.getCallback();
                    if(callback != null) {
                        MqttCallbackEntity callbackEntity = new MqttCallbackEntity(callback,0);
                        callbackEntity.put("topic",topic);
                        callbackEntity.put("message",message);
                        callbackQueue.put(callbackEntity);
                    }
                    waitPublishCompleteMap.remove(packetIdShort);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void run() {
        if(!isRunning) {
            isRunning = true;
            coreExecutorService.execute(publishQueueManger);
            coreExecutorService.execute(pubackQueueManger);
            coreExecutorService.execute(pubrecQueueManger);
            coreExecutorService.execute(pubcompQueueManger);
        }
    }

    public void stop() {
        this.isRunning = false;
    }
}