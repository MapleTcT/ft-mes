package com.mapletct.ftmes.bpi;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

final class SourceSequenceEvidenceTestFixture {
    static final String FINGERPRINT = "sha256:" + "1".repeat(64);

    private SourceSequenceEvidenceTestFixture() {
    }

    static void qualifyCurrentDevice(
            JdbcTemplate jdbc,
            String tenantId,
            String plantId,
            String lineId,
            String productId,
            String deviceId,
            String sourceEventId) {
        int affected = jdbc.update("""
                INSERT INTO bpi.bpi_source_sequence_evidence_current
                    (id, tenant_id, source, source_instance, plant_id, line_id,
                     product_id, device_id, binding_fingerprint, status, sequence_origin,
                     source_epoch, first_sequence, last_sequence, observation_count,
                     first_observed_at, last_observed_at, valid_until, source_event_id,
                     observed_at, payload_checksum, revision)
                SELECT ?, snapshot.tenant_id, snapshot.source, snapshot.source_instance,
                       snapshot.plant_id, snapshot.line_id, entry.product_id, entry.device_id,
                       entry.source_sequence_binding_fingerprint, 'QUALIFIED',
                       entry.source_sequence_origin, 1, 1, 2, 2,
                       GREATEST(snapshot.observed_at, now()),
                       GREATEST(snapshot.observed_at, now()) + interval '1 second',
                       GREATEST(snapshot.observed_at, now()) + interval '1 day', ?,
                       GREATEST(snapshot.observed_at, now()) + interval '1 second', ?, 1
                  FROM bpi.bpi_point_catalog_snapshots snapshot
                  JOIN bpi.bpi_point_catalog_entries entry
                    ON entry.tenant_id = snapshot.tenant_id
                   AND entry.snapshot_id = snapshot.id
                 WHERE snapshot.tenant_id = ?
                   AND snapshot.plant_id = ?
                   AND snapshot.line_id = ?
                   AND entry.product_id = ?
                   AND entry.device_id = ?
                   AND entry.source_sequence_required
                   AND entry.source_sequence_origin IN ('DEVICE', 'GATEWAY')
                   AND entry.source_sequence_binding_fingerprint IS NOT NULL
                 ORDER BY snapshot.observed_at DESC, snapshot.imported_at DESC, snapshot.id DESC
                 LIMIT 1
                ON CONFLICT
                    (tenant_id, source, source_instance, plant_id, line_id,
                     product_id, device_id, binding_fingerprint)
                DO UPDATE SET
                    status = EXCLUDED.status,
                    sequence_origin = EXCLUDED.sequence_origin,
                    source_epoch = EXCLUDED.source_epoch,
                    first_sequence = EXCLUDED.first_sequence,
                    last_sequence = EXCLUDED.last_sequence,
                    observation_count = EXCLUDED.observation_count,
                    first_observed_at = EXCLUDED.first_observed_at,
                    last_observed_at = EXCLUDED.last_observed_at,
                    valid_until = EXCLUDED.valid_until,
                    source_event_id = EXCLUDED.source_event_id,
                    observed_at = EXCLUDED.observed_at,
                    payload_checksum = EXCLUDED.payload_checksum,
                    revision = bpi.bpi_source_sequence_evidence_current.revision + 1,
                    updated_at = now()
                """, UUID.randomUUID(), sourceEventId, "e".repeat(64), tenantId,
                plantId, lineId, productId, deviceId);
        if (affected != 1) {
            throw new AssertionError("Expected one structured source sequence binding for "
                    + tenantId + "/" + plantId + "/" + lineId + "/" + productId + "/" + deviceId);
        }
    }

    static void cleanup(JdbcTemplate jdbc, String tenantId) {
        jdbc.update("DELETE FROM bpi.bpi_source_sequence_evidence_current WHERE tenant_id = ?", tenantId);
    }
}
