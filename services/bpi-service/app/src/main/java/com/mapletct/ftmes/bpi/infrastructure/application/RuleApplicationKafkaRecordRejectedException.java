package com.mapletct.ftmes.bpi.infrastructure.application;

public class RuleApplicationKafkaRecordRejectedException extends RuntimeException {
    public RuleApplicationKafkaRecordRejectedException(String message) {
        super(message);
    }

    public RuleApplicationKafkaRecordRejectedException(String message, Throwable cause) {
        super(message, cause);
    }
}
