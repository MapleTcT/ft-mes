/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.reactivestreams.Publisher
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.cloud.gateway.filter.GatewayFilterChain
 *  org.springframework.cloud.gateway.filter.GlobalFilter
 *  org.springframework.cloud.gateway.filter.factory.rewrite.CachedBodyOutputMessage
 *  org.springframework.cloud.gateway.support.BodyInserterContext
 *  org.springframework.cloud.gateway.support.DefaultClientResponse
 *  org.springframework.core.Ordered
 *  org.springframework.core.io.buffer.DataBuffer
 *  org.springframework.core.io.buffer.DataBufferUtils
 *  org.springframework.http.HttpHeaders
 *  org.springframework.http.HttpStatus
 *  org.springframework.http.MediaType
 *  org.springframework.http.ReactiveHttpOutputMessage
 *  org.springframework.http.ResponseCookie
 *  org.springframework.http.client.reactive.ClientHttpResponse
 *  org.springframework.http.server.reactive.ServerHttpResponse
 *  org.springframework.http.server.reactive.ServerHttpResponseDecorator
 *  org.springframework.util.MultiValueMap
 *  org.springframework.web.reactive.function.BodyInserter
 *  org.springframework.web.reactive.function.BodyInserter$Context
 *  org.springframework.web.reactive.function.BodyInserters
 *  org.springframework.web.reactive.function.client.ExchangeStrategies
 *  org.springframework.web.server.ServerWebExchange
 *  reactor.core.publisher.Flux
 *  reactor.core.publisher.Mono
 */
package com.supcon.supos.suposgateway.filter;

import com.supcon.supos.suposgateway.SuposGatewayConstants;
import com.supcon.supos.suposgateway.filter.FilterOrder;
import com.supcon.supos.suposgateway.utils.HttpUtils;
import com.supcon.supos.suposgateway.utils.ILogger;
import java.util.Optional;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.factory.rewrite.CachedBodyOutputMessage;
import org.springframework.cloud.gateway.support.BodyInserterContext;
import org.springframework.cloud.gateway.support.DefaultClientResponse;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.http.ResponseCookie;
import org.springframework.http.client.reactive.ClientHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class CacheResponseBodyGlobalFilter
implements GlobalFilter,
Ordered,
ILogger {
    private static final Logger LOGGER = LoggerFactory.getLogger(CacheResponseBodyGlobalFilter.class);

    public Mono<Void> filter(final ServerWebExchange exchange, GatewayFilterChain chain) {
        if (HttpUtils.isWS(exchange.getRequest())) {
            return chain.filter(exchange);
        }
        ServerHttpResponseDecorator responseDecorator = new ServerHttpResponseDecorator(exchange.getResponse()){

            public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                return DataBufferUtils.join((Publisher)Flux.from(body)).flatMap(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release((DataBuffer)dataBuffer);
                    Flux cachedFlux = Flux.defer(() -> {
                        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
                        DataBufferUtils.retain((DataBuffer)buffer);
                        return Mono.just((Object)buffer);
                    });
                    BodyInserter bodyInserter = BodyInserters.fromDataBuffers((Publisher)cachedFlux);
                    CachedBodyOutputMessage outputMessage = new CachedBodyOutputMessage(exchange, exchange.getResponse().getHeaders());
                    DefaultClientResponse clientResponse = new DefaultClientResponse((ClientHttpResponse)new ResponseAdapter((Publisher<? extends DataBuffer>)cachedFlux, exchange.getResponse().getHeaders()), ExchangeStrategies.withDefaults());
                    Optional optionalMediaType = clientResponse.headers().contentType();
                    if (!optionalMediaType.isPresent() || !((MediaType)optionalMediaType.get()).equals((Object)MediaType.APPLICATION_JSON) && !((MediaType)optionalMediaType.get()).equals((Object)MediaType.APPLICATION_JSON_UTF8)) {
                        return Mono.defer(() -> bodyInserter.insert((ReactiveHttpOutputMessage)outputMessage, (BodyInserter.Context)new BodyInserterContext()).then(Mono.defer(() -> {
                            Flux messageBody = cachedFlux;
                            HttpHeaders headers = this.getDelegate().getHeaders();
                            if (!headers.containsKey((Object)"Transfer-Encoding")) {
                                messageBody = messageBody.doOnNext(data -> headers.setContentLength((long)data.readableByteCount()));
                            }
                            return this.getDelegate().writeWith((Publisher)messageBody);
                        })));
                    }
                    return clientResponse.bodyToMono(Object.class).doOnNext(originalBody -> exchange.getAttributes().put(SuposGatewayConstants.CACHED_RESPONSE_BODY, originalBody)).then(Mono.defer(() -> bodyInserter.insert((ReactiveHttpOutputMessage)outputMessage, (BodyInserter.Context)new BodyInserterContext()).then(Mono.defer(() -> {
                        Flux messageBody = cachedFlux;
                        HttpHeaders headers = this.getDelegate().getHeaders();
                        if (!headers.containsKey((Object)"Transfer-Encoding")) {
                            messageBody = messageBody.doOnNext(data -> headers.setContentLength((long)data.readableByteCount()));
                        }
                        return this.getDelegate().writeWith((Publisher)messageBody);
                    }))));
                });
            }

            public Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body) {
                return this.writeWith((Publisher<? extends DataBuffer>)Flux.from(body).flatMapSequential(p -> p));
            }
        };
        return chain.filter(exchange.mutate().response((ServerHttpResponse)responseDecorator).build());
    }

    public int getOrder() {
        return FilterOrder.CACHE_RESPONSE_BODY.getOrder();
    }

    @Override
    public Logger getLogger() {
        return LOGGER;
    }

    public static class ResponseAdapter
    implements ClientHttpResponse {
        private final Flux<DataBuffer> flux;
        private final HttpHeaders headers;

        public ResponseAdapter(Publisher<? extends DataBuffer> body, HttpHeaders headers) {
            this.headers = headers;
            this.flux = body instanceof Flux ? (Flux)body : ((Mono)body).flux();
        }

        public Flux<DataBuffer> getBody() {
            return this.flux;
        }

        public HttpHeaders getHeaders() {
            return this.headers;
        }

        public HttpStatus getStatusCode() {
            return null;
        }

        public int getRawStatusCode() {
            return 0;
        }

        public MultiValueMap<String, ResponseCookie> getCookies() {
            return null;
        }
    }
}

