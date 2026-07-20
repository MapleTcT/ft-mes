package com.mapletct.ftmes.bpi.infrastructure.integration;

public class Phase2IntegrationKafkaRecordRejectedException extends RuntimeException {
    public Phase2IntegrationKafkaRecordRejectedException(String message) {
        super(message);
    }

    public Phase2IntegrationKafkaRecordRejectedException(String message, Throwable cause) {
        super(message, cause);
    }
}
