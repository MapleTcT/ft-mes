package com.mapletct.ftmes.bpi.domain;

import java.util.Map;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TopologyVersionView(
        UUID id,
        String code,
        String version,
        String state,
        long revision,
        String plantId,
        String lineId,
        String checksum,
        Map<String, Object> definition,
        String validationStatus,
        List<TopologyValidationIssue> validationErrors,
        List<TopologyValidationIssue> validationWarnings,
        String validatedBy,
        Instant validatedAt,
        UUID validatedPointCatalogSnapshotId,
        String validatedPointCatalogChecksum,
        String publishedBy,
        Instant publishedAt) {
}
