\set ON_ERROR_STOP on

BEGIN;

DELETE FROM wms_stock_documents document
 WHERE document.tenant_id = '1000'
   AND document.document_type = 'COMPLETION_INBOUND_REVERSAL'
   AND document.source_system = 'BPI'
   AND EXISTS (
       SELECT 1 FROM wms_stock_document_lines line
        WHERE line.document_id = document.id
          AND line.batch_no = :'marker'
          AND line.material_code = :'marker' || '_MATERIAL'
   );
DELETE FROM wms_stock_documents document
 WHERE document.tenant_id = '1000'
   AND document.document_type = 'COMPLETION_INBOUND'
   AND document.source_system = 'BPI'
   AND EXISTS (
       SELECT 1 FROM wms_stock_document_lines line
        WHERE line.document_id = document.id
          AND line.batch_no = :'marker'
          AND line.material_code = :'marker' || '_MATERIAL'
   );
DELETE FROM wms_batch_stocks
 WHERE tenant_id = '1000'
   AND material_code = :'marker' || '_MATERIAL'
   AND batch_no = :'marker'
   AND warehouse_code = 'WARE-E2E'
   AND location_code = 'LOC-E2E';

COMMIT;

SELECT (
    (SELECT count(*) FROM wms_stock_document_lines
      WHERE tenant_id = '1000' AND batch_no = :'marker'
        AND material_code = :'marker' || '_MATERIAL')
  + (SELECT count(*) FROM wms_inventory_transactions
      WHERE tenant_id = '1000' AND batch_no = :'marker'
        AND material_code = :'marker' || '_MATERIAL')
  + (SELECT count(*) FROM wms_batch_stocks
      WHERE tenant_id = '1000' AND batch_no = :'marker'
        AND material_code = :'marker' || '_MATERIAL')
) AS residual_rows
\gset

\if :residual_rows
    \echo 'Formal WMS roundtrip material cleanup left residual rows:' :residual_rows
    \quit 9
\else
    \echo 'Formal WMS roundtrip material cleanup residualRows=0'
\endif
