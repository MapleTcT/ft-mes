package com.mapletct.ftmes.bpi;

import com.mapletct.ftmes.bpi.application.Checksums;
import com.mapletct.ftmes.bpi.application.RuleRuntimeReadinessReceiptService;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryRuleRuntimeReadinessStatusV1;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryRuleRuntimeReadinessV1;
import com.mapletct.ftmes.bpi.infrastructure.application.BpiRuleApplicationKafkaProperties;
import com.mapletct.ftmes.bpi.infrastructure.application.RuleRuntimeReadinessKafkaRecordProcessor;
import com.mapletct.ftmes.bpi.infrastructure.application.RuleRuntimeReadinessKafkaRecordRejectedException;
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

class RuleRuntimeReadinessKafkaRecordProcessorTest {
    private static final String TOPIC = "bpi.boundary.rule-runtime-readiness.v1";
    private static final String TENANT = "TENANT-E2E";
    private static final String PLANT = "PLANT-01";
    private static final String LINE = "LINE-01";

    private RuleRuntimeReadinessReceiptService receiptService;
    private RuleRuntimeReadinessKafkaRecordProcessor processor;

    @BeforeEach
    void setUp() {
        receiptService = mock(RuleRuntimeReadinessReceiptService.class);
        processor = new RuleRuntimeReadinessKafkaRecordProcessor(receiptService, properties());
    }

    @Test
    void trustedReceiptPassesRawPayloadChecksumToTransactionalService() {
        BoundaryRuleRuntimeReadinessV1 event = event(
                "READINESS-1", BoundaryRuleRuntimeReadinessStatusV1.READY, "", "");

        processor.process(record(event));

        verify(receiptService).apply(event, Checksums.sha256(event.toByteArray()));
    }

    @Test
    void degradedReceiptWithoutReasonIsRejectedBeforePersistence() {
        BoundaryRuleRuntimeReadinessV1 event = event(
                "READINESS-2", BoundaryRuleRuntimeReadinessStatusV1.DEGRADED, "", "");

        assertThatThrownBy(() -> processor.process(record(event)))
                .isInstanceOf(RuleRuntimeReadinessKafkaRecordRejectedException.class)
                .hasMessageContaining("requires a reason and detail");
        verifyNoInteractions(receiptService);
    }

    private static BpiRuleApplicationKafkaProperties properties() {
        return new BpiRuleApplicationKafkaProperties(
                false,
                "localhost:29092",
                "bpi.boundary.rule-application.v1",
                "bpi.boundary.rule-application.dlq.v1",
                TOPIC,
                "bpi.boundary.rule-runtime-readiness.dlq.v1",
                "rule-readiness-test",
                "rule-readiness-test",
                Set.of(TENANT),
                Set.of(PLANT),
                Set.of(LINE),
                1,
                4,
                Duration.ofSeconds(1),
                65_536);
    }

    private static BoundaryRuleRuntimeReadinessV1 event(
            String eventId,
            BoundaryRuleRuntimeReadinessStatusV1 status,
            String reasonCode,
            String detail) {
        return BoundaryRuleRuntimeReadinessV1.newBuilder()
                .setEventId(eventId)
                .setPublicationEventId(UUID.randomUUID().toString())
                .setTenantId(TENANT)
                .setPlantId(PLANT)
                .setLineId(LINE)
                .setRuleCode("RULE-START")
                .setRuleVersion("1")
                .setChecksum("a".repeat(64))
                .setDeploymentId("flink-a")
                .setStatus(status)
                .setReasonCode(reasonCode)
                .setDetail(detail)
                .setObservedAtMs(Instant.parse("2026-07-13T01:00:00Z").toEpochMilli())
                .build();
    }

    private static ConsumerRecord<byte[], byte[]> record(BoundaryRuleRuntimeReadinessV1 event) {
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
