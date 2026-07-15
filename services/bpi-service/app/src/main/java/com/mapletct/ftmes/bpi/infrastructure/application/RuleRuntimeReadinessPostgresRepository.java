package com.mapletct.ftmes.bpi.infrastructure.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mapletct.ftmes.bpi.application.error.BpiConflictException;
import com.mapletct.ftmes.bpi.application.error.BpiNotFoundException;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryRuleRuntimeReadinessV1;
import com.mapletct.ftmes.bpi.domain.RuleRuntimeReadinessTarget;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.Map;
import java.util.UUID;

@Repository
public class RuleRuntimeReadinessPostgresRepository {
    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public RuleRuntimeReadinessPostgresRepository(
            NamedParameterJdbcTemplate jdbc,
            ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    public RuleRuntimeReadinessTarget lockTarget(String tenantId, UUID publicationId) {
        try {
            RuleRuntimeReadinessTarget target = jdbc.queryForObject("""
                    SELECT o.id AS publication_id, o.aggregate_id AS rule_id,
                           o.tenant_id, o.plant_id, o.line_id, o.status AS publication_status,
                           o.revision AS publication_revision, o.runtime_readiness_status,
                           o.runtime_readiness_event_id, o.runtime_readiness_observed_at,
                           o.runtime_readiness_reason_code,
                           r.rule_code, r.version AS rule_version, r.checksum AS rule_checksum
                      FROM bpi.bpi_outbox_events o
                      JOIN bpi.bpi_rule_versions r
                        ON r.tenant_id = o.tenant_id AND r.id = o.aggregate_id
                     WHERE o.tenant_id = :tenantId
                       AND o.id = :publicationId
                       AND o.aggregate_type = 'RULE_VERSION'
                       AND o.event_type = 'BOUNDARY_RULE_PUBLISHED'
                     FOR UPDATE OF o
                    """, new MapSqlParameterSource()
                    .addValue("tenantId", tenantId)
                    .addValue("publicationId", publicationId),
                    (rs, rowNum) -> {
                        Timestamp observedAt = rs.getTimestamp("runtime_readiness_observed_at");
                        return new RuleRuntimeReadinessTarget(
                                rs.getObject("publication_id", UUID.class),
                                rs.getObject("rule_id", UUID.class),
                                rs.getString("tenant_id"), rs.getString("plant_id"),
                                rs.getString("line_id"), rs.getString("rule_code"),
                                rs.getString("rule_version"), rs.getString("rule_checksum"),
                                rs.getString("publication_status"), rs.getLong("publication_revision"),
                                rs.getString("runtime_readiness_status"),
                                rs.getString("runtime_readiness_event_id"),
                                observedAt == null ? null : observedAt.toInstant(),
                                rs.getString("runtime_readiness_reason_code"));
                    });
            if (target == null) throw new BpiNotFoundException("Rule publication event not found.");
            return target;
        } catch (EmptyResultDataAccessException error) {
            throw new BpiNotFoundException("Rule publication event not found.");
        }
    }

    public long updateReadiness(
            RuleRuntimeReadinessTarget target,
            BoundaryRuleRuntimeReadinessV1 event) {
        int updated = jdbc.update("""
                UPDATE bpi.bpi_outbox_events
                   SET runtime_readiness_status = :status,
                       runtime_readiness_event_id = :eventId,
                       runtime_readiness_deployment_id = :deploymentId,
                       runtime_readiness_observed_at = :observedAt,
                       runtime_readiness_received_at = now(),
                       runtime_readiness_reason_code = :reasonCode,
                       runtime_readiness_detail = :detail,
                       runtime_point_catalog_event_id = :pointCatalogEventId,
                       runtime_point_catalog_source_revision = :pointCatalogSourceRevision,
                       revision = revision + 1,
                       updated_at = now()
                 WHERE tenant_id = :tenantId
                   AND id = :publicationId
                   AND revision = :expectedRevision
                   AND runtime_readiness_status = :expectedStatus
                """, new MapSqlParameterSource()
                .addValue("status", event.getStatus().name())
                .addValue("eventId", event.getEventId())
                .addValue("deploymentId", event.getDeploymentId())
                .addValue("observedAt", new Timestamp(event.getObservedAtMs()))
                .addValue("reasonCode", blankToNull(event.getReasonCode()))
                .addValue("detail", blankToNull(event.getDetail()))
                .addValue("pointCatalogEventId", blankToNull(event.getPointCatalogEventId()))
                .addValue("pointCatalogSourceRevision", blankToNull(event.getPointCatalogSourceRevision()))
                .addValue("tenantId", target.tenantId())
                .addValue("publicationId", target.publicationId())
                .addValue("expectedRevision", target.publicationRevision())
                .addValue("expectedStatus", target.runtimeReadinessStatus()));
        if (updated != 1) {
            throw new BpiConflictException(
                    "Rule runtime readiness changed concurrently.", target.publicationRevision());
        }
        return target.publicationRevision() + 1;
    }

    public void insertAudit(
            RuleRuntimeReadinessTarget target,
            BoundaryRuleRuntimeReadinessV1 event,
            long afterRevision,
            String traceId) {
        jdbc.update("""
                INSERT INTO bpi.bpi_audit_events
                    (id, tenant_id, plant_id, line_id, object_type, object_id, action, actor_id,
                     before_revision, after_revision, reason, trace_id, detail)
                VALUES (:id, :tenantId, :plantId, :lineId, 'RULE_PUBLICATION', :objectId,
                        :action, :actorId, :beforeRevision, :afterRevision, :reason,
                        :traceId, CAST(:detail AS jsonb))
                """, new MapSqlParameterSource()
                .addValue("id", UUID.randomUUID())
                .addValue("tenantId", target.tenantId())
                .addValue("plantId", target.plantId())
                .addValue("lineId", target.lineId())
                .addValue("objectId", target.publicationId())
                .addValue("action", "RULE_RUNTIME_" + event.getStatus().name())
                .addValue("actorId", "flink:" + event.getDeploymentId())
                .addValue("beforeRevision", target.publicationRevision())
                .addValue("afterRevision", afterRevision)
                .addValue("reason", blankToNull(limit(event.getDetail(), 500)))
                .addValue("traceId", traceId)
                .addValue("detail", writeJson(Map.of(
                        "runtimeReadinessEventId", event.getEventId(),
                        "publicationEventId", event.getPublicationEventId(),
                        "deploymentId", event.getDeploymentId(),
                        "status", event.getStatus().name(),
                        "reasonCode", event.getReasonCode(),
                        "checksum", event.getChecksum(),
                        "observedAtMs", event.getObservedAtMs(),
                        "pointCatalogEventId", event.getPointCatalogEventId(),
                        "pointCatalogSourceRevision", event.getPointCatalogSourceRevision()))));
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception error) {
            throw new IllegalStateException("Could not write rule runtime-readiness audit JSON", error);
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static String limit(String value, int maximum) {
        return value == null || value.length() <= maximum ? value : value.substring(0, maximum);
    }
}
