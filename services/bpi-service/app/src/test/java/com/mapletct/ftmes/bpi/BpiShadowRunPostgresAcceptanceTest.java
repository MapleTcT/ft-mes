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

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
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
class BpiShadowRunPostgresAcceptanceTest {
    private static final String SECRET = "bpi-test-secret-0123456789abcdef";
    private static final String PLANT_ID = "PLANT-01";
    private static final String LINE_ID = "LINE-S07-01";

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> System.getenv("BPI_TEST_DATABASE_URL"));
        registry.add("spring.datasource.username", () -> env("BPI_TEST_DATABASE_USER", "postgres"));
        registry.add("spring.datasource.password", () -> env("BPI_TEST_DATABASE_PASSWORD", ""));
        registry.add("bpi.security.internal-jwt-secret", () -> SECRET);
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JdbcTemplate jdbc;

    private String tenantId;
    private UUID snapshotId;
    private UUID topologyId;
    private UUID ruleId;
    private UUID outboxId;
    private String authorToken;
    private String authorAdminToken;
    private String reviewerToken;
    private String independentAdminToken;
    private String viewerToken;

    @BeforeEach
    void setUpPublishedBinding() throws Exception {
        tenantId = "ADP_E2E_BPI_SHADOW_" + UUID.randomUUID().toString().replace("-", "");
        snapshotId = UUID.randomUUID();
        topologyId = UUID.randomUUID();
        ruleId = UUID.randomUUID();
        outboxId = UUID.randomUUID();
        authorToken = token("shadow-author", List.of("BPI_ENGINEER"));
        authorAdminToken = token("shadow-author", List.of("BPI_ADMIN"));
        reviewerToken = token("shadow-reviewer", List.of("BPI_SHIFT_LEAD"));
        independentAdminToken = token("shadow-admin", List.of("BPI_ADMIN"));
        viewerToken = token("shadow-viewer", List.of("BPI_VIEWER"));

        jdbc.update("""
                INSERT INTO bpi.bpi_point_catalog_snapshots
                    (id, tenant_id, source, source_instance, source_revision, plant_id, line_id,
                     checksum, observed_at, point_count, source_claim_ready_point_count, imported_by)
                VALUES (?, ?, 'JETLINKS', 'shadow-run-acceptance', 'revision-1', ?, ?, ?, now(), 1, 1, 'fixture')
                """, snapshotId, tenantId, PLANT_ID, LINE_ID, "a".repeat(64));
        jdbc.update("""
                INSERT INTO bpi.bpi_point_catalog_entries
                    (id, tenant_id, snapshot_id, plant_id, line_id, product_id, device_id,
                     property_id, source_property_id, point_name, unit, data_type, device_state,
                     registered, property_present, calibration_version, calibration_status,
                     source_sequence_enabled, source_sequence_required, source_sequence_origin,
                     source_sequence_binding_fingerprint)
                VALUES (?, ?, ?, ?, ?, 'PRODUCT-SUGAR', 'DEVICE-SHADOW',
                        'flow.instant', 'flow.instant', 'Shadow flow', 't/h', 'DECIMAL', 'ACTIVE',
                        true, true, 'CAL-SHADOW-1', 'VERIFIED', true, true, 'DEVICE', ?)
                """, UUID.randomUUID(), tenantId, snapshotId, PLANT_ID, LINE_ID,
                SourceSequenceEvidenceTestFixture.FINGERPRINT);
        SourceSequenceEvidenceTestFixture.qualifyCurrentDevice(
                jdbc, tenantId, PLANT_ID, LINE_ID, "PRODUCT-SUGAR", "DEVICE-SHADOW",
                tenantId + "_SOURCE_SEQUENCE");
        jdbc.update("""
                INSERT INTO bpi.bpi_point_calibrations
                    (id, tenant_id, plant_id, line_id, product_id, device_id, property_id,
                     calibration_version, certificate_reference, certificate_checksum,
                     valid_from, valid_until, state, revision, submitted_by, submit_reason,
                     decided_by, decided_at, decision_reason)
                VALUES (?, ?, ?, ?, 'PRODUCT-SUGAR', 'DEVICE-SHADOW', 'flow.instant',
                        'CAL-SHADOW-1', 'certificate://shadow-run-fixture', ?,
                        now() - interval '1 day', now() + interval '30 days',
                        'APPROVED', 2, 'fixture-author', 'Fixture submission',
                        'fixture-reviewer', now(), 'Fixture approval')
                """, UUID.randomUUID(), tenantId, PLANT_ID, LINE_ID, "d".repeat(64));
        jdbc.update("""
                INSERT INTO bpi.bpi_topology_versions
                    (id, tenant_id, topology_code, version, state, checksum, definition,
                     plant_id, line_id, revision, created_by, updated_by,
                     validation_status, validated_checksum, validated_by, validated_at,
                     validated_point_catalog_snapshot_id, validated_point_catalog_checksum,
                     published_by, published_at)
                VALUES (?, ?, 'TOPOLOGY-SHADOW', '1.0.0', 'PUBLISHED', ?, CAST(? AS jsonb),
                        ?, ?, 2, 'fixture-author', 'fixture-author',
                        'PASSED', ?, 'fixture-reviewer', now(), ?, ?, 'fixture-reviewer', now())
                """, topologyId, tenantId, "b".repeat(64), "{\"nodes\":[],\"edges\":[]}",
                PLANT_ID, LINE_ID, "b".repeat(64), snapshotId, "a".repeat(64));
        jdbc.update("""
                INSERT INTO bpi.bpi_rule_versions
                    (id, tenant_id, rule_code, version, topology_version_id, state,
                     checksum, definition, revision, plant_id, line_id, created_by, updated_by)
                VALUES (?, ?, 'RULE-SHADOW', '1.0.0', ?, 'PUBLISHED', ?, CAST(? AS jsonb),
                        4, ?, ?, 'fixture-author', 'fixture-author')
                """, ruleId, tenantId, topologyId, "c".repeat(64), "{\"logic\":\"fixture\"}",
                PLANT_ID, LINE_ID);
        jdbc.update("""
                INSERT INTO bpi.bpi_outbox_events
                    (id, tenant_id, plant_id, line_id, aggregate_type, aggregate_id,
                     event_type, topic, partition_key, payload, headers, status,
                     application_status, runtime_readiness_status,
                     lifecycle_action, lifecycle_sequence, lifecycle_active)
                VALUES (?, ?, ?, ?, 'RULE_VERSION', ?, 'BOUNDARY_RULE_PUBLISHED',
                        'bpi.boundary.rule-publication.v1', ?, ?, CAST(? AS jsonb),
                        'PUBLISHED', 'WAITING', 'WAITING', 'ACTIVATE', 1, true)
                """, outboxId, tenantId, PLANT_ID, LINE_ID, ruleId,
                tenantId + ":" + LINE_ID + ":RULE-SHADOW:1.0.0", new byte[] {1, 2, 3},
                "{\"lifecycle_action\":\"ACTIVATE\"}");
    }

    @AfterEach
    void cleanupMarker() {
        if (tenantId == null) return;
        jdbc.update("DELETE FROM bpi.bpi_shadow_run_batch_reviews WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_shadow_runs WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_telemetry_point_rejects WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_telemetry_points WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_telemetry_events WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_telemetry_source_state WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_data_quality_incident_actions WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_data_quality_incident_events WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_data_quality_incidents WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_audit_events WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_api_idempotency WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_outbox_events WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_batch_instances WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_rule_versions WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_topology_versions WHERE tenant_id = ?", tenantId);
        SourceSequenceEvidenceTestFixture.cleanup(jdbc, tenantId);
        jdbc.update("DELETE FROM bpi.bpi_point_catalog_entries WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_point_calibrations WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_point_catalog_snapshots WHERE tenant_id = ?", tenantId);
    }

    @Test
    void realShadowRunRequiresPinnedReadinessHumanAgreementQuantityAndIndependentApproval() throws Exception {
        byte[] createBody = objectMapper.writeValueAsBytes(Map.ofEntries(
                Map.entry("runCode", "SHADOW-" + tenantId.substring(tenantId.length() - 12)),
                Map.entry("name", "Seven day shadow acceptance"),
                Map.entry("plantId", PLANT_ID),
                Map.entry("lineId", LINE_ID),
                Map.entry("ruleVersionId", ruleId),
                Map.entry("minimumDurationDays", 7),
                Map.entry("minimumReviewedBatches", 10),
                Map.entry("boundaryToleranceSeconds", 60),
                Map.entry("minimumBoundaryAgreement", new BigDecimal("0.950000")),
                Map.entry("quantityTolerancePercent", new BigDecimal("2.000000")),
                Map.entry("reason", "Create controlled shadow acceptance run")));

        mockMvc.perform(post("/bpi/v1/shadow-runs")
                        .header("Authorization", "Bearer " + viewerToken)
                        .header("Idempotency-Key", "shadow-viewer-denied-" + tenantId)
                        .header("If-Match", "0")
                        .contentType(MediaType.APPLICATION_JSON).content(createBody))
                .andExpect(status().isForbidden());

        String createKey = "shadow-create-" + tenantId;
        MvcResult createdResult = mockMvc.perform(post("/bpi/v1/shadow-runs")
                        .header("Authorization", "Bearer " + authorToken)
                        .header("Idempotency-Key", createKey)
                        .header("If-Match", "0")
                        .header("X-Trace-Id", tenantId + "_CREATE")
                        .contentType(MediaType.APPLICATION_JSON).content(createBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("DRAFT"))
                .andExpect(jsonPath("$.data.revision").value(1))
                .andExpect(jsonPath("$.data.readiness.applicationApplied").value(false))
                .andExpect(jsonPath("$.data.readiness.runtimeReady").value(false))
                .andExpect(jsonPath("$.data.sourceCoverage.pinnedPointCount").value(1))
                .andExpect(jsonPath("$.data.sourceCoverage.activeRegisteredPointCount").value(1))
                .andExpect(jsonPath("$.data.sourceCoverage.physicalIdentityPointCount").value(1))
                .andExpect(jsonPath("$.data.sourceCoverage.freshSequenceQualifiedPointCount").value(1))
                .andExpect(jsonPath("$.data.sourceCoverage.approvedCalibrationPointCount").value(1))
                .andExpect(jsonPath("$.data.sourceCoverage.readyPointCount").value(1))
                .andExpect(jsonPath("$.data.sourceCoverage.fullyReady").value(true))
                .andExpect(jsonPath("$.data.telemetryCoverage.windowStarted").value(false))
                .andExpect(jsonPath("$.data.telemetryCoverage.pinnedPointCount").value(1))
                .andExpect(jsonPath("$.data.telemetryCoverage.observedPointCount").value(0))
                .andExpect(jsonPath("$.data.telemetryCoverage.fullyCovered").value(false))
                .andExpect(jsonPath("$.data.telemetryCoverage.blockers[*]")
                        .value(hasItem("TELEMETRY_WINDOW_NOT_STARTED")))
                .andExpect(jsonPath("$.data.trainingDataCoverage.policyVersion")
                        .value("bpi-training-data-coverage/batch-start-boundary-v1"))
                .andExpect(jsonPath("$.data.trainingDataCoverage.reviewedBatchCount").value(0))
                .andExpect(jsonPath("$.data.trainingDataCoverage.thresholdsMet").value(false))
                .andExpect(jsonPath("$.data.trainingDataCoverage.blockers[*]")
                        .value(hasItem("TRAINING_REVIEWED_BATCHES_BELOW_MINIMUM")))
                .andExpect(jsonPath("$.data.blockers[*]").value(hasItem("RULE_APPLICATION_NOT_APPLIED")))
                .andReturn();
        UUID runId = UUID.fromString(response(createdResult).path("data").path("id").asText());

        mockMvc.perform(post("/bpi/v1/shadow-runs")
                        .header("Authorization", "Bearer " + authorToken)
                        .header("Idempotency-Key", createKey)
                        .header("If-Match", "0")
                        .contentType(MediaType.APPLICATION_JSON).content(createBody))
                .andExpect(status().isOk())
                .andExpect(header().string("Idempotent-Replay", "true"))
                .andExpect(jsonPath("$.data.id").value(runId.toString()));

        byte[] startReason = reason("Start after runtime readiness confirmation");
        mockMvc.perform(post("/bpi/v1/shadow-runs/{id}/start", runId)
                        .header("Authorization", "Bearer " + authorToken)
                        .header("Idempotency-Key", "shadow-start-blocked-" + tenantId)
                        .header("If-Match", "1")
                        .contentType(MediaType.APPLICATION_JSON).content(startReason))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail", containsString("RULE_APPLICATION_NOT_APPLIED")));

        jdbc.update("""
                UPDATE bpi.bpi_outbox_events
                   SET application_status = 'APPLIED', runtime_readiness_status = 'READY'
                 WHERE tenant_id = ? AND id = ?
                """, tenantId, outboxId);
        mockMvc.perform(post("/bpi/v1/shadow-runs/{id}/start", runId)
                        .header("Authorization", "Bearer " + authorToken)
                        .header("Idempotency-Key", "shadow-start-" + tenantId)
                        .header("If-Match", "1")
                        .header("X-Trace-Id", tenantId + "_START")
                        .contentType(MediaType.APPLICATION_JSON).content(startReason))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("RUNNING"))
                .andExpect(jsonPath("$.data.revision").value(2))
                .andExpect(jsonPath("$.data.readiness.ready").value(true))
                .andExpect(jsonPath("$.data.telemetryCoverage.windowStarted").value(true))
                .andExpect(jsonPath("$.data.telemetryCoverage.observedPointCount").value(0))
                .andExpect(jsonPath("$.data.blockers[*]")
                        .value(hasItem("TELEMETRY_POINTS_NOT_OBSERVED")));

        List<BatchFixture> batches = insertClosedShadowBatches();
        long revision = 2;
        for (int index = 0; index < batches.size(); index++) {
            BatchFixture batch = batches.get(index);
            Instant manualStart = index == 0 ? batch.startTime().plusSeconds(61) : batch.startTime();
            BigDecimal referenceQuantity = index == 0
                    ? new BigDecimal("101.000000") : new BigDecimal("100.000000");
            byte[] reviewBody = objectMapper.writeValueAsBytes(Map.of(
                    "batchId", batch.id(),
                    "manualStartTime", manualStart,
                    "manualEndTime", batch.endTime(),
                    "referenceQuantity", referenceQuantity,
                    "quantityUnit", "t",
                    "reason", "Human boundary and weighbridge comparison " + index));
            String reviewKey = "shadow-review-" + index + "-" + tenantId;
            MvcResult reviewResult = mockMvc.perform(post(
                            "/bpi/v1/shadow-runs/{id}/batch-reviews", runId)
                            .header("Authorization", "Bearer " + reviewerToken)
                            .header("Idempotency-Key", reviewKey)
                            .header("If-Match", String.valueOf(revision))
                            .contentType(MediaType.APPLICATION_JSON).content(reviewBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.run.revision").value(revision + 1))
                    .andExpect(jsonPath("$.data.review.batchId").value(batch.id().toString()))
                    .andReturn();
            if (index == 0) {
                mockMvc.perform(post("/bpi/v1/shadow-runs/{id}/batch-reviews", runId)
                                .header("Authorization", "Bearer " + reviewerToken)
                                .header("Idempotency-Key", reviewKey)
                                .header("If-Match", String.valueOf(revision))
                                .contentType(MediaType.APPLICATION_JSON).content(reviewBody))
                        .andExpect(status().isOk())
                        .andExpect(header().string("Idempotent-Replay", "true"))
                        .andExpect(jsonPath("$.data.review.id")
                                .value(response(reviewResult).path("data").path("review").path("id").asText()));
            }
            revision++;
        }

        mockMvc.perform(post("/bpi/v1/shadow-runs/{id}/complete", runId)
                        .header("Authorization", "Bearer " + authorToken)
                        .header("Idempotency-Key", "shadow-complete-early-" + tenantId)
                        .header("If-Match", String.valueOf(revision))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reason("Attempt completion before seven days")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail", containsString("minimum duration")));

        jdbc.update("""
                UPDATE bpi.bpi_shadow_runs
                   SET created_at = now() - interval '8 days',
                       started_at = now() - interval '7 days 1 minute'
                 WHERE tenant_id = ? AND id = ?
                """, tenantId, runId);
        insertTelemetryObservation(
                "MAIN", Instant.now().minusSeconds(30), Instant.now(),
                100L, "IN_ORDER", "GOOD", "CAL-SHADOW-1");
        UUID incidentId = insertCriticalIncident();
        mockMvc.perform(post("/bpi/v1/shadow-runs/{id}/complete", runId)
                        .header("Authorization", "Bearer " + authorToken)
                        .header("Idempotency-Key", "shadow-complete-" + tenantId)
                        .header("If-Match", String.valueOf(revision))
                        .header("X-Trace-Id", tenantId + "_COMPLETE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reason("Close observation window for independent evaluation")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("EVALUATING"))
                .andExpect(jsonPath("$.data.metrics.reviewedBatchCount").value(10))
                .andExpect(jsonPath("$.data.metrics.acceptedBoundaryCount").value(19))
                .andExpect(jsonPath("$.data.metrics.totalBoundaryCount").value(20))
                .andExpect(jsonPath("$.data.metrics.boundaryAgreement").value(0.95))
                .andExpect(jsonPath("$.data.metrics.quantityGatePassed").value(true))
                .andExpect(jsonPath("$.data.metrics.unresolvedCriticalIncidentCount").value(1))
                .andExpect(jsonPath("$.data.telemetryCoverage.observedPointCount").value(1))
                .andExpect(jsonPath("$.data.telemetryCoverage.authoritativeSequencePointCount").value(1))
                .andExpect(jsonPath("$.data.telemetryCoverage.calibratedPointCount").value(1))
                .andExpect(jsonPath("$.data.telemetryCoverage.goodQualityPointCount").value(1))
                .andExpect(jsonPath("$.data.telemetryCoverage.acceptedEventCount").value(1))
                .andExpect(jsonPath("$.data.telemetryCoverage.acceptedObservationCount").value(1))
                .andExpect(jsonPath("$.data.telemetryCoverage.gapEventCount").value(0))
                .andExpect(jsonPath("$.data.telemetryCoverage.outOfOrderEventCount").value(0))
                .andExpect(jsonPath("$.data.telemetryCoverage.fullyCovered").value(true))
                .andExpect(jsonPath("$.data.telemetryCoverage.blockers.length()").value(0))
                .andExpect(jsonPath("$.data.trainingDataCoverage.reviewedBatchCount").value(10))
                .andExpect(jsonPath("$.data.trainingDataCoverage.acceptedStartLabelCount").value(9))
                .andExpect(jsonPath("$.data.trainingDataCoverage.rejectedStartLabelCount").value(1))
                .andExpect(jsonPath("$.data.trainingDataCoverage.thresholdsMet").value(false))
                .andExpect(jsonPath("$.data.trainingDataCoverage.blockers[*]")
                        .value(hasItem("TRAINING_PRODUCTION_DAYS_BELOW_MINIMUM")))
                .andExpect(jsonPath("$.data.blockers[*]")
                        .value(hasItem("UNRESOLVED_CRITICAL_DATA_QUALITY")));
        revision++;

        byte[] approvalReason = reason("Independent acceptance against the pinned evidence set");
        mockMvc.perform(post("/bpi/v1/shadow-runs/{id}/approve", runId)
                        .header("Authorization", "Bearer " + authorAdminToken)
                        .header("Idempotency-Key", "shadow-self-approve-" + tenantId)
                        .header("If-Match", String.valueOf(revision))
                        .contentType(MediaType.APPLICATION_JSON).content(approvalReason))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail", containsString("other than the creator")));
        mockMvc.perform(post("/bpi/v1/shadow-runs/{id}/approve", runId)
                        .header("Authorization", "Bearer " + independentAdminToken)
                        .header("Idempotency-Key", "shadow-quality-blocked-" + tenantId)
                        .header("If-Match", String.valueOf(revision))
                        .contentType(MediaType.APPLICATION_JSON).content(approvalReason))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail", containsString("UNRESOLVED_CRITICAL_DATA_QUALITY")));

        resolveCriticalIncident(incidentId);
        String approveKey = "shadow-approve-" + tenantId;
        mockMvc.perform(post("/bpi/v1/shadow-runs/{id}/approve", runId)
                        .header("Authorization", "Bearer " + independentAdminToken)
                        .header("Idempotency-Key", approveKey)
                        .header("If-Match", String.valueOf(revision))
                        .header("X-Trace-Id", tenantId + "_APPROVE")
                        .contentType(MediaType.APPLICATION_JSON).content(approvalReason))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("APPROVED"))
                .andExpect(jsonPath("$.data.revision").value(revision + 1))
                .andExpect(jsonPath("$.data.blockers.length()").value(0));
        mockMvc.perform(post("/bpi/v1/shadow-runs/{id}/approve", runId)
                        .header("Authorization", "Bearer " + independentAdminToken)
                        .header("Idempotency-Key", approveKey)
                        .header("If-Match", String.valueOf(revision))
                        .contentType(MediaType.APPLICATION_JSON).content(approvalReason))
                .andExpect(status().isOk())
                .andExpect(header().string("Idempotent-Replay", "true"));

        mockMvc.perform(get("/bpi/v1/shadow-runs/{id}/batch-reviews", runId)
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(10));

        assertThat(jdbc.queryForObject("""
                SELECT state || '|' || revision || '|' || created_by || '|' || decided_by
                  FROM bpi.bpi_shadow_runs WHERE tenant_id = ? AND id = ?
                """, String.class, tenantId, runId))
                .isEqualTo("APPROVED|14|shadow-author|shadow-admin");
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM bpi.bpi_shadow_run_batch_reviews
                 WHERE tenant_id = ? AND shadow_run_id = ? AND state = 'ACTIVE'
                """, Integer.class, tenantId, runId)).isEqualTo(10);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM bpi.bpi_audit_events
                 WHERE tenant_id = ? AND object_type = 'SHADOW_RUN'
                   AND action IN ('SHADOW_RUN_CREATED', 'SHADOW_RUN_STARTED',
                                  'SHADOW_RUN_BATCH_REVIEWED', 'SHADOW_RUN_COMPLETED',
                                  'SHADOW_RUN_APPROVED')
                """, Integer.class, tenantId)).isEqualTo(14);
    }

    @Test
    void telemetryCoverageRejectsPreWindowPersistenceAndSurfacesSequenceGaps() throws Exception {
        UUID runId = UUID.randomUUID();
        Instant createdAt = Instant.now().minusSeconds(7_200);
        Instant startedAt = Instant.now().minusSeconds(3_600);
        jdbc.update("""
                INSERT INTO bpi.bpi_shadow_runs
                    (id, tenant_id, run_code, name, plant_id, line_id, state, revision,
                     rule_version_id, topology_version_id, point_catalog_snapshot_id,
                     minimum_duration_days, minimum_reviewed_batches,
                     boundary_tolerance_seconds, minimum_boundary_agreement,
                     quantity_tolerance_percent, created_by, created_at, updated_by,
                     started_by, started_at)
                VALUES (?, ?, ?, 'Telemetry coverage projection', ?, ?, 'RUNNING', 1,
                        ?, ?, ?, 7, 10, 60, 0.950000, 2.000000,
                        'fixture-author', ?, 'fixture-author', 'fixture-author', ?)
                """, runId, tenantId, "TELEMETRY-" + tenantId.substring(tenantId.length() - 12),
                PLANT_ID, LINE_ID, ruleId, topologyId, snapshotId,
                Timestamp.from(createdAt), Timestamp.from(startedAt));

        insertTelemetryObservation(
                "OLD-REPLAY", startedAt.plusSeconds(1_200), startedAt.minusSeconds(60),
                200L, "IN_ORDER", "GOOD", "CAL-SHADOW-1");
        insertTelemetryObservation(
                "GAP", startedAt.plusSeconds(1_800), startedAt.plusSeconds(1_801),
                202L, "GAP", "GOOD", "CAL-SHADOW-1");

        mockMvc.perform(get("/bpi/v1/shadow-runs/{id}", runId)
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.telemetryCoverage.windowStarted").value(true))
                .andExpect(jsonPath("$.data.telemetryCoverage.pinnedPointCount").value(1))
                .andExpect(jsonPath("$.data.telemetryCoverage.observedPointCount").value(1))
                .andExpect(jsonPath("$.data.telemetryCoverage.authoritativeSequencePointCount").value(1))
                .andExpect(jsonPath("$.data.telemetryCoverage.calibratedPointCount").value(1))
                .andExpect(jsonPath("$.data.telemetryCoverage.goodQualityPointCount").value(1))
                .andExpect(jsonPath("$.data.telemetryCoverage.acceptedEventCount").value(1))
                .andExpect(jsonPath("$.data.telemetryCoverage.acceptedObservationCount").value(1))
                .andExpect(jsonPath("$.data.telemetryCoverage.gapEventCount").value(1))
                .andExpect(jsonPath("$.data.telemetryCoverage.fullyCovered").value(false))
                .andExpect(jsonPath("$.data.telemetryCoverage.blockers[*]")
                        .value(hasItem("TELEMETRY_SEQUENCE_GAP_DETECTED")))
                .andExpect(jsonPath("$.data.blockers[*]")
                        .value(hasItem("TELEMETRY_SEQUENCE_GAP_DETECTED")));
    }

    @Test
    void trainingDataCoverageUsesDistinctActiveBatchesAndUtcProductionDates() throws Exception {
        UUID runId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-06-29T00:00:00Z");
        Instant startedAt = Instant.parse("2026-06-30T00:00:00Z");
        jdbc.update("""
                INSERT INTO bpi.bpi_shadow_runs
                    (id, tenant_id, run_code, name, plant_id, line_id, state, revision,
                     rule_version_id, topology_version_id, point_catalog_snapshot_id,
                     minimum_duration_days, minimum_reviewed_batches,
                     boundary_tolerance_seconds, minimum_boundary_agreement,
                     quantity_tolerance_percent, created_by, created_at, updated_by,
                     started_by, started_at)
                VALUES (?, ?, ?, 'Field data coverage projection', ?, ?, 'RUNNING', 1,
                        ?, ?, ?, 7, 10, 60, 0.950000, 2.000000,
                        'fixture-author', ?, 'fixture-author', 'fixture-author', ?)
                """, runId, tenantId, "COVERAGE-" + tenantId.substring(tenantId.length() - 12),
                PLANT_ID, LINE_ID, ruleId, topologyId, snapshotId,
                Timestamp.from(createdAt), Timestamp.from(startedAt));

        Instant firstProductionDay = Instant.parse("2026-07-01T00:00:00Z");
        for (int index = 0; index < 200; index++) {
            UUID batchId = UUID.randomUUID();
            Instant start = firstProductionDay
                    .plusSeconds((index % 7) * 86_400L)
                    .plusSeconds((index / 7) * 60L);
            Instant end = start.plusSeconds(300);
            jdbc.update("""
                    INSERT INTO bpi.bpi_batch_instances
                        (id, tenant_id, plant_id, batch_no, line_id, stage_code, order_id,
                         material_code, state, revision, is_shadow, start_time, end_time,
                         quantity, quantity_unit, topology_version_id, rule_version_id, created_by)
                    VALUES (?, ?, ?, ?, ?, 'SUGAR-STAGE', ?, 'SUGAR', 'CLOSED_RAW', 2, true,
                            ?, ?, 100.000000, 't', ?, ?, 'fixture')
                    """, batchId, tenantId, PLANT_ID,
                    "COVERAGE-BATCH-" + String.format("%03d", index), LINE_ID,
                    "COVERAGE-ORDER-" + index, Timestamp.from(start), Timestamp.from(end),
                    topologyId, ruleId);
            boolean startAccepted = index < 100;
            jdbc.update("""
                    INSERT INTO bpi.bpi_shadow_run_batch_reviews
                        (id, tenant_id, shadow_run_id, batch_id, review_sequence, state,
                         automatic_start_time, automatic_end_time, manual_start_time, manual_end_time,
                         start_deviation_seconds, end_deviation_seconds,
                         start_boundary_accepted, end_boundary_accepted,
                         automatic_quantity, reference_quantity, quantity_unit,
                         quantity_deviation_percent, quantity_within_tolerance,
                         reviewed_by, review_reason, reviewed_at)
                    VALUES (?, ?, ?, ?, 1, 'ACTIVE', ?, ?, ?, ?,
                            ?, 0, ?, true, 100.000000, 100.000000, 't',
                            0.000000000, true, 'fixture-reviewer',
                            'Field data coverage projection fixture', ?)
                    """, UUID.randomUUID(), tenantId, runId, batchId,
                    Timestamp.from(start), Timestamp.from(end),
                    Timestamp.from(startAccepted ? start : start.plusSeconds(61)), Timestamp.from(end),
                    startAccepted ? 0 : 61, startAccepted, Timestamp.from(end.plusSeconds(60)));
        }

        mockMvc.perform(get("/bpi/v1/shadow-runs/{id}", runId)
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("RUNNING"))
                .andExpect(jsonPath("$.data.readyForApproval").value(false))
                .andExpect(jsonPath("$.data.sourceCoverage.pinnedPointCount").value(1))
                .andExpect(jsonPath("$.data.sourceCoverage.readyPointCount").value(1))
                .andExpect(jsonPath("$.data.sourceCoverage.fullyReady").value(true))
                .andExpect(jsonPath("$.data.trainingDataCoverage.requiredReviewedBatchCount").value(200))
                .andExpect(jsonPath("$.data.trainingDataCoverage.reviewedBatchCount").value(200))
                .andExpect(jsonPath("$.data.trainingDataCoverage.requiredProductionDayCount").value(7))
                .andExpect(jsonPath("$.data.trainingDataCoverage.distinctProductionDayCount").value(7))
                .andExpect(jsonPath("$.data.trainingDataCoverage.requiredAcceptedStartLabelCount").value(100))
                .andExpect(jsonPath("$.data.trainingDataCoverage.acceptedStartLabelCount").value(100))
                .andExpect(jsonPath("$.data.trainingDataCoverage.requiredRejectedStartLabelCount").value(10))
                .andExpect(jsonPath("$.data.trainingDataCoverage.rejectedStartLabelCount").value(100))
                .andExpect(jsonPath("$.data.trainingDataCoverage.thresholdsMet").value(true))
                .andExpect(jsonPath("$.data.trainingDataCoverage.blockers.length()").value(0));
    }

    private List<BatchFixture> insertClosedShadowBatches() {
        List<BatchFixture> result = new ArrayList<>();
        Instant base = Instant.now().plusSeconds(5);
        for (int index = 0; index < 10; index++) {
            UUID id = UUID.randomUUID();
            Instant start = base.plusSeconds(index * 600L);
            Instant end = start.plusSeconds(300);
            jdbc.update("""
                    INSERT INTO bpi.bpi_batch_instances
                        (id, tenant_id, plant_id, batch_no, line_id, stage_code, order_id,
                         material_code, state, revision, is_shadow, start_time, end_time,
                         quantity, quantity_unit, topology_version_id, rule_version_id, created_by)
                    VALUES (?, ?, ?, ?, ?, 'SUGAR-STAGE', ?, 'SUGAR', 'CLOSED_RAW', 2, true,
                            ?, ?, 100.000000, 't', ?, ?, 'fixture')
                    """, id, tenantId, PLANT_ID, "BATCH-" + index + "-" + tenantId.substring(tenantId.length() - 8),
                    LINE_ID, "ORDER-" + index, Timestamp.from(start), Timestamp.from(end), topologyId, ruleId);
            result.add(new BatchFixture(id, start, end));
        }
        return result;
    }

    private UUID insertCriticalIncident() {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO bpi.bpi_data_quality_incidents
                    (id, tenant_id, plant_id, line_id, source, device_id, property_id,
                     issue_code, severity, state, revision, event_count, first_seen,
                     last_seen, last_event_id, last_detail)
                VALUES (?, ?, ?, ?, 'FLINK', 'DEVICE-SHADOW', 'flow.instant',
                        'SOURCE_SEQUENCE_GAP', 'CRITICAL', 'OPEN', 1, 1,
                        now() - interval '1 hour', now(), ?, 'Sequence gap during shadow run')
                """, id, tenantId, PLANT_ID, LINE_ID, tenantId + "_DQ");
        return id;
    }

    private void insertTelemetryObservation(
            String suffix,
            Instant eventTime,
            Instant createdAt,
            long sequence,
            String sequenceDisposition,
            String qualityCode,
            String calibrationVersion) {
        UUID eventRowId = UUID.randomUUID();
        String eventId = tenantId + "_TELEMETRY_" + suffix;
        jdbc.update("""
                INSERT INTO bpi.bpi_telemetry_events
                    (id, tenant_id, plant_id, line_id, gateway_id, product_id, device_id,
                     event_id, message_id, event_time, ingest_time, source_epoch, sequence,
                     sequence_origin, sequence_disposition, payload_checksum, headers,
                     point_count, accepted_point_count, rejected_point_count, status, created_at)
                VALUES (?, ?, ?, ?, 'GATEWAY-SHADOW', 'PRODUCT-SUGAR', 'DEVICE-SHADOW',
                        ?, ?, ?, ?, 7, ?, 'DEVICE', ?, ?, '{}'::jsonb,
                        1, 1, 0, 'ACCEPTED', ?)
                """, eventRowId, tenantId, PLANT_ID, LINE_ID, eventId, eventId + "_MESSAGE",
                Timestamp.from(eventTime), Timestamp.from(eventTime), BigDecimal.valueOf(sequence),
                sequenceDisposition, "e".repeat(64), Timestamp.from(createdAt));
        jdbc.update("""
                INSERT INTO bpi.bpi_telemetry_points
                    (id, tenant_id, telemetry_event_id, event_id, property_id, value_type,
                     numeric_value, unit, quality_code, sample_time, calibration_version, created_at)
                VALUES (?, ?, ?, ?, 'flow.instant', 'DOUBLE', 18.600000,
                        't/h', ?, ?, ?, ?)
                """, UUID.randomUUID(), tenantId, eventRowId, eventId, qualityCode,
                Timestamp.from(eventTime), calibrationVersion, Timestamp.from(createdAt));
    }

    private void resolveCriticalIncident(UUID incidentId) {
        jdbc.update("""
                UPDATE bpi.bpi_data_quality_incidents
                   SET state = 'RESOLVED', revision = revision + 1,
                       assignee = 'shadow-admin',
                       acknowledged_by = 'shadow-admin', acknowledged_at = now(),
                       acknowledgment_reason = 'Investigated sequence continuity',
                       resolved_by = 'shadow-admin', resolved_at = now(),
                       resolution_reason = 'Source sequence restored', updated_at = now()
                 WHERE tenant_id = ? AND id = ?
                """, tenantId, incidentId);
    }

    private byte[] reason(String value) throws Exception {
        return objectMapper.writeValueAsBytes(Map.of("reason", value));
    }

    private JsonNode response(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsByteArray());
    }

    private String token(String subject, List<String> roles) throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer("ft-mes-adapter")
                .audience("bpi-service")
                .subject(subject)
                .claim("tenant_id", tenantId)
                .claim("roles", roles)
                .claim("plant_ids", List.of(PLANT_ID))
                .claim("line_ids", List.of(LINE_ID))
                .issueTime(new Date())
                .expirationTime(Date.from(Instant.now().plusSeconds(600)))
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        jwt.sign(new MACSigner(SECRET));
        return jwt.serialize();
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    private record BatchFixture(UUID id, Instant startTime, Instant endTime) {
    }
}
