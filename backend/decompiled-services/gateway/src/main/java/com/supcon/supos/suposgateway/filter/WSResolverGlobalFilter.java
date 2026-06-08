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
 *  org.springframework.http.server.reactive.ServerHttpRequest
 *  org.springframework.web.server.ServerWebExchange
 *  reactor.core.publisher.Mono
 */
package com.supcon.supos.suposgateway.filter;

import com.supcon.supos.suposgateway.filter.FilterOrder;
import com.supcon.supos.suposgateway.utils.HttpUtils;
import com.supcon.supos.suposgateway.utils.ILogger;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

public class WSResolverGlobalFilter
implements GlobalFilter,
Ordered,
ILogger {
    private static final Logger LOGGER = LoggerFactory.getLogger(WSResolverGlobalFilter.class);

    @Override
    public Logger getLogger() {
        return LOGGER;
    }

    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!HttpUtils.isWS(exchange.getRequest())) {
            return chain.filter(exchange);
        }
        ServerHttpRequest request = exchange.getRequest();
        String authHeaderValue = request.getHeaders().getFirst("Authorization");
        if (!StringUtils.isEmpty((CharSequence)authHeaderValue) && authHeaderValue.startsWith("Bearer")) {
            return chain.filter(exchange);
        }
        String token = (String)request.getQueryParams().getFirst((Object)"token");
        if (StringUtils.isEmpty((CharSequence)token)) {
            return chain.filter(exchange);
        }
        String value = "Bearer " + token;
        request = request.mutate().headers(httpHeaders -> {
            httpHeaders.remove((Object)"Authorization");
            httpHeaders.add("Authorization", value);
        }).build();
        return chain.filter(exchange.mutate().request(request).build());
    }

    public int getOrder() {
        return FilterOrder.WS_RESOLVER.getOrder();
    }
}

