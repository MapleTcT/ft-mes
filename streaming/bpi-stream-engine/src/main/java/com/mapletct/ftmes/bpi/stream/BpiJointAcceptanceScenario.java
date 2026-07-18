package com.mapletct.ftmes.bpi.stream;

import com.mapletct.ftmes.bpi.contract.v1.PointCalibrationStatusV1;
import com.mapletct.ftmes.bpi.contract.v1.PointCatalogPointV1;
import com.mapletct.ftmes.bpi.contract.v1.PointCatalogSnapshotV1;
import com.mapletct.ftmes.bpi.contract.v1.PointDeviceStateV1;
import com.mapletct.ftmes.bpi.contract.v1.PointValue;
import com.mapletct.ftmes.bpi.contract.v1.ProductionContextEventV1;
import com.mapletct.ftmes.bpi.contract.v1.SequenceOrigin;
import com.mapletct.ftmes.bpi.contract.v1.TelemetryEnvelopeV1;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;

final class BpiJointAcceptanceScenario {

    private BpiJointAcceptanceScenario() {
    }

    static Scenario create(BpiJointAcceptanceReplayConfig config, Instant baseTime) {
        if (baseTime == null || baseTime.toEpochMilli() <= 0) {
            throw new IllegalArgumentException("baseTime must be positive");
        }
        long contextRevision = baseTime.getEpochSecond();
        long sourceEpoch = baseTime.getEpochSecond();
        PointCatalogSnapshotV1 pointCatalog = pointCatalog(config, baseTime.minusSeconds(1));
        ProductionContextEventV1 context = ProductionContextEventV1.newBuilder()
                .setEventId(config.marker() + "-CONTEXT")
                .setTenantId(config.tenantId())
                .setPlantId(config.plantId())
                .setLineId(config.lineId())
                .setOrderId(config.orderId())
                .setTaskId("TASK-" + config.marker())
                .setMaterialCode("MATERIAL-BPI-ACCEPTANCE")
                .setRecipeVersion("RECIPE-BPI-ACCEPTANCE-1")
                .setEffectiveFromMs(baseTime.minusSeconds(60).toEpochMilli())
                .setContextRevision(contextRevision)
                .setActive(true)
                .putAttributes("acceptance_marker", config.marker())
                .build();

        List<TelemetryEnvelopeV1> telemetry = List.of(
                telemetry(config, baseTime.plusSeconds(1), sourceEpoch, 1),
                telemetry(config, baseTime.plusSeconds(2), sourceEpoch, 2),
                telemetry(config, baseTime.plusSeconds(3), sourceEpoch, 3));
        return new Scenario(config, pointCatalog, context, telemetry);
    }

    private static PointCatalogSnapshotV1 pointCatalog(
            BpiJointAcceptanceReplayConfig config,
            Instant observedAt) {
        PointCatalogPointV1 flow = point(
                config,
                config.flowPropertyId(),
                "instantFlow",
                "进料瞬时流量",
                config.flowUnit(),
                "double");
        PointCatalogPointV1 pump = point(
                config,
                config.pumpPropertyId(),
                "pumpRunning",
                "进料泵运行",
                config.pumpUnit(),
                "boolean");
        String sourceInstance = "BPI-JOINT-" + config.marker();
        PointCatalogSnapshotV1 content = PointCatalogSnapshotV1.newBuilder()
                .setSource("ACCEPTANCE")
                .setSourceInstance(sourceInstance)
                .setTenantId(config.tenantId())
                .setPlantId(config.plantId())
                .setLineId(config.lineId())
                .addPoints(flow)
                .addPoints(pump)
                .build();
        String digest = sha256(content.toByteArray());
        return content.toBuilder()
                .setEventId("point-catalog-" + digest)
                .setSourceRevision("sha256:" + digest)
                .setObservedAtMs(observedAt.toEpochMilli())
                .setReason("Controlled browser-rule joint acceptance")
                .build();
    }

    private static PointCatalogPointV1 point(
            BpiJointAcceptanceReplayConfig config,
            String propertyId,
            String sourcePropertyId,
            String pointName,
            String unit,
            String dataType) {
        return PointCatalogPointV1.newBuilder()
                .setLocalityGroup(config.localityGroup())
                .setProductId(config.productId())
                .setDeviceId(config.deviceId())
                .setPropertyId(propertyId)
                .setSourcePropertyId(sourcePropertyId)
                .setPointName(pointName)
                .setUnit(unit)
                .setDataType(dataType)
                .setDeviceState(PointDeviceStateV1.POINT_DEVICE_ACTIVE)
                .setRegistered(true)
                .setPropertyPresent(true)
                .setCalibrationVersion(config.calibrationVersion())
                .setCalibrationStatus(PointCalibrationStatusV1.POINT_CALIBRATION_VERIFIED)
                .setSourceSequenceEnabled(true)
                .build();
    }

    private static String sha256(byte[] value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is not available", error);
        }
    }

    private static TelemetryEnvelopeV1 telemetry(
            BpiJointAcceptanceReplayConfig config,
            Instant eventTime,
            long sourceEpoch,
            long sequence) {
        return TelemetryEnvelopeV1.newBuilder()
                .setEventId(config.marker() + "-TELEMETRY-" + sequence)
                .setMessageId(config.marker() + "-MESSAGE-" + sequence)
                .setTenantId(config.tenantId())
                .setPlantId(config.plantId())
                .setLineId(config.lineId())
                .setGatewayId("GATEWAY-BPI-ACCEPTANCE")
                .setProductId(config.productId())
                .setDeviceId(config.deviceId())
                .setEventTimeMs(eventTime.toEpochMilli())
                .setIngestTimeMs(Instant.now().toEpochMilli())
                .setSequence(sequence)
                .setSourceEpoch(sourceEpoch)
                .setSequenceOrigin(SequenceOrigin.GATEWAY)
                .addPoints(PointValue.newBuilder()
                        .setPropertyId(config.flowPropertyId())
                        .setDoubleValue(18.0 + sequence)
                        .setUnit(config.flowUnit())
                        .setQualityCode("GOOD")
                        .setSampleTimeMs(eventTime.toEpochMilli())
                        .setCalibrationVersion(config.calibrationVersion()))
                .addPoints(PointValue.newBuilder()
                        .setPropertyId(config.pumpPropertyId())
                        .setBoolValue(true)
                        .setUnit(config.pumpUnit())
                        .setQualityCode("GOOD")
                        .setSampleTimeMs(eventTime.toEpochMilli())
                        .setCalibrationVersion(config.calibrationVersion()))
                .putHeaders("acceptance_marker", config.marker())
                .build();
    }

    record Scenario(
            BpiJointAcceptanceReplayConfig config,
            PointCatalogSnapshotV1 pointCatalog,
            ProductionContextEventV1 context,
            List<TelemetryEnvelopeV1> telemetry) {

        String marker() {
            return config.marker();
        }

        String tenantId() {
            return config.tenantId();
        }

        String plantId() {
            return config.plantId();
        }

        String lineId() {
            return config.lineId();
        }

        String ruleCode() {
            return config.ruleCode();
        }

        String orderId() {
            return config.orderId();
        }

        String deviceId() {
            return config.deviceId();
        }

        String contextKey() {
            return tenantId() + "|" + plantId() + "|" + lineId();
        }

        ProductionContextEventV1 closingContext() {
            long closedAt = telemetry.get(telemetry.size() - 1).getEventTimeMs() + 1;
            return context.toBuilder()
                    .setEventId(marker() + "-CONTEXT-CLOSED")
                    .setEffectiveFromMs(closedAt)
                    .setContextRevision(context.getContextRevision() + 1)
                    .setActive(false)
                    .putAttributes("acceptance_cleanup", "true")
                    .build();
        }
    }
}
