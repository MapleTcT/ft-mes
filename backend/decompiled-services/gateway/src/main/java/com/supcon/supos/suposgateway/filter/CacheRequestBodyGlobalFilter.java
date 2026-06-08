/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.reactivestreams.Publisher
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.cloud.gateway.filter.GatewayFilterChain
 *  org.springframework.cloud.gateway.filter.GlobalFilter
 *  org.springframework.core.Ordered
 *  org.springframework.core.io.buffer.DataBuffer
 *  org.springframework.core.io.buffer.DataBufferUtils
 *  org.springframework.http.HttpHeaders
 *  org.springframework.http.HttpMethod
 *  org.springframework.http.MediaType
 *  org.springframework.http.codec.HttpMessageReader
 *  org.springframework.http.server.reactive.ServerHttpRequest
 *  org.springframework.http.server.reactive.ServerHttpRequestDecorator
 *  org.springframework.web.reactive.function.server.HandlerStrategies
 *  org.springframework.web.reactive.function.server.ServerRequest
 *  org.springframework.web.server.ServerWebExchange
 *  reactor.core.publisher.Flux
 *  reactor.core.publisher.Mono
 */
package com.supcon.supos.suposgateway.filter;

import com.supcon.supos.suposgateway.SuposGatewayConstants;
import com.supcon.supos.suposgateway.filter.FilterOrder;
import com.supcon.supos.suposgateway.filter.support.PathMatcher;
import com.supcon.supos.suposgateway.utils.ILogger;
import java.util.List;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class CacheRequestBodyGlobalFilter
implements GlobalFilter,
Ordered,
ILogger {
    private static final Logger LOGGER = LoggerFactory.getLogger(CacheRequestBodyGlobalFilter.class);
    private static final List<HttpMessageReader<?>> MESSAGE_READERS = HandlerStrategies.withDefaults().messageReaders();
    @Autowired
    private PathMatcher pathMatcher;

    public int getOrder() {
        return FilterOrder.CACHE_REQUEST_BODY.getOrder();
    }

    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        if (this.pathMatcher.matchBlackList(exchange) || this.pathMatcher.matchExcludeSign(exchange) || this.pathMatcher.matchExcludeGlobal(exchange)) {
            // empty if block
        }
        HttpHeaders headers = request.getHeaders();
        MediaType contentType = headers.getContentType();
        long length = headers.getContentLength();
        if (length > 0L && contentType != null && (request.getMethod() == HttpMethod.POST || request.getMethod() == HttpMethod.PUT) && (MediaType.APPLICATION_JSON.equals((Object)contentType) || MediaType.APPLICATION_JSON_UTF8.equals((Object)contentType)) && ("/inter-api/auth/login".equals(path) || "/inter-api/auth/company/change".equals(path))) {
            return this.readJsonBody(exchange, chain);
        }
        return chain.filter(exchange);
    }

    private Mono<Void> readJsonBody(ServerWebExchange exchange, GatewayFilterChain chain) {
        return DataBufferUtils.join((Publisher)exchange.getRequest().getBody()).flatMap(dataBuffer -> {
            byte[] bytes = new byte[dataBuffer.readableByteCount()];
            dataBuffer.read(bytes);
            DataBufferUtils.release((DataBuffer)dataBuffer);
            final Flux cachedFlux = Flux.defer(() -> {
                DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
                DataBufferUtils.retain((DataBuffer)buffer);
                return Mono.just((Object)buffer);
            });
            ServerHttpRequestDecorator mutatedRequest = new ServerHttpRequestDecorator(exchange.getRequest()){

                public Flux<DataBuffer> getBody() {
                    return cachedFlux;
                }
            };
            ServerWebExchange mutatedExchange = exchange.mutate().request((ServerHttpRequest)mutatedRequest).build();
            return ServerRequest.create((ServerWebExchange)mutatedExchange, MESSAGE_READERS).bodyToMono(String.class).doOnNext(requestBody -> exchange.getAttributes().put(SuposGatewayConstants.CACHED_REQUEST_BODY, requestBody)).then(chain.filter(mutatedExchange));
        });
    }

    @Override
    public Logger getLogger() {
        return LOGGER;
    }
}

