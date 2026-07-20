package com.mapletct.ftmes.bpi.infrastructure.postgres;

import com.mapletct.ftmes.bpi.application.ActorContext;
import com.mapletct.ftmes.bpi.application.error.BpiConflictException;
import com.mapletct.ftmes.bpi.application.error.BpiNotFoundException;
import com.mapletct.ftmes.bpi.domain.PointCalibrationView;
import com.mapletct.ftmes.bpi.interfaces.rest.PointCalibrationSubmitCommand;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public class PointCalibrationPostgresRepository {
    private static final String SELECT = """
            SELECT id, plant_id, line_id, product_id, device_id, property_id,
                   calibration_version, certificate_reference, certificate_checksum,
                   valid_from, valid_until, state, revision, submitted_by, submitted_at,
                   submit_reason, decided_by, decided_at, decision_reason,
                   revoked_by, revoked_at, revoke_reason
              FROM bpi.bpi_point_calibrations
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public PointCalibrationPostgresRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Instant currentTransactionTime() {
        Timestamp value = jdbc.queryForObject(
                "SELECT transaction_timestamp()",
                new MapSqlParameterSource(),
                Timestamp.class);
        if (value == null) {
            throw new IllegalStateException("PostgreSQL did not return transaction_timestamp().");
        }
        return value.toInstant();
    }

    public List<PointCalibrationView> list(
            ActorContext actor,
            String plantId,
            String lineId,
            String productId,
            String deviceId,
            String propertyId,
            Instant snapshotAt,
            Instant cursorSubmittedAt,
            UUID cursorId,
            int fetchLimit) {
        StringBuilder sql = new StringBuilder(SELECT)
                .append(" WHERE tenant_id = :tenantId AND plant_id = :plantId AND line_id = :lineId")
                .append(" AND submitted_at <= :snapshotAt");
        MapSqlParameterSource parameters = new MapSqlParameterSource("tenantId", actor.tenantId())
                .addValue("plantId", plantId)
                .addValue("lineId", lineId)
                .addValue("snapshotAt", Timestamp.from(snapshotAt))
                .addValue("fetchLimit", fetchLimit);
        addFilter(sql, parameters, "product_id", "productId", productId);
        addFilter(sql, parameters, "device_id", "deviceId", deviceId);
        addFilter(sql, parameters, "property_id", "propertyId", propertyId);
        if (cursorSubmittedAt != null && cursorId != null) {
            sql.append(" AND (submitted_at < :cursorSubmittedAt")
                    .append(" OR (submitted_at = :cursorSubmittedAt AND id < :cursorId))");
            parameters.addValue("cursorSubmittedAt", Timestamp.from(cursorSubmittedAt))
                    .addValue("cursorId", cursorId);
        }
        sql.append(" ORDER BY submitted_at DESC, id DESC LIMIT :fetchLimit");
        return jdbc.query(sql.toString(), parameters, (rs, rowNum) -> map(rs));
    }

    public PointCalibrationView find(ActorContext actor, UUID calibrationId) {
        List<PointCalibrationView> values = jdbc.query(
                SELECT + " WHERE tenant_id = :tenantId AND id = :id",
                new MapSqlParameterSource("tenantId", actor.tenantId()).addValue("id", calibrationId),
                (rs, rowNum) -> map(rs));
        PointCalibrationView value = values.stream().findFirst()
                .orElseThrow(() -> new BpiNotFoundException("Point calibration not found."));
        assertScope(actor, value);
        return value;
    }

    public PointCalibrationView lock(ActorContext actor, UUID calibrationId) {
        List<PointCalibrationView> values = jdbc.query(
                SELECT + " WHERE tenant_id = :tenantId AND id = :id FOR UPDATE",
                new MapSqlParameterSource("tenantId", actor.tenantId()).addValue("id", calibrationId),
                (rs, rowNum) -> map(rs));
        PointCalibrationView value = values.stream().findFirst()
                .orElseThrow(() -> new BpiNotFoundException("Point calibration not found."));
        assertScope(actor, value);
        return value;
    }

    public Set<UUID> lockEffectiveEvidence(
            ActorContext actor,
            Set<UUID> calibrationIds,
            Instant requiredAt) {
        if (calibrationIds.isEmpty()) return Set.of();
        return Set.copyOf(jdbc.query("""
                SELECT id
                  FROM bpi.bpi_point_calibrations
                 WHERE tenant_id = :tenantId
                   AND id IN (:calibrationIds)
                   AND state = 'APPROVED'
                   AND valid_from <= :requiredAt
                   AND valid_until > :requiredAt
                 FOR SHARE
                """, new MapSqlParameterSource("tenantId", actor.tenantId())
                .addValue("calibrationIds", calibrationIds)
                .addValue("requiredAt", Timestamp.from(requiredAt)),
                (rs, rowNum) -> rs.getObject("id", UUID.class)));
    }

    public Optional<String> findUnsafeRuntimeRuleDependency(
            ActorContext actor,
            PointCalibrationView calibration) {
        List<String> dependencies = jdbc.query("""
                SELECT rule.rule_code || '@' || rule.version AS rule_reference
                  FROM bpi.bpi_rule_versions rule
                  JOIN bpi.bpi_topology_versions topology
                    ON topology.tenant_id = rule.tenant_id
                   AND topology.id = rule.topology_version_id
                  CROSS JOIN LATERAL jsonb_array_elements(
                      CASE
                          WHEN jsonb_typeof(topology.definition -> 'bindings') = 'array'
                              THEN topology.definition -> 'bindings'
                          ELSE '[]'::jsonb
                      END
                  ) binding
                  LEFT JOIN LATERAL (
                      SELECT lifecycle_event.lifecycle_action,
                             lifecycle_event.status,
                             lifecycle_event.application_status,
                             lifecycle_event.runtime_readiness_status
                        FROM bpi.bpi_outbox_events lifecycle_event
                       WHERE lifecycle_event.tenant_id = rule.tenant_id
                         AND lifecycle_event.aggregate_type = 'RULE_VERSION'
                         AND lifecycle_event.aggregate_id = rule.id
                         AND lifecycle_event.event_type = 'BOUNDARY_RULE_PUBLISHED'
                       ORDER BY lifecycle_event.lifecycle_sequence DESC
                       LIMIT 1
                  ) lifecycle ON true
                 WHERE rule.tenant_id = :tenantId
                   AND COALESCE(rule.plant_id, topology.plant_id) = :plantId
                   AND COALESCE(rule.line_id, topology.line_id) = :lineId
                   AND rule.state IN ('PUBLISHED', 'RETIRED')
                   AND binding ->> 'productId' = :productId
                   AND binding ->> 'deviceId' = :deviceId
                   AND binding ->> 'propertyId' = :propertyId
                   AND binding ->> 'calibrationVersion' = :calibrationVersion
                   AND (
                       rule.state = 'PUBLISHED'
                       OR lifecycle.lifecycle_action IS DISTINCT FROM 'RETIRE'
                       OR lifecycle.status IS DISTINCT FROM 'PUBLISHED'
                       OR lifecycle.application_status IS DISTINCT FROM 'APPLIED'
                       OR lifecycle.runtime_readiness_status IS DISTINCT FROM 'INACTIVE'
                   )
                 ORDER BY rule.created_at, rule.id
                 LIMIT 1
                """, new MapSqlParameterSource("tenantId", actor.tenantId())
                .addValue("plantId", calibration.plantId())
                .addValue("lineId", calibration.lineId())
                .addValue("productId", calibration.productId())
                .addValue("deviceId", calibration.deviceId())
                .addValue("propertyId", calibration.propertyId())
                .addValue("calibrationVersion", calibration.calibrationVersion()),
                (rs, rowNum) -> rs.getString("rule_reference"));
        return dependencies.stream().findFirst();
    }

    public void insertPending(
            ActorContext actor,
            UUID id,
            PointCalibrationSubmitCommand command) {
        try {
            jdbc.update("""
                    INSERT INTO bpi.bpi_point_calibrations
                        (id, tenant_id, plant_id, line_id, product_id, device_id, property_id,
                         calibration_version, certificate_reference, certificate_checksum,
                         valid_from, valid_until, state, revision, submitted_by, submit_reason)
                    VALUES (:id, :tenantId, :plantId, :lineId, :productId, :deviceId, :propertyId,
                            :calibrationVersion, :certificateReference, :certificateChecksum,
                            :validFrom, :validUntil, 'PENDING', 1, :actorId, :reason)
                    """, new MapSqlParameterSource("id", id)
                    .addValue("tenantId", actor.tenantId())
                    .addValue("plantId", command.plantId()).addValue("lineId", command.lineId())
                    .addValue("productId", command.productId()).addValue("deviceId", command.deviceId())
                    .addValue("propertyId", command.propertyId())
                    .addValue("calibrationVersion", command.calibrationVersion())
                    .addValue("certificateReference", command.certificateReference())
                    .addValue("certificateChecksum", command.certificateChecksum())
                    .addValue("validFrom", Timestamp.from(command.validFrom()))
                    .addValue("validUntil", Timestamp.from(command.validUntil()))
                    .addValue("actorId", actor.userId()).addValue("reason", command.reason()));
        } catch (DataIntegrityViolationException exception) {
            throw new BpiConflictException(
                    "A calibration record already exists for this point and version.", null);
        }
    }

    public void decide(
            ActorContext actor,
            UUID calibrationId,
            String state,
            long expectedRevision,
            String reason) {
        int updated = jdbc.update("""
                UPDATE bpi.bpi_point_calibrations
                   SET state = :state,
                       revision = revision + 1,
                       decided_by = :actorId,
                       decided_at = now(),
                       decision_reason = :reason,
                       updated_at = now()
                 WHERE tenant_id = :tenantId
                   AND id = :id
                   AND state = 'PENDING'
                   AND revision = :expectedRevision
                """, new MapSqlParameterSource("tenantId", actor.tenantId())
                .addValue("id", calibrationId).addValue("state", state)
                .addValue("actorId", actor.userId()).addValue("reason", reason)
                .addValue("expectedRevision", expectedRevision));
        if (updated != 1) {
            throw new BpiConflictException("Point calibration changed before the decision.", expectedRevision);
        }
    }

    public void revoke(
            ActorContext actor,
            UUID calibrationId,
            long expectedRevision,
            String reason) {
        int updated = jdbc.update("""
                UPDATE bpi.bpi_point_calibrations
                   SET state = 'REVOKED',
                       revision = revision + 1,
                       revoked_by = :actorId,
                       revoked_at = now(),
                       revoke_reason = :reason,
                       updated_at = now()
                 WHERE tenant_id = :tenantId
                   AND id = :id
                   AND state = 'APPROVED'
                   AND revision = :expectedRevision
                """, new MapSqlParameterSource("tenantId", actor.tenantId())
                .addValue("id", calibrationId).addValue("actorId", actor.userId())
                .addValue("reason", reason).addValue("expectedRevision", expectedRevision));
        if (updated != 1) {
            throw new BpiConflictException("Point calibration changed before revocation.", expectedRevision);
        }
    }

    public void insertAudit(
            ActorContext actor,
            PointCalibrationView calibration,
            String action,
            long beforeRevision,
            long afterRevision,
            String reason,
            String traceId) {
        jdbc.update("""
                INSERT INTO bpi.bpi_audit_events
                    (id, tenant_id, plant_id, line_id, object_type, object_id, action, actor_id,
                     before_revision, after_revision, reason, trace_id, detail)
                VALUES (:id, :tenantId, :plantId, :lineId, 'POINT_CALIBRATION', :objectId,
                        :action, :actorId, :beforeRevision, :afterRevision, :reason, :traceId,
                        jsonb_build_object(
                            'productId', :productId,
                            'deviceId', :deviceId,
                            'propertyId', :propertyId,
                            'calibrationVersion', :calibrationVersion,
                            'certificateChecksum', :certificateChecksum,
                            'state', :state,
                            'effective', :effective))
                """, new MapSqlParameterSource("id", UUID.randomUUID())
                .addValue("tenantId", actor.tenantId())
                .addValue("plantId", calibration.plantId()).addValue("lineId", calibration.lineId())
                .addValue("objectId", calibration.id()).addValue("action", action)
                .addValue("actorId", actor.userId()).addValue("beforeRevision", beforeRevision)
                .addValue("afterRevision", afterRevision).addValue("reason", reason)
                .addValue("traceId", traceId).addValue("productId", calibration.productId())
                .addValue("deviceId", calibration.deviceId()).addValue("propertyId", calibration.propertyId())
                .addValue("calibrationVersion", calibration.calibrationVersion())
                .addValue("certificateChecksum", calibration.certificateChecksum())
                .addValue("state", calibration.state()).addValue("effective", calibration.effective()));
    }

    private PointCalibrationView map(ResultSet rs) throws SQLException {
        String state = rs.getString("state");
        Instant validFrom = rs.getTimestamp("valid_from").toInstant();
        Instant validUntil = rs.getTimestamp("valid_until").toInstant();
        String effectivenessStatus = effectivenessStatus(state, validFrom, validUntil, Instant.now());
        return new PointCalibrationView(
                rs.getObject("id", UUID.class), rs.getString("plant_id"), rs.getString("line_id"),
                rs.getString("product_id"), rs.getString("device_id"), rs.getString("property_id"),
                rs.getString("calibration_version"), rs.getString("certificate_reference"),
                rs.getString("certificate_checksum"), validFrom, validUntil, state,
                rs.getLong("revision"), rs.getString("submitted_by"),
                rs.getTimestamp("submitted_at").toInstant(), rs.getString("submit_reason"),
                rs.getString("decided_by"), instant(rs, "decided_at"), rs.getString("decision_reason"),
                rs.getString("revoked_by"), instant(rs, "revoked_at"), rs.getString("revoke_reason"),
                "EFFECTIVE".equals(effectivenessStatus), effectivenessStatus);
    }

    private String effectivenessStatus(String state, Instant validFrom, Instant validUntil, Instant now) {
        if (!"APPROVED".equals(state)) return state;
        if (now.isBefore(validFrom)) return "NOT_YET_EFFECTIVE";
        if (!now.isBefore(validUntil)) return "EXPIRED";
        return "EFFECTIVE";
    }

    private Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }

    private void assertScope(ActorContext actor, PointCalibrationView value) {
        if (!actor.canAccess(value.plantId(), value.lineId())) {
            throw new BpiNotFoundException("Point calibration not found.");
        }
    }

    private void addFilter(
            StringBuilder sql,
            MapSqlParameterSource parameters,
            String column,
            String parameter,
            String value) {
        if (value == null || value.isBlank()) return;
        sql.append(" AND ").append(column).append(" = :").append(parameter);
        parameters.addValue(parameter, value);
    }
}
