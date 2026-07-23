package com.mapletct.ftmes.bpi.interfaces.rest;

import com.mapletct.ftmes.bpi.application.ActorContextFactory;
import com.mapletct.ftmes.bpi.application.OverviewService;
import com.mapletct.ftmes.bpi.domain.LineLiveEvidence;
import com.mapletct.ftmes.bpi.domain.LineState;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@PreAuthorize("hasAnyRole('BPI_VIEWER', 'BPI_SHIFT_LEAD', 'BPI_ENGINEER', 'BPI_ADMIN')")
public class OverviewController {
    private final ActorContextFactory actorContextFactory;
    private final OverviewService overviewService;

    public OverviewController(ActorContextFactory actorContextFactory, OverviewService overviewService) {
        this.actorContextFactory = actorContextFactory;
        this.overviewService = overviewService;
    }

    @GetMapping("/bpi/v1/overview")
    public ApiResponse<List<LineState>> overview(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam String plantId,
            @RequestParam(defaultValue = "false") boolean onlyAbnormal,
            HttpServletRequest request) {
        return ApiResponse.of(
                overviewService.overview(actorContextFactory.from(jwt), plantId, onlyAbnormal), request);
    }

    @GetMapping("/bpi/v1/lines/{lineId}/current-state")
    public ApiResponse<LineState> current(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String lineId,
            @RequestParam(required = false) String plantId,
            HttpServletRequest request) {
        return ApiResponse.of(
                overviewService.current(actorContextFactory.from(jwt), plantId, lineId),
                request);
    }

    @GetMapping("/bpi/v1/lines/{lineId}/live-evidence")
    public ApiResponse<LineLiveEvidence> liveEvidence(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String lineId,
            @RequestParam String plantId,
            @RequestParam(required = false) Integer windowMinutes,
            @RequestParam(required = false) Integer limit,
            HttpServletRequest request) {
        return ApiResponse.of(
                overviewService.liveEvidence(
                        actorContextFactory.from(jwt),
                        plantId,
                        lineId,
                        windowMinutes,
                        limit),
                request);
    }
}
