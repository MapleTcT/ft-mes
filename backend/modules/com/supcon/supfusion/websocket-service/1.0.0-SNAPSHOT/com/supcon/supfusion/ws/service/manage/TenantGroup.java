package com.supcon.supfusion.ws.service.manage;

import com.supcon.supfusion.ws.service.pojo.NoticeMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufHolder;
import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroupFuture;
import io.netty.channel.group.ChannelMatcher;

import java.util.List;

public interface TenantGroup {


    /**
     * Writes the specified {@code message} to all {@link Channel}s in this
     * group. If the specified {@code message} is an instance of
     * {@link ByteBuf}, it is automatically
     * {@linkplain ByteBuf#duplicate() duplicated} to avoid a race
     * condition. The same is true for {@link ByteBufHolder}. Please note that this operation is asynchronous as
     * {@link Channel#write(Object)} is.
     *
     * @return itself
     */
    TenantChannelGroupFuture writeAndFlush(Object message, Channel channel);

    /**
     * Writes the specified {@code message} to all {@link Channel}s in this
     * group that are matched by the given {@link ChannelMatcher}. If the specified {@code message} is an instance of
     * {@link ByteBuf}, it is automatically
     * {@linkplain ByteBuf#duplicate() duplicated} to avoid a race
     * condition. The same is true for {@link ByteBufHolder}. Please note that this operation is asynchronous as
     * {@link Channel#write(Object)} is.
     *
     * @return the {@link ChannelGroupFuture} instance that notifies when
     * the operation is done for all channels
     */
    TenantChannelGroupFuture writeAndFlush(Object message, ChannelMatcher matcher, Channel channel);

    /**
     * Writes the specified {@code message} to all {@link Channel}s in this
     * group that are matched by the given {@link ChannelMatcher}. If the specified {@code message} is an instance of
     * {@link ByteBuf}, it is automatically
     * {@linkplain ByteBuf#duplicate() duplicated} to avoid a race
     * condition. The same is true for {@link ByteBufHolder}. Please note that this operation is asynchronous as
     * {@link Channel#write(Object)} is.
     * <p>
     * If {@code voidPromise} is {@code true} {@link Channel#voidPromise()} is used for the writes and so the same
     * restrictions to the returned {@link ChannelGroupFuture} apply as to a void promise.
     *
     * @return the {@link ChannelGroupFuture} instance that notifies when
     * the operation is done for all channels
     */
    TenantChannelGroupFuture writeAndFlush(Object message, ChannelMatcher matcher, boolean voidPromise, Channel channel);

    TenantChannelGroupFuture writeAndFlush(Object message, List<String> userNames, boolean voidPromise, Channel channel);

    TenantChannelGroupFuture writeAndFlush(List<NoticeMessage> noticeMessages, boolean voidPromise, Channel channel, String topic);

    TenantChannelGroupFuture writeAndFlush(String message, boolean voidPromise, Channel channel, String topic);

    void remove(Channel channel);


}
