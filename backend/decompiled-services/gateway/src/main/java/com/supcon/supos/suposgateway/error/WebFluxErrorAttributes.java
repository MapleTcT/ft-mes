/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.beans.factory.annotation.Value
 *  org.springframework.boot.web.reactive.error.DefaultErrorAttributes
 *  org.springframework.core.annotation.AnnotatedElementUtils
 *  org.springframework.http.HttpStatus
 *  org.springframework.web.bind.annotation.ResponseStatus
 *  org.springframework.web.reactive.function.server.ServerRequest
 *  org.springframework.web.server.ResponseStatusException
 *  org.springframework.web.server.ServerWebExchange
 *  reactor.netty.http.client.PrematureCloseException
 */
package com.supcon.supos.suposgateway.error;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.netty.http.client.PrematureCloseException;

public class WebFluxErrorAttributes
extends DefaultErrorAttributes {
    private static final Logger LOGGER = LoggerFactory.getLogger(WebFluxErrorAttributes.class);
    private static final String ERROR_ATTRIBUTE = WebFluxErrorAttributes.class.getName() + ".ERROR";
    @Value(value="${spring.application.name:}")
    private String serviceName;

    public Map<String, Object> getErrorAttributes(ServerRequest request, boolean includeStackTrace) {
        LinkedHashMap<String, Object> errorAttributes = new LinkedHashMap<String, Object>();
        errorAttributes.put("timestamp", new Date());
        errorAttributes.put("path", request.path());
        Throwable error = this.getError(request);
        String message = error.getMessage();
        if (error instanceof ResponseStatusException) {
            message = ((ResponseStatusException)error).getReason();
        }
        HttpStatus errorStatus = this.determineHttpStatus(error);
        errorAttributes.put("status", errorStatus.value());
        errorAttributes.put("error", errorStatus.getReasonPhrase());
        errorAttributes.put("message", message);
        errorAttributes.put("server", this.serviceName);
        LOGGER.error(request.exchange().getLogPrefix() + this.formatError(error, request));
        return errorAttributes;
    }

    private String formatError(Throwable ex, ServerRequest request) {
        String reason = ex.getClass().getSimpleName() + ": " + ex.getMessage();
        return "Resolved [" + reason + "] for HTTP " + request.methodName() + " " + request.path();
    }

    private HttpStatus determineHttpStatus(Throwable error) {
        if (error instanceof ResponseStatusException) {
            return ((ResponseStatusException)error).getStatus();
        }
        if (error instanceof PrematureCloseException) {
            return HttpStatus.SERVICE_UNAVAILABLE;
        }
        ResponseStatus responseStatus = (ResponseStatus)AnnotatedElementUtils.findMergedAnnotation(error.getClass(), ResponseStatus.class);
        if (responseStatus != null) {
            return responseStatus.code();
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    public Throwable getError(ServerRequest request) {
        return (Throwable)request.attribute(ERROR_ATTRIBUTE).orElseThrow(() -> new IllegalStateException("Missing exception attribute in ServerWebExchange"));
    }

    public void storeErrorInformation(Throwable error, ServerWebExchange exchange) {
        exchange.getAttributes().putIfAbsent(ERROR_ATTRIBUTE, error);
    }
}

