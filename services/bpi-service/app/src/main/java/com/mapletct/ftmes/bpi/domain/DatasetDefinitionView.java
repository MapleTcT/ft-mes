package com.mapletct.ftmes.bpi.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DatasetDefinitionView(
        UUID id,
        String datasetCode,
        String version,
        String name,
        String tenantId,
        String plantId,
        List<String> lineIds,
        String state,
        long revision,
        String predictionTimePolicy,
        String featureCutoffPolicy,
        List<String> featureRefs,
        List<ProcessSignalWindowDefinition> processSignalWindows,
        List<String> labelRefs,
        int maxLabelDelayHours,
        BigDecimal minimumConfidence,
        String splitPolicy,
        String checksum,
        String createdBy,
        String createReason,
        Instant createdAt,
        DatasetSnapshotSummary latestSnapshot) {
}
