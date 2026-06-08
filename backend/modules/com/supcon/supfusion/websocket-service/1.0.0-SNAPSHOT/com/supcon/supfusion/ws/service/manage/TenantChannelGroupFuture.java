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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author lifangyuan
 */
public class TenantChannelGroupFuture {
    private final List<String> successUserNames = new ArrayList<>();
    private final List<FailMessage> failUserNames = new ArrayList<>();
    private final Object lock = new Object();
    private Map<Channel, ChannelFuture> futures;
    private volatile Channel channel;
    private final ChannelFutureListener childListener = new ChannelFutureListener() {
        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
            boolean success = future.isSuccess();
            boolean callSetDone;
            synchronized (lock) {
                if (success) {
                    successUserNames.add(future.channel().attr(WsConstants.USERNAME).get());
                } else {
                    FailMessage failMessage = new FailMessage();
                    failMessage.setUserName(future.channel().attr(WsConstants.USERNAME).get());
                    failMessage.setMsg(future.cause().getMessage());
                    failUserNames.add(failMessage);
                }
                callSetDone = successUserNames.size() + failUserNames.size() >= futures.size();
            }
            if (callSetDone) {
                setSuccess();
            }
        }
    };

    public TenantChannelGroupFuture(Channel channel) {
        this.channel = channel;
    }

    public TenantChannelGroupFuture(Map<Channel, ChannelFuture> futures, Channel channel, EventExecutor eventExecutor) {
        this.futures = futures;
        this.channel = channel;
        Map<Channel, ChannelFuture> futureMap = new LinkedHashMap<Channel, ChannelFuture>();
        for (ChannelFuture f : this.futures.values()) {
            futureMap.put(f.channel(), f);
        }
        this.futures = Collections.unmodifiableMap(futureMap);
        for (ChannelFuture f : this.futures.values()) {
            f.addListener(childListener);
        }
    }

    public void addFutures(Map<Channel, ChannelFuture> futures) {
        this.futures = futures;
        Map<Channel, ChannelFuture> futureMap = new LinkedHashMap<Channel, ChannelFuture>();
        for (ChannelFuture f : this.futures.values()) {
            futureMap.put(f.channel(), f);
        }
        this.futures = Collections.unmodifiableMap(futureMap);
        for (ChannelFuture f : this.futures.values()) {
            f.addListener(childListener);
        }
    }

    public void setSuccess() {
        WSResult wsResult = new WSResult();
        if(failUserNames.isEmpty()){
            wsResult.setMessage("success");
            wsResult.setCode(100000000);
        }else {
            wsResult.setMessage("part fail");
            wsResult.setCode(100113001);
            FailMessages failMessages = new FailMessages();
            failMessages.setFail(failUserNames);
            wsResult.setData(failMessages);
        }
        String responseJSON = JSON.toJSONString(wsResult);
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.wrappedBuffer(responseJSON.getBytes()));
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON);
        response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
        channel.writeAndFlush(response).addListener(f -> {
            if (f.isSuccess()) {
                channel.close();
            }
        });
    }

    public void add(FailMessage failMessage) {
        failUserNames.add(failMessage);
    }


}
