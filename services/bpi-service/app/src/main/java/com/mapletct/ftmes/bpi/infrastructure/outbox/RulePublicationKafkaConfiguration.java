package com.mapletct.ftmes.bpi.infrastructure.outbox;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableScheduling
@ConditionalOnProperty(prefix = "bpi.rule-publication-outbox", name = "enabled", havingValue = "true")
public class RulePublicationKafkaConfiguration {

    @Bean("bpiRulePublicationProducerFactory")
    ProducerFactory<byte[], byte[]> bpiRulePublicationProducerFactory(
            RulePublicationOutboxProperties properties) {
        Map<String, Object> configuration = new HashMap<>();
        configuration.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.bootstrapServers());
        configuration.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
        configuration.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
        configuration.put(ProducerConfig.CLIENT_ID_CONFIG, properties.clientId());
        configuration.put(ProducerConfig.ACKS_CONFIG, "all");
        configuration.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        configuration.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);
        configuration.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 10_000);
        configuration.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 30_000);
        configuration.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 30_000);
        return new DefaultKafkaProducerFactory<>(configuration);
    }

    @Bean("bpiRulePublicationKafkaTemplate")
    KafkaTemplate<byte[], byte[]> bpiRulePublicationKafkaTemplate(
            @Qualifier("bpiRulePublicationProducerFactory")
            ProducerFactory<byte[], byte[]> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }
}
