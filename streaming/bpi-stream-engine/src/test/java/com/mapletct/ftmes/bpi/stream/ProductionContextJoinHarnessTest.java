package com.mapletct.ftmes.bpi.stream;

import com.mapletct.ftmes.bpi.contract.v1.ProductionContextEventV1;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.runtime.checkpoint.OperatorSubtaskState;
import org.apache.flink.streaming.api.operators.co.KeyedCoProcessOperator;
import org.apache.flink.streaming.api.watermark.Watermark;
import org.apache.flink.streaming.runtime.streamrecord.StreamRecord;
import org.apache.flink.streaming.util.KeyedTwoInputStreamOperatorTestHarness;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionContextJoinHarnessTest {

    private static final Instant T0 = Instant.parse("2026-07-12T08:00:00Z");

    @Test
    void contextFirstJoinsTelemetryImmediately() throws Exception {
        try (Harness harness = harness()) {
            harness.open();
            ProductionContextEventV1 context = context("CTX-1", 1, "MO-1", T0);
            TelemetryPointEvent telemetry = point("TEL-1", 3);

            harness.processElement2(context, context.getEffectiveFromMs());
            harness.processElement1(telemetry, telemetry.eventTime().toEpochMilli());

            assertEquals(1, outputs(harness).size());
            assertEquals("MO-1", outputs(harness).get(0).context().getOrderId());
            assertTrue(issueCodes(harness).isEmpty());
        }
    }

    @Test
    void telemetryWaitsAndLateContextFlushesItByPointInTime() throws Exception {
        try (Harness harness = harness()) {
            harness.open();
            TelemetryPointEvent telemetry = point("TEL-1", 3);
            ProductionContextEventV1 context = context("CTX-1", 1, "MO-1", T0);

            harness.processElement1(telemetry, telemetry.eventTime().toEpochMilli());
            assertTrue(outputs(harness).isEmpty());
            harness.processElement2(context, context.getEffectiveFromMs());

            assertEquals(1, outputs(harness).size());
            assertEquals("TEL-1", outputs(harness).get(0).telemetry().envelope().getEventId());
            assertTrue(issueCodes(harness).isEmpty());
        }
    }

    @Test
    void timeoutRequiresBothInputWatermarksAndProducesOneIssue() throws Exception {
        try (Harness harness = harness()) {
            harness.open();
            TelemetryPointEvent telemetry = point("TEL-1", 3);
            harness.processElement1(telemetry, telemetry.eventTime().toEpochMilli());

            harness.processWatermark1(new Watermark(T0.plusSeconds(40).toEpochMilli()));
            assertTrue(issueCodes(harness).isEmpty());
            harness.processWatermark2(new Watermark(T0.plusSeconds(40).toEpochMilli()));

            assertEquals(List.of("CONTEXT_WAIT_EXPIRED"), issueCodes(harness));
            assertTrue(outputs(harness).isEmpty());
        }
    }

    @Test
    void identicalPendingReplayIsIdempotentAndChangedIdentityIsRejected() throws Exception {
        try (Harness harness = harness()) {
            harness.open();
            TelemetryPointEvent telemetry = point("TEL-1", 3);
            TelemetryPointEvent conflict = point("TEL-1", 4);

            harness.processElement1(telemetry, telemetry.eventTime().toEpochMilli());
            harness.processElement1(telemetry, telemetry.eventTime().toEpochMilli());
            harness.processElement1(conflict, conflict.eventTime().toEpochMilli());

            assertEquals(List.of("EVENT_ID_CONFLICT"), issueCodes(harness));
            assertTrue(outputs(harness).isEmpty());
        }
    }

    @Test
    void checkpointRestorePreservesPendingTelemetryUntilContextArrives() throws Exception {
        OperatorSubtaskState snapshot;
        TelemetryPointEvent telemetry = point("TEL-1", 3);
        try (Harness first = harness()) {
            first.open();
            first.processElement1(telemetry, telemetry.eventTime().toEpochMilli());
            snapshot = first.snapshot(1, T0.plusSeconds(11).toEpochMilli());
        }

        try (Harness restored = harness()) {
            restored.initializeState(snapshot);
            restored.open();
            ProductionContextEventV1 context = context("CTX-1", 1, "MO-1", T0);
            restored.processElement2(context, context.getEffectiveFromMs());

            assertEquals(1, outputs(restored).size());
            assertTrue(issueCodes(restored).isEmpty());
        }
    }

    @Test
    void conflictingContextRevisionIsIsolated() throws Exception {
        try (Harness harness = harness()) {
            harness.open();
            ProductionContextEventV1 first = context("CTX-1", 1, "MO-1", T0);
            ProductionContextEventV1 conflict = context("CTX-2", 1, "MO-2", T0.plusSeconds(1));

            harness.processElement2(first, first.getEffectiveFromMs());
            harness.processElement2(conflict, conflict.getEffectiveFromMs());

            assertEquals(List.of("CONTEXT_REJECTED"), issueCodes(harness));
        }
    }

    private static Harness harness() throws Exception {
        return new Harness(new KeyedCoProcessOperator<>(
                new ProductionContextJoinFunction(Duration.ofSeconds(30), Duration.ofMinutes(2))));
    }

    private static ProductionContextEventV1 context(
            String eventId,
            long revision,
            String orderId,
            Instant effectiveFrom) {
        return ProductionContextJoinStateCodecTest.context().toBuilder()
                .setEventId(eventId)
                .setContextRevision(revision)
                .setOrderId(orderId)
                .setEffectiveFromMs(effectiveFrom.toEpochMilli())
                .build();
    }

    private static TelemetryPointEvent point(String eventId, double value) {
        TelemetryPointEvent base = ProductionContextJoinStateCodecTest.point();
        return new TelemetryPointEvent(base.envelope().toBuilder()
                .setEventId(eventId)
                .setPoints(0, base.point().toBuilder().setDoubleValue(value))
                .build(), 0);
    }

    private static List<ContextualTelemetryPoint> outputs(Harness harness) {
        return harness.getOutput().stream()
                .filter(StreamRecord.class::isInstance)
                .map(StreamRecord.class::cast)
                .map(StreamRecord::getValue)
                .filter(byte[].class::isInstance)
                .map(byte[].class::cast)
                .map(ContextualTelemetryPointCodec::decode)
                .toList();
    }

    private static List<String> issueCodes(Harness harness) {
        ConcurrentLinkedQueue<StreamRecord<ContextJoinIssue>> issues =
                harness.getSideOutput(ProductionContextJoinFunction.ISSUES);
        if (issues == null) {
            return List.of();
        }
        return issues.stream().map(StreamRecord::getValue).map(ContextJoinIssue::code).toList();
    }

    private static final class Harness extends KeyedTwoInputStreamOperatorTestHarness<
            String, TelemetryPointEvent, ProductionContextEventV1, byte[]> {

        private Harness(KeyedCoProcessOperator<
                String, TelemetryPointEvent, ProductionContextEventV1, byte[]> operator)
                throws Exception {
            super(
                    operator,
                    TelemetryPointEvent::scopeKey,
                    TelemetryPointEvent::contextScopeKey,
                    TypeInformation.of(String.class),
                    1,
                    1,
                    0);
        }
    }
}
