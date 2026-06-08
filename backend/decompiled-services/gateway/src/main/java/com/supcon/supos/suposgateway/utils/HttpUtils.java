/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.http.HttpHeaders
 *  org.springframework.http.server.reactive.ServerHttpRequest
 *  org.springframework.util.StringUtils
 *  org.springframework.web.server.ServerWebExchange
 */
package com.supcon.supos.suposgateway.utils;

import com.supcon.supos.suposgateway.utils.ILogger;
import java.net.InetSocketAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;

public class HttpUtils
implements ILogger {
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpUtils.class);
    public static String LOG_PREFIX_FORMAT = "%s-%s";
    private static HttpUtils instance = new HttpUtils();

    public static String getIpAddress(ServerHttpRequest request) {
        InetSocketAddress inetSocketAddress;
        HttpHeaders headers = request.getHeaders();
        String ip = headers.getFirst("x-forwarded-for");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = headers.getFirst("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = headers.getFirst("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = headers.getFirst("X-Real-IP");
        }
        if ((ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) && (inetSocketAddress = request.getRemoteAddress()) != null) {
            ip = inetSocketAddress.getAddress().getHostAddress();
        }
        if (ip != null && ip.length() > 15 && ip.contains(",")) {
            ip = ip.substring(0, ip.indexOf(","));
        }
        return ip;
    }

    public static boolean isWS(ServerHttpRequest request) {
        String scheme = request.getURI().getScheme();
        String head = request.getHeaders().getFirst("upgrade");
        return "ws".equals(scheme) || !StringUtils.isEmpty((Object)head);
    }

    public static void logInfo(ILogger logger, ServerWebExchange exchange, String format, Object ... arguments) {
        String logPrefix = exchange.getLogPrefix();
        if (!StringUtils.isEmpty((Object)logPrefix)) {
            StringBuilder sb = new StringBuilder(128);
            HttpUtils.logInfo(logger, sb.append(logPrefix).append(format).toString(), arguments);
        } else {
            HttpUtils.logInfo(logger, format, arguments);
        }
    }

    private static void logInfo(ILogger logger, String format, Object ... arguments) {
        if (logger == null || logger.getLogger() == null) {
            logger = instance;
        }
        logger.getLogger().info(format, arguments);
    }

    public static void logWarn(ILogger logger, ServerWebExchange exchange, String format, Object ... arguments) {
        String logPrefix = exchange.getLogPrefix();
        if (!StringUtils.isEmpty((Object)logPrefix)) {
            StringBuilder sb = new StringBuilder(128);
            HttpUtils.logWarn(logger, sb.append(logPrefix).append(format).toString(), arguments);
        } else {
            HttpUtils.logWarn(logger, format, arguments);
        }
    }

    private static void logWarn(ILogger logger, String format, Object ... arguments) {
        if (logger == null || logger.getLogger() == null) {
            logger = instance;
        }
        logger.getLogger().warn(format, arguments);
    }

    @Override
    public Logger getLogger() {
        return LOGGER;
    }
}

