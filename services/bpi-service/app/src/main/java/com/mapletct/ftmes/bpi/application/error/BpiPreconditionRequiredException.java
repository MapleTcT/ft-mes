package com.mapletct.ftmes.bpi.application.error;

public class BpiPreconditionRequiredException extends RuntimeException {
    public BpiPreconditionRequiredException(String message) {
        super(message);
    }
}
