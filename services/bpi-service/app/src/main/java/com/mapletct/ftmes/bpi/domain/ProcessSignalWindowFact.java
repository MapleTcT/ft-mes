package com.mapletct.ftmes.bpi.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ProcessSignalWindowFact(
        UUID reviewId,
        UUID shadowRunId,
        UUID batchId,
        String batchNo,
        String plantId,
        String lineId,
        UUID ruleVersionId,
        UUID topologyVersionId,
        UUID pointCatalogSnapshotId,
        ProcessSignalWindowDefinition definition,
        Instant predictionTime,
        Instant windowStart,
        Instant windowEnd,
        String productId,
        String deviceId,
        String propertyId,
        String bindingCalibrationVersion,
        String pointCatalogCalibrationVersion,
        String pointCatalogDeviceState,
        Boolean pointCatalogRegistered,
        Boolean pointCatalogPropertyPresent,
        String pointCatalogCalibrationStatus,
        int sourcePointCount,
        int acceptedSampleCount,
        int rejectedQualityCount,
        int lateAvailabilityCount,
        int unitMismatchCount,
        int valueTypeMismatchCount,
        int calibrationMismatchCount,
        Instant firstSampleTime,
        Instant lastSampleTime,
        Instant latestIngestTime,
        BigDecimal maximumObservedGapSeconds,
        BigDecimal numericValue,
        String sourceFingerprint,
        String state,
        List<String> blockerCodes,
        String factChecksum) {

    public Map<String, Object> evidencePayload() {
        java.util.LinkedHashMap<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("featureRef", definition.featureRef());
        payload.put("signal", definition.signal());
        payload.put("metric", definition.metric());
        payload.put("valueType", definition.valueType());
        payload.put("windowStart", windowStart);
        payload.put("windowEnd", windowEnd);
        payload.put("predictionTime", predictionTime);
        payload.put("physicalPoint", nullableMap(
                "productId", productId,
                "deviceId", deviceId,
                "propertyId", propertyId));
        payload.put("expectedUnit", definition.expectedUnit());
        payload.put("minimumSamples", definition.minimumSamples());
        payload.put("maximumGapSeconds", definition.maximumGapSeconds());
        payload.put("sourcePointCount", sourcePointCount);
        payload.put("acceptedSampleCount", acceptedSampleCount);
        payload.put("rejectedQualityCount", rejectedQualityCount);
        payload.put("lateAvailabilityCount", lateAvailabilityCount);
        payload.put("unitMismatchCount", unitMismatchCount);
        payload.put("valueTypeMismatchCount", valueTypeMismatchCount);
        payload.put("calibrationMismatchCount", calibrationMismatchCount);
        payload.put("maximumObservedGapSeconds", maximumObservedGapSeconds);
        payload.put("numericValue", numericValue);
        payload.put("state", state);
        payload.put("blockerCodes", blockerCodes);
        payload.put("sourceFingerprint", sourceFingerprint);
        payload.put("factChecksum", factChecksum);
        return payload;
    }

    private Map<String, String> nullableMap(
            String firstKey,
            String firstValue,
            String secondKey,
            String secondValue,
            String thirdKey,
            String thirdValue) {
        java.util.LinkedHashMap<String, String> values = new java.util.LinkedHashMap<>();
        values.put(firstKey, firstValue);
        values.put(secondKey, secondValue);
        values.put(thirdKey, thirdValue);
        return values;
    }
}
