package com.mapletct.ftmes.bpi.infrastructure.postgres;

import java.util.Map;
import java.util.UUID;

public record DatasetRetentionArchiveSource(
        UUID publicationId,
        UUID materializationId,
        UUID snapshotId,
        String manifestChecksum,
        String sourceContentSha256,
        String sourceObjectVersionId,
        long sourceByteSize,
        long sourceRowCount,
        Map<String, Object> sourceSchema,
        String tableIdentifier,
        long icebergSnapshotId,
        String icebergMetadataLocation,
        int icebergSchemaId,
        int icebergPartitionSpecId,
        long catalogVerifiedRowCount,
        String catalogSemanticChecksum) {
}
