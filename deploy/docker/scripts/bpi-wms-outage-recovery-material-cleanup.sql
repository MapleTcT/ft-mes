\set ON_ERROR_STOP on

BEGIN;

DELETE FROM wms_batch_stocks
 WHERE tenant_id = '1000'
   AND material_code = :'marker' || '_MATERIAL'
   AND batch_no = :'marker'
   AND warehouse_code = 'WARE-E2E'
   AND location_code = 'LOC-E2E';
DELETE FROM wms_stock_documents
 WHERE tenant_id = '1000'
   AND document_type = 'COMPLETION_INBOUND'
   AND source_system = 'BPI'
   AND source_document_id = :'outbox_id';

COMMIT;

SELECT (
    (SELECT count(*) FROM wms_stock_documents
      WHERE tenant_id = '1000' AND source_system = 'BPI'
        AND source_document_id = :'outbox_id')
  + (SELECT count(*) FROM wms_stock_document_lines
      WHERE tenant_id = '1000' AND source_system = 'BPI'
        AND source_line_id = :'outbox_id' || ':1')
  + (SELECT count(*) FROM wms_inventory_transactions
      WHERE tenant_id = '1000' AND source_system = 'BPI'
        AND source_document_id = :'outbox_id')
  + (SELECT count(*) FROM wms_batch_stocks
      WHERE tenant_id = '1000'
        AND material_code = :'marker' || '_MATERIAL'
        AND batch_no = :'marker'
        AND warehouse_code = 'WARE-E2E'
        AND location_code = 'LOC-E2E')
) AS residual_rows
\gset

\if :residual_rows
    \echo 'WMS outage recovery material cleanup left residual rows:' :residual_rows
    \quit 6
\else
    \echo 'WMS outage recovery material cleanup residualRows=0'
\endif
