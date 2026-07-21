-- Append-only completion-inbound reversal support.
-- The original blue document remains durable; at most one red document may reference it.

ALTER TABLE wms_stock_documents
    DROP CONSTRAINT IF EXISTS ck_wms_stock_documents_type;
ALTER TABLE wms_stock_documents
    ADD CONSTRAINT ck_wms_stock_documents_type
        CHECK (document_type IN (
            'COMPLETION_INBOUND', 'COMPLETION_INBOUND_REVERSAL', 'PRODUCTION_ISSUE'
        ));

ALTER TABLE wms_stock_documents
    ADD COLUMN IF NOT EXISTS reversal_of_document_id BIGINT;

ALTER TABLE wms_stock_documents
    DROP CONSTRAINT IF EXISTS fk_wms_stock_documents_reversal_original;
ALTER TABLE wms_stock_documents
    ADD CONSTRAINT fk_wms_stock_documents_reversal_original
        FOREIGN KEY (reversal_of_document_id) REFERENCES wms_stock_documents(id);

CREATE UNIQUE INDEX IF NOT EXISTS uk_wms_stock_documents_reversal_original
    ON wms_stock_documents (tenant_id, reversal_of_document_id);

ALTER TABLE wms_inventory_transactions
    DROP CONSTRAINT IF EXISTS ck_wms_inventory_transactions_type;
ALTER TABLE wms_inventory_transactions
    ADD CONSTRAINT ck_wms_inventory_transactions_type
        CHECK (transaction_type IN (
            'COMPLETION_INBOUND', 'COMPLETION_INBOUND_REVERSAL', 'PRODUCTION_ISSUE',
            'QUALITY_RELEASE', 'QUALITY_HOLD',
            'QUALITY_ALLOCATION_HOLD', 'QUALITY_ALLOCATION_RELEASE'
        ));
