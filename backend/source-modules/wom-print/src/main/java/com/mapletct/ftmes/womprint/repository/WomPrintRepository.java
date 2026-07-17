package com.mapletct.ftmes.womprint.repository;

import com.mapletct.ftmes.womprint.domain.PrinterConfig;
import com.mapletct.ftmes.womprint.domain.QrCodeRecord;
import com.mapletct.ftmes.womprint.domain.RequestSummary;
import com.mapletct.ftmes.womprint.domain.TaskContext;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Repository
public class WomPrintRepository {

    private static final String TASK_SQL =
        "SELECT t.id, t.table_no, t.produce_batch_num, t.product_id, t.line_id, "
            + "t.plan_start_time, m.code AS product_code, m.name AS product_name, "
            + "m.is_validity_manage, m.valid_period, m.valid_unit "
            + "FROM wom_produce_tasks t "
            + "LEFT JOIN baseset_materials m ON m.id = t.product_id "
            + "WHERE t.id = ? AND COALESCE(t.valid, TRUE) IS TRUE";

    private static final String INSERT_QR_SQL =
        "INSERT INTO wom_package_qrcodes ("
            + "tenant_id, request_id, request_hash, sequence_no, task_id, task_table_no, "
            + "produce_batch_num, product_id, product_code, product_name, printer_id, "
            + "print_host, print_port, manufacture_date, expiry_date, qr_code, qr_content, detail"
            + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private final JdbcTemplate jdbc;

    public WomPrintRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public TaskContext findTask(long taskId) {
        List<TaskContext> rows = jdbc.query(TASK_SQL, new Object[]{taskId}, (resultSet, rowNum) -> {
            Long productId = nullableLong(resultSet.getLong("product_id"), resultSet.wasNull());
            Long lineId = nullableLong(resultSet.getLong("line_id"), resultSet.wasNull());
            Timestamp planStart = resultSet.getTimestamp("plan_start_time");
            Object validityManaged = resultSet.getObject("is_validity_manage");
            int validPeriodValue = resultSet.getInt("valid_period");
            Integer validPeriod = resultSet.wasNull() ? null : validPeriodValue;
            return new TaskContext(
                resultSet.getLong("id"),
                resultSet.getString("table_no"),
                resultSet.getString("produce_batch_num"),
                productId,
                lineId,
                resultSet.getString("product_code"),
                resultSet.getString("product_name"),
                planStart == null ? null : planStart.toLocalDateTime(),
                validityManaged == null ? null : Boolean.valueOf(String.valueOf(validityManaged)),
                validPeriod,
                resultSet.getString("valid_unit")
            );
        });
        return rows.isEmpty() ? null : rows.get(0);
    }

    public PrinterConfig findPrinter(long printerId) {
        List<PrinterConfig> rows = jdbc.query(
            "SELECT id, COALESCE(print_name, table_no, id::text) AS print_name, "
                + "COALESCE(client_ip, '') AS print_host, COALESCE(bigintparama, 9100) AS print_port "
                + "FROM baseset_printers WHERE id = ? AND COALESCE(valid, TRUE) IS TRUE",
            new Object[]{printerId},
            (resultSet, rowNum) -> new PrinterConfig(
                resultSet.getLong("id"),
                resultSet.getString("print_name"),
                resultSet.getString("print_host"),
                resultSet.getInt("print_port")
            )
        );
        return rows.isEmpty() ? null : rows.get(0);
    }

    public List<PrinterConfig> listPrinters() {
        return jdbc.query(
            "SELECT id, COALESCE(print_name, table_no, id::text) AS print_name, "
                + "COALESCE(client_ip, '') AS print_host, COALESCE(bigintparama, 9100) AS print_port "
                + "FROM baseset_printers WHERE COALESCE(valid, TRUE) IS TRUE "
                + "ORDER BY COALESCE(sort, 0), id",
            (resultSet, rowNum) -> new PrinterConfig(
                resultSet.getLong("id"),
                resultSet.getString("print_name"),
                resultSet.getString("print_host"),
                resultSet.getInt("print_port")
            )
        );
    }

    public RequestSummary findRequestSummary(String tenantId, String requestId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
            "SELECT MIN(request_hash) AS request_hash, COUNT(*) AS record_count "
                + "FROM wom_package_qrcodes WHERE tenant_id = ? AND request_id = ?",
            tenantId,
            requestId
        );
        if (rows.isEmpty()) {
            return null;
        }
        Map<String, Object> row = rows.get(0);
        int count = ((Number) row.get("record_count")).intValue();
        if (count == 0) {
            return null;
        }
        return new RequestSummary(String.valueOf(row.get("request_hash")), count);
    }

    public void lockRequest(String tenantId, String requestId) {
        jdbc.queryForList(
            "SELECT pg_advisory_xact_lock(hashtextextended(?, 0))",
            tenantId + "|" + requestId
        );
    }

    public List<String> findRequestDetails(String tenantId, String requestId) {
        return jdbc.queryForList(
            "SELECT detail FROM wom_package_qrcodes "
                + "WHERE tenant_id = ? AND request_id = ? ORDER BY sequence_no",
            String.class,
            tenantId,
            requestId
        );
    }

    public void ensureDailySequence(String tenantId, LocalDate manufactureDate) {
        jdbc.update(
            "INSERT INTO wom_qrcode_daily_sequences (tenant_id, manufacture_date, last_sequence) "
                + "VALUES (?, ?, 0) ON CONFLICT (tenant_id, manufacture_date) DO NOTHING",
            tenantId,
            Date.valueOf(manufactureDate)
        );
    }

    public int lockDailySequence(String tenantId, LocalDate manufactureDate) {
        Integer value = jdbc.queryForObject(
            "SELECT last_sequence FROM wom_qrcode_daily_sequences "
                + "WHERE tenant_id = ? AND manufacture_date = ? FOR UPDATE",
            Integer.class,
            tenantId,
            Date.valueOf(manufactureDate)
        );
        return value == null ? 0 : value;
    }

    public void updateDailySequence(String tenantId, LocalDate manufactureDate, int nextValue) {
        int updated = jdbc.update(
            "UPDATE wom_qrcode_daily_sequences SET last_sequence = ?, updated_at = CURRENT_TIMESTAMP "
                + "WHERE tenant_id = ? AND manufacture_date = ?",
            nextValue,
            tenantId,
            Date.valueOf(manufactureDate)
        );
        if (updated != 1) {
            throw new IllegalStateException("WOM daily sequence row disappeared during generation");
        }
    }

    public void insertQrCodes(final List<QrCodeRecord> records) {
        if (records.isEmpty()) {
            return;
        }
        jdbc.batchUpdate(INSERT_QR_SQL, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement statement, int index) throws SQLException {
                QrCodeRecord record = records.get(index);
                PrinterConfig printer = record.getPrinter();
                TaskContext task = record.getTask();
                statement.setString(1, record.getTenantId());
                statement.setString(2, record.getRequestId());
                statement.setString(3, record.getRequestHash());
                statement.setInt(4, record.getSequenceNo());
                statement.setLong(5, task.getId());
                statement.setString(6, task.getTableNo());
                statement.setString(7, task.getProduceBatchNum());
                setNullableLong(statement, 8, task.getProductId());
                statement.setString(9, task.getProductCode());
                statement.setString(10, task.getProductName());
                setNullableLong(statement, 11, printer == null ? null : printer.getId());
                statement.setString(12, printer == null ? null : printer.getHost());
                if (printer == null || printer.getPort() == null) {
                    statement.setNull(13, java.sql.Types.INTEGER);
                } else {
                    statement.setInt(13, printer.getPort());
                }
                statement.setDate(14, Date.valueOf(record.getManufactureDate()));
                statement.setDate(15, Date.valueOf(record.getExpiryDate()));
                statement.setString(16, record.getQrCode());
                statement.setString(17, record.getQrContent());
                statement.setString(18, record.getDetail());
            }

            @Override
            public int getBatchSize() {
                return records.size();
            }
        });
    }

    public int backfillPrintState(String tenantId, String detail, boolean printed) {
        return jdbc.update(
            "UPDATE wom_package_qrcodes SET is_print = ?, "
                + "print_count = print_count + CASE WHEN ? AND is_print IS NOT TRUE THEN 1 ELSE 0 END, "
                + "printed_at = CASE WHEN ? THEN COALESCE(printed_at, CURRENT_TIMESTAMP) ELSE NULL END, "
                + "updated_at = CURRENT_TIMESTAMP WHERE tenant_id = ? AND detail = ?",
            printed,
            printed,
            printed,
            tenantId,
            detail
        );
    }

    public List<Map<String, Object>> listTaskRecords(String tenantId, long taskId, int limit) {
        return jdbc.queryForList(
            "SELECT id, request_id AS \"requestId\", sequence_no AS \"sequenceNo\", "
                + "task_id AS \"taskId\", task_table_no AS \"taskTableNo\", "
                + "produce_batch_num AS \"produceBatchNum\", product_code AS \"productCode\", "
                + "product_name AS \"productName\", manufacture_date AS \"manufactureDate\", "
                + "expiry_date AS \"expiryDate\", qr_code AS \"qrCode\", "
                + "qr_content AS \"qrContent\", detail, is_print AS \"isPrint\", "
                + "print_count AS \"printCount\", created_at AS \"createdAt\", "
                + "printed_at AS \"printedAt\" "
                + "FROM wom_package_qrcodes WHERE tenant_id = ? AND task_id = ? "
                + "ORDER BY created_at DESC, sequence_no DESC LIMIT ?",
            tenantId,
            taskId,
            limit
        );
    }

    public String findQrContent(String tenantId, String qrCode) {
        List<String> rows = jdbc.queryForList(
            "SELECT qr_content FROM wom_package_qrcodes WHERE tenant_id = ? AND qr_code = ?",
            String.class,
            tenantId,
            qrCode
        );
        return rows.isEmpty() ? null : rows.get(0);
    }

    private static Long nullableLong(long value, boolean wasNull) {
        return wasNull ? null : value;
    }

    private static void setNullableLong(PreparedStatement statement, int index, Long value) throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.BIGINT);
        } else {
            statement.setLong(index, value);
        }
    }
}
