package com.mapletct.ftmes.bpi;

import com.mapletct.ftmes.bpi.contract.identity.CandidateKeyFactory;
import com.mapletct.ftmes.bpi.contract.v1.BatchCandidateV1;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryType;
import com.mapletct.ftmes.bpi.contract.v1.CandidateEvidenceV1;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
        "bpi.candidate-kafka.enabled=true",
        "bpi.candidate-kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "bpi.candidate-kafka.allowed-tenant-ids=*",
        "bpi.candidate-kafka.allowed-plant-ids=*",
        "bpi.candidate-kafka.allowed-line-ids=*",
        "bpi.candidate-kafka.concurrency=1",
        "bpi.candidate-kafka.retry-backoff=100ms"
})
@EmbeddedKafka(
        partitions = 3,
        topics = {"bpi.batch.candidate.v1", "bpi.batch.candidate.dlq.v1"},
        brokerProperties = {
                "transaction.state.log.replication.factor=1",
                "transaction.state.log.min.isr=1",
                "offsets.topic.replication.factor=1"
        })
@EnabledIfEnvironmentVariable(named = "BPI_TEST_DATABASE_URL", matches = ".+")
class BpiKafkaPostgresAcceptanceTest {

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> System.getenv("BPI_TEST_DATABASE_URL"));
        registry.add("spring.datasource.username", () -> env("BPI_TEST_DATABASE_USER", System.getProperty("user.name")));
        registry.add("spring.datasource.password", () -> env("BPI_TEST_DATABASE_PASSWORD", ""));
        registry.add("bpi.security.internal-jwt-secret", () -> "bpi-kafka-test-secret-0123456789");
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired EmbeddedKafkaBroker broker;
    @Autowired
    @Qualifier("bpiCandidateKafkaTemplate")
    KafkaTemplate<byte[], byte[]> kafkaTemplate;

    private String tenantId;

    @BeforeEach
    void seedPublishedVersions() {
        tenantId = "ADP_E2E_BPI_KAFKA_" + UUID.randomUUID().toString().replace("-", "");
        UUID topologyId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO bpi.bpi_topology_versions
                    (id, tenant_id, topology_code, version, state, checksum, definition, created_by)
                VALUES (?, ?, 'TOPO-KAFKA', '1', 'PUBLISHED', 'topology-checksum', '{}'::jsonb, 'acceptance')
                """, topologyId, tenantId);
        jdbc.update("""
                INSERT INTO bpi.bpi_rule_versions
                    (id, tenant_id, rule_code, version, topology_version_id, state, checksum, definition, created_by)
                VALUES (?, ?, 'RULE-KAFKA-START', '1', ?, 'PUBLISHED', 'rule-checksum', '{}'::jsonb, 'acceptance')
                """, UUID.randomUUID(), tenantId, topologyId);
    }

    @AfterEach
    void cleanupMarker() {
        if (tenantId == null) return;
        jdbc.update("DELETE FROM bpi.bpi_inbox_events WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_batch_candidates WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_rule_versions WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_topology_versions WHERE tenant_id = ?", tenantId);
    }

    @Test
    void realKafkaListenerPersistsOnceAndRoutesPoisonRecordToDlq() throws Exception {
        BatchCandidateV1 event = event();
        kafkaTemplate.send(record(event)).get(10, TimeUnit.SECONDS);
        awaitTableCount("bpi_inbox_events", 1);
        awaitTableCount("bpi_batch_candidates", 1);

        kafkaTemplate.send(record(event)).get(10, TimeUnit.SECONDS);
        awaitStableTableCount("bpi_inbox_events", 1);
        awaitStableTableCount("bpi_batch_candidates", 1);

        byte[] poison = new byte[]{1, 2, 3};
        kafkaTemplate.send(new ProducerRecord<>(
                "bpi.batch.candidate.v1",
                0,
                "invalid|candidate".getBytes(StandardCharsets.UTF_8),
                poison)).get(10, TimeUnit.SECONDS);
        ConsumerRecord<byte[], byte[]> dlq = awaitDlqRecord(Duration.ofSeconds(15));
        assertThat(dlq.value()).containsExactly(poison);
        assertThat(dlq.partition()).isZero();

        assertThat(jdbc.queryForObject("""
                SELECT evidence->0->>'source'
                  FROM bpi.bpi_batch_candidates
                 WHERE tenant_id = ?
                """, String.class, tenantId)).isEqualTo("bpi-stream-engine");
    }

    private ProducerRecord<byte[], byte[]> record(BatchCandidateV1 event) {
        ProducerRecord<byte[], byte[]> record = new ProducerRecord<>(
                "bpi.batch.candidate.v1",
                null,
                event.getBoundaryEventTimeMs(),
                (event.getLineId() + "|" + event.getRuleCode()).getBytes(StandardCharsets.UTF_8),
                event.toByteArray());
        record.headers()
                .add("event_id", event.getEventId().getBytes(StandardCharsets.UTF_8))
                .add("candidate_key", event.getCandidateKey().getBytes(StandardCharsets.UTF_8))
                .add("tenant_id", event.getTenantId().getBytes(StandardCharsets.UTF_8))
                .add("schema_version", "v1".getBytes(StandardCharsets.UTF_8));
        return record;
    }

    private BatchCandidateV1 event() {
        String evidenceId = "ADP_E2E_KAFKA_EVIDENCE_" + UUID.randomUUID();
        String orderId = "ADP_E2E_KAFKA_ORDER_" + UUID.randomUUID();
        Instant boundaryTime = Instant.now().minusSeconds(5);
        String candidateKey = CandidateKeyFactory.startKey(
                tenantId, "LINE-KAFKA-01", "1", orderId, evidenceId);
        return BatchCandidateV1.newBuilder()
                .setEventId("ADP_E2E_KAFKA_CANDIDATE_" + UUID.randomUUID())
                .setCandidateKey(candidateKey)
                .setTenantId(tenantId)
                .setPlantId("PLANT-KAFKA-01")
                .setLineId("LINE-KAFKA-01")
                .setBoundaryType(BoundaryType.START)
                .setRuleCode("RULE-KAFKA-START")
                .setRuleVersion("1")
                .setTopologyVersion("1")
                .setContextOrderId(orderId)
                .setFirstQuorumEvidenceEventId(evidenceId)
                .setBoundaryEventTimeMs(boundaryTime.toEpochMilli())
                .setConfidence(0.95)
                .addEvidenceEventIds(evidenceId)
                .setEmittedAtMs(Instant.now().toEpochMilli())
                .putHeaders("topology_code", "TOPO-KAFKA")
                .addEvidence(CandidateEvidenceV1.newBuilder()
                        .setEventId(evidenceId)
                        .setSignal("feed.flow")
                        .setClassification("QUORUM")
                        .setSatisfied(true)
                        .setValue("20.1")
                        .setUnit("m3/h")
                        .setQualityCode("GOOD")
                        .setEventTimeMs(boundaryTime.toEpochMilli())
                        .setSource("bpi-stream-engine"))
                .build();
    }

    private ConsumerRecord<byte[], byte[]> awaitDlqRecord(Duration timeout) {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, broker.getBrokersAsString());
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "bpi-dlq-acceptance-" + UUID.randomUUID());
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
        try (KafkaConsumer<byte[], byte[]> consumer = new KafkaConsumer<>(properties)) {
            consumer.subscribe(List.of("bpi.batch.candidate.dlq.v1"));
            Instant deadline = Instant.now().plus(timeout);
            while (Instant.now().isBefore(deadline)) {
                ConsumerRecords<byte[], byte[]> records = consumer.poll(Duration.ofMillis(500));
                for (ConsumerRecord<byte[], byte[]> record : records) {
                    return record;
                }
            }
        }
        throw new AssertionError("No candidate DLQ record arrived before timeout");
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
