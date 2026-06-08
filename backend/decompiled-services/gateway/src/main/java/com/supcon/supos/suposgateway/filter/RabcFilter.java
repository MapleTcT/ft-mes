/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.alibaba.fastjson.JSONObject
 *  org.apache.commons.lang3.StringUtils
 *  org.eclipse.jetty.util.MultiMap
 *  org.eclipse.jetty.util.UrlEncoded
 *  org.reactivestreams.Publisher
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.cloud.gateway.filter.GatewayFilterChain
 *  org.springframework.cloud.gateway.filter.GlobalFilter
 *  org.springframework.core.Ordered
 *  org.springframework.core.io.buffer.DataBuffer
 *  org.springframework.data.redis.core.RedisTemplate
 *  org.springframework.http.HttpMethod
 *  org.springframework.http.HttpStatus
 *  org.springframework.util.ObjectUtils
 *  org.springframework.web.server.ServerWebExchange
 *  reactor.core.publisher.Flux
 *  reactor.core.publisher.Mono
 */
package com.supcon.supos.suposgateway.filter;

import com.alibaba.fastjson.JSONObject;
import com.supcon.supos.suposgateway.SuposGatewayConstants;
import com.supcon.supos.suposgateway.enums.MethodUrlEnum;
import com.supcon.supos.suposgateway.feign.client.RbacClient;
import com.supcon.supos.suposgateway.feign.client.WorkFlowClient;
import com.supcon.supos.suposgateway.filter.FilterOrder;
import com.supcon.supos.suposgateway.filter.support.PathMatcher;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.util.MultiMap;
import org.eclipse.jetty.util.UrlEncoded;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.util.ObjectUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class RabcFilter
implements GlobalFilter,
Ordered {
    private static final Logger log = LoggerFactory.getLogger(RabcFilter.class);
    @Resource
    private PathMatcher pathMatcher;
    @Resource
    private RedisTemplate redisTemplate;
    @Resource
    private RbacClient rbacClient;
    @Resource
    private WorkFlowClient workFlowClient;

    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String tenantId = exchange.getRequest().getHeaders().getFirst("X-Tenant-Id");
        String referer = exchange.getRequest().getHeaders().getFirst("Referer");
        if (this.pathMatcher.matchExcludeGlobal(exchange)) {
            return chain.filter(exchange);
        }
        String rawPath = exchange.getRequest().getURI().getRawPath();
        if (!rawPath.contains("open-api") && !rawPath.contains("openapi")) {
            String[] split = rawPath.split("/");
            Long userId = (Long)exchange.getAttribute(SuposGatewayConstants.USERID);
            Long companyId = (Long)exchange.getAttribute(SuposGatewayConstants.COMPANYID);
            log.info("userId=====>" + userId);
            log.info("companyId=====>" + companyId);
            String url = MethodUrlEnum.getUrlByMethod(exchange.getRequest().getMethod());
            if (this.pendingFilter(exchange.getRequest().getURI().toString(), userId)) {
                return chain.filter(exchange);
            }
            if (this.pendingFilter(referer, userId)) {
                return chain.filter(exchange);
            }
            if (StringUtils.isNotEmpty((CharSequence)url)) {
                List auth;
                Map<String, List<String>> map = (Map<String, List<String>>)this.redisTemplate.opsForValue().get((Object)(tenantId + "_" + url + "_" + companyId + "_" + userId));
                if (map == null) {
                    map = this.rbacClient.refreshRedisByUser(userId, companyId, tenantId, MethodUrlEnum.getUrlMethodByMethod(exchange.getRequest().getMethod()), tenantId);
                }
                if ((auth = (List)map.get(split[2])) != null && !auth.isEmpty()) {
                    boolean match = auth.stream().anyMatch(t -> {
                        Pattern pattern = Pattern.compile(t);
                        Matcher matcher = pattern.matcher(rawPath);
                        return matcher.find();
                    });
                    if (!match && !this.iswhiteUrl(exchange.getRequest().getMethod(), split[2], rawPath)) {
                        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                        JSONObject body = new JSONObject();
                        body.put("message", (Object)"\u60a8\u6ca1\u6709\u8bbf\u95ee\u6743\u9650");
                        body.put("code", (Object)100105999);
                        byte[] bytes = body.toJSONString().getBytes(StandardCharsets.UTF_8);
                        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
                        return exchange.getResponse().writeWith((Publisher)Flux.just((Object)buffer));
                    }
                } else if (!this.iswhiteUrl(exchange.getRequest().getMethod(), split[2], rawPath)) {
                    exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                    JSONObject body = new JSONObject();
                    body.put("message", (Object)"\u60a8\u6ca1\u6709\u8bbf\u95ee\u6743\u9650");
                    body.put("code", (Object)100105999);
                    byte[] bytes = body.toJSONString().getBytes(StandardCharsets.UTF_8);
                    DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
                    return exchange.getResponse().writeWith((Publisher)Flux.just((Object)buffer));
                }
            }
            return chain.filter(exchange);
        }
        return chain.filter(exchange);
    }

    private boolean iswhiteUrl(HttpMethod method, String moduleCode, String url) {
        String completeUrl = MethodUrlEnum.getCompleteUrlByMethod(method);
        Set set = (Set)this.redisTemplate.opsForHash().entries((Object)completeUrl).get(moduleCode);
        if (set != null && !set.add(url)) {
            return false;
        }
        String regMatchUrl = MethodUrlEnum.getRegMatchUrlByMethod(method);
        List list = (List)this.redisTemplate.opsForHash().entries((Object)regMatchUrl).get(moduleCode);
        if (list != null && !list.isEmpty()) {
            return !list.stream().anyMatch(t -> {
                Pattern pattern = Pattern.compile(t);
                Matcher matcher = pattern.matcher(url);
                return matcher.find();
            });
        }
        return true;
    }

    private Long resolvePending(String referer) {
        try {
            URL url = new URL(referer);
            String query = url.getQuery();
            if (ObjectUtils.isEmpty((Object)query)) {
                return null;
            }
            MultiMap values = new MultiMap();
            UrlEncoded.decodeTo((String)url.getQuery(), (MultiMap)values, (String)"UTF-8");
            if (!ObjectUtils.isEmpty((Object)values.get((Object)"pendingId")) && !ObjectUtils.isEmpty(((List)values.get((Object)"pendingId")).get(0))) {
                return Long.valueOf((String)((List)values.get((Object)"pendingId")).get(0));
            }
        }
        catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

    private boolean pendingFilter(String referer, Long userId) {
        Long pendingId = this.resolvePending(referer);
        if (ObjectUtils.isEmpty((Object)pendingId)) {
            return false;
        }
        return this.workFlowClient.verificationProcessOwner(pendingId, userId).getData();
    }

    public int getOrder() {
        return FilterOrder.RBAC.getOrder();
    }
}

