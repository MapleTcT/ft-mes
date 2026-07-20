package com.mapletct.ftmes.bpiwmsadapter;

public class WmsCommandRejectedException extends RuntimeException {

    public WmsCommandRejectedException(String message) {
        super(message);
    }

    public WmsCommandRejectedException(String message, Throwable cause) {
        super(message, cause);
    }
}
