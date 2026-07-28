package com.mapletct.ftmes.processanalysis.repository;

import com.mapletct.ftmes.processanalysis.domain.SnapshotType;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class ProcessAnalysisRepository {

    private final JdbcTemplate jdbc;
    private final DataSource dataSource;
    private volatile Boolean postgres;

    public ProcessAnalysisRepository(JdbcTemplate jdbc, DataSource dataSource) {
        this.jdbc = jdbc;
        this.dataSource = dataSource;
    }

    public Map<String, Object> findTaskByBatch(String batchNo) {
        return findTaskByBatchAndProduct(batchNo, "");
    }

    public Map<String, Object> findTaskByBatchAndProduct(String batchNo, String productNo) {
        return first(jdbc.queryForList(
            "SELECT t.id, t.table_no, t.produce_batch_num, t.product_id, m.code AS product_code, "
                + "m.name AS product_name, t.task_run_state, t.check_state, t.check_result, t.plan_num, "
                + "t.finish_num, t.act_start_time, t.act_end_time, t.create_time, t.modify_time "
                + "FROM wom_produce_tasks t LEFT JOIN baseset_materials m ON m.id = t.product_id "
                + "WHERE t.valid IS DISTINCT FROM FALSE AND t.produce_batch_num = ? "
                + "AND (? = '' OR m.code = ?) ORDER BY t.id DESC LIMIT 1",
            batchNo, normalize(productNo), normalize(productNo)
        ));
    }

    public boolean hasTaskExecution(long taskId) {
        Long count = jdbc.queryForObject(
            "SELECT count(*) FROM wom_produce_task_exelog WHERE task_id = ?",
            Long.class, taskId);
        return count != null && count.longValue() > 0;
    }

    public Map<String, Object> findTaskExecution(long sourceId) {
        return first(jdbc.queryForList(
            "SELECT id, task_id, table_no, produce_batch_num, product_id, task_run_state, "
                + "check_state, check_result, finish_num, analysis_flag, act_start_time, act_end_time, "
                + "create_time, modify_time FROM wom_produce_task_exelog WHERE id = ?",
            sourceId
        ));
    }

    public Map<String, Object> findProcessExecution(long sourceId) {
        return first(jdbc.queryForList(
            "SELECT id, task_id, task_process_id, table_no, name, produce_batch_num, process_run_state, "
                + "analysis_flag, act_start_time, act_end_time, long_time, create_time, modify_time "
                + "FROM wom_process_exelogs WHERE id = ?",
            sourceId
        ));
    }

    public Map<String, Object> findProcessExecutionDetail(long sourceId) {
        return first(jdbc.queryForList(
            "SELECT e.id, e.task_id, e.task_process_id, e.table_no, e.name, e.produce_batch_num, "
                + "e.process_run_state, e.analysis_flag, e.act_start_time, e.act_end_time, e.long_time, "
                + "e.create_time, e.modify_time, p.table_no AS planned_process_no, "
                + "COALESCE(p.name, e.name) AS planned_process_name, "
                + "COALESCE(p.exe_order, e.exe_order) AS process_order, "
                + "t.table_no AS task_no, t.product_id, t.line_id AS wom_line_id, t.task_run_state, "
                + "m.code AS product_code, m.name AS product_name, "
                + "line.code AS wom_line_code, line.name AS wom_line_name, "
                + "binding.tenant_id AS bpi_tenant_id, binding.plant_id AS bpi_plant_id, "
                + "binding.line_id AS bpi_line_id "
                + "FROM wom_process_exelogs e "
                + "LEFT JOIN wom_task_processes p ON p.id = e.task_process_id "
                + "LEFT JOIN wom_produce_tasks t ON t.id = e.task_id "
                + "LEFT JOIN baseset_materials m ON m.id = t.product_id "
                + "LEFT JOIN hm_factory_models line ON line.id = t.line_id "
                + "LEFT JOIN wom_bpi_production_context_bindings binding "
                + "ON binding.wom_cid = t.cid AND binding.wom_line_id = t.line_id "
                + "AND binding.enabled IS DISTINCT FROM FALSE "
                + "WHERE e.id = ? AND e.valid IS DISTINCT FROM FALSE",
            sourceId
        ));
    }

    public List<Map<String, Object>> findTaskProcessExecutions(long taskId) {
        return jdbc.queryForList(
            "SELECT e.id, e.task_id, e.task_process_id, e.table_no, e.name, e.produce_batch_num, "
                + "e.process_run_state, e.act_start_time, e.act_end_time, e.long_time, "
                + "COALESCE(p.name, e.name) AS planned_process_name, "
                + "COALESCE(p.exe_order, e.exe_order) AS process_order "
                + "FROM wom_process_exelogs e "
                + "LEFT JOIN wom_task_processes p ON p.id = e.task_process_id "
                + "WHERE e.task_id = ? AND e.valid IS DISTINCT FROM FALSE "
                + "ORDER BY COALESCE(p.exe_order, e.exe_order), e.act_start_time, e.id",
            taskId
        );
    }

    public Map<String, Object> findActivityExecution(long sourceId) {
        return first(jdbc.queryForList(
            "SELECT id, task_id, task_process_id, task_active_id, table_no, name, produce_batch_num, "
                + "material_batch_num, material_id, run_state, analysis_flag, actual_num, use_num, "
                + "act_start_time, act_end_time, create_time, modify_time FROM wom_acti_exelogs WHERE id = ?",
            sourceId
        ));
    }

    public Map<String, Object> loadTraceFacts(String tenantId, String batchNo, String productNo) {
        Map<String, Object> task = findTaskByBatchAndProduct(batchNo, productNo);
        if (task == null) {
            return Collections.emptyMap();
        }
        long taskId = number(task.get("id")).longValue();
        long productId = number(task.get("product_id")).longValue();
        Map<String, Object> facts = new LinkedHashMap<String, Object>();
        facts.put("task", task);
        facts.put("taskExecutions", jdbc.queryForList(
            "SELECT id, task_id, table_no, produce_batch_num, product_id, task_run_state, check_state, "
                + "check_result, finish_num, analysis_flag, act_start_time, act_end_time, create_time, modify_time "
                + "FROM wom_produce_task_exelog WHERE task_id = ? ORDER BY create_time, id",
            taskId));
        facts.put("processes", jdbc.queryForList(
            "SELECT id, task_id, table_no, name, exe_order, process_run_state, act_start_time, act_end_time, "
                + "plan_end_time, create_time, modify_time FROM wom_task_processes WHERE task_id = ? "
                + "AND valid IS DISTINCT FROM FALSE ORDER BY exe_order, id",
            taskId));
        facts.put("processExecutions", jdbc.queryForList(
            "SELECT id, task_id, task_process_id, table_no, name, produce_batch_num, process_run_state, "
                + "analysis_flag, act_start_time, act_end_time, long_time, create_time, modify_time "
                + "FROM wom_process_exelogs WHERE task_id = ? AND valid IS DISTINCT FROM FALSE "
                + "ORDER BY act_start_time NULLS LAST, id",
            taskId));
        facts.put("activities", jdbc.queryForList(
            "SELECT id, task_id, task_process_id, table_no, name, material_batch_num, material_id, run_state, "
                + "check_state, check_result, plan_quantity, sum_num, act_start_time, act_end_time, create_time, modify_time "
                + "FROM wom_task_actives WHERE task_id = ? AND valid IS DISTINCT FROM FALSE "
                + "ORDER BY hidden_sort NULLS LAST, id",
            taskId));
        facts.put("activityExecutions", jdbc.queryForList(
            "SELECT id, task_id, task_process_id, task_active_id, table_no, name, produce_batch_num, "
                + "material_batch_num, material_id, run_state, analysis_flag, actual_num, use_num, "
                + "act_start_time, act_end_time, create_time, modify_time FROM wom_acti_exelogs "
                + "WHERE task_id = ? AND valid IS DISTINCT FROM FALSE ORDER BY act_start_time NULLS LAST, id",
            taskId));
        facts.put("materialInputs", jdbc.queryForList(
            "SELECT DISTINCT d.id, a.task_process_id, p.name AS process_name, d.table_no, d.material_id, "
                + "d.material_batch_num, d.putin_num, d.use_num, d.putin_time, d.putin_end_time, "
                + "d.ware_id, d.store_id, d.is_finish, d.create_time "
                + "FROM wom_acti_exelogs a JOIN wom_putin_details d ON d.id = a.putin_detail_id "
                + "LEFT JOIN wom_task_processes p ON p.id = a.task_process_id "
                + "WHERE a.task_id = ? ORDER BY d.putin_time NULLS LAST, d.id",
            taskId));
        facts.put("materialOutputs", jdbc.queryForList(
            "SELECT DISTINCT d.id, a.task_process_id, p.name AS process_name, d.table_no, "
                + "d.material_batch_num, d.product, d.output_num, d.report_num, d.putin_time, "
                + "d.putin_end_time, d.ware_id, d.store_id, d.create_time "
                + "FROM wom_acti_exelogs a JOIN wom_output_details d ON d.id = a.output_detail_id "
                + "LEFT JOIN wom_task_processes p ON p.id = a.task_process_id "
                + "WHERE a.task_id = ? ORDER BY d.putin_time NULLS LAST, d.id",
            taskId));
        facts.put("materialOutputRecords", jdbc.queryForList(
            "SELECT id, table_no, task_exelog_id, proc_exelog_id, act_exelog_id, material_id, mat_batch_num, "
                + "produce_batch_num, output_num, output_time, output_end_time, ware_id, store_id, create_time "
                + "FROM wom_mat_outpt_records WHERE produce_batch_num = ? AND valid IS DISTINCT FROM FALSE "
                + "ORDER BY output_time NULLS LAST, id",
            batchNo));
        facts.put("batchInfo", jdbc.queryForList(
            "SELECT id, table_no, batch_num, material_id, source_type, production_date, in_store_date, "
                + "check_state, check_result, pass_state, is_available, create_time, modify_time "
                + "FROM baseset_batch_infos WHERE batch_num = ? AND valid IS DISTINCT FROM FALSE ORDER BY id",
            batchNo));
        facts.put("inspections", jdbc.queryForList(
            "SELECT id, table_no, source_id, source_type, sourc_table_no, batch_code, prod_id, quantity, "
                + "check_state, closed, apply_time, create_time, modify_time FROM qcs_inspects "
                + "WHERE batch_code = ? AND prod_id = ? AND valid IS DISTINCT FROM FALSE ORDER BY create_time, id",
            batchNo, productId));
        facts.put("inspectionReports", jdbc.queryForList(
            "SELECT r.id, r.table_no, r.inspect_id, r.batch_code, r.prod_id, r.check_result, r.check_res_code, "
                + "r.un_qlf_deal_flag, r.check_time, r.create_time, r.modify_time FROM qcs_inspect_reports r "
                + "WHERE r.batch_code = ? AND r.prod_id = ? AND r.valid IS DISTINCT FROM FALSE ORDER BY r.create_time, r.id",
            batchNo, productId));
        facts.put("inspectionReportItems", jdbc.queryForList(
            "SELECT c.id, c.report_id, c.report_name, c.disp_value, c.check_result, c.unit_name, "
                + "c.min_value, c.max_value, c.index_range, c.create_time, c.modify_time "
                + "FROM qcs_report_coms c JOIN qcs_inspect_reports r ON r.id = c.report_id "
                + "WHERE r.batch_code = ? AND r.prod_id = ? AND c.valid IS DISTINCT FROM FALSE "
                + "ORDER BY c.sort NULLS LAST, c.id",
            batchNo, productId));
        facts.put("unqualifiedDispositions", jdbc.queryForList(
            "SELECT d.id, d.table_no, d.report_id, d.batch_code, d.prod_id, d.un_qlf_reason, d.deal_time, "
                + "d.status, d.effect_time, d.create_time, d.modify_time FROM qcs_un_qlf_deals d "
                + "WHERE d.batch_code = ? AND d.prod_id = ? AND d.valid IS DISTINCT FROM FALSE ORDER BY d.create_time, d.id",
            batchNo, productId));
        facts.put("wmsDocuments", jdbc.queryForList(
            "SELECT DISTINCT d.id, d.document_no, d.document_type, d.source_document_id, d.source_document_no, "
                + "d.directive_no, d.warehouse_code, d.storage_date, d.status, d.quality_status, d.created_at, d.updated_at "
                + "FROM wms_stock_documents d JOIN wms_stock_document_lines l ON l.document_id = d.id "
                + "WHERE d.tenant_id = ? AND (l.production_batch_no = ? OR l.batch_no = ?) "
                + "ORDER BY d.created_at, d.id",
            normalizeTenant(tenantId), batchNo, batchNo));
        facts.put("wmsLines", jdbc.queryForList(
            "SELECT l.id, l.document_id, l.source_line_id, l.material_code, l.batch_no, l.production_batch_no, "
                + "l.warehouse_code, l.location_code, l.quantity, l.quality_status, l.created_at, l.updated_at "
                + "FROM wms_stock_document_lines l WHERE l.tenant_id = ? "
                + "AND (l.production_batch_no = ? OR l.batch_no = ?) "
                + "ORDER BY l.created_at, l.id",
            normalizeTenant(tenantId), batchNo, batchNo));
        facts.put("wmsTransactions", jdbc.queryForList(
            "SELECT id, event_key, transaction_type, source_document_id, source_line_id, material_code, "
                + "batch_no, production_batch_no, on_hand_delta, available_delta, hold_delta, balance_on_hand, "
                + "balance_available, balance_hold, created_at FROM wms_inventory_transactions "
                + "WHERE tenant_id = ? AND (production_batch_no = ? OR batch_no = ?) ORDER BY created_at, id",
            normalizeTenant(tenantId), batchNo, batchNo));
        return facts;
    }

    public Map<String, Object> upsertSnapshot(
            String tenantId,
            SnapshotType type,
            long sourceId,
            long taskId,
            String batchNo,
            String sourceState,
            String metricsJson,
            Timestamp sourceUpdatedAt) {
        String insert = "INSERT INTO pa_trace_snapshots (tenant_id, source_type, source_id, task_id, batch_no, "
            + "source_state, metrics_json, source_updated_at, revision) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 1)";
        Object[] args = {tenantId, type.name(), sourceId, taskId, batchNo, sourceState, metricsJson, sourceUpdatedAt};
        if (isPostgres()) {
            jdbc.update(insert + " ON CONFLICT (tenant_id, source_type, source_id) DO UPDATE SET "
                + "task_id = EXCLUDED.task_id, batch_no = EXCLUDED.batch_no, source_state = EXCLUDED.source_state, "
                + "metrics_json = EXCLUDED.metrics_json, source_updated_at = EXCLUDED.source_updated_at, "
                + "revision = pa_trace_snapshots.revision + 1, updated_at = CURRENT_TIMESTAMP", args);
        } else {
            try {
                jdbc.update(insert, args);
            } catch (DuplicateKeyException ignored) {
                jdbc.update("UPDATE pa_trace_snapshots SET task_id = ?, batch_no = ?, source_state = ?, "
                        + "metrics_json = ?, source_updated_at = ?, revision = revision + 1, updated_at = CURRENT_TIMESTAMP "
                        + "WHERE tenant_id = ? AND source_type = ? AND source_id = ?",
                    taskId, batchNo, sourceState, metricsJson, sourceUpdatedAt, tenantId, type.name(), sourceId);
            }
        }
        return first(jdbc.queryForList(
            "SELECT id, tenant_id, source_type, source_id, task_id, batch_no, source_state, revision, "
                + "source_updated_at, created_at, updated_at FROM pa_trace_snapshots "
                + "WHERE tenant_id = ? AND source_type = ? AND source_id = ?",
            tenantId, type.name(), sourceId));
    }

    public Map<String, Object> listSnapshots(String tenantId, SnapshotType type, int page, int size) {
        int safeSize = Math.max(1, Math.min(size, 200));
        int safePage = Math.max(page, 0);
        Long total = jdbc.queryForObject(
            "SELECT count(*) FROM pa_trace_snapshots WHERE tenant_id = ? AND source_type = ?",
            Long.class, tenantId, type.name());
        List<Map<String, Object>> rows = jdbc.queryForList(
            "SELECT id, source_type, source_id, task_id, batch_no, source_state, revision, source_updated_at, "
                + "created_at, updated_at FROM pa_trace_snapshots WHERE tenant_id = ? AND source_type = ? "
                + "ORDER BY updated_at DESC, id DESC LIMIT ? OFFSET ?",
            tenantId, type.name(), safeSize, safePage * safeSize);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("rows", rows);
        result.put("items", rows);
        result.put("total", total == null ? 0L : total.longValue());
        result.put("page", safePage);
        result.put("size", safeSize);
        return result;
    }

    private boolean isPostgres() {
        Boolean current = postgres;
        if (current != null) {
            return current.booleanValue();
        }
        try (Connection connection = dataSource.getConnection()) {
            current = connection.getMetaData().getDatabaseProductName().toLowerCase().contains("postgresql");
        } catch (Exception exception) {
            current = Boolean.FALSE;
        }
        postgres = current;
        return current.booleanValue();
    }

    private static Map<String, Object> first(List<Map<String, Object>> rows) {
        return rows == null || rows.isEmpty() ? null : rows.get(0);
    }

    private static Number number(Object value) {
        return value instanceof Number ? (Number) value : Long.valueOf(String.valueOf(value));
    }

    private static String normalizeTenant(String value) {
        String normalized = normalize(value);
        return normalized.isEmpty() ? "default" : normalized;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
