package com.mapletct.ftmes.materialwms.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mapletct.ftmes.materialwms.api.StockDocumentLineRequest;
import com.mapletct.ftmes.materialwms.api.StockDocumentRequest;
import com.mapletct.ftmes.materialwms.domain.DocumentType;
import com.mapletct.ftmes.materialwms.domain.MaterialWmsBusinessException;
import com.mapletct.ftmes.materialwms.domain.QualityStatus;
import com.mapletct.ftmes.materialwms.domain.QualityUpdateResult;
import com.mapletct.ftmes.materialwms.domain.StockDocumentResult;
import com.mapletct.ftmes.materialwms.repository.MaterialWmsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MaterialInventoryService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final MaterialWmsRepository repository;
    private final ObjectMapper objectMapper;

    public MaterialInventoryService(MaterialWmsRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public StockDocumentResult createCompletionInbound(String tenantId, StockDocumentRequest request) {
        return createDocument(tenantId, request, DocumentType.COMPLETION_INBOUND);
    }

    @Transactional
    public StockDocumentResult createFromLegacyOutEndpoint(String tenantId, StockDocumentRequest request) {
        if ("produceIn".equalsIgnoreCase(trim(request.getComeType()))) {
            return createDocument(tenantId, request, DocumentType.COMPLETION_INBOUND);
        }
        return createDocument(tenantId, request, DocumentType.PRODUCTION_ISSUE);
    }

    @Transactional
    public QualityUpdateResult updateQualityResult(String tenantId, String sourceLineId, String legacyResult) {
        String normalizedSourceLineId = required(sourceLineId, "srcId");
        QualityStatus requestedStatus = QualityStatus.fromLegacy(legacyResult);
        if (requestedStatus == QualityStatus.PENDING) {
            throw new MaterialWmsBusinessException(400, "checkResult 必须是合格或不合格");
        }

        repository.ensureQualityResult(tenantId, normalizedSourceLineId);
        Map<String, Object> qualityRow = repository.lockQualityResult(tenantId, normalizedSourceLineId);
        QualityStatus previousStatus = QualityStatus.valueOf(String.valueOf(qualityRow.get("quality_status")));
        long revision = number(qualityRow.get("revision")).longValue();
        if (previousStatus != requestedStatus) {
            revision = repository.updateQualityResult(
                tenantId, normalizedSourceLineId, requestedStatus, revision);
        }

        int appliedLines = 0;
        List<Map<String, Object>> lines = repository.lockInboundLinesBySource(tenantId, normalizedSourceLineId);
        for (Map<String, Object> line : lines) {
            QualityStatus lineStatus = QualityStatus.valueOf(String.valueOf(line.get("quality_status")));
            if (lineStatus == requestedStatus) {
                continue;
            }
            BigDecimal quantity = decimal(line.get("quantity"));
            BigDecimal availableDelta = bucketAvailable(requestedStatus).subtract(bucketAvailable(lineStatus))
                .multiply(quantity);
            BigDecimal holdDelta = bucketHold(requestedStatus).subtract(bucketHold(lineStatus))
                .multiply(quantity);
            Map<String, Object> balance = repository.adjustStock(
                tenantId,
                string(line.get("warehouse_code")),
                string(line.get("location_code")),
                string(line.get("material_code")),
                string(line.get("batch_no")),
                string(line.get("production_batch_no")),
                ZERO,
                availableDelta,
                holdDelta
            );
            long lineId = number(line.get("id")).longValue();
            long documentId = number(line.get("document_id")).longValue();
            repository.updateLineQuality(lineId, requestedStatus);
            repository.insertTransaction(
                tenantId,
                "QUALITY:" + normalizedSourceLineId + ":" + revision,
                requestedStatus == QualityStatus.QUALIFIED ? "QUALITY_RELEASE" : "QUALITY_HOLD",
                documentId,
                lineId,
                string(line.get("source_document_id")),
                normalizedSourceLineId,
                string(line.get("warehouse_code")),
                string(line.get("location_code")),
                string(line.get("material_code")),
                string(line.get("batch_no")),
                string(line.get("production_batch_no")),
                ZERO,
                availableDelta,
                holdDelta,
                balance
            );
            repository.refreshDocumentQuality(documentId);
            appliedLines++;
        }
        return new QualityUpdateResult(normalizedSourceLineId, requestedStatus, appliedLines, revision);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> listCompletionInbounds(
            String tenantId, String keyword, int requestedPage, int requestedSize) {
        int page = Math.max(0, requestedPage);
        int size = Math.max(1, Math.min(200, requestedSize));
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("items", repository.listCompletionInbounds(tenantId, keyword, page, size));
        result.put("total", repository.countCompletionInbounds(tenantId, keyword));
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> completionInboundDetail(String tenantId, long documentId) {
        Map<String, Object> detail = repository.completionInboundDetail(tenantId, documentId);
        if (detail.isEmpty()) {
            throw new MaterialWmsBusinessException(404, "完工入库单不存在: " + documentId);
        }
        return detail;
    }

    private StockDocumentResult createDocument(
            String tenantId, StockDocumentRequest request, DocumentType documentType) {
        validateRequest(request, documentType);
        String sourceDocumentId = request.getSourceDocumentId().trim();
        String warehouseCode = request.getWareCode().trim();
        Map<String, Object> existing = repository.findDocumentBySource(
            tenantId, documentType, sourceDocumentId, warehouseCode);

        LocalDate storageDate = parseDate(request.getStorageDate(), "storageDate", true);
        String documentNo = documentType.getNumberPrefix() + "-"
            + documentNumberPart(sourceDocumentId + "-" + warehouseCode);
        if (existing == null) {
            repository.insertDocumentIfAbsent(
                tenantId, documentType, documentNo, request, storageDate, json(request));
        }
        Map<String, Object> document = repository.findDocumentBySource(
            tenantId, documentType, sourceDocumentId, warehouseCode);
        if (document == null) {
            throw new MaterialWmsBusinessException(500, "库存单据创建后无法读取");
        }

        long documentId = number(document.get("id")).longValue();
        repository.lockDocument(documentId);
        List<StockDocumentLineRequest> lines = request.getDetailList();
        boolean createdAnyLine = false;
        for (int index = 0; index < lines.size(); index++) {
            StockDocumentLineRequest line = lines.get(index);
            String sourceLineId = trim(line.getSrcPartId());
            if (sourceLineId.isEmpty()) {
                sourceLineId = sourceDocumentId + ":" + (index + 1);
            }
            Map<String, Object> existingLine = repository.findLineBySource(
                tenantId, documentType, sourceLineId);
            if (existingLine != null) {
                verifyIdempotentLine(existingLine, request, line);
                continue;
            }
            QualityStatus qualityStatus = documentType == DocumentType.PRODUCTION_ISSUE
                ? QualityStatus.QUALIFIED
                : resolveInitialQuality(tenantId, sourceLineId, line.getCheckResult());
            LocalDate productionDate = parseDate(line.getProductionDate(), "productionDate", false);
            boolean lineInserted = repository.insertLineIfAbsent(
                documentId,
                tenantId,
                documentType,
                repository.nextLineNo(documentId),
                sourceLineId,
                warehouseCode,
                line,
                qualityStatus,
                productionDate
            );
            Map<String, Object> persistedLine = repository.findLineBySource(
                tenantId, documentType, sourceLineId);
            if (!lineInserted) {
                verifyIdempotentLine(persistedLine, request, line);
                continue;
            }
            createdAnyLine = true;
            long lineId = number(persistedLine.get("id")).longValue();

            repository.ensureStock(
                tenantId,
                warehouseCode,
                line.getPlaceSetCode(),
                line.getGoodCode().trim(),
                line.getBatchText(),
                line.getProductionBatchNo()
            );
            BigDecimal direction = documentType == DocumentType.COMPLETION_INBOUND
                ? BigDecimal.ONE : BigDecimal.ONE.negate();
            BigDecimal onHandDelta = line.getQuantity().multiply(direction);
            BigDecimal availableDelta;
            BigDecimal holdDelta;
            if (documentType == DocumentType.PRODUCTION_ISSUE) {
                availableDelta = line.getQuantity().negate();
                holdDelta = ZERO;
            } else {
                availableDelta = qualityStatus == QualityStatus.QUALIFIED ? line.getQuantity() : ZERO;
                holdDelta = qualityStatus == QualityStatus.QUALIFIED ? ZERO : line.getQuantity();
            }
            Map<String, Object> balance = repository.adjustStock(
                tenantId,
                warehouseCode,
                line.getPlaceSetCode(),
                line.getGoodCode().trim(),
                line.getBatchText(),
                line.getProductionBatchNo(),
                onHandDelta,
                availableDelta,
                holdDelta
            );
            repository.insertTransaction(
                tenantId,
                documentType.name() + ":" + sourceLineId,
                documentType.name(),
                documentId,
                lineId,
                sourceDocumentId,
                sourceLineId,
                warehouseCode,
                line.getPlaceSetCode(),
                line.getGoodCode().trim(),
                line.getBatchText(),
                line.getProductionBatchNo(),
                onHandDelta,
                availableDelta,
                holdDelta,
                balance
            );
        }
        repository.refreshDocumentQuality(documentId);
        return new StockDocumentResult(
            documentId, string(document.get("document_no")), documentType, !createdAnyLine);
    }

    private void validateRequest(StockDocumentRequest request, DocumentType documentType) {
        if (request == null) {
            throw new MaterialWmsBusinessException(400, "请求体不能为空");
        }
        required(request.getSourceDocumentId(), "srcID/srcId");
        required(request.getCompanyCode(), "companyCode");
        required(request.getWareCode(), "wareCode");
        if (request.getDetailList() == null || request.getDetailList().isEmpty()) {
            throw new MaterialWmsBusinessException(400, "detailList 不能为空");
        }
        if (!trim(request.getRedBlue()).isEmpty() && !"blue".equalsIgnoreCase(trim(request.getRedBlue()))) {
            throw new MaterialWmsBusinessException(400, "首期不支持红字冲销，请提交 blue 单据");
        }
        for (int index = 0; index < request.getDetailList().size(); index++) {
            StockDocumentLineRequest line = request.getDetailList().get(index);
            if (line == null) {
                throw new MaterialWmsBusinessException(400, "detailList[" + index + "] 不能为空");
            }
            required(line.getGoodCode(), "detailList[" + index + "].goodCode");
            if (line.getQuantity() == null || line.getQuantity().compareTo(ZERO) <= 0) {
                throw new MaterialWmsBusinessException(400,
                    "detailList[" + index + "].quantity 必须大于 0");
            }
        }
        if (documentType == DocumentType.COMPLETION_INBOUND
                && "produceOut".equalsIgnoreCase(trim(request.getComeType()))) {
            throw new MaterialWmsBusinessException(400, "完工入库接口不能提交 produceOut 类型");
        }
    }

    private QualityStatus resolveInitialQuality(String tenantId, String sourceLineId, String requestResult) {
        QualityStatus callbackStatus = repository.findQualityStatus(tenantId, sourceLineId);
        if (callbackStatus != null && callbackStatus != QualityStatus.PENDING) {
            return callbackStatus;
        }
        return QualityStatus.fromLegacy(requestResult);
    }

    private void verifyIdempotentLine(
            Map<String, Object> existingLine,
            StockDocumentRequest request,
            StockDocumentLineRequest line) {
        if (existingLine == null
                || !string(existingLine.get("warehouse_code")).equals(request.getWareCode().trim())
                || !string(existingLine.get("location_code")).equals(MaterialWmsRepository.normalizeDimension(line.getPlaceSetCode()))
                || !string(existingLine.get("material_code")).equals(line.getGoodCode().trim())
                || !string(existingLine.get("batch_no")).equals(MaterialWmsRepository.normalizeDimension(line.getBatchText()))
                || !string(existingLine.get("production_batch_no")).equals(MaterialWmsRepository.normalizeDimension(line.getProductionBatchNo()))
                || decimal(existingLine.get("quantity")).compareTo(line.getQuantity()) != 0) {
            throw new MaterialWmsBusinessException(409, "来源明细幂等键重复，但库存维度或数量不一致");
        }
    }

    private String json(StockDocumentRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException exception) {
            throw new MaterialWmsBusinessException(400, "请求体无法序列化");
        }
    }

    private static LocalDate parseDate(String value, String field, boolean required) {
        String normalized = trim(value);
        if (normalized.isEmpty()) {
            if (required) {
                throw new MaterialWmsBusinessException(400, field + " 不能为空");
            }
            return null;
        }
        try {
            return LocalDate.parse(normalized);
        } catch (DateTimeParseException exception) {
            throw new MaterialWmsBusinessException(400, field + " 必须是 yyyy-MM-dd");
        }
    }

    private static String documentNumberPart(String sourceDocumentId) {
        String normalized = sourceDocumentId.replaceAll("[^A-Za-z0-9_-]", "-");
        if (normalized.length() > 80) {
            normalized = normalized.substring(normalized.length() - 80);
        }
        return normalized;
    }

    private static BigDecimal bucketAvailable(QualityStatus status) {
        return status == QualityStatus.QUALIFIED ? BigDecimal.ONE : ZERO;
    }

    private static BigDecimal bucketHold(QualityStatus status) {
        return status == QualityStatus.QUALIFIED ? ZERO : BigDecimal.ONE;
    }

    private static String required(String value, String field) {
        String normalized = trim(value);
        if (normalized.isEmpty()) {
            throw new MaterialWmsBusinessException(400, field + " 不能为空");
        }
        return normalized;
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
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
        return new BigDecimal(String.valueOf(value));
    }

    private static String string(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
