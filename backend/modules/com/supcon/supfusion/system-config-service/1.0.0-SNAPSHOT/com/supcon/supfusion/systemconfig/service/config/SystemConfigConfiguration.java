package com.supcon.supfusion.systemconfig.service.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author tomcat
 * @date 21-1-15 上午11:37
 */
@Configuration
@EnableConfigurationProperties(SystemConfigProperties.class)
public class SystemConfigConfiguration {}
