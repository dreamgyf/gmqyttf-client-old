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
        while(socket != null && socket.isConnected()) {
            try {
                InputStream in = socket.getInputStream();
                byte[] temp = new byte[1];
                if(in.read(temp) != -1){
                    MqttMessage mqttMessage;
                    if((temp[0] >> 4) == MqttPacketType.CONNACK.getType()) {
                        mqttMessage = new MqttConnackMessage();
                    }
                    else if((temp[0] >> 4) == MqttPacketType.PUBLISH.getType()) {
                        mqttMessage = new MqttPublishMessage();
                    }
                    else if((temp[0] >> 4) == MqttPacketType.PUBACK.getType()) {
                        mqttMessage = new MqttPubackMessage();
                    }
                    else if((temp[0] >> 4) == MqttPacketType.PUBREC.getType()) {
                        mqttMessage = new MqttPubrecMessage();
                    }
                    else if((temp[0] >> 4) == MqttPacketType.PUBREL.getType()) {
                        mqttMessage = new MqttPubrelMessage();
                    }
                    else if((temp[0] >> 4) == MqttPacketType.PUBCOMP.getType()) {
                        mqttMessage = new MqttPubcompMessage();
                    }
                    else if((temp[0] >> 4) == MqttPacketType.SUBACK.getType()) {
                        mqttMessage = new MqttSubackMessage();
                    }
                    else if((temp[0] >> 4) == MqttPacketType.UNSUBACK.getType()) {
                        mqttMessage = new MqttUnsubackMessage();
                    }
                    else if((temp[0] >> 4) == MqttPacketType.PINGRESP.getType()) {
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
