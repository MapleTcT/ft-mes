package com.mapletct.ftmes.bpi.infrastructure.integration;

import com.mapletct.ftmes.bpi.application.error.BpiConflictException;
import com.mapletct.ftmes.bpi.application.error.BpiForbiddenException;
import com.mapletct.ftmes.bpi.application.error.BpiValidationException;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@EnableKafka
@Configuration
@ConditionalOnProperty(prefix = "bpi.phase2-integration", name = "kafka-enabled", havingValue = "true")
public class Phase2IntegrationKafkaConfiguration {

    @Bean("bpiPhase2IntegrationProducerFactory")
    ProducerFactory<byte[], byte[]> bpiPhase2IntegrationProducerFactory(
            BpiPhase2IntegrationProperties properties) {
        Map<String, Object> configuration = new HashMap<>();
        configuration.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.bootstrapServers());
        configuration.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
        configuration.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
        configuration.put(ProducerConfig.CLIENT_ID_CONFIG, properties.clientId() + "-dlq");
        configuration.put(ProducerConfig.ACKS_CONFIG, "all");
        configuration.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        configuration.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 10_000);
        configuration.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 30_000);
        configuration.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 30_000);
        return new DefaultKafkaProducerFactory<>(configuration);
    }

    @Bean("bpiPhase2IntegrationKafkaTemplate")
    KafkaTemplate<byte[], byte[]> bpiPhase2IntegrationKafkaTemplate(
            @Qualifier("bpiPhase2IntegrationProducerFactory")
            ProducerFactory<byte[], byte[]> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    @Bean("bpiPhase2IntegrationKafkaErrorHandler")
    DefaultErrorHandler bpiPhase2IntegrationKafkaErrorHandler(
            BpiPhase2IntegrationProperties properties,
            @Qualifier("bpiPhase2IntegrationKafkaTemplate") KafkaTemplate<byte[], byte[]> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, error) -> new TopicPartition(
                        deadLetterTopic(properties, record.topic()),
                        record.partition()));
        recoverer.setFailIfSendResultIsError(true);
        recoverer.setWaitForSendResultTimeout(Duration.ofSeconds(30));
        DefaultErrorHandler handler = new DefaultErrorHandler(
                recoverer,
                new FixedBackOff(properties.retryBackoff().toMillis(), properties.maxAttempts() - 1L));
        handler.addNotRetryableExceptions(
                Phase2IntegrationKafkaRecordRejectedException.class,
                BpiConflictException.class,
                BpiForbiddenException.class,
                BpiValidationException.class);
        handler.setCommitRecovered(true);
        return handler;
    }

    @Bean("bpiPhase2IntegrationKafkaListenerContainerFactory")
    ConcurrentKafkaListenerContainerFactory<byte[], byte[]> bpiPhase2IntegrationKafkaListenerContainerFactory(
            BpiPhase2IntegrationProperties properties,
            @Qualifier("bpiPhase2IntegrationKafkaErrorHandler") DefaultErrorHandler errorHandler) {
        Map<String, Object> configuration = new HashMap<>();
        configuration.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.bootstrapServers());
        configuration.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
        configuration.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
        configuration.put(ConsumerConfig.GROUP_ID_CONFIG, properties.groupId());
        configuration.put(ConsumerConfig.CLIENT_ID_CONFIG, properties.clientId());
        configuration.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        configuration.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        configuration.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
        configuration.put(ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG, false);
        configuration.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 50);
        ConcurrentKafkaListenerContainerFactory<byte[], byte[]> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(configuration));
        factory.setConcurrency(properties.concurrency());
        factory.setCommonErrorHandler(errorHandler);
        factory.setMissingTopicsFatal(true);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.getContainerProperties().setSyncCommits(true);
        return factory;
    }

    private static String deadLetterTopic(
            BpiPhase2IntegrationProperties properties, String sourceTopic) {
        if (properties.qcsTopic().equals(sourceTopic)) {
            return properties.qcsDlqTopic();
        }
        if (properties.wmsReceiptTopic().equals(sourceTopic)) {
            return properties.wmsReceiptDlqTopic();
        }
        if (properties.wmsReversalReceiptTopic().equals(sourceTopic)) {
            return properties.wmsReversalReceiptDlqTopic();
        }
        throw new IllegalArgumentException(
                "Phase 2 integration received an unconfigured source topic: " + sourceTopic);
    }
}
