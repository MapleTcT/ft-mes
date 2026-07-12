package com.mapletct.ftmes.bpi.application.error;

public class BpiForbiddenException extends RuntimeException {
    public BpiForbiddenException(String message) {
        super(message);
    }
}
