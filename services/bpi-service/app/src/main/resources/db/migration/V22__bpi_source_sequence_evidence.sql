ALTER TABLE bpi.bpi_point_catalog_entries
    ADD COLUMN source_sequence_required boolean NOT NULL DEFAULT false,
    ADD COLUMN source_sequence_origin varchar(16),
    ADD COLUMN source_sequence_binding_fingerprint varchar(71),
    ADD CONSTRAINT ck_bpi_point_catalog_sequence_origin
        CHECK (source_sequence_origin IS NULL OR source_sequence_origin IN ('DEVICE', 'GATEWAY')),
    ADD CONSTRAINT ck_bpi_point_catalog_sequence_fingerprint
        CHECK (
            source_sequence_binding_fingerprint IS NULL
            OR source_sequence_binding_fingerprint ~ '^sha256:[0-9a-f]{64}$'
        );

UPDATE bpi.bpi_point_catalog_entries
   SET source_sequence_required = source_sequence_enabled
 WHERE source_sequence_enabled;

CREATE TABLE bpi.bpi_source_sequence_evidence_current (
    id uuid PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    source varchar(64) NOT NULL,
    source_instance varchar(128) NOT NULL,
    plant_id varchar(64) NOT NULL,
    line_id varchar(128) NOT NULL,
    product_id varchar(128) NOT NULL,
    device_id varchar(128) NOT NULL,
    binding_fingerprint varchar(71) NOT NULL
        CHECK (binding_fingerprint ~ '^sha256:[0-9a-f]{64}$'),
    status varchar(16) NOT NULL
        CHECK (status IN ('DISABLED', 'MISSING', 'PENDING', 'QUALIFIED', 'EXPIRED')),
    sequence_origin varchar(16),
    source_epoch bigint,
    first_sequence bigint,
    last_sequence bigint,
    observation_count integer,
    first_observed_at timestamptz,
    last_observed_at timestamptz,
    valid_until timestamptz,
    source_event_id varchar(128) NOT NULL,
    observed_at timestamptz NOT NULL,
    payload_checksum char(64) NOT NULL,
    revision bigint NOT NULL CHECK (revision > 0),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_bpi_source_sequence_evidence_identity
        UNIQUE (
            tenant_id, source, source_instance, plant_id, line_id,
            product_id, device_id, binding_fingerprint
        ),
    CONSTRAINT ck_bpi_source_sequence_evidence_shape
        CHECK (
            (
                status IN ('DISABLED', 'MISSING')
                AND sequence_origin IS NULL
                AND source_epoch IS NULL
                AND first_sequence IS NULL
                AND last_sequence IS NULL
                AND observation_count IS NULL
                AND first_observed_at IS NULL
                AND last_observed_at IS NULL
                AND valid_until IS NULL
            )
            OR
            (
                status = 'PENDING'
                AND sequence_origin IS NOT NULL
                AND sequence_origin IN ('DEVICE', 'GATEWAY')
                AND source_epoch IS NOT NULL
                AND source_epoch > 0
                AND first_sequence IS NOT NULL
                AND first_sequence > 0
                AND last_sequence IS NOT NULL
                AND last_sequence >= first_sequence
                AND observation_count IS NOT NULL
                AND observation_count >= 1
                AND first_observed_at IS NOT NULL
                AND last_observed_at IS NOT NULL
                AND last_observed_at >= first_observed_at
                AND valid_until IS NOT NULL
                AND valid_until > last_observed_at
            )
            OR
            (
                status IN ('QUALIFIED', 'EXPIRED')
                AND sequence_origin IS NOT NULL
                AND sequence_origin IN ('DEVICE', 'GATEWAY')
                AND source_epoch IS NOT NULL
                AND source_epoch > 0
                AND first_sequence IS NOT NULL
                AND first_sequence > 0
                AND last_sequence IS NOT NULL
                AND last_sequence > first_sequence
                AND observation_count IS NOT NULL
                AND observation_count >= 2
                AND first_observed_at IS NOT NULL
                AND last_observed_at IS NOT NULL
                AND last_observed_at >= first_observed_at
                AND valid_until IS NOT NULL
                AND valid_until > last_observed_at
            )
        )
);

CREATE INDEX idx_bpi_source_sequence_evidence_scope
    ON bpi.bpi_source_sequence_evidence_current
        (tenant_id, plant_id, line_id, product_id, device_id);

CREATE INDEX idx_bpi_source_sequence_evidence_expiry
    ON bpi.bpi_source_sequence_evidence_current
        (tenant_id, status, valid_until)
    WHERE status IN ('QUALIFIED', 'EXPIRED');

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'bpi_service') THEN
        GRANT SELECT, INSERT, UPDATE ON
            bpi.bpi_source_sequence_evidence_current
        TO bpi_service;
    END IF;
END
$$;
