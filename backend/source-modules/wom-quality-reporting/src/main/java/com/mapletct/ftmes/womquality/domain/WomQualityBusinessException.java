package com.mapletct.ftmes.womquality.domain;

public final class WomQualityBusinessException extends RuntimeException {

    private final int code;

    public WomQualityBusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
