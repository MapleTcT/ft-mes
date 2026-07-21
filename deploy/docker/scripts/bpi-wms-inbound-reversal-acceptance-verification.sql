\set ON_ERROR_STOP on

SELECT json_build_object(
    'marker', :'marker',
    'batch', (
        SELECT json_build_object(
            'id', id,
            'batchNo', batch_no,
            'state', state,
            'revision', revision,
            'qualityGate', quality_gate,
            'wmsStatus', wms_status,
            'isShadow', is_shadow,
            'quantity', quantity,
            'quantityUnit', quantity_unit,
            'updatedAt', updated_at
        )
          FROM bpi.bpi_batch_instances
         WHERE tenant_id = '1000' AND id = :'batch_id'::uuid
    ),
    'reversalTask', (
        SELECT json_build_object(
            'id', id,
            'state', state,
            'revision', revision,
            'requestedBy', requested_by,
            'requestedAt', requested_at,
            'requestReason', request_reason,
            'decidedBy', decided_by,
            'decidedAt', decided_at,
            'decisionReason', decision_reason,
            'originalInboundLinkId', original_inbound_link_id,
            'originalCommandEventId', original_command_event_id,
            'originalIdempotencyKey', original_idempotency_key,
            'originalDocumentId', original_document_id,
            'reversalCommandEventId', reversal_command_event_id,
            'reversalIdempotencyKey', reversal_idempotency_key
        )
          FROM bpi.bpi_wms_inbound_reversal_tasks
         WHERE tenant_id = '1000' AND batch_id = :'batch_id'::uuid
         ORDER BY requested_at DESC, id DESC
         LIMIT 1
    ),
    'originalInbound', (
        SELECT json_build_object(
            'id', link.id,
            'commandEventId', link.command_event_id,
            'idempotencyKey', link.idempotency_key,
            'status', link.status,
            'receiptEventId', link.receipt_event_id,
            'documentId', link.document_id,
            'revision', link.revision,
            'payloadSha256', encode(sha256(event.payload), 'hex'),
            'outboxStatus', event.status,
            'outboxRevision', event.revision,
            'publishedAt', event.published_at
        )
          FROM bpi.bpi_wms_inbound_links link
          JOIN bpi.bpi_outbox_events event
            ON event.tenant_id = link.tenant_id AND event.id = link.command_event_id
         WHERE link.tenant_id = '1000' AND link.batch_id = :'batch_id'::uuid
    ),
    'reversalOutbox', (
        SELECT json_build_object(
            'id', id,
            'eventType', event_type,
            'topic', topic,
            'partitionKey', partition_key,
            'status', status,
            'revision', revision,
            'attemptCount', attempt_count,
            'payloadBytes', octet_length(payload),
            'payloadSha256', encode(sha256(payload), 'hex'),
            'headers', headers
        )
          FROM bpi.bpi_outbox_events
         WHERE tenant_id = '1000'
           AND aggregate_id = :'batch_id'::uuid
           AND event_type = 'WMS_COMPLETION_INBOUND_REVERSAL_COMMAND'
         ORDER BY created_at DESC, id DESC
         LIMIT 1
    ),
    'stateEvents', COALESCE((
        SELECT json_agg(json_build_object(
            'revision', revision,
            'action', action,
            'fromState', from_state,
            'toState', to_state,
            'actorId', actor_id,
            'traceId', trace_id
        ) ORDER BY revision)
          FROM bpi.bpi_batch_state_events
         WHERE tenant_id = '1000' AND batch_id = :'batch_id'::uuid
    ), '[]'::json),
    'auditEvents', COALESCE((
        SELECT json_agg(json_build_object(
            'action', action,
            'actorId', actor_id,
            'beforeRevision', before_revision,
            'afterRevision', after_revision,
            'traceId', trace_id
        ) ORDER BY created_at, id)
          FROM bpi.bpi_audit_events
         WHERE tenant_id = '1000'
           AND object_id = :'batch_id'::uuid
           AND action IN ('WMS_INBOUND_REVERSAL_REQUESTED', 'WMS_INBOUND_REVERSAL_APPROVED')
    ), '[]'::json),
    'idempotencyRows', (
        SELECT count(*) FROM bpi.bpi_api_idempotency
         WHERE tenant_id = '1000'
           AND resource_path = '/bpi/v1/batches/' || :'batch_id' || '/wms/reversal'
    ),
    'reversalOutboxRows', (
        SELECT count(*) FROM bpi.bpi_outbox_events
         WHERE tenant_id = '1000'
           AND aggregate_id = :'batch_id'::uuid
           AND event_type = 'WMS_COMPLETION_INBOUND_REVERSAL_COMMAND'
    ),
    'reversalInboxRows', (
        SELECT count(*) FROM bpi.bpi_inbox_events
         WHERE tenant_id = '1000'
           AND idempotency_key LIKE 'WMS_COMPLETION_INBOUND_REVERSAL|1000|' || :'batch_id' || '|%'
    )
);
