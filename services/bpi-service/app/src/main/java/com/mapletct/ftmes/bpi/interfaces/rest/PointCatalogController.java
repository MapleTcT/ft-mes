package com.mapletct.ftmes.bpi.interfaces.rest;

import com.mapletct.ftmes.bpi.application.ActorContextFactory;
import com.mapletct.ftmes.bpi.application.CommandResult;
import com.mapletct.ftmes.bpi.application.PointCatalogService;
import com.mapletct.ftmes.bpi.domain.PointCatalogSnapshotView;
import com.mapletct.ftmes.bpi.domain.PointCatalogView;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@PreAuthorize("hasAnyRole('BPI_VIEWER', 'BPI_SHIFT_LEAD', 'BPI_ENGINEER', 'BPI_ADMIN')")
public class PointCatalogController {
    private final ActorContextFactory actorContextFactory;
    private final PointCatalogService service;

    public PointCatalogController(
            ActorContextFactory actorContextFactory,
            PointCatalogService service) {
        this.actorContextFactory = actorContextFactory;
        this.service = service;
    }

    @GetMapping("/bpi/v1/point-catalog/snapshots")
    public ApiResponse<List<PointCatalogSnapshotView>> listSnapshots(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String plantId,
            @RequestParam(required = false) String lineId,
            HttpServletRequest request) {
        return ApiResponse.of(
                service.listSnapshots(actorContextFactory.from(jwt), plantId, lineId), request);
    }

    @GetMapping("/bpi/v1/point-catalog/current")
    public ApiResponse<PointCatalogView> current(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam String plantId,
            @RequestParam String lineId,
            HttpServletRequest request) {
        return ApiResponse.of(
                service.current(actorContextFactory.from(jwt), plantId, lineId).orElse(null), request);
    }

    @PostMapping("/bpi/v1/point-catalog/snapshots")
    @PreAuthorize("hasRole('BPI_ADMIN')")
    public ResponseEntity<ApiResponse<PointCatalogView>> importSnapshot(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @Valid @RequestBody PointCatalogSnapshotCommand command,
            HttpServletRequest request) {
        CommandResult<PointCatalogView> result = service.importSnapshot(
                actorContextFactory.from(jwt), idempotencyKey, ifMatch, command,
                String.valueOf(request.getAttribute(TraceIdFilter.ATTRIBUTE)));
        ResponseEntity.BodyBuilder response = ResponseEntity.ok();
        if (result.replayed()) response.header("Idempotent-Replay", "true");
        return response.body(ApiResponse.of(result.data(), request));
    }
}
