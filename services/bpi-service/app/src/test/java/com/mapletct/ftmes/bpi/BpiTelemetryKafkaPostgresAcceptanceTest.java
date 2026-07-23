package com.mapletct.ftmes.bpi;

import com.mapletct.ftmes.bpi.contract.v1.PointValue;
import com.mapletct.ftmes.bpi.contract.v1.SequenceOrigin;
import com.mapletct.ftmes.bpi.contract.v1.TelemetryEnvelopeV1;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "bpi.telemetry-kafka.enabled=true",
        "bpi.telemetry-kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "bpi.telemetry-kafka.allowed-tenant-ids=*",
        "bpi.telemetry-kafka.allowed-plant-ids=*",
        "bpi.telemetry-kafka.allowed-line-ids=*",
        "bpi.telemetry-kafka.concurrency=1",
        "bpi.telemetry-kafka.retry-backoff=100ms",
        "bpi.telemetry-kafka.auto-offset-reset=earliest"
})
@EmbeddedKafka(
        partitions = 1,
        topics = {"iot.telemetry.selected.v1", "iot.telemetry.selected.dlq.v1"},
        brokerProperties = {
                "transaction.state.log.replication.factor=1",
                "transaction.state.log.min.isr=1",
                "offsets.topic.replication.factor=1"
        })
@EnabledIfEnvironmentVariable(named = "BPI_TEST_DATABASE_URL", matches = ".+")
class BpiTelemetryKafkaPostgresAcceptanceTest {
    private static final String SOURCE_TOPIC = "iot.telemetry.selected.v1";
    private static final String DLQ_TOPIC = "iot.telemetry.selected.dlq.v1";
    private static final String PLANT_ID = "PLANT-TELEMETRY-01";
    private static final String LINE_ID = "LINE-TELEMETRY-01";
    private static final String GATEWAY_ID = "gateway-telemetry-01";
    private static final String PRODUCT_ID = "flow-product";
    private static final String DEVICE_ID = "flow-meter-01";
    private static final String PROPERTY_ID = "feed.flow";

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> System.getenv("BPI_TEST_DATABASE_URL"));
        registry.add("spring.datasource.username", () -> env(
                "BPI_TEST_DATABASE_USER", System.getProperty("user.name")));
        registry.add("spring.datasource.password", () -> env("BPI_TEST_DATABASE_PASSWORD", ""));
        registry.add("bpi.security.internal-jwt-secret",
                () -> "bpi-telemetry-kafka-test-secret-0123456789");
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired EmbeddedKafkaBroker broker;
    @Autowired
    @Qualifier("bpiTelemetryKafkaTemplate")
    KafkaTemplate<byte[], byte[]> kafkaTemplate;

    private String tenantId;

    @AfterEach
    void cleanupMarker() {
        if (tenantId == null) return;
        jdbc.update("DELETE FROM bpi.bpi_telemetry_quarantine WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_telemetry_point_rejects WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_telemetry_points WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_telemetry_events WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_telemetry_source_state WHERE tenant_id = ?", tenantId);
    }

    @Test
    void kafkaTelemetryPersistsAfterTransactionIsIdempotentAndRoutesPoisonToDlq() throws Exception {
        tenantId = "ADP_E2E_TELEMETRY_KAFKA_" + UUID.randomUUID().toString().replace("-", "");
        Instant observedAt = Instant.now().minusSeconds(1);
        TelemetryEnvelopeV1 first = telemetry(
                "event-telemetry-1", "message-telemetry-1", 7L, 1L, observedAt, 12.5d);

        send(first);
        awaitEventCount(1);
        assertThat(eventState(first.getEventId())).isEqualTo(
                "ACCEPTED|FIRST|DEVICE|7|1|1|0");
        assertThat(pointState(first.getEventId())).isEqualTo(
                PROPERTY_ID + "|12.5|m3/h|GOOD|CAL-TELEMETRY-01");
        assertThat(sourceState()).isEqualTo("7|1|" + first.getEventId() + "|1");

        send(first);
        awaitStableEventCount(1);
        assertThat(pointCount()).isEqualTo(1);

        TelemetryEnvelopeV1 gap = telemetry(
                "event-telemetry-3", "message-telemetry-3", 7L, 3L,
                observedAt.plusSeconds(1), 13.5d);
        send(gap);
        awaitEventCount(2);
        assertThat(eventState(gap.getEventId())).isEqualTo(
                "ACCEPTED|GAP|DEVICE|7|3|1|0");
        assertThat(sourceState()).isEqualTo("7|3|" + gap.getEventId() + "|2");

        byte[] poison = new byte[]{1, 2, 3};
        kafkaTemplate.send(new ProducerRecord<>(
                SOURCE_TOPIC,
                0,
                bytes(PLANT_ID + "|" + DEVICE_ID),
                poison)).get(10, TimeUnit.SECONDS);
        ConsumerRecord<byte[], byte[]> dlq = awaitDlqRecord(Duration.ofSeconds(15));
        assertThat(dlq.value()).containsExactly(poison);
        assertThat(eventCount()).isEqualTo(2);
    }

    private TelemetryEnvelopeV1 telemetry(
            String eventId,
            String messageId,
            long epoch,
            long sequence,
            Instant observedAt,
            double value) {
        long time = observedAt.toEpochMilli();
        return TelemetryEnvelopeV1.newBuilder()
                .setEventId(eventId)
                .setMessageId(messageId)
                .setTenantId(tenantId)
                .setPlantId(PLANT_ID)
                .setLineId(LINE_ID)
                .setGatewayId(GATEWAY_ID)
                .setProductId(PRODUCT_ID)
                .setDeviceId(DEVICE_ID)
                .setEventTimeMs(time)
                .setIngestTimeMs(time)
                .setSourceEpoch(epoch)
                .setSequence(sequence)
                .setSequenceOrigin(SequenceOrigin.DEVICE)
                .addPoints(PointValue.newBuilder()
                        .setPropertyId(PROPERTY_ID)
                        .setDoubleValue(value)
                        .setUnit("m3/h")
                        .setQualityCode("GOOD")
                        .setSampleTimeMs(time)
                        .setCalibrationVersion("CAL-TELEMETRY-01"))
                .putHeaders("locality_group", "line-telemetry.feed")
                .build();
    }

    private void send(TelemetryEnvelopeV1 event) throws Exception {
        kafkaTemplate.send(new ProducerRecord<>(
                SOURCE_TOPIC,
                null,
                event.getEventTimeMs(),
                bytes(event.getPlantId() + "|" + event.getDeviceId()),
                event.toByteArray())).get(10, TimeUnit.SECONDS);
    }

    private String eventState(String eventId) {
        return jdbc.queryForObject("""
                SELECT status || '|' || sequence_disposition || '|' || sequence_origin || '|'
                       || source_epoch || '|' || sequence || '|' || accepted_point_count || '|'
                       || rejected_point_count
                  FROM bpi.bpi_telemetry_events
                 WHERE tenant_id = ? AND event_id = ?
                """, String.class, tenantId, eventId);
    }

    private String pointState(String eventId) {
        return jdbc.queryForObject("""
                SELECT property_id || '|' || numeric_value || '|' || unit || '|'
                       || quality_code || '|' || calibration_version
                  FROM bpi.bpi_telemetry_points
                 WHERE tenant_id = ? AND event_id = ?
                """, String.class, tenantId, eventId);
    }

    private String sourceState() {
        return jdbc.queryForObject("""
                SELECT source_epoch || '|' || last_sequence || '|' || last_event_id || '|' || revision
                  FROM bpi.bpi_telemetry_source_state
                 WHERE tenant_id = ? AND gateway_id = ? AND device_id = ?
                """, String.class, tenantId, GATEWAY_ID, DEVICE_ID);
    }

    private void awaitEventCount(long expected) throws InterruptedException {
        Instant deadline = Instant.now().plusSeconds(15);
        while (Instant.now().isBefore(deadline)) {
            if (eventCount() == expected) return;
            Thread.sleep(100);
        }
        assertThat(eventCount()).isEqualTo(expected);
    }

    private void awaitStableEventCount(long expected) throws InterruptedException {
        awaitEventCount(expected);
        Thread.sleep(500);
        assertThat(eventCount()).isEqualTo(expected);
    }

    private long eventCount() {
        Long value = jdbc.queryForObject(
                "SELECT count(*) FROM bpi.bpi_telemetry_events WHERE tenant_id = ?",
                Long.class,
                tenantId);
        return value == null ? 0L : value;
    }

    private long pointCount() {
        Long value = jdbc.queryForObject(
                "SELECT count(*) FROM bpi.bpi_telemetry_points WHERE tenant_id = ?",
                Long.class,
                tenantId);
        return value == null ? 0L : value;
    }

    private ConsumerRecord<byte[], byte[]> awaitDlqRecord(Duration timeout) {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, broker.getBrokersAsString());
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "telemetry-dlq-" + UUID.randomUUID());
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
        try (KafkaConsumer<byte[], byte[]> consumer = new KafkaConsumer<>(properties)) {
            consumer.subscribe(List.of(DLQ_TOPIC));
            Instant deadline = Instant.now().plus(timeout);
            while (Instant.now().isBefore(deadline)) {
                ConsumerRecords<byte[], byte[]> records = consumer.poll(Duration.ofMillis(500));
                for (ConsumerRecord<byte[], byte[]> record : records) {
                    return record;
                }
            }
        }
        throw new AssertionError("No telemetry DLQ record arrived before timeout");
    }

    private byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null ? fallback : value;
    }
}
