package com.mapletct.ftmes.bpi;

import com.mapletct.ftmes.bpi.infrastructure.security.BpiSecurityProperties;
import com.mapletct.ftmes.bpi.infrastructure.candidate.BpiCandidateEventProperties;
import com.mapletct.ftmes.bpi.infrastructure.telemetry.BpiTelemetryProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({
        BpiSecurityProperties.class,
        BpiTelemetryProperties.class,
        BpiCandidateEventProperties.class
})
public class BpiServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BpiServiceApplication.class, args);
    }
}
