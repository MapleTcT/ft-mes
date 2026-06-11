-- Base-platform compatibility objects found by the full menu browser smoke.
-- The recovered ADP Docker profile runs on PostgreSQL, while several legacy
-- services still expect MySQL-era base tables/views to exist.

CREATE TABLE IF NOT EXISTS public.runtime_module (
  code varchar(1024) NOT NULL,
  ec_env varchar(510) DEFAULT 'product',
  version integer DEFAULT 0,
  delete_time timestamp,
  modify_time timestamp,
  create_time timestamp DEFAULT CURRENT_TIMESTAMP,
  delete_staff_id bigint,
  modify_staff_id bigint,
  create_staff_id bigint,
  valid boolean DEFAULT true,
  is_proto boolean,
  main_module boolean,
  acronym varchar(510),
  copy_module_code varchar(510),
  type varchar(510),
  publish_time timestamp,
  category varchar(510),
  is_hide boolean,
  is_read_only boolean,
  proj_flag boolean,
  is_new_generate boolean,
  is_inherented_base boolean,
  deploy_order varchar(510),
  description text,
  initial_version varchar(510),
  project_version varchar(510),
  artifact varchar(510),
  value_zh_cn varchar(510),
  name varchar(510),
  CONSTRAINT runtime_module_pkey PRIMARY KEY (code)
);

CREATE INDEX IF NOT EXISTS idx_runtime_module_valid
  ON public.runtime_module(valid);

ALTER TABLE public.runtime_module
  ADD COLUMN IF NOT EXISTS is_cluster integer DEFAULT 0;

CREATE TABLE IF NOT EXISTS public.runtime_entity (
  code varchar(1024) NOT NULL,
  id varchar(100),
  ec_env varchar(510) DEFAULT 'product',
  version integer DEFAULT 0,
  delete_time timestamp,
  modify_time timestamp,
  create_time timestamp DEFAULT CURRENT_TIMESTAMP,
  delete_staff_id bigint,
  modify_staff_id bigint,
  create_staff_id bigint,
  valid boolean DEFAULT true,
  enable_fields_permission_conf boolean DEFAULT false,
  enable_ws boolean DEFAULT false,
  enable_rest boolean DEFAULT false,
  proj_flag boolean,
  type varchar(510),
  enable_audit boolean DEFAULT false,
  enable_acl_restrict boolean DEFAULT false,
  mobile boolean DEFAULT false,
  cross_company_flag boolean DEFAULT false,
  is_control boolean DEFAULT false,
  is_inherented_base boolean DEFAULT false,
  inherent_common_flag boolean DEFAULT false,
  is_base boolean DEFAULT false,
  module_code varchar(510),
  pay_close_attention boolean DEFAULT false,
  group_enabled boolean DEFAULT false,
  prefix varchar(510),
  description text,
  workflow_enabled boolean DEFAULT false,
  entity_name varchar(510),
  value_zh_cn varchar(510),
  name varchar(510),
  CONSTRAINT runtime_entity_pkey PRIMARY KEY (code)
);

CREATE INDEX IF NOT EXISTS idx_runtime_entity_valid
  ON public.runtime_entity(valid);

CREATE INDEX IF NOT EXISTS idx_runtime_entity_module_code
  ON public.runtime_entity(module_code);

INSERT INTO public.runtime_entity (
  code, ec_env, version, modify_time, valid, enable_fields_permission_conf,
  enable_ws, enable_rest, enable_audit, enable_acl_restrict, mobile,
  cross_company_flag, is_control, is_inherented_base, inherent_common_flag,
  is_base, module_code, pay_close_attention, group_enabled, prefix,
  workflow_enabled, entity_name, name
)
VALUES
  ('sysbase_1.0_company', 'product', 0, TIMESTAMP '2020-09-24 13:42:03.368000', true, false, false, false, false, false, false, false, false, true, false, true, 'sysbase_1.0', false, false, 'company', false, 'company', 'foundation.login.company'),
  ('sysbase_1.0_department', 'product', 0, TIMESTAMP '2020-09-24 13:42:03.368000', true, false, false, false, false, false, false, false, false, true, false, true, 'sysbase_1.0', false, false, 'department', false, 'department', 'foundation.workbench.filedownloadinfo.dept'),
  ('sysbase_1.0_inherent', 'product', 0, TIMESTAMP '2020-09-24 13:42:03.369000', true, false, false, false, false, false, false, false, false, true, false, true, 'sysbase_1.0', false, false, 'inherent', false, 'inherent', 'foundation.inherent.info'),
  ('sysbase_1.0_position', 'product', 0, TIMESTAMP '2020-09-24 13:42:03.368000', true, false, false, false, false, false, false, false, false, true, false, true, 'sysbase_1.0', false, false, 'position', false, 'position', 'foundation.ec.entity.position'),
  ('sysbase_1.0_role', 'product', 0, TIMESTAMP '2020-09-24 13:42:03.368000', true, false, false, false, false, false, false, false, false, true, false, true, 'sysbase_1.0', false, false, 'role', false, 'role', 'foundation.role.info'),
  ('sysbase_1.0_staff', 'product', 0, TIMESTAMP '2020-09-24 13:42:03.368000', true, false, false, false, false, false, false, false, false, true, false, true, 'sysbase_1.0', false, false, 'staff', false, 'staff', 'foundation.ec.entity.staff'),
  ('sysbase_1.0_status', 'product', 0, TIMESTAMP '2020-09-24 13:42:03.368000', true, false, false, false, false, false, false, false, false, true, false, true, 'sysbase_1.0', false, false, 'status', false, 'status', 'foundation.login.status'),
  ('sysbase_1.0_systemCalendar', 'product', 0, TIMESTAMP '2020-09-24 13:42:03.368000', true, false, false, false, false, false, false, false, false, true, false, true, 'sysbase_1.0', false, false, 'systemCalendar', false, 'systemCalendar', 'foundation.ec.entity.workingCalendar'),
  ('sysbase_1.0_user', 'product', 0, TIMESTAMP '2020-09-24 13:42:03.369000', true, false, false, false, false, false, false, false, false, true, false, true, 'sysbase_1.0', false, false, 'user', false, 'user', 'foundation.user.info')
ON CONFLICT (code) DO UPDATE
SET ec_env = EXCLUDED.ec_env,
    valid = EXCLUDED.valid,
    module_code = EXCLUDED.module_code,
    prefix = EXCLUDED.prefix,
    workflow_enabled = EXCLUDED.workflow_enabled,
    entity_name = EXCLUDED.entity_name,
    name = EXCLUDED.name;

CREATE TABLE IF NOT EXISTS public.runtime_module_reference (
  code varchar(1024) NOT NULL,
  ec_env varchar(510) DEFAULT 'product',
  version integer DEFAULT 0,
  delete_time timestamp,
  modify_time timestamp,
  create_time timestamp DEFAULT CURRENT_TIMESTAMP,
  delete_staff_id bigint,
  modify_staff_id bigint,
  create_staff_id bigint,
  valid boolean DEFAULT true,
  proj_flag boolean,
  module_code varchar(510),
  target_module_code varchar(510),
  CONSTRAINT runtime_module_reference_pkey PRIMARY KEY (code)
);

CREATE INDEX IF NOT EXISTS idx_runtime_module_reference_valid
  ON public.runtime_module_reference(valid);

CREATE TABLE IF NOT EXISTS public.runtime_module_relation (
  code varchar(1024) NOT NULL,
  ec_env varchar(510) DEFAULT 'product',
  version integer DEFAULT 0,
  delete_time timestamp,
  modify_time timestamp,
  create_time timestamp DEFAULT CURRENT_TIMESTAMP,
  delete_staff_id bigint,
  modify_staff_id bigint,
  create_staff_id bigint,
  valid boolean DEFAULT true,
  proj_flag boolean,
  module_code varchar(510),
  target_module_code varchar(510),
  CONSTRAINT runtime_module_relation_pkey PRIMARY KEY (code)
);

CREATE INDEX IF NOT EXISTS idx_runtime_module_relation_valid
  ON public.runtime_module_relation(valid);

INSERT INTO public.runtime_module (
  code, ec_env, version, modify_time, valid, is_hide, is_read_only,
  is_new_generate, is_inherented_base, initial_version, project_version,
  artifact, value_zh_cn, name
)
VALUES (
  'sysbase_1.0', 'product', 0, TIMESTAMP '2020-09-24 13:42:03.850000',
  true, false, true, false, true, '1.0', '1.0', 'foundation',
  '系统基础模块', 'foundation.ec.system.module'
)
ON CONFLICT (code) DO UPDATE
SET ec_env = EXCLUDED.ec_env,
    valid = EXCLUDED.valid,
    is_hide = EXCLUDED.is_hide,
    is_read_only = EXCLUDED.is_read_only,
    is_new_generate = EXCLUDED.is_new_generate,
    is_inherented_base = EXCLUDED.is_inherented_base,
    initial_version = EXCLUDED.initial_version,
    project_version = EXCLUDED.project_version,
    artifact = EXCLUDED.artifact,
    value_zh_cn = EXCLUDED.value_zh_cn,
    name = EXCLUDED.name;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.tables
    WHERE table_schema = 'public'
      AND table_name = 'project_module'
  ) THEN
    CREATE TABLE public.project_module
      (LIKE public.ec_module INCLUDING DEFAULTS INCLUDING CONSTRAINTS);
  END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_project_module_valid
  ON public.project_module(valid);

CREATE TABLE IF NOT EXISTS public.supos_app (
  code varchar(1024) NOT NULL,
  ec_env varchar(510) DEFAULT 'product',
  version integer DEFAULT 0,
  delete_time timestamp,
  modify_time timestamp,
  create_time timestamp DEFAULT CURRENT_TIMESTAMP,
  delete_staff_id bigint,
  modify_staff_id bigint,
  create_staff_id bigint,
  valid smallint DEFAULT 1,
  menus text,
  main_app_code text,
  modules text,
  memory bigint,
  extra_limited_memory bigint,
  app_type integer,
  app_version varchar(64) DEFAULT '1.0.0',
  name text,
  CONSTRAINT supos_app_pkey PRIMARY KEY (code)
);

ALTER TABLE public.supos_app
  ADD COLUMN IF NOT EXISTS extra_limited_memory bigint,
  ADD COLUMN IF NOT EXISTS app_version varchar(64) DEFAULT '1.0.0';

DO $$
BEGIN
  IF EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name = 'supos_app'
      AND column_name = 'app_version'
      AND data_type <> 'character varying'
  ) THEN
    ALTER TABLE public.supos_app
      ALTER COLUMN app_version TYPE varchar(64)
      USING COALESCE(app_version::text, '1.0.0');
  END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_supos_app_valid
  ON public.supos_app(valid);

CREATE TABLE IF NOT EXISTS public.auth_ip_black_white (
  id bigint NOT NULL,
  company_id bigint,
  ip varchar(256) NOT NULL,
  control_type integer NOT NULL,
  creator varchar(32) NOT NULL DEFAULT 'system',
  modifier varchar(32),
  create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  modify_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_staff_id bigint,
  modify_staff_id bigint,
  CONSTRAINT auth_ip_black_white_pkey PRIMARY KEY (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_auth_ip_black_white_company_ip
  ON public.auth_ip_black_white(company_id, ip);

CREATE INDEX IF NOT EXISTS idx_auth_ip_black_white_ip
  ON public.auth_ip_black_white(ip);

CREATE TABLE IF NOT EXISTS public.identity_center_config (
  id bigint NOT NULL,
  oauth_name varchar(64),
  protocol_type varchar(64),
  system_name varchar(64),
  system_flag boolean DEFAULT false,
  enable boolean DEFAULT false,
  app_id varchar(512),
  app_secret varchar(512),
  oauth_url varchar(256),
  token_url varchar(256),
  userinfo_url varchar(256),
  qrcode_url varchar(256),
  logout_url varchar(256),
  refresh_url varchar(256),
  redirect_url varchar(256),
  valid boolean DEFAULT true,
  qrcode_appid varchar(128),
  description varchar(256),
  creator varchar(32) NOT NULL DEFAULT 'system',
  modifier varchar(32),
  create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  modify_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_staff_id bigint,
  modify_staff_id bigint,
  CONSTRAINT identity_center_config_pkey PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_identity_center_config_valid
  ON public.identity_center_config(valid);

CREATE TABLE IF NOT EXISTS public.bap_signature_logs (
  uuid varchar(255) NOT NULL,
  operate_log_uuid text,
  second_sign_time timestamp,
  first_sign_time timestamp,
  transition_name varchar(510),
  task_name varchar(510),
  process_name varchar(510),
  business_key text,
  status boolean DEFAULT true,
  create_time timestamp,
  table_id bigint,
  second_remark varchar(510),
  second_reason varchar(510),
  first_remark varchar(510),
  first_reason varchar(510),
  transition_id bigint,
  task_id bigint,
  process_id bigint,
  button_name varchar(510),
  button_code varchar(510),
  model_name varchar(510),
  model_code varchar(510),
  entity_name varchar(510),
  entity_code varchar(510),
  module_name varchar(510),
  module_code varchar(510),
  cid bigint,
  second_staff_name varchar(510),
  second_staff_id bigint,
  first_staff_name varchar(510),
  first_staff_id bigint,
  signature_type varchar(510),
  ip_address varchar(510),
  second_user_name varchar(510),
  second_user_id bigint,
  first_user_name varchar(510),
  first_user_id bigint,
  CONSTRAINT bap_signature_logs_pkey PRIMARY KEY (uuid)
);

CREATE INDEX IF NOT EXISTS idx_bap_signature_logs_first_sign_time
  ON public.bap_signature_logs(first_sign_time);

CREATE TABLE IF NOT EXISTS public.wf_deployment (
  id bigint PRIMARY KEY,
  process_key varchar(510),
  process_version integer,
  name varchar(510),
  valid smallint DEFAULT 1,
  is_current_version smallint DEFAULT 1
);

ALTER TABLE public.wf_deployment
  ADD COLUMN IF NOT EXISTS version integer DEFAULT 0,
  ADD COLUMN IF NOT EXISTS delete_time timestamp,
  ADD COLUMN IF NOT EXISTS modify_time timestamp,
  ADD COLUMN IF NOT EXISTS create_time timestamp,
  ADD COLUMN IF NOT EXISTS delete_staff_id bigint,
  ADD COLUMN IF NOT EXISTS modify_staff_id bigint,
  ADD COLUMN IF NOT EXISTS create_staff_id bigint,
  ADD COLUMN IF NOT EXISTS signature_enable smallint,
  ADD COLUMN IF NOT EXISTS publish_time timestamp,
  ADD COLUMN IF NOT EXISTS main_view_view_code varchar(510),
  ADD COLUMN IF NOT EXISTS recall_remain_time bigint,
  ADD COLUMN IF NOT EXISTS recall_able smallint,
  ADD COLUMN IF NOT EXISTS gradually_reject smallint,
  ADD COLUMN IF NOT EXISTS allow_invalid smallint,
  ADD COLUMN IF NOT EXISTS mobileapprove smallint,
  ADD COLUMN IF NOT EXISTS mobileinitiate smallint,
  ADD COLUMN IF NOT EXISTS mobilequery smallint,
  ADD COLUMN IF NOT EXISTS required_time numeric(19,2),
  ADD COLUMN IF NOT EXISTS flow_edit_flag smallint,
  ADD COLUMN IF NOT EXISTS temp_process_xml text,
  ADD COLUMN IF NOT EXISTS entry_url varchar(510),
  ADD COLUMN IF NOT EXISTS process_xml text,
  ADD COLUMN IF NOT EXISTS operate_powers text,
  ADD COLUMN IF NOT EXISTS publish_flag smallint,
  ADD COLUMN IF NOT EXISTS entity_code varchar(510),
  ADD COLUMN IF NOT EXISTS menu_code varchar(510),
  ADD COLUMN IF NOT EXISTS menu_info_id bigint,
  ADD COLUMN IF NOT EXISTS process_definition_id varchar(510),
  ADD COLUMN IF NOT EXISTS is_suspended smallint,
  ADD COLUMN IF NOT EXISTS deployment_id varchar(510),
  ADD COLUMN IF NOT EXISTS process_name varchar(510),
  ADD COLUMN IF NOT EXISTS description varchar(510),
  ADD COLUMN IF NOT EXISTS name_zh_cn varchar(510),
  ADD COLUMN IF NOT EXISTS cid bigint,
  ADD COLUMN IF NOT EXISTS cross_company_flag smallint;

ALTER TABLE public.wf_deployment
  ALTER COLUMN process_key TYPE varchar(510) USING process_key::varchar(510),
  ALTER COLUMN name TYPE varchar(510) USING name::varchar(510);

CREATE INDEX IF NOT EXISTS idx_wf_deployment_valid
  ON public.wf_deployment(valid);

CREATE INDEX IF NOT EXISTS idx_wf_deployment_menu_info_id
  ON public.wf_deployment(menu_info_id);

ALTER TABLE public.rbac_menuoperate
  ADD COLUMN IF NOT EXISTS flow_name varchar(64),
  ADD COLUMN IF NOT EXISTS flow_name_display varchar(64);

CREATE TABLE IF NOT EXISTS public.org_group_person (
  id bigint NOT NULL,
  row_version bigint DEFAULT 0,
  group_id bigint NOT NULL,
  person_id bigint NOT NULL,
  valid smallint DEFAULT 1,
  creator varchar(32) NOT NULL DEFAULT 'system',
  modifier varchar(32),
  create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  modify_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_staff_id bigint,
  modify_staff_id bigint,
  CONSTRAINT org_group_person_pkey PRIMARY KEY (id),
  CONSTRAINT udx_org_group_person_ids UNIQUE (group_id, person_id)
);

CREATE INDEX IF NOT EXISTS idx_org_group_person_group_id
  ON public.org_group_person(group_id);

CREATE INDEX IF NOT EXISTS idx_org_group_person_person_id
  ON public.org_group_person(person_id);

DROP VIEW IF EXISTS public.base_menuinfo;
CREATE OR REPLACE VIEW public.base_menuinfo AS
SELECT
  rbac_menuinfo.id,
  rbac_menuinfo.version,
  rbac_menuinfo.create_staff_id,
  rbac_menuinfo.modify_staff_id,
  NULL::bigint AS delete_staff_id,
  rbac_menuinfo.create_time,
  rbac_menuinfo.modify_time,
  rbac_menuinfo.delete_time,
  CASE WHEN rbac_menuinfo.valid THEN 1 ELSE 0 END::smallint AS valid,
  rbac_menuinfo.cid,
  rbac_menuinfo.security_class,
  rbac_menuinfo.absolute_hidden,
  rbac_menuinfo.three_role,
  rbac_menuinfo.show_type,
  rbac_menuinfo.request_type,
  rbac_menuinfo.hidden_type,
  rbac_menuinfo.menu_type,
  rbac_menuinfo.is_hide,
  rbac_menuinfo.group_only,
  rbac_menuinfo.entity_code,
  rbac_menuinfo.entity_code AS ec_entity_code,
  rbac_menuinfo.module_code,
  rbac_menuinfo.system_default,
  rbac_menuinfo.css_class,
  rbac_menuinfo.sort,
  rbac_menuinfo.namespace,
  rbac_menuinfo.url,
  rbac_menuinfo.target,
  rbac_menuinfo.memo,
  rbac_menuinfo.name,
  rbac_menuinfo.name_display AS name_zh_cn,
  rbac_menuinfo.code,
  rbac_menuinfo.lay_no,
  rbac_menuinfo.lay_rec,
  rbac_menuinfo.parent_id,
  rbac_menuinfo.full_path_name,
  rbac_menuinfo.action_url AS action,
  rbac_menuinfo.leaf,
  NULL::smallint AS st_flag,
  NULL::text AS st_digitalsignature,
  NULL::text AS st_intro,
  NULL::text AS st_tabtypeid,
  NULL::text AS st_type,
  NULL::text AS remote_user_name_named,
  NULL::text AS remote_password_named,
  NULL::bigint AS remote_id,
  NULL::text AS pims_menu_type,
  NULL::text AS module,
  NULL::text AS icon_url,
  rbac_menuinfo.status
FROM public.rbac_menuinfo;

DROP VIEW IF EXISTS public.base_menuoperate;
CREATE OR REPLACE VIEW public.base_menuoperate AS
SELECT
  rbac_menuoperate.id,
  rbac_menuoperate.row_version AS version,
  rbac_menuoperate.create_staff_id,
  rbac_menuoperate.modify_staff_id,
  NULL::text AS delete_staff_id,
  rbac_menuoperate.create_time,
  rbac_menuoperate.modify_time,
  rbac_menuoperate.delete_time,
  CASE WHEN rbac_menuoperate.valid THEN 1 ELSE 0 END::smallint AS valid,
  rbac_menuoperate.cid,
  rbac_menuoperate.is_allow_proxy,
  rbac_menuoperate.is_hidden,
  rbac_menuoperate.three_role,
  rbac_menuoperate.view_code,
  rbac_menuoperate.is_query,
  rbac_menuoperate.is_orrelation,
  rbac_menuoperate.for_flow_permission AS for_data_permission,
  rbac_menuoperate.enable_norestrict,
  rbac_menuoperate.enable_custompermission AS enable_otherrestrict,
  rbac_menuoperate.enable_datapermission AS enable_specialpermission,
  rbac_menuoperate.enable_dealerpermission,
  rbac_menuoperate.enable_assignstaff,
  rbac_menuoperate.enable_assignpos,
  rbac_menuoperate.enable_posrestrict,
  rbac_menuoperate.enable_grouprestrict,
  rbac_menuoperate.entity_code,
  rbac_menuoperate.ignore_permission,
  rbac_menuoperate.power_flag,
  NULLIF(rbac_menuoperate.flow_version, '')::integer AS flow_version,
  rbac_menuoperate.flow_key,
  rbac_menuoperate.msg_assembled,
  rbac_menuoperate.deployment_id,
  rbac_menuoperate.menuoperatetype AS type,
  rbac_menuoperate.menuinfo_id,
  rbac_menuoperate.icon_cls,
  rbac_menuoperate.module_code AS module,
  rbac_menuoperate.sort,
  rbac_menuoperate.memo,
  rbac_menuoperate.target,
  rbac_menuoperate.namespace,
  rbac_menuoperate.url,
  rbac_menuoperate.name_zh_cn,
  rbac_menuoperate.name,
  rbac_menuoperate.code,
  rbac_menuoperate.action_url AS action,
  0 AS st_flag,
  NULL::text AS st_type,
  NULL::text AS st_tablecode,
  NULL::text AS st_showstyle,
  NULL::text AS st_operatetype,
  NULL::text AS st_isview,
  NULL::text AS st_iss2flowoperate,
  NULL::text AS st_ismainquery,
  NULL::text AS st_isdefault,
  NULL::text AS st_flowkey,
  NULL::text AS st_digitalsignature,
  NULL::text AS st_defaultdisplay,
  NULL::text AS st_activityid,
  NULL::text AS menuoperate_mainoperatecode,
  NULL::text AS menuoperate_iscontainer,
  NULL::text AS menuoperate_entryoperatecode,
  rbac_menuoperate.menuoperatetype,
  rbac_menuoperate.flow_name,
  rbac_menuoperate.flow_name_display
FROM public.rbac_menuoperate;

DROP VIEW IF EXISTS public.base_roleuser;
CREATE OR REPLACE VIEW public.base_roleuser AS
SELECT
  id,
  version,
  position_flag,
  role_id,
  user_id,
  CASE WHEN valid THEN 1 ELSE 0 END::smallint AS valid,
  end_time,
  start_time
FROM public.rbac_roleuser;

DROP VIEW IF EXISTS public.base_custom_group;
CREATE OR REPLACE VIEW public.base_custom_group AS
SELECT
  id,
  row_version AS version,
  code,
  name AS name_cn,
  description,
  sort,
  company_id AS cid,
  valid,
  create_staff_id,
  modify_staff_id,
  NULL::bigint AS delete_staff_id,
  create_time,
  modify_time,
  NULL::timestamp AS delete_time
FROM public.org_group;

DROP VIEW IF EXISTS public.base_custom_groupmember;
CREATE OR REPLACE VIEW public.base_custom_groupmember AS
SELECT
  gp.id,
  gp.row_version AS version,
  gp.group_id AS related_custom_group_id,
  gp.person_id AS related_staff_id,
  gp.valid,
  gp.create_staff_id,
  gp.modify_staff_id,
  NULL::bigint AS delete_staff_id,
  gp.create_time,
  gp.modify_time,
  g.company_id AS cid,
  NULL::timestamp AS delete_time
FROM public.org_group_person gp
LEFT JOIN public.org_group g ON gp.group_id = g.id;
