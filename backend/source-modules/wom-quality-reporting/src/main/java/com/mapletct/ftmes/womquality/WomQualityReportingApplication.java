package com.mapletct.ftmes.womquality;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class WomQualityReportingApplication {

    public static void main(String[] args) {
        SpringApplication.run(WomQualityReportingApplication.class, args);
    }
}
