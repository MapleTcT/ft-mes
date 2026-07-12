package com.mapletct.ftmes.bpiadapter;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice
public class AdapterErrorHandler {

    @ExceptionHandler(AdapterAccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> denied(AdapterAccessDeniedException error, HttpServletRequest request) {
        Map<String, Object> problem = new LinkedHashMap<String, Object>();
        problem.put("type", "urn:ft-mes:bpi:adapter-access-denied");
        problem.put("title", "BPI access denied");
        problem.put("status", 403);
        problem.put("detail", error.getMessage());
        problem.put("instance", request.getRequestURI());
        problem.put("timestamp", Instant.now().toString());
        problem.put("traceId", UUID.randomUUID().toString());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem);
    }
}
