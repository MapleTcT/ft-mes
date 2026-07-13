CREATE TABLE bpi.bpi_outbox_events (
    id uuid PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    plant_id varchar(64) NOT NULL,
    line_id varchar(128) NOT NULL,
    aggregate_type varchar(32) NOT NULL,
    aggregate_id uuid NOT NULL,
    event_type varchar(64) NOT NULL,
    topic varchar(256) NOT NULL,
    partition_key varchar(512) NOT NULL,
    payload bytea NOT NULL,
    headers jsonb NOT NULL DEFAULT '{}'::jsonb,
    status varchar(16) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'DISPATCHING', 'PUBLISHED', 'FAILED')),
    attempt_count integer NOT NULL DEFAULT 0 CHECK (attempt_count >= 0),
    next_attempt_at timestamptz NOT NULL DEFAULT now(),
    claim_token uuid,
    claimed_at timestamptz,
    published_at timestamptz,
    last_error varchar(1000),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (tenant_id, aggregate_type, aggregate_id, event_type)
);

CREATE INDEX idx_bpi_outbox_dispatch
    ON bpi.bpi_outbox_events (status, next_attempt_at, created_at)
    WHERE status IN ('PENDING', 'DISPATCHING');

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'bpi_service') THEN
        GRANT SELECT, INSERT, UPDATE, DELETE ON bpi.bpi_outbox_events TO bpi_service;
    END IF;
END
$$;
