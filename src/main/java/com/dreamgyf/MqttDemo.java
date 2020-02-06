package com.dreamgyf;

import java.io.IOException;

import com.dreamgyf.exception.MqttException;
import com.dreamgyf.mqtt.MqttVersion;
import com.dreamgyf.mqtt.client.MqttClient;
import com.dreamgyf.mqtt.client.MqttPublishOptions;
import com.dreamgyf.mqtt.client.MqttTopic;
import com.dreamgyf.mqtt.client.callback.MqttConnectCallback;
import com.dreamgyf.mqtt.client.callback.MqttMessageCallback;
import com.dreamgyf.mqtt.client.callback.MqttSubscribeCallback;

public class MqttDemo {

    public static void main(String[] args) throws Exception {
        MqttClient mqttClient = new MqttClient.Builder().setVersion(MqttVersion.V_3_1_1).setBroker("mq.tongxinmao.com").setPort(18831).setCleanSession(true).setClientId("dream").setKeepAliveTime(10).build();
        mqttClient.setCallback(new MqttMessageCallback(){
            @Override
            public void messageArrived(String topic, String message) {
                System.out.println(topic + " : " + message);
            }
        });
        mqttClient.connect(new MqttConnectCallback() {
            @Override
            public void onSuccess() {
                System.out.println("连接成功");
                try {
                    mqttClient.subscribe(new MqttTopic("/public/1"),new MqttSubscribeCallback() {
                    
                        @Override
                        public void onSuccess(String topic, int returnCode) {
                            System.out.println("订阅成功");
                        }
                    
                        @Override
                        public void onFailure(String topic) {
                            System.out.println("订阅失败");
                            
                        }
                    });
                    
                    mqttClient.publish("/public/1", "你好",new MqttPublishOptions().setQoS(0));
                } catch (MqttException | IOException e) {
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
