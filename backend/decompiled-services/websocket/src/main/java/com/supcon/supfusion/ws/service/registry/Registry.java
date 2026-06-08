/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.alibaba.nacos.api.exception.NacosException
 *  com.alibaba.nacos.api.naming.NamingFactory
 *  com.alibaba.nacos.api.naming.NamingService
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package com.supcon.supfusion.ws.service.registry;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.supcon.supfusion.ws.service.util.PropertiesManager;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Registry {
    private static final Logger log = LoggerFactory.getLogger(Registry.class);
    private static NamingService namingService;

    public static NamingService getNamingService() {
        return namingService;
    }

    public static void setNamingService(NamingService namingService) {
        Registry.namingService = namingService;
    }

    public static void registry(String serviceName, String group, int port) {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                Enumeration<InetAddress> ipAddrEnum = ni.getInetAddresses();
                while (ipAddrEnum.hasMoreElements()) {
                    String ip;
                    InetAddress addr = ipAddrEnum.nextElement();
                    if (addr.isLoopbackAddress() || (ip = addr.getHostAddress()).indexOf(":") != -1) continue;
                    Registry.getNamingService().registerInstance(serviceName, group, ip, port);
                }
            }
        }
        catch (NacosException e) {
            log.error("nacos error is ", (Throwable)e);
        }
        catch (SocketException t) {
            log.error("registry ip is error", (Throwable)t);
        }
    }

    static {
        try {
            String address = PropertiesManager.getString("nacos.server-addr", "127.0.0.1:8848");
            namingService = NamingFactory.createNamingService((String)address);
        }
        catch (NacosException e) {
            e.printStackTrace();
        }
    }
}

