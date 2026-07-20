package com.mapletct.ftmes.bpi.infrastructure.outbox;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mapletct.ftmes.bpi.application.ActorContext;
import com.mapletct.ftmes.bpi.application.error.BpiConflictException;
import com.mapletct.ftmes.bpi.application.error.BpiNotFoundException;
import com.mapletct.ftmes.bpi.domain.OutboxEventClaim;
import com.mapletct.ftmes.bpi.domain.RulePublicationEnvelope;
import com.mapletct.ftmes.bpi.domain.RulePublicationView;
import com.mapletct.ftmes.bpi.domain.RuleVersionView;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
public class RulePublicationOutboxRepository {
    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public RulePublicationOutboxRepository(
            NamedParameterJdbcTemplate jdbc,
            ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    public void insertPublication(
            ActorContext actor,
            RuleVersionView rule,
            RulePublicationEnvelope event) {
        insertPublication(actor, rule, event, "ACTIVATE", true);
    }

    public void insertPublication(
            ActorContext actor,
            RuleVersionView rule,
            RulePublicationEnvelope event,
            String lifecycleAction,
            boolean lifecycleActive) {
        jdbc.update("""
                INSERT INTO bpi.bpi_outbox_events
                    (id, tenant_id, plant_id, line_id, aggregate_type, aggregate_id,
                     event_type, topic, partition_key, payload, headers,
                     lifecycle_action, lifecycle_sequence, lifecycle_active)
                VALUES (:id, :tenantId, :plantId, :lineId, 'RULE_VERSION', :aggregateId,
                        'BOUNDARY_RULE_PUBLISHED', :topic, :partitionKey, :payload,
                        CAST(:headers AS jsonb), :lifecycleAction,
                        (SELECT COALESCE(MAX(existing.lifecycle_sequence), 0) + 1
                           FROM bpi.bpi_outbox_events existing
                          WHERE existing.tenant_id = :tenantId
                            AND existing.aggregate_type = 'RULE_VERSION'
                            AND existing.aggregate_id = :aggregateId
                            AND existing.event_type = 'BOUNDARY_RULE_PUBLISHED'),
                        :lifecycleActive)
                """, new MapSqlParameterSource()
                .addValue("id", event.eventId())
                .addValue("tenantId", actor.tenantId())
                .addValue("plantId", rule.plantId())
                .addValue("lineId", rule.lineId())
                .addValue("aggregateId", rule.id())
                .addValue("topic", event.topic())
                .addValue("partitionKey", event.partitionKey())
                .addValue("payload", event.payload())
                .addValue("headers", writeJson(event.headers()))
                .addValue("lifecycleAction", lifecycleAction)
                .addValue("lifecycleActive", lifecycleActive));
    }

    @Transactional
    public List<OutboxEventClaim> claimPending(
            int batchSize,
            Duration claimTimeout) {
        jdbc.update("""
                UPDATE bpi.bpi_outbox_events
                   SET status = 'PENDING', claim_token = NULL, claimed_at = NULL,
                       next_attempt_at = now(), updated_at = now(),
                       last_error = 'Recovered stale dispatcher claim', revision = revision + 1
                 WHERE status = 'DISPATCHING'
                   AND (claimed_at IS NULL
                        OR claimed_at < now() - (:claimTimeoutMs * interval '1 millisecond'))
                """, new MapSqlParameterSource()
                .addValue("claimTimeoutMs", claimTimeout.toMillis()));
        UUID claimToken = UUID.randomUUID();
        return jdbc.query("""
                WITH selected AS (
                    SELECT id
                      FROM bpi.bpi_outbox_events
                     WHERE status = 'PENDING' AND next_attempt_at <= now()
                     ORDER BY created_at, id
                     FOR UPDATE SKIP LOCKED
                     LIMIT :batchSize
                )
                UPDATE bpi.bpi_outbox_events event
                   SET status = 'DISPATCHING', claim_token = :claimToken,
                       claimed_at = now(), attempt_count = attempt_count + 1,
                       total_attempt_count = total_attempt_count + 1,
                       revision = revision + 1, updated_at = now()
                  FROM selected
                 WHERE event.id = selected.id
                RETURNING event.id, event.claim_token, event.topic, event.partition_key,
                          event.payload, event.headers::text AS headers, event.attempt_count
                """, new MapSqlParameterSource()
                .addValue("batchSize", batchSize)
                .addValue("claimToken", claimToken),
                (rs, rowNum) -> new OutboxEventClaim(
                        rs.getObject("id", UUID.class),
                        rs.getObject("claim_token", UUID.class),
                        rs.getString("topic"),
                        rs.getString("partition_key"),
                        rs.getBytes("payload"),
                        readHeaders(rs.getString("headers")),
                        rs.getInt("attempt_count")));
    }

    public boolean markPublished(UUID id, UUID claimToken) {
        return jdbc.update("""
                UPDATE bpi.bpi_outbox_events
                   SET status = 'PUBLISHED', published_at = now(),
                       claim_token = NULL, claimed_at = NULL, last_error = NULL,
                       revision = revision + 1, updated_at = now()
                 WHERE id = :id AND claim_token = :claimToken AND status = 'DISPATCHING'
                """, new MapSqlParameterSource().addValue("id", id)
                .addValue("claimToken", claimToken)) == 1;
    }

    public boolean markFailed(
            UUID id,
            UUID claimToken,
            int attemptCount,
            int maxAttempts,
            Duration retryBackoff,
            String error) {
        boolean terminal = attemptCount >= maxAttempts;
        long cap = Duration.ofMinutes(15).toMillis();
        long base = Math.min(retryBackoff.toMillis(), cap);
        long multiplier = Math.min(Math.max(1L, attemptCount), Math.max(1L, cap / base));
        long delay = Math.min(base * multiplier, cap);
        return jdbc.update("""
                UPDATE bpi.bpi_outbox_events
                   SET status = :status, claim_token = NULL, claimed_at = NULL,
                       next_attempt_at = now() + (:retryDelayMs * interval '1 millisecond'),
                       last_error = :lastError, revision = revision + 1, updated_at = now()
                 WHERE id = :id AND claim_token = :claimToken AND status = 'DISPATCHING'
                """, new MapSqlParameterSource()
                .addValue("status", terminal ? "FAILED" : "PENDING")
                .addValue("retryDelayMs", delay)
                .addValue("lastError", truncate(error))
                .addValue("id", id)
                .addValue("claimToken", claimToken)) == 1;
    }

    public RulePublicationView lockPublication(ActorContext actor, UUID ruleId) {
        try {
            RulePublicationView publication = jdbc.queryForObject("""
                    SELECT id, status, revision, attempt_count, total_attempt_count,
                           manual_retry_count, published_at, last_requeued_at, last_error
                      FROM bpi.bpi_outbox_events
                     WHERE tenant_id = :tenantId
                       AND aggregate_type = 'RULE_VERSION'
                       AND aggregate_id = :ruleId
                       AND event_type = 'BOUNDARY_RULE_PUBLISHED'
                     ORDER BY lifecycle_sequence DESC
                     LIMIT 1
                     FOR UPDATE
                    """, new MapSqlParameterSource()
                    .addValue("tenantId", actor.tenantId())
                    .addValue("ruleId", ruleId),
                    (rs, rowNum) -> mapPublication(rs));
            if (publication == null) throw new BpiNotFoundException("Rule publication event not found.");
            return publication;
        } catch (EmptyResultDataAccessException exception) {
            throw new BpiNotFoundException("Rule publication event not found.");
        }
    }

    public byte[] findActivationPayload(ActorContext actor, UUID ruleId) {
        try {
            byte[] payload = jdbc.queryForObject("""
                    SELECT payload
                      FROM bpi.bpi_outbox_events
                     WHERE tenant_id = :tenantId
                       AND aggregate_type = 'RULE_VERSION'
                       AND aggregate_id = :ruleId
                       AND event_type = 'BOUNDARY_RULE_PUBLISHED'
                       AND lifecycle_action = 'ACTIVATE'
                     ORDER BY lifecycle_sequence DESC
                     LIMIT 1
                    """, new MapSqlParameterSource()
                    .addValue("tenantId", actor.tenantId())
                    .addValue("ruleId", ruleId),
                    byte[].class);
            if (payload == null || payload.length == 0) {
                throw new BpiNotFoundException("Rule activation event not found.");
            }
            return payload;
        } catch (EmptyResultDataAccessException exception) {
            throw new BpiNotFoundException("Rule activation event not found.");
        }
    }

    public RulePublicationView requeueFailed(
            ActorContext actor,
            UUID ruleId,
            UUID publicationId,
            long expectedRevision) {
        int updated = jdbc.update("""
                UPDATE bpi.bpi_outbox_events
                   SET status = 'PENDING', attempt_count = 0,
                       manual_retry_count = manual_retry_count + 1,
                       next_attempt_at = now(), claim_token = NULL, claimed_at = NULL,
                       published_at = NULL, last_error = NULL,
                       last_requeued_at = now(), last_requeued_by = :actorId,
                       revision = revision + 1, updated_at = now()
                 WHERE tenant_id = :tenantId
                   AND aggregate_type = 'RULE_VERSION'
                   AND aggregate_id = :ruleId
                   AND event_type = 'BOUNDARY_RULE_PUBLISHED'
                   AND id = :publicationId
                   AND status = 'FAILED'
                   AND revision = :expectedRevision
                """, new MapSqlParameterSource()
                .addValue("actorId", actor.userId())
                .addValue("tenantId", actor.tenantId())
                .addValue("ruleId", ruleId)
                .addValue("publicationId", publicationId)
                .addValue("expectedRevision", expectedRevision));
        if (updated != 1) {
            throw new BpiConflictException("Rule publication can no longer be retried.", expectedRevision);
        }
        return lockPublication(actor, ruleId);
    }

    public void insertPublicationAudit(
            ActorContext actor,
            RuleVersionView rule,
            RulePublicationView before,
            RulePublicationView after,
            String reason,
            String traceId) {
        jdbc.update("""
                INSERT INTO bpi.bpi_audit_events
                    (id, tenant_id, plant_id, line_id, object_type, object_id, action, actor_id,
                     before_revision, after_revision, reason, trace_id, detail)
                VALUES (:id, :tenantId, :plantId, :lineId, 'RULE_PUBLICATION', :objectId,
                        'RULE_PUBLICATION_REQUEUED', :actorId, :beforeRevision, :afterRevision,
                        :reason, :traceId, CAST(:detail AS jsonb))
                """, new MapSqlParameterSource()
                .addValue("id", UUID.randomUUID())
                .addValue("tenantId", actor.tenantId())
                .addValue("plantId", rule.plantId())
                .addValue("lineId", rule.lineId())
                .addValue("objectId", before.id())
                .addValue("actorId", actor.userId())
                .addValue("beforeRevision", before.revision())
                .addValue("afterRevision", after.revision())
                .addValue("reason", reason)
                .addValue("traceId", traceId)
                .addValue("detail", writeJson(Map.of(
                        "ruleId", rule.id(),
                        "previousStatus", before.status(),
                        "nextStatus", after.status(),
                        "previousLastError", before.lastError() == null ? "" : before.lastError(),
                        "previousCycleAttemptCount", before.attemptCount(),
                        "totalAttemptCount", before.totalAttemptCount(),
                        "manualRetryCount", after.manualRetryCount()))));
    }

    private RulePublicationView mapPublication(java.sql.ResultSet rs) throws java.sql.SQLException {
        Timestamp publishedAt = rs.getTimestamp("published_at");
        Timestamp lastRequeuedAt = rs.getTimestamp("last_requeued_at");
        return new RulePublicationView(
                rs.getObject("id", UUID.class), rs.getString("status"), rs.getLong("revision"),
                rs.getInt("attempt_count"), rs.getInt("total_attempt_count"),
                rs.getInt("manual_retry_count"),
                publishedAt == null ? null : publishedAt.toInstant(),
                lastRequeuedAt == null ? null : lastRequeuedAt.toInstant(),
                rs.getString("last_error"));
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not serialize BPI outbox headers", exception);
        }
    }

    private Map<String, String> readHeaders(String value) {
        try {
            return objectMapper.readValue(value, new TypeReference<Map<String, String>>() {});
        } catch (Exception exception) {
            throw new IllegalStateException("Could not read BPI outbox headers", exception);
        }
    }

    private String truncate(String value) {
        if (value == null || value.isBlank()) return "Unknown Kafka publication failure";
        return value.length() <= 1000 ? value : value.substring(0, 1000);
    }
}
