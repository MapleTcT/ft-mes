/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.netty.handler.codec.http.HttpHeaders
 *  org.apache.commons.logging.Log
 *  org.apache.commons.logging.LogFactory
 *  org.springframework.core.io.buffer.NettyDataBufferFactory
 *  org.springframework.http.HttpHeaders
 *  org.springframework.util.StringUtils
 *  org.springframework.web.reactive.socket.HandshakeInfo
 *  org.springframework.web.reactive.socket.WebSocketHandler
 *  org.springframework.web.reactive.socket.WebSocketSession
 *  org.springframework.web.reactive.socket.adapter.ReactorNettyWebSocketSession
 *  org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient
 *  reactor.core.publisher.Mono
 *  reactor.netty.http.client.HttpClient
 *  reactor.netty.http.client.HttpClient$WebsocketSender
 *  reactor.netty.http.websocket.WebsocketInbound
 */
package com.supcon.supos.suposgateway.websocket;

import java.net.URI;
import java.util.Collection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.reactive.socket.adapter.ReactorNettyWebSocketSession;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.websocket.WebsocketInbound;

public class ReactorNettyWebSocketClient
extends org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient {
    private static final Log logger = LogFactory.getLog(ReactorNettyWebSocketClient.class);
    private int maxFramePayloadLength;

    public ReactorNettyWebSocketClient(HttpClient httpClient, int maxFramePayloadLength) {
        super(httpClient);
        this.maxFramePayloadLength = maxFramePayloadLength;
    }

    public Mono<Void> execute(URI url, HttpHeaders requestHeaders, WebSocketHandler handler) {
        return ((HttpClient.WebsocketSender)this.getHttpClient().headers(nettyHeaders -> this.setNettyHeaders(requestHeaders, (io.netty.handler.codec.http.HttpHeaders)nettyHeaders)).websocket(StringUtils.collectionToCommaDelimitedString((Collection)handler.getSubProtocols()), this.maxFramePayloadLength).uri(url.toString())).handle((inbound, outbound) -> {
            HttpHeaders responseHeaders = this.toHttpHeaders((WebsocketInbound)inbound);
            String protocol = responseHeaders.getFirst("Sec-WebSocket-Protocol");
            HandshakeInfo info = new HandshakeInfo(url, responseHeaders, Mono.empty(), protocol);
            NettyDataBufferFactory factory = new NettyDataBufferFactory(outbound.alloc());
            ReactorNettyWebSocketSession session = new ReactorNettyWebSocketSession(inbound, outbound, info, factory);
            if (logger.isDebugEnabled()) {
                logger.debug((Object)("Started session '" + session.getId() + "' for " + url));
            }
            return handler.handle((WebSocketSession)session);
        }).doOnRequest(n -> {
            if (logger.isDebugEnabled()) {
                logger.debug((Object)("Connecting to " + url));
            }
        }).next();
    }

    private void setNettyHeaders(HttpHeaders httpHeaders, io.netty.handler.codec.http.HttpHeaders nettyHeaders) {
        httpHeaders.forEach((arg_0, arg_1) -> ((io.netty.handler.codec.http.HttpHeaders)nettyHeaders).set(arg_0, arg_1));
    }

    private HttpHeaders toHttpHeaders(WebsocketInbound inbound) {
        HttpHeaders headers = new HttpHeaders();
        io.netty.handler.codec.http.HttpHeaders nettyHeaders = inbound.headers();
        nettyHeaders.forEach(entry -> {
            String name = (String)entry.getKey();
            headers.put(name, nettyHeaders.getAll(name));
        });
        return headers;
    }
}

