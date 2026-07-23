package com.mapletct.ftmes.bpi.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LineState(
        String plantId,
        String lineId,
        String lineName,
        String orderId,
        UUID currentBatchId,
        String stageCode,
        String status,
        BigDecimal confidence,
        BigDecimal instantFlow,
        BigDecimal totalizedQuantity,
        String dataHealth,
        int pendingCandidates,
        int affectedRules,
        Instant lastEventTime,
        LineTelemetryState telemetry) {
}
