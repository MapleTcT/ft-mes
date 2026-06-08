CREATE TABLE IF NOT EXISTS public.org_company (
  id bigint NOT NULL,
  row_version bigint DEFAULT 0,
  code varchar(256) NOT NULL,
  description varchar(512) DEFAULT NULL,
  short_name varchar(256) NOT NULL,
  full_name varchar(512) NOT NULL,
  full_path varchar(4096) NOT NULL,
  address varchar(512) DEFAULT NULL,
  lay_no integer NOT NULL,
  lay_rec varchar(4096) NOT NULL,
  sort double precision NOT NULL,
  parent_id bigint DEFAULT NULL,
  old_id varchar(256) NOT NULL,
  valid smallint DEFAULT 1,
  creator varchar(32) NOT NULL DEFAULT 'system',
  modifier varchar(32) DEFAULT NULL,
  create_time timestamp NOT NULL DEFAULT current_timestamp,
  modify_time timestamp NOT NULL DEFAULT current_timestamp,
  create_staff_id bigint DEFAULT NULL,
  modify_staff_id bigint DEFAULT NULL,
  tenant_id varchar(64) DEFAULT NULL,
  CONSTRAINT org_company_pkey PRIMARY KEY (id),
  CONSTRAINT idx_company_old_id UNIQUE (old_id)
);

INSERT INTO public.org_company(id, code, short_name, full_name, full_path, lay_no, lay_rec, sort, old_id)
VALUES (1000, 'default_org_company', '默认公司', '默认公司', '/默认公司', 0, '1000', 0, 'Company_default_org_company')
ON CONFLICT DO NOTHING;

CREATE TABLE IF NOT EXISTS public.org_department (
  id bigint NOT NULL,
  row_version bigint DEFAULT 0,
  code varchar(256) NOT NULL,
  name varchar(256) NOT NULL,
  dept_type varchar(64) DEFAULT 'general',
  description varchar(512) DEFAULT NULL,
  full_path varchar(4096) NOT NULL,
  lay_rec varchar(4096) NOT NULL,
  lay_no integer NOT NULL,
  sort double precision NOT NULL,
  company_id bigint NOT NULL,
  parent_id bigint DEFAULT NULL,
  valid smallint DEFAULT 1,
  sys_flag smallint DEFAULT 0,
  old_id varchar(256) NOT NULL,
  leaf smallint DEFAULT 1,
  creator varchar(32) NOT NULL DEFAULT 'system',
  modifier varchar(32) DEFAULT NULL,
  create_time timestamp NOT NULL DEFAULT current_timestamp,
  modify_time timestamp NOT NULL DEFAULT current_timestamp,
  create_staff_id bigint DEFAULT NULL,
  modify_staff_id bigint DEFAULT NULL,
  tenant_id varchar(64) DEFAULT NULL,
  CONSTRAINT org_department_pkey PRIMARY KEY (id),
  CONSTRAINT idx_dept_old_id UNIQUE (old_id)
);

INSERT INTO public.org_department(id, code, name, dept_type, full_path, lay_rec, lay_no, sort, company_id, sys_flag, old_id)
VALUES (1, 'default_department', '虚拟部门', 'sys_department_type/general', '/默认公司/虚拟部门', '1', 1, 1000, 1000, 1, 'Department_1')
ON CONFLICT DO NOTHING;

CREATE TABLE IF NOT EXISTS public.org_position (
  id bigint NOT NULL,
  row_version bigint DEFAULT 0,
  code varchar(256) NOT NULL,
  name varchar(256) NOT NULL,
  description varchar(512) DEFAULT NULL,
  full_path varchar(4096) NOT NULL,
  lay_rec varchar(4096) NOT NULL,
  lay_no integer NOT NULL,
  sort double precision NOT NULL,
  company_id bigint NOT NULL,
  dep_id bigint NOT NULL,
  parent_id bigint DEFAULT NULL,
  valid smallint DEFAULT 1,
  sys_flag smallint DEFAULT 0,
  leaf smallint DEFAULT 1,
  old_id varchar(256) NOT NULL,
  creator varchar(32) NOT NULL DEFAULT 'system',
  modifier varchar(32) DEFAULT NULL,
  create_time timestamp NOT NULL DEFAULT current_timestamp,
  modify_time timestamp NOT NULL DEFAULT current_timestamp,
  create_staff_id bigint DEFAULT NULL,
  modify_staff_id bigint DEFAULT NULL,
  tenant_id varchar(64) DEFAULT NULL,
  CONSTRAINT org_position_pkey PRIMARY KEY (id),
  CONSTRAINT idx_pos_old_id UNIQUE (old_id)
);

INSERT INTO public.org_position(id, code, name, full_path, lay_rec, lay_no, sort, company_id, dep_id, sys_flag, old_id)
VALUES (1, 'default_position', '虚拟岗位', '/默认公司/虚拟岗位', '1', 1, 1000, 1000, 1, 1, 'Position_1')
ON CONFLICT DO NOTHING;

CREATE TABLE IF NOT EXISTS public.org_group (
  id bigint NOT NULL,
  row_version bigint DEFAULT 0,
  code varchar(256) NOT NULL,
  name varchar(256) NOT NULL,
  description varchar(512) DEFAULT NULL,
  full_path varchar(4096) DEFAULT NULL,
  sort double precision NOT NULL,
  company_id bigint NOT NULL,
  manager_id bigint DEFAULT NULL,
  manager_name varchar(256) DEFAULT NULL,
  valid smallint DEFAULT 1,
  creator varchar(32) NOT NULL DEFAULT 'system',
  modifier varchar(32) DEFAULT NULL,
  create_time timestamp NOT NULL DEFAULT current_timestamp,
  modify_time timestamp NOT NULL DEFAULT current_timestamp,
  create_staff_id bigint DEFAULT NULL,
  modify_staff_id bigint DEFAULT NULL,
  tenant_id varchar(64) DEFAULT NULL,
  CONSTRAINT org_group_pkey PRIMARY KEY (id),
  CONSTRAINT udx_grp_org_code UNIQUE (company_id, code)
);

CREATE TABLE IF NOT EXISTS public.scheduler_job_info (
  id bigint NOT NULL,
  model_name varchar(64) NOT NULL,
  module_code varchar(256),
  job_name varchar(256) NOT NULL,
  code varchar(256) NOT NULL,
  job_key varchar(64) NOT NULL,
  job_desc varchar(510),
  job_cron varchar(550) NOT NULL,
  job_status integer DEFAULT 0,
  job_service_api varchar(510) NOT NULL,
  job_service_params varchar(510),
  job_call_no bigint DEFAULT 0,
  last_time timestamp NOT NULL DEFAULT current_timestamp,
  next_time timestamp NOT NULL DEFAULT current_timestamp,
  user_name varchar(64) NOT NULL,
  delete_time timestamp DEFAULT NULL,
  modify_time timestamp DEFAULT current_timestamp,
  create_time timestamp DEFAULT current_timestamp,
  terminator varchar(32),
  modifier varchar(32),
  creator varchar(32),
  create_staff_id bigint,
  modify_staff_id bigint,
  tenant_id varchar(64) DEFAULT NULL,
  CONSTRAINT scheduler_job_info_pkey PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS index_jobinfo_code ON public.scheduler_job_info(code);

CREATE TABLE IF NOT EXISTS public.scheduler_job_log_info (
  id bigint NOT NULL,
  model_name varchar(64) NOT NULL,
  job_name varchar(256) NOT NULL,
  code varchar(256) NOT NULL,
  job_service_api varchar(510) NOT NULL,
  job_service_params varchar(510),
  job_message varchar(510),
  job_status integer,
  exception_info varchar(510),
  user_name varchar(64) NOT NULL,
  delete_time timestamp DEFAULT NULL,
  modify_time timestamp DEFAULT current_timestamp,
  create_time timestamp DEFAULT current_timestamp,
  terminator varchar(32),
  modifier varchar(32),
  creator varchar(32),
  create_staff_id bigint,
  modify_staff_id bigint,
  tenant_id varchar(64) DEFAULT NULL,
  CONSTRAINT scheduler_job_log_info_pkey PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS index_jobloginfo_code ON public.scheduler_job_log_info(code);
CREATE INDEX IF NOT EXISTS index_jobloginfo_model_name ON public.scheduler_job_log_info(model_name);
CREATE INDEX IF NOT EXISTS index_jobloginfo_job_status ON public.scheduler_job_log_info(job_status);

CREATE TABLE IF NOT EXISTS public.qrtz_blob_triggers (
  sched_name varchar(120) NOT NULL,
  trigger_name varchar(200) NOT NULL,
  trigger_group varchar(200) NOT NULL,
  blob_data bytea,
  CONSTRAINT qrtz_blob_triggers_pkey PRIMARY KEY (sched_name, trigger_name, trigger_group)
);

CREATE INDEX IF NOT EXISTS idx_qrtz_blob_triggers ON public.qrtz_blob_triggers(sched_name, trigger_name, trigger_group);
