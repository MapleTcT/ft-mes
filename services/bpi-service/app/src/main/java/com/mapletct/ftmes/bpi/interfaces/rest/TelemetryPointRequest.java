package com.mapletct.ftmes.bpi.interfaces.rest;

public record TelemetryPointRequest(
        String propertyId,
        Double doubleValue,
        Long longValue,
        String stringValue,
        Boolean boolValue,
        String unit,
        String qualityCode,
        long sampleTimeMs,
        String calibrationVersion) {
}
