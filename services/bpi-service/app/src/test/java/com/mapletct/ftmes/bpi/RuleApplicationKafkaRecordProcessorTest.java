package com.mapletct.ftmes.bpi;

import com.mapletct.ftmes.bpi.application.Checksums;
import com.mapletct.ftmes.bpi.application.RuleApplicationReceiptService;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryRuleApplicationStatusV1;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryRuleApplicationV1;
import com.mapletct.ftmes.bpi.infrastructure.application.BpiRuleApplicationKafkaProperties;
import com.mapletct.ftmes.bpi.infrastructure.application.RuleApplicationKafkaRecordProcessor;
import com.mapletct.ftmes.bpi.infrastructure.application.RuleApplicationKafkaRecordRejectedException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class RuleApplicationKafkaRecordProcessorTest {
    private static final String TOPIC = "bpi.boundary.rule-application.v1";
    private static final String TENANT = "TENANT-E2E";
    private static final String PLANT = "PLANT-01";
    private static final String LINE = "LINE-01";

    private RuleApplicationReceiptService receiptService;
    private RuleApplicationKafkaRecordProcessor processor;

    @BeforeEach
    void setUp() {
        receiptService = mock(RuleApplicationReceiptService.class);
        processor = new RuleApplicationKafkaRecordProcessor(receiptService, properties());
    }

    @Test
    void trustedReceiptPassesRawPayloadChecksumToTransactionalService() {
        BoundaryRuleApplicationV1 event = event("APPLICATION-1", "flink-a");
        ConsumerRecord<byte[], byte[]> record = record(event);

        processor.process(record);

        verify(receiptService).apply(event, Checksums.sha256(event.toByteArray()));
    }

    @Test
    void oversizedDeploymentIdentityIsRejectedBeforePersistence() {
        BoundaryRuleApplicationV1 oversized = event("APPLICATION-2", "x".repeat(129));

        assertThatThrownBy(() -> processor.process(record(oversized)))
                .isInstanceOf(RuleApplicationKafkaRecordRejectedException.class)
                .hasMessageContaining("deployment_id exceeds 128");
        verifyNoInteractions(receiptService);
    }

    private static BpiRuleApplicationKafkaProperties properties() {
        return new BpiRuleApplicationKafkaProperties(
                false,
                "localhost:29092",
                TOPIC,
                "bpi.boundary.rule-application.dlq.v1",
                "rule-application-test",
                "rule-application-test",
                Set.of(TENANT),
                Set.of(PLANT),
                Set.of(LINE),
                1,
                4,
                Duration.ofSeconds(1),
                65_536);
    }

    private static BoundaryRuleApplicationV1 event(String eventId, String deploymentId) {
        return BoundaryRuleApplicationV1.newBuilder()
                .setEventId(eventId)
                .setPublicationEventId(UUID.randomUUID().toString())
                .setTenantId(TENANT)
                .setPlantId(PLANT)
                .setLineId(LINE)
                .setRuleCode("RULE-START")
                .setRuleVersion("1")
                .setChecksum("a".repeat(64))
                .setDeploymentId(deploymentId)
                .setStatus(BoundaryRuleApplicationStatusV1.APPLIED)
                .setObservedAtMs(Instant.parse("2026-07-13T01:00:00Z").toEpochMilli())
                .build();
    }

    private static ConsumerRecord<byte[], byte[]> record(BoundaryRuleApplicationV1 event) {
        ConsumerRecord<byte[], byte[]> record = new ConsumerRecord<>(
                TOPIC,
                0,
                1L,
                event.getPublicationEventId().getBytes(StandardCharsets.UTF_8),
                event.toByteArray());
        record.headers()
                .add("event_id", event.getEventId().getBytes(StandardCharsets.UTF_8))
                .add("publication_event_id", event.getPublicationEventId().getBytes(StandardCharsets.UTF_8))
                .add("tenant_id", event.getTenantId().getBytes(StandardCharsets.UTF_8))
                .add("status", event.getStatus().name().getBytes(StandardCharsets.UTF_8))
                .add("schema_version", "v1".getBytes(StandardCharsets.UTF_8));
        return record;
    }
}
