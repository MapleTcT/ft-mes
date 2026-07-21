package com.mapletct.ftmes.bpi.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record DatasetManifestSample(
        UUID reviewId,
        UUID shadowRunId,
        UUID batchId,
        String batchNo,
        String lineId,
        boolean included,
        List<String> exclusionReasons,
        Instant predictionTime,
        Instant featureCutoff,
        Instant labelAvailableAt,
        BigDecimal confidence,
        String splitKey,
        Map<String, Object> featurePayload,
        Map<String, Object> labelPayload,
        Map<String, Object> sourcePayload) {
}
