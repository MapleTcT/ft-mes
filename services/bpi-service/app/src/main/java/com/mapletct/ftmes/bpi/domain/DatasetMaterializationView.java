package com.mapletct.ftmes.bpi.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record DatasetMaterializationView(
        UUID id,
        UUID snapshotId,
        UUID datasetId,
        String datasetCode,
        String datasetVersion,
        String tenantId,
        String plantId,
        List<String> lineIds,
        String artifactFormat,
        String artifactSchemaVersion,
        String materializerVersion,
        String state,
        long revision,
        String manifestChecksum,
        String requestedBy,
        String requestReason,
        Instant createdAt,
        Instant startedAt,
        Instant completedAt,
        int attemptCount,
        String artifactUri,
        String objectBucket,
        String objectKey,
        String contentSha256,
        Long byteSize,
        Long rowCount,
        Map<String, Object> schema,
        Map<String, Object> artifactMetadata,
        String failureCode,
        String failureDetail) {
}
