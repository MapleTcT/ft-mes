package com.mapletct.ftmes.bpi.infrastructure.sourcesequence;

import com.mapletct.ftmes.bpi.application.SourceSequenceEvidenceIngestionService;
import com.mapletct.ftmes.bpi.application.error.BpiConflictException;
import com.mapletct.ftmes.bpi.contract.v1.SourceSequenceEvidenceV1;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public class SourceSequenceEvidencePostgresRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public SourceSequenceEvidencePostgresRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public CurrentEvidence lockCurrent(SourceSequenceEvidenceV1 event) {
        List<CurrentEvidence> values = jdbc.query("""
                SELECT id, status, sequence_origin, source_epoch, source_event_id, observed_at, revision
                  FROM bpi.bpi_source_sequence_evidence_current
                 WHERE tenant_id = :tenantId
                   AND source = :source
                   AND source_instance = :sourceInstance
                   AND plant_id = :plantId
                   AND line_id = :lineId
                   AND product_id = :productId
                   AND device_id = :deviceId
                   AND binding_fingerprint = :bindingFingerprint
                 FOR UPDATE
                """, identityParameters(event), (rs, rowNum) -> new CurrentEvidence(
                rs.getObject("id", UUID.class),
                rs.getString("status"),
                rs.getString("sequence_origin"),
                nullableLong(rs, "source_epoch"),
                rs.getString("source_event_id"),
                rs.getTimestamp("observed_at").toInstant(),
                rs.getLong("revision")
        ));
        return values.stream().findFirst().orElse(null);
    }

    public long upsert(
            UUID evidenceId,
            SourceSequenceEvidenceV1 event,
            String payloadChecksum,
            CurrentEvidence current) {
        MapSqlParameterSource parameters = eventParameters(evidenceId, event, payloadChecksum);
        if (current == null) {
            jdbc.update("""
                    INSERT INTO bpi.bpi_source_sequence_evidence_current
                        (id, tenant_id, source, source_instance, plant_id, line_id, product_id,
                         device_id, binding_fingerprint, status, sequence_origin, source_epoch,
                         first_sequence, last_sequence, observation_count, first_observed_at,
                         last_observed_at, valid_until, source_event_id, observed_at,
                         payload_checksum, revision)
                    VALUES (:id, :tenantId, :source, :sourceInstance, :plantId, :lineId, :productId,
                            :deviceId, :bindingFingerprint, :status, :sequenceOrigin, :sourceEpoch,
                            :firstSequence, :lastSequence, :observationCount, :firstObservedAt,
                            :lastObservedAt, :validUntil, :sourceEventId, :observedAt,
                            :payloadChecksum, 1)
                    """, parameters);
            return 1L;
        }

        parameters.addValue("expectedRevision", current.revision());
        int updated = jdbc.update("""
                UPDATE bpi.bpi_source_sequence_evidence_current
                   SET status = :status,
                       sequence_origin = :sequenceOrigin,
                       source_epoch = :sourceEpoch,
                       first_sequence = :firstSequence,
                       last_sequence = :lastSequence,
                       observation_count = :observationCount,
                       first_observed_at = :firstObservedAt,
                       last_observed_at = :lastObservedAt,
                       valid_until = :validUntil,
                       source_event_id = :sourceEventId,
                       observed_at = :observedAt,
                       payload_checksum = :payloadChecksum,
                       revision = revision + 1,
                       updated_at = now()
                 WHERE id = :id
                   AND tenant_id = :tenantId
                   AND revision = :expectedRevision
                """, parameters);
        if (updated != 1) {
            throw new BpiConflictException(
                    "Source sequence evidence changed concurrently.", current.revision());
        }
        return current.revision() + 1L;
    }

    public void insertAudit(
            UUID evidenceId,
            SourceSequenceEvidenceV1 event,
            String actorId,
            Long beforeRevision,
            long afterRevision) {
        MapSqlParameterSource values = eventParameters(evidenceId, event, "");
        values.addValue("auditId", UUID.randomUUID())
                .addValue("actorId", actorId)
                .addValue("beforeRevision", beforeRevision)
                .addValue("afterRevision", afterRevision);
        jdbc.update("""
                INSERT INTO bpi.bpi_audit_events
                    (id, tenant_id, plant_id, line_id, object_type, object_id, action, actor_id,
                     before_revision, after_revision, reason, trace_id, detail)
                VALUES (:auditId, :tenantId, :plantId, :lineId, 'SOURCE_SEQUENCE_EVIDENCE', :id,
                        :action, :actorId, :beforeRevision, :afterRevision, :reason, :sourceEventId,
                        jsonb_build_object(
                            'source', :source,
                            'sourceInstance', :sourceInstance,
                            'productId', :productId,
                            'deviceId', :deviceId,
                            'bindingFingerprint', :bindingFingerprint,
                            'status', :status,
                            'sequenceOrigin', CAST(:sequenceOrigin AS varchar),
                            'sourceEpoch', CAST(:sourceEpoch AS bigint),
                            'firstSequence', CAST(:firstSequence AS bigint),
                            'lastSequence', CAST(:lastSequence AS bigint),
                            'observationCount', CAST(:observationCount AS integer),
                            'validUntil', CAST(:validUntil AS timestamptz),
                            'observedAt', CAST(:observedAt AS timestamptz)))
                """, values.addValue("action", "SOURCE_SEQUENCE_EVIDENCE_" + status(event)));
    }

    private static MapSqlParameterSource identityParameters(SourceSequenceEvidenceV1 event) {
        return new MapSqlParameterSource()
                .addValue("tenantId", event.getTenantId())
                .addValue("source", event.getSource())
                .addValue("sourceInstance", event.getSourceInstance())
                .addValue("plantId", event.getPlantId())
                .addValue("lineId", event.getLineId())
                .addValue("productId", event.getProductId())
                .addValue("deviceId", event.getDeviceId())
                .addValue("bindingFingerprint", event.getBindingFingerprint());
    }

    private static MapSqlParameterSource eventParameters(
            UUID evidenceId,
            SourceSequenceEvidenceV1 event,
            String payloadChecksum) {
        boolean hasSequence = SourceSequenceEvidenceIngestionService.origin(event) != null;
        return identityParameters(event)
                .addValue("id", evidenceId)
                .addValue("status", status(event))
                .addValue("sequenceOrigin", SourceSequenceEvidenceIngestionService.origin(event))
                .addValue("sourceEpoch", hasSequence ? event.getSourceEpoch() : null)
                .addValue("firstSequence", hasSequence ? event.getFirstSequence() : null)
                .addValue("lastSequence", hasSequence ? event.getLastSequence() : null)
                .addValue("observationCount", hasSequence ? event.getObservationCount() : null)
                .addValue("firstObservedAt", timestamp(hasSequence, event.getFirstObservedAtMs()))
                .addValue("lastObservedAt", timestamp(hasSequence, event.getLastObservedAtMs()))
                .addValue("validUntil", timestamp(hasSequence, event.getValidUntilMs()))
                .addValue("sourceEventId", event.getEventId())
                .addValue("observedAt", Timestamp.from(Instant.ofEpochMilli(event.getObservedAtMs())))
                .addValue("payloadChecksum", payloadChecksum)
                .addValue("reason", event.getReason());
    }

    private static Timestamp timestamp(boolean present, long epochMillis) {
        return present ? Timestamp.from(Instant.ofEpochMilli(epochMillis)) : null;
    }

    private static String status(SourceSequenceEvidenceV1 event) {
        return SourceSequenceEvidenceIngestionService.status(event);
    }

    private static Long nullableLong(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
        Number value = (Number) rs.getObject(column);
        return value == null ? null : value.longValue();
    }

    public record CurrentEvidence(
            UUID id,
            String status,
            String sequenceOrigin,
            Long sourceEpoch,
            String sourceEventId,
            Instant observedAt,
            long revision) {
    }
}
