package com.mapletct.ftmes.bpi.infrastructure.postgres;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mapletct.ftmes.bpi.application.ActorContext;
import com.mapletct.ftmes.bpi.application.error.BpiConflictException;
import com.mapletct.ftmes.bpi.application.error.BpiNotFoundException;
import com.mapletct.ftmes.bpi.domain.GoldenBoundary;
import com.mapletct.ftmes.bpi.domain.RuleSimulationView;
import com.mapletct.ftmes.bpi.domain.RuleVersionView;
import com.mapletct.ftmes.bpi.domain.TelemetryObservation;
import com.mapletct.ftmes.bpi.domain.TopologyVersionView;
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
                   COALESCE(o.status, CASE WHEN r.state = 'PUBLISHED'
                       THEN 'NOT_TRACKED' ELSE 'NOT_PUBLISHED' END) AS publication_status,
                   COALESCE(o.revision, 0) AS publication_revision,
                   COALESCE(o.attempt_count, 0) AS publication_attempt_count,
                   COALESCE(o.total_attempt_count, 0) AS publication_total_attempt_count,
                   COALESCE(o.manual_retry_count, 0) AS publication_manual_retry_count,
                   o.published_at AS publication_published_at,
                   o.last_requeued_at AS publication_last_requeued_at,
                   o.last_error AS publication_last_error
              FROM bpi.bpi_rule_versions r
              JOIN bpi.bpi_topology_versions t
                ON t.tenant_id = r.tenant_id AND t.id = r.topology_version_id
              LEFT JOIN bpi.bpi_outbox_events o
                ON o.tenant_id = r.tenant_id
               AND o.aggregate_type = 'RULE_VERSION'
               AND o.aggregate_id = r.id
               AND o.event_type = 'BOUNDARY_RULE_PUBLISHED'
            """;
    private static final String TOPOLOGY_SELECT = """
            SELECT id, topology_code, version, state, revision, plant_id, line_id,
                   checksum, definition::text AS definition
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
        return jdbc.query(sql.toString(), parameters, (rs, rowNum) -> new TopologyVersionView(
                rs.getObject("id", UUID.class), rs.getString("topology_code"), rs.getString("version"),
                rs.getString("state"), rs.getLong("revision"), rs.getString("plant_id"),
                rs.getString("line_id"), rs.getString("checksum"), readMap(rs.getString("definition"))));
    }

    public TopologyVersionView findTopology(ActorContext actor, UUID topologyId) {
        try {
            TopologyVersionView topology = jdbc.queryForObject(
                    TOPOLOGY_SELECT + " WHERE tenant_id = :tenantId AND id = :id",
                    new MapSqlParameterSource().addValue("tenantId", actor.tenantId()).addValue("id", topologyId),
                    (rs, rowNum) -> new TopologyVersionView(
                            rs.getObject("id", UUID.class), rs.getString("topology_code"), rs.getString("version"),
                            rs.getString("state"), rs.getLong("revision"), rs.getString("plant_id"),
                            rs.getString("line_id"), rs.getString("checksum"), readMap(rs.getString("definition"))));
            if (topology == null || topology.plantId() == null || topology.lineId() == null
                    || !actor.canAccess(topology.plantId(), topology.lineId())) {
                throw new BpiNotFoundException("Topology version not found.");
            }
            return topology;
        } catch (EmptyResultDataAccessException exception) {
            throw new BpiNotFoundException("Topology version not found.");
        }
    }

    public TopologyVersionView findTopologyForRule(ActorContext actor, UUID ruleId) {
        try {
            TopologyVersionView topology = jdbc.queryForObject("""
                    SELECT t.id, t.topology_code, t.version, t.state, t.revision,
                           t.plant_id, t.line_id, t.checksum, t.definition::text AS definition
                      FROM bpi.bpi_rule_versions r
                      JOIN bpi.bpi_topology_versions t
                        ON t.tenant_id = r.tenant_id AND t.id = r.topology_version_id
                     WHERE r.tenant_id = :tenantId AND r.id = :ruleId
                    """, new MapSqlParameterSource().addValue("tenantId", actor.tenantId())
                            .addValue("ruleId", ruleId),
                    (rs, rowNum) -> new TopologyVersionView(
                            rs.getObject("id", UUID.class), rs.getString("topology_code"),
                            rs.getString("version"), rs.getString("state"), rs.getLong("revision"),
                            rs.getString("plant_id"), rs.getString("line_id"), rs.getString("checksum"),
                            readMap(rs.getString("definition"))));
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

    public void publishRule(
            String tenantId, UUID ruleId, long expectedRevision, UUID simulationId, String actorId) {
        int updated = jdbc.update("""
                UPDATE bpi.bpi_rule_versions
                   SET state = 'PUBLISHED', revision = revision + 1,
                       updated_by = :actorId, updated_at = now()
                 WHERE tenant_id = :tenantId AND id = :ruleId
                   AND revision = :expectedRevision
                   AND state = 'SIMULATION_PASSED'
                   AND latest_simulation_id = :simulationId
                """, new MapSqlParameterSource().addValue("tenantId", tenantId).addValue("ruleId", ruleId)
                .addValue("expectedRevision", expectedRevision).addValue("simulationId", simulationId)
                .addValue("actorId", actorId));
        if (updated != 1) throw new BpiConflictException("Rule is not ready for publication.", expectedRevision);
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

    private RuleVersionView mapRule(java.sql.ResultSet rs) throws java.sql.SQLException {
        Timestamp publicationPublishedAt = rs.getTimestamp("publication_published_at");
        Timestamp publicationLastRequeuedAt = rs.getTimestamp("publication_last_requeued_at");
        return new RuleVersionView(
                rs.getObject("id", UUID.class), rs.getString("rule_code"), rs.getString("version"),
                rs.getString("state"), rs.getLong("revision"), rs.getString("plant_id"),
                rs.getString("line_id"), rs.getString("topology_version"), rs.getString("checksum"),
                readMap(rs.getString("definition")), rs.getObject("latest_simulation_id", UUID.class),
                rs.getString("publication_status"), rs.getLong("publication_revision"),
                rs.getInt("publication_attempt_count"), rs.getInt("publication_total_attempt_count"),
                rs.getInt("publication_manual_retry_count"),
                publicationPublishedAt == null ? null : publicationPublishedAt.toInstant(),
                publicationLastRequeuedAt == null ? null : publicationLastRequeuedAt.toInstant(),
                rs.getString("publication_last_error"));
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
