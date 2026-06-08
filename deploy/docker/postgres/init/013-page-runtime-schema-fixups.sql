ALTER TABLE public.org_person
  ADD COLUMN IF NOT EXISTS avatar_url varchar(256) DEFAULT NULL;

DO $$
BEGIN
  IF EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name = 'org_person'
      AND column_name = 'image_url'
  ) THEN
    UPDATE public.org_person
    SET avatar_url = image_url
    WHERE avatar_url IS NULL
      AND image_url IS NOT NULL;
  END IF;
END $$;

CREATE TABLE IF NOT EXISTS public.mod_module_registry (
  id bigint NOT NULL,
  module_id varchar(64) NOT NULL,
  module_code varchar(128) DEFAULT NULL,
  module_name varchar(64) NOT NULL,
  module_type varchar(24) DEFAULT 'SYSTEM',
  creator varchar(64) DEFAULT '',
  create_time timestamp DEFAULT current_timestamp,
  create_staff_id bigint DEFAULT NULL,
  modifier varchar(64) DEFAULT '',
  modify_time timestamp DEFAULT NULL,
  modify_staff_id bigint DEFAULT NULL,
  tenant_id varchar(64) DEFAULT NULL,
  CONSTRAINT mod_module_registry_pkey PRIMARY KEY (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS udx_mod_id
  ON public.mod_module_registry(module_id);

CREATE TABLE IF NOT EXISTS public.mod_module_app_rel (
  id bigint NOT NULL,
  app_id varchar(64) NOT NULL,
  module_id varchar(64) NOT NULL,
  creator varchar(64) DEFAULT '',
  create_time timestamp DEFAULT current_timestamp,
  create_staff_id bigint DEFAULT NULL,
  modifier varchar(64) DEFAULT '',
  modify_time timestamp DEFAULT NULL,
  modify_staff_id bigint DEFAULT NULL,
  tenant_id varchar(64) DEFAULT NULL,
  CONSTRAINT mod_module_app_rel_pkey PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_module_app_rel_appid
  ON public.mod_module_app_rel(app_id);

INSERT INTO public.mod_module_registry(id, module_id, module_name, module_type)
VALUES
  (10000, 'authentication', 'reg.moduleName.authentication', 'SYSTEM'),
  (10001, 'notificationEmail', 'reg.moduleName.notificationEmail', 'SYSTEM'),
  (10002, 'appConfig', 'reg.moduleName.appConfig', 'SYSTEM'),
  (10003, 'notificationDingtalk', 'reg.moduleName.notificationDingtalk', 'SYSTEM'),
  (10004, 'systemConfig', 'reg.moduleName.systemConfig', 'SYSTEM'),
  (10005, 'notificationWechat', 'reg.moduleName.notificationWechat', 'SYSTEM'),
  (10006, 'reg', 'reg.moduleName.reg', 'SYSTEM'),
  (10007, 'sys', 'reg.moduleName.sys', 'SYSTEM'),
  (10008, 'i18n', 'reg.moduleName.i18n', 'SYSTEM'),
  (10009, 'notificationStationletter', 'reg.moduleName.notificationStationletter', 'SYSTEM'),
  (10010, 'notificationApiServer', 'reg.moduleName.notificationApiServer', 'SYSTEM'),
  (10011, 'workflow', 'reg.moduleName.workflow', 'SYSTEM'),
  (10012, 'organization', 'reg.moduleName.organization', 'SYSTEM'),
  (10013, 'theme', 'reg.moduleName.theme', 'SYSTEM'),
  (10014, 'userManagement', 'reg.moduleName.userManagement', 'SYSTEM'),
  (10015, 'composeManage', 'reg.moduleName.composeManage', 'SYSTEM'),
  (10016, 'notificationAdmin', 'reg.moduleName.notificationAdmin', 'SYSTEM'),
  (10017, 'notificationEngine', 'reg.moduleName.notificationEngine', 'SYSTEM'),
  (10018, 'rbac', 'reg.moduleName.rbac', 'SYSTEM'),
  (10019, 'systemCode', 'reg.moduleName.systemCode', 'SYSTEM'),
  (10020, 'appManager', 'reg.moduleName.appManager', 'SYSTEM'),
  (10021, 'signature', 'reg.moduleName.signature', 'SYSTEM'),
  (10022, 'portal', 'reg.moduleName.portal', 'SYSTEM'),
  (10023, 'fileServer', 'reg.moduleName.fileServer', 'SYSTEM'),
  (10024, 'taskScheduler', 'reg.moduleName.taskScheduler', 'SYSTEM'),
  (10025, 'counter', 'reg.moduleName.counter', 'SYSTEM'),
  (10026, 'sysbase', 'reg.moduleName.sysbase', 'SYSTEM'),
  (10027, 'printer', 'reg.moduleName.printer', 'SYSTEM'),
  (10028, 'auditlog', 'reg.moduleName.auditlog', 'SYSTEM'),
  (10029, 'notificationMobile', 'reg.moduleName.notificationMobile', 'SYSTEM'),
  (10030, 'notificationSupplant', 'reg.moduleName.notificationSupplant', 'SYSTEM'),
  (10031, 'notificationSMS', 'reg.moduleName.notificationSMS', 'SYSTEM')
ON CONFLICT (module_id) DO UPDATE
SET module_name = EXCLUDED.module_name,
    module_type = EXCLUDED.module_type;

CREATE TABLE IF NOT EXISTS public.systemconfig_config_info (
  id bigint NOT NULL,
  catalog_id bigint DEFAULT NULL,
  sort numeric(20,6) DEFAULT NULL,
  code varchar(256) DEFAULT NULL,
  name varchar(256) DEFAULT NULL,
  app_code varchar(256) DEFAULT NULL,
  module_code varchar(256) DEFAULT NULL,
  widget_type integer DEFAULT NULL,
  default_value varchar(4000) DEFAULT NULL,
  widget_value varchar(4000) DEFAULT NULL,
  max_value integer DEFAULT NULL,
  min_value integer DEFAULT NULL,
  reg_format varchar(256) DEFAULT NULL,
  reg_message varchar(256) DEFAULT NULL,
  has_require boolean DEFAULT NULL,
  custom varchar(256) DEFAULT NULL,
  description varchar(256) DEFAULT NULL,
  creator varchar(256) NOT NULL DEFAULT 'system',
  create_time timestamp NOT NULL DEFAULT current_timestamp,
  modifier varchar(256) DEFAULT NULL,
  modify_time timestamp DEFAULT NULL,
  create_staff_id bigint DEFAULT NULL,
  modify_staff_id bigint DEFAULT NULL,
  tenant_id varchar(64) DEFAULT NULL,
  CONSTRAINT systemconfig_config_info_pkey PRIMARY KEY (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS udx_sysconf_info_code
  ON public.systemconfig_config_info(app_code, code);

CREATE TABLE IF NOT EXISTS public.systemconfig_config_version (
  id bigint NOT NULL,
  config_version varchar(256) NOT NULL,
  tid_module_key varchar(256) NOT NULL,
  creator varchar(256) NOT NULL DEFAULT 'system',
  create_time timestamp NOT NULL DEFAULT current_timestamp,
  modifier varchar(256) DEFAULT NULL,
  modify_time timestamp DEFAULT NULL,
  create_staff_id bigint DEFAULT NULL,
  modify_staff_id bigint DEFAULT NULL,
  tenant_id varchar(64) DEFAULT NULL,
  CONSTRAINT systemconfig_config_version_pkey PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS public.systemconfig_config_option (
  id bigint NOT NULL,
  config_id bigint DEFAULT NULL,
  sort numeric(20,0) DEFAULT NULL,
  label varchar(256) DEFAULT NULL,
  select_value varchar(256) DEFAULT NULL,
  creator varchar(256) NOT NULL DEFAULT 'system',
  create_time timestamp NOT NULL DEFAULT current_timestamp,
  modifier varchar(256) DEFAULT NULL,
  modify_time timestamp DEFAULT NULL,
  create_staff_id bigint DEFAULT NULL,
  modify_staff_id bigint DEFAULT NULL,
  tenant_id varchar(64) DEFAULT NULL,
  CONSTRAINT systemconfig_config_option_pkey PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS public.systemconfig_config_catalog (
  id bigint NOT NULL,
  parent_id bigint DEFAULT NULL,
  sort numeric(20,0) DEFAULT NULL,
  code varchar(256) DEFAULT NULL,
  name varchar(256) DEFAULT NULL,
  app_code varchar(256) DEFAULT NULL,
  catalog_type integer DEFAULT NULL,
  has_hide boolean DEFAULT false,
  creator varchar(256) NOT NULL DEFAULT 'system',
  create_time timestamp NOT NULL DEFAULT current_timestamp,
  modifier varchar(256) DEFAULT NULL,
  modify_time timestamp DEFAULT NULL,
  create_staff_id bigint DEFAULT NULL,
  modify_staff_id bigint DEFAULT NULL,
  tenant_id varchar(64) DEFAULT NULL,
  CONSTRAINT systemconfig_config_catalog_pkey PRIMARY KEY (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS udx_sysconf_catalog_code
  ON public.systemconfig_config_catalog(app_code, code);

INSERT INTO public.systemconfig_config_catalog(id, sort, code, parent_id, name, app_code, catalog_type, has_hide)
VALUES
  (1, 1, 'system', NULL, 'systemConfig.system', 'system', 1, false),
  (2, 2, 'app', NULL, 'systemConfig.app', 'app', 2, false),
  (3, 2, 'userAD', 1, 'systemConfig.userAD', 'system', 1, false),
  (4, 3, 'printer', 1, 'systemConfig.printer', 'printer', 1, false),
  (5, 4, 'AKSK', 1, 'systemConfig.AKSK', 'AKSK', 1, false),
  (6, 5, 'baseImages', 1, 'systemConfig.baseImages', 'baseImages', 1, false),
  (7, 6, 'passwordConfig', 1, 'systemConfig.passwordConfig', 'passwordConfig', 1, false)
ON CONFLICT (id) DO UPDATE
SET sort = EXCLUDED.sort,
    code = EXCLUDED.code,
    parent_id = EXCLUDED.parent_id,
    name = EXCLUDED.name,
    app_code = EXCLUDED.app_code,
    catalog_type = EXCLUDED.catalog_type,
    has_hide = EXCLUDED.has_hide;

INSERT INTO public.systemconfig_config_info(
  id, catalog_id, sort, code, name, app_code, module_code, widget_type,
  default_value, widget_value, max_value, min_value, reg_format, reg_message,
  has_require, custom, description
)
VALUES
  (1, 4, 1, 'spreadjs.licence', 'spreadjs.licence', 'printer', 'printer', 7,
   '', '', NULL, NULL, NULL, NULL, true, NULL, '')
ON CONFLICT (app_code, code) DO UPDATE
SET catalog_id = EXCLUDED.catalog_id,
    sort = EXCLUDED.sort,
    name = EXCLUDED.name,
    module_code = EXCLUDED.module_code,
    widget_type = EXCLUDED.widget_type,
    has_require = EXCLUDED.has_require;

CREATE TABLE IF NOT EXISTS public.sys_entity (
  id bigint NOT NULL,
  row_version bigint DEFAULT 0,
  type varchar(200) NOT NULL,
  code varchar(200) NOT NULL,
  name varchar(510) DEFAULT NULL,
  display_name varchar(1000) DEFAULT NULL,
  module_id varchar(200) DEFAULT NULL,
  cid bigint DEFAULT NULL,
  valid smallint DEFAULT 1,
  multi_flag smallint DEFAULT 0,
  sys_default smallint DEFAULT 0,
  memo varchar(600) DEFAULT NULL,
  source varchar(10) DEFAULT NULL,
  creator varchar(200) NOT NULL DEFAULT 'system',
  modifier varchar(200) DEFAULT NULL,
  create_staff_id bigint DEFAULT NULL,
  modify_staff_id bigint DEFAULT NULL,
  create_time timestamp NOT NULL DEFAULT current_timestamp,
  modify_time timestamp DEFAULT NULL,
  tenant_id varchar(64) DEFAULT NULL,
  CONSTRAINT sys_entity_pkey PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_sys_entity_code
  ON public.sys_entity(code);

CREATE TABLE IF NOT EXISTS public.sys_code (
  id bigint NOT NULL,
  row_version bigint DEFAULT 0,
  type varchar(200) DEFAULT NULL,
  code varchar(200) NOT NULL,
  entity_code varchar(200) NOT NULL,
  name varchar(510) DEFAULT NULL,
  display_name varchar(1000) NOT NULL,
  cid bigint DEFAULT NULL,
  valid smallint DEFAULT 1,
  leaf smallint DEFAULT 0,
  default_flag smallint DEFAULT 0,
  full_path varchar(4000) DEFAULT NULL,
  full_path_name varchar(4000) DEFAULT NULL,
  parent_id bigint DEFAULT NULL,
  parent_name varchar(200) DEFAULT NULL,
  lay_no integer DEFAULT NULL,
  lay_rec varchar(500) DEFAULT NULL,
  seq_id bigint DEFAULT NULL,
  sort numeric(20,4) DEFAULT NULL,
  des_a varchar(600) DEFAULT NULL,
  des_b varchar(600) DEFAULT NULL,
  des_c varchar(600) DEFAULT NULL,
  memo varchar(600) DEFAULT NULL,
  creator varchar(200) NOT NULL DEFAULT 'system',
  modifier varchar(200) DEFAULT NULL,
  create_staff_id bigint DEFAULT NULL,
  modify_staff_id bigint DEFAULT NULL,
  create_time timestamp NOT NULL DEFAULT current_timestamp,
  modify_time timestamp DEFAULT NULL,
  tenant_id varchar(64) DEFAULT NULL,
  CONSTRAINT sys_code_pkey PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_sys_code_entity_code
  ON public.sys_code(entity_code);

CREATE INDEX IF NOT EXISTS idx_sys_code_code
  ON public.sys_code(code);

INSERT INTO public.sys_entity(id, code, name, display_name, type, module_id, cid, sys_default, memo)
VALUES
  (1000, 'sys_gender', 'systemCode.sys_gender', '性别', 'list', 'sys', 1000, 1, '系统默认'),
  (1001, 'sys_education', 'systemCode.sys_education', '学历', 'list', 'sys', 1000, 1, '系统默认'),
  (1002, 'sys_person_status', 'systemCode.sys_person_status', '状态', 'list', 'sys', 1000, 1, '系统默认'),
  (1003, 'sys_classified_grade', 'systemCode.sys_classified_grade', '涉密等级', 'list', 'sys', 1000, 1, '系统默认'),
  (1004, 'sys_department_type', 'systemCode.sys_department_type', '部门类型', 'list', 'sys', 1000, 1, '系统默认'),
  (1005, 'sys_auth_user_directory', 'systemCode.sys_auth_user_directory', '用户目录类型', 'list', 'sys', 1000, 1, '系统默认'),
  (1006, 'sys_auth_protocol', 'systemCode.sys_auth_protocol', '认证协议类型', 'list', 'sys', 1000, 1, '系统默认'),
  (1007, 'sys_auth_grant', 'systemCode.sys_auth_grant', '授权类型', 'list', 'sys', 1000, 1, '系统默认'),
  (1008, 'sys_auth_method', 'systemCode.sys_auth_method', '认证方式', 'list', 'sys', 1000, 1, '系统默认'),
  (1009, 'sys_operate_type', 'systemCode.sys_operate_type', '操作类型', 'list', 'sys', 1000, 1, '系统默认'),
  (1011, 'sys_person_title', 'sys.sys_person_title', '职称', 'list', 'sys', 1000, 1, '系统默认')
ON CONFLICT (id) DO UPDATE
SET code = EXCLUDED.code,
    name = EXCLUDED.name,
    display_name = EXCLUDED.display_name,
    type = EXCLUDED.type,
    module_id = EXCLUDED.module_id,
    cid = EXCLUDED.cid,
    sys_default = EXCLUDED.sys_default,
    memo = EXCLUDED.memo;

INSERT INTO public.sys_code(
  id, code, name, display_name, entity_code, cid, default_flag,
  full_path, parent_name, lay_no, sort, memo
)
VALUES
  (1000, 'male', 'systemCode.male', '男', 'sys_gender', 1000, 1, 'male', 'systemCode.sys_gender', 1, 1, '系统默认'),
  (1001, 'female', 'systemCode.female', '女', 'sys_gender', 1000, 0, 'female', 'systemCode.sys_gender', 1, 2, '系统默认'),
  (1002, 'phd', 'systemCode.phd', '博士', 'sys_education', 1000, 1, 'phd', 'systemCode.sys_education', 1, 1, '系统默认'),
  (1003, 'master', 'systemCode.master', '硕士', 'sys_education', 1000, 0, 'master', 'systemCode.sys_education', 1, 2, '系统默认'),
  (1004, 'college', 'systemCode.college', '本科', 'sys_education', 1000, 0, 'college', 'systemCode.sys_education', 1, 3, '系统默认'),
  (1005, 'degree', 'systemCode.degree', '大专', 'sys_education', 1000, 0, 'degree', 'systemCode.sys_education', 1, 4, '系统默认'),
  (1006, 'specialOrOther', 'systemCode.specialOrOther', '中专或其他', 'sys_education', 1000, 0, 'specialOrOther', 'systemCode.sys_education', 1, 5, '系统默认'),
  (1007, 'onWork', 'systemCode.onWork', '在职', 'sys_person_status', 1000, 1, 'onWork', 'systemCode.sys_person_status', 1, 1, '系统默认'),
  (1008, 'offWork', 'systemCode.offWork', '离职', 'sys_person_status', 1000, 0, 'offWork', 'systemCode.sys_person_status', 1, 2, '系统默认'),
  (1009, 'unclassified', 'systemCode.unclassified', '非密', 'sys_classified_grade', 1000, 1, 'unclassified', 'systemCode.sys_classified_grade', 1, 1, '系统默认'),
  (1010, 'generalClassified', 'systemCode.generalClassified', '一般涉密', 'sys_classified_grade', 1000, 0, 'generalClassified', 'systemCode.sys_classified_grade', 1, 2, '系统默认'),
  (1011, 'importantClassified', 'systemCode.importantClassified', '重要涉密', 'sys_classified_grade', 1000, 0, 'importantClassified', 'systemCode.sys_classified_grade', 1, 3, '系统默认'),
  (1012, 'coreClassified', 'systemCode.coreClassified', '核心涉密', 'sys_classified_grade', 1000, 0, 'coreClassified', 'systemCode.sys_classified_grade', 1, 4, '系统默认'),
  (1013, 'general', 'systemCode.general', '普通部门', 'sys_department_type', 1000, 1, 'general', 'systemCode.sys_department_type', 1, 1, '系统默认'),
  (1014, 'emergency', 'systemCode.emergency', '应急部门', 'sys_department_type', 1000, 0, 'emergency', 'systemCode.sys_department_type', 1, 2, '系统默认'),
  (1015, 'msad', 'systemCode.msad', 'Microsoft活动目录', 'sys_auth_user_directory', 1000, 1, 'msad', 'systemCode.sys_auth_user_directory', 1, 1, '系统默认'),
  (1016, 'ldap', 'systemCode.ldap', 'LDAP', 'sys_auth_user_directory', 1000, 0, 'ldap', 'systemCode.sys_auth_user_directory', 1, 2, '系统默认'),
  (1017, 'oauth', 'systemCode.oauth', 'OAuth', 'sys_auth_protocol', 1000, 1, 'oauth', 'systemCode.sys_auth_protocol', 1, 1, '系统默认'),
  (1018, 'saml', 'systemCode.saml', 'SAML', 'sys_auth_protocol', 1000, 0, 'saml', 'systemCode.sys_auth_protocol', 1, 2, '系统默认'),
  (1019, 'authorizationCode', 'systemCode.authorizationCode', '授权码模式', 'sys_auth_grant', 1000, 1, 'authorizationCode', 'systemCode.sys_auth_grant', 1, 1, '系统默认'),
  (1020, 'implicit', 'systemCode.implicit', '隐式模式', 'sys_auth_grant', 1000, 0, 'implicit', 'systemCode.sys_auth_grant', 1, 2, '系统默认'),
  (1021, 'passwordCredentials', 'systemCode.passwordCredentials', '密码模式', 'sys_auth_grant', 1000, 0, 'passwordCredentials', 'systemCode.sys_auth_grant', 1, 3, '系统默认'),
  (1022, 'clientCredentials', 'systemCode.clientCredentials', '客户端凭证模式', 'sys_auth_grant', 1000, 0, 'clientCredentials', 'systemCode.sys_auth_grant', 1, 4, '系统默认'),
  (1023, 'np', 'systemCode.np', '用户名+密码', 'sys_auth_method', 1000, 1, 'np', 'systemCode.sys_auth_method', 1, 1, '系统默认'),
  (1024, 'npm', 'systemCode.npm', '用户名+密码+短信', 'sys_auth_method', 1000, 0, 'npm', 'systemCode.sys_auth_method', 1, 2, '系统默认')
ON CONFLICT (id) DO UPDATE
SET code = EXCLUDED.code,
    name = EXCLUDED.name,
    display_name = EXCLUDED.display_name,
    entity_code = EXCLUDED.entity_code,
    cid = EXCLUDED.cid,
    default_flag = EXCLUDED.default_flag,
    full_path = EXCLUDED.full_path,
    parent_name = EXCLUDED.parent_name,
    lay_no = EXCLUDED.lay_no,
    sort = EXCLUDED.sort,
    memo = EXCLUDED.memo;

CREATE OR REPLACE VIEW public.base_systementity AS
SELECT
  id,
  row_version AS version,
  type AS list_type,
  code,
  name,
  module_id AS module_code,
  cid,
  valid,
  multi_flag,
  sys_default,
  memo,
  NULL::bigint AS create_staff_id,
  NULL::bigint AS modify_staff_id,
  NULL::bigint AS delete_staff_id,
  create_time,
  modify_time,
  NULL::timestamp AS delete_time,
  NULL::varchar AS code_desc,
  NULL::varchar AS code_desb,
  NULL::varchar AS code_desa,
  NULL::varchar AS sys_class_code
FROM public.sys_entity
WHERE valid = 1;

CREATE OR REPLACE VIEW public.base_systemcode AS
SELECT
  entity_code || '/' || code AS id,
  row_version AS version,
  code,
  type,
  entity_code,
  name AS value,
  display_name AS value_zh_cn,
  cid,
  valid,
  leaf,
  default_flag,
  full_path_name,
  parent_id,
  lay_no,
  lay_rec,
  seq_id,
  sort,
  des_a AS code_desa,
  des_b AS code_desb,
  des_c AS code_desc,
  memo,
  NULL::bigint AS create_staff_id,
  NULL::bigint AS modify_staff_id,
  NULL::bigint AS delete_staff_id,
  create_time,
  modify_time,
  NULL::timestamp AS delete_time,
  NULL::varchar AS attribute
FROM public.sys_code
WHERE valid = 1;
