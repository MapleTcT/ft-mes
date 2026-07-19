package com.mapletct.ftmes.bpi;

import com.mapletct.ftmes.bpi.contract.v1.DataQualityEventV1;
import com.mapletct.ftmes.bpi.contract.v1.DataQualitySeverity;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfEnvironmentVariable(named = "BPI_TARGET_KAFKA_BOOTSTRAP_SERVERS", matches = ".+")
class BpiDataQualityTargetMarkerProducerTest {

    @Test
    void publishesScopedProtobufMarkerToTargetKafka() throws Exception {
        String marker = required("BPI_TARGET_MARKER");
        assertThat(marker).matches("[A-Za-z0-9_-]{8,80}");
        String bootstrapServers = required("BPI_TARGET_KAFKA_BOOTSTRAP_SERVERS");
        String topic = value("BPI_TARGET_KAFKA_TOPIC", "bpi.data-quality.v1");
        String tenantId = value("BPI_TARGET_TENANT_ID", "1000");
        String plantId = value("BPI_TARGET_PLANT_ID", "PLANT-01");
        String lineId = value("BPI_TARGET_LINE_ID", "LINE-S07-01");
        String eventId = marker + "_EVENT_1";
        String sourceEventId = marker + "_SOURCE_1";
        long detectedAt = System.currentTimeMillis();

        DataQualityEventV1 event = DataQualityEventV1.newBuilder()
                .setEventId(eventId)
                .setSourceEventId(sourceEventId)
                .setTenantId(tenantId)
                .setPlantId(plantId)
                .setLineId(lineId)
                .setDeviceId(marker + "_DEVICE")
                .setPropertyId("flow.instant")
                .setIssueCode("CLOCK_DRIFT")
                .setSeverity(DataQualitySeverity.ERROR)
                .setDetail(marker + " target Kafka/PostgreSQL/browser acceptance")
                .setDetectedAtMs(detectedAt)
                .putHeaders("stage", "target-acceptance")
                .putHeaders("rule_key", String.join(
                        "|", tenantId, plantId, lineId, "ADP_E2E_DQ_RULE", "1.0.0"))
                .build();
        String key = String.join("|", tenantId, lineId, sourceEventId,
                event.getPropertyId(), event.getIssueCode());

        Properties configuration = new Properties();
        configuration.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configuration.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
        configuration.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
        configuration.put(ProducerConfig.CLIENT_ID_CONFIG, marker + "-producer");
        configuration.put(ProducerConfig.ACKS_CONFIG, "all");
        configuration.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        configuration.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 30_000);
        configuration.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 10_000);
        configuration.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 30_000);

        RecordMetadata metadata;
        try (KafkaProducer<byte[], byte[]> producer = new KafkaProducer<>(configuration)) {
            ProducerRecord<byte[], byte[]> record = new ProducerRecord<>(
                    topic, key.getBytes(StandardCharsets.UTF_8), event.toByteArray());
            record.headers()
                    .add("event_id", eventId.getBytes(StandardCharsets.UTF_8))
                    .add("issue_code", event.getIssueCode().getBytes(StandardCharsets.UTF_8))
                    .add("tenant_id", tenantId.getBytes(StandardCharsets.UTF_8))
                    .add("schema_version", "v1".getBytes(StandardCharsets.UTF_8));
            metadata = producer.send(record).get(30, TimeUnit.SECONDS);
            producer.flush();
        }

        assertThat(metadata.hasOffset()).isTrue();
        System.out.printf(
                "BPI target marker published marker=%s topic=%s partition=%d offset=%d eventId=%s%n",
                marker, metadata.topic(), metadata.partition(), metadata.offset(), eventId);
    }

    private String required(String name) {
        String result = System.getenv(name);
        if (result == null || result.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return result.trim();
    }

    private String value(String name, String fallback) {
        String result = System.getenv(name);
        return result == null || result.isBlank() ? fallback : result.trim();
    }
}
