package com.mapletct.ftmes.bpi.interfaces.rest;

import com.mapletct.ftmes.bpi.application.ActorContextFactory;
import com.mapletct.ftmes.bpi.application.ProcessEvidenceService;
import com.mapletct.ftmes.bpi.domain.ProcessEvidenceView;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@PreAuthorize("hasAnyRole('BPI_VIEWER', 'BPI_SHIFT_LEAD', 'BPI_ENGINEER', 'BPI_ADMIN')")
public class ProcessEvidenceController {

    private final ActorContextFactory actorContextFactory;
    private final ProcessEvidenceService service;

    public ProcessEvidenceController(
            ActorContextFactory actorContextFactory,
            ProcessEvidenceService service) {
        this.actorContextFactory = actorContextFactory;
        this.service = service;
    }

    @GetMapping("/bpi/v1/process-evidence")
    public ApiResponse<ProcessEvidenceView> processEvidence(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam String plantId,
            @RequestParam String lineId,
            @RequestParam String orderId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(name = "property", required = false) List<String> properties,
            HttpServletRequest request) {
        return ApiResponse.of(
                service.get(
                        actorContextFactory.from(jwt),
                        plantId,
                        lineId,
                        orderId,
                        from,
                        to,
                        properties),
                request);
    }
}
