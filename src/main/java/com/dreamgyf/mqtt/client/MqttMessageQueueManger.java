package com.dreamgyf.mqtt.client;

import com.dreamgyf.mqtt.client.callback.MqttPublishCallback;
import com.dreamgyf.mqtt.packet.MqttPacket;
import com.dreamgyf.mqtt.packet.MqttPubackPacket;
import com.dreamgyf.mqtt.packet.MqttPubcompPacket;
import com.dreamgyf.mqtt.packet.MqttPubrecPacket;
import com.dreamgyf.utils.ByteUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class MqttMessageQueueManger {

    private ExecutorService executorService = Executors.newFixedThreadPool(10);

    private final Socket socket;

    private final Object socketLock;
    
    private final List<MqttPacket> packetList;

    private final Object packetListLock;

    private final Set<Short> packetIdSet = new HashSet<>();

    private final Object packetIdSetLock = new Object();

    private final MqttPing mqttPing;

    private final Queue<MqttPublishPacketBuilder> publishQueue;

    private final Queue<MqttPubrelPacketBuilder> pubrelQueue;

    private final Object publishQueueLock = new Object();

    private final Object pubrelQueueLock = new Object();

    private boolean isRunning = false;

    private MqttPublishQueueManger publishQueueManger = new MqttPublishQueueManger();

    private MqttPubrelQueueManger pubrelQueueManger = new MqttPubrelQueueManger();

    protected MqttMessageQueueManger(final Socket socket, final Object socketLock, final List<MqttPacket> packetList, final Object packetListLock,
                                    final Set<Short> packetIdSet, final Object packetIdSetLock,
                                    final MqttPing mqttPing, final Queue<MqttPublishPacketBuilder> publishQueue, final Queue<MqttPubrelPacketBuilder> pubrelQueue) {
        this.socket = socket;
        this.socketLock = socketLock;
        this.packetList = packetList;
        this.packetListLock = packetListLock;
        this.mqttPing = mqttPing;
        this.publishQueue = publishQueue;
        this.pubrelQueue = pubrelQueue;
    }

    private class MqttPublishQueueManger implements Runnable {

        @Override
        public void run() {
            Thread.currentThread().setName("Thread-MqttPublishQueueManger");
            while(isRunning) {
                if(publishQueue.peek() != null) {
                    MqttPublishPacketBuilder publishPacketBuilder = publishQueue.peek();
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
                        synchronized (publishQueueLock) {
                            publishQueue.poll();
                        }
                        if(callback != null) {
                            callback.messageArrived(topic, message);
                        }
                    }
                    else if(options.getQoS() == 1) {
                        boolean isPubacked = false;
                        while (!isPubacked){
                            synchronized (packetListLock) {
                                Iterator<MqttPacket> iterator = packetList.iterator();
                                while(iterator.hasNext()){
                                    MqttPacket mqttMessage = iterator.next();
                                    if(mqttMessage instanceof MqttPubackPacket) {
                                        if(Arrays.equals(((MqttPubackPacket) mqttMessage).getPacketId(), id) && publishQueue.peek() != null && Arrays.equals(publishQueue.peek().getPacketId(), id)) {
                                            synchronized (publishQueueLock) {
                                                publishQueue.poll();
                                            }
                                            synchronized (packetIdSetLock) {
                                                packetIdSet.remove(ByteUtils.byte2ToShort(id));
                                            }
                                            iterator.remove();
                                            isPubacked = true;
                                        }
                                    }
                                }
                            }
                            if(isPubacked && callback != null)
                                callback.messageArrived(topic,message);
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    } 
                    else if(options.getQoS() == 2) {
                        boolean isPubreced = false;
                        while (!isPubreced){
                            synchronized (packetListLock) {
                                Iterator<MqttPacket> iterator = packetList.iterator();
                                while(iterator.hasNext()){
                                    MqttPacket mqttMessage = iterator.next();
                                    if(mqttMessage instanceof MqttPubrecPacket) {
                                        synchronized (publishQueueLock) {
                                            if(Arrays.equals(((MqttPubrecPacket) mqttMessage).getPacketId(), id) && publishQueue.peek() != null && Arrays.equals(publishQueue.peek().getPacketId(), id)) {
                                                publishQueue.poll();
                                                iterator.remove();
                                                isPubreced = true;
                                                synchronized (pubrelQueueLock) {
                                                    MqttPubrelPacketBuilder mqttPubrelPacketBuilder = new MqttPubrelPacketBuilder(id, topic, message, callback);
                                                    pubrelQueue.offer(mqttPubrelPacketBuilder);
                                                }
                                            }
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
                }

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class MqttPubrelQueueManger implements Runnable {

        @Override
        public void run() {
            Thread.currentThread().setName("Thread-MqttPubrelQueueManger");
            while(isRunning) {
                if(pubrelQueue.peek() != null) {
                    MqttPubrelPacketBuilder pubrelPacketBuilder = pubrelQueue.peek();
                    byte[] id = pubrelPacketBuilder.getId();
                    String topic = pubrelPacketBuilder.getTopic();
                    String message = pubrelPacketBuilder.getMessage();
                    MqttPublishCallback callback = pubrelPacketBuilder.getCallback();
                    byte[] packet = pubrelPacketBuilder.build();
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
                    boolean isPubcomped = false;
                    while (!isPubcomped){
                        synchronized (packetListLock) {
                            Iterator<MqttPacket> iterator = packetList.iterator();
                            while(iterator.hasNext()){
                                MqttPacket mqttMessage = iterator.next();
                                if(mqttMessage instanceof MqttPubcompPacket) {
                                    synchronized (pubrelQueueLock) {
                                        if(Arrays.equals(((MqttPubcompPacket) mqttMessage).getPacketId(), id) && pubrelQueue.peek() != null && Arrays.equals(pubrelQueue.peek().getPacketId(), id)) {
                                            pubrelQueue.poll();
                                            synchronized (packetIdSetLock) {
                                                packetIdSet.remove(ByteUtils.byte2ToShort(id));
                                            }
                                            iterator.remove();
                                            isPubcomped = true;
                                        }
                                    }
                                }
                            }
                        }
                        if(isPubcomped && callback != null)
                            callback.messageArrived(topic,message);
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
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
    }

    public void run() {
        if(!isRunning) {
            isRunning = true;
            executorService = Executors.newFixedThreadPool(10);
            executorService.execute(publishQueueManger);
            executorService.execute(pubrelQueueManger);
        }
    }

    public void stop() {
        this.isRunning = false;
        executorService.shutdownNow();
    }
}