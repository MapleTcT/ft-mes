package com.mapletct.ftmes.bpi.interfaces.rest;

import com.mapletct.ftmes.bpi.application.ActorContextFactory;
import com.mapletct.ftmes.bpi.application.CommandResult;
import com.mapletct.ftmes.bpi.application.FeatureFlagService;
import com.mapletct.ftmes.bpi.domain.FeatureFlagView;
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

@RestController
@PreAuthorize("hasAnyRole('BPI_VIEWER', 'BPI_SHIFT_LEAD', 'BPI_ENGINEER', 'BPI_ADMIN')")
public class FeatureFlagController {
    private final ActorContextFactory actorContextFactory;
    private final FeatureFlagService service;

    public FeatureFlagController(
            ActorContextFactory actorContextFactory,
            FeatureFlagService service) {
        this.actorContextFactory = actorContextFactory;
        this.service = service;
    }

    @GetMapping("/bpi/v1/feature-flags")
    public ApiResponse<List<FeatureFlagView>> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam String plantId,
            @RequestParam String lineId,
            @RequestParam(defaultValue = "LINE") String scopeType,
            HttpServletRequest request) {
        return ApiResponse.of(service.list(
                actorContextFactory.from(jwt), plantId, lineId, scopeType), request);
    }

    @PostMapping("/bpi/v1/feature-flags/{flagKey}")
    @PreAuthorize("hasRole('BPI_ADMIN')")
    public ResponseEntity<ApiResponse<FeatureFlagView>> change(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String flagKey,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @Valid @RequestBody FeatureFlagOverrideCommand command,
            HttpServletRequest request) {
        CommandResult<FeatureFlagView> result = service.change(
                actorContextFactory.from(jwt), flagKey, idempotencyKey, ifMatch,
                command, traceId(request));
        ResponseEntity.BodyBuilder response = ResponseEntity.ok();
        if (result.replayed()) response.header("Idempotent-Replay", "true");
        return response.body(ApiResponse.of(result.data(), request));
    }

    private String traceId(HttpServletRequest request) {
        return String.valueOf(request.getAttribute(TraceIdFilter.ATTRIBUTE));
    }
}
