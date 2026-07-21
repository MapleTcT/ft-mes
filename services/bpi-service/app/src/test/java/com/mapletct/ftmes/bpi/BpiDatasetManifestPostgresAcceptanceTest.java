package com.mapletct.ftmes.bpi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mapletct.ftmes.bpi.application.DatasetManifestProcessor;
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
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "BPI_TEST_DATABASE_URL", matches = ".+")
class BpiDatasetManifestPostgresAcceptanceTest {
    private static final String SECRET = "bpi-test-secret-0123456789abcdef";
    private static final String PLANT_ID = "PLANT-01";
    private static final String LINE_ID = "LINE-S07-01";

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> System.getenv("BPI_TEST_DATABASE_URL"));
        registry.add("spring.datasource.username", () -> env("BPI_TEST_DATABASE_USER", "postgres"));
        registry.add("spring.datasource.password", () -> env("BPI_TEST_DATABASE_PASSWORD", ""));
        registry.add("bpi.security.internal-jwt-secret", () -> SECRET);
        registry.add("bpi.dataset-manifest.enabled", () -> false);
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JdbcTemplate jdbc;
    @Autowired DatasetManifestProcessor processor;

    private String tenantId;
    private UUID pointSnapshotId;
    private UUID topologyId;
    private UUID ruleId;
    private UUID shadowRunId;
    private Instant freezeAt;
    private String engineerToken;
    private String viewerToken;
    private String wrongScopeToken;
    private final List<UUID> batchIds = new ArrayList<>();
    private final List<UUID> reviewIds = new ArrayList<>();

    @BeforeEach
    void setUpApprovedShadowFacts() throws Exception {
        tenantId = "ADP_E2E_BPI_DATASET_" + UUID.randomUUID().toString().replace("-", "");
        pointSnapshotId = UUID.randomUUID();
        topologyId = UUID.randomUUID();
        ruleId = UUID.randomUUID();
        shadowRunId = UUID.randomUUID();
        freezeAt = Instant.now().minusSeconds(60).truncatedTo(ChronoUnit.SECONDS);
        engineerToken = token("dataset-engineer", List.of("BPI_ENGINEER"), List.of(LINE_ID));
        viewerToken = token("dataset-viewer", List.of("BPI_VIEWER"), List.of(LINE_ID));
        wrongScopeToken = token("dataset-other-line", List.of("BPI_ENGINEER"), List.of("LINE-OTHER"));

        jdbc.update("""
                INSERT INTO bpi.bpi_point_catalog_snapshots
                    (id, tenant_id, source, source_instance, source_revision, plant_id, line_id,
                     checksum, observed_at, point_count, source_claim_ready_point_count, imported_by)
                VALUES (?, ?, 'JETLINKS', 'dataset-acceptance', 'revision-1', ?, ?, ?, ?, 1, 1, 'fixture')
                """, pointSnapshotId, tenantId, PLANT_ID, LINE_ID, "a".repeat(64),
                Timestamp.from(freezeAt.minus(9, ChronoUnit.DAYS)));
        jdbc.update("""
                INSERT INTO bpi.bpi_topology_versions
                    (id, tenant_id, topology_code, version, state, checksum, definition,
                     plant_id, line_id, revision, created_by, updated_by,
                     validation_status, validated_checksum, validated_by, validated_at,
                     validated_point_catalog_snapshot_id, validated_point_catalog_checksum,
                     published_by, published_at)
                VALUES (?, ?, 'TOPOLOGY-DATASET', '1.0.0', 'PUBLISHED', ?, CAST(? AS jsonb),
                        ?, ?, 2, 'fixture-author', 'fixture-author', 'PASSED', ?,
                        'fixture-reviewer', ?, ?, ?, 'fixture-reviewer', ?)
                """, topologyId, tenantId, "b".repeat(64), "{\"nodes\":[],\"edges\":[]}",
                PLANT_ID, LINE_ID, "b".repeat(64), Timestamp.from(freezeAt.minus(9, ChronoUnit.DAYS)),
                pointSnapshotId, "a".repeat(64), Timestamp.from(freezeAt.minus(9, ChronoUnit.DAYS)));
        jdbc.update("""
                INSERT INTO bpi.bpi_rule_versions
                    (id, tenant_id, rule_code, version, topology_version_id, state,
                     checksum, definition, revision, plant_id, line_id, created_by, updated_by)
                VALUES (?, ?, 'RULE-DATASET', '1.0.0', ?, 'PUBLISHED', ?, CAST(? AS jsonb),
                        4, ?, ?, 'fixture-author', 'fixture-author')
                """, ruleId, tenantId, topologyId, "c".repeat(64), "{\"logic\":\"fixture\"}",
                PLANT_ID, LINE_ID);

        Instant createdAt = freezeAt.minus(9, ChronoUnit.DAYS);
        Instant startedAt = freezeAt.minus(8, ChronoUnit.DAYS);
        Instant completedAt = freezeAt.minus(2, ChronoUnit.HOURS);
        Instant decidedAt = freezeAt.minus(1, ChronoUnit.HOURS);
        jdbc.update("""
                INSERT INTO bpi.bpi_shadow_runs
                    (id, tenant_id, run_code, name, plant_id, line_id, state, revision,
                     rule_version_id, topology_version_id, point_catalog_snapshot_id,
                     minimum_duration_days, minimum_reviewed_batches, boundary_tolerance_seconds,
                     minimum_boundary_agreement, quantity_tolerance_percent,
                     created_by, created_at, updated_by, updated_at,
                     started_by, started_at, completed_by, completed_at,
                     decided_by, decided_at, decision_reason)
                VALUES (?, ?, ?, 'Approved dataset source', ?, ?, 'APPROVED', 14,
                        ?, ?, ?, 7, 10, 60, 0.950000, 2.000000,
                        'fixture-author', ?, 'fixture-admin', ?, 'fixture-author', ?,
                        'fixture-author', ?, 'fixture-admin', ?, 'Approved for dataset acceptance')
                """, shadowRunId, tenantId, "SHADOW-DATASET-" + tenantId.substring(tenantId.length() - 8),
                PLANT_ID, LINE_ID, ruleId, topologyId, pointSnapshotId,
                Timestamp.from(createdAt), Timestamp.from(decidedAt), Timestamp.from(startedAt),
                Timestamp.from(completedAt), Timestamp.from(decidedAt));

        insertReviewedBatch("DATASET-HIGH", freezeAt.minus(6, ChronoUnit.HOURS),
                freezeAt.minus(5, ChronoUnit.HOURS), true, true, true);
        insertReviewedBatch("DATASET-LOW", freezeAt.minus(4, ChronoUnit.HOURS),
                freezeAt.minus(3, ChronoUnit.HOURS), false, true, true);
        insertReviewedBatch("DATASET-DELAYED", freezeAt.minus(50, ChronoUnit.HOURS),
                freezeAt.minus(1, ChronoUnit.HOURS), true, true, true);
        insertCrossPlantReviewedBatch();
    }

    @AfterEach
    void cleanupMarker() {
        if (tenantId == null) return;
        jdbc.update("DELETE FROM bpi.bpi_dataset_snapshot_samples WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_dataset_snapshots WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_dataset_definitions WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_audit_events WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_api_idempotency WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_shadow_run_batch_reviews WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_shadow_runs WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_batch_instances WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_rule_versions WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_topology_versions WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_point_catalog_snapshots WHERE tenant_id = ?", tenantId);
    }

    @Test
    void apiWorkerAndPostgresProveLeakageSafeReproducibleManifestOnlySnapshot() throws Exception {
        byte[] definitionBody = objectMapper.writeValueAsBytes(Map.ofEntries(
                Map.entry("datasetCode", "BOUNDARY-LABELS-" + tenantId.substring(tenantId.length() - 8)),
                Map.entry("version", "1.0.0"),
                Map.entry("name", "Boundary and quantity supervised labels"),
                Map.entry("plantId", PLANT_ID),
                Map.entry("lineIds", List.of(LINE_ID)),
                Map.entry("predictionTimePolicy", "AUTOMATIC_BATCH_START"),
                Map.entry("featureCutoffPolicy", "AT_OR_BEFORE_PREDICTION_TIME"),
                Map.entry("featureRefs", List.of("batch.order_id", "batch.material_code", "rule.version_id")),
                Map.entry("labelRefs", List.of("review.manual_start_time", "review.reference_quantity")),
                Map.entry("maxLabelDelayHours", 24),
                Map.entry("minimumConfidence", BigDecimal.ONE),
                Map.entry("splitPolicy", "PRODUCTION_TIME"),
                Map.entry("reason", "Create point-in-time dataset definition")));

        mockMvc.perform(post("/bpi/v1/datasets")
                        .header("Authorization", "Bearer " + viewerToken)
                        .header("Idempotency-Key", "dataset-viewer-denied-" + tenantId)
                        .header("If-Match", "0")
                        .contentType(MediaType.APPLICATION_JSON).content(definitionBody))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/bpi/v1/datasets")
                        .header("Authorization", "Bearer " + wrongScopeToken)
                        .header("Idempotency-Key", "dataset-scope-denied-" + tenantId)
                        .header("If-Match", "0")
                        .contentType(MediaType.APPLICATION_JSON).content(definitionBody))
                .andExpect(status().isForbidden());

        String definitionKey = "dataset-definition-" + tenantId;
        MvcResult definitionResult = mockMvc.perform(post("/bpi/v1/datasets")
                        .header("Authorization", "Bearer " + engineerToken)
                        .header("Idempotency-Key", definitionKey)
                        .header("If-Match", "0")
                        .header("X-Trace-Id", tenantId + "_DEFINITION")
                        .contentType(MediaType.APPLICATION_JSON).content(definitionBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("ACTIVE"))
                .andExpect(jsonPath("$.data.revision").value(1))
                .andExpect(jsonPath("$.data.checksum").isString())
                .andReturn();
        UUID datasetId = UUID.fromString(response(definitionResult).path("data").path("id").asText());

        mockMvc.perform(post("/bpi/v1/datasets")
                        .header("Authorization", "Bearer " + engineerToken)
                        .header("Idempotency-Key", definitionKey)
                        .header("If-Match", "0")
                        .contentType(MediaType.APPLICATION_JSON).content(definitionBody))
                .andExpect(status().isOk())
                .andExpect(header().string("Idempotent-Replay", "true"))
                .andExpect(jsonPath("$.data.id").value(datasetId.toString()));

        byte[] futureBody = snapshotBody(Instant.now().plusSeconds(300));
        mockMvc.perform(post("/bpi/v1/datasets/{id}/snapshots", datasetId)
                        .header("Authorization", "Bearer " + engineerToken)
                        .header("Idempotency-Key", "dataset-future-" + tenantId)
                        .header("If-Match", "1")
                        .contentType(MediaType.APPLICATION_JSON).content(futureBody))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail", containsString("future")));

        byte[] snapshotBody = snapshotBody(freezeAt);
        String snapshotKey = "dataset-snapshot-first-" + tenantId;
        MvcResult queuedResult = mockMvc.perform(post("/bpi/v1/datasets/{id}/snapshots", datasetId)
                        .header("Authorization", "Bearer " + engineerToken)
                        .header("Idempotency-Key", snapshotKey)
                        .header("If-Match", "1")
                        .header("X-Trace-Id", tenantId + "_SNAPSHOT")
                        .contentType(MediaType.APPLICATION_JSON).content(snapshotBody))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.state").value("QUEUED"))
                .andExpect(jsonPath("$.data.materializationState").value("NOT_STARTED"))
                .andReturn();
        UUID firstSnapshotId = UUID.fromString(response(queuedResult).path("data").path("id").asText());

        mockMvc.perform(post("/bpi/v1/datasets/{id}/snapshots", datasetId)
                        .header("Authorization", "Bearer " + engineerToken)
                        .header("Idempotency-Key", snapshotKey)
                        .header("If-Match", "1")
                        .contentType(MediaType.APPLICATION_JSON).content(snapshotBody))
                .andExpect(status().isAccepted())
                .andExpect(header().string("Idempotent-Replay", "true"))
                .andExpect(jsonPath("$.data.id").value(firstSnapshotId.toString()));

        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM bpi.bpi_dataset_snapshots
                 WHERE tenant_id = ? AND id = ? AND state = 'QUEUED'
                """, Long.class, tenantId, firstSnapshotId)).isEqualTo(1L);
        assertThat(processor.processOne()).isTrue();

        MvcResult readyResult = mockMvc.perform(get("/bpi/v1/dataset-snapshots/{id}", firstSnapshotId)
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("MANIFEST_READY"))
                .andExpect(jsonPath("$.data.includedCount").value(1))
                .andExpect(jsonPath("$.data.excludedCount").value(2))
                .andExpect(jsonPath("$.data.materializationState").value("NOT_STARTED"))
                .andExpect(jsonPath("$.data.artifactUri").doesNotExist())
                .andExpect(jsonPath("$.data.manifest.phaseBoundary.deliveryState").value("MANIFEST_ONLY"))
                .andExpect(jsonPath("$.data.manifest.phaseBoundary.icebergReady").value(false))
                .andExpect(jsonPath("$.data.manifest.phaseBoundary.mlflowRegistered").value(false))
                .andExpect(jsonPath("$.data.manifest.phaseBoundary.modelTrained").value(false))
                .andReturn();
        String firstChecksum = response(readyResult).path("data").path("manifestChecksum").asText();
        assertThat(firstChecksum).hasSize(64);

        Map<String, Object> persisted = jdbc.queryForMap("""
                SELECT count(*)::integer AS total,
                       count(*) FILTER (WHERE included)::integer AS included,
                       count(*) FILTER (WHERE NOT included)::integer AS excluded,
                       count(*) FILTER (WHERE feature_cutoff = prediction_time)::integer AS cutoff_safe,
                       count(*) FILTER (
                           WHERE jsonb_exists(feature_payload, 'review.manual_start_time')
                              OR jsonb_exists(feature_payload, 'review.reference_quantity'))::integer AS leaked
                  FROM bpi.bpi_dataset_snapshot_samples
                 WHERE tenant_id = ? AND snapshot_id = ?
                """, tenantId, firstSnapshotId);
        assertThat(persisted).containsEntry("total", 3)
                .containsEntry("included", 1)
                .containsEntry("excluded", 2)
                .containsEntry("cutoff_safe", 3)
                .containsEntry("leaked", 0);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM bpi.bpi_dataset_snapshot_samples
                 WHERE tenant_id = ? AND snapshot_id = ? AND batch_no = 'DATASET-CROSS-PLANT'
                """, Long.class, tenantId, firstSnapshotId)).isZero();
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM bpi.bpi_dataset_snapshot_samples
                 WHERE tenant_id = ? AND snapshot_id = ?
                   AND jsonb_exists(exclusion_reasons, 'CONFIDENCE_BELOW_THRESHOLD')
                """, Long.class, tenantId, firstSnapshotId)).isEqualTo(1L);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM bpi.bpi_dataset_snapshot_samples
                 WHERE tenant_id = ? AND snapshot_id = ?
                   AND jsonb_exists(exclusion_reasons, 'LABEL_DELAY_EXCEEDED')
                """, Long.class, tenantId, firstSnapshotId)).isEqualTo(1L);

        MvcResult secondQueued = mockMvc.perform(post("/bpi/v1/datasets/{id}/snapshots", datasetId)
                        .header("Authorization", "Bearer " + engineerToken)
                        .header("Idempotency-Key", "dataset-snapshot-second-" + tenantId)
                        .header("If-Match", "1")
                        .contentType(MediaType.APPLICATION_JSON).content(snapshotBody))
                .andExpect(status().isAccepted()).andReturn();
        UUID secondSnapshotId = UUID.fromString(response(secondQueued).path("data").path("id").asText());
        assertThat(processor.processOne()).isTrue();
        String secondChecksum = jdbc.queryForObject("""
                SELECT manifest_checksum FROM bpi.bpi_dataset_snapshots
                 WHERE tenant_id = ? AND id = ?
                """, String.class, tenantId, secondSnapshotId);
        assertThat(secondChecksum).isEqualTo(firstChecksum);

        assertThatThrownBy(() -> jdbc.update("""
                UPDATE bpi.bpi_dataset_snapshots SET request_reason = 'mutated'
                 WHERE tenant_id = ? AND id = ?
                """, tenantId, firstSnapshotId))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("immutable");
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM bpi.bpi_audit_events
                 WHERE tenant_id = ? AND object_id IN (?, ?)
                   AND action = 'DATASET_MANIFEST_READY'
                """, Long.class, tenantId, firstSnapshotId, secondSnapshotId)).isEqualTo(2L);
    }

    private void insertReviewedBatch(
            String batchNo,
            Instant start,
            Instant reviewedAt,
            boolean startAccepted,
            boolean endAccepted,
            boolean quantityAccepted) {
        UUID batchId = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();
        Instant end = start.plus(30, ChronoUnit.MINUTES);
        batchIds.add(batchId);
        reviewIds.add(reviewId);
        jdbc.update("""
                INSERT INTO bpi.bpi_batch_instances
                    (id, tenant_id, plant_id, batch_no, line_id, stage_code, order_id,
                     material_code, state, revision, is_shadow, start_time, end_time,
                     quantity, quantity_unit, quality_gate, wms_status,
                     topology_version_id, rule_version_id, created_by)
                VALUES (?, ?, ?, ?, ?, 'EVAPORATION', 'ORDER-DATASET', 'SUGAR-JUICE',
                        'CLOSED_RAW', 2, true, ?, ?, 100.000000, 't', 'NOT_APPLICABLE',
                        'NOT_REQUESTED', ?, ?, 'fixture')
                """, batchId, tenantId, PLANT_ID, batchNo, LINE_ID,
                Timestamp.from(start), Timestamp.from(end), topologyId, ruleId);
        BigDecimal deviation = quantityAccepted ? new BigDecimal("0.500000000")
                : new BigDecimal("5.000000000");
        jdbc.update("""
                INSERT INTO bpi.bpi_shadow_run_batch_reviews
                    (id, tenant_id, shadow_run_id, batch_id, review_sequence, state,
                     automatic_start_time, automatic_end_time, manual_start_time, manual_end_time,
                     start_deviation_seconds, end_deviation_seconds,
                     start_boundary_accepted, end_boundary_accepted,
                     automatic_quantity, reference_quantity, quantity_unit,
                     quantity_deviation_percent, quantity_within_tolerance,
                     reviewed_by, review_reason, reviewed_at)
                VALUES (?, ?, ?, ?, 1, 'ACTIVE', ?, ?, ?, ?, ?, 0, ?, ?,
                        100.000000, 99.500000, 't', ?, ?, 'fixture-reviewer',
                        'Dataset acceptance human label', ?)
                """, reviewId, tenantId, shadowRunId, batchId,
                Timestamp.from(start), Timestamp.from(end),
                Timestamp.from(start.plusSeconds(startAccepted ? 0 : 61)), Timestamp.from(end),
                startAccepted ? 0 : 61, startAccepted, endAccepted, deviation, quantityAccepted,
                Timestamp.from(reviewedAt));
    }

    private void insertCrossPlantReviewedBatch() {
        UUID crossRunId = UUID.randomUUID();
        UUID batchId = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();
        Instant start = freezeAt.minus(3, ChronoUnit.HOURS);
        Instant end = start.plus(30, ChronoUnit.MINUTES);
        Instant decidedAt = freezeAt.minus(1, ChronoUnit.HOURS);
        jdbc.update("""
                INSERT INTO bpi.bpi_shadow_runs
                    (id, tenant_id, run_code, name, plant_id, line_id, state, revision,
                     rule_version_id, topology_version_id, point_catalog_snapshot_id,
                     minimum_duration_days, minimum_reviewed_batches, boundary_tolerance_seconds,
                     minimum_boundary_agreement, quantity_tolerance_percent,
                     created_by, created_at, updated_by, updated_at,
                     started_by, started_at, completed_by, completed_at,
                     decided_by, decided_at, decision_reason)
                VALUES (?, ?, ?, 'Cross-plant collision source', 'PLANT-02', ?, 'APPROVED', 14,
                        ?, ?, ?, 7, 10, 60, 0.950000, 2.000000,
                        'fixture-author', ?, 'fixture-admin', ?, 'fixture-author', ?,
                        'fixture-author', ?, 'fixture-admin', ?, 'Cross-plant negative fixture')
                """, crossRunId, tenantId,
                "SHADOW-CROSS-" + tenantId.substring(tenantId.length() - 8), LINE_ID,
                ruleId, topologyId, pointSnapshotId,
                Timestamp.from(freezeAt.minus(9, ChronoUnit.DAYS)), Timestamp.from(decidedAt),
                Timestamp.from(freezeAt.minus(8, ChronoUnit.DAYS)),
                Timestamp.from(freezeAt.minus(2, ChronoUnit.HOURS)), Timestamp.from(decidedAt));
        jdbc.update("""
                INSERT INTO bpi.bpi_batch_instances
                    (id, tenant_id, plant_id, batch_no, line_id, stage_code, order_id,
                     material_code, state, revision, is_shadow, start_time, end_time,
                     quantity, quantity_unit, quality_gate, wms_status,
                     topology_version_id, rule_version_id, created_by)
                VALUES (?, ?, 'PLANT-02', 'DATASET-CROSS-PLANT', ?, 'EVAPORATION',
                        'ORDER-CROSS-PLANT', 'SUGAR-JUICE', 'CLOSED_RAW', 2, true, ?, ?,
                        100.000000, 't', 'NOT_APPLICABLE', 'NOT_REQUESTED', ?, ?, 'fixture')
                """, batchId, tenantId, LINE_ID, Timestamp.from(start), Timestamp.from(end),
                topologyId, ruleId);
        jdbc.update("""
                INSERT INTO bpi.bpi_shadow_run_batch_reviews
                    (id, tenant_id, shadow_run_id, batch_id, review_sequence, state,
                     automatic_start_time, automatic_end_time, manual_start_time, manual_end_time,
                     start_deviation_seconds, end_deviation_seconds,
                     start_boundary_accepted, end_boundary_accepted,
                     automatic_quantity, reference_quantity, quantity_unit,
                     quantity_deviation_percent, quantity_within_tolerance,
                     reviewed_by, review_reason, reviewed_at)
                VALUES (?, ?, ?, ?, 1, 'ACTIVE', ?, ?, ?, ?, 0, 0, true, true,
                        100.000000, 100.000000, 't', 0.000000000, true,
                        'fixture-reviewer', 'Must remain outside PLANT-01 manifest', ?)
                """, reviewId, tenantId, crossRunId, batchId, Timestamp.from(start),
                Timestamp.from(end), Timestamp.from(start), Timestamp.from(end),
                Timestamp.from(freezeAt.minus(30, ChronoUnit.MINUTES)));
    }

    private byte[] snapshotBody(Instant freeze) throws Exception {
        return objectMapper.writeValueAsBytes(Map.of(
                "freezeAt", freeze,
                "lineIds", List.of(LINE_ID),
                "predictionTimePolicy", "AUTOMATIC_BATCH_START",
                "ruleVersionIds", List.of(ruleId),
                "excludeLowConfidence", true,
                "reason", "Freeze reviewed shadow labels for deterministic manifest"));
    }

    private JsonNode response(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsByteArray());
    }

    private String token(String subject, List<String> roles, List<String> lineIds) throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer("ft-mes-adapter")
                .audience("bpi-service")
                .subject(subject)
                .claim("tenant_id", tenantId)
                .claim("roles", roles)
                .claim("plant_ids", List.of(PLANT_ID))
                .claim("line_ids", lineIds)
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
}
