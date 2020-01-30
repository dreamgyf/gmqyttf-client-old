package com.dreamgyf.exception;

public class MqttException extends Exception {
    
    private static final long serialVersionUID = -1778367226140060536L;

    public MqttException() {
        super();
    }

    public MqttException(String message) {
        super(message);
    }

    public MqttException(String message, Throwable cause) {
        super(message, cause);
    }

    public MqttException(Throwable cause) {
        super(cause);
    }

    protected MqttException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
