/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.netty.channel.Channel
 *  io.netty.channel.group.ChannelMatcher
 */
package com.supcon.supfusion.ws.service.manage;

import com.supcon.supfusion.ws.service.manage.TenantChannelGroupFuture;
import com.supcon.supfusion.ws.service.pojo.NoticeMessage;
import io.netty.channel.Channel;
import io.netty.channel.group.ChannelMatcher;
import java.util.List;

public interface TenantGroup {
    public TenantChannelGroupFuture writeAndFlush(Object var1, Channel var2);

    public TenantChannelGroupFuture writeAndFlush(Object var1, ChannelMatcher var2, Channel var3);

    public TenantChannelGroupFuture writeAndFlush(Object var1, ChannelMatcher var2, boolean var3, Channel var4);

    public TenantChannelGroupFuture writeAndFlush(Object var1, List<String> var2, boolean var3, Channel var4);

    public TenantChannelGroupFuture writeAndFlush(List<NoticeMessage> var1, boolean var2, Channel var3, String var4);

    public TenantChannelGroupFuture writeAndFlush(String var1, boolean var2, Channel var3, String var4);

    public void remove(Channel var1);
}

