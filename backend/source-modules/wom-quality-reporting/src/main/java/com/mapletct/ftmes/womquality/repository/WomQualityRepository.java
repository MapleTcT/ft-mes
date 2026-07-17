package com.mapletct.ftmes.womquality.repository;

import com.mapletct.ftmes.womquality.domain.WomQualityBusinessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Repository
public class WomQualityRepository {

    private final JdbcTemplate jdbc;
    private final DataSource dataSource;
    private volatile Boolean postgres;

    public WomQualityRepository(JdbcTemplate jdbc, DataSource dataSource) {
        this.jdbc = jdbc;
        this.dataSource = dataSource;
    }

    public List<Map<String, Object>> listTasks(
            String tenantId, String keyword, int page, int size) {
        String pattern = "%" + normalizeKeyword(keyword) + "%";
        return jdbc.queryForList(
            "SELECT t.id, t.table_no AS task_no, t.produce_batch_num AS batch_no, "
                + "t.plan_num, t.finish_num, t.task_run_state, "
                + "COUNT(DISTINCT o.id) AS output_count, "
                + "COALESCE(SUM(COALESCE(o.report_num, o.output_num)), 0) AS reported_quantity, "
                + "COALESCE(SUM(CASE WHEN q.status IN ('CONFIRMED', 'REVERSAL_PENDING') "
                + "THEN q.bad_quantity ELSE 0 END), 0) AS bad_quantity "
                + "FROM wom_produce_tasks t "
                + "LEFT JOIN wom_produce_task_exelog e ON e.task_id = t.id AND COALESCE(e.valid, TRUE) "
                + "LEFT JOIN wom_mat_outpt_records o ON o.task_exelog_id = e.id AND COALESCE(o.valid, TRUE) "
                + "LEFT JOIN wom_quality_quantity_reports q ON q.tenant_id = ? "
                + "AND q.source_output_id = o.id AND q.status IN ('CONFIRMED', 'REVERSAL_PENDING') "
                + "WHERE COALESCE(t.valid, TRUE) AND (LOWER(COALESCE(t.table_no, '')) LIKE ? "
                + "OR LOWER(COALESCE(t.produce_batch_num, '')) LIKE ?) "
                + "GROUP BY t.id, t.table_no, t.produce_batch_num, t.plan_num, t.finish_num, t.task_run_state "
                + "ORDER BY t.create_time DESC, t.id DESC LIMIT ? OFFSET ?",
            tenantId, pattern, pattern, size, page * size
        );
    }

    public long countTasks(String keyword) {
        String pattern = "%" + normalizeKeyword(keyword) + "%";
        Long count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM wom_produce_tasks t WHERE COALESCE(t.valid, TRUE) "
                + "AND (LOWER(COALESCE(t.table_no, '')) LIKE ? "
                + "OR LOWER(COALESCE(t.produce_batch_num, '')) LIKE ?)",
            Long.class, pattern, pattern
        );
        return count == null ? 0L : count.longValue();
    }

    public List<Map<String, Object>> listOutputs(String tenantId, long taskId) {
        return jdbc.queryForList(
            "SELECT o.id, o.table_no AS output_no, o.report_num, o.output_num, "
                + "COALESCE(o.report_num, o.output_num) AS authoritative_quantity, "
                + "o.mat_batch_num, o.produce_batch_num, o.material_id, o.sync_state, o.create_time, "
                + "q.id AS active_report_id, q.bad_quantity, q.good_quantity, q.status AS report_status, "
                + "q.wms_sync_state "
                + "FROM wom_produce_task_exelog e "
                + "JOIN wom_mat_outpt_records o ON o.task_exelog_id = e.id AND COALESCE(o.valid, TRUE) "
                + "LEFT JOIN wom_quality_quantity_reports q ON q.tenant_id = ? "
                + "AND q.source_output_id = o.id AND q.status IN ('CONFIRMED', 'REVERSAL_PENDING') "
                + "WHERE e.task_id = ? AND COALESCE(e.valid, TRUE) "
                + "ORDER BY o.create_time DESC, o.id DESC",
            tenantId, taskId
        );
    }

    public Map<String, Object> findTaskOutput(long taskId, long outputId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
            "SELECT t.id AS task_id, t.table_no AS task_no, t.produce_batch_num AS task_batch_no, "
                + "t.plan_num, t.finish_num, o.id AS source_output_id, o.table_no AS source_output_no, "
                + "o.report_num, o.output_num, COALESCE(o.report_num, o.output_num) AS reported_quantity, "
                + "COALESCE(o.mat_batch_num, o.produce_batch_num, t.produce_batch_num) AS batch_no "
                + "FROM wom_produce_tasks t "
                + "JOIN wom_produce_task_exelog e ON e.task_id = t.id AND COALESCE(e.valid, TRUE) "
                + "JOIN wom_mat_outpt_records o ON o.task_exelog_id = e.id AND COALESCE(o.valid, TRUE) "
                + "WHERE t.id = ? AND o.id = ? AND COALESCE(t.valid, TRUE)",
            taskId, outputId
        );
        return rows.isEmpty() ? null : rows.get(0);
    }

    public Map<String, Object> findByRequest(String tenantId, String requestId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
            "SELECT * FROM wom_quality_quantity_reports WHERE tenant_id = ? AND request_id = ?",
            tenantId, requestId
        );
        return rows.isEmpty() ? null : rows.get(0);
    }

    public Map<String, Object> findActiveByOutput(String tenantId, long outputId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
            "SELECT * FROM wom_quality_quantity_reports WHERE tenant_id = ? AND source_output_id = ? "
                + "AND status IN ('CONFIRMED', 'REVERSAL_PENDING')",
            tenantId, outputId
        );
        return rows.isEmpty() ? null : rows.get(0);
    }

    public Map<String, Object> findReport(String tenantId, long reportId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
            "SELECT * FROM wom_quality_quantity_reports WHERE tenant_id = ? AND id = ?",
            tenantId, reportId
        );
        return rows.isEmpty() ? null : rows.get(0);
    }

    public Map<String, Object> lockReport(String tenantId, long reportId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
            "SELECT * FROM wom_quality_quantity_reports WHERE tenant_id = ? AND id = ? FOR UPDATE",
            tenantId, reportId
        );
        return rows.isEmpty() ? null : rows.get(0);
    }

    public Long insertReport(
            final String tenantId,
            final String requestId,
            final String requestHash,
            final Map<String, Object> source,
            final BigDecimal reportedQuantity,
            final BigDecimal goodQuantity,
            final BigDecimal badQuantity,
            final String unitCode,
            final String reasonCode,
            final String reasonText,
            final Map<String, Object> qualityLinks,
            final String actor) {
        String sql = "INSERT INTO wom_quality_quantity_reports ("
            + "tenant_id, request_id, request_hash, task_id, task_no, source_output_id, source_output_no, "
            + "batch_no, reported_quantity, good_quantity, bad_quantity, unit_code, reason_code, reason_text, "
            + "qcs_inspect_id, qcs_report_id, qcs_deal_id, status, wms_sync_state, confirmed_by"
            + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'CONFIRMED', 'PENDING', ?)";
        if (isPostgres()) {
            sql += " ON CONFLICT DO NOTHING";
        }
        final String insertSql = sql;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        try {
            int inserted = jdbc.update(new PreparedStatementCreator() {
                @Override
                public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
                    PreparedStatement statement = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS);
                    int index = 1;
                    statement.setString(index++, tenantId);
                    statement.setString(index++, requestId);
                    statement.setString(index++, requestHash);
                    statement.setLong(index++, number(source.get("task_id")).longValue());
                    statement.setString(index++, string(source.get("task_no")));
                    statement.setLong(index++, number(source.get("source_output_id")).longValue());
                    statement.setString(index++, string(source.get("source_output_no")));
                    statement.setString(index++, string(source.get("batch_no")));
                    statement.setBigDecimal(index++, reportedQuantity);
                    statement.setBigDecimal(index++, goodQuantity);
                    statement.setBigDecimal(index++, badQuantity);
                    statement.setString(index++, unitCode);
                    statement.setString(index++, reasonCode);
                    statement.setString(index++, reasonText);
                    setNullableLong(statement, index++, qualityLinks.get("qcs_inspect_id"));
                    setNullableLong(statement, index++, qualityLinks.get("qcs_report_id"));
                    setNullableLong(statement, index++, qualityLinks.get("qcs_deal_id"));
                    statement.setString(index, actor);
                    return statement;
                }
            }, keyHolder);
            if (inserted == 0) {
                return null;
            }
        } catch (DuplicateKeyException exception) {
            return null;
        }
        Map<String, Object> keys = keyHolder.getKeys();
        Number key = keys != null && keys.get("id") instanceof Number
            ? (Number) keys.get("id") : null;
        if (key == null) {
            throw new WomQualityBusinessException(500, "不良数量登记创建后未返回主键");
        }
        return key.longValue();
    }

    public Map<String, Object> latestQualityLinks(long taskId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
            "SELECT i.id AS qcs_inspect_id, r.id AS qcs_report_id, d.id AS qcs_deal_id "
                + "FROM qcs_inspects i "
                + "LEFT JOIN qcs_inspect_reports r ON r.inspect_id = i.id AND COALESCE(r.valid, TRUE) "
                + "LEFT JOIN qcs_un_qlf_deals d ON d.report_id = r.id AND COALESCE(d.valid, TRUE) "
                + "WHERE i.source_id = ? AND COALESCE(i.valid, TRUE) "
                + "ORDER BY i.create_time DESC, r.create_time DESC, d.create_time DESC LIMIT 1",
            taskId
        );
        return rows.isEmpty() ? Collections.<String, Object>emptyMap() : rows.get(0);
    }

    public Map<String, Object> qualityContext(long inspectId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
            "SELECT i.id AS qcs_inspect_id, i.source_id AS task_id, "
                + "t.table_no AS task_no, t.produce_batch_num AS batch_no "
                + "FROM qcs_inspects i "
                + "JOIN wom_produce_tasks t ON t.id = i.source_id AND COALESCE(t.valid, TRUE) "
                + "WHERE i.id = ? AND COALESCE(i.valid, TRUE)",
            inspectId
        );
        return rows.isEmpty() ? null : rows.get(0);
    }

    public void linkLatestQuality(
            String tenantId, long reportId, long version, Map<String, Object> qualityLinks) {
        int updated = jdbc.update(
            "UPDATE wom_quality_quantity_reports SET qcs_inspect_id = ?, qcs_report_id = ?, qcs_deal_id = ?, "
                + "version = version + 1, updated_at = CURRENT_TIMESTAMP "
                + "WHERE tenant_id = ? AND id = ? AND version = ?",
            qualityLinks.get("qcs_inspect_id"), qualityLinks.get("qcs_report_id"),
            qualityLinks.get("qcs_deal_id"), tenantId, reportId, version
        );
        if (updated != 1) {
            throw new WomQualityBusinessException(409, "关联质检记录时发生并发冲突，请刷新后重试");
        }
    }

    public void addEvent(long reportId, String eventType, String actor, String reason, String payload) {
        Long next = jdbc.queryForObject(
            "SELECT COALESCE(MAX(event_no), 0) + 1 FROM wom_quality_quantity_events WHERE report_id = ?",
            Long.class, reportId
        );
        jdbc.update(
            "INSERT INTO wom_quality_quantity_events "
                + "(report_id, event_no, event_type, actor, reason, payload) VALUES (?, ?, ?, ?, ?, ?)",
            reportId, next == null ? 1L : next.longValue(), eventType, actor, reason, payload
        );
    }

    public void markSyncApplied(String tenantId, long reportId, long version) {
        int updated = jdbc.update(
            "UPDATE wom_quality_quantity_reports SET wms_sync_state = 'APPLIED', wms_sync_message = NULL, "
                + "wms_synced_at = CURRENT_TIMESTAMP, version = version + 1, updated_at = CURRENT_TIMESTAMP "
                + "WHERE tenant_id = ? AND id = ? AND version = ?",
            tenantId, reportId, version
        );
        requireUpdated(updated, "WMS 同步回写发生并发冲突");
    }

    public void markSyncFailed(String tenantId, long reportId, long version, String message) {
        int updated = jdbc.update(
            "UPDATE wom_quality_quantity_reports SET wms_sync_state = 'FAILED', wms_sync_message = ?, "
                + "version = version + 1, updated_at = CURRENT_TIMESTAMP "
                + "WHERE tenant_id = ? AND id = ? AND version = ?",
            limit(message, 1000), tenantId, reportId, version
        );
        requireUpdated(updated, "WMS 同步失败状态回写发生并发冲突");
    }

    public void requestReversal(
            String tenantId, long reportId, long version, String reason, String actor) {
        int updated = jdbc.update(
            "UPDATE wom_quality_quantity_reports SET status = 'REVERSAL_PENDING', "
                + "wms_sync_state = 'PENDING', wms_sync_message = NULL, reversal_reason = ?, reversed_by = ?, "
                + "version = version + 1, updated_at = CURRENT_TIMESTAMP "
                + "WHERE tenant_id = ? AND id = ? AND version = ? AND status = 'CONFIRMED'",
            reason, actor, tenantId, reportId, version
        );
        requireUpdated(updated, "登记状态已变化，请刷新后重试冲销");
    }

    public void markReversed(String tenantId, long reportId, long version) {
        int updated = jdbc.update(
            "UPDATE wom_quality_quantity_reports SET status = 'REVERSED', wms_sync_state = 'APPLIED', "
                + "wms_sync_message = NULL, wms_synced_at = CURRENT_TIMESTAMP, reversed_at = CURRENT_TIMESTAMP, "
                + "version = version + 1, updated_at = CURRENT_TIMESTAMP "
                + "WHERE tenant_id = ? AND id = ? AND version = ? AND status = 'REVERSAL_PENDING'",
            tenantId, reportId, version
        );
        requireUpdated(updated, "冲销完成回写发生并发冲突");
    }

    public List<Map<String, Object>> listReports(
            String tenantId, Long taskId, String keyword, int page, int size) {
        String pattern = "%" + normalizeKeyword(keyword) + "%";
        if (taskId == null) {
            return jdbc.queryForList(
                "SELECT * FROM wom_quality_quantity_reports WHERE tenant_id = ? "
                    + "AND (LOWER(COALESCE(task_no, '')) LIKE ? OR LOWER(COALESCE(batch_no, '')) LIKE ? "
                    + "OR LOWER(COALESCE(reason_text, '')) LIKE ?) "
                    + "ORDER BY created_at DESC, id DESC LIMIT ? OFFSET ?",
                tenantId, pattern, pattern, pattern, size, page * size
            );
        }
        return jdbc.queryForList(
            "SELECT * FROM wom_quality_quantity_reports WHERE tenant_id = ? "
                + "AND task_id = ? "
                + "AND (LOWER(COALESCE(task_no, '')) LIKE ? OR LOWER(COALESCE(batch_no, '')) LIKE ? "
                + "OR LOWER(COALESCE(reason_text, '')) LIKE ?) "
                + "ORDER BY created_at DESC, id DESC LIMIT ? OFFSET ?",
            tenantId, taskId, pattern, pattern, pattern, size, page * size
        );
    }

    public long countReports(String tenantId, Long taskId, String keyword) {
        String pattern = "%" + normalizeKeyword(keyword) + "%";
        if (taskId == null) {
            Long count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM wom_quality_quantity_reports WHERE tenant_id = ? "
                    + "AND (LOWER(COALESCE(task_no, '')) LIKE ? OR LOWER(COALESCE(batch_no, '')) LIKE ? "
                    + "OR LOWER(COALESCE(reason_text, '')) LIKE ?)",
                Long.class, tenantId, pattern, pattern, pattern
            );
            return count == null ? 0L : count.longValue();
        }
        Long count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM wom_quality_quantity_reports WHERE tenant_id = ? "
                + "AND task_id = ? "
                + "AND (LOWER(COALESCE(task_no, '')) LIKE ? OR LOWER(COALESCE(batch_no, '')) LIKE ? "
                + "OR LOWER(COALESCE(reason_text, '')) LIKE ?)",
            Long.class, tenantId, taskId, pattern, pattern, pattern
        );
        return count == null ? 0L : count.longValue();
    }

    public List<Map<String, Object>> events(long reportId) {
        return jdbc.queryForList(
            "SELECT * FROM wom_quality_quantity_events WHERE report_id = ? ORDER BY event_no", reportId);
    }

    public List<Map<String, Object>> syncCandidates(int limit) {
        return jdbc.queryForList(
            "SELECT id, tenant_id FROM wom_quality_quantity_reports "
                + "WHERE wms_sync_state IN ('PENDING', 'FAILED') "
                + "AND status IN ('CONFIRMED', 'REVERSAL_PENDING') "
                + "ORDER BY updated_at, id LIMIT ?",
            limit
        );
    }

    private boolean isPostgres() {
        Boolean cached = postgres;
        if (cached != null) {
            return cached.booleanValue();
        }
        try (Connection connection = dataSource.getConnection()) {
            boolean detected = connection.getMetaData().getDatabaseProductName()
                .toLowerCase().contains("postgresql");
            postgres = Boolean.valueOf(detected);
            return detected;
        } catch (SQLException exception) {
            throw new IllegalStateException("无法识别 wom-quality-reporting 数据库类型", exception);
        }
    }

    private static void setNullableLong(PreparedStatement statement, int index, Object value)
            throws SQLException {
        if (value == null) {
            statement.setObject(index, null);
        } else {
            statement.setLong(index, number(value).longValue());
        }
    }

    private static void requireUpdated(int updated, String message) {
        if (updated != 1) {
            throw new WomQualityBusinessException(409, message);
        }
    }

    private static String normalizeKeyword(String keyword) {
        return keyword == null ? "" : keyword.trim().toLowerCase();
    }

    private static String limit(String value, int maxLength) {
        String normalized = value == null ? "" : value;
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }

    private static Number number(Object value) {
        if (value instanceof Number) {
            return (Number) value;
        }
        return new BigDecimal(String.valueOf(value));
    }

    private static String string(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
