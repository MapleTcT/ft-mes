package com.mapletct.ftmes.bpi.domain;

import java.time.Instant;
import java.util.UUID;

public record ForceCloseTaskView(
        UUID taskId,
        UUID batchId,
        String state,
        long revision,
        long batchRevision,
        BatchState sourceState,
        Instant boundaryTime,
        String requestedBy,
        Instant requestedAt,
        String requestReason,
        String requestComment,
        String decidedBy,
        Instant decidedAt,
        String decisionReason,
        String decisionComment) {
}
