ALTER TABLE bpi.bpi_point_catalog_entries
    ADD COLUMN source_property_id varchar(128);

COMMENT ON COLUMN bpi.bpi_point_catalog_entries.property_id IS
    'Canonical property identity emitted by the exporter and consumed by BPI.';

COMMENT ON COLUMN bpi.bpi_point_catalog_entries.source_property_id IS
    'Original JetLinks/source property identity before exporter normalization.';
