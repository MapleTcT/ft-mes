package com.mapletct.ftmes.bpi;

import com.mapletct.ftmes.bpi.application.ActorContext;
import com.mapletct.ftmes.bpi.application.CandidateEventMapper;
import com.mapletct.ftmes.bpi.application.CandidateIngestionService;
import com.mapletct.ftmes.bpi.contract.identity.CandidateKeyFactory;
import com.mapletct.ftmes.bpi.contract.v1.BatchCandidateV1;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryType;
import com.mapletct.ftmes.bpi.contract.v1.CandidateEvidenceV1;
import com.mapletct.ftmes.bpi.infrastructure.candidate.BpiCandidateEventProperties;
import com.mapletct.ftmes.bpi.infrastructure.candidate.BpiCandidateKafkaProperties;
import com.mapletct.ftmes.bpi.infrastructure.candidate.CandidateKafkaListener;
import com.mapletct.ftmes.bpi.infrastructure.candidate.CandidateKafkaRecordProcessor;
import com.mapletct.ftmes.bpi.infrastructure.candidate.CandidateKafkaRecordRejectedException;
import com.mapletct.ftmes.bpi.interfaces.rest.CandidateIngestRequest;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.support.Acknowledgment;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CandidateKafkaRecordProcessorTest {
    private static final String TOPIC = "bpi.batch.candidate.v1";
    private static final String TENANT = "TENANT-E2E";
    private static final String PLANT = "PLANT-01";
    private static final String LINE = "LINE-01";

    private CandidateIngestionService ingestionService;
    private CandidateKafkaRecordProcessor processor;

    @BeforeEach
    void setUp() {
        ingestionService = mock(CandidateIngestionService.class);
        processor = processor(Set.of(TENANT), Set.of(PLANT), Set.of(LINE));
    }

    @Test
    void validTrustedRecordUsesScopedActorAndSharedIngestionService() {
        BatchCandidateV1 event = event();
        ConsumerRecord<byte[], byte[]> record = record(event);

        processor.process(record);

        ArgumentCaptor<ActorContext> actor = ArgumentCaptor.forClass(ActorContext.class);
        ArgumentCaptor<CandidateIngestRequest> request = ArgumentCaptor.forClass(CandidateIngestRequest.class);
        verify(ingestionService).ingest(actor.capture(), request.capture());
        assertThat(actor.getValue().tenantId()).isEqualTo(TENANT);
        assertThat(actor.getValue().userId()).isEqualTo("bpi-stream-engine");
        assertThat(actor.getValue().plantIds()).containsExactly(PLANT);
        assertThat(actor.getValue().lineIds()).containsExactly(LINE);
        assertThat(request.getValue().candidateKey().toString()).isEqualTo(event.getCandidateKey());
    }

    @Test
    void malformedOrOutOfScopePayloadIsRejectedBeforePersistence() {
        ConsumerRecord<byte[], byte[]> malformed = new ConsumerRecord<>(
                TOPIC, 0, 1L, "x".getBytes(StandardCharsets.UTF_8), new byte[]{1, 2, 3});
        assertThatThrownBy(() -> processor.process(malformed))
                .isInstanceOf(CandidateKafkaRecordRejectedException.class)
                .hasMessageContaining("Protobuf");

        CandidateKafkaRecordProcessor denyAll = processor(
                Set.of("OTHER"), Set.of(PLANT), Set.of(LINE));
        assertThatThrownBy(() -> denyAll.process(record(event())))
                .isInstanceOf(CandidateKafkaRecordRejectedException.class)
                .hasMessageContaining("outside");
        verify(ingestionService, never()).ingest(any(), any());
    }

    @Test
    void keyAndRequiredHeadersMustExactlyMatchPayload() {
        BatchCandidateV1 event = event();
        ConsumerRecord<byte[], byte[]> wrongKey = copyWithKey(record(event), "OTHER|KEY");
        assertThatThrownBy(() -> processor.process(wrongKey))
                .isInstanceOf(CandidateKafkaRecordRejectedException.class)
                .hasMessageContaining("record key");

        ConsumerRecord<byte[], byte[]> duplicateTenant = record(event);
        duplicateTenant.headers().add("tenant_id", TENANT.getBytes(StandardCharsets.UTF_8));
        assertThatThrownBy(() -> processor.process(duplicateTenant))
                .isInstanceOf(CandidateKafkaRecordRejectedException.class)
                .hasMessageContaining("exactly once");

        ConsumerRecord<byte[], byte[]> wrongEvent = record(event);
        wrongEvent.headers().remove("event_id");
        wrongEvent.headers().add("event_id", "OTHER".getBytes(StandardCharsets.UTF_8));
        assertThatThrownBy(() -> processor.process(wrongEvent))
                .isInstanceOf(CandidateKafkaRecordRejectedException.class)
                .hasMessageContaining("does not match");

        ConsumerRecord<byte[], byte[]> wrongSchema = record(event);
        wrongSchema.headers().remove("schema_version");
        wrongSchema.headers().add("schema_version", "v2".getBytes(StandardCharsets.UTF_8));
        assertThatThrownBy(() -> processor.process(wrongSchema))
                .isInstanceOf(CandidateKafkaRecordRejectedException.class)
                .hasMessageContaining("schema_version");

        ConsumerRecord<byte[], byte[]> malformedUtf8Key = new ConsumerRecord<>(
                TOPIC, 0, 1L, new byte[]{(byte) 0xc3, 0x28}, event.toByteArray());
        record(event).headers().forEach(header -> malformedUtf8Key.headers().add(header));
        assertThatThrownBy(() -> processor.process(malformedUtf8Key))
                .isInstanceOf(CandidateKafkaRecordRejectedException.class)
                .hasMessageContaining("UTF-8");
        verify(ingestionService, never()).ingest(any(), any());
    }

    @Test
    void listenerAcknowledgesOnlyAfterProcessorReturnsSuccessfully() {
        CandidateKafkaRecordProcessor mockedProcessor = mock(CandidateKafkaRecordProcessor.class);
        CandidateKafkaListener listener = new CandidateKafkaListener(mockedProcessor);
        Acknowledgment acknowledgment = mock(Acknowledgment.class);
        ConsumerRecord<byte[], byte[]> record = record(event());

        listener.receive(record, acknowledgment);
        verify(acknowledgment, times(1)).acknowledge();

        doThrow(new IllegalStateException("database unavailable"))
                .when(mockedProcessor).process(record);
        assertThatThrownBy(() -> listener.receive(record, acknowledgment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("database unavailable");
        verify(acknowledgment).acknowledge();
    }

    private CandidateKafkaRecordProcessor processor(
            Set<String> tenants,
            Set<String> plants,
            Set<String> lines) {
        return new CandidateKafkaRecordProcessor(
                new CandidateEventMapper(),
                ingestionService,
                new BpiCandidateEventProperties(false, 1_048_576),
                new BpiCandidateKafkaProperties(
                        false,
                        "localhost:29092",
                        TOPIC,
                        "bpi.batch.candidate.dlq.v1",
                        "bpi-service-test",
                        "bpi-service-test",
                        "bpi-stream-engine",
                        tenants,
                        plants,
                        lines,
                        1,
                        4,
                        Duration.ofSeconds(2)));
    }

    private static BatchCandidateV1 event() {
        Instant boundaryTime = Instant.parse("2026-07-12T08:15:00Z");
        String eventId = "ADP_E2E_KAFKA_" + UUID.randomUUID();
        String evidenceEventId = eventId + "-EVIDENCE";
        String orderId = "ORDER-E2E";
        String ruleVersion = "1";
        return BatchCandidateV1.newBuilder()
                .setEventId(eventId)
                .setCandidateKey(CandidateKeyFactory.startKey(
                        TENANT, LINE, ruleVersion, orderId, evidenceEventId))
                .setTenantId(TENANT)
                .setPlantId(PLANT)
                .setLineId(LINE)
                .setBoundaryType(BoundaryType.START)
                .setRuleCode("RULE-START")
                .setRuleVersion(ruleVersion)
                .setTopologyVersion("1")
                .setContextOrderId(orderId)
                .setFirstQuorumEvidenceEventId(evidenceEventId)
                .setBoundaryEventTimeMs(boundaryTime.toEpochMilli())
                .setConfidence(0.9)
                .addEvidenceEventIds(evidenceEventId)
                .setEmittedAtMs(boundaryTime.plusSeconds(1).toEpochMilli())
                .putHeaders("topology_code", "TOPO-E2E")
                .addEvidence(CandidateEvidenceV1.newBuilder()
                        .setEventId(evidenceEventId)
                        .setSignal("feed.flow")
                        .setClassification("QUORUM")
                        .setSatisfied(true)
                        .setValue("18.6")
                        .setUnit("m3/h")
                        .setQualityCode("GOOD")
                        .setEventTimeMs(boundaryTime.toEpochMilli())
                        .setSource("bpi-stream-engine"))
                .build();
    }

    private static ConsumerRecord<byte[], byte[]> record(BatchCandidateV1 event) {
        ConsumerRecord<byte[], byte[]> record = new ConsumerRecord<>(
                TOPIC,
                0,
                1L,
                (event.getLineId() + "|" + event.getRuleCode()).getBytes(StandardCharsets.UTF_8),
                event.toByteArray());
        record.headers()
                .add("event_id", event.getEventId().getBytes(StandardCharsets.UTF_8))
                .add("candidate_key", event.getCandidateKey().getBytes(StandardCharsets.UTF_8))
                .add("tenant_id", event.getTenantId().getBytes(StandardCharsets.UTF_8))
                .add("schema_version", "v1".getBytes(StandardCharsets.UTF_8));
        return record;
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
