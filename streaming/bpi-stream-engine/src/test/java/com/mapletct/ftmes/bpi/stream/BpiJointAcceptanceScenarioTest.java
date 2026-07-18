package com.mapletct.ftmes.bpi.stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mapletct.ftmes.bpi.contract.validation.BpiContractValidator;
import com.mapletct.ftmes.bpi.contract.v1.BatchCandidateV1;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryType;
import com.mapletct.ftmes.bpi.contract.v1.DataQualityEventV1;
import com.mapletct.ftmes.bpi.contract.v1.ProductionContextEventV1;
import com.mapletct.ftmes.bpi.contract.v1.SequenceOrigin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BpiJointAcceptanceScenarioTest {

    private static final String MARKER = "ADP_E2E_20260714_001";
    private static final Instant T0 = Instant.parse("2026-07-14T08:00:00Z");

    @TempDir
    Path tempDir;

    @Test
    void scenarioProducesMatchingCatalogContextAndTelemetryForAnExistingPublishedRule() {
        BpiJointAcceptanceReplayConfig config = config(tempDir.resolve("joint.json"));
        BpiJointAcceptanceScenario.Scenario scenario = BpiJointAcceptanceScenario.create(config, T0);

        assertEquals("1000|PLANT-01|LINE-S07-01", scenario.contextKey());
        assertEquals(2, scenario.pointCatalog().getPointsCount());
        assertEquals("PRODUCT-SUGAR", scenario.pointCatalog().getPoints(0).getProductId());
        assertEquals("LOCALITY-S07-V2", scenario.pointCatalog().getPoints(0).getLocalityGroup());
        assertTrue(scenario.pointCatalog().getSourceRevision().matches("sha256:[0-9a-f]{64}"));
        assertEquals(
                "point-catalog-" + scenario.pointCatalog().getSourceRevision().substring("sha256:".length()),
                scenario.pointCatalog().getEventId());
        assertEquals("MO-" + MARKER, scenario.context().getOrderId());
        assertEquals(T0.getEpochSecond(), scenario.context().getContextRevision());
        assertEquals(3, scenario.telemetry().size());
        scenario.telemetry().forEach(envelope -> {
            assertEquals("PRODUCT-SUGAR", envelope.getProductId());
            assertEquals(SequenceOrigin.GATEWAY, envelope.getSequenceOrigin());
            var validation = BpiContractValidator.validate(envelope);
            assertTrue(validation.isEnvelopeAccepted());
            assertEquals(2, validation.getAcceptedPointIndexes().size());
            assertTrue(validation.getPointRejections().isEmpty());
        });
    }

    @Test
    void candidateAndIssueFiltersRequireTheConfiguredBrowserPublishedRuleScope() {
        BpiJointAcceptanceScenario.Scenario scenario = BpiJointAcceptanceScenario.create(
                config(tempDir.resolve("joint.json")), T0);
        BatchCandidateV1 candidate = BatchCandidateV1.newBuilder()
                .setTenantId(scenario.tenantId())
                .setPlantId(scenario.plantId())
                .setLineId(scenario.lineId())
                .setRuleCode(scenario.ruleCode())
                .setContextOrderId(scenario.orderId())
                .setBoundaryType(BoundaryType.START)
                .build();
        assertTrue(BpiJointAcceptanceReplay.matchesCandidate(candidate, scenario));
        assertFalse(BpiJointAcceptanceReplay.matchesCandidate(
                candidate.toBuilder().setRuleCode("OTHER").build(), scenario));

        DataQualityEventV1 issue = DataQualityEventV1.newBuilder()
                .setSourceEventId(MARKER + "-TELEMETRY-1")
                .build();
        assertTrue(BpiJointAcceptanceReplay.matchesIssue(issue, scenario));
        assertFalse(BpiJointAcceptanceReplay.matchesIssue(
                issue.toBuilder().setSourceEventId("OTHER").build(), scenario));
    }

    @Test
    void configurationFailsClosedWithoutExplicitScopeAndRuleIdentity() {
        BpiJointAcceptanceReplayConfig config = config(tempDir.resolve("joint.json"));
        assertEquals("ft-mes-bpi-joint-acceptance-" + MARKER, config.consumerGroup());
        assertThrows(IllegalArgumentException.class, () ->
                BpiJointAcceptanceReplayConfig.fromEnvironment(Map.of(
                        "BPI_KAFKA_BOOTSTRAP_SERVERS", "kafka-1:19092",
                        "BPI_JOINT_MARKER", MARKER,
                        "BPI_JOINT_REPORT", "/tmp/joint.json")));
    }

    @Test
    void mesOutboxModeRequiresAndMatchesTheRealWomOrder() {
        Map<String, String> environment = new HashMap<>(environment(tempDir.resolve("mes-outbox.json")));
        environment.put("BPI_JOINT_CONTEXT_SOURCE", "MES_OUTBOX");
        environment.put("BPI_JOINT_ORDER_ID", "ADP_E2E_WOM_REAL_CONTEXT_TASK_TN");
        BpiJointAcceptanceReplayConfig config = BpiJointAcceptanceReplayConfig.fromEnvironment(environment);
        BpiJointAcceptanceScenario.Scenario scenario = BpiJointAcceptanceScenario.create(config, T0);

        assertTrue(config.usesMesOutboxContext());
        assertEquals("ADP_E2E_WOM_REAL_CONTEXT_TASK_TN", scenario.orderId());
        assertTrue(BpiJointAcceptanceReplay.matchesContext(scenario.context(), scenario));
        assertFalse(BpiJointAcceptanceReplay.matchesContext(
                scenario.context().toBuilder().setOrderId("OTHER").build(), scenario));
        assertFalse(BpiJointAcceptanceReplay.matchesContext(
                scenario.context().toBuilder().setActive(false).build(), scenario));
    }

    @Test
    void mesOutboxModeRejectsAnImplicitSyntheticOrderId() {
        Map<String, String> environment = new HashMap<>(environment(tempDir.resolve("missing-order.json")));
        environment.put("BPI_JOINT_CONTEXT_SOURCE", "MES_OUTBOX");

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> BpiJointAcceptanceReplayConfig.fromEnvironment(environment));

        assertTrue(error.getMessage().contains("BPI_JOINT_ORDER_ID"));
    }

    @Test
    void latestInactiveContextSupersedesAnEarlierActiveRevision() {
        BpiJointAcceptanceScenario.Scenario scenario = BpiJointAcceptanceScenario.create(
                config(tempDir.resolve("latest-context.json")), T0);
        ProductionContextEventV1 inactive = scenario.closingContext();

        assertTrue(BpiJointAcceptanceReplay.isNewerContext(inactive, scenario.context()));
        assertFalse(BpiJointAcceptanceReplay.isNewerContext(scenario.context(), inactive));
    }

    @Test
    void syntheticScenarioProvidesAnInactiveClosingRevisionAfterTelemetry() {
        BpiJointAcceptanceReplayConfig config = config(tempDir.resolve("closed.json"));
        BpiJointAcceptanceScenario.Scenario scenario = BpiJointAcceptanceScenario.create(config, T0);

        ProductionContextEventV1 closing = scenario.closingContext();

        assertFalse(closing.getActive());
        assertEquals(scenario.context().getContextRevision() + 1, closing.getContextRevision());
        assertTrue(closing.getEffectiveFromMs()
                > scenario.telemetry().get(scenario.telemetry().size() - 1).getEventTimeMs());
        assertEquals("true", closing.getAttributesOrThrow("acceptance_cleanup"));
    }

    @Test
    void reportStatesThatTheRuleCameFromTheBrowserPublicationOutbox() throws Exception {
        Path report = tempDir.resolve("joint.json").toAbsolutePath();
        BpiJointAcceptanceReplayConfig config = config(report);
        BpiJointAcceptanceScenario.Scenario scenario = BpiJointAcceptanceScenario.create(config, T0);
        BatchCandidateV1 candidate = BatchCandidateV1.newBuilder()
                .setEventId("CANDIDATE-1")
                .setCandidateKey("00000000-0000-5000-8000-000000000001")
                .addEvidenceEventIds(MARKER + "-TELEMETRY-1")
                .build();
        BpiJointAcceptanceReplay.ReplayResult result = new BpiJointAcceptanceReplay.ReplayResult(
                java.util.List.of(new BpiJointAcceptanceReplay.InputOffset(
                        config.telemetryTopic(), 1, 42, MARKER + "-TELEMETRY-1")),
                candidate,
                new BpiJointAcceptanceReplay.OutputOffset(config.candidateTopic(), 2, 84),
                1,
                java.util.List.of(),
                new BpiJointAcceptanceReplay.RuntimeReadinessEvidence(
                        new BpiJointAcceptanceReplay.OutputOffset(
                                config.ruleRuntimeReadinessTopic(), 0, 21),
                        "READY-1",
                        "flink-test",
                        scenario.pointCatalog().getEventId(),
                        scenario.pointCatalog().getSourceRevision()));

        BpiJointAcceptanceReplay.writeReport(config, scenario, result, "PASS", null);

        JsonNode json = new ObjectMapper().readTree(report.toFile());
        assertEquals("PASS", json.path("status").asText());
        assertEquals("BPI_BROWSER_PUBLICATION_OUTBOX", json.path("ruleSource").asText());
        assertEquals("SYNTHETIC_REPLAY", json.path("contextSource").asText());
        assertEquals("RULE-S07-START@1.2.0", json.path("scope").path("rule").asText());
        assertEquals("iot.point-catalog.snapshot.v1", json.path("topics").path("pointCatalog").asText());
        assertEquals("READY-1", json.path("runtimeReadiness").path("eventId").asText());
        assertEquals(21, json.path("runtimeReadiness").path("offset").asLong());
        assertEquals(84, json.path("candidate").path("offset").asLong());
        assertTrue(json.path("error").isNull());
    }

    private BpiJointAcceptanceReplayConfig config(Path report) {
        return BpiJointAcceptanceReplayConfig.fromEnvironment(environment(report));
    }

    private Map<String, String> environment(Path report) {
        return Map.ofEntries(
                Map.entry("BPI_KAFKA_BOOTSTRAP_SERVERS", "kafka-1:19092"),
                Map.entry("BPI_JOINT_MARKER", MARKER),
                Map.entry("BPI_JOINT_TENANT_ID", "1000"),
                Map.entry("BPI_JOINT_PLANT_ID", "PLANT-01"),
                Map.entry("BPI_JOINT_LINE_ID", "LINE-S07-01"),
                Map.entry("BPI_JOINT_TOPOLOGY_CODE", "TOPO-S07"),
                Map.entry("BPI_JOINT_TOPOLOGY_VERSION", "3"),
                Map.entry("BPI_JOINT_RULE_CODE", "RULE-S07-START"),
                Map.entry("BPI_JOINT_RULE_VERSION", "1.2.0"),
                Map.entry("BPI_JOINT_LOCALITY_GROUP", "LOCALITY-S07-V2"),
                Map.entry("BPI_JOINT_PRODUCT_ID", "PRODUCT-SUGAR"),
                Map.entry("BPI_JOINT_DEVICE_ID", "DEVICE-S07-01"),
                Map.entry("BPI_JOINT_REPORT", report.toAbsolutePath().toString()));
    }
}
