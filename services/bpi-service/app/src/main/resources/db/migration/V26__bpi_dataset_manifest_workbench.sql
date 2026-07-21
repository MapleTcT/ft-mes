CREATE TABLE bpi.bpi_dataset_definitions (
    id uuid PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    dataset_code varchar(128) NOT NULL,
    version varchar(64) NOT NULL,
    name varchar(256) NOT NULL,
    plant_id varchar(64) NOT NULL,
    line_ids jsonb NOT NULL,
    state varchar(16) NOT NULL DEFAULT 'ACTIVE' CHECK (state = 'ACTIVE'),
    revision bigint NOT NULL DEFAULT 1 CHECK (revision = 1),
    prediction_time_policy varchar(64) NOT NULL
        CHECK (prediction_time_policy = 'AUTOMATIC_BATCH_START'),
    feature_cutoff_policy varchar(64) NOT NULL
        CHECK (feature_cutoff_policy = 'AT_OR_BEFORE_PREDICTION_TIME'),
    feature_refs jsonb NOT NULL,
    label_refs jsonb NOT NULL,
    max_label_delay_hours integer NOT NULL
        CHECK (max_label_delay_hours BETWEEN 1 AND 2160),
    minimum_confidence numeric(7,6) NOT NULL
        CHECK (minimum_confidence BETWEEN 0 AND 1),
    split_policy varchar(32) NOT NULL CHECK (split_policy = 'PRODUCTION_TIME'),
    checksum varchar(64) NOT NULL CHECK (length(checksum) = 64),
    created_by varchar(128) NOT NULL,
    create_reason varchar(500) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_bpi_dataset_definition_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_bpi_dataset_definition_version
        UNIQUE (tenant_id, dataset_code, version),
    CONSTRAINT chk_bpi_dataset_definition_lines
        CHECK (jsonb_typeof(line_ids) = 'array' AND jsonb_array_length(line_ids) > 0),
    CONSTRAINT chk_bpi_dataset_definition_features
        CHECK (jsonb_typeof(feature_refs) = 'array' AND jsonb_array_length(feature_refs) > 0),
    CONSTRAINT chk_bpi_dataset_definition_labels
        CHECK (jsonb_typeof(label_refs) = 'array' AND jsonb_array_length(label_refs) > 0)
);

CREATE INDEX idx_bpi_dataset_definition_scope
    ON bpi.bpi_dataset_definitions
       (tenant_id, plant_id, dataset_code, version, created_at DESC, id DESC);

CREATE TABLE bpi.bpi_dataset_snapshots (
    id uuid PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    dataset_id uuid NOT NULL,
    snapshot_version bigint NOT NULL CHECK (snapshot_version > 0),
    state varchar(24) NOT NULL
        CHECK (state IN ('QUEUED', 'BUILDING', 'MANIFEST_READY', 'FAILED')),
    revision bigint NOT NULL DEFAULT 1 CHECK (revision > 0),
    freeze_at timestamptz NOT NULL,
    line_ids jsonb NOT NULL,
    prediction_time_policy varchar(64) NOT NULL
        CHECK (prediction_time_policy = 'AUTOMATIC_BATCH_START'),
    rule_version_ids jsonb NOT NULL DEFAULT '[]'::jsonb,
    exclude_low_confidence boolean NOT NULL DEFAULT true,
    definition_checksum varchar(64) NOT NULL CHECK (length(definition_checksum) = 64),
    manifest_schema_version varchar(64) NOT NULL DEFAULT 'bpi.dataset-manifest.v1',
    manifest_checksum varchar(64),
    manifest jsonb,
    included_count integer,
    excluded_count integer,
    exclusion_summary jsonb,
    materialization_state varchar(24) NOT NULL DEFAULT 'NOT_STARTED'
        CHECK (materialization_state = 'NOT_STARTED'),
    artifact_uri text,
    requested_by varchar(128) NOT NULL,
    request_reason varchar(500) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    started_at timestamptz,
    completed_at timestamptz,
    claim_token uuid,
    claimed_at timestamptz,
    attempt_count integer NOT NULL DEFAULT 0 CHECK (attempt_count >= 0),
    failure_code varchar(128),
    failure_detail varchar(1000),
    CONSTRAINT uq_bpi_dataset_snapshot_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_bpi_dataset_snapshot_version
        UNIQUE (tenant_id, dataset_id, snapshot_version),
    CONSTRAINT fk_bpi_dataset_snapshot_definition_tenant
        FOREIGN KEY (tenant_id, dataset_id)
        REFERENCES bpi.bpi_dataset_definitions (tenant_id, id),
    CONSTRAINT chk_bpi_dataset_snapshot_lines
        CHECK (jsonb_typeof(line_ids) = 'array' AND jsonb_array_length(line_ids) > 0),
    CONSTRAINT chk_bpi_dataset_snapshot_rules
        CHECK (jsonb_typeof(rule_version_ids) = 'array'),
    CONSTRAINT chk_bpi_dataset_snapshot_lifecycle CHECK (
        (state = 'QUEUED'
            AND manifest_checksum IS NULL AND manifest IS NULL
            AND included_count IS NULL AND excluded_count IS NULL
            AND exclusion_summary IS NULL
            AND started_at IS NULL AND completed_at IS NULL
            AND claim_token IS NULL AND claimed_at IS NULL
            AND failure_code IS NULL AND failure_detail IS NULL)
        OR
        (state = 'BUILDING'
            AND manifest_checksum IS NULL AND manifest IS NULL
            AND included_count IS NULL AND excluded_count IS NULL
            AND exclusion_summary IS NULL
            AND started_at IS NOT NULL AND completed_at IS NULL
            AND claim_token IS NOT NULL AND claimed_at IS NOT NULL
            AND failure_code IS NULL AND failure_detail IS NULL)
        OR
        (state = 'MANIFEST_READY'
            AND manifest_checksum IS NOT NULL AND length(manifest_checksum) = 64
            AND manifest IS NOT NULL
            AND included_count IS NOT NULL AND included_count >= 0
            AND excluded_count IS NOT NULL AND excluded_count >= 0
            AND exclusion_summary IS NOT NULL
            AND started_at IS NOT NULL AND completed_at IS NOT NULL
            AND claim_token IS NULL AND claimed_at IS NULL
            AND failure_code IS NULL AND failure_detail IS NULL)
        OR
        (state = 'FAILED'
            AND manifest_checksum IS NULL AND manifest IS NULL
            AND included_count IS NULL AND excluded_count IS NULL
            AND exclusion_summary IS NULL
            AND started_at IS NOT NULL AND completed_at IS NOT NULL
            AND claim_token IS NULL AND claimed_at IS NULL
            AND failure_code IS NOT NULL AND failure_detail IS NOT NULL)
    ),
    CONSTRAINT chk_bpi_dataset_snapshot_materialization_boundary
        CHECK (artifact_uri IS NULL)
);

CREATE INDEX idx_bpi_dataset_snapshot_queue
    ON bpi.bpi_dataset_snapshots (state, created_at, id)
    WHERE state IN ('QUEUED', 'BUILDING');

CREATE INDEX idx_bpi_dataset_snapshot_history
    ON bpi.bpi_dataset_snapshots
       (tenant_id, dataset_id, snapshot_version DESC, created_at DESC, id DESC);

CREATE TABLE bpi.bpi_dataset_snapshot_samples (
    snapshot_id uuid NOT NULL,
    review_id uuid NOT NULL,
    tenant_id varchar(64) NOT NULL,
    shadow_run_id uuid NOT NULL,
    batch_id uuid NOT NULL,
    batch_no varchar(128) NOT NULL,
    line_id varchar(128) NOT NULL,
    included boolean NOT NULL,
    exclusion_reasons jsonb NOT NULL DEFAULT '[]'::jsonb,
    prediction_time timestamptz NOT NULL,
    feature_cutoff timestamptz NOT NULL,
    label_available_at timestamptz NOT NULL,
    confidence numeric(7,6) NOT NULL CHECK (confidence BETWEEN 0 AND 1),
    split_key varchar(32) NOT NULL,
    feature_payload jsonb NOT NULL,
    label_payload jsonb NOT NULL,
    source_payload jsonb NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (snapshot_id, review_id),
    CONSTRAINT fk_bpi_dataset_sample_snapshot_tenant
        FOREIGN KEY (tenant_id, snapshot_id)
        REFERENCES bpi.bpi_dataset_snapshots (tenant_id, id),
    CONSTRAINT fk_bpi_dataset_sample_review_tenant
        FOREIGN KEY (tenant_id, review_id)
        REFERENCES bpi.bpi_shadow_run_batch_reviews (tenant_id, id),
    CONSTRAINT fk_bpi_dataset_sample_run_tenant
        FOREIGN KEY (tenant_id, shadow_run_id)
        REFERENCES bpi.bpi_shadow_runs (tenant_id, id),
    CONSTRAINT fk_bpi_dataset_sample_batch_tenant
        FOREIGN KEY (tenant_id, batch_id)
        REFERENCES bpi.bpi_batch_instances (tenant_id, id),
    CONSTRAINT chk_bpi_dataset_sample_reasons
        CHECK (jsonb_typeof(exclusion_reasons) = 'array'),
    CONSTRAINT chk_bpi_dataset_sample_payloads
        CHECK (jsonb_typeof(feature_payload) = 'object'
            AND jsonb_typeof(label_payload) = 'object'
            AND jsonb_typeof(source_payload) = 'object'),
    CONSTRAINT chk_bpi_dataset_sample_inclusion CHECK (
        (included AND jsonb_array_length(exclusion_reasons) = 0)
        OR (NOT included AND jsonb_array_length(exclusion_reasons) > 0)
    ),
    CONSTRAINT chk_bpi_dataset_sample_time_point
        CHECK (feature_cutoff = prediction_time)
);

CREATE INDEX idx_bpi_dataset_sample_manifest
    ON bpi.bpi_dataset_snapshot_samples
       (tenant_id, snapshot_id, included, prediction_time, review_id);

CREATE OR REPLACE FUNCTION bpi.reject_dataset_definition_mutation()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION 'BPI dataset definitions are immutable; create a new version';
END
$$;

CREATE TRIGGER trg_bpi_dataset_definition_immutable
    BEFORE UPDATE ON bpi.bpi_dataset_definitions
    FOR EACH ROW EXECUTE FUNCTION bpi.reject_dataset_definition_mutation();

CREATE OR REPLACE FUNCTION bpi.guard_dataset_snapshot_transition()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF OLD.state IN ('MANIFEST_READY', 'FAILED') THEN
        RAISE EXCEPTION 'Terminal BPI dataset snapshots are immutable';
    END IF;
    IF NOT (
        (OLD.state = 'QUEUED' AND NEW.state = 'BUILDING')
        OR (OLD.state = 'BUILDING' AND NEW.state IN ('QUEUED', 'MANIFEST_READY', 'FAILED'))
    ) THEN
        RAISE EXCEPTION 'Invalid BPI dataset snapshot transition: % -> %', OLD.state, NEW.state;
    END IF;
    RETURN NEW;
END
$$;

CREATE TRIGGER trg_bpi_dataset_snapshot_transition
    BEFORE UPDATE ON bpi.bpi_dataset_snapshots
    FOR EACH ROW EXECUTE FUNCTION bpi.guard_dataset_snapshot_transition();

CREATE OR REPLACE FUNCTION bpi.reject_dataset_sample_mutation()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION 'BPI dataset snapshot samples are immutable';
END
$$;

CREATE TRIGGER trg_bpi_dataset_sample_immutable
    BEFORE UPDATE ON bpi.bpi_dataset_snapshot_samples
    FOR EACH ROW EXECUTE FUNCTION bpi.reject_dataset_sample_mutation();

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'bpi_service') THEN
        GRANT SELECT, INSERT ON bpi.bpi_dataset_definitions TO bpi_service;
        GRANT SELECT, INSERT, UPDATE ON bpi.bpi_dataset_snapshots TO bpi_service;
        GRANT SELECT, INSERT ON bpi.bpi_dataset_snapshot_samples TO bpi_service;
    END IF;
END
$$;
