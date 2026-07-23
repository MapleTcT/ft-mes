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
), target_archive AS (
    SELECT archive.*
      FROM bpi.bpi_dataset_retention_archives archive
     WHERE archive.tenant_id = '1000'
       AND archive.catalog_publication_id = (SELECT id FROM target_publication)
), target_registration AS (
    SELECT registration.*
      FROM bpi.bpi_dataset_mlflow_registrations registration
     WHERE registration.tenant_id = '1000'
       AND registration.retention_archive_id = (SELECT id FROM target_archive)
), target_audits AS (
    SELECT audit.*
      FROM bpi.bpi_audit_events audit
     WHERE audit.tenant_id = '1000'
       AND audit.object_id = (SELECT id FROM target_registration)
), target_idempotency AS (
    SELECT idempotency.*
      FROM bpi.bpi_api_idempotency idempotency
     WHERE idempotency.tenant_id = '1000'
       AND (
           idempotency.response_body::text LIKE '%' ||
               (SELECT id::text FROM target_registration) || '%'
           OR idempotency.resource_path = '/bpi/v1/dataset-retention-archives/' ||
               (SELECT id::text FROM target_archive) || '/mlflow-registrations'
           OR idempotency.resource_path = '/bpi/v1/dataset-mlflow-registrations/' ||
               (SELECT id::text FROM target_registration) || '/retry'
       )
)
SELECT jsonb_pretty(jsonb_build_object(
    'marker', :'marker',
    'flywayVersion', (SELECT max(version::integer)
                        FROM bpi.flyway_schema_history WHERE success),
    'definitionId', (SELECT id FROM target_definition),
    'snapshotId', (SELECT id FROM target_snapshot),
    'materializationId', (SELECT id FROM target_materialization),
    'publicationId', (SELECT id FROM target_publication),
    'archive', (SELECT jsonb_build_object(
        'id', id,
        'state', state,
        'archiveBucket', archive_bucket,
        'sourceArchiveObjectKey', source_archive_object_key,
        'sourceArchiveVersionId', source_archive_version_id,
        'catalogSemanticChecksum', catalog_semantic_checksum,
        'objectLockVerified', archive_metadata -> 'objectLockVerified',
        'recoveryVerified', archive_metadata -> 'recoveryVerified'
    ) FROM target_archive),
    'registration', (SELECT jsonb_build_object(
        'id', id,
        'state', state,
        'revision', revision,
        'attemptCount', attempt_count,
        'registrarVersion', registrar_version,
        'trackingProfile', tracking_profile,
        'experimentName', experiment_name,
        'datasetName', dataset_name,
        'datasetDigest', dataset_digest,
        'mlflowExperimentId', mlflow_experiment_id,
        'mlflowRunId', mlflow_run_id,
        'mlflowArtifactUri', mlflow_artifact_uri,
        'mlflowDatasetSource', mlflow_dataset_source,
        'sourceArchiveVersionId', source_archive_version_id,
        'tableIdentifier', table_identifier,
        'icebergSnapshotId', iceberg_snapshot_id::text,
        'catalogSemanticChecksum', catalog_semantic_checksum,
        'sourceFactsVerified', registration_metadata -> 'sourceFactsVerified',
        'datasetInputVerified', registration_metadata -> 'datasetInputVerified',
        'lineageVerified', registration_metadata -> 'lineageVerified',
        'modelTrained', registration_metadata -> 'modelTrained',
        'modelRegistered', registration_metadata -> 'modelRegistered',
        'onlineInferenceEnabled', registration_metadata -> 'onlineInferenceEnabled',
        'productionActivationAllowed',
            registration_metadata -> 'productionActivationAllowed',
        'failureCode', failure_code,
        'failureDetail', failure_detail
    ) FROM target_registration),
    'audits', (SELECT jsonb_build_object(
        'rows', count(*),
        'sequence', jsonb_agg(action ORDER BY after_revision, created_at, id),
        'queued', count(*) FILTER (
            WHERE action = 'DATASET_MLFLOW_REGISTRATION_QUEUED'),
        'registering', count(*) FILTER (
            WHERE action = 'DATASET_MLFLOW_REGISTRATION_REGISTERING'),
        'failed', count(*) FILTER (
            WHERE action = 'DATASET_MLFLOW_REGISTRATION_FAILED'),
        'retried', count(*) FILTER (
            WHERE action = 'DATASET_MLFLOW_REGISTRATION_RETRIED'),
        'registered', count(*) FILTER (
            WHERE action = 'DATASET_MLFLOW_REGISTRATION_REGISTERED')
    ) FROM target_audits),
    'idempotency', (SELECT jsonb_build_object(
        'rows', count(*),
        'completed', count(*) FILTER (WHERE state = 'COMPLETED'),
        'statuses', jsonb_agg(response_status ORDER BY created_at),
        'paths', jsonb_agg(resource_path ORDER BY created_at)
    ) FROM target_idempotency)
));
