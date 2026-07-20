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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "BPI_TEST_DATABASE_URL", matches = ".+")
class BpiPointCatalogPostgresAcceptanceTest {
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

    private String marker;
    private String tenantId;
    private String engineerToken;
    private String adminToken;

    @BeforeEach
    void setUpTenant() throws Exception {
        marker = env(
                "BPI_TEST_MARKER",
                "ADP_E2E_BPI_POINTS_" + UUID.randomUUID().toString().replace("-", ""));
        tenantId = marker;
        engineerToken = token("point-engineer", List.of("BPI_ENGINEER"));
        adminToken = token("point-admin", List.of("BPI_ADMIN"));
        jdbc.update("""
                INSERT INTO bpi.bpi_feature_flags
                    (id, tenant_id, scope_type, scope_key, flag_key, enabled, revision, updated_by)
                VALUES (?, ?, 'LINE', ?, 'bpi.rule-management', true, 1, 'acceptance')
                """, UUID.randomUUID(), tenantId, LINE_ID);
        insertApprovedCalibration("DEVICE-S07-01", "flow.instant", "CAL-2026-01");
        insertApprovedCalibration("DEVICE-S07-03", "flow.sequence-missing", "CAL-2026-01");
    }

    @AfterEach
    void cleanupMarker() {
        if (tenantId == null) return;
        jdbc.update("DELETE FROM bpi.bpi_audit_events WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_api_idempotency WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_rule_versions WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_topology_versions WHERE tenant_id = ?", tenantId);
        SourceSequenceEvidenceTestFixture.cleanup(jdbc, tenantId);
        jdbc.update("DELETE FROM bpi.bpi_point_catalog_entries WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_point_catalog_snapshots WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_point_calibrations WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_feature_flags WHERE tenant_id = ?", tenantId);
    }

    @Test
    void snapshotImportAndTopologyReadinessArePersistedAndFailClosed() throws Exception {
        mockMvc.perform(get("/bpi/v1/point-catalog/current")
                        .header("Authorization", "Bearer " + engineerToken)
                        .param("plantId", PLANT_ID).param("lineId", LINE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(nullValue()));

        UUID noCatalogTopology = createTopology(
                marker + "_NO_CATALOG", readyDefinition(), "no-catalog-create-" + marker);
        mockMvc.perform(post("/bpi/v1/topologies/{id}/validate", noCatalogTopology)
                        .header("Authorization", "Bearer " + engineerToken)
                        .header("Idempotency-Key", "no-catalog-validate-" + marker)
                        .header("If-Match", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reason("验证无目录时必须阻断")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.validationStatus").value("FAILED"))
                .andExpect(jsonPath("$.data.validationErrors[*].code")
                        .value(hasItem("POINT_CATALOG_SNAPSHOT_MISSING")))
                .andExpect(jsonPath("$.data.validatedPointCatalogSnapshotId").doesNotExist());

        byte[] snapshotBody = pointCatalogBody();
        mockMvc.perform(post("/bpi/v1/point-catalog/snapshots")
                        .header("Authorization", "Bearer " + engineerToken)
                        .header("Idempotency-Key", "point-import-denied-" + marker)
                        .header("If-Match", "0")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(snapshotBody))
                .andExpect(status().isForbidden());

        String importKey = "point-import-" + marker;
        MvcResult imported = mockMvc.perform(post("/bpi/v1/point-catalog/snapshots")
                        .header("Authorization", "Bearer " + adminToken)
                        .header("Idempotency-Key", importKey)
                        .header("If-Match", "0")
                        .header("X-Trace-Id", marker + "_IMPORT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(snapshotBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.snapshot.pointCount").value(3))
                .andExpect(jsonPath("$.data.snapshot.readyPointCount").value(0))
                .andExpect(jsonPath("$.data.points.length()").value(3))
                .andExpect(jsonPath("$.data.points[0].ready").value(false))
                .andExpect(jsonPath("$.data.points[0].readinessIssues[*]")
                        .value(hasItem("SOURCE_SEQUENCE_EVIDENCE_MISSING")))
                .andExpect(jsonPath("$.data.points[1].ready").value(false))
                .andExpect(jsonPath("$.data.points[2].ready").value(false))
                .andExpect(jsonPath("$.data.points[2].readinessIssues[*]")
                        .value(hasItem("SOURCE_SEQUENCE_DISABLED")))
                .andReturn();
        UUID snapshotId = UUID.fromString(response(imported).path("data").path("snapshot").path("id").asText());
        String snapshotChecksum = response(imported).path("data").path("snapshot").path("checksum").asText();
        qualifyReadyDevice(marker + "_SEQUENCE_1");

        mockMvc.perform(post("/bpi/v1/point-catalog/snapshots")
                        .header("Authorization", "Bearer " + adminToken)
                        .header("Idempotency-Key", importKey)
                        .header("If-Match", "0")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(snapshotBody))
                .andExpect(status().isOk())
                .andExpect(header().string("Idempotent-Replay", "true"))
                .andExpect(jsonPath("$.data.snapshot.id").value(snapshotId.toString()));

        mockMvc.perform(get("/bpi/v1/point-catalog/current")
                        .header("Authorization", "Bearer " + engineerToken)
                        .param("plantId", PLANT_ID).param("lineId", LINE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.snapshot.id").value(snapshotId.toString()))
                .andExpect(jsonPath("$.data.snapshot.checksum").value(snapshotChecksum))
                .andExpect(jsonPath("$.data.snapshot.readyPointCount").value(1))
                .andExpect(jsonPath("$.data.points[0].sourceSequenceQualified").value(true));

        UUID staleTopology = createTopology(
                marker + "_STALE", readyDefinition(), "stale-create-" + marker);
        mockMvc.perform(post("/bpi/v1/topologies/{id}/validate", staleTopology)
                        .header("Authorization", "Bearer " + engineerToken)
                        .header("Idempotency-Key", "stale-validate-" + marker)
                        .header("If-Match", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reason("验证发布前点位快照漂移")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.validationStatus").value("PASSED"))
                .andExpect(jsonPath("$.data.validatedPointCatalogSnapshotId").value(snapshotId.toString()));

        MvcResult refreshed = mockMvc.perform(post("/bpi/v1/point-catalog/snapshots")
                        .header("Authorization", "Bearer " + adminToken)
                        .header("Idempotency-Key", "point-refresh-" + marker)
                        .header("If-Match", "0")
                        .header("X-Trace-Id", marker + "_IMPORT_2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pointCatalogBody(marker + "_REFRESHED", Instant.now().minusSeconds(1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.snapshot.pointCount").value(3))
                .andExpect(jsonPath("$.data.snapshot.readyPointCount").value(1))
                .andReturn();
        UUID currentSnapshotId = UUID.fromString(
                response(refreshed).path("data").path("snapshot").path("id").asText());
        String currentSnapshotChecksum = response(refreshed)
                .path("data").path("snapshot").path("checksum").asText();
        qualifyReadyDevice(marker + "_SEQUENCE_2");

        mockMvc.perform(post("/bpi/v1/topologies/{id}/publish", staleTopology)
                        .header("Authorization", "Bearer " + adminToken)
                        .header("Idempotency-Key", "stale-publish-" + marker)
                        .header("If-Match", "2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reason("拒绝使用旧点位快照发布")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.currentRevision").value(2));

        UUID readyTopology = createTopology(
                marker + "_READY", readyDefinition(), "ready-create-" + marker);
        mockMvc.perform(post("/bpi/v1/topologies/{id}/validate", readyTopology)
                        .header("Authorization", "Bearer " + engineerToken)
                        .header("Idempotency-Key", "ready-validate-" + marker)
                        .header("If-Match", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reason("验证真实就绪点绑定")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.validationStatus").value("PASSED"))
                .andExpect(jsonPath("$.data.validatedPointCatalogSnapshotId").value(currentSnapshotId.toString()))
                .andExpect(jsonPath("$.data.validatedPointCatalogChecksum").value(currentSnapshotChecksum));
        mockMvc.perform(post("/bpi/v1/topologies/{id}/publish", readyTopology)
                        .header("Authorization", "Bearer " + adminToken)
                        .header("Idempotency-Key", "ready-publish-" + marker)
                        .header("If-Match", "2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reason("独立管理员发布就绪拓扑")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("PUBLISHED"));

        UUID blockedTopology = createTopology(
                marker + "_BLOCKED", blockedDefinition(), "blocked-create-" + marker);
        mockMvc.perform(post("/bpi/v1/topologies/{id}/validate", blockedTopology)
                        .header("Authorization", "Bearer " + engineerToken)
                        .header("Idempotency-Key", "blocked-validate-" + marker)
                        .header("If-Match", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reason("验证未激活缺属性点必须阻断")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.validationStatus").value("FAILED"))
                .andExpect(jsonPath("$.data.validationErrors[*].code")
                        .value(hasItem("POINT_DEVICE_NOT_REGISTERED")))
                .andExpect(jsonPath("$.data.validationErrors[*].code")
                        .value(hasItem("POINT_DEVICE_NOT_ACTIVE")))
                .andExpect(jsonPath("$.data.validationErrors[*].code")
                        .value(hasItem("POINT_PROPERTY_NOT_AVAILABLE")))
                .andExpect(jsonPath("$.data.validationErrors[*].code")
                        .value(hasItem("POINT_UNIT_MISSING")))
                .andExpect(jsonPath("$.data.validationErrors[*].code")
                        .value(hasItem("POINT_CALIBRATION_NOT_VERIFIED")))
                .andExpect(jsonPath("$.data.validationErrors[*].code")
                        .value(hasItem("POINT_SOURCE_SEQUENCE_DISABLED")));

        UUID sequenceBlockedTopology = createTopology(
                marker + "_SEQUENCE_BLOCKED", sequenceBlockedDefinition(), "sequence-create-" + marker);
        mockMvc.perform(post("/bpi/v1/topologies/{id}/validate", sequenceBlockedTopology)
                        .header("Authorization", "Bearer " + engineerToken)
                        .header("Idempotency-Key", "sequence-validate-" + marker)
                        .header("If-Match", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reason("验证仅缺来源序列的点位必须阻断")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.validationStatus").value("FAILED"))
                .andExpect(jsonPath("$.data.validationErrors.length()").value(1))
                .andExpect(jsonPath("$.data.validationErrors[0].code")
                        .value("POINT_SOURCE_SEQUENCE_DISABLED"))
                .andExpect(jsonPath("$.data.validationErrors[0].severity").value("ERROR"));

        assertThat(jdbc.queryForObject("""
                SELECT point_count || '|' || source_claim_ready_point_count || '|' || checksum
                  FROM bpi.bpi_point_catalog_snapshots
                 WHERE tenant_id = ? AND id = ?
                """, String.class, tenantId, currentSnapshotId)).isEqualTo("3|1|" + currentSnapshotChecksum);
        assertThat(jdbc.queryForObject("""
                SELECT validated_point_catalog_snapshot_id::text || '|' || validated_point_catalog_checksum
                  FROM bpi.bpi_topology_versions
                 WHERE tenant_id = ? AND id = ?
                """, String.class, tenantId, readyTopology))
                .isEqualTo(currentSnapshotId + "|" + currentSnapshotChecksum);
        assertThat(jdbc.queryForObject("""
                SELECT source_property_id
                  FROM bpi.bpi_point_catalog_entries
                 WHERE tenant_id = ? AND snapshot_id = ? AND property_id = 'flow.instant'
                """, String.class, tenantId, currentSnapshotId)).isEqualTo("instantFlow");
        assertThat(jdbc.queryForObject("""
                SELECT action || '|' || object_type || '|' || trace_id
                  FROM bpi.bpi_audit_events
                 WHERE tenant_id = ? AND object_id = ?
                """, String.class, tenantId, snapshotId))
                .isEqualTo("POINT_CATALOG_SNAPSHOT_IMPORTED|POINT_CATALOG_SNAPSHOT|" + marker + "_IMPORT");
        assertThat(count("bpi_point_catalog_snapshots")).isEqualTo(2);
        assertThat(count("bpi_point_catalog_entries")).isEqualTo(6);
        assertThat(count("bpi_api_idempotency")).isEqualTo(13);
    }

    @Test
    void repeatedSourceRevisionWithLaterObservationCanRevokeReadiness() throws Exception {
        String sourceRevision = marker + "_REPEATED";
        Instant firstObservedAt = Instant.now().minusSeconds(10);
        Instant secondObservedAt = firstObservedAt.plusSeconds(5);

        MvcResult first = mockMvc.perform(post("/bpi/v1/point-catalog/snapshots")
                        .header("Authorization", "Bearer " + adminToken)
                        .header("Idempotency-Key", "point-cycle-first-" + marker)
                        .header("If-Match", "0")
                .contentType(MediaType.APPLICATION_JSON)
                .content(pointCatalogBody(sourceRevision, firstObservedAt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.snapshot.readyPointCount").value(0))
                .andReturn();
        qualifyReadyDevice(marker + "_SEQUENCE_CYCLE");
        mockMvc.perform(get("/bpi/v1/point-catalog/current")
                        .header("Authorization", "Bearer " + engineerToken)
                        .param("plantId", PLANT_ID).param("lineId", LINE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.snapshot.readyPointCount").value(1));

        JsonNode revokedBody = objectMapper.readTree(pointCatalogBody(sourceRevision, secondObservedAt));
        com.fasterxml.jackson.databind.node.ObjectNode revokedPoint =
                (com.fasterxml.jackson.databind.node.ObjectNode) revokedBody.path("points").get(0);
        revokedPoint.put("sourceSequenceEnabled", false);
        revokedPoint.put("sourceSequenceRequired", false);
        revokedPoint.remove(List.of("sourceSequenceOrigin", "sourceSequenceBindingFingerprint"));
        MvcResult revoked = mockMvc.perform(post("/bpi/v1/point-catalog/snapshots")
                        .header("Authorization", "Bearer " + adminToken)
                        .header("Idempotency-Key", "point-cycle-revoked-" + marker)
                        .header("If-Match", "0")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(revokedBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.snapshot.readyPointCount").value(0))
                .andReturn();

        String firstId = response(first).path("data").path("snapshot").path("id").asText();
        String revokedId = response(revoked).path("data").path("snapshot").path("id").asText();
        assertThat(revokedId).isNotEqualTo(firstId);
        assertThat(jdbc.queryForObject("""
                SELECT count(*)
                  FROM bpi.bpi_point_catalog_snapshots
                 WHERE tenant_id = ? AND source_revision = ?
                """, Integer.class, tenantId, sourceRevision)).isEqualTo(2);

        mockMvc.perform(get("/bpi/v1/point-catalog/current")
                        .header("Authorization", "Bearer " + engineerToken)
                        .param("plantId", PLANT_ID).param("lineId", LINE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.snapshot.id").value(revokedId))
                .andExpect(jsonPath("$.data.snapshot.readyPointCount").value(0))
                .andExpect(jsonPath("$.data.points[0].sourceSequenceEnabled").value(false));
    }

    @Test
    void currentPointCatalogCursorPinsImmutableSnapshotAndSearchScope() throws Exception {
        Instant firstObservedAt = Instant.now().minusSeconds(20);
        MvcResult imported = importPageCatalog(
                marker + "_PAGE_FIRST", firstObservedAt, 5, "point-page-first-" + marker);
        String firstSnapshotId = response(imported).path("data").path("snapshot").path("id").asText();

        MvcResult firstPage = mockMvc.perform(get("/bpi/v1/point-catalog/current")
                        .header("Authorization", "Bearer " + engineerToken)
                        .param("plantId", PLANT_ID).param("lineId", LINE_ID)
                        .param("search", "product-page").param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.snapshot.id").value(firstSnapshotId))
                .andExpect(jsonPath("$.data.points.length()").value(2))
                .andExpect(jsonPath("$.meta.nextCursor").isNotEmpty())
                .andExpect(jsonPath("$.meta.snapshotAt").isNotEmpty())
                .andReturn();
        JsonNode firstBody = response(firstPage);
        String firstCursor = firstBody.path("meta").path("nextCursor").asText();
        Set<String> ids = pointIds(firstBody);

        MvcResult refreshed = importPageCatalog(
                marker + "_PAGE_SECOND", firstObservedAt.plusSeconds(10), 1,
                "point-page-second-" + marker);
        String refreshedSnapshotId = response(refreshed).path("data").path("snapshot").path("id").asText();
        assertThat(refreshedSnapshotId).isNotEqualTo(firstSnapshotId);

        MvcResult secondPage = mockMvc.perform(get("/bpi/v1/point-catalog/current")
                        .header("Authorization", "Bearer " + engineerToken)
                        .param("plantId", PLANT_ID).param("lineId", LINE_ID)
                        .param("search", "product-page").param("limit", "2")
                        .param("cursor", firstCursor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.snapshot.id").value(firstSnapshotId))
                .andExpect(jsonPath("$.data.points.length()").value(2))
                .andExpect(jsonPath("$.meta.nextCursor").isNotEmpty())
                .andReturn();
        JsonNode secondBody = response(secondPage);
        assertThat(ids.addAll(pointIds(secondBody))).isTrue();

        MvcResult thirdPage = mockMvc.perform(get("/bpi/v1/point-catalog/current")
                        .header("Authorization", "Bearer " + engineerToken)
                        .param("plantId", PLANT_ID).param("lineId", LINE_ID)
                        .param("search", "product-page").param("limit", "2")
                        .param("cursor", secondBody.path("meta").path("nextCursor").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.snapshot.id").value(firstSnapshotId))
                .andExpect(jsonPath("$.data.points.length()").value(1))
                .andExpect(jsonPath("$.meta.nextCursor").doesNotExist())
                .andReturn();
        assertThat(ids.addAll(pointIds(response(thirdPage)))).isTrue();
        assertThat(ids).hasSize(5);

        mockMvc.perform(get("/bpi/v1/point-catalog/current")
                        .header("Authorization", "Bearer " + engineerToken)
                        .param("plantId", PLANT_ID).param("lineId", LINE_ID)
                        .param("search", "product-page").param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.snapshot.id").value(refreshedSnapshotId))
                .andExpect(jsonPath("$.data.points.length()").value(1))
                .andExpect(jsonPath("$.meta.nextCursor").doesNotExist());

        mockMvc.perform(get("/bpi/v1/point-catalog/current")
                        .header("Authorization", "Bearer " + engineerToken)
                        .param("plantId", PLANT_ID).param("lineId", LINE_ID)
                        .param("search", "product-page").param("limit", "2")
                        .param("cursor", firstCursor + "a"))
                .andExpect(status().isUnprocessableEntity());
        mockMvc.perform(get("/bpi/v1/point-catalog/current")
                        .header("Authorization", "Bearer " + engineerToken)
                        .param("plantId", PLANT_ID).param("lineId", LINE_ID)
                        .param("search", "different-search").param("limit", "2")
                        .param("cursor", firstCursor))
                .andExpect(status().isUnprocessableEntity());
        mockMvc.perform(get("/bpi/v1/point-catalog/current")
                        .header("Authorization", "Bearer " + engineerToken)
                        .param("plantId", PLANT_ID).param("lineId", LINE_ID)
                        .param("limit", "201"))
                .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(get("/bpi/v1/point-catalog/current")
                        .header("Authorization", "Bearer " + engineerToken)
                        .param("plantId", PLANT_ID).param("lineId", LINE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.snapshot.id").value(refreshedSnapshotId))
                .andExpect(jsonPath("$.data.points.length()").value(1));
        assertThat(count("bpi_point_catalog_snapshots")).isEqualTo(2);
        assertThat(count("bpi_point_catalog_entries")).isEqualTo(6);
    }

    private UUID createTopology(String code, Map<String, Object> definition, String key) throws Exception {
        MvcResult result = mockMvc.perform(post("/bpi/v1/topologies/drafts")
                        .header("Authorization", "Bearer " + engineerToken)
                        .header("Idempotency-Key", key)
                        .header("If-Match", "0")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "code", code,
                                "version", "1.0.0",
                                "plantId", PLANT_ID,
                                "lineId", LINE_ID,
                                "definition", definition,
                                "reason", "创建点位准入验收拓扑"))))
                .andExpect(status().isOk())
                .andReturn();
        return UUID.fromString(response(result).path("data").path("id").asText());
    }

    private byte[] pointCatalogBody() throws Exception {
        return pointCatalogBody(marker, Instant.now().minusSeconds(5));
    }

    private byte[] pointCatalogBody(String sourceRevision, Instant observedAt) throws Exception {
        return objectMapper.writeValueAsBytes(Map.of(
                "source", "JETLINKS",
                "sourceInstance", "acceptance-jetlinks",
                "sourceRevision", sourceRevision,
                "plantId", PLANT_ID,
                "lineId", LINE_ID,
                "observedAt", observedAt.toString(),
                "reason", "导入 JetLinks 点位目录验收快照",
                "points", List.of(
                        Map.ofEntries(
                                Map.entry("localityGroup", "LOCALITY-S07-EVAP"),
                                Map.entry("productId", "PRODUCT-SUGAR"),
                                Map.entry("deviceId", "DEVICE-S07-01"),
                                Map.entry("propertyId", "flow.instant"),
                                Map.entry("sourcePropertyId", "instantFlow"),
                                Map.entry("pointName", "进料瞬时流量"),
                                Map.entry("unit", "m\u00b3/h"),
                                Map.entry("dataType", "double"),
                                Map.entry("deviceState", "ACTIVE"),
                                Map.entry("registered", true),
                                Map.entry("propertyPresent", true),
                                Map.entry("calibrationVersion", "CAL-2026-01"),
                                Map.entry("calibrationStatus", "VERIFIED"),
                                Map.entry("sourceSequenceEnabled", true),
                                Map.entry("sourceSequenceRequired", true),
                                Map.entry("sourceSequenceOrigin", "DEVICE"),
                                Map.entry("sourceSequenceBindingFingerprint",
                                        SourceSequenceEvidenceTestFixture.FINGERPRINT)),
                        Map.ofEntries(
                                Map.entry("localityGroup", "LOCALITY-S07-EVAP"),
                                Map.entry("productId", "PRODUCT-SUGAR"),
                                Map.entry("deviceId", "DEVICE-S07-02"),
                                Map.entry("propertyId", "tank.level"),
                                Map.entry("sourcePropertyId", "tankLevel"),
                                Map.entry("pointName", "未就绪液位"),
                                Map.entry("deviceState", "INACTIVE"),
                                Map.entry("registered", false),
                                Map.entry("propertyPresent", false),
                                Map.entry("calibrationStatus", "MISSING"),
                                Map.entry("sourceSequenceEnabled", false)),
                        Map.ofEntries(
                                Map.entry("localityGroup", "LOCALITY-S07-EVAP"),
                                Map.entry("productId", "PRODUCT-SUGAR"),
                                Map.entry("deviceId", "DEVICE-S07-03"),
                                Map.entry("propertyId", "flow.sequence-missing"),
                                Map.entry("sourcePropertyId", "sequenceMissingFlow"),
                                Map.entry("pointName", "缺来源序列流量"),
                                Map.entry("unit", "m3/h"),
                                Map.entry("dataType", "double"),
                                Map.entry("deviceState", "ACTIVE"),
                                Map.entry("registered", true),
                                Map.entry("propertyPresent", true),
                                Map.entry("calibrationVersion", "CAL-2026-01"),
                                Map.entry("calibrationStatus", "VERIFIED"),
                                Map.entry("sourceSequenceEnabled", false)))));
    }

    private MvcResult importPageCatalog(
            String sourceRevision,
            Instant observedAt,
            int pointCount,
            String idempotencyKey) throws Exception {
        List<Map<String, Object>> points = new ArrayList<>();
        for (int index = 0; index < pointCount; index++) {
            points.add(Map.ofEntries(
                    Map.entry("localityGroup", "LOCALITY-PAGE"),
                    Map.entry("productId", "PRODUCT-PAGE"),
                    Map.entry("deviceId", "DEVICE-PAGE-" + String.format("%03d", index)),
                    Map.entry("propertyId", "flow.page." + String.format("%03d", index)),
                    Map.entry("sourcePropertyId", "pageFlow" + index),
                    Map.entry("pointName", "分页点位 " + String.format("%03d", index)),
                    Map.entry("unit", "m3/h"),
                    Map.entry("dataType", "double"),
                    Map.entry("deviceState", "ACTIVE"),
                    Map.entry("registered", true),
                    Map.entry("propertyPresent", true),
                    Map.entry("calibrationVersion", "CAL-PAGE-01"),
                    Map.entry("calibrationStatus", "VERIFIED"),
                    Map.entry("sourceSequenceEnabled", true),
                    Map.entry("sourceSequenceRequired", true),
                    Map.entry("sourceSequenceOrigin", "DEVICE"),
                    Map.entry("sourceSequenceBindingFingerprint",
                            SourceSequenceEvidenceTestFixture.FINGERPRINT)));
        }
        byte[] body = objectMapper.writeValueAsBytes(Map.of(
                "source", "JETLINKS",
                "sourceInstance", "acceptance-page-jetlinks",
                "sourceRevision", sourceRevision,
                "plantId", PLANT_ID,
                "lineId", LINE_ID,
                "observedAt", observedAt.toString(),
                "reason", "验收点位目录稳定游标分页",
                "points", points));
        return mockMvc.perform(post("/bpi/v1/point-catalog/snapshots")
                        .header("Authorization", "Bearer " + adminToken)
                        .header("Idempotency-Key", idempotencyKey)
                        .header("If-Match", "0")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.snapshot.pointCount").value(pointCount))
                .andReturn();
    }

    private void qualifyReadyDevice(String sourceEventId) {
        SourceSequenceEvidenceTestFixture.qualifyCurrentDevice(
                jdbc, tenantId, PLANT_ID, LINE_ID, "PRODUCT-SUGAR", "DEVICE-S07-01", sourceEventId);
    }

    private Set<String> pointIds(JsonNode body) {
        Set<String> ids = new HashSet<>();
        body.path("data").path("points").forEach(point -> ids.add(point.path("id").asText()));
        return ids;
    }

    private Map<String, Object> readyDefinition() {
        return definition("DEVICE-S07-01", "flow.instant", "m3/h", "CAL-2026-01");
    }

    private Map<String, Object> blockedDefinition() {
        return definition("DEVICE-S07-02", "tank.level", "m", "CAL-MISSING");
    }

    private Map<String, Object> sequenceBlockedDefinition() {
        return definition("DEVICE-S07-03", "flow.sequence-missing", "m3/h", "CAL-2026-01");
    }

    private Map<String, Object> definition(
            String deviceId, String propertyId, String unit, String calibrationVersion) {
        return Map.of(
                "localityGroup", "LOCALITY-S07-EVAP",
                "nodes", List.of(
                        Map.of("code", "FEED-TANK", "type", "TANK"),
                        Map.of("code", "FLOW-METER", "type", "METER")),
                "edges", List.of(Map.of("from", "FEED-TANK", "to", "FLOW-METER")),
                "bindings", List.of(Map.of(
                        "signal", "feed.flow",
                        "productId", "PRODUCT-SUGAR",
                        "deviceId", deviceId,
                        "propertyId", propertyId,
                        "expectedUnit", unit,
                        "calibrationVersion", calibrationVersion)),
                "requiredSignals", List.of("feed.flow"));
    }

    private byte[] reason(String value) throws Exception {
        return objectMapper.writeValueAsBytes(Map.of("reason", value));
    }

    private void insertApprovedCalibration(
            String deviceId,
            String propertyId,
            String calibrationVersion) {
        jdbc.update("""
                INSERT INTO bpi.bpi_point_calibrations
                    (id, tenant_id, plant_id, line_id, product_id, device_id, property_id,
                     calibration_version, certificate_reference, certificate_checksum,
                     valid_from, valid_until, state, revision, submitted_by, submit_reason,
                     decided_by, decided_at, decision_reason)
                VALUES (?, ?, ?, ?, 'PRODUCT-SUGAR', ?, ?, ?, ?, ?,
                        now() - interval '1 day', now() + interval '1 year',
                        'APPROVED', 2, 'calibration-author', '验收校准证据',
                        'calibration-reviewer', now(), '独立复核通过')
                """, UUID.randomUUID(), tenantId, PLANT_ID, LINE_ID, deviceId, propertyId,
                calibrationVersion, "urn:adp:test:" + deviceId + ":" + propertyId,
                "c".repeat(64));
    }

    private JsonNode response(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private long count(String table) {
        Long value = jdbc.queryForObject(
                "SELECT count(*) FROM bpi." + table + " WHERE tenant_id = ?", Long.class, tenantId);
        return value == null ? 0 : value;
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
