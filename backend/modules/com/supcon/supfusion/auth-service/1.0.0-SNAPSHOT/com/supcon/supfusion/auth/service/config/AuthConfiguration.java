package com.supcon.supfusion.auth.service.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author tomcat
 * @date 21-1-19 下午5:09
 */
@Configuration
@EnableConfigurationProperties(AuthProperties.class)
public class AuthConfiguration {}
