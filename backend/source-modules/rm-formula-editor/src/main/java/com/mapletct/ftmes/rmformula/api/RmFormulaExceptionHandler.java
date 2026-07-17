package com.mapletct.ftmes.rmformula.api;

import com.mapletct.ftmes.rmformula.domain.RmFormulaBusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class RmFormulaExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(RmFormulaExceptionHandler.class);

    @ExceptionHandler(RmFormulaBusinessException.class)
    public ResponseEntity<LegacyResult<Object>> handleBusiness(RmFormulaBusinessException exception) {
        return ResponseEntity.ok(LegacyResult.failure(exception.getCode(), exception.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<LegacyResult<Object>> handleUnexpected(Exception exception) {
        LOGGER.error("rm-formula-editor unexpected error", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(LegacyResult.failure(500, "配方编辑服务异常"));
    }
}
