package com.supcon.supfusion.notification.app.config.constant;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component("appSuposConfiguration")
@ConfigurationProperties(
        prefix = "supfusion.supos"
)
@Data
public class SuposConfiguration {
    private String ak;
    private String sk;
    private String suposHost;
    private String appId="notification-app";
}
