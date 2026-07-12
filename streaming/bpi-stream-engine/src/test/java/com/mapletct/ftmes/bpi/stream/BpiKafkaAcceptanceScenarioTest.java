package com.mapletct.ftmes.bpi.stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mapletct.ftmes.bpi.contract.validation.BpiContractValidator;
import com.mapletct.ftmes.bpi.contract.v1.BatchCandidateV1;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryType;
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
        assertThrows(IllegalArgumentException.class, () ->
                BpiKafkaAcceptanceReplayConfig.fromEnvironment(Map.of(
                        "BPI_REPLAY_MARKER", MARKER,
                        "BPI_REPLAY_REPORT", "/tmp/replay.json")));
        assertThrows(IllegalArgumentException.class, () ->
                BpiKafkaAcceptanceScenario.create("bad marker", T0));
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
                new BpiKafkaAcceptanceReplay.InputOffset(
                        config.ruleTopic(), 0, 85, MARKER + "-RULE-INACTIVE"));

        BpiKafkaAcceptanceReplay.writeReport(config, scenario, result, "PASS", null);

        JsonNode json = new ObjectMapper().readTree(report.toFile());
        assertEquals("PASS", json.path("status").asText());
        assertEquals(MARKER, json.path("marker").asText());
        assertEquals(42, json.path("inputs").get(0).path("offset").asLong());
        assertEquals(84, json.path("candidate").path("offset").asLong());
        assertEquals(85, json.path("cleanup").path("offset").asLong());
        assertTrue(json.path("error").isNull());
    }
}
