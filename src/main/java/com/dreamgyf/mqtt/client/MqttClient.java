package com.dreamgyf.mqtt.client;

import com.dreamgyf.exception.MqttException;
import com.dreamgyf.mqtt.MqttPacketType;
import com.dreamgyf.mqtt.client.callback.MqttConnectCallback;
import com.dreamgyf.mqtt.message.MqttConnackMessage;
import com.dreamgyf.mqtt.message.MqttMessage;
import com.dreamgyf.utils.MqttBuildUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MqttClient {

    private ExecutorService executorService = Executors.newFixedThreadPool(10);

    private final Object lock = new Object();

    private Socket socket;

    private boolean isConnected = false;

    private String broker;

    private int port;

    private byte[] connectMessage;

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

        receiver = new MqttReceiver(socket);
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
            while (isConnected){
                synchronized (lock) {
                    List<MqttMessage> messageList = receiver.getMessageList();
                    Iterator<MqttMessage> iterator = messageList.iterator();
                    while(iterator.hasNext()){
                        MqttMessage mqttMessage = iterator.next();
                        if(mqttMessage instanceof MqttConnackMessage) {
                            if(callback != null) {
                                if(((MqttConnackMessage) mqttMessage).getReturnCode() == 0)
                                    callback.onSuccess();
                                else {
                                    isConnected = false;
                                    callback.onFailure();
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
    }

    public void publish(String topic, String message) throws IOException, MqttException {
        publish(topic, message, new MqttPublishOptions());
    }

    public void publish(String topic, String message, MqttPublishOptions options) throws IOException, MqttException {
        byte[] fixedHeader = new byte[2];
        fixedHeader[0] = MqttPacketType.PUBLISH.getCode();
        fixedHeader[0] <<= 4;
        if(options.getDUP())
            fixedHeader[0] |= 0b00001000;
        fixedHeader[0] |= options.getQoS() << 1;
        if(options.getRETAIN())
            fixedHeader[0] |= 0b00000001;
        byte[] variableHeader;
        byte[] topicByte = MqttBuildUtils.utf8EncodedStrings(topic);
        //构建报文标识符 Packet Identifier
        byte[] packetIdentifier = new byte[0];
        if(options.getQoS() != 0) {
            packetIdentifier = new byte[2];
            //...未完成
        }
        variableHeader = MqttBuildUtils.combineBytes(topicByte,packetIdentifier);
        byte[] payLoad = message.getBytes(StandardCharsets.UTF_8);
        byte[] packet = MqttBuildUtils.combineBytes(fixedHeader,variableHeader,payLoad);
        //设置报文长度
        packet[1] = (byte) (packet.length - 2);
        //发送消息
        if(!isConnected)
            throw new MqttException("You has not been connected");
        OutputStream os = socket.getOutputStream();
        os.write(packet);
    }
}
