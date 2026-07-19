package com.mapletct.ftmes.bpi.interfaces.rest;

import com.mapletct.ftmes.bpi.application.ActorContextFactory;
import com.mapletct.ftmes.bpi.application.CommandResult;
import com.mapletct.ftmes.bpi.application.DataQualityIncidentPage;
import com.mapletct.ftmes.bpi.application.DataQualityIncidentService;
import com.mapletct.ftmes.bpi.domain.DataQualityIncidentDetail;
import com.mapletct.ftmes.bpi.domain.DataQualityIncidentView;
import com.mapletct.ftmes.bpi.domain.DataQualitySummary;
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
public class DataQualityController {
    private final ActorContextFactory actorContextFactory;
    private final DataQualityIncidentService service;

    public DataQualityController(
            ActorContextFactory actorContextFactory,
            DataQualityIncidentService service) {
        this.actorContextFactory = actorContextFactory;
        this.service = service;
    }

    @GetMapping("/bpi/v1/data-quality/incidents")
    public ApiResponse<List<DataQualityIncidentView>> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam String plantId,
            @RequestParam(required = false) String lineId,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer limit,
            HttpServletRequest request) {
        DataQualityIncidentPage page = service.list(
                actorContextFactory.from(jwt), plantId, lineId, state, search, cursor, limit);
        return ApiResponse.of(page.items(), request, page.snapshotAt(), page.nextCursor());
    }

    @GetMapping("/bpi/v1/data-quality/summary")
    public ApiResponse<DataQualitySummary> summary(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam String plantId,
            @RequestParam(required = false) String lineId,
            HttpServletRequest request) {
        return ApiResponse.of(service.summary(actorContextFactory.from(jwt), plantId, lineId), request);
    }

    @GetMapping("/bpi/v1/data-quality/incidents/{incidentId}")
    public ApiResponse<DataQualityIncidentDetail> detail(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID incidentId,
            HttpServletRequest request) {
        return ApiResponse.of(service.detail(actorContextFactory.from(jwt), incidentId), request);
    }

    @PostMapping("/bpi/v1/data-quality/incidents/{incidentId}/acknowledge")
    @PreAuthorize("hasAnyRole('BPI_SHIFT_LEAD', 'BPI_ENGINEER', 'BPI_ADMIN')")
    public ResponseEntity<ApiResponse<DataQualityIncidentView>> acknowledge(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID incidentId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @Valid @RequestBody DataQualityAcknowledgeCommand command,
            HttpServletRequest request) {
        return ok(service.acknowledge(actorContextFactory.from(jwt), incidentId,
                idempotencyKey, ifMatch, command, traceId(request)), request);
    }

    @PostMapping("/bpi/v1/data-quality/incidents/{incidentId}/resolve")
    @PreAuthorize("hasAnyRole('BPI_SHIFT_LEAD', 'BPI_ENGINEER', 'BPI_ADMIN')")
    public ResponseEntity<ApiResponse<DataQualityIncidentView>> resolve(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID incidentId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @Valid @RequestBody ReasonCommand command,
            HttpServletRequest request) {
        return ok(service.resolve(actorContextFactory.from(jwt), incidentId,
                idempotencyKey, ifMatch, command, traceId(request)), request);
    }

    private ResponseEntity<ApiResponse<DataQualityIncidentView>> ok(
            CommandResult<DataQualityIncidentView> result,
            HttpServletRequest request) {
        ResponseEntity.BodyBuilder response = ResponseEntity.ok();
        if (result.replayed()) response.header("Idempotent-Replay", "true");
        return response.body(ApiResponse.of(result.data(), request));
    }

    private String traceId(HttpServletRequest request) {
        return String.valueOf(request.getAttribute(TraceIdFilter.ATTRIBUTE));
    }
}
