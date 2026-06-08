package com.supcon.supfusion.systemconfig.service.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author tomcat
 * @date 21-1-15 上午11:37
 */
@Setter
@Getter
@ToString
@ConfigurationProperties(prefix = SystemConfigProperties.PROP_PREFIX)
public class SystemConfigProperties {

    public static final String PROP_PREFIX = "supfusion.sysconf";

    /**
     * 文件存储目录
     */
    private String fileStoreDir = "/tmp/sysconf";
}
