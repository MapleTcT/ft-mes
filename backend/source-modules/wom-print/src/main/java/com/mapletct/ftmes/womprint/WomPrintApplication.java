package com.mapletct.ftmes.womprint;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class WomPrintApplication {

    public static void main(String[] args) {
        SpringApplication.run(WomPrintApplication.class, args);
    }
}
