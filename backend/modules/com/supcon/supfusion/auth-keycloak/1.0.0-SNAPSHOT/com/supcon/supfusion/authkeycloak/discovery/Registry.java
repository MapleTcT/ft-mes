package com.supcon.supfusion.authkeycloak.discovery;

import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.supcon.supfusion.authkeycloak.configure.PropertiesConfigure;
import lombok.extern.jbosslog.JBossLog;

@JBossLog
public class Registry {

    private Registry() {

    }

    private static NamingService namingService;

    static {
        try {
            String address = PropertiesConfigure.getProperties().getProperty("nacos.address", "nacos:8848");
            namingService = NamingFactory.createNamingService(address);
        } catch (Exception e) {
            log.info("nacos is not ok");
            log.error("nacos error is ", e);
            System.exit(0);
        }
    }

    public static NamingService getNamingService() {
        return namingService;
    }

    public static void setNamingService(NamingService namingService) {
        Registry.namingService = namingService;
    }
}
