package com.mapletct.ftmes.bpi;

import com.mapletct.ftmes.bpi.contract.v1.BoundaryRulePublicationV1;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryType;
import com.mapletct.ftmes.bpi.infrastructure.outbox.RulePublicationOutboxDispatcher;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "bpi.rule-publication-outbox.enabled=true",
        "bpi.rule-publication-outbox.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "bpi.rule-publication-outbox.poll-delay=1h",
        "bpi.rule-publication-outbox.claim-timeout=2s",
        "bpi.rule-publication-outbox.retry-backoff=100ms",
        "bpi.rule-publication-outbox.batch-size=10",
        "bpi.rule-publication-outbox.max-attempts=3"
})
@EmbeddedKafka(
        partitions = 1,
        topics = {"bpi.boundary.rule-publication.v1"},
        brokerProperties = {
                "transaction.state.log.replication.factor=1",
                "transaction.state.log.min.isr=1",
                "offsets.topic.replication.factor=1"
        })
@EnabledIfEnvironmentVariable(named = "BPI_TEST_DATABASE_URL", matches = ".+")
class BpiRuleOutboxKafkaPostgresAcceptanceTest {

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> System.getenv("BPI_TEST_DATABASE_URL"));
        registry.add("spring.datasource.username", () -> env("BPI_TEST_DATABASE_USER", System.getProperty("user.name")));
        registry.add("spring.datasource.password", () -> env("BPI_TEST_DATABASE_PASSWORD", ""));
        registry.add("bpi.security.internal-jwt-secret", () -> "bpi-outbox-test-secret-0123456789");
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired EmbeddedKafkaBroker broker;
    @Autowired RulePublicationOutboxDispatcher dispatcher;

    private String tenantId;

    @AfterEach
    void cleanupMarker() {
        if (tenantId != null) {
            jdbc.update("DELETE FROM bpi.bpi_outbox_events WHERE tenant_id = ?", tenantId);
        }
    }

    @Test
    void pendingPostgresEventReachesKafkaAndBecomesPublishedExactlyOnce() throws Exception {
        tenantId = "ADP_E2E_BPI_OUTBOX_" + UUID.randomUUID().toString().replace("-", "");
        UUID outboxId = UUID.randomUUID();
        UUID ruleId = UUID.randomUUID();
        String partitionKey = tenantId + ":LINE-OUTBOX-01:RULE-OUTBOX-START:1";
        BoundaryRulePublicationV1 publication = publication(outboxId);
        jdbc.update("""
                INSERT INTO bpi.bpi_outbox_events
                    (id, tenant_id, plant_id, line_id, aggregate_type, aggregate_id,
                     event_type, topic, partition_key, payload, headers)
                VALUES (?, ?, 'PLANT-OUTBOX-01', 'LINE-OUTBOX-01', 'RULE_VERSION', ?,
                        'BOUNDARY_RULE_PUBLISHED', 'bpi.boundary.rule-publication.v1', ?, ?,
                        CAST(? AS jsonb))
                """, outboxId, tenantId, ruleId, partitionKey, publication.toByteArray(),
                "{\"event_id\":\"" + outboxId + "\",\"tenant_id\":\"" + tenantId
                        + "\",\"event_type\":\"BOUNDARY_RULE_PUBLISHED\",\"schema_version\":\"1\"}");

        try (KafkaConsumer<byte[], byte[]> consumer = consumer()) {
            consumer.subscribe(List.of("bpi.boundary.rule-publication.v1"));
            dispatcher.dispatchPending();

            ConsumerRecord<byte[], byte[]> record = awaitRecord(consumer, outboxId, Duration.ofSeconds(15));
            BoundaryRulePublicationV1 actual = BoundaryRulePublicationV1.parseFrom(record.value());
            assertThat(actual.getEventId()).isEqualTo(outboxId.toString());
            assertThat(actual.getTenantId()).isEqualTo(tenantId);
            assertThat(actual.getRuleCode()).isEqualTo("RULE-OUTBOX-START");
            assertThat(new String(record.key(), StandardCharsets.UTF_8)).isEqualTo(partitionKey);
            assertThat(header(record, "schema_version")).isEqualTo("1");
            assertThat(header(record, "outbox_event_id")).isEqualTo(outboxId.toString());

            awaitPublished(outboxId);
            assertThat(outboxState(outboxId)).isEqualTo("PUBLISHED|1|true");

            dispatcher.dispatchPending();
            assertThat(hasMatchingRecord(consumer, outboxId, Duration.ofSeconds(2))).isFalse();
            assertThat(jdbc.queryForObject(
                    "SELECT count(*) FROM bpi.bpi_outbox_events WHERE id = ?",
                    Long.class, outboxId)).isEqualTo(1L);
        }
    }

    private BoundaryRulePublicationV1 publication(UUID eventId) {
        return BoundaryRulePublicationV1.newBuilder()
                .setEventId(eventId.toString())
                .setTenantId(tenantId)
                .setPlantId("PLANT-OUTBOX-01")
                .setLineId("LINE-OUTBOX-01")
                .setLocalityGroup("SUGAR-HOUSE-01")
                .setTopologyCode("TOPO-OUTBOX")
                .setRuleCode("RULE-OUTBOX-START")
                .setRuleVersion("1")
                .setTopologyVersion("1")
                .setBoundaryType(BoundaryType.START)
                .setPublishedAtMs(Instant.now().toEpochMilli())
                .setChecksum("a".repeat(64))
                .setActive(true)
                .putHeaders("schema_version", "1")
                .build();
    }

    private KafkaConsumer<byte[], byte[]> consumer() {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, broker.getBrokersAsString());
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "bpi-rule-outbox-acceptance-" + UUID.randomUUID());
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
        return new KafkaConsumer<>(properties);
    }

    private ConsumerRecord<byte[], byte[]> awaitRecord(
            KafkaConsumer<byte[], byte[]> consumer,
            UUID eventId,
            Duration timeout) {
        Instant deadline = Instant.now().plus(timeout);
        while (Instant.now().isBefore(deadline)) {
            ConsumerRecords<byte[], byte[]> records = consumer.poll(Duration.ofMillis(250));
            for (ConsumerRecord<byte[], byte[]> record : records) {
                if (eventId.toString().equals(header(record, "outbox_event_id"))) return record;
            }
        }
        throw new AssertionError("No rule publication record arrived before timeout: " + eventId);
    }

    private boolean hasMatchingRecord(
            KafkaConsumer<byte[], byte[]> consumer,
            UUID eventId,
            Duration timeout) {
        Instant deadline = Instant.now().plus(timeout);
        while (Instant.now().isBefore(deadline)) {
            ConsumerRecords<byte[], byte[]> records = consumer.poll(Duration.ofMillis(250));
            for (ConsumerRecord<byte[], byte[]> record : records) {
                if (eventId.toString().equals(header(record, "outbox_event_id"))) return true;
            }
        }
        return false;
    }

    private void awaitPublished(UUID outboxId) throws InterruptedException {
        Instant deadline = Instant.now().plusSeconds(10);
        while (Instant.now().isBefore(deadline)) {
            if (outboxState(outboxId).startsWith("PUBLISHED|")) return;
            Thread.sleep(100);
        }
        assertThat(outboxState(outboxId)).startsWith("PUBLISHED|");
    }

    private String outboxState(UUID outboxId) {
        return jdbc.queryForObject("""
                SELECT status || '|' || attempt_count || '|' || (published_at IS NOT NULL)
                  FROM bpi.bpi_outbox_events
                 WHERE id = ?
                """, String.class, outboxId);
    }

    private String header(ConsumerRecord<byte[], byte[]> record, String name) {
        Header header = record.headers().lastHeader(name);
        return header == null ? null : new String(header.value(), StandardCharsets.UTF_8);
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null ? fallback : value;
    }
}
