\set ON_ERROR_STOP on

SELECT json_build_object(
    'marker', :'marker',
    'batch', (
        SELECT json_build_object(
            'id', id, 'state', state, 'revision', revision,
            'qualityGate', quality_gate, 'wmsStatus', wms_status,
            'quantity', quantity, 'quantityUnit', quantity_unit, 'isShadow', is_shadow
        )
          FROM bpi.bpi_batch_instances
         WHERE tenant_id = '1000' AND id = :'batch_id'::uuid
    ),
    'blue', (
        SELECT json_build_object(
            'linkId', link.id, 'commandEventId', link.command_event_id,
            'idempotencyKey', link.idempotency_key, 'status', link.status,
            'revision', link.revision, 'receiptEventId', link.receipt_event_id,
            'documentId', link.document_id, 'outboxStatus', event.status,
            'outboxRevision', event.revision, 'topic', event.topic,
            'payloadSha256', encode(sha256(event.payload), 'hex')
        )
          FROM bpi.bpi_wms_inbound_links link
          JOIN bpi.bpi_outbox_events event
            ON event.tenant_id = link.tenant_id AND event.id = link.command_event_id
         WHERE link.tenant_id = '1000' AND link.batch_id = :'batch_id'::uuid
    ),
    'red', (
        SELECT json_build_object(
            'taskId', task.id, 'state', task.state, 'revision', task.revision,
            'requestedBy', task.requested_by, 'decidedBy', task.decided_by,
            'originalCommandEventId', task.original_command_event_id,
            'originalIdempotencyKey', task.original_idempotency_key,
            'originalDocumentId', task.original_document_id,
            'commandEventId', task.reversal_command_event_id,
            'idempotencyKey', task.reversal_idempotency_key,
            'receiptEventId', task.reversal_receipt_event_id,
            'documentId', task.reversal_document_id,
            'errorCode', task.error_code, 'outboxStatus', event.status,
            'outboxRevision', event.revision, 'topic', event.topic,
            'payloadSha256', encode(sha256(event.payload), 'hex')
        )
          FROM bpi.bpi_wms_inbound_reversal_tasks task
          JOIN bpi.bpi_outbox_events event
            ON event.tenant_id = task.tenant_id
           AND event.id = task.reversal_command_event_id
         WHERE task.tenant_id = '1000' AND task.batch_id = :'batch_id'::uuid
         ORDER BY task.requested_at DESC, task.id DESC
         LIMIT 1
    ),
    'stateEvents', COALESCE((
        SELECT json_agg(json_build_object(
            'revision', revision, 'action', action,
            'fromState', from_state, 'toState', to_state, 'actorId', actor_id
        ) ORDER BY revision)
          FROM bpi.bpi_batch_state_events
         WHERE tenant_id = '1000' AND batch_id = :'batch_id'::uuid
    ), '[]'::json),
    'auditActions', COALESCE((
        SELECT json_agg(action ORDER BY created_at, id)
          FROM bpi.bpi_audit_events
         WHERE tenant_id = '1000' AND object_id = :'batch_id'::uuid
    ), '[]'::json),
    'blueInboxRows', (
        SELECT count(*) FROM bpi.bpi_inbox_events
         WHERE tenant_id = '1000' AND source = 'wms.completion-inbound.receipt.v1'
           AND idempotency_key = :'marker' || '|WMS|1'
    ),
    'redInboxRows', (
        SELECT count(*) FROM bpi.bpi_inbox_events
         WHERE tenant_id = '1000'
           AND idempotency_key LIKE 'WMS_COMPLETION_INBOUND_REVERSAL|1000|'
               || :'batch_id' || '|%'
    ),
    'reversalApiIdempotencyRows', (
        SELECT count(*) FROM bpi.bpi_api_idempotency
         WHERE tenant_id = '1000'
           AND resource_path = '/bpi/v1/batches/' || :'batch_id' || '/wms/reversal'
           AND state = 'COMPLETED'
    )
) AS evidence;

SELECT (
    SELECT count(*) = 1
      FROM bpi.bpi_batch_instances batch
      JOIN bpi.bpi_wms_inbound_links blue
        ON blue.tenant_id = batch.tenant_id AND blue.batch_id = batch.id
      JOIN bpi.bpi_outbox_events blue_event
        ON blue_event.tenant_id = blue.tenant_id AND blue_event.id = blue.command_event_id
      JOIN bpi.bpi_wms_inbound_reversal_tasks red
        ON red.tenant_id = batch.tenant_id AND red.batch_id = batch.id
      JOIN bpi.bpi_outbox_events red_event
        ON red_event.tenant_id = red.tenant_id AND red_event.id = red.reversal_command_event_id
     WHERE batch.tenant_id = '1000' AND batch.id = :'batch_id'::uuid
       AND batch.batch_no = :'marker'
       AND batch.state = 'INBOUND_REVERSED' AND batch.revision = 7
       AND batch.quality_gate = 'ACCEPTED' AND batch.wms_status = 'REVERSED'
       AND batch.is_shadow = false
       AND blue.command_event_id = :'outbox_id'::uuid
       AND blue.idempotency_key = :'marker' || '|WMS|1'
       AND blue.status = 'ACCEPTED' AND blue.receipt_event_id IS NOT NULL
       AND blue.document_id IS NOT NULL
       AND blue_event.status = 'PUBLISHED' AND blue_event.topic = :'blue_command_topic'
       AND red.state = 'COMPLETED' AND red.revision = 3
       AND red.requested_by <> red.decided_by
       AND red.original_command_event_id = blue.command_event_id
       AND red.original_idempotency_key = blue.idempotency_key
       AND red.original_document_id = blue.document_id
       AND red.reversal_command_event_id <> blue.command_event_id
       AND red.reversal_receipt_event_id IS NOT NULL
       AND red.reversal_document_id IS NOT NULL
       AND red.error_code IS NULL
       AND red_event.status = 'PUBLISHED' AND red_event.topic = :'red_command_topic'
) AND (
    SELECT array_agg(action ORDER BY revision) = ARRAY[
        'WMS_INBOUND_ACCEPTED',
        'WMS_INBOUND_REVERSAL_REQUESTED',
        'WMS_INBOUND_REVERSAL_APPROVED',
        'WMS_INBOUND_REVERSAL_ACCEPTED'
    ]::varchar[]
      FROM bpi.bpi_batch_state_events
     WHERE tenant_id = '1000' AND batch_id = :'batch_id'::uuid
) AND (
    SELECT count(*) = 1 FROM bpi.bpi_inbox_events
     WHERE tenant_id = '1000' AND source = 'wms.completion-inbound.receipt.v1'
       AND idempotency_key = :'marker' || '|WMS|1'
) AND (
    SELECT count(*) = 1 FROM bpi.bpi_inbox_events
     WHERE tenant_id = '1000'
       AND idempotency_key LIKE 'WMS_COMPLETION_INBOUND_REVERSAL|1000|'
           || :'batch_id' || '|%'
) AND (
    SELECT count(*) = 2 FROM bpi.bpi_api_idempotency
     WHERE tenant_id = '1000'
       AND resource_path = '/bpi/v1/batches/' || :'batch_id' || '/wms/reversal'
       AND state = 'COMPLETED'
) AS acceptance_pass
\gset

\if :acceptance_pass
\else
    \echo 'Formal WMS roundtrip BPI verification failed'
    \quit 7
\endif
