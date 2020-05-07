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

    private volatile MqttPacketQueue mqttPacketQueue;

    private boolean isRunning = true;

    public MqttReceiver(Socket socket, MqttPacketQueue mqttPacketQueue) {
        this.socket = socket;
        this.mqttPacketQueue = mqttPacketQueue;
    }

    @Override
    public void run() {
        Thread.currentThread().setName("Thread-MqttReceiver");
        while(isRunning) {
            try {
                InputStream in = socket.getInputStream();
                byte[] temp = new byte[1];
                if(in.read(temp) != -1){
                    MqttPacket mqttPacket;
                    if(((temp[0] & 0xff) >>> 4) == MqttPacketType.CONNACK.getCode()) {
                        mqttPacket = new MqttConnackPacket();
                    }
                    else if(((temp[0] & 0xff) >>> 4) == MqttPacketType.PUBLISH.getCode()) {
                        mqttPacket = new MqttPublishPacket();
                    }
                    else if(((temp[0] & 0xff) >>> 4) == MqttPacketType.PUBACK.getCode()) {
                        mqttPacket = new MqttPubackPacket();
                    }
                    else if(((temp[0] & 0xff) >>> 4) == MqttPacketType.PUBREC.getCode()) {
                        mqttPacket = new MqttPubrecPacket();
                    }
                    else if(((temp[0] & 0xff) >>> 4) == MqttPacketType.PUBREL.getCode()) {
                        mqttPacket = new MqttPubrelPacket();
                    }
                    else if(((temp[0] & 0xff) >>> 4) == MqttPacketType.PUBCOMP.getCode()) {
                        mqttPacket = new MqttPubcompPacket();
                    }
                    else if(((temp[0] & 0xff) >>> 4) == MqttPacketType.SUBACK.getCode()) {
                        mqttPacket = new MqttSubackPacket();
                    }
                    else if(((temp[0] & 0xff) >>> 4) == MqttPacketType.UNSUBACK.getCode()) {
                        mqttPacket = new MqttUnsubackPacket();
                    }
                    else if(((temp[0] & 0xff) >>> 4) == MqttPacketType.PINGRESP.getCode()) {
                        mqttPacket = new MqttPingrespPacket();
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
                    mqttPacket.setPacket(data);
                    try {
                        if(mqttPacket instanceof MqttConnackPacket) {
                            mqttPacketQueue.connack.put((MqttConnackPacket) mqttPacket);
                        }
                        else if(mqttPacket instanceof MqttPublishPacket) {
                            mqttPacketQueue.publish.put((MqttPublishPacket) mqttPacket);
                        }
                        else if(mqttPacket instanceof MqttPubackPacket) {
                            mqttPacketQueue.puback.put((MqttPubackPacket) mqttPacket);
                        }
                        else if(mqttPacket instanceof MqttPubrecPacket) {
                            mqttPacketQueue.pubrec.put((MqttPubrecPacket) mqttPacket);
                        }
                        else if(mqttPacket instanceof MqttPubrelPacket) {
                            mqttPacketQueue.pubrel.put((MqttPubrelPacket) mqttPacket);
                        }
                        else if(mqttPacket instanceof MqttPubcompPacket) {
                            mqttPacketQueue.pubcomp.put((MqttPubcompPacket) mqttPacket);
                        }
                        else if(mqttPacket instanceof MqttSubackPacket) {
                            mqttPacketQueue.suback.put((MqttSubackPacket) mqttPacket);
                        }
                        else if(mqttPacket instanceof MqttUnsubackPacket) {
                            mqttPacketQueue.unsuback.put((MqttUnsubackPacket) mqttPacket);
                        }
                        else if(mqttPacket instanceof MqttPingrespPacket) {
                            mqttPacketQueue.pingresp.put((MqttPingrespPacket) mqttPacket);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
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
