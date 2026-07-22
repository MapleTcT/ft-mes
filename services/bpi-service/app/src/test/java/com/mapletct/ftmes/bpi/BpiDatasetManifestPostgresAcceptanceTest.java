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
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
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

    private JdbcTemplate cleanupJdbc;
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
        cleanupJdbc = new JdbcTemplate(new DriverManagerDataSource(
                env("BPI_TEST_CLEANUP_DATABASE_URL", System.getenv("BPI_TEST_DATABASE_URL")),
                env("BPI_TEST_CLEANUP_DATABASE_USER", env("BPI_TEST_DATABASE_USER", "postgres")),
                env("BPI_TEST_CLEANUP_DATABASE_PASSWORD", env("BPI_TEST_DATABASE_PASSWORD", ""))));
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
        if (Boolean.parseBoolean(env("BPI_TEST_KEEP_MARKER", "false"))) return;
        cleanupJdbc.update("DELETE FROM bpi.bpi_dataset_mlflow_registrations WHERE tenant_id = ?", tenantId);
        cleanupJdbc.update("DELETE FROM bpi.bpi_dataset_retention_archives WHERE tenant_id = ?", tenantId);
        cleanupJdbc.update("DELETE FROM bpi.bpi_dataset_catalog_publications WHERE tenant_id = ?", tenantId);
        cleanupJdbc.update("DELETE FROM bpi.bpi_dataset_materializations WHERE tenant_id = ?", tenantId);
        cleanupJdbc.update("DELETE FROM bpi.bpi_dataset_snapshot_samples WHERE tenant_id = ?", tenantId);
        cleanupJdbc.update("DELETE FROM bpi.bpi_dataset_snapshots WHERE tenant_id = ?", tenantId);
        cleanupJdbc.update("DELETE FROM bpi.bpi_dataset_definitions WHERE tenant_id = ?", tenantId);
        cleanupJdbc.update("DELETE FROM bpi.bpi_audit_events WHERE tenant_id = ?", tenantId);
        cleanupJdbc.update("DELETE FROM bpi.bpi_api_idempotency WHERE tenant_id = ?", tenantId);
        cleanupJdbc.update("DELETE FROM bpi.bpi_shadow_run_batch_reviews WHERE tenant_id = ?", tenantId);
        cleanupJdbc.update("DELETE FROM bpi.bpi_shadow_runs WHERE tenant_id = ?", tenantId);
        cleanupJdbc.update("DELETE FROM bpi.bpi_batch_instances WHERE tenant_id = ?", tenantId);
        cleanupJdbc.update("DELETE FROM bpi.bpi_rule_versions WHERE tenant_id = ?", tenantId);
        cleanupJdbc.update("DELETE FROM bpi.bpi_topology_versions WHERE tenant_id = ?", tenantId);
        cleanupJdbc.update("DELETE FROM bpi.bpi_point_catalog_snapshots WHERE tenant_id = ?", tenantId);
    }

    @Test
    void apiWorkerAndPostgresProveLeakageSafeReproducibleManifestOnlySnapshot() throws Exception {
        boolean externalMaterializer = Boolean.parseBoolean(
                env("BPI_TEST_EXTERNAL_MATERIALIZER", "false"));
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

        long snapshotRevision = jdbc.queryForObject("""
                SELECT revision FROM bpi.bpi_dataset_snapshots
                 WHERE tenant_id = ? AND id = ?
                """, Long.class, tenantId, firstSnapshotId);
        byte[] materializationBody = objectMapper.writeValueAsBytes(Map.of(
                "artifactFormat", "PARQUET",
                "reason", "Materialize the immutable acceptance manifest"));

        mockMvc.perform(post("/bpi/v1/dataset-snapshots/{id}/materializations", firstSnapshotId)
                        .header("Authorization", "Bearer " + viewerToken)
                        .header("Idempotency-Key", "materialization-viewer-denied-" + tenantId)
                        .header("If-Match", snapshotRevision)
                        .contentType(MediaType.APPLICATION_JSON).content(materializationBody))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/bpi/v1/dataset-snapshots/{id}/materializations", firstSnapshotId)
                        .header("Authorization", "Bearer " + wrongScopeToken)
                        .header("Idempotency-Key", "materialization-scope-denied-" + tenantId)
                        .header("If-Match", snapshotRevision)
                        .contentType(MediaType.APPLICATION_JSON).content(materializationBody))
                .andExpect(status().isNotFound());

        String materializationKey = "materialization-request-" + tenantId;
        MvcResult materializationResult = mockMvc.perform(
                        post("/bpi/v1/dataset-snapshots/{id}/materializations", firstSnapshotId)
                                .header("Authorization", "Bearer " + engineerToken)
                                .header("Idempotency-Key", materializationKey)
                                .header("If-Match", snapshotRevision)
                                .header("X-Trace-Id", tenantId + "_MATERIALIZATION")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(materializationBody))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.state").value("QUEUED"))
                .andExpect(jsonPath("$.data.revision").value(1))
                .andExpect(jsonPath("$.data.artifactFormat").value("PARQUET"))
                .andExpect(jsonPath("$.data.artifactSchemaVersion")
                        .value("bpi.dataset-parquet.v1"))
                .andExpect(jsonPath("$.data.materializerVersion")
                        .value("bpi-dataset-materializer/0.1.0"))
                .andExpect(jsonPath("$.data.artifactUri").doesNotExist())
                .andReturn();
        UUID materializationId = UUID.fromString(
                response(materializationResult).path("data").path("id").asText());

        mockMvc.perform(post("/bpi/v1/dataset-snapshots/{id}/materializations", firstSnapshotId)
                        .header("Authorization", "Bearer " + engineerToken)
                        .header("Idempotency-Key", materializationKey)
                        .header("If-Match", snapshotRevision)
                        .contentType(MediaType.APPLICATION_JSON).content(materializationBody))
                .andExpect(status().isAccepted())
                .andExpect(header().string("Idempotent-Replay", "true"))
                .andExpect(jsonPath("$.data.id").value(materializationId.toString()));

        byte[] changedMaterializationBody = objectMapper.writeValueAsBytes(Map.of(
                "artifactFormat", "PARQUET",
                "reason", "Reuse must be rejected"));
        mockMvc.perform(post("/bpi/v1/dataset-snapshots/{id}/materializations", firstSnapshotId)
                        .header("Authorization", "Bearer " + engineerToken)
                        .header("Idempotency-Key", materializationKey)
                        .header("If-Match", snapshotRevision)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(changedMaterializationBody))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/bpi/v1/dataset-snapshots/{id}/materializations", firstSnapshotId)
                        .header("Authorization", "Bearer " + engineerToken)
                        .header("Idempotency-Key", "materialization-duplicate-" + tenantId)
                        .header("If-Match", snapshotRevision)
                        .contentType(MediaType.APPLICATION_JSON).content(materializationBody))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/bpi/v1/dataset-materializations/{id}", materializationId)
                        .header("Authorization", "Bearer " + wrongScopeToken))
                .andExpect(status().isNotFound());
        var queuedRead = mockMvc.perform(
                        get("/bpi/v1/dataset-materializations/{id}", materializationId)
                                .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.snapshotId").value(firstSnapshotId.toString()));
        if (!externalMaterializer) {
            queuedRead.andExpect(jsonPath("$.data.state").value("QUEUED"));
        }

        mockMvc.perform(post("/bpi/v1/dataset-materializations/{id}/retry", materializationId)
                        .header("Authorization", "Bearer " + engineerToken)
                        .header("Idempotency-Key", "materialization-premature-retry-" + tenantId)
                        .header("If-Match", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "reason", "A queued task cannot be retried"))))
                .andExpect(status().isConflict());

        String contentSha;
        String objectKey;
        String artifactUri;
        if (externalMaterializer) {
            Map<String, Object> artifact = awaitExternalMaterialization(materializationId);
            contentSha = (String) artifact.get("content_sha256");
            objectKey = (String) artifact.get("object_key");
            artifactUri = (String) artifact.get("artifact_uri");
            String objectVersionId = (String) artifact.get("object_version_id");
            assertThat(contentSha).hasSize(64);
            assertThat(objectKey)
                    .startsWith("datasets/%s/%s/bpi-dataset-materializer-0.1.0/"
                            .formatted(firstSnapshotId, firstChecksum))
                    .endsWith("/" + contentSha + ".parquet");
            String versionedUriPrefix = "s3://bpi-datasets/" + objectKey + "?versionId=";
            assertThat(artifactUri).startsWith(versionedUriPrefix);
            assertThat(URLDecoder.decode(
                    artifactUri.substring(versionedUriPrefix.length()), StandardCharsets.UTF_8))
                    .isEqualTo(objectVersionId);
            assertThat(objectVersionId).isNotBlank();
            assertThat(artifact).containsEntry("object_content_verified", "true");
            assertThat(((Number) artifact.get("byte_size")).longValue()).isPositive();
            assertThat(((Number) artifact.get("row_count")).longValue()).isEqualTo(1L);
            assertThat(artifact).containsEntry("source_payload_included", "false")
                    .containsEntry("excluded_samples_included", "false")
                    .containsEntry("iceberg_ready", "false")
                    .containsEntry("mlflow_registered", "false")
                    .containsEntry("model_trained", "false");
        } else {
            UUID failedClaim = UUID.randomUUID();
            assertThat(jdbc.update("""
                    UPDATE bpi.bpi_dataset_materializations
                       SET state = 'WRITING', revision = revision + 1,
                           started_at = now(), claim_token = ?, claimed_at = now(),
                           attempt_count = attempt_count + 1
                     WHERE tenant_id = ? AND id = ? AND state = 'QUEUED'
                    """, failedClaim, tenantId, materializationId)).isEqualTo(1);
            assertThat(jdbc.update("""
                    UPDATE bpi.bpi_dataset_materializations
                       SET state = 'FAILED', revision = revision + 1,
                           completed_at = now(), claim_token = NULL, claimed_at = NULL,
                           failure_code = 'MINIO_UNAVAILABLE',
                           failure_detail = 'Acceptance-injected transient failure'
                     WHERE tenant_id = ? AND id = ? AND state = 'WRITING'
                       AND claim_token = ?
                    """, tenantId, materializationId, failedClaim)).isEqualTo(1);

            mockMvc.perform(post("/bpi/v1/dataset-materializations/{id}/retry", materializationId)
                            .header("Authorization", "Bearer " + wrongScopeToken)
                            .header("Idempotency-Key", "materialization-retry-scope-" + tenantId)
                            .header("If-Match", 3)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsBytes(Map.of(
                                    "reason", "Wrong line scope must stay hidden"))))
                    .andExpect(status().isNotFound());

            byte[] retryBody = objectMapper.writeValueAsBytes(Map.of(
                    "reason", "Retry after the transient object-store failure"));
            String retryKey = "materialization-retry-" + tenantId;
            mockMvc.perform(post("/bpi/v1/dataset-materializations/{id}/retry", materializationId)
                            .header("Authorization", "Bearer " + engineerToken)
                            .header("Idempotency-Key", retryKey)
                            .header("If-Match", 3)
                            .contentType(MediaType.APPLICATION_JSON).content(retryBody))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.data.state").value("QUEUED"))
                    .andExpect(jsonPath("$.data.revision").value(4))
                    .andExpect(jsonPath("$.data.failureCode").doesNotExist());
            mockMvc.perform(post("/bpi/v1/dataset-materializations/{id}/retry", materializationId)
                            .header("Authorization", "Bearer " + engineerToken)
                            .header("Idempotency-Key", retryKey)
                            .header("If-Match", 3)
                            .contentType(MediaType.APPLICATION_JSON).content(retryBody))
                    .andExpect(status().isAccepted())
                    .andExpect(header().string("Idempotent-Replay", "true"))
                    .andExpect(jsonPath("$.data.revision").value(4));

            contentSha = "d".repeat(64);
            objectKey = "datasets/%s/%s/bpi-dataset-materializer-0.1.0/%s.parquet"
                    .formatted(firstSnapshotId, firstChecksum, contentSha);
            String objectVersionId = "acceptance-version-1";
            artifactUri = "s3://bpi-datasets/" + objectKey
                    + "?versionId=" + objectVersionId;
            UUID readyClaim = UUID.randomUUID();
            assertThat(jdbc.update("""
                    UPDATE bpi.bpi_dataset_materializations
                       SET state = 'WRITING', revision = revision + 1,
                           started_at = now(), claim_token = ?, claimed_at = now(),
                           attempt_count = attempt_count + 1
                     WHERE tenant_id = ? AND id = ? AND state = 'QUEUED'
                    """, readyClaim, tenantId, materializationId)).isEqualTo(1);
            assertThatThrownBy(() -> jdbc.update("""
                    UPDATE bpi.bpi_dataset_materializations
                       SET state = 'READY', revision = revision + 1,
                           completed_at = now(), claim_token = NULL, claimed_at = NULL,
                           artifact_uri = ?, object_bucket = 'bpi-datasets', object_key = ?,
                           content_sha256 = ?, byte_size = 4096, row_count = 1,
                           schema_json = CAST(? AS jsonb),
                           artifact_metadata = CAST(? AS jsonb)
                     WHERE tenant_id = ? AND id = ? AND state = 'WRITING'
                       AND claim_token = ?
                    """, "s3://bpi-datasets/" + objectKey, objectKey, contentSha,
                    "{\"version\":\"bpi.dataset-parquet.v1\"}",
                    "{\"objectContentVerified\":false}",
                    tenantId, materializationId, readyClaim))
                    .isInstanceOf(DataAccessException.class)
                    .hasMessageContaining("chk_bpi_dataset_materialization_lifecycle");
            assertThat(jdbc.update("""
                    UPDATE bpi.bpi_dataset_materializations
                       SET state = 'READY', revision = revision + 1,
                           completed_at = now(), claim_token = NULL, claimed_at = NULL,
                           artifact_uri = ?, object_bucket = 'bpi-datasets', object_key = ?,
                           content_sha256 = ?, byte_size = 4096, row_count = 1,
                           schema_json = CAST(? AS jsonb),
                           artifact_metadata = CAST(? AS jsonb)
                     WHERE tenant_id = ? AND id = ? AND state = 'WRITING'
                       AND claim_token = ?
                    """, artifactUri, objectKey, contentSha,
                    "{\"version\":\"bpi.dataset-parquet.v1\"}",
                    "{\"compression\":\"zstd\","
                            + "\"objectVersionId\":\"" + objectVersionId + "\","
                            + "\"objectContentVerified\":true,\"icebergReady\":false,"
                            + "\"mlflowRegistered\":false,\"modelTrained\":false}",
                    tenantId, materializationId, readyClaim)).isEqualTo(1);
        }

        mockMvc.perform(get("/bpi/v1/dataset-snapshots/{id}", firstSnapshotId)
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("MANIFEST_READY"))
                .andExpect(jsonPath("$.data.materializationState").value("READY"))
                .andExpect(jsonPath("$.data.artifactUri").value(artifactUri))
                .andExpect(jsonPath("$.data.latestMaterialization.id")
                        .value(materializationId.toString()))
                .andExpect(jsonPath("$.data.latestMaterialization.state").value("READY"))
                .andExpect(jsonPath("$.data.latestMaterialization.contentSha256")
                        .value(contentSha));

        proveCatalogPublicationLifecycle(
                materializationId, datasetId, firstSnapshotId, firstChecksum, contentSha);

        if (externalMaterializer) {
            assertThat(jdbc.queryForObject("""
                    SELECT count(*) FROM bpi.bpi_audit_events
                     WHERE tenant_id = ? AND object_id = ?
                       AND action IN ('DATASET_MATERIALIZATION_QUEUED',
                                      'DATASET_MATERIALIZATION_WRITING',
                                      'DATASET_MATERIALIZATION_READY')
                    """, Long.class, tenantId, materializationId)).isEqualTo(3L);
        } else {
            assertThat(jdbc.queryForObject("""
                    SELECT count(*) FROM bpi.bpi_audit_events
                     WHERE tenant_id = ? AND object_id = ?
                       AND action IN ('DATASET_MATERIALIZATION_QUEUED',
                                      'DATASET_MATERIALIZATION_RETRIED')
                    """, Long.class, tenantId, materializationId)).isEqualTo(2L);
        }
        assertThatThrownBy(() -> jdbc.update("""
                UPDATE bpi.bpi_dataset_materializations SET request_reason = 'mutated'
                 WHERE tenant_id = ? AND id = ?
                """, tenantId, materializationId))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("immutable");

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

    private void proveCatalogPublicationLifecycle(
            UUID materializationId,
            UUID datasetId,
            UUID snapshotId,
            String manifestChecksum,
            String sourceContentSha256) throws Exception {
        long materializationRevision = jdbc.queryForObject("""
                SELECT revision FROM bpi.bpi_dataset_materializations
                 WHERE tenant_id = ? AND id = ?
                """, Long.class, tenantId, materializationId);
        byte[] requestBody = objectMapper.writeValueAsBytes(Map.of(
                "reason", "Publish the verified Parquet version to the Iceberg catalog"));

        mockMvc.perform(get(
                        "/bpi/v1/dataset-materializations/{id}/catalog-publications",
                        materializationId)
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").doesNotExist());

        mockMvc.perform(post(
                        "/bpi/v1/dataset-materializations/{id}/catalog-publications",
                        materializationId)
                        .header("Authorization", "Bearer " + viewerToken)
                        .header("Idempotency-Key", "catalog-viewer-denied-" + tenantId)
                        .header("If-Match", materializationRevision)
                        .contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(
                        "/bpi/v1/dataset-materializations/{id}/catalog-publications",
                        materializationId)
                        .header("Authorization", "Bearer " + wrongScopeToken)
                        .header("Idempotency-Key", "catalog-scope-denied-" + tenantId)
                        .header("If-Match", materializationRevision)
                        .contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isNotFound());
        mockMvc.perform(post(
                        "/bpi/v1/dataset-materializations/{id}/catalog-publications",
                        materializationId)
                        .header("Authorization", "Bearer " + engineerToken)
                        .header("Idempotency-Key", "catalog-stale-" + tenantId)
                        .header("If-Match", materializationRevision - 1)
                        .contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isConflict());

        String requestKey = "catalog-request-" + tenantId;
        MvcResult requestResult = mockMvc.perform(post(
                        "/bpi/v1/dataset-materializations/{id}/catalog-publications",
                        materializationId)
                        .header("Authorization", "Bearer " + engineerToken)
                        .header("Idempotency-Key", requestKey)
                        .header("If-Match", materializationRevision)
                        .header("X-Trace-Id", tenantId + "_CATALOG")
                        .contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.state").value("QUEUED"))
                .andExpect(jsonPath("$.data.revision").value(1))
                .andExpect(jsonPath("$.data.materializationId")
                        .value(materializationId.toString()))
                .andExpect(jsonPath("$.data.snapshotId").value(snapshotId.toString()))
                .andExpect(jsonPath("$.data.datasetId").value(datasetId.toString()))
                .andExpect(jsonPath("$.data.catalogName").value("ft_mes_bpi"))
                .andExpect(jsonPath("$.data.publisherVersion")
                        .value("bpi-dataset-catalog-publisher/0.1.0"))
                .andExpect(jsonPath("$.data.manifestChecksum").value(manifestChecksum))
                .andExpect(jsonPath("$.data.sourceContentSha256")
                        .value(sourceContentSha256))
                .andExpect(jsonPath("$.data.icebergSnapshotId").doesNotExist())
                .andReturn();
        UUID publicationId = UUID.fromString(
                response(requestResult).path("data").path("id").asText());

        mockMvc.perform(get(
                        "/bpi/v1/dataset-materializations/{id}/catalog-publications",
                        materializationId)
                        .header("Authorization", "Bearer " + wrongScopeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").doesNotExist());
        mockMvc.perform(get(
                        "/bpi/v1/dataset-materializations/{id}/catalog-publications",
                        materializationId)
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(publicationId.toString()))
                .andExpect(jsonPath("$.data.state").value("QUEUED"));

        mockMvc.perform(post(
                        "/bpi/v1/dataset-materializations/{id}/catalog-publications",
                        materializationId)
                        .header("Authorization", "Bearer " + engineerToken)
                        .header("Idempotency-Key", requestKey)
                        .header("If-Match", materializationRevision)
                        .contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isAccepted())
                .andExpect(header().string("Idempotent-Replay", "true"))
                .andExpect(jsonPath("$.data.id").value(publicationId.toString()));
        mockMvc.perform(post(
                        "/bpi/v1/dataset-materializations/{id}/catalog-publications",
                        materializationId)
                        .header("Authorization", "Bearer " + engineerToken)
                        .header("Idempotency-Key", "catalog-duplicate-" + tenantId)
                        .header("If-Match", materializationRevision)
                        .contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/bpi/v1/dataset-catalog-publications/{id}", publicationId)
                        .header("Authorization", "Bearer " + wrongScopeToken))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/bpi/v1/dataset-catalog-publications/{id}", publicationId)
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("QUEUED"));
        mockMvc.perform(post(
                        "/bpi/v1/dataset-catalog-publications/{id}/retry", publicationId)
                        .header("Authorization", "Bearer " + engineerToken)
                        .header("Idempotency-Key", "catalog-premature-retry-" + tenantId)
                        .header("If-Match", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "reason", "A queued publication cannot be retried"))))
                .andExpect(status().isConflict());

        if (Boolean.parseBoolean(env("BPI_TEST_EXTERNAL_CATALOG_PUBLISHER", "false"))) {
            Map<String, Object> publication = awaitExternalCatalogPublication(publicationId);
            String snapshotIdText = (String) publication.get("iceberg_snapshot_id");
            assertThat(snapshotIdText).isNotBlank().matches("-?[0-9]+");
            assertThat(publication)
                    .containsEntry("verified_row_count", 1L)
                    .containsEntry("catalog_snapshot_verified", "true")
                    .containsEntry("source_version_verified", "true")
                    .containsEntry("manifest_checksum_verified", "true")
                    .containsEntry("iceberg_ready", "true")
                    .containsEntry("mlflow_registered", "false")
                    .containsEntry("model_trained", "false");
            assertThat((String) publication.get("semantic_checksum")).hasSize(64);
            assertThat((String) publication.get("iceberg_metadata_location"))
                    .startsWith("s3://bpi-iceberg-warehouse/warehouse/")
                    .endsWith(".metadata.json");
            assertThat((String) publication.get("table_identifier"))
                    .startsWith("ft_mes_bpi.bpi_training.");

            mockMvc.perform(get("/bpi/v1/dataset-catalog-publications/{id}", publicationId)
                            .header("Authorization", "Bearer " + viewerToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.state").value("READY"))
                    .andExpect(jsonPath("$.data.icebergSnapshotId").value(snapshotIdText))
                    .andExpect(jsonPath("$.data.verifiedRowCount").value(1))
                    .andExpect(jsonPath("$.data.catalogMetadata.catalogSnapshotVerified")
                            .value(true))
                    .andExpect(jsonPath("$.data.catalogMetadata.mlflowRegistered")
                            .value(false));
            assertThat(jdbc.queryForObject("""
                    SELECT count(*) FROM bpi.bpi_audit_events
                     WHERE tenant_id = ? AND object_id = ?
                       AND action IN ('DATASET_CATALOG_PUBLICATION_COMMITTING',
                                      'DATASET_CATALOG_PUBLICATION_VERIFYING',
                                      'DATASET_CATALOG_PUBLICATION_READY')
                    """, Long.class, tenantId, publicationId)).isEqualTo(3L);
            proveRetentionArchiveLifecycle(publicationId);
            return;
        }

        UUID failedClaim = UUID.randomUUID();
        assertThat(jdbc.update("""
                UPDATE bpi.bpi_dataset_catalog_publications
                   SET state = 'COMMITTING', revision = revision + 1,
                       started_at = now(), claim_token = ?, claimed_at = now(),
                       attempt_count = attempt_count + 1
                 WHERE tenant_id = ? AND id = ? AND state = 'QUEUED'
                """, failedClaim, tenantId, publicationId)).isEqualTo(1);
        assertThat(jdbc.update("""
                UPDATE bpi.bpi_dataset_catalog_publications
                   SET state = 'FAILED', revision = revision + 1,
                       completed_at = now(), claim_token = NULL, claimed_at = NULL,
                       failure_code = 'POLARIS_UNAVAILABLE',
                       failure_detail = 'Acceptance-injected transient catalog failure'
                 WHERE tenant_id = ? AND id = ? AND state = 'COMMITTING'
                   AND claim_token = ?
                """, tenantId, publicationId, failedClaim)).isEqualTo(1);

        byte[] retryBody = objectMapper.writeValueAsBytes(Map.of(
                "reason", "Retry after the transient catalog failure"));
        String retryKey = "catalog-retry-" + tenantId;
        mockMvc.perform(post(
                        "/bpi/v1/dataset-catalog-publications/{id}/retry", publicationId)
                        .header("Authorization", "Bearer " + engineerToken)
                        .header("Idempotency-Key", retryKey)
                        .header("If-Match", 3)
                        .contentType(MediaType.APPLICATION_JSON).content(retryBody))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.state").value("QUEUED"))
                .andExpect(jsonPath("$.data.revision").value(4))
                .andExpect(jsonPath("$.data.failureCode").doesNotExist());
        mockMvc.perform(post(
                        "/bpi/v1/dataset-catalog-publications/{id}/retry", publicationId)
                        .header("Authorization", "Bearer " + engineerToken)
                        .header("Idempotency-Key", retryKey)
                        .header("If-Match", 3)
                        .contentType(MediaType.APPLICATION_JSON).content(retryBody))
                .andExpect(status().isAccepted())
                .andExpect(header().string("Idempotent-Replay", "true"))
                .andExpect(jsonPath("$.data.revision").value(4));

        UUID readyClaim = UUID.randomUUID();
        assertThat(jdbc.update("""
                UPDATE bpi.bpi_dataset_catalog_publications
                   SET state = 'COMMITTING', revision = revision + 1,
                       started_at = now(), claim_token = ?, claimed_at = now(),
                       attempt_count = attempt_count + 1
                 WHERE tenant_id = ? AND id = ? AND state = 'QUEUED'
                """, readyClaim, tenantId, publicationId)).isEqualTo(1);
        long icebergSnapshotId = 792644343122L;
        String metadataLocation = "s3://bpi-iceberg/warehouse/acceptance/metadata/v1.metadata.json";
        assertThat(jdbc.update("""
                UPDATE bpi.bpi_dataset_catalog_publications
                   SET state = 'VERIFYING', revision = revision + 1,
                       iceberg_snapshot_id = ?, iceberg_metadata_location = ?,
                       iceberg_schema_id = 0, iceberg_partition_spec_id = 0
                 WHERE tenant_id = ? AND id = ? AND state = 'COMMITTING'
                   AND claim_token = ?
                """, icebergSnapshotId, metadataLocation,
                tenantId, publicationId, readyClaim)).isEqualTo(1);
        assertThatThrownBy(() -> jdbc.update("""
                UPDATE bpi.bpi_dataset_catalog_publications
                   SET state = 'READY', revision = revision + 1,
                       completed_at = now(), claim_token = NULL, claimed_at = NULL,
                       verified_row_count = source_row_count + 1,
                       semantic_checksum = ?,
                       catalog_metadata = '{"catalogSnapshotVerified":true}'::jsonb
                 WHERE tenant_id = ? AND id = ? AND state = 'VERIFYING'
                   AND claim_token = ?
                """, "e".repeat(64), tenantId, publicationId, readyClaim))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("chk_bpi_dataset_catalog_publication_lifecycle");
        String semanticChecksum = "e".repeat(64);
        assertThat(jdbc.update("""
                UPDATE bpi.bpi_dataset_catalog_publications
                   SET state = 'READY', revision = revision + 1,
                       completed_at = now(), claim_token = NULL, claimed_at = NULL,
                       verified_row_count = source_row_count,
                       semantic_checksum = ?,
                       catalog_metadata = CAST(? AS jsonb)
                 WHERE tenant_id = ? AND id = ? AND state = 'VERIFYING'
                   AND claim_token = ?
                """, semanticChecksum,
                "{\"catalogSnapshotVerified\":true,\"sourceVersionVerified\":true}",
                tenantId, publicationId, readyClaim)).isEqualTo(1);

        mockMvc.perform(get("/bpi/v1/dataset-catalog-publications/{id}", publicationId)
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("READY"))
                .andExpect(jsonPath("$.data.revision").value(7))
                .andExpect(jsonPath("$.data.attemptCount").value(2))
                .andExpect(jsonPath("$.data.icebergSnapshotId")
                        .value(String.valueOf(icebergSnapshotId)))
                .andExpect(jsonPath("$.data.icebergMetadataLocation")
                        .value(metadataLocation))
                .andExpect(jsonPath("$.data.verifiedRowCount").value(1))
                .andExpect(jsonPath("$.data.semanticChecksum").value(semanticChecksum))
                .andExpect(jsonPath("$.data.catalogMetadata.catalogSnapshotVerified")
                        .value(true));
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM bpi.bpi_audit_events
                 WHERE tenant_id = ? AND object_id = ?
                   AND action IN ('DATASET_CATALOG_PUBLICATION_QUEUED',
                                  'DATASET_CATALOG_PUBLICATION_RETRIED')
                """, Long.class, tenantId, publicationId)).isEqualTo(2L);
        proveRetentionArchiveLifecycle(publicationId);
        assertThatThrownBy(() -> jdbc.update("""
                UPDATE bpi.bpi_dataset_catalog_publications
                   SET request_reason = 'mutated'
                 WHERE tenant_id = ? AND id = ?
                """, tenantId, publicationId))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("immutable");
    }

    private void proveRetentionArchiveLifecycle(UUID publicationId) throws Exception {
        long publicationRevision = jdbc.queryForObject("""
                SELECT revision FROM bpi.bpi_dataset_catalog_publications
                 WHERE tenant_id = ? AND id = ? AND state = 'READY'
                """, Long.class, tenantId, publicationId);
        String catalogSemanticChecksum = jdbc.queryForObject("""
                SELECT semantic_checksum FROM bpi.bpi_dataset_catalog_publications
                 WHERE tenant_id = ? AND id = ?
                """, String.class, tenantId, publicationId);
        byte[] requestBody = objectMapper.writeValueAsBytes(Map.of(
                "reason", "Freeze an exact Object Lock dataset recovery package"));

        mockMvc.perform(get(
                        "/bpi/v1/dataset-catalog-publications/{id}/retention-archives",
                        publicationId)
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").doesNotExist());
        mockMvc.perform(post(
                        "/bpi/v1/dataset-catalog-publications/{id}/retention-archives",
                        publicationId)
                        .header("Authorization", "Bearer " + viewerToken)
                        .header("Idempotency-Key", "archive-viewer-denied-" + tenantId)
                        .header("If-Match", publicationRevision)
                        .contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(
                        "/bpi/v1/dataset-catalog-publications/{id}/retention-archives",
                        publicationId)
                        .header("Authorization", "Bearer " + wrongScopeToken)
                        .header("Idempotency-Key", "archive-scope-denied-" + tenantId)
                        .header("If-Match", publicationRevision)
                        .contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isNotFound());
        mockMvc.perform(post(
                        "/bpi/v1/dataset-catalog-publications/{id}/retention-archives",
                        publicationId)
                        .header("Authorization", "Bearer " + engineerToken)
                        .header("Idempotency-Key", "archive-stale-" + tenantId)
                        .header("If-Match", publicationRevision - 1)
                        .contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isConflict());

        String requestKey = "archive-request-" + tenantId;
        MvcResult requestResult = mockMvc.perform(post(
                        "/bpi/v1/dataset-catalog-publications/{id}/retention-archives",
                        publicationId)
                        .header("Authorization", "Bearer " + engineerToken)
                        .header("Idempotency-Key", requestKey)
                        .header("If-Match", publicationRevision)
                        .header("X-Trace-Id", tenantId + "_RETENTION_ARCHIVE")
                        .contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.state").value("QUEUED"))
                .andExpect(jsonPath("$.data.revision").value(1))
                .andExpect(jsonPath("$.data.catalogPublicationId")
                        .value(publicationId.toString()))
                .andExpect(jsonPath("$.data.archiverVersion")
                        .value("bpi-dataset-retention-archiver/0.1.0"))
                .andExpect(jsonPath("$.data.archiveProfile")
                        .value("bpi-dataset-recovery-v1"))
                .andExpect(jsonPath("$.data.catalogSemanticChecksum")
                        .value(catalogSemanticChecksum))
                .andExpect(jsonPath("$.data.retentionMode").doesNotExist())
                .andReturn();
        UUID archiveId = UUID.fromString(
                response(requestResult).path("data").path("id").asText());

        mockMvc.perform(post(
                        "/bpi/v1/dataset-catalog-publications/{id}/retention-archives",
                        publicationId)
                        .header("Authorization", "Bearer " + engineerToken)
                        .header("Idempotency-Key", requestKey)
                        .header("If-Match", publicationRevision)
                        .contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isAccepted())
                .andExpect(header().string("Idempotent-Replay", "true"))
                .andExpect(jsonPath("$.data.id").value(archiveId.toString()));
        mockMvc.perform(post(
                        "/bpi/v1/dataset-catalog-publications/{id}/retention-archives",
                        publicationId)
                        .header("Authorization", "Bearer " + engineerToken)
                        .header("Idempotency-Key", "archive-duplicate-" + tenantId)
                        .header("If-Match", publicationRevision)
                        .contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isConflict());
        mockMvc.perform(get("/bpi/v1/dataset-retention-archives/{id}", archiveId)
                        .header("Authorization", "Bearer " + wrongScopeToken))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/bpi/v1/dataset-retention-archives/{id}", archiveId)
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("QUEUED"));
        mockMvc.perform(post(
                        "/bpi/v1/dataset-retention-archives/{id}/retry", archiveId)
                        .header("Authorization", "Bearer " + engineerToken)
                        .header("Idempotency-Key", "archive-premature-retry-" + tenantId)
                        .header("If-Match", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "reason", "A queued recovery package cannot be retried"))))
                .andExpect(status().isConflict());

        if (Boolean.parseBoolean(env("BPI_TEST_EXTERNAL_RETENTION_ARCHIVER", "false"))) {
            Map<String, Object> archive = awaitExternalRetentionArchive(archiveId);
            assertThat(archive)
                    .containsEntry("state", "LOCKED")
                    .containsEntry("retention_mode", "GOVERNANCE")
                    .containsEntry("legal_hold_enabled", false)
                    .containsEntry("archive_bucket", "bpi-dataset-recovery")
                    .containsEntry("archive_object_count", 2)
                    .containsEntry("verified_row_count", 1L)
                    .containsEntry("verified_semantic_checksum", catalogSemanticChecksum)
                    .containsEntry("object_lock_verified", "true")
                    .containsEntry("recovery_verified", "true")
                    .containsEntry("mlflow_registered", "false")
                    .containsEntry("model_trained", "false");
            assertThat((String) archive.get("source_archive_version_id")).isNotBlank();
            assertThat((String) archive.get("archive_manifest_version_id")).isNotBlank();
            assertThat((String) archive.get("archive_manifest_sha256")).hasSize(64);
            assertThat((String) archive.get("archive_prefix"))
                    .startsWith("archives/tenant_")
                    .contains(publicationId.toString())
                    .endsWith(archiveId.toString());
            mockMvc.perform(get("/bpi/v1/dataset-retention-archives/{id}", archiveId)
                            .header("Authorization", "Bearer " + viewerToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.state").value("LOCKED"))
                    .andExpect(jsonPath("$.data.verifiedSemanticChecksum")
                            .value(catalogSemanticChecksum))
                    .andExpect(jsonPath("$.data.archiveMetadata.objectLockVerified")
                            .value(true))
                    .andExpect(jsonPath("$.data.archiveMetadata.recoveryVerified")
                            .value(true));
            assertThat(jdbc.queryForObject("""
                    SELECT count(*) FROM bpi.bpi_audit_events
                     WHERE tenant_id = ? AND object_id = ?
                       AND action IN ('DATASET_RETENTION_ARCHIVE_ARCHIVING',
                                      'DATASET_RETENTION_ARCHIVE_VERIFYING',
                                      'DATASET_RETENTION_ARCHIVE_LOCKED')
                    """, Long.class, tenantId, archiveId)).isEqualTo(3L);
            assertThatThrownBy(() -> jdbc.update("""
                    UPDATE bpi.bpi_dataset_retention_archives
                       SET request_reason = 'mutated'
                     WHERE tenant_id = ? AND id = ?
                    """, tenantId, archiveId))
                    .isInstanceOf(DataAccessException.class)
                    .hasMessageContaining("LOCKED");
            proveMlflowRegistrationLifecycle(archiveId);
            return;
        }

        UUID failedClaim = UUID.randomUUID();
        assertThat(jdbc.update("""
                UPDATE bpi.bpi_dataset_retention_archives
                   SET state = 'ARCHIVING', revision = revision + 1,
                       started_at = now(), claim_token = ?, claimed_at = now(),
                       attempt_count = attempt_count + 1,
                       retention_mode = 'GOVERNANCE',
                       retain_until = now() + interval '30 days',
                       legal_hold_enabled = false
                 WHERE tenant_id = ? AND id = ? AND state = 'QUEUED'
                """, failedClaim, tenantId, archiveId)).isEqualTo(1);
        String prefix = "archives/tenant_acceptance/" + publicationId + "/" + archiveId;
        String sourceVersion = "retained-source-version-001";
        String manifestVersion = "retained-manifest-version-001";
        String manifestSha = "f".repeat(64);
        assertThat(jdbc.update("""
                UPDATE bpi.bpi_dataset_retention_archives
                   SET state = 'VERIFYING', revision = revision + 1,
                       archive_bucket = 'bpi-dataset-recovery', archive_prefix = ?,
                       source_archive_object_key = ?, source_archive_version_id = ?,
                       archive_manifest_object_key = ?, archive_manifest_version_id = ?,
                       archive_manifest_sha256 = ?, archive_object_count = 2,
                       archive_total_bytes = source_byte_size + 2048
                 WHERE tenant_id = ? AND id = ? AND state = 'ARCHIVING'
                   AND claim_token = ?
                """, prefix, prefix + "/source.parquet", sourceVersion,
                prefix + "/recovery-manifest.json", manifestVersion, manifestSha,
                tenantId, archiveId, failedClaim)).isEqualTo(1);
        assertThat(jdbc.update("""
                UPDATE bpi.bpi_dataset_retention_archives
                   SET state = 'FAILED', revision = revision + 1,
                       completed_at = now(), claim_token = NULL, claimed_at = NULL,
                       failure_code = 'OBJECT_LOCK_STORE_ERROR',
                       failure_detail = 'Acceptance-injected post-write verification failure'
                 WHERE tenant_id = ? AND id = ? AND state = 'VERIFYING'
                   AND claim_token = ?
                """, tenantId, archiveId, failedClaim)).isEqualTo(1);

        byte[] retryBody = objectMapper.writeValueAsBytes(Map.of(
                "reason", "Retry exact-version recovery verification after store recovery"));
        String retryKey = "archive-retry-" + tenantId;
        mockMvc.perform(post(
                        "/bpi/v1/dataset-retention-archives/{id}/retry", archiveId)
                        .header("Authorization", "Bearer " + engineerToken)
                        .header("Idempotency-Key", retryKey)
                        .header("If-Match", 4)
                        .contentType(MediaType.APPLICATION_JSON).content(retryBody))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.state").value("QUEUED"))
                .andExpect(jsonPath("$.data.revision").value(5))
                .andExpect(jsonPath("$.data.sourceArchiveVersionId")
                        .value(sourceVersion))
                .andExpect(jsonPath("$.data.archiveManifestVersionId")
                        .value(manifestVersion));
        mockMvc.perform(post(
                        "/bpi/v1/dataset-retention-archives/{id}/retry", archiveId)
                        .header("Authorization", "Bearer " + engineerToken)
                        .header("Idempotency-Key", retryKey)
                        .header("If-Match", 4)
                        .contentType(MediaType.APPLICATION_JSON).content(retryBody))
                .andExpect(status().isAccepted())
                .andExpect(header().string("Idempotent-Replay", "true"))
                .andExpect(jsonPath("$.data.revision").value(5));

        UUID readyClaim = UUID.randomUUID();
        assertThat(jdbc.update("""
                UPDATE bpi.bpi_dataset_retention_archives
                   SET state = 'ARCHIVING', revision = revision + 1,
                       started_at = now(), claim_token = ?, claimed_at = now(),
                       attempt_count = attempt_count + 1
                 WHERE tenant_id = ? AND id = ? AND state = 'QUEUED'
                """, readyClaim, tenantId, archiveId)).isEqualTo(1);
        assertThat(jdbc.update("""
                UPDATE bpi.bpi_dataset_retention_archives
                   SET state = 'VERIFYING', revision = revision + 1
                 WHERE tenant_id = ? AND id = ? AND state = 'ARCHIVING'
                   AND claim_token = ?
                """, tenantId, archiveId, readyClaim)).isEqualTo(1);
        assertThatThrownBy(() -> jdbc.update("""
                UPDATE bpi.bpi_dataset_retention_archives
                   SET state = 'LOCKED', revision = revision + 1,
                       completed_at = now(), claim_token = NULL, claimed_at = NULL,
                       verified_row_count = catalog_verified_row_count,
                       verified_semantic_checksum = ?,
                       archive_metadata = '{"objectLockVerified":true,"recoveryVerified":true}'::jsonb
                 WHERE tenant_id = ? AND id = ? AND state = 'VERIFYING'
                   AND claim_token = ?
                """, "0".repeat(64), tenantId, archiveId, readyClaim))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("chk_bpi_dataset_retention_archive_lifecycle");
        assertThat(jdbc.update("""
                UPDATE bpi.bpi_dataset_retention_archives
                   SET state = 'LOCKED', revision = revision + 1,
                       completed_at = now(), claim_token = NULL, claimed_at = NULL,
                       verified_row_count = catalog_verified_row_count,
                       verified_semantic_checksum = catalog_semantic_checksum,
                       archive_metadata = CAST(? AS jsonb)
                 WHERE tenant_id = ? AND id = ? AND state = 'VERIFYING'
                   AND claim_token = ?
                """, "{\"objectLockVerified\":true,\"recoveryVerified\":true,"
                + "\"sourceVersionVerified\":true,\"manifestVersionVerified\":true,"
                + "\"mlflowRegistered\":false,\"modelTrained\":false}",
                tenantId, archiveId, readyClaim)).isEqualTo(1);

        mockMvc.perform(get("/bpi/v1/dataset-retention-archives/{id}", archiveId)
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("LOCKED"))
                .andExpect(jsonPath("$.data.revision").value(8))
                .andExpect(jsonPath("$.data.attemptCount").value(2))
                .andExpect(jsonPath("$.data.retentionMode").value("GOVERNANCE"))
                .andExpect(jsonPath("$.data.archiveBucket").value("bpi-dataset-recovery"))
                .andExpect(jsonPath("$.data.sourceArchiveVersionId").value(sourceVersion))
                .andExpect(jsonPath("$.data.archiveManifestVersionId").value(manifestVersion))
                .andExpect(jsonPath("$.data.verifiedSemanticChecksum")
                        .value(catalogSemanticChecksum))
                .andExpect(jsonPath("$.data.archiveMetadata.objectLockVerified").value(true))
                .andExpect(jsonPath("$.data.archiveMetadata.recoveryVerified").value(true))
                .andExpect(jsonPath("$.data.archiveMetadata.mlflowRegistered").value(false));
        mockMvc.perform(post(
                        "/bpi/v1/dataset-retention-archives/{id}/retry", archiveId)
                        .header("Authorization", "Bearer " + engineerToken)
                        .header("Idempotency-Key", "archive-locked-retry-" + tenantId)
                        .header("If-Match", 8)
                        .contentType(MediaType.APPLICATION_JSON).content(retryBody))
                .andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM bpi.bpi_audit_events
                 WHERE tenant_id = ? AND object_id = ?
                   AND action IN ('DATASET_RETENTION_ARCHIVE_QUEUED',
                                  'DATASET_RETENTION_ARCHIVE_RETRIED')
                """, Long.class, tenantId, archiveId)).isEqualTo(2L);
        assertThatThrownBy(() -> jdbc.update("""
                UPDATE bpi.bpi_dataset_retention_archives
                   SET request_reason = 'mutated'
                 WHERE tenant_id = ? AND id = ?
                """, tenantId, archiveId))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("LOCKED");
        proveMlflowRegistrationLifecycle(archiveId);
    }

    private void proveMlflowRegistrationLifecycle(UUID archiveId) throws Exception {
        long archiveRevision = jdbc.queryForObject("""
                SELECT revision
                  FROM bpi.bpi_dataset_retention_archives
                 WHERE tenant_id = ? AND id = ? AND state = 'LOCKED'
                """, Long.class, tenantId, archiveId);
        String semanticChecksum = jdbc.queryForObject("""
                SELECT catalog_semantic_checksum
                  FROM bpi.bpi_dataset_retention_archives
                 WHERE tenant_id = ? AND id = ?
                """, String.class, tenantId, archiveId);
        byte[] requestBody = objectMapper.writeValueAsBytes(Map.of(
                "reason", "Register the exact locked recovery dataset in MLflow"));

        mockMvc.perform(get(
                        "/bpi/v1/dataset-retention-archives/{id}/mlflow-registrations",
                        archiveId)
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").doesNotExist());
        mockMvc.perform(post(
                        "/bpi/v1/dataset-retention-archives/{id}/mlflow-registrations",
                        archiveId)
                        .header("Authorization", "Bearer " + viewerToken)
                        .header("Idempotency-Key", "mlflow-viewer-denied-" + tenantId)
                        .header("If-Match", archiveRevision)
                        .contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(
                        "/bpi/v1/dataset-retention-archives/{id}/mlflow-registrations",
                        archiveId)
                        .header("Authorization", "Bearer " + wrongScopeToken)
                        .header("Idempotency-Key", "mlflow-scope-denied-" + tenantId)
                        .header("If-Match", archiveRevision)
                        .contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isNotFound());
        mockMvc.perform(post(
                        "/bpi/v1/dataset-retention-archives/{id}/mlflow-registrations",
                        archiveId)
                        .header("Authorization", "Bearer " + engineerToken)
                        .header("Idempotency-Key", "mlflow-stale-" + tenantId)
                        .header("If-Match", archiveRevision - 1)
                        .contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isConflict());

        String requestKey = "mlflow-registration-request-" + tenantId;
        MvcResult requestResult = mockMvc.perform(post(
                        "/bpi/v1/dataset-retention-archives/{id}/mlflow-registrations",
                        archiveId)
                        .header("Authorization", "Bearer " + engineerToken)
                        .header("Idempotency-Key", requestKey)
                        .header("If-Match", archiveRevision)
                        .header("X-Trace-Id", tenantId + "_MLFLOW_REGISTRATION")
                        .contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.state").value("QUEUED"))
                .andExpect(jsonPath("$.data.revision").value(1))
                .andExpect(jsonPath("$.data.retentionArchiveId")
                        .value(archiveId.toString()))
                .andExpect(jsonPath("$.data.registrarVersion")
                        .value("bpi-dataset-mlflow-registrar/0.1.0"))
                .andExpect(jsonPath("$.data.trackingProfile")
                        .value("bpi-mlflow-dataset-v1"))
                .andExpect(jsonPath("$.data.datasetDigest")
                        .value(semanticChecksum.substring(0, 16)))
                .andExpect(jsonPath("$.data.mlflowRunId").doesNotExist())
                .andExpect(jsonPath("$.data.registrationMetadata").doesNotExist())
                .andReturn();
        UUID registrationId = UUID.fromString(
                response(requestResult).path("data").path("id").asText());

        mockMvc.perform(post(
                        "/bpi/v1/dataset-retention-archives/{id}/mlflow-registrations",
                        archiveId)
                        .header("Authorization", "Bearer " + engineerToken)
                        .header("Idempotency-Key", requestKey)
                        .header("If-Match", archiveRevision)
                        .contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isAccepted())
                .andExpect(header().string("Idempotent-Replay", "true"))
                .andExpect(jsonPath("$.data.id").value(registrationId.toString()));
        mockMvc.perform(post(
                        "/bpi/v1/dataset-retention-archives/{id}/mlflow-registrations",
                        archiveId)
                        .header("Authorization", "Bearer " + engineerToken)
                        .header("Idempotency-Key", "mlflow-duplicate-" + tenantId)
                        .header("If-Match", archiveRevision)
                        .contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isConflict());
        mockMvc.perform(get(
                        "/bpi/v1/dataset-mlflow-registrations/{id}", registrationId)
                        .header("Authorization", "Bearer " + wrongScopeToken))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(
                        "/bpi/v1/dataset-mlflow-registrations/{id}", registrationId)
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("QUEUED"));
        mockMvc.perform(post(
                        "/bpi/v1/dataset-mlflow-registrations/{id}/retry", registrationId)
                        .header("Authorization", "Bearer " + engineerToken)
                        .header("Idempotency-Key", "mlflow-premature-retry-" + tenantId)
                        .header("If-Match", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "reason", "A queued registration cannot be retried"))))
                .andExpect(status().isConflict());

        if (Boolean.parseBoolean(env("BPI_TEST_EXTERNAL_MLFLOW_REGISTRAR", "false"))) {
            Map<String, Object> registration = awaitExternalMlflowRegistration(registrationId);
            assertThat(registration)
                    .containsEntry("state", "REGISTERED")
                    .containsEntry("dataset_input_verified", "true")
                    .containsEntry("lineage_verified", "true")
                    .containsEntry("model_trained", "false")
                    .containsEntry("model_registered", "false")
                    .containsEntry("online_inference_enabled", "false")
                    .containsEntry("production_activation_allowed", "false");
            assertThat((String) registration.get("mlflow_experiment_id")).isNotBlank();
            assertThat((String) registration.get("mlflow_run_id")).isNotBlank();
            assertThat((String) registration.get("mlflow_artifact_uri")).isNotBlank();
            assertThat((String) registration.get("mlflow_dataset_source"))
                    .startsWith("s3://bpi-dataset-recovery/")
                    .contains("?versionId=");
            mockMvc.perform(get(
                            "/bpi/v1/dataset-mlflow-registrations/{id}", registrationId)
                            .header("Authorization", "Bearer " + viewerToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.state").value("REGISTERED"))
                    .andExpect(jsonPath("$.data.registrationMetadata.datasetInputVerified")
                            .value(true))
                    .andExpect(jsonPath("$.data.registrationMetadata.modelTrained")
                            .value(false))
                    .andExpect(jsonPath("$.data.registrationMetadata.productionActivationAllowed")
                            .value(false));
            assertThat(jdbc.queryForObject("""
                    SELECT count(*) FROM bpi.bpi_audit_events
                     WHERE tenant_id = ? AND object_id = ?
                       AND action IN ('DATASET_MLFLOW_REGISTRATION_QUEUED',
                                      'DATASET_MLFLOW_REGISTRATION_REGISTERING',
                                      'DATASET_MLFLOW_REGISTRATION_REGISTERED')
                    """, Long.class, tenantId, registrationId)).isEqualTo(3L);
            assertThatThrownBy(() -> jdbc.update("""
                    UPDATE bpi.bpi_dataset_mlflow_registrations
                       SET request_reason = 'mutated'
                     WHERE tenant_id = ? AND id = ?
                    """, tenantId, registrationId))
                    .isInstanceOf(DataAccessException.class)
                    .hasMessageContaining("REGISTERED");
            return;
        }

        UUID failedClaim = UUID.randomUUID();
        assertThat(jdbc.update("""
                UPDATE bpi.bpi_dataset_mlflow_registrations
                   SET state = 'REGISTERING', revision = revision + 1,
                       started_at = now(), claim_token = ?, claimed_at = now(),
                       attempt_count = attempt_count + 1
                 WHERE tenant_id = ? AND id = ? AND state = 'QUEUED'
                """, failedClaim, tenantId, registrationId)).isEqualTo(1);
        assertThat(jdbc.update("""
                UPDATE bpi.bpi_dataset_mlflow_registrations
                   SET state = 'FAILED', revision = revision + 1,
                       completed_at = now(), claim_token = NULL, claimed_at = NULL,
                       failure_code = 'MLFLOW_TRANSPORT_ERROR',
                       failure_detail = 'Acceptance-injected MLflow outage'
                 WHERE tenant_id = ? AND id = ? AND state = 'REGISTERING'
                   AND claim_token = ?
                """, tenantId, registrationId, failedClaim)).isEqualTo(1);
        mockMvc.perform(get(
                        "/bpi/v1/dataset-mlflow-registrations/{id}", registrationId)
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("FAILED"))
                .andExpect(jsonPath("$.data.revision").value(3))
                .andExpect(jsonPath("$.data.failureCode").value("MLFLOW_TRANSPORT_ERROR"));

        byte[] retryBody = objectMapper.writeValueAsBytes(Map.of(
                "reason", "Retry after the MLflow tracking service recovers"));
        String retryKey = "mlflow-registration-retry-" + tenantId;
        mockMvc.perform(post(
                        "/bpi/v1/dataset-mlflow-registrations/{id}/retry", registrationId)
                        .header("Authorization", "Bearer " + engineerToken)
                        .header("Idempotency-Key", retryKey)
                        .header("If-Match", 3)
                        .contentType(MediaType.APPLICATION_JSON).content(retryBody))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.state").value("QUEUED"))
                .andExpect(jsonPath("$.data.revision").value(4))
                .andExpect(jsonPath("$.data.attemptCount").value(1));
        mockMvc.perform(post(
                        "/bpi/v1/dataset-mlflow-registrations/{id}/retry", registrationId)
                        .header("Authorization", "Bearer " + engineerToken)
                        .header("Idempotency-Key", retryKey)
                        .header("If-Match", 3)
                        .contentType(MediaType.APPLICATION_JSON).content(retryBody))
                .andExpect(status().isAccepted())
                .andExpect(header().string("Idempotent-Replay", "true"))
                .andExpect(jsonPath("$.data.revision").value(4));

        UUID readyClaim = UUID.randomUUID();
        assertThat(jdbc.update("""
                UPDATE bpi.bpi_dataset_mlflow_registrations
                   SET state = 'REGISTERING', revision = revision + 1,
                       started_at = now(), claim_token = ?, claimed_at = now(),
                       attempt_count = attempt_count + 1
                 WHERE tenant_id = ? AND id = ? AND state = 'QUEUED'
                """, readyClaim, tenantId, registrationId)).isEqualTo(1);
        Map<String, Object> source = jdbc.queryForMap("""
                SELECT archive_bucket, source_archive_object_key, source_archive_version_id
                  FROM bpi.bpi_dataset_mlflow_registrations
                 WHERE tenant_id = ? AND id = ?
                """, tenantId, registrationId);
        String datasetSource = "s3://" + source.get("archive_bucket") + "/"
                + source.get("source_archive_object_key") + "?versionId="
                + source.get("source_archive_version_id");
        String invalidMetadata = "{\"datasetInputVerified\":true,"
                + "\"lineageVerified\":true,\"modelTrained\":false,"
                + "\"modelRegistered\":false,\"onlineInferenceEnabled\":false,"
                + "\"productionActivationAllowed\":true}";
        assertThatThrownBy(() -> jdbc.update("""
                UPDATE bpi.bpi_dataset_mlflow_registrations
                   SET state = 'REGISTERED', revision = revision + 1,
                       completed_at = now(), claim_token = NULL, claimed_at = NULL,
                       mlflow_experiment_id = '31', mlflow_run_id = 'run-invalid',
                       mlflow_artifact_uri = 'mlflow-artifacts:/31/run-invalid/artifacts',
                       mlflow_dataset_source = ?, registration_metadata = CAST(? AS jsonb)
                 WHERE tenant_id = ? AND id = ? AND state = 'REGISTERING'
                   AND claim_token = ?
                """, datasetSource, invalidMetadata, tenantId, registrationId, readyClaim))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("chk_bpi_dataset_mlflow_registration_lifecycle");

        String metadata = "{\"datasetInputVerified\":true,"
                + "\"lineageVerified\":true,\"sourceFactsVerified\":true,"
                + "\"modelTrained\":false,\"modelRegistered\":false,"
                + "\"onlineInferenceEnabled\":false,"
                + "\"productionActivationAllowed\":false}";
        assertThat(jdbc.update("""
                UPDATE bpi.bpi_dataset_mlflow_registrations
                   SET state = 'REGISTERED', revision = revision + 1,
                       completed_at = now(), claim_token = NULL, claimed_at = NULL,
                       mlflow_experiment_id = '31', mlflow_run_id = 'run-acceptance',
                       mlflow_artifact_uri = 'mlflow-artifacts:/31/run-acceptance/artifacts',
                       mlflow_dataset_source = ?, registration_metadata = CAST(? AS jsonb)
                 WHERE tenant_id = ? AND id = ? AND state = 'REGISTERING'
                   AND claim_token = ?
                """, datasetSource, metadata, tenantId, registrationId, readyClaim)).isEqualTo(1);

        mockMvc.perform(get(
                        "/bpi/v1/dataset-retention-archives/{id}/mlflow-registrations",
                        archiveId)
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(registrationId.toString()))
                .andExpect(jsonPath("$.data.state").value("REGISTERED"))
                .andExpect(jsonPath("$.data.revision").value(6))
                .andExpect(jsonPath("$.data.attemptCount").value(2))
                .andExpect(jsonPath("$.data.mlflowExperimentId").value("31"))
                .andExpect(jsonPath("$.data.mlflowRunId").value("run-acceptance"))
                .andExpect(jsonPath("$.data.mlflowDatasetSource").value(datasetSource))
                .andExpect(jsonPath("$.data.registrationMetadata.datasetInputVerified")
                        .value(true))
                .andExpect(jsonPath("$.data.registrationMetadata.lineageVerified").value(true))
                .andExpect(jsonPath("$.data.registrationMetadata.modelTrained").value(false))
                .andExpect(jsonPath("$.data.registrationMetadata.modelRegistered").value(false))
                .andExpect(jsonPath("$.data.registrationMetadata.onlineInferenceEnabled")
                        .value(false))
                .andExpect(jsonPath("$.data.registrationMetadata.productionActivationAllowed")
                        .value(false));
        assertThat(datasetSource).contains("?versionId=");
        mockMvc.perform(post(
                        "/bpi/v1/dataset-mlflow-registrations/{id}/retry", registrationId)
                        .header("Authorization", "Bearer " + engineerToken)
                        .header("Idempotency-Key", "mlflow-registered-retry-" + tenantId)
                        .header("If-Match", 6)
                        .contentType(MediaType.APPLICATION_JSON).content(retryBody))
                .andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM bpi.bpi_audit_events
                 WHERE tenant_id = ? AND object_id = ?
                   AND action IN ('DATASET_MLFLOW_REGISTRATION_QUEUED',
                                  'DATASET_MLFLOW_REGISTRATION_RETRIED')
                """, Long.class, tenantId, registrationId)).isEqualTo(2L);
        assertThatThrownBy(() -> jdbc.update("""
                UPDATE bpi.bpi_dataset_mlflow_registrations
                   SET request_reason = 'mutated'
                 WHERE tenant_id = ? AND id = ?
                """, tenantId, registrationId))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("REGISTERED");
    }

    private Map<String, Object> awaitExternalMaterialization(UUID materializationId)
            throws InterruptedException {
        Instant deadline = Instant.now().plusSeconds(
                Integer.parseInt(env("BPI_TEST_MATERIALIZER_TIMEOUT_SECONDS", "90")));
        while (Instant.now().isBefore(deadline)) {
            Map<String, Object> artifact = jdbc.queryForMap("""
                    SELECT state, artifact_uri, object_key, content_sha256, byte_size, row_count,
                           artifact_metadata ->> 'sourcePayloadIncluded'
                               AS source_payload_included,
                           artifact_metadata ->> 'excludedSamplesIncluded'
                               AS excluded_samples_included,
                           artifact_metadata ->> 'objectVersionId' AS object_version_id,
                           artifact_metadata ->> 'objectContentVerified'
                               AS object_content_verified,
                           artifact_metadata ->> 'icebergReady' AS iceberg_ready,
                           artifact_metadata ->> 'mlflowRegistered' AS mlflow_registered,
                           artifact_metadata ->> 'modelTrained' AS model_trained,
                           failure_code, failure_detail
                      FROM bpi.bpi_dataset_materializations
                     WHERE tenant_id = ? AND id = ?
                    """, tenantId, materializationId);
            String state = (String) artifact.get("state");
            if ("READY".equals(state)) return artifact;
            if ("FAILED".equals(state)) {
                throw new AssertionError("External materializer failed: "
                        + artifact.get("failure_code") + " " + artifact.get("failure_detail"));
            }
            Thread.sleep(250);
        }
        throw new AssertionError("External materializer did not reach READY before timeout");
    }

    private Map<String, Object> awaitExternalRetentionArchive(UUID archiveId)
            throws InterruptedException {
        Instant deadline = Instant.now().plusSeconds(
                Integer.parseInt(env("BPI_TEST_RETENTION_ARCHIVER_TIMEOUT_SECONDS", "90")));
        while (Instant.now().isBefore(deadline)) {
            Map<String, Object> archive = jdbc.queryForMap("""
                    SELECT state, revision, attempt_count, retention_mode, retain_until,
                           legal_hold_enabled, archive_bucket, archive_prefix,
                           source_archive_version_id, archive_manifest_version_id,
                           archive_manifest_sha256, archive_object_count, archive_total_bytes,
                           verified_row_count, verified_semantic_checksum,
                           archive_metadata ->> 'objectLockVerified' AS object_lock_verified,
                           archive_metadata ->> 'recoveryVerified' AS recovery_verified,
                           archive_metadata ->> 'mlflowRegistered' AS mlflow_registered,
                           archive_metadata ->> 'modelTrained' AS model_trained,
                           failure_code, failure_detail
                      FROM bpi.bpi_dataset_retention_archives
                     WHERE tenant_id = ? AND id = ?
                    """, tenantId, archiveId);
            String state = (String) archive.get("state");
            if ("LOCKED".equals(state)) return archive;
            if ("FAILED".equals(state)) {
                throw new AssertionError("External retention archiver failed: "
                        + archive.get("failure_code") + " " + archive.get("failure_detail"));
            }
            Thread.sleep(250);
        }
        throw new AssertionError("Timed out waiting for external retention archive " + archiveId);
    }

    private Map<String, Object> awaitExternalMlflowRegistration(UUID registrationId)
            throws InterruptedException {
        Instant deadline = Instant.now().plusSeconds(
                Integer.parseInt(env("BPI_TEST_MLFLOW_REGISTRAR_TIMEOUT_SECONDS", "90")));
        while (Instant.now().isBefore(deadline)) {
            Map<String, Object> registration = jdbc.queryForMap("""
                    SELECT state, revision, attempt_count,
                           mlflow_experiment_id, mlflow_run_id,
                           mlflow_artifact_uri, mlflow_dataset_source,
                           registration_metadata ->> 'datasetInputVerified'
                               AS dataset_input_verified,
                           registration_metadata ->> 'lineageVerified'
                               AS lineage_verified,
                           registration_metadata ->> 'modelTrained' AS model_trained,
                           registration_metadata ->> 'modelRegistered' AS model_registered,
                           registration_metadata ->> 'onlineInferenceEnabled'
                               AS online_inference_enabled,
                           registration_metadata ->> 'productionActivationAllowed'
                               AS production_activation_allowed,
                           failure_code, failure_detail
                      FROM bpi.bpi_dataset_mlflow_registrations
                     WHERE tenant_id = ? AND id = ?
                    """, tenantId, registrationId);
            String state = (String) registration.get("state");
            if ("REGISTERED".equals(state)) return registration;
            if ("FAILED".equals(state)) {
                throw new AssertionError("External MLflow registrar failed: "
                        + registration.get("failure_code") + " "
                        + registration.get("failure_detail"));
            }
            Thread.sleep(250);
        }
        throw new AssertionError(
                "Timed out waiting for external MLflow registration " + registrationId);
    }

    private Map<String, Object> awaitExternalCatalogPublication(UUID publicationId)
            throws InterruptedException {
        Instant deadline = Instant.now().plusSeconds(
                Integer.parseInt(env("BPI_TEST_CATALOG_PUBLISHER_TIMEOUT_SECONDS", "120")));
        while (Instant.now().isBefore(deadline)) {
            Map<String, Object> publication = jdbc.queryForMap("""
                    SELECT state, table_identifier, iceberg_snapshot_id::text,
                           iceberg_metadata_location, iceberg_schema_id,
                           iceberg_partition_spec_id, verified_row_count,
                           semantic_checksum,
                           catalog_metadata ->> 'catalogSnapshotVerified'
                               AS catalog_snapshot_verified,
                           catalog_metadata ->> 'sourceVersionVerified'
                               AS source_version_verified,
                           catalog_metadata ->> 'manifestChecksumVerified'
                               AS manifest_checksum_verified,
                           catalog_metadata ->> 'icebergReady' AS iceberg_ready,
                           catalog_metadata ->> 'mlflowRegistered' AS mlflow_registered,
                           catalog_metadata ->> 'modelTrained' AS model_trained,
                           failure_code, failure_detail
                      FROM bpi.bpi_dataset_catalog_publications
                     WHERE tenant_id = ? AND id = ?
                    """, tenantId, publicationId);
            String state = (String) publication.get("state");
            if ("READY".equals(state)) return publication;
            if ("FAILED".equals(state)) {
                throw new AssertionError("External catalog publisher failed: "
                        + publication.get("failure_code") + " "
                        + publication.get("failure_detail"));
            }
            Thread.sleep(250);
        }
        throw new AssertionError("External catalog publisher did not reach READY before timeout");
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
