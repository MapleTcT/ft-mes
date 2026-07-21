package com.mapletct.ftmes.materialwms.api;

import com.mapletct.ftmes.materialwms.service.MaterialInventoryService;
import com.mapletct.ftmes.materialwms.support.BpiIntegrationKeyVerifier;
import com.mapletct.ftmes.materialwms.support.TenantResolver;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
public class MaterialWmsController {

    private final MaterialInventoryService inventoryService;
    private final TenantResolver tenantResolver;
    private final BpiIntegrationKeyVerifier bpiIntegrationKeyVerifier;

    public MaterialWmsController(
            MaterialInventoryService inventoryService,
            TenantResolver tenantResolver,
            BpiIntegrationKeyVerifier bpiIntegrationKeyVerifier) {
        this.inventoryService = inventoryService;
        this.tenantResolver = tenantResolver;
        this.bpiIntegrationKeyVerifier = bpiIntegrationKeyVerifier;
    }

    @PostMapping({
        "/public/material/produceInSingles/produceInSingl/generateProductInSingle",
        "/material/produceInSingles/produceInSingl/generateProductInSingle"
    })
    public LegacyResult<Map<String, Object>> createCompletionInbound(
            HttpServletRequest servletRequest,
            @RequestHeader(value = "X-BPI-WMS-Key", required = false) String bpiApiKey,
            @RequestBody StockDocumentRequest request) {
        bpiIntegrationKeyVerifier.verifyIfBpi(request.getSourceSystem(), bpiApiKey);
        return LegacyResult.success(inventoryService.createCompletionInbound(
            tenantResolver.resolve(servletRequest, request.getCompanyCode()), request).toMap());
    }

    @PostMapping("/material/wms/completion-inbound-reversals")
    public LegacyResult<Map<String, Object>> createCompletionInboundReversal(
            HttpServletRequest servletRequest,
            @RequestHeader(value = "X-BPI-WMS-Key", required = false) String bpiApiKey,
            @RequestBody StockDocumentRequest request) {
        bpiIntegrationKeyVerifier.verifyIfBpi(request.getSourceSystem(), bpiApiKey);
        return LegacyResult.success(inventoryService.createCompletionInboundReversal(
            tenantResolver.resolve(servletRequest, request.getCompanyCode()), request).toMap());
    }

    @PostMapping({
        "/public/material/produceOutSingle/produceOutSing/generateProduceOutSing",
        "/material/produceOutSingle/produceOutSing/generateProduceOutSing"
    })
    public LegacyResult<Map<String, Object>> createLegacyProductionMovement(
            HttpServletRequest servletRequest,
            @RequestBody StockDocumentRequest request) {
        return LegacyResult.success(inventoryService.createFromLegacyOutEndpoint(
            tenantResolver.resolve(servletRequest, request.getCompanyCode()), request).toMap());
    }

    @PostMapping({
        "/material/foreign/foreign/checkProdResult",
        "/public/material/foreign/foreign/checkProdResult"
    })
    public LegacyResult<Map<String, Object>> updateQualityResult(
            HttpServletRequest servletRequest,
            @RequestParam("srcId") String sourceLineId,
            @RequestParam("checkResult") String checkResult) {
        return LegacyResult.success(inventoryService.updateQualityResult(
            tenantResolver.resolve(servletRequest, null), sourceLineId, checkResult).toMap());
    }

    @PostMapping({
        "/material/wms/quality-allocations",
        "/public/material/wms/quality-allocations"
    })
    public LegacyResult<Map<String, Object>> applyQualityAllocation(
            HttpServletRequest servletRequest,
            @RequestBody QualityAllocationRequest request) {
        return LegacyResult.success(inventoryService.applyQualityAllocation(
            tenantResolver.resolve(servletRequest, null), request).toMap());
    }

    @GetMapping(value = "/material/wms", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<Resource> operationalPage() {
        return ResponseEntity.ok()
            .contentType(new MediaType("text", "html", StandardCharsets.UTF_8))
            .body(new ClassPathResource("static/material-wms.html"));
    }

    @GetMapping("/material/wms/completion-inbounds")
    public LegacyResult<Map<String, Object>> listCompletionInbounds(
            HttpServletRequest servletRequest,
            @RequestParam(value = "keyword", defaultValue = "") String keyword,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "50") int size) {
        return LegacyResult.success(inventoryService.listCompletionInbounds(
            tenantResolver.resolve(servletRequest, null), keyword, page, size));
    }

    @GetMapping("/material/wms/completion-inbounds/{documentId}")
    public LegacyResult<Map<String, Object>> completionInboundDetail(
            HttpServletRequest servletRequest,
            @PathVariable("documentId") long documentId) {
        return LegacyResult.success(inventoryService.completionInboundDetail(
            tenantResolver.resolve(servletRequest, null), documentId));
    }

    @GetMapping("/material/wms/completion-inbounds/by-idempotency")
    public LegacyResult<Map<String, Object>> completionInboundByIdempotency(
            HttpServletRequest servletRequest,
            @RequestHeader(value = "X-BPI-WMS-Key", required = false) String bpiApiKey,
            @RequestParam("sourceSystem") String sourceSystem,
            @RequestParam("idempotencyKey") String idempotencyKey) {
        bpiIntegrationKeyVerifier.verifyIfBpi(sourceSystem, bpiApiKey);
        return LegacyResult.success(inventoryService.completionInboundByIdempotency(
            tenantResolver.resolve(servletRequest, null), sourceSystem, idempotencyKey));
    }

    @GetMapping("/material/wms/completion-inbound-reversals/by-idempotency")
    public LegacyResult<Map<String, Object>> completionInboundReversalByIdempotency(
            HttpServletRequest servletRequest,
            @RequestHeader(value = "X-BPI-WMS-Key", required = false) String bpiApiKey,
            @RequestParam("sourceSystem") String sourceSystem,
            @RequestParam("idempotencyKey") String idempotencyKey) {
        bpiIntegrationKeyVerifier.verifyIfBpi(sourceSystem, bpiApiKey);
        return LegacyResult.success(inventoryService.completionInboundReversalByIdempotency(
            tenantResolver.resolve(servletRequest, null), sourceSystem, idempotencyKey));
    }
}
