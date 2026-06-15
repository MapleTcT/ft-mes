-- Fixups required after legacy ADP auth/RBAC scripts run under PostgreSQL.

ALTER TABLE auth_user ADD COLUMN IF NOT EXISTS ldap_user_name VARCHAR(256) DEFAULT NULL;
ALTER TABLE auth_user ADD COLUMN IF NOT EXISTS user_directory_id BIGINT DEFAULT NULL;
ALTER TABLE auth_user ADD COLUMN IF NOT EXISTS lock_reason SMALLINT DEFAULT NULL;
ALTER TABLE auth_user ADD COLUMN IF NOT EXISTS lock_time TIMESTAMP DEFAULT NULL;
ALTER TABLE auth_user ADD COLUMN IF NOT EXISTS third_identity VARCHAR(256) DEFAULT NULL;
ALTER TABLE auth_user ADD COLUMN IF NOT EXISTS third_source VARCHAR(256) DEFAULT NULL;
ALTER TABLE auth_user ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(64) DEFAULT NULL;

CREATE INDEX IF NOT EXISTS idx_user_ldap_user_name ON auth_user(ldap_user_name);

CREATE TABLE IF NOT EXISTS rbac_role (
    id BIGINT PRIMARY KEY,
    valid INT DEFAULT 1,
    version INT DEFAULT 0,
    cid BIGINT,
    leaf INT DEFAULT 0,
    full_path_name VARCHAR(4000),
    parent_id BIGINT,
    lay_no BIGINT,
    lay_rec VARCHAR(4000),
    uuid VARCHAR(4000),
    three_role_type INT,
    role_type VARCHAR(4000),
    sort DOUBLE PRECISION,
    description VARCHAR(510),
    name VARCHAR(160),
    code VARCHAR(160),
    sys_init INT DEFAULT 0,
    creator VARCHAR(32),
    modifier VARCHAR(32),
    terminator VARCHAR(32),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modify_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    delete_time TIMESTAMP,
    create_staff_id BIGINT,
    modify_staff_id BIGINT,
    tag VARCHAR(510)
);

CREATE INDEX IF NOT EXISTS base_role_valid_idx ON rbac_role(valid);
CREATE INDEX IF NOT EXISTS index_role_code ON rbac_role(code);
CREATE INDEX IF NOT EXISTS index_role_name ON rbac_role(name);
CREATE INDEX IF NOT EXISTS index_role_tag ON rbac_role(tag);

CREATE TABLE IF NOT EXISTS rbac_roleuser (
    id BIGINT PRIMARY KEY,
    version INT DEFAULT 0,
    position_flag INT DEFAULT 0,
    role_id BIGINT,
    user_id BIGINT,
    valid INT DEFAULT 1,
    end_time TIMESTAMP,
    start_time TIMESTAMP,
    person_name VARCHAR(160),
    person_code VARCHAR(160),
    user_name VARCHAR(160),
    terminator VARCHAR(32),
    modifier VARCHAR(32),
    creator VARCHAR(32),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modify_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    delete_time TIMESTAMP,
    create_staff_id BIGINT,
    modify_staff_id BIGINT,
    from_position INT DEFAULT 1
);

CREATE INDEX IF NOT EXISTS index_roleuser_role_id ON rbac_roleuser(role_id);
CREATE INDEX IF NOT EXISTS index_roleuser_user_id ON rbac_roleuser(user_id);

CREATE TABLE IF NOT EXISTS rbac_menuinfo (
    id BIGINT PRIMARY KEY,
    version INT DEFAULT 0,
    delete_time TIMESTAMP,
    modify_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    terminator VARCHAR(32),
    leaf INT DEFAULT 0,
    modifier VARCHAR(32),
    creator VARCHAR(32),
    create_staff_id BIGINT,
    modify_staff_id BIGINT,
    valid INT DEFAULT 1,
    cid BIGINT,
    security_class VARCHAR(510),
    absolute_hidden INT DEFAULT 0,
    three_role INT DEFAULT 0,
    show_type INT,
    request_type INT,
    hidden_type INT,
    menu_type INT,
    is_hide INT DEFAULT 0,
    group_only INT DEFAULT 0,
    entity_code VARCHAR(510),
    module_code VARCHAR(510),
    system_default INT DEFAULT 0,
    css_class VARCHAR(510),
    sort DOUBLE PRECISION,
    action_url VARCHAR(510),
    namespace VARCHAR(510),
    url VARCHAR(510),
    target VARCHAR(510) DEFAULT 'SELF',
    memo VARCHAR(510),
    name VARCHAR(510),
    name_display VARCHAR(510),
    code VARCHAR(510),
    app VARCHAR(510),
    appid VARCHAR(510),
    enable INT DEFAULT 1,
    lay_no INT,
    lay_rec VARCHAR(4000),
    parent_id BIGINT,
    full_path VARCHAR(4000),
    full_path_name VARCHAR(4000),
    source VARCHAR(510),
    edited INT DEFAULT 0,
    type INT DEFAULT 0,
    no_restrict INT DEFAULT 0,
    status INT DEFAULT 0,
    route VARCHAR(510),
    extra VARCHAR(1000),
    company_read_only INT DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_menuinfo_code ON rbac_menuinfo(code);
CREATE INDEX IF NOT EXISTS index_menuinfo_name ON rbac_menuinfo(name);
CREATE INDEX IF NOT EXISTS idx_menuinfo_modulecode ON rbac_menuinfo(module_code);
CREATE INDEX IF NOT EXISTS base_menuinfo_valid_idx ON rbac_menuinfo(valid);

CREATE TABLE IF NOT EXISTS rbac_menuoperate (
    id BIGINT PRIMARY KEY,
    row_version BIGINT DEFAULT 0,
    version BIGINT DEFAULT 0,
    delete_time TIMESTAMP,
    modify_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    terminator VARCHAR(32),
    modifier VARCHAR(32),
    creator VARCHAR(32),
    create_staff_id BIGINT,
    modify_staff_id BIGINT,
    valid INT DEFAULT 1,
    cid BIGINT,
    is_allow_proxy INT DEFAULT 0,
    is_hidden INT DEFAULT 0,
    three_role INT DEFAULT 0,
    view_code VARCHAR(510),
    is_query INT DEFAULT 0,
    is_orrelation INT,
    enable_datapermission INT DEFAULT 0,
    enable_custompermission INT DEFAULT 0,
    for_flow_permission INT DEFAULT 0,
    enable_norestrict INT DEFAULT 1,
    enable_dealerpermission INT DEFAULT 0,
    enable_assignstaff INT DEFAULT 0,
    enable_assignpos INT DEFAULT 0,
    enable_posrestrict INT DEFAULT 0,
    enable_deptrict INT DEFAULT 0,
    enable_assigndept INT DEFAULT 0,
    enable_grouprestrict INT DEFAULT 0,
    entity_code VARCHAR(510),
    ignore_permission INT DEFAULT 0,
    power_flag INT DEFAULT 0,
    flow_version VARCHAR(510),
    flow_key VARCHAR(510),
    msg_assembled INT,
    deployment_id BIGINT,
    menuoperatetype VARCHAR(510),
    menuinfo_id BIGINT,
    icon_cls VARCHAR(510),
    module_code VARCHAR(510),
    sort DOUBLE PRECISION,
    memo VARCHAR(510),
    target VARCHAR(510),
    action_url VARCHAR(510),
    namespace VARCHAR(510),
    url VARCHAR(510),
    name_zh_cn VARCHAR(510),
    name VARCHAR(510),
    name_display VARCHAR(510),
    code VARCHAR(510),
    app VARCHAR(510),
    default_operate INT DEFAULT 0,
    edited INT DEFAULT 0
);

CREATE INDEX IF NOT EXISTS index_menuoperate_code ON rbac_menuoperate(code);
CREATE INDEX IF NOT EXISTS index_menuoperate_name ON rbac_menuoperate(name);
CREATE INDEX IF NOT EXISTS ind_menuoperate_url ON rbac_menuoperate(url);
CREATE INDEX IF NOT EXISTS ind_menuoperate_namespace ON rbac_menuoperate(namespace);
CREATE INDEX IF NOT EXISTS ind_menuoperate_menuinfo_id ON rbac_menuoperate(menuinfo_id);
CREATE INDEX IF NOT EXISTS idx_menuoperate_entitycode ON rbac_menuoperate(entity_code);
CREATE INDEX IF NOT EXISTS base_menuoperate_valid_idx ON rbac_menuoperate(valid);

CREATE TABLE IF NOT EXISTS rbac_userpermission (
    id BIGINT PRIMARY KEY,
    version INT DEFAULT 0,
    user_id BIGINT,
    deal_staff BIGINT,
    cid BIGINT,
    menuoperate_id BIGINT,
    purview_type INT,
    position_flag INT DEFAULT 0,
    department_flag INT DEFAULT 0,
    group_flag INT DEFAULT 0,
    assign_staff_flag INT DEFAULT 0,
    assign_pos_flag INT DEFAULT 0,
    assign_dept_flag INT DEFAULT 0,
    dealer_permission_flag INT DEFAULT 0,
    no_restrict_flag INT DEFAULT 0,
    assign_datapermission_flag INT DEFAULT 0,
    assign_custompermission_flag INT DEFAULT 0,
    url_pattern VARCHAR(510),
    menuoperate_code VARCHAR(510),
    delete_time TIMESTAMP,
    modify_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    terminator VARCHAR(32),
    modifier VARCHAR(32),
    creator VARCHAR(32),
    create_staff_id BIGINT,
    modify_staff_id BIGINT
);

CREATE INDEX IF NOT EXISTS ind_userpermission_user_id ON rbac_userpermission(user_id);
CREATE INDEX IF NOT EXISTS index_userpermission_mo ON rbac_userpermission(menuoperate_id);

CREATE TABLE IF NOT EXISTS rbac_rolepermission (
    id BIGINT PRIMARY KEY,
    cid BIGINT,
    version INT DEFAULT 0,
    role_id BIGINT,
    menuoperate_id BIGINT,
    position_flag INT DEFAULT 0,
    department_flag INT DEFAULT 0,
    group_flag INT DEFAULT 0,
    assign_staff_flag INT DEFAULT 0,
    assign_pos_flag INT DEFAULT 0,
    assign_dept_flag INT DEFAULT 0,
    dealer_permission_flag INT DEFAULT 0,
    no_restrict_flag INT DEFAULT 0,
    assign_datapermission_flag INT DEFAULT 0,
    assign_custompermission_flag INT DEFAULT 0,
    url_pattern VARCHAR(510),
    delete_time TIMESTAMP,
    modify_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    terminator VARCHAR(32),
    modifier VARCHAR(32),
    creator VARCHAR(32),
    create_staff_id BIGINT,
    modify_staff_id BIGINT
);

CREATE INDEX IF NOT EXISTS index_rm_role_id ON rbac_rolepermission(role_id);
CREATE INDEX IF NOT EXISTS index_rm_menuoperate_id ON rbac_rolepermission(menuoperate_id);

CREATE TABLE IF NOT EXISTS rbac_menuinfo_company_ref (
    id BIGINT PRIMARY KEY,
    menuinfo_id BIGINT,
    company_id BIGINT,
    valid INT DEFAULT 1,
    creator VARCHAR(32),
    modifier VARCHAR(32),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modify_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_staff_id BIGINT,
    modify_staff_id BIGINT
);

CREATE TABLE IF NOT EXISTS rbac_menu_mnecode (
    id BIGINT PRIMARY KEY,
    menu_info BIGINT,
    mne_code VARCHAR(510),
    valid INT DEFAULT 1,
    creator VARCHAR(32),
    modifier VARCHAR(32),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modify_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_staff_id BIGINT,
    modify_staff_id BIGINT
);

CREATE TABLE IF NOT EXISTS rbac_app_ref (
    id BIGINT PRIMARY KEY,
    menuid BIGINT,
    appid VARCHAR(510),
    valid INT DEFAULT 1,
    creator VARCHAR(32),
    modifier VARCHAR(32),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modify_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_staff_id BIGINT,
    modify_staff_id BIGINT
);

CREATE TABLE IF NOT EXISTS rbac_menuoperatecode_url_ref (
    id BIGINT PRIMARY KEY,
    menuoperate_code VARCHAR(510),
    url VARCHAR(1024),
    http_method VARCHAR(64),
    reg_match INT DEFAULT 0,
    is_custom INT DEFAULT 0,
    valid INT DEFAULT 1,
    creator VARCHAR(32),
    modifier VARCHAR(32),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modify_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_staff_id BIGINT,
    modify_staff_id BIGINT
);

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
    '0',
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
    '1',
    1,
    '-1',
    NULL,
    '-1',
    'rbac.MENU_NAME_MENU_LIST',
    NULL,
    '0'
)
ON CONFLICT (id) DO NOTHING;
