CREATE TABLE bpi.bpi_dataset_materializations (
    id uuid PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    snapshot_id uuid NOT NULL,
    artifact_format varchar(16) NOT NULL CHECK (artifact_format = 'PARQUET'),
    artifact_schema_version varchar(64) NOT NULL,
    materializer_version varchar(128) NOT NULL,
    state varchar(24) NOT NULL
        CHECK (state IN ('QUEUED', 'WRITING', 'READY', 'FAILED')),
    revision bigint NOT NULL DEFAULT 1 CHECK (revision > 0),
    manifest_checksum varchar(64) NOT NULL CHECK (length(manifest_checksum) = 64),
    requested_by varchar(128) NOT NULL,
    request_reason varchar(500) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    started_at timestamptz,
    completed_at timestamptz,
    claim_token uuid,
    claimed_at timestamptz,
    attempt_count integer NOT NULL DEFAULT 0 CHECK (attempt_count >= 0),
    artifact_uri text,
    object_bucket varchar(128),
    object_key text,
    content_sha256 varchar(64),
    byte_size bigint,
    row_count bigint,
    schema_json jsonb,
    artifact_metadata jsonb,
    failure_code varchar(128),
    failure_detail varchar(1000),
    CONSTRAINT uq_bpi_dataset_materialization_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_bpi_dataset_materialization_contract UNIQUE
        (tenant_id, snapshot_id, artifact_format,
         artifact_schema_version, materializer_version),
    CONSTRAINT fk_bpi_dataset_materialization_snapshot_tenant
        FOREIGN KEY (tenant_id, snapshot_id)
        REFERENCES bpi.bpi_dataset_snapshots (tenant_id, id),
    CONSTRAINT chk_bpi_dataset_materialization_lifecycle CHECK (
        (state = 'QUEUED'
            AND started_at IS NULL AND completed_at IS NULL
            AND claim_token IS NULL AND claimed_at IS NULL
            AND artifact_uri IS NULL AND object_bucket IS NULL AND object_key IS NULL
            AND content_sha256 IS NULL AND byte_size IS NULL AND row_count IS NULL
            AND schema_json IS NULL AND artifact_metadata IS NULL
            AND failure_code IS NULL AND failure_detail IS NULL)
        OR
        (state = 'WRITING'
            AND started_at IS NOT NULL AND completed_at IS NULL
            AND claim_token IS NOT NULL AND claimed_at IS NOT NULL
            AND artifact_uri IS NULL AND object_bucket IS NULL AND object_key IS NULL
            AND content_sha256 IS NULL AND byte_size IS NULL AND row_count IS NULL
            AND schema_json IS NULL AND artifact_metadata IS NULL
            AND failure_code IS NULL AND failure_detail IS NULL)
        OR
        (state = 'READY'
            AND started_at IS NOT NULL AND completed_at IS NOT NULL
            AND claim_token IS NULL AND claimed_at IS NULL
            AND artifact_uri IS NOT NULL AND object_bucket IS NOT NULL AND object_key IS NOT NULL
            AND starts_with(
                    artifact_uri,
                    's3://' || object_bucket || '/' || object_key || '?versionId=')
            AND content_sha256 IS NOT NULL AND length(content_sha256) = 64
            AND byte_size IS NOT NULL AND byte_size > 0
            AND row_count IS NOT NULL AND row_count >= 0
            AND schema_json IS NOT NULL AND jsonb_typeof(schema_json) = 'object'
            AND artifact_metadata IS NOT NULL AND jsonb_typeof(artifact_metadata) = 'object'
            AND jsonb_typeof(artifact_metadata -> 'objectVersionId') = 'string'
            AND length(artifact_metadata ->> 'objectVersionId') > 0
            AND artifact_metadata @> '{"objectContentVerified": true}'::jsonb
            AND failure_code IS NULL AND failure_detail IS NULL)
        OR
        (state = 'FAILED'
            AND started_at IS NOT NULL AND completed_at IS NOT NULL
            AND claim_token IS NULL AND claimed_at IS NULL
            AND artifact_uri IS NULL AND object_bucket IS NULL AND object_key IS NULL
            AND content_sha256 IS NULL AND byte_size IS NULL AND row_count IS NULL
            AND schema_json IS NULL AND artifact_metadata IS NULL
            AND failure_code IS NOT NULL AND failure_detail IS NOT NULL)
    )
);

CREATE INDEX idx_bpi_dataset_materialization_queue
    ON bpi.bpi_dataset_materializations (state, created_at, id)
    WHERE state IN ('QUEUED', 'WRITING');

CREATE INDEX idx_bpi_dataset_materialization_snapshot
    ON bpi.bpi_dataset_materializations
       (tenant_id, snapshot_id, created_at DESC, id DESC);

CREATE OR REPLACE FUNCTION bpi.guard_dataset_materialization_transition()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF OLD.state = 'READY' THEN
        RAISE EXCEPTION 'READY BPI dataset materializations are immutable';
    END IF;
    IF NOT (
        (OLD.state = 'QUEUED' AND NEW.state = 'WRITING')
        OR (OLD.state = 'WRITING' AND NEW.state IN ('QUEUED', 'READY', 'FAILED'))
        OR (OLD.state = 'FAILED' AND NEW.state = 'QUEUED')
    ) THEN
        RAISE EXCEPTION 'Invalid BPI dataset materialization transition: % -> %',
            OLD.state, NEW.state;
    END IF;
    IF NEW.tenant_id <> OLD.tenant_id
        OR NEW.snapshot_id <> OLD.snapshot_id
        OR NEW.artifact_format <> OLD.artifact_format
        OR NEW.artifact_schema_version <> OLD.artifact_schema_version
        OR NEW.materializer_version <> OLD.materializer_version
        OR NEW.manifest_checksum <> OLD.manifest_checksum
        OR NEW.requested_by <> OLD.requested_by
        OR NEW.request_reason <> OLD.request_reason
        OR NEW.created_at <> OLD.created_at THEN
        RAISE EXCEPTION 'BPI dataset materialization identity is immutable';
    END IF;
    RETURN NEW;
END
$$;

CREATE TRIGGER trg_bpi_dataset_materialization_transition
    BEFORE UPDATE ON bpi.bpi_dataset_materializations
    FOR EACH ROW EXECUTE FUNCTION bpi.guard_dataset_materialization_transition();

REVOKE EXECUTE ON ALL FUNCTIONS IN SCHEMA bpi FROM PUBLIC;
ALTER DEFAULT PRIVILEGES IN SCHEMA bpi
    REVOKE EXECUTE ON FUNCTIONS FROM PUBLIC;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'bpi_service') THEN
        GRANT SELECT, INSERT, UPDATE ON bpi.bpi_dataset_materializations TO bpi_service;
    END IF;
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'bpi_materializer') THEN
        GRANT USAGE ON SCHEMA bpi TO bpi_materializer;
        GRANT SELECT ON bpi.bpi_dataset_definitions TO bpi_materializer;
        GRANT SELECT ON bpi.bpi_dataset_snapshots TO bpi_materializer;
        GRANT SELECT ON bpi.bpi_dataset_snapshot_samples TO bpi_materializer;
        GRANT SELECT, UPDATE ON bpi.bpi_dataset_materializations TO bpi_materializer;
        GRANT INSERT ON bpi.bpi_audit_events TO bpi_materializer;
    END IF;
END
$$;
