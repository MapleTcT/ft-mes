package com.mapletct.ftmes.bpi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
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
                VALUES (?, ?, 'TOPO-S07', '3', 'PUBLISHED', ?, '{}'::jsonb,
                        'PLANT-01', 'LINE-S07-01', 1, 'acceptance', 'acceptance')
                """, topologyId, tenantId, "t".repeat(64));
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
                .andExpect(jsonPath("$.data[0].state").value("DRAFT"));
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
                .andExpect(jsonPath("$.data.revision").value(3));
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
        assertThat(jdbc.queryForList("""
                SELECT action || '|' || before_revision || '|' || after_revision
                  FROM bpi.bpi_audit_events
                 WHERE tenant_id = ? AND object_id = ?
                 ORDER BY after_revision
                """, String.class, tenantId, ruleId))
                .containsExactly("RULE_SIMULATED|1|2", "RULE_PUBLISHED|2|3");
        assertThat(count("bpi_api_idempotency")).isEqualTo(2);

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
