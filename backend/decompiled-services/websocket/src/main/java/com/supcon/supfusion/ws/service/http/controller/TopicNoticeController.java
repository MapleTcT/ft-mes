/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.alibaba.fastjson.JSONObject
 *  io.netty.buffer.Unpooled
 *  io.netty.channel.Channel
 *  io.netty.channel.ChannelHandlerContext
 *  io.netty.handler.codec.http.DefaultFullHttpResponse
 *  io.netty.handler.codec.http.FullHttpRequest
 *  io.netty.handler.codec.http.HttpHeaderNames
 *  io.netty.handler.codec.http.HttpHeaderValues
 *  io.netty.handler.codec.http.HttpResponseStatus
 *  io.netty.handler.codec.http.HttpVersion
 *  io.netty.util.CharsetUtil
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package com.supcon.supfusion.ws.service.http.controller;

import com.alibaba.fastjson.JSONObject;
import com.supcon.supfusion.ws.service.annotation.Controller;
import com.supcon.supfusion.ws.service.manage.ChannelGroupManager;
import com.supcon.supfusion.ws.service.manage.TenantGroup;
import com.supcon.supfusion.ws.service.pojo.FailMessage;
import com.supcon.supfusion.ws.service.pojo.FailMessages;
import com.supcon.supfusion.ws.service.pojo.WSResult;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.CharsetUtil;
import java.util.Collections;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller(method="POST", uriPattern="/service-api/ws/v1/notice/topic/{topic}")
public class TopicNoticeController {
    private static final Logger log = LoggerFactory.getLogger(TopicNoticeController.class);

    public void hand(ChannelHandlerContext ctx, FullHttpRequest req, String topic) {
        String message = req.content().toString(CharsetUtil.UTF_8);
        String tenantId = Optional.ofNullable(req.headers().get("X-Tenant-Id")).orElse("admin");
        TenantGroup channelGroup = ChannelGroupManager.getChannelGroup(tenantId);
        if (channelGroup != null) {
            channelGroup.writeAndFlush(message, false, ctx.channel(), topic);
        } else {
            this.doFailedResponse(ctx);
        }
    }

    private void doFailedResponse(ChannelHandlerContext ctx) {
        FailMessage failMessage = new FailMessage();
        failMessage.setMsg("no connect");
        FailMessages failMessages = new FailMessages();
        failMessages.setFail(Collections.singletonList(failMessage));
        WSResult result = new WSResult();
        result.setCode(100113001);
        result.setMessage("part fail");
        result.setData(failMessages);
        String responseJSON = JSONObject.toJSONString((Object)failMessage);
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.wrappedBuffer((byte[])responseJSON.getBytes()));
        response.headers().set((CharSequence)HttpHeaderNames.CONTENT_LENGTH, (Object)response.content().readableBytes());
        response.headers().set((CharSequence)HttpHeaderNames.CONTENT_TYPE, (Object)HttpHeaderValues.APPLICATION_JSON);
        response.headers().set((CharSequence)HttpHeaderNames.CONNECTION, (Object)HttpHeaderValues.CLOSE);
        Channel channel = ctx.channel();
        channel.writeAndFlush((Object)response).addListener(f -> {
            if (f.isSuccess()) {
                channel.close();
            }
        });
    }
}

