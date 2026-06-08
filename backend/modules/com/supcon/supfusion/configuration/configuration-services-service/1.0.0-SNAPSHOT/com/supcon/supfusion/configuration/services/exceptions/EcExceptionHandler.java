package com.supcon.supfusion.configuration.services.exceptions;


import com.supcon.supfusion.base.services.InternationalService;
import com.supcon.supfusion.base.utils.ProjectFlagHolder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
@RestControllerAdvice
public class EcExceptionHandler {

    @Autowired
    private InternationalService internationalService;

    @ExceptionHandler(EcException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map handleError(EcException e) {
        ProjectFlagHolder.getInstance().getProjFlag().set(false);
        log.error("业务异常", e);
        String message = null;
        EcException.Code code = e.getCode();
        if (code != null) {
            message = internationalService.getI18nValue(code.toMessageKey(),e.getArgs());
            if (StringUtils.isEmpty(message)) {
                message = code.toMessageKey();
            }
        } else if (!StringUtils.isEmpty(e.getMessageKey())) {
        	message = internationalService.getI18nValue(e.getMessageKey(), e.getArgs());
        }
        Map map = new HashMap();
        map.put("actionErrors", new Object[]{});
        map.put("items", new Object[]{});
        map.put("success", false);
        map.put("exceptionMsg", StringUtils.isEmpty(message) ? e.getMessage() : message);
        return map;
    }

}