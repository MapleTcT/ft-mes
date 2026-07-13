package com.mapletct.ftmes.bpi.domain;

import java.time.Instant;
import java.util.UUID;

public record RulePublicationView(
        UUID id,
        String status,
        long revision,
        int attemptCount,
        int totalAttemptCount,
        int manualRetryCount,
        Instant publishedAt,
        Instant lastRequeuedAt,
        String lastError) {
}
