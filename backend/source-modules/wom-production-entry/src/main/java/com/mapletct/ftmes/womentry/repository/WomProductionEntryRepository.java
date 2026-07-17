package com.mapletct.ftmes.womentry.repository;

import com.mapletct.ftmes.womentry.domain.ManualTaskRequestRecord;
import com.mapletct.ftmes.womentry.domain.ProductionOption;
import com.mapletct.ftmes.womentry.domain.TaskResult;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Locale;

@Repository
public class WomProductionEntryRepository {

    private static final String OPTION_SELECT =
        "SELECT m.id AS product_id, m.code AS product_code, m.name AS product_name, "
            + "f.id AS formula_id, f.formual_code AS formula_code, f.formula_name, "
            + "h.id AS line_id, h.code AS line_code, h.name AS line_name, "
            + "u.id AS unit_id, u.name AS unit_name, u.symbol AS unit_symbol "
            + "FROM rm_line_formulas lf "
            + "JOIN rm_formulas f ON f.id = lf.formula_id AND COALESCE(f.valid, TRUE) IS TRUE "
            + "JOIN baseset_materials m ON m.id = f.product_id AND COALESCE(m.valid, TRUE) IS TRUE "
            + "JOIN hm_factory_models h ON h.id = lf.line_id AND COALESCE(h.valid, TRUE) IS TRUE "
            + "LEFT JOIN baseset_units u ON u.id = m.produce_unit "
            + "AND (u.valid IS NULL OR LOWER(u.valid::text) IN ('true', 't', '1')) "
            + "WHERE COALESCE(lf.valid, TRUE) IS TRUE ";

    private final JdbcTemplate jdbc;

    public WomProductionEntryRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<ProductionOption> listOptions(String keyword, int limit) {
        String normalized = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        String pattern = "%" + normalized + "%";
        return jdbc.query(
            OPTION_SELECT
                + "AND (? = '' OR LOWER(COALESCE(m.code, '') || ' ' || COALESCE(m.name, '') || ' ' "
                + "|| COALESCE(f.formual_code, '') || ' ' || COALESCE(f.formula_name, '') || ' ' "
                + "|| COALESCE(h.code, '') || ' ' || COALESCE(h.name, '')) LIKE ?) "
                + "ORDER BY m.code, f.formual_code, h.code LIMIT ?",
            new Object[]{normalized, pattern, limit},
            (resultSet, rowNum) -> mapOption(resultSet)
        );
    }

    public ProductionOption findOption(String productCode, String formulaCode, long lineId) {
        List<ProductionOption> rows = jdbc.query(
            OPTION_SELECT
                + "AND m.code = ? AND f.formual_code = ? AND h.id = ? "
                + "ORDER BY f.id LIMIT 2",
            new Object[]{productCode, formulaCode, lineId},
            (resultSet, rowNum) -> mapOption(resultSet)
        );
        return rows.isEmpty() ? null : rows.get(0);
    }

    public TaskResult findActiveTaskByBatch(String batchCode) {
        List<TaskResult> rows = jdbc.query(
            "SELECT t.id AS task_id, t.version, t.table_no, t.produce_batch_num, "
                + "COALESCE(t.status, 0) AS task_status, COALESCE(t.valid, TRUE) AS task_valid, "
                + "p.id AS pending_id, p.open_url AS pending_open_url, p.activity_name "
                + "FROM wom_produce_tasks t "
                + "LEFT JOIN LATERAL ("
                + "  SELECT wp.id, wp.open_url, wp.activity_name "
                + "  FROM wfm_task_pending wp "
                + "  WHERE wp.model_id = t.id AND wp.task_status = 88 "
                + "  ORDER BY wp.create_time DESC NULLS LAST, wp.id DESC LIMIT 1"
                + ") p ON TRUE "
                + "WHERE LOWER(TRIM(t.produce_batch_num)) = LOWER(TRIM(?)) "
                + "AND COALESCE(t.valid, TRUE) IS TRUE "
                + "ORDER BY t.create_time DESC NULLS LAST, t.id DESC LIMIT 2",
            new Object[]{batchCode},
            (resultSet, rowNum) -> {
                long pendingValue = resultSet.getLong("pending_id");
                Long pendingId = resultSet.wasNull() ? null : Long.valueOf(pendingValue);
                return new TaskResult(
                    resultSet.getLong("task_id"),
                    resultSet.getInt("version"),
                    resultSet.getString("table_no"),
                    resultSet.getString("produce_batch_num"),
                    resultSet.getInt("task_status"),
                    resultSet.getBoolean("task_valid"),
                    pendingId,
                    resultSet.getString("pending_open_url"),
                    resultSet.getString("activity_name")
                );
            }
        );
        return rows.isEmpty() ? null : rows.get(0);
    }

    public ManualTaskRequestRecord findRequest(String tenantId, String requestId) {
        List<ManualTaskRequestRecord> rows = jdbc.query(
            "SELECT tenant_id, request_id, request_hash, batch_code, status, task_id, updated_at "
                + "FROM wom_manual_task_requests WHERE tenant_id = ? AND request_id = ?",
            new Object[]{tenantId, requestId},
            (resultSet, rowNum) -> {
                long taskValue = resultSet.getLong("task_id");
                Long taskId = resultSet.wasNull() ? null : Long.valueOf(taskValue);
                Timestamp updatedAt = resultSet.getTimestamp("updated_at");
                return new ManualTaskRequestRecord(
                    resultSet.getString("tenant_id"),
                    resultSet.getString("request_id"),
                    resultSet.getString("request_hash"),
                    resultSet.getString("batch_code"),
                    resultSet.getString("status"),
                    taskId,
                    updatedAt == null ? null : updatedAt.toLocalDateTime()
                );
            }
        );
        return rows.isEmpty() ? null : rows.get(0);
    }

    public boolean insertRequest(
            String tenantId,
            String requestId,
            String requestHash,
            String batchCode,
            String payloadJson) {
        return jdbc.update(
            "INSERT INTO wom_manual_task_requests "
                + "(tenant_id, request_id, request_hash, batch_code, payload_json, status) "
                + "VALUES (?, ?, ?, ?, CAST(? AS jsonb), 'PROCESSING') "
                + "ON CONFLICT DO NOTHING",
            tenantId,
            requestId,
            requestHash,
            batchCode,
            payloadJson
        ) == 1;
    }

    public boolean retryRequest(String tenantId, String requestId) {
        try {
            int updated = jdbc.update(
                "UPDATE wom_manual_task_requests SET status = 'PROCESSING', error_message = NULL, "
                    + "updated_at = CURRENT_TIMESTAMP WHERE tenant_id = ? AND request_id = ?",
                tenantId,
                requestId
            );
            if (updated != 1) {
                throw new IllegalStateException("Manual task request disappeared before retry");
            }
            return true;
        } catch (DuplicateKeyException exception) {
            return false;
        }
    }

    public void markSuccess(
            String tenantId,
            String requestId,
            long taskId,
            String upstreamResponseJson) {
        int updated = jdbc.update(
            "UPDATE wom_manual_task_requests SET status = 'SUCCESS', task_id = ?, "
                + "upstream_response = CAST(? AS jsonb), error_message = NULL, updated_at = CURRENT_TIMESTAMP "
                + "WHERE tenant_id = ? AND request_id = ?",
            taskId,
            upstreamResponseJson,
            tenantId,
            requestId
        );
        if (updated != 1) {
            throw new IllegalStateException("Manual task request disappeared before success update");
        }
    }

    public void markFailed(String tenantId, String requestId, String errorMessage) {
        jdbc.update(
            "UPDATE wom_manual_task_requests SET status = 'FAILED', error_message = ?, "
                + "updated_at = CURRENT_TIMESTAMP WHERE tenant_id = ? AND request_id = ?",
            truncate(errorMessage, 1000),
            tenantId,
            requestId
        );
    }

    private static ProductionOption mapOption(java.sql.ResultSet resultSet) throws java.sql.SQLException {
        long unitValue = resultSet.getLong("unit_id");
        Long unitId = resultSet.wasNull() ? null : Long.valueOf(unitValue);
        return new ProductionOption(
            resultSet.getLong("product_id"),
            resultSet.getString("product_code"),
            resultSet.getString("product_name"),
            resultSet.getLong("formula_id"),
            resultSet.getString("formula_code"),
            resultSet.getString("formula_name"),
            resultSet.getLong("line_id"),
            resultSet.getString("line_code"),
            resultSet.getString("line_name"),
            unitId,
            resultSet.getString("unit_name"),
            resultSet.getString("unit_symbol")
        );
    }

    private static String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
