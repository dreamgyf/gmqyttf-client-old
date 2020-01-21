package com.dreamgyf.mqtt.client;

import com.dreamgyf.mqtt.client.callback.MqttConnectCallback;
import com.dreamgyf.mqtt.message.MqttConnackMessage;
import com.dreamgyf.mqtt.message.MqttMessage;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
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

    private byte[] message;

    MqttReceiver receiver;

    protected MqttClient(String broker, int port, byte[] message) {
        this.broker = broker;
        this.port = port;
        this.message = message;
    }

    public void connect() throws IOException {
        connect(null);
    }

    public void connect(MqttConnectCallback callback) throws IOException {
        socket = new Socket(broker,port);
        OutputStream os = socket.getOutputStream();
        os.write(message);
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
            while (isConnected){
                synchronized (lock) {
                    List<MqttMessage> messageList = receiver.getMessageList();
                    Iterator<MqttMessage> iterator = messageList.iterator();
                    while(iterator.hasNext()){
                        MqttMessage mqttMessage = iterator.next();
                        if(mqttMessage instanceof MqttConnackMessage) {
                            if(((MqttConnackMessage) mqttMessage).getReturnCode() == 0)
                                callback.onSuccess();
                            else {
                                isConnected = false;
                                callback.onFailure();
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
}
