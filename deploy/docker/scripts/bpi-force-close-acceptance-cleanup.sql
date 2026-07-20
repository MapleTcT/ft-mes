\set ON_ERROR_STOP on

BEGIN;

DELETE FROM bpi.bpi_quality_links
 WHERE tenant_id = '1000'
   AND batch_id = :'batch_id'::uuid;
DELETE FROM bpi.bpi_wms_inbound_links
 WHERE tenant_id = '1000'
   AND batch_id = :'batch_id'::uuid;
DELETE FROM bpi.bpi_quality_gates
 WHERE tenant_id = '1000'
   AND batch_id = :'batch_id'::uuid;
DELETE FROM bpi.bpi_outbox_events
 WHERE tenant_id = '1000'
   AND aggregate_id = :'batch_id'::uuid;
DELETE FROM bpi.bpi_boundary_evidence
 WHERE tenant_id = '1000'
   AND batch_id = :'batch_id'::uuid;
DELETE FROM bpi.bpi_batch_state_events
 WHERE tenant_id = '1000'
   AND batch_id = :'batch_id'::uuid;
DELETE FROM bpi.bpi_batch_force_close_tasks
 WHERE tenant_id = '1000'
   AND batch_id = :'batch_id'::uuid;
DELETE FROM bpi.bpi_api_idempotency
 WHERE tenant_id = '1000'
   AND resource_path = '/bpi/v1/batches/' || :'batch_id' || '/force-close';
DELETE FROM bpi.bpi_audit_events
 WHERE tenant_id = '1000'
   AND object_id = :'batch_id'::uuid;
UPDATE bpi.bpi_batch_candidates
   SET batch_id = NULL,
       updated_at = now()
 WHERE tenant_id = '1000'
   AND batch_id = :'batch_id'::uuid;
DELETE FROM bpi.bpi_batch_instances
 WHERE tenant_id = '1000'
   AND id = :'batch_id'::uuid;
DELETE FROM bpi.bpi_feature_flags
 WHERE tenant_id = '1000'
   AND id = :'commands_flag_id'::uuid;

COMMIT;

SELECT (
    (SELECT count(*) FROM bpi.bpi_batch_instances
      WHERE tenant_id = '1000' AND id = :'batch_id'::uuid)
  + (SELECT count(*) FROM bpi.bpi_batch_force_close_tasks
      WHERE tenant_id = '1000' AND batch_id = :'batch_id'::uuid)
  + (SELECT count(*) FROM bpi.bpi_batch_state_events
      WHERE tenant_id = '1000' AND batch_id = :'batch_id'::uuid)
  + (SELECT count(*) FROM bpi.bpi_boundary_evidence
      WHERE tenant_id = '1000' AND batch_id = :'batch_id'::uuid)
  + (SELECT count(*) FROM bpi.bpi_quality_gates
      WHERE tenant_id = '1000' AND batch_id = :'batch_id'::uuid)
  + (SELECT count(*) FROM bpi.bpi_wms_inbound_links
      WHERE tenant_id = '1000' AND batch_id = :'batch_id'::uuid)
  + (SELECT count(*) FROM bpi.bpi_outbox_events
      WHERE tenant_id = '1000' AND aggregate_id = :'batch_id'::uuid)
  + (SELECT count(*) FROM bpi.bpi_api_idempotency
      WHERE tenant_id = '1000'
        AND resource_path = '/bpi/v1/batches/' || :'batch_id' || '/force-close')
  + (SELECT count(*) FROM bpi.bpi_audit_events
      WHERE tenant_id = '1000' AND object_id = :'batch_id'::uuid)
  + (SELECT count(*) FROM bpi.bpi_feature_flags
      WHERE tenant_id = '1000' AND id = :'commands_flag_id'::uuid)
) AS residual_rows
\gset

\if :residual_rows
    \echo 'BPI force-close acceptance cleanup left residual rows:' :residual_rows
    \quit 5
\else
    \echo 'BPI force-close acceptance cleanup residualRows=0'
\endif
