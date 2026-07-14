package com.mapletct.ftmes.bpi.stream;

import com.mapletct.ftmes.bpi.contract.v1.PointValue;
import com.mapletct.ftmes.bpi.contract.v1.ProductionContextEventV1;
import com.mapletct.ftmes.bpi.contract.v1.SequenceOrigin;
import com.mapletct.ftmes.bpi.contract.v1.TelemetryEnvelopeV1;

import java.time.Instant;
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
        return new Scenario(config, context, telemetry);
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
                .setProductId("PRODUCT-BPI-ACCEPTANCE")
                .setDeviceId(config.deviceId())
                .setEventTimeMs(eventTime.toEpochMilli())
                .setIngestTimeMs(Instant.now().toEpochMilli())
                .setSequence(sequence)
                .setSourceEpoch(sourceEpoch)
                .setSequenceOrigin(SequenceOrigin.EXPORTER)
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
    }
}
