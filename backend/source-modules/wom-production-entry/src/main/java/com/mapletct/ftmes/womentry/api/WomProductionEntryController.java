package com.mapletct.ftmes.womentry.api;

import com.mapletct.ftmes.womentry.domain.CreateInstructionResult;
import com.mapletct.ftmes.womentry.domain.ProductionOption;
import com.mapletct.ftmes.womentry.domain.TaskResult;
import com.mapletct.ftmes.womentry.service.WomProductionEntryService;
import com.mapletct.ftmes.womentry.support.RequestAuthContext;
import com.mapletct.ftmes.womentry.support.TenantResolver;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/msService/WOM/produceTask/manual-entry")
public class WomProductionEntryController {

    private final WomProductionEntryService service;
    private final TenantResolver tenantResolver;

    public WomProductionEntryController(WomProductionEntryService service, TenantResolver tenantResolver) {
        this.service = service;
        this.tenantResolver = tenantResolver;
    }

    @GetMapping(value = "/page", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<Resource> page() {
        return ResponseEntity.ok()
            .contentType(new MediaType("text", "html", StandardCharsets.UTF_8))
            .cacheControl(CacheControl.noStore())
            .body(new ClassPathResource("static/wom-manual-task-create.html"));
    }

    @GetMapping("/options")
    public LegacyResult<List<ProductionOption>> options(
            @RequestParam(value = "keyword", defaultValue = "") String keyword,
            @RequestParam(value = "limit", defaultValue = "100") int limit) {
        return LegacyResult.success(service.options(keyword, limit));
    }

    @GetMapping("/result")
    public LegacyResult<TaskResult> result(@RequestParam("batchCode") String batchCode) {
        return LegacyResult.success(service.result(batchCode));
    }

    @PostMapping(value = "/create", consumes = MediaType.APPLICATION_JSON_VALUE)
    public LegacyResult<CreateInstructionResult> create(
            HttpServletRequest servletRequest,
            @RequestBody CreateInstructionRequest request) {
        String tenantId = tenantResolver.resolve(servletRequest);
        RequestAuthContext authContext = new RequestAuthContext(
            servletRequest.getHeader(HttpHeaders.AUTHORIZATION),
            servletRequest.getHeader(HttpHeaders.COOKIE),
            tenantId,
            servletRequest.getHeader(HttpHeaders.ACCEPT_LANGUAGE)
        );
        return LegacyResult.success(service.create(tenantId, request, authContext));
    }
}
