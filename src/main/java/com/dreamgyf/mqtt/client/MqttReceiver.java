package com.dreamgyf.mqtt.client;

import com.dreamgyf.mqtt.MqttPacketType;
import com.dreamgyf.mqtt.message.*;
import com.dreamgyf.utils.MqttBuildUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class MqttReceiver implements Runnable {

    private Socket socket;

    private List<MqttMessage> messageList;

    public MqttReceiver(Socket socket) {
        this.socket = socket;
        messageList = new ArrayList<>();
    }

    @Override
    public void run() {
        Thread.currentThread().setName("Thread-MqttReceiver");
        while(socket != null && socket.isConnected()) {
            try {
                InputStream in = socket.getInputStream();
                byte[] temp = new byte[1];
                if(in.read(temp) != -1){
                    MqttMessage mqttMessage;
                    if((temp[0] >> 4) == MqttPacketType.CONNACK.getCode()) {
                        mqttMessage = new MqttConnackMessage();
                    }
                    else if((temp[0] >> 4) == MqttPacketType.PUBLISH.getCode()) {
                        mqttMessage = new MqttPublishMessage();
                    }
                    else if((temp[0] >> 4) == MqttPacketType.PUBACK.getCode()) {
                        mqttMessage = new MqttPubackMessage();
                    }
                    else if((temp[0] >> 4) == MqttPacketType.PUBREC.getCode()) {
                        mqttMessage = new MqttPubrecMessage();
                    }
                    else if((temp[0] >> 4) == MqttPacketType.PUBREL.getCode()) {
                        mqttMessage = new MqttPubrelMessage();
                    }
                    else if((temp[0] >> 4) == MqttPacketType.PUBCOMP.getCode()) {
                        mqttMessage = new MqttPubcompMessage();
                    }
                    else if((temp[0] >> 4) == MqttPacketType.SUBACK.getCode()) {
                        mqttMessage = new MqttSubackMessage();
                    }
                    else if((temp[0] >> 4) == MqttPacketType.UNSUBACK.getCode()) {
                        mqttMessage = new MqttUnsubackMessage();
                    }
                    else if((temp[0] >> 4) == MqttPacketType.PINGRESP.getCode()) {
                        mqttMessage = new MqttPingrespMessage();
                    }
                    else {
                        in.read();
                        continue;
                    }
                    byte[] fixedHeader = new byte[2];
                    fixedHeader[0] = temp[0];
                    in.read(temp);
                    fixedHeader[1] = temp[0];
                    int size = fixedHeader[1];
                    byte[] residue = new byte[size];
                    in.read(residue);
                    byte[] data = MqttBuildUtils.combineBytes(fixedHeader,residue);
                    mqttMessage.setMessage(data);
                    messageList.add(mqttMessage);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    protected List<MqttMessage> getMessageList() {
        return messageList;
    }
}
