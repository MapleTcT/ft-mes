package com.mapletct.ftmes.womentry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class WomProductionEntryApplication {

    public static void main(String[] args) {
        SpringApplication.run(WomProductionEntryApplication.class, args);
    }
}
