/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.alibaba.cloud.nacos.NacosDiscoveryProperties
 *  com.alibaba.nacos.api.exception.NacosException
 *  com.alibaba.nacos.api.naming.pojo.Instance
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.cloud.gateway.filter.GatewayFilter
 *  org.springframework.cloud.gateway.filter.GatewayFilterChain
 *  org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory
 *  org.springframework.cloud.gateway.support.ServerWebExchangeUtils
 *  org.springframework.http.HttpStatus
 *  org.springframework.http.server.PathContainer
 *  org.springframework.http.server.PathContainer$Element
 *  org.springframework.http.server.PathContainer$PathSegment
 *  org.springframework.http.server.reactive.ServerHttpRequest
 *  org.springframework.util.StringUtils
 *  org.springframework.web.server.ServerWebExchange
 *  reactor.core.publisher.Mono
 */
package com.supcon.supos.suposgateway.filter.factory;

import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.supcon.supos.suposgateway.exception.GatewayResponseStatusException;
import com.supcon.supos.suposgateway.filter.FilterOrder;
import com.supcon.supos.suposgateway.filter.OrderedGatewayFilter;
import com.supcon.supos.suposgateway.utils.HttpUtils;
import com.supcon.supos.suposgateway.utils.ILogger;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.PathContainer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

public class AddTenantAppUriHeaderGatewayFilterFactory
extends AbstractGatewayFilterFactory<Config> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AddTenantAppUriHeaderGatewayFilterFactory.class);
    private static final String HEADER_KEY = "header";
    @Autowired
    private NacosDiscoveryProperties nacosDiscoveryProperties;

    public AddTenantAppUriHeaderGatewayFilterFactory() {
        super(Config.class);
    }

    public List<String> shortcutFieldOrder() {
        return Collections.singletonList(HEADER_KEY);
    }

    public GatewayFilter apply(final Config config) {
        return new OrderedGatewayFilter(){

            @Override
            public Logger getLogger() {
                return LOGGER;
            }

            public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
                String group;
                String serviceId;
                ServerHttpRequest request = exchange.getRequest();
                String rawPath = exchange.getRequest().getURI().getRawPath();
                PathContainer path = PathContainer.parsePath((String)rawPath);
                List elements = path.elements();
                int serviceIdIndex = 5;
                if (rawPath.startsWith("/apps")) {
                    serviceIdIndex = 3;
                }
                try {
                    serviceId = ((PathContainer.PathSegment)elements.get(serviceIdIndex)).valueToMatch();
                }
                catch (Exception e) {
                    throw new GatewayResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Can't resolve 'serviceId' pathSegment", e);
                }
                if (StringUtils.isEmpty((Object)serviceId)) {
                    throw new GatewayResponseStatusException(HttpStatus.NOT_FOUND, "Can't resolve 'serviceId' pathSegment");
                }
                int newPathIndex = serviceIdIndex + 1;
                StringBuilder newPath = new StringBuilder();
                if (elements.size() > newPathIndex) {
                    for (int i = newPathIndex; i < elements.size(); ++i) {
                        PathContainer.Element element = (PathContainer.Element)elements.get(i);
                        if (!(element instanceof PathContainer.PathSegment)) {
                            newPath.append("/");
                            continue;
                        }
                        PathContainer.PathSegment p = (PathContainer.PathSegment)element;
                        newPath.append(p.valueToMatch());
                    }
                }
                if (StringUtils.isEmpty((Object)(group = (String)ServerWebExchangeUtils.getUriTemplateVariables((ServerWebExchange)exchange).get("X-Tenant-Id")))) {
                    throw new GatewayResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, String.format("Request [%s] can not resolve 'group'", request.getPath()));
                }
                try {
                    Instance instance = AddTenantAppUriHeaderGatewayFilterFactory.this.nacosDiscoveryProperties.namingServiceInstance().selectOneHealthyInstance(serviceId, group);
                    String schema = "http";
                    String host = instance.getIp();
                    int port = instance.getPort();
                    String query = request.getURI().getRawQuery();
                    request = request.mutate().headers(httpHeaders -> {
                        httpHeaders.remove((Object)config.getHeader());
                        String newUri = "http://" + host + ":" + port;
                        if (newPath.length() > 0) {
                            newUri = newUri + newPath;
                            if (!StringUtils.isEmpty((Object)query)) {
                                newUri = newUri + "?" + query;
                            }
                        }
                        if (LOGGER.isDebugEnabled()) {
                            HttpUtils.logInfo((ILogger)this, exchange, "to tenant uri -> {}", newUri);
                        }
                        httpHeaders.add(config.getHeader(), newUri);
                    }).build();
                    return chain.filter(exchange.mutate().request(request).build());
                }
                catch (NacosException ne) {
                    throw new GatewayResponseStatusException(HttpStatus.NOT_FOUND, String.format("Request [%s] can not find any tenant app service", request.getPath()), ne);
                }
                catch (Exception e) {
                    throw new GatewayResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, String.format("Request [%s] can not find any tenant app service", request.getPath()), e);
                }
            }

            public int getOrder() {
                return FilterOrder.ADD_TENANT_APP_URI_HEADER.getOrder();
            }
        };
    }

    public static class Config {
        private String header;

        public String getHeader() {
            return this.header;
        }

        public void setHeader(String header) {
            this.header = header;
        }
    }
}

