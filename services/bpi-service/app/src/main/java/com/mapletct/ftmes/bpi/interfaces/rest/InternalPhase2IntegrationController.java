package com.mapletct.ftmes.bpi.interfaces.rest;

import com.google.protobuf.InvalidProtocolBufferException;
import com.mapletct.ftmes.bpi.application.ActorContext;
import com.mapletct.ftmes.bpi.application.ActorContextFactory;
import com.mapletct.ftmes.bpi.application.BatchQueryService;
import com.mapletct.ftmes.bpi.application.BatchReleaseService;
import com.mapletct.ftmes.bpi.application.Checksums;
import com.mapletct.ftmes.bpi.application.WmsInboundReversalService;
import com.mapletct.ftmes.bpi.application.error.BpiForbiddenException;
import com.mapletct.ftmes.bpi.application.error.BpiValidationException;
import com.mapletct.ftmes.bpi.contract.v1.QcsQualityGateV1;
import com.mapletct.ftmes.bpi.contract.v1.WmsCompletionInboundReceiptV1;
import com.mapletct.ftmes.bpi.contract.v1.WmsCompletionInboundReversalReceiptV1;
import com.mapletct.ftmes.bpi.domain.BatchInstance;
import com.mapletct.ftmes.bpi.domain.BatchReleaseView;
import com.mapletct.ftmes.bpi.domain.IntegrationBatchResolution;
import com.mapletct.ftmes.bpi.domain.WmsInboundReversalTaskView;
import com.mapletct.ftmes.bpi.infrastructure.integration.BpiPhase2IntegrationProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InternalPhase2IntegrationController {
    private static final String PROTOBUF_MEDIA_TYPE = "application/x-protobuf";

    private final ActorContextFactory actorContextFactory;
    private final BatchQueryService batchQueryService;
    private final BatchReleaseService batchReleaseService;
    private final WmsInboundReversalService wmsInboundReversalService;
    private final BpiPhase2IntegrationProperties properties;

    public InternalPhase2IntegrationController(
            ActorContextFactory actorContextFactory,
            BatchQueryService batchQueryService,
            BatchReleaseService batchReleaseService,
            WmsInboundReversalService wmsInboundReversalService,
            BpiPhase2IntegrationProperties properties) {
        this.actorContextFactory = actorContextFactory;
        this.batchQueryService = batchQueryService;
        this.batchReleaseService = batchReleaseService;
        this.wmsInboundReversalService = wmsInboundReversalService;
        this.properties = properties;
    }

    @GetMapping("/internal/bpi/v1/batches/resolve")
    @PreAuthorize("hasAnyRole('BPI_INTEGRATION_INGEST', 'BPI_ADMIN')")
    public ApiResponse<IntegrationBatchResolution> resolveBatch(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam String plantId,
            @RequestParam String lineId,
            @RequestParam String orderId,
            HttpServletRequest request) {
        ActorContext actor = actorContextFactory.from(jwt);
        assertIntegrationScope(actor, plantId, lineId);
        BatchInstance batch = batchQueryService.resolveIntegrationBatch(
                actor, plantId, lineId, orderId);
        BatchReleaseView release = batchReleaseService.get(actor, batch.id());
        return ApiResponse.of(IntegrationBatchResolution.from(release), request);
    }

    @PostMapping(path = "/internal/bpi/v1/qcs-quality-gates", consumes = PROTOBUF_MEDIA_TYPE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('BPI_INTEGRATION_INGEST', 'BPI_ADMIN')")
    public ApiResponse<BatchReleaseView> qualityGate(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody byte[] payload,
            HttpServletRequest request) {
        assertIngress(payload);
        QcsQualityGateV1 event;
        try {
            event = QcsQualityGateV1.parseFrom(payload);
        } catch (InvalidProtocolBufferException error) {
            throw new BpiValidationException("Payload is not valid QcsQualityGateV1 Protobuf.");
        }
        ActorContext actor = actorContextFactory.from(jwt);
        return ApiResponse.of(
                batchReleaseService.applyQualityGate(actor, event, Checksums.sha256(payload)),
                request);
    }

    @PostMapping(path = "/internal/bpi/v1/wms-inbound-receipts", consumes = PROTOBUF_MEDIA_TYPE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('BPI_INTEGRATION_INGEST', 'BPI_ADMIN')")
    public ApiResponse<BatchReleaseView> wmsReceipt(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody byte[] payload,
            HttpServletRequest request) {
        assertIngress(payload);
        WmsCompletionInboundReceiptV1 event;
        try {
            event = WmsCompletionInboundReceiptV1.parseFrom(payload);
        } catch (InvalidProtocolBufferException error) {
            throw new BpiValidationException(
                    "Payload is not valid WmsCompletionInboundReceiptV1 Protobuf.");
        }
        ActorContext actor = actorContextFactory.from(jwt);
        return ApiResponse.of(
                batchReleaseService.applyWmsReceipt(actor, event, Checksums.sha256(payload)),
                request);
    }

    @PostMapping(
            path = "/internal/bpi/v1/wms-inbound-reversal-receipts",
            consumes = PROTOBUF_MEDIA_TYPE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('BPI_INTEGRATION_INGEST', 'BPI_ADMIN')")
    public ApiResponse<WmsInboundReversalTaskView> wmsReversalReceipt(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody byte[] payload,
            HttpServletRequest request) {
        assertIngress(payload);
        WmsCompletionInboundReversalReceiptV1 event;
        try {
            event = WmsCompletionInboundReversalReceiptV1.parseFrom(payload);
        } catch (InvalidProtocolBufferException error) {
            throw new BpiValidationException(
                    "Payload is not valid WmsCompletionInboundReversalReceiptV1 Protobuf.");
        }
        ActorContext actor = actorContextFactory.from(jwt);
        return ApiResponse.of(
                wmsInboundReversalService.applyReceipt(
                        actor, event, Checksums.sha256(payload)),
                request);
    }

    private void assertIngress(byte[] payload) {
        if (!properties.enabled() || !properties.protobufHttpIngressEnabled()) {
            throw new BpiForbiddenException("BPI Phase 2 Protobuf HTTP ingress is disabled.");
        }
        if (payload.length == 0 || payload.length > properties.maxPayloadBytes()) {
            throw new BpiValidationException("Phase 2 integration payload size is invalid.");
        }
    }

    private void assertIntegrationScope(
            ActorContext actor, String plantId, String lineId) {
        if (!properties.enabled()) {
            throw new BpiForbiddenException("BPI Phase 2 integration is disabled.");
        }
        if (!properties.allows(actor.tenantId(), plantId, lineId)) {
            throw new BpiForbiddenException("BPI Phase 2 integration scope is not allowed.");
        }
    }
}
