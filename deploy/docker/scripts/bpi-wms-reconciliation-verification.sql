\set ON_ERROR_STOP on

SELECT json_build_object(
    'marker', :'marker',
    'batchId', batch.id,
    'batchState', batch.state,
    'batchRevision', batch.revision,
    'wmsStatus', batch.wms_status,
    'commandEventId', link.command_event_id,
    'wmsIdempotencyKey', link.idempotency_key,
    'linkStatus', link.status,
    'linkRevision', link.revision,
    'outboxStatus', event.status,
    'outboxRevision', event.revision,
    'manualRetryCount', event.manual_retry_count,
    'totalAttemptCount', event.total_attempt_count,
    'payloadText', convert_from(event.payload, 'UTF8'),
    'lastRequeuedBy', event.last_requeued_by,
    'lastRequeuedAt', event.last_requeued_at,
    'auditRows', (
        SELECT count(*) FROM bpi.bpi_audit_events audit
         WHERE audit.tenant_id = '1000'
           AND audit.object_type = 'WMS_INBOUND_LINK'
           AND audit.object_id = :'wms_link_id'::uuid
           AND audit.action = 'WMS_INBOUND_RECONCILIATION_QUEUED'
    ),
    'idempotencyRows', (
        SELECT count(*) FROM bpi.bpi_api_idempotency command
         WHERE command.tenant_id = '1000'
           AND command.resource_path = '/bpi/v1/batches/' || :'batch_id' || '/wms/reconcile'
           AND command.state = 'COMPLETED'
    ),
    'outboxRows', (
        SELECT count(*) FROM bpi.bpi_outbox_events duplicate
         WHERE duplicate.tenant_id = '1000'
           AND duplicate.aggregate_id = :'batch_id'::uuid
           AND duplicate.event_type = 'WMS_COMPLETION_INBOUND_COMMAND'
    )
) AS evidence
  FROM bpi.bpi_batch_instances batch
  JOIN bpi.bpi_wms_inbound_links link
    ON link.tenant_id = batch.tenant_id AND link.batch_id = batch.id
  JOIN bpi.bpi_outbox_events event
    ON event.tenant_id = link.tenant_id AND event.id = link.command_event_id
 WHERE batch.tenant_id = '1000'
   AND batch.id = :'batch_id'::uuid;

SELECT (
    SELECT count(*) = 1
      FROM bpi.bpi_batch_instances batch
      JOIN bpi.bpi_wms_inbound_links link
        ON link.tenant_id = batch.tenant_id AND link.batch_id = batch.id
      JOIN bpi.bpi_outbox_events event
        ON event.tenant_id = link.tenant_id AND event.id = link.command_event_id
     WHERE batch.tenant_id = '1000'
       AND batch.id = :'batch_id'::uuid
       AND batch.batch_no = :'marker'
       AND batch.state = 'RELEASED'
       AND batch.wms_status = 'PENDING'
       AND link.command_event_id = :'outbox_id'::uuid
       AND link.idempotency_key = :'marker' || '|WMS|1'
       AND link.status = 'PENDING'
       AND link.revision = 2
       AND event.status = 'PENDING'
       AND event.revision = 2
       AND event.manual_retry_count = 1
       AND event.last_requeued_at IS NOT NULL
       AND event.last_requeued_by IS NOT NULL
       AND event.payload = convert_to(:'marker', 'UTF8')
) AND (
    SELECT count(*) = 1
      FROM bpi.bpi_audit_events audit
     WHERE audit.tenant_id = '1000'
       AND audit.object_type = 'WMS_INBOUND_LINK'
       AND audit.object_id = :'wms_link_id'::uuid
       AND audit.action = 'WMS_INBOUND_RECONCILIATION_QUEUED'
) AND (
    SELECT count(*) = 1
      FROM bpi.bpi_api_idempotency command
     WHERE command.tenant_id = '1000'
       AND command.resource_path = '/bpi/v1/batches/' || :'batch_id' || '/wms/reconcile'
       AND command.state = 'COMPLETED'
) AS acceptance_pass
\gset

\if :acceptance_pass
\else
    \echo 'WMS reconciliation persistence verification failed'
    \quit 3
\endif
