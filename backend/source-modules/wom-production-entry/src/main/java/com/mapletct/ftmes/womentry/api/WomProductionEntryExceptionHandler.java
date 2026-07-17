package com.mapletct.ftmes.womentry.api;

import com.mapletct.ftmes.womentry.domain.WomEntryBusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = WomProductionEntryController.class)
public class WomProductionEntryExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(WomProductionEntryExceptionHandler.class);

    @ExceptionHandler(WomEntryBusinessException.class)
    public LegacyResult<Object> handleBusiness(WomEntryBusinessException exception) {
        if (exception.getCode() >= 500) {
            LOGGER.warn("WOM production entry failed: {}", exception.getMessage());
        }
        return LegacyResult.failure(exception.getCode(), exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public LegacyResult<Object> handleUnexpected(Exception exception) {
        LOGGER.error("Unexpected WOM production entry failure", exception);
        return LegacyResult.failure(500, "制造指令创建服务异常");
    }
}
