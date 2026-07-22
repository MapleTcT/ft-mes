package com.mapletct.ftmes.bpi.domain;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record DatasetRetentionArchiveView(
        UUID id,
        UUID catalogPublicationId,
        UUID materializationId,
        UUID snapshotId,
        UUID datasetId,
        String datasetCode,
        String datasetVersion,
        String tenantId,
        String plantId,
        List<String> lineIds,
        String archiverVersion,
        String archiveProfile,
        String state,
        long revision,
        String manifestChecksum,
        String sourceContentSha256,
        String sourceObjectVersionId,
        long sourceByteSize,
        long sourceRowCount,
        Map<String, Object> sourceSchema,
        String tableIdentifier,
        @JsonSerialize(using = ToStringSerializer.class) long icebergSnapshotId,
        String icebergMetadataLocation,
        int icebergSchemaId,
        int icebergPartitionSpecId,
        long catalogVerifiedRowCount,
        String catalogSemanticChecksum,
        String requestedBy,
        String requestReason,
        Instant createdAt,
        Instant startedAt,
        Instant completedAt,
        int attemptCount,
        String retentionMode,
        Instant retainUntil,
        Boolean legalHoldEnabled,
        String archiveBucket,
        String archivePrefix,
        String sourceArchiveObjectKey,
        String sourceArchiveVersionId,
        String archiveManifestObjectKey,
        String archiveManifestVersionId,
        String archiveManifestSha256,
        Integer archiveObjectCount,
        Long archiveTotalBytes,
        Long verifiedRowCount,
        String verifiedSemanticChecksum,
        Map<String, Object> archiveMetadata,
        String failureCode,
        String failureDetail) {
}
