package com.mapletct.ftmes.processanalysis.api;

import com.mapletct.ftmes.processanalysis.domain.ProcessAnalysisBusinessException;
import com.mapletct.ftmes.processanalysis.domain.SnapshotType;
import com.mapletct.ftmes.processanalysis.service.TraceabilityService;
import com.mapletct.ftmes.processanalysis.support.TenantResolver;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping({"", "/ProcessAnalysis"})
public class ProcessAnalysisController {

    private final TraceabilityService traceabilityService;
    private final TenantResolver tenantResolver;

    public ProcessAnalysisController(TraceabilityService traceabilityService, TenantResolver tenantResolver) {
        this.traceabilityService = traceabilityService;
        this.tenantResolver = tenantResolver;
    }

    @GetMapping("/analysisParam/analysisParam/isProdprocessView")
    public LegacyResult<Map<String, Object>> availability(@RequestParam("batchNo") String batchNo) {
        return LegacyResult.success(traceabilityService.availability(batchNo));
    }

    @GetMapping(value = {
        "/processAnalysis/exelogSecond/processBatchViewOut",
        "/processAnalysis/exelogSecond/processView"
    }, produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<Resource> tracePage() {
        return ResponseEntity.ok()
            .contentType(new MediaType("text", "html", StandardCharsets.UTF_8))
            .body(new ClassPathResource("static/process-batch-trace.html"));
    }

    @GetMapping(value = "/processAnalysis/processExecution/detail", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<Resource> processExecutionDetailPage() {
        return ResponseEntity.ok()
            .contentType(new MediaType("text", "html", StandardCharsets.UTF_8))
            .body(new ClassPathResource("static/process-execution-detail.html"));
    }

    @GetMapping("/processAnalysis/api/trace")
    public LegacyResult<Map<String, Object>> trace(
            HttpServletRequest request,
            @RequestParam("batchNo") String batchNo,
            @RequestParam(value = "productNo", required = false, defaultValue = "") String productNo) {
        return LegacyResult.success(traceabilityService.trace(tenantResolver.resolve(request), batchNo, productNo));
    }

    @GetMapping("/processAnalysis/api/process-executions/{processExecutionId}")
    public LegacyResult<Map<String, Object>> processExecutionDetail(
            HttpServletRequest request,
            @PathVariable("processExecutionId") long processExecutionId) {
        return LegacyResult.success(traceabilityService.processExecutionDetail(
            tenantResolver.resolve(request), processExecutionId));
    }

    @GetMapping("/paramDetail/paramDetail/analysisiTask")
    public LegacyResult<Map<String, Object>> analyzeTask(
            HttpServletRequest request,
            @RequestParam(value = "taskExeLogId", defaultValue = "0") long taskExecutionId) {
        try {
            return LegacyResult.success(traceabilityService.analyzeTask(
                tenantResolver.resolve(request), taskExecutionId));
        } catch (ProcessAnalysisBusinessException exception) {
            Map<String, Object> data = new LinkedHashMap<String, Object>();
            data.put("success", false);
            data.put("message", exception.getMessage());
            return LegacyResult.of(exception.getCode(), data, exception.getMessage());
        }
    }

    @GetMapping("/paramStatRec/paramStatRec/manualStatProcess")
    public LegacyResult<Map<String, Object>> analyzeProcess(
            HttpServletRequest request,
            @RequestParam("processId") long processExecutionId) {
        return LegacyResult.success(traceabilityService.analyzeProcess(
            tenantResolver.resolve(request), processExecutionId));
    }

    @GetMapping("/paramStatRec/paramStatRec/manualStatActive")
    public LegacyResult<Map<String, Object>> analyzeActivity(
            HttpServletRequest request,
            @RequestParam("activeId") long activityExecutionId) {
        return LegacyResult.success(traceabilityService.analyzeActivity(
            tenantResolver.resolve(request), activityExecutionId));
    }

    @PostMapping("/produceTask/paPrExeLog/paPrExeLogList-query")
    public LegacyResult<Map<String, Object>> processSnapshots(
            HttpServletRequest request,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "50") int size) {
        return LegacyResult.success(traceabilityService.listSnapshots(
            tenantResolver.resolve(request), SnapshotType.PROCESS, page, size));
    }

    @PostMapping("/produceTask/paActiExeLog/paActiExeLogList-query")
    public LegacyResult<Map<String, Object>> activitySnapshots(
            HttpServletRequest request,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "50") int size) {
        return LegacyResult.success(traceabilityService.listSnapshots(
            tenantResolver.resolve(request), SnapshotType.ACTIVITY, page, size));
    }
}
