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

CREATE TABLE IF NOT EXISTS public.org_person (
  id bigint NOT NULL,
  row_version bigint DEFAULT 0,
  code varchar(50) NOT NULL,
  name varchar(50) NOT NULL,
  old_id varchar(50) DEFAULT NULL,
  description varchar(512) DEFAULT NULL,
  gender varchar(200) DEFAULT NULL,
  phone varchar(256) DEFAULT NULL,
  email varchar(256) DEFAULT NULL,
  status varchar(200) DEFAULT NULL,
  classified_level varchar(200) DEFAULT NULL,
  valid smallint DEFAULT 1,
  sys_flag smallint DEFAULT 0,
  main_position bigint DEFAULT NULL,
  direct_leader_id bigint DEFAULT NULL,
  grand_leader_id bigint DEFAULT NULL,
  create_user smallint DEFAULT 0,
  user_id bigint DEFAULT NULL,
  user_name varchar(256) DEFAULT NULL,
  avatar_url varchar(256) DEFAULT NULL,
  creator varchar(32) NOT NULL DEFAULT 'system',
  modifier varchar(32) DEFAULT NULL,
  create_time timestamp NOT NULL DEFAULT current_timestamp,
  modify_time timestamp NOT NULL DEFAULT current_timestamp,
  create_staff_id bigint DEFAULT NULL,
  modify_staff_id bigint DEFAULT NULL,
  tenant_id varchar(64) DEFAULT NULL,
  CONSTRAINT org_person_pkey PRIMARY KEY (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS udx_person_old_id
  ON public.org_person(old_id);

INSERT INTO public.org_person(
  id, code, name, old_id, description, status, sys_flag, main_position, user_id, user_name
)
VALUES (
  1, 'default_person', '虚拟人员', 'Person_1', '虚拟人员', 'sys_person_status/onWork', 1, 1, 1, 'admin'
)
ON CONFLICT (id) DO UPDATE
SET description = EXCLUDED.description,
    user_id = COALESCE(public.org_person.user_id, EXCLUDED.user_id),
    user_name = COALESCE(public.org_person.user_name, EXCLUDED.user_name);

CREATE TABLE IF NOT EXISTS public.org_person_position (
  id bigint NOT NULL,
  row_version bigint DEFAULT 0,
  position_id bigint NOT NULL,
  person_id bigint NOT NULL,
  work_time date DEFAULT NULL,
  off_time date DEFAULT NULL,
  remark varchar(256) DEFAULT NULL,
  valid smallint DEFAULT 1,
  creator varchar(32) NOT NULL DEFAULT 'system',
  modifier varchar(32) DEFAULT NULL,
  create_time timestamp NOT NULL DEFAULT current_timestamp,
  modify_time timestamp NOT NULL DEFAULT current_timestamp,
  create_staff_id bigint DEFAULT NULL,
  modify_staff_id bigint DEFAULT NULL,
  tenant_id varchar(64) DEFAULT NULL,
  CONSTRAINT org_person_position_pkey PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_person_position
  ON public.org_person_position(valid, position_id, person_id);

INSERT INTO public.org_person_position(id, position_id, person_id)
VALUES (1, 1, 1)
ON CONFLICT (id) DO UPDATE
SET position_id = EXCLUDED.position_id,
    person_id = EXCLUDED.person_id;

CREATE TABLE IF NOT EXISTS public.org_person_department (
  id bigint NOT NULL,
  row_version bigint DEFAULT 0,
  dept_id bigint NOT NULL,
  person_id bigint NOT NULL,
  position_id bigint NOT NULL,
  valid smallint DEFAULT 1,
  creator varchar(32) NOT NULL DEFAULT 'system',
  modifier varchar(32) DEFAULT NULL,
  create_time timestamp NOT NULL DEFAULT current_timestamp,
  modify_time timestamp NOT NULL DEFAULT current_timestamp,
  create_staff_id bigint DEFAULT NULL,
  modify_staff_id bigint DEFAULT NULL,
  tenant_id varchar(64) DEFAULT NULL,
  CONSTRAINT org_person_department_pkey PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_person_dept_ids
  ON public.org_person_department(valid, dept_id, person_id, position_id);

INSERT INTO public.org_person_department(id, dept_id, position_id, person_id)
VALUES (1, 1, 1, 1)
ON CONFLICT (id) DO UPDATE
SET dept_id = EXCLUDED.dept_id,
    position_id = EXCLUDED.position_id,
    person_id = EXCLUDED.person_id;

CREATE TABLE IF NOT EXISTS public.org_person_company (
  id bigint NOT NULL,
  row_version bigint DEFAULT 0,
  company_id bigint NOT NULL,
  person_id bigint NOT NULL,
  position_id bigint NOT NULL,
  valid smallint DEFAULT 1,
  creator varchar(32) NOT NULL DEFAULT 'system',
  modifier varchar(32) DEFAULT NULL,
  create_time timestamp NOT NULL DEFAULT current_timestamp,
  modify_time timestamp NOT NULL DEFAULT current_timestamp,
  create_staff_id bigint DEFAULT NULL,
  modify_staff_id bigint DEFAULT NULL,
  tenant_id varchar(64) DEFAULT NULL,
  CONSTRAINT org_person_company_pkey PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_person_company_ids
  ON public.org_person_company(valid, company_id, person_id, position_id);

INSERT INTO public.org_person_company(id, company_id, position_id, person_id)
VALUES (1, 1000, 1, 1)
ON CONFLICT (id) DO UPDATE
SET company_id = EXCLUDED.company_id,
    position_id = EXCLUDED.position_id,
    person_id = EXCLUDED.person_id;

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
  callback_flag boolean DEFAULT false,
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
  callback_time timestamp DEFAULT NULL,
  callback_data varchar(510),
  CONSTRAINT scheduler_job_log_info_pkey PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS index_jobloginfo_code ON public.scheduler_job_log_info(code);
CREATE INDEX IF NOT EXISTS index_jobloginfo_model_name ON public.scheduler_job_log_info(model_name);
CREATE INDEX IF NOT EXISTS index_jobloginfo_job_status ON public.scheduler_job_log_info(job_status);

CREATE TABLE IF NOT EXISTS public.scheduler_job_operation_log (
  id bigint NOT NULL,
  model_code varchar(64),
  job_name varchar(256),
  code varchar(64),
  user_name varchar(64),
  operation varchar(64),
  source varchar(64),
  creator varchar(32) DEFAULT NULL,
  modifier varchar(32) DEFAULT NULL,
  terminator varchar(32) DEFAULT NULL,
  create_time timestamp DEFAULT current_timestamp,
  modify_time timestamp DEFAULT current_timestamp,
  delete_time timestamp DEFAULT NULL,
  create_staff_id bigint,
  modify_staff_id bigint,
  CONSTRAINT scheduler_job_operation_log_pkey PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS public.license_info (
  id bigint NOT NULL,
  module_code varchar(200) NOT NULL,
  license_key varchar(256) NOT NULL,
  value varchar(50) NOT NULL,
  description varchar(256) DEFAULT NULL,
  application_name varchar(256) DEFAULT NULL,
  application_type varchar(256) DEFAULT NULL,
  time varchar(256) NOT NULL,
  hash_code varchar(256) NOT NULL,
  valid smallint DEFAULT 1,
  creator varchar(32) DEFAULT NULL,
  modifier varchar(32) DEFAULT NULL,
  create_time timestamp NOT NULL DEFAULT current_timestamp,
  modify_time timestamp NOT NULL DEFAULT current_timestamp,
  create_staff_id bigint DEFAULT NULL,
  modify_staff_id bigint DEFAULT NULL,
  CONSTRAINT license_info_pkey PRIMARY KEY (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS unique_module_code ON public.license_info(module_code);

CREATE TABLE IF NOT EXISTS public.license_info_date (
  id bigint NOT NULL,
  time varchar(256) NOT NULL,
  valid smallint DEFAULT 1,
  creator varchar(32) DEFAULT NULL,
  modifier varchar(32) DEFAULT NULL,
  create_time timestamp NOT NULL DEFAULT current_timestamp,
  modify_time timestamp NOT NULL DEFAULT current_timestamp,
  create_staff_id bigint DEFAULT NULL,
  modify_staff_id bigint DEFAULT NULL,
  CONSTRAINT license_info_date_pkey PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS public.qrtz_job_details (
  sched_name varchar(120) NOT NULL,
  job_name varchar(200) NOT NULL,
  job_group varchar(200) NOT NULL,
  description varchar(250),
  job_class_name varchar(250) NOT NULL,
  is_durable varchar(8) NOT NULL,
  is_nonconcurrent varchar(8) NOT NULL,
  is_update_data varchar(8) NOT NULL,
  requests_recovery varchar(8) NOT NULL,
  job_data bytea,
  CONSTRAINT qrtz_job_details_pkey PRIMARY KEY (sched_name, job_name, job_group)
);

CREATE TABLE IF NOT EXISTS public.qrtz_triggers (
  sched_name varchar(120) NOT NULL,
  trigger_name varchar(200) NOT NULL,
  trigger_group varchar(200) NOT NULL,
  job_name varchar(200) NOT NULL,
  job_group varchar(200) NOT NULL,
  description varchar(250),
  next_fire_time bigint,
  prev_fire_time bigint,
  priority integer,
  trigger_state varchar(16) NOT NULL,
  trigger_type varchar(8) NOT NULL,
  start_time bigint NOT NULL,
  end_time bigint,
  calendar_name varchar(200),
  misfire_instr smallint,
  job_data bytea,
  CONSTRAINT qrtz_triggers_pkey PRIMARY KEY (sched_name, trigger_name, trigger_group)
);

CREATE TABLE IF NOT EXISTS public.qrtz_simple_triggers (
  sched_name varchar(120) NOT NULL,
  trigger_name varchar(200) NOT NULL,
  trigger_group varchar(200) NOT NULL,
  repeat_count bigint NOT NULL,
  repeat_interval bigint NOT NULL,
  times_triggered bigint NOT NULL,
  CONSTRAINT qrtz_simple_triggers_pkey PRIMARY KEY (sched_name, trigger_name, trigger_group)
);

CREATE TABLE IF NOT EXISTS public.qrtz_cron_triggers (
  sched_name varchar(120) NOT NULL,
  trigger_name varchar(200) NOT NULL,
  trigger_group varchar(200) NOT NULL,
  cron_expression varchar(550) NOT NULL,
  time_zone_id varchar(80),
  CONSTRAINT qrtz_cron_triggers_pkey PRIMARY KEY (sched_name, trigger_name, trigger_group)
);

CREATE TABLE IF NOT EXISTS public.qrtz_simprop_triggers (
  sched_name varchar(120) NOT NULL,
  trigger_name varchar(200) NOT NULL,
  trigger_group varchar(200) NOT NULL,
  str_prop_1 varchar(512),
  str_prop_2 varchar(512),
  str_prop_3 varchar(512),
  int_prop_1 integer,
  int_prop_2 integer,
  long_prop_1 bigint,
  long_prop_2 bigint,
  dec_prop_1 numeric(13,4),
  dec_prop_2 numeric(13,4),
  bool_prop_1 varchar(8),
  bool_prop_2 varchar(8),
  CONSTRAINT qrtz_simprop_triggers_pkey PRIMARY KEY (sched_name, trigger_name, trigger_group)
);

CREATE TABLE IF NOT EXISTS public.qrtz_blob_triggers (
  sched_name varchar(120) NOT NULL,
  trigger_name varchar(200) NOT NULL,
  trigger_group varchar(200) NOT NULL,
  blob_data bytea,
  CONSTRAINT qrtz_blob_triggers_pkey PRIMARY KEY (sched_name, trigger_name, trigger_group)
);

CREATE TABLE IF NOT EXISTS public.qrtz_calendars (
  sched_name varchar(120) NOT NULL,
  calendar_name varchar(200) NOT NULL,
  calendar bytea NOT NULL,
  CONSTRAINT qrtz_calendars_pkey PRIMARY KEY (sched_name, calendar_name)
);

CREATE TABLE IF NOT EXISTS public.qrtz_paused_trigger_grps (
  sched_name varchar(120) NOT NULL,
  trigger_group varchar(200) NOT NULL,
  CONSTRAINT qrtz_paused_trigger_grps_pkey PRIMARY KEY (sched_name, trigger_group)
);

CREATE TABLE IF NOT EXISTS public.qrtz_fired_triggers (
  sched_name varchar(120) NOT NULL,
  entry_id varchar(95) NOT NULL,
  trigger_name varchar(200) NOT NULL,
  trigger_group varchar(200) NOT NULL,
  instance_name varchar(200) NOT NULL,
  fired_time bigint NOT NULL,
  sched_time bigint NOT NULL,
  priority integer NOT NULL,
  state varchar(16) NOT NULL,
  job_name varchar(200),
  job_group varchar(200),
  is_nonconcurrent varchar(8),
  requests_recovery varchar(8),
  CONSTRAINT qrtz_fired_triggers_pkey PRIMARY KEY (sched_name, entry_id)
);

CREATE TABLE IF NOT EXISTS public.qrtz_scheduler_state (
  sched_name varchar(120) NOT NULL,
  instance_name varchar(200) NOT NULL,
  last_checkin_time bigint NOT NULL,
  checkin_interval bigint NOT NULL,
  CONSTRAINT qrtz_scheduler_state_pkey PRIMARY KEY (sched_name, instance_name)
);

CREATE TABLE IF NOT EXISTS public.qrtz_locks (
  sched_name varchar(120) NOT NULL,
  lock_name varchar(40) NOT NULL,
  CONSTRAINT qrtz_locks_pkey PRIMARY KEY (sched_name, lock_name)
);

CREATE INDEX IF NOT EXISTS idx_qrtz_j_req_recovery ON public.qrtz_job_details(sched_name, requests_recovery);
CREATE INDEX IF NOT EXISTS idx_qrtz_j_grp ON public.qrtz_job_details(sched_name, job_group);
CREATE INDEX IF NOT EXISTS idx_qrtz_t_j ON public.qrtz_triggers(sched_name, job_name, job_group);
CREATE INDEX IF NOT EXISTS idx_qrtz_t_jg ON public.qrtz_triggers(sched_name, job_group);
CREATE INDEX IF NOT EXISTS idx_qrtz_t_c ON public.qrtz_triggers(sched_name, calendar_name);
CREATE INDEX IF NOT EXISTS idx_qrtz_t_g ON public.qrtz_triggers(sched_name, trigger_group);
CREATE INDEX IF NOT EXISTS idx_qrtz_t_state ON public.qrtz_triggers(sched_name, trigger_state);
CREATE INDEX IF NOT EXISTS idx_qrtz_t_n_state ON public.qrtz_triggers(sched_name, trigger_name, trigger_group, trigger_state);
CREATE INDEX IF NOT EXISTS idx_qrtz_t_n_g_state ON public.qrtz_triggers(sched_name, trigger_group, trigger_state);
CREATE INDEX IF NOT EXISTS idx_qrtz_t_next_fire_time ON public.qrtz_triggers(sched_name, next_fire_time);
CREATE INDEX IF NOT EXISTS idx_qrtz_t_nft_st ON public.qrtz_triggers(sched_name, trigger_state, next_fire_time);
CREATE INDEX IF NOT EXISTS idx_qrtz_t_nft_misfire ON public.qrtz_triggers(sched_name, misfire_instr, next_fire_time);
CREATE INDEX IF NOT EXISTS idx_qrtz_t_nft_st_misfire ON public.qrtz_triggers(sched_name, misfire_instr, next_fire_time, trigger_state);
CREATE INDEX IF NOT EXISTS idx_qrtz_t_nft_st_misfire_grp ON public.qrtz_triggers(sched_name, misfire_instr, next_fire_time, trigger_group, trigger_state);
CREATE INDEX IF NOT EXISTS idx_qrtz_blob_triggers ON public.qrtz_blob_triggers(sched_name, trigger_name, trigger_group);
CREATE INDEX IF NOT EXISTS idx_qrtz_ft_trig_inst_name ON public.qrtz_fired_triggers(sched_name, instance_name);
CREATE INDEX IF NOT EXISTS idx_qrtz_ft_inst_job_req_rcvry ON public.qrtz_fired_triggers(sched_name, instance_name, requests_recovery);
CREATE INDEX IF NOT EXISTS idx_qrtz_ft_j_g ON public.qrtz_fired_triggers(sched_name, job_name, job_group);
CREATE INDEX IF NOT EXISTS idx_qrtz_ft_jg ON public.qrtz_fired_triggers(sched_name, job_group);
CREATE INDEX IF NOT EXISTS idx_qrtz_ft_t_g ON public.qrtz_fired_triggers(sched_name, trigger_name, trigger_group);
CREATE INDEX IF NOT EXISTS idx_qrtz_ft_tg ON public.qrtz_fired_triggers(sched_name, trigger_group);
