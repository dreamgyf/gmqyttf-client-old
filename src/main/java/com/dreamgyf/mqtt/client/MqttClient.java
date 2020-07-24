package com.dreamgyf.mqtt.client;

import com.dreamgyf.exception.MqttException;
import com.dreamgyf.mqtt.MqttPacketType;
import com.dreamgyf.mqtt.MqttVersion;
import com.dreamgyf.mqtt.client.callback.*;
import com.dreamgyf.mqtt.packet.MqttConnackPacket;
import com.dreamgyf.mqtt.packet.MqttSubackPacket;
import com.dreamgyf.mqtt.packet.MqttUnsubackPacket;
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
import java.util.regex.Pattern;

public class MqttClient {

    public static class Builder {

        private MqttVersion version = MqttVersion.V_3_1_1;
    
        /**
         * 清理会话 Clean Session
         */
        private boolean cleanSession;
    
        /**
         * 遗嘱标志 Will Flag
         */
        private boolean willFlag;
    
        /**
         * 遗嘱QoS Will QoS
         */
        private int willQoS = 0;
    
        /**
         * 遗嘱保留 Will Retain
         */
        private boolean willRetain;
    
        /**
         * 用户名标志 User Name Flag
         */
        private boolean usernameFlag;
    
        /**
         * 密码标志 Password Flag
         */
        private boolean passwordFlag;
    
        /**
         * 保持连接 Keep Alive
         */
        private int keepAliveTime = 10;
    
        /**
         * 客户端标识符 Client Identifier
         */
        private String clientId = "default";
    
        /**
         * 遗嘱主题 Will Topic
         */
        private String willTopic = "";
    
        /**
         * 遗嘱消息 Will Message
         */
        private String willMessage = "";
    
        /**
         * 用户名 User Name
         */
        private String username = "";
    
        /**
         * 密码 Password
         */
        private String password = "";

        private String broker = null;

        private int port = 0;
        
        public Builder() {
        }
    
        public MqttVersion getVersion() {
            return this.version;
        }
    
        public Builder setVersion(MqttVersion version) {
            this.version = version;
            return this;
        }
    
        public boolean getCleanSession() {
            return this.cleanSession;
        }
    
        public Builder setCleanSession(boolean cleanSession) {
            this.cleanSession = cleanSession;
            return this;
        }
    
        public boolean getWillFlag() {
            return this.willFlag;
        }
    
        public Builder setWillFlag(boolean willFlag) {
            this.willFlag = willFlag;
            return this;
        }
    
        public int getWillQoS() {
            return this.willQoS;
        }
    
        public Builder setWillQoS(int willQoS) {
            if(willQoS < 0 || willQoS > 2)
                throw new IllegalArgumentException("The value of QoS must be between 0 and 2.");
            this.willQoS = willQoS;
            return this;
        }
    
        public boolean getWillRetain() {
            return this.willRetain;
        }
    
        public Builder setWillRetain(boolean willRetain) {
            this.willRetain = willRetain;
            return this;
        }
    
        public boolean getUsernameFlag() {
            return this.usernameFlag;
        }
    
        public Builder setUsernameFlag(boolean usernameFlag) {
            this.usernameFlag = usernameFlag;
            return this;
        }
    
        public boolean getPasswordFlag() {
            return this.passwordFlag;
        }
    
        public Builder setPasswordFlag(boolean passwordFlag) {
            this.passwordFlag = passwordFlag;
            return this;
        }
    
        public int getKeepAliveTime() {
            return this.keepAliveTime;
        }
    
        public Builder setKeepAliveTime(int keepAliveTime) {
            this.keepAliveTime = keepAliveTime;
            return this;
        }
    
        public String getClientId() {
            return this.clientId;
        }
    
        public Builder setClientId(String clientId) {
            if(!Pattern.matches("^[a-zA-Z0-9]+$",clientId))
                throw new IllegalArgumentException("illegal character,Client ID can only contain letters and Numbers");
            this.clientId = clientId;
            return this;
        }
    
        public String getWillTopic() {
            return this.willTopic;
        }
    
        public Builder setWillTopic(String willTopic) {
            this.willTopic = willTopic;
            return this;
        }
    
        public String getWillMessage() {
            return this.willMessage;
        }
    
        public Builder setWillMessage(String willMessage) {
            this.willMessage = willMessage;
            return this;
        }
    
        public String getUsername() {
            return this.username;
        }
    
        public Builder setUsername(String username) {
            this.username = username;
            return this;
        }
    
        public String getPassword() {
            return this.password;
        }
    
        public Builder setPassword(String password) {
            this.password = password;
            return this;
        }

        public String getBroker() {
            return this.broker;
        }

        public Builder setBroker(String broker) {
            this.broker = broker;
            return this;
        }

        public int getPort() {
            return this.port;
        }

        public Builder setPort(int port) {
            this.port = port;
            return this;
        }

        public MqttClient build() {
            return new MqttClient(version, cleanSession, willFlag, willQoS, willRetain, usernameFlag, passwordFlag, keepAliveTime, clientId, willTopic, willMessage, username, password, broker, port);
        }
    }
    
    private MqttVersion version;
    
    /**
     * 清理会话 Clean Session
     */
    private boolean cleanSession;

    /**
     * 遗嘱标志 Will Flag
     */
    private boolean willFlag;

    /**
     * 遗嘱QoS Will QoS
     */
    private int willQoS = 0;

    /**
     * 遗嘱保留 Will Retain
     */
    private boolean willRetain;

    /**
     * 用户名标志 User Name Flag
     */
    private boolean usernameFlag;

    /**
     * 密码标志 Password Flag
     */
    private boolean passwordFlag;

    /**
     * 保持连接 Keep Alive
     */
    private int keepAliveTime = 10;

    /**
     * 客户端标识符 Client Identifier
     */
    private String clientId = "default";

    /**
     * 遗嘱主题 Will Topic
     */
    private String willTopic = "";

    /**
     * 遗嘱消息 Will Message
     */
    private String willMessage = "";

    /**
     * 用户名 User Name
     */
    private String username = "";

    /**
     * 密码 Password
     */
    private String password = "";

    private String broker = null;

    private int port = 0;

    public MqttVersion getVersion() {
        return this.version;
    }

    public void setVersion(MqttVersion version) {
        this.version = version;
    }

    public boolean getCleanSession() {
        return this.cleanSession;
    }

    public void setCleanSession(boolean cleanSession) {
        this.cleanSession = cleanSession;
    }

    public boolean getWillFlag() {
        return this.willFlag;
    }

    public void setWillFlag(boolean willFlag) {
        this.willFlag = willFlag;
    }

    public int getWillQoS() {
        return this.willQoS;
    }

    public void setWillQoS(int willQoS) {
        if(willQoS < 0 || willQoS > 2)
            throw new IllegalArgumentException("The value of QoS must be between 0 and 2.");
        this.willQoS = willQoS;
    }

    public boolean getWillRetain() {
        return this.willRetain;
    }

    public void setWillRetain(boolean willRetain) {
        this.willRetain = willRetain;
    }

    public boolean getUsernameFlag() {
        return this.usernameFlag;
    }

    public void setUsernameFlag(boolean usernameFlag) {
        this.usernameFlag = usernameFlag;
    }

    public boolean getPasswordFlag() {
        return this.passwordFlag;
    }

    public void setPasswordFlag(boolean passwordFlag) {
        this.passwordFlag = passwordFlag;
    }

    public int getKeepAliveTime() {
        return this.keepAliveTime;
    }

    public void setKeepAliveTime(int keepAliveTime) {
        this.keepAliveTime = keepAliveTime;
    }

    public String getClientId() {
        return this.clientId;
    }

    public void setClientId(String clientId) {
        if(!Pattern.matches("^[a-zA-Z0-9]+$",clientId))
            throw new IllegalArgumentException("illegal character,Client ID can only contain letters and Numbers");
        this.clientId = clientId;
    }

    public String getWillTopic() {
        return this.willTopic;
    }

    public void setWillTopic(String willTopic) {
        this.willTopic = willTopic;
    }

    public String getWillMessage() {
        return this.willMessage;
    }

    public void setWillMessage(String willMessage) {
        this.willMessage = willMessage;
    }

    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getBroker() {
        return this.broker;
    }

    public void setBroker(String broker) {
        this.broker = broker;
    }

    public int getPort() {
        return this.port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    private MqttClient(MqttVersion version, boolean cleanSession, boolean willFlag, int willQoS, 
                        boolean willRetain, boolean usernameFlag, boolean passwordFlag, int keepAliveTime, 
                        String clientId, String willTopic, String willMessage, String username, String password, 
                        String broker, int port) {
        this.version = version;
        this.cleanSession = cleanSession;
        this.willFlag = willFlag;
        this.willQoS = willQoS;
        this.willRetain = willRetain;
        this.usernameFlag = usernameFlag;
        this.passwordFlag = passwordFlag;
        this.clientId = clientId;
        this.willTopic = willTopic;
        this.willMessage = willMessage;
        this.username = username;
        this.password = password;
        this.broker = broker;
        this.port = port;
    }

    /**
     * MQTT报文处理核心线程池
     */
    private ExecutorService coreExecutorService = Executors.newFixedThreadPool(30);

    private final Object socketLock = new Object();

    private volatile Socket socket;

    private volatile boolean isConnected = false;
    /**
     * MQTT数据包接收队列
     */
    private final MqttPacketQueue mqttPacketQueue = new MqttPacketQueue();
    /**
     * MQTT消息回调队列
     */
    private final LinkedBlockingQueue<MqttCallbackEntity> callbackQueue = new LinkedBlockingQueue<>();
    /**
     * 客户端Packet Id集合
     */
    private final Set<Short> clientPacketIdSet = ConcurrentHashMap.newKeySet();
    /**
     * 服务端Packet Id集合
     */
    private final Set<Short> serverPacketIdSet = ConcurrentHashMap.newKeySet();
    /**
     * MQTT发送publish队列
     */
    private final LinkedBlockingQueue<MqttPublishPacketBuilder> publishQueue = new LinkedBlockingQueue<>();
    /**
     * MQTT数据包接收器
     */
    private MqttReceiver receiver;
    /**
     * MQTT心跳
     */
    private MqttPing mqttPing;
    /**
     * MQTT消息接收处理器
     */
    private MqttMessageHandler messageHandler;
    /**
     * MQTT消息发送队列管理器
     */
    private MqttMessageQueueManger messageQueueManger;
    /**
     * MQTT消息回调队列管理器
     */
    private MqttCallbackQueueManger callbackQueueManger;
    /**
     * MQTT接收到消息后的回调接口
     */
    private MqttMessageCallback messageCallback;

    /**
     * 清空连接所有状态
     */
    private void clean() {
        if(cleanSession) {
            mqttPacketQueue.clear();
            publishQueue.clear();
            clientPacketIdSet.clear();
            serverPacketIdSet.clear();
            callbackQueue.clear();
        }
    }

    public void connect() throws IOException, MqttException {
        connect(null);
    }

    public void connect(MqttConnectCallback callback) throws IOException, MqttException {
        if(broker == null || broker.equals("") || port == 0)
            throw new MqttException("Need to set borker and port");
        if(isConnected)
            throw new MqttException("Already Connected!");
        //构建连接报文
        //构建可变报头 Variable header
        byte[] protocolName = version.getProtocolName();
        byte protocolLevel = version.getProtocolLevel();
        //构建连接标志 Connect Flags
        byte connectFlags = 0;
        if(cleanSession)
            connectFlags |= 0b00000010;
        if(willFlag) {
            connectFlags |= 0b00000100;
            connectFlags |= (willQoS << 3);
            if(willRetain)
                connectFlags |= 0b00100000;
        }
        if(usernameFlag)
            connectFlags |= 0b10000000;
        if(passwordFlag)
            connectFlags |= 0b01000000;
        byte[] variableHeader = new byte[protocolName.length + 4]; //Protocol Name + Protocol Level + Connect Flags + Keep Alive
        int pos = 0;
        while(pos < protocolName.length) {
            variableHeader[pos] = protocolName[pos];
            pos++;
        }
        variableHeader[pos++] = protocolLevel;
        variableHeader[pos++] = connectFlags;
        byte[] keepAlive = ByteUtils.shortToByte2((short) keepAliveTime);
        variableHeader[pos++] = keepAlive[0];
        variableHeader[pos] = keepAlive[1];
        //构建客户端标识符 Client Identifier
        byte[] clientIdByte = MqttBuildUtils.utf8EncodedStrings(clientId);
        //构建遗嘱主题 Will Topic 遗嘱消息 Will Message
        byte[] willTopicByte = new byte[0];
        byte[] willMessageByte = new byte[0];
        if(willFlag) {
            willTopicByte = MqttBuildUtils.utf8EncodedStrings(willTopic);
            willMessageByte = MqttBuildUtils.utf8EncodedStrings(willTopic);
        }
        //构建用户名 User Name
        byte[] usernameByte = new byte[0];
        if(usernameFlag)
            usernameByte = MqttBuildUtils.utf8EncodedStrings(username);
        //构建密码 Password
        byte[] passwordByte = new byte[0];
        if(passwordFlag)
            passwordByte = MqttBuildUtils.utf8EncodedStrings(password);
        //构建有效载荷 Payload
        byte[] payLoad = MqttBuildUtils.combineBytes(clientIdByte,willTopicByte,willMessageByte,usernameByte,passwordByte);
        //构建固定报头 Fixed header
        byte[] remainingLength = MqttBuildUtils.buildRemainingLength(variableHeader.length + payLoad.length);
        byte[] header = new byte[1];
        header[0] = 0b00010000;
        byte[] fixedHeader = MqttBuildUtils.combineBytes(header,remainingLength);
        //构建整个报文
        byte[] packet = MqttBuildUtils.combineBytes(fixedHeader,variableHeader,payLoad);

        clean();

        socket = new Socket(broker,port);
        OutputStream os = socket.getOutputStream();
        os.write(packet);

        //创建报文接收器
        receiver = new MqttReceiver(socket,mqttPacketQueue);
        coreExecutorService.execute(receiver);
        
        //创建心跳线程
        mqttPing = new MqttPing(coreExecutorService, socket, socketLock, mqttPacketQueue.pingresp, keepAliveTime, new MqttConnectStateCallback(){
            @Override
            public void onDisconnected() {
                isConnected = false;
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                stopService();
            }
        });
        coreExecutorService.execute(mqttPing);

        ConnackListener connackListener = new ConnackListener(callback);
        coreExecutorService.execute(connackListener);

        //创建消息接收处理器
        messageHandler = new MqttMessageHandler(coreExecutorService, socket, socketLock,
                mqttPacketQueue.publish,mqttPacketQueue.pubrel, serverPacketIdSet,
                callbackQueue, messageCallback);
        messageHandler.run();

        //创建消息发送队列管理器
        messageQueueManger = new MqttMessageQueueManger(coreExecutorService, socket, socketLock,
                mqttPacketQueue.puback,mqttPacketQueue.pubrec,mqttPacketQueue.pubcomp, callbackQueue,
                clientPacketIdSet, mqttPing, publishQueue);
        messageQueueManger.run();

        callbackQueueManger = new MqttCallbackQueueManger(callbackQueue);
        coreExecutorService.execute(callbackQueueManger);
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
                try {
                    MqttConnackPacket connackPacket = mqttPacketQueue.connack.take();
                    isSucceed = connackPacket.getReturnCode() == 0;
                    isConnacked = true;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if(isConnacked && callback != null) {
                    if(isSucceed) {
                        isConnected = true;
                        callback.onSuccess();
                    }
                    else {
                        isConnected = false;
                        callback.onFailure();
                    }
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
            synchronized (clientPacketIdSet) {
                do {
                    id = (short) (new Random(System.currentTimeMillis()).nextInt(Short.MAX_VALUE - 1) + 1);
                } while(clientPacketIdSet.contains(id));
                packetId = ByteUtils.shortToByte2(id);
                mqttPublishPacketBuilder = new MqttPublishPacketBuilder(packetId, topic, message, options, callback);
                clientPacketIdSet.add(id);
            }
        }
        else
            mqttPublishPacketBuilder = new MqttPublishPacketBuilder(new byte[0], topic, message, options, callback);
        try {
            publishQueue.put(mqttPublishPacketBuilder);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void subscribe(MqttTopic topic) throws MqttException, IOException {
        subscribe(Arrays.asList(topic),null);
    }

    public void subscribe(MqttTopic topic, MqttSubscribeCallback callback) throws MqttException, IOException {
        subscribe(Arrays.asList(topic),callback);
    }

    public void subscribe(MqttTopic... topics) throws MqttException, IOException {
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
        synchronized (clientPacketIdSet) {
            do {
                id = (short) (new Random(System.currentTimeMillis()).nextInt(Short.MAX_VALUE - 1) + 1);
            } while(clientPacketIdSet.contains(id));
            variableHeader = ByteUtils.shortToByte2(id);
            clientPacketIdSet.add(id);
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
        SubackListener subackListener = new SubackListener(variableHeader, topics, callback);
        coreExecutorService.execute(subackListener);

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
                synchronized (mqttPacketQueue.suback) {
                    MqttSubackPacket subackPacket = mqttPacketQueue.suback.peek();
                    if(subackPacket != null && Arrays.equals(subackPacket.getPacketId(),id)) {
                        short shortId = ByteUtils.byte2ToShort(id);
                        clientPacketIdSet.remove(shortId);
                        byte[] returnCodeList = subackPacket.getReturnCodeList();
                        isSubacked = true;
                        if(callback != null) {
                            Iterator<MqttTopic> topicIterator = topics.iterator();
                            int pos = 0;
                            while(topicIterator.hasNext()) {
                                final int i = pos;
                                final MqttTopic topic = topicIterator.next();
                                coreExecutorService.execute(new Runnable(){
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
        synchronized (clientPacketIdSet) {
            do {
                id = (short) (new Random(System.currentTimeMillis()).nextInt(Short.MAX_VALUE - 1) + 1);
            } while(clientPacketIdSet.contains(id));
            variableHeader = ByteUtils.shortToByte2(id);
            clientPacketIdSet.add(id);
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
        coreExecutorService.execute(unsubackListener);

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
                synchronized (mqttPacketQueue.unsuback) {
                    MqttUnsubackPacket unsubackPacket = mqttPacketQueue.unsuback.peek();
                    if(unsubackPacket != null && Arrays.equals(unsubackPacket.getPacketId(),id)) {
                        short shortId = ByteUtils.byte2ToShort(id);
                        clientPacketIdSet.remove(shortId);
                        isUnsubacked = true;
                    }
                }
                if(isUnsubacked && callback != null) {
                    callback.onSuccess(topics);
                }
            }
        }
    }

    public void disconnect() throws MqttException, IOException {
        isConnected = false;
        stopService();
        byte[] packet = new byte[2];
        packet[0] = MqttPacketType.DISCONNECT.getCode();
        packet[0] <<= 4;
        synchronized (socketLock) {
            if(socket.isConnected()) {
                OutputStream os = socket.getOutputStream();
                os.write(packet);
                socket.close();
            }
        }
    }

    private void stopService() {
        callbackQueueManger.stop();
        callbackQueueManger = null;
        messageQueueManger.stop();
        messageQueueManger = null;
        receiver.stop();
        receiver = null;
        mqttPing.stop();
        mqttPing = null;
        messageHandler.stop();
        messageHandler = null;
        System.gc();
    }

    public void setCallback(MqttMessageCallback callback) {
        this.messageCallback = callback;
        //重新指定消息处理器
        if(messageHandler != null) {
            messageHandler.setCallback(callback);
        }
    }
}
