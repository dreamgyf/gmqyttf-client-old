package com.dreamgyf.mqtt.client;

import com.dreamgyf.mqtt.MqttPacketType;
import com.dreamgyf.mqtt.message.*;
import com.dreamgyf.utils.MqttBuildUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.List;

public class MqttReceiver implements Runnable {

    private Socket socket;

    private List<MqttPacket> packetList;

    private final Object packetListLock;

    private boolean isRunning = true;

    public MqttReceiver(Socket socket, List<MqttPacket> packetList, final Object packetListLock) {
        this.socket = socket;
        this.packetList = packetList;
        this.packetListLock = packetListLock;
    }

    @Override
    public void run() {
        Thread.currentThread().setName("Thread-MqttReceiver");
        while(isRunning) {
            try {
                InputStream in = socket.getInputStream();
                byte[] temp = new byte[1];
                if(in.read(temp) != -1){
                    MqttPacket mqttMessage;
                    if(((temp[0] & 0xff) >> 4) == MqttPacketType.CONNACK.getCode()) {
                        mqttMessage = new MqttConnackPacket();
                    }
                    else if(((temp[0] & 0xff) >> 4) == MqttPacketType.PUBLISH.getCode()) {
                        mqttMessage = new MqttPublishPacket();
                    }
                    else if(((temp[0] & 0xff) >> 4) == MqttPacketType.PUBACK.getCode()) {
                        mqttMessage = new MqttPubackPacket();
                    }
                    else if(((temp[0] & 0xff) >> 4) == MqttPacketType.PUBREC.getCode()) {
                        mqttMessage = new MqttPubrecPacket();
                    }
                    else if(((temp[0] & 0xff) >> 4) == MqttPacketType.PUBREL.getCode()) {
                        mqttMessage = new MqttPubrelPacket();
                    }
                    else if(((temp[0] & 0xff) >> 4) == MqttPacketType.PUBCOMP.getCode()) {
                        mqttMessage = new MqttPubcompPacket();
                    }
                    else if(((temp[0] & 0xff) >> 4) == MqttPacketType.SUBACK.getCode()) {
                        mqttMessage = new MqttSubackPacket();
                    }
                    else if(((temp[0] & 0xff) >> 4) == MqttPacketType.UNSUBACK.getCode()) {
                        mqttMessage = new MqttUnsubackPacket();
                    }
                    else if(((temp[0] & 0xff) >> 4) == MqttPacketType.PINGRESP.getCode()) {
                        mqttMessage = new MqttPingrespPacket();
                    }
                    else {
                        in.read();
                        continue;
                    }
                    byte[] fixedHeader = new byte[2];
                    fixedHeader[0] = temp[0];
                    in.read(temp);
                    fixedHeader[1] = temp[0];
                    int size = fixedHeader[1] & 0xff;
                    byte[] residue = new byte[size];
                    in.read(residue);
                    byte[] data = MqttBuildUtils.combineBytes(fixedHeader,residue);
                    mqttMessage.setPacket(data);
                    synchronized (packetListLock) {
                        packetList.add(mqttMessage);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    protected void stop() {
        isRunning = false;
    }
}
