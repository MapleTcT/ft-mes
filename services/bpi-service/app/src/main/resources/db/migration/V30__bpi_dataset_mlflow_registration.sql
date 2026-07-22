CREATE TABLE bpi.bpi_dataset_mlflow_registrations (
    id uuid PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    retention_archive_id uuid NOT NULL,
    catalog_publication_id uuid NOT NULL,
    source_snapshot_id uuid NOT NULL,
    source_materialization_id uuid NOT NULL,
    registrar_version varchar(128) NOT NULL,
    tracking_profile varchar(128) NOT NULL,
    state varchar(24) NOT NULL
        CHECK (state IN ('QUEUED', 'REGISTERING', 'REGISTERED', 'FAILED')),
    revision bigint NOT NULL DEFAULT 1 CHECK (revision > 0),
    manifest_checksum varchar(64) NOT NULL CHECK (length(manifest_checksum) = 64),
    source_content_sha256 varchar(64) NOT NULL CHECK (length(source_content_sha256) = 64),
    source_object_version_id varchar(255) NOT NULL,
    source_byte_size bigint NOT NULL CHECK (source_byte_size > 0),
    source_row_count bigint NOT NULL CHECK (source_row_count > 0),
    source_schema_json jsonb NOT NULL CHECK (jsonb_typeof(source_schema_json) = 'object'),
    table_identifier varchar(600) NOT NULL,
    iceberg_snapshot_id bigint NOT NULL,
    catalog_semantic_checksum varchar(64) NOT NULL
        CHECK (length(catalog_semantic_checksum) = 64),
    archive_bucket varchar(128) NOT NULL,
    source_archive_object_key text NOT NULL,
    source_archive_version_id varchar(255) NOT NULL,
    archive_manifest_object_key text NOT NULL,
    archive_manifest_version_id varchar(255) NOT NULL,
    archive_manifest_sha256 varchar(64) NOT NULL
        CHECK (length(archive_manifest_sha256) = 64),
    experiment_name varchar(500) NOT NULL,
    dataset_name varchar(500) NOT NULL,
    dataset_digest varchar(32) NOT NULL CHECK (length(dataset_digest) = 16),
    requested_by varchar(128) NOT NULL,
    request_reason varchar(500) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    started_at timestamptz,
    completed_at timestamptz,
    claim_token uuid,
    claimed_at timestamptz,
    attempt_count integer NOT NULL DEFAULT 0 CHECK (attempt_count >= 0),
    mlflow_experiment_id varchar(128),
    mlflow_run_id varchar(128),
    mlflow_artifact_uri text,
    mlflow_dataset_source text,
    registration_metadata jsonb,
    failure_code varchar(128),
    failure_detail varchar(1000),
    CONSTRAINT uq_bpi_dataset_mlflow_registration_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_bpi_dataset_mlflow_registration_contract UNIQUE
        (tenant_id, retention_archive_id, registrar_version),
    CONSTRAINT fk_bpi_dataset_mlflow_registration_archive_tenant
        FOREIGN KEY (tenant_id, retention_archive_id)
        REFERENCES bpi.bpi_dataset_retention_archives (tenant_id, id),
    CONSTRAINT chk_bpi_dataset_mlflow_registration_digest
        CHECK (dataset_digest = left(catalog_semantic_checksum, 16)),
    CONSTRAINT chk_bpi_dataset_mlflow_registration_lifecycle CHECK (
        (state = 'QUEUED'
            AND started_at IS NULL AND completed_at IS NULL
            AND claim_token IS NULL AND claimed_at IS NULL
            AND mlflow_experiment_id IS NULL AND mlflow_run_id IS NULL
            AND mlflow_artifact_uri IS NULL AND mlflow_dataset_source IS NULL
            AND registration_metadata IS NULL
            AND failure_code IS NULL AND failure_detail IS NULL)
        OR
        (state = 'REGISTERING'
            AND started_at IS NOT NULL AND completed_at IS NULL
            AND claim_token IS NOT NULL AND claimed_at IS NOT NULL
            AND mlflow_experiment_id IS NULL AND mlflow_run_id IS NULL
            AND mlflow_artifact_uri IS NULL AND mlflow_dataset_source IS NULL
            AND registration_metadata IS NULL
            AND failure_code IS NULL AND failure_detail IS NULL)
        OR
        (state = 'REGISTERED'
            AND started_at IS NOT NULL AND completed_at IS NOT NULL
            AND claim_token IS NULL AND claimed_at IS NULL
            AND mlflow_experiment_id IS NOT NULL AND mlflow_run_id IS NOT NULL
            AND mlflow_artifact_uri IS NOT NULL AND mlflow_dataset_source IS NOT NULL
            AND registration_metadata IS NOT NULL
            AND jsonb_typeof(registration_metadata) = 'object'
            AND registration_metadata @> '{
                "datasetInputVerified": true,
                "lineageVerified": true,
                "modelTrained": false,
                "modelRegistered": false,
                "onlineInferenceEnabled": false,
                "productionActivationAllowed": false
            }'::jsonb
            AND failure_code IS NULL AND failure_detail IS NULL)
        OR
        (state = 'FAILED'
            AND started_at IS NOT NULL AND completed_at IS NOT NULL
            AND claim_token IS NULL AND claimed_at IS NULL
            AND mlflow_experiment_id IS NULL AND mlflow_run_id IS NULL
            AND mlflow_artifact_uri IS NULL AND mlflow_dataset_source IS NULL
            AND registration_metadata IS NULL
            AND failure_code IS NOT NULL AND failure_detail IS NOT NULL)
    )
);

CREATE INDEX idx_bpi_dataset_mlflow_registration_queue
    ON bpi.bpi_dataset_mlflow_registrations (state, created_at, id)
    WHERE state IN ('QUEUED', 'REGISTERING');

CREATE INDEX idx_bpi_dataset_mlflow_registration_archive
    ON bpi.bpi_dataset_mlflow_registrations
       (tenant_id, retention_archive_id, created_at DESC, id DESC);

CREATE OR REPLACE FUNCTION bpi.guard_dataset_mlflow_registration_transition()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF OLD.state = 'REGISTERED' THEN
        RAISE EXCEPTION 'REGISTERED BPI MLflow dataset registrations are immutable';
    END IF;
    IF NOT (
        (OLD.state = 'QUEUED' AND NEW.state = 'REGISTERING')
        OR (OLD.state = 'REGISTERING' AND NEW.state IN ('QUEUED', 'REGISTERED', 'FAILED'))
        OR (OLD.state = 'FAILED' AND NEW.state = 'QUEUED')
    ) THEN
        RAISE EXCEPTION 'Invalid BPI MLflow dataset registration transition: % -> %',
            OLD.state, NEW.state;
    END IF;
    IF NEW.revision <> OLD.revision + 1 THEN
        RAISE EXCEPTION 'BPI MLflow dataset registration revision must increase by one';
    END IF;
    IF NEW.tenant_id <> OLD.tenant_id
        OR NEW.retention_archive_id <> OLD.retention_archive_id
        OR NEW.catalog_publication_id <> OLD.catalog_publication_id
        OR NEW.source_snapshot_id <> OLD.source_snapshot_id
        OR NEW.source_materialization_id <> OLD.source_materialization_id
        OR NEW.registrar_version <> OLD.registrar_version
        OR NEW.tracking_profile <> OLD.tracking_profile
        OR NEW.manifest_checksum <> OLD.manifest_checksum
        OR NEW.source_content_sha256 <> OLD.source_content_sha256
        OR NEW.source_object_version_id <> OLD.source_object_version_id
        OR NEW.source_byte_size <> OLD.source_byte_size
        OR NEW.source_row_count <> OLD.source_row_count
        OR NEW.source_schema_json <> OLD.source_schema_json
        OR NEW.table_identifier <> OLD.table_identifier
        OR NEW.iceberg_snapshot_id <> OLD.iceberg_snapshot_id
        OR NEW.catalog_semantic_checksum <> OLD.catalog_semantic_checksum
        OR NEW.archive_bucket <> OLD.archive_bucket
        OR NEW.source_archive_object_key <> OLD.source_archive_object_key
        OR NEW.source_archive_version_id <> OLD.source_archive_version_id
        OR NEW.archive_manifest_object_key <> OLD.archive_manifest_object_key
        OR NEW.archive_manifest_version_id <> OLD.archive_manifest_version_id
        OR NEW.archive_manifest_sha256 <> OLD.archive_manifest_sha256
        OR NEW.experiment_name <> OLD.experiment_name
        OR NEW.dataset_name <> OLD.dataset_name
        OR NEW.dataset_digest <> OLD.dataset_digest
        OR NEW.requested_by <> OLD.requested_by
        OR NEW.request_reason <> OLD.request_reason
        OR NEW.created_at <> OLD.created_at THEN
        RAISE EXCEPTION 'BPI MLflow dataset registration identity is immutable';
    END IF;
    RETURN NEW;
END
$$;

CREATE TRIGGER trg_bpi_dataset_mlflow_registration_transition
    BEFORE UPDATE ON bpi.bpi_dataset_mlflow_registrations
    FOR EACH ROW EXECUTE FUNCTION bpi.guard_dataset_mlflow_registration_transition();

REVOKE EXECUTE ON ALL FUNCTIONS IN SCHEMA bpi FROM PUBLIC;
ALTER DEFAULT PRIVILEGES IN SCHEMA bpi
    REVOKE EXECUTE ON FUNCTIONS FROM PUBLIC;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'bpi_service') THEN
        GRANT SELECT, INSERT, UPDATE
            ON bpi.bpi_dataset_mlflow_registrations TO bpi_service;
    END IF;
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'bpi_mlflow_registrar') THEN
        GRANT USAGE ON SCHEMA bpi TO bpi_mlflow_registrar;
        GRANT SELECT ON bpi.bpi_dataset_definitions TO bpi_mlflow_registrar;
        GRANT SELECT ON bpi.bpi_dataset_snapshots TO bpi_mlflow_registrar;
        GRANT SELECT ON bpi.bpi_dataset_materializations TO bpi_mlflow_registrar;
        GRANT SELECT ON bpi.bpi_dataset_catalog_publications TO bpi_mlflow_registrar;
        GRANT SELECT ON bpi.bpi_dataset_retention_archives TO bpi_mlflow_registrar;
        GRANT SELECT, UPDATE
            ON bpi.bpi_dataset_mlflow_registrations TO bpi_mlflow_registrar;
        GRANT INSERT ON bpi.bpi_audit_events TO bpi_mlflow_registrar;
    END IF;
END
$$;
