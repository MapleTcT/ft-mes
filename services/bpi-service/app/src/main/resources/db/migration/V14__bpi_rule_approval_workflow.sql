ALTER TABLE bpi.bpi_rule_versions
    DROP CONSTRAINT IF EXISTS bpi_rule_versions_state_check;

ALTER TABLE bpi.bpi_rule_versions
    ADD CONSTRAINT bpi_rule_versions_state_check
        CHECK (state IN (
            'DRAFT',
            'SIMULATION_PASSED',
            'PENDING_APPROVAL',
            'PUBLISHED',
            'RETIRED'
        ));

CREATE TABLE bpi.bpi_rule_approval_requests (
    id uuid PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    rule_version_id uuid NOT NULL,
    simulation_id uuid NOT NULL,
    simulation_checksum char(64) NOT NULL,
    state varchar(16) NOT NULL CHECK (state IN ('PENDING', 'APPROVED', 'REJECTED')),
    revision bigint NOT NULL DEFAULT 1 CHECK (revision > 0),
    submitted_by varchar(128) NOT NULL,
    submitted_at timestamptz NOT NULL DEFAULT now(),
    submit_reason varchar(500) NOT NULL,
    decided_by varchar(128),
    decided_at timestamptz,
    decision_reason varchar(500),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_bpi_rule_approval_rule_tenant
        FOREIGN KEY (tenant_id, rule_version_id)
        REFERENCES bpi.bpi_rule_versions (tenant_id, id),
    CONSTRAINT fk_bpi_rule_approval_simulation_tenant
        FOREIGN KEY (tenant_id, simulation_id)
        REFERENCES bpi.bpi_rule_simulations (tenant_id, id),
    CONSTRAINT chk_bpi_rule_approval_decision
        CHECK (
            (state = 'PENDING' AND decided_by IS NULL AND decided_at IS NULL)
            OR
            (state IN ('APPROVED', 'REJECTED') AND decided_by IS NOT NULL AND decided_at IS NOT NULL)
        )
);

CREATE UNIQUE INDEX uq_bpi_rule_approval_pending
    ON bpi.bpi_rule_approval_requests (tenant_id, rule_version_id)
    WHERE state = 'PENDING';

CREATE INDEX idx_bpi_rule_approval_history
    ON bpi.bpi_rule_approval_requests
        (tenant_id, rule_version_id, submitted_at DESC, id);

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'bpi_service') THEN
        GRANT SELECT, INSERT, UPDATE, DELETE ON bpi.bpi_rule_approval_requests TO bpi_service;
    END IF;
END
$$;
