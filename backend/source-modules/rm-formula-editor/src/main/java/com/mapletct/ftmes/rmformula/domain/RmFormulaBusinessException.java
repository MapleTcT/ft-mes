package com.mapletct.ftmes.rmformula.domain;

public class RmFormulaBusinessException extends RuntimeException {
    private final int code;

    public RmFormulaBusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
