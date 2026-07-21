\set ON_ERROR_STOP on

BEGIN;

DELETE FROM bpi.bpi_inbox_events inbox
 WHERE inbox.tenant_id = '1000'
   AND inbox.source = 'qcs.batch.quality-gate.v1'
   AND (
       inbox.event_id = :'event_id'
       OR inbox.event_id IN (
           SELECT gate.source_event_id FROM bpi.bpi_quality_gates gate
            WHERE gate.tenant_id = '1000' AND gate.batch_id = :'batch_id'::uuid
       )
   );
DELETE FROM bpi.bpi_api_idempotency
 WHERE tenant_id = '1000'
   AND resource_path LIKE '%/' || :'batch_id' || '/%';
DELETE FROM bpi.bpi_quality_links
 WHERE tenant_id = '1000' AND batch_id = :'batch_id'::uuid;
DELETE FROM bpi.bpi_wms_inbound_links
 WHERE tenant_id = '1000' AND batch_id = :'batch_id'::uuid;
DELETE FROM bpi.bpi_quality_gates
 WHERE tenant_id = '1000' AND batch_id = :'batch_id'::uuid;
DELETE FROM bpi.bpi_outbox_events
 WHERE tenant_id = '1000' AND aggregate_id = :'batch_id'::uuid;
DELETE FROM bpi.bpi_audit_events
 WHERE tenant_id = '1000' AND object_id = :'batch_id'::uuid;
DELETE FROM bpi.bpi_batch_state_events
 WHERE tenant_id = '1000' AND batch_id = :'batch_id'::uuid;
DELETE FROM bpi.bpi_batch_instances
 WHERE tenant_id = '1000' AND id = :'batch_id'::uuid;
DELETE FROM bpi.bpi_feature_flags
 WHERE tenant_id = '1000' AND id = :'qcs_flag_id'::uuid;

COMMIT;

SELECT (
    (SELECT count(*) FROM bpi.bpi_inbox_events
      WHERE tenant_id = '1000' AND source = 'qcs.batch.quality-gate.v1'
        AND event_id = :'event_id')
  + (SELECT count(*) FROM bpi.bpi_api_idempotency
      WHERE tenant_id = '1000' AND resource_path LIKE '%/' || :'batch_id' || '/%')
  + (SELECT count(*) FROM bpi.bpi_quality_links
      WHERE tenant_id = '1000' AND batch_id = :'batch_id'::uuid)
  + (SELECT count(*) FROM bpi.bpi_wms_inbound_links
      WHERE tenant_id = '1000' AND batch_id = :'batch_id'::uuid)
  + (SELECT count(*) FROM bpi.bpi_quality_gates
      WHERE tenant_id = '1000' AND batch_id = :'batch_id'::uuid)
  + (SELECT count(*) FROM bpi.bpi_outbox_events
      WHERE tenant_id = '1000' AND aggregate_id = :'batch_id'::uuid)
  + (SELECT count(*) FROM bpi.bpi_audit_events
      WHERE tenant_id = '1000' AND object_id = :'batch_id'::uuid)
  + (SELECT count(*) FROM bpi.bpi_batch_state_events
      WHERE tenant_id = '1000' AND batch_id = :'batch_id'::uuid)
  + (SELECT count(*) FROM bpi.bpi_batch_instances
      WHERE tenant_id = '1000' AND id = :'batch_id'::uuid)
  + (SELECT count(*) FROM bpi.bpi_feature_flags
      WHERE tenant_id = '1000' AND id = :'qcs_flag_id'::uuid)
) AS residual_rows;
