package com.mapletct.ftmes.womentry.service;

import com.mapletct.ftmes.womentry.api.CreateInstructionRequest;
import com.mapletct.ftmes.womentry.domain.CreateInstructionResult;
import com.mapletct.ftmes.womentry.domain.ManualTaskRequestRecord;
import com.mapletct.ftmes.womentry.domain.ProductionOption;
import com.mapletct.ftmes.womentry.domain.TaskResult;
import com.mapletct.ftmes.womentry.domain.WomEntryBusinessException;
import com.mapletct.ftmes.womentry.repository.WomProductionEntryRepository;
import com.mapletct.ftmes.womentry.support.RequestAuthContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class WomProductionEntryService {

    private static final Pattern REQUEST_ID_PATTERN = Pattern.compile("[A-Za-z0-9._:-]{8,80}");
    private static final DateTimeFormatter DATE_TIME_FORMAT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final WomProductionEntryRepository repository;
    private final WomUpstreamClient upstreamClient;
    private final ObjectMapper objectMapper;
    private final int resultPollAttempts;
    private final long resultPollIntervalMs;
    private final long processingTimeoutSeconds;

    public WomProductionEntryService(
            WomProductionEntryRepository repository,
            WomUpstreamClient upstreamClient,
            ObjectMapper objectMapper,
            @Value("${wom-production-entry.result-poll-attempts:15}") int resultPollAttempts,
            @Value("${wom-production-entry.result-poll-interval-ms:200}") long resultPollIntervalMs,
            @Value("${wom-production-entry.processing-timeout-seconds:120}") long processingTimeoutSeconds) {
        this.repository = repository;
        this.upstreamClient = upstreamClient;
        this.objectMapper = objectMapper;
        this.resultPollAttempts = Math.max(1, Math.min(resultPollAttempts, 50));
        this.resultPollIntervalMs = Math.max(0, Math.min(resultPollIntervalMs, 2000));
        this.processingTimeoutSeconds = Math.max(10, processingTimeoutSeconds);
    }

    public List<ProductionOption> options(String keyword, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 200));
        return repository.listOptions(keyword, safeLimit);
    }

    public TaskResult result(String batchCode) {
        String normalized = requiredText(batchCode, "生产批号", 128);
        return normalizeTask(repository.findActiveTaskByBatch(normalized));
    }

    public CreateInstructionResult create(
            String tenantId,
            CreateInstructionRequest request,
            RequestAuthContext authContext) {
        validateRequest(request);
        String normalizedTenant = requiredText(tenantId, "租户", 64);
        String requestId = request.getRequestId().trim();
        String batchCode = request.getBatchCode().trim();
        String requestHash = requestHash(request);
        String requestJson = toJson(request);

        ProductionOption option = repository.findOption(
            request.getProductCode().trim(),
            request.getFormulaCode().trim(),
            request.getWorkLineId().longValue()
        );
        if (option == null) {
            throw new WomEntryBusinessException(400, "产品、配方与生产线组合不存在或已失效");
        }
        if (!option.isEligible()) {
            throw new WomEntryBusinessException(400, option.getIssue());
        }

        boolean claimed = false;
        for (int raceAttempt = 0; raceAttempt < 2; raceAttempt += 1) {
            ManualTaskRequestRecord existingRequest = repository.findRequest(normalizedTenant, requestId);
            if (existingRequest != null) {
                if (!requestHash.equals(existingRequest.getRequestHash())) {
                    throw new WomEntryBusinessException(409, "requestId 已被不同的创建参数使用");
                }
                TaskResult existingTask = normalizeTask(repository.findActiveTaskByBatch(batchCode));
                if (existingTask != null) {
                    repository.markSuccess(
                        normalizedTenant,
                        requestId,
                        existingTask.getTaskId(),
                        toJson(Collections.<String, Object>singletonMap("recovered", Boolean.TRUE))
                    );
                    return new CreateInstructionResult(requestId, true, existingTask);
                }
                if ("SUCCESS".equals(existingRequest.getStatus())) {
                    throw new WomEntryBusinessException(409, "幂等记录已成功，但对应制造指令不存在");
                }
                if ("PROCESSING".equals(existingRequest.getStatus()) && !isStale(existingRequest)) {
                    throw new WomEntryBusinessException(409, "该创建请求正在处理中，请稍后查询");
                }
                if (!repository.retryRequest(normalizedTenant, requestId)) {
                    throw new WomEntryBusinessException(409, "该生产批号已有创建请求正在处理或已经成功");
                }
                claimed = true;
                break;
            }

            TaskResult existingBatch = normalizeTask(repository.findActiveTaskByBatch(batchCode));
            if (existingBatch != null) {
                throw new WomEntryBusinessException(409, "生产批号已存在，请更换批号");
            }
            if (repository.insertRequest(normalizedTenant, requestId, requestHash, batchCode, requestJson)) {
                claimed = true;
                break;
            }
        }
        if (!claimed) {
            throw new WomEntryBusinessException(409, "创建请求发生并发冲突，请重试");
        }

        try {
            Map<String, Object> upstreamResponse = upstreamClient.create(request, authContext);
            TaskResult task = waitForTask(batchCode);
            if (task == null) {
                throw new WomEntryBusinessException(502, "WOM 返回成功，但 PostgreSQL 未找到新增制造指令");
            }
            repository.markSuccess(
                normalizedTenant,
                requestId,
                task.getTaskId(),
                toJson(upstreamResponse)
            );
            return new CreateInstructionResult(requestId, false, task);
        } catch (RuntimeException exception) {
            try {
                repository.markFailed(normalizedTenant, requestId, rootMessage(exception));
            } catch (RuntimeException ignored) {
                // Preserve the business failure that caused creation to stop.
            }
            if (exception instanceof WomEntryBusinessException) {
                throw exception;
            }
            throw new WomEntryBusinessException(500, "制造指令创建失败", exception);
        }
    }

    private TaskResult waitForTask(String batchCode) {
        for (int attempt = 0; attempt < resultPollAttempts; attempt += 1) {
            TaskResult task = normalizeTask(repository.findActiveTaskByBatch(batchCode));
            if (task != null) {
                return task;
            }
            if (attempt + 1 < resultPollAttempts && resultPollIntervalMs > 0) {
                try {
                    Thread.sleep(resultPollIntervalMs);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new WomEntryBusinessException(503, "制造指令结果查询被中断", exception);
                }
            }
        }
        return null;
    }

    private TaskResult normalizeTask(TaskResult task) {
        if (task == null) {
            return null;
        }
        String openUrl = task.getPendingOpenUrl();
        if ((openUrl == null || openUrl.trim().isEmpty()) && task.getPendingId() != null) {
            openUrl = "/msService/WOM/produceTask/produceTask/makeTaskEdit"
                + "?pendingId=" + task.getPendingId()
                + "&entityCode=WOM_1.0.0_produceTask"
                + "&viewCode=WOM_1.0.0_produceTask_makeTaskEdit";
        }
        return new TaskResult(
            task.getTaskId(),
            task.getVersion(),
            task.getTableNo(),
            task.getBatchCode(),
            task.getStatus(),
            task.isValid(),
            task.getPendingId(),
            openUrl,
            task.getActivityName()
        );
    }

    private boolean isStale(ManualTaskRequestRecord record) {
        LocalDateTime updatedAt = record.getUpdatedAt();
        return updatedAt == null || updatedAt.plusSeconds(processingTimeoutSeconds).isBefore(LocalDateTime.now());
    }

    private static void validateRequest(CreateInstructionRequest request) {
        if (request == null) {
            throw new WomEntryBusinessException(400, "创建参数不能为空");
        }
        String requestId = requiredText(request.getRequestId(), "requestId", 80);
        if (!REQUEST_ID_PATTERN.matcher(requestId).matches()) {
            throw new WomEntryBusinessException(400, "requestId 格式不正确");
        }
        requiredText(request.getProductCode(), "产品编码", 200);
        requiredText(request.getFormulaCode(), "配方编码", 255);
        requiredText(request.getBatchCode(), "生产批号", 128);
        if (request.getWorkLineId() == null || request.getWorkLineId().longValue() <= 0) {
            throw new WomEntryBusinessException(400, "生产线不能为空");
        }
        BigDecimal planNum = request.getPlanNum();
        if (planNum == null || planNum.compareTo(BigDecimal.ZERO) <= 0) {
            throw new WomEntryBusinessException(400, "计划数量必须大于 0");
        }
        if (planNum.precision() > 18 || planNum.scale() > 6) {
            throw new WomEntryBusinessException(400, "计划数量最多 18 位且小数不超过 6 位");
        }
        LocalDateTime start = parseDateTime(request.getPlanStartDate(), "计划开始时间");
        LocalDateTime end = parseDateTime(request.getPlanEndDate(), "计划结束时间");
        if (!end.isAfter(start)) {
            throw new WomEntryBusinessException(400, "计划结束时间必须晚于开始时间");
        }
    }

    private static LocalDateTime parseDateTime(String value, String label) {
        String normalized = requiredText(value, label, 19);
        try {
            return LocalDateTime.parse(normalized, DATE_TIME_FORMAT);
        } catch (DateTimeParseException exception) {
            throw new WomEntryBusinessException(400, label + "格式应为 yyyy-MM-dd HH:mm:ss");
        }
    }

    private static String requiredText(String value, String label, int maxLength) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) {
            throw new WomEntryBusinessException(400, label + "不能为空");
        }
        if (normalized.length() > maxLength) {
            throw new WomEntryBusinessException(400, label + "长度不能超过 " + maxLength);
        }
        for (int index = 0; index < normalized.length(); index += 1) {
            if (Character.isISOControl(normalized.charAt(index))) {
                throw new WomEntryBusinessException(400, label + "包含非法控制字符");
            }
        }
        return normalized;
    }

    static String requestHash(CreateInstructionRequest request) {
        String canonical = String.join("\n",
            request.getProductCode().trim(),
            request.getFormulaCode().trim(),
            String.valueOf(request.getWorkLineId()),
            request.getPlanNum().stripTrailingZeros().toPlainString(),
            request.getPlanStartDate().trim(),
            request.getPlanEndDate().trim(),
            request.getBatchCode().trim(),
            String.valueOf(Boolean.TRUE.equals(request.getNeedPack()))
        );
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(canonical.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(bytes.length * 2);
            for (byte value : bytes) {
                result.append(String.format("%02x", value & 0xff));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new WomEntryBusinessException(500, "创建参数序列化失败", exception);
        }
    }

    private static String rootMessage(Throwable throwable) {
        Throwable cursor = throwable;
        while (cursor.getCause() != null && cursor.getCause() != cursor) {
            cursor = cursor.getCause();
        }
        String message = cursor.getMessage();
        return message == null || message.trim().isEmpty()
            ? throwable.getClass().getSimpleName()
            : message;
    }
}
