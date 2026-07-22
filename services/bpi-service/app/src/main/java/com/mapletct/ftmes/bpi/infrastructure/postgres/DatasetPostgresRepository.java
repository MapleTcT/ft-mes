package com.mapletct.ftmes.bpi.infrastructure.postgres;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mapletct.ftmes.bpi.application.ActorContext;
import com.mapletct.ftmes.bpi.application.error.BpiConflictException;
import com.mapletct.ftmes.bpi.application.error.BpiNotFoundException;
import com.mapletct.ftmes.bpi.domain.DatasetDefinitionView;
import com.mapletct.ftmes.bpi.domain.DatasetManifestBuild;
import com.mapletct.ftmes.bpi.domain.DatasetManifestClaim;
import com.mapletct.ftmes.bpi.domain.DatasetManifestSample;
import com.mapletct.ftmes.bpi.domain.DatasetMaterializationView;
import com.mapletct.ftmes.bpi.domain.DatasetSampleSource;
import com.mapletct.ftmes.bpi.domain.DatasetSnapshotSummary;
import com.mapletct.ftmes.bpi.domain.DatasetSnapshotView;
import com.mapletct.ftmes.bpi.interfaces.rest.DatasetDefinitionCreateCommand;
import com.mapletct.ftmes.bpi.interfaces.rest.DatasetSnapshotCommand;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Repository
public class DatasetPostgresRepository {
    private static final String DEFINITION_SELECT = """
            SELECT d.*,
                   latest.id AS latest_id,
                   latest.snapshot_version AS latest_snapshot_version,
                   latest.state AS latest_state,
                   latest.revision AS latest_revision,
                   latest.freeze_at AS latest_freeze_at,
                   latest.manifest_checksum AS latest_manifest_checksum,
                   latest.included_count AS latest_included_count,
                   latest.excluded_count AS latest_excluded_count,
                   COALESCE(materialization.state, latest.materialization_state)
                       AS latest_materialization_state,
                   latest.created_at AS latest_created_at,
                   latest.completed_at AS latest_completed_at,
                   latest.failure_code AS latest_failure_code,
                   latest.failure_detail AS latest_failure_detail
              FROM bpi.bpi_dataset_definitions d
              LEFT JOIN LATERAL (
                    SELECT snapshot.*
                      FROM bpi.bpi_dataset_snapshots snapshot
                     WHERE snapshot.tenant_id = d.tenant_id
                       AND snapshot.dataset_id = d.id
                     ORDER BY snapshot.snapshot_version DESC, snapshot.id DESC
                     LIMIT 1
              ) latest ON true
              LEFT JOIN LATERAL (
                    SELECT materialization.state
                      FROM bpi.bpi_dataset_materializations materialization
                     WHERE materialization.tenant_id = latest.tenant_id
                       AND materialization.snapshot_id = latest.id
                     ORDER BY materialization.created_at DESC, materialization.id DESC
                     LIMIT 1
              ) materialization ON true
            """;

    private static final String SNAPSHOT_SELECT = """
            SELECT snapshot.*, definition.dataset_code, definition.version AS dataset_version,
                   definition.name AS dataset_name, definition.plant_id,
                   COALESCE(materialization.state, snapshot.materialization_state)
                       AS effective_materialization_state,
                   COALESCE(materialization.artifact_uri, snapshot.artifact_uri)
                       AS effective_artifact_uri,
                   materialization.id AS materialization_id,
                   materialization.snapshot_id AS materialization_snapshot_id,
                   materialization.artifact_format AS materialization_artifact_format,
                   materialization.artifact_schema_version AS materialization_artifact_schema_version,
                   materialization.materializer_version AS materialization_materializer_version,
                   materialization.state AS materialization_state_value,
                   materialization.revision AS materialization_revision,
                   materialization.manifest_checksum AS materialization_manifest_checksum,
                   materialization.requested_by AS materialization_requested_by,
                   materialization.request_reason AS materialization_request_reason,
                   materialization.created_at AS materialization_created_at,
                   materialization.started_at AS materialization_started_at,
                   materialization.completed_at AS materialization_completed_at,
                   materialization.attempt_count AS materialization_attempt_count,
                   materialization.artifact_uri AS materialization_artifact_uri,
                   materialization.object_bucket AS materialization_object_bucket,
                   materialization.object_key AS materialization_object_key,
                   materialization.content_sha256 AS materialization_content_sha256,
                   materialization.byte_size AS materialization_byte_size,
                   materialization.row_count AS materialization_row_count,
                   materialization.schema_json::text AS materialization_schema_json,
                   materialization.artifact_metadata::text AS materialization_artifact_metadata,
                   materialization.failure_code AS materialization_failure_code,
                   materialization.failure_detail AS materialization_failure_detail
              FROM bpi.bpi_dataset_snapshots snapshot
              JOIN bpi.bpi_dataset_definitions definition
                ON definition.tenant_id = snapshot.tenant_id
               AND definition.id = snapshot.dataset_id
              LEFT JOIN LATERAL (
                    SELECT candidate.*
                      FROM bpi.bpi_dataset_materializations candidate
                     WHERE candidate.tenant_id = snapshot.tenant_id
                       AND candidate.snapshot_id = snapshot.id
                     ORDER BY candidate.created_at DESC, candidate.id DESC
                     LIMIT 1
              ) materialization ON true
            """;

    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public DatasetPostgresRepository(
            NamedParameterJdbcTemplate jdbc,
            ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    public List<DatasetDefinitionView> list(ActorContext actor, String plantId, int limit) {
        StringBuilder sql = new StringBuilder(DEFINITION_SELECT)
                .append(" WHERE d.tenant_id = :tenantId AND d.plant_id = :plantId");
        MapSqlParameterSource parameters = scoped(actor, sql)
                .addValue("tenantId", actor.tenantId())
                .addValue("plantId", plantId)
                .addValue("limit", limit);
        sql.append(" ORDER BY d.dataset_code, d.version, d.created_at DESC, d.id DESC LIMIT :limit");
        return jdbc.query(sql.toString(), parameters, (rs, rowNum) -> mapDefinition(rs));
    }

    public DatasetDefinitionView findDefinition(ActorContext actor, UUID datasetId) {
        StringBuilder sql = new StringBuilder(DEFINITION_SELECT)
                .append(" WHERE d.tenant_id = :tenantId AND d.id = :datasetId");
        MapSqlParameterSource parameters = scoped(actor, sql)
                .addValue("tenantId", actor.tenantId())
                .addValue("datasetId", datasetId);
        try {
            DatasetDefinitionView value = jdbc.queryForObject(
                    sql.toString(), parameters, (rs, rowNum) -> mapDefinition(rs));
            if (value == null) throw new BpiNotFoundException("Dataset definition not found.");
            return value;
        } catch (EmptyResultDataAccessException exception) {
            throw new BpiNotFoundException("Dataset definition not found.");
        }
    }

    public void insertDefinition(
            ActorContext actor,
            UUID id,
            DatasetDefinitionCreateCommand command,
            List<String> lineIds,
            List<String> featureRefs,
            List<String> labelRefs,
            String checksum) {
        try {
            jdbc.update("""
                    INSERT INTO bpi.bpi_dataset_definitions
                        (id, tenant_id, dataset_code, version, name, plant_id, line_ids,
                         prediction_time_policy, feature_cutoff_policy, feature_refs, label_refs,
                         max_label_delay_hours, minimum_confidence, split_policy, checksum,
                         created_by, create_reason)
                    VALUES (:id, :tenantId, :datasetCode, :version, :name, :plantId,
                            CAST(:lineIds AS jsonb), :predictionTimePolicy, :featureCutoffPolicy,
                            CAST(:featureRefs AS jsonb), CAST(:labelRefs AS jsonb),
                            :maxLabelDelayHours, :minimumConfidence, :splitPolicy, :checksum,
                            :createdBy, :reason)
                    """, new MapSqlParameterSource()
                    .addValue("id", id)
                    .addValue("tenantId", actor.tenantId())
                    .addValue("datasetCode", command.datasetCode())
                    .addValue("version", command.version())
                    .addValue("name", command.name())
                    .addValue("plantId", command.plantId())
                    .addValue("lineIds", writeJson(lineIds))
                    .addValue("predictionTimePolicy", command.predictionTimePolicy())
                    .addValue("featureCutoffPolicy", command.featureCutoffPolicy())
                    .addValue("featureRefs", writeJson(featureRefs))
                    .addValue("labelRefs", writeJson(labelRefs))
                    .addValue("maxLabelDelayHours", command.maxLabelDelayHours())
                    .addValue("minimumConfidence", command.minimumConfidence())
                    .addValue("splitPolicy", command.splitPolicy())
                    .addValue("checksum", checksum)
                    .addValue("createdBy", actor.userId())
                    .addValue("reason", command.reason()));
        } catch (DataIntegrityViolationException exception) {
            throw new BpiConflictException(
                    "Dataset code and version already exist or violate the immutable definition contract.", null);
        }
    }

    @Transactional
    public long nextSnapshotVersion(ActorContext actor, UUID datasetId, long expectedRevision) {
        DatasetDefinitionView definition = lockDefinition(actor, datasetId);
        if (definition.revision() != expectedRevision) {
            throw new BpiConflictException("Dataset definition revision is stale.", definition.revision());
        }
        Long value = jdbc.queryForObject("""
                SELECT COALESCE(MAX(snapshot_version), 0) + 1
                  FROM bpi.bpi_dataset_snapshots
                 WHERE tenant_id = :tenantId AND dataset_id = :datasetId
                """, new MapSqlParameterSource()
                .addValue("tenantId", actor.tenantId())
                .addValue("datasetId", datasetId), Long.class);
        return value == null ? 1 : value;
    }

    public void insertSnapshot(
            ActorContext actor,
            UUID snapshotId,
            DatasetDefinitionView definition,
            long snapshotVersion,
            DatasetSnapshotCommand command,
            List<String> lineIds,
            List<UUID> ruleVersionIds) {
        jdbc.update("""
                INSERT INTO bpi.bpi_dataset_snapshots
                    (id, tenant_id, dataset_id, snapshot_version, state, freeze_at, line_ids,
                     prediction_time_policy, rule_version_ids, exclude_low_confidence,
                     definition_checksum, requested_by, request_reason)
                VALUES (:id, :tenantId, :datasetId, :snapshotVersion, 'QUEUED', :freezeAt,
                        CAST(:lineIds AS jsonb), :predictionTimePolicy,
                        CAST(:ruleVersionIds AS jsonb), :excludeLowConfidence,
                        :definitionChecksum, :requestedBy, :requestReason)
                """, new MapSqlParameterSource()
                .addValue("id", snapshotId)
                .addValue("tenantId", actor.tenantId())
                .addValue("datasetId", definition.id())
                .addValue("snapshotVersion", snapshotVersion)
                .addValue("freezeAt", Timestamp.from(command.freezeAt()))
                .addValue("lineIds", writeJson(lineIds))
                .addValue("predictionTimePolicy", command.predictionTimePolicy())
                .addValue("ruleVersionIds", writeJson(ruleVersionIds))
                .addValue("excludeLowConfidence", command.effectiveExcludeLowConfidence())
                .addValue("definitionChecksum", definition.checksum())
                .addValue("requestedBy", actor.userId())
                .addValue("requestReason", command.reason()));
    }

    public Set<String> eligibleLines(
            String tenantId,
            UUID datasetId,
            Instant freezeAt,
            List<String> lineIds,
            List<UUID> ruleVersionIds) {
        StringBuilder sql = new StringBuilder("""
                SELECT DISTINCT run.line_id
                  FROM bpi.bpi_shadow_runs run
                  JOIN bpi.bpi_shadow_run_batch_reviews review
                    ON review.tenant_id = run.tenant_id AND review.shadow_run_id = run.id
                  JOIN bpi.bpi_batch_instances batch
                    ON batch.tenant_id = review.tenant_id AND batch.id = review.batch_id
                   AND batch.plant_id = run.plant_id AND batch.line_id = run.line_id
                 WHERE run.tenant_id = :tenantId
                   AND run.state = 'APPROVED'
                   AND run.decided_at <= :freezeAt
                   AND run.line_id IN (:lineIds)
                   AND review.reviewed_at <= :freezeAt
                   AND (review.state = 'ACTIVE' OR review.superseded_at > :freezeAt)
                """);
        MapSqlParameterSource parameters = selectionParameters(
                tenantId, datasetId, freezeAt, lineIds, ruleVersionIds, sql);
        return new LinkedHashSet<>(jdbc.queryForList(sql.toString(), parameters, String.class));
    }

    public Set<UUID> eligibleRuleVersions(
            String tenantId,
            UUID datasetId,
            Instant freezeAt,
            List<String> lineIds,
            List<UUID> ruleVersionIds) {
        StringBuilder sql = new StringBuilder("""
                SELECT DISTINCT run.rule_version_id
                  FROM bpi.bpi_shadow_runs run
                  JOIN bpi.bpi_shadow_run_batch_reviews review
                    ON review.tenant_id = run.tenant_id AND review.shadow_run_id = run.id
                  JOIN bpi.bpi_batch_instances batch
                    ON batch.tenant_id = review.tenant_id AND batch.id = review.batch_id
                   AND batch.plant_id = run.plant_id AND batch.line_id = run.line_id
                 WHERE run.tenant_id = :tenantId
                   AND run.state = 'APPROVED'
                   AND run.decided_at <= :freezeAt
                   AND run.line_id IN (:lineIds)
                   AND review.reviewed_at <= :freezeAt
                   AND (review.state = 'ACTIVE' OR review.superseded_at > :freezeAt)
                """);
        MapSqlParameterSource parameters = selectionParameters(
                tenantId, datasetId, freezeAt, lineIds, ruleVersionIds, sql);
        return new LinkedHashSet<>(jdbc.query(
                sql.toString(), parameters, (rs, rowNum) -> rs.getObject(1, UUID.class)));
    }

    @Transactional
    public DatasetManifestClaim claimPending(Duration claimTimeout, int maxAttempts) {
        jdbc.update("""
                UPDATE bpi.bpi_dataset_snapshots
                   SET state = 'FAILED', revision = revision + 1,
                       completed_at = now(), claim_token = NULL, claimed_at = NULL,
                       failure_code = 'WORKER_CLAIM_EXHAUSTED',
                       failure_detail = 'Manifest worker claim expired too many times'
                 WHERE state = 'BUILDING'
                   AND claimed_at < now() - (:claimTimeoutMs * interval '1 millisecond')
                   AND attempt_count >= :maxAttempts
                """, new MapSqlParameterSource()
                .addValue("claimTimeoutMs", claimTimeout.toMillis())
                .addValue("maxAttempts", maxAttempts));
        jdbc.update("""
                UPDATE bpi.bpi_dataset_snapshots
                   SET state = 'QUEUED', revision = revision + 1,
                       started_at = NULL, claim_token = NULL, claimed_at = NULL
                 WHERE state = 'BUILDING'
                   AND claimed_at < now() - (:claimTimeoutMs * interval '1 millisecond')
                   AND attempt_count < :maxAttempts
                """, new MapSqlParameterSource()
                .addValue("claimTimeoutMs", claimTimeout.toMillis())
                .addValue("maxAttempts", maxAttempts));

        UUID claimToken = UUID.randomUUID();
        List<UUID> claimed = jdbc.query("""
                WITH selected AS (
                    SELECT id
                      FROM bpi.bpi_dataset_snapshots
                     WHERE state = 'QUEUED'
                     ORDER BY created_at, id
                     FOR UPDATE SKIP LOCKED
                     LIMIT 1
                )
                UPDATE bpi.bpi_dataset_snapshots snapshot
                   SET state = 'BUILDING', revision = revision + 1,
                       started_at = now(), claim_token = :claimToken, claimed_at = now(),
                       attempt_count = attempt_count + 1
                  FROM selected
                 WHERE snapshot.id = selected.id
                RETURNING snapshot.id
                """, new MapSqlParameterSource().addValue("claimToken", claimToken),
                (rs, rowNum) -> rs.getObject(1, UUID.class));
        if (claimed.isEmpty()) return null;
        return findClaim(claimed.get(0), claimToken);
    }

    public List<DatasetSampleSource> findSampleSources(
            DatasetManifestClaim claim,
            int limit) {
        StringBuilder sql = new StringBuilder("""
                SELECT review.id AS review_id, review.shadow_run_id, review.batch_id,
                       batch.batch_no, batch.plant_id, batch.line_id, batch.stage_code,
                       batch.order_id, batch.material_code, batch.rule_version_id,
                       batch.topology_version_id, run.point_catalog_snapshot_id,
                       review.automatic_start_time, review.automatic_end_time,
                       review.manual_start_time, review.manual_end_time,
                       review.automatic_quantity, review.reference_quantity,
                       review.quantity_unit, review.start_boundary_accepted,
                       review.end_boundary_accepted, review.quantity_within_tolerance,
                       review.reviewed_at
                  FROM bpi.bpi_shadow_runs run
                  JOIN bpi.bpi_shadow_run_batch_reviews review
                    ON review.tenant_id = run.tenant_id AND review.shadow_run_id = run.id
                  JOIN bpi.bpi_batch_instances batch
                    ON batch.tenant_id = review.tenant_id AND batch.id = review.batch_id
                   AND batch.plant_id = run.plant_id AND batch.line_id = run.line_id
                 WHERE run.tenant_id = :tenantId
                   AND run.state = 'APPROVED'
                   AND run.decided_at <= :freezeAt
                   AND run.line_id IN (:lineIds)
                   AND review.reviewed_at <= :freezeAt
                   AND (review.state = 'ACTIVE' OR review.superseded_at > :freezeAt)
                """);
        MapSqlParameterSource parameters = selectionParameters(
                claim.tenantId(), claim.datasetId(), claim.freezeAt(), claim.lineIds(),
                claim.ruleVersionIds(), sql).addValue("limit", limit);
        sql.append(" ORDER BY run.line_id, review.automatic_start_time, review.batch_id, review.id LIMIT :limit");
        return jdbc.query(sql.toString(), parameters, (rs, rowNum) -> mapSource(rs));
    }

    @Transactional
    public void completeManifest(
            DatasetManifestClaim claim,
            DatasetManifestBuild build) {
        SqlParameterSource[] samples = build.samples().stream()
                .map(sample -> sampleParameters(claim, sample))
                .toArray(SqlParameterSource[]::new);
        if (samples.length > 0) {
            jdbc.batchUpdate("""
                    INSERT INTO bpi.bpi_dataset_snapshot_samples
                        (snapshot_id, review_id, tenant_id, shadow_run_id, batch_id, batch_no,
                         line_id, included, exclusion_reasons, prediction_time, feature_cutoff,
                         label_available_at, confidence, split_key, feature_payload,
                         label_payload, source_payload)
                    VALUES (:snapshotId, :reviewId, :tenantId, :shadowRunId, :batchId, :batchNo,
                            :lineId, :included, CAST(:exclusionReasons AS jsonb), :predictionTime,
                            :featureCutoff, :labelAvailableAt, :confidence, :splitKey,
                            CAST(:featurePayload AS jsonb), CAST(:labelPayload AS jsonb),
                            CAST(:sourcePayload AS jsonb))
                    """, samples);
        }
        int updated = jdbc.update("""
                UPDATE bpi.bpi_dataset_snapshots
                   SET state = 'MANIFEST_READY', revision = revision + 1,
                       manifest_checksum = :manifestChecksum,
                       manifest = CAST(:manifest AS jsonb),
                       included_count = :includedCount, excluded_count = :excludedCount,
                       exclusion_summary = CAST(:exclusionSummary AS jsonb),
                       completed_at = now(), claim_token = NULL, claimed_at = NULL
                 WHERE tenant_id = :tenantId AND id = :snapshotId
                   AND state = 'BUILDING' AND claim_token = :claimToken
                """, new MapSqlParameterSource()
                .addValue("manifestChecksum", build.manifestChecksum())
                .addValue("manifest", writeJson(build.manifest()))
                .addValue("includedCount", build.includedCount())
                .addValue("excludedCount", build.excludedCount())
                .addValue("exclusionSummary", writeJson(build.exclusionSummary()))
                .addValue("tenantId", claim.tenantId())
                .addValue("snapshotId", claim.snapshotId())
                .addValue("claimToken", claim.claimToken()));
        if (updated != 1) {
            throw new BpiConflictException("Dataset manifest worker lost its snapshot claim.", null);
        }
        insertWorkerAudit(claim, build);
    }

    public void failManifest(DatasetManifestClaim claim, String code, String detail) {
        jdbc.update("""
                UPDATE bpi.bpi_dataset_snapshots
                   SET state = 'FAILED', revision = revision + 1,
                       completed_at = now(), claim_token = NULL, claimed_at = NULL,
                       failure_code = :failureCode, failure_detail = :failureDetail
                 WHERE tenant_id = :tenantId AND id = :snapshotId
                   AND state = 'BUILDING' AND claim_token = :claimToken
                """, new MapSqlParameterSource()
                .addValue("failureCode", code)
                .addValue("failureDetail", truncate(detail))
                .addValue("tenantId", claim.tenantId())
                .addValue("snapshotId", claim.snapshotId())
                .addValue("claimToken", claim.claimToken()));
    }

    public DatasetSnapshotView findSnapshot(ActorContext actor, UUID snapshotId) {
        StringBuilder sql = new StringBuilder(SNAPSHOT_SELECT)
                .append(" WHERE snapshot.tenant_id = :tenantId AND snapshot.id = :snapshotId");
        MapSqlParameterSource parameters = scopedDefinition(actor, sql)
                .addValue("tenantId", actor.tenantId())
                .addValue("snapshotId", snapshotId);
        try {
            DatasetSnapshotView value = jdbc.queryForObject(
                    sql.toString(), parameters, (rs, rowNum) -> mapSnapshot(rs));
            if (value == null) throw new BpiNotFoundException("Dataset snapshot not found.");
            return value;
        } catch (EmptyResultDataAccessException exception) {
            throw new BpiNotFoundException("Dataset snapshot not found.");
        }
    }

    public void insertAudit(
            ActorContext actor,
            String plantId,
            String lineId,
            String objectType,
            UUID objectId,
            String action,
            Long beforeRevision,
            Long afterRevision,
            String reason,
            String traceId,
            Map<String, Object> detail) {
        jdbc.update("""
                INSERT INTO bpi.bpi_audit_events
                    (id, tenant_id, plant_id, line_id, object_type, object_id, action,
                     actor_id, before_revision, after_revision, reason, trace_id, detail)
                VALUES (:id, :tenantId, :plantId, :lineId, :objectType, :objectId, :action,
                        :actorId, :beforeRevision, :afterRevision, :reason, :traceId,
                        CAST(:detail AS jsonb))
                """, new MapSqlParameterSource()
                .addValue("id", UUID.randomUUID())
                .addValue("tenantId", actor.tenantId())
                .addValue("plantId", plantId)
                .addValue("lineId", lineId)
                .addValue("objectType", objectType)
                .addValue("objectId", objectId)
                .addValue("action", action)
                .addValue("actorId", actor.userId())
                .addValue("beforeRevision", beforeRevision)
                .addValue("afterRevision", afterRevision)
                .addValue("reason", reason)
                .addValue("traceId", traceId)
                .addValue("detail", writeJson(detail)));
    }

    private DatasetDefinitionView lockDefinition(ActorContext actor, UUID datasetId) {
        StringBuilder sql = new StringBuilder(DEFINITION_SELECT)
                .append(" WHERE d.tenant_id = :tenantId AND d.id = :datasetId");
        MapSqlParameterSource parameters = scoped(actor, sql)
                .addValue("tenantId", actor.tenantId())
                .addValue("datasetId", datasetId);
        sql.append(" FOR UPDATE OF d");
        try {
            DatasetDefinitionView value = jdbc.queryForObject(
                    sql.toString(), parameters, (rs, rowNum) -> mapDefinition(rs));
            if (value == null) throw new BpiNotFoundException("Dataset definition not found.");
            return value;
        } catch (EmptyResultDataAccessException exception) {
            throw new BpiNotFoundException("Dataset definition not found.");
        }
    }

    private DatasetManifestClaim findClaim(UUID snapshotId, UUID claimToken) {
        return jdbc.queryForObject("""
                SELECT snapshot.id, snapshot.claim_token, snapshot.attempt_count,
                       snapshot.tenant_id, snapshot.dataset_id, snapshot.freeze_at,
                       snapshot.line_ids::text AS snapshot_line_ids,
                       snapshot.rule_version_ids::text AS rule_version_ids,
                       snapshot.exclude_low_confidence, snapshot.definition_checksum,
                       definition.dataset_code, definition.version, definition.name,
                       definition.plant_id, definition.prediction_time_policy,
                       definition.feature_cutoff_policy,
                       definition.feature_refs::text AS feature_refs,
                       definition.label_refs::text AS label_refs,
                       definition.max_label_delay_hours, definition.minimum_confidence,
                       definition.split_policy
                  FROM bpi.bpi_dataset_snapshots snapshot
                  JOIN bpi.bpi_dataset_definitions definition
                    ON definition.tenant_id = snapshot.tenant_id
                   AND definition.id = snapshot.dataset_id
                 WHERE snapshot.id = :snapshotId AND snapshot.claim_token = :claimToken
                """, new MapSqlParameterSource()
                .addValue("snapshotId", snapshotId)
                .addValue("claimToken", claimToken),
                (rs, rowNum) -> new DatasetManifestClaim(
                        rs.getObject("id", UUID.class), rs.getObject("claim_token", UUID.class),
                        rs.getInt("attempt_count"), rs.getString("tenant_id"),
                        rs.getObject("dataset_id", UUID.class), rs.getString("dataset_code"),
                        rs.getString("version"), rs.getString("name"), rs.getString("plant_id"),
                        instant(rs, "freeze_at"), readStrings(rs.getString("snapshot_line_ids")),
                        readUuids(rs.getString("rule_version_ids")),
                        rs.getBoolean("exclude_low_confidence"), rs.getString("definition_checksum"),
                        rs.getString("prediction_time_policy"), rs.getString("feature_cutoff_policy"),
                        readStrings(rs.getString("feature_refs")), readStrings(rs.getString("label_refs")),
                        rs.getInt("max_label_delay_hours"), rs.getBigDecimal("minimum_confidence"),
                        rs.getString("split_policy")));
    }

    private DatasetDefinitionView mapDefinition(ResultSet rs) throws SQLException {
        UUID latestId = rs.getObject("latest_id", UUID.class);
        DatasetSnapshotSummary latest = latestId == null ? null : new DatasetSnapshotSummary(
                latestId, rs.getLong("latest_snapshot_version"), rs.getString("latest_state"),
                rs.getLong("latest_revision"), instant(rs, "latest_freeze_at"),
                rs.getString("latest_manifest_checksum"),
                rs.getObject("latest_included_count", Integer.class),
                rs.getObject("latest_excluded_count", Integer.class),
                rs.getString("latest_materialization_state"), instant(rs, "latest_created_at"),
                instant(rs, "latest_completed_at"), rs.getString("latest_failure_code"),
                rs.getString("latest_failure_detail"));
        return new DatasetDefinitionView(
                rs.getObject("id", UUID.class), rs.getString("dataset_code"),
                rs.getString("version"), rs.getString("name"), rs.getString("tenant_id"),
                rs.getString("plant_id"), readStrings(rs.getString("line_ids")),
                rs.getString("state"), rs.getLong("revision"),
                rs.getString("prediction_time_policy"), rs.getString("feature_cutoff_policy"),
                readStrings(rs.getString("feature_refs")), readStrings(rs.getString("label_refs")),
                rs.getInt("max_label_delay_hours"), rs.getBigDecimal("minimum_confidence"),
                rs.getString("split_policy"), rs.getString("checksum"), rs.getString("created_by"),
                rs.getString("create_reason"), instant(rs, "created_at"), latest);
    }

    private DatasetSnapshotView mapSnapshot(ResultSet rs) throws SQLException {
        UUID materializationId = rs.getObject("materialization_id", UUID.class);
        DatasetMaterializationView materialization = materializationId == null ? null
                : new DatasetMaterializationView(
                        materializationId,
                        rs.getObject("materialization_snapshot_id", UUID.class),
                        rs.getObject("dataset_id", UUID.class),
                        rs.getString("dataset_code"),
                        rs.getString("dataset_version"),
                        rs.getString("tenant_id"),
                        rs.getString("plant_id"),
                        readStrings(rs.getString("line_ids")),
                        rs.getString("materialization_artifact_format"),
                        rs.getString("materialization_artifact_schema_version"),
                        rs.getString("materialization_materializer_version"),
                        rs.getString("materialization_state_value"),
                        rs.getLong("materialization_revision"),
                        rs.getString("materialization_manifest_checksum"),
                        rs.getString("materialization_requested_by"),
                        rs.getString("materialization_request_reason"),
                        instant(rs, "materialization_created_at"),
                        instant(rs, "materialization_started_at"),
                        instant(rs, "materialization_completed_at"),
                        rs.getInt("materialization_attempt_count"),
                        rs.getString("materialization_artifact_uri"),
                        rs.getString("materialization_object_bucket"),
                        rs.getString("materialization_object_key"),
                        rs.getString("materialization_content_sha256"),
                        rs.getObject("materialization_byte_size", Long.class),
                        rs.getObject("materialization_row_count", Long.class),
                        readMap(rs.getString("materialization_schema_json")),
                        readMap(rs.getString("materialization_artifact_metadata")),
                        rs.getString("materialization_failure_code"),
                        rs.getString("materialization_failure_detail"));
        return new DatasetSnapshotView(
                rs.getObject("id", UUID.class), rs.getObject("dataset_id", UUID.class),
                rs.getString("dataset_code"), rs.getString("dataset_version"),
                rs.getString("dataset_name"), rs.getString("tenant_id"), rs.getString("plant_id"),
                rs.getLong("snapshot_version"), rs.getString("state"), rs.getLong("revision"),
                instant(rs, "freeze_at"), readStrings(rs.getString("line_ids")),
                rs.getString("prediction_time_policy"), readUuids(rs.getString("rule_version_ids")),
                rs.getBoolean("exclude_low_confidence"), rs.getString("definition_checksum"),
                rs.getString("manifest_schema_version"), rs.getString("manifest_checksum"),
                readMap(rs.getString("manifest")),
                rs.getObject("included_count", Integer.class),
                rs.getObject("excluded_count", Integer.class),
                readIntegerMap(rs.getString("exclusion_summary")),
                rs.getString("effective_materialization_state"),
                rs.getString("effective_artifact_uri"),
                rs.getString("requested_by"), rs.getString("request_reason"),
                instant(rs, "created_at"), instant(rs, "started_at"), instant(rs, "completed_at"),
                rs.getInt("attempt_count"), rs.getString("failure_code"),
                rs.getString("failure_detail"), materialization);
    }

    private DatasetSampleSource mapSource(ResultSet rs) throws SQLException {
        return new DatasetSampleSource(
                rs.getObject("review_id", UUID.class), rs.getObject("shadow_run_id", UUID.class),
                rs.getObject("batch_id", UUID.class), rs.getString("batch_no"),
                rs.getString("plant_id"), rs.getString("line_id"), rs.getString("stage_code"),
                rs.getString("order_id"), rs.getString("material_code"),
                rs.getObject("rule_version_id", UUID.class),
                rs.getObject("topology_version_id", UUID.class),
                rs.getObject("point_catalog_snapshot_id", UUID.class),
                instant(rs, "automatic_start_time"), instant(rs, "automatic_end_time"),
                instant(rs, "manual_start_time"), instant(rs, "manual_end_time"),
                rs.getBigDecimal("automatic_quantity"), rs.getBigDecimal("reference_quantity"),
                rs.getString("quantity_unit"), rs.getBoolean("start_boundary_accepted"),
                rs.getBoolean("end_boundary_accepted"), rs.getBoolean("quantity_within_tolerance"),
                instant(rs, "reviewed_at"));
    }

    private MapSqlParameterSource sampleParameters(
            DatasetManifestClaim claim,
            DatasetManifestSample sample) {
        return new MapSqlParameterSource()
                .addValue("snapshotId", claim.snapshotId())
                .addValue("reviewId", sample.reviewId())
                .addValue("tenantId", claim.tenantId())
                .addValue("shadowRunId", sample.shadowRunId())
                .addValue("batchId", sample.batchId())
                .addValue("batchNo", sample.batchNo())
                .addValue("lineId", sample.lineId())
                .addValue("included", sample.included())
                .addValue("exclusionReasons", writeJson(sample.exclusionReasons()))
                .addValue("predictionTime", Timestamp.from(sample.predictionTime()))
                .addValue("featureCutoff", Timestamp.from(sample.featureCutoff()))
                .addValue("labelAvailableAt", Timestamp.from(sample.labelAvailableAt()))
                .addValue("confidence", sample.confidence())
                .addValue("splitKey", sample.splitKey())
                .addValue("featurePayload", writeJson(sample.featurePayload()))
                .addValue("labelPayload", writeJson(sample.labelPayload()))
                .addValue("sourcePayload", writeJson(sample.sourcePayload()));
    }

    private MapSqlParameterSource selectionParameters(
            String tenantId,
            UUID datasetId,
            Instant freezeAt,
            List<String> lineIds,
            List<UUID> ruleVersionIds,
            StringBuilder sql) {
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("tenantId", tenantId)
                .addValue("datasetId", datasetId)
                .addValue("freezeAt", Timestamp.from(freezeAt))
                .addValue("lineIds", lineIds);
        sql.append(" AND run.plant_id = ("
                + "SELECT plant_id FROM bpi.bpi_dataset_definitions "
                + "WHERE tenant_id = :tenantId AND id = :datasetId)");
        if (ruleVersionIds != null && !ruleVersionIds.isEmpty()) {
            sql.append(" AND run.rule_version_id IN (:ruleVersionIds)");
            parameters.addValue("ruleVersionIds", ruleVersionIds);
        }
        return parameters;
    }

    private MapSqlParameterSource scoped(ActorContext actor, StringBuilder sql) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        if (!actor.plantIds().contains("*")) {
            sql.append(" AND d.plant_id IN (:allowedPlants)");
            parameters.addValue("allowedPlants", actor.plantIds().isEmpty()
                    ? List.of("__NO_PLANT_SCOPE__") : actor.plantIds());
        }
        if (!actor.lineIds().contains("*")) {
            sql.append(" AND CAST(:allowedLines AS jsonb) @> d.line_ids");
            List<String> lines = actor.lineIds().isEmpty()
                    ? List.of("__NO_LINE_SCOPE__") : actor.lineIds().stream().sorted().toList();
            parameters.addValue("allowedLines", writeJson(lines));
        }
        return parameters;
    }

    private MapSqlParameterSource scopedDefinition(ActorContext actor, StringBuilder sql) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        if (!actor.plantIds().contains("*")) {
            sql.append(" AND definition.plant_id IN (:allowedPlants)");
            parameters.addValue("allowedPlants", actor.plantIds().isEmpty()
                    ? List.of("__NO_PLANT_SCOPE__") : actor.plantIds());
        }
        if (!actor.lineIds().contains("*")) {
            sql.append(" AND CAST(:allowedLines AS jsonb) @> snapshot.line_ids");
            List<String> lines = actor.lineIds().isEmpty()
                    ? List.of("__NO_LINE_SCOPE__") : actor.lineIds().stream().sorted().toList();
            parameters.addValue("allowedLines", writeJson(lines));
        }
        return parameters;
    }

    private void insertWorkerAudit(DatasetManifestClaim claim, DatasetManifestBuild build) {
        jdbc.update("""
                INSERT INTO bpi.bpi_audit_events
                    (id, tenant_id, plant_id, line_id, object_type, object_id, action,
                     actor_id, before_revision, after_revision, reason, trace_id, detail)
                VALUES (:id, :tenantId, :plantId, NULL, 'DATASET_SNAPSHOT', :objectId,
                        'DATASET_MANIFEST_READY', 'dataset-manifest-worker', NULL, NULL,
                        'Deterministic manifest build completed', :traceId, CAST(:detail AS jsonb))
                """, new MapSqlParameterSource()
                .addValue("id", UUID.randomUUID())
                .addValue("tenantId", claim.tenantId())
                .addValue("plantId", claim.plantId())
                .addValue("objectId", claim.snapshotId())
                .addValue("traceId", "dataset-manifest:" + claim.snapshotId())
                .addValue("detail", writeJson(Map.of(
                        "manifestChecksum", build.manifestChecksum(),
                        "includedCount", build.includedCount(),
                        "excludedCount", build.excludedCount(),
                        "materializationState", "NOT_STARTED"))));
    }

    private Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not serialize BPI dataset data", exception);
        }
    }

    private List<String> readStrings(String value) {
        if (value == null) return List.of();
        try {
            return objectMapper.readValue(value, new TypeReference<List<String>>() {});
        } catch (Exception exception) {
            throw new IllegalStateException("Could not read BPI dataset string list", exception);
        }
    }

    private List<UUID> readUuids(String value) {
        if (value == null) return List.of();
        try {
            return objectMapper.readValue(value, new TypeReference<List<UUID>>() {});
        } catch (Exception exception) {
            throw new IllegalStateException("Could not read BPI dataset UUID list", exception);
        }
    }

    private Map<String, Object> readMap(String value) {
        if (value == null) return null;
        try {
            return objectMapper.readValue(value, new TypeReference<Map<String, Object>>() {});
        } catch (Exception exception) {
            throw new IllegalStateException("Could not read BPI dataset manifest", exception);
        }
    }

    private Map<String, Integer> readIntegerMap(String value) {
        if (value == null) return null;
        try {
            return objectMapper.readValue(value, new TypeReference<Map<String, Integer>>() {});
        } catch (Exception exception) {
            throw new IllegalStateException("Could not read BPI dataset exclusion summary", exception);
        }
    }

    private String truncate(String value) {
        String detail = value == null || value.isBlank() ? "Unknown dataset manifest failure" : value;
        return detail.length() <= 1000 ? detail : detail.substring(0, 1000);
    }
}
