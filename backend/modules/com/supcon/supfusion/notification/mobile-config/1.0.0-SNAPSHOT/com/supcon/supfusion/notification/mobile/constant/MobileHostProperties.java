package com.supcon.supfusion.notification.mobile.constant;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @author zhang yafei
 */

@Component
@ConfigurationProperties(prefix = "mobile.hosts")
@Data
@Slf4j
public class MobileHostProperties {
    private String serverIp;
    private String systemService;

    public String getServerIp() {
        InetAddress localHost = null;
        try {
             localHost = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            log.error("获取服务IP失败:{}",e.getMessage());
        }
        if (localHost == null){
            throw new RuntimeException("获取服务IP失败");
        }
        return localHost.getHostAddress();
    }

}
