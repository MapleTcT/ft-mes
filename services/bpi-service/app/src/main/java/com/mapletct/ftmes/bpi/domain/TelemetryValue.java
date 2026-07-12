package com.mapletct.ftmes.bpi.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record TelemetryValue(
        String propertyId,
        String valueType,
        BigDecimal numericValue,
        String stringValue,
        Boolean booleanValue,
        String unit,
        String qualityCode,
        Instant sampleTime,
        String calibrationVersion) {
}
