package com.dreamgyf;

import com.dreamgyf.exception.MqttException;
import com.dreamgyf.mqtt.MqttVersion;
import com.dreamgyf.mqtt.client.MqttClient;
import com.dreamgyf.mqtt.client.MqttTopic;
import com.dreamgyf.mqtt.client.callback.MqttConnectCallback;
import com.dreamgyf.mqtt.client.callback.MqttMessageCallback;
import com.dreamgyf.mqtt.client.callback.MqttSubscribeCallback;

import java.io.IOException;

public class MqttDemo {

    public static void main(String[] args) throws Exception {
        MqttClient mqttClient = new MqttClient.Builder().setVersion(MqttVersion.V_3_1_1).setClientId("Dimtest1").setCleanSession(false).setBroker("mq.tongxinmao.com").setPort(18831).build();
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
                    mqttClient.subscribe(new MqttTopic("/Dim/1/message/friend/3").setQoS(2),new MqttSubscribeCallback() {
                    
                        @Override
                        public void onSuccess(String topic, int returnCode) {
                            System.out.println("订阅成功");
                        }
                    
                        @Override
                        public void onFailure(String topic) {
                            System.out.println("订阅失败");
                            
                        }
                    });
                    
//                    mqttClient.publish("/test/test", "test",new MqttPublishOptions().setQoS(2));
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
