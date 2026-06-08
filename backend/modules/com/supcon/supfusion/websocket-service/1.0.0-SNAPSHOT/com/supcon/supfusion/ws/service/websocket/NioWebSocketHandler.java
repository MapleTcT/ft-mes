package com.supcon.supfusion.ws.service.websocket;

import com.supcon.supfusion.ws.service.constant.WsConstants;
import com.supcon.supfusion.ws.service.http.HttpHandler;
import com.supcon.supfusion.ws.service.manage.ChannelGroupManager;
import com.supcon.supfusion.ws.service.util.ClassScanner;
import com.supcon.supfusion.ws.service.util.UriParser;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
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
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.netty.handler.codec.http.HttpUtil.isKeepAlive;

@Slf4j
public class NioWebSocketHandler extends SimpleChannelInboundHandler<Object> {
    private WebSocketServerHandshaker handshaker;
    private ClassScanner.MethodMapping methodMapping;

    /**
     * 拒绝不合法的请求，并返回错误信息
     */
    private static void sendHttpResponse(ChannelHandlerContext ctx,
                                         FullHttpRequest req, DefaultFullHttpResponse res) {
        // 返回应答给客户端
        if (res.status().code() != 200) {
            ByteBuf buf = Unpooled.copiedBuffer(res.status().toString(),
                    CharsetUtil.UTF_8);
            res.content().writeBytes(buf);
            buf.release();
        }
        ChannelFuture f = ctx.channel().writeAndFlush(res);
        // 如果是非Keep-Alive，关闭连接
        if (!isKeepAlive(req) || res.status().code() != 200) {
            f.addListener(ChannelFutureListener.CLOSE);
        }
    }

    private static String getWebSocketLocation(FullHttpRequest req) {
        String location = req.headers().get(HttpHeaderNames.HOST) + req.uri();
        return "ws://" + location;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        log.info("收到消息：" + msg);
        if (msg instanceof FullHttpRequest) {
            //以http请求形式接入，但是走的是websocket
            handleHttpRequest(ctx, (FullHttpRequest) msg);
        } else if (msg instanceof WebSocketFrame) {
            //处理websocket客户端的消息
            handlerWebSocketFrame(ctx, (WebSocketFrame) msg);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelActive();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        //断开连接
        log.info("客户端断开连接：" + ctx.channel());
        ChannelGroupManager.removeChannel(ctx.channel());
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    private void handlerWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) throws InvocationTargetException, IllegalAccessException {

        // 判断是否关闭链路的指令
        if (frame instanceof CloseWebSocketFrame) {
           // Method onClose = methodMapping.getOnClose();
            ///  onClose.invoke(methodMapping.getInstance(), ctx.channel(), (CloseWebSocketFrame) frame);
            handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
            return;
        }
        // 判断是否ping消息
        if (frame instanceof PingWebSocketFrame) {
            frame.retain();
            ctx.channel().writeAndFlush(new PongWebSocketFrame(frame.content()));
            return;
        }
        if (frame instanceof TextWebSocketFrame) {
            //  Method onMessage = methodMapping.getOnMessage();
            //  onMessage.invoke(methodMapping.getInstance(), ctx.channel(), (TextWebSocketFrame) frame);
            log.info(((TextWebSocketFrame) frame).text());
        }
    }

    /**
     * 唯一的一次http请求，用于创建websocket
     */
    private void handleHttpRequest(ChannelHandlerContext ctx,
                                   FullHttpRequest req) throws InvocationTargetException, IllegalAccessException {
        if (!"websocket".equals(req.headers().get(WsConstants.UPGRADE))) {
            //若不是websocket方式，则创建BAD_REQUEST的req，返回给客户端
            ChannelPipeline pipeline = ctx.channel().pipeline();
            pipeline.remove("ws");
            HttpHandler httpHandler = new HttpHandler();
            pipeline.addLast(httpHandler);
            try {
                req.retain();
                httpHandler.channelRead(ctx, req);
            } catch (Exception e) {
                log.error("handle http error", e);
            }
        } else {
            //要求Upgrade为websocket，过滤掉get/Post
            Map<String, ClassScanner.MethodMapping> wsController = ClassScanner.getWsController();
            String requestUri = req.uri();
            for (Map.Entry<String, ClassScanner.MethodMapping> method : wsController.entrySet()) {
                String patternUri = method.getKey();
                Pattern pattern = Pattern.compile(patternUri);
                Matcher matcher = pattern.matcher(requestUri);
                if (matcher.find()) {
                    methodMapping = method.getValue();
                    Method handshake = methodMapping.getBeforeHandshake();
                    handshake.setAccessible(true);
                    int index = patternUri.lastIndexOf(WsConstants.EVERY_CHAR_REGEX);
                    if (index < 0) {
                        handshake.invoke(methodMapping.getInstance(), ctx.channel(), req);
                    } else {
                        handshake.invoke(methodMapping.getInstance(), ctx.channel(), req, UriParser.getTopic(index, requestUri));
                    }
                    break;
                }
            }
            if (methodMapping == null) {
                WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
                return;
            }

            WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(
                    getWebSocketLocation(req), null, false);
            handshaker = wsFactory.newHandshaker(req);
            if (handshaker == null) {
                WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
            } else {
                handshaker.handshake(ctx.channel(), req);
            }
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.READER_IDLE) {
                log.info("websocket read time out");
                log.info(ctx.channel().attr(WsConstants.USERNAME).get());
                ctx.channel().close();
                String userName = ctx.channel().attr(WsConstants.USERNAME).get();
                if (userName != null) {
                    ChannelGroupManager.removeChannel(ctx.channel());
                }
            }
        }
    }
}
