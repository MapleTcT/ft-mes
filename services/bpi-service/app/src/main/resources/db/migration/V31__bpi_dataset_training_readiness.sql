CREATE TABLE bpi.bpi_dataset_training_readiness_assessments (
    id uuid PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    mlflow_registration_id uuid NOT NULL,
    source_snapshot_id uuid NOT NULL,
    objective_code varchar(128) NOT NULL
        CHECK (objective_code = 'BATCH_START_BOUNDARY_REVIEW_RISK'),
    policy_version varchar(128) NOT NULL
        CHECK (policy_version = 'bpi-training-readiness/batch-start-boundary-v1'),
    assessment_sequence bigint NOT NULL CHECK (assessment_sequence > 0),
    state varchar(16) NOT NULL CHECK (state IN ('ELIGIBLE', 'BLOCKED')),
    revision bigint NOT NULL DEFAULT 1 CHECK (revision = 1),
    source_registration_revision bigint NOT NULL CHECK (source_registration_revision > 0),
    manifest_checksum varchar(64) NOT NULL CHECK (length(manifest_checksum) = 64),
    dataset_digest varchar(16) NOT NULL CHECK (length(dataset_digest) = 16),
    required_thresholds jsonb NOT NULL
        CHECK (jsonb_typeof(required_thresholds) = 'object'),
    observed_metrics jsonb NOT NULL
        CHECK (jsonb_typeof(observed_metrics) = 'object'),
    gate_results jsonb NOT NULL
        CHECK (jsonb_typeof(gate_results) = 'array'
            AND jsonb_array_length(gate_results) > 0),
    blocker_codes jsonb NOT NULL
        CHECK (jsonb_typeof(blocker_codes) = 'array'),
    phase_boundary jsonb NOT NULL
        CHECK (jsonb_typeof(phase_boundary) = 'object'
            AND phase_boundary @> '{
                "assessmentOnly": true,
                "trainingStarted": false,
                "modelCreated": false,
                "modelRegistered": false,
                "onlineInferenceEnabled": false,
                "productionActivationAllowed": false
            }'::jsonb),
    assessment_checksum varchar(64) NOT NULL
        CHECK (length(assessment_checksum) = 64),
    assessed_by varchar(128) NOT NULL,
    assessment_reason varchar(500) NOT NULL,
    assessed_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_bpi_training_readiness_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_bpi_training_readiness_sequence UNIQUE
        (tenant_id, mlflow_registration_id, objective_code,
         policy_version, assessment_sequence),
    CONSTRAINT fk_bpi_training_readiness_registration_tenant
        FOREIGN KEY (tenant_id, mlflow_registration_id)
        REFERENCES bpi.bpi_dataset_mlflow_registrations (tenant_id, id),
    CONSTRAINT fk_bpi_training_readiness_snapshot_tenant
        FOREIGN KEY (tenant_id, source_snapshot_id)
        REFERENCES bpi.bpi_dataset_snapshots (tenant_id, id),
    CONSTRAINT chk_bpi_training_readiness_state CHECK (
        (state = 'ELIGIBLE' AND jsonb_array_length(blocker_codes) = 0)
        OR
        (state = 'BLOCKED' AND jsonb_array_length(blocker_codes) > 0)
    )
);

CREATE INDEX idx_bpi_training_readiness_latest
    ON bpi.bpi_dataset_training_readiness_assessments
       (tenant_id, mlflow_registration_id, objective_code,
        policy_version, assessment_sequence DESC, id DESC);

CREATE OR REPLACE FUNCTION bpi.reject_dataset_training_readiness_mutation()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION 'BPI dataset training readiness assessments are immutable';
END
$$;

CREATE TRIGGER trg_bpi_dataset_training_readiness_immutable
    BEFORE UPDATE ON bpi.bpi_dataset_training_readiness_assessments
    FOR EACH ROW EXECUTE FUNCTION bpi.reject_dataset_training_readiness_mutation();

REVOKE EXECUTE ON ALL FUNCTIONS IN SCHEMA bpi FROM PUBLIC;
ALTER DEFAULT PRIVILEGES IN SCHEMA bpi
    REVOKE EXECUTE ON FUNCTIONS FROM PUBLIC;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'bpi_service') THEN
        GRANT SELECT, INSERT
            ON bpi.bpi_dataset_training_readiness_assessments TO bpi_service;
    END IF;
END
$$;
