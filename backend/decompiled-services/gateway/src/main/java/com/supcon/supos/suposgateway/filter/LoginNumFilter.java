/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.alibaba.fastjson.JSONObject
 *  org.reactivestreams.Publisher
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.cloud.gateway.filter.GatewayFilterChain
 *  org.springframework.cloud.gateway.filter.GlobalFilter
 *  org.springframework.core.Ordered
 *  org.springframework.core.io.buffer.DataBuffer
 *  org.springframework.data.redis.core.StringRedisTemplate
 *  org.springframework.http.HttpStatus
 *  org.springframework.http.server.reactive.ServerHttpResponse
 *  org.springframework.web.server.ServerWebExchange
 *  reactor.core.publisher.Flux
 *  reactor.core.publisher.Mono
 */
package com.supcon.supos.suposgateway.filter;

import com.alibaba.fastjson.JSONObject;
import com.supcon.supos.suposgateway.filter.FilterOrder;
import com.supcon.supos.suposgateway.utils.ILogger;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class LoginNumFilter
implements GlobalFilter,
Ordered,
ILogger {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoginNumFilter.class);
    @Autowired
    private StringRedisTemplate redisTemplate;

    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().toString();
        if ("/inter-api/auth/logout".equals(path) || !"/inter-api/auth/login".equals(path)) {
            return chain.filter(exchange);
        }
        String redisValue = (String)this.redisTemplate.opsForValue().get((Object)"LICENSE:LOGIN_NUM");
        String[] split = redisValue.split(",");
        int licenseValue = Integer.parseInt(split[0]);
        int onlineValue = Integer.parseInt(split[1]);
        if (255 == licenseValue) {
            return chain.filter(exchange);
        }
        if (licenseValue <= onlineValue) {
            HashMap<String, Object> map = new HashMap<String, Object>();
            map.put("code", 100105016);
            map.put("message", String.format("\u8d85\u8fc7\u6700\u5927\u5e76\u53d1\u6570(%s)", licenseValue));
            byte[] bytes = JSONObject.toJSONString(map).getBytes(StandardCharsets.UTF_8);
            ServerHttpResponse response = exchange.getResponse();
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
            response.getHeaders().add("Content-Type", "application/json;charset=UTF-8");
            response.getHeaders().add("Content-Length", String.valueOf(buffer.readableByteCount()));
            return response.writeWith((Publisher)Flux.just((Object)buffer));
        }
        return chain.filter(exchange);
    }

    public int getOrder() {
        return FilterOrder.LOGIN_NUM.getOrder();
    }

    @Override
    public Logger getLogger() {
        return LOGGER;
    }
}

