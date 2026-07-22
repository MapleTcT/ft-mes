package com.mapletct.ftmes.bpi.infrastructure.postgres;

import java.util.Map;
import java.util.UUID;

public record DatasetCatalogPublicationSource(
        UUID materializationId,
        UUID snapshotId,
        String manifestChecksum,
        String contentSha256,
        String objectVersionId,
        long byteSize,
        long rowCount,
        Map<String, Object> schema) {
}
