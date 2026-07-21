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
        return create(Scope.defaults(marker), baseTime);
    }

    static Scenario create(BpiKafkaAcceptanceReplayConfig config, Instant baseTime) {
        if (config == null) {
            throw new IllegalArgumentException("replay config is required");
        }
        return create(new Scope(
                config.marker(),
                config.tenantId(),
                config.plantId(),
                config.lineId(),
                config.topologyCode(),
                config.topologyVersion(),
                config.ruleCode(),
                config.ruleVersion(),
                config.orderId(),
                config.productId(),
                config.deviceId(),
                config.pointCatalogSourceInstance()), baseTime);
    }

    private static Scenario create(Scope scope, Instant baseTime) {
        String marker = scope.marker();
        if (marker == null || !SAFE_MARKER.matcher(marker).matches()) {
            throw new IllegalArgumentException("replay marker must be 8-80 safe token characters");
        }
        if (baseTime == null || baseTime.toEpochMilli() <= 0) {
            throw new IllegalArgumentException("baseTime must be positive");
        }
        String tenantId = scope.tenantId();
        String plantId = scope.plantId();
        String lineId = scope.lineId();
        String ruleCode = scope.ruleCode();
        String orderId = scope.orderId();
        String deviceId = scope.deviceId();

        BoundaryRulePublicationV1 publication = BoundaryRulePublicationV1.newBuilder()
                .setEventId(marker + "-RULE-ACTIVE")
                .setTenantId(tenantId)
                .setPlantId(plantId)
                .setLineId(lineId)
                .setLocalityGroup("FEED")
                .setTopologyCode(scope.topologyCode())
                .setTopologyVersion(scope.topologyVersion())
                .setRuleCode(ruleCode)
                .setRuleVersion(scope.ruleVersion())
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
                        .setProductId(scope.productId())
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
                telemetry(marker, tenantId, plantId, lineId, scope.productId(), deviceId,
                        baseTime.plusSeconds(1), 1),
                telemetry(marker, tenantId, plantId, lineId, scope.productId(), deviceId,
                        baseTime.plusSeconds(2), 2),
                telemetry(marker, tenantId, plantId, lineId, scope.productId(), deviceId,
                        baseTime.plusSeconds(3), 3));
        PointCatalogSnapshotV1 pointCatalog = PointCatalogSnapshotV1.newBuilder()
                .setEventId(marker + "-POINT-CATALOG")
                .setSource("ACCEPTANCE")
                .setSourceInstance(scope.pointCatalogSourceInstance())
                .setSourceRevision("sha256:" + marker)
                .setTenantId(tenantId)
                .setPlantId(plantId)
                .setLineId(lineId)
                .setObservedAtMs(baseTime.minusSeconds(1).toEpochMilli())
                .setReason("Controlled local Kafka acceptance")
                .addPoints(PointCatalogPointV1.newBuilder()
                        .setProductId(scope.productId())
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
                scope.ruleVersion(), publication, pointCatalog, context, telemetry);
    }

    private static TelemetryEnvelopeV1 telemetry(
            String marker,
            String tenantId,
            String plantId,
            String lineId,
            String productId,
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
                .setProductId(productId)
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

    private record Scope(
            String marker,
            String tenantId,
            String plantId,
            String lineId,
            String topologyCode,
            String topologyVersion,
            String ruleCode,
            String ruleVersion,
            String orderId,
            String productId,
            String deviceId,
            String pointCatalogSourceInstance) {

        private static Scope defaults(String marker) {
            return new Scope(
                    marker,
                    "TENANT-E2E",
                    "PLANT-E2E",
                    "LINE-" + marker,
                    "TOPO-E2E",
                    "1",
                    "START-" + marker,
                    "1",
                    "MO-" + marker,
                    "PRODUCT-E2E",
                    "DEVICE-" + marker,
                    "LOCAL-KAFKA");
        }
    }

    record Scenario(
            String marker,
            String tenantId,
            String plantId,
            String lineId,
            String ruleCode,
            String orderId,
            String deviceId,
            String ruleVersion,
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
            return tenantId + "|" + plantId + "|" + lineId + "|" + ruleCode + "|" + ruleVersion;
        }

        String contextKey() {
            return tenantId + "|" + plantId + "|" + lineId;
        }
    }
}
