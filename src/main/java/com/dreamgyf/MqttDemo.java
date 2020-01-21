package com.dreamgyf;

import com.dreamgyf.mqtt.MqttConnect;
import com.dreamgyf.mqtt.MqttConnectBuilder;
import com.dreamgyf.mqtt.MqttVersion;

public class MqttDemo {

    public static void main(String[] args) throws Exception{

        MqttConnect mqttConnect = new MqttConnectBuilder(MqttVersion.V_3_1_1).setCleanSession(true)
                .setClientId("test1").setKeepAliveTimeOut((short) 120).build("mq.tongxinmao.com",18831);
        mqttConnect.connect();
    }
}
