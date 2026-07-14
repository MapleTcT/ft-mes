CREATE TABLE bpi.bpi_point_catalog_snapshots (
    id uuid PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    source varchar(64) NOT NULL,
    source_instance varchar(128) NOT NULL,
    source_revision varchar(128) NOT NULL,
    plant_id varchar(64) NOT NULL,
    line_id varchar(128) NOT NULL,
    checksum char(64) NOT NULL,
    observed_at timestamptz NOT NULL,
    point_count integer NOT NULL CHECK (point_count >= 0),
    ready_point_count integer NOT NULL CHECK (ready_point_count >= 0 AND ready_point_count <= point_count),
    imported_by varchar(128) NOT NULL,
    imported_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (tenant_id, id),
    UNIQUE (tenant_id, source, source_instance, plant_id, line_id, source_revision)
);

CREATE INDEX idx_bpi_point_catalog_scope_time
    ON bpi.bpi_point_catalog_snapshots
        (tenant_id, plant_id, line_id, observed_at DESC, imported_at DESC);

CREATE TABLE bpi.bpi_point_catalog_entries (
    id uuid PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    snapshot_id uuid NOT NULL,
    plant_id varchar(64) NOT NULL,
    line_id varchar(128) NOT NULL,
    locality_group varchar(128),
    product_id varchar(128) NOT NULL,
    device_id varchar(128) NOT NULL,
    property_id varchar(128) NOT NULL,
    point_name varchar(256),
    unit varchar(32),
    data_type varchar(64),
    device_state varchar(16) NOT NULL CHECK (device_state IN ('ACTIVE', 'INACTIVE', 'UNKNOWN')),
    registered boolean NOT NULL,
    property_present boolean NOT NULL,
    calibration_version varchar(128),
    calibration_status varchar(16) NOT NULL
        CHECK (calibration_status IN ('VERIFIED', 'UNVERIFIED', 'MISSING')),
    source_sequence_enabled boolean NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_bpi_point_catalog_snapshot_tenant
        FOREIGN KEY (tenant_id, snapshot_id)
        REFERENCES bpi.bpi_point_catalog_snapshots (tenant_id, id),
    UNIQUE (tenant_id, snapshot_id, product_id, device_id, property_id)
);

CREATE INDEX idx_bpi_point_catalog_entry_lookup
    ON bpi.bpi_point_catalog_entries
        (tenant_id, snapshot_id, product_id, device_id, property_id);

ALTER TABLE bpi.bpi_topology_versions
    ADD COLUMN validated_point_catalog_snapshot_id uuid,
    ADD COLUMN validated_point_catalog_checksum char(64),
    ADD CONSTRAINT fk_bpi_topology_point_catalog_tenant
        FOREIGN KEY (tenant_id, validated_point_catalog_snapshot_id)
        REFERENCES bpi.bpi_point_catalog_snapshots (tenant_id, id);

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'bpi_service') THEN
        GRANT SELECT, INSERT ON
            bpi.bpi_point_catalog_snapshots,
            bpi.bpi_point_catalog_entries
        TO bpi_service;
    END IF;
END
$$;
