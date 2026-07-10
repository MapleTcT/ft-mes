package com.mapletct.ftmes.materialwms.api;

import com.mapletct.ftmes.materialwms.domain.MaterialWmsBusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class MaterialWmsExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(MaterialWmsExceptionHandler.class);

    @ExceptionHandler(MaterialWmsBusinessException.class)
    public ResponseEntity<LegacyResult<Object>> handleBusiness(MaterialWmsBusinessException exception) {
        return ResponseEntity.ok(LegacyResult.failure(exception.getCode(), exception.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<LegacyResult<Object>> handleUnexpected(Exception exception) {
        LOGGER.error("material-wms unexpected error", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(LegacyResult.failure(500, "material-wms 系统错误"));
    }
}
