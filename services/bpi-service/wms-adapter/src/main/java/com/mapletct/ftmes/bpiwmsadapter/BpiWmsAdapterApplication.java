package com.mapletct.ftmes.bpiwmsadapter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(BpiWmsAdapterProperties.class)
public class BpiWmsAdapterApplication {

    public static void main(String[] args) {
        SpringApplication.run(BpiWmsAdapterApplication.class, args);
    }
}
