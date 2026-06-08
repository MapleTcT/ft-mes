/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.cloud.gateway.filter.GatewayFilterChain
 *  org.springframework.cloud.gateway.filter.GlobalFilter
 *  org.springframework.core.Ordered
 *  org.springframework.http.server.reactive.ServerHttpRequest
 *  org.springframework.http.server.reactive.ServerHttpResponse
 *  org.springframework.util.StringUtils
 *  org.springframework.web.server.ServerWebExchange
 *  reactor.core.publisher.Mono
 */
package com.supcon.supos.suposgateway.filter;

import com.supcon.supos.suposgateway.filter.FilterOrder;
import com.supcon.supos.suposgateway.utils.HttpUtils;
import com.supcon.supos.suposgateway.utils.ILogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

public class LogGlobalFilter
implements GlobalFilter,
Ordered,
ILogger {
    private static final Logger LOGGER = LoggerFactory.getLogger(LogGlobalFilter.class);

    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        exchange.getAttributes().put("requestTimeStarted", System.currentTimeMillis());
        return chain.filter(exchange).then(Mono.fromRunnable(() -> this.doLog(exchange)));
    }

    public int getOrder() {
        return FilterOrder.REQUEST_LOG.getOrder();
    }

    private void doLog(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        Long startTime = (Long)exchange.getAttribute("requestTimeStarted");
        Long elapsedTime = startTime != null ? System.currentTimeMillis() - startTime : -1L;
        ServerHttpResponse response = exchange.getResponse();
        StringBuilder url = new StringBuilder();
        url.append(request.getURI().getRawPath());
        if (!StringUtils.isEmpty((Object)request.getURI().getRawQuery())) {
            url.append("?").append(request.getURI().getRawQuery());
        }
        HttpUtils.logInfo((ILogger)this, exchange, "Resolved [{}] for {} {} {}, sourceIp [{}], elapsed [{}]ms", response.getStatusCode(), request.getURI().getScheme(), request.getMethod(), url.toString(), HttpUtils.getIpAddress(request), elapsedTime);
    }

    @Override
    public Logger getLogger() {
        return LOGGER;
    }
}

