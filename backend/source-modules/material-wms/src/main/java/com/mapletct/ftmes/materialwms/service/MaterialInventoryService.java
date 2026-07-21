package com.mapletct.ftmes.materialwms.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mapletct.ftmes.materialwms.api.QualityAllocationRequest;
import com.mapletct.ftmes.materialwms.api.StockDocumentLineRequest;
import com.mapletct.ftmes.materialwms.api.StockDocumentRequest;
import com.mapletct.ftmes.materialwms.domain.DocumentType;
import com.mapletct.ftmes.materialwms.domain.MaterialWmsBusinessException;
import com.mapletct.ftmes.materialwms.domain.QualityAllocationAction;
import com.mapletct.ftmes.materialwms.domain.QualityAllocationResult;
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
import java.util.regex.Pattern;

@Service
public class MaterialInventoryService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final String DEFAULT_SOURCE_SYSTEM = "WOM";
    private static final Pattern SOURCE_SYSTEM_PATTERN = Pattern.compile("[A-Z][A-Z0-9_-]{0,31}");

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
    public StockDocumentResult createCompletionInboundReversal(
            String tenantId, StockDocumentRequest request) {
        validateRequest(request, DocumentType.COMPLETION_INBOUND_REVERSAL);
        String sourceSystem = sourceSystem(request.getSourceSystem());
        if (!"BPI".equals(sourceSystem)) {
            throw new MaterialWmsBusinessException(400, "完工入库冲销接口只接受 BPI 来源");
        }
        String idempotencyKey = requiredWithMaximum(
            request.getIdempotencyKey(), "idempotencyKey", 256);
        String originalDocumentNo = requiredWithMaximum(
            request.getOriginalDocumentNo(), "originalDocumentNo", 96);
        Map<String, Object> original = repository.lockCompletionInboundByDocumentNo(
            tenantId, sourceSystem, originalDocumentNo);
        if (original == null) {
            throw new MaterialWmsBusinessException(404, "原完工入库单不存在: " + originalDocumentNo);
        }
        long originalDocumentId = number(original.get("id")).longValue();
        Map<String, Object> sameKey = repository.findDocumentByIdempotency(
            tenantId, DocumentType.COMPLETION_INBOUND_REVERSAL, sourceSystem, idempotencyKey);
        if (sameKey != null) {
            Object linkedOriginal = sameKey.get("reversal_of_document_id");
            if (linkedOriginal == null
                    || number(linkedOriginal).longValue() != originalDocumentId) {
                throw new MaterialWmsBusinessException(409, "冲销幂等键已关联其他原入库单");
            }
        }
        Map<String, Object> previousReversal = repository.findReversalByOriginal(
            tenantId, originalDocumentId);
        if (previousReversal != null) {
            if (!idempotencyKey.equals(string(previousReversal.get("idempotency_key")))) {
                throw new MaterialWmsBusinessException(409, "原完工入库单已经存在其他红字冲销单");
            }
            return createDocument(
                tenantId, request, DocumentType.COMPLETION_INBOUND_REVERSAL);
        }
        if (!"POSTED".equals(string(original.get("status")))) {
            throw new MaterialWmsBusinessException(409, "原完工入库单不是可冲销的 POSTED 状态");
        }
        verifyReversalFacts(original, repository.findDocumentLines(originalDocumentId), request);

        StockDocumentResult result = createDocument(
            tenantId, request, DocumentType.COMPLETION_INBOUND_REVERSAL);
        repository.linkReversalToOriginal(tenantId, result.getDocumentId(), originalDocumentId);
        return result;
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
        QualityStatus appliedStatus = requestedStatus;
        List<Map<String, Object>> lines = repository.lockInboundLinesBySource(tenantId, normalizedSourceLineId);
        for (Map<String, Object> line : lines) {
            QualityStatus lineStatus = QualityStatus.valueOf(String.valueOf(line.get("quality_status")));
            BigDecimal quantity = decimal(line.get("quantity"));
            BigDecimal goodQuantity = decimalOrDefault(line.get("good_quantity"), quantity);
            BigDecimal badQuantity = decimalOrDefault(line.get("bad_quantity"), ZERO);
            QualityStatus targetStatus = requestedStatus == QualityStatus.QUALIFIED
                && badQuantity.compareTo(ZERO) > 0
                ? QualityStatus.PARTIAL : requestedStatus;
            if (lineStatus == targetStatus) {
                appliedStatus = targetStatus;
                continue;
            }
            BigDecimal previousAvailable = availableQuantity(lineStatus, quantity, goodQuantity);
            BigDecimal targetAvailable = availableQuantity(targetStatus, quantity, goodQuantity);
            BigDecimal availableDelta = targetAvailable.subtract(previousAvailable);
            BigDecimal holdDelta = availableDelta.negate();
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
            repository.updateLineQuality(lineId, targetStatus);
            repository.insertTransaction(
                tenantId,
                "QUALITY:" + normalizedSourceLineId + ":" + revision,
                requestedStatus == QualityStatus.QUALIFIED ? "QUALITY_RELEASE" : "QUALITY_HOLD",
                DEFAULT_SOURCE_SYSTEM,
                documentId,
                lineId,
                string(line.get("source_document_id")),
                normalizedSourceLineId,
                string(line.get("warehouse_code")),
                string(line.get("location_code")),
                string(line.get("material_code")),
                string(line.get("batch_no")),
                string(line.get("production_batch_no")),
                string(line.get("unit_code")),
                ZERO,
                availableDelta,
                holdDelta,
                balance
            );
            repository.refreshDocumentQuality(documentId);
            appliedLines++;
            appliedStatus = targetStatus;
        }
        return new QualityUpdateResult(normalizedSourceLineId, appliedStatus, appliedLines, revision);
    }

    @Transactional
    public QualityAllocationResult applyQualityAllocation(
            String tenantId, QualityAllocationRequest request) {
        QualityAllocationAction action = validateAllocationRequest(request);
        String sourceLineId = request.getSourceLineId().trim();
        String eventKey = action.name() + ":" + request.getRequestId().trim();
        if (repository.allocationEventExists(tenantId, eventKey)) {
            Map<String, Object> allocation = repository.lockAllocation(tenantId, sourceLineId);
            if (allocation == null) {
                throw new MaterialWmsBusinessException(409, "分配事件存在但当前分配记录不存在");
            }
            if (!sameAllocation(allocation, request)) {
                throw new MaterialWmsBusinessException(409, "requestId 已被不同分配内容使用");
            }
            return allocationResult(allocation, 0, true);
        }

        Map<String, Object> allocation = repository.lockAllocation(tenantId, sourceLineId);
        if (repository.allocationEventExists(tenantId, eventKey)) {
            if (allocation == null) {
                throw new MaterialWmsBusinessException(409, "分配事件存在但当前分配记录不存在");
            }
            if (!sameAllocation(allocation, request)) {
                throw new MaterialWmsBusinessException(409, "requestId 已被不同分配内容使用");
            }
            return allocationResult(allocation, 0, true);
        }
        if (action == QualityAllocationAction.APPLY) {
            boolean newlyCreated = false;
            if (allocation == null) {
                newlyCreated = repository.insertAllocationIfAbsent(
                    tenantId,
                    sourceLineId,
                    request.getTaskId().trim(),
                    request.getQualityReportId().trim(),
                    request.getTotalQuantity(),
                    request.getGoodQuantity(),
                    request.getBadQuantity()
                );
                allocation = repository.lockAllocation(tenantId, sourceLineId);
            }
            if (allocation == null) {
                throw new MaterialWmsBusinessException(500, "不良数量分配创建后无法读取");
            }
            String currentStatus = string(allocation.get("status"));
            boolean sameAllocation = sameAllocation(allocation, request);
            if ("ACTIVE".equals(currentStatus) && !sameAllocation) {
                throw new MaterialWmsBusinessException(409, "该产出记录已有生效的不良数量登记，请先冲销");
            }
            if ("ACTIVE".equals(currentStatus) && sameAllocation && !newlyCreated) {
                repository.insertAllocationEvent(
                    tenantId, eventKey, number(allocation.get("id")).longValue(),
                    action.name(), request.getQualityReportId().trim(), json(request));
                return allocationResult(allocation, 0, true);
            }
            if (!newlyCreated) {
                repository.updateAllocation(
                    number(allocation.get("id")).longValue(),
                    number(allocation.get("version")).longValue(),
                    request.getTaskId().trim(),
                    request.getQualityReportId().trim(),
                    request.getTotalQuantity(),
                    request.getGoodQuantity(),
                    request.getBadQuantity(),
                    "ACTIVE"
                );
            }
        } else {
            if (allocation == null || "REVERSED".equals(string(allocation.get("status")))) {
                throw new MaterialWmsBusinessException(409, "该产出记录不存在可冲销的不良数量分配");
            }
            if (!sameAllocation(allocation, request)) {
                throw new MaterialWmsBusinessException(409, "冲销数据与当前生效分配不一致");
            }
        }

        allocation = repository.lockAllocation(tenantId, sourceLineId);
        int appliedLines = applyAllocationToInboundLines(tenantId, eventKey, action, request);
        if (action == QualityAllocationAction.REVERSE) {
            repository.updateAllocation(
                number(allocation.get("id")).longValue(),
                number(allocation.get("version")).longValue(),
                string(allocation.get("task_id")),
                string(allocation.get("quality_report_id")),
                decimal(allocation.get("total_quantity")),
                decimal(allocation.get("good_quantity")),
                decimal(allocation.get("bad_quantity")),
                "REVERSED"
            );
        }
        repository.insertAllocationEvent(
            tenantId, eventKey, number(allocation.get("id")).longValue(),
            action.name(), request.getQualityReportId().trim(), json(request));
        allocation = repository.lockAllocation(tenantId, sourceLineId);
        return allocationResult(allocation, appliedLines, false);
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

    @Transactional(readOnly = true)
    public Map<String, Object> completionInboundByIdempotency(
            String tenantId, String requestedSourceSystem, String requestedIdempotencyKey) {
        String sourceSystem = sourceSystem(requestedSourceSystem);
        String idempotencyKey = requiredWithMaximum(
            requestedIdempotencyKey, "idempotencyKey", 256);
        Map<String, Object> document = repository.findDocumentByIdempotency(
            tenantId, DocumentType.COMPLETION_INBOUND, sourceSystem, idempotencyKey);
        if (document == null) {
            throw new MaterialWmsBusinessException(404,
                "完工入库单不存在: " + sourceSystem + "/" + idempotencyKey);
        }
        return repository.completionInboundDetail(
            tenantId, number(document.get("id")).longValue());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> completionInboundReversalByIdempotency(
            String tenantId, String requestedSourceSystem, String requestedIdempotencyKey) {
        String sourceSystem = sourceSystem(requestedSourceSystem);
        String idempotencyKey = requiredWithMaximum(
            requestedIdempotencyKey, "idempotencyKey", 256);
        Map<String, Object> document = repository.findDocumentByIdempotency(
            tenantId, DocumentType.COMPLETION_INBOUND_REVERSAL, sourceSystem, idempotencyKey);
        if (document == null) {
            throw new MaterialWmsBusinessException(404,
                "完工入库冲销单不存在: " + sourceSystem + "/" + idempotencyKey);
        }
        return repository.completionInboundReversalDetail(
            tenantId, number(document.get("id")).longValue());
    }

    private StockDocumentResult createDocument(
            String tenantId, StockDocumentRequest request, DocumentType documentType) {
        validateRequest(request, documentType);
        String sourceDocumentId = request.getSourceDocumentId().trim();
        String sourceSystem = sourceSystem(request.getSourceSystem());
        String idempotencyKey = optionalWithMaximum(request.getIdempotencyKey(), "idempotencyKey", 256);
        if (!DEFAULT_SOURCE_SYSTEM.equals(sourceSystem) && idempotencyKey == null) {
            throw new MaterialWmsBusinessException(400,
                "非 WOM 来源必须提交 idempotencyKey");
        }
        String warehouseCode = request.getWareCode().trim();
        Map<String, Object> sourceDocument = repository.findDocumentBySource(
            tenantId, documentType, sourceSystem, sourceDocumentId, warehouseCode);
        Map<String, Object> idempotentDocument = idempotencyKey == null ? null
            : repository.findDocumentByIdempotency(
                tenantId, documentType, sourceSystem, idempotencyKey);
        Map<String, Object> existing = idempotentDocument == null ? sourceDocument : idempotentDocument;
        if (existing != null) {
            verifyIdempotentDocument(
                existing, sourceSystem, sourceDocumentId, warehouseCode, idempotencyKey);
        }

        LocalDate storageDate = parseDate(request.getStorageDate(), "storageDate", true);
        String documentNo = documentType.getNumberPrefix() + "-"
            + documentNumberPart(sourceDocumentId + "-" + warehouseCode);
        if (existing == null) {
            repository.insertDocumentIfAbsent(
                tenantId, documentType, sourceSystem, idempotencyKey,
                documentNo, request, storageDate, json(request));
        }
        Map<String, Object> document = idempotencyKey == null
            ? repository.findDocumentBySource(
                tenantId, documentType, sourceSystem, sourceDocumentId, warehouseCode)
            : repository.findDocumentByIdempotency(
                tenantId, documentType, sourceSystem, idempotencyKey);
        if (document == null && idempotencyKey != null) {
            // A concurrent request can win on the source-document key with a different
            // idempotency key. Resolve that row so the caller gets a durable 409.
            document = repository.findDocumentBySource(
                tenantId, documentType, sourceSystem, sourceDocumentId, warehouseCode);
        }
        if (document == null) {
            throw new MaterialWmsBusinessException(500, "库存单据创建后无法读取");
        }
        verifyIdempotentDocument(
            document, sourceSystem, sourceDocumentId, warehouseCode, idempotencyKey);

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
                tenantId, documentType, sourceSystem, sourceLineId);
            if (existingLine != null) {
                verifyIdempotentLine(existingLine, request, line);
                continue;
            }
            BigDecimal goodQuantity = line.getQuantity();
            BigDecimal badQuantity = ZERO;
            Map<String, Object> allocation = documentType == DocumentType.COMPLETION_INBOUND
                    && DEFAULT_SOURCE_SYSTEM.equals(sourceSystem)
                ? repository.findActiveAllocation(tenantId, sourceLineId) : null;
            if (allocation != null) {
                verifyAllocationQuantity(allocation, line.getQuantity());
                goodQuantity = decimal(allocation.get("good_quantity"));
                badQuantity = decimal(allocation.get("bad_quantity"));
            }
            boolean outbound = documentType == DocumentType.PRODUCTION_ISSUE
                || documentType == DocumentType.COMPLETION_INBOUND_REVERSAL;
            QualityStatus qualityStatus = outbound
                ? QualityStatus.QUALIFIED
                : resolveInitialQuality(tenantId, sourceSystem, sourceLineId, line.getCheckResult());
            if (qualityStatus == QualityStatus.QUALIFIED && badQuantity.compareTo(ZERO) > 0) {
                qualityStatus = QualityStatus.PARTIAL;
            }
            LocalDate productionDate = parseDate(line.getProductionDate(), "productionDate", false);
            boolean lineInserted = repository.insertLineIfAbsent(
                documentId,
                tenantId,
                documentType,
                sourceSystem,
                repository.nextLineNo(documentId),
                sourceLineId,
                warehouseCode,
                line,
                qualityStatus,
                goodQuantity,
                badQuantity,
                productionDate
            );
            Map<String, Object> persistedLine = repository.findLineBySource(
                tenantId, documentType, sourceSystem, sourceLineId);
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
            BigDecimal direction = outbound ? BigDecimal.ONE.negate() : BigDecimal.ONE;
            BigDecimal onHandDelta = line.getQuantity().multiply(direction);
            BigDecimal availableDelta;
            BigDecimal holdDelta;
            if (outbound) {
                availableDelta = line.getQuantity().negate();
                holdDelta = ZERO;
            } else {
                availableDelta = availableQuantity(qualityStatus, line.getQuantity(), goodQuantity);
                holdDelta = line.getQuantity().subtract(availableDelta);
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
                documentType.name() + ":" + sourceSystem + ":" + sourceLineId,
                documentType.name(),
                sourceSystem,
                documentId,
                lineId,
                sourceDocumentId,
                sourceLineId,
                warehouseCode,
                line.getPlaceSetCode(),
                line.getGoodCode().trim(),
                line.getBatchText(),
                line.getProductionBatchNo(),
                line.getUnitCode(),
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
        requiredWithMaximum(request.getSourceDocumentId(), "srcID/srcId", 128);
        required(request.getCompanyCode(), "companyCode");
        required(request.getWareCode(), "wareCode");
        if (request.getDetailList() == null || request.getDetailList().isEmpty()) {
            throw new MaterialWmsBusinessException(400, "detailList 不能为空");
        }
        String redBlue = trim(request.getRedBlue());
        if (documentType == DocumentType.COMPLETION_INBOUND_REVERSAL) {
            if (!"red".equalsIgnoreCase(redBlue)) {
                throw new MaterialWmsBusinessException(400, "完工入库冲销必须提交 red 单据");
            }
            requiredWithMaximum(request.getOriginalDocumentNo(), "originalDocumentNo", 96);
            if (request.getDetailList().size() != 1) {
                throw new MaterialWmsBusinessException(400, "BPI 完工入库冲销必须包含且仅包含一条明细");
            }
        } else if (!redBlue.isEmpty() && !"blue".equalsIgnoreCase(redBlue)) {
            throw new MaterialWmsBusinessException(400, "普通库存动作必须提交 blue 单据");
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
            optionalWithMaximum(line.getUnitCode(),
                "detailList[" + index + "].unitCode", 64);
        }
        if (documentType == DocumentType.COMPLETION_INBOUND
                && "produceOut".equalsIgnoreCase(trim(request.getComeType()))) {
            throw new MaterialWmsBusinessException(400, "完工入库接口不能提交 produceOut 类型");
        }
    }

    private void verifyReversalFacts(
            Map<String, Object> original,
            List<Map<String, Object>> originalLines,
            StockDocumentRequest request) {
        if (!"QUALIFIED".equals(string(original.get("quality_status")))
                || originalLines.size() != 1) {
            throw new MaterialWmsBusinessException(409, "只有单明细且质量合格的 BPI 入库单可以冲销");
        }
        Map<String, Object> persisted = originalLines.get(0);
        StockDocumentLineRequest requested = request.getDetailList().get(0);
        if (!"QUALIFIED".equals(string(persisted.get("quality_status")))
                || !string(original.get("warehouse_code")).equals(request.getWareCode().trim())
                || !string(persisted.get("material_code")).equals(requested.getGoodCode().trim())
                || !string(persisted.get("batch_no")).equals(
                    MaterialWmsRepository.normalizeDimension(requested.getBatchText()))
                || !string(persisted.get("production_batch_no")).equals(
                    MaterialWmsRepository.normalizeDimension(requested.getProductionBatchNo()))
                || !string(persisted.get("location_code")).equals(
                    MaterialWmsRepository.normalizeDimension(requested.getPlaceSetCode()))
                || !string(persisted.get("unit_code")).equals(
                    MaterialWmsRepository.normalizeDimension(requested.getUnitCode()))
                || decimal(persisted.get("quantity")).compareTo(requested.getQuantity()) != 0) {
            throw new MaterialWmsBusinessException(409,
                "红字冲销的物料、批次、仓库、库位、数量或单位与原入库单不一致");
        }
    }

    private QualityStatus resolveInitialQuality(
            String tenantId, String sourceSystem, String sourceLineId, String requestResult) {
        QualityStatus callbackStatus = DEFAULT_SOURCE_SYSTEM.equals(sourceSystem)
            ? repository.findQualityStatus(tenantId, sourceLineId) : null;
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
                || !string(existingLine.get("unit_code")).equals(MaterialWmsRepository.normalizeDimension(line.getUnitCode()))
                || decimal(existingLine.get("quantity")).compareTo(line.getQuantity()) != 0) {
            throw new MaterialWmsBusinessException(409, "来源明细幂等键重复，但库存维度或数量不一致");
        }
    }

    private String json(Object request) {
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

    private QualityAllocationAction validateAllocationRequest(QualityAllocationRequest request) {
        if (request == null) {
            throw new MaterialWmsBusinessException(400, "请求体不能为空");
        }
        required(request.getRequestId(), "requestId");
        required(request.getQualityReportId(), "qualityReportId");
        required(request.getTaskId(), "taskId");
        required(request.getSourceLineId(), "sourceLineId");
        if (request.getTotalQuantity() == null || request.getTotalQuantity().compareTo(ZERO) <= 0) {
            throw new MaterialWmsBusinessException(400, "totalQuantity 必须大于 0");
        }
        if (request.getGoodQuantity() == null || request.getGoodQuantity().compareTo(ZERO) < 0) {
            throw new MaterialWmsBusinessException(400, "goodQuantity 不能小于 0");
        }
        if (request.getBadQuantity() == null || request.getBadQuantity().compareTo(ZERO) <= 0) {
            throw new MaterialWmsBusinessException(400, "badQuantity 必须大于 0");
        }
        if (request.getGoodQuantity().add(request.getBadQuantity())
                .compareTo(request.getTotalQuantity()) != 0) {
            throw new MaterialWmsBusinessException(400, "totalQuantity 必须等于 goodQuantity + badQuantity");
        }
        return QualityAllocationAction.from(request.getAction());
    }

    private int applyAllocationToInboundLines(
            String tenantId,
            String eventKey,
            QualityAllocationAction action,
            QualityAllocationRequest request) {
        int appliedLines = 0;
        List<Map<String, Object>> lines = repository.lockInboundLinesBySource(
            tenantId, request.getSourceLineId().trim());
        for (Map<String, Object> line : lines) {
            BigDecimal totalQuantity = decimal(line.get("quantity"));
            if (totalQuantity.compareTo(request.getTotalQuantity()) != 0) {
                throw new MaterialWmsBusinessException(409, "不良数量登记与已入库数量不一致");
            }
            BigDecimal previousGood = decimalOrDefault(line.get("good_quantity"), totalQuantity);
            BigDecimal targetGood = action == QualityAllocationAction.APPLY
                ? request.getGoodQuantity() : totalQuantity;
            BigDecimal targetBad = action == QualityAllocationAction.APPLY
                ? request.getBadQuantity() : ZERO;
            QualityStatus previousStatus = QualityStatus.valueOf(string(line.get("quality_status")));
            QualityStatus targetStatus = allocationTargetStatus(previousStatus, targetBad);
            BigDecimal previousAvailable = availableQuantity(previousStatus, totalQuantity, previousGood);
            BigDecimal targetAvailable = availableQuantity(targetStatus, totalQuantity, targetGood);
            BigDecimal availableDelta = targetAvailable.subtract(previousAvailable);
            BigDecimal holdDelta = availableDelta.negate();
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
            repository.updateLineAllocation(
                lineId, totalQuantity, targetGood, targetBad, targetStatus);
            repository.insertTransaction(
                tenantId,
                "QUALITY_ALLOCATION:" + eventKey + ":" + lineId,
                action == QualityAllocationAction.APPLY
                    ? "QUALITY_ALLOCATION_HOLD" : "QUALITY_ALLOCATION_RELEASE",
                DEFAULT_SOURCE_SYSTEM,
                documentId,
                lineId,
                string(line.get("source_document_id")),
                request.getSourceLineId().trim(),
                string(line.get("warehouse_code")),
                string(line.get("location_code")),
                string(line.get("material_code")),
                string(line.get("batch_no")),
                string(line.get("production_batch_no")),
                string(line.get("unit_code")),
                ZERO,
                availableDelta,
                holdDelta,
                balance
            );
            repository.refreshDocumentQuality(documentId);
            appliedLines++;
        }
        return appliedLines;
    }

    private static QualityStatus allocationTargetStatus(
            QualityStatus currentStatus, BigDecimal badQuantity) {
        if (currentStatus == QualityStatus.PENDING || currentStatus == QualityStatus.UNQUALIFIED) {
            return currentStatus;
        }
        return badQuantity.compareTo(ZERO) > 0 ? QualityStatus.PARTIAL : QualityStatus.QUALIFIED;
    }

    private static BigDecimal availableQuantity(
            QualityStatus status, BigDecimal totalQuantity, BigDecimal goodQuantity) {
        if (status == QualityStatus.QUALIFIED) {
            return totalQuantity;
        }
        if (status == QualityStatus.PARTIAL) {
            return goodQuantity;
        }
        return ZERO;
    }

    private static void verifyAllocationQuantity(
            Map<String, Object> allocation, BigDecimal lineQuantity) {
        if (decimal(allocation.get("total_quantity")).compareTo(lineQuantity) != 0) {
            throw new MaterialWmsBusinessException(409, "生效的不良数量登记与完工入库数量不一致");
        }
    }

    private static boolean sameAllocation(
            Map<String, Object> allocation, QualityAllocationRequest request) {
        return string(allocation.get("task_id")).equals(request.getTaskId().trim())
            && string(allocation.get("quality_report_id")).equals(request.getQualityReportId().trim())
            && decimal(allocation.get("total_quantity")).compareTo(request.getTotalQuantity()) == 0
            && decimal(allocation.get("good_quantity")).compareTo(request.getGoodQuantity()) == 0
            && decimal(allocation.get("bad_quantity")).compareTo(request.getBadQuantity()) == 0;
    }

    private static QualityAllocationResult allocationResult(
            Map<String, Object> allocation, int appliedLines, boolean idempotent) {
        return new QualityAllocationResult(
            string(allocation.get("source_line_id")),
            string(allocation.get("quality_report_id")),
            string(allocation.get("status")),
            decimal(allocation.get("total_quantity")),
            decimal(allocation.get("good_quantity")),
            decimal(allocation.get("bad_quantity")),
            appliedLines,
            idempotent
        );
    }

    private static String required(String value, String field) {
        String normalized = trim(value);
        if (normalized.isEmpty()) {
            throw new MaterialWmsBusinessException(400, field + " 不能为空");
        }
        return normalized;
    }

    private static String requiredWithMaximum(String value, String field, int maximum) {
        String normalized = required(value, field);
        if (normalized.length() > maximum) {
            throw new MaterialWmsBusinessException(400,
                field + " 长度不能超过 " + maximum);
        }
        return normalized;
    }

    private static String optionalWithMaximum(String value, String field, int maximum) {
        String normalized = trim(value);
        if (normalized.isEmpty()) {
            return null;
        }
        if (normalized.length() > maximum) {
            throw new MaterialWmsBusinessException(400,
                field + " 长度不能超过 " + maximum);
        }
        return normalized;
    }

    private static String sourceSystem(String value) {
        String normalized = trim(value).toUpperCase(java.util.Locale.ROOT);
        if (normalized.isEmpty()) {
            return DEFAULT_SOURCE_SYSTEM;
        }
        if (!SOURCE_SYSTEM_PATTERN.matcher(normalized).matches()) {
            throw new MaterialWmsBusinessException(400,
                "sourceSystem 必须是 1-32 位大写字母、数字、下划线或连字符");
        }
        return normalized;
    }

    private static void verifyIdempotentDocument(
            Map<String, Object> document,
            String sourceSystem,
            String sourceDocumentId,
            String warehouseCode,
            String idempotencyKey) {
        if (!sourceSystem.equals(string(document.get("source_system")))
                || !sourceDocumentId.equals(string(document.get("source_document_id")))
                || !warehouseCode.equals(string(document.get("warehouse_code")))
                || (idempotencyKey != null
                    && !idempotencyKey.equals(string(document.get("idempotency_key"))))) {
            throw new MaterialWmsBusinessException(409,
                "幂等键已被不同的完工入库单据使用");
        }
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

    private static BigDecimal decimalOrDefault(Object value, BigDecimal defaultValue) {
        return value == null ? defaultValue : decimal(value);
    }

    private static String string(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
