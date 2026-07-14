package com.mapletct.ftmes.contextoutbox;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableConfigurationProperties(ProductionContextOutboxProperties.class)
@SpringBootApplication
public class ProductionContextOutboxApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductionContextOutboxApplication.class, args);
    }
}
