package com.mapletct.ftmes.bpi.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record LineTelemetrySample(
        String eventId,
        String signal,
        String value,
        BigDecimal numericValue,
        String unit,
        String qualityCode,
        String sequenceDisposition,
        Instant sampleTime,
        String calibrationVersion) {
}
