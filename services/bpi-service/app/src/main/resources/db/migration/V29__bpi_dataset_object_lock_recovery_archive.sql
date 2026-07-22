CREATE TABLE bpi.bpi_dataset_retention_archives (
    id uuid PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    catalog_publication_id uuid NOT NULL,
    source_snapshot_id uuid NOT NULL,
    source_materialization_id uuid NOT NULL,
    archiver_version varchar(128) NOT NULL,
    archive_profile varchar(128) NOT NULL,
    state varchar(24) NOT NULL
        CHECK (state IN ('QUEUED', 'ARCHIVING', 'VERIFYING', 'LOCKED', 'FAILED')),
    revision bigint NOT NULL DEFAULT 1 CHECK (revision > 0),
    manifest_checksum varchar(64) NOT NULL CHECK (length(manifest_checksum) = 64),
    source_content_sha256 varchar(64) NOT NULL CHECK (length(source_content_sha256) = 64),
    source_object_version_id varchar(255) NOT NULL,
    source_byte_size bigint NOT NULL CHECK (source_byte_size > 0),
    source_row_count bigint NOT NULL CHECK (source_row_count > 0),
    source_schema_json jsonb NOT NULL CHECK (jsonb_typeof(source_schema_json) = 'object'),
    table_identifier varchar(600) NOT NULL,
    iceberg_snapshot_id bigint NOT NULL,
    iceberg_metadata_location text NOT NULL,
    iceberg_schema_id integer NOT NULL,
    iceberg_partition_spec_id integer NOT NULL,
    catalog_verified_row_count bigint NOT NULL CHECK (catalog_verified_row_count > 0),
    catalog_semantic_checksum varchar(64) NOT NULL
        CHECK (length(catalog_semantic_checksum) = 64),
    requested_by varchar(128) NOT NULL,
    request_reason varchar(500) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    started_at timestamptz,
    completed_at timestamptz,
    claim_token uuid,
    claimed_at timestamptz,
    attempt_count integer NOT NULL DEFAULT 0 CHECK (attempt_count >= 0),
    retention_mode varchar(16) CHECK (retention_mode IN ('GOVERNANCE', 'COMPLIANCE')),
    retain_until timestamptz,
    legal_hold_enabled boolean,
    archive_bucket varchar(128),
    archive_prefix text,
    source_archive_object_key text,
    source_archive_version_id varchar(255),
    archive_manifest_object_key text,
    archive_manifest_version_id varchar(255),
    archive_manifest_sha256 varchar(64),
    archive_object_count integer,
    archive_total_bytes bigint,
    verified_row_count bigint,
    verified_semantic_checksum varchar(64),
    archive_metadata jsonb,
    failure_code varchar(128),
    failure_detail varchar(1000),
    CONSTRAINT uq_bpi_dataset_retention_archive_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_bpi_dataset_retention_archive_contract UNIQUE
        (tenant_id, catalog_publication_id, archiver_version),
    CONSTRAINT fk_bpi_dataset_retention_archive_publication_tenant
        FOREIGN KEY (tenant_id, catalog_publication_id)
        REFERENCES bpi.bpi_dataset_catalog_publications (tenant_id, id),
    CONSTRAINT chk_bpi_dataset_retention_archive_catalog_rows
        CHECK (catalog_verified_row_count = source_row_count),
    CONSTRAINT chk_bpi_dataset_retention_policy_bundle CHECK (
        (retention_mode IS NULL AND retain_until IS NULL AND legal_hold_enabled IS NULL)
        OR
        (retention_mode IS NOT NULL AND retain_until IS NOT NULL
            AND retain_until > created_at AND legal_hold_enabled IS NOT NULL)
    ),
    CONSTRAINT chk_bpi_dataset_recovery_object_bundle CHECK (
        (archive_bucket IS NULL AND archive_prefix IS NULL
            AND source_archive_object_key IS NULL AND source_archive_version_id IS NULL
            AND archive_manifest_object_key IS NULL AND archive_manifest_version_id IS NULL
            AND archive_manifest_sha256 IS NULL
            AND archive_object_count IS NULL AND archive_total_bytes IS NULL)
        OR
        (archive_bucket IS NOT NULL AND archive_prefix IS NOT NULL
            AND source_archive_object_key IS NOT NULL AND source_archive_version_id IS NOT NULL
            AND archive_manifest_object_key IS NOT NULL
            AND archive_manifest_version_id IS NOT NULL
            AND archive_manifest_sha256 IS NOT NULL
            AND length(archive_manifest_sha256) = 64
            AND archive_object_count = 2 AND archive_total_bytes > 0)
    ),
    CONSTRAINT chk_bpi_dataset_retention_archive_lifecycle CHECK (
        (state = 'QUEUED'
            AND started_at IS NULL AND completed_at IS NULL
            AND claim_token IS NULL AND claimed_at IS NULL
            AND verified_row_count IS NULL AND verified_semantic_checksum IS NULL
            AND archive_metadata IS NULL
            AND failure_code IS NULL AND failure_detail IS NULL)
        OR
        (state = 'ARCHIVING'
            AND started_at IS NOT NULL AND completed_at IS NULL
            AND claim_token IS NOT NULL AND claimed_at IS NOT NULL
            AND retention_mode IS NOT NULL AND retain_until IS NOT NULL
            AND retain_until > created_at AND legal_hold_enabled IS NOT NULL
            AND verified_row_count IS NULL AND verified_semantic_checksum IS NULL
            AND archive_metadata IS NULL
            AND failure_code IS NULL AND failure_detail IS NULL)
        OR
        (state = 'VERIFYING'
            AND started_at IS NOT NULL AND completed_at IS NULL
            AND claim_token IS NOT NULL AND claimed_at IS NOT NULL
            AND retention_mode IS NOT NULL AND retain_until IS NOT NULL
            AND retain_until > created_at AND legal_hold_enabled IS NOT NULL
            AND archive_bucket IS NOT NULL AND archive_prefix IS NOT NULL
            AND verified_row_count IS NULL AND verified_semantic_checksum IS NULL
            AND archive_metadata IS NULL
            AND failure_code IS NULL AND failure_detail IS NULL)
        OR
        (state = 'LOCKED'
            AND started_at IS NOT NULL AND completed_at IS NOT NULL
            AND claim_token IS NULL AND claimed_at IS NULL
            AND retention_mode IS NOT NULL AND retain_until IS NOT NULL
            AND retain_until > created_at AND legal_hold_enabled IS NOT NULL
            AND archive_bucket IS NOT NULL AND archive_prefix IS NOT NULL
            AND verified_row_count = catalog_verified_row_count
            AND verified_semantic_checksum = catalog_semantic_checksum
            AND archive_metadata IS NOT NULL AND jsonb_typeof(archive_metadata) = 'object'
            AND archive_metadata @> '{"objectLockVerified": true, "recoveryVerified": true}'::jsonb
            AND failure_code IS NULL AND failure_detail IS NULL)
        OR
        (state = 'FAILED'
            AND started_at IS NOT NULL AND completed_at IS NOT NULL
            AND claim_token IS NULL AND claimed_at IS NULL
            AND retention_mode IS NOT NULL AND retain_until IS NOT NULL
            AND retain_until > created_at AND legal_hold_enabled IS NOT NULL
            AND verified_row_count IS NULL AND verified_semantic_checksum IS NULL
            AND archive_metadata IS NULL
            AND failure_code IS NOT NULL AND failure_detail IS NOT NULL)
    )
);

CREATE INDEX idx_bpi_dataset_retention_archive_queue
    ON bpi.bpi_dataset_retention_archives (state, created_at, id)
    WHERE state IN ('QUEUED', 'ARCHIVING', 'VERIFYING');

CREATE INDEX idx_bpi_dataset_retention_archive_publication
    ON bpi.bpi_dataset_retention_archives
       (tenant_id, catalog_publication_id, created_at DESC, id DESC);

CREATE OR REPLACE FUNCTION bpi.guard_dataset_retention_archive_transition()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF OLD.state = 'LOCKED' THEN
        RAISE EXCEPTION 'LOCKED BPI dataset retention archives are immutable';
    END IF;
    IF NOT (
        (OLD.state = 'QUEUED' AND NEW.state = 'ARCHIVING')
        OR (OLD.state = 'ARCHIVING' AND NEW.state IN ('QUEUED', 'VERIFYING', 'FAILED'))
        OR (OLD.state = 'VERIFYING' AND NEW.state IN ('QUEUED', 'LOCKED', 'FAILED'))
        OR (OLD.state = 'FAILED' AND NEW.state = 'QUEUED')
    ) THEN
        RAISE EXCEPTION 'Invalid BPI dataset retention archive transition: % -> %',
            OLD.state, NEW.state;
    END IF;
    IF NEW.revision <> OLD.revision + 1 THEN
        RAISE EXCEPTION 'BPI dataset retention archive revision must increase by one';
    END IF;
    IF NEW.tenant_id <> OLD.tenant_id
        OR NEW.catalog_publication_id <> OLD.catalog_publication_id
        OR NEW.source_snapshot_id <> OLD.source_snapshot_id
        OR NEW.source_materialization_id <> OLD.source_materialization_id
        OR NEW.archiver_version <> OLD.archiver_version
        OR NEW.archive_profile <> OLD.archive_profile
        OR NEW.manifest_checksum <> OLD.manifest_checksum
        OR NEW.source_content_sha256 <> OLD.source_content_sha256
        OR NEW.source_object_version_id <> OLD.source_object_version_id
        OR NEW.source_byte_size <> OLD.source_byte_size
        OR NEW.source_row_count <> OLD.source_row_count
        OR NEW.source_schema_json <> OLD.source_schema_json
        OR NEW.table_identifier <> OLD.table_identifier
        OR NEW.iceberg_snapshot_id <> OLD.iceberg_snapshot_id
        OR NEW.iceberg_metadata_location <> OLD.iceberg_metadata_location
        OR NEW.iceberg_schema_id <> OLD.iceberg_schema_id
        OR NEW.iceberg_partition_spec_id <> OLD.iceberg_partition_spec_id
        OR NEW.catalog_verified_row_count <> OLD.catalog_verified_row_count
        OR NEW.catalog_semantic_checksum <> OLD.catalog_semantic_checksum
        OR NEW.requested_by <> OLD.requested_by
        OR NEW.request_reason <> OLD.request_reason
        OR NEW.created_at <> OLD.created_at THEN
        RAISE EXCEPTION 'BPI dataset retention archive identity is immutable';
    END IF;
    IF OLD.retention_mode IS NOT NULL AND (
        NEW.retention_mode IS DISTINCT FROM OLD.retention_mode
        OR NEW.retain_until IS DISTINCT FROM OLD.retain_until
        OR NEW.legal_hold_enabled IS DISTINCT FROM OLD.legal_hold_enabled
    ) THEN
        RAISE EXCEPTION 'BPI dataset retention policy is immutable after first claim';
    END IF;
    IF OLD.archive_manifest_version_id IS NOT NULL AND (
        NEW.archive_bucket IS DISTINCT FROM OLD.archive_bucket
        OR NEW.archive_prefix IS DISTINCT FROM OLD.archive_prefix
        OR NEW.source_archive_object_key IS DISTINCT FROM OLD.source_archive_object_key
        OR NEW.source_archive_version_id IS DISTINCT FROM OLD.source_archive_version_id
        OR NEW.archive_manifest_object_key IS DISTINCT FROM OLD.archive_manifest_object_key
        OR NEW.archive_manifest_version_id IS DISTINCT FROM OLD.archive_manifest_version_id
        OR NEW.archive_manifest_sha256 IS DISTINCT FROM OLD.archive_manifest_sha256
    ) THEN
        RAISE EXCEPTION 'BPI dataset recovery object identity is immutable';
    END IF;
    RETURN NEW;
END
$$;

CREATE TRIGGER trg_bpi_dataset_retention_archive_transition
    BEFORE UPDATE ON bpi.bpi_dataset_retention_archives
    FOR EACH ROW EXECUTE FUNCTION bpi.guard_dataset_retention_archive_transition();

REVOKE EXECUTE ON ALL FUNCTIONS IN SCHEMA bpi FROM PUBLIC;
ALTER DEFAULT PRIVILEGES IN SCHEMA bpi
    REVOKE EXECUTE ON FUNCTIONS FROM PUBLIC;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'bpi_service') THEN
        GRANT SELECT, INSERT, UPDATE
            ON bpi.bpi_dataset_retention_archives TO bpi_service;
    END IF;
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'bpi_retention_archiver') THEN
        GRANT USAGE ON SCHEMA bpi TO bpi_retention_archiver;
        GRANT SELECT ON bpi.bpi_dataset_definitions TO bpi_retention_archiver;
        GRANT SELECT ON bpi.bpi_dataset_snapshots TO bpi_retention_archiver;
        GRANT SELECT ON bpi.bpi_dataset_materializations TO bpi_retention_archiver;
        GRANT SELECT ON bpi.bpi_dataset_catalog_publications TO bpi_retention_archiver;
        GRANT SELECT, UPDATE
            ON bpi.bpi_dataset_retention_archives TO bpi_retention_archiver;
        GRANT INSERT ON bpi.bpi_audit_events TO bpi_retention_archiver;
    END IF;
END
$$;
