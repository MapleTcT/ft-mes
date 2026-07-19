-- Keyset pagination index for high-cardinality calibration evidence lists.
CREATE INDEX IF NOT EXISTS idx_bpi_point_calibrations_scope_cursor
    ON bpi.bpi_point_calibrations
       (tenant_id, plant_id, line_id, submitted_at DESC, id DESC);
