CREATE TABLE bpi.bpi_telemetry_source_state (
    tenant_id varchar(64) NOT NULL,
    gateway_id varchar(128) NOT NULL,
    device_id varchar(128) NOT NULL,
    source_epoch numeric(20, 0) NOT NULL CHECK (source_epoch >= 0),
    last_sequence numeric(20, 0) NOT NULL CHECK (last_sequence >= 0),
    last_event_id varchar(128) NOT NULL,
    last_event_time timestamptz NOT NULL,
    revision bigint NOT NULL DEFAULT 1 CHECK (revision > 0),
    updated_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (tenant_id, gateway_id, device_id)
);

CREATE TABLE bpi.bpi_telemetry_events (
    id uuid PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    plant_id varchar(64) NOT NULL,
    line_id varchar(128) NOT NULL,
    gateway_id varchar(128) NOT NULL,
    product_id varchar(128) NOT NULL,
    device_id varchar(128) NOT NULL,
    event_id varchar(128) NOT NULL,
    message_id varchar(128) NOT NULL,
    event_time timestamptz NOT NULL,
    ingest_time timestamptz NOT NULL,
    source_epoch numeric(20, 0) NOT NULL CHECK (source_epoch >= 0),
    sequence numeric(20, 0) NOT NULL CHECK (sequence >= 0),
    sequence_origin varchar(16) NOT NULL CHECK (sequence_origin IN ('DEVICE', 'GATEWAY', 'EXPORTER')),
    sequence_disposition varchar(32) NOT NULL,
    payload_checksum char(64) NOT NULL,
    headers jsonb NOT NULL DEFAULT '{}'::jsonb,
    point_count integer NOT NULL CHECK (point_count >= 0),
    accepted_point_count integer NOT NULL CHECK (accepted_point_count >= 0),
    rejected_point_count integer NOT NULL CHECK (rejected_point_count >= 0),
    status varchar(32) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (tenant_id, event_id),
    UNIQUE (tenant_id, gateway_id, device_id, source_epoch, sequence),
    UNIQUE (tenant_id, id)
);

CREATE INDEX idx_bpi_telemetry_events_line_time
    ON bpi.bpi_telemetry_events (tenant_id, plant_id, line_id, event_time DESC);
CREATE INDEX idx_bpi_telemetry_events_device_sequence
    ON bpi.bpi_telemetry_events (tenant_id, gateway_id, device_id, source_epoch, sequence DESC);

CREATE TABLE bpi.bpi_telemetry_points (
    id uuid PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    telemetry_event_id uuid NOT NULL,
    event_id varchar(128) NOT NULL,
    property_id varchar(128) NOT NULL,
    value_type varchar(16) NOT NULL CHECK (value_type IN ('DOUBLE', 'LONG', 'STRING', 'BOOLEAN')),
    numeric_value numeric,
    string_value text,
    boolean_value boolean,
    unit varchar(32) NOT NULL,
    quality_code varchar(16) NOT NULL CHECK (quality_code IN ('GOOD', 'UNCERTAIN', 'BAD', 'STALE', 'SUBSTITUTED')),
    sample_time timestamptz NOT NULL,
    calibration_version varchar(64),
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_bpi_telemetry_point_event
        FOREIGN KEY (tenant_id, telemetry_event_id)
        REFERENCES bpi.bpi_telemetry_events (tenant_id, id)
        ON DELETE CASCADE,
    UNIQUE (tenant_id, telemetry_event_id, property_id)
);

CREATE INDEX idx_bpi_telemetry_points_signal_time
    ON bpi.bpi_telemetry_points (tenant_id, property_id, sample_time DESC);

CREATE TABLE bpi.bpi_telemetry_point_rejects (
    id uuid PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    telemetry_event_id uuid NOT NULL,
    event_id varchar(128) NOT NULL,
    point_index integer NOT NULL CHECK (point_index >= 0),
    property_id varchar(128),
    reason_codes jsonb NOT NULL,
    raw_point jsonb NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_bpi_telemetry_reject_event
        FOREIGN KEY (tenant_id, telemetry_event_id)
        REFERENCES bpi.bpi_telemetry_events (tenant_id, id)
        ON DELETE CASCADE,
    UNIQUE (tenant_id, telemetry_event_id, point_index)
);

CREATE TABLE bpi.bpi_telemetry_quarantine (
    id uuid PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    event_id varchar(128),
    payload_checksum char(64) NOT NULL,
    reason_codes jsonb NOT NULL,
    raw_payload jsonb NOT NULL,
    trace_id varchar(64) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (tenant_id, payload_checksum)
);

CREATE INDEX idx_bpi_telemetry_quarantine_created
    ON bpi.bpi_telemetry_quarantine (tenant_id, created_at DESC);

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'bpi_service') THEN
        GRANT SELECT, INSERT, UPDATE, DELETE ON
            bpi.bpi_telemetry_source_state,
            bpi.bpi_telemetry_events,
            bpi.bpi_telemetry_points,
            bpi.bpi_telemetry_point_rejects,
            bpi.bpi_telemetry_quarantine
        TO bpi_service;
    END IF;
END
$$;
