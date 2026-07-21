\set ON_ERROR_STOP on

SELECT json_build_object(
    'blue', (
        SELECT json_build_object(
            'id', id, 'documentNo', document_no, 'status', status,
            'sourceDocumentId', source_document_id, 'idempotencyKey', idempotency_key,
            'warehouseCode', warehouse_code, 'qualityStatus', quality_status
        ) FROM wms_stock_documents
         WHERE tenant_id = '1000' AND document_type = 'COMPLETION_INBOUND'
           AND source_system = 'BPI' AND source_document_id = :'blue_outbox_id'
    ),
    'red', (
        SELECT json_build_object(
            'id', id, 'documentNo', document_no, 'status', status,
            'sourceDocumentId', source_document_id, 'idempotencyKey', idempotency_key,
            'warehouseCode', warehouse_code, 'qualityStatus', quality_status,
            'reversalOfDocumentId', reversal_of_document_id
        ) FROM wms_stock_documents
         WHERE tenant_id = '1000' AND document_type = 'COMPLETION_INBOUND_REVERSAL'
           AND source_system = 'BPI' AND source_document_id = :'red_outbox_id'
    ),
    'transactions', COALESCE((
        SELECT json_agg(json_build_object(
            'type', transaction_type, 'sourceDocumentId', source_document_id,
            'onHandDelta', on_hand_delta, 'availableDelta', available_delta,
            'balanceOnHand', balance_on_hand, 'balanceAvailable', balance_available
        ) ORDER BY id)
          FROM wms_inventory_transactions
         WHERE tenant_id = '1000' AND source_system = 'BPI'
           AND source_document_id IN (:'blue_outbox_id', :'red_outbox_id')
    ), '[]'::json),
    'stock', (
        SELECT json_build_object(
            'onHand', on_hand_quantity, 'available', available_quantity, 'hold', hold_quantity
        ) FROM wms_batch_stocks
         WHERE tenant_id = '1000' AND material_code = :'marker' || '_MATERIAL'
           AND batch_no = :'marker' AND production_batch_no = :'marker'
           AND warehouse_code = 'WARE-E2E' AND location_code = 'LOC-E2E'
    )
) AS evidence;

SELECT (
    SELECT count(*) = 1
      FROM wms_stock_documents blue
      JOIN wms_stock_document_lines blue_line ON blue_line.document_id = blue.id
      JOIN wms_stock_documents red ON red.reversal_of_document_id = blue.id
      JOIN wms_stock_document_lines red_line ON red_line.document_id = red.id
     WHERE blue.tenant_id = '1000'
       AND blue.document_type = 'COMPLETION_INBOUND'
       AND blue.source_system = 'BPI' AND blue.source_document_id = :'blue_outbox_id'
       AND blue.idempotency_key = :'marker' || '|WMS|1'
       AND blue.document_no = :'blue_document_no' AND blue.status = 'REVERSED'
       AND blue.quality_status = 'QUALIFIED' AND blue.warehouse_code = 'WARE-E2E'
       AND red.tenant_id = blue.tenant_id
       AND red.document_type = 'COMPLETION_INBOUND_REVERSAL'
       AND red.source_system = 'BPI' AND red.source_document_id = :'red_outbox_id'
       AND red.idempotency_key = :'red_idempotency_key'
       AND red.document_no = :'red_document_no' AND red.status = 'POSTED'
       AND red.quality_status = 'QUALIFIED' AND red.warehouse_code = 'WARE-E2E'
       AND blue_line.source_line_id = :'blue_outbox_id' || ':1'
       AND red_line.source_line_id = :'red_outbox_id' || ':1'
       AND blue_line.material_code = :'marker' || '_MATERIAL'
       AND red_line.material_code = blue_line.material_code
       AND blue_line.batch_no = :'marker' AND red_line.batch_no = blue_line.batch_no
       AND blue_line.production_batch_no = :'marker'
       AND red_line.production_batch_no = blue_line.production_batch_no
       AND blue_line.location_code = 'LOC-E2E' AND red_line.location_code = 'LOC-E2E'
       AND blue_line.quantity = 12.345000 AND red_line.quantity = 12.345000
       AND blue_line.unit_code = 'kg' AND red_line.unit_code = 'kg'
) AND (
    SELECT count(*) = 2
      FROM wms_inventory_transactions
     WHERE tenant_id = '1000' AND source_system = 'BPI'
       AND source_document_id IN (:'blue_outbox_id', :'red_outbox_id')
) AND (
    SELECT count(*) = 1
      FROM wms_inventory_transactions
     WHERE tenant_id = '1000' AND source_system = 'BPI'
       AND source_document_id = :'blue_outbox_id'
       AND transaction_type = 'COMPLETION_INBOUND'
       AND on_hand_delta = 12.345000 AND available_delta = 12.345000
) AND (
    SELECT count(*) = 1
      FROM wms_inventory_transactions
     WHERE tenant_id = '1000' AND source_system = 'BPI'
       AND source_document_id = :'red_outbox_id'
       AND transaction_type = 'COMPLETION_INBOUND_REVERSAL'
       AND on_hand_delta = -12.345000 AND available_delta = -12.345000
) AND (
    SELECT count(*) = 1
      FROM wms_batch_stocks
     WHERE tenant_id = '1000' AND material_code = :'marker' || '_MATERIAL'
       AND batch_no = :'marker' AND production_batch_no = :'marker'
       AND warehouse_code = 'WARE-E2E' AND location_code = 'LOC-E2E'
       AND on_hand_quantity = 0 AND available_quantity = 0 AND hold_quantity = 0
) AS acceptance_pass
\gset

\if :acceptance_pass
\else
    \echo 'Formal WMS roundtrip material verification failed'
    \quit 8
\endif
