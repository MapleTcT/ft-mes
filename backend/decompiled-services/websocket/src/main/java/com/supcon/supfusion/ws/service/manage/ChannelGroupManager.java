/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.netty.channel.Channel
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package com.supcon.supfusion.ws.service.manage;

import com.supcon.supfusion.ws.service.constant.WsConstants;
import com.supcon.supfusion.ws.service.manage.TenantChannelGroup;
import com.supcon.supfusion.ws.service.manage.TenantGroup;
import io.netty.channel.Channel;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChannelGroupManager {
    private static final Logger log = LoggerFactory.getLogger(ChannelGroupManager.class);
    private static final ConcurrentMap<String, TenantGroup> tenantChannelMap = new ConcurrentHashMap<String, TenantGroup>();

    public static void addChannel(Channel channel) {
        String tenantId = (String)channel.attr(WsConstants.TENANTID).get();
        TenantChannelGroup group = (TenantChannelGroup)tenantChannelMap.get(tenantId);
        if (group == null) {
            TenantChannelGroup channelGroup = new TenantChannelGroup();
            channelGroup.add(channel);
            tenantChannelMap.putIfAbsent(tenantId, channelGroup);
        } else {
            group.add(channel);
        }
    }

    public static void removeChannel(Channel channel) {
        ((TenantGroup)tenantChannelMap.get(channel.attr(WsConstants.TENANTID).get())).remove(channel);
    }

    public static TenantGroup getChannelGroup(String tenantId) {
        return (TenantGroup)tenantChannelMap.get(tenantId);
    }
}

