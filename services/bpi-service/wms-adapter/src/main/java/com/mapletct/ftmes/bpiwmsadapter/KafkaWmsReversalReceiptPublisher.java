package com.mapletct.ftmes.bpiwmsadapter;

import com.mapletct.ftmes.bpi.contract.v1.WmsCompletionInboundReversalCommandV1;
import com.mapletct.ftmes.bpi.contract.v1.WmsCompletionInboundReversalReceiptV1;
import com.mapletct.ftmes.bpi.contract.v1.WmsCompletionInboundReversalStatusV1;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(prefix = "bpi.wms-adapter", name = "enabled", havingValue = "true")
public class KafkaWmsReversalReceiptPublisher implements WmsReversalReceiptPublisher {

    private static final UUID RECEIPT_NAMESPACE =
            UUID.fromString("7f0d26b7-63e0-5df6-95c0-cd27c4871df2");

    private final BpiWmsAdapterProperties properties;
    private final KafkaTemplate<byte[], byte[]> kafkaTemplate;

    public KafkaWmsReversalReceiptPublisher(
            BpiWmsAdapterProperties properties,
            @Qualifier("bpiWmsAdapterKafkaTemplate") KafkaTemplate<byte[], byte[]> kafkaTemplate) {
        this.properties = properties;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void accepted(
            WmsCompletionInboundReversalCommandV1 command,
            MaterialWmsReversalDocument document,
            String detail) {
        publish(
                command,
                WmsCompletionInboundReversalStatusV1
                        .WMS_COMPLETION_INBOUND_REVERSAL_ACCEPTED,
                document.documentNo(),
                document.originalDocument().documentNo(),
                "",
                detail);
    }

    @Override
    public void rejected(
            WmsCompletionInboundReversalCommandV1 command,
            String errorCode,
            String detail) {
        publish(
                command,
                WmsCompletionInboundReversalStatusV1
                        .WMS_COMPLETION_INBOUND_REVERSAL_REJECTED,
                "",
                command.getOriginalDocumentId(),
                errorCode,
                detail);
    }

    private void publish(
            WmsCompletionInboundReversalCommandV1 command,
            WmsCompletionInboundReversalStatusV1 status,
            String reversalDocumentId,
            String originalDocumentId,
            String errorCode,
            String detail) {
        String eventId = uuidV5(RECEIPT_NAMESPACE, command.getEventId()).toString();
        WmsCompletionInboundReversalReceiptV1 receipt =
                WmsCompletionInboundReversalReceiptV1.newBuilder()
                        .setEventId(eventId)
                        .setIdempotencyKey(command.getIdempotencyKey())
                        .setCommandEventId(command.getEventId())
                        .setTenantId(command.getTenantId())
                        .setPlantId(command.getPlantId())
                        .setLineId(command.getLineId())
                        .setBatchId(command.getBatchId())
                        .setStatus(status)
                        .setReversalDocumentId(reversalDocumentId)
                        .setOriginalDocumentId(originalDocumentId)
                        .setErrorCode(errorCode)
                        .setDetail(limit(detail, 1000))
                        .setObservedAtMs(Instant.now().toEpochMilli())
                        .putHeaders("event_id", eventId)
                        .putHeaders("idempotency_key", command.getIdempotencyKey())
                        .putHeaders("tenant_id", command.getTenantId())
                        .putHeaders("schema_version", "v1")
                        .putHeaders(
                                "trace_id",
                                command.getHeadersMap().getOrDefault("trace_id", eventId))
                        .build();
        ProducerRecord<byte[], byte[]> record = new ProducerRecord<>(
                properties.reversalReceiptTopic(),
                null,
                null,
                command.getEventId().getBytes(StandardCharsets.UTF_8),
                receipt.toByteArray(),
                headers(receipt));
        try {
            kafkaTemplate.send(record)
                    .get(properties.publishTimeout().toMillis(), TimeUnit.MILLISECONDS);
        } catch (Exception error) {
            throw new MaterialWmsTransientException(
                    "Could not publish the WMS reversal receipt.", error);
        }
    }

    private static Iterable<Header> headers(WmsCompletionInboundReversalReceiptV1 receipt) {
        List<Header> result = new ArrayList<>();
        receipt.getHeadersMap().forEach((key, value) -> result.add(new RecordHeader(
                key, value.getBytes(StandardCharsets.UTF_8))));
        return result;
    }

    private static UUID uuidV5(UUID namespace, String name) {
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            sha1.update(ByteBuffer.allocate(16)
                    .putLong(namespace.getMostSignificantBits())
                    .putLong(namespace.getLeastSignificantBits())
                    .array());
            byte[] digest = sha1.digest(name.getBytes(StandardCharsets.UTF_8));
            digest[6] = (byte) ((digest[6] & 0x0f) | 0x50);
            digest[8] = (byte) ((digest[8] & 0x3f) | 0x80);
            ByteBuffer bytes = ByteBuffer.wrap(digest);
            return new UUID(bytes.getLong(), bytes.getLong());
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-1 is required by the Java runtime.", error);
        }
    }

    private static String limit(String value, int maximum) {
        if (value == null) {
            return "";
        }
        return value.length() <= maximum ? value : value.substring(0, maximum);
    }
}
