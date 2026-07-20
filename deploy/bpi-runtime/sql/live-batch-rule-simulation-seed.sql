\set ON_ERROR_STOP on

BEGIN;

SELECT 1 / CASE
    WHEN :'marker' ~ '^[A-Za-z0-9_-]{8,96}$' THEN 1
    ELSE 0
END AS marker_guard;

SELECT 1 / CASE WHEN EXISTS (
    SELECT 1
      FROM bpi.bpi_telemetry_events
     WHERE tenant_id = :'tenant_id'
       AND headers ->> 'acceptance_marker' = :'marker'
) THEN 0 ELSE 1 END AS duplicate_marker_guard;

INSERT INTO bpi.bpi_rule_golden_boundaries
    (id, tenant_id, plant_id, line_id, golden_set_id, boundary_type,
     boundary_time, tolerance_seconds, source_ref, created_by)
VALUES
    (md5(:'marker' || ':golden:start')::uuid,
     :'tenant_id', :'plant_id', :'line_id', :'start_golden_set_id', 'START',
     :'start_boundary_time'::timestamptz, 5,
     'test-only-live-batch-rule-qualification:' || :'marker' || ':START', :'marker'),
    (md5(:'marker' || ':golden:end')::uuid,
     :'tenant_id', :'plant_id', :'line_id', :'end_golden_set_id', 'END',
     :'end_boundary_time'::timestamptz, 5,
     'test-only-live-batch-rule-qualification:' || :'marker' || ':END', :'marker');

INSERT INTO bpi.bpi_telemetry_events
    (id, tenant_id, plant_id, line_id, gateway_id, product_id, device_id,
     event_id, message_id, event_time, ingest_time, source_epoch, sequence,
     sequence_origin, sequence_disposition, payload_checksum, headers,
     point_count, accepted_point_count, rejected_point_count, status)
VALUES
    (md5(:'marker' || ':telemetry:start')::uuid,
     :'tenant_id', :'plant_id', :'line_id', 'GW-TEST-ONLY-RULE-QUALIFICATION',
     :'product_id', :'device_id', :'marker' || '-HISTORY-START', :'marker' || '-HISTORY-START-MSG',
     :'start_boundary_time'::timestamptz - interval '2 seconds',
     :'start_boundary_time'::timestamptz - interval '1990 milliseconds',
     1, 1, 'EXPORTER', 'FIRST', repeat('a', 64),
     jsonb_build_object(
         'acceptance_marker', :'marker',
         'fixture_kind', 'TEST_ONLY_RULE_QUALIFICATION',
         'boundary_type', 'START'),
     1, 1, 0, 'ACCEPTED'),
    (md5(:'marker' || ':telemetry:end')::uuid,
     :'tenant_id', :'plant_id', :'line_id', 'GW-TEST-ONLY-RULE-QUALIFICATION',
     :'product_id', :'device_id', :'marker' || '-HISTORY-END', :'marker' || '-HISTORY-END-MSG',
     :'end_boundary_time'::timestamptz - interval '2 seconds',
     :'end_boundary_time'::timestamptz - interval '1990 milliseconds',
     1, 2, 'EXPORTER', 'IN_ORDER', repeat('b', 64),
     jsonb_build_object(
         'acceptance_marker', :'marker',
         'fixture_kind', 'TEST_ONLY_RULE_QUALIFICATION',
         'boundary_type', 'END'),
     1, 1, 0, 'ACCEPTED');

INSERT INTO bpi.bpi_telemetry_points
    (id, tenant_id, telemetry_event_id, event_id, property_id, value_type,
     numeric_value, string_value, boolean_value, unit, quality_code,
     sample_time, calibration_version)
VALUES
    (md5(:'marker' || ':point:start')::uuid, :'tenant_id',
     md5(:'marker' || ':telemetry:start')::uuid, :'marker' || '-HISTORY-START-FLOW',
     :'property_id', 'DOUBLE', 18.6, NULL, NULL, :'unit', 'GOOD',
     :'start_boundary_time'::timestamptz - interval '2 seconds', :'calibration_version'),
    (md5(:'marker' || ':point:end')::uuid, :'tenant_id',
     md5(:'marker' || ':telemetry:end')::uuid, :'marker' || '-HISTORY-END-FLOW',
     :'property_id', 'DOUBLE', 0.2, NULL, NULL, :'unit', 'GOOD',
     :'end_boundary_time'::timestamptz - interval '2 seconds', :'calibration_version');

SELECT json_build_object(
    'status', 'SEEDED',
    'fixtureKind', 'TEST_ONLY_RULE_QUALIFICATION',
    'marker', :'marker',
    'startGoldenSetId', :'start_golden_set_id',
    'endGoldenSetId', :'end_golden_set_id',
    'telemetryEvents', 2,
    'telemetryPoints', 2,
    'goldenBoundaries', 2
);

COMMIT;
