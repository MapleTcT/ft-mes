-- Compatibility tables reached by the recovered base-platform pages during
-- browser smoke tests. These tables normally come from the original ADP DB
-- initialization chain; the Docker PostgreSQL profile creates the minimal
-- structures needed for empty test data to render without 500s.

CREATE TABLE IF NOT EXISTS public.rbac_menuclick_log (
  id bigint PRIMARY KEY,
  user_id bigint,
  user_name varchar(32),
  staff_id bigint,
  staff_name varchar(32),
  staff_code varchar(32),
  ip_address varchar(520),
  menuinfo_code varchar(520),
  menuinfo_name varchar(520),
  menu_url varchar(520),
  cid bigint,
  module_code varchar(510),
  module_name varchar(510),
  source varchar(512),
  create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  modify_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  delete_time timestamp NULL,
  create_staff_id bigint,
  modify_staff_id bigint,
  modifier varchar(32),
  creator varchar(32)
);

CREATE TABLE IF NOT EXISTS public.rbac_role_folder (
  id bigint PRIMARY KEY,
  cid bigint NOT NULL,
  name varchar(160) NOT NULL,
  description varchar(510),
  leaf boolean DEFAULT false,
  valid boolean DEFAULT true,
  lay_no bigint DEFAULT 1,
  sort double precision DEFAULT 1000,
  parent_id bigint,
  full_path_name varchar(4000) NOT NULL,
  lay_rec varchar(4000) NOT NULL,
  creator varchar(32) DEFAULT 'system',
  modifier varchar(32),
  create_time timestamp DEFAULT CURRENT_TIMESTAMP,
  modify_time timestamp DEFAULT CURRENT_TIMESTAMP,
  create_staff_id bigint,
  modify_staff_id bigint
);
CREATE UNIQUE INDEX IF NOT EXISTS udx_role_folder_layrec
  ON public.rbac_role_folder(lay_rec);

CREATE TABLE IF NOT EXISTS public.rbac_role_folder_rel (
  id bigint PRIMARY KEY,
  folder_id bigint NOT NULL,
  role_id bigint NOT NULL,
  creator varchar(32) DEFAULT 'system',
  modifier varchar(32),
  create_time timestamp DEFAULT CURRENT_TIMESTAMP,
  modify_time timestamp DEFAULT CURRENT_TIMESTAMP,
  create_staff_id bigint,
  modify_staff_id bigint
);
CREATE UNIQUE INDEX IF NOT EXISTS udx_rel_fid_rid
  ON public.rbac_role_folder_rel(role_id, folder_id);

CREATE TABLE IF NOT EXISTS public.org_tag (
  id bigint PRIMARY KEY,
  row_version bigint DEFAULT 0,
  tag_type varchar(32) DEFAULT 'Company',
  name varchar(128) NOT NULL,
  company_id bigint NOT NULL,
  valid boolean DEFAULT true,
  creator varchar(32) NOT NULL DEFAULT 'system',
  modifier varchar(32),
  create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  modify_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_staff_id bigint,
  modify_staff_id bigint
);
CREATE UNIQUE INDEX IF NOT EXISTS udx_tag_cid_tag_type
  ON public.org_tag(company_id, name, tag_type);

CREATE TABLE IF NOT EXISTS public.ec_module (
  code varchar(2000) PRIMARY KEY,
  ec_env varchar(64) DEFAULT 'product',
  version integer DEFAULT 0,
  valid boolean NOT NULL DEFAULT true,
  create_staff_id bigint,
  modify_staff_id bigint,
  delete_staff_id bigint,
  create_time timestamp DEFAULT CURRENT_TIMESTAMP,
  modify_time timestamp DEFAULT CURRENT_TIMESTAMP,
  delete_time timestamp,
  name varchar(510),
  value_zh_cn varchar(510),
  artifact varchar(510),
  project_version varchar(128),
  initial_version varchar(128),
  description varchar(2000),
  deploy_order varchar(64),
  is_inherented_base boolean DEFAULT false,
  is_new_generate boolean DEFAULT false,
  proj_flag boolean,
  is_read_only boolean DEFAULT false,
  is_hide boolean DEFAULT false,
  category varchar(510),
  publish_time timestamp,
  type varchar(64) DEFAULT 'Mis',
  acronym varchar(128),
  is_proto boolean DEFAULT false,
  main_module boolean DEFAULT false,
  is_cluster integer DEFAULT 0
);

CREATE TABLE IF NOT EXISTS public.ec_module_relation (
  code varchar(2000) PRIMARY KEY,
  ec_env varchar(64) DEFAULT 'product',
  version integer DEFAULT 0,
  valid boolean NOT NULL DEFAULT true,
  create_staff_id bigint,
  modify_staff_id bigint,
  delete_staff_id bigint,
  create_time timestamp DEFAULT CURRENT_TIMESTAMP,
  modify_time timestamp DEFAULT CURRENT_TIMESTAMP,
  delete_time timestamp,
  module_code varchar(2000),
  target_module_code varchar(2000),
  proj_flag boolean
);

CREATE TABLE IF NOT EXISTS public.ec_module_reference (
  code varchar(2000) PRIMARY KEY,
  ec_env varchar(64) DEFAULT 'product',
  version integer DEFAULT 0,
  valid boolean NOT NULL DEFAULT true,
  create_staff_id bigint,
  modify_staff_id bigint,
  delete_staff_id bigint,
  create_time timestamp DEFAULT CURRENT_TIMESTAMP,
  modify_time timestamp DEFAULT CURRENT_TIMESTAMP,
  delete_time timestamp,
  module_code varchar(2000),
  target_module_code varchar(2000)
);

CREATE TABLE IF NOT EXISTS public.ec_msmodule (
  code varchar(2000) PRIMARY KEY,
  ec_env varchar(64) DEFAULT 'product',
  version integer DEFAULT 0,
  valid boolean NOT NULL DEFAULT true,
  create_staff_id bigint,
  modify_staff_id bigint,
  delete_staff_id bigint,
  create_time timestamp DEFAULT CURRENT_TIMESTAMP,
  modify_time timestamp DEFAULT CURRENT_TIMESTAMP,
  delete_time timestamp,
  name varchar(510),
  value_zh_cn varchar(510),
  showname varchar(510),
  artifact varchar(510),
  description varchar(2000),
  deploy_order varchar(64),
  is_inherented_base boolean DEFAULT false,
  is_new_generate boolean DEFAULT false,
  proj_flag boolean,
  is_read_only integer DEFAULT 0,
  is_hide integer DEFAULT 0,
  category varchar(510),
  cpunum integer,
  ramnum varchar(64),
  colony integer,
  status integer
);

CREATE TABLE IF NOT EXISTS public.ec_msmodule_ipadress (
  code varchar(2000) PRIMARY KEY,
  ec_env varchar(64) DEFAULT 'product',
  version integer DEFAULT 0,
  valid boolean NOT NULL DEFAULT true,
  create_staff_id bigint,
  modify_staff_id bigint,
  delete_staff_id bigint,
  create_time timestamp DEFAULT CURRENT_TIMESTAMP,
  modify_time timestamp DEFAULT CURRENT_TIMESTAMP,
  delete_time timestamp,
  ipadress varchar(255),
  msmodule_code varchar(2000),
  description varchar(2000),
  publish_time timestamp,
  status integer
);

CREATE TABLE IF NOT EXISTS public.ec_msmodule_relation (
  code varchar(2000) PRIMARY KEY,
  ec_env varchar(64) DEFAULT 'product',
  version integer DEFAULT 0,
  valid boolean NOT NULL DEFAULT true,
  create_staff_id bigint,
  modify_staff_id bigint,
  delete_staff_id bigint,
  create_time timestamp DEFAULT CURRENT_TIMESTAMP,
  modify_time timestamp DEFAULT CURRENT_TIMESTAMP,
  delete_time timestamp,
  msmodule_code varchar(2000)
);

CREATE TABLE IF NOT EXISTS public.wf_expected_consign (
  id bigint PRIMARY KEY,
  user_id bigint,
  create_date timestamp,
  start_date timestamp,
  end_date timestamp,
  valid smallint DEFAULT 1,
  memo varchar(2000),
  type varchar(32),
  recall_flag smallint DEFAULT 0,
  include_before smallint DEFAULT 0
);

CREATE TABLE IF NOT EXISTS public.wf_expected_consign_active (
  id bigint PRIMARY KEY,
  expected_consign_id bigint,
  valid smallint DEFAULT 1,
  flow_version varchar(128),
  flow_key varchar(255),
  active_code varchar(255)
);

CREATE TABLE IF NOT EXISTS public.wf_expected_consign_consignor (
  id bigint PRIMARY KEY,
  expected_consign_id bigint,
  valid smallint DEFAULT 1,
  consignor_id bigint,
  consignor_name varchar(255),
  consignor_staff_id bigint,
  consignor_staff_name varchar(255)
);

CREATE TABLE IF NOT EXISTS public.wf_expected_consign_date (
  id bigint PRIMARY KEY,
  expected_consign_id bigint,
  valid smallint DEFAULT 1,
  start_date timestamp,
  end_date timestamp
);

CREATE TABLE IF NOT EXISTS public.wf_deployment (
  id bigint PRIMARY KEY,
  process_key varchar(255),
  process_version integer,
  name varchar(510),
  valid smallint DEFAULT 1,
  is_current_version smallint DEFAULT 1
);

DO $$
BEGIN
  ALTER TABLE public.wf_expected_consign
    ALTER COLUMN valid DROP DEFAULT,
    ALTER COLUMN recall_flag DROP DEFAULT,
    ALTER COLUMN include_before DROP DEFAULT;
  ALTER TABLE public.wf_expected_consign
    ALTER COLUMN valid TYPE smallint USING CASE WHEN valid::text IN ('true', '1') THEN 1 ELSE 0 END,
    ALTER COLUMN recall_flag TYPE smallint USING CASE WHEN recall_flag::text IN ('true', '1') THEN 1 ELSE 0 END,
    ALTER COLUMN include_before TYPE smallint USING CASE WHEN include_before::text IN ('true', '1') THEN 1 ELSE 0 END;
  ALTER TABLE public.wf_expected_consign
    ALTER COLUMN valid SET DEFAULT 1,
    ALTER COLUMN recall_flag SET DEFAULT 0,
    ALTER COLUMN include_before SET DEFAULT 0;

  ALTER TABLE public.wf_expected_consign_active
    ALTER COLUMN valid DROP DEFAULT;
  ALTER TABLE public.wf_expected_consign_active
    ALTER COLUMN valid TYPE smallint USING CASE WHEN valid::text IN ('true', '1') THEN 1 ELSE 0 END;
  ALTER TABLE public.wf_expected_consign_active
    ALTER COLUMN valid SET DEFAULT 1;

  ALTER TABLE public.wf_expected_consign_consignor
    ALTER COLUMN valid DROP DEFAULT;
  ALTER TABLE public.wf_expected_consign_consignor
    ALTER COLUMN valid TYPE smallint USING CASE WHEN valid::text IN ('true', '1') THEN 1 ELSE 0 END;
  ALTER TABLE public.wf_expected_consign_consignor
    ALTER COLUMN valid SET DEFAULT 1;

  ALTER TABLE public.wf_expected_consign_date
    ALTER COLUMN valid DROP DEFAULT;
  ALTER TABLE public.wf_expected_consign_date
    ALTER COLUMN valid TYPE smallint USING CASE WHEN valid::text IN ('true', '1') THEN 1 ELSE 0 END;
  ALTER TABLE public.wf_expected_consign_date
    ALTER COLUMN valid SET DEFAULT 1;

  ALTER TABLE public.wf_deployment
    ALTER COLUMN valid DROP DEFAULT,
    ALTER COLUMN is_current_version DROP DEFAULT;
  ALTER TABLE public.wf_deployment
    ALTER COLUMN process_version TYPE integer USING NULLIF(process_version::text, '')::integer,
    ALTER COLUMN valid TYPE smallint USING CASE WHEN valid::text IN ('true', '1') THEN 1 ELSE 0 END,
    ALTER COLUMN is_current_version TYPE smallint USING CASE WHEN is_current_version::text IN ('true', '1') THEN 1 ELSE 0 END;
  ALTER TABLE public.wf_deployment
    ALTER COLUMN valid SET DEFAULT 1,
    ALTER COLUMN is_current_version SET DEFAULT 1;
END $$;

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
  rbac_menuoperate.menuoperatetype
FROM public.rbac_menuoperate;
