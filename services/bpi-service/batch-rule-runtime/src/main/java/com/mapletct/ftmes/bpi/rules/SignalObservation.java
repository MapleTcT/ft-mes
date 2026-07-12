package com.mapletct.ftmes.bpi.rules;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public record SignalObservation(
        String eventId,
        String signal,
        BigDecimal numericValue,
        Boolean booleanValue,
        SignalQuality quality,
        Instant eventTime) {

    public SignalObservation {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(signal, "signal");
        Objects.requireNonNull(quality, "quality");
        Objects.requireNonNull(eventTime, "eventTime");
        if (eventId.isBlank() || signal.isBlank()) {
            throw new IllegalArgumentException("eventId and signal are required");
        }
        if ((numericValue == null) == (booleanValue == null)) {
            throw new IllegalArgumentException("exactly one signal value is required");
        }
    }

    public static SignalObservation numeric(
            String eventId, String signal, BigDecimal value, SignalQuality quality, Instant eventTime) {
        return new SignalObservation(eventId, signal, value, null, quality, eventTime);
    }

    public static SignalObservation bool(
            String eventId, String signal, boolean value, SignalQuality quality, Instant eventTime) {
        return new SignalObservation(eventId, signal, null, value, quality, eventTime);
    }
}
