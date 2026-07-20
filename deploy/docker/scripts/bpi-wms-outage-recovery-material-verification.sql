\set ON_ERROR_STOP on

SELECT json_build_object(
    'documents', (SELECT count(*) FROM wms_stock_documents
                   WHERE tenant_id = '1000' AND document_type = 'COMPLETION_INBOUND'
                     AND source_system = 'BPI' AND source_document_id = :'outbox_id'),
    'lines', (SELECT count(*) FROM wms_stock_document_lines line
               JOIN wms_stock_documents document ON document.id = line.document_id
              WHERE document.tenant_id = '1000' AND document.source_system = 'BPI'
                AND document.source_document_id = :'outbox_id'),
    'transactions', (SELECT count(*) FROM wms_inventory_transactions transaction_row
                      JOIN wms_stock_documents document ON document.id = transaction_row.document_id
                     WHERE document.tenant_id = '1000' AND document.source_system = 'BPI'
                       AND document.source_document_id = :'outbox_id'),
    'stockRows', (SELECT count(*) FROM wms_batch_stocks
                   WHERE tenant_id = '1000' AND material_code = :'marker' || '_MATERIAL'
                     AND batch_no = :'marker' AND warehouse_code = 'WARE-E2E'
                     AND location_code = 'LOC-E2E'),
    'documentId', (SELECT document_no FROM wms_stock_documents
                    WHERE tenant_id = '1000' AND source_system = 'BPI'
                      AND source_document_id = :'outbox_id' LIMIT 1),
    'onHandQuantity', (SELECT on_hand_quantity FROM wms_batch_stocks
                        WHERE tenant_id = '1000' AND material_code = :'marker' || '_MATERIAL'
                          AND batch_no = :'marker' AND warehouse_code = 'WARE-E2E'
                          AND location_code = 'LOC-E2E' LIMIT 1)
) AS evidence;

SELECT (
    SELECT count(*) = 1
      FROM wms_stock_documents document
      JOIN wms_stock_document_lines line ON line.document_id = document.id
     WHERE document.tenant_id = '1000'
       AND document.document_type = 'COMPLETION_INBOUND'
       AND document.source_system = 'BPI'
       AND document.source_document_id = :'outbox_id'
       AND document.idempotency_key = :'marker' || '|WMS|1'
       AND document.status = 'POSTED'
       AND document.quality_status = 'QUALIFIED'
       AND line.source_system = 'BPI'
       AND line.source_line_id = :'outbox_id' || ':1'
       AND line.material_code = :'marker' || '_MATERIAL'
       AND line.batch_no = :'marker'
       AND line.production_batch_no = :'marker'
       AND line.warehouse_code = 'WARE-E2E'
       AND line.location_code = 'LOC-E2E'
       AND line.quantity = 12.345000
       AND line.unit_code = 'kg'
) AND (
    SELECT count(*) = 1
      FROM wms_inventory_transactions transaction_row
      JOIN wms_stock_documents document ON document.id = transaction_row.document_id
     WHERE document.tenant_id = '1000'
       AND document.source_system = 'BPI'
       AND document.source_document_id = :'outbox_id'
) AND (
    SELECT count(*) = 1
      FROM wms_batch_stocks stock
     WHERE stock.tenant_id = '1000'
       AND stock.material_code = :'marker' || '_MATERIAL'
       AND stock.batch_no = :'marker'
       AND stock.warehouse_code = 'WARE-E2E'
       AND stock.location_code = 'LOC-E2E'
       AND stock.on_hand_quantity = 12.345000
       AND stock.available_quantity = 12.345000
       AND stock.hold_quantity = 0
) AS acceptance_pass
\gset

\if :acceptance_pass
\else
    \echo 'WMS outage recovery material verification failed'
    \quit 4
\endif
