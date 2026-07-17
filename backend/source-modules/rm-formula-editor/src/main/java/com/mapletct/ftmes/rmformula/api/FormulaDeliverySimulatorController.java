package com.mapletct.ftmes.rmformula.api;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FormulaDeliverySimulatorController {
    private final boolean enabled;
    private final String secret;
    private final boolean failFirst;
    private final ConcurrentHashMap<String, Integer> attempts = new ConcurrentHashMap<String, Integer>();

    public FormulaDeliverySimulatorController(
            @Value("${rm-formula-editor.simulator-enabled:false}") boolean enabled,
            @Value("${rm-formula-editor.simulator-secret:disabled}") String secret,
            @Value("${rm-formula-editor.simulator-fail-first:false}") boolean failFirst) {
        this.enabled = enabled;
        this.secret = secret;
        this.failFirst = failFirst;
    }

    @PostMapping("/internal/test/batch-dcs")
    public ResponseEntity<Map<String, Object>> accept(
            @RequestHeader(value = "X-ADP-Simulator-Secret", required = false) String suppliedSecret,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String requestId,
            @RequestBody Map<String, Object> payload) {
        if (!enabled) {
            return ResponseEntity.notFound().build();
        }
        if (secret == null || !secret.equals(suppliedSecret)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        String key = requestId == null ? "missing" : requestId;
        int attempt = attempts.merge(key, 1, Integer::sum);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("idempotencyKey", key);
        result.put("attempt", attempt);
        result.put("contractVersion", payload.get("contractVersion"));
        if (failFirst && attempt == 1) {
            result.put("accepted", false);
            result.put("status", "RETRY_REQUIRED");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(result);
        }
        result.put("accepted", true);
        result.put("status", "ACKNOWLEDGED");
        return ResponseEntity.ok(result);
    }
}
