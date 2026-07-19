package com.mapletct.ftmes.bpi.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ShadowRunBatchSource(
        UUID id,
        String batchNo,
        String plantId,
        String lineId,
        String state,
        boolean shadow,
        Instant startTime,
        Instant endTime,
        BigDecimal quantity,
        String quantityUnit,
        UUID ruleVersionId,
        UUID topologyVersionId) {
}
