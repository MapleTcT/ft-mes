package com.mapletct.ftmes.bpi.domain;

import java.time.Instant;
import java.util.UUID;

public record FeatureFlagRecord(
        UUID id,
        String tenantId,
        String scopeType,
        String scopeKey,
        String flagKey,
        boolean enabled,
        boolean active,
        long revision,
        String updatedBy,
        Instant updatedAt,
        String lastReason) {
}
