package com.mapletct.ftmes.bpi.infrastructure.postgres;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mapletct.ftmes.bpi.application.ActorContext;
import com.mapletct.ftmes.bpi.application.error.BpiConflictException;
import com.mapletct.ftmes.bpi.application.error.BpiNotFoundException;
import com.mapletct.ftmes.bpi.domain.BatchState;
import com.mapletct.ftmes.bpi.domain.QualityGateState;
import com.mapletct.ftmes.bpi.domain.QualityGateView;
import com.mapletct.ftmes.bpi.domain.QualityInspectionView;
import com.mapletct.ftmes.bpi.domain.WmsInboundTarget;
import com.mapletct.ftmes.bpi.domain.WmsInboundView;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
public class BatchReleasePostgresRepository {
    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public BatchReleasePostgresRepository(
            NamedParameterJdbcTemplate jdbc,
            ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    public QualityGateView findQualityGate(ActorContext actor, UUID batchId) {
        return findQualityGate(actor.tenantId(), batchId, false);
    }

    public QualityGateView lockQualityGate(String tenantId, UUID batchId) {
        return findQualityGate(tenantId, batchId, true);
    }

    private QualityGateView findQualityGate(String tenantId, UUID batchId, boolean lock) {
        List<QualityGateHeader> matches = jdbc.query("""
                SELECT id, external_gate_id, external_revision, source_event_id, state,
                       release_quantity, quantity_unit, material_code, observed_at
                  FROM bpi.bpi_quality_gates
                 WHERE tenant_id = :tenantId AND batch_id = :batchId
                """ + (lock ? " FOR UPDATE" : ""), new MapSqlParameterSource()
                .addValue("tenantId", tenantId)
                .addValue("batchId", batchId),
                (rs, rowNum) -> new QualityGateHeader(
                            rs.getObject("id", UUID.class),
                            rs.getString("external_gate_id"),
                            rs.getLong("external_revision"),
                            rs.getString("source_event_id"),
                            QualityGateState.valueOf(rs.getString("state")),
                            rs.getBigDecimal("release_quantity"),
                            rs.getString("quantity_unit"),
                            rs.getString("material_code"),
                            rs.getTimestamp("observed_at").toInstant()));
        if (matches.isEmpty()) return null;
        QualityGateHeader gate = matches.get(0);
        return new QualityGateView(
                gate.id(), gate.externalGateId(), gate.externalRevision(), gate.sourceEventId(),
                gate.state(), gate.releaseQuantity(), gate.quantityUnit(), gate.materialCode(),
                gate.observedAt(), findInspections(tenantId, gate.id()));
    }

    public WmsInboundView findWmsInbound(ActorContext actor, UUID batchId) {
        List<WmsInboundView> matches = jdbc.query("""
                SELECT id, command_event_id, idempotency_key, status, receipt_event_id,
                       document_id, error_code, detail, observed_at, revision
                  FROM bpi.bpi_wms_inbound_links
                 WHERE tenant_id = :tenantId AND batch_id = :batchId
                """, new MapSqlParameterSource()
                .addValue("tenantId", actor.tenantId())
                .addValue("batchId", batchId),
                (rs, rowNum) -> new WmsInboundView(
                        rs.getObject("id", UUID.class),
                        rs.getObject("command_event_id", UUID.class),
                        rs.getString("idempotency_key"),
                        rs.getString("status"),
                        rs.getString("receipt_event_id"),
                        rs.getString("document_id"),
                        rs.getString("error_code"),
                        rs.getString("detail"),
                        nullableInstant(rs.getTimestamp("observed_at")),
                        rs.getLong("revision")));
        return matches.isEmpty() ? null : matches.get(0);
    }

    public void saveQualityGate(
            String tenantId,
            UUID batchId,
            UUID gateId,
            String externalGateId,
            long externalRevision,
            String sourceEventId,
            String payloadChecksum,
            QualityGateState state,
            BigDecimal releaseQuantity,
            String quantityUnit,
            String materialCode,
            Instant observedAt,
            QualityGateView previous,
            List<QualityInspectionView> inspections) {
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("id", gateId)
                .addValue("tenantId", tenantId)
                .addValue("batchId", batchId)
                .addValue("externalGateId", externalGateId)
                .addValue("externalRevision", externalRevision)
                .addValue("sourceEventId", sourceEventId)
                .addValue("payloadChecksum", payloadChecksum)
                .addValue("state", state.name())
                .addValue("releaseQuantity", releaseQuantity)
                .addValue("quantityUnit", quantityUnit)
                .addValue("materialCode", materialCode)
                .addValue("observedAt", Timestamp.from(observedAt));
        if (previous == null) {
            jdbc.update("""
                    INSERT INTO bpi.bpi_quality_gates
                        (id, tenant_id, batch_id, external_gate_id, external_revision,
                         source_event_id, payload_checksum, state, release_quantity,
                         quantity_unit, material_code, observed_at)
                    VALUES (:id, :tenantId, :batchId, :externalGateId, :externalRevision,
                            :sourceEventId, :payloadChecksum, :state, :releaseQuantity,
                            :quantityUnit, :materialCode, :observedAt)
                    """, parameters);
        } else {
            int updated = jdbc.update("""
                    UPDATE bpi.bpi_quality_gates
                       SET external_revision = :externalRevision,
                           source_event_id = :sourceEventId,
                           payload_checksum = :payloadChecksum,
                           state = :state,
                           release_quantity = :releaseQuantity,
                           quantity_unit = :quantityUnit,
                           material_code = :materialCode,
                           observed_at = :observedAt,
                           updated_at = now()
                     WHERE tenant_id = :tenantId
                       AND batch_id = :batchId
                       AND id = :id
                       AND external_revision = :previousRevision
                    """, parameters.addValue("previousRevision", previous.externalRevision()));
            if (updated != 1) {
                throw new BpiConflictException(
                        "Quality gate changed concurrently.", previous.externalRevision());
            }
            jdbc.update("""
                    DELETE FROM bpi.bpi_quality_links
                     WHERE tenant_id = :tenantId AND quality_gate_id = :id
                    """, parameters);
        }
        for (QualityInspectionView inspection : inspections) {
            jdbc.update("""
                    INSERT INTO bpi.bpi_quality_links
                        (id, tenant_id, batch_id, quality_gate_id, inspection_code,
                         inspection_record_id, required, disposition, final_result, observed_at)
                    VALUES (:id, :tenantId, :batchId, :qualityGateId, :inspectionCode,
                            :inspectionRecordId, :required, :disposition, :finalResult, :observedAt)
                    """, new MapSqlParameterSource()
                    .addValue("id", UUID.randomUUID())
                    .addValue("tenantId", tenantId)
                    .addValue("batchId", batchId)
                    .addValue("qualityGateId", gateId)
                    .addValue("inspectionCode", inspection.inspectionCode())
                    .addValue("inspectionRecordId", inspection.inspectionRecordId())
                    .addValue("required", inspection.required())
                    .addValue("disposition", inspection.disposition())
                    .addValue("finalResult", inspection.finalResult())
                    .addValue("observedAt", Timestamp.from(inspection.observedAt())));
        }
    }

    public long transitionBatch(
            String tenantId,
            UUID batchId,
            long expectedRevision,
            BatchState fromState,
            BatchState toState,
            String qualityGate,
            String wmsStatus,
            String materialCode) {
        int updated = jdbc.update("""
                UPDATE bpi.bpi_batch_instances
                   SET state = :toState,
                       revision = revision + 1,
                       quality_gate = :qualityGate,
                       wms_status = COALESCE(:wmsStatus, wms_status),
                       material_code = COALESCE(material_code, :materialCode),
                       updated_at = now()
                 WHERE tenant_id = :tenantId
                   AND id = :batchId
                   AND revision = :expectedRevision
                   AND state = :fromState
                """, new MapSqlParameterSource()
                .addValue("tenantId", tenantId)
                .addValue("batchId", batchId)
                .addValue("expectedRevision", expectedRevision)
                .addValue("fromState", fromState.name())
                .addValue("toState", toState.name())
                .addValue("qualityGate", qualityGate)
                .addValue("wmsStatus", wmsStatus)
                .addValue("materialCode", materialCode));
        if (updated != 1) {
            throw new BpiConflictException("Batch quality state changed concurrently.", expectedRevision);
        }
        return expectedRevision + 1;
    }

    public void insertWmsCommand(
            String tenantId,
            String plantId,
            String lineId,
            UUID batchId,
            UUID eventId,
            String topic,
            String partitionKey,
            byte[] payload,
            Map<String, String> headers,
            UUID linkId,
            String idempotencyKey) {
        jdbc.update("""
                INSERT INTO bpi.bpi_outbox_events
                    (id, tenant_id, plant_id, line_id, aggregate_type, aggregate_id,
                     event_type, topic, partition_key, payload, headers)
                VALUES (:id, :tenantId, :plantId, :lineId, 'BATCH_INSTANCE', :batchId,
                        'WMS_COMPLETION_INBOUND_COMMAND', :topic, :partitionKey,
                        :payload, CAST(:headers AS jsonb))
                """, new MapSqlParameterSource()
                .addValue("id", eventId)
                .addValue("tenantId", tenantId)
                .addValue("plantId", plantId)
                .addValue("lineId", lineId)
                .addValue("batchId", batchId)
                .addValue("topic", topic)
                .addValue("partitionKey", partitionKey)
                .addValue("payload", payload)
                .addValue("headers", writeJson(headers)));
        jdbc.update("""
                INSERT INTO bpi.bpi_wms_inbound_links
                    (id, tenant_id, batch_id, command_event_id, idempotency_key, status)
                VALUES (:id, :tenantId, :batchId, :commandEventId, :idempotencyKey, 'PENDING')
                """, new MapSqlParameterSource()
                .addValue("id", linkId)
                .addValue("tenantId", tenantId)
                .addValue("batchId", batchId)
                .addValue("commandEventId", eventId)
                .addValue("idempotencyKey", idempotencyKey));
    }

    public WmsInboundTarget lockWmsInbound(
            String tenantId,
            UUID batchId,
            UUID commandEventId) {
        try {
            return jdbc.queryForObject("""
                    SELECT link.id, link.command_event_id, link.idempotency_key,
                           link.status, link.revision, event.status AS outbox_status
                      FROM bpi.bpi_wms_inbound_links link
                      JOIN bpi.bpi_outbox_events event
                        ON event.id = link.command_event_id
                       AND event.tenant_id = link.tenant_id
                     WHERE link.tenant_id = :tenantId
                       AND link.batch_id = :batchId
                       AND link.command_event_id = :commandEventId
                     FOR UPDATE OF link
                    """, new MapSqlParameterSource()
                    .addValue("tenantId", tenantId)
                    .addValue("batchId", batchId)
                    .addValue("commandEventId", commandEventId),
                    (rs, rowNum) -> new WmsInboundTarget(
                            rs.getObject("id", UUID.class),
                            rs.getObject("command_event_id", UUID.class),
                            rs.getString("idempotency_key"),
                            rs.getString("status"),
                            rs.getLong("revision"),
                            rs.getString("outbox_status")));
        } catch (EmptyResultDataAccessException error) {
            throw new BpiNotFoundException("WMS completion-inbound command not found.");
        }
    }

    public void updateWmsReceipt(
            String tenantId,
            WmsInboundTarget target,
            String status,
            String receiptEventId,
            String documentId,
            String errorCode,
            String detail,
            Instant observedAt) {
        int updated = jdbc.update("""
                UPDATE bpi.bpi_wms_inbound_links
                   SET status = :status,
                       receipt_event_id = :receiptEventId,
                       document_id = :documentId,
                       error_code = :errorCode,
                       detail = :detail,
                       observed_at = :observedAt,
                       revision = revision + 1,
                       updated_at = now()
                 WHERE tenant_id = :tenantId
                   AND id = :id
                   AND revision = :expectedRevision
                   AND status = 'PENDING'
                """, new MapSqlParameterSource()
                .addValue("status", status)
                .addValue("receiptEventId", receiptEventId)
                .addValue("documentId", blankToNull(documentId))
                .addValue("errorCode", blankToNull(errorCode))
                .addValue("detail", limit(blankToNull(detail), 1000))
                .addValue("observedAt", Timestamp.from(observedAt))
                .addValue("tenantId", tenantId)
                .addValue("id", target.linkId())
                .addValue("expectedRevision", target.linkRevision()));
        if (updated != 1) {
            throw new BpiConflictException(
                    "WMS completion-inbound receipt changed concurrently.", target.linkRevision());
        }
    }

    private List<QualityInspectionView> findInspections(String tenantId, UUID gateId) {
        return jdbc.query("""
                SELECT inspection_code, inspection_record_id, required, disposition,
                       final_result, observed_at
                  FROM bpi.bpi_quality_links
                 WHERE tenant_id = :tenantId AND quality_gate_id = :gateId
                 ORDER BY inspection_code
                """, new MapSqlParameterSource()
                .addValue("tenantId", tenantId)
                .addValue("gateId", gateId),
                (rs, rowNum) -> new QualityInspectionView(
                        rs.getString("inspection_code"),
                        rs.getString("inspection_record_id"),
                        rs.getBoolean("required"),
                        rs.getString("disposition"),
                        rs.getBoolean("final_result"),
                        rs.getTimestamp("observed_at").toInstant()));
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception error) {
            throw new IllegalStateException("Could not serialize integration outbox headers", error);
        }
    }

    private static Instant nullableInstant(Timestamp value) {
        return value == null ? null : value.toInstant();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static String limit(String value, int maximum) {
        return value == null || value.length() <= maximum ? value : value.substring(0, maximum);
    }

    private record QualityGateHeader(
            UUID id,
            String externalGateId,
            long externalRevision,
            String sourceEventId,
            QualityGateState state,
            BigDecimal releaseQuantity,
            String quantityUnit,
            String materialCode,
            Instant observedAt) {
    }
}
