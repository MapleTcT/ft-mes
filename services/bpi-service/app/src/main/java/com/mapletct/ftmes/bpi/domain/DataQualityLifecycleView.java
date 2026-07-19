package com.mapletct.ftmes.bpi.domain;

import java.time.Instant;

public record DataQualityLifecycleView(
        long revision,
        String action,
        String fromState,
        String toState,
        String actorId,
        String assignee,
        String reason,
        Instant at) {
}
