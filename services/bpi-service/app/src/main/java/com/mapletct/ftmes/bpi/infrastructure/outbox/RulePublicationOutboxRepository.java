package com.mapletct.ftmes.bpi.infrastructure.outbox;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mapletct.ftmes.bpi.application.ActorContext;
import com.mapletct.ftmes.bpi.domain.OutboxEventClaim;
import com.mapletct.ftmes.bpi.domain.RulePublicationEnvelope;
import com.mapletct.ftmes.bpi.domain.RuleVersionView;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
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
        jdbc.update("""
                INSERT INTO bpi.bpi_outbox_events
                    (id, tenant_id, plant_id, line_id, aggregate_type, aggregate_id,
                     event_type, topic, partition_key, payload, headers)
                VALUES (:id, :tenantId, :plantId, :lineId, 'RULE_VERSION', :aggregateId,
                        'BOUNDARY_RULE_PUBLISHED', :topic, :partitionKey, :payload,
                        CAST(:headers AS jsonb))
                """, new MapSqlParameterSource()
                .addValue("id", event.eventId())
                .addValue("tenantId", actor.tenantId())
                .addValue("plantId", rule.plantId())
                .addValue("lineId", rule.lineId())
                .addValue("aggregateId", rule.id())
                .addValue("topic", event.topic())
                .addValue("partitionKey", event.partitionKey())
                .addValue("payload", event.payload())
                .addValue("headers", writeJson(event.headers())));
    }

    @Transactional
    public List<OutboxEventClaim> claimPending(
            int batchSize,
            Duration claimTimeout) {
        jdbc.update("""
                UPDATE bpi.bpi_outbox_events
                   SET status = 'PENDING', claim_token = NULL, claimed_at = NULL,
                       next_attempt_at = now(), updated_at = now(),
                       last_error = 'Recovered stale dispatcher claim'
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
                       updated_at = now()
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
                       updated_at = now()
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
                       last_error = :lastError, updated_at = now()
                 WHERE id = :id AND claim_token = :claimToken AND status = 'DISPATCHING'
                """, new MapSqlParameterSource()
                .addValue("status", terminal ? "FAILED" : "PENDING")
                .addValue("retryDelayMs", delay)
                .addValue("lastError", truncate(error))
                .addValue("id", id)
                .addValue("claimToken", claimToken)) == 1;
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
