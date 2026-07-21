package com.mapletct.ftmes.qcsoutbox;

public class RetryableQcsOutboxException extends RuntimeException {

    public RetryableQcsOutboxException(String message) {
        super(message);
    }

    public RetryableQcsOutboxException(String message, Throwable cause) {
        super(message, cause);
    }
}
