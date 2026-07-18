package com.mapletct.ftmes.bpi.infrastructure.postgres;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mapletct.ftmes.bpi.application.ActorContext;
import com.mapletct.ftmes.bpi.application.error.BpiConflictException;
import com.mapletct.ftmes.bpi.application.error.BpiNotFoundException;
import com.mapletct.ftmes.bpi.domain.GoldenBoundary;
import com.mapletct.ftmes.bpi.domain.RuleApprovalView;
import com.mapletct.ftmes.bpi.domain.RuleSimulationView;
import com.mapletct.ftmes.bpi.domain.RuleVersionView;
import com.mapletct.ftmes.bpi.domain.TelemetryObservation;
import com.mapletct.ftmes.bpi.domain.TopologyValidationIssue;
import com.mapletct.ftmes.bpi.domain.TopologyVersionView;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Repository
public class RulePostgresRepository {
    private static final String RULE_SELECT = """
            SELECT r.id, r.rule_code, r.version, r.state, r.revision, r.plant_id, r.line_id,
                   r.checksum, r.definition::text AS definition, r.latest_simulation_id,
                   t.topology_code || '@' || t.version AS topology_version,
                   a.id AS approval_id,
                   COALESCE(a.state, 'NOT_REQUESTED') AS approval_status,
                   COALESCE(a.revision, 0) AS approval_revision,
                   a.submitted_by AS approval_submitted_by,
                   a.submitted_at AS approval_submitted_at,
                   a.decided_by AS approval_decided_by,
                   a.decided_at AS approval_decided_at,
                   COALESCE(o.lifecycle_action, 'NOT_PUBLISHED') AS lifecycle_action,
                   COALESCE(o.lifecycle_sequence, 0) AS lifecycle_sequence,
                   COALESCE(o.lifecycle_active, false) AS lifecycle_active,
                   COALESCE(o.status, CASE WHEN r.state IN ('PUBLISHED', 'RETIRED')
                       THEN 'NOT_TRACKED' ELSE 'NOT_PUBLISHED' END) AS publication_status,
                   COALESCE(o.revision, 0) AS publication_revision,
                   COALESCE(o.attempt_count, 0) AS publication_attempt_count,
                   COALESCE(o.total_attempt_count, 0) AS publication_total_attempt_count,
                   COALESCE(o.manual_retry_count, 0) AS publication_manual_retry_count,
                   o.published_at AS publication_published_at,
                   o.last_requeued_at AS publication_last_requeued_at,
                   o.last_error AS publication_last_error,
                   COALESCE(o.application_status, CASE WHEN r.state IN ('PUBLISHED', 'RETIRED')
                       THEN 'NOT_TRACKED' ELSE 'NOT_PUBLISHED' END) AS application_status,
                   o.application_deployment_id,
                   o.application_observed_at,
                   o.application_received_at,
                   o.application_error_code,
                   o.application_error_detail,
                   COALESCE(o.runtime_readiness_status, CASE WHEN r.state IN ('PUBLISHED', 'RETIRED')
                       THEN 'NOT_TRACKED' ELSE 'NOT_PUBLISHED' END) AS runtime_readiness_status,
                   o.runtime_readiness_deployment_id,
                   o.runtime_readiness_observed_at,
                   o.runtime_readiness_received_at,
                   o.runtime_readiness_reason_code,
                   o.runtime_readiness_detail,
                   o.runtime_point_catalog_event_id,
                   o.runtime_point_catalog_source_revision
              FROM bpi.bpi_rule_versions r
              JOIN bpi.bpi_topology_versions t
                ON t.tenant_id = r.tenant_id AND t.id = r.topology_version_id
              LEFT JOIN LATERAL (
                  SELECT approval.id, approval.state, approval.revision,
                         approval.submitted_by, approval.submitted_at,
                         approval.decided_by, approval.decided_at
                    FROM bpi.bpi_rule_approval_requests approval
                   WHERE approval.tenant_id = r.tenant_id
                     AND approval.rule_version_id = r.id
                   ORDER BY approval.submitted_at DESC, approval.id
                   LIMIT 1
              ) a ON true
              LEFT JOIN LATERAL (
                  SELECT lifecycle_event.*
                    FROM bpi.bpi_outbox_events lifecycle_event
                   WHERE lifecycle_event.tenant_id = r.tenant_id
                     AND lifecycle_event.aggregate_type = 'RULE_VERSION'
                     AND lifecycle_event.aggregate_id = r.id
                     AND lifecycle_event.event_type = 'BOUNDARY_RULE_PUBLISHED'
                   ORDER BY lifecycle_event.lifecycle_sequence DESC
                   LIMIT 1
              ) o ON true
            """;
    private static final String TOPOLOGY_SELECT = """
            SELECT id, topology_code, version, state, revision, plant_id, line_id,
                   checksum, definition::text AS definition, validation_status,
                   validation_errors::text AS validation_errors,
                   validation_warnings::text AS validation_warnings,
                   validated_by, validated_at, validated_point_catalog_snapshot_id,
                   validated_point_catalog_checksum, published_by, published_at
              FROM bpi.bpi_topology_versions
            """;

    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public RulePostgresRepository(NamedParameterJdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    public List<TopologyVersionView> listTopologies(
            ActorContext actor, String plantId, String lineId) {
        StringBuilder sql = new StringBuilder(TOPOLOGY_SELECT)
                .append(" WHERE tenant_id = :tenantId AND plant_id IS NOT NULL AND line_id IS NOT NULL");
        MapSqlParameterSource parameters = scope(actor, sql)
                .addValue("tenantId", actor.tenantId());
        addRequestedScope(sql, parameters, plantId, lineId);
        sql.append(" ORDER BY created_at DESC, id");
        return jdbc.query(sql.toString(), parameters, (rs, rowNum) -> mapTopology(rs));
    }

    public TopologyVersionView findTopology(ActorContext actor, UUID topologyId) {
        return findTopology(actor, topologyId, false);
    }

    public TopologyVersionView lockTopology(ActorContext actor, UUID topologyId) {
        return findTopology(actor, topologyId, true);
    }

    private TopologyVersionView findTopology(ActorContext actor, UUID topologyId, boolean lock) {
        try {
            TopologyVersionView topology = jdbc.queryForObject(
                    TOPOLOGY_SELECT + " WHERE tenant_id = :tenantId AND id = :id"
                            + (lock ? " FOR UPDATE" : ""),
                    new MapSqlParameterSource().addValue("tenantId", actor.tenantId()).addValue("id", topologyId),
                    (rs, rowNum) -> mapTopology(rs));
            if (topology == null || topology.plantId() == null || topology.lineId() == null
                    || !actor.canAccess(topology.plantId(), topology.lineId())) {
                throw new BpiNotFoundException("Topology version not found.");
            }
            return topology;
        } catch (EmptyResultDataAccessException exception) {
            throw new BpiNotFoundException("Topology version not found.");
        }
    }

    public TopologyVersionView findPublishedTopologyByRef(
            ActorContext actor, String lineId, String topologyVersion) {
        int separator = topologyVersion.lastIndexOf('@');
        if (separator <= 0 || separator == topologyVersion.length() - 1) {
            throw new BpiNotFoundException("Published topology version not found.");
        }
        try {
            TopologyVersionView topology = jdbc.queryForObject(
                    TOPOLOGY_SELECT + """
                             WHERE tenant_id = :tenantId
                               AND topology_code = :code
                               AND version = :version
                               AND line_id = :lineId
                               AND state = 'PUBLISHED'
                            """, new MapSqlParameterSource().addValue("tenantId", actor.tenantId())
                            .addValue("code", topologyVersion.substring(0, separator))
                            .addValue("version", topologyVersion.substring(separator + 1))
                            .addValue("lineId", lineId),
                    (rs, rowNum) -> mapTopology(rs));
            if (topology == null || !actor.canAccess(topology.plantId(), topology.lineId())) {
                throw new BpiNotFoundException("Published topology version not found.");
            }
            return topology;
        } catch (EmptyResultDataAccessException exception) {
            throw new BpiNotFoundException("Published topology version not found.");
        }
    }

    public void insertTopologyDraft(
            ActorContext actor,
            UUID id,
            String code,
            String version,
            String plantId,
            String lineId,
            String checksum,
            Map<String, Object> definition) {
        try {
            jdbc.update("""
                    INSERT INTO bpi.bpi_topology_versions
                        (id, tenant_id, topology_code, version, state, checksum, definition,
                         plant_id, line_id, revision, created_by, updated_by)
                    VALUES (:id, :tenantId, :code, :version, 'DRAFT', :checksum,
                            CAST(:definition AS jsonb), :plantId, :lineId, 1, :actorId, :actorId)
                    """, new MapSqlParameterSource().addValue("id", id)
                    .addValue("tenantId", actor.tenantId()).addValue("code", code)
                    .addValue("version", version).addValue("checksum", checksum)
                    .addValue("definition", writeJson(definition)).addValue("plantId", plantId)
                    .addValue("lineId", lineId).addValue("actorId", actor.userId()));
        } catch (DataIntegrityViolationException exception) {
            throw new BpiConflictException("Topology code and version already exist.", null);
        }
    }

    public void recordTopologyValidation(
            ActorContext actor,
            UUID topologyId,
            long expectedRevision,
            String checksum,
            UUID pointCatalogSnapshotId,
            String pointCatalogChecksum,
            List<TopologyValidationIssue> errors,
            List<TopologyValidationIssue> warnings) {
        int updated = jdbc.update("""
                UPDATE bpi.bpi_topology_versions
                   SET validation_status = :status,
                       validation_errors = CAST(:errors AS jsonb),
                       validation_warnings = CAST(:warnings AS jsonb),
                       validated_checksum = :checksum,
                       validated_point_catalog_snapshot_id = :pointCatalogSnapshotId,
                       validated_point_catalog_checksum = :pointCatalogChecksum,
                       validated_by = :actorId,
                       validated_at = now(),
                       revision = revision + 1,
                       updated_by = :actorId,
                       updated_at = now()
                 WHERE tenant_id = :tenantId
                   AND id = :id
                   AND revision = :expectedRevision
                   AND state = 'DRAFT'
                """, new MapSqlParameterSource().addValue("status", errors.isEmpty() ? "PASSED" : "FAILED")
                .addValue("errors", writeJson(errors)).addValue("warnings", writeJson(warnings))
                .addValue("checksum", checksum).addValue("actorId", actor.userId())
                .addValue("pointCatalogSnapshotId", pointCatalogSnapshotId)
                .addValue("pointCatalogChecksum", pointCatalogChecksum)
                .addValue("tenantId", actor.tenantId()).addValue("id", topologyId)
                .addValue("expectedRevision", expectedRevision));
        if (updated != 1) {
            throw new BpiConflictException("Topology was changed before validation completed.", expectedRevision);
        }
    }

    public String findTopologyCreator(String tenantId, UUID topologyId) {
        return jdbc.queryForObject("""
                SELECT created_by
                  FROM bpi.bpi_topology_versions
                 WHERE tenant_id = :tenantId AND id = :id
                """, new MapSqlParameterSource().addValue("tenantId", tenantId).addValue("id", topologyId),
                String.class);
    }

    public void publishTopology(
            ActorContext actor, UUID topologyId, long expectedRevision, String checksum) {
        int updated = jdbc.update("""
                UPDATE bpi.bpi_topology_versions AS topology
                   SET state = 'PUBLISHED', revision = revision + 1,
                       published_by = :actorId, published_at = now(),
                       updated_by = :actorId, updated_at = now()
                 WHERE tenant_id = :tenantId
                   AND id = :id
                   AND revision = :expectedRevision
                   AND state = 'DRAFT'
                   AND validation_status = 'PASSED'
                   AND validated_checksum = :checksum
                   AND validated_point_catalog_snapshot_id IS NOT NULL
                   AND validated_point_catalog_checksum IS NOT NULL
                   AND EXISTS (
                       SELECT 1
                         FROM bpi.bpi_point_catalog_snapshots pinned
                        WHERE pinned.tenant_id = topology.tenant_id
                          AND pinned.plant_id = topology.plant_id
                          AND pinned.line_id = topology.line_id
                          AND pinned.id = topology.validated_point_catalog_snapshot_id
                          AND pinned.checksum = topology.validated_point_catalog_checksum
                          AND pinned.id = (
                              SELECT current_snapshot.id
                                FROM bpi.bpi_point_catalog_snapshots current_snapshot
                               WHERE current_snapshot.tenant_id = topology.tenant_id
                                 AND current_snapshot.plant_id = topology.plant_id
                                 AND current_snapshot.line_id = topology.line_id
                               ORDER BY current_snapshot.observed_at DESC,
                                        current_snapshot.imported_at DESC,
                                        current_snapshot.id
                               LIMIT 1
                          )
                   )
                """, new MapSqlParameterSource().addValue("actorId", actor.userId())
                .addValue("tenantId", actor.tenantId()).addValue("id", topologyId)
                .addValue("expectedRevision", expectedRevision).addValue("checksum", checksum));
        if (updated != 1) {
            throw new BpiConflictException(
                    "Topology is not validated against the current point catalog snapshot.", expectedRevision);
        }
    }

    public void insertTopologyAudit(
            ActorContext actor,
            TopologyVersionView topology,
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
                VALUES (:id, :tenantId, :plantId, :lineId, 'TOPOLOGY_VERSION', :objectId, :action, :actorId,
                        :beforeRevision, :afterRevision, :reason, :traceId, CAST(:detail AS jsonb))
                """, new MapSqlParameterSource().addValue("id", UUID.randomUUID())
                .addValue("tenantId", actor.tenantId()).addValue("plantId", topology.plantId())
                .addValue("lineId", topology.lineId()).addValue("objectId", topology.id())
                .addValue("action", action).addValue("actorId", actor.userId())
                .addValue("beforeRevision", beforeRevision).addValue("afterRevision", afterRevision)
                .addValue("reason", reason).addValue("traceId", traceId)
                .addValue("detail", writeJson(detail)));
    }

    public TopologyVersionView findTopologyForRule(ActorContext actor, UUID ruleId) {
        try {
            TopologyVersionView topology = jdbc.queryForObject("""
                    SELECT t.id, t.topology_code, t.version, t.state, t.revision,
                           t.plant_id, t.line_id, t.checksum, t.definition::text AS definition,
                           t.validation_status, t.validation_errors::text AS validation_errors,
                           t.validation_warnings::text AS validation_warnings,
                           t.validated_by, t.validated_at, t.validated_point_catalog_snapshot_id,
                           t.validated_point_catalog_checksum, t.published_by, t.published_at
                      FROM bpi.bpi_rule_versions r
                      JOIN bpi.bpi_topology_versions t
                        ON t.tenant_id = r.tenant_id AND t.id = r.topology_version_id
                     WHERE r.tenant_id = :tenantId AND r.id = :ruleId
                    """, new MapSqlParameterSource().addValue("tenantId", actor.tenantId())
                            .addValue("ruleId", ruleId),
                    (rs, rowNum) -> mapTopology(rs));
            if (topology == null || topology.plantId() == null || topology.lineId() == null
                    || !actor.canAccess(topology.plantId(), topology.lineId())) {
                throw new BpiNotFoundException("Topology version not found.");
            }
            return topology;
        } catch (EmptyResultDataAccessException exception) {
            throw new BpiNotFoundException("Topology version not found.");
        }
    }

    public List<RuleVersionView> listRules(ActorContext actor, String plantId, String lineId) {
        StringBuilder sql = new StringBuilder(RULE_SELECT)
                .append(" WHERE r.tenant_id = :tenantId AND r.plant_id IS NOT NULL AND r.line_id IS NOT NULL");
        MapSqlParameterSource parameters = scope(actor, sql, "r")
                .addValue("tenantId", actor.tenantId());
        addRequestedScope(sql, parameters, plantId, lineId, "r");
        sql.append(" ORDER BY r.created_at DESC, r.id");
        return jdbc.query(sql.toString(), parameters, (rs, rowNum) -> mapRule(rs));
    }

    public RuleVersionView findRule(ActorContext actor, UUID ruleId) {
        return findRule(actor, ruleId, false);
    }

    public RuleVersionView lockRule(ActorContext actor, UUID ruleId) {
        return findRule(actor, ruleId, true);
    }

    public String findRuleCreator(String tenantId, UUID ruleId) {
        return jdbc.queryForObject("""
                SELECT created_by
                  FROM bpi.bpi_rule_versions
                 WHERE tenant_id = :tenantId AND id = :id
                """, new MapSqlParameterSource()
                .addValue("tenantId", tenantId)
                .addValue("id", ruleId), String.class);
    }

    public void lockRuleCodeScope(ActorContext actor, RuleVersionView rule) {
        String lockKey = String.join("|", actor.tenantId(), rule.plantId(), rule.lineId(), rule.code());
        jdbc.query(
                "SELECT pg_advisory_xact_lock(hashtextextended(:lockKey, 0))",
                new MapSqlParameterSource().addValue("lockKey", lockKey),
                rs -> null);
    }

    public void assertRulePublicationHandoffReady(ActorContext actor, RuleVersionView rule) {
        Integer activeVersions = jdbc.queryForObject("""
                SELECT count(*)
                  FROM bpi.bpi_rule_versions existing
                 WHERE existing.tenant_id = :tenantId
                   AND existing.plant_id = :plantId
                   AND existing.line_id = :lineId
                   AND existing.rule_code = :ruleCode
                   AND existing.state = 'PUBLISHED'
                   AND existing.id <> :ruleId
                """, ruleScope(actor, rule), Integer.class);
        if (activeVersions != null && activeVersions > 0) {
            throw new BpiConflictException(
                    "Retire the currently published rule version before publishing its replacement.",
                    rule.revision());
        }

        Integer incompleteRetirements = jdbc.queryForObject("""
                SELECT count(*)
                  FROM bpi.bpi_rule_versions retired
                  LEFT JOIN LATERAL (
                      SELECT lifecycle_event.lifecycle_action,
                             lifecycle_event.status,
                             lifecycle_event.application_status,
                             lifecycle_event.runtime_readiness_status
                        FROM bpi.bpi_outbox_events lifecycle_event
                       WHERE lifecycle_event.tenant_id = retired.tenant_id
                         AND lifecycle_event.aggregate_type = 'RULE_VERSION'
                         AND lifecycle_event.aggregate_id = retired.id
                         AND lifecycle_event.event_type = 'BOUNDARY_RULE_PUBLISHED'
                       ORDER BY lifecycle_event.lifecycle_sequence DESC
                       LIMIT 1
                  ) lifecycle ON true
                 WHERE retired.tenant_id = :tenantId
                   AND retired.plant_id = :plantId
                   AND retired.line_id = :lineId
                   AND retired.rule_code = :ruleCode
                   AND retired.state = 'RETIRED'
                   AND retired.id <> :ruleId
                   AND (lifecycle.lifecycle_action IS DISTINCT FROM 'RETIRE'
                        OR lifecycle.status IS DISTINCT FROM 'PUBLISHED'
                        OR lifecycle.application_status IS DISTINCT FROM 'APPLIED'
                        OR lifecycle.runtime_readiness_status IS DISTINCT FROM 'INACTIVE')
                """, ruleScope(actor, rule), Integer.class);
        if (incompleteRetirements != null && incompleteRetirements > 0) {
            throw new BpiConflictException(
                    "The previous rule retirement must reach Kafka PUBLISHED, Flink APPLIED and runtime INACTIVE before replacement publication.",
                    rule.revision());
        }
    }

    public void insertRuleDraft(
            ActorContext actor,
            UUID id,
            String code,
            String version,
            TopologyVersionView topology,
            String checksum,
            Map<String, Object> ast) {
        try {
            jdbc.update("""
                    INSERT INTO bpi.bpi_rule_versions
                        (id, tenant_id, rule_code, version, topology_version_id, state,
                         checksum, definition, revision, plant_id, line_id, created_by, updated_by)
                    VALUES (:id, :tenantId, :code, :version, :topologyId, 'DRAFT',
                            :checksum, CAST(:definition AS jsonb), 1, :plantId, :lineId, :actorId, :actorId)
                    """, new MapSqlParameterSource().addValue("id", id)
                    .addValue("tenantId", actor.tenantId()).addValue("code", code)
                    .addValue("version", version).addValue("topologyId", topology.id())
                    .addValue("checksum", checksum).addValue("definition", writeJson(ast))
                    .addValue("plantId", topology.plantId()).addValue("lineId", topology.lineId())
                    .addValue("actorId", actor.userId()));
        } catch (DataIntegrityViolationException exception) {
            throw new BpiConflictException("Rule code and version already exist.", null);
        }
    }

    private RuleVersionView findRule(ActorContext actor, UUID ruleId, boolean lock) {
        try {
            RuleVersionView rule = jdbc.queryForObject(
                    RULE_SELECT + " WHERE r.tenant_id = :tenantId AND r.id = :id"
                            + (lock ? " FOR UPDATE OF r" : ""),
                    new MapSqlParameterSource().addValue("tenantId", actor.tenantId()).addValue("id", ruleId),
                    (rs, rowNum) -> mapRule(rs));
            if (rule == null || rule.plantId() == null || rule.lineId() == null
                    || !actor.canAccess(rule.plantId(), rule.lineId())) {
                throw new BpiNotFoundException("Rule version not found.");
            }
            return rule;
        } catch (EmptyResultDataAccessException exception) {
            throw new BpiNotFoundException("Rule version not found.");
        }
    }

    public List<TelemetryObservation> findObservations(
            ActorContext actor,
            String plantId,
            String lineId,
            Instant from,
            Instant to,
            String calibrationVersion,
            Set<String> signals) {
        if (signals.isEmpty()) return List.of();
        return jdbc.query("""
                SELECT p.event_id, p.property_id, p.numeric_value, p.boolean_value,
                       p.quality_code, p.sample_time
                  FROM bpi.bpi_telemetry_points p
                  JOIN bpi.bpi_telemetry_events e
                    ON e.tenant_id = p.tenant_id AND e.id = p.telemetry_event_id
                 WHERE p.tenant_id = :tenantId
                   AND e.plant_id = :plantId
                   AND e.line_id = :lineId
                   AND p.sample_time >= :fromTime
                   AND p.sample_time <= :toTime
                   AND p.property_id IN (:signals)
                   AND p.calibration_version = :calibrationVersion
                 ORDER BY p.sample_time, p.event_id, p.property_id
                 LIMIT 100001
                """, new MapSqlParameterSource()
                        .addValue("tenantId", actor.tenantId()).addValue("plantId", plantId)
                        .addValue("lineId", lineId).addValue("fromTime", Timestamp.from(from))
                        .addValue("toTime", Timestamp.from(to)).addValue("signals", signals)
                        .addValue("calibrationVersion", calibrationVersion),
                (rs, rowNum) -> new TelemetryObservation(
                        rs.getString("event_id"), rs.getString("property_id"),
                        rs.getBigDecimal("numeric_value"), rs.getObject("boolean_value", Boolean.class),
                        rs.getString("quality_code"), rs.getTimestamp("sample_time").toInstant()));
    }

    public List<GoldenBoundary> findGoldenBoundaries(
            ActorContext actor,
            String plantId,
            String lineId,
            String goldenSetId,
            String boundaryType,
            Instant from,
            Instant to) {
        return jdbc.query("""
                SELECT boundary_time, tolerance_seconds
                  FROM bpi.bpi_rule_golden_boundaries
                 WHERE tenant_id = :tenantId
                   AND plant_id = :plantId
                   AND line_id = :lineId
                   AND golden_set_id = :goldenSetId
                   AND boundary_type = :boundaryType
                   AND boundary_time >= :fromTime
                   AND boundary_time <= :toTime
                 ORDER BY boundary_time
                """, new MapSqlParameterSource().addValue("tenantId", actor.tenantId())
                        .addValue("plantId", plantId).addValue("lineId", lineId)
                        .addValue("goldenSetId", goldenSetId).addValue("boundaryType", boundaryType)
                        .addValue("fromTime", Timestamp.from(from)).addValue("toTime", Timestamp.from(to)),
                (rs, rowNum) -> new GoldenBoundary(
                        rs.getTimestamp("boundary_time").toInstant(), rs.getInt("tolerance_seconds")));
    }

    public void insertSimulation(
            ActorContext actor,
            RuleSimulationView simulation,
            RuleVersionView rule) {
        jdbc.update("""
                INSERT INTO bpi.bpi_rule_simulations
                    (id, tenant_id, plant_id, line_id, rule_version_id, state, checksum,
                     input_manifest, metrics, emitted_boundaries, failure_reason, created_by)
                VALUES (:id, :tenantId, :plantId, :lineId, :ruleId, :state, :checksum,
                        CAST(:inputManifest AS jsonb), CAST(:metrics AS jsonb),
                        CAST(:emittedBoundaries AS jsonb), :failureReason, :createdBy)
                """, new MapSqlParameterSource().addValue("id", simulation.id())
                .addValue("tenantId", actor.tenantId()).addValue("plantId", rule.plantId())
                .addValue("lineId", rule.lineId()).addValue("ruleId", rule.id())
                .addValue("state", simulation.state()).addValue("checksum", simulation.checksum())
                .addValue("inputManifest", writeJson(simulation.inputManifest()))
                .addValue("metrics", writeJson(simulation.metrics()))
                .addValue("emittedBoundaries", writeJson(simulation.emittedBoundaries()))
                .addValue("failureReason", simulation.failureReason()).addValue("createdBy", actor.userId()));
    }

    public void recordSimulationResult(
            String tenantId, UUID ruleId, long expectedRevision, UUID simulationId, boolean passed, String actorId) {
        int updated = jdbc.update("""
                UPDATE bpi.bpi_rule_versions
                   SET state = :state, revision = revision + 1,
                       latest_simulation_id = :simulationId,
                       updated_by = :actorId, updated_at = now()
                 WHERE tenant_id = :tenantId AND id = :ruleId
                   AND revision = :expectedRevision AND state IN ('DRAFT', 'SIMULATION_PASSED')
                """, new MapSqlParameterSource().addValue("state", passed ? "SIMULATION_PASSED" : "DRAFT")
                .addValue("simulationId", simulationId).addValue("actorId", actorId)
                .addValue("tenantId", tenantId).addValue("ruleId", ruleId)
                .addValue("expectedRevision", expectedRevision));
        if (updated != 1) throw new BpiConflictException("Rule was changed before simulation completed.", expectedRevision);
    }

    public RuleSimulationView findSimulation(ActorContext actor, UUID simulationId) {
        try {
            RuleSimulationView simulation = jdbc.queryForObject("""
                    SELECT id, rule_version_id, plant_id, line_id, state, checksum,
                           input_manifest::text AS input_manifest, metrics::text AS metrics,
                           emitted_boundaries::text AS emitted_boundaries, failure_reason
                      FROM bpi.bpi_rule_simulations
                     WHERE tenant_id = :tenantId AND id = :id
                    """, new MapSqlParameterSource().addValue("tenantId", actor.tenantId())
                            .addValue("id", simulationId),
                    (rs, rowNum) -> {
                        String plantId = rs.getString("plant_id");
                        String lineId = rs.getString("line_id");
                        if (!actor.canAccess(plantId, lineId)) return null;
                        return new RuleSimulationView(
                                rs.getObject("id", UUID.class), rs.getObject("rule_version_id", UUID.class),
                                rs.getString("state"), rs.getString("checksum"),
                                readMap(rs.getString("metrics")), readMap(rs.getString("input_manifest")),
                                readInstants(rs.getString("emitted_boundaries")), rs.getString("failure_reason"));
                    });
            if (simulation == null) throw new BpiNotFoundException("Rule simulation not found.");
            return simulation;
        } catch (EmptyResultDataAccessException exception) {
            throw new BpiNotFoundException("Rule simulation not found.");
        }
    }

    public RuleApprovalView submitRuleApproval(
            ActorContext actor,
            RuleVersionView rule,
            RuleSimulationView simulation,
            String reason) {
        UUID approvalId = UUID.randomUUID();
        int updated = jdbc.update("""
                UPDATE bpi.bpi_rule_versions
                   SET state = 'PENDING_APPROVAL', revision = revision + 1,
                       updated_by = :actorId, updated_at = now()
                 WHERE tenant_id = :tenantId AND id = :ruleId
                   AND revision = :expectedRevision
                   AND state = 'SIMULATION_PASSED'
                   AND latest_simulation_id = :simulationId
                """, new MapSqlParameterSource()
                .addValue("actorId", actor.userId())
                .addValue("tenantId", actor.tenantId())
                .addValue("ruleId", rule.id())
                .addValue("expectedRevision", rule.revision())
                .addValue("simulationId", simulation.id()));
        if (updated != 1) {
            throw new BpiConflictException("Rule is not ready to submit for approval.", rule.revision());
        }
        jdbc.update("""
                INSERT INTO bpi.bpi_rule_approval_requests
                    (id, tenant_id, rule_version_id, simulation_id, simulation_checksum,
                     state, revision, submitted_by, submit_reason)
                VALUES (:id, :tenantId, :ruleId, :simulationId, :simulationChecksum,
                        'PENDING', 1, :actorId, :reason)
                """, new MapSqlParameterSource()
                .addValue("id", approvalId)
                .addValue("tenantId", actor.tenantId())
                .addValue("ruleId", rule.id())
                .addValue("simulationId", simulation.id())
                .addValue("simulationChecksum", simulation.checksum())
                .addValue("actorId", actor.userId())
                .addValue("reason", reason));
        return lockPendingApproval(actor, rule.id());
    }

    public RuleApprovalView lockPendingApproval(ActorContext actor, UUID ruleId) {
        try {
            RuleApprovalView approval = jdbc.queryForObject("""
                    SELECT id, rule_version_id, simulation_id, simulation_checksum, state, revision,
                           submitted_by, submitted_at, submit_reason,
                           decided_by, decided_at, decision_reason
                      FROM bpi.bpi_rule_approval_requests
                     WHERE tenant_id = :tenantId
                       AND rule_version_id = :ruleId
                       AND state = 'PENDING'
                     FOR UPDATE
                    """, new MapSqlParameterSource()
                    .addValue("tenantId", actor.tenantId())
                    .addValue("ruleId", ruleId),
                    (rs, rowNum) -> mapApproval(rs));
            if (approval == null) throw new BpiNotFoundException("Pending rule approval not found.");
            return approval;
        } catch (EmptyResultDataAccessException exception) {
            throw new BpiNotFoundException("Pending rule approval not found.");
        }
    }

    public void approveRule(
            ActorContext actor,
            RuleVersionView rule,
            RuleApprovalView approval,
            String reason) {
        int approvalUpdated = jdbc.update("""
                UPDATE bpi.bpi_rule_approval_requests
                   SET state = 'APPROVED', revision = revision + 1,
                       decided_by = :actorId, decided_at = now(),
                       decision_reason = :reason, updated_at = now()
                 WHERE tenant_id = :tenantId AND id = :approvalId
                   AND revision = :approvalRevision AND state = 'PENDING'
                """, new MapSqlParameterSource()
                .addValue("actorId", actor.userId())
                .addValue("reason", reason)
                .addValue("tenantId", actor.tenantId())
                .addValue("approvalId", approval.id())
                .addValue("approvalRevision", approval.revision()));
        if (approvalUpdated != 1) {
            throw new BpiConflictException("Rule approval was already decided.", approval.revision());
        }
        int ruleUpdated;
        try {
            ruleUpdated = jdbc.update("""
                    UPDATE bpi.bpi_rule_versions
                       SET state = 'PUBLISHED', revision = revision + 1,
                           updated_by = :actorId, updated_at = now()
                     WHERE tenant_id = :tenantId AND id = :ruleId
                       AND revision = :expectedRevision
                       AND state = 'PENDING_APPROVAL'
                       AND latest_simulation_id = :simulationId
                    """, new MapSqlParameterSource()
                    .addValue("actorId", actor.userId())
                    .addValue("tenantId", actor.tenantId())
                    .addValue("ruleId", rule.id())
                    .addValue("expectedRevision", rule.revision())
                    .addValue("simulationId", approval.simulationId()));
        } catch (DataIntegrityViolationException exception) {
            throw new BpiConflictException(
                    "Another version of this rule is already published in the same scope.",
                    rule.revision());
        }
        if (ruleUpdated != 1) {
            throw new BpiConflictException("Rule is not ready for approval.", rule.revision());
        }
    }

    public void retireRule(ActorContext actor, RuleVersionView rule) {
        int updated = jdbc.update("""
                UPDATE bpi.bpi_rule_versions
                   SET state = 'RETIRED', revision = revision + 1,
                       updated_by = :actorId, updated_at = now()
                 WHERE tenant_id = :tenantId AND id = :ruleId
                   AND revision = :expectedRevision
                   AND state = 'PUBLISHED'
                """, new MapSqlParameterSource()
                .addValue("actorId", actor.userId())
                .addValue("tenantId", actor.tenantId())
                .addValue("ruleId", rule.id())
                .addValue("expectedRevision", rule.revision()));
        if (updated != 1) {
            throw new BpiConflictException("Rule can no longer be retired.", rule.revision());
        }
    }

    public void rejectRuleApproval(
            ActorContext actor,
            RuleVersionView rule,
            RuleApprovalView approval,
            String reason) {
        int approvalUpdated = jdbc.update("""
                UPDATE bpi.bpi_rule_approval_requests
                   SET state = 'REJECTED', revision = revision + 1,
                       decided_by = :actorId, decided_at = now(),
                       decision_reason = :reason, updated_at = now()
                 WHERE tenant_id = :tenantId AND id = :approvalId
                   AND revision = :approvalRevision AND state = 'PENDING'
                """, new MapSqlParameterSource()
                .addValue("actorId", actor.userId())
                .addValue("reason", reason)
                .addValue("tenantId", actor.tenantId())
                .addValue("approvalId", approval.id())
                .addValue("approvalRevision", approval.revision()));
        if (approvalUpdated != 1) {
            throw new BpiConflictException("Rule approval was already decided.", approval.revision());
        }
        int ruleUpdated = jdbc.update("""
                UPDATE bpi.bpi_rule_versions
                   SET state = 'DRAFT', revision = revision + 1,
                       updated_by = :actorId, updated_at = now()
                 WHERE tenant_id = :tenantId AND id = :ruleId
                   AND revision = :expectedRevision AND state = 'PENDING_APPROVAL'
                """, new MapSqlParameterSource()
                .addValue("actorId", actor.userId())
                .addValue("tenantId", actor.tenantId())
                .addValue("ruleId", rule.id())
                .addValue("expectedRevision", rule.revision()));
        if (ruleUpdated != 1) {
            throw new BpiConflictException("Rule is not pending approval.", rule.revision());
        }
    }

    public void insertRuleAudit(
            ActorContext actor,
            RuleVersionView rule,
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
                VALUES (:id, :tenantId, :plantId, :lineId, 'RULE_VERSION', :objectId, :action, :actorId,
                        :beforeRevision, :afterRevision, :reason, :traceId, CAST(:detail AS jsonb))
                """, new MapSqlParameterSource().addValue("id", UUID.randomUUID())
                .addValue("tenantId", actor.tenantId()).addValue("plantId", rule.plantId())
                .addValue("lineId", rule.lineId()).addValue("objectId", rule.id())
                .addValue("action", action).addValue("actorId", actor.userId())
                .addValue("beforeRevision", beforeRevision).addValue("afterRevision", afterRevision)
                .addValue("reason", reason).addValue("traceId", traceId)
                .addValue("detail", writeJson(detail)));
    }

    private TopologyVersionView mapTopology(java.sql.ResultSet rs) throws java.sql.SQLException {
        Timestamp validatedAt = rs.getTimestamp("validated_at");
        Timestamp publishedAt = rs.getTimestamp("published_at");
        return new TopologyVersionView(
                rs.getObject("id", UUID.class), rs.getString("topology_code"), rs.getString("version"),
                rs.getString("state"), rs.getLong("revision"), rs.getString("plant_id"),
                rs.getString("line_id"), rs.getString("checksum"), readMap(rs.getString("definition")),
                rs.getString("validation_status"), readIssues(rs.getString("validation_errors")),
                readIssues(rs.getString("validation_warnings")), rs.getString("validated_by"),
                validatedAt == null ? null : validatedAt.toInstant(),
                rs.getObject("validated_point_catalog_snapshot_id", UUID.class),
                rs.getString("validated_point_catalog_checksum"), rs.getString("published_by"),
                publishedAt == null ? null : publishedAt.toInstant());
    }

    private RuleVersionView mapRule(java.sql.ResultSet rs) throws java.sql.SQLException {
        Timestamp publicationPublishedAt = rs.getTimestamp("publication_published_at");
        Timestamp publicationLastRequeuedAt = rs.getTimestamp("publication_last_requeued_at");
        Timestamp applicationObservedAt = rs.getTimestamp("application_observed_at");
        Timestamp applicationReceivedAt = rs.getTimestamp("application_received_at");
        Timestamp runtimeReadinessObservedAt = rs.getTimestamp("runtime_readiness_observed_at");
        Timestamp runtimeReadinessReceivedAt = rs.getTimestamp("runtime_readiness_received_at");
        Timestamp approvalSubmittedAt = rs.getTimestamp("approval_submitted_at");
        Timestamp approvalDecidedAt = rs.getTimestamp("approval_decided_at");
        return new RuleVersionView(
                rs.getObject("id", UUID.class), rs.getString("rule_code"), rs.getString("version"),
                rs.getString("state"), rs.getLong("revision"), rs.getString("plant_id"),
                rs.getString("line_id"), rs.getString("topology_version"), rs.getString("checksum"),
                readMap(rs.getString("definition")), rs.getObject("latest_simulation_id", UUID.class),
                rs.getObject("approval_id", UUID.class), rs.getString("approval_status"),
                rs.getLong("approval_revision"), rs.getString("approval_submitted_by"),
                approvalSubmittedAt == null ? null : approvalSubmittedAt.toInstant(),
                rs.getString("approval_decided_by"),
                approvalDecidedAt == null ? null : approvalDecidedAt.toInstant(),
                rs.getString("lifecycle_action"), rs.getLong("lifecycle_sequence"),
                rs.getBoolean("lifecycle_active"),
                rs.getString("publication_status"), rs.getLong("publication_revision"),
                rs.getInt("publication_attempt_count"), rs.getInt("publication_total_attempt_count"),
                rs.getInt("publication_manual_retry_count"),
                publicationPublishedAt == null ? null : publicationPublishedAt.toInstant(),
                publicationLastRequeuedAt == null ? null : publicationLastRequeuedAt.toInstant(),
                rs.getString("publication_last_error"), rs.getString("application_status"),
                rs.getString("application_deployment_id"),
                applicationObservedAt == null ? null : applicationObservedAt.toInstant(),
                applicationReceivedAt == null ? null : applicationReceivedAt.toInstant(),
                rs.getString("application_error_code"), rs.getString("application_error_detail"),
                rs.getString("runtime_readiness_status"),
                rs.getString("runtime_readiness_deployment_id"),
                runtimeReadinessObservedAt == null ? null : runtimeReadinessObservedAt.toInstant(),
                runtimeReadinessReceivedAt == null ? null : runtimeReadinessReceivedAt.toInstant(),
                rs.getString("runtime_readiness_reason_code"),
                rs.getString("runtime_readiness_detail"),
                rs.getString("runtime_point_catalog_event_id"),
                rs.getString("runtime_point_catalog_source_revision"));
    }

    private MapSqlParameterSource ruleScope(ActorContext actor, RuleVersionView rule) {
        return new MapSqlParameterSource()
                .addValue("tenantId", actor.tenantId())
                .addValue("plantId", rule.plantId())
                .addValue("lineId", rule.lineId())
                .addValue("ruleCode", rule.code())
                .addValue("ruleId", rule.id());
    }

    private RuleApprovalView mapApproval(java.sql.ResultSet rs) throws java.sql.SQLException {
        Timestamp submittedAt = rs.getTimestamp("submitted_at");
        Timestamp decidedAt = rs.getTimestamp("decided_at");
        return new RuleApprovalView(
                rs.getObject("id", UUID.class),
                rs.getObject("rule_version_id", UUID.class),
                rs.getObject("simulation_id", UUID.class),
                rs.getString("simulation_checksum"),
                rs.getString("state"),
                rs.getLong("revision"),
                rs.getString("submitted_by"),
                submittedAt == null ? null : submittedAt.toInstant(),
                rs.getString("submit_reason"),
                rs.getString("decided_by"),
                decidedAt == null ? null : decidedAt.toInstant(),
                rs.getString("decision_reason"));
    }

    private MapSqlParameterSource scope(ActorContext actor, StringBuilder sql) {
        return scope(actor, sql, "");
    }

    private MapSqlParameterSource scope(ActorContext actor, StringBuilder sql, String alias) {
        String prefix = alias.isBlank() ? "" : alias + ".";
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        if (!actor.plantIds().contains("*")) {
            if (actor.plantIds().isEmpty()) sql.append(" AND 1 = 0");
            else {
                sql.append(" AND ").append(prefix).append("plant_id IN (:actorPlantIds)");
                parameters.addValue("actorPlantIds", actor.plantIds());
            }
        }
        if (!actor.lineIds().contains("*")) {
            if (actor.lineIds().isEmpty()) sql.append(" AND 1 = 0");
            else {
                sql.append(" AND ").append(prefix).append("line_id IN (:actorLineIds)");
                parameters.addValue("actorLineIds", actor.lineIds());
            }
        }
        return parameters;
    }

    private void addRequestedScope(
            StringBuilder sql, MapSqlParameterSource parameters, String plantId, String lineId) {
        addRequestedScope(sql, parameters, plantId, lineId, "");
    }

    private void addRequestedScope(
            StringBuilder sql,
            MapSqlParameterSource parameters,
            String plantId,
            String lineId,
            String alias) {
        String prefix = alias.isBlank() ? "" : alias + ".";
        if (plantId != null && !plantId.isBlank()) {
            sql.append(" AND ").append(prefix).append("plant_id = :plantId");
            parameters.addValue("plantId", plantId);
        }
        if (lineId != null && !lineId.isBlank()) {
            sql.append(" AND ").append(prefix).append("line_id = :lineId");
            parameters.addValue("lineId", lineId);
        }
    }

    private Map<String, Object> readMap(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception exception) {
            throw new IllegalStateException("Could not read BPI rule JSON", exception);
        }
    }

    private List<TopologyValidationIssue> readIssues(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<TopologyValidationIssue>>() {});
        } catch (Exception exception) {
            throw new IllegalStateException("Could not read BPI topology validation JSON", exception);
        }
    }

    private List<Instant> readInstants(String json) {
        try {
            List<String> values = objectMapper.readValue(json, new TypeReference<List<String>>() {});
            List<Instant> result = new ArrayList<>();
            for (String value : values) result.add(Instant.parse(value));
            return List.copyOf(result);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not read BPI simulation boundaries", exception);
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not write BPI rule JSON", exception);
        }
    }
}
