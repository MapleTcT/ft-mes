package com.mapletct.ftmes.bpi;

import com.mapletct.ftmes.bpi.application.ActorContext;
import com.mapletct.ftmes.bpi.application.Checksums;
import com.mapletct.ftmes.bpi.contract.v1.SequenceOrigin;
import com.mapletct.ftmes.bpi.contract.v1.SourceSequenceEvidenceStatusV1;
import com.mapletct.ftmes.bpi.contract.v1.SourceSequenceEvidenceV1;
import com.mapletct.ftmes.bpi.domain.PointCatalogPointView;
import com.mapletct.ftmes.bpi.domain.PointCatalogSnapshotView;
import com.mapletct.ftmes.bpi.infrastructure.postgres.PointCatalogPostgresRepository;
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
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "bpi.source-sequence-kafka.enabled=true",
        "bpi.source-sequence-kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "bpi.source-sequence-kafka.allowed-tenant-ids=*",
        "bpi.source-sequence-kafka.allowed-plant-ids=*",
        "bpi.source-sequence-kafka.allowed-line-ids=*",
        "bpi.source-sequence-kafka.concurrency=1",
        "bpi.source-sequence-kafka.retry-backoff=100ms"
})
@EmbeddedKafka(
        partitions = 1,
        topics = {"iot.source-sequence.evidence.v1", "iot.source-sequence.evidence.dlq.v1"},
        brokerProperties = {
                "transaction.state.log.replication.factor=1",
                "transaction.state.log.min.isr=1",
                "offsets.topic.replication.factor=1"
        })
@EnabledIfEnvironmentVariable(named = "BPI_TEST_DATABASE_URL", matches = ".+")
class BpiSourceSequenceKafkaPostgresAcceptanceTest {
    private static final String SOURCE_TOPIC = "iot.source-sequence.evidence.v1";
    private static final String DLQ_TOPIC = "iot.source-sequence.evidence.dlq.v1";
    private static final String SOURCE_INSTANCE = "jetlinks-pilot-01";
    private static final String PLANT_ID = "PLANT-SOURCE-SEQUENCE-01";
    private static final String LINE_ID = "LINE-SOURCE-SEQUENCE-01";
    private static final String PRODUCT_ID = "flow-product";
    private static final String DEVICE_ID = "meter-01";
    private static final String PROPERTY_ID = "feed.flow";
    private static final String FINGERPRINT =
            "sha256:0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> System.getenv("BPI_TEST_DATABASE_URL"));
        registry.add("spring.datasource.username", () -> env(
                "BPI_TEST_DATABASE_USER", System.getProperty("user.name")));
        registry.add("spring.datasource.password", () -> env("BPI_TEST_DATABASE_PASSWORD", ""));
        registry.add("bpi.security.internal-jwt-secret",
                () -> "bpi-source-sequence-test-secret-0123456789");
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired EmbeddedKafkaBroker broker;
    @Autowired PointCatalogPostgresRepository pointCatalogRepository;
    @Autowired
    @Qualifier("bpiSourceSequenceKafkaTemplate")
    KafkaTemplate<byte[], byte[]> kafkaTemplate;

    private String tenantId;

    @AfterEach
    void cleanupMarker() {
        if (tenantId == null) return;
        jdbc.update("DELETE FROM bpi.bpi_audit_events WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_inbox_events WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_source_sequence_evidence_current WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_point_catalog_entries WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_point_catalog_snapshots WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_point_calibrations WHERE tenant_id = ?", tenantId);
    }

    @Test
    void kafkaEvidenceIsIdempotentOrderedAuditedAndControlsPointReadiness() throws Exception {
        tenantId = "ADP_E2E_SOURCE_SEQUENCE_" + UUID.randomUUID().toString().replace("-", "");
        Instant snapshotObservedAt = Instant.now().minusSeconds(120);
        insertReadyPointFixture(snapshotObservedAt);

        PointCatalogPointView missing = currentPoint();
        assertThat(missing.ready()).isFalse();
        assertThat(missing.sourceSequenceQualified()).isFalse();
        assertThat(missing.sourceSequenceEvidenceStatus()).isNull();
        assertThat(missing.readinessIssues()).containsExactly("SOURCE_SEQUENCE_EVIDENCE_MISSING");

        Instant firstObservedAt = snapshotObservedAt.plusSeconds(30);
        SourceSequenceEvidenceV1 qualified = evidence(
                SourceSequenceEvidenceStatusV1.SOURCE_SEQUENCE_EVIDENCE_QUALIFIED,
                firstObservedAt,
                43L,
                2,
                firstObservedAt.plus(Duration.ofMinutes(30)));
        send(qualified);
        awaitEvidenceRevision(1L);

        assertThat(evidenceState()).isEqualTo("QUALIFIED|GATEWAY|7|42|43|2|1");
        assertThat(count("bpi_inbox_events")).isEqualTo(1);
        assertThat(evidenceAuditCount()).isEqualTo(1);
        assertThat(currentSnapshot().readyPointCount()).isEqualTo(1);
        PointCatalogPointView ready = currentPoint();
        assertThat(ready.ready()).isTrue();
        assertThat(ready.sourceSequenceQualified()).isTrue();
        assertThat(ready.sourceSequenceEvidenceStatus()).isEqualTo("QUALIFIED");
        assertThat(ready.sourceSequenceEvidenceEventId()).isEqualTo(qualified.getEventId());

        send(qualified);
        awaitStableEvidenceRevision(1L);
        assertThat(count("bpi_inbox_events")).isEqualTo(1);
        assertThat(evidenceAuditCount()).isEqualTo(1);

        Instant heartbeatObservedAt = firstObservedAt.plusSeconds(20);
        SourceSequenceEvidenceV1 heartbeat = evidence(
                SourceSequenceEvidenceStatusV1.SOURCE_SEQUENCE_EVIDENCE_QUALIFIED,
                heartbeatObservedAt,
                44L,
                3,
                heartbeatObservedAt.plus(Duration.ofMinutes(30)));
        send(heartbeat);
        awaitEvidenceRevision(2L);
        assertThat(count("bpi_inbox_events")).isEqualTo(2);
        assertThat(evidenceAuditCount()).isEqualTo(1);
        assertThat(currentPoint().sourceSequenceEvidenceEventId()).isEqualTo(heartbeat.getEventId());

        SourceSequenceEvidenceV1 stale = evidence(
                SourceSequenceEvidenceStatusV1.SOURCE_SEQUENCE_EVIDENCE_PENDING,
                firstObservedAt.plusSeconds(10),
                43L,
                2,
                firstObservedAt.plus(Duration.ofMinutes(20)));
        send(stale);
        awaitTableCount("bpi_inbox_events", 3);
        awaitStableEvidenceRevision(2L);
        assertThat(evidenceAuditCount()).isEqualTo(1);

        Instant expiredObservedAt = Instant.now().minusSeconds(1);
        SourceSequenceEvidenceV1 expired = evidence(
                SourceSequenceEvidenceStatusV1.SOURCE_SEQUENCE_EVIDENCE_EXPIRED,
                expiredObservedAt,
                44L,
                3,
                expiredObservedAt.minusMillis(1));
        send(expired);
        awaitEvidenceRevision(3L);
        assertThat(count("bpi_inbox_events")).isEqualTo(4);
        assertThat(evidenceAuditCount()).isEqualTo(2);
        assertThat(currentSnapshot().readyPointCount()).isZero();
        PointCatalogPointView blocked = currentPoint();
        assertThat(blocked.ready()).isFalse();
        assertThat(blocked.sourceSequenceEvidenceStatus()).isEqualTo("EXPIRED");
        assertThat(blocked.readinessIssues()).containsExactly("SOURCE_SEQUENCE_EVIDENCE_EXPIRED");

        byte[] poison = new byte[]{1, 2, 3};
        kafkaTemplate.send(new ProducerRecord<>(
                SOURCE_TOPIC,
                0,
                "invalid|source-sequence".getBytes(StandardCharsets.UTF_8),
                poison)).get(10, TimeUnit.SECONDS);
        ConsumerRecord<byte[], byte[]> dlq = awaitDlqRecord(Duration.ofSeconds(15));
        assertThat(dlq.value()).containsExactly(poison);
    }

    private void insertReadyPointFixture(Instant snapshotObservedAt) {
        UUID snapshotId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO bpi.bpi_point_calibrations
                    (id, tenant_id, plant_id, line_id, product_id, device_id, property_id,
                     calibration_version, certificate_reference, certificate_checksum,
                     valid_from, valid_until, state, revision, submitted_by, submitted_at,
                     submit_reason, decided_by, decided_at, decision_reason)
                VALUES (?, ?, ?, ?, ?, ?, ?, 'CAL-SOURCE-SEQUENCE-01', ?, ?, ?, ?,
                        'APPROVED', 1, 'acceptance', ?, 'source sequence acceptance',
                        'reviewer', ?, 'approved for source sequence acceptance')
                """,
                UUID.randomUUID(), tenantId, PLANT_ID, LINE_ID, PRODUCT_ID, DEVICE_ID, PROPERTY_ID,
                "urn:adp:calibration:" + tenantId,
                "a".repeat(64),
                Timestamp.from(snapshotObservedAt.minus(Duration.ofDays(1))),
                Timestamp.from(Instant.now().plus(Duration.ofDays(1))),
                Timestamp.from(snapshotObservedAt.minusSeconds(10)),
                Timestamp.from(snapshotObservedAt.minusSeconds(5)));
        jdbc.update("""
                INSERT INTO bpi.bpi_point_catalog_snapshots
                    (id, tenant_id, source, source_instance, source_revision, plant_id, line_id,
                     checksum, observed_at, point_count, source_claim_ready_point_count, imported_by)
                VALUES (?, ?, 'JETLINKS', ?, ?, ?, ?, ?, ?, 1, 1, 'acceptance')
                """,
                snapshotId, tenantId, SOURCE_INSTANCE, "sha256:" + "b".repeat(64),
                PLANT_ID, LINE_ID, "c".repeat(64), Timestamp.from(snapshotObservedAt));
        jdbc.update("""
                INSERT INTO bpi.bpi_point_catalog_entries
                    (id, tenant_id, snapshot_id, plant_id, line_id, locality_group,
                     product_id, device_id, property_id, source_property_id, point_name, unit,
                     data_type, device_state, registered, property_present, calibration_version,
                     calibration_status, source_sequence_enabled, source_sequence_required,
                     source_sequence_origin, source_sequence_binding_fingerprint)
                VALUES (?, ?, ?, ?, ?, 'line-source-sequence.feed', ?, ?, ?, 'instantFlow',
                        'Instant flow', 'm3/h', 'double', 'ACTIVE', true, true,
                        'CAL-SOURCE-SEQUENCE-01', 'VERIFIED', true, true, 'GATEWAY', ?)
                """,
                UUID.randomUUID(), tenantId, snapshotId, PLANT_ID, LINE_ID,
                PRODUCT_ID, DEVICE_ID, PROPERTY_ID, FINGERPRINT);
    }

    private SourceSequenceEvidenceV1 evidence(
            SourceSequenceEvidenceStatusV1 status,
            Instant observedAt,
            long lastSequence,
            int observationCount,
            Instant validUntil) {
        Instant lastObservedAt = status == SourceSequenceEvidenceStatusV1.SOURCE_SEQUENCE_EVIDENCE_EXPIRED
                ? validUntil.minusMillis(1)
                : observedAt.minusSeconds(1);
        SourceSequenceEvidenceV1.Builder builder = SourceSequenceEvidenceV1.newBuilder()
                .setSource("JETLINKS")
                .setSourceInstance(SOURCE_INSTANCE)
                .setTenantId(tenantId)
                .setPlantId(PLANT_ID)
                .setLineId(LINE_ID)
                .setProductId(PRODUCT_ID)
                .setDeviceId(DEVICE_ID)
                .setBindingFingerprint(FINGERPRINT)
                .setStatus(status)
                .setSequenceOrigin(SequenceOrigin.GATEWAY)
                .setSourceEpoch(7L)
                .setFirstSequence(42L)
                .setLastSequence(lastSequence)
                .setObservationCount(observationCount)
                .setFirstObservedAtMs(lastObservedAt.minusSeconds(10).toEpochMilli())
                .setLastObservedAtMs(lastObservedAt.toEpochMilli())
                .setValidUntilMs(validUntil.toEpochMilli())
                .setObservedAtMs(observedAt.toEpochMilli())
                .setReason("Automatic JetLinks source sequence evidence acceptance");
        SourceSequenceEvidenceV1 content = builder.clearEventId().build();
        return content.toBuilder()
                .setEventId("source-sequence-evidence-" + Checksums.sha256(content.toByteArray()))
                .build();
    }

    private void send(SourceSequenceEvidenceV1 event) throws Exception {
        ProducerRecord<byte[], byte[]> record = new ProducerRecord<>(
                SOURCE_TOPIC,
                null,
                event.getObservedAtMs(),
                key(event).getBytes(StandardCharsets.UTF_8),
                event.toByteArray());
        record.headers()
                .add("event_id", event.getEventId().getBytes(StandardCharsets.UTF_8))
                .add("tenant_id", event.getTenantId().getBytes(StandardCharsets.UTF_8))
                .add("binding_fingerprint", event.getBindingFingerprint().getBytes(StandardCharsets.UTF_8))
                .add("schema_version", "v1".getBytes(StandardCharsets.UTF_8));
        kafkaTemplate.send(record).get(10, TimeUnit.SECONDS);
    }

    private String key(SourceSequenceEvidenceV1 event) {
        return String.join("|",
                event.getTenantId(), event.getPlantId(), event.getLineId(),
                event.getProductId(), event.getDeviceId(), event.getBindingFingerprint());
    }

    private PointCatalogSnapshotView currentSnapshot() {
        return pointCatalogRepository.findCurrentSnapshot(actor(), PLANT_ID, LINE_ID).orElseThrow();
    }

    private PointCatalogPointView currentPoint() {
        return pointCatalogRepository.listPoints(actor(), currentSnapshot()).get(0);
    }

    private ActorContext actor() {
        return new ActorContext(
                tenantId, "source-sequence-acceptance", Set.of("BPI_ADMIN"), Set.of("*"), Set.of("*"));
    }

    private String evidenceState() {
        return jdbc.queryForObject("""
                SELECT status || '|' || sequence_origin || '|' || source_epoch || '|'
                       || first_sequence || '|' || last_sequence || '|'
                       || observation_count || '|' || revision
                  FROM bpi.bpi_source_sequence_evidence_current
                 WHERE tenant_id = ?
                """, String.class, tenantId);
    }

    private int evidenceAuditCount() {
        Integer count = jdbc.queryForObject("""
                SELECT count(*) FROM bpi.bpi_audit_events
                 WHERE tenant_id = ? AND object_type = 'SOURCE_SEQUENCE_EVIDENCE'
                """, Integer.class, tenantId);
        return count == null ? 0 : count;
    }

    private void awaitEvidenceRevision(long expected) throws InterruptedException {
        Instant deadline = Instant.now().plusSeconds(15);
        while (Instant.now().isBefore(deadline)) {
            if (evidenceRevision() == expected) return;
            Thread.sleep(100);
        }
        ConsumerRecord<byte[], byte[]> rejected = awaitDlqRecord(Duration.ofSeconds(2));
        StringBuilder diagnostic = new StringBuilder("Source sequence event reached DLQ: ");
        rejected.headers().forEach(header -> diagnostic
                .append(header.key())
                .append('=')
                .append(header.value() == null
                        ? "null"
                        : new String(header.value(), StandardCharsets.UTF_8))
                .append(';'));
        if (rejected.value() != null) {
            diagnostic.append(" payloadSha256=").append(Checksums.sha256(rejected.value()));
        }
        assertThat(evidenceRevision()).withFailMessage(diagnostic.toString()).isEqualTo(expected);
    }

    private void awaitStableEvidenceRevision(long expected) throws InterruptedException {
        awaitEvidenceRevision(expected);
        Thread.sleep(500);
        assertThat(evidenceRevision()).isEqualTo(expected);
    }

    private long evidenceRevision() {
        List<Long> values = jdbc.query(
                "SELECT revision FROM bpi.bpi_source_sequence_evidence_current WHERE tenant_id = ?",
                (rs, rowNum) -> rs.getLong(1), tenantId);
        return values.isEmpty() ? 0L : values.get(0);
    }

    private void awaitTableCount(String table, long expected) throws InterruptedException {
        Instant deadline = Instant.now().plusSeconds(15);
        while (Instant.now().isBefore(deadline)) {
            if (count(table) == expected) return;
            Thread.sleep(100);
        }
        assertThat(count(table)).isEqualTo(expected);
    }

    private long count(String table) {
        Long count = jdbc.queryForObject(
                "SELECT count(*) FROM bpi." + table + " WHERE tenant_id = ?", Long.class, tenantId);
        return count == null ? 0L : count;
    }

    private ConsumerRecord<byte[], byte[]> awaitDlqRecord(Duration timeout) {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, broker.getBrokersAsString());
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "source-sequence-dlq-" + UUID.randomUUID());
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
        throw new AssertionError("No source sequence evidence DLQ record arrived before timeout");
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null ? fallback : value;
    }
}
