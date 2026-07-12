package com.mapletct.ftmes.bpi.infrastructure.postgres;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mapletct.ftmes.bpi.application.ActorContext;
import com.mapletct.ftmes.bpi.application.VersionRefs;
import com.mapletct.ftmes.bpi.application.error.BpiConflictException;
import com.mapletct.ftmes.bpi.application.error.BpiNotFoundException;
import com.mapletct.ftmes.bpi.application.error.BpiValidationException;
import com.mapletct.ftmes.bpi.domain.BatchCandidate;
import com.mapletct.ftmes.bpi.domain.BatchInstance;
import com.mapletct.ftmes.bpi.domain.BatchState;
import com.mapletct.ftmes.bpi.domain.BatchStateEvent;
import com.mapletct.ftmes.bpi.domain.BoundaryType;
import com.mapletct.ftmes.bpi.domain.CandidateState;
import com.mapletct.ftmes.bpi.domain.EvidenceView;
import com.mapletct.ftmes.bpi.domain.ReviewView;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
public class BpiPostgresRepository {
    private static final String CANDIDATE_SELECT = """
            SELECT c.*, r.rule_code || '@' || r.version AS rule_version_ref,
                   t.topology_code || '@' || t.version AS topology_version_ref
              FROM bpi.bpi_batch_candidates c
              JOIN bpi.bpi_rule_versions r ON r.id = c.rule_version_id
              JOIN bpi.bpi_topology_versions t ON t.id = c.topology_version_id
            """;
    private static final String BATCH_SELECT = """
            SELECT b.*, r.rule_code || '@' || r.version AS rule_version_ref,
                   t.topology_code || '@' || t.version AS topology_version_ref
              FROM bpi.bpi_batch_instances b
              JOIN bpi.bpi_rule_versions r ON r.id = b.rule_version_id
              JOIN bpi.bpi_topology_versions t ON t.id = b.topology_version_id
            """;

    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public BpiPostgresRepository(NamedParameterJdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    public VersionRefs resolveVersions(
            String tenantId,
            String topologyCode,
            String topologyVersion,
            String ruleCode,
            String ruleVersion) {
        String sql = """
                SELECT t.id AS topology_id, r.id AS rule_id
                  FROM bpi.bpi_topology_versions t
                  JOIN bpi.bpi_rule_versions r ON r.topology_version_id = t.id
                 WHERE t.tenant_id = :tenantId
                   AND t.topology_code = :topologyCode
                   AND t.version = :topologyVersion
                   AND t.state = 'PUBLISHED'
                   AND r.tenant_id = :tenantId
                   AND r.rule_code = :ruleCode
                   AND r.version = :ruleVersion
                   AND r.state = 'PUBLISHED'
                """;
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("tenantId", tenantId)
                .addValue("topologyCode", topologyCode)
                .addValue("topologyVersion", topologyVersion)
                .addValue("ruleCode", ruleCode)
                .addValue("ruleVersion", ruleVersion);
        try {
            return jdbc.queryForObject(sql, parameters, (rs, rowNum) ->
                    new VersionRefs(rs.getObject("topology_id", UUID.class), rs.getObject("rule_id", UUID.class)));
        } catch (EmptyResultDataAccessException exception) {
            throw new BpiValidationException("Published topology/rule version pair does not exist.");
        }
    }

    public boolean recordInbox(
            UUID id,
            String tenantId,
            String source,
            String idempotencyKey,
            String eventId,
            String payloadChecksum) {
        int inserted = jdbc.update("""
                INSERT INTO bpi.bpi_inbox_events
                    (id, tenant_id, source, idempotency_key, event_id, payload_checksum, processed_at)
                VALUES (:id, :tenantId, :source, :key, :eventId, :checksum, now())
                ON CONFLICT DO NOTHING
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("tenantId", tenantId)
                .addValue("source", source)
                .addValue("key", idempotencyKey)
                .addValue("eventId", eventId)
                .addValue("checksum", payloadChecksum));
        if (inserted == 1) {
            return true;
        }
        List<Map<String, Object>> existing = jdbc.queryForList("""
                SELECT idempotency_key, event_id, payload_checksum
                  FROM bpi.bpi_inbox_events
                 WHERE tenant_id = :tenantId
                   AND source = :source
                   AND (idempotency_key = :key OR event_id = :eventId)
                """, new MapSqlParameterSource()
                .addValue("tenantId", tenantId)
                .addValue("source", source)
                .addValue("key", idempotencyKey)
                .addValue("eventId", eventId));
        boolean exactReplay = existing.size() == 1
                && idempotencyKey.equals(existing.get(0).get("idempotency_key"))
                && eventId.equals(existing.get(0).get("event_id"))
                && payloadChecksum.equals(existing.get(0).get("payload_checksum"));
        if (!exactReplay) {
            throw new BpiConflictException(
                    "Candidate key or event ID was replayed with different identity or payload.", null);
        }
        return false;
    }

    public void insertCandidate(
            UUID id,
            UUID candidateKey,
            ActorContext actor,
            String plantId,
            String lineId,
            BoundaryType boundaryType,
            String orderId,
            Instant boundaryTime,
            BigDecimal confidence,
            VersionRefs versions,
            List<EvidenceView> evidence,
            List<String> missingSignals) {
        jdbc.update("""
                INSERT INTO bpi.bpi_batch_candidates
                    (id, candidate_key, tenant_id, plant_id, line_id, boundary_type, order_id,
                     boundary_time, state, revision, confidence, topology_version_id, rule_version_id,
                     evidence, missing_signals)
                VALUES (:id, :candidateKey, :tenantId, :plantId, :lineId, :boundaryType, :orderId,
                        :boundaryTime, 'PENDING', 1, :confidence, :topologyId, :ruleId,
                        CAST(:evidence AS jsonb), CAST(:missingSignals AS jsonb))
                ON CONFLICT (tenant_id, candidate_key) DO NOTHING
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("candidateKey", candidateKey)
                .addValue("tenantId", actor.tenantId())
                .addValue("plantId", plantId)
                .addValue("lineId", lineId)
                .addValue("boundaryType", boundaryType.name())
                .addValue("orderId", orderId)
                .addValue("boundaryTime", Timestamp.from(boundaryTime))
                .addValue("confidence", confidence)
                .addValue("topologyId", versions.topologyVersionId())
                .addValue("ruleId", versions.ruleVersionId())
                .addValue("evidence", writeJson(evidence))
                .addValue("missingSignals", writeJson(missingSignals)));
    }

    public List<BatchCandidate> listCandidates(
            ActorContext actor,
            String plantId,
            String lineId,
            CandidateState state,
            int limit) {
        StringBuilder sql = new StringBuilder(CANDIDATE_SELECT)
                .append(" WHERE c.tenant_id = :tenantId");
        MapSqlParameterSource parameters = scopedParameters(actor, sql, "c")
                .addValue("limit", Math.min(Math.max(limit, 1), 200));
        if (plantId != null && !plantId.isBlank()) {
            sql.append(" AND c.plant_id = :plantId");
            parameters.addValue("plantId", plantId);
        }
        if (lineId != null && !lineId.isBlank()) {
            sql.append(" AND c.line_id = :lineId");
            parameters.addValue("lineId", lineId);
        }
        if (state != null) {
            sql.append(" AND c.state = :state");
            parameters.addValue("state", state.name());
        }
        sql.append(" ORDER BY c.boundary_time DESC, c.id LIMIT :limit");
        return jdbc.query(sql.toString(), parameters, candidateRowMapper());
    }

    public BatchCandidate findCandidate(ActorContext actor, UUID id) {
        return findPersistedCandidate(actor, id, false).candidate();
    }

    public PersistedCandidate lockCandidate(ActorContext actor, UUID id) {
        return findPersistedCandidate(actor, id, true);
    }

    public BatchCandidate findCandidateByKey(ActorContext actor, UUID candidateKey) {
        String sql = CANDIDATE_SELECT + " WHERE c.tenant_id = :tenantId AND c.candidate_key = :candidateKey";
        try {
            return jdbc.queryForObject(sql, new MapSqlParameterSource()
                    .addValue("tenantId", actor.tenantId())
                    .addValue("candidateKey", candidateKey), candidateRowMapper());
        } catch (EmptyResultDataAccessException exception) {
            throw new BpiNotFoundException("Candidate not found.");
        }
    }

    public boolean commandsEnabled(ActorContext actor, BatchCandidate candidate) {
        List<Boolean> matches = jdbc.query("""
                SELECT enabled
                  FROM bpi.bpi_feature_flags
                 WHERE flag_key = 'bpi.commands'
                   AND tenant_id IN (:tenantId, '*')
                   AND (
                        (tenant_id = :tenantId AND scope_type = 'LINE' AND scope_key = :lineId)
                     OR (tenant_id = :tenantId AND scope_type = 'PLANT' AND scope_key = :plantId)
                     OR (tenant_id = :tenantId AND scope_type = 'TENANT' AND scope_key = :tenantId)
                     OR (tenant_id = :tenantId AND scope_type = 'GLOBAL' AND scope_key = '*')
                     OR (tenant_id = '*' AND scope_type = 'GLOBAL' AND scope_key = '*')
                   )
                 ORDER BY CASE
                     WHEN tenant_id = :tenantId AND scope_type = 'LINE' THEN 50
                     WHEN tenant_id = :tenantId AND scope_type = 'PLANT' THEN 40
                     WHEN tenant_id = :tenantId AND scope_type = 'TENANT' THEN 30
                     WHEN tenant_id = :tenantId AND scope_type = 'GLOBAL' THEN 20
                     ELSE 10
                 END DESC
                 LIMIT 1
                """, new MapSqlParameterSource()
                .addValue("tenantId", actor.tenantId())
                .addValue("plantId", candidate.plantId())
                .addValue("lineId", candidate.lineId()),
                (rs, rowNum) -> rs.getBoolean("enabled"));
        return !matches.isEmpty() && matches.get(0);
    }

    private PersistedCandidate findPersistedCandidate(ActorContext actor, UUID id, boolean lock) {
        String sql = CANDIDATE_SELECT + " WHERE c.tenant_id = :tenantId AND c.id = :id" + (lock ? " FOR UPDATE OF c" : "");
        try {
            return jdbc.queryForObject(sql, new MapSqlParameterSource()
                            .addValue("tenantId", actor.tenantId()).addValue("id", id),
                    (rs, rowNum) -> new PersistedCandidate(
                            mapCandidate(rs),
                            rs.getObject("topology_version_id", UUID.class),
                            rs.getObject("rule_version_id", UUID.class)));
        } catch (EmptyResultDataAccessException exception) {
            throw new BpiNotFoundException("Candidate not found.");
        }
    }

    public boolean reserveIdempotency(
            UUID id,
            String tenantId,
            String key,
            String method,
            String path,
            String requestChecksum) {
        return jdbc.update("""
                INSERT INTO bpi.bpi_api_idempotency
                    (id, tenant_id, idempotency_key, method, resource_path, request_checksum, state)
                VALUES (:id, :tenantId, :key, :method, :path, :checksum, 'PROCESSING')
                ON CONFLICT (tenant_id, idempotency_key) DO NOTHING
                """, new MapSqlParameterSource()
                .addValue("id", id).addValue("tenantId", tenantId).addValue("key", key)
                .addValue("method", method).addValue("path", path).addValue("checksum", requestChecksum)) == 1;
    }

    public IdempotencyRecord lockIdempotency(String tenantId, String key) {
        return jdbc.queryForObject("""
                SELECT method, resource_path, request_checksum, state,
                       response_status, response_body::text AS response_body
                  FROM bpi.bpi_api_idempotency
                 WHERE tenant_id = :tenantId AND idempotency_key = :key
                 FOR UPDATE
                """, new MapSqlParameterSource().addValue("tenantId", tenantId).addValue("key", key),
                (rs, rowNum) -> new IdempotencyRecord(
                        rs.getString("method"), rs.getString("resource_path"),
                        rs.getString("request_checksum"), rs.getString("state"),
                        rs.getObject("response_status", Integer.class), rs.getString("response_body")));
    }

    public void completeIdempotency(String tenantId, String key, int status, String responseBody) {
        jdbc.update("""
                UPDATE bpi.bpi_api_idempotency
                   SET state = 'COMPLETED', response_status = :status,
                       response_body = CAST(:body AS jsonb), completed_at = now()
                 WHERE tenant_id = :tenantId AND idempotency_key = :key
                """, new MapSqlParameterSource().addValue("status", status).addValue("body", responseBody)
                .addValue("tenantId", tenantId).addValue("key", key));
    }

    public void insertBatch(BatchInstance batch, PersistedCandidate source, String actorId) {
        jdbc.update("""
                INSERT INTO bpi.bpi_batch_instances
                    (id, tenant_id, plant_id, batch_no, line_id, stage_code, order_id, material_code,
                     state, revision, is_shadow, start_time, end_time, quantity, quantity_unit,
                     dry_matter, quality_gate, wms_status, topology_version_id, rule_version_id, created_by)
                VALUES (:id, :tenantId, :plantId, :batchNo, :lineId, :stageCode, :orderId, :materialCode,
                        :state, :revision, :shadow, :startTime, :endTime, :quantity, :quantityUnit,
                        :dryMatter, :qualityGate, :wmsStatus, :topologyId, :ruleId, :actorId)
                """, batchParameters(batch)
                .addValue("topologyId", source.topologyVersionId())
                .addValue("ruleId", source.ruleVersionId())
                .addValue("actorId", actorId));
    }

    public void confirmCandidate(UUID candidateId, long expectedRevision, UUID batchId, String actorId, String reason) {
        int updated = jdbc.update("""
                UPDATE bpi.bpi_batch_candidates
                   SET state = 'CONFIRMED', revision = revision + 1, batch_id = :batchId,
                       reviewed_by = :actorId, review_reason = :reason, reviewed_at = now(), updated_at = now()
                 WHERE id = :candidateId AND revision = :expectedRevision AND state = 'PENDING'
                """, new MapSqlParameterSource().addValue("batchId", batchId).addValue("actorId", actorId)
                .addValue("reason", reason).addValue("candidateId", candidateId)
                .addValue("expectedRevision", expectedRevision));
        if (updated != 1) {
            throw new BpiConflictException("Candidate was changed by another command.", expectedRevision);
        }
    }

    public void rejectCandidate(UUID candidateId, long expectedRevision, String actorId, String reason) {
        int updated = jdbc.update("""
                UPDATE bpi.bpi_batch_candidates
                   SET state = 'REJECTED', revision = revision + 1, batch_id = NULL,
                       reviewed_by = :actorId, review_reason = :reason, reviewed_at = now(), updated_at = now()
                 WHERE id = :candidateId AND revision = :expectedRevision AND state = 'PENDING'
                """, new MapSqlParameterSource().addValue("actorId", actorId)
                .addValue("reason", reason).addValue("candidateId", candidateId)
                .addValue("expectedRevision", expectedRevision));
        if (updated != 1) {
            throw new BpiConflictException("Candidate was changed by another command.", expectedRevision);
        }
    }

    public void insertEvidence(String tenantId, UUID batchId, BoundaryType boundaryType, List<EvidenceView> evidence) {
        for (EvidenceView item : evidence) {
            jdbc.update("""
                    INSERT INTO bpi.bpi_boundary_evidence
                        (id, tenant_id, batch_id, boundary_type, source_event_id, signal, classification,
                         satisfied, value_text, unit, quality, event_time, source)
                    VALUES (:id, :tenantId, :batchId, :boundaryType, :eventId, :signal, :classification,
                            :satisfied, :value, :unit, :quality, :eventTime, :source)
                    """, new MapSqlParameterSource().addValue("id", UUID.randomUUID())
                    .addValue("tenantId", tenantId).addValue("batchId", batchId)
                    .addValue("boundaryType", boundaryType.name()).addValue("eventId", item.eventId())
                    .addValue("signal", item.signal()).addValue("classification", item.classification())
                    .addValue("satisfied", item.satisfied()).addValue("value", item.value())
                    .addValue("unit", item.unit()).addValue("quality", item.quality())
                    .addValue("eventTime", Timestamp.from(item.eventTime())).addValue("source", item.source()));
        }
    }

    public void insertStateEvent(
            String tenantId, UUID batchId, String action, String toState, String reason,
            String actorId, Instant eventTime, String traceId) {
        jdbc.update("""
                INSERT INTO bpi.bpi_batch_state_events
                    (id, tenant_id, batch_id, revision, action, to_state, reason, actor_id, event_time, trace_id)
                VALUES (:id, :tenantId, :batchId, 1, :action, :toState, :reason, :actorId, :eventTime, :traceId)
                """, new MapSqlParameterSource().addValue("id", UUID.randomUUID()).addValue("tenantId", tenantId)
                .addValue("batchId", batchId).addValue("action", action).addValue("toState", toState)
                .addValue("reason", reason).addValue("actorId", actorId)
                .addValue("eventTime", Timestamp.from(eventTime)).addValue("traceId", traceId));
    }

    public void insertAudit(
            ActorContext actor, BatchCandidate candidate, String action, long beforeRevision,
            long afterRevision, String reason, String traceId, Map<String, Object> detail) {
        jdbc.update("""
                INSERT INTO bpi.bpi_audit_events
                    (id, tenant_id, plant_id, line_id, object_type, object_id, action, actor_id,
                     before_revision, after_revision, reason, trace_id, detail)
                VALUES (:id, :tenantId, :plantId, :lineId, 'BATCH_CANDIDATE', :objectId, :action, :actorId,
                        :beforeRevision, :afterRevision, :reason, :traceId, CAST(:detail AS jsonb))
                """, new MapSqlParameterSource().addValue("id", UUID.randomUUID())
                .addValue("tenantId", actor.tenantId()).addValue("plantId", candidate.plantId())
                .addValue("lineId", candidate.lineId()).addValue("objectId", candidate.id())
                .addValue("action", action).addValue("actorId", actor.userId())
                .addValue("beforeRevision", beforeRevision).addValue("afterRevision", afterRevision)
                .addValue("reason", reason).addValue("traceId", traceId).addValue("detail", writeJson(detail)));
    }

    public List<BatchInstance> listBatches(ActorContext actor, String plantId, String lineId, String state, int limit) {
        StringBuilder sql = new StringBuilder(BATCH_SELECT).append(" WHERE b.tenant_id = :tenantId");
        MapSqlParameterSource parameters = scopedParameters(actor, sql, "b").addValue("limit", Math.min(Math.max(limit, 1), 200));
        if (plantId != null && !plantId.isBlank()) {
            sql.append(" AND b.plant_id = :plantId");
            parameters.addValue("plantId", plantId);
        }
        if (lineId != null && !lineId.isBlank()) {
            sql.append(" AND b.line_id = :lineId");
            parameters.addValue("lineId", lineId);
        }
        if (state != null && !state.isBlank()) {
            sql.append(" AND b.state = :state");
            parameters.addValue("state", state);
        }
        sql.append(" ORDER BY b.start_time DESC, b.id LIMIT :limit");
        return jdbc.query(sql.toString(), parameters, batchRowMapper());
    }

    public BatchInstance findBatch(ActorContext actor, UUID batchId) {
        try {
            return jdbc.queryForObject(BATCH_SELECT + " WHERE b.tenant_id = :tenantId AND b.id = :id",
                    new MapSqlParameterSource().addValue("tenantId", actor.tenantId()).addValue("id", batchId),
                    batchRowMapper());
        } catch (EmptyResultDataAccessException exception) {
            throw new BpiNotFoundException("Batch not found.");
        }
    }

    public List<EvidenceView> findEvidence(ActorContext actor, UUID batchId) {
        findBatch(actor, batchId);
        return jdbc.query("""
                SELECT source_event_id, signal, classification, satisfied, value_text, unit,
                       quality, event_time, source
                  FROM bpi.bpi_boundary_evidence
                 WHERE tenant_id = :tenantId AND batch_id = :batchId
                 ORDER BY event_time, id
                """, new MapSqlParameterSource().addValue("tenantId", actor.tenantId()).addValue("batchId", batchId),
                (rs, rowNum) -> new EvidenceView(
                        rs.getString("source_event_id"), rs.getString("signal"), rs.getString("classification"),
                        rs.getBoolean("satisfied"), rs.getString("value_text"), rs.getString("unit"),
                        rs.getString("quality"), rs.getTimestamp("event_time").toInstant(), rs.getString("source")));
    }

    public List<BatchStateEvent> findTimeline(ActorContext actor, UUID batchId) {
        findBatch(actor, batchId);
        return jdbc.query("""
                SELECT revision, action, event_time, actor_id, reason
                  FROM bpi.bpi_batch_state_events
                 WHERE tenant_id = :tenantId AND batch_id = :batchId
                 ORDER BY revision
                """, new MapSqlParameterSource().addValue("tenantId", actor.tenantId()).addValue("batchId", batchId),
                (rs, rowNum) -> new BatchStateEvent(rs.getLong("revision"), rs.getString("action"),
                        rs.getTimestamp("event_time").toInstant(), rs.getString("actor_id"), rs.getString("reason")));
    }

    private MapSqlParameterSource scopedParameters(ActorContext actor, StringBuilder sql, String alias) {
        MapSqlParameterSource parameters = new MapSqlParameterSource().addValue("tenantId", actor.tenantId());
        if (!actor.plantIds().contains("*")) {
            if (actor.plantIds().isEmpty()) {
                sql.append(" AND 1 = 0");
            } else {
                sql.append(" AND ").append(alias).append(".plant_id IN (:allowedPlants)");
                parameters.addValue("allowedPlants", actor.plantIds());
            }
        }
        if (!actor.lineIds().contains("*")) {
            if (actor.lineIds().isEmpty()) {
                sql.append(" AND 1 = 0");
            } else {
                sql.append(" AND ").append(alias).append(".line_id IN (:allowedLines)");
                parameters.addValue("allowedLines", actor.lineIds());
            }
        }
        return parameters;
    }

    private RowMapper<BatchCandidate> candidateRowMapper() {
        return (rs, rowNum) -> mapCandidate(rs);
    }

    private BatchCandidate mapCandidate(ResultSet rs) throws SQLException {
        ReviewView review = rs.getString("reviewed_by") == null ? null : new ReviewView(
                rs.getString("reviewed_by"), rs.getString("review_reason"), rs.getTimestamp("reviewed_at").toInstant());
        return new BatchCandidate(
                rs.getObject("id", UUID.class), rs.getObject("candidate_key", UUID.class),
                rs.getString("tenant_id"), rs.getString("plant_id"), rs.getString("line_id"),
                BoundaryType.valueOf(rs.getString("boundary_type")), rs.getString("order_id"),
                rs.getObject("batch_id", UUID.class), rs.getTimestamp("boundary_time").toInstant(),
                CandidateState.valueOf(rs.getString("state")), rs.getLong("revision"),
                rs.getBigDecimal("confidence"), rs.getString("rule_version_ref"),
                rs.getString("topology_version_ref"), readJson(rs.getString("missing_signals"), new TypeReference<>() {}),
                readJson(rs.getString("evidence"), new TypeReference<>() {}), review);
    }

    private RowMapper<BatchInstance> batchRowMapper() {
        return (rs, rowNum) -> new BatchInstance(
                rs.getObject("id", UUID.class), rs.getString("batch_no"), rs.getString("tenant_id"),
                rs.getString("plant_id"), rs.getString("line_id"), rs.getString("stage_code"),
                rs.getString("order_id"), rs.getString("material_code"), BatchState.valueOf(rs.getString("state")),
                rs.getLong("revision"), rs.getBoolean("is_shadow"), rs.getTimestamp("start_time").toInstant(),
                optionalInstant(rs, "end_time"), rs.getBigDecimal("quantity"), rs.getString("quantity_unit"),
                rs.getBigDecimal("dry_matter"), rs.getString("quality_gate"), rs.getString("wms_status"),
                rs.getString("rule_version_ref"), rs.getString("topology_version_ref"));
    }

    private MapSqlParameterSource batchParameters(BatchInstance batch) {
        return new MapSqlParameterSource().addValue("id", batch.id()).addValue("tenantId", batch.tenantId())
                .addValue("plantId", batch.plantId()).addValue("batchNo", batch.batchNo())
                .addValue("lineId", batch.lineId()).addValue("stageCode", batch.stageCode())
                .addValue("orderId", batch.orderId()).addValue("materialCode", batch.materialCode())
                .addValue("state", batch.state().name()).addValue("revision", batch.revision())
                .addValue("shadow", batch.shadow()).addValue("startTime", Timestamp.from(batch.startTime()))
                .addValue("endTime", batch.endTime() == null ? null : Timestamp.from(batch.endTime()))
                .addValue("quantity", batch.quantity()).addValue("quantityUnit", batch.quantityUnit())
                .addValue("dryMatter", batch.dryMatter()).addValue("qualityGate", batch.qualityGate())
                .addValue("wmsStatus", batch.wmsStatus());
    }

    private Instant optionalInstant(ResultSet rs, String column) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new BpiValidationException("Could not serialize BPI payload: " + exception.getMessage());
        }
    }

    public <T> T readJson(String value, TypeReference<T> type) {
        try {
            return objectMapper.readValue(value, type);
        } catch (Exception exception) {
            throw new IllegalStateException("Stored BPI JSON is invalid", exception);
        }
    }
}
