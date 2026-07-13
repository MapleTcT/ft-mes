package com.mapletct.ftmes.bpi.stream;

import org.apache.flink.connector.base.DeliveryGuarantee;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;
import org.apache.kafka.clients.producer.ProducerConfig;

public final class BpiKafkaIO {

    private BpiKafkaIO() {
    }

    @SuppressWarnings("deprecation") // Flink 2.2 connector still exposes Kafka's legacy reset enum.
    public static KafkaSource<byte[]> source(
            BpiKafkaJobConfig config,
            String topic,
            String lane) {
        return KafkaSource.<byte[]>builder()
                .setBootstrapServers(config.bootstrapServers())
                .setTopics(topic)
                .setGroupId(config.consumerGroup(lane))
                .setClientIdPrefix(config.consumerGroup(lane))
                .setStartingOffsets(OffsetsInitializer.committedOffsets(OffsetResetStrategy.EARLIEST))
                .setDeserializer(new KafkaIngressDeserializationSchema())
                .setProperty(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed")
                .setProperty(ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG, "false")
                .build();
    }

    public static KafkaSink<byte[]> candidateSink(BpiKafkaJobConfig config) {
        return exactlyOnceSink(
                config,
                "candidate",
                new CandidateKafkaSerializationSchema(config.candidateTopic()));
    }

    public static KafkaSink<byte[]> dataQualitySink(BpiKafkaJobConfig config) {
        return exactlyOnceSink(
                config,
                "data-quality",
                new DataQualityKafkaSerializationSchema(config.dataQualityTopic()));
    }

    public static KafkaSink<byte[]> ruleApplicationSink(BpiKafkaJobConfig config) {
        return exactlyOnceSink(
                config,
                "rule-application",
                new RuleApplicationKafkaSerializationSchema(config.ruleApplicationTopic()));
    }

    private static KafkaSink<byte[]> exactlyOnceSink(
            BpiKafkaJobConfig config,
            String lane,
            KafkaRecordSerializationSchema<byte[]> serializer) {
        return KafkaSink.<byte[]>builder()
                .setBootstrapServers(config.bootstrapServers())
                .setDeliveryGuarantee(DeliveryGuarantee.EXACTLY_ONCE)
                .setTransactionalIdPrefix(config.transactionalIdPrefix(lane))
                .setRecordSerializer(serializer)
                .setProperty(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true")
                .setProperty(ProducerConfig.ACKS_CONFIG, "all")
                .setProperty(ProducerConfig.TRANSACTION_TIMEOUT_CONFIG,
                        Long.toString(config.transactionTimeout().toMillis()))
                .build();
    }
}
