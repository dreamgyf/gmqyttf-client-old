package com.dreamgyf.mqtt.client;

import com.dreamgyf.exception.MqttException;
import com.dreamgyf.mqtt.client.callback.MqttConnectCallback;
import com.dreamgyf.mqtt.client.callback.MqttPublishCallback;
import com.dreamgyf.mqtt.message.MqttConnackMessage;
import com.dreamgyf.mqtt.message.MqttMessage;
import com.dreamgyf.mqtt.message.MqttPubackMessage;
import com.dreamgyf.mqtt.message.MqttPubcompMessage;
import com.dreamgyf.mqtt.message.MqttPubrecMessage;
import com.dreamgyf.utils.ByteUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MqttClient {

    private ExecutorService executorService = Executors.newFixedThreadPool(10);

    private final Object packetIdSetLock = new Object();

    private final Object packetListLock = new Object();

    private final Object publishQueueLock = new Object();

    private final Object pubrelQueueLock = new Object();

    private Socket socket;

    private boolean isConnected = false;

    private String broker;

    private int port;

    private byte[] connectMessage;

    private Set<Short> packetIdSet = new HashSet<>();

    private List<MqttMessage> packetList = new ArrayList<>();

    private Queue<MqttPublishPacketBuilder> publishQueue = new LinkedList<>();

    private Queue<MqttPubrelPacketBuilder> pubrelQueue = new LinkedList<>();

    MqttReceiver receiver;

    protected MqttClient(String broker, int port, byte[] message) {
        this.broker = broker;
        this.port = port;
        this.connectMessage = message;
    }

    public void connect() throws IOException {
        connect(null);
    }

    public void connect(MqttConnectCallback callback) throws IOException {
        socket = new Socket(broker,port);
        OutputStream os = socket.getOutputStream();
        os.write(connectMessage);
        isConnected = socket.isConnected();

        receiver = new MqttReceiver(socket,packetList,packetListLock);
        executorService.execute(receiver);
        ConnackListener connackListener = new ConnackListener(callback);
        executorService.execute(connackListener);
    }

    public boolean isConnected() {
        return isConnected;
    }

    private class ConnackListener implements Runnable {

        private MqttConnectCallback callback;

        private ConnackListener(MqttConnectCallback callback) {
            super();
            this.callback = callback;
        }

        @Override
        public void run() {
            Thread.currentThread().setName("Thread-ConnackListener");
            boolean isConnacked = false;
            boolean isSucceed = false;
            while (!isConnacked){
                synchronized (packetListLock) {
                    Iterator<MqttMessage> iterator = packetList.iterator();
                    while(iterator.hasNext()){
                        MqttMessage mqttMessage = iterator.next();
                        if(mqttMessage instanceof MqttConnackMessage) {
                            isSucceed = ((MqttConnackMessage) mqttMessage).getReturnCode() == 0;
                            iterator.remove();
                            isConnacked = true;
                        }
                    }
                }
                if(isConnacked && callback != null) {
                    if(isSucceed)
                        callback.onSuccess();
                    else {
                        isConnected = false;
                        callback.onFailure();
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

    public void publish(String topic, String message) throws IOException, MqttException {
        publish(topic, message, new MqttPublishOptions(), null);
    }

    public void publish(String topic, String message, MqttPublishCallback callback) throws IOException, MqttException {
        publish(topic, message, new MqttPublishOptions(),callback);
    }

    public void publish(String topic, String message, MqttPublishOptions options) throws IOException, MqttException {
        publish(topic, message, options,null);
    }

    public void publish(String topic, String message, MqttPublishOptions options, MqttPublishCallback callback) throws IOException, MqttException {
        MqttPublishPacketBuilder mqttPublishPacketBuilder;
        byte[] packetId = new byte[0];
        if(options.getQoS() != 0) {
            short id;
            synchronized (packetIdSetLock) {
                do {
                    id = (short) (new Random(System.currentTimeMillis()).nextInt(Short.MAX_VALUE - 1) + 1);
                } while(packetIdSet.contains(id));
                packetId = ByteUtils.shortToByte2(id);
                mqttPublishPacketBuilder = new MqttPublishPacketBuilder(packetId, topic, message, options, callback);
                packetIdSet.add(id);
            }
        }
        else
            mqttPublishPacketBuilder = new MqttPublishPacketBuilder(new byte[0], topic, message, options, callback);
        byte[] packet = mqttPublishPacketBuilder.build();
        //发送消息
        if(!isConnected)
            throw new MqttException("Disconnected");
        OutputStream os = socket.getOutputStream();
        os.write(packet);
        //放入发布队列
        if(options.getQoS() != 0) {
            publishQueue.offer(mqttPublishPacketBuilder);
        }

        if(options.getQoS() == 1) {
            PubackListener pubackListener = new PubackListener(packetId,topic,message,callback);
            executorService.execute(pubackListener);
        } 
        else if(options.getQoS() == 2) {
            PubrecListener pubrecListener = new PubrecListener(packetId,topic,message,callback);
            executorService.execute(pubrecListener);
        }
           
    }

    private class PubackListener implements Runnable {

        private byte[] id;

        private String topic;

        private String message;

        private MqttPublishCallback callback;

        private PubackListener(byte[] id, String topic, String message, MqttPublishCallback callback) {
            super();
            this.id = id;
            this.topic = topic;
            this.message = message;
            this.callback = callback;
        }

        @Override
        public void run() {
            Thread.currentThread().setName("Thread-PubackListener");
            boolean isPubacked = false;
            while (!isPubacked){
                synchronized (packetListLock) {
                    Iterator<MqttMessage> iterator = packetList.iterator();
                    while(iterator.hasNext()){
                        MqttMessage mqttMessage = iterator.next();
                        if(mqttMessage instanceof MqttPubackMessage) {
                            synchronized (publishQueueLock) {
                                if(Arrays.equals(((MqttPubackMessage) mqttMessage).getPacketId(), id) && Arrays.equals(publishQueue.peek().getPacketId(), id)) {
                                    publishQueue.poll();
                                    synchronized (packetIdSetLock) {
                                        packetIdSet.remove(ByteUtils.byte2ToShort(id));
                                    }
                                    iterator.remove();
                                    isPubacked = true;
                                }
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
    }

    private class PubrecListener implements Runnable {

        private byte[] id;

        private String topic;

        private String message;

        private MqttPublishCallback callback;

        private PubrecListener(byte[] id, String topic, String message, MqttPublishCallback callback) {
            super();
            this.id = id;
            this.topic = topic;
            this.message = message;
            this.callback = callback;
        }

        @Override
        public void run() {
            Thread.currentThread().setName("Thread-PubrecListener");
            boolean isPubreced = false;
            while (!isPubreced){
                synchronized (packetListLock) {
                    Iterator<MqttMessage> iterator = packetList.iterator();
                    while(iterator.hasNext()){
                        MqttMessage mqttMessage = iterator.next();
                        if(mqttMessage instanceof MqttPubrecMessage) {
                            synchronized (publishQueueLock) {
                                if(Arrays.equals(((MqttPubrecMessage) mqttMessage).getPacketId(), id) && Arrays.equals(publishQueue.peek().getPacketId(), id)) {
                                    publishQueue.poll();
                                    iterator.remove();
                                    isPubreced = true;
                                    synchronized (pubrelQueueLock) {
                                        MqttPubrelPacketBuilder mqttPubrelPacketBuilder = new MqttPubrelPacketBuilder(id, topic, message, callback);
                                        pubrelQueue.offer(mqttPubrelPacketBuilder);
                                        byte[] packet = mqttPubrelPacketBuilder.build();
                                        if(isConnected) {
                                            OutputStream os;
                                            try {
                                                os = socket.getOutputStream();
                                                os.write(packet);
                                                PubcompListener pubcompListener = new PubcompListener(id,topic,message,callback);
                                                executorService.execute(pubcompListener);
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                        
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

    private class PubcompListener implements Runnable {

        private byte[] id;

        private String topic;

        private String message;

        private MqttPublishCallback callback;

        private PubcompListener(byte[] id, String topic, String message, MqttPublishCallback callback) {
            super();
            this.id = id;
            this.topic = topic;
            this.message = message;
            this.callback = callback;
        }

        @Override
        public void run() {
            Thread.currentThread().setName("Thread-PubcompListener");
            boolean isPubcomped = false;
            while (!isPubcomped){
                synchronized (packetListLock) {
                    Iterator<MqttMessage> iterator = packetList.iterator();
                    while(iterator.hasNext()){
                        MqttMessage mqttMessage = iterator.next();
                        if(mqttMessage instanceof MqttPubcompMessage) {
                            synchronized (pubrelQueueLock) {
                                if(Arrays.equals(((MqttPubcompMessage) mqttMessage).getPacketId(), id) && Arrays.equals(pubrelQueue.peek().getPacketId(), id)) {
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
    }
}
