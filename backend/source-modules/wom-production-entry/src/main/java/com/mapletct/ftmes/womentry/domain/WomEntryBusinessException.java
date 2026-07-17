package com.mapletct.ftmes.womentry.domain;

public class WomEntryBusinessException extends RuntimeException {

    private final int code;

    public WomEntryBusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public WomEntryBusinessException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
