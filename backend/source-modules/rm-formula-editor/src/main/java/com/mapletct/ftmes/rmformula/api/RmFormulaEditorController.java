package com.mapletct.ftmes.rmformula.api;

import com.mapletct.ftmes.rmformula.service.FormulaDeliveryService;
import com.mapletct.ftmes.rmformula.service.FormulaEditorService;
import com.mapletct.ftmes.rmformula.support.RequestContext;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/msService/RM/formula-editor", "/RM/formula-editor"})
public class RmFormulaEditorController {
    private final FormulaEditorService editorService;
    private final FormulaDeliveryService deliveryService;
    private final RequestContext requestContext;

    public RmFormulaEditorController(FormulaEditorService editorService,
                                     FormulaDeliveryService deliveryService,
                                     RequestContext requestContext) {
        this.editorService = editorService;
        this.deliveryService = deliveryService;
        this.requestContext = requestContext;
    }

    @GetMapping("/formulas")
    public LegacyResult<Map<String, Object>> formulas(HttpServletRequest request,
                                                       @RequestParam(value = "query", required = false) String query,
                                                       @RequestParam(value = "limit", required = false) Integer limit) {
        requestContext.requireAuthenticated(request);
        return LegacyResult.success(editorService.list(query, limit));
    }

    @GetMapping("/formulas/{formulaId}")
    public LegacyResult<Map<String, Object>> formula(HttpServletRequest request,
                                                      @PathVariable("formulaId") long formulaId) {
        requestContext.requireAuthenticated(request);
        return LegacyResult.success(editorService.detail(formulaId));
    }

    @GetMapping("/references/materials")
    public LegacyResult<Map<String, Object>> materials(HttpServletRequest request,
                                                        @RequestParam(value = "query", required = false) String query,
                                                        @RequestParam(value = "limit", required = false) Integer limit) {
        requestContext.requireAuthenticated(request);
        return LegacyResult.success(editorService.materials(query, limit));
    }

    @GetMapping("/references/batch-servers")
    public LegacyResult<Map<String, Object>> batchServers(HttpServletRequest request,
                                                           @RequestParam(value = "query", required = false) String query,
                                                           @RequestParam(value = "limit", required = false) Integer limit) {
        requestContext.requireAuthenticated(request);
        return LegacyResult.success(editorService.batchServers(query, limit));
    }

    @PostMapping("/formulas")
    public LegacyResult<Map<String, Object>> create(HttpServletRequest request,
                                                     @RequestBody FormulaSaveRequest body) {
        requestContext.requireAuthenticated(request);
        return LegacyResult.success(editorService.create(requestContext.tenant(), body));
    }

    @PutMapping("/formulas/{formulaId}")
    public LegacyResult<Map<String, Object>> update(HttpServletRequest request,
                                                     @PathVariable("formulaId") long formulaId,
                                                     @RequestBody FormulaSaveRequest body) {
        requestContext.requireAuthenticated(request);
        return LegacyResult.success(editorService.update(requestContext.tenant(), formulaId, body));
    }

    @PostMapping("/formulas/{formulaId}/deliveries")
    public LegacyResult<Map<String, Object>> publish(HttpServletRequest request,
                                                      @PathVariable("formulaId") long formulaId,
                                                      @RequestBody DeliveryRequest body) {
        requestContext.requireAuthenticated(request);
        return LegacyResult.success(deliveryService.publish(requestContext.tenant(), formulaId, body));
    }

    @GetMapping("/deliveries/{deliveryId}")
    public LegacyResult<Map<String, Object>> delivery(HttpServletRequest request,
                                                       @PathVariable("deliveryId") long deliveryId) {
        requestContext.requireAuthenticated(request);
        return LegacyResult.success(deliveryService.delivery(deliveryId));
    }

    @PostMapping("/deliveries/{deliveryId}/retry")
    public LegacyResult<Map<String, Object>> retry(HttpServletRequest request,
                                                    @PathVariable("deliveryId") long deliveryId) {
        requestContext.requireAuthenticated(request);
        return LegacyResult.success(deliveryService.retry(deliveryId));
    }
}
