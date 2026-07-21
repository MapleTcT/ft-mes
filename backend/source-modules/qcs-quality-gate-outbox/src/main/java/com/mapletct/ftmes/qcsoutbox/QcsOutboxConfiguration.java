package com.mapletct.ftmes.qcsoutbox;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ConditionalOnProperty(name = "qcs.bpi.outbox.enabled", havingValue = "true")
public class QcsOutboxConfiguration {

    @Bean
    public RestTemplate qcsBpiRestTemplate(QcsQualityGateOutboxProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.getBpiConnectTimeoutMs());
        factory.setReadTimeout(properties.getBpiReadTimeoutMs());
        return new RestTemplate(factory);
    }

    @Bean
    public ProducerFactory<String, byte[]> qcsQualityGateProducerFactory(
            QcsQualityGateOutboxProperties properties) {
        Map<String, Object> settings = new HashMap<String, Object>();
        settings.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getKafkaBootstrapServers());
        settings.put(ProducerConfig.CLIENT_ID_CONFIG, properties.getKafkaClientId());
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
    public KafkaTemplate<String, byte[]> qcsQualityGateKafkaTemplate(
            ProducerFactory<String, byte[]> qcsQualityGateProducerFactory) {
        return new KafkaTemplate<String, byte[]>(qcsQualityGateProducerFactory);
    }
}
