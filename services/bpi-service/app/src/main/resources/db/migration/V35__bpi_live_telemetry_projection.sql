CREATE TABLE bpi.bpi_telemetry_point_latest (
    tenant_id varchar(64) NOT NULL,
    plant_id varchar(64) NOT NULL,
    line_id varchar(128) NOT NULL,
    gateway_id varchar(128) NOT NULL,
    product_id varchar(128) NOT NULL,
    device_id varchar(128) NOT NULL,
    property_id varchar(128) NOT NULL,
    telemetry_event_id uuid NOT NULL,
    event_id varchar(128) NOT NULL,
    source_epoch numeric(20, 0) NOT NULL CHECK (source_epoch >= 0),
    sequence numeric(20, 0) NOT NULL CHECK (sequence >= 0),
    sequence_origin varchar(16) NOT NULL CHECK (sequence_origin IN ('DEVICE', 'GATEWAY', 'EXPORTER')),
    sequence_disposition varchar(32) NOT NULL,
    value_type varchar(16) NOT NULL CHECK (value_type IN ('DOUBLE', 'LONG', 'STRING', 'BOOLEAN')),
    numeric_value numeric,
    string_value text,
    boolean_value boolean,
    unit varchar(32) NOT NULL,
    quality_code varchar(16) NOT NULL
        CHECK (quality_code IN ('GOOD', 'UNCERTAIN', 'BAD', 'STALE', 'SUBSTITUTED')),
    sample_time timestamptz NOT NULL,
    calibration_version varchar(64),
    updated_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (tenant_id, plant_id, line_id, product_id, device_id, property_id),
    CONSTRAINT fk_bpi_telemetry_latest_event
        FOREIGN KEY (tenant_id, telemetry_event_id)
        REFERENCES bpi.bpi_telemetry_events (tenant_id, id)
        ON DELETE CASCADE
);

CREATE INDEX idx_bpi_telemetry_latest_line_time
    ON bpi.bpi_telemetry_point_latest
        (tenant_id, plant_id, line_id, sample_time DESC)
    INCLUDE (
        product_id, device_id, property_id, numeric_value, string_value,
        boolean_value, unit, quality_code, sequence_disposition,
        calibration_version
    );

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'bpi_service') THEN
        GRANT SELECT, INSERT, UPDATE, DELETE
            ON bpi.bpi_telemetry_point_latest
            TO bpi_service;
    END IF;
END
$$;
