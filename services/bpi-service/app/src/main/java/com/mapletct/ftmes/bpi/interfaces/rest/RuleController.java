package com.mapletct.ftmes.bpi.interfaces.rest;

import com.mapletct.ftmes.bpi.application.ActorContext;
import com.mapletct.ftmes.bpi.application.ActorContextFactory;
import com.mapletct.ftmes.bpi.application.CommandResult;
import com.mapletct.ftmes.bpi.application.RuleService;
import com.mapletct.ftmes.bpi.domain.RuleSimulationView;
import com.mapletct.ftmes.bpi.domain.RuleVersionView;
import com.mapletct.ftmes.bpi.domain.TopologyVersionView;
import com.mapletct.ftmes.bpi.domain.VersionComparisonView;
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

    @GetMapping("/bpi/v1/topologies/{topologyId}/compare")
    public ApiResponse<VersionComparisonView> compareTopologies(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID topologyId,
            @RequestParam UUID against,
            HttpServletRequest request) {
        return ApiResponse.of(
                ruleService.compareTopologies(actorContextFactory.from(jwt), topologyId, against), request);
    }

    @PostMapping("/bpi/v1/topologies/drafts")
    @PreAuthorize("hasAnyRole('BPI_ENGINEER', 'BPI_ADMIN')")
    public ResponseEntity<ApiResponse<TopologyVersionView>> createTopologyDraft(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @Valid @RequestBody TopologyDraftCommand command,
            HttpServletRequest request) {
        CommandResult<TopologyVersionView> result = ruleService.createTopologyDraft(
                actorContextFactory.from(jwt), idempotencyKey, ifMatch, command, traceId(request));
        return ok(result, request);
    }

    @PostMapping("/bpi/v1/topologies/{topologyId}/validate")
    @PreAuthorize("hasAnyRole('BPI_ENGINEER', 'BPI_ADMIN')")
    public ResponseEntity<ApiResponse<TopologyVersionView>> validateTopology(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID topologyId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @Valid @RequestBody ReasonCommand command,
            HttpServletRequest request) {
        CommandResult<TopologyVersionView> result = ruleService.validateTopology(
                actorContextFactory.from(jwt), topologyId, idempotencyKey, ifMatch, command, traceId(request));
        return ok(result, request);
    }

    @PostMapping("/bpi/v1/topologies/{topologyId}/publish")
    @PreAuthorize("hasRole('BPI_ADMIN')")
    public ResponseEntity<ApiResponse<TopologyVersionView>> publishTopology(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID topologyId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @Valid @RequestBody ReasonCommand command,
            HttpServletRequest request) {
        CommandResult<TopologyVersionView> result = ruleService.publishTopology(
                actorContextFactory.from(jwt), topologyId, idempotencyKey, ifMatch, command, traceId(request));
        return ok(result, request);
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

    @GetMapping("/bpi/v1/rules/{ruleId}/compare")
    public ApiResponse<VersionComparisonView> compareRules(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID ruleId,
            @RequestParam UUID against,
            HttpServletRequest request) {
        return ApiResponse.of(
                ruleService.compareRules(actorContextFactory.from(jwt), ruleId, against), request);
    }

    @PostMapping("/bpi/v1/rules/drafts")
    @PreAuthorize("hasAnyRole('BPI_ENGINEER', 'BPI_ADMIN')")
    public ResponseEntity<ApiResponse<RuleVersionView>> createRuleDraft(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @Valid @RequestBody RuleDraftCommand command,
            HttpServletRequest request) {
        CommandResult<RuleVersionView> result = ruleService.createRuleDraft(
                actorContextFactory.from(jwt), idempotencyKey, ifMatch, command, traceId(request));
        return ok(result, request);
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
    @PreAuthorize("hasRole('BPI_ADMIN')")
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

    @PostMapping("/bpi/v1/rules/{ruleId}/submit-approval")
    @PreAuthorize("hasAnyRole('BPI_ENGINEER', 'BPI_ADMIN')")
    public ResponseEntity<ApiResponse<RuleVersionView>> submitApproval(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID ruleId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @Valid @RequestBody RulePublishCommand command,
            HttpServletRequest request) {
        CommandResult<RuleVersionView> result = ruleService.submitApproval(
                actorContextFactory.from(jwt), ruleId, idempotencyKey, ifMatch, command, traceId(request));
        return ok(result, request);
    }

    @PostMapping("/bpi/v1/rules/{ruleId}/reject-approval")
    @PreAuthorize("hasRole('BPI_ADMIN')")
    public ResponseEntity<ApiResponse<RuleVersionView>> rejectApproval(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID ruleId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @Valid @RequestBody ReasonCommand command,
            HttpServletRequest request) {
        CommandResult<RuleVersionView> result = ruleService.rejectApproval(
                actorContextFactory.from(jwt), ruleId, idempotencyKey, ifMatch, command, traceId(request));
        return ok(result, request);
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

    private <T> ResponseEntity<ApiResponse<T>> ok(
            CommandResult<T> result, HttpServletRequest request) {
        ResponseEntity.BodyBuilder response = ResponseEntity.ok();
        if (result.replayed()) response.header("Idempotent-Replay", "true");
        return response.body(ApiResponse.of(result.data(), request));
    }
}
