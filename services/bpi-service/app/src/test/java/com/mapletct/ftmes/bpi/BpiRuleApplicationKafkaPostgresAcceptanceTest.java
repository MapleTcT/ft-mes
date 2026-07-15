package com.mapletct.ftmes.bpi;

import com.mapletct.ftmes.bpi.contract.v1.BoundaryRuleApplicationStatusV1;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryRuleApplicationV1;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "bpi.rule-application-kafka.enabled=true",
        "bpi.rule-application-kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "bpi.rule-application-kafka.topic=bpi.boundary.rule-application.v1",
        "bpi.rule-application-kafka.dlq-topic=bpi.boundary.rule-application.dlq.v1",
        "bpi.rule-application-kafka.runtime-readiness-topic=bpi.boundary.rule-runtime-readiness.v1",
        "bpi.rule-application-kafka.runtime-readiness-dlq-topic=bpi.boundary.rule-runtime-readiness.dlq.v1",
        "bpi.rule-application-kafka.group-id=bpi-rule-application-acceptance",
        "bpi.rule-application-kafka.client-id=bpi-rule-application-acceptance",
        "bpi.rule-application-kafka.allowed-tenant-ids=*",
        "bpi.rule-application-kafka.allowed-plant-ids=*",
        "bpi.rule-application-kafka.allowed-line-ids=*",
        "bpi.rule-application-kafka.concurrency=1",
        "bpi.rule-application-kafka.max-attempts=2",
        "bpi.rule-application-kafka.retry-backoff=100ms",
        "bpi.rule-application-kafka.max-payload-bytes=65536"
})
@EmbeddedKafka(
        partitions = 1,
        topics = {
                "bpi.boundary.rule-application.v1",
                "bpi.boundary.rule-application.dlq.v1",
                "bpi.boundary.rule-runtime-readiness.v1",
                "bpi.boundary.rule-runtime-readiness.dlq.v1"
        },
        brokerProperties = {
                "transaction.state.log.replication.factor=1",
                "transaction.state.log.min.isr=1",
                "offsets.topic.replication.factor=1"
        })
@EnabledIfEnvironmentVariable(named = "BPI_TEST_DATABASE_URL", matches = ".+")
class BpiRuleApplicationKafkaPostgresAcceptanceTest {
    private static final String SOURCE_TOPIC = "bpi.boundary.rule-application.v1";
    private static final String DLQ_TOPIC = "bpi.boundary.rule-application.dlq.v1";
    private static final String LISTENER_ID = "bpi-rule-application-acceptance-listener";

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> System.getenv("BPI_TEST_DATABASE_URL"));
        registry.add("spring.datasource.username", () -> env(
                "BPI_TEST_DATABASE_USER", System.getProperty("user.name")));
        registry.add("spring.datasource.password", () -> env("BPI_TEST_DATABASE_PASSWORD", ""));
        registry.add("bpi.security.internal-jwt-secret", () -> "bpi-rule-application-kafka-test-secret");
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired EmbeddedKafkaBroker broker;
    @Autowired KafkaListenerEndpointRegistry listenerRegistry;

    private String tenantId;
    private UUID ruleId;
    private UUID publicationId;

    @BeforeEach
    void seedPublishedRule() {
        tenantId = "ADP_E2E_BPI_RULE_APP_KAFKA_" + UUID.randomUUID().toString().replace("-", "");
        UUID topologyId = UUID.randomUUID();
        ruleId = UUID.randomUUID();
        publicationId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO bpi.bpi_topology_versions
                    (id, tenant_id, topology_code, version, state, checksum, definition,
                     plant_id, line_id, revision, created_by, updated_by)
                VALUES (?, ?, 'TOPO-RULE-APP', '1', 'PUBLISHED', ?, '{}'::jsonb,
                        'PLANT-RULE-APP', 'LINE-RULE-APP', 1, 'acceptance', 'acceptance')
                """, topologyId, tenantId, "t".repeat(64));
        jdbc.update("""
                INSERT INTO bpi.bpi_rule_versions
                    (id, tenant_id, rule_code, version, topology_version_id, state, checksum,
                     definition, revision, plant_id, line_id, created_by, updated_by)
                VALUES (?, ?, 'RULE-RULE-APP-START', '1', ?, 'PUBLISHED', ?, '{}'::jsonb,
                        1, 'PLANT-RULE-APP', 'LINE-RULE-APP', 'acceptance', 'acceptance')
                """, ruleId, tenantId, topologyId, "r".repeat(64));
        jdbc.update("""
                INSERT INTO bpi.bpi_outbox_events
                    (id, tenant_id, plant_id, line_id, aggregate_type, aggregate_id,
                     event_type, topic, partition_key, payload, status, revision, published_at)
                VALUES (?, ?, 'PLANT-RULE-APP', 'LINE-RULE-APP', 'RULE_VERSION', ?,
                        'BOUNDARY_RULE_PUBLISHED', 'bpi.boundary.rule-publication.v1', ?, ?,
                        'PUBLISHED', 1, now())
                """, publicationId, tenantId, ruleId,
                tenantId + ":LINE-RULE-APP:RULE-RULE-APP-START:1", new byte[] {1, 2, 3});
    }

    @AfterEach
    void cleanupMarker() {
        MessageListenerContainer listener = listenerRegistry.getListenerContainer(LISTENER_ID);
        if (listener != null && !listener.isRunning()) listener.start();
        if (tenantId == null) return;
        jdbc.update("DELETE FROM bpi.bpi_audit_events WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_inbox_events WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_outbox_events WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_rule_versions WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_topology_versions WHERE tenant_id = ?", tenantId);
    }

    @Test
    void committedReceiptSurvivesRestartAndPoisonRecordReachesDlq() throws Exception {
        MessageListenerContainer listener = listenerRegistry.getListenerContainer(LISTENER_ID);
        assertThat(listener).isNotNull();
        ContainerTestUtils.waitForAssignment(listener, 2);

        BoundaryRuleApplicationV1 rejected = application(
                "REJECTED-" + UUID.randomUUID(),
                BoundaryRuleApplicationStatusV1.REJECTED,
                "flink-rule-app-a",
                "RULE_WINDOW_EXCEEDS_STATE_TTL",
                "rule window exceeds state TTL");

        try (KafkaProducer<byte[], byte[]> producer = transactionalProducer()) {
            producer.initTransactions();
            sendTransaction(producer, record(rejected), false);
            awaitStableState("WAITING||1|", 0, 0);

            sendTransaction(producer, record(rejected), true);
            awaitState("REJECTED|flink-rule-app-a|2|RULE_WINDOW_EXCEEDS_STATE_TTL", 1, 1);

            stop(listener);
            sendTransaction(producer, record(rejected), true);
            listener.start();
            ContainerTestUtils.waitForAssignment(listener, 2);
            awaitStableState(
                    "REJECTED|flink-rule-app-a|2|RULE_WINDOW_EXCEEDS_STATE_TTL", 1, 1);

            BoundaryRuleApplicationV1 applied = application(
                    "APPLIED-" + UUID.randomUUID(),
                    BoundaryRuleApplicationStatusV1.APPLIED,
                    "flink-rule-app-b",
                    "",
                    "");
            sendTransaction(producer, record(applied), true);
            awaitState("APPLIED|flink-rule-app-b|3|", 2, 2);

            BoundaryRuleApplicationV1 staleRejected = application(
                    "STALE-REJECTED-" + UUID.randomUUID(),
                    BoundaryRuleApplicationStatusV1.REJECTED,
                    "flink-rule-app-stale",
                    "STALE_REJECTION",
                    "stale rejection after APPLIED");
            sendTransaction(producer, record(staleRejected), true);
            awaitStableState("APPLIED|flink-rule-app-b|3|", 3, 2);

            byte[] poison = new byte[] {1, 2, 3};
            sendTransaction(producer, new ProducerRecord<>(
                    SOURCE_TOPIC,
                    publicationId.toString().getBytes(StandardCharsets.UTF_8),
                    poison), true);
            ConsumerRecord<byte[], byte[]> dlq = awaitDlqRecord(Duration.ofSeconds(15));
            assertThat(dlq.value()).containsExactly(poison);
            assertThat(dlq.partition()).isZero();
            assertThat(header(dlq, "kafka_dlt-original-topic")).isEqualTo(SOURCE_TOPIC);
            assertThat(count("bpi_inbox_events")).isEqualTo(3);
            assertThat(applicationState()).startsWith("APPLIED|flink-rule-app-b|3|");
        }
    }

    private BoundaryRuleApplicationV1 application(
            String eventId,
            BoundaryRuleApplicationStatusV1 status,
            String deploymentId,
            String errorCode,
            String detail) {
        return BoundaryRuleApplicationV1.newBuilder()
                .setEventId(eventId)
                .setPublicationEventId(publicationId.toString())
                .setTenantId(tenantId)
                .setPlantId("PLANT-RULE-APP")
                .setLineId("LINE-RULE-APP")
                .setRuleCode("RULE-RULE-APP-START")
                .setRuleVersion("1")
                .setChecksum("r".repeat(64))
                .setDeploymentId(deploymentId)
                .setStatus(status)
                .setErrorCode(errorCode)
                .setDetail(detail)
                .setObservedAtMs(Instant.now().toEpochMilli())
                .putHeaders("trace_id", "TRACE-" + eventId)
                .build();
    }

    private ProducerRecord<byte[], byte[]> record(BoundaryRuleApplicationV1 event) {
        ProducerRecord<byte[], byte[]> record = new ProducerRecord<>(
                SOURCE_TOPIC,
                publicationId.toString().getBytes(StandardCharsets.UTF_8),
                event.toByteArray());
        record.headers()
                .add("event_id", event.getEventId().getBytes(StandardCharsets.UTF_8))
                .add("publication_event_id", publicationId.toString().getBytes(StandardCharsets.UTF_8))
                .add("tenant_id", tenantId.getBytes(StandardCharsets.UTF_8))
                .add("status", event.getStatus().name().getBytes(StandardCharsets.UTF_8))
                .add("schema_version", "v1".getBytes(StandardCharsets.UTF_8));
        return record;
    }

    private KafkaProducer<byte[], byte[]> transactionalProducer() {
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, broker.getBrokersAsString());
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
        properties.put(ProducerConfig.ACKS_CONFIG, "all");
        properties.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        properties.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, "bpi-rule-application-test-" + UUID.randomUUID());
        return new KafkaProducer<>(properties);
    }

    private void sendTransaction(
            KafkaProducer<byte[], byte[]> producer,
            ProducerRecord<byte[], byte[]> record,
            boolean commit) throws Exception {
        producer.beginTransaction();
        producer.send(record).get(10, TimeUnit.SECONDS);
        if (commit) producer.commitTransaction();
        else producer.abortTransaction();
    }

    private void stop(MessageListenerContainer listener) throws InterruptedException {
        CountDownLatch stopped = new CountDownLatch(1);
        listener.stop(stopped::countDown);
        assertThat(stopped.await(10, TimeUnit.SECONDS)).isTrue();
    }

    private void awaitState(String expected, long inboxCount, long auditCount) throws InterruptedException {
        Instant deadline = Instant.now().plusSeconds(15);
        while (Instant.now().isBefore(deadline)) {
            if (applicationState().equals(expected)
                    && count("bpi_inbox_events") == inboxCount
                    && count("bpi_audit_events") == auditCount) return;
            Thread.sleep(100);
        }
        assertThat(applicationState()).isEqualTo(expected);
        assertThat(count("bpi_inbox_events")).isEqualTo(inboxCount);
        assertThat(count("bpi_audit_events")).isEqualTo(auditCount);
    }

    private void awaitStableState(String expected, long inboxCount, long auditCount)
            throws InterruptedException {
        Thread.sleep(750);
        assertThat(applicationState()).isEqualTo(expected);
        assertThat(count("bpi_inbox_events")).isEqualTo(inboxCount);
        assertThat(count("bpi_audit_events")).isEqualTo(auditCount);
    }

    private String applicationState() {
        return jdbc.queryForObject("""
                SELECT application_status || '|' || COALESCE(application_deployment_id, '') || '|'
                       || revision || '|' || COALESCE(application_error_code, '')
                  FROM bpi.bpi_outbox_events
                 WHERE tenant_id = ? AND id = ?
                """, String.class, tenantId, publicationId);
    }

    private long count(String table) {
        Long count = jdbc.queryForObject(
                "SELECT count(*) FROM bpi." + table + " WHERE tenant_id = ?",
                Long.class,
                tenantId);
        return count == null ? 0 : count;
    }

    private ConsumerRecord<byte[], byte[]> awaitDlqRecord(Duration timeout) {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, broker.getBrokersAsString());
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "bpi-rule-application-dlq-" + UUID.randomUUID());
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
        try (KafkaConsumer<byte[], byte[]> consumer = new KafkaConsumer<>(properties)) {
            consumer.subscribe(List.of(DLQ_TOPIC));
            Instant deadline = Instant.now().plus(timeout);
            while (Instant.now().isBefore(deadline)) {
                ConsumerRecords<byte[], byte[]> records = consumer.poll(Duration.ofMillis(500));
                for (ConsumerRecord<byte[], byte[]> record : records) return record;
            }
        }
        throw new AssertionError("No rule application DLQ record arrived before timeout");
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
