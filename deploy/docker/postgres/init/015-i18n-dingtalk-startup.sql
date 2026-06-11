-- Runtime startup tables for base-package services whose original SQL is
-- shipped in non-PostgreSQL dialects.

CREATE TABLE IF NOT EXISTS public.supfusion_i18n_excel (
  id bigint PRIMARY KEY,
  status integer NOT NULL,
  file_name varchar(255) NOT NULL,
  error_file varchar(255),
  error_message varchar(255),
  operate_type varchar(100) NOT NULL,
  error_num integer DEFAULT 0,
  add_num integer DEFAULT 0,
  update_num integer DEFAULT 0,
  all_num integer DEFAULT 0,
  valid varchar(16) DEFAULT '1',
  tenant_id varchar(64) DEFAULT 'dt',
  creator varchar(32) DEFAULT 'system',
  create_time timestamp DEFAULT CURRENT_TIMESTAMP,
  create_staff_id bigint DEFAULT 0,
  modifier varchar(32),
  modify_time timestamp,
  modify_staff_id bigint
);

CREATE TABLE IF NOT EXISTS public.supfusion_i18n_index (
  id bigint PRIMARY KEY,
  module_code varchar(256) NOT NULL,
  module_index_code varchar(256) NOT NULL,
  valid varchar(16) DEFAULT '1',
  tenant_id varchar(64) DEFAULT 'dt',
  creator varchar(32) DEFAULT 'system',
  create_time timestamp DEFAULT CURRENT_TIMESTAMP,
  create_staff_id bigint DEFAULT 0,
  modifier varchar(32),
  modify_time timestamp,
  modify_staff_id bigint
);

CREATE TABLE IF NOT EXISTS public.supfusion_i18n_language (
  id bigint PRIMARY KEY,
  langu_code varchar(256) NOT NULL,
  langu_type varchar(256) NOT NULL,
  langu_name varchar(256) NOT NULL,
  has_used varchar(16) DEFAULT '1',
  valid varchar(16) DEFAULT '1',
  tenant_id varchar(64) DEFAULT 'dt',
  creator varchar(32) DEFAULT 'system',
  create_time timestamp DEFAULT CURRENT_TIMESTAMP,
  create_staff_id bigint DEFAULT 0,
  modifier varchar(32),
  modify_time timestamp,
  modify_staff_id bigint
);

CREATE TABLE IF NOT EXISTS public.supfusion_i18n_resource (
  id bigint PRIMARY KEY,
  i18n_key varchar(255) NOT NULL,
  i18n_value varchar(1024),
  langu_code varchar(255) NOT NULL,
  module_code varchar(255) NOT NULL,
  module_version_code varchar(256),
  valid varchar(16) DEFAULT '1',
  tenant_id varchar(64) DEFAULT 'dt',
  creator varchar(32) DEFAULT 'system',
  create_time timestamp DEFAULT CURRENT_TIMESTAMP,
  create_staff_id bigint DEFAULT 0,
  modifier varchar(32),
  modify_time timestamp,
  modify_staff_id bigint
);

CREATE TABLE IF NOT EXISTS public.supfusion_i18n_token (
  id bigint PRIMARY KEY,
  module_code varchar(256) NOT NULL,
  has_lock smallint DEFAULT 0,
  token varchar(255) NOT NULL,
  valid varchar(16) DEFAULT '1',
  creator varchar(32) DEFAULT 'system',
  create_time timestamp DEFAULT CURRENT_TIMESTAMP,
  create_staff_id bigint DEFAULT 0,
  modifier varchar(32),
  modify_time timestamp,
  modify_staff_id bigint
);

CREATE TABLE IF NOT EXISTS public.supfusion_i18n_version (
  id bigint PRIMARY KEY,
  module_code varchar(256) NOT NULL,
  module_version_code varchar(256) NOT NULL,
  valid varchar(16) DEFAULT '1',
  creator varchar(32) DEFAULT 'system',
  create_time timestamp DEFAULT CURRENT_TIMESTAMP,
  create_staff_id bigint DEFAULT 0,
  modifier varchar(32),
  modify_time timestamp,
  modify_staff_id bigint
);

CREATE UNIQUE INDEX IF NOT EXISTS udx_i18n_index_code_tenant
  ON public.supfusion_i18n_index(module_code, tenant_id);
CREATE UNIQUE INDEX IF NOT EXISTS udx_i18n_version_module_code
  ON public.supfusion_i18n_version(module_code);
CREATE INDEX IF NOT EXISTS idx_i18n_resource_key
  ON public.supfusion_i18n_resource(i18n_key);
CREATE INDEX IF NOT EXISTS idx_i18n_resource_language
  ON public.supfusion_i18n_resource(langu_code);
CREATE INDEX IF NOT EXISTS idx_i18n_resource_module
  ON public.supfusion_i18n_resource(module_code);
CREATE INDEX IF NOT EXISTS idx_i18n_resource_modify_time
  ON public.supfusion_i18n_resource(modify_time);

INSERT INTO public.supfusion_i18n_language (
  id, langu_code, langu_type, langu_name, has_used, valid, tenant_id, creator,
  create_time, create_staff_id, modifier, modify_time, modify_staff_id
) VALUES
  (1, 'zh_CN', 'i18n.language_tips_zh_cn', '中文（简体）', '1', '1', 'dt', 'system', CURRENT_TIMESTAMP, 1, NULL, CURRENT_TIMESTAMP, NULL),
  (2, 'zh_HK', 'i18n.language_tips_zh_hk', '中文（繁体）', '0', '1', 'dt', 'system', CURRENT_TIMESTAMP, 1, NULL, CURRENT_TIMESTAMP, NULL),
  (3, 'en_US', 'i18n.language_tips_en_us', 'English', '1', '1', 'dt', 'system', CURRENT_TIMESTAMP, 1, NULL, CURRENT_TIMESTAMP, NULL),
  (4, 'id_ID', 'i18n.language_tips_id_id', 'IndonesiaName', '0', '1', 'dt', 'system', CURRENT_TIMESTAMP, 1, NULL, CURRENT_TIMESTAMP, NULL)
ON CONFLICT (id) DO NOTHING;

UPDATE public.supfusion_i18n_excel SET tenant_id = 'dt' WHERE tenant_id IS NULL;
UPDATE public.supfusion_i18n_index SET tenant_id = 'dt' WHERE tenant_id IS NULL;
UPDATE public.supfusion_i18n_language SET tenant_id = 'dt' WHERE tenant_id IS NULL;
UPDATE public.supfusion_i18n_resource SET tenant_id = 'dt' WHERE tenant_id IS NULL;

CREATE TABLE IF NOT EXISTS public.ding_person (
  id bigint PRIMARY KEY,
  ding_user_id varchar(100) NOT NULL,
  name varchar(50),
  phone varchar(256),
  sup_person_id bigint NOT NULL,
  sup_person_code varchar(50) NOT NULL,
  creator varchar(32) NOT NULL,
  create_staff_id bigint NOT NULL,
  create_time timestamp NOT NULL,
  modifier varchar(32),
  modify_staff_id bigint,
  modify_time timestamp
);

CREATE TABLE IF NOT EXISTS public.ding_sup_person (
  id bigint PRIMARY KEY,
  company_id bigint,
  name varchar(50),
  phone varchar(256),
  main_position bigint,
  code varchar(50) NOT NULL,
  status varchar(200),
  valid smallint DEFAULT 1,
  creator varchar(32) NOT NULL,
  create_staff_id bigint NOT NULL,
  create_time timestamp NOT NULL,
  modifier varchar(32),
  modify_staff_id bigint,
  modify_time timestamp
);

CREATE TABLE IF NOT EXISTS public.ding_task (
  id bigint PRIMARY KEY,
  task_id bigint NOT NULL,
  message_id varchar(100) NOT NULL,
  ding_user_id varchar(100) NOT NULL,
  status varchar(100),
  sharding_time bigint,
  send_status smallint,
  read_status smallint,
  error_result varchar(200),
  creator varchar(32) NOT NULL,
  create_staff_id bigint NOT NULL,
  create_time timestamp NOT NULL,
  modifier varchar(32),
  modify_staff_id bigint,
  modify_time timestamp
);
