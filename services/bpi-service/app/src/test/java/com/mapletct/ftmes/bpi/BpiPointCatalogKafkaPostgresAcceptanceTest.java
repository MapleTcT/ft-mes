package com.mapletct.ftmes.bpi;

import com.mapletct.ftmes.bpi.contract.v1.PointCalibrationStatusV1;
import com.mapletct.ftmes.bpi.contract.v1.PointCatalogPointV1;
import com.mapletct.ftmes.bpi.contract.v1.PointCatalogSnapshotV1;
import com.mapletct.ftmes.bpi.contract.v1.PointDeviceStateV1;
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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "bpi.point-catalog-kafka.enabled=true",
        "bpi.point-catalog-kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "bpi.point-catalog-kafka.allowed-tenant-ids=*",
        "bpi.point-catalog-kafka.allowed-plant-ids=*",
        "bpi.point-catalog-kafka.allowed-line-ids=*",
        "bpi.point-catalog-kafka.concurrency=1",
        "bpi.point-catalog-kafka.retry-backoff=100ms"
})
@EmbeddedKafka(
        partitions = 1,
        topics = {"iot.point-catalog.snapshot.v1", "iot.point-catalog.snapshot.dlq.v1"},
        brokerProperties = {
                "transaction.state.log.replication.factor=1",
                "transaction.state.log.min.isr=1",
                "offsets.topic.replication.factor=1"
        })
@EnabledIfEnvironmentVariable(named = "BPI_TEST_DATABASE_URL", matches = ".+")
class BpiPointCatalogKafkaPostgresAcceptanceTest {

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> System.getenv("BPI_TEST_DATABASE_URL"));
        registry.add("spring.datasource.username", () -> env("BPI_TEST_DATABASE_USER", System.getProperty("user.name")));
        registry.add("spring.datasource.password", () -> env("BPI_TEST_DATABASE_PASSWORD", ""));
        registry.add("bpi.security.internal-jwt-secret", () -> "bpi-point-catalog-test-secret-0123456789");
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired EmbeddedKafkaBroker broker;
    @Autowired
    @Qualifier("bpiPointCatalogKafkaTemplate")
    KafkaTemplate<byte[], byte[]> kafkaTemplate;

    private String tenantId;

    @AfterEach
    void cleanupMarker() {
        if (tenantId == null) return;
        jdbc.update("DELETE FROM bpi.bpi_audit_events WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_api_idempotency WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_point_catalog_entries WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_point_catalog_snapshots WHERE tenant_id = ?", tenantId);
    }

    @Test
    void realKafkaListenerPersistsImmutableSnapshotOnceAndRoutesPoisonRecordToDlq() throws Exception {
        tenantId = "ADP_E2E_POINT_CATALOG_KAFKA_" + UUID.randomUUID().toString().replace("-", "");
        PointCatalogSnapshotV1 event = event();

        kafkaTemplate.send(record(event)).get(10, TimeUnit.SECONDS);
        awaitTableCount("bpi_point_catalog_snapshots", 1);
        awaitTableCount("bpi_point_catalog_entries", 1);
        awaitTableCount("bpi_api_idempotency", 1);

        kafkaTemplate.send(record(event)).get(10, TimeUnit.SECONDS);
        awaitStableTableCount("bpi_point_catalog_snapshots", 1);
        awaitStableTableCount("bpi_point_catalog_entries", 1);
        awaitStableTableCount("bpi_api_idempotency", 1);

        assertThat(jdbc.queryForObject("""
                SELECT source || '|' || source_instance || '|' || source_revision || '|'
                       || point_count || '|' || ready_point_count || '|' || imported_by
                  FROM bpi.bpi_point_catalog_snapshots
                 WHERE tenant_id = ?
                """, String.class, tenantId)).isEqualTo(
                "JETLINKS|jetlinks-pilot-01|" + event.getSourceRevision()
                        + "|1|1|jetlinks-point-catalog-sync");
        assertThat(jdbc.queryForObject("""
                SELECT source_property_id || '|' || calibration_status || '|' || source_sequence_enabled
                  FROM bpi.bpi_point_catalog_entries
                 WHERE tenant_id = ?
                """, String.class, tenantId)).isEqualTo("instantFlow|VERIFIED|true");
        assertThat(jdbc.queryForObject("""
                SELECT action || '|' || object_type || '|' || trace_id
                  FROM bpi.bpi_audit_events
                 WHERE tenant_id = ?
                """, String.class, tenantId)).isEqualTo(
                "POINT_CATALOG_SNAPSHOT_IMPORTED|POINT_CATALOG_SNAPSHOT|" + event.getEventId());

        byte[] poison = new byte[]{1, 2, 3};
        kafkaTemplate.send(new ProducerRecord<>(
                "iot.point-catalog.snapshot.v1",
                0,
                "invalid|catalog".getBytes(StandardCharsets.UTF_8),
                poison)).get(10, TimeUnit.SECONDS);
        ConsumerRecord<byte[], byte[]> dlq = awaitDlqRecord(Duration.ofSeconds(15));
        assertThat(dlq.value()).containsExactly(poison);
    }

    private PointCatalogSnapshotV1 event() {
        PointCatalogPointV1 point = PointCatalogPointV1.newBuilder()
                .setLocalityGroup("line-kafka.feed")
                .setProductId("flow-product")
                .setDeviceId("meter-01")
                .setPropertyId("feed.flow")
                .setSourcePropertyId("instantFlow")
                .setPointName("Instant flow")
                .setUnit("m3/h")
                .setDataType("double")
                .setDeviceState(PointDeviceStateV1.POINT_DEVICE_ACTIVE)
                .setRegistered(true)
                .setPropertyPresent(true)
                .setCalibrationVersion("calibration-v1")
                .setCalibrationStatus(PointCalibrationStatusV1.POINT_CALIBRATION_VERIFIED)
                .setSourceSequenceEnabled(true)
                .build();
        PointCatalogSnapshotV1 content = PointCatalogSnapshotV1.newBuilder()
                .setSource("JETLINKS")
                .setSourceInstance("jetlinks-pilot-01")
                .setTenantId(tenantId)
                .setPlantId("PLANT-KAFKA-01")
                .setLineId("LINE-KAFKA-01")
                .addPoints(point)
                .build();
        String digest = sha256(content.toByteArray());
        return content.toBuilder()
                .setEventId("point-catalog-" + digest)
                .setSourceRevision("sha256:" + digest)
                .setObservedAtMs(Instant.now().minusSeconds(1).toEpochMilli())
                .setReason("Automatic JetLinks point catalog synchronization")
                .build();
    }

    private static String sha256(byte[] value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException(error);
        }
    }

    private ProducerRecord<byte[], byte[]> record(PointCatalogSnapshotV1 event) {
        ProducerRecord<byte[], byte[]> record = new ProducerRecord<>(
                "iot.point-catalog.snapshot.v1",
                null,
                event.getObservedAtMs(),
                key(event).getBytes(StandardCharsets.UTF_8),
                event.toByteArray());
        record.headers()
                .add("event_id", event.getEventId().getBytes(StandardCharsets.UTF_8))
                .add("tenant_id", event.getTenantId().getBytes(StandardCharsets.UTF_8))
                .add("source_revision", event.getSourceRevision().getBytes(StandardCharsets.UTF_8))
                .add("schema_version", "v1".getBytes(StandardCharsets.UTF_8));
        return record;
    }

    private String key(PointCatalogSnapshotV1 event) {
        return String.join(
                "|", event.getTenantId(), event.getPlantId(), event.getLineId(), event.getSourceInstance());
    }

    private ConsumerRecord<byte[], byte[]> awaitDlqRecord(Duration timeout) {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, broker.getBrokersAsString());
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "point-catalog-dlq-" + UUID.randomUUID());
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
        try (KafkaConsumer<byte[], byte[]> consumer = new KafkaConsumer<>(properties)) {
            consumer.subscribe(List.of("iot.point-catalog.snapshot.dlq.v1"));
            Instant deadline = Instant.now().plus(timeout);
            while (Instant.now().isBefore(deadline)) {
                ConsumerRecords<byte[], byte[]> records = consumer.poll(Duration.ofMillis(500));
                for (ConsumerRecord<byte[], byte[]> record : records) {
                    return record;
                }
            }
        }
        throw new AssertionError("No point catalog DLQ record arrived before timeout");
    }

    private void awaitTableCount(String table, long expected) throws InterruptedException {
        Instant deadline = Instant.now().plusSeconds(15);
        while (Instant.now().isBefore(deadline)) {
            if (count(table) == expected) return;
            Thread.sleep(100);
        }
        assertThat(count(table)).isEqualTo(expected);
    }

    private void awaitStableTableCount(String table, long expected) throws InterruptedException {
        awaitTableCount(table, expected);
        Thread.sleep(500);
        assertThat(count(table)).isEqualTo(expected);
    }

    private long count(String table) {
        Long count = jdbc.queryForObject(
                "SELECT count(*) FROM bpi." + table + " WHERE tenant_id = ?", Long.class, tenantId);
        return count == null ? 0 : count;
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null ? fallback : value;
    }
}
