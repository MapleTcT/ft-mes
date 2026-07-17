-- PostgreSQL-first WOM QR generation and print-state persistence.

CREATE TABLE IF NOT EXISTS wom_qrcode_daily_sequences (
    tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
    manufacture_date DATE NOT NULL,
    last_sequence INTEGER NOT NULL DEFAULT 0,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (tenant_id, manufacture_date),
    CONSTRAINT wom_qrcode_daily_sequences_range_ck
        CHECK (last_sequence >= 0 AND last_sequence <= 99999)
);

CREATE TABLE IF NOT EXISTS wom_package_qrcodes (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
    request_id VARCHAR(80) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    sequence_no INTEGER NOT NULL,
    task_id BIGINT NOT NULL,
    task_table_no VARCHAR(255),
    produce_batch_num VARCHAR(255),
    product_id BIGINT,
    product_code VARCHAR(255),
    product_name VARCHAR(255),
    printer_id BIGINT,
    print_host VARCHAR(255),
    print_port INTEGER,
    manufacture_date DATE NOT NULL,
    expiry_date DATE NOT NULL,
    qr_code VARCHAR(64) NOT NULL,
    qr_content TEXT NOT NULL,
    detail TEXT NOT NULL,
    is_print BOOLEAN NOT NULL DEFAULT FALSE,
    print_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    printed_at TIMESTAMP WITHOUT TIME ZONE
);

ALTER TABLE wom_package_qrcodes
    ADD COLUMN IF NOT EXISTS print_count INTEGER NOT NULL DEFAULT 0;

UPDATE wom_package_qrcodes
SET print_count = CASE
        WHEN is_print IS TRUE THEN GREATEST(COALESCE(print_count, 0), 1)
        ELSE COALESCE(print_count, 0)
    END,
    updated_at = CURRENT_TIMESTAMP
WHERE print_count IS NULL
   OR (is_print IS TRUE AND print_count = 0);

ALTER TABLE wom_package_qrcodes
    ALTER COLUMN print_count SET DEFAULT 0,
    ALTER COLUMN print_count SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'wom_package_qrcodes_request_sequence_uk'
          AND conrelid = 'wom_package_qrcodes'::regclass
    ) THEN
        ALTER TABLE wom_package_qrcodes
            ADD CONSTRAINT wom_package_qrcodes_request_sequence_uk
            UNIQUE (tenant_id, request_id, sequence_no);
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'wom_package_qrcodes_qr_code_uk'
          AND conrelid = 'wom_package_qrcodes'::regclass
    ) THEN
        ALTER TABLE wom_package_qrcodes
            ADD CONSTRAINT wom_package_qrcodes_qr_code_uk
            UNIQUE (tenant_id, qr_code);
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'wom_package_qrcodes_sequence_ck'
          AND conrelid = 'wom_package_qrcodes'::regclass
    ) THEN
        ALTER TABLE wom_package_qrcodes
            ADD CONSTRAINT wom_package_qrcodes_sequence_ck CHECK (sequence_no > 0);
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'wom_package_qrcodes_date_ck'
          AND conrelid = 'wom_package_qrcodes'::regclass
    ) THEN
        ALTER TABLE wom_package_qrcodes
            ADD CONSTRAINT wom_package_qrcodes_date_ck CHECK (expiry_date >= manufacture_date);
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'wom_package_qrcodes_print_port_ck'
          AND conrelid = 'wom_package_qrcodes'::regclass
    ) THEN
        ALTER TABLE wom_package_qrcodes
            ADD CONSTRAINT wom_package_qrcodes_print_port_ck
            CHECK (print_port IS NULL OR print_port BETWEEN 1 AND 65535);
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'wom_package_qrcodes_print_count_ck'
          AND conrelid = 'wom_package_qrcodes'::regclass
    ) THEN
        ALTER TABLE wom_package_qrcodes
            ADD CONSTRAINT wom_package_qrcodes_print_count_ck CHECK (print_count >= 0);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS wom_package_qrcodes_task_created_idx
    ON wom_package_qrcodes (tenant_id, task_id, created_at DESC);

CREATE INDEX IF NOT EXISTS wom_package_qrcodes_detail_idx
    ON wom_package_qrcodes (tenant_id, detail);

INSERT INTO wom_qrcode_daily_sequences (tenant_id, manufacture_date, last_sequence)
SELECT tenant_id, manufacture_date, MAX(RIGHT(qr_code, 5)::INTEGER)
FROM wom_package_qrcodes
WHERE qr_code ~ '^[0-9]{11}$'
GROUP BY tenant_id, manufacture_date
ON CONFLICT (tenant_id, manufacture_date) DO UPDATE
SET last_sequence = GREATEST(
        wom_qrcode_daily_sequences.last_sequence,
        EXCLUDED.last_sequence
    ),
    updated_at = CURRENT_TIMESTAMP;

COMMENT ON TABLE wom_qrcode_daily_sequences IS
    'Transactional daily sequence allocator for WOM yyMMdd plus five-digit QR codes.';
COMMENT ON TABLE wom_package_qrcodes IS
    'Generated WOM package QR records and their latest print state.';
