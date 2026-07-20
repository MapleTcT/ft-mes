package com.mapletct.ftmes.bpi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryRulePublicationV1;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryRuleApplicationStatusV1;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryRuleApplicationV1;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryRuleRuntimeReadinessStatusV1;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryRuleRuntimeReadinessV1;
import com.mapletct.ftmes.bpi.application.ActorContext;
import com.mapletct.ftmes.bpi.application.Checksums;
import com.mapletct.ftmes.bpi.application.RuleApplicationReceiptService;
import com.mapletct.ftmes.bpi.application.RuleRuntimeReadinessReceiptService;
import com.mapletct.ftmes.bpi.application.error.BpiConflictException;
import com.mapletct.ftmes.bpi.domain.OutboxEventClaim;
import com.mapletct.ftmes.bpi.infrastructure.outbox.RulePublicationOutboxRepository;
import com.mapletct.ftmes.bpi.infrastructure.postgres.RulePostgresRepository;
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
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    @Autowired RulePostgresRepository ruleRepository;
    @Autowired RuleApplicationReceiptService receiptService;
    @Autowired RuleRuntimeReadinessReceiptService runtimeReadinessReceiptService;

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
        insertApprovedCalibration("flow.instant");
        insertApprovedCalibration("pump.running");
        insertCatalogSnapshot(true, boundaryTime.minusSeconds(1));
    }

    @AfterEach
    void cleanupMarker() {
        if (tenantId == null) return;
        jdbc.update("DELETE FROM bpi.bpi_audit_events WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_api_idempotency WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_inbox_events WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_outbox_events WHERE tenant_id = ?", tenantId);
        jdbc.update("UPDATE bpi.bpi_rule_versions SET latest_simulation_id = NULL WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_rule_approval_requests WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_rule_simulations WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_rule_golden_boundaries WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_telemetry_points WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_telemetry_events WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_telemetry_source_state WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_feature_flags WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_rule_versions WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_topology_versions WHERE tenant_id = ?", tenantId);
        SourceSequenceEvidenceTestFixture.cleanup(jdbc, tenantId);
        jdbc.update("DELETE FROM bpi.bpi_point_catalog_entries WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_point_catalog_snapshots WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_point_calibrations WHERE tenant_id = ?", tenantId);
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
                "reason", "提交规则审批",
                "simulationId", simulationId,
                "simulationChecksum", "bad-checksum"));
        mockMvc.perform(post("/bpi/v1/rules/{id}/submit-approval", ruleId)
                        .header("Authorization", "Bearer " + engineerToken)
                        .header("Idempotency-Key", "bad-submit-" + ruleId)
                        .header("If-Match", "2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badPublish))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("matching checksum")));
        assertThat(count("bpi_api_idempotency")).isEqualTo(1);
        assertThat(count("bpi_outbox_events")).isZero();

        byte[] publishBody = objectMapper.writeValueAsBytes(Map.of(
                "reason", "提交规则审批",
                "simulationId", simulationId,
                "simulationChecksum", simulationChecksum));
        String submitKey = "submit-rule-approval-" + ruleId;
        mockMvc.perform(post("/bpi/v1/rules/{id}/submit-approval", ruleId)
                        .header("Authorization", "Bearer " + engineerToken)
                        .header("Idempotency-Key", submitKey)
                        .header("If-Match", "2")
                        .header("X-Trace-Id", "TRACE-SUBMIT-" + ruleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(publishBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("PENDING_APPROVAL"))
                .andExpect(jsonPath("$.data.revision").value(3))
                .andExpect(jsonPath("$.data.approvalStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.approvalSubmittedBy").value("rule-acceptance-user"));
        mockMvc.perform(post("/bpi/v1/rules/{id}/submit-approval", ruleId)
                        .header("Authorization", "Bearer " + engineerToken)
                        .header("Idempotency-Key", submitKey)
                        .header("If-Match", "2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(publishBody))
                .andExpect(status().isOk())
                .andExpect(header().string("Idempotent-Replay", "true"))
                .andExpect(jsonPath("$.data.revision").value(3));
        assertThat(jdbc.queryForObject("""
                SELECT state || '|' || revision FROM bpi.bpi_rule_versions
                 WHERE tenant_id = ? AND id = ?
                """, String.class, tenantId, ruleId)).isEqualTo("PENDING_APPROVAL|3");
        assertThat(jdbc.queryForObject("""
                SELECT state || '|' || revision || '|' || submitted_by
                  FROM bpi.bpi_rule_approval_requests
                 WHERE tenant_id = ? AND rule_version_id = ?
                """, String.class, tenantId, ruleId)).isEqualTo("PENDING|1|rule-acceptance-user");

        mockMvc.perform(post("/bpi/v1/rules/{id}/publish", ruleId)
                        .header("Authorization", "Bearer " + engineerToken)
                        .header("Idempotency-Key", "engineer-publish-denied-" + ruleId)
                        .header("If-Match", "3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(publishBody))
                .andExpect(status().isForbidden());

        String adminToken = token(
                "rule-approval-admin", tenantId, List.of("BPI_ADMIN"),
                List.of("PLANT-01"), List.of("LINE-S07-01"));

        insertCatalogSnapshot(false, boundaryTime.plusSeconds(2));
        mockMvc.perform(post("/bpi/v1/rules/{id}/publish", ruleId)
                        .header("Authorization", "Bearer " + adminToken)
                        .header("Idempotency-Key", "catalog-drift-publish-" + ruleId)
                        .header("If-Match", "3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "reason", "目录漂移时禁止发布",
                                "simulationId", simulationId,
                                "simulationChecksum", simulationChecksum))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value(
                        org.hamcrest.Matchers.containsString("POINT_DEVICE_NOT_ACTIVE")));
        assertThat(jdbc.queryForObject("""
                SELECT state || '|' || revision FROM bpi.bpi_rule_versions
                 WHERE tenant_id = ? AND id = ?
                """, String.class, tenantId, ruleId)).isEqualTo("PENDING_APPROVAL|3");
        assertThat(count("bpi_outbox_events")).isZero();
        assertThat(count("bpi_api_idempotency")).isEqualTo(2);
        insertCatalogSnapshot(true, boundaryTime.plusSeconds(3));

        String publishKey = "publish-rule-" + ruleId;
        mockMvc.perform(post("/bpi/v1/rules/{id}/publish", ruleId)
                        .header("Authorization", "Bearer " + adminToken)
                        .header("Idempotency-Key", publishKey)
                        .header("If-Match", "3")
                        .header("X-Trace-Id", "TRACE-PUBLISH-" + ruleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(publishBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("PUBLISHED"))
                .andExpect(jsonPath("$.data.revision").value(4))
                .andExpect(jsonPath("$.data.approvalStatus").value("APPROVED"))
                .andExpect(jsonPath("$.data.approvalDecidedBy").value("rule-approval-admin"))
                .andExpect(jsonPath("$.data.publicationStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.publicationAttemptCount").value(0));
        mockMvc.perform(post("/bpi/v1/rules/{id}/publish", ruleId)
                        .header("Authorization", "Bearer " + adminToken)
                        .header("Idempotency-Key", publishKey)
                        .header("If-Match", "3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(publishBody))
                .andExpect(status().isOk())
                .andExpect(header().string("Idempotent-Replay", "true"))
                .andExpect(jsonPath("$.data.revision").value(4));

        assertThat(jdbc.queryForObject("""
                SELECT state || '|' || revision FROM bpi.bpi_rule_versions
                 WHERE tenant_id = ? AND id = ?
                """, String.class, tenantId, ruleId)).isEqualTo("PUBLISHED|4");
        assertThat(jdbc.queryForObject("""
                SELECT state || '|' || revision || '|' || decided_by
                  FROM bpi.bpi_rule_approval_requests
                 WHERE tenant_id = ? AND rule_version_id = ?
                """, String.class, tenantId, ruleId)).isEqualTo("APPROVED|2|rule-approval-admin");
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
        assertThat(publication.getSignalBindingsList())
                .allSatisfy(binding -> {
                    assertThat(binding.getProductId()).isEqualTo("PRODUCT-SUGAR");
                    assertThat(binding.getCalibrationVersion()).isEqualTo("CAL-1");
                });
        assertThat(publication.getActive()).isTrue();
        assertThat(publication.getChecksum()).isEqualTo("r".repeat(64));
        assertThat(jdbc.queryForList("""
                SELECT action || '|' || before_revision || '|' || after_revision
                  FROM bpi.bpi_audit_events
                 WHERE tenant_id = ? AND object_id = ?
                 ORDER BY after_revision
                """, String.class, tenantId, ruleId))
                .containsExactly(
                        "RULE_SIMULATED|1|2",
                        "RULE_APPROVAL_SUBMITTED|2|3",
                        "RULE_PUBLISHED|3|4");
        assertThat(count("bpi_api_idempotency")).isEqualTo(3);
        assertThat(count("bpi_outbox_events")).isOne();

        String wrongScopeToken = token(
                tenantId, List.of("BPI_VIEWER"), List.of("PLANT-01"), List.of("LINE-OTHER"));
        mockMvc.perform(get("/bpi/v1/rules/{id}", ruleId)
                        .header("Authorization", "Bearer " + wrongScopeToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void administratorRetiresAppliedRuleAndCreatesRollbackDraftFromInactiveVersion() throws Exception {
        jdbc.update("""
                UPDATE bpi.bpi_rule_versions
                   SET state = 'PUBLISHED', revision = 4
                 WHERE tenant_id = ? AND id = ?
                """, tenantId, ruleId);
        UUID activationEventId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO bpi.bpi_outbox_events
                    (id, tenant_id, plant_id, line_id, aggregate_type, aggregate_id,
                     event_type, topic, partition_key, payload, headers, status,
                     application_status, runtime_readiness_status,
                     lifecycle_action, lifecycle_sequence, lifecycle_active)
                VALUES (?, ?, 'PLANT-01', 'LINE-S07-01', 'RULE_VERSION', ?,
                        'BOUNDARY_RULE_PUBLISHED', 'bpi.boundary.rule-publication.v1', ?, ?,
                        CAST(? AS jsonb), 'PUBLISHED', 'APPLIED', 'READY',
                        'ACTIVATE', 1, true)
                """, activationEventId, tenantId, ruleId,
                tenantId + ":LINE-S07-01:RULE-S07-START:1.2.0",
                new byte[] {1, 2, 3}, "{\"lifecycle_action\":\"ACTIVATE\"}");
        UUID replacementRuleId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO bpi.bpi_rule_versions
                    (id, tenant_id, rule_code, version, topology_version_id, state, checksum, definition,
                     revision, plant_id, line_id, created_by, updated_by)
                VALUES (?, ?, 'RULE-S07-START', '1.2.99', ?, 'DRAFT', ?, CAST(? AS jsonb),
                        1, 'PLANT-01', 'LINE-S07-01', 'replacement', 'replacement')
                """, replacementRuleId, tenantId, topologyId, "h".repeat(64), ruleDefinition());
        ActorContext replacementActor = new ActorContext(
                tenantId, "rule-retirement-admin", Set.of("BPI_ADMIN"),
                Set.of("PLANT-01"), Set.of("LINE-S07-01"));
        var replacementRule = ruleRepository.findRule(replacementActor, replacementRuleId);
        assertThatThrownBy(() -> ruleRepository.assertRulePublicationHandoffReady(
                replacementActor, replacementRule))
                .isInstanceOf(BpiConflictException.class)
                .hasMessage("Retire the currently published rule version before publishing its replacement.");
        String engineerToken = token(
                tenantId, List.of("BPI_ENGINEER"), List.of("PLANT-01"), List.of("LINE-S07-01"));
        String adminToken = token(
                "rule-retirement-admin", tenantId, List.of("BPI_ADMIN"),
                List.of("PLANT-01"), List.of("LINE-S07-01"));
        byte[] retireBody = objectMapper.writeValueAsBytes(Map.of(
                "reason", "发布替代版本前安全停止当前边界规则"));

        mockMvc.perform(post("/bpi/v1/rules/{id}/retire", ruleId)
                        .header("Authorization", "Bearer " + engineerToken)
                        .header("Idempotency-Key", "retire-denied-" + ruleId)
                        .header("If-Match", "4")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(retireBody))
                .andExpect(status().isForbidden());

        String retirementKey = "retire-rule-" + ruleId;
        mockMvc.perform(post("/bpi/v1/rules/{id}/retire", ruleId)
                        .header("Authorization", "Bearer " + adminToken)
                        .header("Idempotency-Key", retirementKey)
                        .header("If-Match", "4")
                        .header("X-Trace-Id", "TRACE-RETIRE-" + ruleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(retireBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("RETIRED"))
                .andExpect(jsonPath("$.data.revision").value(5))
                .andExpect(jsonPath("$.data.lifecycleAction").value("RETIRE"))
                .andExpect(jsonPath("$.data.lifecycleSequence").value(2))
                .andExpect(jsonPath("$.data.lifecycleActive").value(false))
                .andExpect(jsonPath("$.data.publicationStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.applicationStatus").value("WAITING"))
                .andExpect(jsonPath("$.data.runtimeReadinessStatus").value("WAITING"));
        mockMvc.perform(post("/bpi/v1/rules/{id}/retire", ruleId)
                        .header("Authorization", "Bearer " + adminToken)
                        .header("Idempotency-Key", retirementKey)
                        .header("If-Match", "4")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(retireBody))
                .andExpect(status().isOk())
                .andExpect(header().string("Idempotent-Replay", "true"))
                .andExpect(jsonPath("$.data.lifecycleSequence").value(2));

        List<Map<String, Object>> lifecycle = jdbc.queryForList("""
                SELECT id, lifecycle_action, lifecycle_sequence, lifecycle_active, payload,
                       headers ->> 'lifecycle_action' AS header_action
                  FROM bpi.bpi_outbox_events
                 WHERE tenant_id = ? AND aggregate_id = ?
                 ORDER BY lifecycle_sequence
                """, tenantId, ruleId);
        assertThat(lifecycle).hasSize(2);
        assertThat(lifecycle.get(0))
                .containsEntry("lifecycle_action", "ACTIVATE")
                .containsEntry("lifecycle_sequence", 1L)
                .containsEntry("lifecycle_active", true);
        assertThat(lifecycle.get(1))
                .containsEntry("lifecycle_action", "RETIRE")
                .containsEntry("lifecycle_sequence", 2L)
                .containsEntry("lifecycle_active", false)
                .containsEntry("header_action", "RETIRE");
        UUID retirementEventId = (UUID) lifecycle.get(1).get("id");
        assertThat(retirementEventId).isNotEqualTo(activationEventId);
        BoundaryRulePublicationV1 retirement = BoundaryRulePublicationV1.parseFrom(
                (byte[]) lifecycle.get(1).get("payload"));
        assertThat(retirement.getEventId()).isEqualTo(retirementEventId.toString());
        assertThat(retirement.getActive()).isFalse();
        assertThat(retirement.getRuleCode()).isEqualTo("RULE-S07-START");
        assertThat(retirement.getRuleVersion()).isEqualTo("1.2.0");
        assertThatThrownBy(() -> ruleRepository.assertRulePublicationHandoffReady(
                replacementActor, replacementRule))
                .isInstanceOf(BpiConflictException.class)
                .hasMessage("The previous rule retirement must reach Kafka PUBLISHED, Flink APPLIED and runtime INACTIVE before replacement publication.");

        jdbc.update("""
                UPDATE bpi.bpi_outbox_events
                   SET status = 'PUBLISHED', published_at = now(), revision = revision + 1
                 WHERE tenant_id = ? AND id = ?
                """, tenantId, retirementEventId);
        BoundaryRuleApplicationV1 applied = application(
                retirementEventId,
                "RETIREMENT-APPLIED-" + retirementEventId,
                BoundaryRuleApplicationStatusV1.APPLIED,
                "",
                "inactive rule update applied",
                boundaryTime.plusSeconds(4));
        receiptService.apply(applied, Checksums.sha256(applied.toByteArray()));
        BoundaryRuleRuntimeReadinessV1 inactive = runtimeReadiness(
                retirementEventId,
                "RETIREMENT-INACTIVE-" + retirementEventId,
                BoundaryRuleRuntimeReadinessStatusV1.INACTIVE,
                "RULE_INACTIVE",
                "published rule version is inactive",
                boundaryTime.plusSeconds(5),
                "CATALOG-RETIREMENT",
                "revision-retirement");
        runtimeReadinessReceiptService.apply(inactive, Checksums.sha256(inactive.toByteArray()));
        assertThatCode(() -> ruleRepository.assertRulePublicationHandoffReady(
                replacementActor, replacementRule)).doesNotThrowAnyException();
        jdbc.update("DELETE FROM bpi.bpi_rule_versions WHERE tenant_id = ? AND id = ?",
                tenantId, replacementRuleId);

        mockMvc.perform(get("/bpi/v1/rules/{id}", ruleId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("RETIRED"))
                .andExpect(jsonPath("$.data.lifecycleAction").value("RETIRE"))
                .andExpect(jsonPath("$.data.publicationStatus").value("PUBLISHED"))
                .andExpect(jsonPath("$.data.applicationStatus").value("APPLIED"))
                .andExpect(jsonPath("$.data.runtimeReadinessStatus").value("INACTIVE"))
                .andExpect(jsonPath("$.data.runtimeReadinessReasonCode").value("RULE_INACTIVE"));

        byte[] rollbackDraft = objectMapper.writeValueAsBytes(Map.of(
                "code", "RULE-S07-START",
                "version", "1.2.1",
                "lineId", "LINE-S07-01",
                "topologyVersion", "TOPO-S07@3",
                "baseVersionId", ruleId,
                "ast", objectMapper.readTree(ruleDefinition()),
                "reason", "从已退役稳定版本创建受控回滚草稿"));
        MvcResult rollbackResult = mockMvc.perform(post("/bpi/v1/rules/drafts")
                        .header("Authorization", "Bearer " + adminToken)
                        .header("Idempotency-Key", "rollback-draft-" + ruleId)
                        .header("If-Match", "5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rollbackDraft))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("DRAFT"))
                .andExpect(jsonPath("$.data.version").value("1.2.1"))
                .andExpect(jsonPath("$.data.lifecycleAction").value("NOT_PUBLISHED"))
                .andReturn();
        UUID rollbackRuleId = UUID.fromString(objectMapper.readTree(
                rollbackResult.getResponse().getContentAsString()).path("data").path("id").asText());
        assertThat(rollbackRuleId).isNotEqualTo(ruleId);
        assertThat(count("bpi_rule_versions")).isEqualTo(2);
        assertThat(count("bpi_outbox_events")).isEqualTo(2);
        assertThat(jdbc.queryForList("""
                SELECT action FROM bpi.bpi_audit_events
                 WHERE tenant_id = ? AND object_id IN (?, ?)
                 ORDER BY created_at
                """, String.class, tenantId, ruleId, rollbackRuleId))
                .containsExactly("RULE_RETIRED", "RULE_DRAFT_CREATED");
    }

    @Test
    void ruleAndTopologyComparisonUseScopedControlledContent() throws Exception {
        UUID topologyTargetId = UUID.randomUUID();
        UUID ruleTargetId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO bpi.bpi_topology_versions
                    (id, tenant_id, topology_code, version, state, checksum, definition,
                     plant_id, line_id, revision, created_by, updated_by)
                VALUES (?, ?, 'TOPO-S07', '4', 'DRAFT', ?, CAST(? AS jsonb),
                        'PLANT-01', 'LINE-S07-01', 1, 'comparison', 'comparison')
                """, topologyTargetId, tenantId, "u".repeat(64),
                objectMapper.writeValueAsString(Map.of(
                        "localityGroup", "LOCALITY-S07-EVAP",
                        "description", "comparison target",
                        "bindings", List.of())));
        jdbc.update("""
                INSERT INTO bpi.bpi_rule_versions
                    (id, tenant_id, rule_code, version, topology_version_id, state, checksum, definition,
                     revision, plant_id, line_id, created_by, updated_by)
                VALUES (?, ?, 'RULE-S07-START', '1.3.0', ?, 'DRAFT', ?, CAST(? AS jsonb),
                        1, 'PLANT-01', 'LINE-S07-01', 'comparison', 'comparison')
                """, ruleTargetId, tenantId, topologyId, "s".repeat(64), ruleDefinition(15));

        String viewerToken = token(
                tenantId, List.of("BPI_VIEWER"), List.of("PLANT-01"), List.of("LINE-S07-01"));
        mockMvc.perform(get("/bpi/v1/rules/{id}/compare", ruleTargetId)
                        .header("Authorization", "Bearer " + viewerToken)
                        .param("against", ruleId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.objectType").value("RULE_VERSION"))
                .andExpect(jsonPath("$.data.base.version").value("1.2.0"))
                .andExpect(jsonPath("$.data.target.version").value("1.3.0"))
                .andExpect(jsonPath("$.data.identical").value(false))
                .andExpect(jsonPath("$.data.changeCount").value(2))
                .andExpect(jsonPath("$.data.changes[0].path").value("/ast/conditions/0/holdSeconds"));
        mockMvc.perform(get("/bpi/v1/topologies/{id}/compare", topologyTargetId)
                        .header("Authorization", "Bearer " + viewerToken)
                        .param("against", topologyId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.objectType").value("TOPOLOGY_VERSION"))
                .andExpect(jsonPath("$.data.changeCount").value(org.hamcrest.Matchers.greaterThan(0)));
    }

    @Test
    void independentAdministratorCanRejectApprovalBackToDraftWithoutPublication() throws Exception {
        String engineerToken = token(
                tenantId, List.of("BPI_ENGINEER"), List.of("PLANT-01"), List.of("LINE-S07-01"));
        byte[] simulationBody = objectMapper.writeValueAsBytes(Map.of(
                "lineId", "LINE-S07-01",
                "from", boundaryTime.minusSeconds(1).toString(),
                "to", boundaryTime.plusSeconds(1).toString(),
                "topologyVersion", "TOPO-S07@3",
                "calibrationVersion", "CAL-1",
                "goldenSetId", "GOLDEN-S07-2026Q2"));
        MvcResult simulated = mockMvc.perform(post("/bpi/v1/rules/{id}/simulate", ruleId)
                        .header("Authorization", "Bearer " + engineerToken)
                        .header("Idempotency-Key", "reject-flow-simulate-" + ruleId)
                        .header("If-Match", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(simulationBody))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.state").value("PASSED"))
                .andReturn();
        JsonNode simulation = objectMapper.readTree(simulated.getResponse().getContentAsString()).path("data");
        UUID simulationId = UUID.fromString(simulation.path("id").asText());
        String simulationChecksum = simulation.path("checksum").asText();
        byte[] approvalBody = objectMapper.writeValueAsBytes(Map.of(
                "reason", "提交后验证管理员驳回",
                "simulationId", simulationId,
                "simulationChecksum", simulationChecksum));

        mockMvc.perform(post("/bpi/v1/rules/{id}/submit-approval", ruleId)
                        .header("Authorization", "Bearer " + engineerToken)
                        .header("Idempotency-Key", "reject-flow-submit-" + ruleId)
                        .header("If-Match", "2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(approvalBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("PENDING_APPROVAL"))
                .andExpect(jsonPath("$.data.approvalStatus").value("PENDING"));

        String adminToken = token(
                "rule-rejection-admin", tenantId, List.of("BPI_ADMIN"),
                List.of("PLANT-01"), List.of("LINE-S07-01"));
        String rejectionKey = "reject-flow-decision-" + ruleId;
        byte[] rejectionBody = objectMapper.writeValueAsBytes(Map.of(
                "reason", "模拟证据不足，退回修订",
                "comment", "验收确认驳回不会产生规则发布事件"));
        mockMvc.perform(post("/bpi/v1/rules/{id}/reject-approval", ruleId)
                        .header("Authorization", "Bearer " + adminToken)
                        .header("Idempotency-Key", rejectionKey)
                        .header("If-Match", "3")
                        .header("X-Trace-Id", "TRACE-REJECT-" + ruleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rejectionBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("DRAFT"))
                .andExpect(jsonPath("$.data.revision").value(4))
                .andExpect(jsonPath("$.data.approvalStatus").value("REJECTED"))
                .andExpect(jsonPath("$.data.approvalDecidedBy").value("rule-rejection-admin"));
        mockMvc.perform(post("/bpi/v1/rules/{id}/reject-approval", ruleId)
                        .header("Authorization", "Bearer " + adminToken)
                        .header("Idempotency-Key", rejectionKey)
                        .header("If-Match", "3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rejectionBody))
                .andExpect(status().isOk())
                .andExpect(header().string("Idempotent-Replay", "true"))
                .andExpect(jsonPath("$.data.state").value("DRAFT"));

        assertThat(jdbc.queryForObject("""
                SELECT state || '|' || revision FROM bpi.bpi_rule_versions
                 WHERE tenant_id = ? AND id = ?
                """, String.class, tenantId, ruleId)).isEqualTo("DRAFT|4");
        assertThat(jdbc.queryForObject("""
                SELECT state || '|' || revision || '|' || decided_by || '|' || decision_reason
                  FROM bpi.bpi_rule_approval_requests
                 WHERE tenant_id = ? AND rule_version_id = ?
                """, String.class, tenantId, ruleId))
                .isEqualTo("REJECTED|2|rule-rejection-admin|模拟证据不足，退回修订");
        assertThat(jdbc.queryForList("""
                SELECT action || '|' || before_revision || '|' || after_revision
                  FROM bpi.bpi_audit_events
                 WHERE tenant_id = ? AND object_id = ?
                 ORDER BY after_revision
                """, String.class, tenantId, ruleId))
                .containsExactly(
                        "RULE_SIMULATED|1|2",
                        "RULE_APPROVAL_SUBMITTED|2|3",
                        "RULE_APPROVAL_REJECTED|3|4");
        assertThat(count("bpi_api_idempotency")).isEqualTo(3);
        assertThat(count("bpi_outbox_events")).isZero();
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

    @Test
    void adminRequeuesOnlyFailedPublicationWithAuditIdempotencyAndOptimisticLocking() throws Exception {
        jdbc.update("""
                UPDATE bpi.bpi_rule_versions
                   SET state = 'PUBLISHED', revision = 3
                 WHERE tenant_id = ? AND id = ?
                """, tenantId, ruleId);
        UUID publicationId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO bpi.bpi_outbox_events
                    (id, tenant_id, plant_id, line_id, aggregate_type, aggregate_id,
                     event_type, topic, partition_key, payload, status, revision,
                     attempt_count, total_attempt_count, last_error)
                VALUES (?, ?, 'PLANT-01', 'LINE-S07-01', 'RULE_VERSION', ?,
                        'BOUNDARY_RULE_PUBLISHED', 'bpi.boundary.rule-publication.v1', ?, ?,
                        'FAILED', 7, 3, 3, 'Kafka unavailable after three attempts')
                """, publicationId, tenantId, ruleId,
                tenantId + ":LINE-S07-01:RULE-S07-START:1.2.0", new byte[] {1, 2, 3});
        byte[] body = objectMapper.writeValueAsBytes(Map.of(
                "reason", "Kafka 集群恢复并完成 broker 连通性检查"));
        String engineerToken = token(
                tenantId, List.of("BPI_ENGINEER"), List.of("PLANT-01"), List.of("LINE-S07-01"));
        String adminToken = token(
                tenantId, List.of("BPI_ADMIN"), List.of("PLANT-01"), List.of("LINE-S07-01"));

        mockMvc.perform(post("/bpi/v1/rules/{id}/publication/retry", ruleId)
                        .header("Authorization", "Bearer " + engineerToken)
                        .header("Idempotency-Key", "retry-denied-" + ruleId)
                        .header("If-Match", "7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/bpi/v1/rules/{id}/publication/retry", ruleId)
                        .header("Authorization", "Bearer " + adminToken)
                        .header("Idempotency-Key", "retry-stale-" + ruleId)
                        .header("If-Match", "6")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.currentRevision").value(7));

        String retryKey = "retry-publication-" + ruleId;
        mockMvc.perform(post("/bpi/v1/rules/{id}/publication/retry", ruleId)
                        .header("Authorization", "Bearer " + adminToken)
                        .header("Idempotency-Key", retryKey)
                        .header("If-Match", "7")
                        .header("X-Trace-Id", "TRACE-RETRY-" + ruleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("PUBLISHED"))
                .andExpect(jsonPath("$.data.revision").value(3))
                .andExpect(jsonPath("$.data.publicationStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.publicationRevision").value(8))
                .andExpect(jsonPath("$.data.publicationAttemptCount").value(0))
                .andExpect(jsonPath("$.data.publicationTotalAttemptCount").value(3))
                .andExpect(jsonPath("$.data.publicationManualRetryCount").value(1))
                .andExpect(jsonPath("$.data.publicationLastRequeuedAt").isNotEmpty())
                .andExpect(jsonPath("$.data.publicationLastError").doesNotExist());
        mockMvc.perform(post("/bpi/v1/rules/{id}/publication/retry", ruleId)
                        .header("Authorization", "Bearer " + adminToken)
                        .header("Idempotency-Key", retryKey)
                        .header("If-Match", "7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(header().string("Idempotent-Replay", "true"))
                .andExpect(jsonPath("$.data.publicationRevision").value(8));

        Map<String, Object> publication = jdbc.queryForMap("""
                SELECT status, revision, attempt_count, total_attempt_count,
                       manual_retry_count, last_error, last_requeued_at, last_requeued_by
                  FROM bpi.bpi_outbox_events
                 WHERE tenant_id = ? AND id = ?
                """, tenantId, publicationId);
        assertThat(publication.get("status")).isEqualTo("PENDING");
        assertThat(publication.get("revision")).isEqualTo(8L);
        assertThat(publication.get("attempt_count")).isEqualTo(0);
        assertThat(publication.get("total_attempt_count")).isEqualTo(3);
        assertThat(publication.get("manual_retry_count")).isEqualTo(1);
        assertThat(publication.get("last_error")).isNull();
        assertThat(publication.get("last_requeued_at")).isNotNull();
        assertThat(publication.get("last_requeued_by")).isEqualTo("rule-acceptance-user");
        assertThat(jdbc.queryForObject("""
                SELECT object_type || '|' || action || '|' || before_revision || '|' || after_revision
                  FROM bpi.bpi_audit_events
                 WHERE tenant_id = ? AND object_id = ?
                """, String.class, tenantId, publicationId))
                .isEqualTo("RULE_PUBLICATION|RULE_PUBLICATION_REQUEUED|7|8");
        assertThat(count("bpi_api_idempotency")).isOne();
    }

    @Test
    void flinkRuleApplicationReceiptTransitionsRejectedToAppliedWithInboxAuditAndReplaySafety() {
        jdbc.update("""
                UPDATE bpi.bpi_rule_versions
                   SET state = 'PUBLISHED', revision = 3
                 WHERE tenant_id = ? AND id = ?
                """, tenantId, ruleId);
        UUID publicationId = insertOutbox(
                "BOUNDARY_RULE_PUBLISHED", "PUBLISHED", 1, null, null);
        BoundaryRuleApplicationV1 rejected = application(
                publicationId,
                "APPLICATION-REJECTED-" + publicationId,
                BoundaryRuleApplicationStatusV1.REJECTED,
                "RULE_WINDOW_EXCEEDS_STATE_TTL",
                "rule window exceeds state TTL",
                boundaryTime.plusSeconds(1));

        var rejectedRule = receiptService.apply(rejected, Checksums.sha256(rejected.toByteArray()));

        assertThat(rejectedRule.applicationStatus()).isEqualTo("REJECTED");
        assertThat(rejectedRule.applicationDeploymentId()).isEqualTo("flink-acceptance-a");
        assertThat(rejectedRule.applicationErrorCode()).isEqualTo("RULE_WINDOW_EXCEEDS_STATE_TTL");
        assertThat(rejectedRule.publicationRevision()).isEqualTo(2);

        BoundaryRuleApplicationV1 applied = application(
                publicationId,
                "APPLICATION-APPLIED-" + publicationId,
                BoundaryRuleApplicationStatusV1.APPLIED,
                "",
                "",
                boundaryTime.plusSeconds(2));
        var appliedRule = receiptService.apply(applied, Checksums.sha256(applied.toByteArray()));
        var replayedRule = receiptService.apply(applied, Checksums.sha256(applied.toByteArray()));

        assertThat(appliedRule.applicationStatus()).isEqualTo("APPLIED");
        assertThat(appliedRule.applicationErrorCode()).isNull();
        assertThat(appliedRule.applicationErrorDetail()).isNull();
        assertThat(appliedRule.publicationRevision()).isEqualTo(3);
        assertThat(replayedRule.publicationRevision()).isEqualTo(3);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM bpi.bpi_inbox_events
                 WHERE tenant_id = ? AND source = 'bpi.boundary.rule-application.v1'
                """, Integer.class, tenantId)).isEqualTo(2);
        assertThat(jdbc.queryForList("""
                SELECT action || '|' || before_revision || '|' || after_revision
                  FROM bpi.bpi_audit_events
                 WHERE tenant_id = ? AND object_id = ?
                 ORDER BY after_revision
                """, String.class, tenantId, publicationId))
                .containsExactly(
                        "RULE_PUBLICATION_REJECTED|1|2",
                        "RULE_PUBLICATION_APPLIED|2|3");
        assertThat(jdbc.queryForObject("""
                SELECT application_status || '|' || application_deployment_id || '|'
                       || revision || '|' || (application_received_at IS NOT NULL)
                  FROM bpi.bpi_outbox_events
                 WHERE tenant_id = ? AND id = ?
                """, String.class, tenantId, publicationId))
                .isEqualTo("APPLIED|flink-acceptance-a|3|true");
    }

    @Test
    void flinkRuntimeReadinessTransitionsPersistAndOlderReceiptCannotOverwriteApiTruth() throws Exception {
        jdbc.update("""
                UPDATE bpi.bpi_rule_versions
                   SET state = 'PUBLISHED', revision = 3
                 WHERE tenant_id = ? AND id = ?
                """, tenantId, ruleId);
        UUID publicationId = insertOutbox(
                "BOUNDARY_RULE_PUBLISHED", "PUBLISHED", 1, null, null);
        BoundaryRuleRuntimeReadinessV1 degraded = runtimeReadiness(
                publicationId,
                "READINESS-DEGRADED-" + publicationId,
                BoundaryRuleRuntimeReadinessStatusV1.DEGRADED,
                "POINT_DEVICE_NOT_ACTIVE",
                "bound device is not active",
                boundaryTime.plusSeconds(2),
                "CATALOG-DEGRADED",
                "revision-degraded");

        var degradedRule = runtimeReadinessReceiptService.apply(
                degraded, Checksums.sha256(degraded.toByteArray()));

        assertThat(degradedRule.runtimeReadinessStatus()).isEqualTo("DEGRADED");
        assertThat(degradedRule.runtimeReadinessReasonCode()).isEqualTo("POINT_DEVICE_NOT_ACTIVE");
        assertThat(degradedRule.runtimePointCatalogEventId()).isEqualTo("CATALOG-DEGRADED");
        assertThat(degradedRule.publicationRevision()).isEqualTo(2);

        BoundaryRuleRuntimeReadinessV1 olderReady = runtimeReadiness(
                publicationId,
                "READINESS-OLDER-" + publicationId,
                BoundaryRuleRuntimeReadinessStatusV1.READY,
                "",
                "",
                boundaryTime.plusSeconds(1),
                "CATALOG-OLDER",
                "revision-older");
        var afterOlder = runtimeReadinessReceiptService.apply(
                olderReady, Checksums.sha256(olderReady.toByteArray()));
        assertThat(afterOlder.runtimeReadinessStatus()).isEqualTo("DEGRADED");
        assertThat(afterOlder.publicationRevision()).isEqualTo(2);

        BoundaryRuleRuntimeReadinessV1 ready = runtimeReadiness(
                publicationId,
                "READINESS-READY-" + publicationId,
                BoundaryRuleRuntimeReadinessStatusV1.READY,
                "",
                "",
                boundaryTime.plusSeconds(3),
                "CATALOG-READY",
                "revision-ready");
        var readyRule = runtimeReadinessReceiptService.apply(ready, Checksums.sha256(ready.toByteArray()));
        var replayedRule = runtimeReadinessReceiptService.apply(ready, Checksums.sha256(ready.toByteArray()));

        assertThat(readyRule.runtimeReadinessStatus()).isEqualTo("READY");
        assertThat(readyRule.runtimeReadinessReasonCode()).isNull();
        assertThat(readyRule.runtimeReadinessDetail()).isNull();
        assertThat(readyRule.runtimePointCatalogEventId()).isEqualTo("CATALOG-READY");
        assertThat(readyRule.runtimePointCatalogSourceRevision()).isEqualTo("revision-ready");
        assertThat(readyRule.publicationRevision()).isEqualTo(3);
        assertThat(replayedRule.publicationRevision()).isEqualTo(3);

        String viewerToken = token(
                tenantId, List.of("BPI_VIEWER"), List.of("PLANT-01"), List.of("LINE-S07-01"));
        mockMvc.perform(get("/bpi/v1/rules/{id}", ruleId)
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.applicationStatus").value("WAITING"))
                .andExpect(jsonPath("$.data.runtimeReadinessStatus").value("READY"))
                .andExpect(jsonPath("$.data.runtimeReadinessDeploymentId").value("flink-acceptance-a"))
                .andExpect(jsonPath("$.data.runtimePointCatalogEventId").value("CATALOG-READY"))
                .andExpect(jsonPath("$.data.runtimePointCatalogSourceRevision").value("revision-ready"));
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM bpi.bpi_inbox_events
                 WHERE tenant_id = ? AND source = 'bpi.boundary.rule-runtime-readiness.v1'
                """, Integer.class, tenantId)).isEqualTo(3);
        assertThat(jdbc.queryForList("""
                SELECT action || '|' || before_revision || '|' || after_revision
                  FROM bpi.bpi_audit_events
                 WHERE tenant_id = ? AND object_id = ? AND action LIKE 'RULE_RUNTIME_%'
                 ORDER BY after_revision
                """, String.class, tenantId, publicationId))
                .containsExactly("RULE_RUNTIME_DEGRADED|1|2", "RULE_RUNTIME_READY|2|3");
    }

    private BoundaryRuleApplicationV1 application(
            UUID publicationId,
            String eventId,
            BoundaryRuleApplicationStatusV1 status,
            String errorCode,
            String detail,
            Instant observedAt) {
        return BoundaryRuleApplicationV1.newBuilder()
                .setEventId(eventId)
                .setPublicationEventId(publicationId.toString())
                .setTenantId(tenantId)
                .setPlantId("PLANT-01")
                .setLineId("LINE-S07-01")
                .setRuleCode("RULE-S07-START")
                .setRuleVersion("1.2.0")
                .setChecksum("r".repeat(64))
                .setDeploymentId("flink-acceptance-a")
                .setStatus(status)
                .setErrorCode(errorCode)
                .setDetail(detail)
                .setObservedAtMs(observedAt.toEpochMilli())
                .putHeaders("trace_id", "TRACE-APPLICATION-" + publicationId)
                .build();
    }

    private BoundaryRuleRuntimeReadinessV1 runtimeReadiness(
            UUID publicationId,
            String eventId,
            BoundaryRuleRuntimeReadinessStatusV1 status,
            String reasonCode,
            String detail,
            Instant observedAt,
            String pointCatalogEventId,
            String pointCatalogSourceRevision) {
        return BoundaryRuleRuntimeReadinessV1.newBuilder()
                .setEventId(eventId)
                .setPublicationEventId(publicationId.toString())
                .setTenantId(tenantId)
                .setPlantId("PLANT-01")
                .setLineId("LINE-S07-01")
                .setRuleCode("RULE-S07-START")
                .setRuleVersion("1.2.0")
                .setChecksum("r".repeat(64))
                .setDeploymentId("flink-acceptance-a")
                .setStatus(status)
                .setReasonCode(reasonCode)
                .setDetail(detail)
                .setObservedAtMs(observedAt.toEpochMilli())
                .setPointCatalogEventId(pointCatalogEventId)
                .setPointCatalogSourceRevision(pointCatalogSourceRevision)
                .putHeaders("trace_id", "TRACE-READINESS-" + publicationId)
                .build();
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
                     total_attempt_count,
                     claim_token, claimed_at)
                VALUES (?, ?, 'PLANT-01', 'LINE-S07-01', 'RULE_VERSION', ?, ?,
                        'bpi.boundary.rule-publication.v1', ?, ?, ?, ?, ?, ?, ?)
                """, id, tenantId, ruleId, eventType,
                tenantId + ":LINE-S07-01:" + eventType, new byte[] {1, 2, 3},
                state, attempts, attempts, claimToken, claimedAt);
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
                                "productId", "PRODUCT-SUGAR",
                                "deviceId", "DEVICE-S07-01",
                                "propertyId", "flow.instant",
                                "expectedUnit", "t/h",
                                "calibrationVersion", "CAL-1"),
                        Map.of(
                                "signal", "pump.running",
                                "productId", "PRODUCT-SUGAR",
                                "deviceId", "DEVICE-S07-01",
                                "propertyId", "pump.running",
                                "expectedUnit", "bool",
                                "calibrationVersion", "CAL-1"))));
    }

    private void insertCatalogSnapshot(boolean active, Instant observedAt) {
        UUID snapshotId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO bpi.bpi_point_catalog_snapshots
                    (id, tenant_id, source, source_instance, source_revision,
                     plant_id, line_id, checksum, observed_at, point_count,
                     source_claim_ready_point_count, imported_by)
                VALUES (?, ?, 'JETLINKS', 'RULE-ACCEPTANCE', ?,
                        'PLANT-01', 'LINE-S07-01', ?, ?, 2, ?, 'acceptance')
                """, snapshotId, tenantId, "revision-" + snapshotId,
                UUID.randomUUID().toString().replace("-", "").repeat(2).substring(0, 64),
                java.sql.Timestamp.from(observedAt), active ? 2 : 0);
        for (String property : List.of("flow.instant", "pump.running")) {
            jdbc.update("""
                    INSERT INTO bpi.bpi_point_catalog_entries
                        (id, tenant_id, snapshot_id, plant_id, line_id, locality_group,
                         product_id, device_id, property_id, point_name, unit, data_type,
                         device_state, registered, property_present, calibration_version,
                         calibration_status, source_sequence_enabled, source_sequence_required,
                         source_sequence_origin, source_sequence_binding_fingerprint)
                    VALUES (?, ?, ?, 'PLANT-01', 'LINE-S07-01', 'LOCALITY-S07-EVAP',
                            'PRODUCT-SUGAR', 'DEVICE-S07-01', ?, ?, ?, ?, ?, true, true,
                            'CAL-1', 'VERIFIED', true, true, 'DEVICE', ?)
                    """, UUID.randomUUID(), tenantId, snapshotId, property, property,
                    "flow.instant".equals(property) ? "t/h" : "bool",
                    "flow.instant".equals(property) ? "double" : "boolean",
                    active ? "ACTIVE" : "INACTIVE",
                    SourceSequenceEvidenceTestFixture.FINGERPRINT);
        }
        SourceSequenceEvidenceTestFixture.qualifyCurrentDevice(
                jdbc, tenantId, "PLANT-01", "LINE-S07-01", "PRODUCT-SUGAR", "DEVICE-S07-01",
                "RULE_SEQUENCE_" + snapshotId);
    }

    private void insertApprovedCalibration(String propertyId) {
        jdbc.update("""
                INSERT INTO bpi.bpi_point_calibrations
                    (id, tenant_id, plant_id, line_id, product_id, device_id, property_id,
                     calibration_version, certificate_reference, certificate_checksum,
                     valid_from, valid_until, state, revision, submitted_by, submit_reason,
                     decided_by, decided_at, decision_reason)
                VALUES (?, ?, 'PLANT-01', 'LINE-S07-01', 'PRODUCT-SUGAR', 'DEVICE-S07-01', ?,
                        'CAL-1', ?, ?, ?, '2027-07-12T00:00:00Z',
                        'APPROVED', 2, 'calibration-author', '验收校准证据',
                        'calibration-reviewer', now(), '独立复核通过')
                """, UUID.randomUUID(), tenantId, propertyId,
                "urn:adp:test:" + propertyId, "c".repeat(64),
                java.sql.Timestamp.from(boundaryTime.minusSeconds(3600)));
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
        return token("rule-acceptance-user", tenant, roles, plants, lines);
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
