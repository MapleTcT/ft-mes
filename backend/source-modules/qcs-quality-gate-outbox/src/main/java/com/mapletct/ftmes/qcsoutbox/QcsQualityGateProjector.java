package com.mapletct.ftmes.qcsoutbox;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mapletct.ftmes.bpi.contract.v1.QcsInspectionDispositionV1;
import com.mapletct.ftmes.bpi.contract.v1.QcsInspectionResultV1;
import com.mapletct.ftmes.bpi.contract.v1.QcsQualityGateV1;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
public class QcsQualityGateProjector {

    private final ObjectMapper objectMapper;

    public QcsQualityGateProjector(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public QcsQualityGateV1 project(QcsQualityGateOutboxRecord record, ResolvedBpiBatch batch) {
        validateRecord(record, batch);
        List<QcsInspectionSnapshot> snapshots = inspections(record.getInspectionsJson());
        if (snapshots.isEmpty() || snapshots.size() > 200) {
            throw new PermanentQcsOutboxException("QCS outbox must contain 1 to 200 required inspections");
        }

        QcsQualityGateV1.Builder event = QcsQualityGateV1.newBuilder()
            .setEventId(record.getEventId())
            .setIdempotencyKey(record.getIdempotencyKey())
            .setTenantId(record.getTenantId())
            .setPlantId(record.getPlantId())
            .setLineId(record.getLineId())
            .setBatchId(batch.getId())
            .setQualityGateId(record.getQualityGateId())
            .setQualityGateRevision(record.getQualityGateRevision())
            .setObservedAtMs(record.getObservedAtMs())
            .putHeaders("event_id", record.getEventId())
            .putHeaders("idempotency_key", record.getIdempotencyKey())
            .putHeaders("tenant_id", record.getTenantId())
            .putHeaders("schema_version", "v1")
            .putHeaders("trace_id", record.getEventId())
            .putHeaders("source", "QCS")
            .putHeaders("qcs_report_id", Long.toString(record.getQcsReportId()))
            .putHeaders("qcs_report_version", Integer.toString(record.getQcsReportVersion()))
            .putHeaders("source_order_id", record.getSourceOrderId());

        boolean accepted = true;
        Set<String> codes = new HashSet<String>();
        for (QcsInspectionSnapshot snapshot : snapshots) {
            String code = requireText(snapshot.getInspectionCode(), "inspectionCode", 128);
            if (!codes.add(code)) {
                throw new PermanentQcsOutboxException("QCS inspectionCode values must be unique: " + code);
            }
            String inspectionRecordId = requireText(
                snapshot.getInspectionRecordId(), "inspectionRecordId", 256);
            if (!snapshot.isRequired() || !snapshot.isFinalResult() || snapshot.getObservedAtMs() <= 0) {
                throw new PermanentQcsOutboxException("QCS outbox contains a non-final or non-required inspection");
            }
            QcsInspectionDispositionV1 disposition = disposition(snapshot.getDisposition());
            if (disposition == QcsInspectionDispositionV1.QCS_INSPECTION_REJECTED) accepted = false;
            event.addInspections(QcsInspectionResultV1.newBuilder()
                .setInspectionCode(code)
                .setInspectionRecordId(inspectionRecordId)
                .setRequired(true)
                .setDisposition(disposition)
                .setFinalResult(true)
                .setObservedAtMs(snapshot.getObservedAtMs())
                .build());
        }

        if (!blank(batch.getMaterialCode())) event.setMaterialCode(batch.getMaterialCode().trim());
        if (accepted) {
            BigDecimal quantity = batch.getQuantity();
            if (quantity == null || quantity.signum() <= 0 || blank(batch.getQuantityUnit())
                    || blank(batch.getMaterialCode())) {
                throw new PermanentQcsOutboxException(
                    "Accepted QCS gate requires canonical BPI material, positive quantity and quantity unit");
            }
            event.setReleaseQuantityDecimal(quantity.stripTrailingZeros().toPlainString());
            event.setQuantityUnit(batch.getQuantityUnit().trim());
        }
        return event.build();
    }

    private List<QcsInspectionSnapshot> inspections(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<QcsInspectionSnapshot>>() { });
        } catch (Exception error) {
            throw new PermanentQcsOutboxException("QCS inspection snapshot is not valid JSON", error);
        }
    }

    private static void validateRecord(QcsQualityGateOutboxRecord record, ResolvedBpiBatch batch) {
        requireText(record.getEventId(), "eventId", 256);
        requireText(record.getIdempotencyKey(), "idempotencyKey", 256);
        requireText(record.getTenantId(), "tenantId", 64);
        requireText(record.getPlantId(), "plantId", 64);
        requireText(record.getLineId(), "lineId", 64);
        requireText(record.getSourceOrderId(), "sourceOrderId", 255);
        requireText(record.getQualityGateId(), "qualityGateId", 256);
        if (record.getQualityGateRevision() <= 0 || record.getObservedAtMs() <= 0) {
            throw new PermanentQcsOutboxException("QCS gate revision and observed time must be positive");
        }
        if (!record.getTenantId().equals(batch.getTenantId())
                || !record.getPlantId().equals(batch.getPlantId())
                || !record.getLineId().equals(batch.getLineId())
                || !record.getSourceOrderId().equals(batch.getOrderId())) {
            throw new PermanentQcsOutboxException("BPI batch identity does not match the QCS outbox record");
        }
        try {
            UUID.fromString(batch.getId());
        } catch (Exception error) {
            throw new PermanentQcsOutboxException("BPI batch id is not a UUID", error);
        }
    }

    private static QcsInspectionDispositionV1 disposition(String value) {
        if ("ACCEPTED".equals(value)) return QcsInspectionDispositionV1.QCS_INSPECTION_ACCEPTED;
        if ("REJECTED".equals(value)) return QcsInspectionDispositionV1.QCS_INSPECTION_REJECTED;
        throw new PermanentQcsOutboxException("Unsupported QCS inspection disposition: " + value);
    }

    private static String requireText(String value, String field, int maxLength) {
        if (blank(value) || value.trim().length() > maxLength) {
            throw new PermanentQcsOutboxException(field + " is required and must not exceed " + maxLength);
        }
        return value.trim();
    }

    private static boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
