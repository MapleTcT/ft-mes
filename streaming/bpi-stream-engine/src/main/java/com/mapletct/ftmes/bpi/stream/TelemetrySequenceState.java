package com.mapletct.ftmes.bpi.stream;

import java.util.Objects;

record TelemetrySequenceState(
        long sourceEpoch,
        long highestSequence,
        String eventId,
        String payloadSha256) {

    TelemetrySequenceState {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(payloadSha256, "payloadSha256");
        if (eventId.isBlank() || payloadSha256.isBlank()) {
            throw new IllegalArgumentException("telemetry sequence identity is required");
        }
    }
}
