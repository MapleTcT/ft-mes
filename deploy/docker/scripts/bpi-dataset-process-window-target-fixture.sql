\set ON_ERROR_STOP on

BEGIN;

UPDATE bpi.bpi_point_catalog_snapshots
   SET point_count = 2,
       source_claim_ready_point_count = 2
 WHERE tenant_id = '1000'
   AND id = md5(:'marker' || ':catalog')::uuid;

INSERT INTO bpi.bpi_point_catalog_entries
    (id, tenant_id, snapshot_id, plant_id, line_id, locality_group,
     product_id, device_id, property_id, point_name, unit, data_type,
     device_state, registered, property_present, calibration_version,
     calibration_status, source_sequence_enabled)
VALUES
    (md5(:'marker' || ':catalog-entry-flow')::uuid, '1000',
     md5(:'marker' || ':catalog')::uuid, 'PLANT-01', 'LINE-S07-01',
     :'marker' || '_LOCALITY', :'marker' || '_PRODUCT',
     :'marker' || '_FLOW_DEVICE', 'flow.instant', '进料瞬时流量',
     't/h', 'DOUBLE', 'ACTIVE', true, true, :'marker' || '_CAL_FLOW_1',
     'VERIFIED', true),
    (md5(:'marker' || ':catalog-entry-pump')::uuid, '1000',
     md5(:'marker' || ':catalog')::uuid, 'PLANT-01', 'LINE-S07-01',
     :'marker' || '_LOCALITY', :'marker' || '_PRODUCT',
     :'marker' || '_PUMP_DEVICE', 'pump.running', '进料泵运行状态',
     'bool', 'BOOLEAN', 'ACTIVE', true, true, NULL,
     'MISSING', true);

INSERT INTO bpi.bpi_point_calibrations
    (id, tenant_id, plant_id, line_id, product_id, device_id, property_id,
     calibration_version, certificate_reference, certificate_checksum,
     valid_from, valid_until, state, revision, submitted_by, submitted_at,
     submit_reason, decided_by, decided_at, decision_reason)
VALUES
    (md5(:'marker' || ':flow-calibration')::uuid, '1000', 'PLANT-01',
     'LINE-S07-01', :'marker' || '_PRODUCT', :'marker' || '_FLOW_DEVICE',
     'flow.instant', :'marker' || '_CAL_FLOW_1',
     'urn:adp:calibration:' || :'marker', repeat('f', 64),
     now() - interval '10 days', now() + interval '10 days',
     'APPROVED', 2, :'marker' || '_CAL_AUTHOR', now() - interval '9 days',
     :'marker' || ' 工艺窗口目标验收校准',
     :'marker' || '_CAL_REVIEWER', now() - interval '9 days',
     '独立复核通过');

UPDATE bpi.bpi_topology_versions
   SET definition = jsonb_build_object(
       'localityGroup', :'marker' || '_LOCALITY',
       'nodes', jsonb_build_array(
           jsonb_build_object('code', 'FEED-METER', 'type', 'METER'),
           jsonb_build_object('code', 'FEED-PUMP', 'type', 'PUMP')),
       'edges', jsonb_build_array(
           jsonb_build_object('from', 'FEED-METER', 'to', 'FEED-PUMP')),
       'bindings', jsonb_build_array(
           jsonb_build_object(
               'signal', 'flow.instant',
               'productId', :'marker' || '_PRODUCT',
               'deviceId', :'marker' || '_FLOW_DEVICE',
               'propertyId', 'flow.instant',
               'expectedUnit', 't/h',
               'calibrationVersion', :'marker' || '_CAL_FLOW_1'),
           jsonb_build_object(
               'signal', 'pump.running',
               'productId', :'marker' || '_PRODUCT',
               'deviceId', :'marker' || '_PUMP_DEVICE',
               'propertyId', 'pump.running',
               'expectedUnit', 'bool',
               'calibrationVersion', :'marker' || '_CAL_PUMP_NOT_REQUIRED')),
       'requiredSignals', jsonb_build_array('flow.instant', 'pump.running'))
 WHERE tenant_id = '1000'
   AND id = md5(:'marker' || ':topology')::uuid;

WITH prediction AS (
    SELECT review.automatic_start_time AS prediction_time
      FROM bpi.bpi_shadow_run_batch_reviews review
      JOIN bpi.bpi_batch_instances batch
        ON batch.tenant_id = review.tenant_id
       AND batch.id = review.batch_id
     WHERE review.tenant_id = '1000'
       AND batch.batch_no = :'marker' || '_HIGH'
), event_rows AS (
    SELECT values.*
      FROM prediction
      CROSS JOIN LATERAL (
          VALUES
              ('FLOW_1', :'marker' || '_FLOW_DEVICE',
               prediction.prediction_time - interval '60 seconds',
               prediction.prediction_time - interval '59 seconds', 1),
              ('FLOW_2', :'marker' || '_FLOW_DEVICE',
               prediction.prediction_time - interval '30 seconds',
               prediction.prediction_time - interval '29 seconds', 2),
              ('FLOW_3', :'marker' || '_FLOW_DEVICE',
               prediction.prediction_time,
               prediction.prediction_time, 3),
              ('FLOW_LATE', :'marker' || '_FLOW_DEVICE',
               prediction.prediction_time - interval '15 seconds',
               prediction.prediction_time + interval '5 seconds', 4),
              ('FLOW_POST_FREEZE', :'marker' || '_FLOW_DEVICE',
               prediction.prediction_time - interval '10 seconds',
               now() + interval '1 day', 5),
              ('PUMP_1', :'marker' || '_PUMP_DEVICE',
               prediction.prediction_time - interval '30 seconds',
               prediction.prediction_time - interval '29 seconds', 1),
              ('PUMP_2', :'marker' || '_PUMP_DEVICE',
               prediction.prediction_time,
               prediction.prediction_time, 2)
      ) AS values(tag, device_id, event_time, ingest_time, sequence)
)
INSERT INTO bpi.bpi_telemetry_events
    (id, tenant_id, plant_id, line_id, gateway_id, product_id, device_id,
     event_id, message_id, event_time, ingest_time, source_epoch, sequence,
     sequence_origin, sequence_disposition, payload_checksum, headers,
     point_count, accepted_point_count, rejected_point_count, status)
SELECT md5(:'marker' || ':event:' || tag)::uuid, '1000', 'PLANT-01',
       'LINE-S07-01', :'marker' || '_GATEWAY', :'marker' || '_PRODUCT',
       device_id, :'marker' || '_EVENT_' || tag, :'marker' || '_MESSAGE_' || tag,
       event_time, ingest_time, 1, sequence, 'DEVICE', 'ACCEPTED',
       repeat('e', 64), '{}'::jsonb, 1, 1, 0, 'ACCEPTED'
  FROM event_rows;

WITH prediction AS (
    SELECT review.automatic_start_time AS prediction_time
      FROM bpi.bpi_shadow_run_batch_reviews review
      JOIN bpi.bpi_batch_instances batch
        ON batch.tenant_id = review.tenant_id
       AND batch.id = review.batch_id
     WHERE review.tenant_id = '1000'
       AND batch.batch_no = :'marker' || '_HIGH'
), point_rows AS (
    SELECT values.*
      FROM prediction
      CROSS JOIN LATERAL (
          VALUES
              ('FLOW_1', 'flow.instant', 'DOUBLE', 10::numeric, NULL::boolean,
               't/h', :'marker' || '_CAL_FLOW_1',
               prediction.prediction_time - interval '60 seconds'),
              ('FLOW_2', 'flow.instant', 'DOUBLE', 20::numeric, NULL::boolean,
               't/h', :'marker' || '_CAL_FLOW_1',
               prediction.prediction_time - interval '30 seconds'),
              ('FLOW_3', 'flow.instant', 'DOUBLE', 30::numeric, NULL::boolean,
               't/h', :'marker' || '_CAL_FLOW_1',
               prediction.prediction_time),
              ('FLOW_LATE', 'flow.instant', 'DOUBLE', 999::numeric, NULL::boolean,
               't/h', :'marker' || '_CAL_FLOW_1',
               prediction.prediction_time - interval '15 seconds'),
              ('FLOW_POST_FREEZE', 'flow.instant', 'DOUBLE', 888::numeric, NULL::boolean,
               't/h', :'marker' || '_CAL_FLOW_1',
               prediction.prediction_time - interval '10 seconds'),
              ('PUMP_1', 'pump.running', 'BOOLEAN', NULL::numeric, true,
               'bool', NULL::text,
               prediction.prediction_time - interval '30 seconds'),
              ('PUMP_2', 'pump.running', 'BOOLEAN', NULL::numeric, false,
               'bool', NULL::text,
               prediction.prediction_time)
      ) AS values(
          tag, property_id, value_type, numeric_value, boolean_value,
          unit, calibration_version, sample_time)
)
INSERT INTO bpi.bpi_telemetry_points
    (id, tenant_id, telemetry_event_id, event_id, property_id, value_type,
     numeric_value, string_value, boolean_value, unit, quality_code,
     sample_time, calibration_version)
SELECT md5(:'marker' || ':point:' || point_rows.tag)::uuid, '1000',
       event.id, event.event_id, point_rows.property_id, point_rows.value_type,
       point_rows.numeric_value, NULL, point_rows.boolean_value, point_rows.unit,
       'GOOD', point_rows.sample_time, point_rows.calibration_version
  FROM point_rows
  JOIN bpi.bpi_telemetry_events event
    ON event.tenant_id = '1000'
   AND event.id = md5(:'marker' || ':event:' || point_rows.tag)::uuid;

COMMIT;

SELECT jsonb_pretty(jsonb_build_object(
    'marker', :'marker',
    'catalogEntries', (SELECT count(*) FROM bpi.bpi_point_catalog_entries
                        WHERE tenant_id = '1000'
                          AND snapshot_id = md5(:'marker' || ':catalog')::uuid),
    'calibrations', (SELECT count(*) FROM bpi.bpi_point_calibrations
                      WHERE tenant_id = '1000'
                        AND certificate_reference = 'urn:adp:calibration:' || :'marker'),
    'telemetryEvents', (SELECT count(*) FROM bpi.bpi_telemetry_events
                         WHERE tenant_id = '1000'
                           AND event_id LIKE :'marker' || '_EVENT_%'),
    'telemetryPoints', (SELECT count(*) FROM bpi.bpi_telemetry_points
                         WHERE tenant_id = '1000'
                           AND event_id LIKE :'marker' || '_EVENT_%')
));
