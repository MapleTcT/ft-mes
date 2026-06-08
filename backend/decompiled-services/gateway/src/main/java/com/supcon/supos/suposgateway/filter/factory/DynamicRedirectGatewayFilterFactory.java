/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.cloud.gateway.filter.GatewayFilter
 *  org.springframework.cloud.gateway.filter.GatewayFilterChain
 *  org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory
 *  org.springframework.cloud.gateway.support.GatewayToStringStyler
 *  org.springframework.cloud.gateway.support.HttpStatusHolder
 *  org.springframework.cloud.gateway.support.ServerWebExchangeUtils
 *  org.springframework.http.HttpStatus
 *  org.springframework.http.server.reactive.ServerHttpResponse
 *  org.springframework.util.Assert
 *  org.springframework.util.StringUtils
 *  org.springframework.web.server.ServerWebExchange
 *  org.springframework.web.util.UriTemplate
 *  reactor.core.publisher.Mono
 */
package com.supcon.supos.suposgateway.filter.factory;

import com.supcon.supos.suposgateway.filter.FilterOrder;
import com.supcon.supos.suposgateway.filter.OrderedGatewayFilter;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.support.GatewayToStringStyler;
import org.springframework.cloud.gateway.support.HttpStatusHolder;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriTemplate;
import reactor.core.publisher.Mono;

public class DynamicRedirectGatewayFilterFactory
extends AbstractGatewayFilterFactory<Config> {
    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicRedirectGatewayFilterFactory.class);
    public static final String STATUS_KEY = "status";
    public static final String TEMPLATE_KEY = "template";

    public DynamicRedirectGatewayFilterFactory() {
        super(Config.class);
    }

    public List<String> shortcutFieldOrder() {
        return Arrays.asList(STATUS_KEY, TEMPLATE_KEY);
    }

    public GatewayFilter apply(Config config) {
        return this.apply(config.status, config.template);
    }

    public GatewayFilter apply(String statusString, String templateUrl) {
        HttpStatusHolder httpStatus = HttpStatusHolder.parse((String)statusString);
        Assert.isTrue((boolean)httpStatus.is3xxRedirection(), (String)("status must be a 3xx code, but was " + statusString));
        UriTemplate uriTemplate = new UriTemplate(templateUrl);
        return this.apply(httpStatus, uriTemplate);
    }

    public GatewayFilter apply(HttpStatus httpStatus, UriTemplate uriTemplate) {
        return this.apply(new HttpStatusHolder(httpStatus, null), uriTemplate);
    }

    public GatewayFilter apply(final HttpStatusHolder httpStatus, final UriTemplate uriTemplate) {
        return new OrderedGatewayFilter(){

            @Override
            public Logger getLogger() {
                return LOGGER;
            }

            public int getOrder() {
                return FilterOrder.DYNAMIC_REDIRECT.getOrder();
            }

            public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
                if (!exchange.getResponse().isCommitted()) {
                    ServerWebExchangeUtils.setResponseStatus((ServerWebExchange)exchange, (HttpStatusHolder)httpStatus);
                    ServerHttpResponse response = exchange.getResponse();
                    Map uriVariables = ServerWebExchangeUtils.getUriTemplateVariables((ServerWebExchange)exchange);
                    URI uri = uriTemplate.expand(uriVariables);
                    String query = exchange.getRequest().getURI().getRawQuery();
                    String finalUri = uri.toString();
                    if (!StringUtils.isEmpty((Object)query)) {
                        finalUri = finalUri + "?" + query;
                    }
                    response.getHeaders().set("Location", finalUri);
                    return response.setComplete();
                }
                return Mono.empty();
            }

            public String toString() {
                String status = httpStatus.getHttpStatus() != null ? String.valueOf(httpStatus.getHttpStatus().value()) : httpStatus.getStatus().toString();
                return GatewayToStringStyler.filterToStringCreator((Object)((Object)DynamicRedirectGatewayFilterFactory.this)).append(status, (Object)uriTemplate).toString();
            }
        };
    }

    public static class Config {
        String status;
        String template;

        public String getStatus() {
            return this.status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getTemplate() {
            return this.template;
        }

        public void setTemplate(String template) {
            this.template = template;
        }
    }
}

