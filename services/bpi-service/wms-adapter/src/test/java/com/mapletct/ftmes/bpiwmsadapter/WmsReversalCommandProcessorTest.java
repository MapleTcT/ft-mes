package com.mapletct.ftmes.bpiwmsadapter;

import com.mapletct.ftmes.bpi.contract.v1.WmsCompletionInboundReversalCommandV1;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WmsReversalCommandProcessorTest {

    private MaterialWmsGateway materialWms;
    private WmsReversalReceiptPublisher receipts;
    private WmsReversalCommandProcessor processor;

    @BeforeEach
    void setUp() {
        materialWms = mock(MaterialWmsGateway.class);
        receipts = mock(WmsReversalReceiptPublisher.class);
        processor = new WmsReversalCommandProcessor(properties(), materialWms, receipts);
    }

    @Test
    void findsExistingRedDocumentBeforeAnyCreateAttempt() {
        WmsCompletionInboundReversalCommandV1 command = command("kg", "REQUESTER", "APPROVER");
        MaterialWmsReversalDocument document = document(command, "10.000000", "CIN-001");
        when(materialWms.findReversalByIdempotency(
                command.getTenantId(), command.getIdempotencyKey()))
            .thenReturn(Optional.of(document));

        WmsReversalCommandProcessor.WmsReversalProcessingResult result =
                processor.process(record(command));

        assertThat(result.status()).isEqualTo("ACCEPTED");
        assertThat(result.created()).isFalse();
        verify(materialWms, never()).createCompletionInboundReversal(any());
        verify(receipts).accepted(any(), any(), any());
    }

    @Test
    void createsThenRequiresExactDurableRedDocumentBeforeReceipt() {
        WmsCompletionInboundReversalCommandV1 command = command("kg", "REQUESTER", "APPROVER");
        MaterialWmsReversalDocument document = document(command, "10", "CIN-001");
        when(materialWms.findReversalByIdempotency(
                command.getTenantId(), command.getIdempotencyKey()))
            .thenReturn(Optional.empty(), Optional.of(document));

        WmsReversalCommandProcessor.WmsReversalProcessingResult result =
                processor.process(record(command));

        assertThat(result.status()).isEqualTo("ACCEPTED");
        assertThat(result.created()).isTrue();
        verify(materialWms).createCompletionInboundReversal(any());
        verify(materialWms, times(2)).findReversalByIdempotency(
                command.getTenantId(), command.getIdempotencyKey());
        verify(receipts).accepted(any(), any(), any());
    }

    @Test
    void responseLossAfterRedCommitIsRecoveredByImmediateLookup() {
        WmsCompletionInboundReversalCommandV1 command = command("kg", "REQUESTER", "APPROVER");
        MaterialWmsReversalDocument document = document(command, "10", "CIN-001");
        when(materialWms.findReversalByIdempotency(
                command.getTenantId(), command.getIdempotencyKey()))
            .thenReturn(Optional.empty(), Optional.of(document));
        org.mockito.Mockito.doThrow(new MaterialWmsTransientException("response lost"))
            .when(materialWms).createCompletionInboundReversal(any());

        WmsReversalCommandProcessor.WmsReversalProcessingResult result =
                processor.process(record(command));

        assertThat(result.status()).isEqualTo("ACCEPTED");
        assertThat(result.created()).isTrue();
        verify(receipts).accepted(any(), any(), any());
        verify(receipts, never()).rejected(any(), any(), any());
    }

    @Test
    void ambiguousRedCreateWithoutDurableDocumentRemainsRetryable() {
        WmsCompletionInboundReversalCommandV1 command = command("kg", "REQUESTER", "APPROVER");
        when(materialWms.findReversalByIdempotency(
                command.getTenantId(), command.getIdempotencyKey()))
            .thenReturn(Optional.empty(), Optional.empty());
        org.mockito.Mockito.doThrow(new MaterialWmsTransientException("response lost"))
            .when(materialWms).createCompletionInboundReversal(any());

        assertThatThrownBy(() -> processor.process(record(command)))
            .isInstanceOf(MaterialWmsTransientException.class)
            .hasMessage("response lost");
        verify(receipts, never()).accepted(any(), any(), any());
        verify(receipts, never()).rejected(any(), any(), any());
    }

    @Test
    void ambiguousRedCreatePreservesRecoveryLookupFailureForRetryDiagnostics() {
        WmsCompletionInboundReversalCommandV1 command = command("kg", "REQUESTER", "APPROVER");
        MaterialWmsTransientException createError =
                new MaterialWmsTransientException("response lost");
        MaterialWmsTransientException lookupError =
                new MaterialWmsTransientException("lookup unavailable");
        when(materialWms.findReversalByIdempotency(
                command.getTenantId(), command.getIdempotencyKey()))
            .thenReturn(Optional.empty())
            .thenThrow(lookupError);
        org.mockito.Mockito.doThrow(createError)
            .when(materialWms).createCompletionInboundReversal(any());

        Throwable thrown = catchThrowable(() -> processor.process(record(command)));

        assertThat(thrown).isSameAs(createError);
        assertThat(thrown.getSuppressed()).containsExactly(lookupError);
        verify(receipts, never()).accepted(any(), any(), any());
        verify(receipts, never()).rejected(any(), any(), any());
    }

    @Test
    void businessRejectionIsRequeriedBeforeTerminalReceipt() {
        WmsCompletionInboundReversalCommandV1 command = command("kg", "REQUESTER", "APPROVER");
        when(materialWms.findReversalByIdempotency(
                command.getTenantId(), command.getIdempotencyKey()))
            .thenReturn(Optional.empty(), Optional.empty());
        org.mockito.Mockito.doThrow(
                new MaterialWmsBusinessException("MATERIAL_WMS_409", "stock consumed"))
            .when(materialWms).createCompletionInboundReversal(any());

        WmsReversalCommandProcessor.WmsReversalProcessingResult result =
                processor.process(record(command));

        assertThat(result.status()).isEqualTo("REJECTED");
        verify(materialWms, times(2)).findReversalByIdempotency(
                command.getTenantId(), command.getIdempotencyKey());
        verify(receipts).rejected(command, "MATERIAL_WMS_409", "stock consumed");
    }

    @Test
    void originalDocumentMismatchFailsClosed() {
        WmsCompletionInboundReversalCommandV1 command = command("kg", "REQUESTER", "APPROVER");
        MaterialWmsReversalDocument conflicting = document(command, "10", "OTHER-CIN");
        when(materialWms.findReversalByIdempotency(
                command.getTenantId(), command.getIdempotencyKey()))
            .thenReturn(Optional.of(conflicting));

        WmsReversalCommandProcessor.WmsReversalProcessingResult result =
                processor.process(record(command));

        assertThat(result.status()).isEqualTo("REJECTED");
        verify(materialWms, never()).createCompletionInboundReversal(any());
        verify(receipts).rejected(
                command,
                "WMS_REVERSAL_IDEMPOTENCY_CONFLICT",
                "The red document does not reference the exact original BPI completion-inbound document.");
    }

    @Test
    void unitMismatchFailsClosedBeforeCallingMaterialWms() {
        WmsCompletionInboundReversalCommandV1 command = command("t", "REQUESTER", "APPROVER");

        WmsReversalCommandProcessor.WmsReversalProcessingResult result =
                processor.process(record(command));

        assertThat(result.status()).isEqualTo("REJECTED");
        verify(materialWms, never()).findReversalByIdempotency(any(), any());
        verify(receipts).rejected(
                any(), org.mockito.ArgumentMatchers.eq("WMS_REVERSAL_UNIT_MISMATCH"), any());
    }

    @Test
    void sameRequesterAndApproverIsRejectedBeforeCallingMaterialWms() {
        WmsCompletionInboundReversalCommandV1 command = command("kg", "ADMIN", "ADMIN");

        assertThatThrownBy(() -> processor.process(record(command)))
            .isInstanceOf(WmsCommandRejectedException.class)
            .hasMessageContaining("requester and approver must differ");
        verify(materialWms, never()).findReversalByIdempotency(any(), any());
    }

    @Test
    void mismatchedKafkaIdentityIsRejectedBeforeCallingMaterialWms() {
        WmsCompletionInboundReversalCommandV1 command = command("kg", "REQUESTER", "APPROVER");
        ConsumerRecord<byte[], byte[]> record = record(command);
        record.headers().remove("tenant_id");
        record.headers().add(new RecordHeader(
                "tenant_id", "OTHER".getBytes(StandardCharsets.UTF_8)));

        assertThatThrownBy(() -> processor.process(record))
            .isInstanceOf(WmsCommandRejectedException.class)
            .hasMessageContaining("tenant_id");
        verify(materialWms, never()).findReversalByIdempotency(any(), any());
    }

    private BpiWmsAdapterProperties properties() {
        return new BpiWmsAdapterProperties(
                true,
                "kafka:9092",
                "bpi.wms.completion-inbound-command.v1",
                "bpi.wms.completion-inbound-command.dlq.v1",
                "wms.completion-inbound.receipt.v1",
                "bpi.wms.completion-inbound-reversal-command.v1",
                "bpi.wms.completion-inbound-reversal-command.dlq.v1",
                "wms.completion-inbound-reversal.receipt.v1",
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

    private WmsCompletionInboundReversalCommandV1 command(
            String unit, String requestedBy, String approvedBy) {
        String eventId = "c9288339-f020-59cd-a50e-2e2855115582";
        String batchId = "a0683f52-7d70-5d67-a95f-3b54621e22e1";
        String originalEventId = "2ea229c2-f2bb-5da8-b84c-5b4bd00148ce";
        String idempotencyKey =
                "WMS_COMPLETION_INBOUND_REVERSAL|TENANT|" + batchId + "|TASK|1";
        return WmsCompletionInboundReversalCommandV1.newBuilder()
                .setEventId(eventId)
                .setIdempotencyKey(idempotencyKey)
                .setTenantId("TENANT")
                .setPlantId("PLANT")
                .setLineId("LINE")
                .setBatchId(batchId)
                .setOriginalCommandEventId(originalEventId)
                .setOriginalIdempotencyKey(
                        "WMS_COMPLETION_INBOUND|TENANT|" + batchId + "|GATE|1")
                .setOriginalDocumentId("CIN-001")
                .setBatchNo("BATCH-001")
                .setOrderId("MO-001")
                .setMaterialCode("MAT-001")
                .setQuantityDecimal("10")
                .setQuantityUnit(unit)
                .setReason("Incorrect completion posting")
                .setRequestedBy(requestedBy)
                .setApprovedBy(approvedBy)
                .setRequestedAtMs(Instant.parse("2026-07-21T08:00:00Z").toEpochMilli())
                .setApprovedAtMs(Instant.parse("2026-07-21T08:05:00Z").toEpochMilli())
                .putHeaders("event_id", eventId)
                .putHeaders("idempotency_key", idempotencyKey)
                .putHeaders("tenant_id", "TENANT")
                .putHeaders("schema_version", "v1")
                .putHeaders("trace_id", eventId)
                .build();
    }

    private ConsumerRecord<byte[], byte[]> record(
            WmsCompletionInboundReversalCommandV1 command) {
        String key = command.getTenantId() + "|" + command.getPlantId()
                + "|" + command.getBatchId();
        ConsumerRecord<byte[], byte[]> record = new ConsumerRecord<>(
                properties().reversalCommandTopic(),
                0,
                0,
                key.getBytes(StandardCharsets.UTF_8),
                command.toByteArray());
        command.getHeadersMap().forEach((name, value) -> record.headers().add(
                new RecordHeader(name, value.getBytes(StandardCharsets.UTF_8))));
        return record;
    }

    private MaterialWmsReversalDocument document(
            WmsCompletionInboundReversalCommandV1 command,
            String quantity,
            String originalDocumentNo) {
        MaterialWmsReversalDocument.OriginalDocument original =
                new MaterialWmsReversalDocument.OriginalDocument(
                        101L,
                        originalDocumentNo,
                        "COMPLETION_INBOUND",
                        "BPI",
                        command.getOriginalCommandEventId(),
                        command.getOriginalIdempotencyKey(),
                        "WARE",
                        "REVERSED",
                        "QUALIFIED");
        return new MaterialWmsReversalDocument(
                202L,
                "CIR-001",
                "COMPLETION_INBOUND_REVERSAL",
                "BPI",
                command.getEventId(),
                command.getIdempotencyKey(),
                "WARE",
                "POSTED",
                "QUALIFIED",
                101L,
                original,
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
