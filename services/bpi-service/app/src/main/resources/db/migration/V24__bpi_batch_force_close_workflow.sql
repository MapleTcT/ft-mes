CREATE TABLE bpi.bpi_batch_force_close_tasks (
    id uuid PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    batch_id uuid NOT NULL,
    state varchar(24) NOT NULL CHECK (state IN ('PENDING_APPROVAL', 'COMPLETED')),
    revision bigint NOT NULL DEFAULT 1 CHECK (revision > 0),
    source_state varchar(32) NOT NULL CHECK (source_state IN ('ACTIVE', 'SUSPENDED')),
    boundary_time timestamptz NOT NULL,
    requested_by varchar(128) NOT NULL,
    requested_at timestamptz NOT NULL DEFAULT now(),
    request_reason varchar(500) NOT NULL,
    request_comment varchar(2000),
    decided_by varchar(128),
    decided_at timestamptz,
    decision_reason varchar(500),
    decision_comment varchar(2000),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_bpi_batch_force_close_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_bpi_batch_force_close_batch_tenant
        FOREIGN KEY (tenant_id, batch_id)
        REFERENCES bpi.bpi_batch_instances (tenant_id, id),
    CONSTRAINT chk_bpi_batch_force_close_decision
        CHECK (
            (state = 'PENDING_APPROVAL' AND decided_by IS NULL AND decided_at IS NULL)
            OR
            (state = 'COMPLETED' AND decided_by IS NOT NULL AND decided_at IS NOT NULL)
        )
);

CREATE UNIQUE INDEX uq_bpi_batch_force_close_pending
    ON bpi.bpi_batch_force_close_tasks (tenant_id, batch_id)
    WHERE state = 'PENDING_APPROVAL';

CREATE INDEX idx_bpi_batch_force_close_history
    ON bpi.bpi_batch_force_close_tasks
        (tenant_id, batch_id, requested_at DESC, id);

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'bpi_service') THEN
        GRANT SELECT, INSERT, UPDATE, DELETE ON bpi.bpi_batch_force_close_tasks TO bpi_service;
    END IF;
END
$$;
