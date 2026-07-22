package com.mapletct.ftmes.bpi.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record DatasetSnapshotView(
        UUID id,
        UUID datasetId,
        String datasetCode,
        String datasetVersion,
        String datasetName,
        String tenantId,
        String plantId,
        long snapshotVersion,
        String state,
        long revision,
        Instant freezeAt,
        List<String> lineIds,
        String predictionTimePolicy,
        List<UUID> ruleVersionIds,
        boolean excludeLowConfidence,
        String definitionChecksum,
        String manifestSchemaVersion,
        String manifestChecksum,
        Map<String, Object> manifest,
        Integer includedCount,
        Integer excludedCount,
        Map<String, Integer> exclusionSummary,
        String materializationState,
        String artifactUri,
        String requestedBy,
        String requestReason,
        Instant createdAt,
        Instant startedAt,
        Instant completedAt,
        int attemptCount,
        String failureCode,
        String failureDetail,
        DatasetMaterializationView latestMaterialization) {
}
