/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.cloud.gateway.filter.GatewayFilterChain
 *  org.springframework.cloud.gateway.filter.GlobalFilter
 *  org.springframework.core.Ordered
 *  org.springframework.http.HttpStatus
 *  org.springframework.http.server.reactive.ServerHttpRequest
 *  org.springframework.util.ObjectUtils
 *  org.springframework.web.server.ServerWebExchange
 *  reactor.core.publisher.Mono
 */
package com.supcon.supos.suposgateway.filter;

import com.supcon.supos.suposgateway.exception.GatewayResponseStatusException;
import com.supcon.supos.suposgateway.feign.dto.LicenseInfoDTO;
import com.supcon.supos.suposgateway.filter.FilterOrder;
import com.supcon.supos.suposgateway.task.LicenseServiceTask;
import com.supcon.supos.suposgateway.utils.ILogger;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.ObjectUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

public class LicenseFilter
implements GlobalFilter,
Ordered,
ILogger {
    private static final Logger log = LoggerFactory.getLogger(LicenseFilter.class);
    private static final Logger LOGGER = LoggerFactory.getLogger(LicenseFilter.class);

    @Override
    public Logger getLogger() {
        return LOGGER;
    }

    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ConcurrentHashMap<String, LicenseInfoDTO> licenseMap;
        LicenseInfoDTO licenseInfoDTO;
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        String[] split = path.split("/");
        String moduleCode = split[2];
        if (!path.startsWith("/msService") || "public".equalsIgnoreCase(moduleCode)) {
            return chain.filter(exchange);
        }
        if ("ec".equalsIgnoreCase(moduleCode) || "servicemanager".equalsIgnoreCase(moduleCode)) {
            moduleCode = "supPlant-Dev";
        }
        if (ObjectUtils.isEmpty((Object)(licenseInfoDTO = (licenseMap = LicenseServiceTask.licenseMap).get(moduleCode)))) {
            String firstCode = moduleCode.substring(0, 1);
            firstCode = Character.isUpperCase(firstCode.toCharArray()[0]) ? firstCode.toLowerCase() : firstCode.toUpperCase();
            String upperModuleCode = firstCode + moduleCode.substring(1);
            licenseInfoDTO = licenseMap.get(upperModuleCode);
        }
        if (ObjectUtils.isEmpty((Object)licenseInfoDTO)) {
            return chain.filter(exchange);
        }
        if (licenseInfoDTO.getValue().equals(-1)) {
            throw new GatewayResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "\u5f53\u524d\u6a21\u5757\u65e0\u8f6f\u4ef6\u72d7\u6388\u6743");
        }
        return chain.filter(exchange);
    }

    public int getOrder() {
        return FilterOrder.LICENSE.getOrder();
    }
}

