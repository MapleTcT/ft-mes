package com.mapletct.ftmes.rmformula.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mapletct.ftmes.rmformula.api.FormulaSaveRequest;
import com.mapletct.ftmes.rmformula.domain.RmFormulaBusinessException;
import com.mapletct.ftmes.rmformula.repository.FormulaEditorRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FormulaEditorService {
    private static final Pattern REQUEST_ID = Pattern.compile("[A-Za-z0-9._:-]{8,80}");
    private static final BigDecimal MAX_QUANTITY = new BigDecimal("999999999999999.999999");
    private final FormulaEditorRepository repository;
    private final ObjectMapper objectMapper;

    public FormulaEditorService(FormulaEditorRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> list(String query, Integer limit) {
        String normalized = text(query, 120, "查询条件");
        int safeLimit = limit == null ? 100 : Math.max(1, Math.min(limit, 300));
        List<Map<String, Object>> formulas = repository.formulas(normalized, safeLimit);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("items", formulas);
        result.put("count", formulas.size());
        result.put("limit", safeLimit);
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> detail(long formulaId) {
        Map<String, Object> formula = repository.formula(formulaId);
        if (formula.isEmpty()) {
            throw new RmFormulaBusinessException(404, "配方不存在或已停用");
        }
        return detail(formula);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> materials(String query, Integer limit) {
        String normalized = text(query, 120, "产品查询条件");
        int safeLimit = limit == null ? 300 : Math.max(1, Math.min(limit, 500));
        return referenceResult(repository.materials(normalized, safeLimit), safeLimit);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> batchServers(String query, Integer limit) {
        String normalized = text(query, 120, "Batch 服务器查询条件");
        int safeLimit = limit == null ? 100 : Math.max(1, Math.min(limit, 300));
        return referenceResult(repository.batchServers(normalized, safeLimit), safeLimit);
    }

    @Transactional
    public Map<String, Object> create(String tenant, FormulaSaveRequest request) {
        normalizeAndValidate(request, true);
        repository.lock(tenant + "|request|" + request.getRequestId());
        String payloadJson = json(request);
        String requestHash = sha256(payloadJson);
        Map<String, Object> previous = repository.revisionByRequest(tenant, request.getRequestId());
        if (!previous.isEmpty()) {
            return idempotentResult(previous, requestHash, null);
        }
        repository.lock("formula-code|" + request.getFormulaCode().toLowerCase());
        if (repository.formulaCodeUsed(request.getFormulaCode(), null)) {
            throw new RmFormulaBusinessException(409, "配方编码已存在");
        }

        long formulaId = repository.nextFormulaId();
        assignChildIds(formulaId, request, false);
        repository.insertFormula(formulaId, request);
        saveChildren(formulaId, request);
        long revisionId = repository.insertRevision(
                tenant, formulaId, request.getRequestId(), requestHash, 0, json(detail(formulaId)));
        return savedResult(formulaId, revisionId, false);
    }

    @Transactional
    public Map<String, Object> update(String tenant, long formulaId, FormulaSaveRequest request) {
        normalizeAndValidate(request, false);
        repository.lock(tenant + "|request|" + request.getRequestId());
        String requestJson = json(request);
        String requestHash = sha256(requestJson);
        Map<String, Object> previous = repository.revisionByRequest(tenant, request.getRequestId());
        if (!previous.isEmpty()) {
            return idempotentResult(previous, requestHash, formulaId);
        }

        repository.lock("formula|" + formulaId);
        Map<String, Object> current = repository.formula(formulaId);
        if (current.isEmpty()) {
            throw new RmFormulaBusinessException(404, "配方不存在或已停用");
        }
        repository.lock("formula-code|" + request.getFormulaCode().toLowerCase());
        if (repository.formulaCodeUsed(request.getFormulaCode(), formulaId)) {
            throw new RmFormulaBusinessException(409, "配方编码已存在");
        }
        assignChildIds(formulaId, request, true);
        if (repository.updateFormula(formulaId, request.getExpectedVersion(), request) != 1) {
            throw new RmFormulaBusinessException(409, "配方已被其他用户修改，请刷新后重试");
        }
        saveChildren(formulaId, request);
        int savedVersion = request.getExpectedVersion() + 1;
        long revisionId = repository.insertRevision(
                tenant, formulaId, request.getRequestId(), requestHash, savedVersion, json(detail(formulaId)));
        return savedResult(formulaId, revisionId, false);
    }

    private Map<String, Object> detail(Map<String, Object> formula) {
        long formulaId = ((Number) formula.get("id")).longValue();
        Map<String, Object> result = new LinkedHashMap<String, Object>(formula);
        result.put("processes", repository.processes(formulaId));
        result.put("activities", repository.activities(formulaId));
        result.put("latestRevision", repository.latestRevision(formulaId));
        result.put("latestDelivery", repository.latestDelivery(formulaId));
        return result;
    }

    private Map<String, Object> savedResult(long formulaId, long revisionId, boolean idempotent) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("formula", detail(formulaId));
        result.put("revisionId", revisionId);
        result.put("idempotent", idempotent);
        return result;
    }

    private Map<String, Object> idempotentResult(Map<String, Object> revision, String requestHash, Long formulaId) {
        if (!requestHash.equals(String.valueOf(revision.get("requestHash")))) {
            throw new RmFormulaBusinessException(409, "请求编号已被不同内容使用");
        }
        long previousFormulaId = ((Number) revision.get("formulaId")).longValue();
        if (formulaId != null && formulaId.longValue() != previousFormulaId) {
            throw new RmFormulaBusinessException(409, "请求编号属于其他配方");
        }
        return savedResult(previousFormulaId, ((Number) revision.get("id")).longValue(), true);
    }

    private void assignChildIds(long formulaId, FormulaSaveRequest request, boolean existingFormula) {
        Map<String, Long> processIds = new LinkedHashMap<String, Long>();
        for (FormulaSaveRequest.ProcessInput process : request.getProcesses()) {
            if (process.getId() != null) {
                if (!existingFormula || !repository.processBelongs(process.getId(), formulaId)) {
                    throw new RmFormulaBusinessException(409, "工序不属于当前配方: " + process.getId());
                }
            } else {
                process.setId(repository.nextProcessId());
            }
            processIds.put(process.getClientKey(), process.getId());
        }
        for (FormulaSaveRequest.ActivityInput activity : request.getActivities()) {
            if (activity.getId() != null) {
                if (!existingFormula || !repository.activityBelongs(activity.getId(), formulaId)) {
                    throw new RmFormulaBusinessException(409, "活动不属于当前配方: " + activity.getId());
                }
            } else {
                activity.setId(repository.nextActivityId());
            }
            if (!processIds.containsKey(activity.getProcessKey())) {
                throw new RmFormulaBusinessException(400, "活动关联的工序不存在: " + activity.getProcessKey());
            }
        }
    }

    private void saveChildren(long formulaId, FormulaSaveRequest request) {
        Map<String, Long> processIds = new LinkedHashMap<String, Long>();
        repository.retireProcesses(formulaId);
        for (FormulaSaveRequest.ProcessInput process : request.getProcesses()) {
            repository.saveProcess(formulaId, process);
            processIds.put(process.getClientKey(), process.getId());
        }
        repository.retireActivities(formulaId);
        for (FormulaSaveRequest.ActivityInput activity : request.getActivities()) {
            repository.saveActivity(formulaId, processIds.get(activity.getProcessKey()), activity);
        }
    }

    private void normalizeAndValidate(FormulaSaveRequest request, boolean creating) {
        if (request == null) {
            throw new RmFormulaBusinessException(400, "请求内容不能为空");
        }
        request.setRequestId(text(request.getRequestId(), 80, "请求编号"));
        if (!REQUEST_ID.matcher(request.getRequestId()).matches()) {
            throw new RmFormulaBusinessException(400, "请求编号格式不正确");
        }
        if (!creating && request.getExpectedVersion() == null) {
            throw new RmFormulaBusinessException(400, "缺少配方版本，无法进行并发校验");
        }
        if (request.getExpectedVersion() != null && request.getExpectedVersion() < 0) {
            throw new RmFormulaBusinessException(400, "配方版本不能为负数");
        }
        request.setFormulaCode(required(request.getFormulaCode(), 128, "配方编码"));
        request.setFormulaName(required(request.getFormulaName(), 255, "配方名称"));
        request.setFormulaEdition(text(request.getFormulaEdition(), 64, "配方版本"));
        request.setBatchFormulaId(text(request.getBatchFormulaId(), 128, "Batch 配方标识"));
        request.setBatchFormulaCode(text(request.getBatchFormulaCode(), 128, "Batch 配方编码"));
        request.setBatchFormulaEdition(text(request.getBatchFormulaEdition(), 64, "Batch 配方版本"));
        request.setNormalSize(text(request.getNormalSize(), 64, "标准批量"));
        request.setDescription(text(request.getDescription(), 2000, "说明"));
        request.setReportType(text(request.getReportType(), 128, "报表类型"));
        request.setSetProcess(text(request.getSetProcess(), 128, "配方类型"));
        if (request.getProcesses() == null) {
            request.setProcesses(new ArrayList<FormulaSaveRequest.ProcessInput>());
        }
        if (request.getActivities() == null) {
            request.setActivities(new ArrayList<FormulaSaveRequest.ActivityInput>());
        }
        if (request.getProcesses().isEmpty()) {
            throw new RmFormulaBusinessException(400, "至少需要一个工序");
        }
        normalizeProcesses(request.getProcesses());
        normalizeActivities(request.getActivities());
        validateReferences(request);
    }

    private void validateReferences(FormulaSaveRequest request) {
        if (request.getProductId() == null || request.getProductId() <= 0) {
            throw new RmFormulaBusinessException(400, "请选择有效产品");
        }
        if (!repository.materialExists(request.getProductId())) {
            throw new RmFormulaBusinessException(400, "所选产品不存在或已停用");
        }
        if (request.getBatchServerId() != null) {
            if (request.getBatchServerId() <= 0 || !repository.batchServerExists(request.getBatchServerId())) {
                throw new RmFormulaBusinessException(400, "所选 Batch 服务器不存在或已停用");
            }
        }
    }

    private static Map<String, Object> referenceResult(List<Map<String, Object>> items, int limit) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("items", items);
        result.put("count", items.size());
        result.put("limit", limit);
        return result;
    }

    private void normalizeProcesses(List<FormulaSaveRequest.ProcessInput> processes) {
        Set<String> keys = new LinkedHashSet<String>();
        int order = 1;
        for (FormulaSaveRequest.ProcessInput process : processes) {
            if (process == null) {
                throw new RmFormulaBusinessException(400, "工序内容不能为空");
            }
            String fallback = process.getId() == null ? "new-process-" + order : "p-" + process.getId();
            process.setClientKey(required(process.getClientKey() == null ? fallback : process.getClientKey(), 80, "工序标识"));
            if (!keys.add(process.getClientKey())) {
                throw new RmFormulaBusinessException(400, "工序标识重复: " + process.getClientKey());
            }
            process.setName(required(process.getName(), 255, "工序名称"));
            process.setProcessSort(text(process.getProcessSort(), 64, "工序排序"));
            process.setBatchUnitId(text(process.getBatchUnitId(), 128, "Batch 单元"));
            process.setRemark(text(process.getRemark(), 1000, "工序备注"));
            process.setAutoStart(Boolean.TRUE.equals(process.getAutoStart()));
            process.setFirstProcess(Boolean.TRUE.equals(process.getFirstProcess()));
            process.setLastProcess(Boolean.TRUE.equals(process.getLastProcess()));
            if (process.getExecutionOrder() == null) {
                process.setExecutionOrder(order);
            }
            nonNegative(process.getDuration(), "工序时长");
            order += 1;
        }
    }

    private void normalizeActivities(List<FormulaSaveRequest.ActivityInput> activities) {
        Set<String> keys = new LinkedHashSet<String>();
        int order = 1;
        for (FormulaSaveRequest.ActivityInput activity : activities) {
            if (activity == null) {
                throw new RmFormulaBusinessException(400, "活动内容不能为空");
            }
            String fallback = activity.getId() == null ? "new-activity-" + order : "a-" + activity.getId();
            activity.setClientKey(required(activity.getClientKey() == null ? fallback : activity.getClientKey(), 80, "活动标识"));
            if (!keys.add(activity.getClientKey())) {
                throw new RmFormulaBusinessException(400, "活动标识重复: " + activity.getClientKey());
            }
            activity.setProcessKey(required(activity.getProcessKey(), 80, "关联工序"));
            activity.setName(required(activity.getName(), 255, "活动名称"));
            activity.setActiveType(text(activity.getActiveType(), 128, "活动类型"));
            activity.setBatchPhaseId(text(activity.getBatchPhaseId(), 255, "Batch 阶段标识"));
            activity.setBatchPhaseName(text(activity.getBatchPhaseName(), 255, "Batch 阶段名称"));
            activity.setBatchSite(text(activity.getBatchSite(), 255, "Batch 站点"));
            activity.setDispatchSystem(text(activity.getDispatchSystem(), 128, "调度系统"));
            activity.setExecutionSystem(text(activity.getExecutionSystem(), 128, "执行系统"));
            activity.setReleaseConditions(text(activity.getReleaseConditions(), 2000, "释放条件"));
            activity.setResponseItem(text(activity.getResponseItem(), 1000, "响应点位"));
            activity.setSetItem(text(activity.getSetItem(), 1000, "设定点位"));
            activity.setUseItem(text(activity.getUseItem(), 1000, "使用点位"));
            activity.setRemark(text(activity.getRemark(), 1000, "活动备注"));
            activity.setAutomatic(Boolean.TRUE.equals(activity.getAutomatic()));
            activity.setFixedQuantity(Boolean.TRUE.equals(activity.getFixedQuantity()));
            nonNegative(activity.getQuantity(), "活动数量");
            nonNegative(activity.getMinimumQuantity(), "最小数量");
            nonNegative(activity.getMaximumQuantity(), "最大数量");
            if (activity.getMinimumQuantity() != null && activity.getMaximumQuantity() != null
                    && activity.getMinimumQuantity().compareTo(activity.getMaximumQuantity()) > 0) {
                throw new RmFormulaBusinessException(400, "活动最小数量不能大于最大数量");
            }
            order += 1;
        }
    }

    private static void nonNegative(BigDecimal value, String label) {
        if (value != null && (value.signum() < 0 || value.compareTo(MAX_QUANTITY) > 0)) {
            throw new RmFormulaBusinessException(400, label + "超出允许范围");
        }
    }

    private static String required(String value, int maxLength, String label) {
        String normalized = text(value, maxLength, label);
        if (normalized.isEmpty()) {
            throw new RmFormulaBusinessException(400, label + "不能为空");
        }
        return normalized;
    }

    private static String text(String value, int maxLength, String label) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.length() > maxLength) {
            throw new RmFormulaBusinessException(400, label + "长度不能超过 " + maxLength);
        }
        for (int index = 0; index < normalized.length(); index += 1) {
            if (Character.isISOControl(normalized.charAt(index))) {
                throw new RmFormulaBusinessException(400, label + "包含非法控制字符");
            }
        }
        return normalized;
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot serialize formula payload", exception);
        }
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(bytes.length * 2);
            for (byte item : bytes) {
                result.append(String.format("%02x", item & 0xff));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }
}
