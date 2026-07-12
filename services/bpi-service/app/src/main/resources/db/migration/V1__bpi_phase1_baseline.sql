CREATE SCHEMA IF NOT EXISTS bpi;

CREATE TABLE bpi.bpi_topology_versions (
    id uuid PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    topology_code varchar(128) NOT NULL,
    version varchar(64) NOT NULL,
    state varchar(32) NOT NULL CHECK (state IN ('DRAFT', 'PUBLISHED', 'RETIRED')),
    checksum varchar(128) NOT NULL,
    definition jsonb NOT NULL,
    created_by varchar(128) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (tenant_id, topology_code, version)
);

CREATE TABLE bpi.bpi_rule_versions (
    id uuid PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    rule_code varchar(128) NOT NULL,
    version varchar(64) NOT NULL,
    topology_version_id uuid NOT NULL REFERENCES bpi.bpi_topology_versions(id),
    state varchar(32) NOT NULL CHECK (state IN ('DRAFT', 'SIMULATION_PASSED', 'PUBLISHED', 'RETIRED')),
    checksum varchar(128) NOT NULL,
    definition jsonb NOT NULL,
    revision bigint NOT NULL DEFAULT 1 CHECK (revision > 0),
    created_by varchar(128) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (tenant_id, rule_code, version)
);

CREATE TABLE bpi.bpi_inbox_events (
    id uuid PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    source varchar(128) NOT NULL,
    idempotency_key varchar(256) NOT NULL,
    event_id varchar(256) NOT NULL,
    payload_checksum varchar(128) NOT NULL,
    received_at timestamptz NOT NULL DEFAULT now(),
    processed_at timestamptz,
    UNIQUE (tenant_id, source, idempotency_key),
    UNIQUE (tenant_id, source, event_id)
);

CREATE TABLE bpi.bpi_batch_candidates (
    id uuid PRIMARY KEY,
    candidate_key uuid NOT NULL,
    tenant_id varchar(64) NOT NULL,
    plant_id varchar(64) NOT NULL,
    line_id varchar(128) NOT NULL,
    boundary_type varchar(16) NOT NULL CHECK (boundary_type IN ('START', 'END')),
    order_id varchar(128),
    boundary_time timestamptz NOT NULL,
    state varchar(24) NOT NULL CHECK (state IN ('PENDING', 'CONFIRMED', 'REJECTED')),
    revision bigint NOT NULL CHECK (revision > 0),
    confidence numeric(7,6) NOT NULL CHECK (confidence >= 0 AND confidence <= 1),
    topology_version_id uuid NOT NULL REFERENCES bpi.bpi_topology_versions(id),
    rule_version_id uuid NOT NULL REFERENCES bpi.bpi_rule_versions(id),
    evidence jsonb NOT NULL,
    missing_signals jsonb NOT NULL DEFAULT '[]'::jsonb,
    batch_id uuid,
    reviewed_by varchar(128),
    review_reason varchar(500),
    reviewed_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (tenant_id, candidate_key)
);

CREATE INDEX idx_bpi_candidates_queue
    ON bpi.bpi_batch_candidates (tenant_id, plant_id, state, boundary_time DESC);
CREATE INDEX idx_bpi_candidates_line
    ON bpi.bpi_batch_candidates (tenant_id, line_id, boundary_time DESC);

CREATE TABLE bpi.bpi_batch_instances (
    id uuid PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    plant_id varchar(64) NOT NULL,
    batch_no varchar(128) NOT NULL,
    line_id varchar(128) NOT NULL,
    stage_code varchar(128) NOT NULL,
    order_id varchar(128),
    material_code varchar(128),
    state varchar(32) NOT NULL CHECK (state IN ('ACTIVE', 'SUSPENDED', 'RECONCILING', 'WAIT_QA', 'RELEASED', 'INBOUNDED', 'CLOSED')),
    revision bigint NOT NULL CHECK (revision > 0),
    is_shadow boolean NOT NULL DEFAULT true,
    start_time timestamptz NOT NULL,
    end_time timestamptz,
    quantity numeric(24,6) NOT NULL DEFAULT 0,
    quantity_unit varchar(32) NOT NULL DEFAULT 't',
    dry_matter numeric(24,6),
    quality_gate varchar(32) NOT NULL DEFAULT 'NOT_APPLICABLE',
    wms_status varchar(32) NOT NULL DEFAULT 'NOT_REQUESTED',
    topology_version_id uuid NOT NULL REFERENCES bpi.bpi_topology_versions(id),
    rule_version_id uuid NOT NULL REFERENCES bpi.bpi_rule_versions(id),
    created_by varchar(128) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (tenant_id, batch_no)
);

ALTER TABLE bpi.bpi_batch_candidates
    ADD CONSTRAINT fk_bpi_candidate_batch FOREIGN KEY (batch_id) REFERENCES bpi.bpi_batch_instances(id);

CREATE INDEX idx_bpi_batches_line_state_time
    ON bpi.bpi_batch_instances (tenant_id, line_id, state, start_time DESC);

CREATE TABLE bpi.bpi_batch_state_events (
    id uuid PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    batch_id uuid NOT NULL REFERENCES bpi.bpi_batch_instances(id),
    revision bigint NOT NULL,
    action varchar(128) NOT NULL,
    from_state varchar(32),
    to_state varchar(32) NOT NULL,
    reason varchar(500),
    actor_id varchar(128) NOT NULL,
    event_time timestamptz NOT NULL,
    trace_id varchar(128) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (batch_id, revision),
    UNIQUE (tenant_id, trace_id, action)
);

CREATE TABLE bpi.bpi_boundary_evidence (
    id uuid PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    batch_id uuid NOT NULL REFERENCES bpi.bpi_batch_instances(id),
    boundary_type varchar(16) NOT NULL CHECK (boundary_type IN ('START', 'END')),
    source_event_id varchar(256) NOT NULL,
    signal varchar(128) NOT NULL,
    classification varchar(16) NOT NULL CHECK (classification IN ('REQUIRED', 'QUORUM', 'OPTIONAL')),
    satisfied boolean NOT NULL,
    value_text text,
    unit varchar(32),
    quality varchar(32) NOT NULL,
    event_time timestamptz NOT NULL,
    source varchar(128) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_bpi_evidence_batch_boundary
    ON bpi.bpi_boundary_evidence (tenant_id, batch_id, boundary_type, event_time);

CREATE TABLE bpi.bpi_audit_events (
    id uuid PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    plant_id varchar(64) NOT NULL,
    line_id varchar(128),
    object_type varchar(64) NOT NULL,
    object_id uuid NOT NULL,
    action varchar(128) NOT NULL,
    actor_id varchar(128) NOT NULL,
    before_revision bigint,
    after_revision bigint,
    reason varchar(500),
    trace_id varchar(128) NOT NULL,
    detail jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_bpi_audit_object
    ON bpi.bpi_audit_events (tenant_id, object_type, object_id, created_at DESC);

CREATE TABLE bpi.bpi_api_idempotency (
    id uuid PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    idempotency_key varchar(128) NOT NULL,
    method varchar(16) NOT NULL,
    resource_path varchar(512) NOT NULL,
    request_checksum varchar(128) NOT NULL,
    state varchar(24) NOT NULL CHECK (state IN ('PROCESSING', 'COMPLETED')),
    response_status integer,
    response_body jsonb,
    created_at timestamptz NOT NULL DEFAULT now(),
    completed_at timestamptz,
    UNIQUE (tenant_id, idempotency_key)
);

CREATE TABLE bpi.bpi_feature_flags (
    id uuid PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    scope_type varchar(16) NOT NULL CHECK (scope_type IN ('GLOBAL', 'TENANT', 'PLANT', 'LINE')),
    scope_key varchar(128) NOT NULL,
    flag_key varchar(128) NOT NULL,
    enabled boolean NOT NULL,
    revision bigint NOT NULL CHECK (revision > 0),
    updated_by varchar(128) NOT NULL,
    updated_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (tenant_id, scope_type, scope_key, flag_key)
);

INSERT INTO bpi.bpi_feature_flags
    (id, tenant_id, scope_type, scope_key, flag_key, enabled, revision, updated_by)
VALUES
    ('00000000-0000-0000-0000-000000000001', '*', 'GLOBAL', '*', 'bpi.ui', false, 1, 'flyway'),
    ('00000000-0000-0000-0000-000000000002', '*', 'GLOBAL', '*', 'bpi.commands', false, 1, 'flyway'),
    ('00000000-0000-0000-0000-000000000003', '*', 'GLOBAL', '*', 'bpi.shadow-only', true, 1, 'flyway');
