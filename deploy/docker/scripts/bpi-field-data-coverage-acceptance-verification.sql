\set ON_ERROR_STOP on

WITH target_run AS (
    SELECT *
      FROM bpi.bpi_shadow_runs
     WHERE tenant_id = '1000' AND run_code = :'marker'
), target_snapshot AS (
    SELECT snapshot.*
      FROM bpi.bpi_point_catalog_snapshots snapshot
     WHERE snapshot.tenant_id = '1000'
       AND snapshot.id = md5(:'marker' || ':catalog')::uuid
), point_status AS (
    SELECT entry.*,
           entry.registered
               AND entry.device_state = 'ACTIVE'
               AND entry.property_present
               AND entry.unit IS NOT NULL
               AND btrim(entry.unit) <> '' AS active_registered,
           entry.source_sequence_enabled
               AND entry.source_sequence_required
               AND entry.source_sequence_origin IN ('DEVICE', 'GATEWAY')
               AND entry.source_sequence_binding_fingerprint IS NOT NULL
               AS physical_identity,
           EXISTS (
               SELECT 1
                 FROM bpi.bpi_source_sequence_evidence_current evidence
                 JOIN target_snapshot snapshot ON true
                WHERE evidence.tenant_id = entry.tenant_id
                  AND evidence.source = snapshot.source
                  AND evidence.source_instance = snapshot.source_instance
                  AND evidence.plant_id = entry.plant_id
                  AND evidence.line_id = entry.line_id
                  AND evidence.product_id = entry.product_id
                  AND evidence.device_id = entry.device_id
                  AND evidence.binding_fingerprint
                      = entry.source_sequence_binding_fingerprint
                  AND evidence.status = 'QUALIFIED'
                  AND evidence.sequence_origin = entry.source_sequence_origin
                  AND evidence.observed_at >= snapshot.observed_at
                  AND evidence.valid_until > snapshot.observed_at
                  AND evidence.valid_until > CURRENT_TIMESTAMP
           ) AS fresh_sequence,
           entry.calibration_version IS NOT NULL
               AND EXISTS (
                   SELECT 1
                     FROM bpi.bpi_point_calibrations calibration
                     JOIN target_snapshot snapshot ON true
                    WHERE calibration.tenant_id = entry.tenant_id
                      AND calibration.plant_id = entry.plant_id
                      AND calibration.line_id = entry.line_id
                      AND calibration.product_id = entry.product_id
                      AND calibration.device_id = entry.device_id
                      AND calibration.property_id = entry.property_id
                      AND calibration.calibration_version = entry.calibration_version
                      AND calibration.state = 'APPROVED'
                      AND calibration.valid_from <= snapshot.observed_at
                      AND calibration.valid_until > snapshot.observed_at
                      AND calibration.valid_from <= CURRENT_TIMESTAMP
                      AND calibration.valid_until > CURRENT_TIMESTAMP
               ) AS approved_calibration
      FROM bpi.bpi_point_catalog_entries entry
     WHERE entry.tenant_id = '1000'
       AND entry.snapshot_id = md5(:'marker' || ':catalog')::uuid
), training_metrics AS (
    SELECT count(DISTINCT review.batch_id)::integer AS reviewed_batches,
           count(DISTINCT
               (review.automatic_start_time AT TIME ZONE 'UTC')::date)::integer
               AS production_days,
           COALESCE(sum(review.start_boundary_accepted::integer), 0)::integer
               AS accepted_start_labels,
           COALESCE(sum((NOT review.start_boundary_accepted)::integer), 0)::integer
               AS rejected_start_labels
      FROM bpi.bpi_shadow_run_batch_reviews review
      JOIN target_run run
        ON run.tenant_id = review.tenant_id
       AND run.id = review.shadow_run_id
     WHERE review.state = 'ACTIVE'
), flyway AS (
    SELECT max(version::integer) AS version
      FROM bpi.flyway_schema_history
     WHERE success
)
SELECT jsonb_pretty(jsonb_build_object(
    'marker', :'marker',
    'database', 'PostgreSQL',
    'flywayVersion', (SELECT version FROM flyway),
    'run', (
        SELECT jsonb_build_object(
            'id', id,
            'state', state,
            'revision', revision,
            'createdBy', created_by,
            'cancelledBy', cancelled_by,
            'ruleVersionId', rule_version_id,
            'topologyVersionId', topology_version_id,
            'pointCatalogSnapshotId', point_catalog_snapshot_id
        )
          FROM target_run
    ),
    'sourceCoverage', jsonb_build_object(
        'pinnedPointCount', (SELECT count(*) FROM point_status),
        'activeRegisteredPointCount', (
            SELECT count(*) FROM point_status WHERE active_registered
        ),
        'physicalIdentityPointCount', (
            SELECT count(*) FROM point_status
             WHERE active_registered AND physical_identity
        ),
        'freshSequenceQualifiedPointCount', (
            SELECT count(*) FROM point_status
             WHERE active_registered AND physical_identity AND fresh_sequence
        ),
        'approvedCalibrationPointCount', (
            SELECT count(*) FROM point_status
             WHERE active_registered AND approved_calibration
        ),
        'readyPointCount', (
            SELECT count(*) FROM point_status
             WHERE active_registered
               AND physical_identity
               AND fresh_sequence
               AND approved_calibration
        )
    ),
    'trainingDataCoverage', (
        SELECT jsonb_build_object(
            'reviewedBatchCount', reviewed_batches,
            'distinctProductionDayCount', production_days,
            'acceptedStartLabelCount', accepted_start_labels,
            'rejectedStartLabelCount', rejected_start_labels
        )
          FROM training_metrics
    ),
    'persistence', jsonb_build_object(
        'batchRows', (
            SELECT count(*) FROM bpi.bpi_batch_instances
             WHERE tenant_id = '1000' AND batch_no LIKE :'marker' || '%'
        ),
        'reviewRows', (
            SELECT count(*) FROM bpi.bpi_shadow_run_batch_reviews review
             JOIN target_run run
               ON run.tenant_id = review.tenant_id
              AND run.id = review.shadow_run_id
        ),
        'auditActions', (
            SELECT jsonb_agg(action ORDER BY created_at, id)
              FROM bpi.bpi_audit_events
             WHERE tenant_id = '1000'
               AND object_id IN (SELECT id FROM target_run)
        ),
        'idempotencyRows', (
            SELECT count(*) FROM bpi.bpi_api_idempotency
             WHERE tenant_id = '1000'
               AND response_body::text LIKE '%' || :'marker' || '%'
        )
    )
));
