package com.mapletct.ftmes.bpi.stream;

import com.mapletct.ftmes.bpi.contract.v1.PointValue;
import com.mapletct.ftmes.bpi.contract.v1.ProductionContextEventV1;
import com.mapletct.ftmes.bpi.contract.v1.TelemetryEnvelopeV1;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductionContextJoinStateCodecTest {

    private static final Instant T0 = Instant.parse("2026-07-12T08:00:00Z");

    @Test
    void stateRoundTripsContextAndPendingTelemetryDeterministically() {
        ProductionContextEventV1 context = context();
        TelemetryPointEvent telemetry = point();
        ProductionContextJoinState state = new ProductionContextJoinState(
                List.of(context), List.of(new PendingContextPoint(telemetry, T0.plusSeconds(40).toEpochMilli())));

        ProductionContextJoinState restored = ProductionContextJoinStateCodec.decode(
                ProductionContextJoinStateCodec.encode(state));

        assertEquals(state, restored);
        assertEquals(
                Arrays.toString(ProductionContextJoinStateCodec.encode(state)),
                Arrays.toString(ProductionContextJoinStateCodec.encode(restored)));
    }

    @Test
    void stateRejectsUnknownVersion() {
        byte[] encoded = ProductionContextJoinStateCodec.encode(ProductionContextJoinState.empty());
        encoded[7] = 99;

        assertThrows(IllegalStateException.class, () -> ProductionContextJoinStateCodec.decode(encoded));
    }

    @Test
    void telemetryPointWireRoundTripsWithoutProtobufKryo() {
        TelemetryPointEvent event = point();

        assertEquals(event, TelemetryPointEventCodec.decode(TelemetryPointEventCodec.encode(event)));
    }

    @Test
    void telemetryPointWireRejectsUnknownVersion() {
        byte[] encoded = TelemetryPointEventCodec.encode(point());
        encoded[7] = 99;

        assertThrows(IllegalStateException.class, () -> TelemetryPointEventCodec.decode(encoded));
    }

    static ProductionContextEventV1 context() {
        return ProductionContextEventV1.newBuilder()
                .setEventId("CTX-1")
                .setTenantId("TENANT-A")
                .setPlantId("PLANT-01")
                .setLineId("LINE-01")
                .setOrderId("MO-1")
                .setEffectiveFromMs(T0.toEpochMilli())
                .setContextRevision(1)
                .setActive(true)
                .build();
    }

    static TelemetryPointEvent point() {
        TelemetryEnvelopeV1 envelope = TelemetryEnvelopeV1.newBuilder()
                .setEventId("TEL-1")
                .setTenantId("TENANT-A")
                .setPlantId("PLANT-01")
                .setLineId("LINE-01")
                .setDeviceId("DEVICE-1")
                .setEventTimeMs(T0.plusSeconds(10).toEpochMilli())
                .addPoints(PointValue.newBuilder()
                        .setPropertyId("flow")
                        .setDoubleValue(3)
                        .setUnit("m3/h")
                        .setQualityCode("GOOD")
                        .setSampleTimeMs(T0.plusSeconds(10).toEpochMilli()))
                .build();
        return new TelemetryPointEvent(envelope, 0);
    }
}
