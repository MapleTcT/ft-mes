package com.mapletct.ftmes.bpiadapter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EnableConfigurationProperties(BpiAdapterProperties.class)
public class BpiAdapterApplication {

    public static void main(String[] args) {
        SpringApplication.run(BpiAdapterApplication.class, args);
    }
}
