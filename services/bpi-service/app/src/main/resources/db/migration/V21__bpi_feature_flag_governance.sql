ALTER TABLE bpi.bpi_feature_flags
    ADD COLUMN active boolean NOT NULL DEFAULT true,
    ADD COLUMN last_reason varchar(500) NOT NULL DEFAULT 'Imported from the existing runtime configuration';

INSERT INTO bpi.bpi_feature_flags
    (id, tenant_id, scope_type, scope_key, flag_key, enabled, revision, updated_by, active, last_reason)
VALUES
    ('00000000-0000-0000-0000-000000000005', '*', 'GLOBAL', '*',
     'bpi.auto-confirm', false, 1, 'flyway', true, 'Phase 1 keeps automatic confirmation disabled'),
    ('00000000-0000-0000-0000-000000000006', '*', 'GLOBAL', '*',
     'bpi.wms-link', false, 1, 'flyway', true, 'Phase 1 keeps WMS write-back disabled')
ON CONFLICT (tenant_id, scope_type, scope_key, flag_key) DO NOTHING;

CREATE INDEX idx_bpi_feature_flag_resolution
    ON bpi.bpi_feature_flags (flag_key, tenant_id, scope_type, scope_key)
    WHERE active;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'bpi_service') THEN
        GRANT SELECT, INSERT, UPDATE ON bpi.bpi_feature_flags TO bpi_service;
    END IF;
END
$$;
