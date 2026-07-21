package com.mapletct.ftmes.bpi.domain;

import java.time.Instant;
import java.util.UUID;

public record DatasetSnapshotSummary(
        UUID id,
        long snapshotVersion,
        String state,
        long revision,
        Instant freezeAt,
        String manifestChecksum,
        Integer includedCount,
        Integer excludedCount,
        String materializationState,
        Instant createdAt,
        Instant completedAt,
        String failureCode,
        String failureDetail) {
}
