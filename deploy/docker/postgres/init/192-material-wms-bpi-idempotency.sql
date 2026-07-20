-- External completion-inbound provenance and exact idempotency lookup.
-- Existing WOM rows remain valid; new BPI writes use source_system = 'BPI'.

ALTER TABLE wms_stock_documents
    ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(256);

CREATE UNIQUE INDEX IF NOT EXISTS uk_wms_stock_documents_idempotency
    ON wms_stock_documents (tenant_id, document_type, source_system, idempotency_key);

ALTER TABLE wms_stock_document_lines
    ADD COLUMN IF NOT EXISTS source_system VARCHAR(32) NOT NULL DEFAULT 'WOM';
ALTER TABLE wms_stock_document_lines
    ADD COLUMN IF NOT EXISTS unit_code VARCHAR(64) NOT NULL DEFAULT '';

DROP INDEX IF EXISTS uk_wms_stock_document_lines_source;
CREATE UNIQUE INDEX IF NOT EXISTS uk_wms_stock_document_lines_source
    ON wms_stock_document_lines (
        tenant_id, document_type, source_system, source_line_id
    );

ALTER TABLE wms_inventory_transactions
    ADD COLUMN IF NOT EXISTS source_system VARCHAR(32) NOT NULL DEFAULT 'WOM';
ALTER TABLE wms_inventory_transactions
    ADD COLUMN IF NOT EXISTS unit_code VARCHAR(64) NOT NULL DEFAULT '';

CREATE INDEX IF NOT EXISTS idx_wms_inventory_transactions_source
    ON wms_inventory_transactions (
        tenant_id, source_system, source_document_id, source_line_id
    );
