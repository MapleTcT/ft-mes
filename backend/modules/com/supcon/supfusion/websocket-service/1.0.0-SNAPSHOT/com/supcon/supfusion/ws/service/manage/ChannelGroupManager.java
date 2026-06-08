package com.supcon.supfusion.ws.service.manage;

import com.supcon.supfusion.ws.service.constant.WsConstants;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;


import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


/**
 * @author lifangyuan
 */
@Slf4j
public class ChannelGroupManager {

    private static final ConcurrentMap<String, TenantGroup> tenantChannelMap = new ConcurrentHashMap<>();

    public static void addChannel(Channel channel) {
        String tenantId = channel.attr(WsConstants.TENANTID).get();
        TenantChannelGroup group = (TenantChannelGroup) tenantChannelMap.get(tenantId);
        if (group == null) {
            TenantChannelGroup channelGroup = new TenantChannelGroup();
            channelGroup.add(channel);
            tenantChannelMap.putIfAbsent(tenantId, channelGroup);
        } else {
            group.add(channel);
        }
    }


    public static void removeChannel(Channel channel) {
        tenantChannelMap.get(channel.attr(WsConstants.TENANTID).get()).remove(channel);
    }

    public static TenantGroup getChannelGroup(String tenantId) {
        return tenantChannelMap.get(tenantId);
    }


}
