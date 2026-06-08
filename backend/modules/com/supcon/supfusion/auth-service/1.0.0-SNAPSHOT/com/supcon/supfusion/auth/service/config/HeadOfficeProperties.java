package com.supcon.supfusion.auth.service.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Collection;
import java.util.Collections;

/**
 * 分厂中总厂配置
 * @author caokele
 */
@Setter
@Getter
@ToString
@Configuration
@ConfigurationProperties(prefix = HeadOfficeProperties.PREFIX)
public class HeadOfficeProperties {
    public static final String PREFIX = "supfusion.auth.head-office";

    /**
     * 总厂地址
     */
    private String address = "http://127.0.0.1:8080";

    /**
     * 总厂认证地址
     */
    private String authorize = "/inter-api/auth/v1/head-office/authorize";

    /**
     * 认证信息地址
     */
    private String loginInfo = "/inter-api/auth/v1/head-office/login-info";

    /**
     * 刷新令牌地址
     */
    private String refreshToken = "/inter-api/auth/v1/head-office/refresh-token";

    /**
     * 总厂注销地址
     */
    private String logout = "/inter-api/auth/v1/head-office/logout";

    /**
     * 允许认证的客户端
     */
    private Collection<BranchOfficeClientProperties> clients = Collections.emptyList();
}
