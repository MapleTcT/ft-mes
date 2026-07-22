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
), target_materialization AS (
    SELECT materialization.*
      FROM bpi.bpi_dataset_materializations materialization
     WHERE materialization.tenant_id = '1000'
       AND materialization.snapshot_id = (SELECT id FROM target_snapshot)
), target_audits AS (
    SELECT action
      FROM bpi.bpi_audit_events
     WHERE tenant_id = '1000'
       AND object_id IN (SELECT id FROM target_materialization)
)
SELECT jsonb_pretty(jsonb_build_object(
    'marker', :'marker',
    'flywayVersion', (SELECT max(version::integer)
                        FROM bpi.flyway_schema_history WHERE success),
    'snapshotBoundary', (SELECT jsonb_build_object(
        'state', state,
        'materializationState', materialization_state,
        'artifactUri', artifact_uri,
        'manifestDeliveryState', manifest #>> '{phaseBoundary,deliveryState}',
        'manifestMaterializationState', manifest #>> '{phaseBoundary,materializationState}',
        'manifestArtifactUri', manifest #> '{phaseBoundary,artifactUri}',
        'icebergReady', manifest #> '{phaseBoundary,icebergReady}',
        'mlflowRegistered', manifest #> '{phaseBoundary,mlflowRegistered}',
        'modelTrained', manifest #> '{phaseBoundary,modelTrained}'
    ) FROM target_snapshot),
    'materialization', (SELECT jsonb_build_object(
        'rows', count(*),
        'id', min(id::text),
        'state', min(state),
        'revision', min(revision),
        'attemptCount', min(attempt_count),
        'artifactFormat', min(artifact_format),
        'artifactSchemaVersion', min(artifact_schema_version),
        'materializerVersion', min(materializer_version),
        'manifestChecksumMatches', bool_and(manifest_checksum =
            (SELECT manifest_checksum FROM target_snapshot)),
        'artifactUri', min(artifact_uri),
        'objectBucket', min(object_bucket),
        'objectKey', min(object_key),
        'contentSha256', min(content_sha256),
        'byteSize', min(byte_size),
        'rowCount', min(row_count),
        'schemaFieldCount', min(jsonb_array_length(schema_json -> 'fields')),
        'objectVersionId', min(artifact_metadata ->> 'objectVersionId'),
        'objectContentVerified', bool_and(
            (artifact_metadata ->> 'objectContentVerified')::boolean),
        'simulationOnly', bool_or(coalesce(
            (artifact_metadata ->> 'simulationOnly')::boolean, false)),
        'failureCode', min(failure_code),
        'failureDetail', min(failure_detail)
    ) FROM target_materialization),
    'audits', (SELECT jsonb_build_object(
        'rows', count(*),
        'queued', count(*) FILTER (WHERE action = 'DATASET_MATERIALIZATION_QUEUED'),
        'writing', count(*) FILTER (WHERE action = 'DATASET_MATERIALIZATION_WRITING'),
        'failed', count(*) FILTER (WHERE action = 'DATASET_MATERIALIZATION_FAILED'),
        'retried', count(*) FILTER (WHERE action = 'DATASET_MATERIALIZATION_RETRIED'),
        'ready', count(*) FILTER (WHERE action = 'DATASET_MATERIALIZATION_READY')
    ) FROM target_audits),
    'idempotency', (SELECT jsonb_build_object(
        'rows', count(*),
        'completed', count(*) FILTER (WHERE state = 'COMPLETED'),
        'statuses', jsonb_agg(response_status ORDER BY response_status))
      FROM bpi.bpi_api_idempotency
     WHERE tenant_id = '1000'
       AND response_body::text LIKE '%' ||
           (SELECT id::text FROM target_materialization) || '%')
));
