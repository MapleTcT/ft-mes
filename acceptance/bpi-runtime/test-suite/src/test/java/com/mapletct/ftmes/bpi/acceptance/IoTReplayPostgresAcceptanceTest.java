package com.mapletct.ftmes.bpi.acceptance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mapletct.ftmes.bpi.BpiServiceApplication;
import com.mapletct.ftmes.bpi.contract.v1.BatchCandidateV1;
import com.mapletct.ftmes.bpi.rules.BoundaryKind;
import com.mapletct.ftmes.bpi.rules.BoundaryRuleDefinition;
import com.mapletct.ftmes.bpi.rules.ConditionOperator;
import com.mapletct.ftmes.bpi.rules.EvidenceClass;
import com.mapletct.ftmes.bpi.rules.EvidenceCondition;
import com.mapletct.ftmes.bpi.rules.SignalObservation;
import com.mapletct.ftmes.bpi.rules.SignalQuality;
import com.mapletct.ftmes.bpi.stream.BoundaryEngineInput;
import com.mapletct.ftmes.bpi.stream.BoundaryExecutionContext;
import com.mapletct.ftmes.bpi.stream.BoundaryReplayEngine;
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

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = BpiServiceApplication.class)
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "BPI_TEST_DATABASE_URL", matches = ".+")
class IoTReplayPostgresAcceptanceTest {
    private static final String SECRET = "bpi-test-secret-0123456789abcdef";
    private static final Instant T0 = Instant.parse("2026-07-12T08:30:00Z");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> System.getenv("BPI_TEST_DATABASE_URL"));
        registry.add("spring.datasource.username", () -> env("BPI_TEST_DATABASE_USER", System.getProperty("user.name")));
        registry.add("spring.datasource.password", () -> env("BPI_TEST_DATABASE_PASSWORD", ""));
        registry.add("bpi.security.internal-jwt-secret", () -> SECRET);
        registry.add("bpi.candidate-event.protobuf-http-ingress-enabled", () -> true);
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JdbcTemplate jdbc;

    private String tenantId;

    @BeforeEach
    void seedPublishedRuntime() {
        tenantId = "ADP_E2E_IOT_REPLAY_" + UUID.randomUUID().toString().replace("-", "");
        UUID topologyId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO bpi.bpi_topology_versions
                    (id, tenant_id, topology_code, version, state, checksum, definition, created_by)
                VALUES (?, ?, 'TOPO-S07', '3', 'PUBLISHED', 'iot-replay-topology', '{}'::jsonb, 'acceptance')
                """, topologyId, tenantId);
        jdbc.update("""
                INSERT INTO bpi.bpi_rule_versions
                    (id, tenant_id, rule_code, version, topology_version_id, state, checksum, definition, created_by)
                VALUES (?, ?, 'RULE-S07-START', '1.2.0', ?, 'PUBLISHED', 'iot-replay-rule', '{}'::jsonb, 'acceptance')
                """, UUID.randomUUID(), tenantId, topologyId);
        jdbc.update("""
                INSERT INTO bpi.bpi_feature_flags
                    (id, tenant_id, scope_type, scope_key, flag_key, enabled, revision, updated_by)
                VALUES (?, ?, 'LINE', 'LINE-S07-01', 'bpi.commands', true, 1, 'acceptance')
                """, UUID.randomUUID(), tenantId);
    }

    @AfterEach
    void cleanupMarker() {
        if (tenantId == null) return;
        for (String table : List.of(
                "bpi_audit_events", "bpi_batch_state_events", "bpi_boundary_evidence",
                "bpi_api_idempotency", "bpi_inbox_events", "bpi_batch_candidates",
                "bpi_batch_instances", "bpi_feature_flags", "bpi_rule_versions",
                "bpi_topology_versions")) {
            jdbc.update("DELETE FROM bpi." + table + " WHERE tenant_id = ?", tenantId);
        }
    }

    @Test
    void iotSignalsProduceProtobufCandidateAndConfirmedPostgresShadowBatch() throws Exception {
        String orderId = tenantId + "_ORDER_001";
        BoundaryExecutionContext context = new BoundaryExecutionContext(
                tenantId, "PLANT-01", "LINE-S07-01", "S07-FEED",
                "TOPO-S07", "3", orderId, null);
        List<BoundaryEngineInput> inputs = List.of(
                input(context, SignalObservation.bool(
                        tenantId + "_ORDER", "order.active", true, SignalQuality.GOOD, T0)),
                input(context, SignalObservation.bool(
                        tenantId + "_PUMP", "feed.pump", true, SignalQuality.GOOD, T0.plusSeconds(1))),
                input(context, SignalObservation.numeric(
                        tenantId + "_FLOW", "feed.flow", new BigDecimal("18.6"),
                        SignalQuality.GOOD, T0.plusSeconds(1))));

        List<BatchCandidateV1> candidates = BoundaryReplayEngine.replay(
                startRule(), inputs, T0.plusSeconds(11));
        assertThat(candidates).hasSize(1);
        BatchCandidateV1 candidate = candidates.get(0);
        assertThat(candidate.getEvidenceCount()).isEqualTo(3);
        assertThat(candidate.getMissingSignalsList()).containsExactly("column.level");

        String ingestToken = token(
                List.of("BPI_EVENT_INGEST"), List.of("PLANT-01"), List.of("LINE-S07-01"));
        MvcResult ingested = mockMvc.perform(post("/internal/bpi/v1/candidate-events")
                        .header("Authorization", "Bearer " + ingestToken)
                        .contentType("application/x-protobuf")
                        .content(candidate.toByteArray()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.state").value("PENDING"))
                .andExpect(jsonPath("$.data.orderId").value(orderId))
                .andReturn();
        UUID candidateId = UUID.fromString(objectMapper.readTree(
                ingested.getResponse().getContentAsString()).path("data").path("id").asText());

        String shiftToken = token(
                List.of("BPI_SHIFT_LEAD"), List.of("PLANT-01"), List.of("LINE-S07-01"));
        mockMvc.perform(post("/bpi/v1/candidates/{id}/confirm", candidateId)
                        .header("Authorization", "Bearer " + shiftToken)
                        .header("Idempotency-Key", tenantId + "_CONFIRM")
                        .header("If-Match", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("reason", "IoT replay acceptance"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.candidate.state").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.batch.shadow").value(true));

        assertThat(count("bpi_inbox_events")).isEqualTo(1);
        assertThat(count("bpi_batch_candidates")).isEqualTo(1);
        assertThat(count("bpi_batch_instances")).isEqualTo(1);
        assertThat(count("bpi_boundary_evidence")).isEqualTo(3);
        assertThat(jdbc.queryForObject("""
                SELECT evidence->1->>'value'
                  FROM bpi.bpi_batch_candidates
                 WHERE tenant_id = ?
                """, String.class, tenantId)).isEqualTo("18.6");
    }

    private static BoundaryRuleDefinition startRule() {
        return new BoundaryRuleDefinition(
                "RULE-S07-START", "1.2.0", BoundaryKind.START, 2, 0.85, 0.8,
                List.of(
                        condition("order.active", ConditionOperator.EQUALS_TRUE, null, 0, EvidenceClass.REQUIRED, 40),
                        condition("feed.pump", ConditionOperator.EQUALS_TRUE, null, 3, EvidenceClass.QUORUM, 20),
                        condition("feed.flow", ConditionOperator.GREATER_THAN, "2.0", 10, EvidenceClass.QUORUM, 30),
                        condition("column.level", ConditionOperator.RISING, "0.1", 15, EvidenceClass.OPTIONAL, 10)));
    }

    private static EvidenceCondition condition(
            String signal, ConditionOperator operator, String threshold, long holdSeconds,
            EvidenceClass classification, int weight) {
        return new EvidenceCondition(
                signal, operator, threshold == null ? null : new BigDecimal(threshold),
                Duration.ofSeconds(holdSeconds), Duration.ofSeconds(30), classification, weight);
    }

    private static BoundaryEngineInput input(
            BoundaryExecutionContext context, SignalObservation observation) {
        return new BoundaryEngineInput(context, observation);
    }

    private long count(String table) {
        Long result = jdbc.queryForObject(
                "SELECT count(*) FROM bpi." + table + " WHERE tenant_id = ?", Long.class, tenantId);
        return result == null ? 0 : result;
    }

    private String token(List<String> roles, List<String> plants, List<String> lines) throws Exception {
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer("ft-mes-adapter")
                .audience("bpi-service")
                .subject("iot-replay-acceptance")
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(600)))
                .claim("tenant_id", tenantId)
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
