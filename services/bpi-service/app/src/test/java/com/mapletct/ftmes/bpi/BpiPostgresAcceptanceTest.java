package com.mapletct.ftmes.bpi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.mapletct.ftmes.bpi.application.error.BpiValidationException;
import com.mapletct.ftmes.bpi.contract.identity.CandidateKeyFactory;
import com.mapletct.ftmes.bpi.contract.v1.BatchCandidateV1;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryType;
import com.mapletct.ftmes.bpi.contract.v1.CandidateEvidenceV1;
import com.mapletct.ftmes.bpi.infrastructure.candidate.CandidateKafkaRecordProcessor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
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
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
        registry.add("bpi.candidate-kafka.allowed-tenant-ids", () -> "*");
        registry.add("bpi.candidate-kafka.allowed-plant-ids", () -> "*");
        registry.add("bpi.candidate-kafka.allowed-line-ids", () -> "*");
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JdbcTemplate jdbc;
    @Autowired CandidateKafkaRecordProcessor candidateKafkaRecordProcessor;

    private String tenantId;
    private UUID topologyId;
    private UUID ruleId;
    private UUID endRuleId;
    private UUID candidateKey;
    private String ingestPayload;

    @BeforeEach
    void seedPublishedRuleAndTopology() throws Exception {
        tenantId = "ADP_E2E_BPI_" + UUID.randomUUID().toString().replace("-", "");
        topologyId = UUID.randomUUID();
        ruleId = UUID.randomUUID();
        endRuleId = UUID.randomUUID();
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
                INSERT INTO bpi.bpi_rule_versions
                    (id, tenant_id, rule_code, version, topology_version_id, state, checksum, definition, created_by)
                VALUES (?, ?, 'RULE-S07-END', '1.2.0', ?, 'PUBLISHED', 'end-rule-checksum', '{}'::jsonb, 'acceptance')
                """, endRuleId, tenantId, topologyId);
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
        jdbc.update("DELETE FROM bpi.bpi_batch_force_close_tasks WHERE tenant_id = ?", tenantId);
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
    void realApiRejectsCandidateOnceWithoutCreatingShadowBatch() throws Exception {
        String ingestToken = token(
                tenantId, List.of("BPI_EVENT_INGEST"), List.of("PLANT-01"), List.of("LINE-S07-01"));
        MvcResult ingested = mockMvc.perform(post("/internal/bpi/v1/candidates")
                        .header("Authorization", "Bearer " + ingestToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ingestPayload))
                .andExpect(status().isCreated())
                .andReturn();
        UUID candidateId = UUID.fromString(objectMapper.readTree(
                ingested.getResponse().getContentAsString()).path("data").path("id").asText());

        String shiftToken = token(
                tenantId, List.of("BPI_SHIFT_LEAD"), List.of("PLANT-01"), List.of("LINE-S07-01"));
        String commandBody = objectMapper.writeValueAsString(Map.of("reason", "现场确认该边界为流量波动误判"));
        mockMvc.perform(post("/bpi/v1/candidates/{id}/reject", candidateId)
                        .header("Authorization", "Bearer " + shiftToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(commandBody))
                .andExpect(status().is(428));

        String idempotencyKey = "reject-" + candidateKey;
        mockMvc.perform(post("/bpi/v1/candidates/{id}/reject", candidateId)
                        .header("Authorization", "Bearer " + shiftToken)
                        .header("Idempotency-Key", idempotencyKey)
                        .header("If-Match", "1")
                        .header("X-Trace-Id", "TRACE-REJECT-" + candidateKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(commandBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("REJECTED"))
                .andExpect(jsonPath("$.data.revision").value(2))
                .andExpect(jsonPath("$.data.batchId").doesNotExist());

        mockMvc.perform(post("/bpi/v1/candidates/{id}/reject", candidateId)
                        .header("Authorization", "Bearer " + shiftToken)
                        .header("Idempotency-Key", idempotencyKey)
                        .header("If-Match", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(commandBody))
                .andExpect(status().isOk())
                .andExpect(header().string("Idempotent-Replay", "true"))
                .andExpect(jsonPath("$.data.state").value("REJECTED"));

        mockMvc.perform(post("/bpi/v1/candidates/{id}/confirm", candidateId)
                        .header("Authorization", "Bearer " + shiftToken)
                        .header("Idempotency-Key", idempotencyKey)
                        .header("If-Match", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(commandBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("reused")));

        mockMvc.perform(post("/bpi/v1/candidates/{id}/confirm", candidateId)
                        .header("Authorization", "Bearer " + shiftToken)
                        .header("Idempotency-Key", "confirm-after-reject-" + candidateKey)
                        .header("If-Match", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("reason", "过期确认不得成功"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.currentRevision").value(2));

        assertThat(jdbc.queryForObject("""
                SELECT state || '|' || revision || '|' || reviewed_by || '|' || review_reason
                  FROM bpi.bpi_batch_candidates
                 WHERE tenant_id = ? AND id = ?
                """, String.class, tenantId, candidateId))
                .isEqualTo("REJECTED|2|acceptance-user|现场确认该边界为流量波动误判");
        assertThat(jdbc.queryForObject("""
                SELECT action || '|' || before_revision || '|' || after_revision
                  FROM bpi.bpi_audit_events
                 WHERE tenant_id = ? AND object_id = ?
                """, String.class, tenantId, candidateId))
                .isEqualTo("CANDIDATE_REJECTED|1|2");
        assertThat(count("bpi_batch_instances")).isZero();
        assertThat(count("bpi_batch_state_events")).isZero();
        assertThat(count("bpi_boundary_evidence")).isZero();
        assertThat(count("bpi_audit_events")).isEqualTo(1);
        assertThat(count("bpi_api_idempotency")).isEqualTo(1);
    }

    @Test
    void batchLifecycleSuspendsAndResumesWithRevisionIdempotencyAndAuditEvidence() throws Exception {
        String ingestToken = token(
                tenantId, List.of("BPI_EVENT_INGEST"), List.of("PLANT-01"), List.of("LINE-S07-01"));
        MvcResult ingested = mockMvc.perform(post("/internal/bpi/v1/candidates")
                        .header("Authorization", "Bearer " + ingestToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ingestPayload))
                .andExpect(status().isCreated())
                .andReturn();
        UUID candidateId = UUID.fromString(objectMapper.readTree(
                ingested.getResponse().getContentAsString()).path("data").path("id").asText());

        String shiftToken = token(
                tenantId, List.of("BPI_SHIFT_LEAD"), List.of("PLANT-01"), List.of("LINE-S07-01"));
        MvcResult confirmed = mockMvc.perform(post("/bpi/v1/candidates/{id}/confirm", candidateId)
                        .header("Authorization", "Bearer " + shiftToken)
                        .header("Idempotency-Key", "lifecycle-confirm-" + candidateKey)
                        .header("If-Match", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("reason", "创建批次用于状态闭环验收"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.batch.state").value("ACTIVE"))
                .andExpect(jsonPath("$.data.batch.revision").value(1))
                .andReturn();
        UUID batchId = UUID.fromString(objectMapper.readTree(confirmed.getResponse().getContentAsString())
                .path("data").path("batch").path("id").asText());

        String suspendBody = objectMapper.writeValueAsString(Map.ofEntries(
                Map.entry("reason", "上游制造指令上下文已过期"),
                Map.entry("comment", "UPSTREAM_CONTEXT_STALE")));
        mockMvc.perform(post("/bpi/v1/batches/{id}/suspend", batchId)
                        .header("Authorization", "Bearer " + shiftToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(suspendBody))
                .andExpect(status().is(428));

        String suspendKey = "lifecycle-suspend-" + candidateKey;
        mockMvc.perform(post("/bpi/v1/batches/{id}/suspend", batchId)
                        .header("Authorization", "Bearer " + shiftToken)
                        .header("Idempotency-Key", suspendKey)
                        .header("If-Match", "1")
                        .header("X-Trace-Id", "TRACE-SUSPEND-" + candidateKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(suspendBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("SUSPENDED"))
                .andExpect(jsonPath("$.data.revision").value(2));
        mockMvc.perform(get("/bpi/v1/lines/LINE-S07-01/current-state")
                        .header("Authorization", "Bearer " + shiftToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("BLOCKED"))
                .andExpect(jsonPath("$.data.currentBatchId").value(batchId.toString()));
        mockMvc.perform(get("/bpi/v1/overview")
                        .header("Authorization", "Bearer " + shiftToken)
                        .param("plantId", "PLANT-01")
                        .param("onlyAbnormal", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].status").value("BLOCKED"));

        mockMvc.perform(post("/bpi/v1/batches/{id}/suspend", batchId)
                        .header("Authorization", "Bearer " + shiftToken)
                        .header("Idempotency-Key", suspendKey)
                        .header("If-Match", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(suspendBody))
                .andExpect(status().isOk())
                .andExpect(header().string("Idempotent-Replay", "true"))
                .andExpect(jsonPath("$.data.state").value("SUSPENDED"))
                .andExpect(jsonPath("$.data.revision").value(2));

        String resumeBody = objectMapper.writeValueAsString(Map.ofEntries(
                Map.entry("reason", "上游制造指令上下文已恢复"),
                Map.entry("comment", "WOM_CONTEXT_RECOVERED")));
        mockMvc.perform(post("/bpi/v1/batches/{id}/resume", batchId)
                        .header("Authorization", "Bearer " + shiftToken)
                        .header("Idempotency-Key", suspendKey)
                        .header("If-Match", "2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resumeBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("reused")));

        mockMvc.perform(post("/bpi/v1/batches/{id}/suspend", batchId)
                        .header("Authorization", "Bearer " + shiftToken)
                        .header("Idempotency-Key", "repeat-suspend-" + candidateKey)
                        .header("If-Match", "2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(suspendBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.currentRevision").value(2));

        mockMvc.perform(post("/bpi/v1/batches/{id}/resume", batchId)
                        .header("Authorization", "Bearer " + shiftToken)
                        .header("Idempotency-Key", "stale-resume-" + candidateKey)
                        .header("If-Match", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resumeBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.currentRevision").value(2));

        String resumeKey = "lifecycle-resume-" + candidateKey;
        mockMvc.perform(post("/bpi/v1/batches/{id}/resume", batchId)
                        .header("Authorization", "Bearer " + shiftToken)
                        .header("Idempotency-Key", resumeKey)
                        .header("If-Match", "2")
                        .header("X-Trace-Id", "TRACE-RESUME-" + candidateKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resumeBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("ACTIVE"))
                .andExpect(jsonPath("$.data.revision").value(3));

        mockMvc.perform(post("/bpi/v1/batches/{id}/resume", batchId)
                        .header("Authorization", "Bearer " + shiftToken)
                        .header("Idempotency-Key", resumeKey)
                        .header("If-Match", "2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resumeBody))
                .andExpect(status().isOk())
                .andExpect(header().string("Idempotent-Replay", "true"))
                .andExpect(jsonPath("$.data.state").value("ACTIVE"))
                .andExpect(jsonPath("$.data.revision").value(3));
        mockMvc.perform(get("/bpi/v1/lines/LINE-S07-01/current-state")
                        .header("Authorization", "Bearer " + shiftToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RUNNING"))
                .andExpect(jsonPath("$.data.currentBatchId").value(batchId.toString()));
        mockMvc.perform(get("/bpi/v1/overview")
                        .header("Authorization", "Bearer " + shiftToken)
                        .param("plantId", "PLANT-01")
                        .param("onlyAbnormal", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));

        String wrongLineToken = token(
                tenantId, List.of("BPI_SHIFT_LEAD"), List.of("PLANT-01"), List.of("LINE-OTHER"));
        mockMvc.perform(post("/bpi/v1/batches/{id}/suspend", batchId)
                        .header("Authorization", "Bearer " + wrongLineToken)
                        .header("Idempotency-Key", "wrong-scope-" + candidateKey)
                        .header("If-Match", "3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(suspendBody))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("scope")));

        assertThat(jdbc.queryForObject("""
                SELECT state || '|' || revision
                  FROM bpi.bpi_batch_instances
                 WHERE tenant_id = ? AND id = ?
                """, String.class, tenantId, batchId)).isEqualTo("ACTIVE|3");
        assertThat(jdbc.queryForList("""
                SELECT revision || '|' || action || '|' || coalesce(from_state, '-') || '|' || to_state AS event
                  FROM bpi.bpi_batch_state_events
                 WHERE tenant_id = ? AND batch_id = ?
                 ORDER BY revision
                """, String.class, tenantId, batchId))
                .containsExactly(
                        "1|SHADOW_BATCH_CREATED|-|ACTIVE",
                        "2|BATCH_SUSPENDED|ACTIVE|SUSPENDED",
                        "3|BATCH_RESUMED|SUSPENDED|ACTIVE");
        assertThat(jdbc.queryForList("""
                SELECT action || '|' || before_revision || '|' || after_revision AS audit
                  FROM bpi.bpi_audit_events
                 WHERE tenant_id = ? AND object_type = 'BATCH_INSTANCE' AND object_id = ?
                 ORDER BY after_revision
                """, String.class, tenantId, batchId))
                .containsExactly("BATCH_SUSPENDED|1|2", "BATCH_RESUMED|2|3");
        assertThat(count("bpi_api_idempotency")).isEqualTo(3);
    }

    @Test
    void forceCloseRequiresIndependentApprovalAndPersistsRecoverableTaskAndAuditTrail() throws Exception {
        String ingestToken = token(
                tenantId, List.of("BPI_EVENT_INGEST"), List.of("PLANT-01"), List.of("LINE-S07-01"));
        MvcResult ingested = mockMvc.perform(post("/internal/bpi/v1/candidates")
                        .header("Authorization", "Bearer " + ingestToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ingestPayload))
                .andExpect(status().isCreated())
                .andReturn();
        UUID candidateId = UUID.fromString(objectMapper.readTree(
                ingested.getResponse().getContentAsString()).path("data").path("id").asText());

        String requesterId = "force-close-requester";
        String approverId = "force-close-approver";
        String requesterToken = token(
                requesterId, tenantId, List.of("BPI_SHIFT_LEAD"),
                List.of("PLANT-01"), List.of("LINE-S07-01"));
        MvcResult confirmed = mockMvc.perform(post("/bpi/v1/candidates/{id}/confirm", candidateId)
                        .header("Authorization", "Bearer " + requesterToken)
                        .header("Idempotency-Key", "force-close-confirm-" + candidateKey)
                        .header("If-Match", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "reason", "创建批次用于双人强制结束验收"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.batch.state").value("ACTIVE"))
                .andExpect(jsonPath("$.data.batch.revision").value(1))
                .andReturn();
        UUID batchId = UUID.fromString(objectMapper.readTree(confirmed.getResponse().getContentAsString())
                .path("data").path("batch").path("id").asText());

        Instant boundaryTime = Instant.ofEpochMilli(Instant.now().minusSeconds(30).toEpochMilli());
        String requestBody = objectMapper.writeValueAsString(Map.ofEntries(
                Map.entry("reason", "自动结束边界缺失，申请人工强制结束"),
                Map.entry("comment", "ADP_E2E_FORCE_CLOSE_REQUEST"),
                Map.entry("boundaryTime", boundaryTime.toString()),
                Map.entry("approvalMode", "REQUEST")));
        mockMvc.perform(post("/bpi/v1/batches/{id}/force-close", batchId)
                        .header("Authorization", "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().is(428));

        String requestKey = "force-close-request-" + candidateKey;
        MvcResult requested = mockMvc.perform(post("/bpi/v1/batches/{id}/force-close", batchId)
                        .header("Authorization", "Bearer " + requesterToken)
                        .header("Idempotency-Key", requestKey)
                        .header("If-Match", "1")
                        .header("X-Trace-Id", "TRACE-FORCE-CLOSE-REQUEST-" + candidateKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.state").value("PENDING_APPROVAL"))
                .andExpect(jsonPath("$.data.revision").value(1))
                .andExpect(jsonPath("$.data.batchRevision").value(2))
                .andExpect(jsonPath("$.data.sourceState").value("ACTIVE"))
                .andExpect(jsonPath("$.data.requestedBy").value(requesterId))
                .andReturn();
        JsonNode requestedBody = objectMapper.readTree(requested.getResponse().getContentAsString());
        UUID taskId = UUID.fromString(requestedBody.path("data").path("taskId").asText());

        mockMvc.perform(post("/bpi/v1/batches/{id}/force-close", batchId)
                        .header("Authorization", "Bearer " + requesterToken)
                        .header("Idempotency-Key", requestKey)
                        .header("If-Match", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isAccepted())
                .andExpect(header().string("Idempotent-Replay", "true"))
                .andExpect(jsonPath("$.data.taskId").value(taskId.toString()));
        mockMvc.perform(get("/bpi/v1/batches/{id}/force-close", batchId)
                        .header("Authorization", "Bearer " + requesterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.taskId").value(taskId.toString()))
                .andExpect(jsonPath("$.data.state").value("PENDING_APPROVAL"))
                .andExpect(jsonPath("$.data.batchRevision").value(2));

        String blockedSuspendBody = objectMapper.writeValueAsString(Map.of(
                "reason", "验证待审批期间禁止运行态变更"));
        mockMvc.perform(post("/bpi/v1/batches/{id}/suspend", batchId)
                        .header("Authorization", "Bearer " + requesterToken)
                        .header("Idempotency-Key", "force-close-block-suspend-" + candidateKey)
                        .header("If-Match", "2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(blockedSuspendBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(
                        org.hamcrest.Matchers.containsString("pending force-close")));

        UUID endCandidateKey = UUID.randomUUID();
        MvcResult endIngested = mockMvc.perform(post("/internal/bpi/v1/candidates")
                        .header("Authorization", "Bearer " + ingestToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(endCandidatePayload(
                                endCandidateKey, Instant.parse("2026-07-12T08:29:40Z"))))
                .andExpect(status().isCreated())
                .andReturn();
        UUID endCandidateId = UUID.fromString(objectMapper.readTree(
                endIngested.getResponse().getContentAsString()).path("data").path("id").asText());
        mockMvc.perform(post("/bpi/v1/candidates/{id}/confirm", endCandidateId)
                        .header("Authorization", "Bearer " + requesterToken)
                        .header("Idempotency-Key", "force-close-block-end-" + endCandidateKey)
                        .header("If-Match", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "reason", "验证待审批期间禁止普通结束边界关闭批次"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(
                        org.hamcrest.Matchers.containsString("changed before END")));
        assertThat(jdbc.queryForObject("""
                SELECT state
                  FROM bpi.bpi_batch_candidates
                 WHERE tenant_id = ? AND id = ?
                """, String.class, tenantId, endCandidateId)).isEqualTo("PENDING");

        String approvalBody = objectMapper.writeValueAsString(Map.ofEntries(
                Map.entry("reason", "管理员复核现场记录后批准强制结束"),
                Map.entry("comment", "ADP_E2E_FORCE_CLOSE_APPROVE"),
                Map.entry("boundaryTime", boundaryTime.toString()),
                Map.entry("approvalMode", "APPROVE")));
        String samePersonAdminToken = token(
                requesterId, tenantId, List.of("BPI_ADMIN"),
                List.of("PLANT-01"), List.of("LINE-S07-01"));
        mockMvc.perform(post("/bpi/v1/batches/{id}/force-close", batchId)
                        .header("Authorization", "Bearer " + samePersonAdminToken)
                        .header("Idempotency-Key", "force-close-self-approve-" + candidateKey)
                        .header("If-Match", "2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(approvalBody))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value(
                        org.hamcrest.Matchers.containsString("different administrator")));

        String approverToken = token(
                approverId, tenantId, List.of("BPI_ADMIN"),
                List.of("PLANT-01"), List.of("LINE-S07-01"));
        String changedBoundaryBody = objectMapper.writeValueAsString(Map.ofEntries(
                Map.entry("reason", "尝试篡改结束边界"),
                Map.entry("boundaryTime", boundaryTime.plusSeconds(1).toString()),
                Map.entry("approvalMode", "APPROVE")));
        mockMvc.perform(post("/bpi/v1/batches/{id}/force-close", batchId)
                        .header("Authorization", "Bearer " + approverToken)
                        .header("Idempotency-Key", "force-close-changed-boundary-" + candidateKey)
                        .header("If-Match", "2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(changedBoundaryBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(
                        org.hamcrest.Matchers.containsString("must match")));

        String approvalKey = "force-close-approve-" + candidateKey;
        mockMvc.perform(post("/bpi/v1/batches/{id}/force-close", batchId)
                        .header("Authorization", "Bearer " + approverToken)
                        .header("Idempotency-Key", approvalKey)
                        .header("If-Match", "2")
                        .header("X-Trace-Id", "TRACE-FORCE-CLOSE-APPROVE-" + candidateKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(approvalBody))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.taskId").value(taskId.toString()))
                .andExpect(jsonPath("$.data.state").value("COMPLETED"))
                .andExpect(jsonPath("$.data.revision").value(2))
                .andExpect(jsonPath("$.data.batchRevision").value(3))
                .andExpect(jsonPath("$.data.requestedBy").value(requesterId))
                .andExpect(jsonPath("$.data.decidedBy").value(approverId));
        mockMvc.perform(post("/bpi/v1/batches/{id}/force-close", batchId)
                        .header("Authorization", "Bearer " + approverToken)
                        .header("Idempotency-Key", approvalKey)
                        .header("If-Match", "2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(approvalBody))
                .andExpect(status().isAccepted())
                .andExpect(header().string("Idempotent-Replay", "true"))
                .andExpect(jsonPath("$.data.state").value("COMPLETED"));

        mockMvc.perform(get("/bpi/v1/batches/{id}", batchId)
                        .header("Authorization", "Bearer " + requesterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("CLOSED_RAW"))
                .andExpect(jsonPath("$.data.revision").value(3))
                .andExpect(jsonPath("$.data.endTime").value(boundaryTime.toString()));
        mockMvc.perform(get("/bpi/v1/lines/LINE-S07-01/current-state")
                        .header("Authorization", "Bearer " + requesterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IDLE"))
                .andExpect(jsonPath("$.data.currentBatchId").doesNotExist());

        assertThat(jdbc.queryForObject("""
                SELECT state || '|' || revision || '|' || requested_by || '|' || decided_by
                  FROM bpi.bpi_batch_force_close_tasks
                 WHERE tenant_id = ? AND id = ?
                """, String.class, tenantId, taskId))
                .isEqualTo("COMPLETED|2|" + requesterId + "|" + approverId);
        assertThat(jdbc.queryForObject("""
                SELECT state || '|' || revision
                  FROM bpi.bpi_batch_instances
                 WHERE tenant_id = ? AND id = ?
                """, String.class, tenantId, batchId)).isEqualTo("CLOSED_RAW|3");
        assertThat(jdbc.queryForObject("""
                SELECT floor(extract(epoch from end_time) * 1000)::bigint
                  FROM bpi.bpi_batch_instances
                 WHERE tenant_id = ? AND id = ?
                """, Long.class, tenantId, batchId)).isEqualTo(boundaryTime.toEpochMilli());
        assertThat(jdbc.queryForList("""
                SELECT revision || '|' || action || '|' || coalesce(from_state, '-') || '|' || to_state AS event
                  FROM bpi.bpi_batch_state_events
                 WHERE tenant_id = ? AND batch_id = ?
                 ORDER BY revision
                """, String.class, tenantId, batchId))
                .containsExactly(
                        "1|SHADOW_BATCH_CREATED|-|ACTIVE",
                        "2|BATCH_FORCE_CLOSE_REQUESTED|ACTIVE|ACTIVE",
                        "3|BATCH_FORCE_CLOSED|ACTIVE|CLOSED_RAW");
        assertThat(jdbc.queryForList("""
                SELECT action || '|' || before_revision || '|' || after_revision AS audit
                  FROM bpi.bpi_audit_events
                 WHERE tenant_id = ? AND object_type = 'BATCH_INSTANCE' AND object_id = ?
                 ORDER BY after_revision
                """, String.class, tenantId, batchId))
                .containsExactly(
                        "BATCH_FORCE_CLOSE_REQUESTED|1|2",
                        "BATCH_FORCE_CLOSED|2|3");
        assertThat(count("bpi_batch_force_close_tasks")).isEqualTo(1);
        assertThat(count("bpi_batch_instances")).isEqualTo(1);
        assertThat(count("bpi_batch_state_events")).isEqualTo(3);
        assertThat(count("bpi_audit_events")).isEqualTo(3);
        assertThat(count("bpi_api_idempotency")).isEqualTo(3);
    }

    @Test
    void endCandidateClosesTheMatchingShadowBatchWithEndEvidenceAndRawClosureAudit() throws Exception {
        String ingestToken = token(
                tenantId, List.of("BPI_EVENT_INGEST"), List.of("PLANT-01"), List.of("LINE-S07-01"));
        MvcResult startIngested = mockMvc.perform(post("/internal/bpi/v1/candidates")
                        .header("Authorization", "Bearer " + ingestToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ingestPayload))
                .andExpect(status().isCreated())
                .andReturn();
        UUID startCandidateId = UUID.fromString(objectMapper.readTree(
                startIngested.getResponse().getContentAsString()).path("data").path("id").asText());

        String shiftToken = token(
                tenantId, List.of("BPI_SHIFT_LEAD"), List.of("PLANT-01"), List.of("LINE-S07-01"));
        MvcResult started = mockMvc.perform(post("/bpi/v1/candidates/{id}/confirm", startCandidateId)
                        .header("Authorization", "Bearer " + shiftToken)
                        .header("Idempotency-Key", "end-flow-start-" + candidateKey)
                        .header("If-Match", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("reason", "确认启动边界并建立待结束批次"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.batch.state").value("ACTIVE"))
                .andReturn();
        UUID batchId = UUID.fromString(objectMapper.readTree(started.getResponse().getContentAsString())
                .path("data").path("batch").path("id").asText());

        UUID endCandidateKey = UUID.randomUUID();
        Instant endBoundaryTime = Instant.parse("2026-07-12T08:29:40Z");
        MvcResult endIngested = mockMvc.perform(post("/internal/bpi/v1/candidates")
                        .header("Authorization", "Bearer " + ingestToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(endCandidatePayload(endCandidateKey, endBoundaryTime)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.boundaryType").value("END"))
                .andExpect(jsonPath("$.data.state").value("PENDING"))
                .andReturn();
        UUID endCandidateId = UUID.fromString(objectMapper.readTree(
                endIngested.getResponse().getContentAsString()).path("data").path("id").asText());

        String endBody = objectMapper.writeValueAsString(Map.of(
                "reason", "流量归零且泵阀路径停止，确认结束边界"));
        mockMvc.perform(post("/bpi/v1/candidates/{id}/confirm", endCandidateId)
                        .header("Authorization", "Bearer " + shiftToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(endBody))
                .andExpect(status().is(428));

        String endKey = "end-flow-confirm-" + endCandidateKey;
        MvcResult closed = mockMvc.perform(post("/bpi/v1/candidates/{id}/confirm", endCandidateId)
                        .header("Authorization", "Bearer " + shiftToken)
                        .header("Idempotency-Key", endKey)
                        .header("If-Match", "1")
                        .header("X-Trace-Id", "TRACE-END-" + endCandidateKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(endBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.candidate.state").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.candidate.revision").value(2))
                .andExpect(jsonPath("$.data.candidate.batchId").value(batchId.toString()))
                .andExpect(jsonPath("$.data.batch.id").value(batchId.toString()))
                .andExpect(jsonPath("$.data.batch.state").value("CLOSED_RAW"))
                .andExpect(jsonPath("$.data.batch.revision").value(2))
                .andExpect(jsonPath("$.data.batch.endTime").value(endBoundaryTime.toString()))
                .andReturn();

        mockMvc.perform(post("/bpi/v1/candidates/{id}/confirm", endCandidateId)
                        .header("Authorization", "Bearer " + shiftToken)
                        .header("Idempotency-Key", endKey)
                        .header("If-Match", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(endBody))
                .andExpect(status().isOk())
                .andExpect(header().string("Idempotent-Replay", "true"))
                .andExpect(jsonPath("$.data.batch.state").value("CLOSED_RAW"))
                .andExpect(jsonPath("$.data.batch.revision").value(2));

        mockMvc.perform(get("/bpi/v1/batches/{id}/evidence", batchId)
                        .header("Authorization", "Bearer " + shiftToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.start.length()").value(1))
                .andExpect(jsonPath("$.data.end.length()").value(1))
                .andExpect(jsonPath("$.data.end[0].signal").value("instantFlowBelowStopThreshold"));
        mockMvc.perform(get("/bpi/v1/batches/{id}/timeline", batchId)
                        .header("Authorization", "Bearer " + shiftToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].action").value("SHADOW_BATCH_CREATED"))
                .andExpect(jsonPath("$.data[1].action").value("END_BOUNDARY_CONFIRMED"));
        mockMvc.perform(get("/bpi/v1/lines/LINE-S07-01/current-state")
                        .header("Authorization", "Bearer " + shiftToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IDLE"))
                .andExpect(jsonPath("$.data.currentBatchId").doesNotExist());

        assertThat(objectMapper.readTree(closed.getResponse().getContentAsString())
                .path("data").path("batch").path("endTime").asText()).isEqualTo(endBoundaryTime.toString());
        assertThat(jdbc.queryForObject("""
                SELECT state || '|' || revision || '|' || to_char(end_time AT TIME ZONE 'UTC', 'YYYY-MM-DD"T"HH24:MI:SS"Z"')
                  FROM bpi.bpi_batch_instances
                 WHERE tenant_id = ? AND id = ?
                """, String.class, tenantId, batchId))
                .isEqualTo("CLOSED_RAW|2|2026-07-12T08:29:40Z");
        assertThat(jdbc.queryForList("""
                SELECT revision || '|' || action || '|' || coalesce(from_state, '-') || '|' || to_state AS event
                  FROM bpi.bpi_batch_state_events
                 WHERE tenant_id = ? AND batch_id = ?
                 ORDER BY revision
                """, String.class, tenantId, batchId))
                .containsExactly(
                        "1|SHADOW_BATCH_CREATED|-|ACTIVE",
                        "2|END_BOUNDARY_CONFIRMED|ACTIVE|CLOSED_RAW");
        assertThat(jdbc.queryForList("""
                SELECT action
                 FROM bpi.bpi_audit_events
                 WHERE tenant_id = ?
                 ORDER BY action
                """, String.class, tenantId))
                .containsExactly("BATCH_CLOSED_RAW", "CANDIDATE_CONFIRMED", "END_CANDIDATE_CONFIRMED");
        assertThat(count("bpi_batch_candidates")).isEqualTo(2);
        assertThat(count("bpi_inbox_events")).isEqualTo(2);
        assertThat(count("bpi_batch_instances")).isEqualTo(1);
        assertThat(count("bpi_batch_state_events")).isEqualTo(2);
        assertThat(count("bpi_boundary_evidence")).isEqualTo(2);
        assertThat(count("bpi_audit_events")).isEqualTo(3);
        assertThat(count("bpi_api_idempotency")).isEqualTo(2);
    }

    @Test
    void secondStartCandidateCannotCreateAnotherOpenBatchOnTheSameLine() throws Exception {
        String ingestToken = token(
                tenantId, List.of("BPI_EVENT_INGEST"), List.of("PLANT-01"), List.of("LINE-S07-01"));
        MvcResult firstIngested = mockMvc.perform(post("/internal/bpi/v1/candidates")
                        .header("Authorization", "Bearer " + ingestToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ingestPayload))
                .andExpect(status().isCreated())
                .andReturn();
        UUID firstCandidateId = UUID.fromString(objectMapper.readTree(
                firstIngested.getResponse().getContentAsString()).path("data").path("id").asText());

        String shiftToken = token(
                tenantId, List.of("BPI_SHIFT_LEAD"), List.of("PLANT-01"), List.of("LINE-S07-01"));
        mockMvc.perform(post("/bpi/v1/candidates/{id}/confirm", firstCandidateId)
                        .header("Authorization", "Bearer " + shiftToken)
                        .header("Idempotency-Key", "single-open-first-" + candidateKey)
                        .header("If-Match", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("reason", "确认首个启动边界"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.batch.state").value("ACTIVE"));

        UUID duplicateKey = UUID.randomUUID();
        var duplicateStart = (com.fasterxml.jackson.databind.node.ObjectNode)
                objectMapper.readTree(ingestPayload);
        duplicateStart.put("eventId", "DUPLICATE-START-EVENT-" + duplicateKey);
        duplicateStart.put("candidateKey", duplicateKey.toString());
        duplicateStart.put("boundaryTime", "2026-07-12T08:00:40Z");
        var duplicateEvidence = (com.fasterxml.jackson.databind.node.ObjectNode)
                duplicateStart.withArray("evidence").get(0);
        duplicateEvidence.put("eventId", "DUPLICATE-START-EVIDENCE-" + duplicateKey);
        duplicateEvidence.put("eventTime", "2026-07-12T08:00:40Z");
        MvcResult secondIngested = mockMvc.perform(post("/internal/bpi/v1/candidates")
                        .header("Authorization", "Bearer " + ingestToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(duplicateStart)))
                .andExpect(status().isCreated())
                .andReturn();
        UUID secondCandidateId = UUID.fromString(objectMapper.readTree(
                secondIngested.getResponse().getContentAsString()).path("data").path("id").asText());

        mockMvc.perform(post("/bpi/v1/candidates/{id}/confirm", secondCandidateId)
                        .header("Authorization", "Bearer " + shiftToken)
                        .header("Idempotency-Key", "single-open-second-" + duplicateKey)
                        .header("If-Match", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("reason", "尝试创建第二个开放批次"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("open batch")));

        assertThat(jdbc.queryForObject("""
                SELECT count(*)
                  FROM bpi.bpi_batch_instances
                 WHERE tenant_id = ? AND line_id = ? AND state IN ('ACTIVE', 'SUSPENDED')
                """, Long.class, tenantId, "LINE-S07-01")).isEqualTo(1L);
        assertThat(jdbc.queryForObject("""
                SELECT state
                  FROM bpi.bpi_batch_candidates
                 WHERE tenant_id = ? AND id = ?
                """, String.class, tenantId, secondCandidateId)).isEqualTo("PENDING");
        assertThat(count("bpi_batch_instances")).isEqualTo(1);
        assertThat(count("bpi_batch_state_events")).isEqualTo(1);
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

    @Test
    void kafkaCandidateRecordPersistsOnceAcrossAtLeastOnceRedelivery() {
        String evidenceEventId = "ADP_E2E_BPI_KAFKA_FLOW_" + candidateKey;
        String orderId = "ADP_E2E_BPI_KAFKA_ORDER_" + candidateKey;
        Instant boundaryTime = Instant.parse("2026-07-12T08:30:00Z");
        BatchCandidateV1 event = candidateEvent(evidenceEventId, orderId, boundaryTime);
        ConsumerRecord<byte[], byte[]> record = new ConsumerRecord<>(
                "bpi.batch.candidate.v1",
                2,
                42L,
                (event.getLineId() + "|" + event.getRuleCode()).getBytes(StandardCharsets.UTF_8),
                event.toByteArray());
        record.headers()
                .add("event_id", event.getEventId().getBytes(StandardCharsets.UTF_8))
                .add("candidate_key", event.getCandidateKey().getBytes(StandardCharsets.UTF_8))
                .add("tenant_id", event.getTenantId().getBytes(StandardCharsets.UTF_8))
                .add("schema_version", "v1".getBytes(StandardCharsets.UTF_8));

        candidateKafkaRecordProcessor.process(record);
        candidateKafkaRecordProcessor.process(record);

        assertThat(count("bpi_inbox_events")).isEqualTo(1);
        assertThat(count("bpi_batch_candidates")).isEqualTo(1);
        assertThat(jdbc.queryForObject("""
                SELECT evidence->0->>'source'
                  FROM bpi.bpi_batch_candidates
                 WHERE tenant_id = ? AND candidate_key = ?
                """, String.class, tenantId, UUID.fromString(event.getCandidateKey())))
                .isEqualTo("bpi-stream-engine");
    }

    @Test
    void delayedKafkaCandidatePersistsAfterRuleAndTopologyRetirement() {
        String evidenceEventId = "ADP_E2E_BPI_RETIRED_FLOW_" + candidateKey;
        String orderId = "ADP_E2E_BPI_RETIRED_ORDER_" + candidateKey;
        BatchCandidateV1 event = candidateEvent(
                evidenceEventId, orderId, Instant.parse("2026-07-12T08:45:00Z"));
        ConsumerRecord<byte[], byte[]> record = candidateRecord(event, 43L);

        jdbc.update("UPDATE bpi.bpi_rule_versions SET state = 'RETIRED' WHERE id = ?", ruleId);
        jdbc.update("UPDATE bpi.bpi_topology_versions SET state = 'RETIRED' WHERE id = ?", topologyId);

        candidateKafkaRecordProcessor.process(record);
        candidateKafkaRecordProcessor.process(record);

        assertThat(count("bpi_inbox_events")).isEqualTo(1);
        assertThat(count("bpi_batch_candidates")).isEqualTo(1);
        assertThat(jdbc.queryForObject("""
                SELECT r.state
                  FROM bpi.bpi_batch_candidates c
                  JOIN bpi.bpi_rule_versions r ON r.id = c.rule_version_id
                 WHERE c.tenant_id = ? AND c.candidate_key = ?
                """, String.class, tenantId, UUID.fromString(event.getCandidateKey())))
                .isEqualTo("RETIRED");
    }

    @Test
    void kafkaCandidateCannotReferenceNeverPublishedDraftRule() {
        String evidenceEventId = "ADP_E2E_BPI_DRAFT_FLOW_" + candidateKey;
        String orderId = "ADP_E2E_BPI_DRAFT_ORDER_" + candidateKey;
        BatchCandidateV1 event = candidateEvent(
                evidenceEventId, orderId, Instant.parse("2026-07-12T09:00:00Z"));
        ConsumerRecord<byte[], byte[]> record = candidateRecord(event, 44L);

        jdbc.update("UPDATE bpi.bpi_rule_versions SET state = 'DRAFT' WHERE id = ?", ruleId);

        assertThatThrownBy(() -> candidateKafkaRecordProcessor.process(record))
                .isInstanceOf(BpiValidationException.class)
                .hasMessageContaining("Published topology/rule version pair does not exist");
        assertThat(count("bpi_inbox_events")).isZero();
        assertThat(count("bpi_batch_candidates")).isZero();
    }

    private ConsumerRecord<byte[], byte[]> candidateRecord(BatchCandidateV1 event, long offset) {
        ConsumerRecord<byte[], byte[]> record = new ConsumerRecord<>(
                "bpi.batch.candidate.v1",
                2,
                offset,
                (event.getLineId() + "|" + event.getRuleCode()).getBytes(StandardCharsets.UTF_8),
                event.toByteArray());
        record.headers()
                .add("event_id", event.getEventId().getBytes(StandardCharsets.UTF_8))
                .add("candidate_key", event.getCandidateKey().getBytes(StandardCharsets.UTF_8))
                .add("tenant_id", event.getTenantId().getBytes(StandardCharsets.UTF_8))
                .add("schema_version", "v1".getBytes(StandardCharsets.UTF_8));
        return record;
    }

    private BatchCandidateV1 candidateEvent(
            String evidenceEventId,
            String orderId,
            Instant boundaryTime) {
        String stableKey = CandidateKeyFactory.startKey(
                tenantId, "LINE-S07-01", "1.2.0", orderId, evidenceEventId);
        return BatchCandidateV1.newBuilder()
                .setEventId("ADP_E2E_BPI_KAFKA_CANDIDATE_" + candidateKey)
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
                .setEmittedAtMs(boundaryTime.plusSeconds(1).toEpochMilli())
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
                .build();
    }

    private String endCandidatePayload(UUID key, Instant boundaryTime) throws Exception {
        Map<String, Object> evidence = Map.of(
                "eventId", "EVT-END-FLOW-" + key,
                "signal", "instantFlowBelowStopThreshold",
                "classification", "QUORUM",
                "satisfied", true,
                "value", "0.2",
                "unit", "t/h",
                "quality", "GOOD",
                "eventTime", boundaryTime.toString(),
                "source", "approved-replay");
        return objectMapper.writeValueAsString(Map.ofEntries(
                Map.entry("eventId", "END-CANDIDATE-EVENT-" + key),
                Map.entry("candidateKey", key),
                Map.entry("plantId", "PLANT-01"),
                Map.entry("lineId", "LINE-S07-01"),
                Map.entry("boundaryType", "END"),
                Map.entry("orderId", "MO-20260712-001"),
                Map.entry("boundaryTime", boundaryTime.toString()),
                Map.entry("ruleCode", "RULE-S07-END"),
                Map.entry("ruleVersion", "1.2.0"),
                Map.entry("topologyCode", "TOPO-S07"),
                Map.entry("topologyVersion", "3"),
                Map.entry("confidence", 0.96),
                Map.entry("evidence", List.of(evidence)),
                Map.entry("missingSignals", List.of())));
    }

    private long count(String table) {
        Long result = jdbc.queryForObject(
                "SELECT count(*) FROM bpi." + table + " WHERE tenant_id = ?", Long.class, tenantId);
        return result == null ? 0 : result;
    }

    private String token(String tenant, List<String> roles, List<String> plants, List<String> lines) throws Exception {
        return token("acceptance-user", tenant, roles, plants, lines);
    }

    private String token(
            String subject,
            String tenant,
            List<String> roles,
            List<String> plants,
            List<String> lines) throws Exception {
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer("ft-mes-adapter")
                .audience("bpi-service")
                .subject(subject)
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
