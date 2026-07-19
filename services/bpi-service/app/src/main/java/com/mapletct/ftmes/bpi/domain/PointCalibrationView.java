package com.mapletct.ftmes.bpi.domain;

import java.time.Instant;
import java.util.UUID;

public record PointCalibrationView(
        UUID id,
        String plantId,
        String lineId,
        String productId,
        String deviceId,
        String propertyId,
        String calibrationVersion,
        String certificateReference,
        String certificateChecksum,
        Instant validFrom,
        Instant validUntil,
        String state,
        long revision,
        String submittedBy,
        Instant submittedAt,
        String submitReason,
        String decidedBy,
        Instant decidedAt,
        String decisionReason,
        String revokedBy,
        Instant revokedAt,
        String revokeReason,
        boolean effective,
        String effectivenessStatus) {
}
