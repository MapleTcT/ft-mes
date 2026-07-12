package com.mapletct.ftmes.bpi.interfaces.rest;

import com.google.protobuf.InvalidProtocolBufferException;
import com.mapletct.ftmes.bpi.application.ActorContext;
import com.mapletct.ftmes.bpi.application.ActorContextFactory;
import com.mapletct.ftmes.bpi.application.CandidateEventMapper;
import com.mapletct.ftmes.bpi.application.CandidateIngestionService;
import com.mapletct.ftmes.bpi.application.error.BpiForbiddenException;
import com.mapletct.ftmes.bpi.application.error.BpiValidationException;
import com.mapletct.ftmes.bpi.contract.v1.BatchCandidateV1;
import com.mapletct.ftmes.bpi.domain.BatchCandidate;
import com.mapletct.ftmes.bpi.infrastructure.candidate.BpiCandidateEventProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InternalCandidateEventController {
    static final String PROTOBUF_MEDIA_TYPE = "application/x-protobuf";

    private final ActorContextFactory actorContextFactory;
    private final CandidateEventMapper eventMapper;
    private final CandidateIngestionService ingestionService;
    private final BpiCandidateEventProperties properties;

    public InternalCandidateEventController(
            ActorContextFactory actorContextFactory,
            CandidateEventMapper eventMapper,
            CandidateIngestionService ingestionService,
            BpiCandidateEventProperties properties) {
        this.actorContextFactory = actorContextFactory;
        this.eventMapper = eventMapper;
        this.ingestionService = ingestionService;
        this.properties = properties;
    }

    @PostMapping(path = "/internal/bpi/v1/candidate-events", consumes = PROTOBUF_MEDIA_TYPE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('BPI_EVENT_INGEST', 'BPI_ADMIN')")
    public ApiResponse<BatchCandidate> ingest(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody byte[] payload,
            HttpServletRequest servletRequest) {
        if (!properties.protobufHttpIngressEnabled()) {
            throw new BpiForbiddenException("Candidate Protobuf HTTP ingress is disabled.");
        }
        if (payload.length == 0 || payload.length > properties.maxPayloadBytes()) {
            throw new BpiValidationException("Candidate event payload size is invalid.");
        }
        BatchCandidateV1 event;
        try {
            event = BatchCandidateV1.parseFrom(payload);
        } catch (InvalidProtocolBufferException error) {
            throw new BpiValidationException("Candidate event is not valid BatchCandidateV1 Protobuf.");
        }
        ActorContext actor = actorContextFactory.from(jwt);
        return ApiResponse.of(
                ingestionService.ingest(actor, eventMapper.toRequest(actor, event)),
                servletRequest);
    }
}
