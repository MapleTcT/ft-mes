package com.supcon.supfusion.framework.scaffold.auditlog.propreties;

import com.supcon.supfusion.framework.cloud.common.constants.SystemConstant;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 审计日志配置类
 * @author caokele
 */
@Getter
@Setter
@ConfigurationProperties(value = SystemConstant.CONFIGURATION_PROPERTIES_PREFIX + ".logger", ignoreInvalidFields = true)
public class AuditLogProperties {
}
