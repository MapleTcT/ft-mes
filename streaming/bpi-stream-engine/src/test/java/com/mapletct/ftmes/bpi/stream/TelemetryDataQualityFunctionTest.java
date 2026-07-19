package com.mapletct.ftmes.bpi.stream;

import com.mapletct.ftmes.bpi.contract.v1.DataQualityEventV1;
import com.mapletct.ftmes.bpi.contract.v1.DataQualitySeverity;
import com.mapletct.ftmes.bpi.contract.v1.PointValue;
import com.mapletct.ftmes.bpi.contract.v1.SequenceOrigin;
import com.mapletct.ftmes.bpi.contract.v1.TelemetryEnvelopeV1;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.runtime.checkpoint.OperatorSubtaskState;
import org.apache.flink.streaming.api.operators.KeyedProcessOperator;
import org.apache.flink.streaming.runtime.streamrecord.StreamRecord;
import org.apache.flink.streaming.util.KeyedOneInputStreamOperatorTestHarness;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TelemetryDataQualityFunctionTest {

    private static final Instant T0 = Instant.parse("2026-07-19T12:00:00Z");

    @Test
    void monotonicGoodTelemetryProducesNoIncident() throws Exception {
        try (Harness harness = harness()) {
            harness.open();

            harness.processElement(telemetry("EVENT-1", 4, 1).toByteArray(), T0.toEpochMilli());
            harness.processElement(telemetry("EVENT-2", 4, 2).toByteArray(), T0.plusSeconds(1).toEpochMilli());

            assertTrue(events(harness).isEmpty());
        }
    }

    @Test
    void sequenceGapDuplicateConflictOutOfOrderAndEpochRegressionAreExplicit() throws Exception {
        try (Harness harness = harness()) {
            harness.open();
            TelemetryEnvelopeV1 high = telemetry("EVENT-12", 4, 12);

            harness.processElement(telemetry("EVENT-10", 4, 10).toByteArray(), T0.toEpochMilli());
            harness.processElement(high.toByteArray(), T0.plusSeconds(1).toEpochMilli());
            harness.processElement(high.toByteArray(), T0.plusSeconds(1).toEpochMilli());
            harness.processElement(high.toBuilder().setEventId("EVENT-CONFLICT").build().toByteArray(),
                    T0.plusSeconds(1).toEpochMilli());
            harness.processElement(telemetry("EVENT-11", 4, 11).toByteArray(),
                    T0.plusSeconds(2).toEpochMilli());
            harness.processElement(telemetry("EVENT-OLD-EPOCH", 3, 13).toByteArray(),
                    T0.plusSeconds(3).toEpochMilli());

            assertEquals(
                    List.of(
                            "SOURCE_SEQUENCE_GAP:WARNING",
                            "SOURCE_SEQUENCE_DUPLICATE:INFO",
                            "SOURCE_SEQUENCE_CONFLICT:CRITICAL",
                            "SOURCE_SEQUENCE_OUT_OF_ORDER:WARNING",
                            "SOURCE_EPOCH_REGRESSION:ERROR"),
                    events(harness).stream()
                            .map(event -> event.getIssueCode() + ":" + event.getSeverity().name())
                            .toList());
            assertTrue(events(harness).stream().allMatch(event ->
                    event.getHeadersOrDefault("stage", "").equals("telemetry-data-quality")));
        }
    }

    @Test
    void clockDriftAndEveryNonGoodPointKeepTypedScopeAndSeverity() throws Exception {
        TelemetryEnvelopeV1 envelope = telemetry("EVENT-QUALITY", 7, 1).toBuilder()
                .setIngestTimeMs(T0.plusSeconds(601).toEpochMilli())
                .clearPoints()
                .addPoints(point("flow", "BAD"))
                .addPoints(point("baume", "UNCERTAIN"))
                .addPoints(point("pressure", "SUBSTITUTED"))
                .build();
        try (Harness harness = harness()) {
            harness.open();

            harness.processElement(envelope.toByteArray(), T0.toEpochMilli());

            List<DataQualityEventV1> events = events(harness);
            assertEquals(
                    List.of(
                            "CLOCK_DRIFT:ERROR:",
                            "POINT_QUALITY_BAD:ERROR:flow",
                            "POINT_QUALITY_UNCERTAIN:WARNING:baume",
                            "POINT_QUALITY_SUBSTITUTED:WARNING:pressure"),
                    events.stream()
                            .map(event -> event.getIssueCode() + ":" + event.getSeverity().name()
                                    + ":" + event.getPropertyId())
                            .toList());
            assertTrue(events.stream().allMatch(event ->
                    event.getTenantId().equals("TENANT-A")
                            && event.getPlantId().equals("PLANT-01")
                            && event.getLineId().equals("LINE-01")
                            && event.getDeviceId().equals("DEVICE-01")
                            && event.getDetectedAtMs() == envelope.getIngestTimeMs()));
        }
    }

    @Test
    void checkpointRestoreRetainsSequenceHighWaterMark() throws Exception {
        OperatorSubtaskState snapshot;
        try (Harness first = harness()) {
            first.open();
            first.processElement(telemetry("EVENT-5", 9, 5).toByteArray(), T0.toEpochMilli());
            snapshot = first.snapshot(1, T0.toEpochMilli());
        }

        try (Harness restored = harness()) {
            restored.initializeState(snapshot);
            restored.open();
            restored.processElement(telemetry("EVENT-7", 9, 7).toByteArray(),
                    T0.plusSeconds(1).toEpochMilli());

            assertEquals(List.of("SOURCE_SEQUENCE_GAP"),
                    events(restored).stream().map(DataQualityEventV1::getIssueCode).toList());
        }
    }

    @Test
    void unsignedSequenceCanCrossSignedBoundaryWithoutFalseGap() throws Exception {
        try (Harness harness = harness()) {
            harness.open();
            harness.processElement(telemetry("EVENT-MAX-SIGNED", 11, Long.MAX_VALUE).toByteArray(),
                    T0.toEpochMilli());
            harness.processElement(telemetry("EVENT-MIN-SIGNED", 11, Long.MIN_VALUE).toByteArray(),
                    T0.plusSeconds(1).toEpochMilli());

            assertTrue(events(harness).isEmpty());
        }
    }

    @Test
    void deterministicFingerprintIgnoresProtobufMapInsertionOrder() throws Exception {
        TelemetryEnvelopeV1 first = telemetry("EVENT-MAP", 12, 1).toBuilder()
                .putHeaders("zeta", "2")
                .putHeaders("alpha", "1")
                .build();
        TelemetryEnvelopeV1 replay = first.toBuilder()
                .clearHeaders()
                .putHeaders("alpha", "1")
                .putHeaders("zeta", "2")
                .build();
        try (Harness harness = harness()) {
            harness.open();
            harness.processElement(first.toByteArray(), T0.toEpochMilli());
            harness.processElement(replay.toByteArray(), T0.plusSeconds(1).toEpochMilli());

            assertEquals(List.of("SOURCE_SEQUENCE_DUPLICATE"),
                    events(harness).stream().map(DataQualityEventV1::getIssueCode).toList());
        }
    }

    @Test
    void contractRejectedQualityCodeDoesNotCreateASecondUncontrolledIssueCode() throws Exception {
        TelemetryEnvelopeV1 invalidPoint = telemetry("EVENT-INVALID-QUALITY", 13, 1).toBuilder()
                .setPoints(0, point("flow", "NOT_CONTROLLED"))
                .build();
        try (Harness harness = harness()) {
            harness.open();
            harness.processElement(invalidPoint.toByteArray(), T0.toEpochMilli());

            assertTrue(events(harness).isEmpty());
        }
    }

    @Test
    void nonAuthoritativeSequenceMetadataDoesNotCreateFalseSequenceIncidents() throws Exception {
        TelemetryEnvelopeV1 first = telemetry("EVENT-UNTRUSTED-1", 0, 0).toBuilder()
                .setSequenceOrigin(SequenceOrigin.EXPORTER)
                .build();
        TelemetryEnvelopeV1 second = first.toBuilder()
                .setEventId("EVENT-UNTRUSTED-2")
                .setMessageId("MESSAGE-EVENT-UNTRUSTED-2")
                .build();
        try (Harness harness = harness()) {
            harness.open();
            harness.processElement(first.toByteArray(), T0.toEpochMilli());
            harness.processElement(second.toByteArray(), T0.plusSeconds(1).toEpochMilli());

            assertTrue(events(harness).isEmpty());
        }
    }

    private static Harness harness() throws Exception {
        return new Harness(new KeyedProcessOperator<>(
                new TelemetryDataQualityFunction(Duration.ofMinutes(5), Duration.ofDays(7))));
    }

    private static TelemetryEnvelopeV1 telemetry(String eventId, long epoch, long sequence) {
        Instant eventTime = T0.plusSeconds(Math.max(0, sequence > 100 ? 0 : sequence));
        return TelemetryEnvelopeV1.newBuilder()
                .setEventId(eventId)
                .setMessageId("MESSAGE-" + eventId)
                .setTenantId("TENANT-A")
                .setPlantId("PLANT-01")
                .setLineId("LINE-01")
                .setGatewayId("GATEWAY-01")
                .setProductId("PRODUCT-01")
                .setDeviceId("DEVICE-01")
                .setEventTimeMs(eventTime.toEpochMilli())
                .setIngestTimeMs(eventTime.plusMillis(50).toEpochMilli())
                .setSourceEpoch(epoch)
                .setSequence(sequence)
                .setSequenceOrigin(SequenceOrigin.GATEWAY)
                .addPoints(point("flow", "GOOD"))
                .build();
    }

    private static PointValue point(String propertyId, String qualityCode) {
        return PointValue.newBuilder()
                .setPropertyId(propertyId)
                .setDoubleValue(3.5)
                .setUnit("m3/h")
                .setQualityCode(qualityCode)
                .setSampleTimeMs(T0.toEpochMilli())
                .setCalibrationVersion("CAL-1")
                .build();
    }

    private static List<DataQualityEventV1> events(Harness harness) {
        return harness.getOutput().stream()
                .filter(StreamRecord.class::isInstance)
                .map(StreamRecord.class::cast)
                .map(StreamRecord::getValue)
                .filter(byte[].class::isInstance)
                .map(byte[].class::cast)
                .map(TelemetryDataQualityFunctionTest::parse)
                .toList();
    }

    private static DataQualityEventV1 parse(byte[] bytes) {
        try {
            return DataQualityEventV1.parseFrom(bytes);
        } catch (com.google.protobuf.InvalidProtocolBufferException error) {
            throw new IllegalStateException(error);
        }
    }

    private static final class Harness extends KeyedOneInputStreamOperatorTestHarness<
            String, byte[], byte[]> {

        private Harness(KeyedProcessOperator<String, byte[], byte[]> operator) throws Exception {
            super(
                    operator,
                    TelemetryDataQualityFunction::sourceKey,
                    TypeInformation.of(String.class),
                    1,
                    1,
                    0);
        }
    }
}
