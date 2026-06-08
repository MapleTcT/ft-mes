/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  ch.qos.logback.classic.pattern.ClassicConverter
 *  ch.qos.logback.classic.spi.ILoggingEvent
 *  org.slf4j.MDC
 *  org.springframework.util.StringUtils
 */
package com.supcon.supfusion.framework.cloud.logger.converter;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;

public class TraceIdConverter
extends ClassicConverter {
    public String convert(ILoggingEvent iLoggingEvent) {
        Long traceId = RpcContext.getContext().getTraceId();
        if (traceId == null && !StringUtils.isEmpty((Object)MDC.get((String)"traceId"))) {
            traceId = Long.valueOf(MDC.get((String)"traceId"));
        }
        if (!StringUtils.isEmpty((Object)traceId)) {
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            sb.append(traceId.toString());
            sb.append("]");
            return sb.toString();
        }
        return "";
    }
}

