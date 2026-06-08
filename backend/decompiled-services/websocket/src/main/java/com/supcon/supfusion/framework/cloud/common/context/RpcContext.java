/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.servlet.http.HttpServletRequest
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package com.supcon.supfusion.framework.cloud.common.context;

import com.supcon.supfusion.framework.cloud.common.context.RpcContextHolder;
import java.io.Serializable;
import java.util.Locale;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RpcContext
implements Serializable {
    private static final long serialVersionUID = -1983093380352069023L;
    private static Logger logger = LoggerFactory.getLogger(RpcContext.class);
    private String id = UUID.randomUUID().toString().replaceAll("-", "");
    private HttpServletRequest request;
    private String tenantId;
    private Locale language;
    private Long traceId;
    private String fromServiceName = "unknown";
    private String extra;

    public static RpcContext getContext() {
        return RpcContextHolder.getContext();
    }

    public static void removeContext() {
        RpcContextHolder.removeContext();
    }

    public String getId() {
        return this.id;
    }

    public HttpServletRequest getRequest() {
        return this.request;
    }

    public void setRequest(HttpServletRequest request) {
        if (logger.isDebugEnabled()) {
            logger.info("=>[framework]set http request, context={}, url={}, thread={}", new Object[]{this.id, request.getRequestURI(), Thread.currentThread().getName() + '@' + Thread.currentThread().getId()});
        }
        this.request = request;
    }

    public String getTenantId() {
        return this.tenantId;
    }

    public void setTenantId(String tenantId) {
        if (logger.isDebugEnabled()) {
            logger.info("=>[framework]set tenant, id={}, context={}, thread={}", new Object[]{tenantId, this.id, Thread.currentThread().getName() + '@' + Thread.currentThread().getId()});
        }
        this.tenantId = tenantId;
    }

    public Locale getLanguage() {
        return this.language;
    }

    public void setLanguage(Locale language) {
        if (logger.isDebugEnabled()) {
            logger.info("=>[framework]set language, language={}, context={}, thread={}", new Object[]{language.toString(), this.id, Thread.currentThread().getName() + '@' + Thread.currentThread().getId()});
        }
        this.language = language;
    }

    public Long getTraceId() {
        return this.traceId;
    }

    public void setTraceId(Long traceId) {
        if (logger.isDebugEnabled()) {
            logger.info("=>[framework]set trace id, id={}, context={}, thread={}", new Object[]{traceId, this.id, Thread.currentThread().getName() + '@' + Thread.currentThread().getId()});
        }
        this.traceId = traceId;
    }

    public String getFromServiceName() {
        return this.fromServiceName;
    }

    public void setFromServiceName(String fromServiceName) {
        if (logger.isDebugEnabled()) {
            logger.info("=>[framework]set from service, name={}, context={}, thread={}", new Object[]{fromServiceName, this.id, Thread.currentThread().getName() + '@' + Thread.currentThread().getId()});
        }
        this.fromServiceName = fromServiceName;
    }

    public String getExtra() {
        return this.extra;
    }

    public void setExtra(String extra) {
        this.extra = extra;
    }
}

