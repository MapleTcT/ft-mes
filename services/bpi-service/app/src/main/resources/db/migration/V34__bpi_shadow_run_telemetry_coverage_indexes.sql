CREATE INDEX idx_bpi_telemetry_events_shadow_window
    ON bpi.bpi_telemetry_events
        (tenant_id, plant_id, line_id, product_id, device_id, event_time, created_at)
    INCLUDE (
        id, sequence_origin, source_epoch, sequence, sequence_disposition,
        accepted_point_count, rejected_point_count
    );

CREATE INDEX idx_bpi_telemetry_points_event_property_window
    ON bpi.bpi_telemetry_points
        (tenant_id, telemetry_event_id, property_id, sample_time, created_at)
    INCLUDE (quality_code, calibration_version);

CREATE INDEX idx_bpi_telemetry_rejects_event_property
    ON bpi.bpi_telemetry_point_rejects
        (tenant_id, telemetry_event_id, property_id);
