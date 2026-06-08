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
 *  org.springframework.beans.factory.annotation.Value
 *  org.springframework.cloud.gateway.filter.GatewayFilter
 *  org.springframework.cloud.gateway.filter.GatewayFilterChain
 *  org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory
 *  org.springframework.cloud.gateway.support.ServerWebExchangeUtils
 *  org.springframework.http.HttpStatus
 *  org.springframework.http.server.PathContainer
 *  org.springframework.http.server.PathContainer$Element
 *  org.springframework.http.server.PathContainer$PathSegment
 *  org.springframework.http.server.reactive.ServerHttpRequest
 *  org.springframework.util.ObjectUtils
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
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.PathContainer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

public class ForwardAppUrlGatewayFilterFactory
extends AbstractGatewayFilterFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(ForwardAppUrlGatewayFilterFactory.class);
    private AtomicInteger atomicInteger = new AtomicInteger(0);
    @Value(value="${spring.cloud.nacos.config.group}")
    private String groupName;
    @Autowired
    private NacosDiscoveryProperties nacosDiscoveryProperties;

    public GatewayFilter apply(Object config) {
        return new OrderedGatewayFilter(){

            @Override
            public Logger getLogger() {
                return LOGGER;
            }

            public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
                String tenantId;
                String serviceId;
                ServerHttpRequest request = exchange.getRequest();
                String rawPath = exchange.getRequest().getURI().getRawPath();
                PathContainer path = PathContainer.parsePath((String)rawPath);
                List elements = path.elements();
                int serviceIdIndex = 3;
                try {
                    serviceId = ((PathContainer.PathSegment)elements.get(serviceIdIndex)).valueToMatch();
                    if (!StringUtils.isEmpty((Object)serviceId) && ("public".equals(serviceId) || "appws".equals(serviceId))) {
                        serviceIdIndex = 5;
                        serviceId = ((PathContainer.PathSegment)elements.get(serviceIdIndex)).valueToMatch();
                    }
                }
                catch (Exception e) {
                    throw new GatewayResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Can't resolve 'serviceId' pathSegment", e);
                }
                if (StringUtils.isEmpty((Object)serviceId)) {
                    throw new GatewayResponseStatusException(HttpStatus.NOT_FOUND, "Can't resolve 'serviceId' pathSegment");
                }
                int newPathIndex = serviceIdIndex == 5 ? serviceIdIndex - 3 : ("appws".equals(((PathContainer.PathSegment)elements.get(1)).valueToMatch()) ? 0 : serviceIdIndex - 1);
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
                if (StringUtils.isEmpty((Object)(tenantId = (String)ServerWebExchangeUtils.getUriTemplateVariables((ServerWebExchange)exchange).get("X-Tenant-Id")))) {
                    throw new GatewayResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, String.format("Request [%s] can not resolve 'tenantId'", request.getPath()));
                }
                ArrayList resultInstanceList = new ArrayList();
                try {
                    int i;
                    this.getLogger().info("groupName:{}", (Object)ForwardAppUrlGatewayFilterFactory.this.groupName);
                    List instanceList = ForwardAppUrlGatewayFilterFactory.this.nacosDiscoveryProperties.namingServiceInstance().selectInstances(serviceId, ForwardAppUrlGatewayFilterFactory.this.groupName, true);
                    if (ObjectUtils.isEmpty((Object)instanceList)) {
                        this.getLogger().info("can not find any tenant app service1");
                        throw new GatewayResponseStatusException(HttpStatus.NOT_FOUND, String.format("Request [%s] can not find any tenant app service", request.getPath()));
                    }
                    for (i = 0; i < instanceList.size(); ++i) {
                        if (ObjectUtils.isEmpty((Object)((Instance)instanceList.get(i)).getMetadata()) || !tenantId.equals(((Instance)instanceList.get(i)).getMetadata().get("supfusion.tenantid"))) continue;
                        resultInstanceList.add(instanceList.get(i));
                    }
                    if (resultInstanceList.size() == 0) {
                        for (i = 0; i < instanceList.size(); ++i) {
                            if (ObjectUtils.isEmpty((Object)((Instance)instanceList.get(i)).getMetadata()) || !"default".equals(((Instance)instanceList.get(i)).getMetadata().get("supfusion.tenantid"))) continue;
                            resultInstanceList.add(instanceList.get(i));
                        }
                        if (resultInstanceList.size() == 0) {
                            this.getLogger().info("can not find any tenant app service2");
                            throw new GatewayResponseStatusException(HttpStatus.NOT_FOUND, String.format("Request [%s] can not find any tenant app service", request.getPath()));
                        }
                    }
                    int index = ForwardAppUrlGatewayFilterFactory.this.getAndIncrement() % resultInstanceList.size();
                    Instance instance = (Instance)resultInstanceList.get(index);
                    String host = instance.getIp();
                    int port = instance.getPort();
                    String query = request.getURI().getRawQuery();
                    String requestPath = "http://" + host + ":" + port + newPath;
                    if (!StringUtils.isEmpty((Object)query)) {
                        requestPath = requestPath + "?" + query;
                    }
                    request = request.mutate().uri(new URI(requestPath)).build();
                    exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR, request.getURI());
                    return chain.filter(exchange.mutate().request(request).build());
                }
                catch (NacosException ne) {
                    this.getLogger().info("can not find any tenant app service3:{}", (Object)ne.getErrMsg());
                    throw new GatewayResponseStatusException(HttpStatus.NOT_FOUND, String.format("Request [%s] can not find any tenant app service", request.getPath()), ne);
                }
                catch (Exception e) {
                    throw new GatewayResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, String.format("Request [%s] can not find any tenant app service", request.getPath()), e);
                }
            }

            public int getOrder() {
                return FilterOrder.FORWARD_APP_URL.getOrder();
            }
        };
    }

    public final int getAndIncrement() {
        int next;
        int current;
        while (!this.atomicInteger.compareAndSet(current, next = (current = this.atomicInteger.get()) >= Integer.MAX_VALUE ? 0 : current + 1)) {
        }
        return next;
    }
}

