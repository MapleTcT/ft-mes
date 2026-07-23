package com.mapletct.ftmes.bpi.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record DatasetTrainingReadinessView(
        UUID id,
        UUID mlflowRegistrationId,
        UUID sourceSnapshotId,
        UUID datasetId,
        String datasetCode,
        String datasetVersion,
        String tenantId,
        String plantId,
        List<String> lineIds,
        String objectiveCode,
        String policyVersion,
        long assessmentSequence,
        String state,
        long revision,
        long sourceRegistrationRevision,
        String manifestChecksum,
        String datasetDigest,
        Map<String, Object> requiredThresholds,
        Map<String, Object> observedMetrics,
        List<Map<String, Object>> gateResults,
        List<String> blockerCodes,
        Map<String, Object> phaseBoundary,
        String assessmentChecksum,
        String assessedBy,
        String assessmentReason,
        Instant assessedAt) {
}
