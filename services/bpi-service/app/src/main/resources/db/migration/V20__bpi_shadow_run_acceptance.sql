CREATE TABLE bpi.bpi_shadow_runs (
    id uuid PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    run_code varchar(128) NOT NULL,
    name varchar(256) NOT NULL,
    plant_id varchar(64) NOT NULL,
    line_id varchar(128) NOT NULL,
    state varchar(24) NOT NULL
        CHECK (state IN ('DRAFT', 'RUNNING', 'EVALUATING', 'APPROVED', 'REJECTED', 'CANCELLED')),
    revision bigint NOT NULL CHECK (revision > 0),
    rule_version_id uuid NOT NULL,
    topology_version_id uuid NOT NULL,
    point_catalog_snapshot_id uuid NOT NULL,
    minimum_duration_days smallint NOT NULL
        CHECK (minimum_duration_days BETWEEN 7 AND 14),
    minimum_reviewed_batches integer NOT NULL
        CHECK (minimum_reviewed_batches BETWEEN 10 AND 10000),
    boundary_tolerance_seconds integer NOT NULL
        CHECK (boundary_tolerance_seconds BETWEEN 0 AND 3600),
    minimum_boundary_agreement numeric(7,6) NOT NULL
        CHECK (minimum_boundary_agreement BETWEEN 0.950000 AND 1.000000),
    quantity_tolerance_percent numeric(9,6) NOT NULL
        CHECK (quantity_tolerance_percent > 0 AND quantity_tolerance_percent <= 100),
    created_by varchar(128) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_by varchar(128) NOT NULL,
    updated_at timestamptz NOT NULL DEFAULT now(),
    started_by varchar(128),
    started_at timestamptz,
    completed_by varchar(128),
    completed_at timestamptz,
    decided_by varchar(128),
    decided_at timestamptz,
    decision_reason varchar(500),
    cancelled_by varchar(128),
    cancelled_at timestamptz,
    cancellation_reason varchar(500),
    CONSTRAINT uq_bpi_shadow_run_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_bpi_shadow_run_code UNIQUE (tenant_id, run_code),
    CONSTRAINT fk_bpi_shadow_run_rule_tenant
        FOREIGN KEY (tenant_id, rule_version_id)
        REFERENCES bpi.bpi_rule_versions (tenant_id, id),
    CONSTRAINT fk_bpi_shadow_run_topology_tenant
        FOREIGN KEY (tenant_id, topology_version_id)
        REFERENCES bpi.bpi_topology_versions (tenant_id, id),
    CONSTRAINT fk_bpi_shadow_run_point_catalog_tenant
        FOREIGN KEY (tenant_id, point_catalog_snapshot_id)
        REFERENCES bpi.bpi_point_catalog_snapshots (tenant_id, id),
    CONSTRAINT chk_bpi_shadow_run_lifecycle CHECK (
        (state = 'DRAFT'
            AND started_by IS NULL AND started_at IS NULL
            AND completed_by IS NULL AND completed_at IS NULL
            AND decided_by IS NULL AND decided_at IS NULL AND decision_reason IS NULL
            AND cancelled_by IS NULL AND cancelled_at IS NULL AND cancellation_reason IS NULL)
        OR
        (state = 'RUNNING'
            AND started_by IS NOT NULL AND started_at IS NOT NULL
            AND completed_by IS NULL AND completed_at IS NULL
            AND decided_by IS NULL AND decided_at IS NULL AND decision_reason IS NULL
            AND cancelled_by IS NULL AND cancelled_at IS NULL AND cancellation_reason IS NULL)
        OR
        (state = 'EVALUATING'
            AND started_by IS NOT NULL AND started_at IS NOT NULL
            AND completed_by IS NOT NULL AND completed_at IS NOT NULL
            AND decided_by IS NULL AND decided_at IS NULL AND decision_reason IS NULL
            AND cancelled_by IS NULL AND cancelled_at IS NULL AND cancellation_reason IS NULL)
        OR
        (state IN ('APPROVED', 'REJECTED')
            AND started_by IS NOT NULL AND started_at IS NOT NULL
            AND completed_by IS NOT NULL AND completed_at IS NOT NULL
            AND decided_by IS NOT NULL AND decided_at IS NOT NULL AND decision_reason IS NOT NULL
            AND cancelled_by IS NULL AND cancelled_at IS NULL AND cancellation_reason IS NULL)
        OR
        (state = 'CANCELLED'
            AND completed_by IS NULL AND completed_at IS NULL
            AND decided_by IS NULL AND decided_at IS NULL AND decision_reason IS NULL
            AND cancelled_by IS NOT NULL AND cancelled_at IS NOT NULL AND cancellation_reason IS NOT NULL)
    ),
    CONSTRAINT chk_bpi_shadow_run_time_order CHECK (
        (started_at IS NULL OR started_at >= created_at)
        AND (completed_at IS NULL OR (started_at IS NOT NULL AND completed_at >= started_at))
        AND (decided_at IS NULL OR (completed_at IS NOT NULL AND decided_at >= completed_at))
        AND (cancelled_at IS NULL OR cancelled_at >= created_at)
    )
);

CREATE UNIQUE INDEX uq_bpi_shadow_run_active_scope
    ON bpi.bpi_shadow_runs (tenant_id, plant_id, line_id)
    WHERE state = 'RUNNING';

CREATE INDEX idx_bpi_shadow_run_queue
    ON bpi.bpi_shadow_runs
       (tenant_id, plant_id, line_id, state, created_at DESC, id DESC);

CREATE TABLE bpi.bpi_shadow_run_batch_reviews (
    id uuid PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    shadow_run_id uuid NOT NULL,
    batch_id uuid NOT NULL,
    review_sequence bigint NOT NULL CHECK (review_sequence > 0),
    state varchar(16) NOT NULL CHECK (state IN ('ACTIVE', 'SUPERSEDED')),
    automatic_start_time timestamptz NOT NULL,
    automatic_end_time timestamptz NOT NULL,
    manual_start_time timestamptz NOT NULL,
    manual_end_time timestamptz NOT NULL,
    start_deviation_seconds bigint NOT NULL CHECK (start_deviation_seconds >= 0),
    end_deviation_seconds bigint NOT NULL CHECK (end_deviation_seconds >= 0),
    start_boundary_accepted boolean NOT NULL,
    end_boundary_accepted boolean NOT NULL,
    automatic_quantity numeric(24,6) NOT NULL CHECK (automatic_quantity >= 0),
    reference_quantity numeric(24,6) NOT NULL CHECK (reference_quantity > 0),
    quantity_unit varchar(32) NOT NULL,
    quantity_deviation_percent numeric(18,9) NOT NULL
        CHECK (quantity_deviation_percent >= 0),
    quantity_within_tolerance boolean NOT NULL,
    reviewed_by varchar(128) NOT NULL,
    review_reason varchar(500) NOT NULL,
    reviewed_at timestamptz NOT NULL DEFAULT now(),
    superseded_at timestamptz,
    CONSTRAINT uq_bpi_shadow_review_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_bpi_shadow_review_sequence
        UNIQUE (tenant_id, shadow_run_id, batch_id, review_sequence),
    CONSTRAINT fk_bpi_shadow_review_run_tenant
        FOREIGN KEY (tenant_id, shadow_run_id)
        REFERENCES bpi.bpi_shadow_runs (tenant_id, id),
    CONSTRAINT fk_bpi_shadow_review_batch_tenant
        FOREIGN KEY (tenant_id, batch_id)
        REFERENCES bpi.bpi_batch_instances (tenant_id, id),
    CONSTRAINT chk_bpi_shadow_review_time_order CHECK (
        automatic_end_time >= automatic_start_time
        AND manual_end_time >= manual_start_time
    ),
    CONSTRAINT chk_bpi_shadow_review_supersession CHECK (
        (state = 'ACTIVE' AND superseded_at IS NULL)
        OR (state = 'SUPERSEDED' AND superseded_at IS NOT NULL)
    )
);

CREATE UNIQUE INDEX uq_bpi_shadow_review_active_batch
    ON bpi.bpi_shadow_run_batch_reviews (tenant_id, shadow_run_id, batch_id)
    WHERE state = 'ACTIVE';

CREATE INDEX idx_bpi_shadow_review_run_metrics
    ON bpi.bpi_shadow_run_batch_reviews
       (tenant_id, shadow_run_id, state, reviewed_at, id);

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'bpi_service') THEN
        GRANT SELECT, INSERT, UPDATE ON bpi.bpi_shadow_runs TO bpi_service;
        GRANT SELECT, INSERT, UPDATE ON bpi.bpi_shadow_run_batch_reviews TO bpi_service;
    END IF;
END
$$;
