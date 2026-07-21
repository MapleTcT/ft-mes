\set ON_ERROR_STOP on

BEGIN;

DELETE FROM bpi.bpi_api_idempotency
 WHERE tenant_id = '1000'
   AND resource_path = '/bpi/v1/batches/' || :'batch_id' || '/wms/reversal';
DELETE FROM bpi.bpi_inbox_events
 WHERE tenant_id = '1000'
   AND idempotency_key LIKE 'WMS_COMPLETION_INBOUND_REVERSAL|1000|' || :'batch_id' || '|%';
DELETE FROM bpi.bpi_batch_state_events
 WHERE tenant_id = '1000' AND batch_id = :'batch_id'::uuid;
DELETE FROM bpi.bpi_audit_events
 WHERE tenant_id = '1000' AND object_id = :'batch_id'::uuid;
DELETE FROM bpi.bpi_wms_inbound_reversal_tasks
 WHERE tenant_id = '1000' AND batch_id = :'batch_id'::uuid;
DELETE FROM bpi.bpi_quality_links
 WHERE tenant_id = '1000' AND id = :'quality_link_id'::uuid;
DELETE FROM bpi.bpi_wms_inbound_links
 WHERE tenant_id = '1000' AND id = :'wms_link_id'::uuid;
DELETE FROM bpi.bpi_quality_gates
 WHERE tenant_id = '1000' AND id = :'quality_gate_id'::uuid;
DELETE FROM bpi.bpi_outbox_events
 WHERE tenant_id = '1000' AND aggregate_id = :'batch_id'::uuid;
UPDATE bpi.bpi_batch_candidates
   SET batch_id = NULL,
       updated_at = now()
 WHERE tenant_id = '1000' AND batch_id = :'batch_id'::uuid;
DELETE FROM bpi.bpi_batch_instances
 WHERE tenant_id = '1000' AND id = :'batch_id'::uuid;
DELETE FROM bpi.bpi_feature_flags
 WHERE tenant_id = '1000'
   AND id IN (:'commands_flag_id'::uuid, :'wms_flag_id'::uuid);

COMMIT;

SELECT (
    (SELECT count(*) FROM bpi.bpi_api_idempotency
      WHERE tenant_id = '1000'
        AND resource_path = '/bpi/v1/batches/' || :'batch_id' || '/wms/reversal')
  + (SELECT count(*) FROM bpi.bpi_inbox_events
      WHERE tenant_id = '1000'
        AND idempotency_key LIKE 'WMS_COMPLETION_INBOUND_REVERSAL|1000|' || :'batch_id' || '|%')
  + (SELECT count(*) FROM bpi.bpi_batch_state_events
      WHERE tenant_id = '1000' AND batch_id = :'batch_id'::uuid)
  + (SELECT count(*) FROM bpi.bpi_audit_events
      WHERE tenant_id = '1000' AND object_id = :'batch_id'::uuid)
  + (SELECT count(*) FROM bpi.bpi_wms_inbound_reversal_tasks
      WHERE tenant_id = '1000' AND batch_id = :'batch_id'::uuid)
  + (SELECT count(*) FROM bpi.bpi_quality_links
      WHERE tenant_id = '1000' AND id = :'quality_link_id'::uuid)
  + (SELECT count(*) FROM bpi.bpi_wms_inbound_links
      WHERE tenant_id = '1000' AND id = :'wms_link_id'::uuid)
  + (SELECT count(*) FROM bpi.bpi_quality_gates
      WHERE tenant_id = '1000' AND id = :'quality_gate_id'::uuid)
  + (SELECT count(*) FROM bpi.bpi_outbox_events
      WHERE tenant_id = '1000' AND aggregate_id = :'batch_id'::uuid)
  + (SELECT count(*) FROM bpi.bpi_batch_instances
      WHERE tenant_id = '1000' AND id = :'batch_id'::uuid)
  + (SELECT count(*) FROM bpi.bpi_feature_flags
      WHERE tenant_id = '1000'
        AND id IN (:'commands_flag_id'::uuid, :'wms_flag_id'::uuid))
) AS residual_rows
\gset

\if :residual_rows
    \echo 'BPI WMS inbound reversal acceptance cleanup left residual rows:' :residual_rows
    \quit 5
\else
    \echo 'BPI WMS inbound reversal acceptance cleanup residualRows=0'
\endif
