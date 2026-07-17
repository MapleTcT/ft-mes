package com.mapletct.ftmes.womprint.domain;

public class WomPrintBusinessException extends RuntimeException {

    private final int code;

    public WomPrintBusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
