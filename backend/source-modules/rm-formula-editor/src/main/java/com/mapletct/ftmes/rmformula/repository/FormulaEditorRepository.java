package com.mapletct.ftmes.rmformula.repository;

import com.mapletct.ftmes.rmformula.api.FormulaSaveRequest;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class FormulaEditorRepository {
    private final JdbcTemplate jdbc;

    public FormulaEditorRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void lock(String key) {
        jdbc.query("SELECT pg_advisory_xact_lock(hashtextextended(?, 0))", resultSet -> { }, key);
    }

    public List<Map<String, Object>> formulas(String query, int limit) {
        String pattern = "%" + query.toLowerCase() + "%";
        return jdbc.queryForList(
                "SELECT f.id, COALESCE(f.version, 0) AS version, f.formual_code AS \"formulaCode\", "
                        + "f.formula_name AS \"formulaName\", f.formula_edtion AS \"formulaEdition\", "
                        + "f.product_id AS \"productId\", m.code AS \"productCode\", m.name AS \"productName\", "
                        + "f.batch_formulaid AS \"batchFormulaId\", f.batch_formula_code AS \"batchFormulaCode\", "
                        + "f.batch_formula_edition AS \"batchFormulaEdition\", f.batch_status AS \"batchStatus\", "
                        + "f.modify_time AS \"modifiedAt\" "
                        + "FROM rm_formulas f LEFT JOIN baseset_materials m ON m.id = f.product_id "
                        + "WHERE COALESCE(f.valid, TRUE) IS TRUE "
                        + "AND (? = '%%' OR LOWER(COALESCE(f.formual_code, '') || ' ' || COALESCE(f.formula_name, '') "
                        + "|| ' ' || COALESCE(f.batch_formula_code, '')) LIKE ?) "
                        + "ORDER BY COALESCE(f.modify_time, f.create_time) DESC NULLS LAST, f.id DESC LIMIT ?",
                pattern,
                pattern,
                limit);
    }

    public Map<String, Object> formula(long formulaId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT f.id, COALESCE(f.version, 0) AS version, f.formual_code AS \"formulaCode\", "
                        + "f.formula_name AS \"formulaName\", f.formula_edtion AS \"formulaEdition\", "
                        + "f.product_id AS \"productId\", m.code AS \"productCode\", m.name AS \"productName\", "
                        + "f.batch_formulaid AS \"batchFormulaId\", f.batch_formula_code AS \"batchFormulaCode\", "
                        + "f.batch_formula_edition AS \"batchFormulaEdition\", f.batch_server_id AS \"batchServerId\", "
                        + "bs.code AS \"batchServerCode\", bs.system_name AS \"batchServerName\", "
                        + "f.batch_status AS \"batchStatus\", f.nor_size AS \"normalSize\", "
                        + "f.description, f.report_type AS \"reportType\", f.set_process AS \"setProcess\", "
                        + "f.status, f.create_time AS \"createdAt\", f.modify_time AS \"modifiedAt\" "
                        + "FROM rm_formulas f LEFT JOIN baseset_materials m ON m.id = f.product_id "
                        + "LEFT JOIN baseset_other_systems bs ON bs.id = f.batch_server_id "
                        + "WHERE f.id = ? AND COALESCE(f.valid, TRUE) IS TRUE",
                formulaId);
        return first(rows);
    }

    public List<Map<String, Object>> materials(String query, int limit) {
        String pattern = "%" + query.toLowerCase() + "%";
        return jdbc.queryForList(
                "SELECT id, code, name FROM baseset_materials "
                        + "WHERE COALESCE(valid, TRUE) IS TRUE "
                        + "AND (? = '%%' OR LOWER(COALESCE(code, '') || ' ' || COALESCE(name, '')) LIKE ?) "
                        + "ORDER BY code NULLS LAST, name NULLS LAST, id LIMIT ?",
                pattern, pattern, limit);
    }

    public List<Map<String, Object>> batchServers(String query, int limit) {
        String pattern = "%" + query.toLowerCase() + "%";
        return jdbc.queryForList(
                "SELECT id, code, system_name AS name FROM baseset_other_systems "
                        + "WHERE COALESCE(valid, TRUE) IS TRUE "
                        + "AND (? = '%%' OR LOWER(COALESCE(code, '') || ' ' || COALESCE(system_name, '')) LIKE ?) "
                        + "ORDER BY code NULLS LAST, system_name NULLS LAST, id LIMIT ?",
                pattern, pattern, limit);
    }

    public boolean materialExists(long materialId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM baseset_materials WHERE id = ? AND COALESCE(valid, TRUE) IS TRUE",
                new Object[] { materialId }, Integer.class);
        return count != null && count > 0;
    }

    public boolean batchServerExists(long batchServerId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM baseset_other_systems WHERE id = ? AND COALESCE(valid, TRUE) IS TRUE",
                new Object[] { batchServerId }, Integer.class);
        return count != null && count > 0;
    }

    public List<Map<String, Object>> processes(long formulaId) {
        return jdbc.queryForList(
                "SELECT p.id, 'p-' || p.id AS \"clientKey\", p.name, p.proc_sort AS \"processSort\", "
                        + "p.batch_unit_id AS \"batchUnitId\", COALESCE(p.auto_start, FALSE) AS \"autoStart\", "
                        + "p.exe_order AS \"executionOrder\", p.long_time AS duration, "
                        + "COALESCE(p.is_first_process, FALSE) AS \"firstProcess\", "
                        + "COALESCE(p.is_last_process, FALSE) AS \"lastProcess\", p.remark "
                        + "FROM rm_formula_processes p "
                        + "WHERE COALESCE(p.formula_id, p.formula) = ? AND COALESCE(p.valid, TRUE) IS TRUE "
                        + "ORDER BY COALESCE(p.exe_order, 2147483647), p.proc_sort NULLS LAST, p.id",
                formulaId);
    }

    public List<Map<String, Object>> activities(long formulaId) {
        return jdbc.queryForList(
                "SELECT a.id, 'a-' || a.id AS \"clientKey\", "
                        + "CASE WHEN a.process_id IS NULL THEN NULL ELSE 'p-' || a.process_id END AS \"processKey\", "
                        + "a.process_id AS \"processId\", a.name, a.active_type AS \"activeType\", "
                        + "a.batch_phase_id AS \"batchPhaseId\", a.batch_phase_name AS \"batchPhaseName\", "
                        + "a.batch_site AS \"batchSite\", a.dispatch_system AS \"dispatchSystem\", "
                        + "a.exe_system AS \"executionSystem\", COALESCE(a.is_auto, FALSE) AS automatic, "
                        + "COALESCE(a.is_fixed_quantity, FALSE) AS \"fixedQuantity\", a.quantity, "
                        + "a.min_quantity AS \"minimumQuantity\", a.max_quantity AS \"maximumQuantity\", "
                        + "a.release_conditions AS \"releaseConditions\", "
                        + "a.switch_response_item AS \"responseItem\", a.switch_set_item AS \"setItem\", "
                        + "a.use_item AS \"useItem\", a.remark "
                        + "FROM rm_process_actives a "
                        + "WHERE a.formula_id = ? AND COALESCE(a.valid, TRUE) IS TRUE "
                        + "ORDER BY a.process_id NULLS LAST, COALESCE(a.ingredients_order, 2147483647), a.id",
                formulaId);
    }

    public long nextFormulaId() {
        return jdbc.queryForObject("SELECT nextval('rm_web_formula_id_seq')", Long.class);
    }

    public long nextProcessId() {
        return jdbc.queryForObject("SELECT nextval('rm_web_formula_process_id_seq')", Long.class);
    }

    public long nextActivityId() {
        return jdbc.queryForObject("SELECT nextval('rm_web_process_active_id_seq')", Long.class);
    }

    public boolean formulaCodeUsed(String formulaCode, Long excludedId) {
        Integer count;
        if (excludedId == null) {
            count = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM rm_formulas WHERE LOWER(TRIM(formual_code)) = LOWER(TRIM(?)) "
                            + "AND COALESCE(valid, TRUE) IS TRUE",
                    new Object[] { formulaCode }, Integer.class);
        } else {
            count = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM rm_formulas WHERE LOWER(TRIM(formual_code)) = LOWER(TRIM(?)) "
                            + "AND COALESCE(valid, TRUE) IS TRUE AND id <> ?",
                    new Object[] { formulaCode, excludedId }, Integer.class);
        }
        return count != null && count > 0;
    }

    public boolean processBelongs(long processId, long formulaId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM rm_formula_processes WHERE id = ? AND COALESCE(formula_id, formula) = ?",
                new Object[] { processId, formulaId }, Integer.class);
        return count != null && count > 0;
    }

    public boolean activityBelongs(long activityId, long formulaId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM rm_process_actives WHERE id = ? AND formula_id = ?",
                new Object[] { activityId, formulaId }, Integer.class);
        return count != null && count > 0;
    }

    public int insertFormula(long formulaId, FormulaSaveRequest request) {
        return jdbc.update(
                "INSERT INTO rm_formulas "
                        + "(id, version, create_time, modify_time, valid, status, table_no, table_info_id, "
                        + "formual_code, formula_name, formula_edtion, product_id, batch_formulaid, "
                        + "batch_formula_code, batch_formula_edition, batch_server_id, batch_status, nor_size, "
                        + "description, report_type, set_process) "
                        + "VALUES (?, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, TRUE, 0, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, "
                        + "'WEB_DRAFT', ?, ?, ?, ?)",
                formulaId,
                request.getFormulaCode(),
                formulaId,
                request.getFormulaCode(),
                request.getFormulaName(),
                request.getFormulaEdition(),
                request.getProductId(),
                request.getBatchFormulaId(),
                request.getBatchFormulaCode(),
                request.getBatchFormulaEdition(),
                request.getBatchServerId(),
                request.getNormalSize(),
                request.getDescription(),
                request.getReportType(),
                request.getSetProcess());
    }

    public int updateFormula(long formulaId, int expectedVersion, FormulaSaveRequest request) {
        return jdbc.update(
                "UPDATE rm_formulas SET version = COALESCE(version, 0) + 1, modify_time = CURRENT_TIMESTAMP, "
                        + "formual_code = ?, formula_name = ?, formula_edtion = ?, product_id = ?, "
                        + "batch_formulaid = ?, batch_formula_code = ?, batch_formula_edition = ?, batch_server_id = ?, "
                        + "batch_status = 'WEB_DRAFT', nor_size = ?, description = ?, report_type = ?, set_process = ? "
                        + "WHERE id = ? AND COALESCE(valid, TRUE) IS TRUE AND COALESCE(version, 0) = ?",
                request.getFormulaCode(),
                request.getFormulaName(),
                request.getFormulaEdition(),
                request.getProductId(),
                request.getBatchFormulaId(),
                request.getBatchFormulaCode(),
                request.getBatchFormulaEdition(),
                request.getBatchServerId(),
                request.getNormalSize(),
                request.getDescription(),
                request.getReportType(),
                request.getSetProcess(),
                formulaId,
                expectedVersion);
    }

    public void retireProcesses(long formulaId) {
        jdbc.update("UPDATE rm_formula_processes SET valid = FALSE, modify_time = CURRENT_TIMESTAMP "
                + "WHERE COALESCE(formula_id, formula) = ? AND COALESCE(valid, TRUE) IS TRUE", formulaId);
    }

    public void saveProcess(long formulaId, FormulaSaveRequest.ProcessInput process) {
        int changed = jdbc.update(
                "UPDATE rm_formula_processes SET valid = TRUE, modify_time = CURRENT_TIMESTAMP, formula = ?, "
                        + "formula_id = ?, name = ?, proc_sort = ?, batch_unit_id = ?, auto_start = ?, exe_order = ?, "
                        + "long_time = ?, is_first_process = ?, is_last_process = ?, remark = ? "
                        + "WHERE id = ? AND COALESCE(formula_id, formula) = ?",
                formulaId, formulaId, process.getName(), process.getProcessSort(), process.getBatchUnitId(),
                process.getAutoStart(), process.getExecutionOrder(), process.getDuration(), process.getFirstProcess(),
                process.getLastProcess(), process.getRemark(), process.getId(), formulaId);
        if (changed == 0) {
            jdbc.update(
                    "INSERT INTO rm_formula_processes "
                            + "(id, version, create_time, modify_time, valid, status, table_no, table_info_id, formula, "
                            + "formula_id, name, proc_sort, batch_unit_id, auto_start, exe_order, long_time, "
                            + "is_first_process, is_last_process, remark) "
                            + "VALUES (?, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, TRUE, 0, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    process.getId(), "WEB-P-" + process.getId(), process.getId(), formulaId, formulaId,
                    process.getName(), process.getProcessSort(), process.getBatchUnitId(), process.getAutoStart(),
                    process.getExecutionOrder(), process.getDuration(), process.getFirstProcess(),
                    process.getLastProcess(), process.getRemark());
        }
    }

    public void retireActivities(long formulaId) {
        jdbc.update("UPDATE rm_process_actives SET valid = FALSE, modify_time = CURRENT_TIMESTAMP "
                + "WHERE formula_id = ? AND COALESCE(valid, TRUE) IS TRUE", formulaId);
    }

    public void saveActivity(long formulaId, long processId, FormulaSaveRequest.ActivityInput activity) {
        int changed = jdbc.update(
                "UPDATE rm_process_actives SET valid = TRUE, modify_time = CURRENT_TIMESTAMP, formula_id = ?, "
                        + "process_id = ?, name = ?, active_type = ?, batch_phase_id = ?, batch_phase_name = ?, "
                        + "batch_site = ?, dispatch_system = ?, exe_system = ?, is_auto = ?, is_fixed_quantity = ?, "
                        + "quantity = ?, min_quantity = ?, max_quantity = ?, release_conditions = ?, "
                        + "switch_response_item = ?, switch_set_item = ?, use_item = ?, remark = ? "
                        + "WHERE id = ? AND formula_id = ?",
                formulaId, processId, activity.getName(), activity.getActiveType(), activity.getBatchPhaseId(),
                activity.getBatchPhaseName(), activity.getBatchSite(), activity.getDispatchSystem(),
                activity.getExecutionSystem(), activity.getAutomatic(), activity.getFixedQuantity(),
                activity.getQuantity(), activity.getMinimumQuantity(), activity.getMaximumQuantity(),
                activity.getReleaseConditions(), activity.getResponseItem(), activity.getSetItem(),
                activity.getUseItem(), activity.getRemark(), activity.getId(), formulaId);
        if (changed == 0) {
            jdbc.update(
                    "INSERT INTO rm_process_actives "
                            + "(id, version, create_time, modify_time, valid, status, table_no, table_info_id, formula_id, "
                            + "process_id, name, active_type, batch_phase_id, batch_phase_name, batch_site, "
                            + "dispatch_system, exe_system, is_auto, is_fixed_quantity, quantity, min_quantity, "
                            + "max_quantity, release_conditions, switch_response_item, switch_set_item, use_item, remark) "
                            + "VALUES (?, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, TRUE, 0, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, "
                            + "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    activity.getId(), "WEB-A-" + activity.getId(), activity.getId(), formulaId, processId,
                    activity.getName(), activity.getActiveType(), activity.getBatchPhaseId(), activity.getBatchPhaseName(),
                    activity.getBatchSite(), activity.getDispatchSystem(), activity.getExecutionSystem(),
                    activity.getAutomatic(), activity.getFixedQuantity(), activity.getQuantity(),
                    activity.getMinimumQuantity(), activity.getMaximumQuantity(), activity.getReleaseConditions(),
                    activity.getResponseItem(), activity.getSetItem(), activity.getUseItem(), activity.getRemark());
        }
    }

    public Map<String, Object> revisionByRequest(String tenant, String requestId) {
        return first(jdbc.queryForList(
                "SELECT id, formula_id AS \"formulaId\", request_hash AS \"requestHash\", "
                        + "revision_no AS \"revisionNo\" FROM rm_formula_editor_revisions "
                        + "WHERE tenant_id = ? AND request_id = ?",
                tenant, requestId));
    }

    public long insertRevision(String tenant, long formulaId, String requestId, String requestHash,
                               int version, String payloadJson) {
        return jdbc.queryForObject(
                "INSERT INTO rm_formula_editor_revisions "
                        + "(tenant_id, formula_id, request_id, request_hash, revision_no, formula_version, payload) "
                        + "VALUES (?, ?, ?, ?, "
                        + "COALESCE((SELECT MAX(revision_no) + 1 FROM rm_formula_editor_revisions WHERE formula_id = ?), 1), "
                        + "?, CAST(? AS jsonb)) RETURNING id",
                new Object[] { tenant, formulaId, requestId, requestHash, formulaId, version, payloadJson },
                Long.class);
    }

    public Map<String, Object> latestRevision(long formulaId) {
        return first(jdbc.queryForList(
                "SELECT id, formula_id AS \"formulaId\", revision_no AS \"revisionNo\", "
                        + "formula_version AS \"formulaVersion\", created_at AS \"createdAt\" "
                        + "FROM rm_formula_editor_revisions WHERE formula_id = ? ORDER BY revision_no DESC LIMIT 1",
                formulaId));
    }

    public Map<String, Object> revision(long formulaId, Long revisionId) {
        if (revisionId == null) {
            return latestRevision(formulaId);
        }
        return first(jdbc.queryForList(
                "SELECT id, formula_id AS \"formulaId\", revision_no AS \"revisionNo\", "
                        + "formula_version AS \"formulaVersion\", created_at AS \"createdAt\" "
                        + "FROM rm_formula_editor_revisions WHERE formula_id = ? AND id = ?",
                formulaId, revisionId));
    }

    public Map<String, Object> deliveryByRequest(String tenant, String requestId) {
        return first(jdbc.queryForList(deliveryColumns()
                + " FROM rm_formula_deliveries WHERE tenant_id = ? AND request_id = ?", tenant, requestId));
    }

    public Map<String, Object> delivery(long deliveryId) {
        return first(jdbc.queryForList(deliveryColumns() + " FROM rm_formula_deliveries WHERE id = ?", deliveryId));
    }

    public String deliveryPayload(long deliveryId) {
        List<String> rows = jdbc.query(
                "SELECT payload::text FROM rm_formula_deliveries WHERE id = ?",
                (resultSet, rowNum) -> resultSet.getString(1),
                deliveryId);
        return rows.isEmpty() ? "" : rows.get(0);
    }

    public Map<String, Object> latestDelivery(long formulaId) {
        return first(jdbc.queryForList(deliveryColumns()
                + " FROM rm_formula_deliveries WHERE formula_id = ? ORDER BY id DESC LIMIT 1", formulaId));
    }

    public long createDelivery(String tenant, String requestId, long formulaId, long revisionId,
                               String endpoint, String payloadJson) {
        return jdbc.queryForObject(
                "INSERT INTO rm_formula_deliveries "
                        + "(tenant_id, request_id, formula_id, revision_id, endpoint, payload, state) "
                        + "VALUES (?, ?, ?, ?, ?, CAST(? AS jsonb), 'PENDING') RETURNING id",
                new Object[] { tenant, requestId, formulaId, revisionId, endpoint, payloadJson }, Long.class);
    }

    public void recordDeliveryAttempt(long deliveryId, String state, Integer httpStatus,
                                      String responseBody, String errorMessage) {
        long attemptNo = jdbc.queryForObject(
                "UPDATE rm_formula_deliveries SET attempts = attempts + 1, state = ?, http_status = ?, "
                        + "response_body = ?, error_message = ?, updated_at = CURRENT_TIMESTAMP, "
                        + "acknowledged_at = CASE WHEN ? = 'ACKNOWLEDGED' THEN CURRENT_TIMESTAMP ELSE acknowledged_at END "
                        + "WHERE id = ? RETURNING attempts",
                new Object[] { state, httpStatus, truncate(responseBody, 8000), truncate(errorMessage, 2000), state, deliveryId },
                Long.class);
        jdbc.update(
                "INSERT INTO rm_formula_delivery_attempts "
                        + "(delivery_id, attempt_no, state, http_status, response_body, error_message) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                deliveryId, attemptNo, state, httpStatus, truncate(responseBody, 8000), truncate(errorMessage, 2000));
    }

    public List<Map<String, Object>> deliveryAttempts(long deliveryId) {
        return jdbc.queryForList(
                "SELECT attempt_no AS \"attemptNo\", state, http_status AS \"httpStatus\", "
                        + "response_body AS \"responseBody\", error_message AS \"errorMessage\", "
                        + "created_at AS \"createdAt\" FROM rm_formula_delivery_attempts "
                        + "WHERE delivery_id = ? ORDER BY attempt_no", deliveryId);
    }

    private static String deliveryColumns() {
        return "SELECT id, tenant_id AS \"tenantId\", request_id AS \"requestId\", formula_id AS \"formulaId\", "
                + "revision_id AS \"revisionId\", endpoint, state, attempts, http_status AS \"httpStatus\", "
                + "response_body AS \"responseBody\", error_message AS \"errorMessage\", "
                + "created_at AS \"createdAt\", updated_at AS \"updatedAt\", acknowledged_at AS \"acknowledgedAt\"";
    }

    private static Map<String, Object> first(List<Map<String, Object>> rows) {
        return rows.isEmpty() ? Collections.<String, Object>emptyMap() : rows.get(0);
    }

    private static String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
