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
                    
                    mqttClient.publish("/public/1", "世界历史的发展就如同一条长河的流淌，源远流长。有水就有岸，世界的长河要流淌，便需要水与岸的共同合作。这便是人类发展的“河流法则”。如果我们把“水”比作“自由”，把“岸”比作“约束”的话，那么“河流法则”边演化成为了一个永恒的议题：自由与约束。无庸置疑，我们需要自由与约束的统一，我们需要遵循“河流法则”。原因是显而易见的。用哲学里的话说，自由与约束作为一对矛盾，是对立统一的，有自由就有约束，二者相互联系。约束离不开自由，自由离开约束也就不称其为自由。我们要长久地发展下去，必然需要自由月约束的统一。将其形象化到生活中，自由就好比人的力量，人为了社会的发展创造了先进的科技，使用着大自然赐予的资源。然而，这种力量不是无止境的，它永远都有一种约束并疏导它的力量，这便是规律。人可以自由运用自身的力量的这种能力正是建立在尊重规律的基础上的。没有这个基础，不受约束，人们的自由就没有发挥的余地，人们也许只能停留在刀耕火种的原始中无限制地探索下去，直到寻找到这一约束。时至今日，我们依然要遵循“河流法则”，自觉地遵循约束我们的规律。于是我们制订法律法规，提出“可持续发展”。“河流法则”的威力可见一斑。再细化到现实生活中，人们时时处处无不在遵循着“河流法则”。这让我想起了古代的那些暴君，当他们以为自由是无限度的时候，其实他们早已经受到民心的约束。现在想来，“和谐社会”似乎也是在顺应“河流法则”了吧。孩子要受父母约束，权利要受法律约束，君主要受人民约束。“河流法则”无处不在。有很多人或者当我们年纪还小的时候都认为自由与约束是相背离的，如果有约束就无法自由。现在想来，其实不然。什么事情都是有对照才显现出其特点，自由与约束实际上是相对应而存在的，正如前面所讲，二者密不可分。约束即是为了保证更好的自由，而正因为有自由才需要约束的存在。当孩子想要做自己认为对的事情时，需要父母的善意引导才会有利于孩子们的成长；当有人要利用自身的权力做扰乱正常社会秩序的事情时，更需要法律的严厉约束才会保证广大社会群体的利益。因此，强调自由也不能忽视约束。然而，遵循“河流法则”首先也最重要的是在于一个“度”的问题。约束算是规律，而自由就是人的能动性。所以在这一法则的履行中，我们的行为不能是盲目的，即不能偏向一方。我们应该筑一道岸形成一种约束来避免人的过分自由，但这个岸也不能过宽过高，否则成为“地上河”，就会有决堤的危险。如果这道岸不够高，使水恣意流淌，同样起不到约束和疏导的效果。因此这个“度”应寻求二者的平衡。其次，遵循这一法则，应是社会和个人共同的责任，也就是说，全社会应该遵循，更重要的是社会中的各个个体应自觉遵守，而不是强迫的，这样也就更有利于把握这个“度”，更好地促进社会发展。有水就有岸，水有水的自由灵动，岸有岸的宽广安全。人在水里感到自在，可是会碰到风浪；人在岸上感到约束，但也会觉得安全。水与岸，一个都不能少，共同构筑起人类的“河流法则”。",new MqttPublishOptions().setQoS(0));
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
