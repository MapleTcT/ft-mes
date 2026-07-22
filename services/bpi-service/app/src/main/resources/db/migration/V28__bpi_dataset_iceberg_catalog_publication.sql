CREATE TABLE bpi.bpi_dataset_catalog_publications (
    id uuid PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    materialization_id uuid NOT NULL,
    source_snapshot_id uuid NOT NULL,
    catalog_name varchar(128) NOT NULL,
    catalog_namespace varchar(255) NOT NULL,
    table_name varchar(255) NOT NULL,
    table_identifier varchar(600) NOT NULL,
    publisher_version varchar(128) NOT NULL,
    state varchar(24) NOT NULL
        CHECK (state IN ('QUEUED', 'COMMITTING', 'VERIFYING', 'READY', 'FAILED')),
    revision bigint NOT NULL DEFAULT 1 CHECK (revision > 0),
    manifest_checksum varchar(64) NOT NULL CHECK (length(manifest_checksum) = 64),
    source_content_sha256 varchar(64) NOT NULL CHECK (length(source_content_sha256) = 64),
    source_object_version_id varchar(255) NOT NULL,
    source_byte_size bigint NOT NULL CHECK (source_byte_size > 0),
    source_row_count bigint NOT NULL CHECK (source_row_count > 0),
    source_schema_json jsonb NOT NULL CHECK (jsonb_typeof(source_schema_json) = 'object'),
    requested_by varchar(128) NOT NULL,
    request_reason varchar(500) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    started_at timestamptz,
    completed_at timestamptz,
    claim_token uuid,
    claimed_at timestamptz,
    attempt_count integer NOT NULL DEFAULT 0 CHECK (attempt_count >= 0),
    iceberg_snapshot_id bigint,
    iceberg_metadata_location text,
    iceberg_schema_id integer,
    iceberg_partition_spec_id integer,
    verified_row_count bigint,
    semantic_checksum varchar(64),
    catalog_metadata jsonb,
    failure_code varchar(128),
    failure_detail varchar(1000),
    CONSTRAINT uq_bpi_dataset_catalog_publication_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_bpi_dataset_catalog_publication_contract UNIQUE
        (tenant_id, materialization_id, catalog_name, publisher_version),
    CONSTRAINT fk_bpi_dataset_catalog_publication_materialization_tenant
        FOREIGN KEY (tenant_id, materialization_id)
        REFERENCES bpi.bpi_dataset_materializations (tenant_id, id),
    CONSTRAINT chk_bpi_dataset_catalog_publication_identifier CHECK (
        table_identifier = catalog_name || '.' || catalog_namespace || '.' || table_name
    ),
    CONSTRAINT chk_bpi_dataset_catalog_publication_commit_tuple CHECK (
        (iceberg_snapshot_id IS NULL
            AND iceberg_metadata_location IS NULL
            AND iceberg_schema_id IS NULL
            AND iceberg_partition_spec_id IS NULL)
        OR
        (iceberg_snapshot_id IS NOT NULL
            AND iceberg_metadata_location IS NOT NULL
            AND iceberg_schema_id IS NOT NULL
            AND iceberg_partition_spec_id IS NOT NULL)
    ),
    CONSTRAINT chk_bpi_dataset_catalog_publication_lifecycle CHECK (
        (state = 'QUEUED'
            AND started_at IS NULL AND completed_at IS NULL
            AND claim_token IS NULL AND claimed_at IS NULL
            AND verified_row_count IS NULL AND semantic_checksum IS NULL
            AND catalog_metadata IS NULL
            AND failure_code IS NULL AND failure_detail IS NULL)
        OR
        (state = 'COMMITTING'
            AND started_at IS NOT NULL AND completed_at IS NULL
            AND claim_token IS NOT NULL AND claimed_at IS NOT NULL
            AND verified_row_count IS NULL AND semantic_checksum IS NULL
            AND catalog_metadata IS NULL
            AND failure_code IS NULL AND failure_detail IS NULL)
        OR
        (state = 'VERIFYING'
            AND started_at IS NOT NULL AND completed_at IS NULL
            AND claim_token IS NOT NULL AND claimed_at IS NOT NULL
            AND iceberg_snapshot_id IS NOT NULL
            AND verified_row_count IS NULL AND semantic_checksum IS NULL
            AND catalog_metadata IS NULL
            AND failure_code IS NULL AND failure_detail IS NULL)
        OR
        (state = 'READY'
            AND started_at IS NOT NULL AND completed_at IS NOT NULL
            AND claim_token IS NULL AND claimed_at IS NULL
            AND iceberg_snapshot_id IS NOT NULL
            AND verified_row_count IS NOT NULL AND verified_row_count > 0
            AND verified_row_count = source_row_count
            AND semantic_checksum IS NOT NULL AND length(semantic_checksum) = 64
            AND catalog_metadata IS NOT NULL AND jsonb_typeof(catalog_metadata) = 'object'
            AND catalog_metadata @> '{"catalogSnapshotVerified": true}'::jsonb
            AND failure_code IS NULL AND failure_detail IS NULL)
        OR
        (state = 'FAILED'
            AND started_at IS NOT NULL AND completed_at IS NOT NULL
            AND claim_token IS NULL AND claimed_at IS NULL
            AND verified_row_count IS NULL AND semantic_checksum IS NULL
            AND catalog_metadata IS NULL
            AND failure_code IS NOT NULL AND failure_detail IS NOT NULL)
    )
);

CREATE INDEX idx_bpi_dataset_catalog_publication_queue
    ON bpi.bpi_dataset_catalog_publications (state, created_at, id)
    WHERE state IN ('QUEUED', 'COMMITTING', 'VERIFYING');

CREATE INDEX idx_bpi_dataset_catalog_publication_materialization
    ON bpi.bpi_dataset_catalog_publications
       (tenant_id, materialization_id, created_at DESC, id DESC);

CREATE INDEX idx_bpi_dataset_catalog_publication_snapshot
    ON bpi.bpi_dataset_catalog_publications
       (tenant_id, source_snapshot_id, created_at DESC, id DESC);

CREATE OR REPLACE FUNCTION bpi.guard_dataset_catalog_publication_transition()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF OLD.state = 'READY' THEN
        RAISE EXCEPTION 'READY BPI dataset catalog publications are immutable';
    END IF;
    IF NOT (
        (OLD.state = 'QUEUED' AND NEW.state = 'COMMITTING')
        OR (OLD.state = 'COMMITTING' AND NEW.state IN ('QUEUED', 'VERIFYING', 'FAILED'))
        OR (OLD.state = 'VERIFYING' AND NEW.state IN ('QUEUED', 'READY', 'FAILED'))
        OR (OLD.state = 'FAILED' AND NEW.state = 'QUEUED')
    ) THEN
        RAISE EXCEPTION 'Invalid BPI dataset catalog publication transition: % -> %',
            OLD.state, NEW.state;
    END IF;
    IF NEW.revision <> OLD.revision + 1 THEN
        RAISE EXCEPTION 'BPI dataset catalog publication revision must increase by one';
    END IF;
    IF NEW.tenant_id <> OLD.tenant_id
        OR NEW.materialization_id <> OLD.materialization_id
        OR NEW.source_snapshot_id <> OLD.source_snapshot_id
        OR NEW.catalog_name <> OLD.catalog_name
        OR NEW.catalog_namespace <> OLD.catalog_namespace
        OR NEW.table_name <> OLD.table_name
        OR NEW.table_identifier <> OLD.table_identifier
        OR NEW.publisher_version <> OLD.publisher_version
        OR NEW.manifest_checksum <> OLD.manifest_checksum
        OR NEW.source_content_sha256 <> OLD.source_content_sha256
        OR NEW.source_object_version_id <> OLD.source_object_version_id
        OR NEW.source_byte_size <> OLD.source_byte_size
        OR NEW.source_row_count <> OLD.source_row_count
        OR NEW.source_schema_json <> OLD.source_schema_json
        OR NEW.requested_by <> OLD.requested_by
        OR NEW.request_reason <> OLD.request_reason
        OR NEW.created_at <> OLD.created_at THEN
        RAISE EXCEPTION 'BPI dataset catalog publication identity is immutable';
    END IF;
    IF OLD.iceberg_snapshot_id IS NOT NULL AND (
        NEW.iceberg_snapshot_id IS DISTINCT FROM OLD.iceberg_snapshot_id
        OR NEW.iceberg_metadata_location IS DISTINCT FROM OLD.iceberg_metadata_location
        OR NEW.iceberg_schema_id IS DISTINCT FROM OLD.iceberg_schema_id
        OR NEW.iceberg_partition_spec_id IS DISTINCT FROM OLD.iceberg_partition_spec_id
    ) THEN
        RAISE EXCEPTION 'BPI dataset catalog commit identity is immutable';
    END IF;
    RETURN NEW;
END
$$;

CREATE TRIGGER trg_bpi_dataset_catalog_publication_transition
    BEFORE UPDATE ON bpi.bpi_dataset_catalog_publications
    FOR EACH ROW EXECUTE FUNCTION bpi.guard_dataset_catalog_publication_transition();

REVOKE EXECUTE ON ALL FUNCTIONS IN SCHEMA bpi FROM PUBLIC;
ALTER DEFAULT PRIVILEGES IN SCHEMA bpi
    REVOKE EXECUTE ON FUNCTIONS FROM PUBLIC;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'bpi_service') THEN
        GRANT SELECT, INSERT, UPDATE
            ON bpi.bpi_dataset_catalog_publications TO bpi_service;
    END IF;
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'bpi_catalog_publisher') THEN
        GRANT USAGE ON SCHEMA bpi TO bpi_catalog_publisher;
        GRANT SELECT ON bpi.bpi_dataset_definitions TO bpi_catalog_publisher;
        GRANT SELECT ON bpi.bpi_dataset_snapshots TO bpi_catalog_publisher;
        GRANT SELECT ON bpi.bpi_dataset_materializations TO bpi_catalog_publisher;
        GRANT SELECT, UPDATE
            ON bpi.bpi_dataset_catalog_publications TO bpi_catalog_publisher;
        GRANT INSERT ON bpi.bpi_audit_events TO bpi_catalog_publisher;
    END IF;
END
$$;
