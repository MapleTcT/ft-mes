package com.mapletct.ftmes.bpi.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record BatchCandidate(
        UUID id,
        UUID candidateKey,
        String tenantId,
        String plantId,
        String lineId,
        BoundaryType boundaryType,
        String orderId,
        UUID batchId,
        Instant boundaryTime,
        CandidateState state,
        long revision,
        BigDecimal confidence,
        String ruleVersion,
        String topologyVersion,
        List<String> missingSignals,
        List<EvidenceView> evidence,
        ReviewView review) {
}
