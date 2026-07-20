package com.mapletct.ftmes.bpiwmsadapter;

public class MaterialWmsTransientException extends RuntimeException {

    public MaterialWmsTransientException(String message) {
        super(message);
    }

    public MaterialWmsTransientException(String message, Throwable cause) {
        super(message, cause);
    }
}
