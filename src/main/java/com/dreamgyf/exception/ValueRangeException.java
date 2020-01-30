package com.dreamgyf.exception;

public class ValueRangeException extends MqttException {
    
    private static final long serialVersionUID = -1778367226140060536L;

    public ValueRangeException() {
        super();
    }

    public ValueRangeException(String message) {
        super(message);
    }

    public ValueRangeException(String message, Throwable cause) {
        super(message, cause);
    }

    public ValueRangeException(Throwable cause) {
        super(cause);
    }

    protected ValueRangeException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
