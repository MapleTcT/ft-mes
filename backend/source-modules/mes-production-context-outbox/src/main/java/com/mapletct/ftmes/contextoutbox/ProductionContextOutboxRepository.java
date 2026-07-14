package com.mapletct.ftmes.contextoutbox;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

@Repository
public class ProductionContextOutboxRepository {

    private static final String CLAIM_SQL =
        "WITH candidates AS (" +
        " SELECT id FROM wom_bpi_production_context_outbox" +
        " WHERE ((publication_state IN ('READY', 'RETRY') AND next_attempt_at <= CURRENT_TIMESTAMP)" +
        "    OR (publication_state = 'SENDING' AND claimed_at < CURRENT_TIMESTAMP - (? * INTERVAL '1 millisecond')))" +
        " ORDER BY id FOR UPDATE SKIP LOCKED LIMIT ?" +
        ") UPDATE wom_bpi_production_context_outbox target" +
        " SET publication_state = 'SENDING', claimed_at = CURRENT_TIMESTAMP, claimed_by = ?," +
        "     attempt_count = attempt_count + 1, updated_at = CURRENT_TIMESTAMP" +
        " FROM candidates WHERE target.id = candidates.id" +
        " RETURNING target.*";

    private final JdbcTemplate jdbcTemplate;

    public ProductionContextOutboxRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<ProductionContextOutboxRecord> claim(String instanceId, int batchSize, long claimTimeoutMs) {
        return jdbcTemplate.query(CLAIM_SQL, new Object[] { claimTimeoutMs, batchSize, instanceId }, mapper());
    }

    public void markSent(long id, String instanceId) {
        int updated = jdbcTemplate.update(
            "UPDATE wom_bpi_production_context_outbox" +
                " SET publication_state = 'SENT', sent_at = CURRENT_TIMESTAMP, claimed_at = NULL," +
                " claimed_by = NULL, last_error = NULL, updated_at = CURRENT_TIMESTAMP" +
                " WHERE id = ? AND publication_state = 'SENDING' AND claimed_by = ?",
            id,
            instanceId
        );
        requireUpdated(updated, id, "mark sent");
    }

    public void markRetry(long id, String instanceId, long delayMs, String error) {
        int updated = jdbcTemplate.update(
            "UPDATE wom_bpi_production_context_outbox" +
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
            "UPDATE wom_bpi_production_context_outbox" +
                " SET publication_state = 'DEAD', claimed_at = NULL, claimed_by = NULL," +
                " last_error = ?, updated_at = CURRENT_TIMESTAMP" +
                " WHERE id = ? AND publication_state = 'SENDING' AND claimed_by = ?",
            limit(error, 2000),
            id,
            instanceId
        );
        requireUpdated(updated, id, "mark dead");
    }

    private static RowMapper<ProductionContextOutboxRecord> mapper() {
        return new RowMapper<ProductionContextOutboxRecord>() {
            @Override
            public ProductionContextOutboxRecord mapRow(ResultSet result, int rowNumber) throws SQLException {
                Timestamp effectiveTo = result.getTimestamp("effective_to");
                return new ProductionContextOutboxRecord(
                    result.getLong("id"),
                    result.getString("event_id"),
                    result.getString("topic"),
                    nullableLong(result, "wom_cid"),
                    nullableLong(result, "wom_line_id"),
                    result.getString("tenant_id"),
                    result.getString("plant_id"),
                    result.getString("line_id"),
                    result.getString("order_id"),
                    result.getString("task_id"),
                    result.getString("material_code"),
                    result.getString("recipe_version"),
                    result.getString("batch_id"),
                    result.getString("source_state"),
                    result.getLong("context_revision"),
                    result.getBoolean("active"),
                    result.getTimestamp("effective_from").getTime(),
                    effectiveTo == null ? null : effectiveTo.getTime(),
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
            throw new IllegalStateException("could not " + action + " for outbox row " + id);
        }
    }
}
