package com.mapletct.ftmes.bpi.stream;

import com.mapletct.ftmes.bpi.contract.v1.BoundaryConditionOperatorV1;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryEvidenceClassV1;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryEvidenceConditionV1;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryRulePublicationV1;
import com.mapletct.ftmes.bpi.contract.v1.BoundarySignalBindingV1;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryType;
import com.mapletct.ftmes.bpi.contract.v1.PointValue;
import com.mapletct.ftmes.bpi.contract.v1.ProductionContextEventV1;
import com.mapletct.ftmes.bpi.contract.v1.TelemetryEnvelopeV1;
import org.apache.flink.streaming.api.operators.co.CoBroadcastWithNonKeyedOperator;
import org.apache.flink.streaming.runtime.streamrecord.StreamRecord;
import org.apache.flink.streaming.util.BroadcastOperatorTestHarness;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoundaryRuleRoutingBroadcastHarnessTest {

    private static final Instant T0 = Instant.parse("2026-07-12T08:00:00Z");

    @Test
    void indexedRouteEmitsOnlyMatchingScopedRule() throws Exception {
        try (BroadcastOperatorTestHarness<byte[], byte[], BoundaryStreamInput> harness = harness()) {
            harness.open();
            BoundaryRulePublicationV1 tenantA = publication("TENANT-A", "RULE-A", true, "sha:a");
            BoundaryRulePublicationV1 tenantB = publication("TENANT-B", "RULE-A", true, "sha:b");
            harness.processBroadcastElement(tenantA.toByteArray(), T0.toEpochMilli());
            harness.processBroadcastElement(tenantB.toByteArray(), T0.toEpochMilli());

            harness.processElement(
                    ContextualTelemetryPointCodec.encode(contextual("TENANT-A")),
                    T0.plusSeconds(1).toEpochMilli());

            List<BoundaryStreamInput> outputs = harness.getOutput().stream()
                    .filter(StreamRecord.class::isInstance)
                    .map(StreamRecord.class::cast)
                    .map(StreamRecord::getValue)
                    .filter(BoundaryStreamInput.class::isInstance)
                    .map(BoundaryStreamInput.class::cast)
                    .toList();
            assertEquals(1, outputs.size());
            assertEquals("TENANT-A|PLANT-01|LINE-01|RULE-A|1", outputs.get(0).ruleRef().key());
            assertTrue(outputs.get(0).ruleRef().scoped());
        }
    }

    @Test
    void oneBoundPointFansOutOnlyToTheTwoIndexedRules() throws Exception {
        try (BroadcastOperatorTestHarness<byte[], byte[], BoundaryStreamInput> harness = harness()) {
            harness.open();
            BoundaryRulePublicationV1 first = publication("TENANT-A", "RULE-A", true, "sha:a");
            BoundaryRulePublicationV1 second = publication("TENANT-A", "RULE-B", true, "sha:b");
            BoundaryRulePublicationV1 unrelated = publication("TENANT-A", "RULE-C", true, "sha:c")
                    .toBuilder()
                    .setSignalBindings(0, BoundarySignalBindingV1.newBuilder()
                            .setDeviceId("DEVICE-2")
                            .setPropertyId("flow")
                            .setSignal("feed.flow")
                            .setExpectedUnit("m3/h"))
                    .build();
            harness.processBroadcastElement(first.toByteArray(), T0.toEpochMilli());
            harness.processBroadcastElement(second.toByteArray(), T0.toEpochMilli());
            harness.processBroadcastElement(unrelated.toByteArray(), T0.toEpochMilli());

            harness.processElement(
                    ContextualTelemetryPointCodec.encode(contextual("TENANT-A")),
                    T0.plusSeconds(1).toEpochMilli());

            assertEquals(
                    List.of("RULE-A", "RULE-B"),
                    harness.getOutput().stream()
                            .filter(StreamRecord.class::isInstance)
                            .map(StreamRecord.class::cast)
                            .map(StreamRecord::getValue)
                            .filter(BoundaryStreamInput.class::isInstance)
                            .map(BoundaryStreamInput.class::cast)
                            .map(input -> input.ruleRef().ruleCode())
                            .sorted()
                            .toList());
        }
    }

    @Test
    void typedInactivePublicationRemovesRouteButRetainsVersionIdentity() throws Exception {
        try (BroadcastOperatorTestHarness<byte[], byte[], BoundaryStreamInput> harness = harness()) {
            harness.open();
            BoundaryRulePublicationV1 active = publication("TENANT-A", "RULE-A", true, "sha:a");
            BoundaryRulePublicationV1 inactive = active.toBuilder()
                    .setEventId("RULE-OFF")
                    .setActive(false)
                    .setPublishedAtMs(T0.plusSeconds(5).toEpochMilli())
                    .build();
            harness.processBroadcastElement(active.toByteArray(), T0.toEpochMilli());
            harness.processBroadcastElement(inactive.toByteArray(), T0.plusSeconds(5).toEpochMilli());

            harness.processElement(
                    ContextualTelemetryPointCodec.encode(contextual("TENANT-A")),
                    T0.plusSeconds(6).toEpochMilli());

            assertTrue(harness.getOutput().stream().noneMatch(StreamRecord.class::isInstance));
            String route = "TENANT-A|PLANT-01|LINE-01|DEVICE-1|flow";
            assertNull(harness.getBroadcastState(BoundaryRuleRoutingBroadcastFunction.ROUTES).get(route));
            assertEquals(
                    false,
                    BoundaryRulePublicationV1.parseFrom(harness.getBroadcastState(
                            BoundaryRuleRoutingBroadcastFunction.PUBLICATIONS).get(
                            "TENANT-A|PLANT-01|LINE-01|RULE-A|1")).getActive());
        }
    }

    @Test
    void sameScopedVersionWithChangedChecksumIsRejectedWithoutReplacingRoute() throws Exception {
        try (BroadcastOperatorTestHarness<byte[], byte[], BoundaryStreamInput> harness = harness()) {
            harness.open();
            BoundaryRulePublicationV1 first = publication("TENANT-A", "RULE-A", true, "sha:a");
            BoundaryRulePublicationV1 conflict = first.toBuilder()
                    .setEventId("RULE-CONFLICT")
                    .setChecksum("sha:changed")
                    .build();
            harness.processBroadcastElement(first.toByteArray(), T0.toEpochMilli());
            harness.processBroadcastElement(conflict.toByteArray(), T0.plusSeconds(1).toEpochMilli());

            assertEquals(
                    "RULE_VERSION_CONFLICT",
                    harness.getSideOutput(BoundaryRuleRoutingBroadcastFunction.ISSUES)
                            .peek().getValue().code());
        }
    }

    private static BroadcastOperatorTestHarness<byte[], byte[], BoundaryStreamInput> harness()
            throws Exception {
        return new BroadcastOperatorTestHarness<>(
                new CoBroadcastWithNonKeyedOperator<>(
                        new BoundaryRuleRoutingBroadcastFunction(),
                        List.of(
                                BoundaryRuleRoutingBroadcastFunction.PUBLICATIONS,
                                BoundaryRuleRoutingBroadcastFunction.ROUTES)),
                1,
                1,
                0);
    }

    static BoundaryRulePublicationV1 publication(
            String tenant,
            String ruleCode,
            boolean active,
            String checksum) {
        return BoundaryRulePublicationV1.newBuilder()
                .setEventId("RULE-" + tenant)
                .setTenantId(tenant)
                .setPlantId("PLANT-01")
                .setLineId("LINE-01")
                .setLocalityGroup("FEED")
                .setTopologyCode("TOPO-1")
                .setTopologyVersion("1")
                .setRuleCode(ruleCode)
                .setRuleVersion("1")
                .setBoundaryType(BoundaryType.START)
                .setQuorumMinimum(1)
                .setMinimumConfidence(1)
                .setMaxCompositePenalty(0)
                .setAllowedLatenessMs(30_000)
                .setWatermarkDelayMs(5_000)
                .setEvaluationTimeoutMs(120_000)
                .addConditions(BoundaryEvidenceConditionV1.newBuilder()
                        .setSignal("feed.flow")
                        .setOperator(BoundaryConditionOperatorV1.GREATER_THAN)
                        .setThresholdDecimal("2")
                        .setHoldForMs(0)
                        .setMaxSilenceMs(30_000)
                        .setClassification(BoundaryEvidenceClassV1.QUORUM)
                        .setWeight(100))
                .addSignalBindings(BoundarySignalBindingV1.newBuilder()
                        .setDeviceId("DEVICE-1")
                        .setPropertyId("flow")
                        .setSignal("feed.flow")
                        .setExpectedUnit("m3/h"))
                .setActive(active)
                .setPublishedAtMs(T0.toEpochMilli())
                .setChecksum(checksum)
                .build();
    }

    private static ContextualTelemetryPoint contextual(String tenant) {
        TelemetryEnvelopeV1 envelope = TelemetryEnvelopeV1.newBuilder()
                .setEventId("TEL-" + tenant)
                .setMessageId("MSG-" + tenant)
                .setTenantId(tenant)
                .setPlantId("PLANT-01")
                .setLineId("LINE-01")
                .setGatewayId("GW-1")
                .setProductId("PRODUCT-1")
                .setDeviceId("DEVICE-1")
                .setEventTimeMs(T0.plusSeconds(1).toEpochMilli())
                .setIngestTimeMs(T0.plusSeconds(2).toEpochMilli())
                .addPoints(PointValue.newBuilder()
                        .setPropertyId("flow")
                        .setDoubleValue(3)
                        .setUnit("m3/h")
                        .setQualityCode("GOOD")
                        .setSampleTimeMs(T0.plusSeconds(1).toEpochMilli()))
                .build();
        ProductionContextEventV1 context = ProductionContextEventV1.newBuilder()
                .setEventId("CTX-" + tenant)
                .setTenantId(tenant)
                .setPlantId("PLANT-01")
                .setLineId("LINE-01")
                .setOrderId("MO-1")
                .setEffectiveFromMs(T0.toEpochMilli())
                .setContextRevision(1)
                .setActive(true)
                .build();
        return new ContextualTelemetryPoint(new TelemetryPointEvent(envelope, 0), context);
    }
}
