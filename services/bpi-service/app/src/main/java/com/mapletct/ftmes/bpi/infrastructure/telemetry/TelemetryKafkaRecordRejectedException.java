package com.mapletct.ftmes.bpi.infrastructure.telemetry;

public class TelemetryKafkaRecordRejectedException extends RuntimeException {
    public TelemetryKafkaRecordRejectedException(String message) {
        super(message);
    }

    public TelemetryKafkaRecordRejectedException(String message, Throwable cause) {
        super(message, cause);
    }
}
