package com.supcon.supfusion.ws.service.websocket.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.supcon.supfusion.ws.service.annotation.BeforeHandshake;
import com.supcon.supfusion.ws.service.annotation.OnClose;
import com.supcon.supfusion.ws.service.annotation.OnMessage;
import com.supcon.supfusion.ws.service.annotation.OnOpen;
import com.supcon.supfusion.ws.service.annotation.WsController;
import com.supcon.supfusion.ws.service.constant.WsConstants;
import com.supcon.supfusion.ws.service.manage.ChannelGroupManager;
import com.supcon.supfusion.ws.service.util.JwtUtil;
import com.supcon.supfusion.ws.service.util.PropertiesManager;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.internal.StringUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@WsController(uriPattern = "/inter-api/ws/v1/notice/{topic}")
public class NoticeController {

    @BeforeHandshake
    public void beforeHandshake(NioSocketChannel channel, FullHttpRequest req, String topic) {
        try {
            String tenantId = req.headers().get(WsConstants.TENANT_ID);
            String token = req.headers().get(PropertiesManager.getString(WsConstants.JWT_HEADER, WsConstants.AUTHORIZATION));
            String payloadJSON = JwtUtil.parsePayload(token.split(WsConstants.SPACE_STRING)[1]);
            JSONObject payload = JSONObject.parseObject(payloadJSON);
            String userName = payload.getString(WsConstants.CLAIM_KEY_USER_NAME);
            String userId = payload.getString(WsConstants.CLAIM_KEY_USER_ID);
            if (StringUtil.isNullOrEmpty(tenantId)) {
                tenantId = "dt";
            }
            if (!StringUtil.isNullOrEmpty(userName)) {
                channel.attr(WsConstants.TOPIC).set(topic);
                channel.attr(WsConstants.USERNAME).set(userName);
                channel.attr(WsConstants.USERID).set(userId);
                channel.attr(WsConstants.TENANTID).set(tenantId);
                ChannelGroupManager.addChannel(channel);
            }
        } catch (Exception e) {
            log.error("handshake is error", e);
        }
    }

    @OnOpen
    public void onOpen(NioSocketChannel channel, FullHttpRequest req) {

    }

    @OnClose
    public void onClose(NioSocketChannel channel, CloseWebSocketFrame closeWebSocketFrame) {

    }

    @OnMessage
    public void onMessage(NioSocketChannel channel, TextWebSocketFrame textWebSocketFrame) {
        String text = textWebSocketFrame.text();
        JSONObject jsonObject1 = JSON.parseObject(text);
        jsonObject1.put("cmd", "pong");
        jsonObject1.remove("data");
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("code", 200);
        jsonObject.put("codeMsg", "sucesss");
        jsonObject.put("data", new JSONObject());
        jsonObject1.put("response", jsonObject);
        log.info(jsonObject1.toString());
        channel.writeAndFlush(new TextWebSocketFrame(jsonObject1.toString()));
    }
}
