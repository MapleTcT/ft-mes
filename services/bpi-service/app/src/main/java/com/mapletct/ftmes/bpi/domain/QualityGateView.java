package com.mapletct.ftmes.bpi.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record QualityGateView(
        UUID id,
        String externalGateId,
        long externalRevision,
        String sourceEventId,
        QualityGateState state,
        BigDecimal releaseQuantity,
        String quantityUnit,
        String materialCode,
        Instant observedAt,
        List<QualityInspectionView> inspections) {
}
