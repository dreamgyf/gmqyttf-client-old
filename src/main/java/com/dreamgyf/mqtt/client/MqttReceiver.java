package com.dreamgyf.mqtt.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.List;

import com.dreamgyf.mqtt.MqttPacketType;
import com.dreamgyf.mqtt.packet.MqttConnackPacket;
import com.dreamgyf.mqtt.packet.MqttPacket;
import com.dreamgyf.mqtt.packet.MqttPingrespPacket;
import com.dreamgyf.mqtt.packet.MqttPubackPacket;
import com.dreamgyf.mqtt.packet.MqttPubcompPacket;
import com.dreamgyf.mqtt.packet.MqttPublishPacket;
import com.dreamgyf.mqtt.packet.MqttPubrecPacket;
import com.dreamgyf.mqtt.packet.MqttPubrelPacket;
import com.dreamgyf.mqtt.packet.MqttSubackPacket;
import com.dreamgyf.mqtt.packet.MqttUnsubackPacket;
import com.dreamgyf.utils.MqttBuildUtils;

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
                        continue;
                    }
                    byte header = temp[0];
                    int remainingLengthPos = 0;
                    byte[] tempRemainingLength = new byte[4];
                    do {
                        in.read(temp);
                        tempRemainingLength[remainingLengthPos++] = temp[0];
                    } while((temp[0] & 0x80) != 0);
                    byte[] fixedHeader = new byte[1 + remainingLengthPos];
                    fixedHeader[0] = header;
                    for(int i = 1;i < fixedHeader.length;i++) {
                        fixedHeader[i] = tempRemainingLength[i - 1];
                    }
                    int size = MqttBuildUtils.getRemainingLength(fixedHeader);
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
