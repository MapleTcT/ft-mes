package com.mapletct.ftmes.bpi.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ShadowRunView(
        UUID id,
        String runCode,
        String name,
        String tenantId,
        String plantId,
        String lineId,
        String state,
        long revision,
        UUID ruleVersionId,
        String ruleVersion,
        UUID topologyVersionId,
        String topologyVersion,
        UUID pointCatalogSnapshotId,
        String pointCatalogChecksum,
        int minimumDurationDays,
        int minimumReviewedBatches,
        int boundaryToleranceSeconds,
        BigDecimal minimumBoundaryAgreement,
        BigDecimal quantityTolerancePercent,
        String createdBy,
        Instant createdAt,
        String startedBy,
        Instant startedAt,
        String completedBy,
        Instant completedAt,
        String decidedBy,
        Instant decidedAt,
        String decisionReason,
        String cancelledBy,
        Instant cancelledAt,
        String cancellationReason,
        ShadowRunReadiness readiness,
        ShadowRunSourceCoverage sourceCoverage,
        ShadowRunMetrics metrics,
        ShadowRunTrainingDataCoverage trainingDataCoverage,
        List<String> blockers,
        boolean readyForApproval) {
}
