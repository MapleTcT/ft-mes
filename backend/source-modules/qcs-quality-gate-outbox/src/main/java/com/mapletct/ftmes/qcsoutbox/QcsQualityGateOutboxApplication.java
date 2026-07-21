package com.mapletct.ftmes.qcsoutbox;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableConfigurationProperties(QcsQualityGateOutboxProperties.class)
@SpringBootApplication
public class QcsQualityGateOutboxApplication {

    public static void main(String[] args) {
        SpringApplication.run(QcsQualityGateOutboxApplication.class, args);
    }
}
