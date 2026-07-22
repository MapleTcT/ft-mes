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
), target_publication AS (
    SELECT publication.*
      FROM bpi.bpi_dataset_catalog_publications publication
     WHERE publication.tenant_id = '1000'
       AND publication.materialization_id = (SELECT id FROM target_materialization)
), target_audits AS (
    SELECT audit.*
      FROM bpi.bpi_audit_events audit
     WHERE audit.tenant_id = '1000'
       AND audit.object_id = (SELECT id FROM target_publication)
)
SELECT jsonb_pretty(jsonb_build_object(
    'marker', :'marker',
    'flywayVersion', (SELECT max(version::integer)
                        FROM bpi.flyway_schema_history WHERE success),
    'snapshot', (SELECT jsonb_build_object(
        'id', id,
        'state', state,
        'revision', revision,
        'manifestChecksum', manifest_checksum,
        'includedCount', included_count,
        'excludedCount', excluded_count
    ) FROM target_snapshot),
    'materialization', (SELECT jsonb_build_object(
        'id', id,
        'state', state,
        'revision', revision,
        'attemptCount', attempt_count,
        'contentSha256', content_sha256,
        'objectVersionId', artifact_metadata ->> 'objectVersionId',
        'byteSize', byte_size,
        'rowCount', row_count,
        'schemaFieldCount', jsonb_array_length(schema_json -> 'fields')
    ) FROM target_materialization),
    'publication', (SELECT jsonb_build_object(
        'id', id,
        'state', state,
        'revision', revision,
        'attemptCount', attempt_count,
        'catalog', catalog_name,
        'namespace', catalog_namespace,
        'table', table_name,
        'tableIdentifier', table_identifier,
        'publisherVersion', publisher_version,
        'icebergSnapshotId', iceberg_snapshot_id::text,
        'metadataLocation', iceberg_metadata_location,
        'schemaId', iceberg_schema_id,
        'partitionSpecId', iceberg_partition_spec_id,
        'sourceContentSha256', source_content_sha256,
        'sourceObjectVersionId', source_object_version_id,
        'sourceRows', source_row_count,
        'verifiedRows', verified_row_count,
        'semanticChecksum', semantic_checksum,
        'catalogSnapshotVerified', catalog_metadata -> 'catalogSnapshotVerified',
        'icebergReady', catalog_metadata -> 'icebergReady',
        'mlflowRegistered', catalog_metadata -> 'mlflowRegistered',
        'modelTrained', catalog_metadata -> 'modelTrained',
        'failureCode', failure_code,
        'failureDetail', failure_detail
    ) FROM target_publication),
    'audits', (SELECT jsonb_build_object(
        'rows', count(*),
        'sequence', jsonb_agg(action ORDER BY after_revision, created_at, id),
        'queued', count(*) FILTER (
            WHERE action = 'DATASET_CATALOG_PUBLICATION_QUEUED'),
        'committing', count(*) FILTER (
            WHERE action = 'DATASET_CATALOG_PUBLICATION_COMMITTING'),
        'claimRecovered', count(*) FILTER (
            WHERE action = 'DATASET_CATALOG_PUBLICATION_CLAIM_RECOVERED'),
        'verifying', count(*) FILTER (
            WHERE action = 'DATASET_CATALOG_PUBLICATION_VERIFYING'),
        'failed', count(*) FILTER (
            WHERE action = 'DATASET_CATALOG_PUBLICATION_FAILED'),
        'retried', count(*) FILTER (
            WHERE action = 'DATASET_CATALOG_PUBLICATION_RETRIED'),
        'ready', count(*) FILTER (
            WHERE action = 'DATASET_CATALOG_PUBLICATION_READY')
    ) FROM target_audits),
    'idempotency', (SELECT jsonb_build_object(
        'rows', count(*),
        'completed', count(*) FILTER (WHERE state = 'COMPLETED'),
        'statuses', jsonb_agg(response_status ORDER BY created_at)
    )
      FROM bpi.bpi_api_idempotency
     WHERE tenant_id = '1000'
       AND response_body::text LIKE '%' ||
           (SELECT id::text FROM target_publication) || '%')
));
