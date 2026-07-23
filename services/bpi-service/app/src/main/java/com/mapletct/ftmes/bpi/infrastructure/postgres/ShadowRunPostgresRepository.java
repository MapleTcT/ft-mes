package com.mapletct.ftmes.bpi.infrastructure.postgres;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mapletct.ftmes.bpi.application.ActorContext;
import com.mapletct.ftmes.bpi.application.error.BpiConflictException;
import com.mapletct.ftmes.bpi.application.error.BpiNotFoundException;
import com.mapletct.ftmes.bpi.domain.ShadowRunBatchReviewView;
import com.mapletct.ftmes.bpi.domain.ShadowRunBatchSource;
import com.mapletct.ftmes.bpi.domain.ShadowRunMetrics;
import com.mapletct.ftmes.bpi.domain.ShadowRunReadiness;
import com.mapletct.ftmes.bpi.domain.ShadowRunSourceCoverage;
import com.mapletct.ftmes.bpi.domain.ShadowRunTrainingDataCoverage;
import com.mapletct.ftmes.bpi.domain.ShadowRunView;
import com.mapletct.ftmes.bpi.interfaces.rest.ShadowRunCreateCommand;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
public class ShadowRunPostgresRepository {
    private static final String TRAINING_DATA_COVERAGE_POLICY =
            "bpi-training-data-coverage/batch-start-boundary-v1";
    private static final int REQUIRED_TRAINING_REVIEWED_BATCHES = 200;
    private static final int REQUIRED_TRAINING_PRODUCTION_DAYS = 7;
    private static final int REQUIRED_TRAINING_ACCEPTED_START_LABELS = 100;
    private static final int REQUIRED_TRAINING_REJECTED_START_LABELS = 10;

    private static final String SHADOW_RUN_SELECT = """
            SELECT sr.*,
                   r.rule_code || '@' || r.version AS rule_version_ref,
                   r.state AS rule_state,
                   t.topology_code || '@' || t.version AS topology_version_ref,
                   t.state AS topology_state,
                   t.validated_point_catalog_snapshot_id,
                   t.validated_point_catalog_checksum,
                   snapshot.checksum AS point_catalog_checksum,
                   COALESCE(source_coverage.pinned_point_count, 0) AS pinned_point_count,
                   COALESCE(source_coverage.active_registered_point_count, 0)
                       AS active_registered_point_count,
                   COALESCE(source_coverage.physical_identity_point_count, 0)
                       AS physical_identity_point_count,
                   COALESCE(source_coverage.fresh_sequence_qualified_point_count, 0)
                       AS fresh_sequence_qualified_point_count,
                   COALESCE(source_coverage.approved_calibration_point_count, 0)
                       AS approved_calibration_point_count,
                   COALESCE(source_coverage.ready_point_count, 0) AS ready_point_count,
                   current_snapshot.id AS current_point_catalog_snapshot_id,
                   COALESCE(lifecycle.lifecycle_action, 'NOT_PUBLISHED') AS lifecycle_action,
                   COALESCE(lifecycle.lifecycle_active, false) AS lifecycle_active,
                   COALESCE(lifecycle.status, 'NOT_PUBLISHED') AS publication_status,
                   COALESCE(lifecycle.application_status, 'NOT_PUBLISHED') AS application_status,
                   COALESCE(lifecycle.runtime_readiness_status, 'NOT_PUBLISHED') AS runtime_readiness_status,
                   CASE WHEN sr.started_at IS NULL THEN 0
                        ELSE GREATEST(0, FLOOR(EXTRACT(EPOCH FROM
                             (COALESCE(sr.completed_at, now()) - sr.started_at))))::bigint
                   END AS observed_duration_seconds,
                   COALESCE(metrics.reviewed_batch_count, 0) AS reviewed_batch_count,
                   COALESCE(metrics.distinct_production_day_count, 0)
                       AS distinct_production_day_count,
                   COALESCE(metrics.accepted_start_label_count, 0)
                       AS accepted_start_label_count,
                   COALESCE(metrics.rejected_start_label_count, 0)
                       AS rejected_start_label_count,
                   COALESCE(metrics.accepted_boundary_count, 0) AS accepted_boundary_count,
                   COALESCE(metrics.total_boundary_count, 0) AS total_boundary_count,
                   metrics.boundary_agreement,
                   COALESCE(metrics.quantity_sample_count, 0) AS quantity_sample_count,
                   COALESCE(metrics.automatic_quantity_total, 0) AS automatic_quantity_total,
                   COALESCE(metrics.reference_quantity_total, 0) AS reference_quantity_total,
                   metrics.quantity_unit,
                   metrics.cumulative_quantity_deviation_percent,
                   metrics.mean_quantity_deviation_percent,
                   metrics.maximum_quantity_deviation_percent,
                   COALESCE(quality.unresolved_critical_incident_count, 0)
                       AS unresolved_critical_incident_count
              FROM bpi.bpi_shadow_runs sr
              JOIN bpi.bpi_rule_versions r
                ON r.tenant_id = sr.tenant_id AND r.id = sr.rule_version_id
              JOIN bpi.bpi_topology_versions t
                ON t.tenant_id = sr.tenant_id AND t.id = sr.topology_version_id
              JOIN bpi.bpi_point_catalog_snapshots snapshot
                ON snapshot.tenant_id = sr.tenant_id AND snapshot.id = sr.point_catalog_snapshot_id
              LEFT JOIN LATERAL (
                  SELECT count(*)::integer AS pinned_point_count,
                         count(*) FILTER (WHERE point.active_registered)::integer
                             AS active_registered_point_count,
                         count(*) FILTER (
                             WHERE point.active_registered AND point.physical_identity)::integer
                             AS physical_identity_point_count,
                         count(*) FILTER (
                             WHERE point.active_registered
                               AND point.physical_identity
                               AND point.fresh_sequence_qualified)::integer
                             AS fresh_sequence_qualified_point_count,
                         count(*) FILTER (
                             WHERE point.active_registered
                               AND point.approved_calibration)::integer
                             AS approved_calibration_point_count,
                         count(*) FILTER (
                             WHERE point.active_registered
                               AND point.physical_identity
                               AND point.fresh_sequence_qualified
                               AND point.approved_calibration)::integer
                             AS ready_point_count
                    FROM (
                        SELECT entry.registered
                                   AND entry.device_state = 'ACTIVE'
                                   AND entry.property_present
                                   AND entry.unit IS NOT NULL
                                   AND btrim(entry.unit) <> '' AS active_registered,
                               entry.source_sequence_enabled
                                   AND entry.source_sequence_required
                                   AND entry.source_sequence_origin IN ('DEVICE', 'GATEWAY')
                                   AND entry.source_sequence_binding_fingerprint IS NOT NULL
                                   AS physical_identity,
                               EXISTS (
                                   SELECT 1
                                     FROM bpi.bpi_source_sequence_evidence_current sequence_evidence
                                    WHERE sequence_evidence.tenant_id = entry.tenant_id
                                      AND sequence_evidence.source = snapshot.source
                                      AND sequence_evidence.source_instance = snapshot.source_instance
                                      AND sequence_evidence.plant_id = entry.plant_id
                                      AND sequence_evidence.line_id = entry.line_id
                                      AND sequence_evidence.product_id = entry.product_id
                                      AND sequence_evidence.device_id = entry.device_id
                                      AND sequence_evidence.binding_fingerprint
                                          = entry.source_sequence_binding_fingerprint
                                      AND sequence_evidence.status = 'QUALIFIED'
                                      AND sequence_evidence.sequence_origin
                                          = entry.source_sequence_origin
                                      AND sequence_evidence.observed_at >= snapshot.observed_at
                                      AND sequence_evidence.valid_until > snapshot.observed_at
                                      AND sequence_evidence.valid_until > CURRENT_TIMESTAMP
                               ) AS fresh_sequence_qualified,
                               entry.calibration_version IS NOT NULL
                                   AND btrim(entry.calibration_version) <> ''
                                   AND EXISTS (
                                       SELECT 1
                                         FROM bpi.bpi_point_calibrations calibration
                                        WHERE calibration.tenant_id = entry.tenant_id
                                          AND calibration.plant_id = entry.plant_id
                                          AND calibration.line_id = entry.line_id
                                          AND calibration.product_id = entry.product_id
                                          AND calibration.device_id = entry.device_id
                                          AND calibration.property_id = entry.property_id
                                          AND calibration.calibration_version
                                              = entry.calibration_version
                                          AND calibration.state = 'APPROVED'
                                          AND calibration.valid_from <= snapshot.observed_at
                                          AND calibration.valid_until > snapshot.observed_at
                                          AND calibration.valid_from <= CURRENT_TIMESTAMP
                                          AND calibration.valid_until > CURRENT_TIMESTAMP
                                   ) AS approved_calibration
                          FROM bpi.bpi_point_catalog_entries entry
                         WHERE entry.tenant_id = snapshot.tenant_id
                           AND entry.snapshot_id = snapshot.id
                    ) point
              ) source_coverage ON true
              LEFT JOIN LATERAL (
                  SELECT event.lifecycle_action, event.lifecycle_active, event.status,
                         event.application_status, event.runtime_readiness_status
                    FROM bpi.bpi_outbox_events event
                   WHERE event.tenant_id = sr.tenant_id
                     AND event.aggregate_type = 'RULE_VERSION'
                     AND event.aggregate_id = sr.rule_version_id
                     AND event.event_type = 'BOUNDARY_RULE_PUBLISHED'
                   ORDER BY event.lifecycle_sequence DESC
                   LIMIT 1
              ) lifecycle ON true
              LEFT JOIN LATERAL (
                  SELECT catalog.id
                    FROM bpi.bpi_point_catalog_snapshots catalog
                   WHERE catalog.tenant_id = sr.tenant_id
                     AND catalog.plant_id = sr.plant_id
                     AND catalog.line_id = sr.line_id
                   ORDER BY catalog.observed_at DESC, catalog.imported_at DESC, catalog.id
                   LIMIT 1
              ) current_snapshot ON true
              LEFT JOIN LATERAL (
                  SELECT count(DISTINCT review.batch_id)::integer AS reviewed_batch_count,
                         count(DISTINCT
                             (review.automatic_start_time AT TIME ZONE 'UTC')::date)::integer
                             AS distinct_production_day_count,
                         COALESCE(sum(review.start_boundary_accepted::integer), 0)::integer
                             AS accepted_start_label_count,
                         COALESCE(sum((NOT review.start_boundary_accepted)::integer), 0)::integer
                             AS rejected_start_label_count,
                         COALESCE(sum((review.start_boundary_accepted::integer)
                                    + (review.end_boundary_accepted::integer)), 0)::integer
                             AS accepted_boundary_count,
                         (count(*) * 2)::integer AS total_boundary_count,
                         CASE WHEN count(*) = 0 THEN NULL
                              ELSE (sum((review.start_boundary_accepted::integer)
                                       + (review.end_boundary_accepted::integer))::numeric
                                    / (count(*) * 2)::numeric)
                         END AS boundary_agreement,
                         count(*)::integer AS quantity_sample_count,
                         sum(review.automatic_quantity) AS automatic_quantity_total,
                         sum(review.reference_quantity) AS reference_quantity_total,
                         CASE WHEN count(DISTINCT review.quantity_unit) = 1
                              THEN min(review.quantity_unit) ELSE NULL END AS quantity_unit,
                         CASE WHEN sum(review.reference_quantity) > 0
                              THEN (abs(sum(review.automatic_quantity)
                                       - sum(review.reference_quantity))
                                    / sum(review.reference_quantity) * 100)
                              ELSE NULL END AS cumulative_quantity_deviation_percent,
                         avg(review.quantity_deviation_percent) AS mean_quantity_deviation_percent,
                         max(review.quantity_deviation_percent) AS maximum_quantity_deviation_percent
                    FROM bpi.bpi_shadow_run_batch_reviews review
                   WHERE review.tenant_id = sr.tenant_id
                     AND review.shadow_run_id = sr.id
                     AND review.state = 'ACTIVE'
              ) metrics ON true
              LEFT JOIN LATERAL (
                  SELECT count(*)::integer AS unresolved_critical_incident_count
                    FROM bpi.bpi_data_quality_incidents incident
                   WHERE sr.started_at IS NOT NULL
                     AND incident.tenant_id = sr.tenant_id
                     AND incident.plant_id = sr.plant_id
                     AND incident.line_id = sr.line_id
                     AND incident.severity = 'CRITICAL'
                     AND incident.state <> 'RESOLVED'
                     AND incident.first_seen <= COALESCE(sr.completed_at, now())
                     AND incident.last_seen >= sr.started_at
              ) quality ON true
            """;

    private static final String REVIEW_SELECT = """
            SELECT review.*, batch.batch_no
              FROM bpi.bpi_shadow_run_batch_reviews review
              JOIN bpi.bpi_batch_instances batch
                ON batch.tenant_id = review.tenant_id AND batch.id = review.batch_id
            """;

    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public ShadowRunPostgresRepository(NamedParameterJdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    public void insertDraft(
            ActorContext actor,
            UUID id,
            UUID topologyVersionId,
            UUID pointCatalogSnapshotId,
            ShadowRunCreateCommand command) {
        try {
            jdbc.update("""
                    INSERT INTO bpi.bpi_shadow_runs
                        (id, tenant_id, run_code, name, plant_id, line_id, state, revision,
                         rule_version_id, topology_version_id, point_catalog_snapshot_id,
                         minimum_duration_days, minimum_reviewed_batches,
                         boundary_tolerance_seconds, minimum_boundary_agreement,
                         quantity_tolerance_percent, created_by, updated_by)
                    VALUES (:id, :tenantId, :runCode, :name, :plantId, :lineId, 'DRAFT', 1,
                            :ruleVersionId, :topologyVersionId, :pointCatalogSnapshotId,
                            :minimumDurationDays, :minimumReviewedBatches,
                            :boundaryToleranceSeconds, :minimumBoundaryAgreement,
                            :quantityTolerancePercent, :actorId, :actorId)
                    """, new MapSqlParameterSource()
                    .addValue("id", id).addValue("tenantId", actor.tenantId())
                    .addValue("runCode", command.runCode()).addValue("name", command.name())
                    .addValue("plantId", command.plantId()).addValue("lineId", command.lineId())
                    .addValue("ruleVersionId", command.ruleVersionId())
                    .addValue("topologyVersionId", topologyVersionId)
                    .addValue("pointCatalogSnapshotId", pointCatalogSnapshotId)
                    .addValue("minimumDurationDays", command.minimumDurationDays())
                    .addValue("minimumReviewedBatches", command.minimumReviewedBatches())
                    .addValue("boundaryToleranceSeconds", command.boundaryToleranceSeconds())
                    .addValue("minimumBoundaryAgreement", command.minimumBoundaryAgreement())
                    .addValue("quantityTolerancePercent", command.quantityTolerancePercent())
                    .addValue("actorId", actor.userId()));
        } catch (DataIntegrityViolationException exception) {
            throw new BpiConflictException("Shadow run code already exists or its pinned versions are invalid.", null);
        }
    }

    public List<ShadowRunView> list(
            ActorContext actor, String plantId, String lineId, String state, int limit) {
        StringBuilder sql = new StringBuilder(SHADOW_RUN_SELECT)
                .append(" WHERE sr.tenant_id = :tenantId");
        MapSqlParameterSource parameters = scoped(actor, sql)
                .addValue("tenantId", actor.tenantId())
                .addValue("limit", Math.min(Math.max(limit, 1), 200));
        if (plantId != null && !plantId.isBlank()) {
            sql.append(" AND sr.plant_id = :plantId");
            parameters.addValue("plantId", plantId);
        }
        if (lineId != null && !lineId.isBlank()) {
            sql.append(" AND sr.line_id = :lineId");
            parameters.addValue("lineId", lineId);
        }
        if (state != null && !state.isBlank()) {
            sql.append(" AND sr.state = :state");
            parameters.addValue("state", state);
        }
        sql.append(" ORDER BY sr.created_at DESC, sr.id DESC LIMIT :limit");
        return jdbc.query(sql.toString(), parameters, (rs, rowNum) -> mapRun(rs));
    }

    public ShadowRunView find(ActorContext actor, UUID runId) {
        return find(actor, runId, false);
    }

    public ShadowRunView lock(ActorContext actor, UUID runId) {
        return find(actor, runId, true);
    }

    private ShadowRunView find(ActorContext actor, UUID runId, boolean lock) {
        try {
            ShadowRunView run = jdbc.queryForObject(
                    SHADOW_RUN_SELECT + " WHERE sr.tenant_id = :tenantId AND sr.id = :id"
                            + (lock ? " FOR UPDATE OF sr" : ""),
                    new MapSqlParameterSource().addValue("tenantId", actor.tenantId()).addValue("id", runId),
                    (rs, rowNum) -> mapRun(rs));
            if (run == null || !actor.canAccess(run.plantId(), run.lineId())) {
                throw new BpiNotFoundException("Shadow run not found.");
            }
            return run;
        } catch (EmptyResultDataAccessException exception) {
            throw new BpiNotFoundException("Shadow run not found.");
        }
    }

    public void start(ActorContext actor, UUID runId, long expectedRevision) {
        updateState(actor, runId, expectedRevision, "DRAFT", """
                state = 'RUNNING', revision = revision + 1,
                started_by = :actorId, started_at = now()
                """);
    }

    public void complete(ActorContext actor, UUID runId, long expectedRevision) {
        updateState(actor, runId, expectedRevision, "RUNNING", """
                state = 'EVALUATING', revision = revision + 1,
                completed_by = :actorId, completed_at = now()
                """);
    }

    public void decide(
            ActorContext actor,
            UUID runId,
            long expectedRevision,
            String targetState,
            String reason) {
        int updated = jdbc.update("""
                UPDATE bpi.bpi_shadow_runs
                   SET state = :targetState, revision = revision + 1,
                       decided_by = :actorId, decided_at = now(), decision_reason = :reason,
                       updated_by = :actorId, updated_at = now()
                 WHERE tenant_id = :tenantId AND id = :id
                   AND revision = :expectedRevision AND state = 'EVALUATING'
                """, new MapSqlParameterSource().addValue("targetState", targetState)
                .addValue("actorId", actor.userId()).addValue("reason", reason)
                .addValue("tenantId", actor.tenantId()).addValue("id", runId)
                .addValue("expectedRevision", expectedRevision));
        if (updated != 1) {
            throw new BpiConflictException("Shadow run was changed before the decision completed.", expectedRevision);
        }
    }

    public void cancel(
            ActorContext actor, UUID runId, long expectedRevision, String reason) {
        int updated = jdbc.update("""
                UPDATE bpi.bpi_shadow_runs
                   SET state = 'CANCELLED', revision = revision + 1,
                       cancelled_by = :actorId, cancelled_at = now(), cancellation_reason = :reason,
                       updated_by = :actorId, updated_at = now()
                 WHERE tenant_id = :tenantId AND id = :id
                   AND revision = :expectedRevision AND state IN ('DRAFT', 'RUNNING')
                """, new MapSqlParameterSource().addValue("actorId", actor.userId())
                .addValue("reason", reason).addValue("tenantId", actor.tenantId())
                .addValue("id", runId).addValue("expectedRevision", expectedRevision));
        if (updated != 1) {
            throw new BpiConflictException("Only a DRAFT or RUNNING shadow run can be cancelled.", expectedRevision);
        }
    }

    public ShadowRunBatchSource lockBatchSource(ActorContext actor, UUID batchId) {
        try {
            ShadowRunBatchSource batch = jdbc.queryForObject("""
                    SELECT id, batch_no, plant_id, line_id, state, is_shadow,
                           start_time, end_time, quantity, quantity_unit,
                           rule_version_id, topology_version_id
                      FROM bpi.bpi_batch_instances
                     WHERE tenant_id = :tenantId AND id = :id
                     FOR UPDATE
                    """, new MapSqlParameterSource().addValue("tenantId", actor.tenantId())
                    .addValue("id", batchId), (rs, rowNum) -> new ShadowRunBatchSource(
                            rs.getObject("id", UUID.class), rs.getString("batch_no"),
                            rs.getString("plant_id"), rs.getString("line_id"), rs.getString("state"),
                            rs.getBoolean("is_shadow"), instant(rs, "start_time"), instant(rs, "end_time"),
                            rs.getBigDecimal("quantity"), rs.getString("quantity_unit"),
                            rs.getObject("rule_version_id", UUID.class),
                            rs.getObject("topology_version_id", UUID.class)));
            if (batch == null || !actor.canAccess(batch.plantId(), batch.lineId())) {
                throw new BpiNotFoundException("Shadow batch not found.");
            }
            return batch;
        } catch (EmptyResultDataAccessException exception) {
            throw new BpiNotFoundException("Shadow batch not found.");
        }
    }

    public String activeQuantityUnit(String tenantId, UUID runId) {
        List<String> units = jdbc.query("""
                SELECT DISTINCT quantity_unit
                  FROM bpi.bpi_shadow_run_batch_reviews
                 WHERE tenant_id = :tenantId AND shadow_run_id = :runId AND state = 'ACTIVE'
                 LIMIT 2
                """, new MapSqlParameterSource().addValue("tenantId", tenantId).addValue("runId", runId),
                (rs, rowNum) -> rs.getString("quantity_unit"));
        return units.size() == 1 ? units.get(0) : null;
    }

    public long nextReviewSequence(String tenantId, UUID runId, UUID batchId) {
        Long next = jdbc.queryForObject("""
                SELECT COALESCE(max(review_sequence), 0) + 1
                  FROM bpi.bpi_shadow_run_batch_reviews
                 WHERE tenant_id = :tenantId AND shadow_run_id = :runId AND batch_id = :batchId
                """, new MapSqlParameterSource().addValue("tenantId", tenantId)
                .addValue("runId", runId).addValue("batchId", batchId), Long.class);
        return next == null ? 1 : next;
    }

    public void supersedeActiveReview(String tenantId, UUID runId, UUID batchId) {
        jdbc.update("""
                UPDATE bpi.bpi_shadow_run_batch_reviews
                   SET state = 'SUPERSEDED', superseded_at = now()
                 WHERE tenant_id = :tenantId AND shadow_run_id = :runId
                   AND batch_id = :batchId AND state = 'ACTIVE'
                """, new MapSqlParameterSource().addValue("tenantId", tenantId)
                .addValue("runId", runId).addValue("batchId", batchId));
    }

    public void insertReview(
            ActorContext actor,
            UUID id,
            UUID runId,
            ShadowRunBatchSource batch,
            long sequence,
            Instant manualStartTime,
            Instant manualEndTime,
            long startDeviationSeconds,
            long endDeviationSeconds,
            boolean startAccepted,
            boolean endAccepted,
            BigDecimal referenceQuantity,
            BigDecimal quantityDeviationPercent,
            boolean quantityWithinTolerance,
            String reason) {
        jdbc.update("""
                INSERT INTO bpi.bpi_shadow_run_batch_reviews
                    (id, tenant_id, shadow_run_id, batch_id, review_sequence, state,
                     automatic_start_time, automatic_end_time, manual_start_time, manual_end_time,
                     start_deviation_seconds, end_deviation_seconds,
                     start_boundary_accepted, end_boundary_accepted,
                     automatic_quantity, reference_quantity, quantity_unit,
                     quantity_deviation_percent, quantity_within_tolerance,
                     reviewed_by, review_reason)
                VALUES (:id, :tenantId, :runId, :batchId, :sequence, 'ACTIVE',
                        :automaticStartTime, :automaticEndTime, :manualStartTime, :manualEndTime,
                        :startDeviationSeconds, :endDeviationSeconds,
                        :startAccepted, :endAccepted,
                        :automaticQuantity, :referenceQuantity, :quantityUnit,
                        :quantityDeviationPercent, :quantityWithinTolerance,
                        :actorId, :reason)
                """, new MapSqlParameterSource().addValue("id", id)
                .addValue("tenantId", actor.tenantId()).addValue("runId", runId)
                .addValue("batchId", batch.id()).addValue("sequence", sequence)
                .addValue("automaticStartTime", Timestamp.from(batch.startTime()))
                .addValue("automaticEndTime", Timestamp.from(batch.endTime()))
                .addValue("manualStartTime", Timestamp.from(manualStartTime))
                .addValue("manualEndTime", Timestamp.from(manualEndTime))
                .addValue("startDeviationSeconds", startDeviationSeconds)
                .addValue("endDeviationSeconds", endDeviationSeconds)
                .addValue("startAccepted", startAccepted).addValue("endAccepted", endAccepted)
                .addValue("automaticQuantity", batch.quantity())
                .addValue("referenceQuantity", referenceQuantity)
                .addValue("quantityUnit", batch.quantityUnit())
                .addValue("quantityDeviationPercent", quantityDeviationPercent)
                .addValue("quantityWithinTolerance", quantityWithinTolerance)
                .addValue("actorId", actor.userId()).addValue("reason", reason));
    }

    public void incrementRevision(ActorContext actor, UUID runId, long expectedRevision) {
        int updated = jdbc.update("""
                UPDATE bpi.bpi_shadow_runs
                   SET revision = revision + 1, updated_by = :actorId, updated_at = now()
                 WHERE tenant_id = :tenantId AND id = :id
                   AND revision = :expectedRevision AND state = 'RUNNING'
                """, new MapSqlParameterSource().addValue("actorId", actor.userId())
                .addValue("tenantId", actor.tenantId()).addValue("id", runId)
                .addValue("expectedRevision", expectedRevision));
        if (updated != 1) {
            throw new BpiConflictException("Shadow run was changed before the batch review completed.", expectedRevision);
        }
    }

    public List<ShadowRunBatchReviewView> listReviews(
            ActorContext actor, UUID runId, boolean includeSuperseded) {
        find(actor, runId);
        String sql = REVIEW_SELECT + " WHERE review.tenant_id = :tenantId AND review.shadow_run_id = :runId"
                + (includeSuperseded ? "" : " AND review.state = 'ACTIVE'")
                + " ORDER BY review.reviewed_at DESC, review.id DESC";
        return jdbc.query(sql, new MapSqlParameterSource().addValue("tenantId", actor.tenantId())
                .addValue("runId", runId), (rs, rowNum) -> mapReview(rs));
    }

    public ShadowRunBatchReviewView findReview(ActorContext actor, UUID reviewId) {
        try {
            ShadowRunBatchReviewView review = jdbc.queryForObject(
                    REVIEW_SELECT + " WHERE review.tenant_id = :tenantId AND review.id = :id",
                    new MapSqlParameterSource().addValue("tenantId", actor.tenantId()).addValue("id", reviewId),
                    (rs, rowNum) -> mapReview(rs));
            if (review == null) throw new BpiNotFoundException("Shadow run batch review not found.");
            find(actor, review.shadowRunId());
            return review;
        } catch (EmptyResultDataAccessException exception) {
            throw new BpiNotFoundException("Shadow run batch review not found.");
        }
    }

    public void insertAudit(
            ActorContext actor,
            ShadowRunView run,
            String action,
            long beforeRevision,
            long afterRevision,
            String reason,
            String traceId,
            Map<String, Object> detail) {
        jdbc.update("""
                INSERT INTO bpi.bpi_audit_events
                    (id, tenant_id, plant_id, line_id, object_type, object_id, action, actor_id,
                     before_revision, after_revision, reason, trace_id, detail)
                VALUES (:id, :tenantId, :plantId, :lineId, 'SHADOW_RUN', :objectId, :action, :actorId,
                        :beforeRevision, :afterRevision, :reason, :traceId, CAST(:detail AS jsonb))
                """, new MapSqlParameterSource().addValue("id", UUID.randomUUID())
                .addValue("tenantId", actor.tenantId()).addValue("plantId", run.plantId())
                .addValue("lineId", run.lineId()).addValue("objectId", run.id())
                .addValue("action", action).addValue("actorId", actor.userId())
                .addValue("beforeRevision", beforeRevision).addValue("afterRevision", afterRevision)
                .addValue("reason", reason).addValue("traceId", traceId)
                .addValue("detail", writeJson(detail)));
    }

    private void updateState(
            ActorContext actor, UUID runId, long expectedRevision, String expectedState, String assignments) {
        int updated;
        try {
            updated = jdbc.update("UPDATE bpi.bpi_shadow_runs SET " + assignments + """
                    , updated_by = :actorId, updated_at = now()
                     WHERE tenant_id = :tenantId AND id = :id
                       AND revision = :expectedRevision AND state = :expectedState
                    """, new MapSqlParameterSource().addValue("actorId", actor.userId())
                    .addValue("tenantId", actor.tenantId()).addValue("id", runId)
                    .addValue("expectedRevision", expectedRevision).addValue("expectedState", expectedState));
        } catch (DataIntegrityViolationException exception) {
            throw new BpiConflictException("Another shadow run is already RUNNING for this line.", expectedRevision);
        }
        if (updated != 1) {
            throw new BpiConflictException("Shadow run state or revision changed before the command completed.", expectedRevision);
        }
    }

    private ShadowRunView mapRun(ResultSet rs) throws SQLException {
        int pinnedPointCount = rs.getInt("pinned_point_count");
        int readyPointCount = rs.getInt("ready_point_count");
        ShadowRunSourceCoverage sourceCoverage = new ShadowRunSourceCoverage(
                pinnedPointCount,
                rs.getInt("active_registered_point_count"),
                rs.getInt("physical_identity_point_count"),
                rs.getInt("fresh_sequence_qualified_point_count"),
                rs.getInt("approved_calibration_point_count"),
                readyPointCount,
                pinnedPointCount > 0 && readyPointCount == pinnedPointCount);
        ShadowRunReadiness readiness = new ShadowRunReadiness(
                "PUBLISHED".equals(rs.getString("rule_state")),
                "ACTIVATE".equals(rs.getString("lifecycle_action")) && rs.getBoolean("lifecycle_active"),
                "PUBLISHED".equals(rs.getString("publication_status")),
                "APPLIED".equals(rs.getString("application_status")),
                "READY".equals(rs.getString("runtime_readiness_status")),
                "PUBLISHED".equals(rs.getString("topology_state")),
                equalUuid(rs, "validated_point_catalog_snapshot_id", "point_catalog_snapshot_id")
                        && equalString(rs, "validated_point_catalog_checksum", "point_catalog_checksum"),
                equalUuid(rs, "current_point_catalog_snapshot_id", "point_catalog_snapshot_id"),
                sourceCoverage.fullyReady());

        long duration = rs.getLong("observed_duration_seconds");
        int reviewed = rs.getInt("reviewed_batch_count");
        int productionDays = rs.getInt("distinct_production_day_count");
        int acceptedStartLabels = rs.getInt("accepted_start_label_count");
        int rejectedStartLabels = rs.getInt("rejected_start_label_count");
        int acceptedBoundaries = rs.getInt("accepted_boundary_count");
        int totalBoundaries = rs.getInt("total_boundary_count");
        BigDecimal boundaryAgreement = scaled(rs.getBigDecimal("boundary_agreement"));
        int quantitySamples = rs.getInt("quantity_sample_count");
        BigDecimal cumulativeDeviation = scaled(rs.getBigDecimal("cumulative_quantity_deviation_percent"));
        int criticalIncidents = rs.getInt("unresolved_critical_incident_count");
        int minimumDays = rs.getInt("minimum_duration_days");
        int minimumReviews = rs.getInt("minimum_reviewed_batches");
        BigDecimal minimumAgreement = rs.getBigDecimal("minimum_boundary_agreement");
        BigDecimal quantityTolerance = rs.getBigDecimal("quantity_tolerance_percent");

        boolean durationGate = duration >= minimumDays * 86_400L;
        boolean reviewGate = reviewed >= minimumReviews;
        boolean boundaryGate = boundaryAgreement != null
                && boundaryAgreement.compareTo(minimumAgreement) >= 0;
        boolean quantityGate = quantitySamples >= minimumReviews
                && cumulativeDeviation != null
                && cumulativeDeviation.compareTo(quantityTolerance) <= 0;
        boolean qualityGate = criticalIncidents == 0;
        ShadowRunMetrics metrics = new ShadowRunMetrics(
                duration, reviewed, acceptedBoundaries, totalBoundaries, boundaryAgreement,
                quantitySamples, zero(rs.getBigDecimal("automatic_quantity_total")),
                zero(rs.getBigDecimal("reference_quantity_total")), rs.getString("quantity_unit"),
                cumulativeDeviation, scaled(rs.getBigDecimal("mean_quantity_deviation_percent")),
                scaled(rs.getBigDecimal("maximum_quantity_deviation_percent")), criticalIncidents,
                durationGate, reviewGate, boundaryGate, quantityGate, qualityGate);
        List<String> trainingCoverageBlockers = trainingDataCoverageBlockers(
                reviewed, productionDays, acceptedStartLabels, rejectedStartLabels);
        ShadowRunTrainingDataCoverage trainingDataCoverage = new ShadowRunTrainingDataCoverage(
                TRAINING_DATA_COVERAGE_POLICY,
                REQUIRED_TRAINING_REVIEWED_BATCHES, reviewed,
                REQUIRED_TRAINING_PRODUCTION_DAYS, productionDays,
                REQUIRED_TRAINING_ACCEPTED_START_LABELS, acceptedStartLabels,
                REQUIRED_TRAINING_REJECTED_START_LABELS, rejectedStartLabels,
                trainingCoverageBlockers.isEmpty(), List.copyOf(trainingCoverageBlockers));

        List<String> blockers = blockers(readiness, metrics);
        String state = rs.getString("state");
        boolean readyForApproval = "EVALUATING".equals(state) && blockers.isEmpty();
        return new ShadowRunView(
                rs.getObject("id", UUID.class), rs.getString("run_code"), rs.getString("name"),
                rs.getString("tenant_id"), rs.getString("plant_id"), rs.getString("line_id"),
                state, rs.getLong("revision"), rs.getObject("rule_version_id", UUID.class),
                rs.getString("rule_version_ref"), rs.getObject("topology_version_id", UUID.class),
                rs.getString("topology_version_ref"),
                rs.getObject("point_catalog_snapshot_id", UUID.class), rs.getString("point_catalog_checksum"),
                minimumDays, minimumReviews, rs.getInt("boundary_tolerance_seconds"),
                minimumAgreement, quantityTolerance, rs.getString("created_by"),
                instant(rs, "created_at"), rs.getString("started_by"), instant(rs, "started_at"),
                rs.getString("completed_by"), instant(rs, "completed_at"), rs.getString("decided_by"),
                instant(rs, "decided_at"), rs.getString("decision_reason"), rs.getString("cancelled_by"),
                instant(rs, "cancelled_at"), rs.getString("cancellation_reason"),
                readiness, sourceCoverage, metrics, trainingDataCoverage,
                List.copyOf(blockers), readyForApproval);
    }

    private List<String> trainingDataCoverageBlockers(
            int reviewedBatchCount,
            int productionDayCount,
            int acceptedStartLabelCount,
            int rejectedStartLabelCount) {
        List<String> blockers = new ArrayList<>();
        if (reviewedBatchCount < REQUIRED_TRAINING_REVIEWED_BATCHES) {
            blockers.add("TRAINING_REVIEWED_BATCHES_BELOW_MINIMUM");
        }
        if (productionDayCount < REQUIRED_TRAINING_PRODUCTION_DAYS) {
            blockers.add("TRAINING_PRODUCTION_DAYS_BELOW_MINIMUM");
        }
        if (acceptedStartLabelCount < REQUIRED_TRAINING_ACCEPTED_START_LABELS) {
            blockers.add("TRAINING_ACCEPTED_START_LABELS_BELOW_MINIMUM");
        }
        if (rejectedStartLabelCount < REQUIRED_TRAINING_REJECTED_START_LABELS) {
            blockers.add("TRAINING_REJECTED_START_LABELS_BELOW_MINIMUM");
        }
        return blockers;
    }

    private List<String> blockers(ShadowRunReadiness readiness, ShadowRunMetrics metrics) {
        List<String> blockers = new ArrayList<>();
        if (!readiness.rulePublished()) blockers.add("RULE_NOT_PUBLISHED");
        if (!readiness.ruleActive()) blockers.add("RULE_NOT_ACTIVE");
        if (!readiness.publicationConfirmed()) blockers.add("RULE_PUBLICATION_NOT_CONFIRMED");
        if (!readiness.applicationApplied()) blockers.add("RULE_APPLICATION_NOT_APPLIED");
        if (!readiness.runtimeReady()) blockers.add("RULE_RUNTIME_NOT_READY");
        if (!readiness.topologyPublished()) blockers.add("TOPOLOGY_NOT_PUBLISHED");
        if (!readiness.topologySnapshotPinned()) blockers.add("TOPOLOGY_POINT_CATALOG_MISMATCH");
        if (!readiness.pointCatalogCurrent()) blockers.add("POINT_CATALOG_NOT_CURRENT");
        if (!readiness.pointCatalogReady()) blockers.add("POINT_CATALOG_NOT_READY");
        if (!metrics.durationGatePassed()) blockers.add("MINIMUM_DURATION_NOT_REACHED");
        if (!metrics.reviewCountGatePassed()) blockers.add("MINIMUM_BATCH_REVIEWS_NOT_REACHED");
        if (!metrics.boundaryAgreementGatePassed()) blockers.add("BOUNDARY_AGREEMENT_BELOW_THRESHOLD");
        if (!metrics.quantityGatePassed()) blockers.add("CUMULATIVE_QUANTITY_DEVIATION_OUT_OF_TOLERANCE");
        if (!metrics.dataQualityGatePassed()) blockers.add("UNRESOLVED_CRITICAL_DATA_QUALITY");
        return blockers;
    }

    private ShadowRunBatchReviewView mapReview(ResultSet rs) throws SQLException {
        return new ShadowRunBatchReviewView(
                rs.getObject("id", UUID.class), rs.getObject("shadow_run_id", UUID.class),
                rs.getObject("batch_id", UUID.class), rs.getString("batch_no"),
                rs.getLong("review_sequence"), rs.getString("state"),
                instant(rs, "automatic_start_time"), instant(rs, "automatic_end_time"),
                instant(rs, "manual_start_time"), instant(rs, "manual_end_time"),
                rs.getLong("start_deviation_seconds"), rs.getLong("end_deviation_seconds"),
                rs.getBoolean("start_boundary_accepted"), rs.getBoolean("end_boundary_accepted"),
                rs.getBigDecimal("automatic_quantity"), rs.getBigDecimal("reference_quantity"),
                rs.getString("quantity_unit"), scaled(rs.getBigDecimal("quantity_deviation_percent")),
                rs.getBoolean("quantity_within_tolerance"), rs.getString("reviewed_by"),
                rs.getString("review_reason"), instant(rs, "reviewed_at"), instant(rs, "superseded_at"));
    }

    private MapSqlParameterSource scoped(ActorContext actor, StringBuilder sql) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        if (!actor.plantIds().contains("*")) {
            if (actor.plantIds().isEmpty()) {
                sql.append(" AND 1 = 0");
            } else {
                sql.append(" AND sr.plant_id IN (:allowedPlants)");
                parameters.addValue("allowedPlants", actor.plantIds());
            }
        }
        if (!actor.lineIds().contains("*")) {
            if (actor.lineIds().isEmpty()) {
                sql.append(" AND 1 = 0");
            } else {
                sql.append(" AND sr.line_id IN (:allowedLines)");
                parameters.addValue("allowedLines", actor.lineIds());
            }
        }
        return parameters;
    }

    private boolean equalUuid(ResultSet rs, String left, String right) throws SQLException {
        UUID leftValue = rs.getObject(left, UUID.class);
        UUID rightValue = rs.getObject(right, UUID.class);
        return leftValue != null && leftValue.equals(rightValue);
    }

    private boolean equalString(ResultSet rs, String left, String right) throws SQLException {
        String leftValue = rs.getString(left);
        String rightValue = rs.getString(right);
        return leftValue != null && leftValue.equals(rightValue);
    }

    private Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }

    private BigDecimal zero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal scaled(BigDecimal value) {
        return value == null ? null : value.setScale(9, RoundingMode.HALF_UP);
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not serialize BPI shadow-run audit detail", exception);
        }
    }
}
