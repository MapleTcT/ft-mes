package com.supcon.supfusion.ws.service.http.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.supcon.supfusion.ws.service.annotation.Controller;
import com.supcon.supfusion.ws.service.constant.WsConstants;
import com.supcon.supfusion.ws.service.manage.ChannelGroupManager;
import com.supcon.supfusion.ws.service.manage.TenantGroup;
import com.supcon.supfusion.ws.service.pojo.FailMessage;
import com.supcon.supfusion.ws.service.pojo.FailMessages;
import com.supcon.supfusion.ws.service.pojo.NoticeMessage;
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
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 给指定主题下指定用户发送通知
 */
@Slf4j
@Controller(method = "POST", uriPattern = "/service-api/ws/v1/notice/topic-user/{topic}")
public class TopicUserNoticeController {

    public void hand(ChannelHandlerContext ctx, FullHttpRequest req, String topic) {
        String content = req.content().toString(CharsetUtil.UTF_8);
        JSONArray jsonArray = JSONObject.parseArray(content);
        List<NoticeMessage> noticeMessages = JSON.parseArray(jsonArray.toJSONString(), NoticeMessage.class);
        String tenantId = Optional.ofNullable(req.headers().get(WsConstants.TENANT_ID)).orElse(WsConstants.ADMIN);
        TenantGroup channelGroup = ChannelGroupManager.getChannelGroup(tenantId);
        if (channelGroup != null) {
            channelGroup.writeAndFlush(noticeMessages, false, ctx.channel(), topic);
        } else {
            doFailedResponse(ctx, noticeMessages);
        }
    }

    private void doFailedResponse(ChannelHandlerContext ctx, List<NoticeMessage> noticeMessages) {
        List<FailMessage> failMessageList = noticeMessages.stream()
                .map(noticeMessage -> {
                    FailMessage failMessage = new FailMessage();
                    failMessage.setUserName(noticeMessage.getUserName());
                    failMessage.setMsg("no connect");
                    return failMessage;
                }).collect(Collectors.toList());
        FailMessages failMessages = new FailMessages();
        failMessages.setFail(failMessageList);
        WSResult result = new WSResult();
        result.setCode(100113001);
        result.setMessage("part fail");
        result.setData(failMessages);
        String responseJSON = JSONObject.toJSONString(result);
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.wrappedBuffer(responseJSON.getBytes()));
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON);
        response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
        Channel channel = ctx.channel();
        channel.writeAndFlush(response).addListener(f -> {
            if (f.isSuccess()) {
                channel.close();
            }
        });
    }
}
