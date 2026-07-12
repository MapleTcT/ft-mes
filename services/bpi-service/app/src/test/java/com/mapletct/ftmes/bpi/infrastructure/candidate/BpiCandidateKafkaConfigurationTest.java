package com.mapletct.ftmes.bpi.infrastructure.candidate;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;

import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BpiCandidateKafkaConfigurationTest {

    @Test
    void listenerFactoryIsReadCommittedManualAndFailClosedByScope() {
        BpiCandidateKafkaProperties properties = new BpiCandidateKafkaProperties(
                true,
                "kafka-1:19092,kafka-2:19092,kafka-3:19092",
                "bpi.batch.candidate.v1",
                "bpi.batch.candidate.dlq.v1",
                "ft-mes-bpi-service-candidates-v1",
                "ft-mes-bpi-service",
                "bpi-stream-engine",
                Set.of("TENANT-01"),
                Set.of("PLANT-01"),
                Set.of("LINE-01"),
                3,
                4,
                Duration.ofSeconds(2));
        BpiCandidateKafkaConfiguration configuration = new BpiCandidateKafkaConfiguration();
        ProducerFactory<byte[], byte[]> producerFactory =
                configuration.bpiCandidateProducerFactory(properties);
        try {
            KafkaTemplate<byte[], byte[]> template =
                    configuration.bpiCandidateKafkaTemplate(producerFactory);
            DefaultErrorHandler errorHandler =
                    configuration.bpiCandidateKafkaErrorHandler(properties, template);
            ConcurrentKafkaListenerContainerFactory<byte[], byte[]> factory =
                    configuration.bpiCandidateKafkaListenerContainerFactory(properties, errorHandler);

            assertThat(factory.getContainerProperties().getAckMode())
                    .isEqualTo(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
            assertThat(factory.getContainerProperties().isSyncCommits()).isTrue();
            assertThat(properties.concurrency()).isEqualTo(3);
            assertThat(factory.getConsumerFactory().getConfigurationProperties())
                    .containsEntry(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false)
                    .containsEntry(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed")
                    .containsEntry(ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG, false);
            assertThat(properties.allows("TENANT-01", "PLANT-01", "LINE-01")).isTrue();
            assertThat(properties.allows("OTHER", "PLANT-01", "LINE-01")).isFalse();
        } finally {
            producerFactory.reset();
        }
    }

    @Test
    void sourceAndDlqTopicsMustDiffer() {
        assertThatThrownBy(() -> new BpiCandidateKafkaProperties(
                true,
                "kafka:9092",
                "bpi.batch.candidate.v1",
                "bpi.batch.candidate.v1",
                "group",
                "client",
                "actor",
                Set.of("TENANT-01"),
                Set.of("PLANT-01"),
                Set.of("LINE-01"),
                1,
                1,
                Duration.ofSeconds(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must differ");
    }
}
