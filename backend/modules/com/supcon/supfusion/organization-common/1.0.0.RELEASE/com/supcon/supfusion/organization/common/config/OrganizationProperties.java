package com.supcon.supfusion.organization.common.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author tomcat
 * @date 21-1-4 下午8:01
 */
@Setter
@Getter
@ToString
@ConfigurationProperties(prefix = "supfusion.organization")
public class OrganizationProperties {

    private String orgType;
}
