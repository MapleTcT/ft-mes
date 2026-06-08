-- RBAC boot code expects this row to exist before reading init_version.

CREATE TABLE IF NOT EXISTS rbac_init_verison_info (
    id BIGINT PRIMARY KEY,
    init_version INTEGER DEFAULT 0,
    app VARCHAR(256) DEFAULT NULL,
    tenant_id VARCHAR(64) DEFAULT NULL
);

INSERT INTO rbac_init_verison_info(id, init_version, app, tenant_id)
VALUES (1, 0, NULL, 'system001')
ON CONFLICT (id) DO NOTHING;
