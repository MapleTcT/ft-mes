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
import com.mapletct.ftmes.bpi.rules.BoundaryKind;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoundarySignalRouterTest {

    private static final Instant T0 = Instant.parse("2026-07-12T08:00:00Z");

    @Test
    void publicationMapsToVersionedRuleAndDeterministicBindings() {
        PublishedBoundaryPlan plan = BoundaryRulePublicationMapper.map(publication(BoundaryType.START, true));

        assertEquals(BoundaryKind.START, plan.rule().boundaryKind());
        assertEquals(Duration.ofSeconds(30), plan.rule().timing().allowedLateness());
        assertEquals(Duration.ofSeconds(5), plan.rule().timing().watermarkDelay());
        assertEquals(Duration.ofMinutes(2), plan.rule().timing().evaluationTimeout());
        assertEquals("feed.flow", plan.bindings().get("DEVICE-1|flow").getSignal());
        assertEquals(BoundaryRuleUpdate.Operation.UPSERT, plan.ruleUpdate().operation());
    }

    @Test
    void telemetryRoutesOnlyBoundPointsUsingPointEventTimeAndActiveOrderContext() {
        PublishedBoundaryPlan plan = BoundaryRulePublicationMapper.map(publication(BoundaryType.START, true));
        TelemetryEnvelopeV1 telemetry = telemetry(
                pointBool("order", true, "", T0.plusSeconds(1)),
                pointDouble("flow", 3.5, "m3/h", T0.plusSeconds(2)),
                pointDouble("temperature", 85, "C", T0.plusSeconds(2)));

        BoundaryRoutingResult result = BoundarySignalRouter.route(plan, context("MO-1", ""), telemetry);

        assertTrue(result.issues().isEmpty());
        assertEquals(2, result.inputs().size());
        assertEquals(List.of("order.active", "feed.flow"), result.inputs().stream()
                .map(item -> item.observation().signal())
                .toList());
        assertEquals(T0.plusSeconds(2), result.inputs().get(1).observation().eventTime());
        assertEquals(
                "TENANT-A|PLANT-01|LINE-01|FEED|START|TENANT-A|PLANT-01|LINE-01|BOUNDARY-01|1",
                result.inputs().get(0).keyedLocality());
    }

    @Test
    void unitMismatchRejectsOnlyTheAffectedPoint() {
        PublishedBoundaryPlan plan = BoundaryRulePublicationMapper.map(publication(BoundaryType.START, true));

        BoundaryRoutingResult result = BoundarySignalRouter.route(
                plan,
                context("MO-1", ""),
                telemetry(
                        pointBool("order", true, "", T0.plusSeconds(1)),
                        pointDouble("flow", 3.5, "kg/h", T0.plusSeconds(2))));

        assertEquals(1, result.inputs().size());
        assertEquals("order.active", result.inputs().get(0).observation().signal());
        assertEquals(List.of("UNIT_MISMATCH"), result.issues().stream()
                .map(BoundaryRoutingIssue::code)
                .toList());
    }

    @Test
    void endRuleRequiresTypedBatchIdentity() {
        PublishedBoundaryPlan plan = BoundaryRulePublicationMapper.map(publication(BoundaryType.END, true));

        BoundaryRoutingResult missing = BoundarySignalRouter.route(
                plan, context("MO-1", ""), telemetry(pointBool("order", true, "", T0.plusSeconds(1))));
        BoundaryRoutingResult present = BoundarySignalRouter.route(
                plan, context("MO-1", "BATCH-1"), telemetry(pointBool("order", true, "", T0.plusSeconds(1))));

        assertTrue(missing.inputs().isEmpty());
        assertEquals("POINT_REJECTED", missing.issues().get(0).code());
        assertEquals("BATCH-1", present.inputs().get(0).context().batchId());
    }

    @Test
    void inactiveRuleAndOutOfRangeContextCannotProduceInputs() {
        PublishedBoundaryPlan inactive = BoundaryRulePublicationMapper.map(publication(BoundaryType.START, false));
        PublishedBoundaryPlan active = BoundaryRulePublicationMapper.map(publication(BoundaryType.START, true));
        TelemetryEnvelopeV1 telemetry = telemetry(pointBool("order", true, "", T0.plusSeconds(1)));
        ProductionContextEventV1 expired = context("MO-1", "").toBuilder()
                .setEffectiveToMs(T0.plusSeconds(1).toEpochMilli())
                .build();

        assertTrue(BoundarySignalRouter.route(inactive, context("MO-1", ""), telemetry).inputs().isEmpty());
        BoundaryRoutingResult expiredResult = BoundarySignalRouter.route(active, expired, telemetry);
        assertTrue(expiredResult.inputs().isEmpty());
        assertEquals("CONTEXT_NOT_EFFECTIVE", expiredResult.issues().get(0).code());
    }

    @Test
    void publicationRejectsUnboundConditionsAndDuplicateDeviceProperties() {
        BoundaryRulePublicationV1 base = publication(BoundaryType.START, true);
        BoundaryRulePublicationV1 missing = base.toBuilder()
                .clearSignalBindings()
                .addSignalBindings(base.getSignalBindings(0))
                .build();
        BoundaryRulePublicationV1 duplicate = base.toBuilder()
                .addSignalBindings(base.getSignalBindings(0).toBuilder().setSignal("feed.flow"))
                .build();
        BoundaryRulePublicationV1 duplicateSignal = base.toBuilder()
                .addSignalBindings(base.getSignalBindings(1).toBuilder().setPropertyId("flow_backup"))
                .build();
        BoundaryRulePublicationV1 missingNumericUnit = base.toBuilder()
                .setSignalBindings(1, base.getSignalBindings(1).toBuilder().clearExpectedUnit())
                .build();

        assertThrows(IllegalArgumentException.class, () -> BoundaryRulePublicationMapper.map(missing));
        assertThrows(IllegalArgumentException.class, () -> BoundaryRulePublicationMapper.map(duplicate));
        assertThrows(IllegalArgumentException.class, () -> BoundaryRulePublicationMapper.map(duplicateSignal));
        assertThrows(IllegalArgumentException.class, () -> BoundaryRulePublicationMapper.map(missingNumericUnit));
    }

    private static BoundaryRulePublicationV1 publication(BoundaryType boundaryType, boolean active) {
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
                .setBoundaryType(boundaryType)
                .setQuorumMinimum(1)
                .setMinimumConfidence(1)
                .setMaxCompositePenalty(0)
                .setAllowedLatenessMs(30_000)
                .setWatermarkDelayMs(5_000)
                .setEvaluationTimeoutMs(120_000)
                .addConditions(condition(
                        "order.active", BoundaryConditionOperatorV1.EQUALS_TRUE,
                        "", 0, BoundaryEvidenceClassV1.REQUIRED, 50))
                .addConditions(condition(
                        "feed.flow", BoundaryConditionOperatorV1.GREATER_THAN,
                        "2", 10_000, BoundaryEvidenceClassV1.QUORUM, 50))
                .addSignalBindings(binding("order", "order.active", ""))
                .addSignalBindings(binding("flow", "feed.flow", "m3/h"))
                .setActive(active)
                .setPublishedAtMs(T0.toEpochMilli())
                .setChecksum("sha256:rule-1")
                .build();
    }

    private static BoundaryEvidenceConditionV1 condition(
            String signal,
            BoundaryConditionOperatorV1 operator,
            String threshold,
            long holdForMs,
            BoundaryEvidenceClassV1 classification,
            int weight) {
        return BoundaryEvidenceConditionV1.newBuilder()
                .setSignal(signal)
                .setOperator(operator)
                .setThresholdDecimal(threshold)
                .setHoldForMs(holdForMs)
                .setMaxSilenceMs(30_000)
                .setClassification(classification)
                .setWeight(weight)
                .build();
    }

    private static BoundarySignalBindingV1 binding(String property, String signal, String unit) {
        return BoundarySignalBindingV1.newBuilder()
                .setDeviceId("DEVICE-1")
                .setPropertyId(property)
                .setSignal(signal)
                .setExpectedUnit(unit)
                .build();
    }

    private static ProductionContextEventV1 context(String orderId, String batchId) {
        return ProductionContextEventV1.newBuilder()
                .setEventId("CONTEXT-1")
                .setTenantId("TENANT-A")
                .setPlantId("PLANT-01")
                .setLineId("LINE-01")
                .setOrderId(orderId)
                .setBatchId(batchId)
                .setEffectiveFromMs(T0.minusSeconds(1).toEpochMilli())
                .setContextRevision(1)
                .setActive(true)
                .build();
    }

    private static TelemetryEnvelopeV1 telemetry(PointValue... points) {
        return TelemetryEnvelopeV1.newBuilder()
                .setEventId("TELEMETRY-1")
                .setTenantId("TENANT-A")
                .setPlantId("PLANT-01")
                .setLineId("LINE-01")
                .setGatewayId("GATEWAY-1")
                .setProductId("PRODUCT-1")
                .setDeviceId("DEVICE-1")
                .setEventTimeMs(T0.toEpochMilli())
                .setIngestTimeMs(T0.plusSeconds(1).toEpochMilli())
                .addAllPoints(List.of(points))
                .build();
    }

    private static PointValue pointDouble(String property, double value, String unit, Instant time) {
        return PointValue.newBuilder()
                .setPropertyId(property)
                .setDoubleValue(value)
                .setUnit(unit)
                .setQualityCode("GOOD")
                .setSampleTimeMs(time.toEpochMilli())
                .build();
    }

    private static PointValue pointBool(String property, boolean value, String unit, Instant time) {
        return PointValue.newBuilder()
                .setPropertyId(property)
                .setBoolValue(value)
                .setUnit(unit)
                .setQualityCode("GOOD")
                .setSampleTimeMs(time.toEpochMilli())
                .build();
    }
}
