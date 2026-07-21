\set ON_ERROR_STOP on

BEGIN;

INSERT INTO bpi.bpi_feature_flags
    (id, tenant_id, scope_type, scope_key, flag_key, enabled, revision,
     updated_by, active, last_reason)
VALUES
    (:'commands_flag_id'::uuid, '1000', 'PLANT', 'PLANT-01', 'bpi.commands', true, 1,
     :'marker', true, :'marker' || ' controlled WMS outage recovery'),
    (:'wms_flag_id'::uuid, '1000', 'PLANT', 'PLANT-01', 'bpi.wms-link', true, 1,
     :'marker', true, :'marker' || ' controlled WMS outage recovery');

INSERT INTO bpi.bpi_batch_instances
    (id, tenant_id, plant_id, batch_no, line_id, stage_code, order_id,
     material_code, state, revision, is_shadow, start_time, end_time,
     quantity, quantity_unit, quality_gate, wms_status,
     topology_version_id, rule_version_id, created_by)
SELECT :'batch_id'::uuid, '1000', 'PLANT-01', :'marker', 'LINE-S07-01',
       'PACKING', :'marker' || '_ORDER', :'marker' || '_MATERIAL',
       'RELEASED', 3, false, now() - interval '2 hours', now() - interval '1 hour',
       12.345000, 'kg', 'ACCEPTED', 'PENDING',
       topology_version_id, rule_version_id, :'marker'
  FROM bpi.bpi_batch_instances
 WHERE tenant_id = '1000'
   AND topology_version_id IS NOT NULL
   AND rule_version_id IS NOT NULL
 ORDER BY created_at DESC
 LIMIT 1;

INSERT INTO bpi.bpi_quality_gates
    (id, tenant_id, batch_id, external_gate_id, external_revision,
     source_event_id, payload_checksum, state, release_quantity,
     quantity_unit, material_code, observed_at, created_at, updated_at)
VALUES
    (:'quality_gate_id'::uuid, '1000', :'batch_id'::uuid,
     :'marker' || '_GATE', 1, :'marker' || '_QUALITY_EVENT',
     md5(:'marker'), 'ACCEPTED', 12.345000, 'kg', :'marker' || '_MATERIAL',
     now() - interval '20 minutes', now() - interval '20 minutes', now() - interval '20 minutes');

INSERT INTO bpi.bpi_quality_links
    (id, tenant_id, batch_id, quality_gate_id, inspection_code,
     inspection_record_id, required, disposition, final_result,
     observed_at, created_at, updated_at)
VALUES
    (:'quality_link_id'::uuid, '1000', :'batch_id'::uuid, :'quality_gate_id'::uuid,
     'FINAL_RELEASE', :'marker' || '_INSPECTION', true, 'ACCEPTED', true,
     now() - interval '20 minutes', now() - interval '20 minutes', now() - interval '20 minutes');

INSERT INTO bpi.bpi_outbox_events
    (id, tenant_id, plant_id, line_id, aggregate_type, aggregate_id,
     event_type, topic, partition_key, payload, headers, status,
     attempt_count, total_attempt_count, manual_retry_count, next_attempt_at,
     revision, created_at, updated_at)
VALUES
    (:'outbox_id'::uuid, '1000', 'PLANT-01', 'LINE-S07-01',
     'BATCH_INSTANCE', :'batch_id'::uuid, 'WMS_COMPLETION_INBOUND_COMMAND',
     :'command_topic', :'partition_key',
     decode(:'payload_base64', 'base64'), CAST(:'headers_json' AS jsonb),
     'PENDING', 0, 0, 0, now(), 1, now(), now());

INSERT INTO bpi.bpi_wms_inbound_links
    (id, tenant_id, batch_id, command_event_id, idempotency_key,
     status, revision, created_at, updated_at)
VALUES
    (:'wms_link_id'::uuid, '1000', :'batch_id'::uuid, :'outbox_id'::uuid,
     :'marker' || '|WMS|1', 'PENDING', 1, now(), now());

COMMIT;

SELECT json_build_object(
    'marker', :'marker',
    'batchId', :'batch_id',
    'commandEventId', :'outbox_id',
    'wmsIdempotencyKey', :'marker' || '|WMS|1',
    'batchRows', (SELECT count(*) FROM bpi.bpi_batch_instances WHERE id = :'batch_id'::uuid),
    'outboxRows', (SELECT count(*) FROM bpi.bpi_outbox_events WHERE id = :'outbox_id'::uuid),
    'wmsLinkRows', (SELECT count(*) FROM bpi.bpi_wms_inbound_links WHERE id = :'wms_link_id'::uuid),
    'payloadBytes', (SELECT octet_length(payload) FROM bpi.bpi_outbox_events WHERE id = :'outbox_id'::uuid)
);
