-- Fixups required after legacy ADP auth/RBAC scripts run under PostgreSQL.

ALTER TABLE auth_user ADD COLUMN IF NOT EXISTS ldap_user_name VARCHAR(256) DEFAULT NULL;
ALTER TABLE auth_user ADD COLUMN IF NOT EXISTS user_directory_id BIGINT DEFAULT NULL;
ALTER TABLE auth_user ADD COLUMN IF NOT EXISTS lock_reason SMALLINT DEFAULT NULL;
ALTER TABLE auth_user ADD COLUMN IF NOT EXISTS third_identity VARCHAR(256) DEFAULT NULL;
ALTER TABLE auth_user ADD COLUMN IF NOT EXISTS third_source VARCHAR(256) DEFAULT NULL;
ALTER TABLE auth_user ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(64) DEFAULT NULL;

CREATE INDEX IF NOT EXISTS idx_user_ldap_user_name ON auth_user(ldap_user_name);

ALTER TABLE rbac_menuinfo ALTER COLUMN create_time SET DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE rbac_menuinfo ALTER COLUMN modify_time SET DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE rbac_menuoperate ALTER COLUMN create_time SET DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE rbac_menuoperate ALTER COLUMN modify_time SET DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE rbac_role ALTER COLUMN create_time SET DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE rbac_role ALTER COLUMN modify_time SET DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE rbac_roleuser ALTER COLUMN create_time SET DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE rbac_roleuser ALTER COLUMN modify_time SET DEFAULT CURRENT_TIMESTAMP;

INSERT INTO rbac_menuinfo (
    id,
    version,
    delete_time,
    modify_time,
    create_time,
    terminator,
    modifier,
    creator,
    valid,
    cid,
    security_class,
    absolute_hidden,
    three_role,
    show_type,
    request_type,
    hidden_type,
    menu_type,
    is_hide,
    group_only,
    entity_code,
    module_code,
    system_default,
    css_class,
    sort,
    action_url,
    namespace,
    url,
    target,
    memo,
    name,
    name_display,
    code,
    app,
    enable,
    lay_no,
    lay_rec,
    parent_id,
    full_path,
    full_path_name,
    source,
    leaf
)
VALUES (
    -1,
    0,
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    NULL,
    NULL,
    'system',
    1,
    1000,
    NULL,
    NULL,
    0,
    0,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    'SELF',
    '系统默认',
    'rbac.MENU_NAME_MENU_LIST',
    '系统默认',
    'menu_list',
    'rbac',
    1,
    1,
    '-1',
    NULL,
    '-1',
    'rbac.MENU_NAME_MENU_LIST',
    NULL,
    0
)
ON CONFLICT (id) DO NOTHING;
