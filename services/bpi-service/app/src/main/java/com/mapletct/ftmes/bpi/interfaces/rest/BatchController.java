package com.mapletct.ftmes.bpi.interfaces.rest;

import com.mapletct.ftmes.bpi.application.ActorContext;
import com.mapletct.ftmes.bpi.application.ActorContextFactory;
import com.mapletct.ftmes.bpi.application.BatchQueryService;
import com.mapletct.ftmes.bpi.domain.BatchInstance;
import com.mapletct.ftmes.bpi.domain.BatchStateEvent;
import com.mapletct.ftmes.bpi.domain.EvidenceView;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    public BatchController(ActorContextFactory actorContextFactory, BatchQueryService batchQueryService) {
        this.actorContextFactory = actorContextFactory;
        this.batchQueryService = batchQueryService;
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
}
