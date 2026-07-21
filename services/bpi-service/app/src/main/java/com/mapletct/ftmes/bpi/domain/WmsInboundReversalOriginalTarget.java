package com.mapletct.ftmes.bpi.domain;

import java.util.UUID;

public record WmsInboundReversalOriginalTarget(
        UUID inboundLinkId,
        UUID originalCommandEventId,
        String originalIdempotencyKey,
        String originalDocumentId,
        String inboundStatus,
        long inboundRevision,
        String outboxStatus,
        byte[] originalCommandPayload) {
}
