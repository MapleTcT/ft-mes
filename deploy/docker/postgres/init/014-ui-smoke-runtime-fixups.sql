CREATE TABLE IF NOT EXISTS public.system_theme (
  id bigint NOT NULL,
  row_version bigint DEFAULT 0,
  theme varchar(500) NOT NULL,
  logo varchar(500) DEFAULT NULL,
  font smallint DEFAULT 12,
  status smallint DEFAULT 0,
  creator varchar(200) DEFAULT NULL,
  modifier varchar(200) DEFAULT NULL,
  create_time timestamp DEFAULT current_timestamp,
  modify_time timestamp DEFAULT NULL,
  tenant_id varchar(64) DEFAULT NULL,
  CONSTRAINT system_theme_pkey PRIMARY KEY (id)
);

INSERT INTO public.system_theme(id, theme, logo, font, status)
VALUES
  (1000, 'default', '/supplant-static/img/login_logo_dc338cd.png', 12, 1),
  (1001, 'dark', '/supplant-static/img/login_logo_dc338cd.png', 12, 0)
ON CONFLICT (id) DO UPDATE
SET theme = EXCLUDED.theme,
    logo = EXCLUDED.logo,
    font = EXCLUDED.font,
    status = EXCLUDED.status;

CREATE TABLE IF NOT EXISTS public.supfusion_i18n_language (
  id bigint NOT NULL,
  langu_code varchar(64) NOT NULL,
  langu_type varchar(64) NOT NULL,
  langu_name varchar(128) NOT NULL,
  has_used varchar(16) DEFAULT '1',
  valid varchar(16) DEFAULT '1',
  creator varchar(32) DEFAULT 'system',
  create_time timestamp DEFAULT current_timestamp,
  create_staff_id bigint DEFAULT NULL,
  modifier varchar(32) DEFAULT NULL,
  modify_time timestamp DEFAULT NULL,
  modify_staff_id bigint DEFAULT NULL,
  tenant_id varchar(64) DEFAULT 'dt',
  CONSTRAINT supfusion_i18n_language_pkey PRIMARY KEY (id)
);

ALTER TABLE public.supfusion_i18n_language
  ALTER COLUMN has_used TYPE varchar(16) USING has_used::text,
  ALTER COLUMN has_used SET DEFAULT '1',
  ALTER COLUMN valid TYPE varchar(16) USING valid::text,
  ALTER COLUMN valid SET DEFAULT '1';

DO $$
BEGIN
  IF to_regclass('public.supfusion_i18n_resource') IS NOT NULL THEN
    ALTER TABLE public.supfusion_i18n_resource
      ALTER COLUMN valid TYPE varchar(16) USING valid::text,
      ALTER COLUMN valid SET DEFAULT '1';

    IF NOT EXISTS (
      SELECT 1 FROM public.supfusion_i18n_resource WHERE valid = '1'
    ) THEN
      UPDATE public.supfusion_i18n_resource
      SET valid = '1'
      WHERE valid IS NULL OR valid = '0';
    END IF;
  END IF;
END $$;

INSERT INTO public.supfusion_i18n_language(
  id, langu_code, langu_type, langu_name, has_used, valid, creator, create_time, tenant_id
)
VALUES
  (1, 'zh_CN', '中文（简体）', '中文（简体）', '1', '1', 'system', current_timestamp, 'dt'),
  (2, 'zh_HK', '中文（香港）', '中文（繁体）', '1', '1', 'system', current_timestamp, 'dt'),
  (3, 'en_US', '英文（美国）', 'English', '1', '1', 'system', current_timestamp, 'dt')
ON CONFLICT (id) DO UPDATE
SET langu_code = EXCLUDED.langu_code,
    langu_type = EXCLUDED.langu_type,
    langu_name = EXCLUDED.langu_name,
    has_used = EXCLUDED.has_used,
    valid = EXCLUDED.valid,
    tenant_id = EXCLUDED.tenant_id;

CREATE TABLE IF NOT EXISTS public.ec_portlet (
  code varchar(510) NOT NULL,
  memo varchar(4000) DEFAULT NULL,
  height integer DEFAULT NULL,
  resize_func text DEFAULT NULL,
  onload_func text DEFAULT NULL,
  iframe_flag smallint DEFAULT NULL,
  menu_info_id bigint DEFAULT NULL,
  menu_operate_id bigint DEFAULT NULL,
  menu_code varchar(510) DEFAULT NULL,
  operate_code varchar(510) DEFAULT NULL,
  cid bigint DEFAULT NULL,
  module_code varchar(510) DEFAULT NULL,
  is_hidden smallint DEFAULT NULL,
  power_flag smallint DEFAULT NULL,
  scope_num integer DEFAULT NULL,
  version integer DEFAULT 0,
  is_default smallint DEFAULT NULL,
  title_color varchar(510) DEFAULT NULL,
  title_key varchar(510) DEFAULT NULL,
  title varchar(510) DEFAULT NULL,
  size_num integer DEFAULT NULL,
  more_target varchar(510) DEFAULT NULL,
  more_url varchar(510) DEFAULT NULL,
  url varchar(510) DEFAULT NULL,
  delete_time timestamp DEFAULT NULL,
  modify_time timestamp DEFAULT NULL,
  create_time timestamp DEFAULT current_timestamp,
  terminator varchar(32) DEFAULT NULL,
  modifier varchar(32) DEFAULT NULL,
  creator varchar(32) DEFAULT NULL,
  create_staff_id bigint DEFAULT NULL,
  modify_staff_id bigint DEFAULT NULL,
  tenant_id varchar(64) DEFAULT NULL,
  CONSTRAINT ec_portlet_pkey PRIMARY KEY (code)
);

CREATE TABLE IF NOT EXISTS public.ec_my_portlet (
  id bigint NOT NULL,
  version integer DEFAULT 0,
  config text DEFAULT NULL,
  user_id bigint DEFAULT NULL,
  tenant_id varchar(64) DEFAULT NULL,
  CONSTRAINT ec_my_portlet_pkey PRIMARY KEY (id)
);

INSERT INTO public.ec_portlet(
  code, height, iframe_flag, power_flag, cid, module_code, title_key, title, url
)
VALUES
  ('myProcess', 350, 1, 0, 0, 'portal', 'portal.homepage.myProcess', 'portal.homepage.myProcess', '/supplant/#/myProcess'),
  ('pendingNotice', 110, 1, 0, 0, 'portal', 'portal.homepage.pendingNotice', 'portal.homepage.pendingNotice', '/supplant/#/pendingNotice')
ON CONFLICT (code) DO UPDATE
SET height = EXCLUDED.height,
    iframe_flag = EXCLUDED.iframe_flag,
    power_flag = EXCLUDED.power_flag,
    cid = EXCLUDED.cid,
    module_code = EXCLUDED.module_code,
    title_key = EXCLUDED.title_key,
    title = EXCLUDED.title,
    url = EXCLUDED.url;

INSERT INTO public.ec_my_portlet(id, config, user_id)
VALUES
  (1, '[{"portlets":[{"cid":0,"code":"myProcess","default":false,"hidden":false,"iframeFlag":false,"menuInfoId":0,"powerFlag":false,"scopeNum":0,"title":"我的流程","titleKey":"portal.homepage.myProcess","url":"/supplant/#/myProcess?__t__=1616033155820"}],"width":"33%"},{"portlets":[{"cid":0,"code":"pendingNotice","default":false,"hidden":false,"iframeFlag":false,"menuInfoId":0,"powerFlag":false,"scopeNum":0,"title":"待办提醒","titleKey":"portal.homepage.pendingNotice","url":"/supplant/#/pendingNotice?__t__=1616033156792"}],"width":"33%"},{"portlets":[],"width":"33%"}]', -1)
ON CONFLICT (id) DO UPDATE
SET config = EXCLUDED.config,
    user_id = EXCLUDED.user_id;

DO $$
BEGIN
  IF to_regclass('public.supos_app') IS NOT NULL THEN
    ALTER TABLE public.supos_app
      ADD COLUMN IF NOT EXISTS app_version integer DEFAULT 0;

    UPDATE public.supos_app
    SET app_version = 0
    WHERE app_version IS NULL;
  END IF;

  IF to_regclass('public.rbac_menuinfo') IS NOT NULL THEN
    UPDATE public.rbac_menuinfo
    SET url = '/taskscheduler/index.html#/tasklog'
    WHERE code = 'taskScheduler_log'
      AND url = '/taskScheduler/index.html#/tasklog';
  END IF;
END $$;

DROP VIEW IF EXISTS public.base_menuoperate;
DO $$
BEGIN
  IF to_regclass('public.rbac_menuoperate') IS NOT NULL THEN
    EXECUTE $view$
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
  rbac_menuoperate.valid,
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
$view$;
  END IF;
END $$;

DROP VIEW IF EXISTS public.base_datapermission;
DO $$
BEGIN
  IF to_regclass('public.rbac_flow_permission') IS NOT NULL THEN
    EXECUTE $view$
CREATE OR REPLACE VIEW public.base_datapermission AS
SELECT
  rbac_flow_permission.id,
  rbac_flow_permission.version,
  rbac_flow_permission.create_staff_id,
  rbac_flow_permission.modify_staff_id,
  NULL::text AS delete_staff_id,
  rbac_flow_permission.create_time,
  rbac_flow_permission.modify_time,
  rbac_flow_permission.delete_time,
  1 AS valid,
  rbac_flow_permission.entity_code,
  rbac_flow_permission.purview_distribution,
  rbac_flow_permission.purview_state,
  rbac_flow_permission.memo,
  rbac_flow_permission.unlimited_power,
  rbac_flow_permission.group_power_flag,
  rbac_flow_permission.assign_staff_flag,
  rbac_flow_permission.assign_pos_flag,
  rbac_flow_permission.position_power_flag,
  rbac_flow_permission.flow_permission_type AS data_permission_type,
  rbac_flow_permission.type_id,
  rbac_flow_permission.activity_code,
  NULLIF(rbac_flow_permission.flow_version, '')::integer AS flow_version,
  rbac_flow_permission.flow_key
FROM public.rbac_flow_permission;
$view$;
  END IF;
END $$;

CREATE OR REPLACE FUNCTION public.bap_bigint_eq_bytea(left_value bigint, right_value bytea)
RETURNS boolean
LANGUAGE plpgsql
IMMUTABLE
AS $function$
BEGIN
  IF right_value IS NULL THEN
    RETURN NULL;
  END IF;

  BEGIN
    RETURN left_value = convert_from(right_value, 'UTF8')::bigint;
  EXCEPTION WHEN others THEN
    RETURN false;
  END;
END;
$function$;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_operator op
    JOIN pg_namespace ns ON ns.oid = op.oprnamespace
    WHERE ns.nspname = 'public'
      AND op.oprname = '='
      AND op.oprleft = 'bigint'::regtype
      AND op.oprright = 'bytea'::regtype
  ) THEN
    CREATE OPERATOR public.= (
      LEFTARG = bigint,
      RIGHTARG = bytea,
      PROCEDURE = public.bap_bigint_eq_bytea
    );
  END IF;
END $$;

CREATE OR REPLACE FUNCTION public.bap_bigint_ge_varchar(left_value bigint, right_value varchar)
RETURNS boolean
LANGUAGE plpgsql
IMMUTABLE
AS $function$
BEGIN
  IF right_value IS NULL OR btrim(right_value) = '' THEN
    RETURN NULL;
  END IF;
  BEGIN
    RETURN left_value >= right_value::bigint;
  EXCEPTION WHEN others THEN
    RETURN false;
  END;
END;
$function$;

CREATE OR REPLACE FUNCTION public.bap_bigint_le_varchar(left_value bigint, right_value varchar)
RETURNS boolean
LANGUAGE plpgsql
IMMUTABLE
AS $function$
BEGIN
  IF right_value IS NULL OR btrim(right_value) = '' THEN
    RETURN NULL;
  END IF;
  BEGIN
    RETURN left_value <= right_value::bigint;
  EXCEPTION WHEN others THEN
    RETURN false;
  END;
END;
$function$;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_operator op
    JOIN pg_namespace ns ON ns.oid = op.oprnamespace
    WHERE ns.nspname = 'public'
      AND op.oprname = '>='
      AND op.oprleft = 'bigint'::regtype
      AND op.oprright = 'character varying'::regtype
  ) THEN
    CREATE OPERATOR public.>= (
      LEFTARG = bigint,
      RIGHTARG = varchar,
      PROCEDURE = public.bap_bigint_ge_varchar
    );
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM pg_operator op
    JOIN pg_namespace ns ON ns.oid = op.oprnamespace
    WHERE ns.nspname = 'public'
      AND op.oprname = '<='
      AND op.oprleft = 'bigint'::regtype
      AND op.oprright = 'character varying'::regtype
  ) THEN
    CREATE OPERATOR public.<= (
      LEFTARG = bigint,
      RIGHTARG = varchar,
      PROCEDURE = public.bap_bigint_le_varchar
    );
  END IF;
END $$;

DROP VIEW IF EXISTS public.base_positionwork;
DO $$
BEGIN
  IF to_regclass('public.org_person_position') IS NOT NULL THEN
    EXECUTE $view$
CREATE OR REPLACE VIEW public.base_positionwork AS
SELECT
  org_person_position.id,
  org_person_position.row_version AS version,
  org_person_position.position_id,
  org_person_position.person_id AS staff_id,
  org_person_position.valid,
  NULL::text AS transfer_out_deal_time,
  NULL::text AS transfer_in_deal_time,
  NULL::text AS transfer_out_dealer_id,
  NULL::text AS transfer_in_dealer_id,
  NULL::text AS edit_date,
  NULL::text AS end_time,
  NULL::text AS start_time
FROM public.org_person_position;
$view$;
  END IF;
END $$;

CREATE TABLE IF NOT EXISTS public.wfm_task_form (
  id bigint NOT NULL,
  process_id varchar(64) NOT NULL,
  instance_id varchar(64) DEFAULT NULL,
  user_id bigint NOT NULL,
  form_data text DEFAULT NULL,
  form_temp_data text DEFAULT NULL,
  tenant_id varchar(64) DEFAULT '',
  creator varchar(32) DEFAULT '',
  create_staff_id bigint DEFAULT NULL,
  create_time timestamp DEFAULT current_timestamp,
  modifier varchar(32) DEFAULT '',
  modify_staff_id bigint DEFAULT NULL,
  modify_time timestamp DEFAULT current_timestamp,
  CONSTRAINT wfm_task_form_pkey PRIMARY KEY (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_wfm_form_ins_id
  ON public.wfm_task_form(instance_id, user_id);

CREATE TABLE IF NOT EXISTS public.wfm_task_pending (
  id bigint NOT NULL,
  user_id bigint NOT NULL,
  person_name varchar(128) DEFAULT NULL,
  app_id varchar(64) DEFAULT NULL,
  task_description varchar(512) DEFAULT NULL,
  activity_type varchar(32) DEFAULT NULL,
  activity_name varchar(64) DEFAULT NULL,
  execution_id varchar(128) DEFAULT NULL,
  task_status smallint NOT NULL DEFAULT 88,
  open_url varchar(1024) DEFAULT NULL,
  instance_id varchar(64) DEFAULT NULL,
  process_key varchar(64) DEFAULT NULL,
  process_version integer DEFAULT 0,
  process_name varchar(200) DEFAULT NULL,
  process_description varchar(512) DEFAULT NULL,
  initiator_id varchar(64) DEFAULT NULL,
  staff_name varchar(128) DEFAULT NULL,
  process_id varchar(64) DEFAULT NULL,
  table_info_id bigint DEFAULT NULL,
  entity_code varchar(64) DEFAULT NULL,
  table_no varchar(64) DEFAULT NULL,
  deployment_id bigint DEFAULT 0,
  task_type smallint DEFAULT 0,
  proxy_source bigint DEFAULT NULL,
  description varchar(512) DEFAULT NULL,
  loops smallint DEFAULT NULL,
  task_source varchar(32) DEFAULT NULL,
  cid bigint DEFAULT NULL,
  model_id bigint DEFAULT NULL,
  main_loop smallint DEFAULT 0,
  attention smallint DEFAULT 0,
  has_read smallint DEFAULT 0,
  multi_company smallint DEFAULT 0,
  source_staff bigint DEFAULT 0,
  latest_user varchar(64) DEFAULT NULL,
  mobile_approve smallint DEFAULT 1,
  description_zh_cn varchar(512) DEFAULT NULL,
  task_description_zh_cn varchar(512) DEFAULT NULL,
  process_description_zh_cn varchar(512) DEFAULT NULL,
  integration_id varchar(64) DEFAULT NULL,
  row_version integer DEFAULT NULL,
  version integer DEFAULT NULL,
  due_time timestamp DEFAULT NULL,
  tenant_id varchar(64) DEFAULT '',
  start_time timestamp NOT NULL DEFAULT current_timestamp,
  creator varchar(32) DEFAULT NULL,
  create_staff_id bigint DEFAULT NULL,
  create_time timestamp DEFAULT current_timestamp,
  modifier varchar(32) DEFAULT NULL,
  modify_staff_id bigint DEFAULT NULL,
  modify_time timestamp DEFAULT current_timestamp,
  CONSTRAINT wfm_task_pending_pkey PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_wfm_task_pending_user_id
  ON public.wfm_task_pending(user_id);

CREATE INDEX IF NOT EXISTS idx_wfm_task_pending_process_id
  ON public.wfm_task_pending(process_id);

CREATE INDEX IF NOT EXISTS idx_task_pending_instanceid
  ON public.wfm_task_pending(instance_id);

CREATE INDEX IF NOT EXISTS wtp_table_info_id_idx
  ON public.wfm_task_pending(table_info_id);

DO $$
DECLARE
  month_suffix text;
  task_table text;
  msg_table text;
BEGIN
  FOR month_suffix IN
    SELECT to_char(date_trunc('month', current_date) + make_interval(months => delta_month), 'YYYYMM')
    FROM generate_series(-1, 1) AS gs(delta_month)
  LOOP
    task_table := 'notice_task_' || month_suffix;
    msg_table := 'notice_msg_' || month_suffix;

    EXECUTE format(
      'CREATE TABLE IF NOT EXISTS public.%I (
        id bigint NOT NULL,
        code varchar(128) NOT NULL,
        bsmod_code varchar(200) NOT NULL,
        bsmod_name varchar(200) NOT NULL,
        task_type smallint NOT NULL,
        status smallint DEFAULT 0,
        sharding_time bigint NOT NULL,
        notice_topic_id bigint DEFAULT NULL,
        creator varchar(32) NOT NULL,
        create_time timestamp NOT NULL,
        create_staff_id bigint NOT NULL,
        modify_staff_id bigint DEFAULT NULL,
        modifier varchar(32) DEFAULT NULL,
        modify_time timestamp DEFAULT NULL,
        CONSTRAINT %I PRIMARY KEY (id)
      )',
      task_table,
      task_table || '_pkey'
    );
    EXECUTE format(
      'CREATE INDEX IF NOT EXISTS %I ON public.%I(sharding_time)',
      'idx_' || task_table || '_sharding_time',
      task_table
    );

    EXECUTE format(
      'CREATE TABLE IF NOT EXISTS public.%I (
        id bigint NOT NULL,
        staff_code varchar(200) NOT NULL,
        staff_name varchar(200) NOT NULL,
        bsmod_code varchar(200) DEFAULT NULL,
        bsmod_name varchar(200) DEFAULT NULL,
        topic_name varchar(32) DEFAULT NULL,
        user_name varchar(200) DEFAULT NULL,
        send_status smallint NOT NULL,
        error_result varchar(200) DEFAULT NULL,
        param text DEFAULT NULL,
        read_status smallint NOT NULL,
        retry smallint DEFAULT 0,
        sharding_time bigint NOT NULL,
        notice_task_id bigint NOT NULL,
        notice_protocol_id bigint NOT NULL,
        notice_task_protocol_id bigint NOT NULL,
        creator varchar(32) NOT NULL,
        create_time timestamp NOT NULL,
        create_staff_id bigint NOT NULL,
        modify_staff_id bigint DEFAULT NULL,
        modifier varchar(32) DEFAULT NULL,
        modify_time timestamp DEFAULT NULL,
        CONSTRAINT %I PRIMARY KEY (id)
      )',
      msg_table,
      msg_table || '_pkey'
    );
    EXECUTE format(
      'CREATE INDEX IF NOT EXISTS %I ON public.%I(sharding_time)',
      'idx_' || msg_table || '_sharding_time',
      msg_table
    );
  END LOOP;
END $$;
