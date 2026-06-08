package com.supcon.supfusion.ws.service.http;

import com.supcon.supfusion.ws.service.constant.WsConstants;
import com.supcon.supfusion.ws.service.util.ClassScanner;
import com.supcon.supfusion.ws.service.util.UriParser;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class HttpHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) throws Exception {
        Map<String, ClassScanner.ControllerRegistry> controllerRegistry = ClassScanner.getControllerRegistry();
        String requestUri = req.uri();
        for (Map.Entry<String, ClassScanner.ControllerRegistry> entry : controllerRegistry.entrySet()) {
            String patternUri = entry.getKey();
            ClassScanner.ControllerRegistry value = entry.getValue();
            Pattern pattern = Pattern.compile(patternUri);
            Matcher matcher = pattern.matcher(requestUri);
            if (req.method().name().equals(value.getMethod()) && matcher.find()) {
                Method method = value.getClassMethod();
                method.setAccessible(true);
                int index = patternUri.lastIndexOf(WsConstants.EVERY_CHAR_REGEX);
                if (index < 0) {
                    method.invoke(value.getInstance(), ctx, req);
                } else {
                    method.invoke(value.getInstance(), ctx, req, UriParser.getTopic(index, requestUri));
                }
                break;
            }
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.READER_IDLE) {
                log.info("http read time out");
                ctx.channel().close().addListener(f -> {
                    log.info("read time out close");
                });
            }
        }
    }


}
