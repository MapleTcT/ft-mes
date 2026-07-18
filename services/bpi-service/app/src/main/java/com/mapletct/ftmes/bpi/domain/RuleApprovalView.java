package com.mapletct.ftmes.bpi.domain;

import java.time.Instant;
import java.util.UUID;

public record RuleApprovalView(
        UUID id,
        UUID ruleId,
        UUID simulationId,
        String simulationChecksum,
        String state,
        long revision,
        String submittedBy,
        Instant submittedAt,
        String submitReason,
        String decidedBy,
        Instant decidedAt,
        String decisionReason) {
}
