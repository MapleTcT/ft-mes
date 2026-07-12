package com.mapletct.ftmes.bpi.interfaces.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.mapletct.ftmes.bpi.application.ActorContext;
import com.mapletct.ftmes.bpi.application.ActorContextFactory;
import com.mapletct.ftmes.bpi.application.TelemetryIngestionService;
import com.mapletct.ftmes.bpi.domain.TelemetryIngestResult;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InternalTelemetryController {

    private final TelemetryIngestionService service;
    private final ActorContextFactory actorContextFactory;

    public InternalTelemetryController(TelemetryIngestionService service, ActorContextFactory actorContextFactory) {
        this.service = service;
        this.actorContextFactory = actorContextFactory;
    }

    @PostMapping(path = "/internal/bpi/v1/telemetry", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<TelemetryIngestResult>> ingest(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody JsonNode payload,
            HttpServletRequest request) {
        ActorContext actor = actorContextFactory.from(jwt);
        String traceId = String.valueOf(request.getAttribute(TraceIdFilter.ATTRIBUTE));
        TelemetryIngestResult result = service.ingest(actor, payload, traceId);
        HttpStatus status = result.replay() ? HttpStatus.OK
                : "QUARANTINED".equals(result.status()) ? HttpStatus.ACCEPTED : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(ApiResponse.of(result, request));
    }
}
