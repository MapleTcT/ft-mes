package com.mapletct.ftmes.bpi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.mapletct.ftmes.bpi.contract.identity.CandidateKeyFactory;
import com.mapletct.ftmes.bpi.contract.v1.BatchCandidateV1;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryType;
import com.mapletct.ftmes.bpi.contract.v1.CandidateEvidenceV1;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "BPI_TEST_DATABASE_URL", matches = ".+")
class BpiPostgresAcceptanceTest {
    private static final String SECRET = "bpi-test-secret-0123456789abcdef";

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
    private UUID topologyId;
    private UUID ruleId;
    private UUID candidateKey;
    private String ingestPayload;

    @BeforeEach
    void seedPublishedRuleAndTopology() throws Exception {
        tenantId = "ADP_E2E_BPI_" + UUID.randomUUID().toString().replace("-", "");
        topologyId = UUID.randomUUID();
        ruleId = UUID.randomUUID();
        candidateKey = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO bpi.bpi_topology_versions
                    (id, tenant_id, topology_code, version, state, checksum, definition, created_by)
                VALUES (?, ?, 'TOPO-S07', '3', 'PUBLISHED', 'topology-checksum', '{}'::jsonb, 'acceptance')
                """, topologyId, tenantId);
        jdbc.update("""
                INSERT INTO bpi.bpi_rule_versions
                    (id, tenant_id, rule_code, version, topology_version_id, state, checksum, definition, created_by)
                VALUES (?, ?, 'RULE-S07-START', '1.2.0', ?, 'PUBLISHED', 'rule-checksum', '{}'::jsonb, 'acceptance')
                """, ruleId, tenantId, topologyId);
        jdbc.update("""
                INSERT INTO bpi.bpi_feature_flags
                    (id, tenant_id, scope_type, scope_key, flag_key, enabled, revision, updated_by)
                VALUES (?, ?, 'LINE', 'LINE-S07-01', 'bpi.commands', true, 1, 'acceptance')
                """, UUID.randomUUID(), tenantId);

        Instant boundaryTime = Instant.parse("2026-07-12T07:59:40Z");
        Map<String, Object> evidence = Map.of(
                "eventId", "EVT-FLOW-" + candidateKey,
                "signal", "instantFlowAboveThreshold",
                "classification", "QUORUM",
                "satisfied", true,
                "value", "18.6",
                "unit", "t/h",
                "quality", "GOOD",
                "eventTime", boundaryTime.toString(),
                "source", "approved-replay");
        ingestPayload = objectMapper.writeValueAsString(Map.ofEntries(
                Map.entry("eventId", "CANDIDATE-EVENT-" + candidateKey),
                Map.entry("candidateKey", candidateKey),
                Map.entry("plantId", "PLANT-01"),
                Map.entry("lineId", "LINE-S07-01"),
                Map.entry("boundaryType", "START"),
                Map.entry("orderId", "MO-20260712-001"),
                Map.entry("boundaryTime", boundaryTime.toString()),
                Map.entry("ruleCode", "RULE-S07-START"),
                Map.entry("ruleVersion", "1.2.0"),
                Map.entry("topologyCode", "TOPO-S07"),
                Map.entry("topologyVersion", "3"),
                Map.entry("confidence", 0.94),
                Map.entry("evidence", List.of(evidence)),
                Map.entry("missingSignals", List.of())));
    }

    @AfterEach
    void cleanupMarker() {
        if (tenantId == null) return;
        jdbc.update("DELETE FROM bpi.bpi_audit_events WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_batch_state_events WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_boundary_evidence WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_api_idempotency WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_inbox_events WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_batch_candidates WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_batch_instances WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_feature_flags WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_rule_versions WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_topology_versions WHERE tenant_id = ?", tenantId);
    }

    @Test
    void realApiCreatesOneAuditedShadowBatchAndRejectsStaleOrCrossTenantAccess() throws Exception {
        String ingestToken = token(tenantId, List.of("BPI_EVENT_INGEST"), List.of("PLANT-01"), List.of("LINE-S07-01"));
        var invalidEvidence = (com.fasterxml.jackson.databind.node.ObjectNode)
                objectMapper.readTree(ingestPayload);
        ((com.fasterxml.jackson.databind.node.ObjectNode) invalidEvidence.withArray("evidence").get(0))
                .remove("eventTime");
        mockMvc.perform(post("/internal/bpi/v1/candidates")
                        .header("Authorization", "Bearer " + ingestToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(invalidEvidence)))
                .andExpect(status().isUnprocessableEntity());
        assertThat(count("bpi_inbox_events")).isZero();

        MvcResult ingested = mockMvc.perform(post("/internal/bpi/v1/candidates")
                        .header("Authorization", "Bearer " + ingestToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ingestPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.state").value("PENDING"))
                .andExpect(jsonPath("$.data.revision").value(1))
                .andReturn();
        UUID candidateId = UUID.fromString(objectMapper.readTree(
                ingested.getResponse().getContentAsString()).path("data").path("id").asText());

        mockMvc.perform(post("/internal/bpi/v1/candidates")
                        .header("Authorization", "Bearer " + ingestToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ingestPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(candidateId.toString()));
        assertThat(count("bpi_batch_candidates")).isEqualTo(1);
        assertThat(count("bpi_inbox_events")).isEqualTo(1);

        var eventIdentityConflict = (com.fasterxml.jackson.databind.node.ObjectNode)
                objectMapper.readTree(ingestPayload);
        eventIdentityConflict.put("candidateKey", UUID.randomUUID().toString());
        mockMvc.perform(post("/internal/bpi/v1/candidates")
                        .header("Authorization", "Bearer " + ingestToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(eventIdentityConflict)))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.title").value("Conflict"))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("event ID")))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
        assertThat(count("bpi_batch_candidates")).isEqualTo(1);
        assertThat(count("bpi_inbox_events")).isEqualTo(1);

        String shiftToken = token(tenantId, List.of("BPI_SHIFT_LEAD"), List.of("PLANT-01"), List.of("LINE-S07-01"));
        String commandBody = objectMapper.writeValueAsString(Map.of("reason", "班长确认回放边界"));
        mockMvc.perform(post("/bpi/v1/candidates/{id}/confirm", candidateId)
                        .header("Authorization", "Bearer " + shiftToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(commandBody))
                .andExpect(status().is(428));

        String idempotencyKey = "confirm-" + candidateKey;
        MvcResult confirmed = mockMvc.perform(post("/bpi/v1/candidates/{id}/confirm", candidateId)
                        .header("Authorization", "Bearer " + shiftToken)
                        .header("Idempotency-Key", idempotencyKey)
                        .header("If-Match", "1")
                        .header("X-Trace-Id", "TRACE-" + candidateKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(commandBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.candidate.state").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.candidate.revision").value(2))
                .andExpect(jsonPath("$.data.batch.shadow").value(true))
                .andExpect(jsonPath("$.data.batch.wmsStatus").value("NOT_REQUESTED"))
                .andReturn();
        JsonNode confirmedBody = objectMapper.readTree(confirmed.getResponse().getContentAsString());
        UUID batchId = UUID.fromString(confirmedBody.path("data").path("batch").path("id").asText());

        mockMvc.perform(post("/bpi/v1/candidates/{id}/confirm", candidateId)
                        .header("Authorization", "Bearer " + shiftToken)
                        .header("Idempotency-Key", idempotencyKey)
                        .header("If-Match", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(commandBody))
                .andExpect(status().isOk())
                .andExpect(header().string("Idempotent-Replay", "true"))
                .andExpect(jsonPath("$.data.batch.id").value(batchId.toString()));

        String wrongLineCommandToken = token(
                tenantId, List.of("BPI_SHIFT_LEAD"), List.of("PLANT-01"), List.of("LINE-OTHER"));
        mockMvc.perform(post("/bpi/v1/candidates/{id}/confirm", candidateId)
                        .header("Authorization", "Bearer " + wrongLineCommandToken)
                        .header("Idempotency-Key", idempotencyKey)
                        .header("If-Match", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(commandBody))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("scope")));

        mockMvc.perform(post("/bpi/v1/candidates/{id}/confirm", candidateId)
                        .header("Authorization", "Bearer " + shiftToken)
                        .header("Idempotency-Key", "stale-" + candidateKey)
                        .header("If-Match", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(commandBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.currentRevision").value(2));

        String viewerToken = token(tenantId, List.of("BPI_VIEWER"), List.of("PLANT-01"), List.of("LINE-S07-01"));
        mockMvc.perform(get("/bpi/v1/batches/{id}", batchId)
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.shadow").value(true));
        mockMvc.perform(get("/bpi/v1/batches/{id}/evidence", batchId)
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.start.length()").value(1));
        mockMvc.perform(get("/bpi/v1/batches/{id}/timeline", batchId)
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].action").value("SHADOW_BATCH_CREATED"));

        String wrongLineToken = token(tenantId, List.of("BPI_VIEWER"), List.of("PLANT-01"), List.of("LINE-OTHER"));
        mockMvc.perform(get("/bpi/v1/candidates/{id}", candidateId)
                        .header("Authorization", "Bearer " + wrongLineToken))
                .andExpect(status().isForbidden());
        String otherTenantToken = token("OTHER-TENANT", List.of("BPI_VIEWER"), List.of("*"), List.of("*"));
        mockMvc.perform(get("/bpi/v1/candidates/{id}", candidateId)
                        .header("Authorization", "Bearer " + otherTenantToken))
                .andExpect(status().isNotFound());

        jdbc.update("DELETE FROM bpi.bpi_feature_flags WHERE tenant_id = ?", tenantId);
        mockMvc.perform(post("/bpi/v1/candidates/{id}/confirm", candidateId)
                        .header("Authorization", "Bearer " + shiftToken)
                        .header("Idempotency-Key", "disabled-" + candidateKey)
                        .header("If-Match", "2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(commandBody))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("disabled")));

        assertThat(count("bpi_batch_instances")).isEqualTo(1);
        assertThat(count("bpi_batch_state_events")).isEqualTo(1);
        assertThat(count("bpi_boundary_evidence")).isEqualTo(1);
        assertThat(count("bpi_audit_events")).isEqualTo(1);
        assertThat(count("bpi_api_idempotency")).isEqualTo(1);
    }

    @Test
    void protobufCandidateEventPersistsRichEvidenceAndConfirmsShadowBatch() throws Exception {
        String evidenceEventId = "ADP_E2E_BPI_PROTO_FLOW_" + candidateKey;
        String orderId = "ADP_E2E_BPI_PROTO_ORDER_" + candidateKey;
        String stableKey = CandidateKeyFactory.startKey(
                tenantId, "LINE-S07-01", "1.2.0", orderId, evidenceEventId);
        Instant boundaryTime = Instant.parse("2026-07-12T08:15:00Z");
        BatchCandidateV1 event = BatchCandidateV1.newBuilder()
                .setEventId("ADP_E2E_BPI_PROTO_CANDIDATE_" + candidateKey)
                .setCandidateKey(stableKey)
                .setTenantId(tenantId)
                .setPlantId("PLANT-01")
                .setLineId("LINE-S07-01")
                .setBoundaryType(BoundaryType.START)
                .setRuleCode("RULE-S07-START")
                .setRuleVersion("1.2.0")
                .setTopologyVersion("3")
                .setContextOrderId(orderId)
                .setFirstQuorumEvidenceEventId(evidenceEventId)
                .setBoundaryEventTimeMs(boundaryTime.toEpochMilli())
                .setConfidence(0.94)
                .addEvidenceEventIds(evidenceEventId)
                .setEmittedAtMs(boundaryTime.toEpochMilli())
                .putHeaders("topology_code", "TOPO-S07")
                .addEvidence(CandidateEvidenceV1.newBuilder()
                        .setEventId(evidenceEventId)
                        .setSignal("feed.flow")
                        .setClassification("QUORUM")
                        .setSatisfied(true)
                        .setValue("18.6")
                        .setUnit("t/h")
                        .setQualityCode("GOOD")
                        .setEventTimeMs(boundaryTime.toEpochMilli())
                        .setSource("bpi-stream-engine"))
                .addMissingSignals("column.level")
                .build();
        String ingestToken = token(
                tenantId, List.of("BPI_EVENT_INGEST"), List.of("PLANT-01"), List.of("LINE-S07-01"));

        MvcResult ingested = mockMvc.perform(post("/internal/bpi/v1/candidate-events")
                        .header("Authorization", "Bearer " + ingestToken)
                        .contentType("application/x-protobuf")
                        .content(event.toByteArray()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.state").value("PENDING"))
                .andExpect(jsonPath("$.data.evidence[0].signal").value("feed.flow"))
                .andExpect(jsonPath("$.data.missingSignals[0]").value("column.level"))
                .andReturn();
        UUID candidateId = UUID.fromString(objectMapper.readTree(
                ingested.getResponse().getContentAsString()).path("data").path("id").asText());

        assertThat(jdbc.queryForObject("""
                SELECT evidence->0->>'source'
                  FROM bpi.bpi_batch_candidates
                 WHERE tenant_id = ? AND candidate_key = ?
                """, String.class, tenantId, UUID.fromString(stableKey)))
                .isEqualTo("bpi-stream-engine");
        assertThat(jdbc.queryForObject("""
                SELECT missing_signals->>0
                  FROM bpi.bpi_batch_candidates
                 WHERE tenant_id = ? AND candidate_key = ?
                """, String.class, tenantId, UUID.fromString(stableKey)))
                .isEqualTo("column.level");

        String shiftToken = token(
                tenantId, List.of("BPI_SHIFT_LEAD"), List.of("PLANT-01"), List.of("LINE-S07-01"));
        mockMvc.perform(post("/bpi/v1/candidates/{id}/confirm", candidateId)
                        .header("Authorization", "Bearer " + shiftToken)
                        .header("Idempotency-Key", "proto-confirm-" + stableKey)
                        .header("If-Match", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("reason", "Protobuf 边界回放确认"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.candidate.state").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.batch.shadow").value(true));

        assertThat(count("bpi_inbox_events")).isEqualTo(1);
        assertThat(count("bpi_batch_candidates")).isEqualTo(1);
        assertThat(count("bpi_batch_instances")).isEqualTo(1);
        assertThat(count("bpi_boundary_evidence")).isEqualTo(1);
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
                .subject("acceptance-user")
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
