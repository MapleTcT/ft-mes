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
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.streaming.runtime.streamrecord.StreamRecord;
import org.apache.flink.streaming.util.OneInputStreamOperatorTestHarness;
import org.apache.flink.streaming.util.ProcessFunctionTestHarnesses;
import org.apache.flink.util.OutputTag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KafkaDecodeFunctionsHarnessTest {

    private static final long EVENT_TIME_MS = 1_720_000_000_000L;

    @Test
    void telemetryEmitsAcceptedPointsAndQuarantinesRejectedPoints() throws Exception {
        TelemetryEnvelopeV1 envelope = validTelemetry().toBuilder()
                .addPoints(validPoint())
                .addPoints(validPoint().toBuilder()
                        .setPropertyId("baume")
                        .setQualityCode("NOT_A_CONTROLLED_CODE"))
                .build();

        try (OneInputStreamOperatorTestHarness<byte[], byte[]> harness =
                     ProcessFunctionTestHarnesses.forProcessFunction(new TelemetryKafkaDecodeFunction())) {
            harness.open();
            harness.processElement(ingress("telemetry.v1", 2, 31L, envelope.toByteArray()), EVENT_TIME_MS);

            List<byte[]> output = mainOutput(harness);
            assertEquals(1, output.size());
            TelemetryPointEvent accepted = TelemetryPointEventCodec.decode(output.get(0));
            assertEquals(envelope, accepted.envelope());
            assertEquals(0, accepted.pointIndex());

            KafkaDecodeIssue issue = onlyIssue(harness, TelemetryKafkaDecodeFunction.ISSUES);
            assertIssue(issue, "TELEMETRY_POINT_REJECTED", "telemetry.v1", 2, 31L, "EVENT-1");
            assertTrue(issue.detail().contains("point[1]"));
            assertTrue(issue.detail().contains("UNKNOWN_QUALITY"));
        }
    }

    @Test
    void telemetryRejectsBrokenIngressTombstoneProtobufAndEnvelope() throws Exception {
        assertRejected(
                new TelemetryKafkaDecodeFunction(), TelemetryKafkaDecodeFunction.ISSUES,
                new byte[]{1, 2, 3}, "KAFKA_RECORD_REJECTED", "", -1, -1L, "");
        assertRejected(
                new TelemetryKafkaDecodeFunction(), TelemetryKafkaDecodeFunction.ISSUES,
                ingress("telemetry.v1", 1, 10L, null),
                "TELEMETRY_TOMBSTONE_REJECTED", "telemetry.v1", 1, 10L, "");
        assertRejected(
                new TelemetryKafkaDecodeFunction(), TelemetryKafkaDecodeFunction.ISSUES,
                ingress("telemetry.v1", 1, 11L, new byte[]{0x0A}),
                "TELEMETRY_PROTOBUF_REJECTED", "telemetry.v1", 1, 11L, "");
        assertRejected(
                new TelemetryKafkaDecodeFunction(), TelemetryKafkaDecodeFunction.ISSUES,
                ingress("telemetry.v1", 1, 12L,
                        validTelemetry().toBuilder().clearDeviceId().build().toByteArray()),
                "TELEMETRY_ENVELOPE_REJECTED", "telemetry.v1", 1, 12L, "EVENT-1");
    }

    @Test
    void productionContextPassesValidPayloadAndRejectsInvalidInputs() throws Exception {
        ProductionContextEventV1 event = validContext();
        assertAccepted(
                new ProductionContextKafkaDecodeFunction(), ProductionContextKafkaDecodeFunction.ISSUES,
                ingress("context.v1", 3, 20L, event.toByteArray()), event.toByteArray());

        assertRejected(
                new ProductionContextKafkaDecodeFunction(), ProductionContextKafkaDecodeFunction.ISSUES,
                new byte[]{1}, "KAFKA_RECORD_REJECTED", "", -1, -1L, "");
        assertRejected(
                new ProductionContextKafkaDecodeFunction(), ProductionContextKafkaDecodeFunction.ISSUES,
                ingress("context.v1", 3, 21L, null),
                "CONTEXT_TOMBSTONE_REJECTED", "context.v1", 3, 21L, "");
        assertRejected(
                new ProductionContextKafkaDecodeFunction(), ProductionContextKafkaDecodeFunction.ISSUES,
                ingress("context.v1", 3, 22L, new byte[]{0x0A}),
                "CONTEXT_PROTOBUF_REJECTED", "context.v1", 3, 22L, "");
        assertRejected(
                new ProductionContextKafkaDecodeFunction(), ProductionContextKafkaDecodeFunction.ISSUES,
                ingress("context.v1", 3, 23L,
                        event.toBuilder().clearLineId().build().toByteArray()),
                "CONTEXT_CONTRACT_REJECTED", "context.v1", 3, 23L, "CONTEXT-1");
    }

    @Test
    void boundaryRulePassesValidPayloadAndRejectsInvalidInputs() throws Exception {
        BoundaryRulePublicationV1 publication = validRulePublication();
        assertAccepted(
                new BoundaryRuleKafkaDecodeFunction(), BoundaryRuleKafkaDecodeFunction.ISSUES,
                ingress("boundary-rules.v1", 4, 40L, publication.toByteArray()), publication.toByteArray());

        assertRejected(
                new BoundaryRuleKafkaDecodeFunction(), BoundaryRuleKafkaDecodeFunction.ISSUES,
                new byte[]{1}, "KAFKA_RECORD_REJECTED", "", -1, -1L, "");
        assertRejected(
                new BoundaryRuleKafkaDecodeFunction(), BoundaryRuleKafkaDecodeFunction.ISSUES,
                ingress("boundary-rules.v1", 4, 41L, null),
                "RULE_TOMBSTONE_REJECTED", "boundary-rules.v1", 4, 41L, "");
        assertRejected(
                new BoundaryRuleKafkaDecodeFunction(), BoundaryRuleKafkaDecodeFunction.ISSUES,
                ingress("boundary-rules.v1", 4, 42L, new byte[]{0x0A}),
                "RULE_PROTOBUF_REJECTED", "boundary-rules.v1", 4, 42L, "");
        assertRejected(
                new BoundaryRuleKafkaDecodeFunction(), BoundaryRuleKafkaDecodeFunction.ISSUES,
                ingress("boundary-rules.v1", 4, 43L,
                        publication.toBuilder().clearChecksum().build().toByteArray()),
                "RULE_CONTRACT_REJECTED", "boundary-rules.v1", 4, 43L, "RULE-EVENT-1");
    }

    @Test
    void pointCatalogPassesValidPayloadAndRejectsInvalidInputs() throws Exception {
        PointCatalogSnapshotV1 snapshot = validPointCatalog();
        assertAccepted(
                new PointCatalogKafkaDecodeFunction(), PointCatalogKafkaDecodeFunction.ISSUES,
                ingress("point-catalog.v1", 5, 50L, snapshot.toByteArray()), snapshot.toByteArray());

        assertRejected(
                new PointCatalogKafkaDecodeFunction(), PointCatalogKafkaDecodeFunction.ISSUES,
                ingress("point-catalog.v1", 5, 51L, null),
                "POINT_CATALOG_TOMBSTONE_REJECTED", "point-catalog.v1", 5, 51L, "");
        assertRejected(
                new PointCatalogKafkaDecodeFunction(), PointCatalogKafkaDecodeFunction.ISSUES,
                ingress("point-catalog.v1", 5, 52L, new byte[]{0x0A}),
                "POINT_CATALOG_PROTOBUF_REJECTED", "point-catalog.v1", 5, 52L, "");
        assertRejected(
                new PointCatalogKafkaDecodeFunction(), PointCatalogKafkaDecodeFunction.ISSUES,
                ingress("point-catalog.v1", 5, 53L,
                        snapshot.toBuilder().clearSourceRevision().build().toByteArray()),
                "POINT_CATALOG_CONTRACT_REJECTED", "point-catalog.v1", 5, 53L, "CATALOG-1");
    }

    private static void assertAccepted(
            ProcessFunction<byte[], byte[]> function,
            OutputTag<KafkaDecodeIssue> issueTag,
            byte[] input,
            byte[] expected) throws Exception {
        try (OneInputStreamOperatorTestHarness<byte[], byte[]> harness =
                     ProcessFunctionTestHarnesses.forProcessFunction(function)) {
            harness.open();
            harness.processElement(input, EVENT_TIME_MS);

            List<byte[]> output = mainOutput(harness);
            assertEquals(1, output.size());
            assertArrayEquals(expected, output.get(0));
            assertTrue(sideOutput(harness, issueTag).isEmpty());
        }
    }

    private static void assertRejected(
            ProcessFunction<byte[], byte[]> function,
            OutputTag<KafkaDecodeIssue> issueTag,
            byte[] input,
            String code,
            String topic,
            int partition,
            long offset,
            String sourceEventId) throws Exception {
        try (OneInputStreamOperatorTestHarness<byte[], byte[]> harness =
                     ProcessFunctionTestHarnesses.forProcessFunction(function)) {
            harness.open();
            harness.processElement(input, EVENT_TIME_MS);

            assertTrue(mainOutput(harness).isEmpty());
            assertIssue(onlyIssue(harness, issueTag), code, topic, partition, offset, sourceEventId);
        }
    }

    private static void assertIssue(
            KafkaDecodeIssue issue,
            String code,
            String topic,
            int partition,
            long offset,
            String sourceEventId) {
        assertEquals(code, issue.code());
        assertEquals(topic, issue.topic());
        assertEquals(partition, issue.partition());
        assertEquals(offset, issue.offset());
        assertEquals(sourceEventId, issue.sourceEventId());
    }

    private static KafkaDecodeIssue onlyIssue(
            OneInputStreamOperatorTestHarness<byte[], byte[]> harness,
            OutputTag<KafkaDecodeIssue> tag) {
        List<KafkaDecodeIssue> issues = sideOutput(harness, tag);
        assertEquals(1, issues.size());
        return issues.get(0);
    }

    private static List<byte[]> mainOutput(OneInputStreamOperatorTestHarness<byte[], byte[]> harness) {
        return harness.getOutput().stream()
                .filter(StreamRecord.class::isInstance)
                .map(StreamRecord.class::cast)
                .map(record -> (byte[]) record.getValue())
                .toList();
    }

    private static List<KafkaDecodeIssue> sideOutput(
            OneInputStreamOperatorTestHarness<byte[], byte[]> harness,
            OutputTag<KafkaDecodeIssue> tag) {
        ConcurrentLinkedQueue<StreamRecord<KafkaDecodeIssue>> output = harness.getSideOutput(tag);
        if (output == null) {
            return List.of();
        }
        return output.stream().map(StreamRecord::getValue).toList();
    }

    private static byte[] ingress(String topic, int partition, long offset, byte[] value) {
        return KafkaIngressRecordCodec.encode(new KafkaIngressRecord(
                topic, partition, offset, EVENT_TIME_MS, new byte[]{7}, value));
    }

    private static TelemetryEnvelopeV1 validTelemetry() {
        return TelemetryEnvelopeV1.newBuilder()
                .setEventId("EVENT-1")
                .setMessageId("MQTT-1")
                .setTenantId("TENANT-A")
                .setPlantId("PLANT-01")
                .setLineId("LINE-01")
                .setGatewayId("GATEWAY-01")
                .setProductId("PRODUCT-01")
                .setDeviceId("DEVICE-01")
                .setEventTimeMs(EVENT_TIME_MS)
                .setIngestTimeMs(EVENT_TIME_MS + 50)
                .setSequence(42L)
                .setSourceEpoch(3L)
                .setSequenceOrigin(SequenceOrigin.GATEWAY)
                .build();
    }

    private static PointValue validPoint() {
        return PointValue.newBuilder()
                .setPropertyId("flow_instant")
                .setDoubleValue(18.5d)
                .setUnit("m3/h")
                .setQualityCode("GOOD")
                .setSampleTimeMs(EVENT_TIME_MS)
                .build();
    }

    private static ProductionContextEventV1 validContext() {
        return ProductionContextEventV1.newBuilder()
                .setEventId("CONTEXT-1")
                .setTenantId("TENANT-A")
                .setPlantId("PLANT-01")
                .setLineId("LINE-01")
                .setOrderId("MO-1")
                .setEffectiveFromMs(EVENT_TIME_MS)
                .setContextRevision(1)
                .setActive(true)
                .build();
    }

    private static BoundaryRulePublicationV1 validRulePublication() {
        return BoundaryRulePublicationV1.newBuilder()
                .setEventId("RULE-EVENT-1")
                .setTenantId("TENANT-A")
                .setPlantId("PLANT-01")
                .setLineId("LINE-01")
                .setLocalityGroup("FEED")
                .setTopologyCode("TOPO-1")
                .setTopologyVersion("7")
                .setRuleCode("BOUNDARY-01")
                .setRuleVersion("1")
                .setBoundaryType(BoundaryType.START)
                .setQuorumMinimum(1)
                .setMinimumConfidence(1)
                .setMaxCompositePenalty(0)
                .setAllowedLatenessMs(30_000)
                .setWatermarkDelayMs(5_000)
                .setEvaluationTimeoutMs(120_000)
                .addConditions(BoundaryEvidenceConditionV1.newBuilder()
                        .setSignal("order.active")
                        .setOperator(BoundaryConditionOperatorV1.EQUALS_TRUE)
                        .setHoldForMs(0)
                        .setMaxSilenceMs(30_000)
                        .setClassification(BoundaryEvidenceClassV1.REQUIRED)
                        .setWeight(50))
                .addConditions(BoundaryEvidenceConditionV1.newBuilder()
                        .setSignal("feed.flow")
                        .setOperator(BoundaryConditionOperatorV1.GREATER_THAN)
                        .setThresholdDecimal("2")
                        .setHoldForMs(10_000)
                        .setMaxSilenceMs(30_000)
                        .setClassification(BoundaryEvidenceClassV1.QUORUM)
                        .setWeight(50))
                .addSignalBindings(BoundarySignalBindingV1.newBuilder()
                        .setProductId("PRODUCT-1")
                        .setDeviceId("DEVICE-1")
                        .setPropertyId("order")
                        .setSignal("order.active")
                        .setCalibrationVersion("CAL-1"))
                .addSignalBindings(BoundarySignalBindingV1.newBuilder()
                        .setProductId("PRODUCT-1")
                        .setDeviceId("DEVICE-1")
                        .setPropertyId("flow")
                        .setSignal("feed.flow")
                        .setExpectedUnit("m3/h")
                        .setCalibrationVersion("CAL-1"))
                .setActive(true)
                .setPublishedAtMs(EVENT_TIME_MS)
                .setChecksum("sha256:rule-1")
                .build();
    }

    private static PointCatalogSnapshotV1 validPointCatalog() {
        return PointCatalogSnapshotV1.newBuilder()
                .setEventId("CATALOG-1")
                .setSource("JETLINKS")
                .setSourceInstance("TEST")
                .setSourceRevision("sha256:catalog-1")
                .setTenantId("TENANT-A")
                .setPlantId("PLANT-01")
                .setLineId("LINE-01")
                .setObservedAtMs(EVENT_TIME_MS)
                .setReason("Kafka decode test")
                .addPoints(PointCatalogPointV1.newBuilder()
                        .setProductId("PRODUCT-1")
                        .setDeviceId("DEVICE-1")
                        .setPropertyId("flow")
                        .setUnit("m3/h")
                        .setDataType("double")
                        .setDeviceState(PointDeviceStateV1.POINT_DEVICE_ACTIVE)
                        .setRegistered(true)
                        .setPropertyPresent(true)
                        .setCalibrationVersion("CAL-1")
                        .setCalibrationStatus(PointCalibrationStatusV1.POINT_CALIBRATION_VERIFIED)
                        .setSourceSequenceEnabled(true))
                .build();
    }
}
