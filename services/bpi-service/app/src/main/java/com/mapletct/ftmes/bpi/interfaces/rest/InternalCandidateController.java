package com.mapletct.ftmes.bpi.interfaces.rest;

import com.mapletct.ftmes.bpi.application.ActorContext;
import com.mapletct.ftmes.bpi.application.ActorContextFactory;
import com.mapletct.ftmes.bpi.application.CandidateIngestionService;
import com.mapletct.ftmes.bpi.domain.BatchCandidate;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InternalCandidateController {
    private final ActorContextFactory actorContextFactory;
    private final CandidateIngestionService ingestionService;

    public InternalCandidateController(
            ActorContextFactory actorContextFactory,
            CandidateIngestionService ingestionService) {
        this.actorContextFactory = actorContextFactory;
        this.ingestionService = ingestionService;
    }

    @PostMapping("/internal/bpi/v1/candidates")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('BPI_EVENT_INGEST', 'BPI_ADMIN')")
    public ApiResponse<BatchCandidate> ingest(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CandidateIngestRequest request,
            HttpServletRequest servletRequest) {
        ActorContext actor = actorContextFactory.from(jwt);
        return ApiResponse.of(ingestionService.ingest(actor, request), servletRequest);
    }
}
