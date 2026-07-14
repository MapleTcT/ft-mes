package com.mapletct.ftmes.bpi.domain;

import java.time.Instant;
import java.util.UUID;

public record PointCatalogSnapshotView(
        UUID id,
        String source,
        String sourceInstance,
        String sourceRevision,
        String plantId,
        String lineId,
        String checksum,
        Instant observedAt,
        int pointCount,
        int readyPointCount,
        String importedBy,
        Instant importedAt) {
}
