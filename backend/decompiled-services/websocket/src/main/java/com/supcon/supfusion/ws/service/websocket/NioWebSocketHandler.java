/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.netty.buffer.ByteBuf
 *  io.netty.buffer.Unpooled
 *  io.netty.channel.Channel
 *  io.netty.channel.ChannelFuture
 *  io.netty.channel.ChannelFutureListener
 *  io.netty.channel.ChannelHandler
 *  io.netty.channel.ChannelHandlerContext
 *  io.netty.channel.ChannelPipeline
 *  io.netty.channel.SimpleChannelInboundHandler
 *  io.netty.handler.codec.http.DefaultFullHttpResponse
 *  io.netty.handler.codec.http.FullHttpRequest
 *  io.netty.handler.codec.http.HttpHeaderNames
 *  io.netty.handler.codec.http.HttpMessage
 *  io.netty.handler.codec.http.HttpRequest
 *  io.netty.handler.codec.http.HttpUtil
 *  io.netty.handler.codec.http.websocketx.CloseWebSocketFrame
 *  io.netty.handler.codec.http.websocketx.PingWebSocketFrame
 *  io.netty.handler.codec.http.websocketx.PongWebSocketFrame
 *  io.netty.handler.codec.http.websocketx.TextWebSocketFrame
 *  io.netty.handler.codec.http.websocketx.WebSocketFrame
 *  io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker
 *  io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory
 *  io.netty.handler.timeout.IdleState
 *  io.netty.handler.timeout.IdleStateEvent
 *  io.netty.util.CharsetUtil
 *  io.netty.util.concurrent.GenericFutureListener
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package com.supcon.supfusion.ws.service.websocket;

import com.supcon.supfusion.ws.service.constant.WsConstants;
import com.supcon.supfusion.ws.service.http.HttpHandler;
import com.supcon.supfusion.ws.service.manage.ChannelGroupManager;
import com.supcon.supfusion.ws.service.util.ClassScanner;
import com.supcon.supfusion.ws.service.util.UriParser;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.GenericFutureListener;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NioWebSocketHandler
extends SimpleChannelInboundHandler<Object> {
    private static final Logger log = LoggerFactory.getLogger(NioWebSocketHandler.class);
    private WebSocketServerHandshaker handshaker;
    private ClassScanner.MethodMapping methodMapping;

    private static void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest req, DefaultFullHttpResponse res) {
        if (res.status().code() != 200) {
            ByteBuf buf = Unpooled.copiedBuffer((CharSequence)res.status().toString(), (Charset)CharsetUtil.UTF_8);
            res.content().writeBytes(buf);
            buf.release();
        }
        ChannelFuture f = ctx.channel().writeAndFlush((Object)res);
        if (!HttpUtil.isKeepAlive((HttpMessage)req) || res.status().code() != 200) {
            f.addListener((GenericFutureListener)ChannelFutureListener.CLOSE);
        }
    }

    private static String getWebSocketLocation(FullHttpRequest req) {
        String location = req.headers().get((CharSequence)HttpHeaderNames.HOST) + req.uri();
        return "ws://" + location;
    }

    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        log.info("\u6536\u5230\u6d88\u606f\uff1a" + msg);
        if (msg instanceof FullHttpRequest) {
            this.handleHttpRequest(ctx, (FullHttpRequest)msg);
        } else if (msg instanceof WebSocketFrame) {
            this.handlerWebSocketFrame(ctx, (WebSocketFrame)msg);
        }
    }

    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelActive();
    }

    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("\u5ba2\u6237\u7aef\u65ad\u5f00\u8fde\u63a5\uff1a" + ctx.channel());
        ChannelGroupManager.removeChannel(ctx.channel());
    }

    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    private void handlerWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) throws InvocationTargetException, IllegalAccessException {
        if (frame instanceof CloseWebSocketFrame) {
            this.handshaker.close(ctx.channel(), (CloseWebSocketFrame)frame.retain());
            return;
        }
        if (frame instanceof PingWebSocketFrame) {
            frame.retain();
            ctx.channel().writeAndFlush((Object)new PongWebSocketFrame(frame.content()));
            return;
        }
        if (frame instanceof TextWebSocketFrame) {
            log.info(((TextWebSocketFrame)frame).text());
        }
    }

    private void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest req) throws InvocationTargetException, IllegalAccessException {
        if (!"websocket".equals(req.headers().get("Upgrade"))) {
            ChannelPipeline pipeline = ctx.channel().pipeline();
            pipeline.remove("ws");
            HttpHandler httpHandler = new HttpHandler();
            pipeline.addLast(new ChannelHandler[]{httpHandler});
            try {
                req.retain();
                httpHandler.channelRead(ctx, req);
            }
            catch (Exception e) {
                log.error("handle http error", (Throwable)e);
            }
        } else {
            Map<String, ClassScanner.MethodMapping> wsController = ClassScanner.getWsController();
            String requestUri = req.uri();
            for (Map.Entry<String, ClassScanner.MethodMapping> method : wsController.entrySet()) {
                String patternUri = method.getKey();
                Pattern pattern = Pattern.compile(patternUri);
                Matcher matcher = pattern.matcher(requestUri);
                if (!matcher.find()) continue;
                this.methodMapping = method.getValue();
                Method handshake = this.methodMapping.getBeforeHandshake();
                handshake.setAccessible(true);
                int index = patternUri.lastIndexOf(".*");
                if (index < 0) {
                    handshake.invoke(this.methodMapping.getInstance(), ctx.channel(), req);
                    break;
                }
                handshake.invoke(this.methodMapping.getInstance(), ctx.channel(), req, UriParser.getTopic(index, requestUri));
                break;
            }
            if (this.methodMapping == null) {
                WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse((Channel)ctx.channel());
                return;
            }
            WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(NioWebSocketHandler.getWebSocketLocation(req), null, false);
            this.handshaker = wsFactory.newHandshaker((HttpRequest)req);
            if (this.handshaker == null) {
                WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse((Channel)ctx.channel());
            } else {
                this.handshaker.handshake(ctx.channel(), req);
            }
        }
    }

    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        IdleStateEvent event;
        if (evt instanceof IdleStateEvent && (event = (IdleStateEvent)evt).state() == IdleState.READER_IDLE) {
            log.info("websocket read time out");
            log.info((String)ctx.channel().attr(WsConstants.USERNAME).get());
            ctx.channel().close();
            String userName = (String)ctx.channel().attr(WsConstants.USERNAME).get();
            if (userName != null) {
                ChannelGroupManager.removeChannel(ctx.channel());
            }
        }
    }
}

