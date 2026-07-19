package com.mapletct.ftmes.bpi.interfaces.rest;

import com.mapletct.ftmes.bpi.application.ActorContextFactory;
import com.mapletct.ftmes.bpi.application.CommandResult;
import com.mapletct.ftmes.bpi.application.ShadowRunService;
import com.mapletct.ftmes.bpi.domain.ShadowRunBatchReviewView;
import com.mapletct.ftmes.bpi.domain.ShadowRunReviewResult;
import com.mapletct.ftmes.bpi.domain.ShadowRunView;
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
import java.util.UUID;

@RestController
@PreAuthorize("hasAnyRole('BPI_VIEWER', 'BPI_SHIFT_LEAD', 'BPI_ENGINEER', 'BPI_ADMIN')")
public class ShadowRunController {
    private final ActorContextFactory actorContextFactory;
    private final ShadowRunService service;

    public ShadowRunController(
            ActorContextFactory actorContextFactory,
            ShadowRunService service) {
        this.actorContextFactory = actorContextFactory;
        this.service = service;
    }

    @GetMapping("/bpi/v1/shadow-runs")
    public ApiResponse<List<ShadowRunView>> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String plantId,
            @RequestParam(required = false) String lineId,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) Integer limit,
            HttpServletRequest request) {
        return ApiResponse.of(
                service.list(actorContextFactory.from(jwt), plantId, lineId, state, limit), request);
    }

    @GetMapping("/bpi/v1/shadow-runs/{runId}")
    public ApiResponse<ShadowRunView> get(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID runId,
            HttpServletRequest request) {
        return ApiResponse.of(service.get(actorContextFactory.from(jwt), runId), request);
    }

    @GetMapping("/bpi/v1/shadow-runs/{runId}/batch-reviews")
    public ApiResponse<List<ShadowRunBatchReviewView>> listReviews(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID runId,
            @RequestParam(defaultValue = "false") boolean includeSuperseded,
            HttpServletRequest request) {
        return ApiResponse.of(
                service.listReviews(actorContextFactory.from(jwt), runId, includeSuperseded), request);
    }

    @PostMapping("/bpi/v1/shadow-runs")
    @PreAuthorize("hasAnyRole('BPI_ENGINEER', 'BPI_ADMIN')")
    public ResponseEntity<ApiResponse<ShadowRunView>> create(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @Valid @RequestBody ShadowRunCreateCommand command,
            HttpServletRequest request) {
        return ok(service.create(actorContextFactory.from(jwt), idempotencyKey, ifMatch,
                command, traceId(request)), request);
    }

    @PostMapping("/bpi/v1/shadow-runs/{runId}/start")
    @PreAuthorize("hasAnyRole('BPI_ENGINEER', 'BPI_ADMIN')")
    public ResponseEntity<ApiResponse<ShadowRunView>> start(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID runId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @Valid @RequestBody ReasonCommand command,
            HttpServletRequest request) {
        return ok(service.start(actorContextFactory.from(jwt), runId, idempotencyKey,
                ifMatch, command, traceId(request)), request);
    }

    @PostMapping("/bpi/v1/shadow-runs/{runId}/batch-reviews")
    @PreAuthorize("hasAnyRole('BPI_SHIFT_LEAD', 'BPI_ENGINEER', 'BPI_ADMIN')")
    public ResponseEntity<ApiResponse<ShadowRunReviewResult>> reviewBatch(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID runId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @Valid @RequestBody ShadowRunBatchReviewCommand command,
            HttpServletRequest request) {
        return ok(service.reviewBatch(actorContextFactory.from(jwt), runId, idempotencyKey,
                ifMatch, command, traceId(request)), request);
    }

    @PostMapping("/bpi/v1/shadow-runs/{runId}/complete")
    @PreAuthorize("hasAnyRole('BPI_ENGINEER', 'BPI_ADMIN')")
    public ResponseEntity<ApiResponse<ShadowRunView>> complete(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID runId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @Valid @RequestBody ReasonCommand command,
            HttpServletRequest request) {
        return ok(service.completeRun(actorContextFactory.from(jwt), runId, idempotencyKey,
                ifMatch, command, traceId(request)), request);
    }

    @PostMapping("/bpi/v1/shadow-runs/{runId}/approve")
    @PreAuthorize("hasRole('BPI_ADMIN')")
    public ResponseEntity<ApiResponse<ShadowRunView>> approve(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID runId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @Valid @RequestBody ReasonCommand command,
            HttpServletRequest request) {
        return ok(service.approve(actorContextFactory.from(jwt), runId, idempotencyKey,
                ifMatch, command, traceId(request)), request);
    }

    @PostMapping("/bpi/v1/shadow-runs/{runId}/reject")
    @PreAuthorize("hasRole('BPI_ADMIN')")
    public ResponseEntity<ApiResponse<ShadowRunView>> reject(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID runId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @Valid @RequestBody ReasonCommand command,
            HttpServletRequest request) {
        return ok(service.reject(actorContextFactory.from(jwt), runId, idempotencyKey,
                ifMatch, command, traceId(request)), request);
    }

    @PostMapping("/bpi/v1/shadow-runs/{runId}/cancel")
    @PreAuthorize("hasAnyRole('BPI_ENGINEER', 'BPI_ADMIN')")
    public ResponseEntity<ApiResponse<ShadowRunView>> cancel(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID runId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @Valid @RequestBody ReasonCommand command,
            HttpServletRequest request) {
        return ok(service.cancel(actorContextFactory.from(jwt), runId, idempotencyKey,
                ifMatch, command, traceId(request)), request);
    }

    private <T> ResponseEntity<ApiResponse<T>> ok(
            CommandResult<T> result,
            HttpServletRequest request) {
        ResponseEntity.BodyBuilder response = ResponseEntity.ok();
        if (result.replayed()) response.header("Idempotent-Replay", "true");
        return response.body(ApiResponse.of(result.data(), request));
    }

    private String traceId(HttpServletRequest request) {
        return String.valueOf(request.getAttribute(TraceIdFilter.ATTRIBUTE));
    }
}
