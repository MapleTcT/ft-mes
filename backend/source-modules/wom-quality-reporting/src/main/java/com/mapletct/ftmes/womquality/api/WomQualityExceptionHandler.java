package com.mapletct.ftmes.womquality.api;

import com.mapletct.ftmes.womquality.domain.WomQualityBusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class WomQualityExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(WomQualityExceptionHandler.class);

    @ExceptionHandler(WomQualityBusinessException.class)
    public ResponseEntity<LegacyResult<Object>> handleBusiness(WomQualityBusinessException exception) {
        return ResponseEntity.ok(LegacyResult.failure(exception.getCode(), exception.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<LegacyResult<Object>> handleUnexpected(Exception exception) {
        LOGGER.error("wom-quality-reporting unexpected error", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(LegacyResult.failure(500, "不良数量登记系统错误"));
    }
}
