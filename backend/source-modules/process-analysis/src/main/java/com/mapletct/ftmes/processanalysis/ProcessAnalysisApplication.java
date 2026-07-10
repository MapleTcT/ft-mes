package com.mapletct.ftmes.processanalysis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class ProcessAnalysisApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProcessAnalysisApplication.class, args);
    }
}
