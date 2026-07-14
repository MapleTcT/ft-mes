ALTER TABLE bpi.bpi_topology_versions
    ADD COLUMN validation_status varchar(24) NOT NULL DEFAULT 'NOT_VALIDATED',
    ADD COLUMN validation_errors jsonb NOT NULL DEFAULT '[]'::jsonb,
    ADD COLUMN validation_warnings jsonb NOT NULL DEFAULT '[]'::jsonb,
    ADD COLUMN validated_checksum char(64),
    ADD COLUMN validated_by varchar(128),
    ADD COLUMN validated_at timestamptz,
    ADD COLUMN published_by varchar(128),
    ADD COLUMN published_at timestamptz;

ALTER TABLE bpi.bpi_topology_versions
    ADD CONSTRAINT chk_bpi_topology_validation_status
        CHECK (validation_status IN ('NOT_VALIDATED', 'PASSED', 'FAILED'));

CREATE INDEX idx_bpi_topology_draft_scope
    ON bpi.bpi_topology_versions (tenant_id, plant_id, line_id, state, updated_at DESC)
    WHERE state = 'DRAFT';
