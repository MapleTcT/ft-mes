package com.supcon.supfusion.auth.service.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author tomcat
 * @date 21-1-19 下午5:10
 */
@Setter
@Getter
@ToString
@ConfigurationProperties(prefix = AuthProperties.PREFIX)
public class AuthProperties {

    public static final String PREFIX = "supfusion.auth";

    /**
     * admin用户最大数量
     */
    Integer maxAdminUserSize = 10;

}
