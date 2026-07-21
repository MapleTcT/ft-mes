package com.mapletct.ftmes.bpi.infrastructure.postgres;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mapletct.ftmes.bpi.application.ActorContext;
import com.mapletct.ftmes.bpi.application.error.BpiConflictException;
import com.mapletct.ftmes.bpi.application.error.BpiNotFoundException;
import com.mapletct.ftmes.bpi.domain.BatchInstance;
import com.mapletct.ftmes.bpi.domain.WmsInboundReversalOriginalTarget;
import com.mapletct.ftmes.bpi.domain.WmsInboundReversalTaskView;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
public class WmsInboundReversalPostgresRepository {

    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public WmsInboundReversalPostgresRepository(
            NamedParameterJdbcTemplate jdbc,
            ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    public WmsInboundReversalTaskView findLatestTask(ActorContext actor, UUID batchId) {
        List<WmsInboundReversalTaskView> tasks = jdbc.query("""
                SELECT task.*, batch.revision AS batch_revision,
                       event.status AS outbox_status,
                       COALESCE(event.total_attempt_count, 0) AS delivery_attempt_count
                  FROM bpi.bpi_wms_inbound_reversal_tasks task
                  JOIN bpi.bpi_batch_instances batch
                    ON batch.tenant_id = task.tenant_id AND batch.id = task.batch_id
                  LEFT JOIN bpi.bpi_outbox_events event
                    ON event.tenant_id = task.tenant_id
                   AND event.id = task.reversal_command_event_id
                 WHERE task.tenant_id = :tenantId
                   AND task.batch_id = :batchId
                 ORDER BY task.requested_at DESC, task.id DESC
                 LIMIT 1
                """, new MapSqlParameterSource()
                .addValue("tenantId", actor.tenantId())
                .addValue("batchId", batchId), taskRowMapper());
        return tasks.isEmpty() ? null : tasks.get(0);
    }

    public WmsInboundReversalTaskView findTask(ActorContext actor, UUID taskId) {
        try {
            return jdbc.queryForObject(taskSql("task.id = :taskId", false),
                    new MapSqlParameterSource()
                            .addValue("tenantId", actor.tenantId())
                            .addValue("taskId", taskId),
                    taskRowMapper());
        } catch (EmptyResultDataAccessException error) {
            throw new BpiNotFoundException("WMS completion-inbound reversal task not found.");
        }
    }

    public WmsInboundReversalOriginalTarget lockOriginalInbound(
            String tenantId, UUID batchId) {
        try {
            return jdbc.queryForObject("""
                    SELECT link.id, link.command_event_id, link.idempotency_key,
                           link.document_id, link.status, link.revision,
                           event.status AS outbox_status, event.payload
                      FROM bpi.bpi_wms_inbound_links link
                      JOIN bpi.bpi_outbox_events event
                        ON event.tenant_id = link.tenant_id
                       AND event.id = link.command_event_id
                     WHERE link.tenant_id = :tenantId
                       AND link.batch_id = :batchId
                     FOR UPDATE OF link
                    """, new MapSqlParameterSource()
                    .addValue("tenantId", tenantId)
                    .addValue("batchId", batchId),
                    (rs, rowNum) -> new WmsInboundReversalOriginalTarget(
                            rs.getObject("id", UUID.class),
                            rs.getObject("command_event_id", UUID.class),
                            rs.getString("idempotency_key"),
                            rs.getString("document_id"),
                            rs.getString("status"),
                            rs.getLong("revision"),
                            rs.getString("outbox_status"),
                            rs.getBytes("payload")));
        } catch (EmptyResultDataAccessException error) {
            throw new BpiNotFoundException("Accepted WMS completion-inbound document not found.");
        }
    }

    public boolean hasActiveTask(String tenantId, UUID batchId) {
        Boolean active = jdbc.queryForObject("""
                SELECT EXISTS (
                    SELECT 1
                      FROM bpi.bpi_wms_inbound_reversal_tasks
                     WHERE tenant_id = :tenantId
                       AND batch_id = :batchId
                       AND state IN ('PENDING_APPROVAL', 'PENDING_WMS')
                )
                """, new MapSqlParameterSource()
                .addValue("tenantId", tenantId)
                .addValue("batchId", batchId), Boolean.class);
        return Boolean.TRUE.equals(active);
    }

    public void touchBatchForRequest(BatchInstance batch) {
        int updated = jdbc.update("""
                UPDATE bpi.bpi_batch_instances
                   SET revision = revision + 1, updated_at = now()
                 WHERE tenant_id = :tenantId
                   AND id = :batchId
                   AND revision = :expectedRevision
                   AND state = 'INBOUNDED'
                """, new MapSqlParameterSource()
                .addValue("tenantId", batch.tenantId())
                .addValue("batchId", batch.id())
                .addValue("expectedRevision", batch.revision()));
        if (updated != 1) {
            throw new BpiConflictException(
                    "Batch changed before WMS reversal submission.", batch.revision());
        }
    }

    public void insertTask(
            ActorContext actor,
            BatchInstance batch,
            WmsInboundReversalOriginalTarget original,
            UUID taskId,
            String reason,
            String comment,
            Instant requestedAt) {
        jdbc.update("""
                INSERT INTO bpi.bpi_wms_inbound_reversal_tasks
                    (id, tenant_id, batch_id, original_inbound_link_id,
                     original_command_event_id, original_idempotency_key,
                     original_document_id, state, revision, requested_by,
                     requested_at, request_reason, request_comment)
                VALUES (:id, :tenantId, :batchId, :originalInboundLinkId,
                        :originalCommandEventId, :originalIdempotencyKey,
                        :originalDocumentId, 'PENDING_APPROVAL', 1, :requestedBy,
                        :requestedAt, :requestReason, :requestComment)
                """, new MapSqlParameterSource()
                .addValue("id", taskId)
                .addValue("tenantId", actor.tenantId())
                .addValue("batchId", batch.id())
                .addValue("originalInboundLinkId", original.inboundLinkId())
                .addValue("originalCommandEventId", original.originalCommandEventId())
                .addValue("originalIdempotencyKey", original.originalIdempotencyKey())
                .addValue("originalDocumentId", original.originalDocumentId())
                .addValue("requestedBy", actor.userId())
                .addValue("requestedAt", Timestamp.from(requestedAt))
                .addValue("requestReason", reason)
                .addValue("requestComment", blankToNull(comment)));
    }

    public WmsInboundReversalTaskView lockPendingApproval(
            ActorContext actor, UUID batchId) {
        try {
            return jdbc.queryForObject(taskSql(
                            "task.batch_id = :batchId AND task.state = 'PENDING_APPROVAL'", true),
                    new MapSqlParameterSource()
                            .addValue("tenantId", actor.tenantId())
                            .addValue("batchId", batchId),
                    taskRowMapper());
        } catch (EmptyResultDataAccessException error) {
            throw new BpiConflictException(
                    "Batch has no pending WMS completion-inbound reversal request.", null);
        }
    }

    public void approveAndInsertCommand(
            ActorContext actor,
            BatchInstance batch,
            WmsInboundReversalTaskView task,
            UUID commandEventId,
            String idempotencyKey,
            String topic,
            String partitionKey,
            byte[] payload,
            Map<String, String> headers,
            String reason,
            String comment,
            Instant decidedAt) {
        jdbc.update("""
                INSERT INTO bpi.bpi_outbox_events
                    (id, tenant_id, plant_id, line_id, aggregate_type, aggregate_id,
                     event_type, topic, partition_key, payload, headers)
                VALUES (:id, :tenantId, :plantId, :lineId, 'BATCH_INSTANCE', :batchId,
                        'WMS_COMPLETION_INBOUND_REVERSAL_COMMAND', :topic, :partitionKey,
                        :payload, CAST(:headers AS jsonb))
                """, new MapSqlParameterSource()
                .addValue("id", commandEventId)
                .addValue("tenantId", actor.tenantId())
                .addValue("plantId", batch.plantId())
                .addValue("lineId", batch.lineId())
                .addValue("batchId", batch.id())
                .addValue("topic", topic)
                .addValue("partitionKey", partitionKey)
                .addValue("payload", payload)
                .addValue("headers", writeJson(headers)));

        int updated = jdbc.update("""
                UPDATE bpi.bpi_wms_inbound_reversal_tasks
                   SET state = 'PENDING_WMS', revision = revision + 1,
                       decided_by = :decidedBy, decided_at = :decidedAt,
                       decision_reason = :decisionReason,
                       decision_comment = :decisionComment,
                       reversal_command_event_id = :commandEventId,
                       reversal_idempotency_key = :idempotencyKey,
                       updated_at = now()
                 WHERE tenant_id = :tenantId
                   AND id = :taskId
                   AND revision = :expectedRevision
                   AND state = 'PENDING_APPROVAL'
                """, new MapSqlParameterSource()
                .addValue("decidedBy", actor.userId())
                .addValue("decidedAt", Timestamp.from(decidedAt))
                .addValue("decisionReason", reason)
                .addValue("decisionComment", blankToNull(comment))
                .addValue("commandEventId", commandEventId)
                .addValue("idempotencyKey", idempotencyKey)
                .addValue("tenantId", actor.tenantId())
                .addValue("taskId", task.taskId())
                .addValue("expectedRevision", task.revision()));
        if (updated != 1) {
            throw new BpiConflictException(
                    "WMS reversal request was already decided.", task.batchRevision());
        }
    }

    public WmsInboundReversalTaskView lockByCommand(
            ActorContext actor, UUID batchId, UUID commandEventId) {
        try {
            return jdbc.queryForObject(taskSql(
                            "task.batch_id = :batchId AND task.reversal_command_event_id = :commandEventId",
                            true),
                    new MapSqlParameterSource()
                            .addValue("tenantId", actor.tenantId())
                            .addValue("batchId", batchId)
                            .addValue("commandEventId", commandEventId),
                    taskRowMapper());
        } catch (EmptyResultDataAccessException error) {
            throw new BpiNotFoundException("WMS completion-inbound reversal command not found.");
        }
    }

    public void updateReceipt(
            ActorContext actor,
            WmsInboundReversalTaskView task,
            boolean accepted,
            String receiptEventId,
            String reversalDocumentId,
            String errorCode,
            String detail,
            Instant observedAt) {
        int updated = jdbc.update("""
                UPDATE bpi.bpi_wms_inbound_reversal_tasks
                   SET state = :state, revision = revision + 1,
                       reversal_receipt_event_id = :receiptEventId,
                       reversal_document_id = :reversalDocumentId,
                       error_code = :errorCode, detail = :detail,
                       observed_at = :observedAt, updated_at = now()
                 WHERE tenant_id = :tenantId
                   AND id = :taskId
                   AND revision = :expectedRevision
                   AND state = 'PENDING_WMS'
                """, new MapSqlParameterSource()
                .addValue("state", accepted ? "COMPLETED" : "FAILED")
                .addValue("receiptEventId", receiptEventId)
                .addValue("reversalDocumentId", accepted ? reversalDocumentId : null)
                .addValue("errorCode", accepted ? null : errorCode)
                .addValue("detail", limit(blankToNull(detail), 1000))
                .addValue("observedAt", Timestamp.from(observedAt))
                .addValue("tenantId", actor.tenantId())
                .addValue("taskId", task.taskId())
                .addValue("expectedRevision", task.revision()));
        if (updated != 1) {
            throw new BpiConflictException(
                    "WMS reversal receipt changed concurrently.", task.batchRevision());
        }
    }

    private String taskSql(String predicate, boolean lock) {
        return """
                SELECT task.*, batch.revision AS batch_revision,
                       event.status AS outbox_status,
                       COALESCE(event.total_attempt_count, 0) AS delivery_attempt_count
                  FROM bpi.bpi_wms_inbound_reversal_tasks task
                  JOIN bpi.bpi_batch_instances batch
                    ON batch.tenant_id = task.tenant_id AND batch.id = task.batch_id
                  LEFT JOIN bpi.bpi_outbox_events event
                    ON event.tenant_id = task.tenant_id
                   AND event.id = task.reversal_command_event_id
                 WHERE task.tenant_id = :tenantId AND
                """ + predicate + (lock ? " FOR UPDATE OF task" : "");
    }

    private RowMapper<WmsInboundReversalTaskView> taskRowMapper() {
        return (rs, rowNum) -> new WmsInboundReversalTaskView(
                rs.getObject("id", UUID.class),
                rs.getObject("batch_id", UUID.class),
                rs.getString("state"),
                rs.getLong("revision"),
                rs.getLong("batch_revision"),
                rs.getObject("original_inbound_link_id", UUID.class),
                rs.getObject("original_command_event_id", UUID.class),
                rs.getString("original_idempotency_key"),
                rs.getString("original_document_id"),
                rs.getString("requested_by"),
                rs.getTimestamp("requested_at").toInstant(),
                rs.getString("request_reason"),
                rs.getString("request_comment"),
                rs.getString("decided_by"),
                nullableInstant(rs.getTimestamp("decided_at")),
                rs.getString("decision_reason"),
                rs.getString("decision_comment"),
                rs.getObject("reversal_command_event_id", UUID.class),
                rs.getString("reversal_idempotency_key"),
                rs.getString("reversal_receipt_event_id"),
                rs.getString("reversal_document_id"),
                rs.getString("error_code"),
                rs.getString("detail"),
                nullableInstant(rs.getTimestamp("observed_at")),
                rs.getString("outbox_status"),
                rs.getInt("delivery_attempt_count"));
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception error) {
            throw new IllegalStateException("Could not serialize WMS reversal outbox headers", error);
        }
    }

    private static Instant nullableInstant(Timestamp value) {
        return value == null ? null : value.toInstant();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static String limit(String value, int maximum) {
        return value == null || value.length() <= maximum ? value : value.substring(0, maximum);
    }
}
