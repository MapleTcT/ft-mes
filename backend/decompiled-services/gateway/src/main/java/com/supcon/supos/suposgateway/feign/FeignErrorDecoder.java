/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  feign.FeignException
 *  feign.Response
 *  feign.codec.ErrorDecoder$Default
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.http.HttpStatus
 *  org.springframework.stereotype.Component
 */
package com.supcon.supos.suposgateway.feign;

import com.supcon.supos.suposgateway.exception.GatewayResponseStatusException;
import feign.FeignException;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class FeignErrorDecoder
extends ErrorDecoder.Default {
    private static final Logger LOGGER = LoggerFactory.getLogger(FeignException.class);

    public Exception decode(String methodKey, Response response) {
        Exception e = super.decode(methodKey, response);
        if (e instanceof FeignException) {
            LOGGER.error("Feign Request Error, HttpStatus {}, Response {}", (Object)response.status(), (Object)((FeignException)e).contentUTF8());
        }
        if (HttpStatus.UNAUTHORIZED.value() == response.status()) {
            return new GatewayResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage(), e);
        }
        return new GatewayResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage(), e);
    }
}

