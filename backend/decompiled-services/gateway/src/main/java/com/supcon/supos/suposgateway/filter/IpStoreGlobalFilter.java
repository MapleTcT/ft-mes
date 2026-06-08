/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.cloud.gateway.filter.GatewayFilterChain
 *  org.springframework.cloud.gateway.filter.GlobalFilter
 *  org.springframework.core.Ordered
 *  org.springframework.web.server.ServerWebExchange
 *  reactor.core.publisher.Mono
 */
package com.supcon.supos.suposgateway.filter;

import com.supcon.custom.ribbon.IpStoreThreadLocal;
import com.supcon.supos.suposgateway.filter.FilterOrder;
import com.supcon.supos.suposgateway.utils.IpUtil;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

public class IpStoreGlobalFilter
implements GlobalFilter,
Ordered {
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String remoteIp = IpUtil.getCurrentIp(exchange.getRequest());
        IpStoreThreadLocal.storeCurrentRemoteIp(remoteIp);
        return chain.filter(exchange);
    }

    public int getOrder() {
        return FilterOrder.STORE_IP.getOrder();
    }
}

