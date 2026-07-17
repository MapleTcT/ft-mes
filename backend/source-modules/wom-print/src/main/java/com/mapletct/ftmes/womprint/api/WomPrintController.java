package com.mapletct.ftmes.womprint.api;

import com.mapletct.ftmes.womprint.domain.GenerationResult;
import com.mapletct.ftmes.womprint.service.WomQrCodeService;
import com.mapletct.ftmes.womprint.support.TenantResolver;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping({"/msService/WOM/printManage", "/WOM/printManage"})
public class WomPrintController {

    private final WomQrCodeService service;
    private final TenantResolver tenantResolver;

    public WomPrintController(WomQrCodeService service, TenantResolver tenantResolver) {
        this.service = service;
        this.tenantResolver = tenantResolver;
    }

    @GetMapping(value = "/printDate/generateCode", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<Resource> generatePage() {
        MediaType htmlUtf8 = new MediaType("text", "html", StandardCharsets.UTF_8);
        return ResponseEntity.ok()
            .contentType(htmlUtf8)
            .cacheControl(CacheControl.noStore())
            .body(new ClassPathResource("static/wom-qr-generate.html"));
    }

    @PostMapping(value = "/generateQrCode", consumes = MediaType.APPLICATION_JSON_VALUE)
    public LegacyResult<List<String>> generate(
            HttpServletRequest servletRequest,
            @RequestBody GenerateQrCodeRequest request) {
        GenerationResult result = service.generate(tenantResolver.resolve(servletRequest), request);
        return LegacyResult.success(result.getDetails());
    }

    @PostMapping(value = "/backfill-printInfo", consumes = MediaType.APPLICATION_JSON_VALUE)
    public LegacyResult<Map<String, Object>> backfill(
            HttpServletRequest servletRequest,
            @RequestBody List<PrintBackfillRequest> requests) {
        return LegacyResult.success(service.backfill(tenantResolver.resolve(servletRequest), requests));
    }

    @GetMapping("/taskContext")
    public LegacyResult<Map<String, Object>> taskContext(@RequestParam("taskId") long taskId) {
        return LegacyResult.success(service.taskContext(taskId));
    }

    @GetMapping("/calculateTermOfValidity")
    public LegacyResult<String> calculateTermOfValidity(
            @RequestParam("taskId") long taskId,
            @RequestParam(value = "manuDate", required = false) String manufactureDate) {
        return LegacyResult.success(service.calculateTermOfValidity(taskId, manufactureDate));
    }

    @GetMapping("/printers")
    public LegacyResult<List<Map<String, Object>>> printers() {
        return LegacyResult.success(service.printers());
    }

    @GetMapping("/getPrintByLineId")
    public LegacyResult<Map<String, Object>> printerForLine(@RequestParam("lineId") String lineId) {
        return LegacyResult.success(service.printerForLine(lineId));
    }

    @GetMapping("/records")
    public LegacyResult<List<Map<String, Object>>> records(
            HttpServletRequest servletRequest,
            @RequestParam("taskId") long taskId,
            @RequestParam(value = "limit", defaultValue = "100") int limit) {
        return LegacyResult.success(service.records(tenantResolver.resolve(servletRequest), taskId, limit));
    }

    @GetMapping(value = "/qrcode/{qrCode}.png", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> qrCodeImage(
            HttpServletRequest servletRequest,
            @PathVariable("qrCode") String qrCode,
            @RequestParam(value = "size", defaultValue = "320") int size) {
        byte[] image = service.renderQrCode(tenantResolver.resolve(servletRequest), qrCode, size);
        return ResponseEntity.ok()
            .contentType(MediaType.IMAGE_PNG)
            .cacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES).cachePrivate())
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + qrCode + ".png\"")
            .body(image);
    }
}
