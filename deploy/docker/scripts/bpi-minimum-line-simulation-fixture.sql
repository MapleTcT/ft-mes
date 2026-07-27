\set ON_ERROR_STOP on

BEGIN;

CREATE TEMP TABLE bpi_minimum_line_samples ON COMMIT DROP AS
SELECT sample_no,
       :'boundary_base_time'::timestamptz + (sample_no - 1) * interval '1 second' AS sample_time,
       CASE
           WHEN sample_no BETWEEN 4 AND 10 THEN 13.5
           WHEN sample_no BETWEEN 11 AND 18 THEN 18.2
           WHEN sample_no BETWEEN 19 AND 25 THEN 0.2
           ELSE 0
       END::numeric AS instant_flow,
       CASE
           WHEN sample_no BETWEEN 4 AND 10 THEN 100 + (sample_no - 4) * 0.3
           WHEN sample_no BETWEEN 11 AND 18 THEN 102.1 + (sample_no - 11) * 0.5
           WHEN sample_no BETWEEN 19 AND 25 THEN 105.6
           ELSE 100
       END::numeric AS totalized_flow,
       sample_no BETWEEN 4 AND 18 AS pump_running,
       sample_no BETWEEN 4 AND 18 AS valve_path_ready,
       CASE
           WHEN sample_no BETWEEN 4 AND 10 THEN 40 + (sample_no - 4) * 0.08
           WHEN sample_no BETWEEN 11 AND 18 THEN 40.56 + (sample_no - 11) * 0.12
           WHEN sample_no BETWEEN 19 AND 25 THEN 41.4
           ELSE 40
       END::numeric AS tank_level
  FROM generate_series(1, 25) AS sample_no;

INSERT INTO bpi.bpi_rule_golden_boundaries
    (id, tenant_id, plant_id, line_id, golden_set_id, boundary_type,
     boundary_time, tolerance_seconds, source_ref, created_by)
VALUES
    (md5(:'marker' || ':golden-start')::uuid, '1000', 'PLANT-01', 'LINE-S07-01',
     :'marker' || '_GOLDEN', 'START',
     :'boundary_base_time'::timestamptz + interval '8 seconds', 1,
     :'marker' || '_CONTROLLED_OPERATOR_START', :'marker'),
    (md5(:'marker' || ':golden-end')::uuid, '1000', 'PLANT-01', 'LINE-S07-01',
     :'marker' || '_GOLDEN', 'END',
     :'boundary_base_time'::timestamptz + interval '22 seconds', 1,
     :'marker' || '_CONTROLLED_OPERATOR_END', :'marker');

INSERT INTO bpi.bpi_telemetry_events
    (id, tenant_id, plant_id, line_id, gateway_id, product_id, device_id,
     event_id, message_id, event_time, ingest_time, source_epoch, sequence,
     sequence_origin, sequence_disposition, payload_checksum, headers,
     point_count, accepted_point_count, rejected_point_count, status)
SELECT md5(:'marker' || ':telemetry:' || sample_no)::uuid,
       '1000', 'PLANT-01', 'LINE-S07-01',
       :'marker' || '_SIM_GATEWAY',
       'bpi-transfer-cell-product-01',
       'bpi-transfer-cell-device-01',
       :'marker' || '_SIM_EVENT_' || sample_no,
       :'marker' || '_SIM_MESSAGE_' || sample_no,
       sample_time,
       sample_time + interval '10 milliseconds',
       1,
       sample_no,
       'GATEWAY',
       CASE WHEN sample_no = 1 THEN 'FIRST' ELSE 'IN_ORDER' END,
       md5(:'marker' || ':payload:' || sample_no)
           || md5(:'marker' || ':payload-extra:' || sample_no),
       jsonb_build_object('source', 'CONTROLLED_MINIMUM_LINE_SIMULATION'),
       5, 5, 0, 'ACCEPTED'
  FROM bpi_minimum_line_samples;

INSERT INTO bpi.bpi_telemetry_points
    (id, tenant_id, telemetry_event_id, event_id, property_id, value_type,
     numeric_value, boolean_value, unit, quality_code, sample_time,
     calibration_version)
SELECT md5(:'marker' || ':point:' || sample.sample_no || ':' || point.property_id)::uuid,
       '1000',
       md5(:'marker' || ':telemetry:' || sample.sample_no)::uuid,
       :'marker' || '_SIM_' || point.event_suffix || '_' || sample.sample_no,
       point.property_id,
       point.value_type,
       point.numeric_value,
       point.boolean_value,
       point.unit,
       'GOOD',
       sample.sample_time,
       :'calibration_version'
  FROM bpi_minimum_line_samples sample
 CROSS JOIN LATERAL (
       VALUES
           ('flow.instant', 'DOUBLE', sample.instant_flow, NULL::boolean, 'm3/h', 'FLOW'),
           ('flow.totalizer', 'DOUBLE', sample.totalized_flow, NULL::boolean, 'm3', 'TOTAL'),
           ('pump.running', 'BOOLEAN', NULL::numeric, sample.pump_running, 'bool', 'PUMP'),
           ('valve.path.ready', 'BOOLEAN', NULL::numeric, sample.valve_path_ready, 'bool', 'VALVE'),
           ('tank.level', 'DOUBLE', sample.tank_level, NULL::boolean, '%', 'LEVEL')
     ) AS point(property_id, value_type, numeric_value, boolean_value, unit, event_suffix);

COMMIT;

SELECT jsonb_build_object(
    'marker', :'marker',
    'goldenSetId', :'marker' || '_GOLDEN',
    'boundaryBaseTime', :'boundary_base_time',
    'eventCount', (
        SELECT count(*)
          FROM bpi.bpi_telemetry_events
         WHERE tenant_id = '1000'
           AND gateway_id = :'marker' || '_SIM_GATEWAY'
    ),
    'pointCount', (
        SELECT count(*)
          FROM bpi.bpi_telemetry_points point
          JOIN bpi.bpi_telemetry_events event
            ON event.tenant_id = point.tenant_id
           AND event.id = point.telemetry_event_id
         WHERE event.tenant_id = '1000'
           AND event.gateway_id = :'marker' || '_SIM_GATEWAY'
    ),
    'goldenCount', (
        SELECT count(*)
          FROM bpi.bpi_rule_golden_boundaries
         WHERE tenant_id = '1000'
           AND golden_set_id = :'marker' || '_GOLDEN'
    )
);
