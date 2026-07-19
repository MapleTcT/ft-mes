package com.mapletct.ftmes.bpi.infrastructure.postgres;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mapletct.ftmes.bpi.application.ActorContext;
import com.mapletct.ftmes.bpi.application.DataQualityIncidentCursorCodec;
import com.mapletct.ftmes.bpi.application.error.BpiConflictException;
import com.mapletct.ftmes.bpi.application.error.BpiNotFoundException;
import com.mapletct.ftmes.bpi.contract.v1.DataQualityEventV1;
import com.mapletct.ftmes.bpi.domain.DataQualityEventView;
import com.mapletct.ftmes.bpi.domain.DataQualityIncidentView;
import com.mapletct.ftmes.bpi.domain.DataQualityLifecycleView;
import com.mapletct.ftmes.bpi.domain.DataQualitySummary;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
public class DataQualityPostgresRepository {
    private static final String SEVERITY_RANK = """
            CASE i.severity
                WHEN 'CRITICAL' THEN 4
                WHEN 'ERROR' THEN 3
                WHEN 'WARNING' THEN 2
                ELSE 1
            END
            """;
    private static final String IMPACT_COUNT = """
            (SELECT count(*)
               FROM bpi.bpi_batch_instances b
              WHERE b.tenant_id = i.tenant_id
                AND b.plant_id = i.plant_id
                AND b.line_id = i.line_id
                AND b.start_time <= i.last_seen
                AND COALESCE(b.end_time, 'infinity'::timestamptz) >= i.first_seen)
            """;
    private static final String INCIDENT_SELECT = """
            SELECT i.*,
                   %s AS severity_rank,
                   %s AS affected_batch_count,
                   ARRAY(
                       SELECT affected.rule_ref
                         FROM (
                             SELECT DISTINCT CASE
                                 WHEN e.headers ->> 'rule_key' ~ '^[^|]+[|][^|]+[|][^|]+[|][^|]+[|][^|]+$'
                                     THEN split_part(e.headers ->> 'rule_key', '|', 4)
                                          || '@' || split_part(e.headers ->> 'rule_key', '|', 5)
                                 WHEN e.headers ->> 'rule_key' ~ '^[^|]+[|][^|]+$'
                                     THEN split_part(e.headers ->> 'rule_key', '|', 1)
                                          || '@' || split_part(e.headers ->> 'rule_key', '|', 2)
                                 ELSE NULLIF(e.headers ->> 'rule_key', '')
                             END AS rule_ref
                               FROM bpi.bpi_data_quality_incident_events e
                              WHERE e.tenant_id = i.tenant_id
                                AND e.incident_id = i.id
                         ) affected
                        WHERE affected.rule_ref IS NOT NULL
                        ORDER BY affected.rule_ref
                        LIMIT 100
                   ) AS affected_rules,
                   ARRAY(
                       SELECT b.batch_no
                         FROM bpi.bpi_batch_instances b
                        WHERE b.tenant_id = i.tenant_id
                          AND b.plant_id = i.plant_id
                          AND b.line_id = i.line_id
                          AND b.start_time <= i.last_seen
                          AND COALESCE(b.end_time, 'infinity'::timestamptz) >= i.first_seen
                        ORDER BY b.start_time DESC, b.id DESC
                        LIMIT 100
                   ) AS affected_batches
              FROM bpi.bpi_data_quality_incidents i
            """.formatted(SEVERITY_RANK, IMPACT_COUNT);

    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public DataQualityPostgresRepository(
            NamedParameterJdbcTemplate jdbc,
            ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    public Instant currentTransactionTime() {
        Timestamp value = jdbc.queryForObject(
                "SELECT transaction_timestamp()", new MapSqlParameterSource(), Timestamp.class);
        if (value == null) throw new IllegalStateException("PostgreSQL did not return transaction_timestamp().");
        return value.toInstant();
    }

    public IngestionResult ingest(
            UUID incidentId,
            DataQualityEventV1 event,
            String source,
            String actorId) {
        int inserted = jdbc.update("""
                INSERT INTO bpi.bpi_data_quality_incidents
                    (id, tenant_id, plant_id, line_id, source, device_id, property_id, issue_code,
                     severity, state, revision, event_count, first_seen, last_seen, last_event_id,
                     last_source_event_id, last_detail)
                VALUES (:id, :tenantId, :plantId, :lineId, :source, :deviceId, :propertyId, :issueCode,
                        :severity, 'OPEN', 1, 1, :detectedAt, :detectedAt, :eventId,
                        :sourceEventId, :detail)
                ON CONFLICT (tenant_id, plant_id, line_id, source, device_id, property_id, issue_code)
                DO NOTHING
                """, eventParameters(incidentId, event, source));

        if (inserted == 1) {
            insertRawEvent(incidentId, event);
            insertAction(event.getTenantId(), incidentId, 1, "CREATED", null, "OPEN",
                    actorId, null, "Data-quality event detected: " + event.getIssueCode(), event.getEventId());
            insertAudit(event.getTenantId(), event.getPlantId(), event.getLineId(), incidentId,
                    "DATA_QUALITY_DETECTED", actorId, 0, 1,
                    "Data-quality event detected: " + event.getIssueCode(), event.getEventId(),
                    event.getIssueCode(), event.getSeverity().name());
            return new IngestionResult(incidentId, true, false);
        }

        Aggregate current = findByIdentityForUpdate(event, source);
        insertRawEvent(current.id(), event);
        boolean reopen = "RESOLVED".equals(current.state())
                && current.resolvedAt() != null
                && Instant.ofEpochMilli(event.getDetectedAtMs()).isAfter(current.resolvedAt());
        long revision = current.revision() + 1;
        String severity = reopen
                ? event.getSeverity().name()
                : maximumSeverity(current.severity(), event.getSeverity().name());
        int updated = jdbc.update("""
                UPDATE bpi.bpi_data_quality_incidents
                   SET severity = :severity,
                       state = CASE WHEN :reopen THEN 'OPEN' ELSE state END,
                       revision = :revision,
                       event_count = event_count + 1,
                       first_seen = LEAST(first_seen, :detectedAt),
                       last_seen = GREATEST(last_seen, :detectedAt),
                       last_event_id = CASE WHEN :detectedAt >= last_seen THEN :eventId ELSE last_event_id END,
                       last_source_event_id = CASE WHEN :detectedAt >= last_seen THEN :sourceEventId ELSE last_source_event_id END,
                       last_detail = CASE WHEN :detectedAt >= last_seen THEN :detail ELSE last_detail END,
                       assignee = CASE WHEN :reopen THEN NULL ELSE assignee END,
                       acknowledged_by = CASE WHEN :reopen THEN NULL ELSE acknowledged_by END,
                       acknowledged_at = CASE WHEN :reopen THEN NULL ELSE acknowledged_at END,
                       acknowledgment_reason = CASE WHEN :reopen THEN NULL ELSE acknowledgment_reason END,
                       resolved_by = CASE WHEN :reopen THEN NULL ELSE resolved_by END,
                       resolved_at = CASE WHEN :reopen THEN NULL ELSE resolved_at END,
                       resolution_reason = CASE WHEN :reopen THEN NULL ELSE resolution_reason END,
                       updated_at = now()
                 WHERE tenant_id = :tenantId AND id = :id AND revision = :expectedRevision
                """, eventParameters(current.id(), event, source)
                .addValue("severity", severity)
                .addValue("reopen", reopen)
                .addValue("revision", revision)
                .addValue("expectedRevision", current.revision()));
        if (updated != 1) {
            throw new BpiConflictException("Data-quality incident changed during event aggregation.", current.revision());
        }
        if (reopen) {
            insertAction(event.getTenantId(), current.id(), revision, "REOPENED", "RESOLVED", "OPEN",
                    actorId, null, "A new event arrived after resolution.", event.getEventId());
            insertAudit(event.getTenantId(), event.getPlantId(), event.getLineId(), current.id(),
                    "DATA_QUALITY_REOPENED", actorId, current.revision(), revision,
                    "A new event arrived after resolution.", event.getEventId(),
                    event.getIssueCode(), event.getSeverity().name());
        }
        return new IngestionResult(current.id(), false, reopen);
    }

    public List<DataQualityIncidentView> list(
            ActorContext actor,
            String plantId,
            String lineId,
            String state,
            String search,
            Instant snapshotAt,
            DataQualityIncidentCursorCodec.Cursor cursor,
            int fetchLimit) {
        StringBuilder sql = new StringBuilder("WITH ranked AS (")
                .append(INCIDENT_SELECT)
                .append(" WHERE i.tenant_id = :tenantId AND i.plant_id = :plantId")
                .append(" AND i.updated_at <= :snapshotAt");
        MapSqlParameterSource parameters = new MapSqlParameterSource("tenantId", actor.tenantId())
                .addValue("plantId", plantId)
                .addValue("snapshotAt", Timestamp.from(snapshotAt))
                .addValue("fetchLimit", fetchLimit);
        appendActorLineScope(actor, sql, parameters, "i");
        if (lineId != null && !lineId.isBlank()) {
            sql.append(" AND i.line_id = :lineId");
            parameters.addValue("lineId", lineId);
        }
        if (state != null) {
            sql.append(" AND i.state = :state");
            parameters.addValue("state", state);
        }
        if (search != null && !search.isBlank()) {
            sql.append(" AND (lower(i.issue_code) LIKE :search OR lower(i.source) LIKE :search")
                    .append(" OR lower(i.device_id) LIKE :search OR lower(i.property_id) LIKE :search")
                    .append(" OR lower(i.last_detail) LIKE :search OR lower(COALESCE(i.assignee, '')) LIKE :search)");
            parameters.addValue("search", "%" + search.toLowerCase() + "%");
        }
        sql.append(") SELECT * FROM ranked WHERE 1 = 1");
        if (cursor != null) {
            sql.append(" AND (affected_batch_count < :cursorBatchCount")
                    .append(" OR (affected_batch_count = :cursorBatchCount AND severity_rank < :cursorSeverity)")
                    .append(" OR (affected_batch_count = :cursorBatchCount AND severity_rank = :cursorSeverity AND last_seen < :cursorLastSeen)")
                    .append(" OR (affected_batch_count = :cursorBatchCount AND severity_rank = :cursorSeverity AND last_seen = :cursorLastSeen AND id < :cursorId))");
            parameters.addValue("cursorBatchCount", cursor.affectedBatchCount())
                    .addValue("cursorSeverity", cursor.severityRank())
                    .addValue("cursorLastSeen", Timestamp.from(cursor.lastSeen()))
                    .addValue("cursorId", cursor.id());
        }
        sql.append(" ORDER BY affected_batch_count DESC, severity_rank DESC, last_seen DESC, id DESC")
                .append(" LIMIT :fetchLimit");
        return jdbc.query(sql.toString(), parameters, (rs, rowNum) -> mapIncident(rs));
    }

    public DataQualityIncidentView find(ActorContext actor, UUID incidentId) {
        List<DataQualityIncidentView> values = jdbc.query(
                INCIDENT_SELECT + " WHERE i.tenant_id = :tenantId AND i.id = :id",
                new MapSqlParameterSource("tenantId", actor.tenantId()).addValue("id", incidentId),
                (rs, rowNum) -> mapIncident(rs));
        DataQualityIncidentView value = values.stream().findFirst()
                .orElseThrow(() -> new BpiNotFoundException("Data-quality incident not found."));
        assertScope(actor, value);
        return value;
    }

    public DataQualityIncidentView lock(ActorContext actor, UUID incidentId) {
        List<DataQualityIncidentView> values = jdbc.query(
                INCIDENT_SELECT + " WHERE i.tenant_id = :tenantId AND i.id = :id FOR UPDATE OF i",
                new MapSqlParameterSource("tenantId", actor.tenantId()).addValue("id", incidentId),
                (rs, rowNum) -> mapIncident(rs));
        DataQualityIncidentView value = values.stream().findFirst()
                .orElseThrow(() -> new BpiNotFoundException("Data-quality incident not found."));
        assertScope(actor, value);
        return value;
    }

    public List<DataQualityEventView> events(ActorContext actor, UUID incidentId, int limit) {
        find(actor, incidentId);
        return jdbc.query("""
                SELECT event_id, source_event_id, severity, detail, detected_at, received_at, headers
                  FROM bpi.bpi_data_quality_incident_events
                 WHERE tenant_id = :tenantId AND incident_id = :incidentId
                 ORDER BY detected_at DESC, id DESC
                 LIMIT :limit
                """, new MapSqlParameterSource("tenantId", actor.tenantId())
                .addValue("incidentId", incidentId).addValue("limit", limit),
                (rs, rowNum) -> new DataQualityEventView(
                        rs.getString("event_id"), rs.getString("source_event_id"), rs.getString("severity"),
                        rs.getString("detail"), rs.getTimestamp("detected_at").toInstant(),
                        rs.getTimestamp("received_at").toInstant(),
                        readJson(rs.getString("headers"), new TypeReference<Map<String, String>>() {})));
    }

    public List<DataQualityLifecycleView> lifecycle(ActorContext actor, UUID incidentId) {
        find(actor, incidentId);
        return jdbc.query("""
                SELECT incident_revision, action, from_state, to_state, actor_id, assignee, reason, action_at
                  FROM bpi.bpi_data_quality_incident_actions
                 WHERE tenant_id = :tenantId AND incident_id = :incidentId
                 ORDER BY action_at, id
                """, new MapSqlParameterSource("tenantId", actor.tenantId())
                .addValue("incidentId", incidentId),
                (rs, rowNum) -> new DataQualityLifecycleView(
                        rs.getLong("incident_revision"), rs.getString("action"), rs.getString("from_state"),
                        rs.getString("to_state"), rs.getString("actor_id"), rs.getString("assignee"),
                        rs.getString("reason"), rs.getTimestamp("action_at").toInstant()));
    }

    public DataQualitySummary summary(ActorContext actor, String plantId, String lineId) {
        StringBuilder scope = new StringBuilder(
                " WHERE i.tenant_id = :tenantId AND i.plant_id = :plantId");
        MapSqlParameterSource parameters = new MapSqlParameterSource("tenantId", actor.tenantId())
                .addValue("plantId", plantId);
        appendActorLineScope(actor, scope, parameters, "i");
        if (lineId != null && !lineId.isBlank()) {
            scope.append(" AND i.line_id = :lineId");
            parameters.addValue("lineId", lineId);
        }
        Map<String, Object> totals = jdbc.queryForMap("""
                SELECT count(*) FILTER (WHERE i.state = 'OPEN') AS open_count,
                       count(*) FILTER (WHERE i.state = 'ACKNOWLEDGED') AS acknowledged_count,
                       count(*) FILTER (WHERE i.state = 'RESOLVED') AS resolved_count,
                       count(*) FILTER (WHERE i.state <> 'RESOLVED' AND i.severity = 'CRITICAL') AS critical_count
                  FROM bpi.bpi_data_quality_incidents i
                """ + scope, parameters);
        Long affectedBatches = jdbc.queryForObject("""
                SELECT count(DISTINCT b.id)
                  FROM bpi.bpi_batch_instances b
                 WHERE b.tenant_id = :tenantId AND b.plant_id = :plantId
                   AND EXISTS (
                       SELECT 1 FROM bpi.bpi_data_quality_incidents i
                """ + scope + """
                         AND i.state <> 'RESOLVED'
                         AND i.line_id = b.line_id
                         AND b.start_time <= i.last_seen
                         AND COALESCE(b.end_time, 'infinity'::timestamptz) >= i.first_seen)
                """, parameters, Long.class);
        Map<String, Long> issueCounts = new LinkedHashMap<>();
        List<Map<String, Object>> issueRows = jdbc.queryForList("""
                SELECT i.issue_code, count(*) AS issue_count
                  FROM bpi.bpi_data_quality_incidents i
                """ + scope + " AND i.state <> 'RESOLVED' GROUP BY i.issue_code ORDER BY i.issue_code",
                parameters);
        for (Map<String, Object> row : issueRows) {
            issueCounts.put(String.valueOf(row.get("issue_code")), number(row.get("issue_count")));
        }
        return new DataQualitySummary(
                number(totals.get("open_count")), number(totals.get("acknowledged_count")),
                number(totals.get("resolved_count")), number(totals.get("critical_count")),
                affectedBatches == null ? 0 : affectedBatches, issueCounts);
    }

    public void acknowledge(
            ActorContext actor,
            UUID incidentId,
            long expectedRevision,
            String assignee,
            String reason,
            String traceId) {
        DataQualityIncidentView current = lock(actor, incidentId);
        if (current.revision() != expectedRevision) {
            throw new BpiConflictException("Data-quality incident revision is stale.", current.revision());
        }
        if ("RESOLVED".equals(current.state())) {
            throw new BpiConflictException("A resolved incident cannot be acknowledged.", current.revision());
        }
        long revision = current.revision() + 1;
        String action = "ACKNOWLEDGED".equals(current.state()) ? "REASSIGNED" : "ACKNOWLEDGED";
        int updated = jdbc.update("""
                UPDATE bpi.bpi_data_quality_incidents
                   SET state = 'ACKNOWLEDGED', revision = :revision, assignee = :assignee,
                       acknowledged_by = :actorId, acknowledged_at = now(),
                       acknowledgment_reason = :reason, updated_at = now()
                 WHERE tenant_id = :tenantId AND id = :id AND revision = :expectedRevision
                   AND state IN ('OPEN', 'ACKNOWLEDGED')
                """, new MapSqlParameterSource("tenantId", actor.tenantId()).addValue("id", incidentId)
                .addValue("revision", revision).addValue("expectedRevision", expectedRevision)
                .addValue("assignee", assignee).addValue("actorId", actor.userId()).addValue("reason", reason));
        if (updated != 1) throw new BpiConflictException("Data-quality incident changed before acknowledgement.", expectedRevision);
        insertAction(actor.tenantId(), incidentId, revision, action, current.state(), "ACKNOWLEDGED",
                actor.userId(), assignee, reason, traceId);
        insertAudit(actor.tenantId(), current.plantId(), current.lineId(),
                incidentId, "DATA_QUALITY_" + action, actor.userId(), expectedRevision, revision,
                reason, traceId, current.issueCode(), current.severity());
    }

    public void resolve(
            ActorContext actor,
            UUID incidentId,
            long expectedRevision,
            String reason,
            String traceId) {
        DataQualityIncidentView current = lock(actor, incidentId);
        if (current.revision() != expectedRevision) {
            throw new BpiConflictException("Data-quality incident revision is stale.", current.revision());
        }
        if (!"ACKNOWLEDGED".equals(current.state())) {
            throw new BpiConflictException("Only an acknowledged incident can be resolved.", current.revision());
        }
        long revision = current.revision() + 1;
        int updated = jdbc.update("""
                UPDATE bpi.bpi_data_quality_incidents
                   SET state = 'RESOLVED', revision = :revision,
                       resolved_by = :actorId, resolved_at = now(), resolution_reason = :reason,
                       updated_at = now()
                 WHERE tenant_id = :tenantId AND id = :id AND revision = :expectedRevision
                   AND state = 'ACKNOWLEDGED'
                """, new MapSqlParameterSource("tenantId", actor.tenantId()).addValue("id", incidentId)
                .addValue("revision", revision).addValue("expectedRevision", expectedRevision)
                .addValue("actorId", actor.userId()).addValue("reason", reason));
        if (updated != 1) throw new BpiConflictException("Data-quality incident changed before resolution.", expectedRevision);
        insertAction(actor.tenantId(), incidentId, revision, "RESOLVED", "ACKNOWLEDGED", "RESOLVED",
                actor.userId(), current.assignee(), reason, traceId);
        insertAudit(actor.tenantId(), current.plantId(), current.lineId(),
                incidentId, "DATA_QUALITY_RESOLVED", actor.userId(), expectedRevision, revision,
                reason, traceId, current.issueCode(), current.severity());
    }

    private Aggregate findByIdentityForUpdate(DataQualityEventV1 event, String source) {
        List<Aggregate> values = jdbc.query("""
                SELECT id, state, revision, severity, resolved_at
                  FROM bpi.bpi_data_quality_incidents
                 WHERE tenant_id = :tenantId AND plant_id = :plantId AND line_id = :lineId
                   AND source = :source AND device_id = :deviceId AND property_id = :propertyId
                   AND issue_code = :issueCode
                 FOR UPDATE
                """, eventParameters(null, event, source),
                (rs, rowNum) -> new Aggregate(
                        rs.getObject("id", UUID.class), rs.getString("state"), rs.getLong("revision"),
                        rs.getString("severity"), instant(rs, "resolved_at")));
        return values.stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("Data-quality incident disappeared during aggregation."));
    }

    private void insertRawEvent(UUID incidentId, DataQualityEventV1 event) {
        try {
            jdbc.update("""
                    INSERT INTO bpi.bpi_data_quality_incident_events
                        (id, tenant_id, incident_id, event_id, source_event_id, severity,
                         detail, detected_at, headers)
                    VALUES (:rowId, :tenantId, :incidentId, :eventId, :sourceEventId, :severity,
                            :detail, :detectedAt, CAST(:headers AS jsonb))
                    """, new MapSqlParameterSource("rowId", UUID.randomUUID())
                    .addValue("tenantId", event.getTenantId()).addValue("incidentId", incidentId)
                    .addValue("eventId", event.getEventId())
                    .addValue("sourceEventId", blankToNull(event.getSourceEventId()))
                    .addValue("severity", event.getSeverity().name()).addValue("detail", event.getDetail())
                    .addValue("detectedAt", Timestamp.from(Instant.ofEpochMilli(event.getDetectedAtMs())))
                    .addValue("headers", writeJson(event.getHeadersMap())));
        } catch (DataIntegrityViolationException exception) {
            throw new BpiConflictException("Data-quality event_id was reused.", null);
        }
    }

    private void insertAction(
            String tenantId,
            UUID incidentId,
            long revision,
            String action,
            String fromState,
            String toState,
            String actorId,
            String assignee,
            String reason,
            String traceId) {
        jdbc.update("""
                INSERT INTO bpi.bpi_data_quality_incident_actions
                    (id, tenant_id, incident_id, incident_revision, action, from_state, to_state,
                     actor_id, assignee, reason, trace_id)
                VALUES (:id, :tenantId, :incidentId, :revision, :action, :fromState, :toState,
                        :actorId, :assignee, :reason, :traceId)
                """, new MapSqlParameterSource("id", UUID.randomUUID()).addValue("tenantId", tenantId)
                .addValue("incidentId", incidentId).addValue("revision", revision).addValue("action", action)
                .addValue("fromState", fromState).addValue("toState", toState).addValue("actorId", actorId)
                .addValue("assignee", assignee).addValue("reason", truncate(reason, 500)).addValue("traceId", traceId));
    }

    private void insertAudit(
            String tenantId,
            String plantId,
            String lineId,
            UUID incidentId,
            String action,
            String actorId,
            long beforeRevision,
            long afterRevision,
            String reason,
            String traceId,
            String issueCode,
            String severity) {
        jdbc.update("""
                INSERT INTO bpi.bpi_audit_events
                    (id, tenant_id, plant_id, line_id, object_type, object_id, action, actor_id,
                     before_revision, after_revision, reason, trace_id, detail)
                VALUES (:id, :tenantId, :plantId, :lineId, 'DATA_QUALITY_INCIDENT', :objectId,
                        :action, :actorId, :beforeRevision, :afterRevision, :reason, :traceId,
                        jsonb_build_object('issueCode', :issueCode, 'severity', :severity))
                """, new MapSqlParameterSource("id", UUID.randomUUID()).addValue("tenantId", tenantId)
                .addValue("plantId", plantId).addValue("lineId", lineId).addValue("objectId", incidentId)
                .addValue("action", action).addValue("actorId", actorId)
                .addValue("beforeRevision", beforeRevision).addValue("afterRevision", afterRevision)
                .addValue("reason", truncate(reason, 500)).addValue("traceId", traceId)
                .addValue("issueCode", issueCode).addValue("severity", severity));
    }

    private MapSqlParameterSource eventParameters(UUID id, DataQualityEventV1 event, String source) {
        return new MapSqlParameterSource("id", id)
                .addValue("tenantId", event.getTenantId()).addValue("plantId", event.getPlantId())
                .addValue("lineId", event.getLineId()).addValue("source", source)
                .addValue("deviceId", event.getDeviceId()).addValue("propertyId", event.getPropertyId())
                .addValue("issueCode", event.getIssueCode()).addValue("severity", event.getSeverity().name())
                .addValue("detectedAt", Timestamp.from(Instant.ofEpochMilli(event.getDetectedAtMs())))
                .addValue("eventId", event.getEventId())
                .addValue("sourceEventId", blankToNull(event.getSourceEventId()))
                .addValue("detail", event.getDetail());
    }

    private DataQualityIncidentView mapIncident(ResultSet rs) throws SQLException {
        return new DataQualityIncidentView(
                rs.getObject("id", UUID.class), rs.getString("issue_code"), rs.getString("severity"),
                rs.getString("state"), rs.getLong("revision"), rs.getString("plant_id"),
                rs.getString("line_id"), rs.getString("source"),
                blankToNull(rs.getString("device_id")), blankToNull(rs.getString("property_id")),
                List.of(rs.getString("line_id")), strings(rs.getArray("affected_rules")),
                strings(rs.getArray("affected_batches")), rs.getLong("affected_batch_count"),
                rs.getLong("event_count"), rs.getTimestamp("first_seen").toInstant(),
                rs.getTimestamp("last_seen").toInstant(), rs.getString("last_detail"),
                rs.getString("assignee"), rs.getString("acknowledged_by"), instant(rs, "acknowledged_at"),
                rs.getString("acknowledgment_reason"), rs.getString("resolved_by"),
                instant(rs, "resolved_at"), rs.getString("resolution_reason"));
    }

    private List<String> strings(Array array) throws SQLException {
        if (array == null) return List.of();
        Object values = array.getArray();
        if (values instanceof String[] strings) return List.copyOf(Arrays.asList(strings));
        Object[] objects = (Object[]) values;
        List<String> result = new ArrayList<>(objects.length);
        for (Object object : objects) result.add(String.valueOf(object));
        return List.copyOf(result);
    }

    private void appendActorLineScope(
            ActorContext actor,
            StringBuilder sql,
            MapSqlParameterSource parameters,
            String alias) {
        if (actor.lineIds().contains("*")) return;
        if (actor.lineIds().isEmpty()) {
            sql.append(" AND 1 = 0");
        } else {
            sql.append(" AND ").append(alias).append(".line_id IN (:allowedLines)");
            parameters.addValue("allowedLines", actor.lineIds());
        }
    }

    private void assertScope(ActorContext actor, DataQualityIncidentView value) {
        if (!actor.canAccess(value.plantId(), value.lineId())) {
            throw new BpiNotFoundException("Data-quality incident not found.");
        }
    }

    private long number(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private String maximumSeverity(String current, String incoming) {
        return severityRank(incoming) > severityRank(current) ? incoming : current;
    }

    private int severityRank(String severity) {
        return switch (severity) {
            case "CRITICAL" -> 4;
            case "ERROR" -> 3;
            case "WARNING" -> 2;
            default -> 1;
        };
    }

    private Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private String truncate(String value, int maximum) {
        return value.length() <= maximum ? value : value.substring(0, maximum);
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not serialize BPI data-quality event", exception);
        }
    }

    private <T> T readJson(String value, TypeReference<T> type) {
        try {
            return objectMapper.readValue(value, type);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not deserialize BPI data-quality event", exception);
        }
    }

    private record Aggregate(UUID id, String state, long revision, String severity, Instant resolvedAt) {
    }

    public record IngestionResult(UUID incidentId, boolean created, boolean reopened) {
    }
}
