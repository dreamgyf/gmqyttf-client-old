package com.dreamgyf.mqtt.client;

import com.dreamgyf.exception.ValueRangeException;

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

    protected byte getQoS() {
        return this.QoS;
    }

    protected MqttPublishOptions setQoS(int QoS) throws ValueRangeException {
        if(QoS < 0 || QoS > 2)
            throw new ValueRangeException("The value of QoS must be between 0 and 3.");
        this.QoS = (byte) QoS;
        return this;
    }

    protected boolean getRETAIN() {
        return this.RETAIN;
    }

    protected MqttPublishOptions setRETAIN(boolean RETAIN) {
        this.RETAIN = RETAIN;
        return this;
    }


}