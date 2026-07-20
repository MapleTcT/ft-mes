package com.mapletct.ftmes.bpi.domain;

import java.time.Instant;
import java.util.UUID;

public record WmsReconciliationTarget(
        UUID linkId,
        UUID commandEventId,
        String idempotencyKey,
        String linkStatus,
        long linkRevision,
        String outboxStatus,
        long outboxRevision,
        int reconciliationCount,
        Instant lastActivityAt) {
}
