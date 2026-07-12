package com.mapletct.ftmes.bpi.interfaces.rest;

import com.mapletct.ftmes.bpi.application.ActorContext;
import com.mapletct.ftmes.bpi.application.ActorContextFactory;
import com.mapletct.ftmes.bpi.application.CandidateService;
import com.mapletct.ftmes.bpi.application.CommandResult;
import com.mapletct.ftmes.bpi.domain.BatchCandidate;
import com.mapletct.ftmes.bpi.domain.CandidateConfirmation;
import com.mapletct.ftmes.bpi.domain.CandidateState;
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
public class CandidateController {
    private final ActorContextFactory actorContextFactory;
    private final CandidateService candidateService;

    public CandidateController(ActorContextFactory actorContextFactory, CandidateService candidateService) {
        this.actorContextFactory = actorContextFactory;
        this.candidateService = candidateService;
    }

    @GetMapping("/bpi/v1/candidates")
    @PreAuthorize("hasAnyRole('BPI_VIEWER', 'BPI_SHIFT_LEAD', 'BPI_ENGINEER', 'BPI_ADMIN')")
    public ApiResponse<List<BatchCandidate>> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam String plantId,
            @RequestParam(required = false) String lineId,
            @RequestParam(required = false) CandidateState state,
            @RequestParam(defaultValue = "50") int limit,
            HttpServletRequest request) {
        ActorContext actor = actorContextFactory.from(jwt);
        return ApiResponse.of(candidateService.list(actor, plantId, lineId, state, limit), request);
    }

    @GetMapping("/bpi/v1/candidates/{candidateId}")
    @PreAuthorize("hasAnyRole('BPI_VIEWER', 'BPI_SHIFT_LEAD', 'BPI_ENGINEER', 'BPI_ADMIN')")
    public ApiResponse<BatchCandidate> get(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID candidateId,
            HttpServletRequest request) {
        return ApiResponse.of(candidateService.get(actorContextFactory.from(jwt), candidateId), request);
    }

    @PostMapping("/bpi/v1/candidates/{candidateId}/confirm")
    @PreAuthorize("hasAnyRole('BPI_SHIFT_LEAD', 'BPI_ADMIN')")
    public ResponseEntity<ApiResponse<CandidateConfirmation>> confirm(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID candidateId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @Valid @RequestBody ReasonCommand command,
            HttpServletRequest request) {
        ActorContext actor = actorContextFactory.from(jwt);
        String traceId = String.valueOf(request.getAttribute(TraceIdFilter.ATTRIBUTE));
        CommandResult<CandidateConfirmation> result = candidateService.confirm(
                actor, candidateId, idempotencyKey, ifMatch, command, traceId);
        ResponseEntity.BodyBuilder response = ResponseEntity.ok();
        if (result.replayed()) {
            response.header("Idempotent-Replay", "true");
        }
        return response.body(ApiResponse.of(result.data(), request));
    }
}
