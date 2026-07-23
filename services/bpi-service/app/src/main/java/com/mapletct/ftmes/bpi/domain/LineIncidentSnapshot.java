package com.mapletct.ftmes.bpi.domain;

import java.time.Instant;

public record LineIncidentSnapshot(
        String issueCode,
        String severity,
        String state,
        long eventCount,
        Instant lastSeen,
        String detail) {
}
