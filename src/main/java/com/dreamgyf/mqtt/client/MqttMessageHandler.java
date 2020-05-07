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
    
    private ExecutorService executorService = Executors.newFixedThreadPool(10);

    private final Socket socket;

    private final Object socketLock;

    private final LinkedBlockingQueue<MqttPublishPacket> publishQueue;

    private final LinkedBlockingQueue<MqttPubrelPacket> pubrelQueue;

    private MqttMessageCallback callback;

    private boolean isRunning = false;

    private Set<Short> packetIdSet = new HashSet<>();

    private final Object packetIdSetLock = new Object();

    private final MqttPublishMessageHandler mqttPublishMessageHandler = new MqttPublishMessageHandler();

    private final MqttPubrelMessageHandler mqttPubrelMessageHandler = new MqttPubrelMessageHandler();

    private final Object callbackLock = new Object();

    public MqttMessageHandler(final Socket socket, final Object socketLock,
                              final LinkedBlockingQueue<MqttPublishPacket> publishQueue,
                              final LinkedBlockingQueue<MqttPubrelPacket> pubrelQueue, MqttMessageCallback callback) {
        this.socket = socket;
        this.socketLock = socketLock;
        this.publishQueue = publishQueue;
        this.pubrelQueue = pubrelQueue;
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
                            callback.messageArrived(topic, message);
                        }
                    }
                    else if(publishPacket.getQoS() == 1) {
                        byte[] packetId = publishPacket.getPacketId();
                        short packageIdShort = ByteUtils.byte2ToShort(packetId);
                        synchronized (packetIdSetLock) {
                            if(!packetIdSet.contains(packageIdShort)) {
                                packetIdSet.add(packageIdShort);
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
                                packetIdSet.remove(packageIdShort);
                                if(callback != null) {
                                    callback.messageArrived(topic, message);
                                }
                            }
                        }
                    }
                    else if(publishPacket.getQoS() == 2) {
                        byte[] packetId = publishPacket.getPacketId();
                        short packageIdShort = ByteUtils.byte2ToShort(packetId);
                        if(!packetIdSet.contains(packageIdShort)) {
                            synchronized (packetIdSetLock) {
                                packetIdSet.add(packageIdShort);
                            }
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
                            synchronized (packetIdSetLock) {
                                packetIdSet.remove(packageIdShort);packetIdSet.remove(packageIdShort);
                            }
                            if(callback != null) {
                                callback.messageArrived(topic, message);
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
                    synchronized (packetIdSetLock) {
                        packetIdSet.remove(ByteUtils.byte2ToShort(packetId));
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
            executorService = Executors.newFixedThreadPool(10);
            executorService.execute(mqttPublishMessageHandler);
            executorService.execute(mqttPubrelMessageHandler);
        }
    }

    protected void stop() {
        isRunning = false;
    }
}
