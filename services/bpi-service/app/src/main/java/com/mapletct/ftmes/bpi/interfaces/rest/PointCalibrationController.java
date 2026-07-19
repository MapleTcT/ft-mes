package com.mapletct.ftmes.bpi.interfaces.rest;

import com.mapletct.ftmes.bpi.application.ActorContextFactory;
import com.mapletct.ftmes.bpi.application.CommandResult;
import com.mapletct.ftmes.bpi.application.PointCalibrationService;
import com.mapletct.ftmes.bpi.domain.PointCalibrationView;
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
public class PointCalibrationController {
    private final ActorContextFactory actorContextFactory;
    private final PointCalibrationService service;

    public PointCalibrationController(
            ActorContextFactory actorContextFactory,
            PointCalibrationService service) {
        this.actorContextFactory = actorContextFactory;
        this.service = service;
    }

    @GetMapping("/bpi/v1/point-calibrations")
    public ApiResponse<List<PointCalibrationView>> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam String plantId,
            @RequestParam String lineId,
            @RequestParam(required = false) String productId,
            @RequestParam(required = false) String deviceId,
            @RequestParam(required = false) String propertyId,
            HttpServletRequest request) {
        return ApiResponse.of(service.list(
                actorContextFactory.from(jwt), plantId, lineId, productId, deviceId, propertyId), request);
    }

    @PostMapping("/bpi/v1/point-calibrations")
    @PreAuthorize("hasAnyRole('BPI_ENGINEER', 'BPI_ADMIN')")
    public ResponseEntity<ApiResponse<PointCalibrationView>> submit(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @Valid @RequestBody PointCalibrationSubmitCommand command,
            HttpServletRequest request) {
        return ok(service.submit(actorContextFactory.from(jwt), idempotencyKey, ifMatch,
                command, traceId(request)), request);
    }

    @PostMapping("/bpi/v1/point-calibrations/{calibrationId}/approve")
    @PreAuthorize("hasRole('BPI_ADMIN')")
    public ResponseEntity<ApiResponse<PointCalibrationView>> approve(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID calibrationId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @Valid @RequestBody ReasonCommand command,
            HttpServletRequest request) {
        return ok(service.approve(actorContextFactory.from(jwt), calibrationId, idempotencyKey,
                ifMatch, command, traceId(request)), request);
    }

    @PostMapping("/bpi/v1/point-calibrations/{calibrationId}/reject")
    @PreAuthorize("hasRole('BPI_ADMIN')")
    public ResponseEntity<ApiResponse<PointCalibrationView>> reject(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID calibrationId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @Valid @RequestBody ReasonCommand command,
            HttpServletRequest request) {
        return ok(service.reject(actorContextFactory.from(jwt), calibrationId, idempotencyKey,
                ifMatch, command, traceId(request)), request);
    }

    @PostMapping("/bpi/v1/point-calibrations/{calibrationId}/revoke")
    @PreAuthorize("hasRole('BPI_ADMIN')")
    public ResponseEntity<ApiResponse<PointCalibrationView>> revoke(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID calibrationId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @Valid @RequestBody ReasonCommand command,
            HttpServletRequest request) {
        return ok(service.revoke(actorContextFactory.from(jwt), calibrationId, idempotencyKey,
                ifMatch, command, traceId(request)), request);
    }

    private ResponseEntity<ApiResponse<PointCalibrationView>> ok(
            CommandResult<PointCalibrationView> result,
            HttpServletRequest request) {
        ResponseEntity.BodyBuilder response = ResponseEntity.ok();
        if (result.replayed()) response.header("Idempotent-Replay", "true");
        return response.body(ApiResponse.of(result.data(), request));
    }

    private String traceId(HttpServletRequest request) {
        return String.valueOf(request.getAttribute(TraceIdFilter.ATTRIBUTE));
    }
}
