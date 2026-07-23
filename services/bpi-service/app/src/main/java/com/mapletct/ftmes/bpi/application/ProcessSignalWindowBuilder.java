package com.mapletct.ftmes.bpi.application;

import com.mapletct.ftmes.bpi.domain.ProcessSignalWindowDefinition;
import com.mapletct.ftmes.bpi.domain.ProcessSignalWindowEvidence;
import com.mapletct.ftmes.bpi.domain.ProcessSignalWindowFact;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ProcessSignalWindowBuilder {
    private final CanonicalJson canonicalJson;

    public ProcessSignalWindowBuilder(CanonicalJson canonicalJson) {
        this.canonicalJson = canonicalJson;
    }

    public List<ProcessSignalWindowFact> build(List<ProcessSignalWindowEvidence> evidenceRows) {
        return evidenceRows.stream()
                .map(this::build)
                .sorted(java.util.Comparator
                        .comparing(ProcessSignalWindowFact::lineId)
                        .thenComparing(ProcessSignalWindowFact::predictionTime)
                        .thenComparing(ProcessSignalWindowFact::batchId)
                        .thenComparing(fact -> fact.definition().featureRef()))
                .toList();
    }

    public ProcessSignalWindowFact build(ProcessSignalWindowEvidence evidence) {
        ProcessSignalWindowDefinition definition = evidence.definition();
        List<String> blockers = new ArrayList<>();
        if (evidence.bindingCount() == 0) {
            blockers.add("WINDOW_BINDING_MISSING");
        } else if (evidence.bindingCount() > 1) {
            blockers.add("WINDOW_BINDING_AMBIGUOUS");
        }
        if (evidence.bindingCount() == 1
                && !definition.expectedUnit().equals(evidence.bindingExpectedUnit())) {
            blockers.add("WINDOW_BINDING_UNIT_MISMATCH");
        }
        if (evidence.productId() == null || evidence.deviceId() == null
                || evidence.propertyId() == null || evidence.pointCatalogRegistered() == null) {
            blockers.add("WINDOW_POINT_CATALOG_MISSING");
        } else {
            boolean pointReady = "ACTIVE".equals(evidence.pointCatalogDeviceState())
                    && Boolean.TRUE.equals(evidence.pointCatalogRegistered())
                    && Boolean.TRUE.equals(evidence.pointCatalogPropertyPresent())
                    && (!definition.requireCalibration()
                    || "VERIFIED".equals(evidence.pointCatalogCalibrationStatus()));
            if (!pointReady) blockers.add("WINDOW_POINT_NOT_READY");
            if (!definition.expectedUnit().equals(evidence.pointCatalogUnit())) {
                blockers.add("WINDOW_POINT_CATALOG_UNIT_MISMATCH");
            }
            if (definition.requireCalibration()
                    && (!sameNonBlank(evidence.bindingCalibrationVersion(),
                    evidence.pointCatalogCalibrationVersion()))) {
                blockers.add("WINDOW_CALIBRATION_MISMATCH");
            }
        }
        if (evidence.unitMismatchCount() > 0) {
            blockers.add("WINDOW_UNIT_MISMATCH");
        }
        if (evidence.valueTypeMismatchCount() > 0) {
            blockers.add("WINDOW_VALUE_TYPE_MISMATCH");
        }
        if (evidence.calibrationMismatchCount() > 0) {
            blockers.add("WINDOW_CALIBRATION_MISMATCH");
        }
        if (evidence.acceptedSampleCount() < definition.minimumSamples()) {
            blockers.add("WINDOW_SAMPLE_COUNT_BELOW_MINIMUM");
        }
        if (evidence.maximumObservedGapSeconds() == null
                || evidence.maximumObservedGapSeconds()
                .compareTo(BigDecimal.valueOf(definition.maximumGapSeconds())) > 0) {
            blockers.add("WINDOW_MAX_GAP_EXCEEDED");
        }
        BigDecimal value = metricValue(evidence);
        if (value == null) blockers.add("WINDOW_METRIC_UNAVAILABLE");

        List<String> stableBlockers = blockers.stream().distinct().sorted().toList();
        String state = stableBlockers.isEmpty() ? "READY" : "BLOCKED";
        Map<String, Object> checksumPayload = new LinkedHashMap<>();
        checksumPayload.put("reviewId", evidence.reviewId());
        checksumPayload.put("batchId", evidence.batchId());
        checksumPayload.put("ruleVersionId", evidence.ruleVersionId());
        checksumPayload.put("topologyVersionId", evidence.topologyVersionId());
        checksumPayload.put("pointCatalogSnapshotId", evidence.pointCatalogSnapshotId());
        checksumPayload.put("windowDefinitionChecksum", definition.checksum());
        checksumPayload.put("predictionTime", evidence.predictionTime());
        checksumPayload.put("windowStart", evidence.windowStart());
        checksumPayload.put("windowEnd", evidence.windowEnd());
        checksumPayload.put("productId", evidence.productId());
        checksumPayload.put("deviceId", evidence.deviceId());
        checksumPayload.put("propertyId", evidence.propertyId());
        checksumPayload.put("sourcePointCount", evidence.sourcePointCount());
        checksumPayload.put("acceptedSampleCount", evidence.acceptedSampleCount());
        checksumPayload.put("rejectedQualityCount", evidence.rejectedQualityCount());
        checksumPayload.put("lateAvailabilityCount", evidence.lateAvailabilityCount());
        checksumPayload.put("unitMismatchCount", evidence.unitMismatchCount());
        checksumPayload.put("valueTypeMismatchCount", evidence.valueTypeMismatchCount());
        checksumPayload.put("calibrationMismatchCount", evidence.calibrationMismatchCount());
        checksumPayload.put("firstSampleTime", evidence.firstSampleTime());
        checksumPayload.put("lastSampleTime", evidence.lastSampleTime());
        checksumPayload.put("latestIngestTime", evidence.latestIngestTime());
        checksumPayload.put("maximumObservedGapSeconds",
                evidence.maximumObservedGapSeconds());
        checksumPayload.put("numericValue", value);
        checksumPayload.put("sourceFingerprint", evidence.sourceFingerprint());
        checksumPayload.put("state", state);
        checksumPayload.put("blockerCodes", stableBlockers);

        return new ProcessSignalWindowFact(
                evidence.reviewId(), evidence.shadowRunId(), evidence.batchId(),
                evidence.batchNo(), evidence.plantId(), evidence.lineId(),
                evidence.ruleVersionId(), evidence.topologyVersionId(),
                evidence.pointCatalogSnapshotId(), definition,
                evidence.predictionTime(), evidence.windowStart(), evidence.windowEnd(),
                evidence.productId(), evidence.deviceId(), evidence.propertyId(),
                evidence.bindingCalibrationVersion(),
                evidence.pointCatalogCalibrationVersion(),
                evidence.pointCatalogDeviceState(), evidence.pointCatalogRegistered(),
                evidence.pointCatalogPropertyPresent(),
                evidence.pointCatalogCalibrationStatus(),
                evidence.sourcePointCount(), evidence.acceptedSampleCount(),
                evidence.rejectedQualityCount(), evidence.lateAvailabilityCount(),
                evidence.unitMismatchCount(), evidence.valueTypeMismatchCount(),
                evidence.calibrationMismatchCount(), evidence.firstSampleTime(),
                evidence.lastSampleTime(), evidence.latestIngestTime(),
                evidence.maximumObservedGapSeconds(), value,
                evidence.sourceFingerprint(), state, stableBlockers,
                Checksums.sha256(canonicalJson.write(checksumPayload)));
    }

    private BigDecimal metricValue(ProcessSignalWindowEvidence evidence) {
        return switch (evidence.definition().metric()) {
            case "MEAN" -> evidence.meanValue();
            case "MIN" -> evidence.minimumValue();
            case "MAX" -> evidence.maximumValue();
            case "LAST" -> evidence.lastValue();
            case "DELTA" -> evidence.firstValue() == null || evidence.lastValue() == null
                    ? null : evidence.lastValue().subtract(evidence.firstValue());
            case "SLOPE" -> evidence.slopeValue();
            case "TRUE_RATIO" -> evidence.trueRatioValue();
            default -> null;
        };
    }

    private boolean sameNonBlank(String first, String second) {
        return first != null && !first.isBlank() && first.equals(second);
    }
}
