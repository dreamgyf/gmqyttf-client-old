package com.dreamgyf.mqtt.client;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import com.dreamgyf.mqtt.MqttPacketType;
import com.dreamgyf.mqtt.client.callback.MqttConnectStateCallback;
import com.dreamgyf.mqtt.packet.MqttPacket;
import com.dreamgyf.mqtt.packet.MqttPingrespPacket;

class MqttPing implements Runnable {

    private final Socket socket;

    private final Object socketLock;

    private final LinkedBlockingQueue<MqttPingrespPacket> pingrespQueue;

    private int keepAliveTime;

    private long lastReqTime;

    private MqttConnectStateCallback callback;

    private boolean isRunning;

    private ExecutorService executorService = Executors.newFixedThreadPool(10);

    protected MqttPing(final Socket socket, final Object socketLock,
                       final LinkedBlockingQueue<MqttPingrespPacket> pingrespQueue,
                       int keepAliveTime, MqttConnectStateCallback callback) {
        this.socket = socket;
        this.socketLock = socketLock;
        this.pingrespQueue = pingrespQueue;
        this.keepAliveTime = keepAliveTime;
        lastReqTime = System.currentTimeMillis();
        this.callback = callback;
        isRunning = true;
    }

    @Override
    public void run() {
        Thread.currentThread().setName("Thread-MqttPing");
        while (isRunning) {
            if ((System.currentTimeMillis() - lastReqTime) > (long) keepAliveTime * 1000L) {
                byte[] packet = new byte[2];
                packet[0] = MqttPacketType.PINGREQ.getCode();
                packet[0] <<= 4;
                synchronized (socketLock) {
                    if (socket.isConnected()) {
                        OutputStream os;
                        try {
                            //发送心跳包
                            os = socket.getOutputStream();
                            os.write(packet);
                            updateLastReqTime();
                            //等待心跳响应
                            PingRespListener pingRespListener = new PingRespListener(System.currentTimeMillis());
                            executorService.execute(pingRespListener);
                        } catch (IOException e) {
                            e.printStackTrace();
                            isRunning = false;
                            callback.onDisconnected();
                        }
                    }
                    else {
                        isRunning = false;
                        callback.onDisconnected();
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

    private class PingRespListener implements Runnable {

        private long sendTime;

        private PingRespListener(long currentTime) {
            super();
            this.sendTime = currentTime;
        }

        @Override
        public void run() {
            Thread.currentThread().setName("Thread-PingRespListener");
            boolean isResponsed = false;
            boolean isTimeOut = false;
            while (!isResponsed && !isTimeOut){
                if((System.currentTimeMillis() - sendTime) > (long) keepAliveTime * 1000L) {
                    isTimeOut = true;
                }
                else {
                    if(pingrespQueue.poll() != null) {
                        isResponsed = true;
                    }
//                    synchronized (packetListLock) {
//                        Iterator<MqttPacket> iterator = packetList.iterator();
//                        while(iterator.hasNext()){
//                            MqttPacket mqttMessage = iterator.next();
//                            if(mqttMessage instanceof MqttPingrespPacket) {
//                                iterator.remove();
//                                isResponsed = true;
//                            }
//                        }
//                    }
                }
                if(isTimeOut) {
                    isRunning = false;
                    callback.onDisconnected();
                }

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    protected void updateLastReqTime() {
        this.lastReqTime = System.currentTimeMillis();
    }

    protected void stop() {
        isRunning = false;
    }
    

}