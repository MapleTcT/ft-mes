package com.mapletct.ftmes.bpi.domain;

import java.time.Instant;
import java.util.UUID;

public record WmsInboundReversalTaskView(
        UUID taskId,
        UUID batchId,
        String state,
        long revision,
        long batchRevision,
        UUID originalInboundLinkId,
        UUID originalCommandEventId,
        String originalIdempotencyKey,
        String originalDocumentId,
        String requestedBy,
        Instant requestedAt,
        String requestReason,
        String requestComment,
        String decidedBy,
        Instant decidedAt,
        String decisionReason,
        String decisionComment,
        UUID reversalCommandEventId,
        String reversalIdempotencyKey,
        String reversalReceiptEventId,
        String reversalDocumentId,
        String errorCode,
        String detail,
        Instant observedAt,
        String outboxStatus,
        int deliveryAttemptCount) {
}
