package com.dreamgyf.mqtt.client;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.dreamgyf.mqtt.MqttPacketType;
import com.dreamgyf.mqtt.client.callback.MqttMessageCallback;
import com.dreamgyf.mqtt.packet.MqttPacket;
import com.dreamgyf.mqtt.packet.MqttPublishPacket;
import com.dreamgyf.mqtt.packet.MqttPubrelPacket;
import com.dreamgyf.utils.ByteUtils;
import com.dreamgyf.utils.MqttBuildUtils;

public class MqttMessageHandler implements Runnable {
    
    private ExecutorService executorService = Executors.newFixedThreadPool(10);

    private final Socket socket;

    private final Object socketLock;

    private List<MqttPacket> packetList;

    private final Object packetListLock;

    private MqttMessageCallback callback;

    private boolean isRunning = true;

    private Set<Short> packetIdSet = new HashSet<>();

    private final Object packetIdSetLock = new Object();

    private Queue<Short> pubrecQueue = new LinkedList<>(); 

    private final Object pubrecQueueLock = new Object();
    

    public MqttMessageHandler(final Socket socket, final Object socketLock, List<MqttPacket> packetList, Object packetListLock, MqttMessageCallback callback) {
        this.socket = socket;
        this.socketLock = socketLock;
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
                        }
                        else if(((MqttPublishPacket) mqttMessage).getQoS() == 1) {
                            byte[] packetId = ((MqttPublishPacket) mqttMessage).getPacketId();
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
                                        executorService.execute(new Runnable(){
                                            @Override
                                            public void run() {
                                                callback.messageArrived(topic, message);
                                            }
                                        });
                                    }
                                }
                            }
                        }
                        else if(((MqttPublishPacket) mqttMessage).getQoS() == 2) {
                            byte[] packetId = ((MqttPublishPacket) mqttMessage).getPacketId();
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
                                synchronized (pubrecQueueLock) {
                                    pubrecQueue.offer(ByteUtils.byte2ToShort(packetId));
                                    synchronized (socketLock) {
                                        if(socket.isConnected()) {
                                            try {
                                                OutputStream os = socket.getOutputStream();
                                                os.write(packet);
                                                PubrelListener pubrelListener = new PubrelListener(packetId, topic, message, callback);
                                                executorService.execute(pubrelListener);
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    }
                                }
                                synchronized (packetIdSetLock) {
                                    packetIdSet.remove(packageIdShort);packetIdSet.remove(packageIdShort);
                                }
                            }
                            
                        }
                        iterator.remove();
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

    private class PubrelListener implements Runnable {

        private byte[] id;

        private String topic;

        private String message;

        private MqttMessageCallback callback;

        private PubrelListener(byte[] id, String topic, String message, MqttMessageCallback callback) {
            super();
            this.id = id;
            this.topic = topic;
            this.message = message;
            this.callback = callback;
        }

        @Override
        public void run() {
            Thread.currentThread().setName("Thread-PubrelListener");
            boolean isPubreled = false;
            while (isRunning && !isPubreled){
                synchronized (packetListLock) {
                    Iterator<MqttPacket> iterator = packetList.iterator();
                    while(iterator.hasNext()){
                        MqttPacket mqttMessage = iterator.next();
                        if(mqttMessage instanceof MqttPubrelPacket) {
                            synchronized (pubrecQueueLock) {
                                if(Arrays.equals(((MqttPubrelPacket) mqttMessage).getPacketId(), id) && Arrays.equals(ByteUtils.shortToByte2(pubrecQueue.peek()), id)) {
                                    pubrecQueue.poll();
                                    iterator.remove();
                                    isPubreled = true;
                                    byte[] fixedHeader = new byte[2];
                                    fixedHeader[0] = MqttPacketType.PUBCOMP.getCode();
                                    fixedHeader[0] <<= 4;
                                    fixedHeader[1] = 0b00000010;
                                    byte[] variableHeader = id;
                                    byte[] packet = MqttBuildUtils.combineBytes(fixedHeader,variableHeader);
                                    synchronized (socketLock) {
                                        if(socket.isConnected()) {
                                            try {
                                                OutputStream os = socket.getOutputStream();
                                                os.write(packet);
                                                if(callback != null) {
                                                    executorService.execute(new Runnable(){
                                                        @Override
                                                        public void run() {
                                                            callback.messageArrived(topic, message);
                                                        }
                                                    });
                                                }
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    }
                                    synchronized (packetIdSetLock) {
                                        packetIdSet.remove(ByteUtils.byte2ToShort(id));
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

    protected void stop() {
        isRunning = false;
    }
}
