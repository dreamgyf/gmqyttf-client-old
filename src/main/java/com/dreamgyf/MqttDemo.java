package com.dreamgyf;

import com.dreamgyf.mqtt.client.MqttClient;
import com.dreamgyf.mqtt.client.MqttClientBuilder;
import com.dreamgyf.mqtt.client.MqttPublishOptions;

import java.io.IOException;

import com.dreamgyf.exception.MqttException;
import com.dreamgyf.mqtt.MqttVersion;
import com.dreamgyf.mqtt.client.callback.MqttConnectCallback;
import com.dreamgyf.mqtt.client.callback.MqttPublishCallback;

public class MqttDemo {

    public static void main(String[] args) throws Exception {

        MqttClient mqttClient = new MqttClientBuilder(MqttVersion.V_3_1_1).setCleanSession(true).setClientId("dream")
                .setKeepAliveTime((short) 120).build("mq.tongxinmao.com", 18831);
        mqttClient.connect(new MqttConnectCallback() {
            @Override
            public void onSuccess() {
                System.out.println("连接成功");
                try {
                    mqttClient.publish("/public/1", "测试publish",new MqttPublishOptions().setQoS(2),new MqttPublishCallback(){
                    
                        @Override
                        public void messageArrived(String topic, String message) {
                            System.out.println(topic + ":" + message + " 发送成功");
                        }
                    });
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
