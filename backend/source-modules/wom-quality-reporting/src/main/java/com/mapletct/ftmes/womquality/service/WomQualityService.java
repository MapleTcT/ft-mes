package com.mapletct.ftmes.womquality.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mapletct.ftmes.womquality.api.BadQuantityCreateRequest;
import com.mapletct.ftmes.womquality.api.BadQuantityReverseRequest;
import com.mapletct.ftmes.womquality.domain.WomQualityBusinessException;
import com.mapletct.ftmes.womquality.integration.MaterialWmsClient;
import com.mapletct.ftmes.womquality.repository.WomQualityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class WomQualityService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final WomQualityRepository repository;
    private final MaterialWmsClient materialWmsClient;
    private final ObjectMapper objectMapper;

    public WomQualityService(
            WomQualityRepository repository,
            MaterialWmsClient materialWmsClient,
            ObjectMapper objectMapper) {
        this.repository = repository;
        this.materialWmsClient = materialWmsClient;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> listTasks(
            String tenantId, String keyword, int requestedPage, int requestedSize) {
        int page = Math.max(0, requestedPage);
        int size = Math.max(1, Math.min(200, requestedSize));
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("items", apiRows(repository.listTasks(tenantId, keyword, page, size)));
        result.put("total", repository.countTasks(keyword));
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listOutputs(String tenantId, String taskId) {
        return apiRows(repository.listOutputs(tenantId, identifier(taskId, "taskId")));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> listReports(
            String tenantId,
            String taskId,
            String keyword,
            int requestedPage,
            int requestedSize) {
        int page = Math.max(0, requestedPage);
        int size = Math.max(1, Math.min(200, requestedSize));
        Long parsedTaskId = trim(taskId).isEmpty() ? null : identifier(taskId, "taskId");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("items", apiRows(repository.listReports(
            tenantId, parsedTaskId, keyword, page, size)));
        result.put("total", repository.countReports(tenantId, parsedTaskId, keyword));
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> detail(String tenantId, String reportId) {
        long id = identifier(reportId, "reportId");
        Map<String, Object> report = repository.findReport(tenantId, id);
        if (report == null) {
            throw new WomQualityBusinessException(404, "不良数量登记不存在: " + reportId);
        }
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("report", apiMap(report));
        result.put("events", apiRows(repository.events(id)));
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> qualityContext(String inspectId) {
        long id = identifier(inspectId, "inspectId");
        Map<String, Object> context = repository.qualityContext(id);
        if (context == null) {
            throw new WomQualityBusinessException(404, "检验申请未关联制造任务: " + inspectId);
        }
        return apiMap(context);
    }

    @Transactional
    public Map<String, Object> create(
            String tenantId, BadQuantityCreateRequest request, String actor) {
        validateCreate(request);
        long taskId = identifier(request.getTaskId(), "taskId");
        long outputId = identifier(request.getSourceOutputId(), "sourceOutputId");
        Map<String, Object> source = repository.findTaskOutput(taskId, outputId);
        if (source == null) {
            throw new WomQualityBusinessException(404, "产出记录不存在或不属于所选制造任务");
        }
        BigDecimal reportedQuantity = decimal(source.get("reported_quantity"));
        if (reportedQuantity.compareTo(ZERO) <= 0) {
            throw new WomQualityBusinessException(409, "产出记录没有可登记的不良数量");
        }
        BigDecimal badQuantity = request.getBadQuantity();
        if (badQuantity.compareTo(reportedQuantity) > 0) {
            throw new WomQualityBusinessException(400, "不良数量不能大于本次报工数量");
        }
        BigDecimal goodQuantity = reportedQuantity.subtract(badQuantity);
        String requestHash = requestHash(
            tenantId, taskId, outputId, badQuantity,
            request.getUnitCode(), request.getReasonCode(), request.getReasonText());
        String requestId = required(request.getRequestId(), "requestId", 128);
        Map<String, Object> existingRequest = repository.findByRequest(tenantId, requestId);
        if (existingRequest != null) {
            if (!requestHash.equals(string(existingRequest.get("request_hash")))) {
                throw new WomQualityBusinessException(409, "requestId 已被不同内容使用");
            }
            return withIdempotent(existingRequest, true);
        }
        Map<String, Object> active = repository.findActiveByOutput(tenantId, outputId);
        if (active != null) {
            throw new WomQualityBusinessException(409, "该产出记录已有生效的不良数量登记，请先冲销");
        }

        Map<String, Object> qualityLinks = repository.latestQualityLinks(taskId);
        Long reportId = repository.insertReport(
            tenantId,
            requestId,
            requestHash,
            source,
            reportedQuantity,
            goodQuantity,
            badQuantity,
            optional(request.getUnitCode(), 64),
            required(request.getReasonCode(), "reasonCode", 64),
            optional(request.getReasonText(), 1000),
            qualityLinks,
            limit(actor, 128)
        );
        if (reportId == null) {
            Map<String, Object> concurrent = repository.findByRequest(tenantId, requestId);
            if (concurrent != null && requestHash.equals(string(concurrent.get("request_hash")))) {
                return withIdempotent(concurrent, true);
            }
            throw new WomQualityBusinessException(409, "不良数量登记并发冲突，请刷新后重试");
        }
        Map<String, Object> created = repository.lockReport(tenantId, reportId);
        repository.addEvent(
            reportId, "CONFIRMED", limit(actor, 128), request.getReasonText(), json(created));
        return withIdempotent(created, false);
    }

    @Transactional
    public Map<String, Object> requestReversal(
            String tenantId,
            String reportId,
            BadQuantityReverseRequest request,
            String actor) {
        if (request == null) {
            throw new WomQualityBusinessException(400, "请求体不能为空");
        }
        String reason = required(request.getReason(), "reason", 1000);
        long id = identifier(reportId, "reportId");
        Map<String, Object> report = repository.lockReport(tenantId, id);
        if (report == null) {
            throw new WomQualityBusinessException(404, "不良数量登记不存在: " + reportId);
        }
        if (!"CONFIRMED".equals(string(report.get("status")))) {
            throw new WomQualityBusinessException(409, "只有已确认登记可以冲销");
        }
        long currentVersion = number(report.get("version")).longValue();
        if (currentVersion != request.getVersion()) {
            throw new WomQualityBusinessException(409, "登记版本已变化，请刷新后重试");
        }
        repository.requestReversal(tenantId, id, currentVersion, reason, limit(actor, 128));
        Map<String, Object> pending = repository.lockReport(tenantId, id);
        repository.addEvent(
            id, "REVERSAL_REQUESTED", limit(actor, 128), reason, json(pending));
        return apiMap(pending);
    }

    @Transactional
    public Map<String, Object> synchronize(String tenantId, String reportId, String actor) {
        long id = identifier(reportId, "reportId");
        Map<String, Object> report = repository.lockReport(tenantId, id);
        if (report == null) {
            throw new WomQualityBusinessException(404, "不良数量登记不存在: " + reportId);
        }
        String status = string(report.get("status"));
        if ("REVERSED".equals(status)
                || ("CONFIRMED".equals(status) && "APPLIED".equals(string(report.get("wms_sync_state"))))) {
            return apiMap(report);
        }
        String action = "REVERSAL_PENDING".equals(status) ? "REVERSE" : "APPLY";
        long version = number(report.get("version")).longValue();
        Map<String, Object> wmsResult;
        try {
            wmsResult = materialWmsClient.apply(tenantId, report, action);
        } catch (WomQualityBusinessException exception) {
            repository.markSyncFailed(tenantId, id, version, exception.getMessage());
            repository.addEvent(
                id, "WMS_SYNC_FAILED", limit(actor, 128), exception.getMessage(), json(report));
            return apiMap(repository.lockReport(tenantId, id));
        }
        if ("REVERSE".equals(action)) {
            repository.markReversed(tenantId, id, version);
            repository.addEvent(
                id, "REVERSED", limit(actor, 128), string(report.get("reversal_reason")), json(wmsResult));
        } else {
            repository.markSyncApplied(tenantId, id, version);
            repository.addEvent(
                id, "WMS_SYNC_APPLIED", limit(actor, 128), null, json(wmsResult));
        }
        return apiMap(repository.lockReport(tenantId, id));
    }

    @Transactional
    public Map<String, Object> linkLatestQuality(
            String tenantId, String reportId, long expectedVersion, String actor) {
        long id = identifier(reportId, "reportId");
        Map<String, Object> report = repository.lockReport(tenantId, id);
        if (report == null) {
            throw new WomQualityBusinessException(404, "不良数量登记不存在: " + reportId);
        }
        Map<String, Object> links = repository.latestQualityLinks(
            number(report.get("task_id")).longValue());
        if (links.isEmpty() || links.get("qcs_inspect_id") == null) {
            throw new WomQualityBusinessException(404, "当前制造任务尚无可关联的 QCS 请检记录");
        }
        repository.linkLatestQuality(tenantId, id, expectedVersion, links);
        Map<String, Object> linked = repository.lockReport(tenantId, id);
        repository.addEvent(id, "QUALITY_LINKED", limit(actor, 128), "关联最新 QCS 记录", json(links));
        return apiMap(linked);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> syncCandidates(int limit) {
        return repository.syncCandidates(Math.max(1, Math.min(100, limit)));
    }

    private void validateCreate(BadQuantityCreateRequest request) {
        if (request == null) {
            throw new WomQualityBusinessException(400, "请求体不能为空");
        }
        required(request.getRequestId(), "requestId", 128);
        required(request.getTaskId(), "taskId", 64);
        required(request.getSourceOutputId(), "sourceOutputId", 64);
        required(request.getReasonCode(), "reasonCode", 64);
        if (request.getBadQuantity() == null || request.getBadQuantity().compareTo(ZERO) <= 0) {
            throw new WomQualityBusinessException(400, "badQuantity 必须大于 0");
        }
        optional(request.getReasonText(), 1000);
        optional(request.getUnitCode(), 64);
    }

    private Map<String, Object> withIdempotent(Map<String, Object> report, boolean idempotent) {
        Map<String, Object> result = apiMap(report);
        result.put("idempotent", idempotent);
        return result;
    }

    private List<Map<String, Object>> apiRows(List<Map<String, Object>> rows) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>(rows.size());
        for (Map<String, Object> row : rows) {
            result.add(apiMap(row));
        }
        return result;
    }

    private Map<String, Object> apiMap(Map<String, Object> source) {
        if (source == null) {
            return Collections.emptyMap();
        }
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value != null && ("id".equals(key) || key.endsWith("_id") || key.endsWith("Id"))) {
                result.put(key, String.valueOf(value));
            } else {
                result.put(key, value);
            }
        }
        return result;
    }

    private String requestHash(
            String tenantId,
            long taskId,
            long outputId,
            BigDecimal badQuantity,
            String unitCode,
            String reasonCode,
            String reasonText) {
        String canonical = tenantId + "|" + taskId + "|" + outputId + "|"
            + badQuantity.stripTrailingZeros().toPlainString() + "|" + trim(unitCode) + "|"
            + trim(reasonCode) + "|" + trim(reasonText);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(canonical.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(bytes.length * 2);
            for (byte value : bytes) {
                result.append(String.format("%02x", value & 0xff));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new WomQualityBusinessException(500, "审计内容无法序列化");
        }
    }

    private static long identifier(String value, String field) {
        String normalized = required(value, field, 64);
        if (!normalized.matches("[0-9]+")) {
            throw new WomQualityBusinessException(400, field + " 必须是十进制整数文本");
        }
        try {
            return Long.parseLong(normalized);
        } catch (NumberFormatException exception) {
            throw new WomQualityBusinessException(400, field + " 超出 bigint 范围");
        }
    }

    private static String required(String value, String field, int maxLength) {
        String normalized = trim(value);
        if (normalized.isEmpty()) {
            throw new WomQualityBusinessException(400, field + " 不能为空");
        }
        if (normalized.length() > maxLength) {
            throw new WomQualityBusinessException(400, field + " 最长 " + maxLength + " 个字符");
        }
        return normalized;
    }

    private static String optional(String value, int maxLength) {
        String normalized = trim(value);
        if (normalized.length() > maxLength) {
            throw new WomQualityBusinessException(400, "文本最长 " + maxLength + " 个字符");
        }
        return normalized.isEmpty() ? null : normalized;
    }

    private static String limit(String value, int maxLength) {
        String normalized = trim(value);
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private static String string(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static Number number(Object value) {
        if (value instanceof Number) {
            return (Number) value;
        }
        return new BigDecimal(String.valueOf(value));
    }

    private static BigDecimal decimal(Object value) {
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value == null) {
            return ZERO;
        }
        return new BigDecimal(String.valueOf(value));
    }
}
