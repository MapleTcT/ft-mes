package com.mapletct.ftmes.bpi.infrastructure.postgres;

import java.util.Map;
import java.util.UUID;

public record DatasetMlflowRegistrationSource(
        UUID archiveId,
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
        String catalogSemanticChecksum,
        String archiveBucket,
        String sourceArchiveObjectKey,
        String sourceArchiveVersionId,
        String archiveManifestObjectKey,
        String archiveManifestVersionId,
        String archiveManifestSha256,
        String datasetCode) {
}
