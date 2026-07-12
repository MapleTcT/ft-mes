package com.mapletct.ftmes.bpi.application.error;

public class BpiConflictException extends RuntimeException {
    private final Long currentRevision;

    public BpiConflictException(String message, Long currentRevision) {
        super(message);
        this.currentRevision = currentRevision;
    }

    public Long getCurrentRevision() {
        return currentRevision;
    }
}
