/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.alibaba.fastjson.JSON
 *  io.netty.buffer.Unpooled
 *  io.netty.channel.Channel
 *  io.netty.channel.ChannelFuture
 *  io.netty.channel.ChannelFutureListener
 *  io.netty.handler.codec.http.DefaultFullHttpResponse
 *  io.netty.handler.codec.http.HttpHeaderNames
 *  io.netty.handler.codec.http.HttpHeaderValues
 *  io.netty.handler.codec.http.HttpResponseStatus
 *  io.netty.handler.codec.http.HttpVersion
 *  io.netty.util.concurrent.EventExecutor
 *  io.netty.util.concurrent.GenericFutureListener
 */
package com.supcon.supfusion.ws.service.manage;

import com.alibaba.fastjson.JSON;
import com.supcon.supfusion.ws.service.constant.WsConstants;
import com.supcon.supfusion.ws.service.pojo.FailMessage;
import com.supcon.supfusion.ws.service.pojo.FailMessages;
import com.supcon.supfusion.ws.service.pojo.WSResult;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.GenericFutureListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TenantChannelGroupFuture {
    private final List<String> successUserNames = new ArrayList<String>();
    private final List<FailMessage> failUserNames = new ArrayList<FailMessage>();
    private final Object lock = new Object();
    private Map<Channel, ChannelFuture> futures;
    private volatile Channel channel;
    private final ChannelFutureListener childListener = new ChannelFutureListener(){

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        public void operationComplete(ChannelFuture future) throws Exception {
            boolean callSetDone;
            boolean success = future.isSuccess();
            Object object = TenantChannelGroupFuture.this.lock;
            synchronized (object) {
                if (success) {
                    TenantChannelGroupFuture.this.successUserNames.add(future.channel().attr(WsConstants.USERNAME).get());
                } else {
                    FailMessage failMessage = new FailMessage();
                    failMessage.setUserName((String)future.channel().attr(WsConstants.USERNAME).get());
                    failMessage.setMsg(future.cause().getMessage());
                    TenantChannelGroupFuture.this.failUserNames.add(failMessage);
                }
                callSetDone = TenantChannelGroupFuture.this.successUserNames.size() + TenantChannelGroupFuture.this.failUserNames.size() >= TenantChannelGroupFuture.this.futures.size();
            }
            if (callSetDone) {
                TenantChannelGroupFuture.this.setSuccess();
            }
        }
    };

    public TenantChannelGroupFuture(Channel channel) {
        this.channel = channel;
    }

    public TenantChannelGroupFuture(Map<Channel, ChannelFuture> futures, Channel channel, EventExecutor eventExecutor) {
        this.futures = futures;
        this.channel = channel;
        LinkedHashMap<Channel, ChannelFuture> futureMap = new LinkedHashMap<Channel, ChannelFuture>();
        for (ChannelFuture f : this.futures.values()) {
            futureMap.put(f.channel(), f);
        }
        this.futures = Collections.unmodifiableMap(futureMap);
        for (ChannelFuture f : this.futures.values()) {
            f.addListener((GenericFutureListener)this.childListener);
        }
    }

    public void addFutures(Map<Channel, ChannelFuture> futures) {
        this.futures = futures;
        LinkedHashMap<Channel, ChannelFuture> futureMap = new LinkedHashMap<Channel, ChannelFuture>();
        for (ChannelFuture f : this.futures.values()) {
            futureMap.put(f.channel(), f);
        }
        this.futures = Collections.unmodifiableMap(futureMap);
        for (ChannelFuture f : this.futures.values()) {
            f.addListener((GenericFutureListener)this.childListener);
        }
    }

    public void setSuccess() {
        WSResult wsResult = new WSResult();
        if (this.failUserNames.isEmpty()) {
            wsResult.setMessage("success");
            wsResult.setCode(100000000);
        } else {
            wsResult.setMessage("part fail");
            wsResult.setCode(100113001);
            FailMessages failMessages = new FailMessages();
            failMessages.setFail(this.failUserNames);
            wsResult.setData(failMessages);
        }
        String responseJSON = JSON.toJSONString((Object)wsResult);
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.wrappedBuffer((byte[])responseJSON.getBytes()));
        response.headers().set((CharSequence)HttpHeaderNames.CONTENT_LENGTH, (Object)response.content().readableBytes());
        response.headers().set((CharSequence)HttpHeaderNames.CONTENT_TYPE, (Object)HttpHeaderValues.APPLICATION_JSON);
        response.headers().set((CharSequence)HttpHeaderNames.CONNECTION, (Object)HttpHeaderValues.CLOSE);
        this.channel.writeAndFlush((Object)response).addListener(f -> {
            if (f.isSuccess()) {
                this.channel.close();
            }
        });
    }

    public void add(FailMessage failMessage) {
        this.failUserNames.add(failMessage);
    }
}

