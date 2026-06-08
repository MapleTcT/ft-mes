/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.netflix.client.config.IClientConfig
 *  com.netflix.loadbalancer.AbstractLoadBalancerRule
 *  com.netflix.loadbalancer.ILoadBalancer
 *  com.netflix.loadbalancer.Server
 *  org.apache.commons.lang3.StringUtils
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package com.supcon.custom.ribbon;

import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.AbstractLoadBalancerRule;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.Server;
import com.supcon.custom.ribbon.IpStoreThreadLocal;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FixedIpRibbonRule
extends AbstractLoadBalancerRule {
    private static Logger log = LoggerFactory.getLogger(FixedIpRibbonRule.class);
    private AtomicInteger nextServerCyclicCounter = new AtomicInteger(0);
    private static List<String> localIps = FixedIpRibbonRule.getLocalIP();
    private static String LOCALHOST = "127.0.0.1";

    public static List<String> getLocalIP() {
        ArrayList<String> localIps = new ArrayList<String>();
        try {
            Enumeration<NetworkInterface> netInterfaces = NetworkInterface.getNetworkInterfaces();
            while (netInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = netInterfaces.nextElement();
                Enumeration<InetAddress> ips = networkInterface.getInetAddresses();
                while (ips.hasMoreElements()) {
                    String sIP;
                    InetAddress ip = ips.nextElement();
                    if (null == ip || "".equals(ip) || (sIP = ip.getHostAddress()) == null || sIP.indexOf(":") > -1) continue;
                    localIps.add(sIP);
                }
            }
        }
        catch (Exception e) {
            localIps.add(LOCALHOST);
            log.error(e.getMessage(), (Throwable)e);
        }
        return localIps;
    }

    public Server choose(ILoadBalancer lb, Object key) {
        if (lb == null) {
            log.warn("no load balancer");
            return null;
        }
        Server server = null;
        int count = 0;
        while (server == null && count++ < 10) {
            List reachableServers = lb.getReachableServers();
            List allServers = lb.getAllServers();
            int upCount = reachableServers.size();
            int serverCount = allServers.size();
            if (upCount == 0 || serverCount == 0) {
                log.warn("No up servers available from load balancer: " + lb);
                return null;
            }
            Integer nextServerIndex = this.fixedIp(reachableServers, serverCount);
            server = (Server)allServers.get(nextServerIndex);
            if (server == null) {
                Thread.yield();
                continue;
            }
            if (server.isAlive() && server.isReadyToServe()) {
                return server;
            }
            server = null;
        }
        if (count >= 10) {
            log.warn("No available alive servers after 10 tries from load balancer: " + lb);
        }
        return server;
    }

    private int incrementAndGetModulo(int modulo) {
        int next;
        int current;
        while (!this.nextServerCyclicCounter.compareAndSet(current = this.nextServerCyclicCounter.get(), next = (current + 1) % modulo)) {
        }
        return next;
    }

    private Integer fixedIp(List<Server> reachableServers, int modulo) {
        String userIp = this.getRemoteAddr();
        Integer position = null;
        Integer localhostPosition = null;
        for (int i = 0; i < reachableServers.size(); ++i) {
            Server server = reachableServers.get(i);
            if (null != server && userIp.equals(server.getHost())) {
                position = i;
                break;
            }
            if (null == server || !localIps.contains(server.getHost())) continue;
            localhostPosition = i;
        }
        if (null == position && null != localhostPosition) {
            position = localhostPosition;
        }
        if (null == position) {
            return this.incrementAndGetModulo(modulo);
        }
        return position;
    }

    private String getRemoteAddr() {
        String remoteAddr = IpStoreThreadLocal.getCurrentRemoteIp();
        IpStoreThreadLocal.resetRemoteIp();
        return StringUtils.isNotBlank((CharSequence)remoteAddr) ? remoteAddr : LOCALHOST;
    }

    public Server choose(Object key) {
        return this.choose(this.getLoadBalancer(), key);
    }

    public void initWithNiwsConfig(IClientConfig clientConfig) {
    }
}

