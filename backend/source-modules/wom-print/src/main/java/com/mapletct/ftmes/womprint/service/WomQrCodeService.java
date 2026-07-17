package com.mapletct.ftmes.womprint.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.mapletct.ftmes.womprint.api.GenerateQrCodeRequest;
import com.mapletct.ftmes.womprint.api.PrintBackfillRequest;
import com.mapletct.ftmes.womprint.domain.GenerationResult;
import com.mapletct.ftmes.womprint.domain.PrinterConfig;
import com.mapletct.ftmes.womprint.domain.QrCodeRecord;
import com.mapletct.ftmes.womprint.domain.RequestSummary;
import com.mapletct.ftmes.womprint.domain.TaskContext;
import com.mapletct.ftmes.womprint.domain.WomPrintBusinessException;
import com.mapletct.ftmes.womprint.repository.WomPrintRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class WomQrCodeService {

    private static final int MAX_PRINT_COUNT = 10000;
    private static final int MAX_DAILY_SEQUENCE = 99999;
    private static final DateTimeFormatter CODE_DATE = DateTimeFormatter.ofPattern("yyMMdd");

    private final WomPrintRepository repository;

    public WomQrCodeService(WomPrintRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public GenerationResult generate(String rawTenantId, GenerateQrCodeRequest request) {
        validateRequest(request);
        String tenantId = normalizeTenant(rawTenantId);
        TaskContext task = requireTask(request.getTaskId());
        PrinterConfig printer = requirePrinter(request.getPrintId());
        LocalDate manufactureDate = parseDate(request.getManuDate(), "manuDate", true);
        LocalDate expiryDate = resolveExpiryDate(task, manufactureDate, request.getApproveDate());
        String requestId = normalizeRequestId(request.getRequestId());
        String requestHash = requestHash(
            tenantId,
            task.getId(),
            manufactureDate,
            expiryDate,
            request.getPrintId(),
            request.getPrintCount()
        );

        repository.lockRequest(tenantId, requestId);

        GenerationResult existing = existingResult(
            tenantId,
            requestId,
            requestHash,
            request.getPrintCount()
        );
        if (existing != null) {
            return existing;
        }

        repository.ensureDailySequence(tenantId, manufactureDate);
        int previousSequence = repository.lockDailySequence(tenantId, manufactureDate);

        // The row lock serializes concurrent requests for the same tenant and date.
        existing = existingResult(tenantId, requestId, requestHash, request.getPrintCount());
        if (existing != null) {
            return existing;
        }

        int lastSequence = previousSequence + request.getPrintCount();
        if (lastSequence > MAX_DAILY_SEQUENCE) {
            throw new WomPrintBusinessException(
                409,
                "该生产日期已生成 " + previousSequence + " 条二维码，剩余流水号不足"
            );
        }

        List<QrCodeRecord> records = new ArrayList<QrCodeRecord>(request.getPrintCount());
        List<String> details = new ArrayList<String>(request.getPrintCount());
        String codePrefix = manufactureDate.format(CODE_DATE);
        for (int offset = 1; offset <= request.getPrintCount(); offset += 1) {
            int sequence = previousSequence + offset;
            String qrCode = codePrefix + String.format(Locale.ROOT, "%05d", sequence);
            String detail = buildLegacyDetail(task, qrCode, manufactureDate, expiryDate);
            records.add(new QrCodeRecord(
                tenantId,
                requestId,
                requestHash,
                sequence,
                task,
                printer,
                manufactureDate,
                expiryDate,
                qrCode,
                detail,
                detail
            ));
            details.add(detail);
        }

        repository.updateDailySequence(tenantId, manufactureDate, lastSequence);
        repository.insertQrCodes(records);
        return new GenerationResult(requestId, details, false);
    }

    @Transactional
    public Map<String, Object> backfill(String rawTenantId, List<PrintBackfillRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new WomPrintBusinessException(400, "打印结果不能为空");
        }
        if (requests.size() > MAX_PRINT_COUNT) {
            throw new WomPrintBusinessException(400, "单次打印结果不能超过 10000 条");
        }
        String tenantId = normalizeTenant(rawTenantId);
        int updated = 0;
        int missing = 0;
        for (PrintBackfillRequest request : requests) {
            if (request == null || trim(request.getDetail()).isEmpty()) {
                throw new WomPrintBusinessException(400, "detail 不能为空");
            }
            if (request.getIsPrint() == null || (request.getIsPrint() != 0 && request.getIsPrint() != 1)) {
                throw new WomPrintBusinessException(400, "isPrint 只能为 0 或 1");
            }
            int count = repository.backfillPrintState(
                tenantId,
                request.getDetail().trim(),
                request.getIsPrint() == 1
            );
            updated += count;
            if (count == 0) {
                missing += 1;
            }
        }
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("requested", requests.size());
        result.put("updated", updated);
        result.put("missing", missing);
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> taskContext(long taskId) {
        TaskContext task = requireTask(taskId);
        LocalDate manufactureDate = task.getPlanStartTime() == null
            ? LocalDate.now()
            : task.getPlanStartTime().toLocalDate();
        LocalDate expiryDate = resolveConfiguredExpiry(task, manufactureDate);
        Map<String, Object> result = task.toMap();
        result.put("manufactureDate", manufactureDate.toString());
        result.put("expiryDate", expiryDate.toString());
        result.put("validityManaged", Boolean.TRUE.equals(task.getValidityManaged()));
        result.put("validPeriod", task.getValidPeriod());
        result.put("validUnit", task.getValidUnit());
        return result;
    }

    @Transactional(readOnly = true)
    public String calculateTermOfValidity(long taskId, String manufactureDateValue) {
        TaskContext task = requireTask(taskId);
        LocalDate manufactureDate;
        if (trim(manufactureDateValue).isEmpty()) {
            manufactureDate = task.getPlanStartTime() == null
                ? LocalDate.now()
                : task.getPlanStartTime().toLocalDate();
        } else {
            manufactureDate = parseDate(manufactureDateValue, "manuDate", true);
        }
        return resolveConfiguredExpiry(task, manufactureDate).toString();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> printers() {
        List<PrinterConfig> printers = repository.listPrinters();
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>(printers.size());
        for (PrinterConfig printer : printers) {
            result.add(printer.toMap());
        }
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> printerForLine(String lineId) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("lineId", lineId);
        result.put("packId", null);
        result.put("lineMappingConfigured", false);
        return result;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> records(String rawTenantId, long taskId, int requestedLimit) {
        int limit = Math.max(1, Math.min(500, requestedLimit));
        return repository.listTaskRecords(normalizeTenant(rawTenantId), taskId, limit);
    }

    @Transactional(readOnly = true)
    public byte[] renderQrCode(String rawTenantId, String rawQrCode, int requestedSize) {
        String qrCode = trim(rawQrCode);
        if (!qrCode.matches("[0-9]{11}")) {
            throw new WomPrintBusinessException(400, "二维码编号格式错误");
        }
        String content = repository.findQrContent(normalizeTenant(rawTenantId), qrCode);
        if (content == null) {
            throw new WomPrintBusinessException(404, "二维码不存在");
        }
        int size = Math.max(128, Math.min(1024, requestedSize));
        Map<EncodeHintType, Object> hints = new EnumMap<EncodeHintType, Object>(EncodeHintType.class);
        hints.put(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.MARGIN, 1);
        try {
            BitMatrix matrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", output);
            return output.toByteArray();
        } catch (WriterException exception) {
            throw new IllegalStateException("二维码编码失败", exception);
        } catch (IOException exception) {
            throw new IllegalStateException("二维码图片输出失败", exception);
        }
    }

    private GenerationResult existingResult(
            String tenantId,
            String requestId,
            String requestHash,
            int expectedCount) {
        RequestSummary summary = repository.findRequestSummary(tenantId, requestId);
        if (summary == null) {
            return null;
        }
        if (!requestHash.equals(summary.getRequestHash())) {
            throw new WomPrintBusinessException(409, "requestId 已被不同参数使用");
        }
        if (summary.getRecordCount() != expectedCount) {
            throw new WomPrintBusinessException(409, "requestId 已存在，但打印数量不一致");
        }
        List<String> details = repository.findRequestDetails(tenantId, requestId);
        if (details.size() != expectedCount) {
            throw new WomPrintBusinessException(409, "requestId 对应的二维码数据不完整");
        }
        return new GenerationResult(requestId, details, true);
    }

    private TaskContext requireTask(long taskId) {
        TaskContext task = repository.findTask(taskId);
        if (task == null) {
            throw new WomPrintBusinessException(404, "制造指令不存在：" + taskId);
        }
        if (trim(task.getProduceBatchNum()).isEmpty()) {
            throw new WomPrintBusinessException(409, "制造指令未设置生产批号");
        }
        if (trim(task.getProductCode()).isEmpty()) {
            throw new WomPrintBusinessException(409, "制造指令未关联有效物料编码");
        }
        return task;
    }

    private PrinterConfig requirePrinter(Long printerId) {
        if (printerId == null) {
            return null;
        }
        if (printerId <= 0L) {
            throw new WomPrintBusinessException(400, "printId 必须为正整数");
        }
        PrinterConfig printer = repository.findPrinter(printerId);
        if (printer == null) {
            throw new WomPrintBusinessException(404, "打印机配置不存在：" + printerId);
        }
        if (trim(printer.getHost()).isEmpty()) {
            throw new WomPrintBusinessException(409, "打印机未配置客户端地址");
        }
        if (printer.getPort() == null || printer.getPort() < 1 || printer.getPort() > 65535) {
            throw new WomPrintBusinessException(409, "打印机端口配置无效");
        }
        return printer;
    }

    private static void validateRequest(GenerateQrCodeRequest request) {
        if (request == null) {
            throw new WomPrintBusinessException(400, "请求内容不能为空");
        }
        if (request.getTaskId() == null || request.getTaskId() <= 0L) {
            throw new WomPrintBusinessException(400, "taskId 必须为正整数");
        }
        if (request.getPrintCount() == null
                || request.getPrintCount() <= 0
                || request.getPrintCount() > MAX_PRINT_COUNT) {
            throw new WomPrintBusinessException(400, "打印数量必须在 1 到 10000 之间");
        }
    }

    private static LocalDate resolveExpiryDate(
            TaskContext task,
            LocalDate manufactureDate,
            String approveDateValue) {
        LocalDate expiryDate = trim(approveDateValue).isEmpty()
            ? resolveConfiguredExpiry(task, manufactureDate)
            : parseDate(approveDateValue, "approveDate", true);
        if (expiryDate.isBefore(manufactureDate)) {
            throw new WomPrintBusinessException(400, "有效期不能早于生产日期");
        }
        return expiryDate;
    }

    private static LocalDate resolveConfiguredExpiry(TaskContext task, LocalDate manufactureDate) {
        if (!Boolean.TRUE.equals(task.getValidityManaged())
                || task.getValidPeriod() == null
                || task.getValidPeriod() <= 0) {
            return manufactureDate;
        }
        long period = task.getValidPeriod();
        String unit = trim(task.getValidUnit()).toLowerCase(Locale.ROOT);
        if (unit.contains("year") || unit.contains("年")) {
            return manufactureDate.plusYears(period);
        }
        if (unit.contains("month") || unit.contains("月")) {
            return manufactureDate.plusMonths(period);
        }
        return manufactureDate.plusDays(period);
    }

    private static String buildLegacyDetail(
            TaskContext task,
            String qrCode,
            LocalDate manufactureDate,
            LocalDate expiryDate) {
        return cleanCsv(task.getProduceBatchNum()) + ","
            + qrCode + ","
            + cleanCsv(task.getProductCode()) + ","
            + manufactureDate + ","
            + expiryDate + ",G0001";
    }

    private static LocalDate parseDate(String raw, String fieldName, boolean required) {
        String value = trim(raw);
        if (value.isEmpty()) {
            if (required) {
                throw new WomPrintBusinessException(400, fieldName + " 不能为空");
            }
            return null;
        }
        try {
            if (value.matches("[0-9]{11,}")) {
                return Instant.ofEpochMilli(Long.parseLong(value))
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
            }
            String normalized = value.replace('/', '-');
            return LocalDate.parse(normalized.length() >= 10 ? normalized.substring(0, 10) : normalized);
        } catch (NumberFormatException exception) {
            throw new WomPrintBusinessException(400, fieldName + " 格式必须为 yyyy-MM-dd 或毫秒时间戳");
        } catch (DateTimeParseException exception) {
            throw new WomPrintBusinessException(400, fieldName + " 格式必须为 yyyy-MM-dd 或毫秒时间戳");
        }
    }

    private static String normalizeRequestId(String raw) {
        String value = trim(raw);
        if (value.isEmpty()) {
            return UUID.randomUUID().toString();
        }
        if (value.length() > 80 || !value.matches("[A-Za-z0-9._:-]+")) {
            throw new WomPrintBusinessException(400, "requestId 含有不支持的字符或长度超过 80");
        }
        return value;
    }

    private static String requestHash(
            String tenantId,
            long taskId,
            LocalDate manufactureDate,
            LocalDate expiryDate,
            Long printerId,
            int printCount) {
        return sha256(
            tenantId + "|" + taskId + "|" + manufactureDate + "|" + expiryDate + "|"
                + (printerId == null ? "" : printerId) + "|" + printCount
        );
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte item : digest) {
                result.append(String.format(Locale.ROOT, "%02x", item & 0xff));
            }
            return result.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("SHA-256 不可用", exception);
        }
    }

    private static String cleanCsv(String value) {
        return trim(value)
            .replace(',', '_')
            .replace('"', '_')
            .replace('\n', '_')
            .replace('\r', '_');
    }

    private static String normalizeTenant(String tenantId) {
        String normalized = trim(tenantId);
        if (normalized.isEmpty()) {
            return "default";
        }
        return normalized.length() <= 64 ? normalized : normalized.substring(0, 64);
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
