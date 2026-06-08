package com.supcon.supfusion.rbac.urlscan.properties;

import com.supcon.supfusion.framework.cloud.common.constants.SystemConstant;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.validation.constraints.NotEmpty;

/**
 * url扫描配置
 *
 * @author yy
 * @version 1.0.0
 * @date 2020-8-8
 */
@ConfigurationProperties(value = SystemConstant.CONFIGURATION_PROPERTIES_PREFIX + ".scan", ignoreInvalidFields = true)
public class ScanProperties {

    /**
     * 远程服务地址
     */
    @NotEmpty
    @Getter
    @Setter
    private String host;

    /**
     * 扫描包范围
     */
    @Getter
    @Setter
    private String forPackage;
}
