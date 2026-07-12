package com.mapletct.ftmes.bpi;

import com.mapletct.ftmes.bpi.infrastructure.security.BpiSecurityProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(BpiSecurityProperties.class)
public class BpiServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BpiServiceApplication.class, args);
    }
}
