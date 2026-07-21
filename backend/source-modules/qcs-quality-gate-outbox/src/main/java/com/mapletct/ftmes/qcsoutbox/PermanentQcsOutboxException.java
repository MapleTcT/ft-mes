package com.mapletct.ftmes.qcsoutbox;

public class PermanentQcsOutboxException extends RuntimeException {

    public PermanentQcsOutboxException(String message) {
        super(message);
    }

    public PermanentQcsOutboxException(String message, Throwable cause) {
        super(message, cause);
    }
}
