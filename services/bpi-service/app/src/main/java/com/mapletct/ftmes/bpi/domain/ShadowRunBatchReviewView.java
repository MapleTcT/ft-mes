package com.mapletct.ftmes.bpi.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ShadowRunBatchReviewView(
        UUID id,
        UUID shadowRunId,
        UUID batchId,
        String batchNo,
        long reviewSequence,
        String state,
        Instant automaticStartTime,
        Instant automaticEndTime,
        Instant manualStartTime,
        Instant manualEndTime,
        long startDeviationSeconds,
        long endDeviationSeconds,
        boolean startBoundaryAccepted,
        boolean endBoundaryAccepted,
        BigDecimal automaticQuantity,
        BigDecimal referenceQuantity,
        String quantityUnit,
        BigDecimal quantityDeviationPercent,
        boolean quantityWithinTolerance,
        String reviewedBy,
        String reviewReason,
        Instant reviewedAt,
        Instant supersededAt) {
}
