package com.mapletct.ftmes.bpi.domain;

import java.time.Instant;
import java.util.UUID;

public record WmsInboundView(
        UUID id,
        UUID commandEventId,
        String idempotencyKey,
        String status,
        String receiptEventId,
        String documentId,
        String errorCode,
        String detail,
        Instant observedAt,
        long revision,
        String outboxStatus,
        int deliveryAttemptCount,
        int reconciliationCount,
        Instant commandPublishedAt,
        Instant lastReconciledAt,
        String lastReconciledBy,
        Instant reconcileAfter,
        boolean reconciliationAllowed,
        String reconciliationBlockedReason) {
}
