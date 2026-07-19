package com.mapletct.ftmes.bpi;

import com.mapletct.ftmes.bpi.application.DataQualityIngestResult;
import com.mapletct.ftmes.bpi.application.DataQualityIngestionService;
import com.mapletct.ftmes.bpi.contract.v1.DataQualityEventV1;
import com.mapletct.ftmes.bpi.contract.v1.DataQualitySeverity;
import com.mapletct.ftmes.bpi.infrastructure.dataquality.BpiDataQualityKafkaProperties;
import com.mapletct.ftmes.bpi.infrastructure.dataquality.DataQualityKafkaListener;
import com.mapletct.ftmes.bpi.infrastructure.dataquality.DataQualityKafkaRecordProcessor;
import com.mapletct.ftmes.bpi.infrastructure.dataquality.DataQualityKafkaRecordRejectedException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.Acknowledgment;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DataQualityKafkaRecordProcessorTest {
    private static final String TOPIC = "bpi.data-quality.v1";
    private static final String TENANT = "TENANT-E2E";
    private static final String PLANT = "PLANT-01";
    private static final String LINE = "LINE-01";

    private DataQualityIngestionService service;
    private DataQualityKafkaRecordProcessor processor;

    @BeforeEach
    void setUp() {
        service = mock(DataQualityIngestionService.class);
        when(service.ingest(any(), eq("bpi-stream-engine")))
                .thenReturn(new DataQualityIngestResult(UUID.randomUUID(), false, true, false));
        processor = processor(Set.of(TENANT), Set.of(PLANT), Set.of(LINE));
    }

    @Test
    void validTrustedRecordUsesConfiguredSystemActor() {
        DataQualityEventV1 event = event();

        processor.process(record(event));

        verify(service).ingest(eq(event), eq("bpi-stream-engine"));
    }

    @Test
    void malformedOutOfScopeFutureAndAmbiguousIdentitiesAreRejectedBeforePersistence() {
        ConsumerRecord<byte[], byte[]> malformed = new ConsumerRecord<>(
                TOPIC, 0, 1L, "x".getBytes(StandardCharsets.UTF_8), new byte[]{1, 2, 3});
        assertThatThrownBy(() -> processor.process(malformed))
                .isInstanceOf(DataQualityKafkaRecordRejectedException.class)
                .hasMessageContaining("Protobuf");

        DataQualityKafkaRecordProcessor denyAll = processor(Set.of("OTHER"), Set.of(PLANT), Set.of(LINE));
        assertThatThrownBy(() -> denyAll.process(record(event())))
                .isInstanceOf(DataQualityKafkaRecordRejectedException.class)
                .hasMessageContaining("outside");

        DataQualityEventV1 future = event().toBuilder()
                .setDetectedAtMs(Instant.now().plus(Duration.ofMinutes(6)).toEpochMilli())
                .build();
        assertThatThrownBy(() -> processor.process(record(future)))
                .isInstanceOf(DataQualityKafkaRecordRejectedException.class)
                .hasMessageContaining("future");

        DataQualityEventV1 ambiguous = event().toBuilder().setLineId("LINE|OTHER").build();
        assertThatThrownBy(() -> processor.process(record(ambiguous)))
                .isInstanceOf(DataQualityKafkaRecordRejectedException.class)
                .hasMessageContaining("cannot contain");
        verify(service, never()).ingest(any(), any());
    }

    @Test
    void keyAndRequiredHeadersMustExactlyMatchPayload() {
        DataQualityEventV1 event = event();
        assertThatThrownBy(() -> processor.process(copyWithKey(record(event), "OTHER|KEY")))
                .isInstanceOf(DataQualityKafkaRecordRejectedException.class)
                .hasMessageContaining("record key");

        ConsumerRecord<byte[], byte[]> duplicateTenant = record(event);
        duplicateTenant.headers().add("tenant_id", TENANT.getBytes(StandardCharsets.UTF_8));
        assertThatThrownBy(() -> processor.process(duplicateTenant))
                .isInstanceOf(DataQualityKafkaRecordRejectedException.class)
                .hasMessageContaining("exactly once");

        ConsumerRecord<byte[], byte[]> wrongIssue = record(event);
        wrongIssue.headers().remove("issue_code");
        wrongIssue.headers().add("issue_code", "OTHER".getBytes(StandardCharsets.UTF_8));
        assertThatThrownBy(() -> processor.process(wrongIssue))
                .isInstanceOf(DataQualityKafkaRecordRejectedException.class)
                .hasMessageContaining("does not match");

        ConsumerRecord<byte[], byte[]> wrongSchema = record(event);
        wrongSchema.headers().remove("schema_version");
        wrongSchema.headers().add("schema_version", "v2".getBytes(StandardCharsets.UTF_8));
        assertThatThrownBy(() -> processor.process(wrongSchema))
                .isInstanceOf(DataQualityKafkaRecordRejectedException.class)
                .hasMessageContaining("schema_version");
        verify(service, never()).ingest(any(), any());
    }

    @Test
    void listenerAcknowledgesOnlyAfterPersistenceReturns() {
        DataQualityKafkaRecordProcessor mockedProcessor = mock(DataQualityKafkaRecordProcessor.class);
        DataQualityKafkaListener listener = new DataQualityKafkaListener(mockedProcessor);
        Acknowledgment acknowledgment = mock(Acknowledgment.class);
        ConsumerRecord<byte[], byte[]> record = record(event());

        listener.receive(record, acknowledgment);
        verify(acknowledgment, times(1)).acknowledge();

        doThrow(new IllegalStateException("database unavailable")).when(mockedProcessor).process(record);
        assertThatThrownBy(() -> listener.receive(record, acknowledgment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("database unavailable");
        verify(acknowledgment, times(1)).acknowledge();
    }

    private DataQualityKafkaRecordProcessor processor(
            Set<String> tenants,
            Set<String> plants,
            Set<String> lines) {
        return new DataQualityKafkaRecordProcessor(
                service,
                new BpiDataQualityKafkaProperties(
                        false,
                        "localhost:29092",
                        TOPIC,
                        "bpi.data-quality.dlq.v1",
                        "bpi-service-data-quality-test",
                        "bpi-service-data-quality-test",
                        "bpi-stream-engine",
                        tenants,
                        plants,
                        lines,
                        1,
                        4,
                        Duration.ofSeconds(2),
                        65_536));
    }

    private static DataQualityEventV1 event() {
        return DataQualityEventV1.newBuilder()
                .setEventId("DQ-E2E-00000001")
                .setSourceEventId("SOURCE-E2E-00000001")
                .setTenantId(TENANT)
                .setPlantId(PLANT)
                .setLineId(LINE)
                .setDeviceId("FLOW-METER-01")
                .setPropertyId("flow.instant")
                .setIssueCode("CLOCK_DRIFT")
                .setSeverity(DataQualitySeverity.ERROR)
                .setDetail("Clock drift exceeds the configured threshold")
                .setDetectedAtMs(Instant.now().minusSeconds(1).toEpochMilli())
                .putHeaders("stage", "boundary-evaluation")
                .putHeaders("rule_key", String.join(
                        "|", TENANT, PLANT, LINE, "RULE-DQ-START", "1.0.0"))
                .build();
    }

    private static ConsumerRecord<byte[], byte[]> record(DataQualityEventV1 event) {
        ConsumerRecord<byte[], byte[]> record = new ConsumerRecord<>(
                TOPIC,
                0,
                1L,
                key(event).getBytes(StandardCharsets.UTF_8),
                event.toByteArray());
        record.headers()
                .add("event_id", event.getEventId().getBytes(StandardCharsets.UTF_8))
                .add("issue_code", event.getIssueCode().getBytes(StandardCharsets.UTF_8))
                .add("tenant_id", event.getTenantId().getBytes(StandardCharsets.UTF_8))
                .add("schema_version", "v1".getBytes(StandardCharsets.UTF_8));
        return record;
    }

    private static String key(DataQualityEventV1 event) {
        return String.join("|", event.getTenantId(), event.getLineId(),
                event.getSourceEventId(), event.getPropertyId(), event.getIssueCode());
    }

    private static ConsumerRecord<byte[], byte[]> copyWithKey(
            ConsumerRecord<byte[], byte[]> source,
            String key) {
        ConsumerRecord<byte[], byte[]> copy = new ConsumerRecord<>(
                source.topic(), source.partition(), source.offset(),
                key.getBytes(StandardCharsets.UTF_8), source.value());
        source.headers().forEach(header -> copy.headers().add(header));
        return copy;
    }
}
