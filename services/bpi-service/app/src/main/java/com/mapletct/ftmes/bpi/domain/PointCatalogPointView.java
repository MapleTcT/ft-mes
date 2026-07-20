package com.mapletct.ftmes.bpi.domain;

import java.util.List;
import java.time.Instant;
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
        String sourceCalibrationStatus,
        String calibrationStatus,
        UUID calibrationEvidenceId,
        Instant calibrationValidUntil,
        boolean sourceSequenceEnabled,
        boolean sourceSequenceRequired,
        String sourceSequenceOrigin,
        String sourceSequenceBindingFingerprint,
        boolean sourceSequenceQualified,
        String sourceSequenceEvidenceStatus,
        Long sourceSequenceEpoch,
        Long sourceSequenceFirst,
        Long sourceSequenceLast,
        Integer sourceSequenceObservationCount,
        Instant sourceSequenceFirstObservedAt,
        Instant sourceSequenceLastObservedAt,
        Instant sourceSequenceValidUntil,
        String sourceSequenceEvidenceEventId,
        Long sourceSequenceEvidenceRevision,
        boolean ready,
        List<String> readinessIssues) {
}
