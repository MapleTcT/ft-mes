package com.mapletct.ftmes.bpi.domain;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record DatasetCatalogPublicationView(
        UUID id,
        UUID materializationId,
        UUID snapshotId,
        UUID datasetId,
        String datasetCode,
        String datasetVersion,
        String tenantId,
        String plantId,
        List<String> lineIds,
        String catalogName,
        String catalogNamespace,
        String tableName,
        String tableIdentifier,
        String publisherVersion,
        String state,
        long revision,
        String manifestChecksum,
        String sourceContentSha256,
        String sourceObjectVersionId,
        long sourceByteSize,
        long sourceRowCount,
        Map<String, Object> sourceSchema,
        String requestedBy,
        String requestReason,
        Instant createdAt,
        Instant startedAt,
        Instant completedAt,
        int attemptCount,
        @JsonSerialize(using = ToStringSerializer.class) Long icebergSnapshotId,
        String icebergMetadataLocation,
        Integer icebergSchemaId,
        Integer icebergPartitionSpecId,
        Long verifiedRowCount,
        String semanticChecksum,
        Map<String, Object> catalogMetadata,
        String failureCode,
        String failureDetail) {
}
