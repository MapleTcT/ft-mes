package com.mapletct.ftmes.processanalysis.api;

import com.mapletct.ftmes.processanalysis.domain.ProcessAnalysisBusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ProcessAnalysisExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessAnalysisExceptionHandler.class);

    @ExceptionHandler(ProcessAnalysisBusinessException.class)
    public ResponseEntity<LegacyResult<Object>> handleBusiness(ProcessAnalysisBusinessException exception) {
        return ResponseEntity.ok(LegacyResult.failure(exception.getCode(), exception.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<LegacyResult<Object>> handleUnexpected(Exception exception) {
        LOGGER.error("process-analysis unexpected error", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(LegacyResult.failure(500, "生产追溯系统错误"));
    }
}
