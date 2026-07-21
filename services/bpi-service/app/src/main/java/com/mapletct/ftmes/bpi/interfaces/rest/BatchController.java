package com.mapletct.ftmes.bpi.interfaces.rest;

import com.mapletct.ftmes.bpi.application.ActorContext;
import com.mapletct.ftmes.bpi.application.ActorContextFactory;
import com.mapletct.ftmes.bpi.application.BatchCommandService;
import com.mapletct.ftmes.bpi.application.BatchQueryService;
import com.mapletct.ftmes.bpi.application.BatchReleaseService;
import com.mapletct.ftmes.bpi.application.CommandResult;
import com.mapletct.ftmes.bpi.application.WmsInboundReversalService;
import com.mapletct.ftmes.bpi.domain.BatchInstance;
import com.mapletct.ftmes.bpi.domain.BatchReleaseView;
import com.mapletct.ftmes.bpi.domain.BatchStateEvent;
import com.mapletct.ftmes.bpi.domain.EvidenceView;
import com.mapletct.ftmes.bpi.domain.ForceCloseTaskView;
import com.mapletct.ftmes.bpi.domain.WmsInboundReversalTaskView;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@PreAuthorize("hasAnyRole('BPI_VIEWER', 'BPI_SHIFT_LEAD', 'BPI_ENGINEER', 'BPI_ADMIN')")
public class BatchController {
    private final ActorContextFactory actorContextFactory;
    private final BatchQueryService batchQueryService;
    private final BatchCommandService batchCommandService;
    private final BatchReleaseService batchReleaseService;
    private final WmsInboundReversalService wmsInboundReversalService;

    public BatchController(
            ActorContextFactory actorContextFactory,
            BatchQueryService batchQueryService,
            BatchCommandService batchCommandService,
            BatchReleaseService batchReleaseService,
            WmsInboundReversalService wmsInboundReversalService) {
        this.actorContextFactory = actorContextFactory;
        this.batchQueryService = batchQueryService;
        this.batchCommandService = batchCommandService;
        this.batchReleaseService = batchReleaseService;
        this.wmsInboundReversalService = wmsInboundReversalService;
    }

    @GetMapping("/bpi/v1/batches")
    public ApiResponse<List<BatchInstance>> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam String plantId,
            @RequestParam(required = false) String lineId,
            @RequestParam(required = false) String state,
            @RequestParam(defaultValue = "50") int limit,
            HttpServletRequest request) {
        ActorContext actor = actorContextFactory.from(jwt);
        return ApiResponse.of(batchQueryService.list(actor, plantId, lineId, state, limit), request);
    }

    @GetMapping("/bpi/v1/batches/{batchId}")
    public ApiResponse<BatchInstance> get(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID batchId,
            HttpServletRequest request) {
        return ApiResponse.of(batchQueryService.get(actorContextFactory.from(jwt), batchId), request);
    }

    @GetMapping("/bpi/v1/batches/{batchId}/evidence")
    public ApiResponse<Map<String, List<EvidenceView>>> evidence(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID batchId,
            HttpServletRequest request) {
        return ApiResponse.of(batchQueryService.evidence(actorContextFactory.from(jwt), batchId), request);
    }

    @GetMapping("/bpi/v1/batches/{batchId}/timeline")
    public ApiResponse<List<BatchStateEvent>> timeline(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID batchId,
            HttpServletRequest request) {
        return ApiResponse.of(batchQueryService.timeline(actorContextFactory.from(jwt), batchId), request);
    }

    @GetMapping("/bpi/v1/batches/{batchId}/release")
    public ApiResponse<BatchReleaseView> release(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID batchId,
            HttpServletRequest request) {
        return ApiResponse.of(batchReleaseService.get(actorContextFactory.from(jwt), batchId), request);
    }

    @GetMapping("/bpi/v1/batches/{batchId}/force-close")
    public ApiResponse<ForceCloseTaskView> forceCloseTask(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID batchId,
            HttpServletRequest request) {
        return ApiResponse.of(
                batchCommandService.latestForceCloseTask(actorContextFactory.from(jwt), batchId), request);
    }

    @GetMapping("/bpi/v1/batches/{batchId}/wms/reversal")
    public ApiResponse<WmsInboundReversalTaskView> wmsInboundReversalTask(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID batchId,
            HttpServletRequest request) {
        return ApiResponse.of(
                wmsInboundReversalService.latest(
                        actorContextFactory.from(jwt), batchId),
                request);
    }

    @PostMapping("/bpi/v1/batches/{batchId}/wms/reconcile")
    @PreAuthorize("hasRole('BPI_ADMIN')")
    public ResponseEntity<ApiResponse<BatchReleaseView>> reconcileWmsInbound(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID batchId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @Valid @RequestBody ReasonCommand command,
            HttpServletRequest request) {
        CommandResult<BatchReleaseView> result = batchReleaseService.reconcileWmsInbound(
                actorContextFactory.from(jwt), batchId, idempotencyKey, ifMatch,
                command, traceId(request));
        ResponseEntity.BodyBuilder response = ResponseEntity.ok();
        if (result.replayed()) {
            response.header("Idempotent-Replay", "true");
        }
        return response.body(ApiResponse.of(result.data(), request));
    }

    @PostMapping("/bpi/v1/batches/{batchId}/wms/reversal")
    @PreAuthorize("hasAnyRole('BPI_SHIFT_LEAD', 'BPI_ADMIN')")
    public ResponseEntity<ApiResponse<WmsInboundReversalTaskView>> wmsInboundReversal(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID batchId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @Valid @RequestBody WmsInboundReversalCommand command,
            HttpServletRequest request) {
        CommandResult<WmsInboundReversalTaskView> result =
                wmsInboundReversalService.command(
                        actorContextFactory.from(jwt), batchId, idempotencyKey,
                        ifMatch, command, traceId(request));
        ResponseEntity.BodyBuilder response = ResponseEntity.accepted();
        if (result.replayed()) {
            response.header("Idempotent-Replay", "true");
        }
        return response.body(ApiResponse.of(result.data(), request));
    }

    @PostMapping("/bpi/v1/batches/{batchId}/suspend")
    @PreAuthorize("hasAnyRole('BPI_SHIFT_LEAD', 'BPI_ADMIN')")
    public ResponseEntity<ApiResponse<BatchInstance>> suspend(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID batchId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @Valid @RequestBody ReasonCommand command,
            HttpServletRequest request) {
        return commandResponse(batchCommandService.suspend(
                actorContextFactory.from(jwt), batchId, idempotencyKey, ifMatch,
                command, traceId(request)), request);
    }

    @PostMapping("/bpi/v1/batches/{batchId}/resume")
    @PreAuthorize("hasAnyRole('BPI_SHIFT_LEAD', 'BPI_ADMIN')")
    public ResponseEntity<ApiResponse<BatchInstance>> resume(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID batchId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @Valid @RequestBody ReasonCommand command,
            HttpServletRequest request) {
        return commandResponse(batchCommandService.resume(
                actorContextFactory.from(jwt), batchId, idempotencyKey, ifMatch,
                command, traceId(request)), request);
    }

    @PostMapping("/bpi/v1/batches/{batchId}/force-close")
    @PreAuthorize("hasAnyRole('BPI_SHIFT_LEAD', 'BPI_ADMIN')")
    public ResponseEntity<ApiResponse<ForceCloseTaskView>> forceClose(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID batchId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @Valid @RequestBody ForceCloseCommand command,
            HttpServletRequest request) {
        CommandResult<ForceCloseTaskView> result = batchCommandService.forceClose(
                actorContextFactory.from(jwt), batchId, idempotencyKey, ifMatch,
                command, traceId(request));
        ResponseEntity.BodyBuilder response = ResponseEntity.accepted();
        if (result.replayed()) {
            response.header("Idempotent-Replay", "true");
        }
        return response.body(ApiResponse.of(result.data(), request));
    }

    private ResponseEntity<ApiResponse<BatchInstance>> commandResponse(
            CommandResult<BatchInstance> result, HttpServletRequest request) {
        ResponseEntity.BodyBuilder response = ResponseEntity.ok();
        if (result.replayed()) {
            response.header("Idempotent-Replay", "true");
        }
        return response.body(ApiResponse.of(result.data(), request));
    }

    private String traceId(HttpServletRequest request) {
        return String.valueOf(request.getAttribute(TraceIdFilter.ATTRIBUTE));
    }
}
