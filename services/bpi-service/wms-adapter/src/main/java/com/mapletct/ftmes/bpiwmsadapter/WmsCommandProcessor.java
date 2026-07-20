package com.mapletct.ftmes.bpiwmsadapter;

import com.google.protobuf.InvalidProtocolBufferException;
import com.mapletct.ftmes.bpi.contract.v1.WmsCompletionInboundCommandV1;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Iterator;
import java.util.Optional;
import java.util.UUID;

@Component
@ConditionalOnProperty(prefix = "bpi.wms-adapter", name = "enabled", havingValue = "true")
public class WmsCommandProcessor {

    private static final String EVENT_ID = "event_id";
    private static final String IDEMPOTENCY_KEY = "idempotency_key";
    private static final String TENANT_ID = "tenant_id";
    private static final String SCHEMA_VERSION = "schema_version";

    private final BpiWmsAdapterProperties properties;
    private final MaterialWmsGateway materialWms;
    private final WmsReceiptPublisher receipts;

    public WmsCommandProcessor(
            BpiWmsAdapterProperties properties,
            MaterialWmsGateway materialWms,
            WmsReceiptPublisher receipts) {
        this.properties = properties;
        this.materialWms = materialWms;
        this.receipts = receipts;
    }

    public WmsProcessingResult process(ConsumerRecord<byte[], byte[]> record) {
        WmsCompletionInboundCommandV1 command = decodeAndValidate(record);
        WmsRoute route = properties.routeFor(
                        command.getTenantId(), command.getPlantId(), command.getLineId())
                .orElseThrow(() -> rejected(
                        "BPI WMS command is outside the exact configured route scope."));
        BigDecimal quantity = quantity(command.getQuantityDecimal());
        if (!route.baseUnit().equals(command.getQuantityUnit())) {
            receipts.rejected(command, "WMS_UNIT_MISMATCH",
                    "Command unit " + command.getQuantityUnit()
                            + " does not match inventory base unit " + route.baseUnit() + ".");
            return new WmsProcessingResult("REJECTED", null, false);
        }

        Optional<MaterialWmsDocument> existing = materialWms.findByIdempotency(
                command.getTenantId(), command.getIdempotencyKey());
        if (existing.isPresent()) {
            return acceptExisting(command, route, quantity, existing.get(), false);
        }

        MaterialWmsCreateRequest createRequest = createRequest(command, route, quantity);
        try {
            materialWms.createCompletionInbound(createRequest);
        } catch (MaterialWmsBusinessException error) {
            Optional<MaterialWmsDocument> raced = materialWms.findByIdempotency(
                    command.getTenantId(), command.getIdempotencyKey());
            if (raced.isPresent()) {
                return acceptExisting(command, route, quantity, raced.get(), false);
            }
            receipts.rejected(command, error.code(), error.getMessage());
            return new WmsProcessingResult("REJECTED", null, false);
        }

        MaterialWmsDocument created = materialWms.findByIdempotency(
                        command.getTenantId(), command.getIdempotencyKey())
                .orElseThrow(() -> new MaterialWmsTransientException(
                        "material-wms acknowledged creation but exact lookup did not find the document."));
        return acceptExisting(command, route, quantity, created, true);
    }

    private WmsProcessingResult acceptExisting(
            WmsCompletionInboundCommandV1 command,
            WmsRoute route,
            BigDecimal quantity,
            MaterialWmsDocument document,
            boolean created) {
        String mismatch = mismatch(command, route, quantity, document);
        if (mismatch != null) {
            receipts.rejected(command, "WMS_IDEMPOTENCY_CONFLICT", mismatch);
            return new WmsProcessingResult("REJECTED", document.documentNo(), created);
        }
        receipts.accepted(command, document,
                (created ? "Created" : "Found") + " durable material-wms document "
                        + document.documentNo() + " (internal id " + document.internalId() + ").");
        return new WmsProcessingResult("ACCEPTED", document.documentNo(), created);
    }

    private String mismatch(
            WmsCompletionInboundCommandV1 command,
            WmsRoute route,
            BigDecimal quantity,
            MaterialWmsDocument document) {
        if (!"BPI".equals(document.sourceSystem())
                || !command.getEventId().equals(document.sourceDocumentId())
                || !command.getIdempotencyKey().equals(document.idempotencyKey())
                || !route.warehouseCode().equals(document.warehouseCode())
                || !"POSTED".equals(document.status())
                || !"QUALIFIED".equals(document.qualityStatus())) {
            return "The idempotency key resolves to a different WMS document identity or state.";
        }
        if (document.lines().size() != 1) {
            return "The BPI completion-inbound document must contain exactly one line.";
        }
        MaterialWmsDocument.Line line = document.lines().get(0);
        if (!"BPI".equals(line.sourceSystem())
                || !(command.getEventId() + ":1").equals(line.sourceLineId())
                || !command.getMaterialCode().equals(line.materialCode())
                || !command.getBatchNo().equals(line.batchNo())
                || !command.getBatchNo().equals(line.productionBatchNo())
                || !route.warehouseCode().equals(line.warehouseCode())
                || !route.locationCode().equals(line.locationCode())
                || quantity.compareTo(line.quantity()) != 0
                || !route.baseUnit().equals(line.unitCode())
                || !"QUALIFIED".equals(line.qualityStatus())) {
            return "The idempotency key resolves to a WMS line with different material, batch, quantity, unit or location.";
        }
        return null;
    }

    private MaterialWmsCreateRequest createRequest(
            WmsCompletionInboundCommandV1 command,
            WmsRoute route,
            BigDecimal quantity) {
        LocalDate storageDate = Instant.ofEpochMilli(command.getRequestedAtMs())
                .atZone(properties.zoneId())
                .toLocalDate();
        return new MaterialWmsCreateRequest(
                command.getTenantId(),
                command.getEventId(),
                command.getIdempotencyKey(),
                command.getBatchNo(),
                command.getOrderId(),
                route.companyCode(),
                route.warehouseCode(),
                storageDate,
                command.getEventId() + ":1",
                command.getMaterialCode(),
                command.getBatchNo(),
                command.getBatchNo(),
                route.locationCode(),
                quantity,
                route.baseUnit(),
                "BPI batch=" + command.getBatchId()
                        + ", qualityGate=" + command.getQualityGateId()
                        + ", revision=" + command.getQualityGateRevision());
    }

    private WmsCompletionInboundCommandV1 decodeAndValidate(ConsumerRecord<byte[], byte[]> record) {
        if (!properties.commandTopic().equals(record.topic())) {
            throw rejected("BPI WMS command arrived from an untrusted topic.");
        }
        byte[] payload = record.value();
        if (payload == null || payload.length == 0 || payload.length > properties.maxPayloadBytes()) {
            throw rejected("BPI WMS command payload size is invalid.");
        }
        WmsCompletionInboundCommandV1 command;
        try {
            command = WmsCompletionInboundCommandV1.parseFrom(payload);
        } catch (InvalidProtocolBufferException error) {
            throw rejected("BPI WMS payload is not valid WmsCompletionInboundCommandV1 Protobuf.", error);
        }
        requiredUuid(command.getEventId(), "event_id");
        requiredUuid(command.getBatchId(), "batch_id");
        required(command.getIdempotencyKey(), "idempotency_key", 256);
        required(command.getTenantId(), "tenant_id", 64);
        required(command.getPlantId(), "plant_id", 128);
        required(command.getLineId(), "line_id", 128);
        required(command.getBatchNo(), "batch_no", 128);
        required(command.getMaterialCode(), "material_code", 128);
        required(command.getQuantityUnit(), "quantity_unit", 64);
        required(command.getQualityGateId(), "quality_gate_id", 256);
        if (command.getQualityGateRevision() == 0 || command.getRequestedAtMs() <= 0) {
            throw rejected("BPI WMS quality gate revision and requested time must be positive.");
        }
        requireHeader(record, EVENT_ID, command.getEventId());
        requireHeader(record, IDEMPOTENCY_KEY, command.getIdempotencyKey());
        requireHeader(record, TENANT_ID, command.getTenantId());
        requireHeader(record, SCHEMA_VERSION, "v1");
        String expectedKey = command.getTenantId() + "|" + command.getPlantId()
                + "|" + command.getBatchId();
        if (!expectedKey.equals(decode(record.key(), "Kafka record key"))) {
            throw rejected("BPI WMS Kafka key does not match the command scope.");
        }
        return command;
    }

    private static BigDecimal quantity(String value) {
        try {
            BigDecimal quantity = new BigDecimal(value);
            if (quantity.signum() <= 0 || quantity.precision() > 20 || quantity.scale() > 6) {
                throw rejected("BPI WMS quantity must be positive with precision 20 and scale 6 or less.");
            }
            return quantity;
        } catch (NumberFormatException error) {
            throw rejected("BPI WMS quantity is not a decimal number.", error);
        }
    }

    private static String required(String value, String field, int maximum) {
        if (value == null || value.isBlank() || value.length() > maximum) {
            throw rejected("BPI WMS " + field + " is required and must not exceed " + maximum + " characters.");
        }
        return value;
    }

    private static void requiredUuid(String value, String field) {
        required(value, field, 64);
        try {
            UUID.fromString(value);
        } catch (IllegalArgumentException error) {
            throw rejected("BPI WMS " + field + " must be a UUID.", error);
        }
    }

    private static void requireHeader(
            ConsumerRecord<byte[], byte[]> record, String name, String expected) {
        Iterator<Header> headers = record.headers().headers(name).iterator();
        if (!headers.hasNext()) {
            throw rejected("BPI WMS Kafka header " + name + " is required.");
        }
        String actual = decode(headers.next().value(), "Kafka header " + name);
        if (headers.hasNext() || !expected.equals(actual)) {
            throw rejected("BPI WMS Kafka header " + name + " must match the command exactly once.");
        }
    }

    private static String decode(byte[] value, String field) {
        if (value == null || value.length == 0) {
            throw rejected(field + " is required.");
        }
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(value))
                    .toString();
        } catch (CharacterCodingException error) {
            throw rejected(field + " is not valid UTF-8.", error);
        }
    }

    private static WmsCommandRejectedException rejected(String message) {
        return new WmsCommandRejectedException(message);
    }

    private static WmsCommandRejectedException rejected(String message, Throwable cause) {
        return new WmsCommandRejectedException(message, cause);
    }

    public record WmsProcessingResult(String status, String documentId, boolean created) {
    }
}
