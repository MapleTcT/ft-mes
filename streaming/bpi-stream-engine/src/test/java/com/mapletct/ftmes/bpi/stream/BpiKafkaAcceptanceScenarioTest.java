package com.mapletct.ftmes.bpi.stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mapletct.ftmes.bpi.contract.validation.BpiContractValidator;
import com.mapletct.ftmes.bpi.contract.v1.BatchCandidateV1;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryType;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryRuleApplicationStatusV1;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryRuleApplicationV1;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryRuleRuntimeReadinessStatusV1;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryRuleRuntimeReadinessV1;
import com.mapletct.ftmes.bpi.contract.v1.DataQualityEventV1;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BpiKafkaAcceptanceScenarioTest {

    private static final String MARKER = "ADP_E2E_20260712_001";
    private static final Instant T0 = Instant.parse("2026-07-12T08:00:00Z");

    @TempDir
    Path tempDir;

    @Test
    void scenarioProducesValidRuleContextAndTelemetryContracts() {
        BpiKafkaAcceptanceScenario.Scenario scenario = BpiKafkaAcceptanceScenario.create(MARKER, T0);

        PublishedBoundaryPlan plan = BoundaryRulePublicationMapper.map(scenario.publication());
        assertEquals(scenario.ruleCode(), plan.rule().ruleCode());
        assertTrue(new ProductionContextTimeline().apply(scenario.context())
                .resolve(scenario.tenantId(), scenario.plantId(), scenario.lineId(), T0)
                .isPresent());
        scenario.telemetry().forEach(envelope -> {
            var validation = BpiContractValidator.validate(envelope);
            assertTrue(validation.isEnvelopeAccepted());
            assertEquals(1, validation.getAcceptedPointIndexes().size());
            assertTrue(validation.getPointRejections().isEmpty());
        });
        assertFalse(scenario.inactivePublication(T0.plusSeconds(10)).getActive());
        assertEquals(scenario.ruleKey(), BoundaryRulePublicationSemantics.key(scenario.publication()));
    }

    @Test
    void candidateAndIssueFiltersAreScopedToTheReplayMarker() {
        BpiKafkaAcceptanceScenario.Scenario scenario = BpiKafkaAcceptanceScenario.create(MARKER, T0);
        BatchCandidateV1 candidate = BatchCandidateV1.newBuilder()
                .setTenantId(scenario.tenantId())
                .setPlantId(scenario.plantId())
                .setLineId(scenario.lineId())
                .setRuleCode(scenario.ruleCode())
                .setContextOrderId(scenario.orderId())
                .setBoundaryType(BoundaryType.START)
                .build();
        assertTrue(BpiKafkaAcceptanceReplay.matchesCandidate(candidate, scenario));
        assertFalse(BpiKafkaAcceptanceReplay.matchesCandidate(
                candidate.toBuilder().setLineId("OTHER").build(), scenario));

        DataQualityEventV1 issue = DataQualityEventV1.newBuilder()
                .setSourceEventId(MARKER + "-TELEMETRY-1")
                .build();
        assertTrue(BpiKafkaAcceptanceReplay.matchesIssue(issue, scenario));
        assertFalse(BpiKafkaAcceptanceReplay.matchesIssue(
                issue.toBuilder().setSourceEventId("OTHER").build(), scenario));
    }

    @Test
    void replayConfigurationIsFailClosedAndGeneratesSafeConsumerIdentity() {
        BpiKafkaAcceptanceReplayConfig config = BpiKafkaAcceptanceReplayConfig.fromEnvironment(Map.of(
                "BPI_KAFKA_BOOTSTRAP_SERVERS", "kafka-1:19092",
                "BPI_REPLAY_MARKER", MARKER,
                "BPI_REPLAY_REPORT", "/tmp/replay.json"));

        assertEquals("ft-mes-bpi-acceptance-" + MARKER, config.consumerGroup());
        assertEquals(Path.of("/tmp/replay.json"), config.reportPath());
        assertEquals("TENANT-E2E", config.tenantId());
        assertEquals("LINE-" + MARKER, config.lineId());
        assertThrows(IllegalArgumentException.class, () ->
                BpiKafkaAcceptanceReplayConfig.fromEnvironment(Map.of(
                        "BPI_REPLAY_MARKER", MARKER,
                        "BPI_REPLAY_REPORT", "/tmp/replay.json")));
        assertThrows(IllegalArgumentException.class, () ->
                BpiKafkaAcceptanceScenario.create("bad marker", T0));
    }

    @Test
    void configuredScenarioUsesTheRealAcceptanceScopeWithoutChangingDefaults() {
        BpiKafkaAcceptanceReplayConfig config = BpiKafkaAcceptanceReplayConfig.fromEnvironment(Map.ofEntries(
                Map.entry("BPI_KAFKA_BOOTSTRAP_SERVERS", "kafka-1:19092"),
                Map.entry("BPI_REPLAY_MARKER", MARKER),
                Map.entry("BPI_REPLAY_REPORT", "/tmp/replay.json"),
                Map.entry("BPI_REPLAY_TENANT_ID", "1000"),
                Map.entry("BPI_REPLAY_PLANT_ID", "PLANT-01"),
                Map.entry("BPI_REPLAY_LINE_ID", "LINE-IRB-01"),
                Map.entry("BPI_REPLAY_TOPOLOGY_CODE", "TOPO-IRB-01"),
                Map.entry("BPI_REPLAY_TOPOLOGY_VERSION", "7"),
                Map.entry("BPI_REPLAY_RULE_CODE", "RULE-IRB-01"),
                Map.entry("BPI_REPLAY_RULE_VERSION", "3"),
                Map.entry("BPI_REPLAY_ORDER_ID", "MO-IRB-01"),
                Map.entry("BPI_REPLAY_PRODUCT_ID", "PRODUCT-IRB-01"),
                Map.entry("BPI_REPLAY_DEVICE_ID", "DEVICE-IRB-01"),
                Map.entry("BPI_REPLAY_POINT_CATALOG_SOURCE_INSTANCE", "BPI-JOINT-" + MARKER)));

        BpiKafkaAcceptanceScenario.Scenario scenario =
                BpiKafkaAcceptanceScenario.create(config, T0);

        assertEquals("1000", scenario.tenantId());
        assertEquals("PLANT-01", scenario.plantId());
        assertEquals("LINE-IRB-01", scenario.lineId());
        assertEquals("TOPO-IRB-01", scenario.publication().getTopologyCode());
        assertEquals("7", scenario.publication().getTopologyVersion());
        assertEquals("RULE-IRB-01", scenario.ruleCode());
        assertEquals("3", scenario.ruleVersion());
        assertEquals("MO-IRB-01", scenario.orderId());
        assertEquals("PRODUCT-IRB-01", scenario.pointCatalog().getPoints(0).getProductId());
        assertEquals("DEVICE-IRB-01", scenario.deviceId());
        assertEquals("BPI-JOINT-" + MARKER, scenario.pointCatalog().getSourceInstance());
        assertTrue(scenario.ruleKey().endsWith("|RULE-IRB-01|3"));
    }

    @Test
    void replayReportIsValidMachineReadableJson() throws Exception {
        Path report = tempDir.resolve("replay.json").toAbsolutePath();
        BpiKafkaAcceptanceReplayConfig config = BpiKafkaAcceptanceReplayConfig.fromEnvironment(Map.of(
                "BPI_KAFKA_BOOTSTRAP_SERVERS", "kafka-1:19092",
                "BPI_REPLAY_MARKER", MARKER,
                "BPI_REPLAY_REPORT", report.toString()));
        BpiKafkaAcceptanceScenario.Scenario scenario = BpiKafkaAcceptanceScenario.create(MARKER, T0);
        BatchCandidateV1 candidate = BatchCandidateV1.newBuilder()
                .setEventId("CANDIDATE-1")
                .setCandidateKey("00000000-0000-5000-8000-000000000001")
                .addEvidenceEventIds(MARKER + "-TELEMETRY-1")
                .build();
        BpiKafkaAcceptanceReplay.ReplayResult result = new BpiKafkaAcceptanceReplay.ReplayResult(
                java.util.List.of(new BpiKafkaAcceptanceReplay.InputOffset(
                        config.telemetryTopic(), 1, 42, MARKER + "-TELEMETRY-1")),
                candidate,
                new BpiKafkaAcceptanceReplay.OutputOffset(config.candidateTopic(), 2, 84),
                1,
                java.util.List.of(),
                new BpiKafkaAcceptanceReplay.LocatedReadiness(
                        BoundaryRuleRuntimeReadinessV1.newBuilder()
                                .setEventId(MARKER + "-RULE-READY")
                                .setPublicationEventId(MARKER + "-RULE-ACTIVE")
                                .setPointCatalogEventId(MARKER + "-POINT-CATALOG")
                                .setStatus(BoundaryRuleRuntimeReadinessStatusV1.READY)
                                .build(),
                        new BpiKafkaAcceptanceReplay.OutputOffset(
                                config.ruleRuntimeReadinessTopic(), 0, 83)),
                new BpiKafkaAcceptanceReplay.InputOffset(
                        config.ruleTopic(), 0, 85, MARKER + "-RULE-INACTIVE"),
                new BpiKafkaAcceptanceReplay.LocatedApplication(
                        BoundaryRuleApplicationV1.newBuilder()
                                .setEventId(MARKER + "-RULE-INACTIVE-APPLIED")
                                .setPublicationEventId(MARKER + "-RULE-INACTIVE")
                                .setDeploymentId("test-deployment")
                                .setStatus(BoundaryRuleApplicationStatusV1.APPLIED)
                                .build(),
                        new BpiKafkaAcceptanceReplay.OutputOffset(
                                config.ruleApplicationTopic(), 0, 86)));

        BpiKafkaAcceptanceReplay.writeReport(config, scenario, result, "PASS", null);

        JsonNode json = new ObjectMapper().readTree(report.toFile());
        assertEquals("PASS", json.path("status").asText());
        assertEquals(MARKER, json.path("marker").asText());
        assertEquals(42, json.path("inputs").get(0).path("offset").asLong());
        assertEquals(84, json.path("candidate").path("offset").asLong());
        assertEquals("READY", json.path("activeReadiness").path("status").asText());
        assertEquals(85, json.path("cleanup").path("publication").path("offset").asLong());
        assertEquals(86, json.path("cleanup").path("application").path("offset").asLong());
        assertEquals("APPLIED", json.path("cleanup").path("application").path("status").asText());
        assertTrue(json.path("error").isNull());
    }
}
