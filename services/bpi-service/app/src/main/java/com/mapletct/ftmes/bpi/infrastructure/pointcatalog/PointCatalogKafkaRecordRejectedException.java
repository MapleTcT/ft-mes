package com.mapletct.ftmes.bpi.infrastructure.pointcatalog;

public class PointCatalogKafkaRecordRejectedException extends RuntimeException {
    public PointCatalogKafkaRecordRejectedException(String message) {
        super(message);
    }

    public PointCatalogKafkaRecordRejectedException(String message, Throwable cause) {
        super(message, cause);
    }
}
