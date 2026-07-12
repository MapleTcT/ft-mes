ALTER TABLE bpi.bpi_topology_versions
    ADD COLUMN plant_id varchar(64),
    ADD COLUMN line_id varchar(128),
    ADD COLUMN revision bigint NOT NULL DEFAULT 1 CHECK (revision > 0),
    ADD COLUMN updated_by varchar(128),
    ADD COLUMN updated_at timestamptz NOT NULL DEFAULT now();

ALTER TABLE bpi.bpi_rule_versions
    ADD COLUMN plant_id varchar(64),
    ADD COLUMN line_id varchar(128),
    ADD COLUMN updated_by varchar(128),
    ADD COLUMN updated_at timestamptz NOT NULL DEFAULT now();

CREATE INDEX idx_bpi_topology_scope
    ON bpi.bpi_topology_versions (tenant_id, plant_id, line_id, state, created_at DESC);

CREATE INDEX idx_bpi_rule_scope
    ON bpi.bpi_rule_versions (tenant_id, plant_id, line_id, state, created_at DESC);

CREATE TABLE bpi.bpi_rule_golden_boundaries (
    id uuid PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    plant_id varchar(64) NOT NULL,
    line_id varchar(128) NOT NULL,
    golden_set_id varchar(128) NOT NULL,
    boundary_type varchar(16) NOT NULL CHECK (boundary_type IN ('START', 'END')),
    boundary_time timestamptz NOT NULL,
    tolerance_seconds integer NOT NULL CHECK (tolerance_seconds BETWEEN 1 AND 3600),
    source_ref varchar(256) NOT NULL,
    created_by varchar(128) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (tenant_id, golden_set_id, boundary_type, boundary_time)
);

CREATE INDEX idx_bpi_golden_scope_time
    ON bpi.bpi_rule_golden_boundaries
        (tenant_id, plant_id, line_id, golden_set_id, boundary_type, boundary_time);

CREATE TABLE bpi.bpi_rule_simulations (
    id uuid PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    plant_id varchar(64) NOT NULL,
    line_id varchar(128) NOT NULL,
    rule_version_id uuid NOT NULL,
    state varchar(16) NOT NULL CHECK (state IN ('PASSED', 'FAILED')),
    checksum char(64) NOT NULL,
    input_manifest jsonb NOT NULL,
    metrics jsonb NOT NULL,
    emitted_boundaries jsonb NOT NULL,
    failure_reason varchar(500),
    created_by varchar(128) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_bpi_simulation_rule_tenant
        FOREIGN KEY (tenant_id, rule_version_id)
        REFERENCES bpi.bpi_rule_versions (tenant_id, id),
    UNIQUE (tenant_id, id)
);

CREATE INDEX idx_bpi_simulation_rule_time
    ON bpi.bpi_rule_simulations (tenant_id, rule_version_id, created_at DESC);

ALTER TABLE bpi.bpi_rule_versions
    ADD COLUMN latest_simulation_id uuid,
    ADD CONSTRAINT fk_bpi_rule_latest_simulation_tenant
        FOREIGN KEY (tenant_id, latest_simulation_id)
        REFERENCES bpi.bpi_rule_simulations (tenant_id, id);

INSERT INTO bpi.bpi_feature_flags
    (id, tenant_id, scope_type, scope_key, flag_key, enabled, revision, updated_by)
VALUES
    ('00000000-0000-0000-0000-000000000004', '*', 'GLOBAL', '*',
     'bpi.rule-management', false, 1, 'flyway')
ON CONFLICT (tenant_id, scope_type, scope_key, flag_key) DO NOTHING;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'bpi_service') THEN
        GRANT SELECT, INSERT, UPDATE, DELETE ON
            bpi.bpi_rule_golden_boundaries,
            bpi.bpi_rule_simulations
        TO bpi_service;
    END IF;
END
$$;
