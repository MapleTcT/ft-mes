package com.mapletct.ftmes.contextoutbox;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ConditionalOnProperty(name = "mes.production-context.outbox.enabled", havingValue = "true")
public class ProductionContextKafkaConfiguration {

    @Bean
    public ProducerFactory<String, byte[]> productionContextProducerFactory(
        @Value("${mes.production-context.kafka.bootstrap-servers}") String bootstrapServers,
        @Value("${mes.production-context.kafka.client-id:ft-mes-context-outbox}") String clientId
    ) {
        Map<String, Object> settings = new HashMap<String, Object>();
        settings.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        settings.put(ProducerConfig.CLIENT_ID_CONFIG, clientId);
        settings.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        settings.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
        settings.put(ProducerConfig.ACKS_CONFIG, "all");
        settings.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        settings.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE);
        settings.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);
        settings.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "lz4");
        return new DefaultKafkaProducerFactory<String, byte[]>(settings);
    }

    @Bean
    public KafkaTemplate<String, byte[]> productionContextKafkaTemplate(
        ProducerFactory<String, byte[]> productionContextProducerFactory
    ) {
        return new KafkaTemplate<String, byte[]>(productionContextProducerFactory);
    }
}
