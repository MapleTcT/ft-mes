\set ON_ERROR_STOP on

SELECT json_build_object(
    'batch', (
        SELECT json_build_object(
            'id', id,
            'batchNo', batch_no,
            'orderId', order_id,
            'state', state,
            'revision', revision,
            'shadow', is_shadow,
            'qualityGate', quality_gate,
            'wmsStatus', wms_status,
            'materialCode', material_code,
            'quantity', quantity,
            'quantityUnit', quantity_unit
        )
          FROM bpi.bpi_batch_instances
         WHERE tenant_id = '1000' AND id = :'batch_id'::uuid
    ),
    'qualityGateCount', (
        SELECT count(*) FROM bpi.bpi_quality_gates
         WHERE tenant_id = '1000' AND batch_id = :'batch_id'::uuid
    ),
    'qualityGate', (
        SELECT json_build_object(
            'id', id,
            'externalGateId', external_gate_id,
            'externalRevision', external_revision,
            'sourceEventId', source_event_id,
            'payloadChecksum', payload_checksum,
            'state', state,
            'releaseQuantity', release_quantity,
            'quantityUnit', quantity_unit,
            'materialCode', material_code
        )
          FROM bpi.bpi_quality_gates
         WHERE tenant_id = '1000' AND batch_id = :'batch_id'::uuid
         ORDER BY updated_at DESC
         LIMIT 1
    ),
    'qualityLinkCount', (
        SELECT count(*) FROM bpi.bpi_quality_links
         WHERE tenant_id = '1000' AND batch_id = :'batch_id'::uuid
    ),
    'inboxCount', (
        SELECT count(*) FROM bpi.bpi_inbox_events inbox
         WHERE inbox.tenant_id = '1000'
           AND inbox.source = 'qcs.batch.quality-gate.v1'
           AND inbox.event_id IN (
               SELECT gate.source_event_id FROM bpi.bpi_quality_gates gate
                WHERE gate.tenant_id = '1000' AND gate.batch_id = :'batch_id'::uuid
           )
    ),
    'stateEventCount', (
        SELECT count(*) FROM bpi.bpi_batch_state_events
         WHERE tenant_id = '1000' AND batch_id = :'batch_id'::uuid
    ),
    'stateActions', (
        SELECT coalesce(json_agg(action ORDER BY revision), '[]'::json)
          FROM bpi.bpi_batch_state_events
         WHERE tenant_id = '1000' AND batch_id = :'batch_id'::uuid
    ),
    'auditCount', (
        SELECT count(*) FROM bpi.bpi_audit_events
         WHERE tenant_id = '1000' AND object_id = :'batch_id'::uuid
    ),
    'wmsOutboxCount', (
        SELECT count(*) FROM bpi.bpi_outbox_events
         WHERE tenant_id = '1000' AND aggregate_id = :'batch_id'::uuid
    )
);
