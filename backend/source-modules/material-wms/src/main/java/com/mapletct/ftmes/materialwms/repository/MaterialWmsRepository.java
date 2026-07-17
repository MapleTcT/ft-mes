package com.mapletct.ftmes.materialwms.repository;

import com.mapletct.ftmes.materialwms.api.StockDocumentLineRequest;
import com.mapletct.ftmes.materialwms.api.StockDocumentRequest;
import com.mapletct.ftmes.materialwms.domain.DocumentType;
import com.mapletct.ftmes.materialwms.domain.MaterialWmsBusinessException;
import com.mapletct.ftmes.materialwms.domain.QualityStatus;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Repository
public class MaterialWmsRepository {

    private final JdbcTemplate jdbc;
    private final DataSource dataSource;
    private volatile Boolean postgres;

    public MaterialWmsRepository(JdbcTemplate jdbc, DataSource dataSource) {
        this.jdbc = jdbc;
        this.dataSource = dataSource;
    }

    public Map<String, Object> findDocumentBySource(
            String tenantId, DocumentType type, String sourceDocumentId, String warehouseCode) {
        List<Map<String, Object>> rows = jdbc.queryForList(
            "SELECT * FROM wms_stock_documents "
                + "WHERE tenant_id = ? AND document_type = ? AND source_system = 'WOM' "
                + "AND source_document_id = ? AND warehouse_code = ?",
            tenantId, type.name(), sourceDocumentId, warehouseCode
        );
        return rows.isEmpty() ? null : rows.get(0);
    }

    public void lockDocument(long documentId) {
        jdbc.queryForObject(
            "SELECT id FROM wms_stock_documents WHERE id = ? FOR UPDATE",
            Long.class,
            documentId
        );
    }

    public boolean insertDocumentIfAbsent(
            String tenantId,
            DocumentType type,
            String documentNo,
            StockDocumentRequest request,
            LocalDate storageDate,
            String requestPayload) {
        String baseSql = "INSERT INTO wms_stock_documents ("
            + "tenant_id, document_no, document_type, source_system, source_document_id, "
            + "source_document_no, directive_no, company_code, department_code, staff_code, "
            + "user_name, warehouse_code, storage_date, status, quality_status, memo, request_payload"
            + ") VALUES (?, ?, ?, 'WOM', ?, ?, ?, ?, ?, ?, ?, ?, ?, 'POSTED', 'PENDING', ?, ?)";
        Object[] args = {
            tenantId, documentNo, type.name(), request.getSourceDocumentId(), request.getSrcTableNo(),
            request.getDirectiveNo(), request.getCompanyCode(), request.getDeptCode(), request.getStaffCode(),
            request.getUserName(), request.getWareCode(), Date.valueOf(storageDate), request.getMemo(), requestPayload
        };
        if (isPostgres()) {
            return jdbc.update(baseSql + " ON CONFLICT (tenant_id, document_type, source_system, source_document_id, warehouse_code) DO NOTHING", args) == 1;
        }
        try {
            jdbc.update(baseSql, args);
            return true;
        } catch (DuplicateKeyException ignored) {
            return false;
        }
    }

    public boolean insertLineIfAbsent(
            long documentId,
            String tenantId,
            DocumentType type,
            int lineNo,
            String sourceLineId,
            String warehouseCode,
            StockDocumentLineRequest line,
            QualityStatus qualityStatus,
            BigDecimal goodQuantity,
            BigDecimal badQuantity,
            LocalDate productionDate) {
        String sql = "INSERT INTO wms_stock_document_lines ("
            + "document_id, tenant_id, document_type, line_no, source_line_id, material_code, "
            + "batch_no, production_batch_no, warehouse_code, location_code, quantity, "
            + "reported_quantity, good_quantity, bad_quantity, production_date, quality_status, memo"
            + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        Object[] args = {
            documentId, tenantId, type.name(), lineNo, sourceLineId, line.getGoodCode(),
            normalizeDimension(line.getBatchText()), normalizeDimension(line.getProductionBatchNo()),
            warehouseCode, normalizeDimension(line.getPlaceSetCode()), line.getQuantity(),
            line.getQuantity(), goodQuantity, badQuantity,
            productionDate == null ? null : Date.valueOf(productionDate), qualityStatus.name(), line.getMemo()
        };
        if (isPostgres()) {
            return jdbc.update(sql + " ON CONFLICT (tenant_id, document_type, source_line_id) DO NOTHING", args) == 1;
        }
        try {
            jdbc.update(sql, args);
            return true;
        } catch (DuplicateKeyException ignored) {
            return false;
        }
    }

    public Map<String, Object> findLineBySource(
            String tenantId, DocumentType type, String sourceLineId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
            "SELECT * FROM wms_stock_document_lines "
                + "WHERE tenant_id = ? AND document_type = ? AND source_line_id = ?",
            tenantId, type.name(), sourceLineId
        );
        return rows.isEmpty() ? null : rows.get(0);
    }

    public Map<String, Object> findActiveAllocation(String tenantId, String sourceLineId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
            "SELECT * FROM wms_quality_allocations "
                + "WHERE tenant_id = ? AND source_system = 'WOM' AND source_line_id = ? "
                + "AND status = 'ACTIVE'",
            tenantId, sourceLineId
        );
        return rows.isEmpty() ? null : rows.get(0);
    }

    public Map<String, Object> lockAllocation(String tenantId, String sourceLineId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
            "SELECT * FROM wms_quality_allocations "
                + "WHERE tenant_id = ? AND source_system = 'WOM' AND source_line_id = ? FOR UPDATE",
            tenantId, sourceLineId
        );
        return rows.isEmpty() ? null : rows.get(0);
    }

    public boolean insertAllocationIfAbsent(
            String tenantId,
            String sourceLineId,
            String taskId,
            String qualityReportId,
            BigDecimal totalQuantity,
            BigDecimal goodQuantity,
            BigDecimal badQuantity) {
        String sql = "INSERT INTO wms_quality_allocations ("
            + "tenant_id, source_system, source_line_id, task_id, quality_report_id, "
            + "total_quantity, good_quantity, bad_quantity, status"
            + ") VALUES (?, 'WOM', ?, ?, ?, ?, ?, ?, 'ACTIVE')";
        Object[] args = {
            tenantId, sourceLineId, taskId, qualityReportId,
            totalQuantity, goodQuantity, badQuantity
        };
        if (isPostgres()) {
            return jdbc.update(sql + " ON CONFLICT (tenant_id, source_system, source_line_id) DO NOTHING", args) == 1;
        }
        try {
            jdbc.update(sql, args);
            return true;
        } catch (DuplicateKeyException ignored) {
            return false;
        }
    }

    public long updateAllocation(
            long allocationId,
            long currentVersion,
            String taskId,
            String qualityReportId,
            BigDecimal totalQuantity,
            BigDecimal goodQuantity,
            BigDecimal badQuantity,
            String status) {
        long nextVersion = currentVersion + 1;
        int updated = jdbc.update(
            "UPDATE wms_quality_allocations SET task_id = ?, quality_report_id = ?, "
                + "total_quantity = ?, good_quantity = ?, bad_quantity = ?, status = ?, "
                + "version = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ? AND version = ?",
            taskId, qualityReportId, totalQuantity, goodQuantity, badQuantity, status,
            nextVersion, allocationId, currentVersion
        );
        if (updated != 1) {
            throw new MaterialWmsBusinessException(409, "不良数量分配并发更新冲突，请重试");
        }
        return nextVersion;
    }

    public boolean allocationEventExists(String tenantId, String eventKey) {
        Long count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM wms_quality_allocation_events WHERE tenant_id = ? AND event_key = ?",
            Long.class, tenantId, eventKey
        );
        return count != null && count.longValue() > 0;
    }

    public void insertAllocationEvent(
            String tenantId,
            String eventKey,
            long allocationId,
            String eventType,
            String qualityReportId,
            String payload) {
        String sql = "INSERT INTO wms_quality_allocation_events ("
            + "tenant_id, event_key, allocation_id, event_type, quality_report_id, payload"
            + ") VALUES (?, ?, ?, ?, ?, ?)";
        Object[] args = {tenantId, eventKey, allocationId, eventType, qualityReportId, payload};
        if (isPostgres()) {
            jdbc.update(sql + " ON CONFLICT (tenant_id, event_key) DO NOTHING", args);
            return;
        }
        try {
            jdbc.update(sql, args);
        } catch (DuplicateKeyException ignored) {
            // Event uniqueness keeps retries idempotent.
        }
    }

    public int nextLineNo(long documentId) {
        Integer value = jdbc.queryForObject(
            "SELECT COALESCE(MAX(line_no), 0) + 1 FROM wms_stock_document_lines WHERE document_id = ?",
            Integer.class, documentId
        );
        return value == null ? 1 : value.intValue();
    }

    public void ensureStock(
            String tenantId,
            String warehouseCode,
            String locationCode,
            String materialCode,
            String batchNo,
            String productionBatchNo) {
        String sql = "INSERT INTO wms_batch_stocks ("
            + "tenant_id, warehouse_code, location_code, material_code, batch_no, production_batch_no"
            + ") VALUES (?, ?, ?, ?, ?, ?)";
        Object[] args = {
            tenantId, warehouseCode, normalizeDimension(locationCode), materialCode,
            normalizeDimension(batchNo), normalizeDimension(productionBatchNo)
        };
        if (isPostgres()) {
            jdbc.update(sql + " ON CONFLICT (tenant_id, warehouse_code, location_code, material_code, batch_no, production_batch_no) DO NOTHING", args);
            return;
        }
        try {
            jdbc.update(sql, args);
        } catch (DuplicateKeyException ignored) {
            // H2 test compatibility; PostgreSQL uses ON CONFLICT above.
        }
    }

    public Map<String, Object> adjustStock(
            String tenantId,
            String warehouseCode,
            String locationCode,
            String materialCode,
            String batchNo,
            String productionBatchNo,
            BigDecimal onHandDelta,
            BigDecimal availableDelta,
            BigDecimal holdDelta) {
        String normalizedLocation = normalizeDimension(locationCode);
        String normalizedBatch = normalizeDimension(batchNo);
        String normalizedProductionBatch = normalizeDimension(productionBatchNo);
        int updated = jdbc.update(
            "UPDATE wms_batch_stocks SET "
                + "on_hand_quantity = on_hand_quantity + ?, "
                + "available_quantity = available_quantity + ?, "
                + "hold_quantity = hold_quantity + ?, version = version + 1, updated_at = CURRENT_TIMESTAMP "
                + "WHERE tenant_id = ? AND warehouse_code = ? AND location_code = ? "
                + "AND material_code = ? AND batch_no = ? AND production_batch_no = ? "
                + "AND on_hand_quantity + ? >= 0 AND available_quantity + ? >= 0 AND hold_quantity + ? >= 0",
            onHandDelta, availableDelta, holdDelta,
            tenantId, warehouseCode, normalizedLocation, materialCode, normalizedBatch, normalizedProductionBatch,
            onHandDelta, availableDelta, holdDelta
        );
        if (updated != 1) {
            throw new MaterialWmsBusinessException(409,
                "库存不足或质量状态变更与已用库存冲突: " + materialCode + "/" + normalizedBatch);
        }
        return findStock(tenantId, warehouseCode, normalizedLocation, materialCode, normalizedBatch, normalizedProductionBatch);
    }

    public Map<String, Object> findStock(
            String tenantId,
            String warehouseCode,
            String locationCode,
            String materialCode,
            String batchNo,
            String productionBatchNo) {
        List<Map<String, Object>> rows = jdbc.queryForList(
            "SELECT * FROM wms_batch_stocks WHERE tenant_id = ? AND warehouse_code = ? "
                + "AND location_code = ? AND material_code = ? AND batch_no = ? AND production_batch_no = ?",
            tenantId, warehouseCode, normalizeDimension(locationCode), materialCode,
            normalizeDimension(batchNo), normalizeDimension(productionBatchNo)
        );
        if (rows.isEmpty()) {
            throw new MaterialWmsBusinessException(404, "库存维度不存在: " + materialCode + "/" + batchNo);
        }
        return rows.get(0);
    }

    public void insertTransaction(
            String tenantId,
            String eventKey,
            String transactionType,
            long documentId,
            long lineId,
            String sourceDocumentId,
            String sourceLineId,
            String warehouseCode,
            String locationCode,
            String materialCode,
            String batchNo,
            String productionBatchNo,
            BigDecimal onHandDelta,
            BigDecimal availableDelta,
            BigDecimal holdDelta,
            Map<String, Object> balance) {
        String sql = "INSERT INTO wms_inventory_transactions ("
            + "tenant_id, event_key, transaction_type, document_id, line_id, source_document_id, source_line_id, "
            + "warehouse_code, location_code, material_code, batch_no, production_batch_no, "
            + "on_hand_delta, available_delta, hold_delta, balance_on_hand, balance_available, balance_hold"
            + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        Object[] args = {
            tenantId, eventKey, transactionType, documentId, lineId, sourceDocumentId, sourceLineId,
            warehouseCode, normalizeDimension(locationCode), materialCode, normalizeDimension(batchNo),
            normalizeDimension(productionBatchNo), onHandDelta, availableDelta, holdDelta,
            balance.get("on_hand_quantity"), balance.get("available_quantity"), balance.get("hold_quantity")
        };
        if (isPostgres()) {
            jdbc.update(sql + " ON CONFLICT (tenant_id, event_key) DO NOTHING", args);
            return;
        }
        try {
            jdbc.update(sql, args);
        } catch (DuplicateKeyException ignored) {
            // Event uniqueness makes retry safe.
        }
    }

    public QualityStatus findQualityStatus(String tenantId, String sourceLineId) {
        List<String> values = jdbc.queryForList(
            "SELECT quality_status FROM wms_quality_results "
                + "WHERE tenant_id = ? AND source_system = 'WOM' AND source_line_id = ?",
            String.class, tenantId, sourceLineId
        );
        return values.isEmpty() ? null : QualityStatus.valueOf(values.get(0));
    }

    public void ensureQualityResult(String tenantId, String sourceLineId) {
        String sql = "INSERT INTO wms_quality_results (tenant_id, source_system, source_line_id, quality_status, revision) "
            + "VALUES (?, 'WOM', ?, 'PENDING', 0)";
        if (isPostgres()) {
            jdbc.update(sql + " ON CONFLICT (tenant_id, source_system, source_line_id) DO NOTHING", tenantId, sourceLineId);
            return;
        }
        try {
            jdbc.update(sql, tenantId, sourceLineId);
        } catch (DuplicateKeyException ignored) {
            // H2 test compatibility.
        }
    }

    public Map<String, Object> lockQualityResult(String tenantId, String sourceLineId) {
        return jdbc.queryForMap(
            "SELECT * FROM wms_quality_results WHERE tenant_id = ? AND source_system = 'WOM' "
                + "AND source_line_id = ? FOR UPDATE",
            tenantId, sourceLineId
        );
    }

    public long updateQualityResult(
            String tenantId, String sourceLineId, QualityStatus qualityStatus, long currentRevision) {
        long nextRevision = currentRevision + 1;
        int updated = jdbc.update(
            "UPDATE wms_quality_results SET quality_status = ?, revision = ?, updated_at = CURRENT_TIMESTAMP "
                + "WHERE tenant_id = ? AND source_system = 'WOM' AND source_line_id = ? AND revision = ?",
            qualityStatus.name(), nextRevision, tenantId, sourceLineId, currentRevision
        );
        if (updated != 1) {
            throw new MaterialWmsBusinessException(409, "质检结果并发更新冲突，请重试");
        }
        return nextRevision;
    }

    public List<Map<String, Object>> lockInboundLinesBySource(String tenantId, String sourceLineId) {
        return jdbc.queryForList(
            "SELECT l.*, d.source_document_id FROM wms_stock_document_lines l "
                + "JOIN wms_stock_documents d ON d.id = l.document_id "
                + "WHERE l.tenant_id = ? AND l.document_type = 'COMPLETION_INBOUND' "
                + "AND l.source_line_id = ? FOR UPDATE",
            tenantId, sourceLineId
        );
    }

    public void updateLineQuality(long lineId, QualityStatus qualityStatus) {
        jdbc.update(
            "UPDATE wms_stock_document_lines SET quality_status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
            qualityStatus.name(), lineId
        );
    }

    public void updateLineAllocation(
            long lineId,
            BigDecimal reportedQuantity,
            BigDecimal goodQuantity,
            BigDecimal badQuantity,
            QualityStatus qualityStatus) {
        jdbc.update(
            "UPDATE wms_stock_document_lines SET reported_quantity = ?, good_quantity = ?, "
                + "bad_quantity = ?, quality_status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
            reportedQuantity, goodQuantity, badQuantity, qualityStatus.name(), lineId
        );
    }

    public void refreshDocumentQuality(long documentId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
            "SELECT quality_status, COUNT(*) AS item_count FROM wms_stock_document_lines "
                + "WHERE document_id = ? GROUP BY quality_status",
            documentId
        );
        QualityStatus status = QualityStatus.QUALIFIED;
        for (Map<String, Object> row : rows) {
            QualityStatus lineStatus = QualityStatus.valueOf(String.valueOf(row.get("quality_status")));
            if (lineStatus == QualityStatus.UNQUALIFIED) {
                status = QualityStatus.UNQUALIFIED;
                break;
            }
            if (lineStatus == QualityStatus.PENDING) {
                status = QualityStatus.PENDING;
                continue;
            }
            if (lineStatus == QualityStatus.PARTIAL && status == QualityStatus.QUALIFIED) {
                status = QualityStatus.PARTIAL;
            }
        }
        jdbc.update(
            "UPDATE wms_stock_documents SET quality_status = ?, "
                + "quantity_allocation_state = CASE WHEN EXISTS ("
                + "SELECT 1 FROM wms_stock_document_lines WHERE document_id = ? AND bad_quantity > 0"
                + ") THEN 'ACTIVE' ELSE 'NONE' END, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
            status.name(), documentId, documentId
        );
    }

    public List<Map<String, Object>> listCompletionInbounds(
            String tenantId, String keyword, int page, int size) {
        String pattern = "%" + (keyword == null ? "" : keyword.trim().toLowerCase()) + "%";
        return jdbc.queryForList(
            "SELECT id, document_no, source_document_no, directive_no, company_code, warehouse_code, "
                + "storage_date, quality_status, status, user_name, created_at "
                + "FROM wms_stock_documents WHERE tenant_id = ? AND document_type = 'COMPLETION_INBOUND' "
                + "AND (LOWER(document_no) LIKE ? OR LOWER(COALESCE(source_document_no, '')) LIKE ? "
                + "OR LOWER(COALESCE(directive_no, '')) LIKE ?) "
                + "ORDER BY created_at DESC, id DESC LIMIT ? OFFSET ?",
            tenantId, pattern, pattern, pattern, size, page * size
        );
    }

    public long countCompletionInbounds(String tenantId, String keyword) {
        String pattern = "%" + (keyword == null ? "" : keyword.trim().toLowerCase()) + "%";
        Long count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM wms_stock_documents "
                + "WHERE tenant_id = ? AND document_type = 'COMPLETION_INBOUND' "
                + "AND (LOWER(document_no) LIKE ? OR LOWER(COALESCE(source_document_no, '')) LIKE ? "
                + "OR LOWER(COALESCE(directive_no, '')) LIKE ?)",
            Long.class, tenantId, pattern, pattern, pattern
        );
        return count == null ? 0L : count;
    }

    public Map<String, Object> completionInboundDetail(String tenantId, long documentId) {
        List<Map<String, Object>> documents = jdbc.queryForList(
            "SELECT * FROM wms_stock_documents WHERE tenant_id = ? AND id = ? "
                + "AND document_type = 'COMPLETION_INBOUND'",
            tenantId, documentId
        );
        if (documents.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Object> result = new java.util.LinkedHashMap<String, Object>();
        result.put("document", documents.get(0));
        result.put("lines", jdbc.queryForList(
            "SELECT * FROM wms_stock_document_lines WHERE document_id = ? ORDER BY line_no", documentId));
        result.put("transactions", jdbc.queryForList(
            "SELECT * FROM wms_inventory_transactions WHERE document_id = ? ORDER BY id", documentId));
        return result;
    }

    private boolean isPostgres() {
        Boolean cached = postgres;
        if (cached != null) {
            return cached.booleanValue();
        }
        try (Connection connection = dataSource.getConnection()) {
            boolean detected = connection.getMetaData().getDatabaseProductName().toLowerCase().contains("postgresql");
            postgres = Boolean.valueOf(detected);
            return detected;
        } catch (Exception exception) {
            throw new IllegalStateException("无法识别 material-wms 数据库类型", exception);
        }
    }

    public static String normalizeDimension(String value) {
        return value == null ? "" : value.trim();
    }
}
