package com.mapletct.ftmes.bpi.domain;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record DatasetMlflowRegistrationView(
        UUID id,
        UUID retentionArchiveId,
        UUID catalogPublicationId,
        UUID materializationId,
        UUID snapshotId,
        UUID datasetId,
        String datasetCode,
        String datasetVersion,
        String tenantId,
        String plantId,
        List<String> lineIds,
        String registrarVersion,
        String trackingProfile,
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
        String catalogSemanticChecksum,
        String archiveBucket,
        String sourceArchiveObjectKey,
        String sourceArchiveVersionId,
        String archiveManifestObjectKey,
        String archiveManifestVersionId,
        String archiveManifestSha256,
        String experimentName,
        String datasetName,
        String datasetDigest,
        String requestedBy,
        String requestReason,
        Instant createdAt,
        Instant startedAt,
        Instant completedAt,
        int attemptCount,
        String mlflowExperimentId,
        String mlflowRunId,
        String mlflowArtifactUri,
        String mlflowDatasetSource,
        Map<String, Object> registrationMetadata,
        String failureCode,
        String failureDetail) {
}
