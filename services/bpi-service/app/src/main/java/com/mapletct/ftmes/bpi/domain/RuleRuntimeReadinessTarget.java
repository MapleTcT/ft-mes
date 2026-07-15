package com.mapletct.ftmes.bpi.domain;

import java.time.Instant;
import java.util.UUID;

public record RuleRuntimeReadinessTarget(
        UUID publicationId,
        UUID ruleId,
        String tenantId,
        String plantId,
        String lineId,
        String ruleCode,
        String ruleVersion,
        String ruleChecksum,
        String publicationStatus,
        long publicationRevision,
        String runtimeReadinessStatus,
        String runtimeReadinessEventId,
        Instant runtimeReadinessObservedAt,
        String runtimeReadinessReasonCode) {
}
