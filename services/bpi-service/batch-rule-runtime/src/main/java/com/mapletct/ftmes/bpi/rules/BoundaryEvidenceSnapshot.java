package com.mapletct.ftmes.bpi.rules;

import java.time.Instant;

public record BoundaryEvidenceSnapshot(
        String signal,
        EvidenceClass classification,
        ConditionStatus status,
        String eventId,
        Instant eventTime,
        SignalQuality quality,
        String value) {
}
