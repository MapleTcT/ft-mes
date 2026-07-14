package com.mapletct.ftmes.bpi.domain;

import java.util.List;
import java.util.UUID;

public record PointCatalogPointView(
        UUID id,
        UUID snapshotId,
        String plantId,
        String lineId,
        String localityGroup,
        String productId,
        String deviceId,
        String propertyId,
        String sourcePropertyId,
        String pointName,
        String unit,
        String dataType,
        String deviceState,
        boolean registered,
        boolean propertyPresent,
        String calibrationVersion,
        String calibrationStatus,
        boolean sourceSequenceEnabled,
        boolean ready,
        List<String> readinessIssues) {
}
