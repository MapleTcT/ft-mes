package com.mapletct.ftmes.bpi.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DatasetManifestClaim(
        UUID snapshotId,
        UUID claimToken,
        int attemptCount,
        String tenantId,
        UUID datasetId,
        String datasetCode,
        String datasetVersion,
        String datasetName,
        String plantId,
        Instant freezeAt,
        List<String> lineIds,
        List<UUID> ruleVersionIds,
        boolean excludeLowConfidence,
        String definitionChecksum,
        String predictionTimePolicy,
        String featureCutoffPolicy,
        List<String> featureRefs,
        List<ProcessSignalWindowDefinition> processSignalWindows,
        List<String> labelRefs,
        int maxLabelDelayHours,
        BigDecimal minimumConfidence,
        String splitPolicy) {
}
