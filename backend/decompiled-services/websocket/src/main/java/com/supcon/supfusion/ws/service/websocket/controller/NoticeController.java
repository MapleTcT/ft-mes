/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.alibaba.fastjson.JSON
 *  com.alibaba.fastjson.JSONObject
 *  io.netty.channel.Channel
 *  io.netty.channel.socket.nio.NioSocketChannel
 *  io.netty.handler.codec.http.FullHttpRequest
 *  io.netty.handler.codec.http.websocketx.CloseWebSocketFrame
 *  io.netty.handler.codec.http.websocketx.TextWebSocketFrame
 *  io.netty.util.internal.StringUtil
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
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
import io.netty.channel.Channel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.internal.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WsController(uriPattern="/inter-api/ws/v1/notice/{topic}")
public class NoticeController {
    private static final Logger log = LoggerFactory.getLogger(NoticeController.class);

    @BeforeHandshake
    public void beforeHandshake(NioSocketChannel channel, FullHttpRequest req, String topic) {
        try {
            String tenantId = req.headers().get("X-Tenant-Id");
            String token = req.headers().get(PropertiesManager.getString("jwt.header", "Authorization"));
            String payloadJSON = JwtUtil.parsePayload(token.split(" ")[1]);
            JSONObject payload = JSONObject.parseObject((String)payloadJSON);
            String userName = payload.getString("user_name");
            String userId = payload.getString("user_id");
            if (StringUtil.isNullOrEmpty((String)tenantId)) {
                tenantId = "dt";
            }
            if (!StringUtil.isNullOrEmpty((String)userName)) {
                channel.attr(WsConstants.TOPIC).set((Object)topic);
                channel.attr(WsConstants.USERNAME).set((Object)userName);
                channel.attr(WsConstants.USERID).set((Object)userId);
                channel.attr(WsConstants.TENANTID).set((Object)tenantId);
                ChannelGroupManager.addChannel((Channel)channel);
            }
        }
        catch (Exception e) {
            log.error("handshake is error", (Throwable)e);
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
        JSONObject jsonObject1 = JSON.parseObject((String)text);
        jsonObject1.put("cmd", (Object)"pong");
        jsonObject1.remove((Object)"data");
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("code", (Object)200);
        jsonObject.put("codeMsg", (Object)"sucesss");
        jsonObject.put("data", (Object)new JSONObject());
        jsonObject1.put("response", (Object)jsonObject);
        log.info(jsonObject1.toString());
        channel.writeAndFlush((Object)new TextWebSocketFrame(jsonObject1.toString()));
    }
}

