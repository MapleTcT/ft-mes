/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  feign.RequestInterceptor
 *  feign.RequestTemplate
 *  javax.servlet.http.HttpServletRequest
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.stereotype.Component
 */
package com.supcon.orchid.entityconf.configuration;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class FeignRequestHeaderInterceptor
implements RequestInterceptor {
    private static final Logger log = LoggerFactory.getLogger(FeignRequestHeaderInterceptor.class);
    @Autowired
    private HttpServletRequest request;

    public void apply(RequestTemplate requestTemplate) {
        String header = null;
        try {
            header = this.request.getHeader("X-Tenant-Id");
        }
        catch (IllegalStateException illegalStateException) {
            // empty catch block
        }
        requestTemplate.header("X-Tenant-Id", new String[]{Optional.ofNullable(header).orElse("dt")});
    }
}

