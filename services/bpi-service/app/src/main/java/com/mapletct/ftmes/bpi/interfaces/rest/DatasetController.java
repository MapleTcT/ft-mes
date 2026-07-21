package com.mapletct.ftmes.bpi.interfaces.rest;

import com.mapletct.ftmes.bpi.application.ActorContextFactory;
import com.mapletct.ftmes.bpi.application.CommandResult;
import com.mapletct.ftmes.bpi.application.DatasetService;
import com.mapletct.ftmes.bpi.domain.DatasetDefinitionView;
import com.mapletct.ftmes.bpi.domain.DatasetSnapshotView;
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
public class DatasetController {
    private final ActorContextFactory actorContextFactory;
    private final DatasetService service;

    public DatasetController(
            ActorContextFactory actorContextFactory,
            DatasetService service) {
        this.actorContextFactory = actorContextFactory;
        this.service = service;
    }

    @GetMapping("/bpi/v1/datasets")
    public ApiResponse<List<DatasetDefinitionView>> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam String plantId,
            @RequestParam(required = false) Integer limit,
            HttpServletRequest request) {
        return ApiResponse.of(
                service.list(actorContextFactory.from(jwt), plantId, limit), request);
    }

    @PostMapping("/bpi/v1/datasets")
    @PreAuthorize("hasAnyRole('BPI_ENGINEER', 'BPI_ADMIN')")
    public ResponseEntity<ApiResponse<DatasetDefinitionView>> createDefinition(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @Valid @RequestBody DatasetDefinitionCreateCommand command,
            HttpServletRequest request) {
        return ok(service.createDefinition(actorContextFactory.from(jwt), idempotencyKey,
                ifMatch, command, traceId(request)), request);
    }

    @PostMapping("/bpi/v1/datasets/{datasetId}/snapshots")
    @PreAuthorize("hasAnyRole('BPI_ENGINEER', 'BPI_ADMIN')")
    public ResponseEntity<ApiResponse<DatasetSnapshotView>> createSnapshot(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID datasetId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @Valid @RequestBody DatasetSnapshotCommand command,
            HttpServletRequest request) {
        CommandResult<DatasetSnapshotView> result = service.createSnapshot(
                actorContextFactory.from(jwt), datasetId, idempotencyKey,
                ifMatch, command, traceId(request));
        ResponseEntity.BodyBuilder response = ResponseEntity.accepted();
        if (result.replayed()) response.header("Idempotent-Replay", "true");
        return response.body(ApiResponse.of(result.data(), request));
    }

    @GetMapping("/bpi/v1/dataset-snapshots/{snapshotId}")
    public ApiResponse<DatasetSnapshotView> getSnapshot(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID snapshotId,
            HttpServletRequest request) {
        return ApiResponse.of(
                service.getSnapshot(actorContextFactory.from(jwt), snapshotId), request);
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
