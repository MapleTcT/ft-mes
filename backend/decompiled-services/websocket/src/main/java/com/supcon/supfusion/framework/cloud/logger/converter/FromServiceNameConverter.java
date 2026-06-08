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

public class FromServiceNameConverter
extends ClassicConverter {
    public String convert(ILoggingEvent event) {
        String serviceName = RpcContext.getContext().getFromServiceName();
        if (StringUtils.isEmpty((Object)serviceName) && !StringUtils.isEmpty((Object)MDC.get((String)"fromServiceName"))) {
            serviceName = MDC.get((String)"fromServiceName");
        }
        if (!StringUtils.isEmpty((Object)serviceName)) {
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            sb.append(serviceName);
            sb.append("]");
            return sb.toString();
        }
        return "";
    }
}

