ALTER TABLE bpi.bpi_point_catalog_snapshots
    DROP CONSTRAINT bpi_point_catalog_snapshots_tenant_id_source_source_instanc_key;

ALTER TABLE bpi.bpi_point_catalog_snapshots
    ADD CONSTRAINT uq_bpi_point_catalog_snapshot_observation
    UNIQUE (tenant_id, source, source_instance, plant_id, line_id, source_revision, observed_at);
