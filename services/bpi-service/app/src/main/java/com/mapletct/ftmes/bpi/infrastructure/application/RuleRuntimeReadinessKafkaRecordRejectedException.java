package com.mapletct.ftmes.bpi.infrastructure.application;

public class RuleRuntimeReadinessKafkaRecordRejectedException extends RuntimeException {
    public RuleRuntimeReadinessKafkaRecordRejectedException(String message) {
        super(message);
    }

    public RuleRuntimeReadinessKafkaRecordRejectedException(String message, Throwable cause) {
        super(message, cause);
    }
}
