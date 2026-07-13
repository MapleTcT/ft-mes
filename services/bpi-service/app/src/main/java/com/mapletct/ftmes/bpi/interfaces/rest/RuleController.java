package com.mapletct.ftmes.bpi.interfaces.rest;

import com.mapletct.ftmes.bpi.application.ActorContext;
import com.mapletct.ftmes.bpi.application.ActorContextFactory;
import com.mapletct.ftmes.bpi.application.CommandResult;
import com.mapletct.ftmes.bpi.application.RuleService;
import com.mapletct.ftmes.bpi.domain.RuleSimulationView;
import com.mapletct.ftmes.bpi.domain.RuleVersionView;
import com.mapletct.ftmes.bpi.domain.TopologyVersionView;
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
public class RuleController {
    private final ActorContextFactory actorContextFactory;
    private final RuleService ruleService;

    public RuleController(ActorContextFactory actorContextFactory, RuleService ruleService) {
        this.actorContextFactory = actorContextFactory;
        this.ruleService = ruleService;
    }

    @GetMapping("/bpi/v1/topologies")
    public ApiResponse<List<TopologyVersionView>> listTopologies(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String plantId,
            @RequestParam(required = false) String lineId,
            HttpServletRequest request) {
        return ApiResponse.of(
                ruleService.listTopologies(actorContextFactory.from(jwt), plantId, lineId), request);
    }

    @GetMapping("/bpi/v1/topologies/{topologyId}")
    public ApiResponse<TopologyVersionView> getTopology(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID topologyId,
            HttpServletRequest request) {
        return ApiResponse.of(ruleService.getTopology(actorContextFactory.from(jwt), topologyId), request);
    }

    @GetMapping("/bpi/v1/rules")
    public ApiResponse<List<RuleVersionView>> listRules(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String plantId,
            @RequestParam(required = false) String lineId,
            HttpServletRequest request) {
        return ApiResponse.of(ruleService.listRules(actorContextFactory.from(jwt), plantId, lineId), request);
    }

    @GetMapping("/bpi/v1/rules/{ruleId}")
    public ApiResponse<RuleVersionView> getRule(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID ruleId,
            HttpServletRequest request) {
        return ApiResponse.of(ruleService.getRule(actorContextFactory.from(jwt), ruleId), request);
    }

    @GetMapping("/bpi/v1/rule-simulations/{simulationId}")
    public ApiResponse<RuleSimulationView> getSimulation(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID simulationId,
            HttpServletRequest request) {
        return ApiResponse.of(
                ruleService.getSimulation(actorContextFactory.from(jwt), simulationId), request);
    }

    @PostMapping("/bpi/v1/rules/{ruleId}/simulate")
    @PreAuthorize("hasAnyRole('BPI_ENGINEER', 'BPI_ADMIN')")
    public ResponseEntity<ApiResponse<RuleSimulationView>> simulate(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID ruleId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @Valid @RequestBody RuleSimulationCommand command,
            HttpServletRequest request) {
        ActorContext actor = actorContextFactory.from(jwt);
        CommandResult<RuleSimulationView> result = ruleService.simulate(
                actor, ruleId, idempotencyKey, ifMatch, command, traceId(request));
        ResponseEntity.BodyBuilder response = ResponseEntity.status(202);
        if (result.replayed()) response.header("Idempotent-Replay", "true");
        return response.body(ApiResponse.of(result.data(), request));
    }

    @PostMapping("/bpi/v1/rules/{ruleId}/publish")
    @PreAuthorize("hasAnyRole('BPI_ENGINEER', 'BPI_ADMIN')")
    public ResponseEntity<ApiResponse<RuleVersionView>> publish(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID ruleId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @Valid @RequestBody RulePublishCommand command,
            HttpServletRequest request) {
        ActorContext actor = actorContextFactory.from(jwt);
        CommandResult<RuleVersionView> result = ruleService.publish(
                actor, ruleId, idempotencyKey, ifMatch, command, traceId(request));
        ResponseEntity.BodyBuilder response = ResponseEntity.ok();
        if (result.replayed()) response.header("Idempotent-Replay", "true");
        return response.body(ApiResponse.of(result.data(), request));
    }

    @PostMapping("/bpi/v1/rules/{ruleId}/publication/retry")
    @PreAuthorize("hasRole('BPI_ADMIN')")
    public ResponseEntity<ApiResponse<RuleVersionView>> retryPublication(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID ruleId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @Valid @RequestBody ReasonCommand command,
            HttpServletRequest request) {
        ActorContext actor = actorContextFactory.from(jwt);
        CommandResult<RuleVersionView> result = ruleService.retryPublication(
                actor, ruleId, idempotencyKey, ifMatch, command, traceId(request));
        ResponseEntity.BodyBuilder response = ResponseEntity.ok();
        if (result.replayed()) response.header("Idempotent-Replay", "true");
        return response.body(ApiResponse.of(result.data(), request));
    }

    private String traceId(HttpServletRequest request) {
        return String.valueOf(request.getAttribute(TraceIdFilter.ATTRIBUTE));
    }
}
