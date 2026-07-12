package com.mapletct.ftmes.bpi.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record TelemetryObservation(
        String eventId,
        String signal,
        BigDecimal numericValue,
        Boolean booleanValue,
        String quality,
        Instant eventTime) {
}
