package com.dreamgyf;

import com.dreamgyf.mqtt.client.MqttClient;
import com.dreamgyf.mqtt.client.MqttClientBuilder;

import java.io.IOException;

import com.dreamgyf.exception.MqttException;
import com.dreamgyf.mqtt.MqttVersion;
import com.dreamgyf.mqtt.client.callback.MqttConnectCallback;

public class MqttDemo {

    public static void main(String[] args) throws Exception {

        MqttClient mqttClient = new MqttClientBuilder(MqttVersion.V_3_1_1).setCleanSession(true).setClientId("dream")
                .setKeepAliveTime((short) 120).build("mq.tongxinmao.com", 18831);
        mqttClient.connect(new MqttConnectCallback() {
            @Override
            public void onSuccess() {
                System.out.println("连接成功");
                try {
                    mqttClient.publish("/public/1", "test");
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (MqttException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure() {
                System.out.println("连接失败");
            }
        });
    }
}
