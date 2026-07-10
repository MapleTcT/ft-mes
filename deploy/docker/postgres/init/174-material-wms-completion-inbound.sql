CREATE TABLE IF NOT EXISTS wms_stock_documents (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
    document_no VARCHAR(96) NOT NULL,
    document_type VARCHAR(32) NOT NULL,
    source_system VARCHAR(32) NOT NULL DEFAULT 'WOM',
    source_document_id VARCHAR(128) NOT NULL,
    source_document_no VARCHAR(128),
    directive_no VARCHAR(128),
    company_code VARCHAR(64) NOT NULL,
    department_code VARCHAR(64),
    staff_code VARCHAR(64),
    user_name VARCHAR(128),
    warehouse_code VARCHAR(64) NOT NULL,
    storage_date DATE NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'POSTED',
    quality_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    memo VARCHAR(1000),
    request_payload TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_wms_stock_documents_type
        CHECK (document_type IN ('COMPLETION_INBOUND', 'PRODUCTION_ISSUE')),
    CONSTRAINT ck_wms_stock_documents_status
        CHECK (status IN ('POSTED', 'REVERSED')),
    CONSTRAINT ck_wms_stock_documents_quality
        CHECK (quality_status IN ('PENDING', 'QUALIFIED', 'UNQUALIFIED'))
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_wms_stock_documents_source
    ON wms_stock_documents (
        tenant_id, document_type, source_system, source_document_id, warehouse_code
    );
CREATE UNIQUE INDEX IF NOT EXISTS uk_wms_stock_documents_no
    ON wms_stock_documents (tenant_id, document_no);
CREATE INDEX IF NOT EXISTS idx_wms_stock_documents_created
    ON wms_stock_documents (tenant_id, document_type, created_at);

CREATE TABLE IF NOT EXISTS wms_stock_document_lines (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL REFERENCES wms_stock_documents(id) ON DELETE CASCADE,
    tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
    document_type VARCHAR(32) NOT NULL,
    line_no INTEGER NOT NULL,
    source_line_id VARCHAR(128) NOT NULL,
    material_code VARCHAR(128) NOT NULL,
    batch_no VARCHAR(128) NOT NULL DEFAULT '',
    production_batch_no VARCHAR(128) NOT NULL DEFAULT '',
    warehouse_code VARCHAR(64) NOT NULL,
    location_code VARCHAR(64) NOT NULL DEFAULT '',
    quantity NUMERIC(20, 6) NOT NULL,
    production_date DATE,
    quality_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    memo VARCHAR(1000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_wms_stock_document_lines_quantity CHECK (quantity > 0),
    CONSTRAINT ck_wms_stock_document_lines_quality
        CHECK (quality_status IN ('PENDING', 'QUALIFIED', 'UNQUALIFIED'))
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_wms_stock_document_lines_source
    ON wms_stock_document_lines (tenant_id, document_type, source_line_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_wms_stock_document_lines_number
    ON wms_stock_document_lines (document_id, line_no);
CREATE INDEX IF NOT EXISTS idx_wms_stock_document_lines_document
    ON wms_stock_document_lines (document_id, line_no);
CREATE INDEX IF NOT EXISTS idx_wms_stock_document_lines_batch
    ON wms_stock_document_lines (tenant_id, material_code, batch_no);

CREATE TABLE IF NOT EXISTS wms_batch_stocks (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
    warehouse_code VARCHAR(64) NOT NULL,
    location_code VARCHAR(64) NOT NULL DEFAULT '',
    material_code VARCHAR(128) NOT NULL,
    batch_no VARCHAR(128) NOT NULL DEFAULT '',
    production_batch_no VARCHAR(128) NOT NULL DEFAULT '',
    on_hand_quantity NUMERIC(20, 6) NOT NULL DEFAULT 0,
    available_quantity NUMERIC(20, 6) NOT NULL DEFAULT 0,
    hold_quantity NUMERIC(20, 6) NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_wms_batch_stocks_on_hand CHECK (on_hand_quantity >= 0),
    CONSTRAINT ck_wms_batch_stocks_available CHECK (available_quantity >= 0),
    CONSTRAINT ck_wms_batch_stocks_hold CHECK (hold_quantity >= 0),
    CONSTRAINT ck_wms_batch_stocks_balance
        CHECK (on_hand_quantity = available_quantity + hold_quantity)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_wms_batch_stocks_dimension
    ON wms_batch_stocks (
        tenant_id, warehouse_code, location_code, material_code, batch_no, production_batch_no
    );
CREATE INDEX IF NOT EXISTS idx_wms_batch_stocks_material
    ON wms_batch_stocks (tenant_id, material_code, batch_no);

CREATE TABLE IF NOT EXISTS wms_inventory_transactions (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
    event_key VARCHAR(256) NOT NULL,
    transaction_type VARCHAR(32) NOT NULL,
    document_id BIGINT REFERENCES wms_stock_documents(id) ON DELETE CASCADE,
    line_id BIGINT REFERENCES wms_stock_document_lines(id) ON DELETE CASCADE,
    source_document_id VARCHAR(128) NOT NULL,
    source_line_id VARCHAR(128) NOT NULL,
    warehouse_code VARCHAR(64) NOT NULL,
    location_code VARCHAR(64) NOT NULL DEFAULT '',
    material_code VARCHAR(128) NOT NULL,
    batch_no VARCHAR(128) NOT NULL DEFAULT '',
    production_batch_no VARCHAR(128) NOT NULL DEFAULT '',
    on_hand_delta NUMERIC(20, 6) NOT NULL DEFAULT 0,
    available_delta NUMERIC(20, 6) NOT NULL DEFAULT 0,
    hold_delta NUMERIC(20, 6) NOT NULL DEFAULT 0,
    balance_on_hand NUMERIC(20, 6) NOT NULL,
    balance_available NUMERIC(20, 6) NOT NULL,
    balance_hold NUMERIC(20, 6) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_wms_inventory_transactions_type
        CHECK (transaction_type IN (
            'COMPLETION_INBOUND', 'PRODUCTION_ISSUE', 'QUALITY_RELEASE', 'QUALITY_HOLD'
        ))
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_wms_inventory_transactions_event
    ON wms_inventory_transactions (tenant_id, event_key);
CREATE INDEX IF NOT EXISTS idx_wms_inventory_transactions_stock
    ON wms_inventory_transactions (tenant_id, material_code, batch_no, created_at);

CREATE TABLE IF NOT EXISTS wms_quality_results (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
    source_system VARCHAR(32) NOT NULL DEFAULT 'WOM',
    source_line_id VARCHAR(128) NOT NULL,
    quality_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    revision BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_wms_quality_results_status
        CHECK (quality_status IN ('PENDING', 'QUALIFIED', 'UNQUALIFIED'))
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_wms_quality_results_source
    ON wms_quality_results (tenant_id, source_system, source_line_id);
