ALTER TABLE public.rbac_menuoperatecode_url_ref
    ADD COLUMN IF NOT EXISTS method_type INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS md5_code_url CHAR(32),
    ADD COLUMN IF NOT EXISTS app VARCHAR(32),
    ADD COLUMN IF NOT EXISTS import_type INTEGER DEFAULT 0;

UPDATE public.rbac_menuoperatecode_url_ref
SET method_type = COALESCE(
        method_type,
        CASE UPPER(COALESCE(http_method, ''))
            WHEN 'POST' THEN 1
            WHEN 'PUT' THEN 2
            WHEN 'DELETE' THEN 3
            ELSE 0
        END
    ),
    md5_code_url = COALESCE(md5_code_url, lower(md5(COALESCE(menuoperate_code, '') || COALESCE(url, '')))),
    import_type = COALESCE(import_type, 0);

CREATE INDEX IF NOT EXISTS menu_operate_code_idx
    ON public.rbac_menuoperatecode_url_ref (menuoperate_code);

CREATE UNIQUE INDEX IF NOT EXISTS m_codeurl_un_idx
    ON public.rbac_menuoperatecode_url_ref (method_type, md5_code_url);

ALTER TABLE public.rbac_userpermission
    ADD COLUMN IF NOT EXISTS assign_departments TEXT,
    ADD COLUMN IF NOT EXISTS assign_positions TEXT,
    ADD COLUMN IF NOT EXISTS assign_staffs TEXT;

ALTER TABLE public.rbac_rolepermission
    ADD COLUMN IF NOT EXISTS assign_departments TEXT,
    ADD COLUMN IF NOT EXISTS assign_positions TEXT,
    ADD COLUMN IF NOT EXISTS assign_staffs TEXT;

CREATE UNIQUE INDEX IF NOT EXISTS rbac_rolepermission_role_menu_uidx
    ON public.rbac_rolepermission (role_id, menuoperate_id)
    WHERE role_id IS NOT NULL AND menuoperate_id IS NOT NULL;

ALTER TABLE public.rbac_menu_mnecode
    ADD COLUMN IF NOT EXISTS row_version BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS language VARCHAR(510);

CREATE TABLE IF NOT EXISTS public.rbac_role_mnecode (
    id BIGINT PRIMARY KEY,
    row_version BIGINT NOT NULL DEFAULT 0,
    role BIGINT,
    mne_code VARCHAR(510)
);

CREATE OR REPLACE VIEW public.base_role_mnecode AS
SELECT
    id,
    row_version AS version,
    role,
    mne_code
FROM public.rbac_role_mnecode;

CREATE TABLE IF NOT EXISTS public.rbac_tag (
    id BIGINT PRIMARY KEY,
    version INTEGER DEFAULT 0,
    type VARCHAR(32),
    name VARCHAR(100) NOT NULL,
    cid BIGINT NOT NULL,
    objectid BIGINT NOT NULL,
    creator VARCHAR(32),
    modifier VARCHAR(32),
    valid BOOLEAN DEFAULT true,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modify_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_staff_id BIGINT,
    modify_staff_id BIGINT
);

CREATE TABLE IF NOT EXISTS public.rbac_data_resource_group (
    id BIGINT PRIMARY KEY,
    group_code VARCHAR(512) NOT NULL,
    group_name VARCHAR(512) NOT NULL,
    resource_url VARCHAR(4000) NOT NULL,
    module_code VARCHAR(100) NOT NULL,
    cid BIGINT,
    creator VARCHAR(32) NOT NULL DEFAULT 'system',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_staff_id BIGINT NOT NULL DEFAULT 0,
    modifier VARCHAR(32),
    modify_time TIMESTAMP,
    modify_staff_id BIGINT
);

CREATE TABLE IF NOT EXISTS public.rbac_flow_permission (
    id BIGINT PRIMARY KEY,
    version INTEGER DEFAULT 0,
    delete_time TIMESTAMP,
    modify_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    terminator VARCHAR(32),
    modifier VARCHAR(32),
    creator VARCHAR(32),
    create_staff_id BIGINT,
    modify_staff_id BIGINT,
    cid BIGINT,
    entity_code VARCHAR(510),
    purview_distribution INTEGER,
    purview_state INTEGER,
    memo VARCHAR(510),
    unlimited_power BOOLEAN DEFAULT false,
    group_power_flag BOOLEAN DEFAULT false,
    assign_staff_flag BOOLEAN DEFAULT false,
    assign_pos_flag BOOLEAN DEFAULT false,
    position_power_flag BOOLEAN DEFAULT false,
    flow_permission_type VARCHAR(510),
    type_id BIGINT,
    activity_code VARCHAR(510),
    flow_version VARCHAR(510),
    flow_key VARCHAR(510),
    flow_name VARCHAR(64),
    flow_name_display VARCHAR(64)
);

CREATE TABLE IF NOT EXISTS public.rbac_flow_permission_staff (
    id BIGINT PRIMARY KEY,
    version INTEGER DEFAULT 0,
    staff_id BIGINT,
    flowpermission_id BIGINT
);

CREATE TABLE IF NOT EXISTS public.rbac_flow_permission_position (
    id BIGINT PRIMARY KEY,
    version INTEGER DEFAULT 0,
    position_id BIGINT,
    include_lower BOOLEAN DEFAULT false,
    flowpermission_id BIGINT
);

CREATE INDEX IF NOT EXISTS idx_dp_typeid
    ON public.rbac_flow_permission (type_id);
CREATE INDEX IF NOT EXISTS idx_dp_dptype
    ON public.rbac_flow_permission (flow_permission_type);
CREATE INDEX IF NOT EXISTS idx_dp_entitycode
    ON public.rbac_flow_permission (entity_code);
CREATE INDEX IF NOT EXISTS idx_rbac_flow_permission_1
    ON public.rbac_flow_permission (flow_permission_type, type_id);
CREATE INDEX IF NOT EXISTS index_flow_staff_id_idx
    ON public.rbac_flow_permission_staff (flowpermission_id);
CREATE INDEX IF NOT EXISTS index_flow_position_id_idx
    ON public.rbac_flow_permission_position (flowpermission_id);

INSERT INTO public.rbac_role (id, name, code, tag, description, cid, valid, create_staff_id, modify_staff_id)
SELECT 1, '管理员角色', 'systemRole', '管理员角色', '管理员角色', 1000, true, 1, 1
WHERE NOT EXISTS (SELECT 1 FROM public.rbac_role WHERE code = 'systemRole');

INSERT INTO public.rbac_role (id, name, code, tag, description, cid, valid, create_staff_id, modify_staff_id)
SELECT 2, '公司管理员角色', 'companySystemRole', '公司管理员角色', '公司管理员角色', 1000, true, 1, 1
WHERE NOT EXISTS (SELECT 1 FROM public.rbac_role WHERE code = 'companySystemRole');

INSERT INTO public.rbac_role (id, name, code, tag, description, cid, valid, create_staff_id, modify_staff_id)
SELECT 3, '普通用户角色', 'normalRole', '普通用户角色', '普通用户角色', 1000, true, 1, 1
WHERE NOT EXISTS (SELECT 1 FROM public.rbac_role WHERE code = 'normalRole');

INSERT INTO public.rbac_roleuser (id, role_id, user_id, person_name, person_code, user_name, valid)
SELECT 1, 1, 1, '默认人员', 'default', 'admin', true
WHERE NOT EXISTS (SELECT 1 FROM public.rbac_roleuser WHERE id = 1);

INSERT INTO public.rbac_tag (id, name, cid, objectid, valid)
SELECT 1, '管理员角色', 1000, 1, true
WHERE NOT EXISTS (SELECT 1 FROM public.rbac_tag WHERE id = 1);

INSERT INTO public.rbac_tag (id, name, cid, objectid, valid)
SELECT 2, '公司管理员角色', 1000, 2, true
WHERE NOT EXISTS (SELECT 1 FROM public.rbac_tag WHERE id = 2);

INSERT INTO public.rbac_tag (id, name, cid, objectid, valid)
SELECT 3, '普通用户角色', 1000, 3, true
WHERE NOT EXISTS (SELECT 1 FROM public.rbac_tag WHERE id = 3);

INSERT INTO public.rbac_data_resource_group (
    id,
    group_code,
    group_name,
    resource_url,
    module_code,
    creator,
    create_staff_id
)
SELECT
    1,
    'oodm-data-group-permission',
    '数据建模权限分组',
    '/project/dam/supngin/api/dam/v1/dataGroups',
    'oodm',
    'system',
    0
WHERE NOT EXISTS (SELECT 1 FROM public.rbac_data_resource_group WHERE id = 1);

DROP VIEW IF EXISTS public.base_datapmsposition;
DROP VIEW IF EXISTS public.base_datapermissionstaff;
DROP VIEW IF EXISTS public.base_datapermission;

CREATE OR REPLACE VIEW public.base_datapermission AS
SELECT
    id,
    version,
    create_staff_id,
    modify_staff_id,
    NULL::bigint AS delete_staff_id,
    create_time,
    modify_time,
    delete_time,
    true AS valid,
    entity_code,
    purview_distribution,
    purview_state,
    memo,
    unlimited_power,
    group_power_flag,
    assign_staff_flag,
    assign_pos_flag,
    position_power_flag,
    flow_permission_type AS data_permission_type,
    type_id,
    activity_code,
    NULLIF(flow_version, '')::integer AS flow_version,
    flow_key
FROM public.rbac_flow_permission;

CREATE OR REPLACE VIEW public.base_datapermissionstaff AS
SELECT
    id,
    version,
    staff_id,
    flowpermission_id AS datapermission_id
FROM public.rbac_flow_permission_staff;

CREATE OR REPLACE VIEW public.base_datapmsposition AS
SELECT
    id,
    version,
    position_id,
    include_lower,
    flowpermission_id AS datapermission_id
FROM public.rbac_flow_permission_position;
