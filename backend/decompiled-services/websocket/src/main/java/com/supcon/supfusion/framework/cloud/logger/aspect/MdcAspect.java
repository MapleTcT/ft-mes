/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.aspectj.lang.JoinPoint
 *  org.aspectj.lang.annotation.Aspect
 *  org.aspectj.lang.annotation.Before
 *  org.aspectj.lang.annotation.Pointcut
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.slf4j.MDC
 *  org.springframework.stereotype.Component
 */
package com.supcon.supfusion.framework.cloud.logger.aspect;

import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import com.supcon.supfusion.framework.cloud.common.util.StringExUtil;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
@Aspect
public class MdcAspect {
    private static final Logger log = LoggerFactory.getLogger(MdcAspect.class);

    @Pointcut(value="execution(public * com.supcon.supfusion..*.*(..))")
    public void mdcAspect() {
    }

    @Before(value="mdcAspect() || @annotation(com.supcon.supfusion.framework.cloud.logger.annotation.MdcOperate)")
    public void doBefore(JoinPoint joinPoint) throws Throwable {
        if (StringExUtil.isBlank(MDC.get((String)"X-Trace-Id")) && null != RpcContext.getContext().getTraceId()) {
            MDC.put((String)"X-Trace-Id", (String)String.valueOf(RpcContext.getContext().getTraceId()));
        }
    }
}

