package com.mapletct.ftmes.bpi.application.error;

public class BpiNotFoundException extends RuntimeException {
    public BpiNotFoundException(String message) {
        super(message);
    }
}
