package com.mapletct.ftmes.bpi.application.error;

public class BpiValidationException extends RuntimeException {
    public BpiValidationException(String message) {
        super(message);
    }
}
