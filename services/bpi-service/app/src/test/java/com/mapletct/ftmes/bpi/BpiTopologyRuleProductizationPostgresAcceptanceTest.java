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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "BPI_TEST_DATABASE_URL", matches = ".+")
class BpiTopologyRuleProductizationPostgresAcceptanceTest {
    private static final String SECRET = "bpi-test-secret-0123456789abcdef";
    private static final String PLANT_ID = "PLANT-01";
    private static final String LINE_ID = "LINE-S07-01";

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
    private String marker;

    @BeforeEach
    void enableRuleManagementForUniqueTenant() {
        marker = "ADP_E2E_BPI_PRODUCT_" + UUID.randomUUID().toString().replace("-", "");
        tenantId = marker;
        jdbc.update("""
                INSERT INTO bpi.bpi_feature_flags
                    (id, tenant_id, scope_type, scope_key, flag_key, enabled, revision, updated_by)
                VALUES (?, ?, 'LINE', ?, 'bpi.rule-management', true, 1, 'acceptance')
                """, UUID.randomUUID(), tenantId, LINE_ID);
        UUID snapshotId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO bpi.bpi_point_calibrations
                    (id, tenant_id, plant_id, line_id, product_id, device_id, property_id,
                     calibration_version, certificate_reference, certificate_checksum,
                     valid_from, valid_until, state, revision, submitted_by, submit_reason,
                     decided_by, decided_at, decision_reason)
                VALUES (?, ?, ?, ?, 'PRODUCT-SUGAR', 'DEVICE-S07-01', 'flow.instant',
                        'CAL-1', ?, ?, now() - interval '1 day', now() + interval '1 year',
                        'APPROVED', 2, 'calibration-author', '验收校准证据',
                        'calibration-reviewer', now(), '独立复核通过')
                """, UUID.randomUUID(), tenantId, PLANT_ID, LINE_ID,
                "urn:adp:test:" + marker, "c".repeat(64));
        jdbc.update("""
                INSERT INTO bpi.bpi_point_catalog_snapshots
                    (id, tenant_id, source, source_instance, source_revision, plant_id, line_id,
                     checksum, observed_at, point_count, source_claim_ready_point_count, imported_by)
                VALUES (?, ?, 'JETLINKS', 'acceptance', ?, ?, ?, ?, now(), 1, 1, 'acceptance')
                """, snapshotId, tenantId, marker, PLANT_ID, LINE_ID, "a".repeat(64));
        jdbc.update("""
                INSERT INTO bpi.bpi_point_catalog_entries
                    (id, tenant_id, snapshot_id, plant_id, line_id, locality_group,
                     product_id, device_id, property_id, point_name, unit, data_type,
                     device_state, registered, property_present, calibration_version,
                     calibration_status, source_sequence_enabled)
                VALUES (?, ?, ?, ?, ?, 'LOCALITY-S07-EVAP', 'PRODUCT-SUGAR', 'DEVICE-S07-01',
                        'flow.instant', '进料瞬时流量', 't/h', 'double', 'ACTIVE', true, true,
                        'CAL-1', 'VERIFIED', true)
                """, UUID.randomUUID(), tenantId, snapshotId, PLANT_ID, LINE_ID);
    }

    @AfterEach
    void cleanupMarker() {
        if (tenantId == null) return;
        jdbc.update("DELETE FROM bpi.bpi_audit_events WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_api_idempotency WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_rule_versions WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_topology_versions WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_point_catalog_entries WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_point_catalog_snapshots WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_point_calibrations WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_feature_flags WHERE tenant_id = ?", tenantId);
    }

    @Test
    void topologyValidationPublicationAndRuleDraftArePersistedWithAuditAndReplaySafety() throws Exception {
        String creatorToken = token("topology-creator", List.of("BPI_ENGINEER"));
        String creatorAdminToken = token("topology-creator", List.of("BPI_ADMIN"));
        String publisherToken = token("topology-publisher", List.of("BPI_ADMIN"));

        String invalidCode = marker + "_INVALID";
        MvcResult invalidCreated = createTopology(
                creatorToken, "create-invalid-" + marker, invalidCode, "1.0.0", invalidDefinition());
        UUID invalidId = responseId(invalidCreated);
        mockMvc.perform(post("/bpi/v1/topologies/{id}/validate", invalidId)
                        .header("Authorization", "Bearer " + creatorToken)
                        .header("Idempotency-Key", "validate-invalid-" + marker)
                        .header("If-Match", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reason("验证故意缺失的拓扑绑定")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.validationStatus").value("FAILED"))
                .andExpect(jsonPath("$.data.validationErrors.length()").value(1))
                .andExpect(jsonPath("$.data.revision").value(2));
        assertThat(jdbc.queryForObject("""
                SELECT validation_status || '|' || revision || '|' || jsonb_array_length(validation_errors)
                  FROM bpi.bpi_topology_versions
                 WHERE tenant_id = ? AND id = ?
                """, String.class, tenantId, invalidId)).isEqualTo("FAILED|2|1");

        String topologyCode = marker + "_TOPO";
        String createKey = "create-valid-" + marker;
        byte[] validBody = topologyBody(topologyCode, "1.0.0", validDefinition());
        MvcResult created = mockMvc.perform(post("/bpi/v1/topologies/drafts")
                        .header("Authorization", "Bearer " + creatorToken)
                        .header("Idempotency-Key", createKey)
                        .header("If-Match", "0")
                        .header("X-Trace-Id", marker + "_CREATE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("DRAFT"))
                .andExpect(jsonPath("$.data.validationStatus").value("NOT_VALIDATED"))
                .andExpect(jsonPath("$.data.revision").value(1))
                .andReturn();
        UUID topologyId = responseId(created);
        mockMvc.perform(post("/bpi/v1/topologies/drafts")
                        .header("Authorization", "Bearer " + creatorToken)
                        .header("Idempotency-Key", createKey)
                        .header("If-Match", "0")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody))
                .andExpect(status().isOk())
                .andExpect(header().string("Idempotent-Replay", "true"))
                .andExpect(jsonPath("$.data.id").value(topologyId.toString()));

        mockMvc.perform(post("/bpi/v1/topologies/{id}/validate", topologyId)
                        .header("Authorization", "Bearer " + creatorToken)
                        .header("Idempotency-Key", "validate-valid-" + marker)
                        .header("If-Match", "1")
                        .header("X-Trace-Id", marker + "_VALIDATE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reason("验证完整拓扑和测点绑定")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.validationStatus").value("PASSED"))
                .andExpect(jsonPath("$.data.validationErrors.length()").value(0))
                .andExpect(jsonPath("$.data.revision").value(2));

        byte[] publishReason = reason("由独立管理员审批并发布拓扑");
        mockMvc.perform(post("/bpi/v1/topologies/{id}/publish", topologyId)
                        .header("Authorization", "Bearer " + creatorToken)
                        .header("Idempotency-Key", "publish-role-denied-" + marker)
                        .header("If-Match", "2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(publishReason))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/bpi/v1/topologies/{id}/publish", topologyId)
                        .header("Authorization", "Bearer " + creatorAdminToken)
                        .header("Idempotency-Key", "publish-creator-denied-" + marker)
                        .header("If-Match", "2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(publishReason))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("other than the creator")));
        mockMvc.perform(post("/bpi/v1/topologies/{id}/publish", topologyId)
                        .header("Authorization", "Bearer " + publisherToken)
                        .header("Idempotency-Key", "publish-valid-" + marker)
                        .header("If-Match", "2")
                        .header("X-Trace-Id", marker + "_PUBLISH")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(publishReason))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("PUBLISHED"))
                .andExpect(jsonPath("$.data.publishedBy").value("topology-publisher"))
                .andExpect(jsonPath("$.data.revision").value(3));

        byte[] unboundRule = ruleBody(
                marker + "_RULE_INVALID", topologyCode + "@1.0.0", "unbound.signal");
        mockMvc.perform(post("/bpi/v1/rules/drafts")
                        .header("Authorization", "Bearer " + creatorToken)
                        .header("Idempotency-Key", "create-rule-invalid-" + marker)
                        .header("If-Match", "0")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(unboundRule))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("not bound")));

        MvcResult ruleCreated = mockMvc.perform(post("/bpi/v1/rules/drafts")
                        .header("Authorization", "Bearer " + creatorToken)
                        .header("Idempotency-Key", "create-rule-valid-" + marker)
                        .header("If-Match", "0")
                        .header("X-Trace-Id", marker + "_RULE_CREATE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ruleBody(marker + "_RULE", topologyCode + "@1.0.0", "feed.flow")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("DRAFT"))
                .andExpect(jsonPath("$.data.topologyVersion").value(topologyCode + "@1.0.0"))
                .andExpect(jsonPath("$.data.revision").value(1))
                .andReturn();
        UUID ruleId = responseId(ruleCreated);

        assertThat(jdbc.queryForObject("""
                SELECT state || '|' || validation_status || '|' || revision || '|' || published_by
                  FROM bpi.bpi_topology_versions
                 WHERE tenant_id = ? AND id = ?
                """, String.class, tenantId, topologyId))
                .isEqualTo("PUBLISHED|PASSED|3|topology-publisher");
        assertThat(jdbc.queryForObject("""
                SELECT state || '|' || revision || '|' || created_by
                  FROM bpi.bpi_rule_versions
                 WHERE tenant_id = ? AND id = ?
                """, String.class, tenantId, ruleId))
                .isEqualTo("DRAFT|1|topology-creator");
        assertThat(jdbc.queryForList("""
                SELECT action || '|' || before_revision || '|' || after_revision
                  FROM bpi.bpi_audit_events
                 WHERE tenant_id = ? AND object_id = ?
                 ORDER BY after_revision
                """, String.class, tenantId, topologyId))
                .containsExactly(
                        "TOPOLOGY_DRAFT_CREATED|0|1",
                        "TOPOLOGY_VALIDATION_PASSED|1|2",
                        "TOPOLOGY_PUBLISHED|2|3");
        assertThat(jdbc.queryForObject("""
                SELECT action || '|' || before_revision || '|' || after_revision
                  FROM bpi.bpi_audit_events
                 WHERE tenant_id = ? AND object_id = ?
                """, String.class, tenantId, ruleId))
                .isEqualTo("RULE_DRAFT_CREATED|0|1");
        assertThat(count("bpi_topology_versions")).isEqualTo(2);
        assertThat(count("bpi_rule_versions")).isOne();
        assertThat(count("bpi_api_idempotency")).isEqualTo(6);
    }

    private MvcResult createTopology(
            String token,
            String idempotencyKey,
            String code,
            String version,
            Map<String, Object> definition) throws Exception {
        return mockMvc.perform(post("/bpi/v1/topologies/drafts")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", idempotencyKey)
                        .header("If-Match", "0")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(topologyBody(code, version, definition)))
                .andExpect(status().isOk())
                .andReturn();
    }

    private byte[] topologyBody(String code, String version, Map<String, Object> definition) throws Exception {
        return objectMapper.writeValueAsBytes(Map.of(
                "code", code,
                "version", version,
                "plantId", PLANT_ID,
                "lineId", LINE_ID,
                "definition", definition,
                "reason", "创建可审计的拓扑草稿"));
    }

    private byte[] ruleBody(String code, String topologyVersion, String signal) throws Exception {
        Map<String, Object> ast = Map.ofEntries(
                Map.entry("boundaryType", "START"),
                Map.entry("quorumMinimum", 1),
                Map.entry("minimumConfidence", 0.80),
                Map.entry("maxCompositePenalty", 0.80),
                Map.entry("timing", Map.of(
                        "allowedLatenessSeconds", 0,
                        "watermarkDelaySeconds", 0,
                        "evaluationTimeoutSeconds", 300)),
                Map.entry("conditions", List.of(Map.ofEntries(
                        Map.entry("signal", signal),
                        Map.entry("operator", "GREATER_THAN"),
                        Map.entry("threshold", 10),
                        Map.entry("holdSeconds", 15),
                        Map.entry("maxSilenceSeconds", 60),
                        Map.entry("classification", "QUORUM"),
                        Map.entry("weight", 100)))));
        return objectMapper.writeValueAsBytes(Map.of(
                "code", code,
                "version", "1.0.0",
                "lineId", LINE_ID,
                "topologyVersion", topologyVersion,
                "ast", ast,
                "reason", "创建批次开始边界规则草稿"));
    }

    private Map<String, Object> validDefinition() {
        return Map.of(
                "localityGroup", "LOCALITY-S07-EVAP",
                "nodes", List.of(
                        Map.of("code", "FEED-TANK", "type", "TANK"),
                        Map.of("code", "FLOW-METER", "type", "METER"),
                        Map.of("code", "RECEIVE-TANK", "type", "TANK")),
                "edges", List.of(
                        Map.of("from", "FEED-TANK", "to", "FLOW-METER"),
                        Map.of("from", "FLOW-METER", "to", "RECEIVE-TANK")),
                "bindings", List.of(Map.of(
                        "signal", "feed.flow",
                        "productId", "PRODUCT-SUGAR",
                        "deviceId", "DEVICE-S07-01",
                        "propertyId", "flow.instant",
                        "expectedUnit", "t/h",
                        "calibrationVersion", "CAL-1")),
                "requiredSignals", List.of("feed.flow"));
    }

    private Map<String, Object> invalidDefinition() {
        return Map.of(
                "localityGroup", "LOCALITY-S07-EVAP",
                "nodes", List.of(Map.of("code", "FLOW-METER", "type", "METER")),
                "edges", List.of());
    }

    private byte[] reason(String value) throws Exception {
        return objectMapper.writeValueAsBytes(Map.of("reason", value));
    }

    private UUID responseId(MvcResult result) throws Exception {
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(response.path("data").path("id").asText());
    }

    private long count(String table) {
        Long result = jdbc.queryForObject(
                "SELECT count(*) FROM bpi." + table + " WHERE tenant_id = ?", Long.class, tenantId);
        return result == null ? 0 : result;
    }

    private String token(String subject, List<String> roles) throws Exception {
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer("ft-mes-adapter")
                .audience("bpi-service")
                .subject(subject)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(600)))
                .claim("tenant_id", tenantId)
                .claim("roles", roles)
                .claim("plant_ids", List.of(PLANT_ID))
                .claim("line_ids", List.of(LINE_ID))
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
