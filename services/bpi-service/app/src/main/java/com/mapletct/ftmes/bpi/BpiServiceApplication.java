package com.mapletct.ftmes.bpi;

import com.mapletct.ftmes.bpi.infrastructure.security.BpiSecurityProperties;
import com.mapletct.ftmes.bpi.infrastructure.candidate.BpiCandidateEventProperties;
import com.mapletct.ftmes.bpi.infrastructure.candidate.BpiCandidateKafkaProperties;
import com.mapletct.ftmes.bpi.infrastructure.telemetry.BpiTelemetryProperties;
import com.mapletct.ftmes.bpi.infrastructure.outbox.RulePublicationOutboxProperties;
import com.mapletct.ftmes.bpi.infrastructure.application.BpiRuleApplicationKafkaProperties;
import com.mapletct.ftmes.bpi.infrastructure.pointcatalog.BpiPointCatalogKafkaProperties;
import com.mapletct.ftmes.bpi.infrastructure.dataquality.BpiDataQualityKafkaProperties;
import com.mapletct.ftmes.bpi.infrastructure.sourcesequence.BpiSourceSequenceKafkaProperties;
import com.mapletct.ftmes.bpi.infrastructure.integration.BpiPhase2IntegrationProperties;
import com.mapletct.ftmes.bpi.infrastructure.integration.BpiWmsOutboxProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({
        BpiSecurityProperties.class,
        BpiTelemetryProperties.class,
        BpiCandidateEventProperties.class,
        BpiCandidateKafkaProperties.class,
        BpiDataQualityKafkaProperties.class,
        BpiPointCatalogKafkaProperties.class,
        BpiSourceSequenceKafkaProperties.class,
        BpiRuleApplicationKafkaProperties.class,
        RulePublicationOutboxProperties.class,
        BpiPhase2IntegrationProperties.class,
        BpiWmsOutboxProperties.class
})
public class BpiServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BpiServiceApplication.class, args);
    }
}
