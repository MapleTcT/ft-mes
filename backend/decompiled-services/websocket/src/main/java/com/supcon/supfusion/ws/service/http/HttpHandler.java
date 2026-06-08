/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.netty.channel.ChannelHandlerContext
 *  io.netty.channel.SimpleChannelInboundHandler
 *  io.netty.handler.codec.http.FullHttpRequest
 *  io.netty.handler.timeout.IdleState
 *  io.netty.handler.timeout.IdleStateEvent
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package com.supcon.supfusion.ws.service.http;

import com.supcon.supfusion.ws.service.util.ClassScanner;
import com.supcon.supfusion.ws.service.util.UriParser;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpHandler
extends SimpleChannelInboundHandler<FullHttpRequest> {
    private static final Logger log = LoggerFactory.getLogger(HttpHandler.class);

    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) throws Exception {
        Map<String, ClassScanner.ControllerRegistry> controllerRegistry = ClassScanner.getControllerRegistry();
        String requestUri = req.uri();
        for (Map.Entry<String, ClassScanner.ControllerRegistry> entry : controllerRegistry.entrySet()) {
            String patternUri = entry.getKey();
            ClassScanner.ControllerRegistry value = entry.getValue();
            Pattern pattern = Pattern.compile(patternUri);
            Matcher matcher = pattern.matcher(requestUri);
            if (!req.method().name().equals(value.getMethod()) || !matcher.find()) continue;
            Method method = value.getClassMethod();
            method.setAccessible(true);
            int index = patternUri.lastIndexOf(".*");
            if (index < 0) {
                method.invoke(value.getInstance(), ctx, req);
                break;
            }
            method.invoke(value.getInstance(), ctx, req, UriParser.getTopic(index, requestUri));
            break;
        }
    }

    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        IdleStateEvent event;
        if (evt instanceof IdleStateEvent && (event = (IdleStateEvent)evt).state() == IdleState.READER_IDLE) {
            log.info("http read time out");
            ctx.channel().close().addListener(f -> log.info("read time out close"));
        }
    }
}

