package com.mapletct.ftmes.bpi.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record BatchInstance(
        UUID id,
        String batchNo,
        String tenantId,
        String plantId,
        String lineId,
        String stageCode,
        String orderId,
        String materialCode,
        BatchState state,
        long revision,
        boolean shadow,
        Instant startTime,
        Instant endTime,
        BigDecimal quantity,
        String quantityUnit,
        BigDecimal dryMatter,
        String qualityGate,
        String wmsStatus,
        String ruleVersion,
        String topologyVersion) {
}
