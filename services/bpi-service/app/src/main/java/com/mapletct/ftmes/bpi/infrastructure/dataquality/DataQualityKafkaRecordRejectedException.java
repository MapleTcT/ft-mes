package com.mapletct.ftmes.bpi.infrastructure.dataquality;

public class DataQualityKafkaRecordRejectedException extends RuntimeException {
    public DataQualityKafkaRecordRejectedException(String message) {
        super(message);
    }

    public DataQualityKafkaRecordRejectedException(String message, Throwable cause) {
        super(message, cause);
    }
}
