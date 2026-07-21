package com.mapletct.ftmes.bpi.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record DatasetSampleSource(
        UUID reviewId,
        UUID shadowRunId,
        UUID batchId,
        String batchNo,
        String plantId,
        String lineId,
        String stageCode,
        String orderId,
        String materialCode,
        UUID ruleVersionId,
        UUID topologyVersionId,
        UUID pointCatalogSnapshotId,
        Instant automaticStartTime,
        Instant automaticEndTime,
        Instant manualStartTime,
        Instant manualEndTime,
        BigDecimal automaticQuantity,
        BigDecimal referenceQuantity,
        String quantityUnit,
        boolean startBoundaryAccepted,
        boolean endBoundaryAccepted,
        boolean quantityWithinTolerance,
        Instant reviewedAt) {
}
