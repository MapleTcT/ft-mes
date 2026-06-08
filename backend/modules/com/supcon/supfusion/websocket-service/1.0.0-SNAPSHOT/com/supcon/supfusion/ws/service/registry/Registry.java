package com.supcon.supfusion.ws.service.registry;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.supcon.supfusion.ws.service.util.PropertiesManager;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;


@Slf4j
public class Registry {
    private static NamingService namingService;

    static {
        try {
            String address = PropertiesManager.getString("nacos.server-addr", "127.0.0.1:8848");
            namingService = NamingFactory.createNamingService(address);
        } catch (NacosException e) {
            e.printStackTrace();
        }
    }

    public static NamingService getNamingService() {
        return namingService;
    }

    public static void setNamingService(NamingService namingService) {
        Registry.namingService = namingService;
    }

    public static void registry(String serviceName, String group, int port) {
        try {
            Enumeration interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = (NetworkInterface) interfaces.nextElement();
                Enumeration ipAddrEnum = ni.getInetAddresses();
                while (ipAddrEnum.hasMoreElements()) {
                    InetAddress addr = (InetAddress) ipAddrEnum.nextElement();
                    if (addr.isLoopbackAddress() == true) {
                        continue;
                    }

                    String ip = addr.getHostAddress();
                    if (ip.indexOf(":") != -1) {
                        //skip the IPv6 addr
                        continue;
                    }
                    Registry.getNamingService().registerInstance(serviceName, group, ip, port);
                }
            }
        } catch (NacosException e) {
            log.error("nacos error is ", e);
        } catch (SocketException t) {
            log.error("registry ip is error", t);
        }

    }
}
