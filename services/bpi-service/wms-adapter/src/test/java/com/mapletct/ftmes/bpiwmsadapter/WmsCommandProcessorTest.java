package com.mapletct.ftmes.bpiwmsadapter;

import com.mapletct.ftmes.bpi.contract.v1.WmsCompletionInboundCommandV1;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WmsCommandProcessorTest {

    private MaterialWmsGateway materialWms;
    private WmsReceiptPublisher receipts;
    private WmsCommandProcessor processor;

    @BeforeEach
    void setUp() {
        materialWms = mock(MaterialWmsGateway.class);
        receipts = mock(WmsReceiptPublisher.class);
        processor = new WmsCommandProcessor(properties(), materialWms, receipts);
    }

    @Test
    void findsExistingDocumentBeforeAnyCreateAttempt() {
        WmsCompletionInboundCommandV1 command = command("kg");
        MaterialWmsDocument document = document(command, "10.000000");
        when(materialWms.findByIdempotency(command.getTenantId(), command.getIdempotencyKey()))
                .thenReturn(Optional.of(document));

        WmsCommandProcessor.WmsProcessingResult result = processor.process(record(command));

        assertThat(result.status()).isEqualTo("ACCEPTED");
        assertThat(result.created()).isFalse();
        verify(materialWms, never()).createCompletionInbound(any());
        verify(receipts).accepted(any(), any(), any());
    }

    @Test
    void createsThenRequiresExactDurableLookupBeforeReceipt() {
        WmsCompletionInboundCommandV1 command = command("kg");
        MaterialWmsDocument document = document(command, "10");
        when(materialWms.findByIdempotency(command.getTenantId(), command.getIdempotencyKey()))
                .thenReturn(Optional.empty(), Optional.of(document));

        WmsCommandProcessor.WmsProcessingResult result = processor.process(record(command));

        assertThat(result.status()).isEqualTo("ACCEPTED");
        assertThat(result.created()).isTrue();
        verify(materialWms).createCompletionInbound(any());
        verify(materialWms, times(2)).findByIdempotency(
                command.getTenantId(), command.getIdempotencyKey());
        verify(receipts).accepted(any(), any(), any());
    }

    @Test
    void transportFailureIsRetriedAndNeverConvertedToBusinessRejection() {
        WmsCompletionInboundCommandV1 command = command("kg");
        when(materialWms.findByIdempotency(command.getTenantId(), command.getIdempotencyKey()))
                .thenThrow(new MaterialWmsTransientException("timeout"));

        assertThatThrownBy(() -> processor.process(record(command)))
                .isInstanceOf(MaterialWmsTransientException.class)
                .hasMessageContaining("timeout");
        verify(receipts, never()).accepted(any(), any(), any());
        verify(receipts, never()).rejected(any(), any(), any());
    }

    @Test
    void createResponseLossIsRecoveredByImmediateExactLookup() {
        WmsCompletionInboundCommandV1 command = command("kg");
        MaterialWmsDocument document = document(command, "10");
        when(materialWms.findByIdempotency(command.getTenantId(), command.getIdempotencyKey()))
                .thenReturn(Optional.empty(), Optional.of(document));
        org.mockito.Mockito.doThrow(new MaterialWmsTransientException("response lost after commit"))
                .when(materialWms).createCompletionInbound(any());

        WmsCommandProcessor.WmsProcessingResult result = processor.process(record(command));

        assertThat(result.status()).isEqualTo("ACCEPTED");
        assertThat(result.created()).isTrue();
        verify(materialWms, times(2)).findByIdempotency(
                command.getTenantId(), command.getIdempotencyKey());
        verify(receipts).accepted(any(), any(), any());
        verify(receipts, never()).rejected(any(), any(), any());
    }

    @Test
    void ambiguousCreateWithoutDurableDocumentRemainsRetryable() {
        WmsCompletionInboundCommandV1 command = command("kg");
        when(materialWms.findByIdempotency(command.getTenantId(), command.getIdempotencyKey()))
                .thenReturn(Optional.empty(), Optional.empty());
        org.mockito.Mockito.doThrow(new MaterialWmsTransientException("response lost"))
                .when(materialWms).createCompletionInbound(any());

        assertThatThrownBy(() -> processor.process(record(command)))
                .isInstanceOf(MaterialWmsTransientException.class)
                .hasMessage("response lost");
        verify(materialWms, times(2)).findByIdempotency(
                command.getTenantId(), command.getIdempotencyKey());
        verify(receipts, never()).accepted(any(), any(), any());
        verify(receipts, never()).rejected(any(), any(), any());
    }

    @Test
    void failedRecoveryLookupPreservesAmbiguousCreateForKafkaRetry() {
        WmsCompletionInboundCommandV1 command = command("kg");
        MaterialWmsTransientException createError =
                new MaterialWmsTransientException("response lost");
        MaterialWmsTransientException lookupError =
                new MaterialWmsTransientException("lookup unavailable");
        when(materialWms.findByIdempotency(command.getTenantId(), command.getIdempotencyKey()))
                .thenReturn(Optional.empty())
                .thenThrow(lookupError);
        org.mockito.Mockito.doThrow(createError).when(materialWms).createCompletionInbound(any());

        assertThatThrownBy(() -> processor.process(record(command)))
                .isSameAs(createError)
                .satisfies(error -> assertThat(error.getSuppressed()).containsExactly(lookupError));
        verify(receipts, never()).accepted(any(), any(), any());
        verify(receipts, never()).rejected(any(), any(), any());
    }

    @Test
    void businessCreateFailureIsRequeriedBeforeRejectedReceipt() {
        WmsCompletionInboundCommandV1 command = command("kg");
        when(materialWms.findByIdempotency(command.getTenantId(), command.getIdempotencyKey()))
                .thenReturn(Optional.empty(), Optional.empty());
        org.mockito.Mockito.doThrow(new MaterialWmsBusinessException("MATERIAL_WMS_409", "conflict"))
                .when(materialWms).createCompletionInbound(any());

        WmsCommandProcessor.WmsProcessingResult result = processor.process(record(command));

        assertThat(result.status()).isEqualTo("REJECTED");
        verify(materialWms, times(2)).findByIdempotency(
                command.getTenantId(), command.getIdempotencyKey());
        verify(receipts).rejected(command, "MATERIAL_WMS_409", "conflict");
    }

    @Test
    void idempotencyLookupWithDifferentBusinessFactsFailsClosed() {
        WmsCompletionInboundCommandV1 command = command("kg");
        MaterialWmsDocument conflicting = document(command, "9.999999");
        when(materialWms.findByIdempotency(command.getTenantId(), command.getIdempotencyKey()))
                .thenReturn(Optional.of(conflicting));

        WmsCommandProcessor.WmsProcessingResult result = processor.process(record(command));

        assertThat(result.status()).isEqualTo("REJECTED");
        assertThat(result.documentId()).isEqualTo(conflicting.documentNo());
        verify(materialWms, never()).createCompletionInbound(any());
        verify(receipts).rejected(
                command,
                "WMS_IDEMPOTENCY_CONFLICT",
                "The idempotency key resolves to a WMS line with different material, batch, quantity, unit or location.");
        verify(receipts, never()).accepted(any(), any(), any());
    }

    @Test
    void unitMismatchFailsClosedBeforeCallingMaterialWms() {
        WmsCompletionInboundCommandV1 command = command("t");

        WmsCommandProcessor.WmsProcessingResult result = processor.process(record(command));

        assertThat(result.status()).isEqualTo("REJECTED");
        verify(materialWms, never()).findByIdempotency(any(), any());
        verify(receipts).rejected(any(), org.mockito.ArgumentMatchers.eq("WMS_UNIT_MISMATCH"), any());
    }

    @Test
    void mismatchedKafkaIdentityIsSentToDlqWithoutCallingWms() {
        WmsCompletionInboundCommandV1 command = command("kg");
        ConsumerRecord<byte[], byte[]> record = record(command);
        record.headers().remove("tenant_id");
        record.headers().add(new RecordHeader(
                "tenant_id", "OTHER".getBytes(StandardCharsets.UTF_8)));

        assertThatThrownBy(() -> processor.process(record))
                .isInstanceOf(WmsCommandRejectedException.class)
                .hasMessageContaining("tenant_id");
        verify(materialWms, never()).findByIdempotency(any(), any());
    }

    private BpiWmsAdapterProperties properties() {
        return new BpiWmsAdapterProperties(
                true,
                "kafka:9092",
                "bpi.wms.completion-inbound-command.v1",
                "bpi.wms.completion-inbound-command.dlq.v1",
                "wms.completion-inbound.receipt.v1",
                "test-group",
                "test-client",
                "http://material:8080",
                "test-key",
                "Asia/Shanghai",
                65_536,
                5,
                1,
                Duration.ofMillis(10),
                Duration.ofSeconds(1),
                Duration.ofSeconds(1),
                List.of("TENANT|PLANT|LINE|WARE|LOC|COMP|kg"));
    }

    private WmsCompletionInboundCommandV1 command(String unit) {
        String eventId = "2ea229c2-f2bb-5da8-b84c-5b4bd00148ce";
        String batchId = "a0683f52-7d70-5d67-a95f-3b54621e22e1";
        String idempotencyKey = "WMS_COMPLETION_INBOUND|TENANT|" + batchId + "|GATE|1";
        return WmsCompletionInboundCommandV1.newBuilder()
                .setEventId(eventId)
                .setIdempotencyKey(idempotencyKey)
                .setTenantId("TENANT")
                .setPlantId("PLANT")
                .setLineId("LINE")
                .setBatchId(batchId)
                .setBatchNo("BATCH-001")
                .setOrderId("MO-001")
                .setMaterialCode("MAT-001")
                .setQuantityDecimal("10")
                .setQuantityUnit(unit)
                .setQualityGateId("QG-001")
                .setQualityGateRevision(1)
                .setRequestedAtMs(Instant.parse("2026-07-20T08:00:00Z").toEpochMilli())
                .putHeaders("event_id", eventId)
                .putHeaders("idempotency_key", idempotencyKey)
                .putHeaders("tenant_id", "TENANT")
                .putHeaders("schema_version", "v1")
                .putHeaders("trace_id", UUID.randomUUID().toString())
                .build();
    }

    private ConsumerRecord<byte[], byte[]> record(WmsCompletionInboundCommandV1 command) {
        String key = command.getTenantId() + "|" + command.getPlantId() + "|" + command.getBatchId();
        ConsumerRecord<byte[], byte[]> record = new ConsumerRecord<>(
                properties().commandTopic(), 0, 0,
                key.getBytes(StandardCharsets.UTF_8), command.toByteArray());
        command.getHeadersMap().forEach((name, value) -> record.headers().add(
                new RecordHeader(name, value.getBytes(StandardCharsets.UTF_8))));
        return record;
    }

    private MaterialWmsDocument document(
            WmsCompletionInboundCommandV1 command, String quantity) {
        return new MaterialWmsDocument(
                101L,
                "CI-20260720-001",
                "BPI",
                command.getEventId(),
                command.getIdempotencyKey(),
                "WARE",
                "POSTED",
                "QUALIFIED",
                List.of(new MaterialWmsDocument.Line(
                        "BPI",
                        command.getEventId() + ":1",
                        command.getMaterialCode(),
                        command.getBatchNo(),
                        command.getBatchNo(),
                        "WARE",
                        "LOC",
                        new BigDecimal(quantity),
                        "kg",
                        "QUALIFIED")));
    }
}
