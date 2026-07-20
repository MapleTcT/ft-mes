package com.mapletct.ftmes.bpi;

import com.mapletct.ftmes.bpi.application.Checksums;
import com.mapletct.ftmes.bpi.application.SourceSequenceEvidenceIngestResult;
import com.mapletct.ftmes.bpi.application.SourceSequenceEvidenceIngestionService;
import com.mapletct.ftmes.bpi.contract.v1.SequenceOrigin;
import com.mapletct.ftmes.bpi.contract.v1.SourceSequenceEvidenceStatusV1;
import com.mapletct.ftmes.bpi.contract.v1.SourceSequenceEvidenceV1;
import com.mapletct.ftmes.bpi.infrastructure.sourcesequence.BpiSourceSequenceKafkaProperties;
import com.mapletct.ftmes.bpi.infrastructure.sourcesequence.SourceSequenceEvidenceKafkaListener;
import com.mapletct.ftmes.bpi.infrastructure.sourcesequence.SourceSequenceEvidenceKafkaRecordProcessor;
import com.mapletct.ftmes.bpi.infrastructure.sourcesequence.SourceSequenceEvidenceKafkaRecordRejectedException;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SourceSequenceEvidenceKafkaRecordProcessorTest {
    private static final String TOPIC = "iot.source-sequence.evidence.v1";
    private static final String TENANT = "TENANT-E2E";
    private static final String PLANT = "PLANT-01";
    private static final String LINE = "LINE-01";
    private static final String FINGERPRINT =
            "sha256:0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    private SourceSequenceEvidenceIngestionService service;
    private SourceSequenceEvidenceKafkaRecordProcessor processor;

    @BeforeEach
    void setUp() {
        service = mock(SourceSequenceEvidenceIngestionService.class);
        when(service.ingest(any(), anyString(), eq("jetlinks-source-sequence-sync")))
                .thenReturn(new SourceSequenceEvidenceIngestResult(
                        UUID.randomUUID(), 1L, false, false, true));
        processor = processor(Set.of(TENANT), Set.of(PLANT), Set.of(LINE));
    }

    @Test
    void validTrustedRecordUsesConfiguredActorAndPayloadChecksum() {
        SourceSequenceEvidenceV1 event = event(
                SourceSequenceEvidenceStatusV1.SOURCE_SEQUENCE_EVIDENCE_QUALIFIED,
                Instant.now().minusSeconds(1).toEpochMilli());

        processor.process(record(event));

        verify(service).ingest(
                eq(event),
                eq(Checksums.sha256(event.toByteArray())),
                eq("jetlinks-source-sequence-sync"));
    }

    @Test
    void malformedOutOfScopeFutureAndInvalidShapesAreRejected() {
        ConsumerRecord<byte[], byte[]> malformed = new ConsumerRecord<>(
                TOPIC, 0, 1L, "x".getBytes(StandardCharsets.UTF_8), new byte[]{1, 2, 3});
        assertThatThrownBy(() -> processor.process(malformed))
                .isInstanceOf(SourceSequenceEvidenceKafkaRecordRejectedException.class)
                .hasMessageContaining("Protobuf");

        SourceSequenceEvidenceKafkaRecordProcessor denyAll =
                processor(Set.of("OTHER"), Set.of(PLANT), Set.of(LINE));
        assertThatThrownBy(() -> denyAll.process(record(event(
                SourceSequenceEvidenceStatusV1.SOURCE_SEQUENCE_EVIDENCE_QUALIFIED,
                Instant.now().minusSeconds(1).toEpochMilli()))))
                .isInstanceOf(SourceSequenceEvidenceKafkaRecordRejectedException.class)
                .hasMessageContaining("outside");

        SourceSequenceEvidenceV1 future = event(
                SourceSequenceEvidenceStatusV1.SOURCE_SEQUENCE_EVIDENCE_QUALIFIED,
                Instant.now().plus(Duration.ofMinutes(6)).toEpochMilli());
        assertThatThrownBy(() -> processor.process(record(future)))
                .isInstanceOf(SourceSequenceEvidenceKafkaRecordRejectedException.class)
                .hasMessageContaining("future");

        SourceSequenceEvidenceV1 missingWithSequence = canonical(
                event(SourceSequenceEvidenceStatusV1.SOURCE_SEQUENCE_EVIDENCE_MISSING,
                        Instant.now().minusSeconds(1).toEpochMilli()).toBuilder()
                        .setSequenceOrigin(SequenceOrigin.GATEWAY)
                        .setSourceEpoch(7L));
        assertThatThrownBy(() -> processor.process(record(missingWithSequence)))
                .isInstanceOf(SourceSequenceEvidenceKafkaRecordRejectedException.class)
                .hasMessageContaining("must not carry");
        verify(service, never()).ingest(any(), anyString(), anyString());
    }

    @Test
    void contentIdentityKeyAndHeadersMustExactlyMatch() {
        SourceSequenceEvidenceV1 event = event(
                SourceSequenceEvidenceStatusV1.SOURCE_SEQUENCE_EVIDENCE_QUALIFIED,
                Instant.now().minusSeconds(1).toEpochMilli());

        assertThatThrownBy(() -> processor.process(copyWithKey(record(event), "OTHER|KEY")))
                .isInstanceOf(SourceSequenceEvidenceKafkaRecordRejectedException.class)
                .hasMessageContaining("record key");

        ConsumerRecord<byte[], byte[]> duplicateTenant = record(event);
        duplicateTenant.headers().add("tenant_id", TENANT.getBytes(StandardCharsets.UTF_8));
        assertThatThrownBy(() -> processor.process(duplicateTenant))
                .isInstanceOf(SourceSequenceEvidenceKafkaRecordRejectedException.class)
                .hasMessageContaining("exactly once");

        SourceSequenceEvidenceV1 tampered = event.toBuilder().setLastSequence(44L).build();
        assertThatThrownBy(() -> processor.process(record(tampered)))
                .isInstanceOf(SourceSequenceEvidenceKafkaRecordRejectedException.class)
                .hasMessageContaining("canonical payload SHA-256");
        verify(service, never()).ingest(any(), anyString(), anyString());
    }

    @Test
    void listenerAcknowledgesOnlyAfterPersistenceReturns() {
        SourceSequenceEvidenceKafkaRecordProcessor mockedProcessor =
                mock(SourceSequenceEvidenceKafkaRecordProcessor.class);
        SourceSequenceEvidenceKafkaListener listener =
                new SourceSequenceEvidenceKafkaListener(mockedProcessor);
        Acknowledgment acknowledgment = mock(Acknowledgment.class);
        ConsumerRecord<byte[], byte[]> record = record(event(
                SourceSequenceEvidenceStatusV1.SOURCE_SEQUENCE_EVIDENCE_QUALIFIED,
                Instant.now().minusSeconds(1).toEpochMilli()));

        listener.receive(record, acknowledgment);
        verify(acknowledgment, times(1)).acknowledge();

        doThrow(new IllegalStateException("database unavailable"))
                .when(mockedProcessor).process(record);
        assertThatThrownBy(() -> listener.receive(record, acknowledgment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("database unavailable");
        verify(acknowledgment, times(1)).acknowledge();
    }

    private SourceSequenceEvidenceKafkaRecordProcessor processor(
            Set<String> tenants,
            Set<String> plants,
            Set<String> lines) {
        return new SourceSequenceEvidenceKafkaRecordProcessor(
                service,
                new BpiSourceSequenceKafkaProperties(
                        false,
                        "localhost:29092",
                        TOPIC,
                        "iot.source-sequence.evidence.dlq.v1",
                        "bpi-source-sequence-test",
                        "bpi-source-sequence-test",
                        "jetlinks-source-sequence-sync",
                        tenants,
                        plants,
                        lines,
                        1,
                        4,
                        Duration.ofSeconds(2),
                        65_536));
    }

    private static SourceSequenceEvidenceV1 event(
            SourceSequenceEvidenceStatusV1 status,
            long observedAtMs) {
        SourceSequenceEvidenceV1.Builder builder = SourceSequenceEvidenceV1.newBuilder()
                .setSource("JETLINKS")
                .setSourceInstance("jetlinks-pilot-01")
                .setTenantId(TENANT)
                .setPlantId(PLANT)
                .setLineId(LINE)
                .setProductId("flow-product")
                .setDeviceId("meter-01")
                .setBindingFingerprint(FINGERPRINT)
                .setStatus(status)
                .setObservedAtMs(observedAtMs)
                .setReason("Automatic JetLinks source sequence evidence synchronization");
        if (status == SourceSequenceEvidenceStatusV1.SOURCE_SEQUENCE_EVIDENCE_QUALIFIED) {
            builder.setSequenceOrigin(SequenceOrigin.GATEWAY)
                    .setSourceEpoch(7L)
                    .setFirstSequence(42L)
                    .setLastSequence(43L)
                    .setObservationCount(2)
                    .setFirstObservedAtMs(observedAtMs - 2_000L)
                    .setLastObservedAtMs(observedAtMs - 1_000L)
                    .setValidUntilMs(observedAtMs + Duration.ofMinutes(30).toMillis());
        }
        return canonical(builder);
    }

    private static SourceSequenceEvidenceV1 canonical(SourceSequenceEvidenceV1.Builder builder) {
        SourceSequenceEvidenceV1 content = builder.clearEventId().build();
        return content.toBuilder()
                .setEventId("source-sequence-evidence-" + Checksums.sha256(content.toByteArray()))
                .build();
    }

    private static ConsumerRecord<byte[], byte[]> record(SourceSequenceEvidenceV1 event) {
        ConsumerRecord<byte[], byte[]> record = new ConsumerRecord<>(
                TOPIC, 0, 1L, key(event).getBytes(StandardCharsets.UTF_8), event.toByteArray());
        record.headers()
                .add("event_id", event.getEventId().getBytes(StandardCharsets.UTF_8))
                .add("tenant_id", event.getTenantId().getBytes(StandardCharsets.UTF_8))
                .add("binding_fingerprint", event.getBindingFingerprint().getBytes(StandardCharsets.UTF_8))
                .add("schema_version", "v1".getBytes(StandardCharsets.UTF_8));
        return record;
    }

    private static String key(SourceSequenceEvidenceV1 event) {
        return String.join("|", event.getTenantId(), event.getPlantId(), event.getLineId(),
                event.getProductId(), event.getDeviceId(), event.getBindingFingerprint());
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
