package com.mapletct.ftmes.bpi.domain;

import java.util.UUID;

public record WmsInboundTarget(
        UUID linkId,
        UUID commandEventId,
        String idempotencyKey,
        String status,
        long linkRevision,
        String outboxStatus) {
}
