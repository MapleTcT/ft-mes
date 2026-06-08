package com.supcon.supfusion.configuration.services.config;

import feign.Request;
import org.springframework.context.annotation.Bean;

/**
 * 自定义feign配置
 * @author caokele
 */
public class CustomFeignConfiguration {
    private static final int CONNECT_TIMEOUT_MILLIS = 300000;
    private static final int READ_TIMEOUT_MILLIS = 300000;

    @Bean
    public Request.Options options() {
        return new Request.Options(CONNECT_TIMEOUT_MILLIS, READ_TIMEOUT_MILLIS);
    }
}
