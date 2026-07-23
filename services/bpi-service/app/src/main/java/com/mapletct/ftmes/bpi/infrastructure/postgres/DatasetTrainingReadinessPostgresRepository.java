package com.mapletct.ftmes.bpi.infrastructure.postgres;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mapletct.ftmes.bpi.application.ActorContext;
import com.mapletct.ftmes.bpi.application.error.BpiConflictException;
import com.mapletct.ftmes.bpi.application.error.BpiNotFoundException;
import com.mapletct.ftmes.bpi.domain.DatasetTrainingReadinessBuild;
import com.mapletct.ftmes.bpi.domain.DatasetTrainingReadinessEvidence;
import com.mapletct.ftmes.bpi.domain.DatasetTrainingReadinessView;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class DatasetTrainingReadinessPostgresRepository {
    private static final String VIEW_SELECT = """
            SELECT assessment.*, snapshot.dataset_id,
                   snapshot.line_ids::text AS line_ids,
                   definition.dataset_code, definition.version AS dataset_version,
                   definition.plant_id
              FROM bpi.bpi_dataset_training_readiness_assessments assessment
              JOIN bpi.bpi_dataset_snapshots snapshot
                ON snapshot.tenant_id = assessment.tenant_id
               AND snapshot.id = assessment.source_snapshot_id
              JOIN bpi.bpi_dataset_definitions definition
                ON definition.tenant_id = snapshot.tenant_id
               AND definition.id = snapshot.dataset_id
            """;

    private static final String EVIDENCE_SELECT = """
            SELECT registration.id AS registration_id,
                   registration.source_snapshot_id AS snapshot_id,
                   snapshot.dataset_id,
                   definition.dataset_code, definition.version AS dataset_version,
                   registration.tenant_id, definition.plant_id,
                   snapshot.line_ids::text AS line_ids,
                   registration.state AS registration_state,
                   registration.revision AS registration_revision,
                   registration.manifest_checksum, registration.dataset_digest,
                   registration.source_row_count,
                   COALESCE(registration.registration_metadata ->> 'datasetInputVerified', 'false')
                       = 'true' AS dataset_input_verified,
                   COALESCE(registration.registration_metadata ->> 'lineageVerified', 'false')
                       = 'true' AS lineage_verified,
                   COALESCE(registration.registration_metadata ->> 'sourceFactsVerified', 'false')
                       = 'true' AS source_facts_verified,
                   definition.prediction_time_policy,
                   definition.feature_cutoff_policy,
                   definition.split_policy,
                   definition.feature_refs::text AS feature_refs,
                   definition.label_refs::text AS label_refs,
                   COALESCE(snapshot.included_count, 0) AS snapshot_included_count,
                   COALESCE(snapshot.excluded_count, 0) AS snapshot_excluded_count,
                   COALESCE(sample_metrics.persisted_sample_count, 0)
                       AS persisted_sample_count,
                   COALESCE(sample_metrics.included_sample_count, 0)
                       AS included_sample_count,
                   COALESCE(sample_metrics.excluded_sample_count, 0)
                       AS excluded_sample_count,
                   COALESCE(sample_metrics.distinct_batch_count, 0)
                       AS distinct_batch_count,
                   COALESCE(sample_metrics.distinct_production_day_count, 0)
                       AS distinct_production_day_count,
                   COALESCE(sample_metrics.production_split_group_count, 0)
                       AS production_split_group_count,
                   COALESCE(sample_metrics.leakage_row_count, 0)
                       AS leakage_row_count,
                   COALESCE(sample_metrics.start_accepted_label_count, 0)
                       AS start_accepted_label_count,
                   COALESCE(sample_metrics.start_rejected_label_count, 0)
                       AS start_rejected_label_count,
                   COALESCE(sample_metrics.start_label_missing_count, 0)
                       AS start_label_missing_count,
                   COALESCE(run_metrics.distinct_shadow_run_count, 0)
                       AS distinct_shadow_run_count,
                   COALESCE(run_metrics.approved_shadow_run_count, 0)
                       AS approved_shadow_run_count,
                   COALESCE(run_metrics.duration_gate_failure_count, 0)
                       AS duration_gate_failure_count,
                   COALESCE(run_metrics.maximum_continuous_shadow_seconds, 0)
                       AS maximum_continuous_shadow_seconds,
                   COALESCE(run_metrics.point_catalog_snapshot_count, 0)
                       AS point_catalog_snapshot_count,
                   COALESCE(run_metrics.ready_point_catalog_snapshot_count, 0)
                       AS ready_point_catalog_snapshot_count,
                   COALESCE(quality_metrics.unresolved_critical_incident_count, 0)
                       AS unresolved_critical_incident_count
              FROM bpi.bpi_dataset_mlflow_registrations registration
              JOIN bpi.bpi_dataset_snapshots snapshot
                ON snapshot.tenant_id = registration.tenant_id
               AND snapshot.id = registration.source_snapshot_id
              JOIN bpi.bpi_dataset_definitions definition
                ON definition.tenant_id = snapshot.tenant_id
               AND definition.id = snapshot.dataset_id
              LEFT JOIN LATERAL (
                  SELECT count(*)::integer AS persisted_sample_count,
                         count(*) FILTER (WHERE sample.included)::integer
                             AS included_sample_count,
                         count(*) FILTER (WHERE NOT sample.included)::integer
                             AS excluded_sample_count,
                         count(DISTINCT sample.batch_id)
                             FILTER (WHERE sample.included)::integer
                             AS distinct_batch_count,
                         count(DISTINCT
                             (sample.prediction_time AT TIME ZONE 'UTC')::date)
                             FILTER (WHERE sample.included)::integer
                             AS distinct_production_day_count,
                         count(DISTINCT sample.split_key)
                             FILTER (WHERE sample.included)::integer
                             AS production_split_group_count,
                         count(*) FILTER (
                             WHERE sample.included
                               AND (sample.feature_cutoff > sample.prediction_time
                                OR sample.label_available_at < sample.prediction_time))::integer
                             AS leakage_row_count,
                         count(*) FILTER (
                             WHERE sample.included
                               AND sample.label_payload -> 'review.boundary_acceptance'
                                   ->> 'start' = 'true')::integer
                             AS start_accepted_label_count,
                         count(*) FILTER (
                             WHERE sample.included
                               AND sample.label_payload -> 'review.boundary_acceptance'
                                   ->> 'start' = 'false')::integer
                             AS start_rejected_label_count,
                         count(*) FILTER (
                             WHERE sample.included
                               AND sample.label_payload -> 'review.boundary_acceptance'
                                   ->> 'start' IS NULL)::integer
                             AS start_label_missing_count
                    FROM bpi.bpi_dataset_snapshot_samples sample
                   WHERE sample.tenant_id = registration.tenant_id
                     AND sample.snapshot_id = registration.source_snapshot_id
              ) sample_metrics ON true
              LEFT JOIN LATERAL (
                  SELECT count(*)::integer AS distinct_shadow_run_count,
                         count(*) FILTER (WHERE run.state = 'APPROVED')::integer
                             AS approved_shadow_run_count,
                         count(*) FILTER (
                             WHERE run.state <> 'APPROVED'
                                OR run.started_at IS NULL OR run.completed_at IS NULL
                                OR EXTRACT(EPOCH FROM
                                    (run.completed_at - run.started_at))
                                    < GREATEST(run.minimum_duration_days, 7) * 86400
                         )::integer AS duration_gate_failure_count,
                         COALESCE(max(FLOOR(EXTRACT(EPOCH FROM
                             (run.completed_at - run.started_at)))), 0)::bigint
                             AS maximum_continuous_shadow_seconds,
                         count(DISTINCT catalog.id)::integer
                             AS point_catalog_snapshot_count,
                         count(DISTINCT catalog.id) FILTER (
                             WHERE catalog.point_count > 0
                               AND catalog.source_claim_ready_point_count
                                   = catalog.point_count)::integer
                             AS ready_point_catalog_snapshot_count
                    FROM (
                        SELECT DISTINCT sample.shadow_run_id
                          FROM bpi.bpi_dataset_snapshot_samples sample
                         WHERE sample.tenant_id = registration.tenant_id
                           AND sample.snapshot_id = registration.source_snapshot_id
                           AND sample.included
                    ) source_run
                    JOIN bpi.bpi_shadow_runs run
                      ON run.tenant_id = registration.tenant_id
                     AND run.id = source_run.shadow_run_id
                    JOIN bpi.bpi_point_catalog_snapshots catalog
                      ON catalog.tenant_id = run.tenant_id
                     AND catalog.id = run.point_catalog_snapshot_id
              ) run_metrics ON true
              LEFT JOIN LATERAL (
                  SELECT count(DISTINCT incident.id)::integer
                             AS unresolved_critical_incident_count
                    FROM (
                        SELECT DISTINCT sample.shadow_run_id
                          FROM bpi.bpi_dataset_snapshot_samples sample
                         WHERE sample.tenant_id = registration.tenant_id
                           AND sample.snapshot_id = registration.source_snapshot_id
                           AND sample.included
                    ) source_run
                    JOIN bpi.bpi_shadow_runs run
                      ON run.tenant_id = registration.tenant_id
                     AND run.id = source_run.shadow_run_id
                    JOIN bpi.bpi_data_quality_incidents incident
                      ON incident.tenant_id = run.tenant_id
                     AND incident.plant_id = run.plant_id
                     AND incident.line_id = run.line_id
                     AND incident.severity = 'CRITICAL'
                     AND incident.state <> 'RESOLVED'
                     AND incident.first_seen <= run.completed_at
                     AND incident.last_seen >= run.started_at
              ) quality_metrics ON true
            """;

    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public DatasetTrainingReadinessPostgresRepository(
            NamedParameterJdbcTemplate jdbc,
            ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    public DatasetTrainingReadinessEvidence lockEvidence(
            ActorContext actor,
            UUID registrationId) {
        StringBuilder sql = new StringBuilder(EVIDENCE_SELECT)
                .append(" WHERE registration.tenant_id = :tenantId")
                .append(" AND registration.id = :registrationId");
        MapSqlParameterSource parameters = scoped(actor, sql, "definition", "snapshot")
                .addValue("tenantId", actor.tenantId())
                .addValue("registrationId", registrationId);
        sql.append(" FOR UPDATE OF registration");
        try {
            DatasetTrainingReadinessEvidence evidence = jdbc.queryForObject(
                    sql.toString(), parameters, (rs, rowIndex) -> mapEvidence(rs));
            if (evidence == null) throw registrationNotFound();
            return evidence;
        } catch (EmptyResultDataAccessException exception) {
            throw registrationNotFound();
        }
    }

    public long nextSequence(
            ActorContext actor,
            UUID registrationId,
            String objectiveCode,
            String policyVersion) {
        return jdbc.queryForObject("""
                SELECT COALESCE(max(assessment_sequence), 0) + 1
                  FROM bpi.bpi_dataset_training_readiness_assessments
                 WHERE tenant_id = :tenantId
                   AND mlflow_registration_id = :registrationId
                   AND objective_code = :objectiveCode
                   AND policy_version = :policyVersion
                """, new MapSqlParameterSource()
                .addValue("tenantId", actor.tenantId())
                .addValue("registrationId", registrationId)
                .addValue("objectiveCode", objectiveCode)
                .addValue("policyVersion", policyVersion), Long.class);
    }

    public void insert(
            ActorContext actor,
            UUID id,
            DatasetTrainingReadinessEvidence evidence,
            String objectiveCode,
            String policyVersion,
            long sequence,
            DatasetTrainingReadinessBuild build,
            String reason) {
        try {
            jdbc.update("""
                    INSERT INTO bpi.bpi_dataset_training_readiness_assessments
                        (id, tenant_id, mlflow_registration_id, source_snapshot_id,
                         objective_code, policy_version, assessment_sequence, state,
                         source_registration_revision, manifest_checksum, dataset_digest,
                         required_thresholds, observed_metrics, gate_results, blocker_codes,
                         phase_boundary, assessment_checksum, assessed_by, assessment_reason)
                    VALUES (:id, :tenantId, :registrationId, :snapshotId,
                            :objectiveCode, :policyVersion, :sequence, :state,
                            :registrationRevision, :manifestChecksum, :datasetDigest,
                            CAST(:requiredThresholds AS jsonb), CAST(:observedMetrics AS jsonb),
                            CAST(:gateResults AS jsonb), CAST(:blockerCodes AS jsonb),
                            CAST(:phaseBoundary AS jsonb), :assessmentChecksum,
                            :assessedBy, :assessmentReason)
                    """, new MapSqlParameterSource()
                    .addValue("id", id)
                    .addValue("tenantId", actor.tenantId())
                    .addValue("registrationId", evidence.registrationId())
                    .addValue("snapshotId", evidence.snapshotId())
                    .addValue("objectiveCode", objectiveCode)
                    .addValue("policyVersion", policyVersion)
                    .addValue("sequence", sequence)
                    .addValue("state", build.state())
                    .addValue("registrationRevision", evidence.registrationRevision())
                    .addValue("manifestChecksum", evidence.manifestChecksum())
                    .addValue("datasetDigest", evidence.datasetDigest())
                    .addValue("requiredThresholds", writeJson(build.requiredThresholds()))
                    .addValue("observedMetrics", writeJson(build.observedMetrics()))
                    .addValue("gateResults", writeJson(build.gateResults()))
                    .addValue("blockerCodes", writeJson(build.blockerCodes()))
                    .addValue("phaseBoundary", writeJson(build.phaseBoundary()))
                    .addValue("assessmentChecksum", build.assessmentChecksum())
                    .addValue("assessedBy", actor.userId())
                    .addValue("assessmentReason", reason));
        } catch (DataIntegrityViolationException exception) {
            throw new BpiConflictException(
                    "Training readiness assessment sequence already exists.", null);
        }
    }

    public DatasetTrainingReadinessView find(ActorContext actor, UUID id) {
        StringBuilder sql = new StringBuilder(VIEW_SELECT)
                .append(" WHERE assessment.tenant_id = :tenantId")
                .append(" AND assessment.id = :id");
        MapSqlParameterSource parameters = scoped(actor, sql, "definition", "snapshot")
                .addValue("tenantId", actor.tenantId())
                .addValue("id", id);
        try {
            DatasetTrainingReadinessView value = jdbc.queryForObject(
                    sql.toString(), parameters, (rs, rowIndex) -> mapView(rs));
            if (value == null) throw assessmentNotFound();
            return value;
        } catch (EmptyResultDataAccessException exception) {
            throw assessmentNotFound();
        }
    }

    public Optional<DatasetTrainingReadinessView> findLatest(
            ActorContext actor,
            UUID registrationId,
            String objectiveCode,
            String policyVersion) {
        StringBuilder sql = new StringBuilder(VIEW_SELECT)
                .append(" WHERE assessment.tenant_id = :tenantId")
                .append(" AND assessment.mlflow_registration_id = :registrationId")
                .append(" AND assessment.objective_code = :objectiveCode")
                .append(" AND assessment.policy_version = :policyVersion");
        MapSqlParameterSource parameters = scoped(actor, sql, "definition", "snapshot")
                .addValue("tenantId", actor.tenantId())
                .addValue("registrationId", registrationId)
                .addValue("objectiveCode", objectiveCode)
                .addValue("policyVersion", policyVersion);
        sql.append(" ORDER BY assessment.assessment_sequence DESC, assessment.id DESC LIMIT 1");
        try {
            return Optional.ofNullable(jdbc.queryForObject(
                    sql.toString(), parameters, (rs, rowIndex) -> mapView(rs)));
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    private MapSqlParameterSource scoped(
            ActorContext actor,
            StringBuilder sql,
            String definitionAlias,
            String snapshotAlias) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        if (!actor.plantIds().contains("*")) {
            sql.append(" AND ").append(definitionAlias).append(".plant_id IN (:allowedPlants)");
            parameters.addValue("allowedPlants", actor.plantIds().isEmpty()
                    ? List.of("__NO_PLANT_SCOPE__") : actor.plantIds());
        }
        if (!actor.lineIds().contains("*")) {
            sql.append(" AND CAST(:allowedLines AS jsonb) @> ")
                    .append(snapshotAlias).append(".line_ids");
            parameters.addValue("allowedLines", writeJson(actor.lineIds().isEmpty()
                    ? List.of("__NO_LINE_SCOPE__")
                    : actor.lineIds().stream().sorted().toList()));
        }
        return parameters;
    }

    private DatasetTrainingReadinessEvidence mapEvidence(ResultSet rs) throws SQLException {
        return new DatasetTrainingReadinessEvidence(
                rs.getObject("registration_id", UUID.class),
                rs.getObject("snapshot_id", UUID.class),
                rs.getObject("dataset_id", UUID.class),
                rs.getString("dataset_code"), rs.getString("dataset_version"),
                rs.getString("tenant_id"), rs.getString("plant_id"),
                readStrings(rs.getString("line_ids")),
                rs.getString("registration_state"), rs.getLong("registration_revision"),
                rs.getString("manifest_checksum"), rs.getString("dataset_digest"),
                rs.getLong("source_row_count"), rs.getBoolean("dataset_input_verified"),
                rs.getBoolean("lineage_verified"), rs.getBoolean("source_facts_verified"),
                rs.getString("prediction_time_policy"),
                rs.getString("feature_cutoff_policy"), rs.getString("split_policy"),
                readStrings(rs.getString("feature_refs")),
                readStrings(rs.getString("label_refs")),
                rs.getInt("snapshot_included_count"),
                rs.getInt("snapshot_excluded_count"),
                rs.getInt("persisted_sample_count"), rs.getInt("included_sample_count"),
                rs.getInt("excluded_sample_count"), rs.getInt("distinct_batch_count"),
                rs.getInt("distinct_production_day_count"),
                rs.getInt("production_split_group_count"), rs.getInt("leakage_row_count"),
                rs.getInt("start_accepted_label_count"),
                rs.getInt("start_rejected_label_count"),
                rs.getInt("start_label_missing_count"),
                rs.getInt("distinct_shadow_run_count"),
                rs.getInt("approved_shadow_run_count"),
                rs.getInt("duration_gate_failure_count"),
                rs.getLong("maximum_continuous_shadow_seconds"),
                rs.getInt("point_catalog_snapshot_count"),
                rs.getInt("ready_point_catalog_snapshot_count"),
                rs.getInt("unresolved_critical_incident_count"));
    }

    private DatasetTrainingReadinessView mapView(ResultSet rs) throws SQLException {
        return new DatasetTrainingReadinessView(
                rs.getObject("id", UUID.class),
                rs.getObject("mlflow_registration_id", UUID.class),
                rs.getObject("source_snapshot_id", UUID.class),
                rs.getObject("dataset_id", UUID.class),
                rs.getString("dataset_code"), rs.getString("dataset_version"),
                rs.getString("tenant_id"), rs.getString("plant_id"),
                readStrings(rs.getString("line_ids")), rs.getString("objective_code"),
                rs.getString("policy_version"), rs.getLong("assessment_sequence"),
                rs.getString("state"), rs.getLong("revision"),
                rs.getLong("source_registration_revision"),
                rs.getString("manifest_checksum"), rs.getString("dataset_digest"),
                readMap(rs.getString("required_thresholds")),
                readMap(rs.getString("observed_metrics")),
                readMaps(rs.getString("gate_results")),
                readStrings(rs.getString("blocker_codes")),
                readMap(rs.getString("phase_boundary")),
                rs.getString("assessment_checksum"), rs.getString("assessed_by"),
                rs.getString("assessment_reason"), instant(rs, "assessed_at"));
    }

    private Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Could not serialize BPI training readiness JSON", exception);
        }
    }

    private List<String> readStrings(String value) {
        if (value == null) return List.of();
        try {
            return objectMapper.readValue(value, new TypeReference<List<String>>() {});
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Could not read BPI training readiness string list", exception);
        }
    }

    private Map<String, Object> readMap(String value) {
        try {
            return objectMapper.readValue(value,
                    new TypeReference<Map<String, Object>>() {});
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Could not read BPI training readiness object", exception);
        }
    }

    private List<Map<String, Object>> readMaps(String value) {
        try {
            return objectMapper.readValue(value,
                    new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Could not read BPI training readiness gate list", exception);
        }
    }

    private BpiNotFoundException registrationNotFound() {
        return new BpiNotFoundException("MLflow dataset registration not found.");
    }

    private BpiNotFoundException assessmentNotFound() {
        return new BpiNotFoundException("Dataset training readiness assessment not found.");
    }
}
