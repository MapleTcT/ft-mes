/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.commons.lang3.StringUtils
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.cloud.gateway.filter.GatewayFilterChain
 *  org.springframework.cloud.gateway.filter.GlobalFilter
 *  org.springframework.cloud.gateway.support.ServerWebExchangeUtils
 *  org.springframework.core.Ordered
 *  org.springframework.http.server.reactive.ServerHttpRequest
 *  org.springframework.web.server.ServerWebExchange
 *  reactor.core.publisher.Mono
 */
package com.supcon.supos.suposgateway.filter;

import com.supcon.supos.suposgateway.filter.FilterOrder;
import com.supcon.supos.suposgateway.utils.HttpUtils;
import com.supcon.supos.suposgateway.utils.ILogger;
import com.supcon.supos.suposgateway.utils.SnowFlake;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

public class InitGlobalFilter
implements GlobalFilter,
Ordered,
ILogger {
    private static final Logger LOGGER = LoggerFactory.getLogger(InitGlobalFilter.class);
    @Resource
    private SnowFlake snowFlake;
    private static Pattern pattern = Pattern.compile("((1[0-9][0-9]\\.)|(2[0-4][0-9]\\.)|(25[0-5]\\.)|([1-9][0-9]\\.)|([0-9]\\.)){3}((1[0-9][0-9])|(2[0-4][0-9])|(25[0-5])|([1-9][0-9])|([0-9]))");

    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        HashMap<String, String> uriVariables = new HashMap<String, String>(1);
        String traceId = String.valueOf(this.snowFlake.getId());
        exchange.getAttributes().put("X-Trace-Id", traceId);
        String tenantId0 = request.getHeaders().getFirst("X-Tenant-Id");
        String authorization = request.getHeaders().getFirst("Authorization");
        if (StringUtils.isEmpty((CharSequence)tenantId0)) {
            String host = request.getURI().getHost();
            this.getLogger().info("host=====>" + host);
            Matcher matcher = pattern.matcher(host);
            tenantId0 = matcher.find() ? "dt" : request.getURI().getHost().split("\\.")[0];
        }
        String tenantId = tenantId0;
        uriVariables.put("X-Tenant-Id", tenantId);
        request = request.mutate().headers(httpHeaders -> {
            String[] arr;
            httpHeaders.remove((Object)"X-Tenant-Id");
            httpHeaders.remove((Object)"X-Trace-Id");
            httpHeaders.remove((Object)"X-Ticket");
            httpHeaders.add("X-Tenant-Id", tenantId);
            httpHeaders.add("X-Trace-Id", traceId);
            if (!StringUtils.isEmpty((CharSequence)authorization) && authorization.startsWith("Bearer") && (arr = authorization.split(" ")).length == 2) {
                String ticket = arr[1];
                httpHeaders.add("X-Ticket", ticket);
            }
        }).build();
        ServerWebExchangeUtils.putUriTemplateVariables((ServerWebExchange)exchange, uriVariables);
        exchange.getAttributes().put(ServerWebExchange.LOG_ID_ATTRIBUTE, String.format(HttpUtils.LOG_PREFIX_FORMAT, traceId, tenantId));
        exchange.getAttributes().put(ServerWebExchangeUtils.PRESERVE_HOST_HEADER_ATTRIBUTE, true);
        return chain.filter(exchange.mutate().request(request).build());
    }

    public int getOrder() {
        return FilterOrder.INIT.getOrder();
    }

    @Override
    public Logger getLogger() {
        return LOGGER;
    }
}

