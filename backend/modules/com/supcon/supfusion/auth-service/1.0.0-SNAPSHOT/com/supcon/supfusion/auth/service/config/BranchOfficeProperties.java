package com.supcon.supfusion.auth.service.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 分厂配置
 * @author caokele
 */
@Setter
@Getter
@ToString
@Configuration
@ConfigurationProperties(prefix = BranchOfficeProperties.PREFIX)
public class BranchOfficeProperties {
    public static final String PREFIX = "supfusion.auth.branch-office";

    /**
     * 分厂id
     */
    private String clientId;

    /**
     * 分厂秘钥
     */
    private String clientSecret;

    /**
     * 回调地址
     */
    private String redirectUri = "/inter-api/auth/v1/branch-office/authorize/callback";

    /**
     * 是否启用
     */
    private Boolean enable = false;

}
