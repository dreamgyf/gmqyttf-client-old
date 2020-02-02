package com.dreamgyf.mqtt.client;

import com.dreamgyf.exception.MqttException;
import com.dreamgyf.mqtt.MqttPacketType;
import com.dreamgyf.mqtt.client.callback.MqttConnectStateCallback;
import com.dreamgyf.mqtt.client.callback.MqttMessageCallback;
import com.dreamgyf.mqtt.client.callback.MqttConnectCallback;
import com.dreamgyf.mqtt.client.callback.MqttPublishCallback;
import com.dreamgyf.mqtt.client.callback.MqttSubscribeCallback;
import com.dreamgyf.mqtt.client.callback.MqttUnsubscribeCallback;
import com.dreamgyf.mqtt.message.MqttConnackPacket;
import com.dreamgyf.mqtt.message.MqttPacket;
import com.dreamgyf.mqtt.message.MqttPubackPacket;
import com.dreamgyf.mqtt.message.MqttPubcompPacket;
import com.dreamgyf.mqtt.message.MqttPubrecPacket;
import com.dreamgyf.mqtt.message.MqttSubackPacket;
import com.dreamgyf.mqtt.message.MqttUnsubackPacket;
import com.dreamgyf.utils.ByteUtils;
import com.dreamgyf.utils.MqttBuildUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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

    private final Object socketLock = new Object();

    private final Object packetIdSetLock = new Object();

    private final Object packetListLock = new Object();

    private final Object publishQueueLock = new Object();

    private final Object pubrelQueueLock = new Object();

    private final Object subscribePacketIdSetLock = new Object();

    private Socket socket;

    private boolean isConnected = false;

    private String broker;

    private short keepAliveTime;

    private int port;

    private byte[] connectMessage;

    private Set<Short> packetIdSet = new HashSet<>();

    private List<MqttPacket> packetList = new ArrayList<>();

    private Queue<MqttPublishPacketBuilder> publishQueue = new LinkedList<>();

    private Queue<MqttPubrelPacketBuilder> pubrelQueue = new LinkedList<>();

    private Set<Short> subscribePacketIdSet = new HashSet<>();

    private MqttReceiver receiver;

    private MqttPing mqttPing;

    private MqttMessageHandler messageHandler;

    private MqttMessageCallback messageCallback;

    protected MqttClient(String broker, int port, byte[] message, short keepAliveTime) {
        this.broker = broker;
        this.port = port;
        this.connectMessage = message;
        this.keepAliveTime = keepAliveTime;
    }

    public void connect() throws IOException {
        connect(null);
    }

    public void connect(MqttConnectCallback callback) throws IOException {
        socket = new Socket(broker,port);
        OutputStream os = socket.getOutputStream();
        os.write(connectMessage);
        isConnected = socket.isConnected();

        //创建报文接收器
        receiver = new MqttReceiver(socket,packetList,packetListLock);
        executorService.execute(receiver);
        
        //创建心跳线程
        mqttPing = new MqttPing(socket, socketLock, keepAliveTime, packetList, packetListLock, new MqttConnectStateCallback(){
            @Override
            public void onDisconnected() {
                isConnected = false;
            }
        });
        executorService.execute(mqttPing);

        ConnackListener connackListener = new ConnackListener(callback);
        executorService.execute(connackListener);

        //创建消息处理器
        messageHandler = new MqttMessageHandler(packetList, packetListLock, messageCallback);
        executorService.execute(messageHandler);
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
                    Iterator<MqttPacket> iterator = packetList.iterator();
                    while(iterator.hasNext()){
                        MqttPacket mqttMessage = iterator.next();
                        if(mqttMessage instanceof MqttConnackPacket) {
                            isSucceed = ((MqttConnackPacket) mqttMessage).getReturnCode() == 0;
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
        synchronized (socketLock) {
            if(!isConnected)
                throw new MqttException("Disconnected");
            OutputStream os = socket.getOutputStream();
            os.write(packet);
            mqttPing.updateLastReqTime();
        }
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
                    Iterator<MqttPacket> iterator = packetList.iterator();
                    while(iterator.hasNext()){
                        MqttPacket mqttMessage = iterator.next();
                        if(mqttMessage instanceof MqttPubackPacket) {
                            synchronized (publishQueueLock) {
                                if(Arrays.equals(((MqttPubackPacket) mqttMessage).getPacketId(), id) && Arrays.equals(publishQueue.peek().getPacketId(), id)) {
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
                    Iterator<MqttPacket> iterator = packetList.iterator();
                    while(iterator.hasNext()){
                        MqttPacket mqttMessage = iterator.next();
                        if(mqttMessage instanceof MqttPubrecPacket) {
                            synchronized (publishQueueLock) {
                                if(Arrays.equals(((MqttPubrecPacket) mqttMessage).getPacketId(), id) && Arrays.equals(publishQueue.peek().getPacketId(), id)) {
                                    publishQueue.poll();
                                    iterator.remove();
                                    isPubreced = true;
                                    synchronized (pubrelQueueLock) {
                                        MqttPubrelPacketBuilder mqttPubrelPacketBuilder = new MqttPubrelPacketBuilder(id, topic, message, callback);
                                        pubrelQueue.offer(mqttPubrelPacketBuilder);
                                        byte[] packet = mqttPubrelPacketBuilder.build();
                                        
                                        synchronized (socketLock) {
                                            if(isConnected) {
                                                try {
                                                    OutputStream os = socket.getOutputStream();
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
                    Iterator<MqttPacket> iterator = packetList.iterator();
                    while(iterator.hasNext()){
                        MqttPacket mqttMessage = iterator.next();
                        if(mqttMessage instanceof MqttPubcompPacket) {
                            synchronized (pubrelQueueLock) {
                                if(Arrays.equals(((MqttPubcompPacket) mqttMessage).getPacketId(), id) && Arrays.equals(pubrelQueue.peek().getPacketId(), id)) {
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

    public void subscribe(MqttTopic topic) throws MqttException, IOException {
        subscribe(Arrays.asList(topic),null);
    }

    public void subscribe(MqttTopic topic, MqttSubscribeCallback callback) throws MqttException, IOException {
        subscribe(Arrays.asList(topic),callback);
    }

    public void subscribe(MqttTopic[] topics) throws MqttException, IOException {
        subscribe(Arrays.asList(topics),null);
    }

    public void subscribe(MqttTopic[] topics, MqttSubscribeCallback callback) throws MqttException, IOException {
        subscribe(Arrays.asList(topics),callback);
    }

    public void subscribe(Collection<MqttTopic> topics) throws MqttException, IOException {
        subscribe(topics,null);
    }

    public void subscribe(Collection<MqttTopic> topics, MqttSubscribeCallback callback) throws MqttException, IOException {
        byte[] header = new byte[1];
        header[0] = MqttPacketType.SUBSCRIBE.getCode();
        header[0] <<= 4;
        header[0] |= 0b00000010;
        short id;
        byte[] variableHeader;
        synchronized (packetIdSetLock) {
            do {
                id = (short) (new Random(System.currentTimeMillis()).nextInt(Short.MAX_VALUE - 1) + 1);
            } while(packetIdSet.contains(id));
            variableHeader = ByteUtils.shortToByte2(id);
            packetIdSet.add(id);
        }
        byte[] payLoad = new byte[0];
        for(MqttTopic topic : topics) {
            payLoad = MqttBuildUtils.combineBytes(payLoad, topic.buildSubscribePayLoadPacket());
        }
        //构建固定报头 Fixed header
        byte[] remainingLength = MqttBuildUtils.buildRemainingLength(variableHeader.length + payLoad.length);
        byte[] fixedHeader = MqttBuildUtils.combineBytes(header,remainingLength);
        //构建整个报文
        byte[] packet = MqttBuildUtils.combineBytes(fixedHeader,variableHeader,payLoad);
        //发送订阅
        synchronized (socketLock) {
            if(!isConnected)
                throw new MqttException("Disconnected");
            OutputStream os = socket.getOutputStream();
            os.write(packet);
            mqttPing.updateLastReqTime();
        }
        //放入订阅队列
        synchronized (subscribePacketIdSetLock) {
            subscribePacketIdSet.add(id);
        }
        
        SubackListener subackListener = new SubackListener(variableHeader, topics, callback);
        executorService.execute(subackListener);

    }

    private class SubackListener implements Runnable {

        private byte[] id;

        private Collection<MqttTopic> topics;

        private MqttSubscribeCallback callback;

        private SubackListener(byte[] id, Collection<MqttTopic> topics, MqttSubscribeCallback callback) {
            super();
            this.id = id;
            this.topics = topics;
            this.callback = callback;
        }

        @Override
        public void run() {
            Thread.currentThread().setName("Thread-SubackListener");
            boolean isSubacked = false;
            while (!isSubacked){
                synchronized (packetListLock) {
                    Iterator<MqttPacket> iterator = packetList.iterator();
                    while(iterator.hasNext()){
                        MqttPacket mqttMessage = iterator.next();
                        if(mqttMessage instanceof MqttSubackPacket) {
                            if(Arrays.equals(((MqttSubackPacket) mqttMessage).getPacketId(),id)) {
                                short shortId = ByteUtils.byte2ToShort(id);
                                synchronized (subscribePacketIdSetLock) {
                                    subscribePacketIdSet.remove(shortId);
                                }
                                synchronized (packetIdSetLock) {
                                    packetIdSet.remove(shortId);
                                }
                                byte[] returnCodeList = ((MqttSubackPacket) mqttMessage).getReturnCodeList();
                                iterator.remove();
                                isSubacked = true;
                                if(callback != null) {
                                    ExecutorService subackExecutor = Executors.newFixedThreadPool(10);
                                    Iterator<MqttTopic> topicIterator = topics.iterator();
                                    int pos = 0;
                                    while(topicIterator.hasNext()) {
                                        final int i = pos;
                                        final MqttTopic topic = topicIterator.next();
                                        subackExecutor.execute(new Runnable(){
                                            @Override
                                            public void run() {
                                                if(returnCodeList[i] == 0x80) {
                                                    callback.onFailure(topic.getTopic());
                                                }
                                                else {
                                                    callback.onSuccess(topic.getTopic(),returnCodeList[i]);
                                                }
                                            }
                                        });
                                        pos++;
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

    public void unsubscribe(MqttTopic topic) throws MqttException, IOException {
        unsubscribe(Arrays.asList(topic),null);
    }

    public void unsubscribe(MqttTopic topic, MqttUnsubscribeCallback callback) throws MqttException, IOException {
        unsubscribe(Arrays.asList(topic),callback);
    }

    public void unsubscribe(MqttTopic[] topics) throws MqttException, IOException {
        unsubscribe(Arrays.asList(topics),null);
    }

    public void unsubscribe(MqttTopic[] topics, MqttUnsubscribeCallback callback) throws MqttException, IOException {
        unsubscribe(Arrays.asList(topics),callback);
    }

    public void unsubscribe(Collection<MqttTopic> topics) throws MqttException, IOException {
        unsubscribe(topics,null);
    }

    public void unsubscribe(Collection<MqttTopic> topics, MqttUnsubscribeCallback callback) throws MqttException, IOException {
        byte[] header = new byte[2];
        header[0] = MqttPacketType.UNSUBSCRIBE.getCode();
        header[0] <<= 4;
        header[0] |= 0b00000010;
        short id;
        byte[] variableHeader;
        synchronized (packetIdSetLock) {
            do {
                id = (short) (new Random(System.currentTimeMillis()).nextInt(Short.MAX_VALUE - 1) + 1);
            } while(packetIdSet.contains(id));
            variableHeader = ByteUtils.shortToByte2(id);
            packetIdSet.add(id);
        }
        byte[] payLoad = new byte[0];
        for(MqttTopic topic : topics) {
            payLoad = MqttBuildUtils.combineBytes(payLoad, topic.buildUnsubscribePayLoadPacket());
        }
        //构建固定报头 Fixed header
        byte[] remainingLength = MqttBuildUtils.buildRemainingLength(variableHeader.length + payLoad.length);
        byte[] fixedHeader = MqttBuildUtils.combineBytes(header,remainingLength);
        //构建整个报文
        byte[] packet = MqttBuildUtils.combineBytes(fixedHeader,variableHeader,payLoad);
        //发送取消订阅
        synchronized (socketLock) {
            if(!isConnected)
                throw new MqttException("Disconnected");
            OutputStream os = socket.getOutputStream();
            os.write(packet);
            mqttPing.updateLastReqTime();
        }
        
        UnsubackListener unsubackListener = new UnsubackListener(variableHeader, topics, callback);
        executorService.execute(unsubackListener);

    }

    private class UnsubackListener implements Runnable {

        private byte[] id;

        private Collection<MqttTopic> topics;

        private MqttUnsubscribeCallback callback;

        private UnsubackListener(byte[] id, Collection<MqttTopic> topics, MqttUnsubscribeCallback callback) {
            super();
            this.id = id;
            this.topics = topics;
            this.callback = callback;
        }

        @Override
        public void run() {
            Thread.currentThread().setName("Thread-UnsubackListener");
            boolean isUnsubacked = false;
            while (!isUnsubacked){
                synchronized (packetListLock) {
                    Iterator<MqttPacket> iterator = packetList.iterator();
                    while(iterator.hasNext()){
                        MqttPacket mqttMessage = iterator.next();
                        if(mqttMessage instanceof MqttUnsubackPacket) {
                            if(Arrays.equals(((MqttUnsubackPacket) mqttMessage).getPacketId(),id)) {
                                short shortId = ByteUtils.byte2ToShort(id);
                                synchronized (packetIdSetLock) {
                                    packetIdSet.remove(shortId);
                                }
                                iterator.remove();
                                isUnsubacked = true;
                            }
                        }
                    }
                }

                if(isUnsubacked && callback != null) {
                    callback.onSuccess(topics);
                }
                
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void disconnect() throws MqttException, IOException {
        byte[] packet = new byte[2];
        packet[0] = MqttPacketType.DISCONNECT.getCode();
        packet[0] <<= 4;
        receiver.stop();
        mqttPing.stop();
        mqttPing = null;
        messageHandler.stop();
        messageHandler = null;
        isConnected = false;
        synchronized (socketLock) {
            if(socket.isConnected()) {
                OutputStream os = socket.getOutputStream();
                os.write(packet);
                socket.close();
            }
        }
    }

    public void setCallback(MqttMessageCallback callback) {
        this.messageCallback = callback;
    }
}
