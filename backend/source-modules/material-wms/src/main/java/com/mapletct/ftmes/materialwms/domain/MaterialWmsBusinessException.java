package com.mapletct.ftmes.materialwms.domain;

public class MaterialWmsBusinessException extends RuntimeException {

    private final int code;

    public MaterialWmsBusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
