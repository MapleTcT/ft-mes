package com.mapletct.ftmes.bpi.infrastructure.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mapletct.ftmes.bpi.domain.OutboxEventClaim;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
public class WmsInboundOutboxRepository {
    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public WmsInboundOutboxRepository(
            NamedParameterJdbcTemplate jdbc,
            ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public List<OutboxEventClaim> claimPending(int batchSize, Duration claimTimeout) {
        MapSqlParameterSource timeout = new MapSqlParameterSource()
                .addValue("claimTimeoutMs", claimTimeout.toMillis());
        jdbc.update("""
                UPDATE bpi.bpi_outbox_events
                   SET status = 'PENDING', claim_token = NULL, claimed_at = NULL,
                       next_attempt_at = now(), updated_at = now(),
                       last_error = 'Recovered stale WMS dispatcher claim', revision = revision + 1
                 WHERE status = 'DISPATCHING'
                   AND aggregate_type = 'BATCH_INSTANCE'
                   AND event_type IN (
                        'WMS_COMPLETION_INBOUND_COMMAND',
                        'WMS_COMPLETION_INBOUND_REVERSAL_COMMAND'
                   )
                   AND (claimed_at IS NULL
                        OR claimed_at < now() - (:claimTimeoutMs * interval '1 millisecond'))
                """, timeout);
        UUID claimToken = UUID.randomUUID();
        return jdbc.query("""
                WITH selected AS (
                    SELECT id
                      FROM bpi.bpi_outbox_events
                     WHERE status = 'PENDING'
                       AND aggregate_type = 'BATCH_INSTANCE'
                       AND event_type IN (
                            'WMS_COMPLETION_INBOUND_COMMAND',
                            'WMS_COMPLETION_INBOUND_REVERSAL_COMMAND'
                       )
                       AND next_attempt_at <= now()
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
                   SET status = 'PUBLISHED', published_at = now(), claim_token = NULL,
                       claimed_at = NULL, last_error = NULL, revision = revision + 1,
                       updated_at = now()
                 WHERE id = :id
                   AND claim_token = :claimToken
                   AND status = 'DISPATCHING'
                   AND aggregate_type = 'BATCH_INSTANCE'
                   AND event_type IN (
                        'WMS_COMPLETION_INBOUND_COMMAND',
                        'WMS_COMPLETION_INBOUND_REVERSAL_COMMAND'
                   )
                """, new MapSqlParameterSource()
                .addValue("id", id)
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
                 WHERE id = :id
                   AND claim_token = :claimToken
                   AND status = 'DISPATCHING'
                   AND aggregate_type = 'BATCH_INSTANCE'
                   AND event_type IN (
                        'WMS_COMPLETION_INBOUND_COMMAND',
                        'WMS_COMPLETION_INBOUND_REVERSAL_COMMAND'
                   )
                """, new MapSqlParameterSource()
                .addValue("status", terminal ? "FAILED" : "PENDING")
                .addValue("retryDelayMs", delay)
                .addValue("lastError", truncate(error))
                .addValue("id", id)
                .addValue("claimToken", claimToken)) == 1;
    }

    private Map<String, String> readHeaders(String value) {
        try {
            return objectMapper.readValue(value, new TypeReference<Map<String, String>>() {});
        } catch (Exception error) {
            throw new IllegalStateException("Could not read WMS outbox headers", error);
        }
    }

    private String truncate(String value) {
        if (value == null || value.isBlank()) return "Unknown WMS publication failure";
        return value.length() <= 1000 ? value : value.substring(0, 1000);
    }
}
