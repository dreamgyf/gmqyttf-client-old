package com.dreamgyf.exception;

public class MqttBuildException extends MqttException {
    
    private static final long serialVersionUID = -1778367226140060536L;

    public MqttBuildException() {
        super();
    }

    public MqttBuildException(String message) {
        super(message);
    }

    public MqttBuildException(String message, Throwable cause) {
        super(message, cause);
    }

    public MqttBuildException(Throwable cause) {
        super(cause);
    }

    protected MqttBuildException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
