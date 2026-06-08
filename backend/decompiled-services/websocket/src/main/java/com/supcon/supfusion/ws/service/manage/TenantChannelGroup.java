/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.alibaba.fastjson.JSON
 *  io.netty.buffer.ByteBuf
 *  io.netty.buffer.ByteBufHolder
 *  io.netty.buffer.Unpooled
 *  io.netty.channel.Channel
 *  io.netty.channel.ChannelFuture
 *  io.netty.channel.ChannelFutureListener
 *  io.netty.channel.group.ChannelMatcher
 *  io.netty.channel.group.ChannelMatchers
 *  io.netty.handler.codec.http.DefaultFullHttpResponse
 *  io.netty.handler.codec.http.HttpHeaderNames
 *  io.netty.handler.codec.http.HttpHeaderValues
 *  io.netty.handler.codec.http.HttpResponseStatus
 *  io.netty.handler.codec.http.HttpVersion
 *  io.netty.handler.codec.http.websocketx.TextWebSocketFrame
 *  io.netty.util.Attribute
 *  io.netty.util.ReferenceCountUtil
 *  io.netty.util.concurrent.GenericFutureListener
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package com.supcon.supfusion.ws.service.manage;

import com.alibaba.fastjson.JSON;
import com.supcon.supfusion.ws.service.constant.WsConstants;
import com.supcon.supfusion.ws.service.manage.TenantChannelGroupFuture;
import com.supcon.supfusion.ws.service.manage.TenantGroup;
import com.supcon.supfusion.ws.service.pojo.FailMessage;
import com.supcon.supfusion.ws.service.pojo.NoticeMessage;
import com.supcon.supfusion.ws.service.pojo.WSResult;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufHolder;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.group.ChannelMatcher;
import io.netty.channel.group.ChannelMatchers;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.Attribute;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.GenericFutureListener;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TenantChannelGroup
extends AbstractSet<Channel>
implements TenantGroup {
    private static final Logger log = LoggerFactory.getLogger(TenantChannelGroup.class);
    private ConcurrentMap<String, List<Channel>> nonServerChannels = new ConcurrentHashMap<String, List<Channel>>();
    private ConcurrentMap<String, List<Channel>> userIdServerChannels = new ConcurrentHashMap<String, List<Channel>>();
    private ChannelFutureListener remover = future -> this.remove(future.channel());

    private static Object safeDuplicate(Object message) {
        if (message instanceof ByteBuf) {
            return ((ByteBuf)message).retainedDuplicate();
        }
        if (message instanceof ByteBufHolder) {
            return ((ByteBufHolder)message).retainedDuplicate();
        }
        return ReferenceCountUtil.retain((Object)message);
    }

    @Override
    public boolean add(Channel channel) {
        String username = (String)channel.attr(WsConstants.USERNAME).get();
        String userId = (String)channel.attr(WsConstants.USERID).get();
        List temp = (List)this.nonServerChannels.get(username);
        if (temp == null) {
            ArrayList channels = new ArrayList();
            ArrayList userIdschannels = new ArrayList();
            this.nonServerChannels.put(username, channels);
            this.userIdServerChannels.put(userId, userIdschannels);
        }
        boolean isAdd = ((List)this.nonServerChannels.get(username)).add(channel);
        ((List)this.userIdServerChannels.get(userId)).add(channel);
        if (isAdd) {
            log.info("add new user {}", (Object)username);
            channel.closeFuture().addListener((GenericFutureListener)this.remover);
        }
        return isAdd;
    }

    @Override
    public boolean remove(Object o) {
        Channel c = null;
        if (o instanceof Channel) {
            c = (Channel)o;
            String username = (String)c.attr(WsConstants.USERNAME).get();
            String userId = (String)c.attr(WsConstants.USERID).get();
            ((List)this.nonServerChannels.get(username)).remove(c);
            ((List)this.userIdServerChannels.get(userId)).remove(c);
        }
        return c != null;
    }

    @Override
    public TenantChannelGroupFuture writeAndFlush(Object message, Channel channel) {
        return this.writeAndFlush(message, ChannelMatchers.all(), channel);
    }

    @Override
    public TenantChannelGroupFuture writeAndFlush(Object message, ChannelMatcher matcher, Channel channel) {
        return this.writeAndFlush(message, matcher, false, channel);
    }

    @Override
    public TenantChannelGroupFuture writeAndFlush(Object message, ChannelMatcher matcher, boolean voidPromise, Channel channel) {
        if (this.nonServerChannels.isEmpty()) {
            return this.getTenantChannelGroupFuture(channel);
        }
        LinkedHashMap<Channel, ChannelFuture> futures = new LinkedHashMap<Channel, ChannelFuture>(this.nonServerChannels.size());
        for (List set : this.nonServerChannels.values()) {
            for (Channel c : set) {
                if (!matcher.matches(c)) continue;
                futures.put(c, c.writeAndFlush(TenantChannelGroup.safeDuplicate(message)));
            }
        }
        ReferenceCountUtil.release((Object)message);
        return new TenantChannelGroupFuture(futures, channel, null);
    }

    @Override
    public TenantChannelGroupFuture writeAndFlush(Object message, List<String> userNames, boolean voidPromise, Channel channel) {
        if (this.nonServerChannels.isEmpty()) {
            return this.getTenantChannelGroupFuture(channel);
        }
        TenantChannelGroupFuture groupFuture = new TenantChannelGroupFuture(channel);
        LinkedHashMap<Channel, ChannelFuture> futures = new LinkedHashMap<Channel, ChannelFuture>(this.nonServerChannels.size());
        for (String userName : userNames) {
            List channels = (List)this.nonServerChannels.get(userName);
            if (channels != null) {
                for (Channel c : channels) {
                    futures.put(c, c.writeAndFlush(TenantChannelGroup.safeDuplicate(message)));
                    ReferenceCountUtil.release((Object)message);
                }
            } else {
                FailMessage failMessage = new FailMessage();
                failMessage.setMsg("no channel");
                failMessage.setUserName(userName);
                groupFuture.add(failMessage);
            }
            if (futures.isEmpty()) {
                groupFuture.setSuccess();
                continue;
            }
            groupFuture.addFutures(futures);
        }
        return groupFuture;
    }

    @Override
    public TenantChannelGroupFuture writeAndFlush(List<NoticeMessage> noticeMessages, boolean voidPromise, Channel channel, String topic) {
        if (this.nonServerChannels.isEmpty()) {
            return this.getTenantChannelGroupFuture(channel);
        }
        TenantChannelGroupFuture groupFuture = new TenantChannelGroupFuture(channel);
        LinkedHashMap<Channel, ChannelFuture> futures = new LinkedHashMap<Channel, ChannelFuture>(this.nonServerChannels.size());
        for (NoticeMessage message : noticeMessages) {
            List channels = (List)this.nonServerChannels.get(message.getUserName());
            if (channels == null) {
                channels = (List)this.userIdServerChannels.get(message.getUserName());
            }
            if (channels != null) {
                for (Channel c : channels) {
                    Attribute topicAttr = c.attr(WsConstants.TOPIC);
                    if (topicAttr == null || !Objects.equals(topicAttr.get(), topic)) continue;
                    TextWebSocketFrame textWebSocketFrame = new TextWebSocketFrame(message.getData().toString());
                    futures.put(c, c.writeAndFlush(TenantChannelGroup.safeDuplicate(textWebSocketFrame)));
                    ReferenceCountUtil.release((Object)textWebSocketFrame);
                }
                continue;
            }
            FailMessage failMessage = new FailMessage();
            failMessage.setMsg("no channel");
            failMessage.setUserName(message.getUserName());
            groupFuture.add(failMessage);
        }
        if (futures.isEmpty()) {
            groupFuture.setSuccess();
        } else {
            groupFuture.addFutures(futures);
        }
        return groupFuture;
    }

    @Override
    public TenantChannelGroupFuture writeAndFlush(String message, boolean voidPromise, Channel channel, String topic) {
        if (this.nonServerChannels.isEmpty()) {
            return this.getTenantChannelGroupFuture(channel);
        }
        LinkedList topicChannels = new LinkedList();
        this.nonServerChannels.forEach((? super K userName, ? super V channels) -> {
            List channelSubList = channels.stream().filter(c -> c.attr(WsConstants.TOPIC) != null && Objects.equals(c.attr(WsConstants.TOPIC).get(), topic)).collect(Collectors.toList());
            topicChannels.addAll(channelSubList);
        });
        if (topicChannels.isEmpty()) {
            return this.getTenantChannelGroupFuture(channel);
        }
        TenantChannelGroupFuture groupFuture = new TenantChannelGroupFuture(channel);
        LinkedHashMap<Channel, ChannelFuture> futures = new LinkedHashMap<Channel, ChannelFuture>(this.nonServerChannels.size());
        for (Channel topicChannel : topicChannels) {
            TextWebSocketFrame textWebSocketFrame = new TextWebSocketFrame(message);
            futures.put(topicChannel, topicChannel.writeAndFlush(TenantChannelGroup.safeDuplicate(textWebSocketFrame)));
            ReferenceCountUtil.release((Object)textWebSocketFrame);
        }
        if (futures.isEmpty()) {
            groupFuture.setSuccess();
        } else {
            groupFuture.addFutures(futures);
        }
        return groupFuture;
    }

    private TenantChannelGroupFuture getTenantChannelGroupFuture(Channel channel) {
        WSResult wsResult = new WSResult();
        wsResult.setCode(100113000);
        wsResult.setMessage("all fail");
        String responseJSON = JSON.toJSONString((Object)wsResult);
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.wrappedBuffer((byte[])responseJSON.getBytes()));
        response.headers().set((CharSequence)HttpHeaderNames.CONTENT_LENGTH, (Object)response.content().readableBytes());
        response.headers().set((CharSequence)HttpHeaderNames.CONTENT_TYPE, (Object)HttpHeaderValues.APPLICATION_JSON);
        response.headers().set((CharSequence)HttpHeaderNames.CONNECTION, (Object)HttpHeaderValues.CLOSE);
        channel.writeAndFlush((Object)response).addListener(f -> {
            if (f.isSuccess()) {
                channel.close();
            }
        });
        return null;
    }

    @Override
    public void remove(Channel channel) {
        String username = (String)channel.attr(WsConstants.USERNAME).get();
        String userId = (String)channel.attr(WsConstants.USERID).get();
        log.info("close channel===={}", (Object)username);
        ((List)this.nonServerChannels.get(username)).remove(channel);
        ((List)this.userIdServerChannels.get(userId)).remove(channel);
    }

    @Override
    public Iterator<Channel> iterator() {
        return null;
    }

    @Override
    public int size() {
        return 0;
    }
}

