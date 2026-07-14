package com.mapletct.ftmes.bpi.stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryRuleApplicationStatusV1;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryRuleApplicationV1;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryRulePublicationV1;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryType;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BpiRuleDeactivationReplayTest {

    @TempDir
    Path tempDir;

    @Test
    void typedInactivePublicationPreservesImmutableRuleIdentity() {
        BoundaryRulePublicationV1 active = publication();
        BoundaryRulePublicationV1 inactive = BpiRuleDeactivationReplay.inactive(
                active, "ADP_E2E_CLEANUP", Instant.ofEpochMilli(2_000));

        assertFalse(inactive.getActive());
        assertEquals(active.getEventId(), inactive.getEventId());
        assertTrue(BoundaryRulePublicationSemantics.equivalent(active, inactive));
        assertEquals("ADP_E2E_CLEANUP", inactive.getHeadersOrThrow("acceptance_cleanup"));
    }

    @Test
    void matchingRequiresTheFullConfiguredScopeAndVersion() {
        var config = config(tempDir.resolve("cleanup.json"));
        assertTrue(BpiRuleDeactivationReplay.matches(publication(), config));
        assertFalse(BpiRuleDeactivationReplay.matches(
                publication().toBuilder().setLineId("OTHER").build(), config));
    }

    @Test
    void configurationFailsClosedWithoutExplicitRuleIdentity() {
        assertThrows(IllegalArgumentException.class, () ->
                BpiRuleDeactivationReplay.Config.fromEnvironment(Map.of(
                        "BPI_KAFKA_BOOTSTRAP_SERVERS", "kafka-1:19092")));
    }

    @Test
    void reportCapturesTheInactiveOffsetAndFlinkReceipt() throws Exception {
        Path report = tempDir.resolve("cleanup.json").toAbsolutePath();
        var config = config(report);
        BoundaryRulePublicationV1 publication = publication();
        BoundaryRuleApplicationV1 application = BoundaryRuleApplicationV1.newBuilder()
                .setEventId("APPLICATION-1")
                .setPublicationEventId(publication.getEventId())
                .setTenantId("1000")
                .setPlantId("PLANT-01")
                .setLineId("LINE-S07-01")
                .setRuleCode("RULE-E2E")
                .setRuleVersion("1")
                .setDeploymentId("ubuntu-test-v1")
                .setStatus(BoundaryRuleApplicationStatusV1.APPLIED)
                .build();
        var source = new BpiRuleDeactivationReplay.LocatedPublication(
                new byte[] {1}, new RecordHeaders(), publication, 1, 10);
        RecordMetadata output = new RecordMetadata(
                new TopicPartition(config.ruleTopic(), 1), 0, 11, 2_000, 0, 1);
        var result = new BpiRuleDeactivationReplay.Result(
                source,
                BpiRuleDeactivationReplay.inactive(publication, config.marker(), Instant.ofEpochMilli(2_000)),
                output,
                new BpiRuleDeactivationReplay.LocatedApplication(application, 2, 12));

        BpiRuleDeactivationReplay.writeReport(config, result, "PASS", null);

        JsonNode json = new ObjectMapper().readTree(report.toFile());
        assertEquals("PASS", json.path("status").asText());
        assertFalse(json.path("inactivePublication").path("active").asBoolean(true));
        assertEquals("APPLIED", json.path("flinkApplication").path("status").asText());
        assertEquals(11, json.path("inactivePublication").path("offset").asLong());
    }

    private BpiRuleDeactivationReplay.Config config(Path report) {
        return new BpiRuleDeactivationReplay.Config(
                "kafka-1:19092",
                "bpi.boundary.rule-publication.v1",
                "bpi.boundary.rule-application.v1",
                "ADP_E2E_CLEANUP",
                "1000",
                "PLANT-01",
                "LINE-S07-01",
                "RULE-E2E",
                "1",
                Duration.ofSeconds(30),
                report.toAbsolutePath());
    }

    private BoundaryRulePublicationV1 publication() {
        return BoundaryRulePublicationV1.newBuilder()
                .setEventId("00000000-0000-4000-8000-000000000001")
                .setTenantId("1000")
                .setPlantId("PLANT-01")
                .setLineId("LINE-S07-01")
                .setLocalityGroup("LOCALITY-E2E")
                .setTopologyCode("TOPO-E2E")
                .setTopologyVersion("1")
                .setRuleCode("RULE-E2E")
                .setRuleVersion("1")
                .setBoundaryType(BoundaryType.START)
                .setQuorumMinimum(1)
                .setMinimumConfidence(1.0)
                .setMaxCompositePenalty(0.0)
                .setAllowedLatenessMs(0)
                .setWatermarkDelayMs(0)
                .setEvaluationTimeoutMs(60_000)
                .setActive(true)
                .setPublishedAtMs(1_000)
                .setChecksum("checksum-e2e")
                .build();
    }
}
