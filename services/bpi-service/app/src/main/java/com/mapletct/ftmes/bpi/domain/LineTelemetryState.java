package com.mapletct.ftmes.bpi.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record LineTelemetryState(
        boolean topologyBound,
        String primarySignal,
        String productId,
        String deviceId,
        String propertyId,
        String value,
        BigDecimal numericValue,
        String unit,
        String qualityCode,
        String sequenceOrigin,
        String sequenceDisposition,
        Instant sampleTime,
        String calibrationVersion,
        long lagSeconds,
        boolean fresh,
        int expectedSignalCount,
        int observedSignalCount,
        int goodSignalCount,
        int openIncidentCount,
        int criticalIncidentCount) {
}
