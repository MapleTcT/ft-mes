package com.supcon.supfusion.file.server.common.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class TemplateConfig {

    @Bean("restTemplateProxy1")
    @LoadBalanced
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

}
