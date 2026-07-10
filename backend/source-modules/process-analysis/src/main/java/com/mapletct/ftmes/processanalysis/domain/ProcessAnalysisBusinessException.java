package com.mapletct.ftmes.processanalysis.domain;

public class ProcessAnalysisBusinessException extends RuntimeException {

    private final int code;

    public ProcessAnalysisBusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
