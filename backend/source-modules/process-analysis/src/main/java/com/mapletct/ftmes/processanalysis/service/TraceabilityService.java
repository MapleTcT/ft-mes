package com.mapletct.ftmes.processanalysis.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mapletct.ftmes.processanalysis.domain.ProcessAnalysisBusinessException;
import com.mapletct.ftmes.processanalysis.domain.SnapshotType;
import com.mapletct.ftmes.processanalysis.repository.ProcessAnalysisRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class TraceabilityService {

    private final ProcessAnalysisRepository repository;
    private final ObjectMapper objectMapper;

    public TraceabilityService(ProcessAnalysisRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> availability(String batchNo) {
        String normalizedBatch = required(batchNo, "batchNo");
        Map<String, Object> task = repository.findTaskByBatch(normalizedBatch);
        boolean available = task != null && repository.hasTaskExecution(number(task.get("id")));
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("dealRes", available);
        result.put("batchNo", normalizedBatch);
        result.put("taskId", available ? task.get("id") : null);
        return result;
    }

    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public Map<String, Object> trace(String tenantId, String batchNo, String productNo) {
        String normalizedBatch = required(batchNo, "batchNo");
        Map<String, Object> facts = repository.loadTraceFacts(normalizeTenant(tenantId), normalizedBatch, trim(productNo));
        if (facts.isEmpty()) {
            throw new ProcessAnalysisBusinessException(404, "未找到批次对应的制造指令");
        }

        List<Map<String, Object>> taskExecutions = rows(facts, "taskExecutions");
        List<Map<String, Object>> processes = rows(facts, "processes");
        List<Map<String, Object>> processExecutions = rows(facts, "processExecutions");
        List<Map<String, Object>> activities = rows(facts, "activities");
        List<Map<String, Object>> activityExecutions = rows(facts, "activityExecutions");
        List<Map<String, Object>> materialInputs = rows(facts, "materialInputs");
        List<Map<String, Object>> materialOutputs = rows(facts, "materialOutputs");
        List<Map<String, Object>> outputRecords = rows(facts, "materialOutputRecords");
        List<Map<String, Object>> inspections = rows(facts, "inspections");
        List<Map<String, Object>> reports = rows(facts, "inspectionReports");
        List<Map<String, Object>> reportItems = rows(facts, "inspectionReportItems");
        List<Map<String, Object>> dispositions = rows(facts, "unqualifiedDispositions");
        List<Map<String, Object>> wmsDocuments = rows(facts, "wmsDocuments");
        List<Map<String, Object>> wmsLines = rows(facts, "wmsLines");
        List<Map<String, Object>> wmsTransactions = rows(facts, "wmsTransactions");

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("batchNo", normalizedBatch);
        result.put("productNo", trim(productNo));
        result.put("task", facts.get("task"));
        result.put("taskExecutions", taskExecutions);
        result.put("processes", processes);
        result.put("processExecutions", processExecutions);
        result.put("activities", activities);
        result.put("activityExecutions", activityExecutions);
        result.put("batchInfo", facts.get("batchInfo"));

        Map<String, Object> materials = new LinkedHashMap<String, Object>();
        materials.put("inputs", materialInputs);
        materials.put("outputs", materialOutputs);
        materials.put("outputRecords", outputRecords);
        materials.put("lineage", lineage(normalizedBatch, materialInputs, materialOutputs, outputRecords));
        result.put("materials", materials);

        Map<String, Object> quality = new LinkedHashMap<String, Object>();
        quality.put("inspections", inspections);
        quality.put("reports", reports);
        quality.put("reportItems", reportItems);
        quality.put("dispositions", dispositions);
        result.put("quality", quality);

        Map<String, Object> inventory = new LinkedHashMap<String, Object>();
        inventory.put("documents", wmsDocuments);
        inventory.put("lines", wmsLines);
        inventory.put("transactions", wmsTransactions);
        result.put("inventory", inventory);

        List<Map<String, Object>> timeline = timeline(
            (Map<String, Object>) facts.get("task"), taskExecutions, processes, processExecutions,
            activities, activityExecutions, inspections, reports, dispositions, wmsTransactions);
        result.put("timeline", timeline);

        Map<String, Object> summary = new LinkedHashMap<String, Object>();
        summary.put("processCount", processes.size());
        summary.put("activityCount", activities.size());
        summary.put("materialEventCount", materialInputs.size() + materialOutputs.size() + outputRecords.size());
        summary.put("qualityEventCount", inspections.size() + reports.size() + reportItems.size() + dispositions.size());
        summary.put("inventoryEventCount", wmsTransactions.size());
        summary.put("timelineEventCount", timeline.size());
        result.put("summary", summary);
        return result;
    }

    @Transactional
    public Map<String, Object> analyzeTask(String tenantId, long sourceId) {
        return snapshot(tenantId, SnapshotType.TASK, sourceId, repository.findTaskExecution(sourceId));
    }

    @Transactional
    public Map<String, Object> analyzeProcess(String tenantId, long sourceId) {
        return snapshot(tenantId, SnapshotType.PROCESS, sourceId, repository.findProcessExecution(sourceId));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> processExecutionDetail(String tenantId, long sourceId) {
        if (sourceId <= 0) {
            throw new ProcessAnalysisBusinessException(400, "工序执行记录 ID 必须大于 0");
        }
        Map<String, Object> current = repository.findProcessExecutionDetail(sourceId);
        if (current == null) {
            throw new ProcessAnalysisBusinessException(404, "工序执行记录不存在");
        }

        List<Map<String, Object>> executions = repository.findTaskProcessExecutions(number(current.get("task_id")));
        int currentIndex = -1;
        for (int index = 0; index < executions.size(); index++) {
            if (number(executions.get(index).get("id")) == sourceId) {
                currentIndex = index;
                break;
            }
        }
        Map<String, Object> previous = currentIndex > 0 ? executions.get(currentIndex - 1) : null;
        Map<String, Object> next = currentIndex >= 0 && currentIndex + 1 < executions.size()
            ? executions.get(currentIndex + 1) : null;

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("processExecution", current);
        result.put("previousProcess", previous);
        result.put("nextProcess", next);

        Map<String, Object> handover = new LinkedHashMap<String, Object>();
        handover.put("previousToCurrent", boundary(previous, current));
        handover.put("currentToNext", boundary(current, next));
        result.put("handover", handover);

        Map<String, Object> bpiContext = new LinkedHashMap<String, Object>();
        bpiContext.put("tenantId", string(current.get("bpi_tenant_id")));
        bpiContext.put("plantId", string(current.get("bpi_plant_id")));
        bpiContext.put("lineId", string(current.get("bpi_line_id")));
        bpiContext.put("orderId", string(current.get("task_no")));
        bpiContext.put("available",
            !string(current.get("bpi_tenant_id")).isEmpty()
                && !string(current.get("bpi_plant_id")).isEmpty()
                && !string(current.get("bpi_line_id")).isEmpty());
        result.put("bpiContext", bpiContext);
        result.put("tenantId", normalizeTenant(tenantId));
        return result;
    }

    @Transactional
    public Map<String, Object> analyzeActivity(String tenantId, long sourceId) {
        return snapshot(tenantId, SnapshotType.ACTIVITY, sourceId, repository.findActivityExecution(sourceId));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> listSnapshots(String tenantId, SnapshotType type, int page, int size) {
        return repository.listSnapshots(tenantId, type, page, size);
    }

    private Map<String, Object> snapshot(
            String tenantId, SnapshotType type, long sourceId, Map<String, Object> source) {
        if (sourceId <= 0) {
            throw new ProcessAnalysisBusinessException(400, "执行记录 ID 必须大于 0");
        }
        if (source == null) {
            throw new ProcessAnalysisBusinessException(404, type.name() + " 执行记录不存在");
        }
        long taskId = number(source.get("task_id"));
        String batchNo = string(source.get("produce_batch_num"));
        String state = firstNonBlank(
            source.get("task_run_state"), source.get("process_run_state"), source.get("run_state"));
        Map<String, Object> metrics = new LinkedHashMap<String, Object>();
        metrics.put("source", source);
        metrics.put("capturedState", state);
        metrics.put("capturedBatchNo", batchNo);
        Map<String, Object> row = repository.upsertSnapshot(
            normalizeTenant(tenantId), type, sourceId, taskId, batchNo, state, json(metrics),
            timestamp(source.get("modify_time"), source.get("create_time")));
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("success", true);
        result.put("snapshot", row);
        return result;
    }

    private static List<Map<String, Object>> lineage(
            String batchNo,
            List<Map<String, Object>> inputs,
            List<Map<String, Object>> outputs,
            List<Map<String, Object>> outputRecords) {
        List<Map<String, Object>> edges = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> input : inputs) {
            String inputBatch = string(input.get("material_batch_num"));
            if (!inputBatch.isEmpty()) {
                edges.add(edge(inputBatch, batchNo, "INPUT", input.get("material_id"), input.get("use_num")));
            }
        }
        for (Map<String, Object> output : outputs) {
            String outputBatch = string(output.get("material_batch_num"));
            if (!outputBatch.isEmpty()) {
                edges.add(edge(batchNo, outputBatch, "OUTPUT", output.get("product"), output.get("output_num")));
            }
        }
        for (Map<String, Object> output : outputRecords) {
            String outputBatch = string(output.get("mat_batch_num"));
            if (!outputBatch.isEmpty()) {
                edges.add(edge(batchNo, outputBatch, "OUTPUT_RECORD", output.get("material_id"), output.get("output_num")));
            }
        }
        return edges;
    }

    private static Map<String, Object> edge(
            String fromBatch, String toBatch, String relation, Object materialId, Object quantity) {
        Map<String, Object> edge = new LinkedHashMap<String, Object>();
        edge.put("fromBatch", fromBatch);
        edge.put("toBatch", toBatch);
        edge.put("relation", relation);
        edge.put("materialId", materialId);
        edge.put("quantity", quantity);
        return edge;
    }

    private static Map<String, Object> boundary(Map<String, Object> from, Map<String, Object> to) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("fromProcessId", from == null ? null : from.get("id"));
        result.put("fromProcessName", from == null ? "" : firstNonBlank(
            from.get("planned_process_name"), from.get("name")));
        result.put("toProcessId", to == null ? null : to.get("id"));
        result.put("toProcessName", to == null ? "" : firstNonBlank(
            to.get("planned_process_name"), to.get("name")));

        Instant fromEnd = instant(from == null ? null : from.get("act_end_time"));
        Instant toStart = instant(to == null ? null : to.get("act_start_time"));
        if (from == null || to == null || fromEnd == null || toStart == null) {
            result.put("state", "OPEN");
            result.put("gapSeconds", null);
            return result;
        }
        long gapSeconds = Duration.between(fromEnd, toStart).getSeconds();
        result.put("gapSeconds", gapSeconds);
        if (gapSeconds < 0) {
            result.put("state", "OVERLAP");
        } else if (gapSeconds <= 60) {
            result.put("state", "CONTIGUOUS");
        } else {
            result.put("state", "GAP");
        }
        return result;
    }

    private static Instant instant(Object value) {
        if (value instanceof Timestamp) {
            return ((Timestamp) value).toInstant();
        }
        if (value instanceof Date) {
            return ((Date) value).toInstant();
        }
        if (value instanceof LocalDateTime) {
            return ((LocalDateTime) value).atZone(java.time.ZoneId.systemDefault()).toInstant();
        }
        return null;
    }

    private static List<Map<String, Object>> timeline(
            Map<String, Object> task,
            List<Map<String, Object>> taskExecutions,
            List<Map<String, Object>> processes,
            List<Map<String, Object>> processExecutions,
            List<Map<String, Object>> activities,
            List<Map<String, Object>> activityExecutions,
            List<Map<String, Object>> inspections,
            List<Map<String, Object>> reports,
            List<Map<String, Object>> dispositions,
            List<Map<String, Object>> wmsTransactions) {
        List<Map<String, Object>> events = new ArrayList<Map<String, Object>>();
        addEvent(events, "TASK_CREATED", "制造指令创建", "wom_produce_tasks", task, "create_time", "task_run_state");
        addEvent(events, "TASK_STARTED", "制造指令开始", "wom_produce_tasks", task, "act_start_time", "task_run_state");
        addEvent(events, "TASK_FINISHED", "制造指令结束", "wom_produce_tasks", task, "act_end_time", "task_run_state");
        addRows(events, "TASK_EXECUTION", "任务执行记录", "wom_produce_task_exelog", taskExecutions, "create_time", "task_run_state");
        addRows(events, "PROCESS_PLANNED", "工序", "wom_task_processes", processes, "create_time", "process_run_state");
        addRows(events, "PROCESS_EXECUTION", "工序执行", "wom_process_exelogs", processExecutions, "act_start_time", "process_run_state");
        addRows(events, "ACTIVITY_PLANNED", "活动", "wom_task_actives", activities, "create_time", "run_state");
        addRows(events, "ACTIVITY_EXECUTION", "活动执行", "wom_acti_exelogs", activityExecutions, "act_start_time", "run_state");
        addRows(events, "INSPECTION", "生产请检", "qcs_inspects", inspections, "create_time", "check_state");
        addRows(events, "INSPECTION_REPORT", "检验报告", "qcs_inspect_reports", reports, "create_time", "check_result");
        addRows(events, "UNQUALIFIED_DISPOSITION", "不合格处置", "qcs_un_qlf_deals", dispositions, "create_time", "status");
        addRows(events, "INVENTORY_TRANSACTION", "库存流水", "wms_inventory_transactions", wmsTransactions, "created_at", "transaction_type");
        Collections.sort(events, new Comparator<Map<String, Object>>() {
            @Override
            public int compare(Map<String, Object> left, Map<String, Object> right) {
                return string(left.get("occurredAt")).compareTo(string(right.get("occurredAt")));
            }
        });
        return events;
    }

    private static void addRows(
            List<Map<String, Object>> target,
            String type,
            String title,
            String table,
            List<Map<String, Object>> rows,
            String timeKey,
            String stateKey) {
        for (Map<String, Object> row : rows) {
            addEvent(target, type, title, table, row, timeKey, stateKey);
        }
    }

    private static void addEvent(
            List<Map<String, Object>> target,
            String type,
            String title,
            String table,
            Map<String, Object> row,
            String timeKey,
            String stateKey) {
        if (row == null || row.get(timeKey) == null) {
            return;
        }
        Map<String, Object> event = new LinkedHashMap<String, Object>();
        event.put("type", type);
        event.put("title", title);
        event.put("sourceTable", table);
        event.put("sourceId", row.get("id"));
        event.put("occurredAt", String.valueOf(row.get(timeKey)));
        event.put("state", row.get(stateKey));
        event.put("name", firstNonBlank(row.get("name"), row.get("table_no"), row.get("event_key")));
        target.add(event);
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> rows(Map<String, Object> facts, String key) {
        Object value = facts.get(key);
        return value instanceof List ? (List<Map<String, Object>>) value : Collections.<Map<String, Object>>emptyList();
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new ProcessAnalysisBusinessException(500, "追溯快照无法序列化");
        }
    }

    private static Timestamp timestamp(Object primary, Object fallback) {
        Object value = primary != null ? primary : fallback;
        if (value instanceof Timestamp) {
            return (Timestamp) value;
        }
        if (value instanceof LocalDateTime) {
            return Timestamp.valueOf((LocalDateTime) value);
        }
        if (value instanceof Date) {
            return new Timestamp(((Date) value).getTime());
        }
        return new Timestamp(System.currentTimeMillis());
    }

    private static String normalizeTenant(String tenantId) {
        String normalized = trim(tenantId);
        return normalized.isEmpty() ? "default" : normalized;
    }

    private static String required(String value, String field) {
        String normalized = trim(value);
        if (normalized.isEmpty()) {
            throw new ProcessAnalysisBusinessException(400, field + " 不能为空");
        }
        return normalized;
    }

    private static long number(Object value) {
        if (value == null) {
            return 0L;
        }
        return value instanceof Number ? ((Number) value).longValue() : Long.parseLong(String.valueOf(value));
    }

    private static String firstNonBlank(Object... values) {
        for (Object value : values) {
            String text = string(value);
            if (!text.isEmpty()) {
                return text;
            }
        }
        return "";
    }

    private static String string(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
