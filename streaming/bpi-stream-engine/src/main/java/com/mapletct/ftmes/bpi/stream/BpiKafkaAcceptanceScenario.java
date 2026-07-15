package com.mapletct.ftmes.bpi.stream;

import com.mapletct.ftmes.bpi.contract.v1.BoundaryConditionOperatorV1;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryEvidenceClassV1;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryEvidenceConditionV1;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryRulePublicationV1;
import com.mapletct.ftmes.bpi.contract.v1.BoundarySignalBindingV1;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryType;
import com.mapletct.ftmes.bpi.contract.v1.PointCalibrationStatusV1;
import com.mapletct.ftmes.bpi.contract.v1.PointCatalogPointV1;
import com.mapletct.ftmes.bpi.contract.v1.PointCatalogSnapshotV1;
import com.mapletct.ftmes.bpi.contract.v1.PointDeviceStateV1;
import com.mapletct.ftmes.bpi.contract.v1.PointValue;
import com.mapletct.ftmes.bpi.contract.v1.ProductionContextEventV1;
import com.mapletct.ftmes.bpi.contract.v1.SequenceOrigin;
import com.mapletct.ftmes.bpi.contract.v1.TelemetryEnvelopeV1;

import java.time.Instant;
import java.util.List;
import java.util.regex.Pattern;

final class BpiKafkaAcceptanceScenario {

    private static final Pattern SAFE_MARKER = Pattern.compile("[A-Za-z0-9._-]{8,80}");

    private BpiKafkaAcceptanceScenario() {
    }

    static Scenario create(String marker, Instant baseTime) {
        if (marker == null || !SAFE_MARKER.matcher(marker).matches()) {
            throw new IllegalArgumentException("replay marker must be 8-80 safe token characters");
        }
        if (baseTime == null || baseTime.toEpochMilli() <= 0) {
            throw new IllegalArgumentException("baseTime must be positive");
        }
        String tenantId = "TENANT-E2E";
        String plantId = "PLANT-E2E";
        String lineId = "LINE-" + marker;
        String ruleCode = "START-" + marker;
        String orderId = "MO-" + marker;
        String deviceId = "DEVICE-" + marker;

        BoundaryRulePublicationV1 publication = BoundaryRulePublicationV1.newBuilder()
                .setEventId(marker + "-RULE-ACTIVE")
                .setTenantId(tenantId)
                .setPlantId(plantId)
                .setLineId(lineId)
                .setLocalityGroup("FEED")
                .setTopologyCode("TOPO-E2E")
                .setTopologyVersion("1")
                .setRuleCode(ruleCode)
                .setRuleVersion("1")
                .setBoundaryType(BoundaryType.START)
                .setQuorumMinimum(1)
                .setMinimumConfidence(1.0)
                .setMaxCompositePenalty(0.0)
                .setAllowedLatenessMs(30_000)
                .setWatermarkDelayMs(30_000)
                .setEvaluationTimeoutMs(300_000)
                .addConditions(BoundaryEvidenceConditionV1.newBuilder()
                        .setSignal("feed.flow")
                        .setOperator(BoundaryConditionOperatorV1.GREATER_THAN)
                        .setThresholdDecimal("2")
                        .setHoldForMs(0)
                        .setMaxSilenceMs(120_000)
                        .setClassification(BoundaryEvidenceClassV1.QUORUM)
                        .setWeight(100))
                .addSignalBindings(BoundarySignalBindingV1.newBuilder()
                        .setProductId("PRODUCT-E2E")
                        .setDeviceId(deviceId)
                        .setPropertyId("flow")
                        .setSignal("feed.flow")
                        .setExpectedUnit("m3/h")
                        .setCalibrationVersion("E2E-1"))
                .setActive(true)
                .setPublishedAtMs(baseTime.toEpochMilli())
                .setChecksum("acceptance:" + marker)
                .putHeaders("acceptance_marker", marker)
                .build();

        ProductionContextEventV1 context = ProductionContextEventV1.newBuilder()
                .setEventId(marker + "-CONTEXT")
                .setTenantId(tenantId)
                .setPlantId(plantId)
                .setLineId(lineId)
                .setOrderId(orderId)
                .setTaskId("TASK-" + marker)
                .setMaterialCode("MATERIAL-E2E")
                .setRecipeVersion("RECIPE-E2E-1")
                .setEffectiveFromMs(baseTime.minusSeconds(60).toEpochMilli())
                .setContextRevision(1)
                .setActive(true)
                .putAttributes("acceptance_marker", marker)
                .build();

        List<TelemetryEnvelopeV1> telemetry = List.of(
                telemetry(marker, tenantId, plantId, lineId, deviceId, baseTime.plusSeconds(1), 1),
                telemetry(marker, tenantId, plantId, lineId, deviceId, baseTime.plusSeconds(2), 2),
                telemetry(marker, tenantId, plantId, lineId, deviceId, baseTime.plusSeconds(3), 3));
        PointCatalogSnapshotV1 pointCatalog = PointCatalogSnapshotV1.newBuilder()
                .setEventId(marker + "-POINT-CATALOG")
                .setSource("ACCEPTANCE")
                .setSourceInstance("LOCAL-KAFKA")
                .setSourceRevision("sha256:" + marker)
                .setTenantId(tenantId)
                .setPlantId(plantId)
                .setLineId(lineId)
                .setObservedAtMs(baseTime.minusSeconds(1).toEpochMilli())
                .setReason("Controlled local Kafka acceptance")
                .addPoints(PointCatalogPointV1.newBuilder()
                        .setProductId("PRODUCT-E2E")
                        .setDeviceId(deviceId)
                        .setPropertyId("flow")
                        .setUnit("m3/h")
                        .setDataType("double")
                        .setDeviceState(PointDeviceStateV1.POINT_DEVICE_ACTIVE)
                        .setRegistered(true)
                        .setPropertyPresent(true)
                        .setCalibrationVersion("E2E-1")
                        .setCalibrationStatus(PointCalibrationStatusV1.POINT_CALIBRATION_VERIFIED)
                        .setSourceSequenceEnabled(true))
                .build();
        return new Scenario(marker, tenantId, plantId, lineId, ruleCode, orderId, deviceId,
                publication, pointCatalog, context, telemetry);
    }

    private static TelemetryEnvelopeV1 telemetry(
            String marker,
            String tenantId,
            String plantId,
            String lineId,
            String deviceId,
            Instant eventTime,
            long sequence) {
        return TelemetryEnvelopeV1.newBuilder()
                .setEventId(marker + "-TELEMETRY-" + sequence)
                .setMessageId(marker + "-MESSAGE-" + sequence)
                .setTenantId(tenantId)
                .setPlantId(plantId)
                .setLineId(lineId)
                .setGatewayId("GATEWAY-E2E")
                .setProductId("PRODUCT-E2E")
                .setDeviceId(deviceId)
                .setEventTimeMs(eventTime.toEpochMilli())
                .setIngestTimeMs(Instant.now().toEpochMilli())
                .setSequence(sequence)
                .setSourceEpoch(1)
                .setSequenceOrigin(SequenceOrigin.GATEWAY)
                .addPoints(PointValue.newBuilder()
                        .setPropertyId("flow")
                        .setDoubleValue(3.0 + sequence)
                        .setUnit("m3/h")
                        .setQualityCode("GOOD")
                        .setSampleTimeMs(eventTime.toEpochMilli())
                        .setCalibrationVersion("E2E-1"))
                .putHeaders("acceptance_marker", marker)
                .build();
    }

    record Scenario(
            String marker,
            String tenantId,
            String plantId,
            String lineId,
            String ruleCode,
            String orderId,
            String deviceId,
            BoundaryRulePublicationV1 publication,
            PointCatalogSnapshotV1 pointCatalog,
            ProductionContextEventV1 context,
            List<TelemetryEnvelopeV1> telemetry) {

        BoundaryRulePublicationV1 inactivePublication(Instant publishedAt) {
            return publication.toBuilder()
                    .setEventId(marker + "-RULE-INACTIVE")
                    .setActive(false)
                    .setPublishedAtMs(publishedAt.toEpochMilli())
                    .build();
        }

        String ruleKey() {
            return tenantId + "|" + plantId + "|" + lineId + "|" + ruleCode + "|1";
        }

        String contextKey() {
            return tenantId + "|" + plantId + "|" + lineId;
        }
    }
}
