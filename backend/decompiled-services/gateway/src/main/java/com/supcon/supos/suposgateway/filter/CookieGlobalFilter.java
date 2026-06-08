/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.commons.lang3.StringUtils
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.cloud.gateway.filter.GatewayFilterChain
 *  org.springframework.cloud.gateway.filter.GlobalFilter
 *  org.springframework.core.Ordered
 *  org.springframework.http.ResponseCookie
 *  org.springframework.http.server.reactive.ServerHttpRequest
 *  org.springframework.web.server.ServerWebExchange
 *  reactor.core.publisher.Mono
 */
package com.supcon.supos.suposgateway.filter;

import com.supcon.supos.suposgateway.filter.FilterOrder;
import com.supcon.supos.suposgateway.utils.HttpUtils;
import com.supcon.supos.suposgateway.utils.ILogger;
import com.supcon.supos.suposgateway.utils.JwtUtil;
import java.time.Duration;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.ResponseCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

public class CookieGlobalFilter
implements GlobalFilter,
Ordered,
ILogger {
    private static final Logger LOGGER = LoggerFactory.getLogger(CookieGlobalFilter.class);

    @Override
    public Logger getLogger() {
        return LOGGER;
    }

    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String rawPath = exchange.getRequest().getURI().getRawPath();
        if (!(rawPath.contains("open-api") || rawPath.contains("openapi") || HttpUtils.isWS(exchange.getRequest()) || request.getCookies().containsKey((Object)"suposTicket"))) {
            String ticket = JwtUtil.parseTokenTicket(request);
            if (StringUtils.isNotBlank((CharSequence)ticket)) {
                exchange.getResponse().addCookie(ResponseCookie.from((String)"suposTicket", (String)ticket).path("/").httpOnly(true).build());
            } else {
                exchange.getResponse().addCookie(ResponseCookie.from((String)"suposTicket", (String)"suposTicket").maxAge(Duration.ZERO).path("/").httpOnly(true).build());
            }
        }
        return chain.filter(exchange);
    }

    public int getOrder() {
        return FilterOrder.COOKIE.getOrder();
    }
}

