package com.mapletct.ftmes.qcsoutbox;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

@Repository
@ConditionalOnProperty(name = "qcs.bpi.outbox.enabled", havingValue = "true")
public class QcsQualityGateOutboxRepository {

    private static final String CLAIM_SQL =
        "WITH candidates AS (" +
        " SELECT id FROM qcs_bpi_quality_gate_outbox" +
        " WHERE ((publication_state IN ('READY', 'RETRY') AND next_attempt_at <= CURRENT_TIMESTAMP)" +
        "    OR (publication_state = 'SENDING' AND claimed_at < CURRENT_TIMESTAMP - (? * INTERVAL '1 millisecond')))" +
        " ORDER BY id FOR UPDATE SKIP LOCKED LIMIT ?" +
        ") UPDATE qcs_bpi_quality_gate_outbox target" +
        " SET publication_state = 'SENDING', claimed_at = CURRENT_TIMESTAMP, claimed_by = ?," +
        "     attempt_count = attempt_count + 1, updated_at = CURRENT_TIMESTAMP" +
        " FROM candidates WHERE target.id = candidates.id" +
        " RETURNING target.*";

    private final JdbcTemplate jdbcTemplate;

    public QcsQualityGateOutboxRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<QcsQualityGateOutboxRecord> claim(String instanceId, int batchSize, long claimTimeoutMs) {
        return jdbcTemplate.query(CLAIM_SQL, new Object[] { claimTimeoutMs, batchSize, instanceId }, mapper());
    }

    public void markSent(long id, String instanceId, UUID resolvedBatchId, String payloadSha256) {
        int updated = jdbcTemplate.update(
            "UPDATE qcs_bpi_quality_gate_outbox" +
                " SET publication_state = 'SENT', sent_at = CURRENT_TIMESTAMP, claimed_at = NULL," +
                " claimed_by = NULL, last_error = NULL, resolved_batch_id = ?, payload_sha256 = ?," +
                " updated_at = CURRENT_TIMESTAMP" +
                " WHERE id = ? AND publication_state = 'SENDING' AND claimed_by = ?",
            resolvedBatchId,
            payloadSha256,
            id,
            instanceId
        );
        requireUpdated(updated, id, "mark sent");
    }

    public void markRetry(long id, String instanceId, long delayMs, String error) {
        int updated = jdbcTemplate.update(
            "UPDATE qcs_bpi_quality_gate_outbox" +
                " SET publication_state = 'RETRY', next_attempt_at = CURRENT_TIMESTAMP + (? * INTERVAL '1 millisecond')," +
                " claimed_at = NULL, claimed_by = NULL, last_error = ?, updated_at = CURRENT_TIMESTAMP" +
                " WHERE id = ? AND publication_state = 'SENDING' AND claimed_by = ?",
            delayMs,
            limit(error, 2000),
            id,
            instanceId
        );
        requireUpdated(updated, id, "mark retry");
    }

    public void markDead(long id, String instanceId, String error) {
        int updated = jdbcTemplate.update(
            "UPDATE qcs_bpi_quality_gate_outbox" +
                " SET publication_state = 'DEAD', claimed_at = NULL, claimed_by = NULL," +
                " last_error = ?, updated_at = CURRENT_TIMESTAMP" +
                " WHERE id = ? AND publication_state = 'SENDING' AND claimed_by = ?",
            limit(error, 2000),
            id,
            instanceId
        );
        requireUpdated(updated, id, "mark dead");
    }

    private static RowMapper<QcsQualityGateOutboxRecord> mapper() {
        return new RowMapper<QcsQualityGateOutboxRecord>() {
            @Override
            public QcsQualityGateOutboxRecord mapRow(ResultSet result, int rowNumber) throws SQLException {
                return new QcsQualityGateOutboxRecord(
                    result.getLong("id"),
                    result.getString("event_id"),
                    result.getString("idempotency_key"),
                    result.getString("topic"),
                    result.getLong("qcs_report_id"),
                    result.getInt("qcs_report_version"),
                    nullableLong(result, "qcs_inspect_id"),
                    nullableLong(result, "wom_task_id"),
                    result.getString("tenant_id"),
                    result.getString("plant_id"),
                    result.getString("line_id"),
                    result.getString("source_order_id"),
                    result.getString("source_batch_code"),
                    result.getString("quality_gate_id"),
                    result.getLong("quality_gate_revision"),
                    result.getTimestamp("observed_at").getTime(),
                    result.getString("inspections"),
                    result.getInt("attempt_count")
                );
            }
        };
    }

    private static Long nullableLong(ResultSet result, String column) throws SQLException {
        long value = result.getLong(column);
        return result.wasNull() ? null : value;
    }

    private static String limit(String value, int maxLength) {
        String safe = value == null ? "unknown publication error" : value;
        return safe.length() <= maxLength ? safe : safe.substring(0, maxLength);
    }

    private static void requireUpdated(int updated, long id, String action) {
        if (updated != 1) {
            throw new IllegalStateException("could not " + action + " for QCS outbox row " + id);
        }
    }
}
