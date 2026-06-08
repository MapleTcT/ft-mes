/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Strings
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.cloud.gateway.filter.GatewayFilter
 *  org.springframework.cloud.gateway.filter.GatewayFilterChain
 *  org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory
 *  org.springframework.cloud.gateway.support.ServerWebExchangeUtils
 *  org.springframework.http.HttpStatus
 *  org.springframework.web.server.ServerWebExchange
 *  reactor.core.publisher.Mono
 */
package com.supcon.supos.suposgateway.filter.factory;

import com.google.common.base.Strings;
import com.supcon.supos.suposgateway.exception.GatewayResponseStatusException;
import com.supcon.supos.suposgateway.filter.FilterOrder;
import com.supcon.supos.suposgateway.filter.OrderedGatewayFilter;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

public class SetPortAfterLoadBalancerGatewayFilterFactory
extends AbstractGatewayFilterFactory<Config> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SetPortAfterLoadBalancerGatewayFilterFactory.class);
    private static final String PORT_KEY = "port";

    public SetPortAfterLoadBalancerGatewayFilterFactory() {
        super(Config.class);
    }

    public List<String> shortcutFieldOrder() {
        return Collections.singletonList(PORT_KEY);
    }

    public GatewayFilter apply(final Config config) {
        return new OrderedGatewayFilter(){

            public int getOrder() {
                return FilterOrder.SET_PORT_AFTER_LOAD_BALANCER.getOrder();
            }

            public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
                URI url = (URI)exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR);
                String schemePrefix = (String)exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_SCHEME_PREFIX_ATTR);
                if (url == null || !"lb".equals(url.getScheme()) && !"lb".equals(schemePrefix)) {
                    return chain.filter(exchange);
                }
                String scheme = url.getScheme();
                String host = url.getHost();
                Integer port = config.getPort();
                try {
                    StringBuilder sb = new StringBuilder();
                    sb.append(scheme).append("://");
                    if (!Strings.isNullOrEmpty((String)url.getRawUserInfo())) {
                        sb.append(url.getRawUserInfo()).append("@");
                    }
                    sb.append(host);
                    if (port >= 0) {
                        sb.append(":").append(port);
                    }
                    sb.append(url.getRawPath());
                    if (!Strings.isNullOrEmpty((String)url.getRawQuery())) {
                        sb.append("?").append(url.getRawQuery());
                    }
                    if (!Strings.isNullOrEmpty((String)url.getRawFragment())) {
                        sb.append("#").append(url.getRawFragment());
                    }
                    URI newUrl = new URI(sb.toString());
                    exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR, newUrl);
                    return chain.filter(exchange);
                }
                catch (Exception e) {
                    throw new GatewayResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, String.format("Request [%s] set port error", exchange.getRequest().getPath()), e);
                }
            }

            @Override
            public Logger getLogger() {
                return LOGGER;
            }
        };
    }

    public static class Config {
        private Integer port;

        public Integer getPort() {
            return this.port;
        }

        public void setPort(Integer port) {
            this.port = port;
        }
    }
}

