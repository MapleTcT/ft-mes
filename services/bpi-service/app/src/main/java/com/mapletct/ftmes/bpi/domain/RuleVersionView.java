package com.mapletct.ftmes.bpi.domain;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record RuleVersionView(
        UUID id,
        String code,
        String version,
        String state,
        long revision,
        String plantId,
        String lineId,
        String topologyVersion,
        String checksum,
        Map<String, Object> ast,
        UUID latestSimulationId,
        String publicationStatus,
        long publicationRevision,
        int publicationAttemptCount,
        int publicationTotalAttemptCount,
        int publicationManualRetryCount,
        Instant publicationPublishedAt,
        Instant publicationLastRequeuedAt,
        String publicationLastError,
        String applicationStatus,
        String applicationDeploymentId,
        Instant applicationObservedAt,
        Instant applicationReceivedAt,
        String applicationErrorCode,
        String applicationErrorDetail,
        String runtimeReadinessStatus,
        String runtimeReadinessDeploymentId,
        Instant runtimeReadinessObservedAt,
        Instant runtimeReadinessReceivedAt,
        String runtimeReadinessReasonCode,
        String runtimeReadinessDetail,
        String runtimePointCatalogEventId,
        String runtimePointCatalogSourceRevision) {
}
