package com.dreamgyf.mqtt.client;

public class MqttPublishOptions {

    /**
     * 重发标志 DUP
     */
    private boolean DUP = false;

    /**
     * 服务质量等级 QoS
     */
    private byte QoS = 0;

    /**
     * 保留标志 RETAIN
     */
    private boolean RETAIN = false;


    protected boolean getDUP() {
        return this.DUP;
    }

    protected MqttPublishOptions setDUP(boolean DUP) {
        this.DUP = DUP;
        return this;
    }

    public byte getQoS() {
        return this.QoS;
    }

    public MqttPublishOptions setQoS(int QoS) {
        if(QoS < 0 || QoS > 2)
            throw new IllegalArgumentException("The value of QoS must be between 0 and 2.");
        this.QoS = (byte) QoS;
        return this;
    }

    public boolean getRETAIN() {
        return this.RETAIN;
    }

    public MqttPublishOptions setRETAIN(boolean RETAIN) {
        this.RETAIN = RETAIN;
        return this;
    }


}