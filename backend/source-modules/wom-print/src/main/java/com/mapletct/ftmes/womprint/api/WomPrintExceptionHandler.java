package com.mapletct.ftmes.womprint.api;

import com.mapletct.ftmes.womprint.domain.WomPrintBusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class WomPrintExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(WomPrintExceptionHandler.class);

    @ExceptionHandler(WomPrintBusinessException.class)
    public ResponseEntity<LegacyResult<Object>> handleBusiness(WomPrintBusinessException exception) {
        return ResponseEntity.ok(LegacyResult.failure(exception.getCode(), exception.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<LegacyResult<Object>> handleUnexpected(Exception exception) {
        LOGGER.error("wom-print unexpected error", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(LegacyResult.failure(500, "二维码服务发生系统错误"));
    }
}
