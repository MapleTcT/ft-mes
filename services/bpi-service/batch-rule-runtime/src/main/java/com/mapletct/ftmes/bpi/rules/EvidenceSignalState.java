package com.mapletct.ftmes.bpi.rules;

import java.math.BigDecimal;
import java.time.Instant;

public record EvidenceSignalState(
        String signal,
        ConditionStatus status,
        Instant trueSince,
        String firstTrueEventId,
        String lastEventId,
        Instant lastEventTime,
        BigDecimal previousNumericValue,
        BigDecimal currentNumericValue,
        Boolean currentBooleanValue,
        SignalQuality quality) {
}
