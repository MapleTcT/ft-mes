package com.mapletct.ftmes.bpi.infrastructure.postgres;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mapletct.ftmes.bpi.application.ActorContext;
import com.mapletct.ftmes.bpi.application.error.BpiConflictException;
import com.mapletct.ftmes.bpi.domain.FeatureFlagRecord;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
public class FeatureFlagPostgresRepository {
    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public FeatureFlagPostgresRepository(
            NamedParameterJdbcTemplate jdbc,
            ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    public List<FeatureFlagRecord> listRelevant(
            String tenantId,
            List<String> flagKeys) {
        return jdbc.query("""
                SELECT id, tenant_id, scope_type, scope_key, flag_key, enabled, active,
                       revision, updated_by, updated_at, last_reason
                  FROM bpi.bpi_feature_flags
                 WHERE tenant_id IN (:tenantId, '*')
                   AND flag_key IN (:flagKeys)
                 ORDER BY flag_key, tenant_id, scope_type, scope_key
                """, new MapSqlParameterSource().addValue("tenantId", tenantId)
                .addValue("flagKeys", flagKeys), (rs, rowNum) -> map(rs));
    }

    public FeatureFlagRecord lockOverride(
            String tenantId,
            String scopeType,
            String scopeKey,
            String flagKey) {
        List<FeatureFlagRecord> rows = jdbc.query("""
                SELECT id, tenant_id, scope_type, scope_key, flag_key, enabled, active,
                       revision, updated_by, updated_at, last_reason
                  FROM bpi.bpi_feature_flags
                 WHERE tenant_id = :tenantId
                   AND scope_type = :scopeType
                   AND scope_key = :scopeKey
                   AND flag_key = :flagKey
                 FOR UPDATE
                """, parameters(tenantId, scopeType, scopeKey, flagKey),
                (rs, rowNum) -> map(rs));
        return rows.isEmpty() ? null : rows.get(0);
    }

    public FeatureFlagRecord insertOverride(
            ActorContext actor,
            UUID id,
            String scopeType,
            String scopeKey,
            String flagKey,
            boolean enabled,
            String reason) {
        try {
            jdbc.update("""
                    INSERT INTO bpi.bpi_feature_flags
                        (id, tenant_id, scope_type, scope_key, flag_key, enabled, active,
                         revision, updated_by, last_reason)
                    VALUES (:id, :tenantId, :scopeType, :scopeKey, :flagKey, :enabled, true,
                            1, :actorId, :reason)
                    """, parameters(actor.tenantId(), scopeType, scopeKey, flagKey)
                    .addValue("id", id).addValue("enabled", enabled)
                    .addValue("actorId", actor.userId()).addValue("reason", reason));
        } catch (DataIntegrityViolationException exception) {
            throw new BpiConflictException(
                    "Feature flag override was created concurrently; refresh before retrying.", null);
        }
        return lockOverride(actor.tenantId(), scopeType, scopeKey, flagKey);
    }

    public FeatureFlagRecord updateOverride(
            ActorContext actor,
            FeatureFlagRecord current,
            long expectedRevision,
            boolean enabled,
            boolean active,
            String reason) {
        int updated = jdbc.update("""
                UPDATE bpi.bpi_feature_flags
                   SET enabled = :enabled, active = :active, revision = revision + 1,
                       updated_by = :actorId, updated_at = now(), last_reason = :reason
                 WHERE id = :id AND tenant_id = :tenantId AND revision = :expectedRevision
                """, new MapSqlParameterSource().addValue("enabled", enabled)
                .addValue("active", active).addValue("actorId", actor.userId())
                .addValue("reason", reason).addValue("id", current.id())
                .addValue("tenantId", actor.tenantId()).addValue("expectedRevision", expectedRevision));
        if (updated != 1) {
            throw new BpiConflictException(
                    "Feature flag override changed before the command completed.", current.revision());
        }
        return lockOverride(actor.tenantId(), current.scopeType(), current.scopeKey(), current.flagKey());
    }

    public void insertAudit(
            ActorContext actor,
            String plantId,
            String lineId,
            FeatureFlagRecord before,
            FeatureFlagRecord after,
            String action,
            String reason,
            String traceId) {
        Map<String, Object> detail = Map.of(
                "flagKey", after.flagKey(),
                "scopeType", after.scopeType(),
                "scopeKey", after.scopeKey(),
                "beforeEnabled", before == null ? "INHERITED" : before.enabled(),
                "beforeActive", before == null ? "INHERITED" : before.active(),
                "afterEnabled", after.enabled(),
                "afterActive", after.active());
        jdbc.update("""
                INSERT INTO bpi.bpi_audit_events
                    (id, tenant_id, plant_id, line_id, object_type, object_id, action, actor_id,
                     before_revision, after_revision, reason, trace_id, detail)
                VALUES (:id, :tenantId, :plantId, :lineId, 'FEATURE_FLAG', :objectId, :action, :actorId,
                        :beforeRevision, :afterRevision, :reason, :traceId, CAST(:detail AS jsonb))
                """, new MapSqlParameterSource().addValue("id", UUID.randomUUID())
                .addValue("tenantId", actor.tenantId()).addValue("plantId", plantId)
                .addValue("lineId", lineId).addValue("objectId", after.id())
                .addValue("action", action).addValue("actorId", actor.userId())
                .addValue("beforeRevision", before == null ? 0 : before.revision())
                .addValue("afterRevision", after.revision()).addValue("reason", reason)
                .addValue("traceId", traceId).addValue("detail", writeJson(detail)));
    }

    private MapSqlParameterSource parameters(
            String tenantId,
            String scopeType,
            String scopeKey,
            String flagKey) {
        return new MapSqlParameterSource().addValue("tenantId", tenantId)
                .addValue("scopeType", scopeType).addValue("scopeKey", scopeKey)
                .addValue("flagKey", flagKey);
    }

    private FeatureFlagRecord map(ResultSet rs) throws SQLException {
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        return new FeatureFlagRecord(
                rs.getObject("id", UUID.class), rs.getString("tenant_id"),
                rs.getString("scope_type"), rs.getString("scope_key"),
                rs.getString("flag_key"), rs.getBoolean("enabled"),
                rs.getBoolean("active"), rs.getLong("revision"),
                rs.getString("updated_by"),
                updatedAt == null ? Instant.EPOCH : updatedAt.toInstant(),
                rs.getString("last_reason"));
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not serialize feature flag audit detail", exception);
        }
    }
}
