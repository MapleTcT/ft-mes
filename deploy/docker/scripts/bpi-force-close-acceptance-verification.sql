\set ON_ERROR_STOP on

SELECT json_build_object(
    'marker', :'marker',
    'batch', (
        SELECT json_build_object(
            'id', id,
            'batchNo', batch_no,
            'state', state,
            'revision', revision,
            'startTime', start_time,
            'endTime', end_time,
            'updatedAt', updated_at
        )
          FROM bpi.bpi_batch_instances
         WHERE tenant_id = '1000'
           AND id = :'batch_id'::uuid
    ),
    'forceCloseTask', (
        SELECT json_build_object(
            'id', id,
            'state', state,
            'revision', revision,
            'sourceState', source_state,
            'boundaryTime', boundary_time,
            'requestedBy', requested_by,
            'requestedAt', requested_at,
            'requestReason', request_reason,
            'decidedBy', decided_by,
            'decidedAt', decided_at,
            'decisionReason', decision_reason
        )
          FROM bpi.bpi_batch_force_close_tasks
         WHERE tenant_id = '1000'
           AND batch_id = :'batch_id'::uuid
         ORDER BY requested_at DESC, id DESC
         LIMIT 1
    ),
    'stateEvents', COALESCE((
        SELECT json_agg(json_build_object(
            'revision', revision,
            'action', action,
            'fromState', from_state,
            'toState', to_state,
            'reason', reason,
            'actorId', actor_id,
            'eventTime', event_time,
            'traceId', trace_id
        ) ORDER BY revision)
          FROM bpi.bpi_batch_state_events
         WHERE tenant_id = '1000'
           AND batch_id = :'batch_id'::uuid
    ), '[]'::json),
    'auditEvents', COALESCE((
        SELECT json_agg(json_build_object(
            'action', action,
            'actorId', actor_id,
            'beforeRevision', before_revision,
            'afterRevision', after_revision,
            'reason', reason,
            'traceId', trace_id
        ) ORDER BY created_at, id)
          FROM bpi.bpi_audit_events
         WHERE tenant_id = '1000'
           AND object_id = :'batch_id'::uuid
           AND action IN ('BATCH_FORCE_CLOSE_REQUESTED', 'BATCH_FORCE_CLOSED')
    ), '[]'::json),
    'idempotencyRows', (
        SELECT count(*)
          FROM bpi.bpi_api_idempotency
         WHERE tenant_id = '1000'
           AND resource_path = '/bpi/v1/batches/' || :'batch_id' || '/force-close'
    ),
    'qualityGateRows', (
        SELECT count(*) FROM bpi.bpi_quality_gates
         WHERE tenant_id = '1000' AND batch_id = :'batch_id'::uuid
    ),
    'wmsLinkRows', (
        SELECT count(*) FROM bpi.bpi_wms_inbound_links
         WHERE tenant_id = '1000' AND batch_id = :'batch_id'::uuid
    ),
    'outboxRows', (
        SELECT count(*) FROM bpi.bpi_outbox_events
         WHERE tenant_id = '1000' AND aggregate_id = :'batch_id'::uuid
    )
);
