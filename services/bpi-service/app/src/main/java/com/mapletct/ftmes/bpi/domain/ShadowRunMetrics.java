package com.mapletct.ftmes.bpi.domain;

import java.math.BigDecimal;

public record ShadowRunMetrics(
        long observedDurationSeconds,
        int reviewedBatchCount,
        int acceptedBoundaryCount,
        int totalBoundaryCount,
        BigDecimal boundaryAgreement,
        int quantitySampleCount,
        BigDecimal automaticQuantityTotal,
        BigDecimal referenceQuantityTotal,
        String quantityUnit,
        BigDecimal cumulativeQuantityDeviationPercent,
        BigDecimal meanQuantityDeviationPercent,
        BigDecimal maximumQuantityDeviationPercent,
        int unresolvedCriticalIncidentCount,
        boolean durationGatePassed,
        boolean reviewCountGatePassed,
        boolean boundaryAgreementGatePassed,
        boolean quantityGatePassed,
        boolean dataQualityGatePassed) {
}
