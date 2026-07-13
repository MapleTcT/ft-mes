package com.mapletct.ftmes.bpi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryRulePublicationV1;
import com.mapletct.ftmes.bpi.domain.OutboxEventClaim;
import com.mapletct.ftmes.bpi.infrastructure.outbox.RulePublicationOutboxRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "BPI_TEST_DATABASE_URL", matches = ".+")
class BpiRulePostgresAcceptanceTest {
    private static final String SECRET = "bpi-test-secret-0123456789abcdef";

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> System.getenv("BPI_TEST_DATABASE_URL"));
        registry.add("spring.datasource.username", () -> env("BPI_TEST_DATABASE_USER", System.getProperty("user.name")));
        registry.add("spring.datasource.password", () -> env("BPI_TEST_DATABASE_PASSWORD", ""));
        registry.add("bpi.security.internal-jwt-secret", () -> SECRET);
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JdbcTemplate jdbc;
    @Autowired RulePublicationOutboxRepository outboxRepository;

    private String tenantId;
    private UUID topologyId;
    private UUID ruleId;
    private UUID telemetryEventId;
    private Instant boundaryTime;

    @BeforeEach
    void seedScopedRuleReplayFacts() throws Exception {
        tenantId = "ADP_E2E_BPI_RULE_" + UUID.randomUUID().toString().replace("-", "");
        topologyId = UUID.randomUUID();
        ruleId = UUID.randomUUID();
        telemetryEventId = UUID.randomUUID();
        boundaryTime = Instant.parse("2026-07-12T08:15:00Z");

        jdbc.update("""
                INSERT INTO bpi.bpi_topology_versions
                    (id, tenant_id, topology_code, version, state, checksum, definition,
                     plant_id, line_id, revision, created_by, updated_by)
                VALUES (?, ?, 'TOPO-S07', '3', 'PUBLISHED', ?, CAST(? AS jsonb),
                        'PLANT-01', 'LINE-S07-01', 1, 'acceptance', 'acceptance')
                """, topologyId, tenantId, "t".repeat(64), topologyDefinition());
        jdbc.update("""
                INSERT INTO bpi.bpi_rule_versions
                    (id, tenant_id, rule_code, version, topology_version_id, state, checksum, definition,
                     revision, plant_id, line_id, created_by, updated_by)
                VALUES (?, ?, 'RULE-S07-START', '1.2.0', ?, 'DRAFT', ?, CAST(? AS jsonb),
                        1, 'PLANT-01', 'LINE-S07-01', 'acceptance', 'acceptance')
                """, ruleId, tenantId, topologyId, "r".repeat(64), ruleDefinition());
        jdbc.update("""
                INSERT INTO bpi.bpi_feature_flags
                    (id, tenant_id, scope_type, scope_key, flag_key, enabled, revision, updated_by)
                VALUES (?, ?, 'LINE', 'LINE-S07-01', 'bpi.rule-management', true, 1, 'acceptance')
                """, UUID.randomUUID(), tenantId);
        jdbc.update("""
                INSERT INTO bpi.bpi_rule_golden_boundaries
                    (id, tenant_id, plant_id, line_id, golden_set_id, boundary_type,
                     boundary_time, tolerance_seconds, source_ref, created_by)
                VALUES (?, ?, 'PLANT-01', 'LINE-S07-01', 'GOLDEN-S07-2026Q2', 'START',
                        ?, 5, 'operator-reviewed-batch-001', 'acceptance')
                """, UUID.randomUUID(), tenantId, java.sql.Timestamp.from(boundaryTime));
        jdbc.update("""
                INSERT INTO bpi.bpi_telemetry_events
                    (id, tenant_id, plant_id, line_id, gateway_id, product_id, device_id,
                     event_id, message_id, event_time, ingest_time, source_epoch, sequence,
                     sequence_origin, sequence_disposition, payload_checksum, headers,
                     point_count, accepted_point_count, rejected_point_count, status)
                VALUES (?, ?, 'PLANT-01', 'LINE-S07-01', 'GW-S07-01', 'PRODUCT-SUGAR', 'DEVICE-S07-01',
                        'RULE-EVT-001', 'RULE-MSG-001', ?, ?, 1, 1,
                        'EXPORTER', 'FIRST', ?, '{}'::jsonb, 2, 2, 0, 'ACCEPTED')
                """, telemetryEventId, tenantId, java.sql.Timestamp.from(boundaryTime),
                java.sql.Timestamp.from(boundaryTime.plusMillis(10)), "a".repeat(64));
        insertPoint("RULE-FLOW-001", "flow.instant", "DOUBLE", 18.6, null, "t/h");
        insertPoint("RULE-PUMP-001", "pump.running", "BOOLEAN", null, true, "bool");
    }

    @AfterEach
    void cleanupMarker() {
        if (tenantId == null) return;
        jdbc.update("DELETE FROM bpi.bpi_audit_events WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_api_idempotency WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_outbox_events WHERE tenant_id = ?", tenantId);
        jdbc.update("UPDATE bpi.bpi_rule_versions SET latest_simulation_id = NULL WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_rule_simulations WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_rule_golden_boundaries WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_telemetry_points WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_telemetry_events WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_telemetry_source_state WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_feature_flags WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_rule_versions WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_topology_versions WHERE tenant_id = ?", tenantId);
    }

    @Test
    void ruleReplayUsesTelemetryAndGoldenBoundaryBeforeIdempotentPublication() throws Exception {
        String viewerToken = token(
                tenantId, List.of("BPI_VIEWER"), List.of("PLANT-01"), List.of("LINE-S07-01"));
        mockMvc.perform(get("/bpi/v1/topologies")
                        .header("Authorization", "Bearer " + viewerToken)
                        .param("plantId", "PLANT-01")
                        .param("lineId", "LINE-S07-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(topologyId.toString()))
                .andExpect(jsonPath("$.data[0].revision").value(1));
        mockMvc.perform(get("/bpi/v1/topologies/{id}", topologyId)
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("TOPO-S07"));
        mockMvc.perform(get("/bpi/v1/rules")
                        .header("Authorization", "Bearer " + viewerToken)
                        .param("plantId", "PLANT-01")
                        .param("lineId", "LINE-S07-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].state").value("DRAFT"))
                .andExpect(jsonPath("$.data[0].publicationStatus").value("NOT_PUBLISHED"))
                .andExpect(jsonPath("$.data[0].publicationAttemptCount").value(0));
        mockMvc.perform(get("/bpi/v1/rules/{id}", ruleId)
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.topologyVersion").value("TOPO-S07@3"));

        String engineerToken = token(
                tenantId, List.of("BPI_ENGINEER"), List.of("PLANT-01"), List.of("LINE-S07-01"));
        byte[] simulationBody = objectMapper.writeValueAsBytes(Map.of(
                "lineId", "LINE-S07-01",
                "from", boundaryTime.minusSeconds(1).toString(),
                "to", boundaryTime.plusSeconds(1).toString(),
                "topologyVersion", "TOPO-S07@3",
                "calibrationVersion", "CAL-1",
                "goldenSetId", "GOLDEN-S07-2026Q2"));
        mockMvc.perform(post("/bpi/v1/rules/{id}/simulate", ruleId)
                        .header("Authorization", "Bearer " + engineerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(simulationBody))
                .andExpect(status().is(428));

        byte[] emptyWindow = objectMapper.writeValueAsBytes(Map.of(
                "lineId", "LINE-S07-01",
                "from", boundaryTime.plusSeconds(30).toString(),
                "to", boundaryTime.plusSeconds(60).toString(),
                "topologyVersion", "TOPO-S07@3",
                "calibrationVersion", "CAL-1",
                "goldenSetId", "GOLDEN-S07-2026Q2"));
        mockMvc.perform(post("/bpi/v1/rules/{id}/simulate", ruleId)
                        .header("Authorization", "Bearer " + engineerToken)
                        .header("Idempotency-Key", "empty-window-" + ruleId)
                        .header("If-Match", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(emptyWindow))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("No calibrated telemetry")));
        assertThat(count("bpi_api_idempotency")).isZero();

        String simulateKey = "simulate-rule-" + ruleId;
        MvcResult simulated = mockMvc.perform(post("/bpi/v1/rules/{id}/simulate", ruleId)
                        .header("Authorization", "Bearer " + engineerToken)
                        .header("Idempotency-Key", simulateKey)
                        .header("If-Match", "1")
                        .header("X-Trace-Id", "TRACE-SIM-" + ruleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(simulationBody))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.state").value("PASSED"))
                .andExpect(jsonPath("$.data.metrics.matched").value(1))
                .andExpect(jsonPath("$.data.metrics.missed").value(0))
                .andExpect(jsonPath("$.data.metrics.falsePositive").value(0))
                .andExpect(jsonPath("$.data.inputManifest.observationCount").value(2))
                .andExpect(jsonPath("$.data.emittedBoundaries[0]").value(boundaryTime.toString()))
                .andReturn();
        JsonNode simulation = objectMapper.readTree(simulated.getResponse().getContentAsString()).path("data");
        UUID simulationId = UUID.fromString(simulation.path("id").asText());
        String simulationChecksum = simulation.path("checksum").asText();
        assertThat(simulationChecksum).matches("[a-f0-9]{64}");

        mockMvc.perform(post("/bpi/v1/rules/{id}/simulate", ruleId)
                        .header("Authorization", "Bearer " + engineerToken)
                        .header("Idempotency-Key", simulateKey)
                        .header("If-Match", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(simulationBody))
                .andExpect(status().isAccepted())
                .andExpect(header().string("Idempotent-Replay", "true"))
                .andExpect(jsonPath("$.data.id").value(simulationId.toString()));
        mockMvc.perform(get("/bpi/v1/rule-simulations/{id}", simulationId)
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.checksum").value(simulationChecksum));

        assertThat(jdbc.queryForObject("""
                SELECT state || '|' || revision FROM bpi.bpi_rule_versions
                 WHERE tenant_id = ? AND id = ?
                """, String.class, tenantId, ruleId)).isEqualTo("SIMULATION_PASSED|2");
        assertThat(count("bpi_rule_simulations")).isEqualTo(1);
        assertThat(count("bpi_api_idempotency")).isEqualTo(1);

        byte[] badPublish = objectMapper.writeValueAsBytes(Map.of(
                "reason", "审批发布规则版本",
                "simulationId", simulationId,
                "simulationChecksum", "bad-checksum"));
        mockMvc.perform(post("/bpi/v1/rules/{id}/publish", ruleId)
                        .header("Authorization", "Bearer " + engineerToken)
                        .header("Idempotency-Key", "bad-publish-" + ruleId)
                        .header("If-Match", "2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badPublish))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("matching checksum")));
        assertThat(count("bpi_api_idempotency")).isEqualTo(1);
        assertThat(count("bpi_outbox_events")).isZero();

        byte[] publishBody = objectMapper.writeValueAsBytes(Map.of(
                "reason", "审批发布规则版本",
                "simulationId", simulationId,
                "simulationChecksum", simulationChecksum));
        String publishKey = "publish-rule-" + ruleId;
        mockMvc.perform(post("/bpi/v1/rules/{id}/publish", ruleId)
                        .header("Authorization", "Bearer " + engineerToken)
                        .header("Idempotency-Key", publishKey)
                        .header("If-Match", "2")
                        .header("X-Trace-Id", "TRACE-PUBLISH-" + ruleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(publishBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("PUBLISHED"))
                .andExpect(jsonPath("$.data.revision").value(3))
                .andExpect(jsonPath("$.data.publicationStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.publicationAttemptCount").value(0));
        mockMvc.perform(post("/bpi/v1/rules/{id}/publish", ruleId)
                        .header("Authorization", "Bearer " + engineerToken)
                        .header("Idempotency-Key", publishKey)
                        .header("If-Match", "2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(publishBody))
                .andExpect(status().isOk())
                .andExpect(header().string("Idempotent-Replay", "true"))
                .andExpect(jsonPath("$.data.revision").value(3));

        assertThat(jdbc.queryForObject("""
                SELECT state || '|' || revision FROM bpi.bpi_rule_versions
                 WHERE tenant_id = ? AND id = ?
                """, String.class, tenantId, ruleId)).isEqualTo("PUBLISHED|3");
        assertThat(count("bpi_outbox_events")).isOne();
        Map<String, Object> outbox = jdbc.queryForMap("""
                SELECT status, attempt_count, topic, partition_key, payload
                  FROM bpi.bpi_outbox_events
                 WHERE tenant_id = ? AND aggregate_id = ?
                """, tenantId, ruleId);
        assertThat(outbox.get("status")).isEqualTo("PENDING");
        assertThat(outbox.get("attempt_count")).isEqualTo(0);
        assertThat(outbox.get("topic")).isEqualTo("bpi.boundary.rule-publication.v1");
        assertThat(outbox.get("partition_key")).isEqualTo(
                tenantId + ":LINE-S07-01:RULE-S07-START:1.2.0");
        BoundaryRulePublicationV1 publication = BoundaryRulePublicationV1.parseFrom(
                (byte[]) outbox.get("payload"));
        assertThat(publication.getTenantId()).isEqualTo(tenantId);
        assertThat(publication.getLocalityGroup()).isEqualTo("LOCALITY-S07-EVAP");
        assertThat(publication.getRuleCode()).isEqualTo("RULE-S07-START");
        assertThat(publication.getTopologyCode()).isEqualTo("TOPO-S07");
        assertThat(publication.getConditionsCount()).isEqualTo(2);
        assertThat(publication.getSignalBindingsCount()).isEqualTo(2);
        assertThat(publication.getActive()).isTrue();
        assertThat(publication.getChecksum()).isEqualTo("r".repeat(64));
        assertThat(jdbc.queryForList("""
                SELECT action || '|' || before_revision || '|' || after_revision
                  FROM bpi.bpi_audit_events
                 WHERE tenant_id = ? AND object_id = ?
                 ORDER BY after_revision
                """, String.class, tenantId, ruleId))
                .containsExactly("RULE_SIMULATED|1|2", "RULE_PUBLISHED|2|3");
        assertThat(count("bpi_api_idempotency")).isEqualTo(2);
        assertThat(count("bpi_outbox_events")).isOne();

        String wrongScopeToken = token(
                tenantId, List.of("BPI_VIEWER"), List.of("PLANT-01"), List.of("LINE-OTHER"));
        mockMvc.perform(get("/bpi/v1/rules/{id}", ruleId)
                        .header("Authorization", "Bearer " + wrongScopeToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void replayEmitsAtTheExactHoldTimerInsteadOfTheWindowEnd() throws Exception {
        Instant expectedBoundary = boundaryTime.plusSeconds(15);
        jdbc.update("UPDATE bpi.bpi_rule_versions SET definition = CAST(? AS jsonb) WHERE id = ?",
                ruleDefinition(15), ruleId);
        jdbc.update("UPDATE bpi.bpi_rule_golden_boundaries SET boundary_time = ? WHERE tenant_id = ?",
                java.sql.Timestamp.from(expectedBoundary), tenantId);
        String engineerToken = token(
                tenantId, List.of("BPI_ENGINEER"), List.of("PLANT-01"), List.of("LINE-S07-01"));
        byte[] body = objectMapper.writeValueAsBytes(Map.of(
                "lineId", "LINE-S07-01",
                "from", boundaryTime.minusSeconds(1).toString(),
                "to", boundaryTime.plusSeconds(30).toString(),
                "topologyVersion", "TOPO-S07@3",
                "calibrationVersion", "CAL-1",
                "goldenSetId", "GOLDEN-S07-2026Q2"));

        mockMvc.perform(post("/bpi/v1/rules/{id}/simulate", ruleId)
                        .header("Authorization", "Bearer " + engineerToken)
                        .header("Idempotency-Key", "hold-timer-" + ruleId)
                        .header("If-Match", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.state").value("PASSED"))
                .andExpect(jsonPath("$.data.emittedBoundaries[0]").value(expectedBoundary.toString()))
                .andExpect(jsonPath("$.data.metrics.meanBoundaryErrorSeconds").value(0.0));
    }

    @Test
    void outboxClaimsRecoverAndReachPublishedOrFailedTerminalState() {
        UUID publishId = insertOutbox("OUTBOX-PUBLISH", "PENDING", 0, null, null);
        List<OutboxEventClaim> firstClaims = outboxRepository.claimPending(10, Duration.ofMinutes(2));
        assertThat(firstClaims).hasSize(1);
        OutboxEventClaim first = firstClaims.get(0);
        assertThat(first.id()).isEqualTo(publishId);
        assertThat(first.attemptCount()).isEqualTo(1);
        assertThat(outboxRepository.claimPending(10, Duration.ofMinutes(2))).isEmpty();

        assertThat(outboxRepository.markFailed(
                first.id(), first.claimToken(), first.attemptCount(), 3,
                Duration.ofMillis(1), "temporary broker failure")).isTrue();
        jdbc.update("UPDATE bpi.bpi_outbox_events SET next_attempt_at = now() WHERE id = ?", publishId);
        OutboxEventClaim retry = outboxRepository.claimPending(10, Duration.ofMinutes(2)).get(0);
        assertThat(retry.attemptCount()).isEqualTo(2);
        assertThat(outboxRepository.markPublished(retry.id(), retry.claimToken())).isTrue();
        assertThat(outboxState(publishId)).isEqualTo("PUBLISHED|2|false");

        UUID staleToken = UUID.randomUUID();
        UUID failedId = insertOutbox(
                "OUTBOX-STALE", "DISPATCHING", 1, staleToken,
                java.sql.Timestamp.from(Instant.now().minusSeconds(600)));
        OutboxEventClaim recovered = outboxRepository.claimPending(10, Duration.ofMinutes(2)).get(0);
        assertThat(recovered.id()).isEqualTo(failedId);
        assertThat(recovered.claimToken()).isNotEqualTo(staleToken);
        assertThat(recovered.attemptCount()).isEqualTo(2);
        assertThat(outboxRepository.markFailed(
                recovered.id(), recovered.claimToken(), recovered.attemptCount(), 2,
                Duration.ofSeconds(1), "permanent broker failure")).isTrue();
        assertThat(outboxState(failedId)).isEqualTo("FAILED|2|false");
        assertThat(outboxRepository.claimPending(10, Duration.ofMinutes(2))).isEmpty();
    }

    private UUID insertOutbox(
            String eventType,
            String state,
            int attempts,
            UUID claimToken,
            java.sql.Timestamp claimedAt) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO bpi.bpi_outbox_events
                    (id, tenant_id, plant_id, line_id, aggregate_type, aggregate_id,
                     event_type, topic, partition_key, payload, status, attempt_count,
                     claim_token, claimed_at)
                VALUES (?, ?, 'PLANT-01', 'LINE-S07-01', 'RULE_VERSION', ?, ?,
                        'bpi.boundary.rule-publication.v1', ?, ?, ?, ?, ?, ?)
                """, id, tenantId, ruleId, eventType,
                tenantId + ":LINE-S07-01:" + eventType, new byte[] {1, 2, 3},
                state, attempts, claimToken, claimedAt);
        return id;
    }

    private String outboxState(UUID id) {
        return jdbc.queryForObject("""
                SELECT status || '|' || attempt_count || '|' || (claim_token IS NOT NULL)
                  FROM bpi.bpi_outbox_events WHERE id = ?
                """, String.class, id);
    }

    private void insertPoint(
            String eventId,
            String propertyId,
            String valueType,
            Double numericValue,
            Boolean booleanValue,
            String unit) {
        jdbc.update("""
                INSERT INTO bpi.bpi_telemetry_points
                    (id, tenant_id, telemetry_event_id, event_id, property_id, value_type,
                     numeric_value, boolean_value, unit, quality_code, sample_time, calibration_version)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'GOOD', ?, 'CAL-1')
                """, UUID.randomUUID(), tenantId, telemetryEventId, eventId, propertyId, valueType,
                numericValue, booleanValue, unit, java.sql.Timestamp.from(boundaryTime));
    }

    private String ruleDefinition() throws Exception {
        return ruleDefinition(0);
    }

    private String topologyDefinition() throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "localityGroup", "LOCALITY-S07-EVAP",
                "bindings", List.of(
                        Map.of(
                                "signal", "flow.instant",
                                "deviceId", "DEVICE-S07-01",
                                "propertyId", "flow.instant",
                                "expectedUnit", "t/h",
                                "calibrationVersion", "CAL-1"),
                        Map.of(
                                "signal", "pump.running",
                                "deviceId", "DEVICE-S07-01",
                                "propertyId", "pump.running",
                                "expectedUnit", "bool",
                                "calibrationVersion", "CAL-1"))));
    }

    private String ruleDefinition(int holdSeconds) throws Exception {
        return objectMapper.writeValueAsString(Map.ofEntries(
                Map.entry("boundaryType", "START"),
                Map.entry("quorumMinimum", 2),
                Map.entry("minimumConfidence", 0.80),
                Map.entry("maxCompositePenalty", 0.80),
                Map.entry("timing", Map.of(
                        "allowedLatenessSeconds", 0,
                        "watermarkDelaySeconds", 0,
                        "evaluationTimeoutSeconds", 300)),
                Map.entry("conditions", List.of(
                        Map.ofEntries(
                                Map.entry("signal", "flow.instant"),
                                Map.entry("operator", "GREATER_THAN"),
                                Map.entry("threshold", 10),
                                Map.entry("holdSeconds", holdSeconds),
                                Map.entry("maxSilenceSeconds", 60),
                                Map.entry("classification", "QUORUM"),
                                Map.entry("weight", 50)),
                        Map.ofEntries(
                                Map.entry("signal", "pump.running"),
                                Map.entry("operator", "EQUALS_TRUE"),
                                Map.entry("holdSeconds", holdSeconds),
                                Map.entry("maxSilenceSeconds", 60),
                                Map.entry("classification", "QUORUM"),
                                Map.entry("weight", 50))))));
    }

    private long count(String table) {
        Long result = jdbc.queryForObject(
                "SELECT count(*) FROM bpi." + table + " WHERE tenant_id = ?", Long.class, tenantId);
        return result == null ? 0 : result;
    }

    private String token(String tenant, List<String> roles, List<String> plants, List<String> lines) throws Exception {
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer("ft-mes-adapter")
                .audience("bpi-service")
                .subject("rule-acceptance-user")
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(600)))
                .claim("tenant_id", tenant)
                .claim("roles", roles)
                .claim("plant_ids", plants)
                .claim("line_ids", lines)
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        jwt.sign(new MACSigner(SECRET));
        return jwt.serialize();
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null ? fallback : value;
    }
}
