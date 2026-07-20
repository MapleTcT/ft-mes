package com.mapletct.ftmes.bpiwmsadapter;

public class MaterialWmsBusinessException extends RuntimeException {

    private final String code;

    public MaterialWmsBusinessException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
