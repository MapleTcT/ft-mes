\set ON_ERROR_STOP on

WITH target_definition AS (
    SELECT *
      FROM bpi.bpi_dataset_definitions
     WHERE tenant_id = '1000' AND dataset_code = :'marker'
), target_snapshot AS (
    SELECT snapshot.*
      FROM bpi.bpi_dataset_snapshots snapshot
      JOIN target_definition definition
        ON definition.tenant_id = snapshot.tenant_id
       AND definition.id = snapshot.dataset_id
     ORDER BY snapshot.snapshot_version DESC
     LIMIT 1
), sample_projection AS (
    SELECT count(*)::integer AS total,
           count(*) FILTER (WHERE included)::integer AS included,
           count(*) FILTER (WHERE NOT included)::integer AS excluded,
           count(*) FILTER (WHERE feature_cutoff = prediction_time)::integer AS cutoff_safe,
           count(*) FILTER (
               WHERE jsonb_exists(feature_payload, 'review.manual_start_time')
                  OR jsonb_exists(feature_payload, 'review.reference_quantity'))::integer AS leaked,
           count(*) FILTER (WHERE batch_no = :'marker' || '_CROSS_PLANT')::integer AS cross_plant_rows
      FROM bpi.bpi_dataset_snapshot_samples
     WHERE tenant_id = '1000'
       AND snapshot_id = (SELECT id FROM target_snapshot)
), exclusion_projection AS (
    SELECT count(*) FILTER (
               WHERE jsonb_exists(exclusion_reasons, 'CONFIDENCE_BELOW_THRESHOLD'))::integer AS low_confidence,
           count(*) FILTER (
               WHERE jsonb_exists(exclusion_reasons, 'LABEL_DELAY_EXCEEDED'))::integer AS label_delayed
      FROM bpi.bpi_dataset_snapshot_samples
     WHERE tenant_id = '1000'
       AND snapshot_id = (SELECT id FROM target_snapshot)
)
SELECT jsonb_pretty(jsonb_build_object(
    'marker', :'marker',
    'flywayVersion', (SELECT max(version::integer) FROM bpi.flyway_schema_history WHERE success),
    'definition', (SELECT jsonb_build_object(
        'rows', count(*), 'id', min(id::text), 'revision', min(revision),
        'plantId', min(plant_id), 'lineIds', min(line_ids::text)::jsonb,
        'checksumLength', min(length(checksum))) FROM target_definition),
    'snapshot', (SELECT jsonb_build_object(
        'rows', count(*), 'id', min(id::text), 'state', min(state),
        'revision', min(revision), 'manifestChecksumLength', min(length(manifest_checksum)),
        'includedCount', min(included_count), 'excludedCount', min(excluded_count),
        'materializationState', min(materialization_state), 'artifactUriRows', count(artifact_uri))
        FROM target_snapshot),
    'samples', (SELECT to_jsonb(sample_projection) FROM sample_projection),
    'exclusions', (SELECT to_jsonb(exclusion_projection) FROM exclusion_projection),
    'auditRows', (SELECT count(*) FROM bpi.bpi_audit_events
                   WHERE tenant_id = '1000'
                     AND object_id IN (
                         SELECT id FROM target_definition
                         UNION ALL SELECT id FROM target_snapshot)),
    'idempotency', (SELECT jsonb_build_object(
        'rows', count(*),
        'completed', count(*) FILTER (WHERE state = 'COMPLETED'),
        'statuses', jsonb_agg(response_status ORDER BY response_status))
        FROM bpi.bpi_api_idempotency
        WHERE tenant_id = '1000' AND response_body::text LIKE '%' || :'marker' || '%')
));
