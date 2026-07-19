ALTER TABLE bpi.bpi_point_catalog_snapshots
    RENAME COLUMN ready_point_count TO source_claim_ready_point_count;

COMMENT ON COLUMN bpi.bpi_point_catalog_snapshots.source_claim_ready_point_count IS
    'Import-time count based on source declarations. Operational readiness is derived from approved BPI calibration evidence.';

CREATE TABLE bpi.bpi_point_calibrations (
    id uuid PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    plant_id varchar(64) NOT NULL,
    line_id varchar(128) NOT NULL,
    product_id varchar(128) NOT NULL,
    device_id varchar(128) NOT NULL,
    property_id varchar(128) NOT NULL,
    calibration_version varchar(128) NOT NULL,
    certificate_reference varchar(512) NOT NULL,
    certificate_checksum char(64) NOT NULL
        CHECK (certificate_checksum ~ '^[a-f0-9]{64}$'),
    valid_from timestamptz NOT NULL,
    valid_until timestamptz NOT NULL,
    state varchar(16) NOT NULL CHECK (state IN ('PENDING', 'APPROVED', 'REJECTED', 'REVOKED')),
    revision bigint NOT NULL DEFAULT 1 CHECK (revision > 0),
    submitted_by varchar(128) NOT NULL,
    submitted_at timestamptz NOT NULL DEFAULT now(),
    submit_reason varchar(500) NOT NULL,
    decided_by varchar(128),
    decided_at timestamptz,
    decision_reason varchar(500),
    revoked_by varchar(128),
    revoked_at timestamptz,
    revoke_reason varchar(500),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_bpi_point_calibration_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_bpi_point_calibration_version UNIQUE
        (tenant_id, plant_id, line_id, product_id, device_id, property_id, calibration_version),
    CONSTRAINT chk_bpi_point_calibration_validity CHECK (valid_until > valid_from),
    CONSTRAINT chk_bpi_point_calibration_decision CHECK (
        (state = 'PENDING'
            AND decided_by IS NULL AND decided_at IS NULL AND decision_reason IS NULL
            AND revoked_by IS NULL AND revoked_at IS NULL AND revoke_reason IS NULL)
        OR
        (state IN ('APPROVED', 'REJECTED')
            AND decided_by IS NOT NULL AND decided_at IS NOT NULL AND decision_reason IS NOT NULL
            AND revoked_by IS NULL AND revoked_at IS NULL AND revoke_reason IS NULL)
        OR
        (state = 'REVOKED'
            AND decided_by IS NOT NULL AND decided_at IS NOT NULL AND decision_reason IS NOT NULL
            AND revoked_by IS NOT NULL AND revoked_at IS NOT NULL AND revoke_reason IS NOT NULL)
    )
);

CREATE INDEX idx_bpi_point_calibration_scope
    ON bpi.bpi_point_calibrations
        (tenant_id, plant_id, line_id, product_id, device_id, property_id, submitted_at DESC);

CREATE INDEX idx_bpi_point_calibration_effective
    ON bpi.bpi_point_calibrations
        (tenant_id, plant_id, line_id, product_id, device_id, property_id,
         calibration_version, valid_from, valid_until)
    WHERE state = 'APPROVED';

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'bpi_service') THEN
        GRANT SELECT, INSERT, UPDATE ON bpi.bpi_point_calibrations TO bpi_service;
        GRANT UPDATE (source_claim_ready_point_count)
            ON bpi.bpi_point_catalog_snapshots TO bpi_service;
    END IF;
END
$$;
