ALTER TABLE bpi.bpi_dataset_definitions
    ADD COLUMN process_signal_windows jsonb NOT NULL DEFAULT '[]'::jsonb,
    ADD CONSTRAINT chk_bpi_dataset_definition_process_windows
        CHECK (jsonb_typeof(process_signal_windows) = 'array'
            AND jsonb_array_length(process_signal_windows) <= 20);

CREATE TABLE bpi.bpi_dataset_process_signal_window_facts (
    snapshot_id uuid NOT NULL,
    review_id uuid NOT NULL,
    feature_ref varchar(128) NOT NULL,
    tenant_id varchar(64) NOT NULL,
    shadow_run_id uuid NOT NULL,
    batch_id uuid NOT NULL,
    batch_no varchar(128) NOT NULL,
    plant_id varchar(64) NOT NULL,
    line_id varchar(128) NOT NULL,
    rule_version_id uuid NOT NULL,
    topology_version_id uuid NOT NULL,
    point_catalog_snapshot_id uuid NOT NULL,
    signal varchar(128) NOT NULL,
    value_type varchar(16) NOT NULL
        CHECK (value_type IN ('NUMERIC', 'BOOLEAN')),
    metric varchar(16) NOT NULL
        CHECK (metric IN ('MEAN', 'MIN', 'MAX', 'LAST', 'DELTA', 'SLOPE', 'TRUE_RATIO')),
    start_offset_seconds integer NOT NULL
        CHECK (start_offset_seconds BETWEEN -3600 AND -1),
    end_offset_seconds integer NOT NULL CHECK (end_offset_seconds <= 0),
    minimum_samples integer NOT NULL CHECK (minimum_samples BETWEEN 2 AND 900),
    maximum_gap_seconds integer NOT NULL CHECK (maximum_gap_seconds BETWEEN 1 AND 600),
    expected_unit varchar(32) NOT NULL,
    require_calibration boolean NOT NULL,
    accepted_quality_codes jsonb NOT NULL,
    window_definition_checksum char(64) NOT NULL,
    prediction_time timestamptz NOT NULL,
    window_start timestamptz NOT NULL,
    window_end timestamptz NOT NULL,
    product_id varchar(128),
    device_id varchar(128),
    property_id varchar(128),
    binding_calibration_version varchar(128),
    point_catalog_calibration_version varchar(128),
    point_catalog_device_state varchar(16),
    point_catalog_registered boolean,
    point_catalog_property_present boolean,
    point_catalog_calibration_status varchar(16),
    source_point_count integer NOT NULL CHECK (source_point_count >= 0),
    accepted_sample_count integer NOT NULL CHECK (accepted_sample_count >= 0),
    rejected_quality_count integer NOT NULL CHECK (rejected_quality_count >= 0),
    late_availability_count integer NOT NULL CHECK (late_availability_count >= 0),
    unit_mismatch_count integer NOT NULL CHECK (unit_mismatch_count >= 0),
    value_type_mismatch_count integer NOT NULL CHECK (value_type_mismatch_count >= 0),
    calibration_mismatch_count integer NOT NULL CHECK (calibration_mismatch_count >= 0),
    first_sample_time timestamptz,
    last_sample_time timestamptz,
    latest_ingest_time timestamptz,
    maximum_observed_gap_seconds numeric(18,6),
    numeric_value numeric,
    source_fingerprint char(32) NOT NULL,
    state varchar(16) NOT NULL CHECK (state IN ('READY', 'BLOCKED')),
    blocker_codes jsonb NOT NULL,
    fact_checksum char(64) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (snapshot_id, review_id, feature_ref),
    CONSTRAINT fk_bpi_dataset_window_sample
        FOREIGN KEY (snapshot_id, review_id)
        REFERENCES bpi.bpi_dataset_snapshot_samples (snapshot_id, review_id),
    CONSTRAINT fk_bpi_dataset_window_snapshot_tenant
        FOREIGN KEY (tenant_id, snapshot_id)
        REFERENCES bpi.bpi_dataset_snapshots (tenant_id, id),
    CONSTRAINT fk_bpi_dataset_window_review_tenant
        FOREIGN KEY (tenant_id, review_id)
        REFERENCES bpi.bpi_shadow_run_batch_reviews (tenant_id, id),
    CONSTRAINT fk_bpi_dataset_window_run_tenant
        FOREIGN KEY (tenant_id, shadow_run_id)
        REFERENCES bpi.bpi_shadow_runs (tenant_id, id),
    CONSTRAINT fk_bpi_dataset_window_batch_tenant
        FOREIGN KEY (tenant_id, batch_id)
        REFERENCES bpi.bpi_batch_instances (tenant_id, id),
    CONSTRAINT chk_bpi_dataset_window_offsets
        CHECK (end_offset_seconds > start_offset_seconds),
    CONSTRAINT chk_bpi_dataset_window_times
        CHECK (window_start < window_end
            AND window_end <= prediction_time
            AND (first_sample_time IS NULL OR first_sample_time >= window_start)
            AND (last_sample_time IS NULL OR last_sample_time <= window_end)
            AND (latest_ingest_time IS NULL OR latest_ingest_time <= prediction_time)),
    CONSTRAINT chk_bpi_dataset_window_quality_codes
        CHECK (jsonb_typeof(accepted_quality_codes) = 'array'
            AND jsonb_array_length(accepted_quality_codes) > 0),
    CONSTRAINT chk_bpi_dataset_window_blockers
        CHECK (jsonb_typeof(blocker_codes) = 'array'),
    CONSTRAINT chk_bpi_dataset_window_state CHECK (
        (state = 'READY'
            AND jsonb_array_length(blocker_codes) = 0
            AND numeric_value IS NOT NULL
            AND accepted_sample_count >= minimum_samples
            AND maximum_observed_gap_seconds <= maximum_gap_seconds)
        OR
        (state = 'BLOCKED' AND jsonb_array_length(blocker_codes) > 0)
    )
);

CREATE INDEX idx_bpi_dataset_process_window_snapshot
    ON bpi.bpi_dataset_process_signal_window_facts
       (tenant_id, snapshot_id, state, review_id, feature_ref);

CREATE INDEX idx_bpi_dataset_process_window_line_time
    ON bpi.bpi_dataset_process_signal_window_facts
       (tenant_id, plant_id, line_id, prediction_time, feature_ref);

CREATE OR REPLACE FUNCTION bpi.reject_dataset_process_signal_window_mutation()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION 'BPI dataset process signal window facts are immutable';
END
$$;

CREATE TRIGGER trg_bpi_dataset_process_signal_window_immutable
    BEFORE UPDATE ON bpi.bpi_dataset_process_signal_window_facts
    FOR EACH ROW EXECUTE FUNCTION bpi.reject_dataset_process_signal_window_mutation();

ALTER TABLE bpi.bpi_dataset_training_readiness_assessments
    DROP CONSTRAINT bpi_dataset_training_readiness_assessments_policy_version_check,
    ADD CONSTRAINT chk_bpi_training_readiness_policy_version
        CHECK (policy_version IN (
            'bpi-training-readiness/batch-start-boundary-v1',
            'bpi-training-readiness/batch-start-boundary-v2'
        ));

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'bpi_service') THEN
        GRANT SELECT, INSERT
            ON bpi.bpi_dataset_process_signal_window_facts TO bpi_service;
    END IF;
END
$$;
