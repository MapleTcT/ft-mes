package com.supcon.supfusion.ws.service.manage;

import com.alibaba.fastjson.JSON;
import com.supcon.supfusion.ws.service.constant.WsConstants;
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
import lombok.extern.slf4j.Slf4j;

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Slf4j
public class TenantChannelGroup extends AbstractSet<Channel> implements TenantGroup {

    private ConcurrentMap<String, List<Channel>> nonServerChannels;
    private ConcurrentMap<String, List<Channel>> userIdServerChannels;
    private ChannelFutureListener remover;

    public TenantChannelGroup() {
        nonServerChannels = new ConcurrentHashMap<>();
        userIdServerChannels = new ConcurrentHashMap<>();
        remover = future -> remove(future.channel());
    }


    private static Object safeDuplicate(Object message) {
        if (message instanceof ByteBuf) {
            return ((ByteBuf) message).retainedDuplicate();
        } else if (message instanceof ByteBufHolder) {
            return ((ByteBufHolder) message).retainedDuplicate();
        } else {
            return ReferenceCountUtil.retain(message);
        }
    }

    @Override
    public boolean add(Channel channel) {
        String username = channel.attr(WsConstants.USERNAME).get();
        String userId = channel.attr(WsConstants.USERID).get();
        List<Channel> temp = nonServerChannels.get(username);
        if (temp == null) {
            List<Channel> channels = new ArrayList<>();
            List<Channel> userIdschannels = new ArrayList<>();
            nonServerChannels.put(username, channels);
            userIdServerChannels.put(userId,userIdschannels);
        }

        boolean isAdd = nonServerChannels.get(username).add(channel);
        userIdServerChannels.get(userId).add(channel);
        if (isAdd) {
            log.info("add new user {}", username);
            channel.closeFuture().addListener(remover);
        }
        return isAdd;
    }

    @Override
    public boolean remove(Object o) {
        Channel c = null;
        if (o instanceof Channel) {
            c = (Channel) o;
            String username = c.attr(WsConstants.USERNAME).get();
            String userId = c.attr(WsConstants.USERID).get();
            nonServerChannels.get(username).remove(c);
            userIdServerChannels.get(userId).remove(c);
        }
        return c != null;
    }


    @Override
    public TenantChannelGroupFuture


    writeAndFlush(Object message, Channel channel) {
        return writeAndFlush(message, ChannelMatchers.all(), channel);
    }

    @Override
    public TenantChannelGroupFuture writeAndFlush(Object message, ChannelMatcher matcher, Channel channel) {
        return writeAndFlush(message, matcher, false, channel);
    }

    @Override
    public TenantChannelGroupFuture writeAndFlush(Object message, ChannelMatcher matcher, boolean voidPromise, Channel channel) {
        if (nonServerChannels.isEmpty()) {
            return getTenantChannelGroupFuture(channel);
        } else {
            Map<Channel, ChannelFuture> futures = new LinkedHashMap<Channel, ChannelFuture>(nonServerChannels.size());
            for (List<Channel> set : nonServerChannels.values()) {
                for (Channel c : set) {
                    if (matcher.matches(c)) {
                        futures.put(c, c.writeAndFlush(safeDuplicate(message)));
                    }
                }
            }
            ReferenceCountUtil.release(message);
            return new TenantChannelGroupFuture(futures, channel, null);
//            }
        }
    }

    @Override
    public TenantChannelGroupFuture writeAndFlush(Object message, List<String> userNames, boolean voidPromise, Channel channel) {
        if (nonServerChannels.isEmpty()) {
            return getTenantChannelGroupFuture(channel);
        } else {
            TenantChannelGroupFuture groupFuture = new TenantChannelGroupFuture(channel);
            Map<Channel, ChannelFuture> futures = new LinkedHashMap<>(nonServerChannels.size());
            for (String userName : userNames) {
                List<Channel> channels = nonServerChannels.get(userName);
                if (channels != null) {
                    for (Channel c : channels) {
                        futures.put(c, c.writeAndFlush(safeDuplicate(message)));
                        ReferenceCountUtil.release(message);
                    }
                } else {
                    FailMessage failMessage = new FailMessage();
                    failMessage.setMsg("no channel");
                    failMessage.setUserName(userName);
                    groupFuture.add(failMessage);
                }
                if (futures.isEmpty()) {
                    groupFuture.setSuccess();
                } else {
                    groupFuture.addFutures(futures);
                }
            }
            return groupFuture;
        }
    }

    @Override
    public TenantChannelGroupFuture writeAndFlush(List<NoticeMessage> noticeMessages, boolean voidPromise, Channel channel, String topic) {
        if (nonServerChannels.isEmpty()) {
            return getTenantChannelGroupFuture(channel);
        }
        TenantChannelGroupFuture groupFuture = new TenantChannelGroupFuture(channel);
        Map<Channel, ChannelFuture> futures = new LinkedHashMap<>(nonServerChannels.size());
        for (NoticeMessage message : noticeMessages) {
            List<Channel> channels = nonServerChannels.get(message.getUserName());
            if(channels==null){
                channels = userIdServerChannels.get(message.getUserName());
            }
            if (channels != null) {
                for (Channel c : channels) {
                    Attribute<String> topicAttr = c.attr(WsConstants.TOPIC);
                    if (topicAttr == null || !Objects.equals(topicAttr.get(), topic)) {
                        continue;
                    }
                    TextWebSocketFrame textWebSocketFrame = new TextWebSocketFrame(message.getData().toString());
                    futures.put(c, c.writeAndFlush(safeDuplicate(textWebSocketFrame)));
                    ReferenceCountUtil.release(textWebSocketFrame);
                }
            } else {
                FailMessage failMessage = new FailMessage();
                failMessage.setMsg("no channel");
                failMessage.setUserName(message.getUserName());
                groupFuture.add(failMessage);
            }
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
        if (nonServerChannels.isEmpty()) {
            return getTenantChannelGroupFuture(channel);
        }
        List<Channel> topicChannels = new LinkedList<>();
        nonServerChannels.forEach((userName, channels) -> {
            List<Channel> channelSubList = channels.stream()
                    .filter(c -> c.attr(WsConstants.TOPIC) != null && Objects.equals(c.attr(WsConstants.TOPIC).get(), topic))
                    .collect(Collectors.toList());
            topicChannels.addAll(channelSubList);
        });
        if (topicChannels.isEmpty()) {
            return getTenantChannelGroupFuture(channel);
        }
        TenantChannelGroupFuture groupFuture = new TenantChannelGroupFuture(channel);
        Map<Channel, ChannelFuture> futures = new LinkedHashMap<>(nonServerChannels.size());
        for (Channel topicChannel : topicChannels) {
            TextWebSocketFrame textWebSocketFrame = new TextWebSocketFrame(message);
            futures.put(topicChannel, topicChannel.writeAndFlush(safeDuplicate(textWebSocketFrame)));
            ReferenceCountUtil.release(textWebSocketFrame);
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
        return null;
    }

    @Override
    public void remove(Channel channel) {
        String username = channel.attr(WsConstants.USERNAME).get();
        String userId = channel.attr(WsConstants.USERID).get();
        log.info("close channel===={}", username);
        nonServerChannels.get(username).remove(channel);
        userIdServerChannels.get(userId).remove(channel);
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
