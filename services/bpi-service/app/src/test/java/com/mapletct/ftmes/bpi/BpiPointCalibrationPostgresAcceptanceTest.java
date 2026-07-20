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
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "BPI_TEST_DATABASE_URL", matches = ".+")
class BpiPointCalibrationPostgresAcceptanceTest {
    private static final String SECRET = "bpi-test-secret-0123456789abcdef";
    private static final String PLANT_ID = "PLANT-01";
    private static final String LINE_ID = "LINE-S07-01";
    private static final String PRODUCT_ID = "PRODUCT-SUGAR";
    private static final String DEVICE_ID = "DEVICE-S07-01";
    private static final String PROPERTY_ID = "flow.instant";

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
    private String calibrationVersion;
    private String authorToken;
    private String authorAdminToken;
    private String reviewerToken;
    private String viewerToken;

    @BeforeEach
    void setUpTenant() throws Exception {
        tenantId = "ADP_E2E_BPI_CAL_" + UUID.randomUUID().toString().replace("-", "");
        calibrationVersion = "CAL-" + tenantId.substring(tenantId.length() - 12);
        authorToken = token("calibration-author", List.of("BPI_ENGINEER"));
        authorAdminToken = token("calibration-author", List.of("BPI_ADMIN"));
        reviewerToken = token("calibration-reviewer", List.of("BPI_ADMIN"));
        viewerToken = token("calibration-viewer", List.of("BPI_VIEWER"));
        jdbc.update("""
                INSERT INTO bpi.bpi_feature_flags
                    (id, tenant_id, scope_type, scope_key, flag_key, enabled, revision, updated_by)
                VALUES (?, ?, 'LINE', ?, 'bpi.rule-management', true, 1, 'acceptance')
                """, UUID.randomUUID(), tenantId, LINE_ID);
    }

    @AfterEach
    void cleanupMarker() {
        if (tenantId == null) return;
        jdbc.update("DELETE FROM bpi.bpi_audit_events WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_api_idempotency WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_outbox_events WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_rule_versions WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_topology_versions WHERE tenant_id = ?", tenantId);
        SourceSequenceEvidenceTestFixture.cleanup(jdbc, tenantId);
        jdbc.update("DELETE FROM bpi.bpi_point_catalog_entries WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_point_catalog_snapshots WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_point_calibrations WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_feature_flags WHERE tenant_id = ?", tenantId);
    }

    @Test
    void independentCalibrationApprovalControlsReadinessExpiryAndRevocation() throws Exception {
        importSourceClaimedVerifiedSnapshot();

        mockMvc.perform(get("/bpi/v1/point-catalog/current")
                        .header("Authorization", "Bearer " + viewerToken)
                        .param("plantId", PLANT_ID).param("lineId", LINE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.snapshot.readyPointCount").value(0))
                .andExpect(jsonPath("$.data.points[0].sourceCalibrationStatus").value("VERIFIED"))
                .andExpect(jsonPath("$.data.points[0].calibrationStatus").value("UNVERIFIED"))
                .andExpect(jsonPath("$.data.points[0].calibrationEvidenceId").doesNotExist())
                .andExpect(jsonPath("$.data.points[0].readinessIssues[*]")
                        .value(hasItem("CALIBRATION_NOT_VERIFIED")));

        byte[] submission = calibrationBody(calibrationVersion, Instant.now().plusSeconds(86_400));
        mockMvc.perform(post("/bpi/v1/point-calibrations")
                        .header("Authorization", "Bearer " + viewerToken)
                        .header("Idempotency-Key", "calibration-viewer-denied-" + tenantId)
                        .header("If-Match", "0")
                        .contentType(MediaType.APPLICATION_JSON).content(submission))
                .andExpect(status().isForbidden());

        String submitKey = "calibration-submit-" + tenantId;
        MvcResult submitted = mockMvc.perform(post("/bpi/v1/point-calibrations")
                        .header("Authorization", "Bearer " + authorToken)
                        .header("Idempotency-Key", submitKey)
                        .header("If-Match", "0")
                        .header("X-Trace-Id", tenantId + "_SUBMIT")
                        .contentType(MediaType.APPLICATION_JSON).content(submission))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("PENDING"))
                .andExpect(jsonPath("$.data.revision").value(1))
                .andExpect(jsonPath("$.data.effective").value(false))
                .andReturn();
        UUID calibrationId = UUID.fromString(response(submitted).path("data").path("id").asText());

        mockMvc.perform(post("/bpi/v1/point-calibrations")
                        .header("Authorization", "Bearer " + authorToken)
                        .header("Idempotency-Key", submitKey)
                        .header("If-Match", "0")
                        .contentType(MediaType.APPLICATION_JSON).content(submission))
                .andExpect(status().isOk())
                .andExpect(header().string("Idempotent-Replay", "true"))
                .andExpect(jsonPath("$.data.id").value(calibrationId.toString()));

        mockMvc.perform(post("/bpi/v1/point-calibrations")
                        .header("Authorization", "Bearer " + authorToken)
                        .header("Idempotency-Key", "calibration-duplicate-" + tenantId)
                        .header("If-Match", "0")
                        .contentType(MediaType.APPLICATION_JSON).content(submission))
                .andExpect(status().isConflict());

        byte[] approveReason = reason("独立管理员核对证书、仪表和有效期后批准");
        mockMvc.perform(post("/bpi/v1/point-calibrations/{id}/approve", calibrationId)
                        .header("Authorization", "Bearer " + authorAdminToken)
                        .header("Idempotency-Key", "calibration-self-approve-" + tenantId)
                        .header("If-Match", "1")
                        .contentType(MediaType.APPLICATION_JSON).content(approveReason))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail", containsString("reviewer other than the submitter")));

        String approveKey = "calibration-approve-" + tenantId;
        mockMvc.perform(post("/bpi/v1/point-calibrations/{id}/approve", calibrationId)
                        .header("Authorization", "Bearer " + reviewerToken)
                        .header("Idempotency-Key", approveKey)
                        .header("If-Match", "1")
                        .header("X-Trace-Id", tenantId + "_APPROVE")
                        .contentType(MediaType.APPLICATION_JSON).content(approveReason))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("APPROVED"))
                .andExpect(jsonPath("$.data.revision").value(2))
                .andExpect(jsonPath("$.data.effective").value(true))
                .andExpect(jsonPath("$.data.effectivenessStatus").value("EFFECTIVE"));
        mockMvc.perform(post("/bpi/v1/point-calibrations/{id}/approve", calibrationId)
                        .header("Authorization", "Bearer " + reviewerToken)
                        .header("Idempotency-Key", approveKey)
                        .header("If-Match", "1")
                        .contentType(MediaType.APPLICATION_JSON).content(approveReason))
                .andExpect(status().isOk())
                .andExpect(header().string("Idempotent-Replay", "true"));

        assertCurrentReadiness(1, "VERIFIED", calibrationId.toString());

        jdbc.update("""
                UPDATE bpi.bpi_point_catalog_snapshots
                   SET observed_at = now() + interval '2 minutes'
                 WHERE tenant_id = ?
                """, tenantId);
        jdbc.update("""
                UPDATE bpi.bpi_point_calibrations
                   SET valid_until = now() + interval '1 minute', updated_at = now()
                 WHERE tenant_id = ? AND id = ?
                """, tenantId, calibrationId);
        assertCurrentReadiness(0, "UNVERIFIED", null);
        jdbc.update("""
                UPDATE bpi.bpi_point_catalog_snapshots
                   SET observed_at = now() - interval '5 seconds'
                 WHERE tenant_id = ?
                """, tenantId);
        jdbc.update("""
                UPDATE bpi.bpi_point_calibrations
                   SET valid_until = now() + interval '1 day', updated_at = now()
                 WHERE tenant_id = ? AND id = ?
                """, tenantId, calibrationId);
        assertCurrentReadiness(1, "VERIFIED", calibrationId.toString());

        UUID topologyId = createTopology();
        mockMvc.perform(post("/bpi/v1/topologies/{id}/validate", topologyId)
                        .header("Authorization", "Bearer " + authorToken)
                        .header("Idempotency-Key", "calibration-topology-validate-" + tenantId)
                        .header("If-Match", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reason("使用权威校准证据验证拓扑")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.validationStatus").value("PASSED"))
                .andExpect(jsonPath("$.data.revision").value(2));

        jdbc.update("""
                UPDATE bpi.bpi_point_calibrations
                   SET valid_until = now() - interval '1 second', updated_at = now()
                 WHERE tenant_id = ? AND id = ?
                """, tenantId, calibrationId);
        assertCurrentReadiness(0, "UNVERIFIED", null);
        mockMvc.perform(get("/bpi/v1/point-calibrations")
                        .header("Authorization", "Bearer " + viewerToken)
                        .param("plantId", PLANT_ID).param("lineId", LINE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].state").value("APPROVED"))
                .andExpect(jsonPath("$.data[0].effective").value(false))
                .andExpect(jsonPath("$.data[0].effectivenessStatus").value("EXPIRED"));
        mockMvc.perform(post("/bpi/v1/topologies/{id}/publish", topologyId)
                        .header("Authorization", "Bearer " + reviewerToken)
                        .header("Idempotency-Key", "calibration-expired-publish-" + tenantId)
                        .header("If-Match", "2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reason("过期证据不得发布拓扑")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail", containsString("POINT_CALIBRATION_NOT_VERIFIED")));

        jdbc.update("""
                UPDATE bpi.bpi_point_calibrations
                   SET valid_until = now() + interval '1 day', updated_at = now()
                 WHERE tenant_id = ? AND id = ?
                """, tenantId, calibrationId);
        assertCurrentReadiness(1, "VERIFIED", calibrationId.toString());

        String dependencyReference = createPublishedRuleDependency(topologyId);
        mockMvc.perform(post("/bpi/v1/point-calibrations/{id}/revoke", calibrationId)
                        .header("Authorization", "Bearer " + reviewerToken)
                        .header("Idempotency-Key", "calibration-active-rule-revoke-" + tenantId)
                        .header("If-Match", "2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reason("规则仍在运行时生效期间不得撤销校准证据")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail", containsString(dependencyReference)))
                .andExpect(jsonPath("$.detail", containsString("runtime INACTIVE")));
        assertThat(jdbc.queryForObject("""
                SELECT state || '|' || revision
                  FROM bpi.bpi_point_calibrations
                 WHERE tenant_id = ? AND id = ?
                """, String.class, tenantId, calibrationId)).isEqualTo("APPROVED|2");
        completeRuleRetirement(dependencyReference.substring(0, dependencyReference.indexOf('@')));

        mockMvc.perform(post("/bpi/v1/point-calibrations/{id}/revoke", calibrationId)
                        .header("Authorization", "Bearer " + reviewerToken)
                        .header("Idempotency-Key", "calibration-revoke-" + tenantId)
                        .header("If-Match", "2")
                        .header("X-Trace-Id", tenantId + "_REVOKE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reason("现场复核发现仪表已更换，撤销旧校准证据")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("REVOKED"))
                .andExpect(jsonPath("$.data.revision").value(3))
                .andExpect(jsonPath("$.data.effective").value(false));
        assertCurrentReadiness(0, "UNVERIFIED", null);

        String rejectedVersion = calibrationVersion + "-R";
        MvcResult rejectedSubmission = mockMvc.perform(post("/bpi/v1/point-calibrations")
                        .header("Authorization", "Bearer " + authorToken)
                        .header("Idempotency-Key", "calibration-reject-submit-" + tenantId)
                        .header("If-Match", "0")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(calibrationBody(rejectedVersion, Instant.now().plusSeconds(86_400))))
                .andExpect(status().isOk()).andReturn();
        UUID rejectedId = UUID.fromString(response(rejectedSubmission).path("data").path("id").asText());
        mockMvc.perform(post("/bpi/v1/point-calibrations/{id}/reject", rejectedId)
                        .header("Authorization", "Bearer " + reviewerToken)
                        .header("Idempotency-Key", "calibration-reject-" + tenantId)
                        .header("If-Match", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reason("证书校验和与受控附件不一致，拒绝")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("REJECTED"))
                .andExpect(jsonPath("$.data.revision").value(2));

        String futureVersion = calibrationVersion + "-FUTURE";
        Instant futureFrom = Instant.now().plusSeconds(3_600);
        MvcResult futureSubmission = mockMvc.perform(post("/bpi/v1/point-calibrations")
                        .header("Authorization", "Bearer " + authorToken)
                        .header("Idempotency-Key", "calibration-future-submit-" + tenantId)
                        .header("If-Match", "0")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(calibrationBody(futureVersion, futureFrom, futureFrom.plusSeconds(86_400))))
                .andExpect(status().isOk()).andReturn();
        UUID futureId = UUID.fromString(response(futureSubmission).path("data").path("id").asText());
        mockMvc.perform(post("/bpi/v1/point-calibrations/{id}/approve", futureId)
                        .header("Authorization", "Bearer " + reviewerToken)
                        .header("Idempotency-Key", "calibration-future-approve-" + tenantId)
                        .header("If-Match", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reason("批准未来生效的计划校准证据")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("APPROVED"))
                .andExpect(jsonPath("$.data.effective").value(false))
                .andExpect(jsonPath("$.data.effectivenessStatus").value("NOT_YET_EFFECTIVE"));
        jdbc.update("""
                UPDATE bpi.bpi_point_catalog_entries
                   SET calibration_version = ?
                 WHERE tenant_id = ?
                """, futureVersion, tenantId);
        jdbc.update("""
                UPDATE bpi.bpi_point_catalog_snapshots
                   SET observed_at = ?
                 WHERE tenant_id = ?
                """, java.sql.Timestamp.from(futureFrom.plusSeconds(60)), tenantId);
        assertCurrentReadiness(0, "UNVERIFIED", null);

        mockMvc.perform(post("/bpi/v1/point-calibrations")
                        .header("Authorization", "Bearer " + authorToken)
                        .header("Idempotency-Key", "calibration-expired-submit-" + tenantId)
                        .header("If-Match", "0")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(calibrationBody(calibrationVersion + "-EXPIRED", Instant.now().minusSeconds(1))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail", containsString("Expired calibration evidence")));

        assertThat(jdbc.queryForObject("""
                SELECT state || '|' || revision || '|' || submitted_by || '|' || decided_by || '|' || revoked_by
                  FROM bpi.bpi_point_calibrations
                 WHERE tenant_id = ? AND id = ?
                """, String.class, tenantId, calibrationId))
                .isEqualTo("REVOKED|3|calibration-author|calibration-reviewer|calibration-reviewer");
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM bpi.bpi_audit_events
                 WHERE tenant_id = ? AND object_type = 'POINT_CALIBRATION'
                   AND action = 'POINT_CALIBRATION_SUBMITTED'
                """, Integer.class, tenantId)).isEqualTo(3);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM bpi.bpi_audit_events
                 WHERE tenant_id = ? AND object_type = 'POINT_CALIBRATION'
                   AND action IN ('POINT_CALIBRATION_APPROVED', 'POINT_CALIBRATION_REJECTED',
                                  'POINT_CALIBRATION_REVOKED')
                """, Integer.class, tenantId)).isEqualTo(4);
    }

    @Test
    void calibrationListUsesStableScopeBoundKeysetCursor() throws Exception {
        Instant base = Instant.now().minusSeconds(90);
        UUID oldestId = submitCalibration("PAGE-OLDEST");
        UUID middleId = submitCalibration("PAGE-MIDDLE");
        UUID newestId = submitCalibration("PAGE-NEWEST");
        setSubmittedAt(oldestId, base);
        setSubmittedAt(middleId, base);
        setSubmittedAt(newestId, base);
        List<UUID> expectedOrder = jdbc.query("""
                SELECT id
                  FROM bpi.bpi_point_calibrations
                 WHERE tenant_id = ?
                 ORDER BY submitted_at DESC, id DESC
                """, (resultSet, rowNum) -> resultSet.getObject("id", UUID.class), tenantId);

        MvcResult firstResult = mockMvc.perform(get("/bpi/v1/point-calibrations")
                        .header("Authorization", "Bearer " + viewerToken)
                        .param("plantId", PLANT_ID).param("lineId", LINE_ID)
                        .param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.meta.nextCursor").isNotEmpty())
                .andReturn();
        JsonNode first = response(firstResult);
        Instant snapshotAt = Instant.parse(first.path("meta").path("snapshotAt").asText());
        String nextCursor = first.path("meta").path("nextCursor").asText();
        assertThat(first.path("data").findValuesAsText("id"))
                .containsExactly(
                        expectedOrder.get(0).toString(),
                        expectedOrder.get(1).toString());

        UUID afterSnapshotId = submitCalibration("PAGE-AFTER-SNAPSHOT");
        Instant afterSnapshotSubmittedAt = jdbc.queryForObject("""
                SELECT submitted_at
                  FROM bpi.bpi_point_calibrations
                 WHERE tenant_id = ? AND id = ?
                """, Timestamp.class, tenantId, afterSnapshotId).toInstant();
        if (!afterSnapshotSubmittedAt.isAfter(snapshotAt)) {
            setSubmittedAt(afterSnapshotId, snapshotAt.plusNanos(1_000));
        }

        MvcResult secondResult = mockMvc.perform(get("/bpi/v1/point-calibrations")
                        .header("Authorization", "Bearer " + viewerToken)
                        .param("plantId", PLANT_ID).param("lineId", LINE_ID)
                        .param("limit", "2").param("cursor", nextCursor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.meta.nextCursor").doesNotExist())
                .andReturn();
        JsonNode second = response(secondResult);
        assertThat(second.path("meta").path("snapshotAt").asText())
                .isEqualTo(snapshotAt.toString());
        assertThat(second.path("data").findValuesAsText("id"))
                .containsExactly(expectedOrder.get(2).toString())
                .doesNotContain(afterSnapshotId.toString());

        mockMvc.perform(get("/bpi/v1/point-calibrations")
                        .header("Authorization", "Bearer " + viewerToken)
                        .param("plantId", PLANT_ID).param("lineId", LINE_ID)
                        .param("cursor", nextCursor).param("productId", "OTHER-PRODUCT"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail", containsString("does not match")));
        mockMvc.perform(get("/bpi/v1/point-calibrations")
                        .header("Authorization", "Bearer " + viewerToken)
                        .param("plantId", PLANT_ID).param("lineId", LINE_ID)
                        .param("cursor", "not-a-cursor"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail", containsString("cursor is invalid")));
        String tamperedCursor = (nextCursor.startsWith("A") ? "B" : "A") + nextCursor.substring(1);
        mockMvc.perform(get("/bpi/v1/point-calibrations")
                        .header("Authorization", "Bearer " + viewerToken)
                        .param("plantId", PLANT_ID).param("lineId", LINE_ID)
                        .param("cursor", tamperedCursor))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail", containsString("cursor is invalid")));
        mockMvc.perform(get("/bpi/v1/point-calibrations")
                        .header("Authorization", "Bearer " + viewerToken)
                        .param("plantId", PLANT_ID).param("lineId", LINE_ID)
                        .param("limit", "201"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail", containsString("between 1 and 200")));

        assertThat(jdbc.queryForObject("""
                SELECT count(*)
                  FROM bpi.bpi_point_calibrations
                 WHERE tenant_id = ?
                """, Integer.class, tenantId)).isEqualTo(4);
    }

    private UUID submitCalibration(String suffix) throws Exception {
        MvcResult result = mockMvc.perform(post("/bpi/v1/point-calibrations")
                        .header("Authorization", "Bearer " + authorToken)
                        .header("Idempotency-Key", "calibration-" + suffix + "-" + tenantId)
                        .header("If-Match", "0")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(calibrationBody(
                                calibrationVersion + "-" + suffix,
                                Instant.now().plusSeconds(86_400))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("PENDING"))
                .andReturn();
        return UUID.fromString(response(result).path("data").path("id").asText());
    }

    private void setSubmittedAt(UUID calibrationId, Instant submittedAt) {
        jdbc.update("""
                UPDATE bpi.bpi_point_calibrations
                   SET submitted_at = ?, updated_at = now()
                 WHERE tenant_id = ? AND id = ?
                """, Timestamp.from(submittedAt), tenantId, calibrationId);
    }

    private void importSourceClaimedVerifiedSnapshot() throws Exception {
        mockMvc.perform(post("/bpi/v1/point-catalog/snapshots")
                        .header("Authorization", "Bearer " + reviewerToken)
                        .header("Idempotency-Key", "calibration-point-import-" + tenantId)
                        .header("If-Match", "0")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "source", "JETLINKS",
                                "sourceInstance", "calibration-acceptance",
                                "sourceRevision", tenantId,
                                "plantId", PLANT_ID,
                                "lineId", LINE_ID,
                                "observedAt", Instant.now().minusSeconds(5).toString(),
                                "reason", "验证来源自声明不能绕过 MES 校准审批",
                                "points", List.of(Map.ofEntries(
                                        Map.entry("productId", PRODUCT_ID),
                                        Map.entry("deviceId", DEVICE_ID),
                                        Map.entry("propertyId", PROPERTY_ID),
                                        Map.entry("sourcePropertyId", "instantFlow"),
                                        Map.entry("pointName", "进料瞬时流量"),
                                        Map.entry("unit", "m3/h"),
                                        Map.entry("dataType", "double"),
                                        Map.entry("deviceState", "ACTIVE"),
                                        Map.entry("registered", true),
                                        Map.entry("propertyPresent", true),
                                        Map.entry("calibrationVersion", calibrationVersion),
                                        Map.entry("calibrationStatus", "VERIFIED"),
                                        Map.entry("sourceSequenceEnabled", true),
                                        Map.entry("sourceSequenceRequired", true),
                                        Map.entry("sourceSequenceOrigin", "DEVICE"),
                                        Map.entry("sourceSequenceBindingFingerprint",
                                                SourceSequenceEvidenceTestFixture.FINGERPRINT)))))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.snapshot.readyPointCount").value(0));
        SourceSequenceEvidenceTestFixture.qualifyCurrentDevice(
                jdbc, tenantId, PLANT_ID, LINE_ID, PRODUCT_ID, DEVICE_ID,
                tenantId + "_SOURCE_SEQUENCE");
    }

    private UUID createTopology() throws Exception {
        MvcResult result = mockMvc.perform(post("/bpi/v1/topologies/drafts")
                        .header("Authorization", "Bearer " + authorToken)
                        .header("Idempotency-Key", "calibration-topology-create-" + tenantId)
                        .header("If-Match", "0")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "code", tenantId + "_TOPOLOGY",
                                "version", "1.0.0",
                                "plantId", PLANT_ID,
                                "lineId", LINE_ID,
                                "reason", "验证校准证据控制拓扑准入",
                                "definition", Map.of(
                                        "localityGroup", "LOCALITY-S07-EVAP",
                                        "nodes", List.of(
                                                Map.of("code", "FEED", "type", "TANK"),
                                                Map.of("code", "METER", "type", "METER")),
                                        "edges", List.of(Map.of("from", "FEED", "to", "METER")),
                                        "bindings", List.of(Map.of(
                                                "signal", "feed.flow",
                                                "productId", PRODUCT_ID,
                                                "deviceId", DEVICE_ID,
                                                "propertyId", PROPERTY_ID,
                                                "expectedUnit", "m3/h",
                                                "calibrationVersion", calibrationVersion)),
                                        "requiredSignals", List.of("feed.flow"))))))
                .andExpect(status().isOk()).andReturn();
        return UUID.fromString(response(result).path("data").path("id").asText());
    }

    private String createPublishedRuleDependency(UUID topologyId) {
        String ruleCode = tenantId + "_CALIBRATION_DEPENDENCY";
        UUID ruleId = UUID.randomUUID();
        jdbc.update("""
                UPDATE bpi.bpi_topology_versions
                   SET state = 'PUBLISHED', updated_by = 'acceptance', updated_at = now()
                 WHERE tenant_id = ? AND id = ?
                """, tenantId, topologyId);
        jdbc.update("""
                INSERT INTO bpi.bpi_rule_versions
                    (id, tenant_id, rule_code, version, topology_version_id, state,
                     checksum, definition, revision, created_by, plant_id, line_id, updated_by)
                VALUES (?, ?, ?, '1.0.0', ?, 'PUBLISHED', ?, '{}'::jsonb, 1,
                        'acceptance', ?, ?, 'acceptance')
                """, ruleId, tenantId, ruleCode, topologyId, "b".repeat(64), PLANT_ID, LINE_ID);
        insertRuleLifecycle(ruleId, "ACTIVATE", 1, true, "READY");
        return ruleCode + "@1.0.0";
    }

    private void completeRuleRetirement(String ruleCode) {
        UUID ruleId = jdbc.queryForObject("""
                SELECT id
                  FROM bpi.bpi_rule_versions
                 WHERE tenant_id = ? AND rule_code = ?
                """, UUID.class, tenantId, ruleCode);
        jdbc.update("""
                UPDATE bpi.bpi_rule_versions
                   SET state = 'RETIRED', revision = revision + 1,
                       updated_by = 'acceptance', updated_at = now()
                 WHERE tenant_id = ? AND id = ?
                """, tenantId, ruleId);
        insertRuleLifecycle(ruleId, "RETIRE", 2, false, "INACTIVE");
    }

    private void insertRuleLifecycle(
            UUID ruleId,
            String lifecycleAction,
            long lifecycleSequence,
            boolean lifecycleActive,
            String runtimeReadinessStatus) {
        jdbc.update("""
                INSERT INTO bpi.bpi_outbox_events
                    (id, tenant_id, plant_id, line_id, aggregate_type, aggregate_id,
                     event_type, topic, partition_key, payload, headers, status,
                     application_status, runtime_readiness_status,
                     lifecycle_action, lifecycle_sequence, lifecycle_active)
                VALUES (?, ?, ?, ?, 'RULE_VERSION', ?, 'BOUNDARY_RULE_PUBLISHED',
                        'bpi.boundary.rule-publication.v1', ?, ?, '{}'::jsonb, 'PUBLISHED',
                        'APPLIED', ?, ?, ?, ?)
                """, UUID.randomUUID(), tenantId, PLANT_ID, LINE_ID, ruleId,
                tenantId + "|" + ruleId, new byte[] {1}, runtimeReadinessStatus,
                lifecycleAction, lifecycleSequence, lifecycleActive);
    }

    private void assertCurrentReadiness(int count, String statusValue, String evidenceId) throws Exception {
        var assertion = mockMvc.perform(get("/bpi/v1/point-catalog/current")
                        .header("Authorization", "Bearer " + viewerToken)
                        .param("plantId", PLANT_ID).param("lineId", LINE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.snapshot.readyPointCount").value(count))
                .andExpect(jsonPath("$.data.points[0].calibrationStatus").value(statusValue))
                .andExpect(jsonPath("$.data.points[0].ready").value(count == 1));
        if (evidenceId == null) {
            assertion.andExpect(jsonPath("$.data.points[0].calibrationEvidenceId").doesNotExist());
        } else {
            assertion.andExpect(jsonPath("$.data.points[0].calibrationEvidenceId").value(evidenceId));
        }
    }

    private byte[] calibrationBody(String version, Instant validUntil) throws Exception {
        return calibrationBody(version, Instant.now().minusSeconds(86_400), validUntil);
    }

    private byte[] calibrationBody(String version, Instant validFrom, Instant validUntil) throws Exception {
        return objectMapper.writeValueAsBytes(Map.ofEntries(
                Map.entry("plantId", PLANT_ID),
                Map.entry("lineId", LINE_ID),
                Map.entry("productId", PRODUCT_ID),
                Map.entry("deviceId", DEVICE_ID),
                Map.entry("propertyId", PROPERTY_ID),
                Map.entry("calibrationVersion", version),
                Map.entry("certificateReference", "urn:adp:calibration:" + tenantId + ":" + version),
                Map.entry("certificateChecksum", "a".repeat(64)),
                Map.entry("validFrom", validFrom.toString()),
                Map.entry("validUntil", validUntil.toString()),
                Map.entry("reason", "提交受控仪表校准证据并申请独立复核")));
    }

    private byte[] reason(String value) throws Exception {
        return objectMapper.writeValueAsBytes(Map.of("reason", value));
    }

    private JsonNode response(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
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
