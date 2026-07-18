package com.mapletct.ftmes.bpi.stream;

import com.mapletct.ftmes.bpi.contract.v1.BoundaryConditionOperatorV1;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryEvidenceClassV1;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryEvidenceConditionV1;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryRulePublicationV1;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryRuleRuntimeReadinessStatusV1;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryRuleRuntimeReadinessV1;
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
            catalog(harness, readyCatalog("TENANT-A", true, "CAL-1", T0));
            rule(harness, tenantA);
            rule(harness, tenantB);

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
                            .setExpectedUnit("m3/h")
                            .setProductId("PRODUCT-1")
                            .setCalibrationVersion("CAL-1"))
                    .build();
            catalog(harness, readyCatalog("TENANT-A", true, "CAL-1", T0));
            rule(harness, first);
            rule(harness, second);
            rule(harness, unrelated);

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
            catalog(harness, readyCatalog("TENANT-A", true, "CAL-1", T0));
            rule(harness, active);
            rule(harness, inactive);

            harness.processElement(
                    ContextualTelemetryPointCodec.encode(contextual("TENANT-A")),
                    T0.plusSeconds(6).toEpochMilli());

            assertTrue(harness.getOutput().stream().noneMatch(StreamRecord.class::isInstance));
            String route = "TENANT-A|PLANT-01|LINE-01|PRODUCT-1|DEVICE-1|flow";
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
            catalog(harness, readyCatalog("TENANT-A", true, "CAL-1", T0));
            rule(harness, first);
            rule(harness, conflict);

            assertEquals(
                    "RULE_VERSION_CONFLICT",
                    harness.getSideOutput(BoundaryRuleRoutingBroadcastFunction.ISSUES)
                            .peek().getValue().code());
        }
    }

    @Test
    void missingCatalogBlocksAnOtherwiseMatchingRule() throws Exception {
        try (BroadcastOperatorTestHarness<byte[], byte[], BoundaryStreamInput> harness = harness()) {
            harness.open();
            rule(harness, publication("TENANT-A", "RULE-A", true, "sha:a"));

            harness.processElement(
                    ContextualTelemetryPointCodec.encode(contextual("TENANT-A")),
                    T0.plusSeconds(1).toEpochMilli());

            assertTrue(harness.getOutput().stream().noneMatch(StreamRecord.class::isInstance));
            assertEquals(
                    "POINT_CATALOG_RUNTIME_MISSING",
                    harness.getSideOutput(BoundaryRuleRoutingBroadcastFunction.ISSUES).peek().getValue().code());
            BoundaryRuleRuntimeReadinessV1 receipt = runtimeReadiness(harness).get(0);
            assertEquals(BoundaryRuleRuntimeReadinessStatusV1.DEGRADED, receipt.getStatus());
            assertEquals("POINT_CATALOG_RUNTIME_MISSING", receipt.getReasonCode());
            assertEquals("", receipt.getPointCatalogEventId());
        }
    }

    @Test
    void readinessReceiptTracksCatalogRevisionAndSuppressesExactReplay() throws Exception {
        try (BroadcastOperatorTestHarness<byte[], byte[], BoundaryStreamInput> harness = harness()) {
            harness.open();
            rule(harness, publication("TENANT-A", "RULE-A", true, "sha:a"));
            catalog(harness, readyCatalog("TENANT-A", true, "CAL-1", T0.plusSeconds(10)));
            catalog(harness, readyCatalog("TENANT-A", true, "CAL-1", T0.plusSeconds(20)));
            catalog(harness, readyCatalog("TENANT-A", true, "CAL-1", T0.plusSeconds(20)));
            catalog(harness, readyCatalog("TENANT-A", false, "CAL-1", T0.plusSeconds(30)));
            catalog(harness, readyCatalog("TENANT-A", true, "CAL-2", T0.plusSeconds(40)));

            List<BoundaryRuleRuntimeReadinessV1> receipts = runtimeReadiness(harness);
            assertEquals(
                    List.of(
                            BoundaryRuleRuntimeReadinessStatusV1.DEGRADED,
                            BoundaryRuleRuntimeReadinessStatusV1.READY,
                            BoundaryRuleRuntimeReadinessStatusV1.READY,
                            BoundaryRuleRuntimeReadinessStatusV1.DEGRADED,
                            BoundaryRuleRuntimeReadinessStatusV1.DEGRADED),
                    receipts.stream().map(BoundaryRuleRuntimeReadinessV1::getStatus).toList());
            assertEquals(
                    List.of(
                            "POINT_CATALOG_RUNTIME_MISSING",
                            "",
                            "",
                            "POINT_DEVICE_NOT_ACTIVE",
                            "POINT_CALIBRATION_VERSION_MISMATCH"),
                    receipts.stream().map(BoundaryRuleRuntimeReadinessV1::getReasonCode).toList());
            assertEquals(
                    "CATALOG-TENANT-A-" + T0.plusSeconds(40).toEpochMilli(),
                    receipts.get(4).getPointCatalogEventId());
            assertEquals(
                    List.of(
                            "CATALOG-TENANT-A-" + T0.plusSeconds(10).toEpochMilli(),
                            "CATALOG-TENANT-A-" + T0.plusSeconds(20).toEpochMilli()),
                    receipts.subList(1, 3).stream()
                            .map(BoundaryRuleRuntimeReadinessV1::getPointCatalogEventId)
                            .toList());
        }
    }

    @Test
    void nonLeaderSubtaskMaintainsBroadcastStateWithoutDuplicatingGlobalOutputs() throws Exception {
        try (BroadcastOperatorTestHarness<byte[], byte[], BoundaryStreamInput> harness = harness(3, 1)) {
            harness.open();
            catalog(harness, readyCatalog("TENANT-A", true, "CAL-1", T0));
            rule(harness, publication("TENANT-A", "RULE-A", true, "sha:a"));

            assertTrue(runtimeReadiness(harness).isEmpty());
            assertTrue(runtimeRuleUpdates(harness).isEmpty());
            assertTrue(harness.getBroadcastState(BoundaryRuleRoutingBroadcastFunction.PUBLICATIONS)
                    .contains("TENANT-A|PLANT-01|LINE-01|RULE-A|1"));
            assertTrue(harness.getBroadcastState(BoundaryRuleRoutingBroadcastFunction.ROUTES)
                    .contains("TENANT-A|PLANT-01|LINE-01|PRODUCT-1|DEVICE-1|flow"));
        }
    }

    @Test
    void newerCatalogDowngradeStopsRoutingAndOlderReadySnapshotCannotRestoreIt() throws Exception {
        try (BroadcastOperatorTestHarness<byte[], byte[], BoundaryStreamInput> harness = harness()) {
            harness.open();
            catalog(harness, readyCatalog("TENANT-A", true, "CAL-1", T0));
            rule(harness, publication("TENANT-A", "RULE-A", true, "sha:a"));
            catalog(harness, readyCatalog("TENANT-A", false, "CAL-1", T0.plusSeconds(10)));
            catalog(harness, readyCatalog("TENANT-A", true, "CAL-1", T0.plusSeconds(5)));

            harness.processElement(
                    ContextualTelemetryPointCodec.encode(contextual("TENANT-A")),
                    T0.plusSeconds(11).toEpochMilli());

            assertTrue(harness.getOutput().stream().noneMatch(StreamRecord.class::isInstance));
            assertEquals(
                    List.of("POINT_DEVICE_NOT_ACTIVE", "POINT_CATALOG_OUT_OF_ORDER"),
                    harness.getSideOutput(BoundaryRuleRoutingBroadcastFunction.ISSUES).stream()
                            .map(StreamRecord::getValue)
                            .map(BoundaryRoutingIssue::code)
                            .toList());
            assertEquals(
                    List.of(BoundaryRuleUpdate.Operation.UPSERT, BoundaryRuleUpdate.Operation.DELETE),
                    runtimeRuleUpdates(harness).stream().map(BoundaryRuleUpdate::operation).toList());
        }
    }

    @Test
    void newerReadyCatalogRestoresRuleOnlyAfterExactCalibrationMatchesAgain() throws Exception {
        try (BroadcastOperatorTestHarness<byte[], byte[], BoundaryStreamInput> harness = harness()) {
            harness.open();
            catalog(harness, readyCatalog("TENANT-A", true, "CAL-1", T0));
            rule(harness, publication("TENANT-A", "RULE-A", true, "sha:a"));
            catalog(harness, readyCatalog("TENANT-A", true, "CAL-2", T0.plusSeconds(10)));
            catalog(harness, readyCatalog("TENANT-A", true, "CAL-1", T0.plusSeconds(20)));

            harness.processElement(
                    ContextualTelemetryPointCodec.encode(contextual("TENANT-A")),
                    T0.plusSeconds(21).toEpochMilli());

            assertEquals(
                    List.of(
                            BoundaryRuleUpdate.Operation.UPSERT,
                            BoundaryRuleUpdate.Operation.DELETE,
                            BoundaryRuleUpdate.Operation.UPSERT),
                    runtimeRuleUpdates(harness).stream().map(BoundaryRuleUpdate::operation).toList());
            assertEquals(1, harness.getOutput().stream().filter(StreamRecord.class::isInstance).count());
        }
    }

    @Test
    void restoredLegacyPublicationWithoutProductAndCalibrationFailsClosedWithoutCrashingCatalogFlow()
            throws Exception {
        try (BroadcastOperatorTestHarness<byte[], byte[], BoundaryStreamInput> harness = harness()) {
            harness.open();
            BoundaryRulePublicationV1 legacy = publication("TENANT-A", "RULE-LEGACY", true, "sha:legacy")
                    .toBuilder()
                    .setSignalBindings(0, BoundarySignalBindingV1.newBuilder()
                            .setDeviceId("DEVICE-1")
                            .setPropertyId("flow")
                            .setSignal("feed.flow")
                            .setExpectedUnit("m3/h"))
                    .build();
            harness.getBroadcastState(BoundaryRuleRoutingBroadcastFunction.PUBLICATIONS)
                    .put("TENANT-A|PLANT-01|LINE-01|RULE-LEGACY|1", legacy.toByteArray());

            catalog(harness, readyCatalog("TENANT-A", true, "CAL-1", T0));

            assertEquals(
                    "RULE_PUBLICATION_RUNTIME_REJECTED",
                    harness.getSideOutput(BoundaryRuleRoutingBroadcastFunction.ISSUES)
                            .peek().getValue().code());
            List<BoundaryRuleUpdate> updates = runtimeRuleUpdates(harness);
            assertEquals(1, updates.size());
            assertEquals(BoundaryRuleUpdate.Operation.DELETE, updates.get(0).operation());
            assertEquals(
                    "TENANT-A|PLANT-01|LINE-01|RULE-LEGACY|1",
                    updates.get(0).ruleRef().key());
        }
    }

    private static BroadcastOperatorTestHarness<byte[], byte[], BoundaryStreamInput> harness()
            throws Exception {
        return harness(1, 0);
    }

    private static BroadcastOperatorTestHarness<byte[], byte[], BoundaryStreamInput> harness(
            int parallelism,
            int subtaskIndex) throws Exception {
        return new BroadcastOperatorTestHarness<>(
                new CoBroadcastWithNonKeyedOperator<>(
                        new BoundaryRuleRoutingBroadcastFunction(),
                        List.of(
                                BoundaryRuleRoutingBroadcastFunction.PUBLICATIONS,
                                BoundaryRuleRoutingBroadcastFunction.ROUTES,
                                BoundaryRuleRoutingBroadcastFunction.POINT_CATALOGS,
                                BoundaryRuleRoutingBroadcastFunction.POINTS,
                                BoundaryRuleRoutingBroadcastFunction.RUNTIME_RULE_STATUS)),
                parallelism,
                parallelism,
                subtaskIndex);
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
                        .setProductId("PRODUCT-1")
                        .setDeviceId("DEVICE-1")
                        .setPropertyId("flow")
                        .setSignal("feed.flow")
                        .setExpectedUnit("m3/h")
                        .setCalibrationVersion("CAL-1"))
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
                .setSourceEpoch(1)
                .setSequence(1)
                .setSequenceOrigin(SequenceOrigin.GATEWAY)
                .setEventTimeMs(T0.plusSeconds(1).toEpochMilli())
                .setIngestTimeMs(T0.plusSeconds(2).toEpochMilli())
                .addPoints(PointValue.newBuilder()
                        .setPropertyId("flow")
                        .setDoubleValue(3)
                        .setUnit("m3/h")
                        .setQualityCode("GOOD")
                        .setCalibrationVersion("CAL-1")
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

    private static PointCatalogSnapshotV1 readyCatalog(
            String tenant,
            boolean active,
            String calibrationVersion,
            Instant observedAt) {
        return PointCatalogSnapshotV1.newBuilder()
                .setEventId("CATALOG-" + tenant + "-" + observedAt.toEpochMilli())
                .setSource("JETLINKS")
                .setSourceInstance("TEST")
                .setSourceRevision("sha256:" + observedAt.toEpochMilli())
                .setTenantId(tenant)
                .setPlantId("PLANT-01")
                .setLineId("LINE-01")
                .setObservedAtMs(observedAt.toEpochMilli())
                .setReason("Runtime readiness test")
                .addPoints(PointCatalogPointV1.newBuilder()
                        .setProductId("PRODUCT-1")
                        .setDeviceId("DEVICE-1")
                        .setPropertyId("flow")
                        .setUnit("m3/h")
                        .setDataType("double")
                        .setDeviceState(active
                                ? PointDeviceStateV1.POINT_DEVICE_ACTIVE
                                : PointDeviceStateV1.POINT_DEVICE_INACTIVE)
                        .setRegistered(true)
                        .setPropertyPresent(true)
                        .setCalibrationVersion(calibrationVersion)
                        .setCalibrationStatus(PointCalibrationStatusV1.POINT_CALIBRATION_VERIFIED)
                        .setSourceSequenceEnabled(true))
                .build();
    }

    private static void rule(
            BroadcastOperatorTestHarness<byte[], byte[], BoundaryStreamInput> harness,
            BoundaryRulePublicationV1 publication) throws Exception {
        harness.processBroadcastElement(
                BoundaryRoutingControlCodec.rule(publication.toByteArray()), publication.getPublishedAtMs());
    }

    private static void catalog(
            BroadcastOperatorTestHarness<byte[], byte[], BoundaryStreamInput> harness,
            PointCatalogSnapshotV1 snapshot) throws Exception {
        harness.processBroadcastElement(
                BoundaryRoutingControlCodec.pointCatalog(snapshot.toByteArray()), snapshot.getObservedAtMs());
    }

    private static List<BoundaryRuleUpdate> runtimeRuleUpdates(
            BroadcastOperatorTestHarness<byte[], byte[], BoundaryStreamInput> harness) {
        var output = harness.getSideOutput(BoundaryRuleRoutingBroadcastFunction.RULE_UPDATES);
        if (output == null) {
            return List.of();
        }
        return output.stream()
                .map(StreamRecord::getValue)
                .map(BoundaryRuleUpdateCodec::decode)
                .toList();
    }

    private static List<BoundaryRuleRuntimeReadinessV1> runtimeReadiness(
            BroadcastOperatorTestHarness<byte[], byte[], BoundaryStreamInput> harness) {
        var output = harness.getSideOutput(BoundaryRuleRoutingBroadcastFunction.RUNTIME_READINESS);
        if (output == null) return List.of();
        return output.stream()
                .map(StreamRecord::getValue)
                .map(bytes -> {
                    try {
                        return BoundaryRuleRuntimeReadinessV1.parseFrom(bytes);
                    } catch (com.google.protobuf.InvalidProtocolBufferException error) {
                        throw new IllegalStateException(error);
                    }
                })
                .toList();
    }
}
