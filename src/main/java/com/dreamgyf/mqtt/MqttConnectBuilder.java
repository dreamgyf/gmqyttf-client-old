package com.dreamgyf.mqtt;

import com.dreamgyf.exception.MqttBuildException;
import com.dreamgyf.utils.ByteUtils;
import com.dreamgyf.utils.MqttBuildUtils;

import java.util.regex.Pattern;

public class MqttConnectBuilder {

    /**
     * 协议版本
     */
    private MqttVersion version;

    /**
     * 固定报头 Fixed header
     */
    private byte[] fixedHeader;

    /**
     * 可变报头 Variable header
     */
    private byte[] variableHeader;

    /**
     * 协议名 Protocol Name
     */
    private byte[] protocolName;

    /**
     * 协议级别 Protocol Level
     */
    private byte protocolLevel;

    /**
     * 连接标志 Connect Flags
     */
    private byte connectFlags;

    /**
     * 保持连接 Keep Alive
     */
    private byte[] keepAlive;

    /**
     * 有效载荷 Payload
     */
    private byte[] payLoad;

    /**
     * 客户端标识符 Client Identifier
     */
    private String clientId;

    /**
     * 遗嘱主题 Will Topic
     */
    private String willTopic;

    /**
     * 遗嘱消息 Will Message
     */
    private String willMessage;

    /**
     * 用户名 User Name
     */
    private String username;

    /**
     * 密码 Password
     */
    private String password;

    public MqttConnectBuilder(MqttVersion version) {
        this.version = version;
        //构建固定报头 Fixed header
        fixedHeader = new byte[2];
        fixedHeader[0] = 0b00010000;
        //设置协议版本
        protocolName = version.getProtocolName();
        protocolLevel = version.getProtocolLevel();
        //构建连接标志 Connect Flags
        connectFlags = 0;
        //构建保持连接 Keep Alive
        keepAlive = new byte[2];
        keepAlive[0] = 0;
        keepAlive[1] = 0;
    }

    public MqttVersion getVersion() {
        return version;
    }

    public MqttConnectBuilder setVersion(MqttVersion version) {
        protocolName = version.getProtocolName();
        protocolLevel = version.getProtocolLevel();
        return this;
    }

    public MqttConnectBuilder setConnectFlags(byte connectFlags) {
        this.connectFlags = connectFlags;
        return this;
    }

    public byte getCleanSession() {
        return (byte) (0b00000001 & (connectFlags >> 1));
    }

    public MqttConnectBuilder setCleanSession(boolean bool) {
        if(bool)
            connectFlags |= 0b00000010;
        else
            connectFlags &= ~0b00000010;
        return this;
    }

    public byte getWillFlag() {
        return (byte) (0b00000001 & (connectFlags >> 2));
    }

    public MqttConnectBuilder setWillFlag(boolean bool) {
        if(bool) {
            connectFlags |= 0b00000100;
        }
        else {
            connectFlags &= ~0b00000100;
            //将WillQoS和WillRetail同时置0
            connectFlags &= ~0b00111000;
        }
        return this;
    }

    public byte getWillQoS() {
        return (byte) (0b00000011 & (connectFlags >> 3));
    }

    public MqttConnectBuilder setWillQoS(byte value) {
        setWillFlag(true);
        connectFlags &= ~0b00011000;
        connectFlags |= (value << 3);
        return this;
    }

    public byte getWillRetain() {
        return (byte) (0b00000001 & (connectFlags >> 5));
    }

    public MqttConnectBuilder setWillRetain(boolean bool) {
        setWillFlag(true);
        if(bool) {
            connectFlags |= 0b00100000;
        }
        else {
            connectFlags &= ~0b00100000;
        }
        return this;
    }

    public byte getUsernameFlag() {
        return (byte) (0b00000001 & (connectFlags >> 7));
    }

    public MqttConnectBuilder setUsernameFlag(boolean bool) {
        if(bool) {
            connectFlags |= 0b10000000;
        }
        else {
            connectFlags &= ~0b10000000;
        }
        return this;
    }

    public byte getPasswordFlag() {
        return (byte) (0b00000001 & (connectFlags >> 6));
    }

    public MqttConnectBuilder setPasswordFlag(boolean bool) {
        if(bool) {
            connectFlags |= 0b01000000;
        }
        else {
            connectFlags &= ~0b01000000;
        }
        return this;
    }

    public short getKeepAliveTimeOut() {
        return ByteUtils.byte2ToShort(keepAlive);
    }

    public MqttConnectBuilder setKeepAliveTimeOut(short timeOut) {
        keepAlive = ByteUtils.shortToByte2(timeOut);
        return this;
    }

    public String getClientId() {
        return clientId;
    }

    public MqttConnectBuilder setClientId(String id) throws MqttBuildException {
        if(!Pattern.matches("^[a-zA-Z0-9]+$",id))
            throw new MqttBuildException("illegal character,Client ID can only contain letters and Numbers");
        this.clientId = id;
        return this;
    }

    public String getWillTopic() {
        return willTopic;
    }

    public MqttConnectBuilder setWillTopic(String willTopic) {
        this.willTopic = willTopic;
        return this;
    }

    public String getWillMessage() {
        return willMessage;
    }

    public MqttConnectBuilder setWillMessage(String willMessage) {
        this.willMessage = willMessage;
        return this;
    }

    public String getUsername() {
        return username;
    }

    public MqttConnectBuilder setUsername(String username) {
        this.username = username;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public MqttConnectBuilder setPassword(String password) {
        this.password = password;
        return this;
    }

    public MqttConnect build(String broker, int port) {
        //构建可变报头 Variable header
        variableHeader = new byte[protocolName.length + 4]; //Protocol Name + Protocol Level + Connect Flags + Keep Alive
        int pos = 0;
        while(pos < protocolName.length) {
            variableHeader[pos] = protocolName[pos];
            pos++;
        }
        variableHeader[pos++] = protocolLevel;
        variableHeader[pos++] = connectFlags;
        variableHeader[pos++] = keepAlive[0];
        variableHeader[pos] = keepAlive[1];
        //构建客户端标识符 Client Identifier
        byte[] clientIdByte = MqttBuildUtils.utf8EncodedStrings(clientId);
        //构建遗嘱主题 Will Topic 遗嘱消息 Will Message
        byte[] willTopicByte = new byte[0];
        byte[] willMessageByte = new byte[0];
        if(getWillFlag() == 1) {
            willTopicByte = MqttBuildUtils.utf8EncodedStrings(willTopic);
            willMessageByte = MqttBuildUtils.utf8EncodedStrings(willTopic);
        }
        //构建用户名 User Name
        byte[] usernameByte = new byte[0];
        if(getUsernameFlag() == 1)
            usernameByte = MqttBuildUtils.utf8EncodedStrings(username);
        //构建密码 Password
        byte[] passwordByte = new byte[0];
        if(getPasswordFlag() == 1)
            passwordByte = MqttBuildUtils.utf8EncodedStrings(password);
        //构建有效载荷 Payload
        payLoad = MqttBuildUtils.combineBytes(clientIdByte,willTopicByte,willMessageByte,usernameByte,passwordByte);
        //构建整个报文
        byte[] message = MqttBuildUtils.combineBytes(fixedHeader,variableHeader,payLoad);
        //设置报文长度
        message[1] = (byte) (message.length - 2);
        return new MqttConnect(broker,port,message);
    }
}