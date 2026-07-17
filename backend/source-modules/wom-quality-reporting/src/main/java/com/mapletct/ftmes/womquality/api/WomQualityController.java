package com.mapletct.ftmes.womquality.api;

import com.mapletct.ftmes.womquality.service.WomQualityService;
import com.mapletct.ftmes.womquality.support.RequestContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/msService")
public class WomQualityController {

    private final WomQualityService service;
    private final RequestContext requestContext;

    public WomQualityController(WomQualityService service, RequestContext requestContext) {
        this.service = service;
        this.requestContext = requestContext;
    }

    @GetMapping(value = "/WOM/quality-quantity/page", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<Resource> page() {
        return ResponseEntity.ok()
            .contentType(new MediaType("text", "html", StandardCharsets.UTF_8))
            .body(new ClassPathResource("static/wom-quality-quantity.html"));
    }

    @GetMapping("/WOM/quality-quantity/tasks")
    public LegacyResult<Map<String, Object>> tasks(
            HttpServletRequest request,
            @RequestParam(value = "keyword", defaultValue = "") String keyword,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "50") int size) {
        return LegacyResult.success(service.listTasks(
            requestContext.tenant(request), keyword, page, size));
    }

    @GetMapping("/WOM/quality-quantity/tasks/{taskId}/outputs")
    public LegacyResult<List<Map<String, Object>>> outputs(
            HttpServletRequest request,
            @PathVariable("taskId") String taskId) {
        return LegacyResult.success(service.listOutputs(requestContext.tenant(request), taskId));
    }

    @GetMapping("/WOM/quality-quantity/reports")
    public LegacyResult<Map<String, Object>> reports(
            HttpServletRequest request,
            @RequestParam(value = "taskId", defaultValue = "") String taskId,
            @RequestParam(value = "keyword", defaultValue = "") String keyword,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "50") int size) {
        return LegacyResult.success(service.listReports(
            requestContext.tenant(request), taskId, keyword, page, size));
    }

    @GetMapping("/WOM/quality-quantity/quality-context/{inspectId}")
    public LegacyResult<Map<String, Object>> qualityContext(
            @PathVariable("inspectId") String inspectId) {
        return LegacyResult.success(service.qualityContext(inspectId));
    }

    @GetMapping("/WOM/quality-quantity/reports/{reportId}")
    public LegacyResult<Map<String, Object>> detail(
            HttpServletRequest request,
            @PathVariable("reportId") String reportId) {
        return LegacyResult.success(service.detail(requestContext.tenant(request), reportId));
    }

    @PostMapping("/WOM/quality-quantity/reports")
    public LegacyResult<Map<String, Object>> create(
            HttpServletRequest request,
            @RequestBody BadQuantityCreateRequest body) {
        String tenantId = requestContext.tenant(request);
        String actor = requestContext.actor(request);
        Map<String, Object> created = service.create(tenantId, body, actor);
        return LegacyResult.success(service.synchronize(
            tenantId, String.valueOf(created.get("id")), actor));
    }

    @PostMapping("/WOM/quality-quantity/reports/{reportId}/reverse")
    public LegacyResult<Map<String, Object>> reverse(
            HttpServletRequest request,
            @PathVariable("reportId") String reportId,
            @RequestBody BadQuantityReverseRequest body) {
        String tenantId = requestContext.tenant(request);
        String actor = requestContext.actor(request);
        service.requestReversal(tenantId, reportId, body, actor);
        return LegacyResult.success(service.synchronize(tenantId, reportId, actor));
    }

    @PostMapping("/WOM/quality-quantity/reports/{reportId}/retry")
    public LegacyResult<Map<String, Object>> retry(
            HttpServletRequest request,
            @PathVariable("reportId") String reportId) {
        return LegacyResult.success(service.synchronize(
            requestContext.tenant(request), reportId, requestContext.actor(request)));
    }

    @PostMapping("/WOM/quality-quantity/reports/{reportId}/link-quality")
    public LegacyResult<Map<String, Object>> linkQuality(
            HttpServletRequest request,
            @PathVariable("reportId") String reportId,
            @RequestParam("version") long version) {
        return LegacyResult.success(service.linkLatestQuality(
            requestContext.tenant(request), reportId, version, requestContext.actor(request)));
    }
}
