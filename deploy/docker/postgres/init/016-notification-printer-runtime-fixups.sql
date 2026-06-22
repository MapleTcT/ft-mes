-- Additional PostgreSQL runtime tables and type fixups found during full
-- service startup QA of the ADP base package.

CREATE TABLE IF NOT EXISTS public.wechat_person (
  id bigint PRIMARY KEY,
  wx_user_id varchar(100) NOT NULL,
  name varchar(50),
  phone varchar(256),
  sup_person_id bigint NOT NULL,
  sup_person_code varchar(50) NOT NULL,
  corp_id varchar(200),
  creator varchar(32) NOT NULL DEFAULT 'system',
  create_staff_id bigint NOT NULL DEFAULT 0,
  create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  modifier varchar(32),
  modify_staff_id bigint,
  modify_time timestamp
);

CREATE TABLE IF NOT EXISTS public.wechat_msg (
  id bigint PRIMARY KEY,
  message_id varchar(100) NOT NULL,
  wx_user_id varchar(100) NOT NULL,
  sharding_time bigint,
  send_status smallint,
  read_status smallint,
  error_result varchar(200),
  corp_id varchar(200),
  status integer,
  creator varchar(32) NOT NULL DEFAULT 'system',
  create_staff_id bigint NOT NULL DEFAULT 0,
  create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  modifier varchar(32),
  modify_staff_id bigint,
  modify_time timestamp
);

CREATE TABLE IF NOT EXISTS public.printer_register (
  id bigint PRIMARY KEY,
  source smallint NOT NULL,
  service_url varchar(1024) NOT NULL,
  service_type smallint NOT NULL,
  call_type smallint NOT NULL,
  creator varchar(32) DEFAULT 'system',
  modifier varchar(32),
  create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  modify_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_staff_id bigint DEFAULT 0,
  modify_staff_id bigint
);

CREATE TABLE IF NOT EXISTS public.printer_log (
  id bigint PRIMARY KEY,
  row_version bigint DEFAULT 0,
  template_id bigint NOT NULL,
  page_id varchar(128) NOT NULL,
  creator varchar(32) DEFAULT 'system',
  modifier varchar(32),
  create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  modify_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_staff_id bigint DEFAULT 0,
  modify_staff_id bigint
);

CREATE TABLE IF NOT EXISTS public.printer_label (
  id bigint PRIMARY KEY,
  label_name varchar(128) NOT NULL,
  creator varchar(32) DEFAULT 'system',
  modifier varchar(32),
  create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  modify_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_staff_id bigint DEFAULT 0,
  modify_staff_id bigint
);

CREATE TABLE IF NOT EXISTS public.printer_template (
  id bigint PRIMARY KEY,
  template_name varchar(128) NOT NULL,
  i18n_key varchar(128),
  template_code varchar(128) NOT NULL,
  app_id varchar(128) NOT NULL,
  label_names varchar(128),
  template_desc varchar(512),
  enabled smallint DEFAULT 1,
  valid smallint DEFAULT 1,
  start_time timestamp,
  end_time timestamp,
  creator varchar(32) DEFAULT 'system',
  modifier varchar(32),
  create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  modify_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_staff_id bigint DEFAULT 0,
  modify_staff_id bigint
);

CREATE TABLE IF NOT EXISTS public.printer_design_content (
  template_id bigint NOT NULL,
  content text,
  valid smallint DEFAULT 1,
  process_key varchar(128),
  creator varchar(32) DEFAULT 'system',
  modifier varchar(32),
  create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  modify_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_staff_id bigint DEFAULT 0,
  modify_staff_id bigint
);

CREATE INDEX IF NOT EXISTS idx_printer_design_content_template_valid
  ON public.printer_design_content(template_id, valid);

CREATE TABLE IF NOT EXISTS public.printer_object_iframe (
  id bigint PRIMARY KEY,
  name varchar(128) NOT NULL,
  source smallint NOT NULL,
  url varchar(1024) NOT NULL,
  valid smallint DEFAULT 1,
  creator varchar(32) DEFAULT 'system',
  modifier varchar(32),
  create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  modify_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_staff_id bigint DEFAULT 0,
  modify_staff_id bigint
);

CREATE TABLE IF NOT EXISTS public.printer_template_relation_page (
  id bigint PRIMARY KEY,
  template_id bigint NOT NULL,
  page_id varchar(128) NOT NULL,
  model_code varchar(128) NOT NULL,
  creator varchar(32) DEFAULT 'system',
  modifier varchar(32),
  create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  modify_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_staff_id bigint DEFAULT 0,
  modify_staff_id bigint
);

CREATE TABLE IF NOT EXISTS public.printer_script (
  id bigint PRIMARY KEY,
  entity_code varchar(510) NOT NULL,
  before_script text,
  after_script text,
  creator varchar(32) DEFAULT 'system',
  modifier varchar(32),
  create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  modify_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_staff_id bigint DEFAULT 0,
  modify_staff_id bigint
);

DO $$
BEGIN
  IF to_regclass('public.ding_task') IS NOT NULL THEN
    IF EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = 'public'
        AND table_name = 'ding_task'
        AND column_name = 'status'
        AND data_type <> 'integer'
    ) THEN
      ALTER TABLE public.ding_task
        ALTER COLUMN status TYPE integer
        USING CASE WHEN status ~ '^-?[0-9]+$' THEN status::integer ELSE NULL END;
    END IF;
  END IF;

  IF to_regclass('public.wechat_msg') IS NOT NULL THEN
    IF EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = 'public'
        AND table_name = 'wechat_msg'
        AND column_name = 'status'
        AND data_type <> 'integer'
    ) THEN
      ALTER TABLE public.wechat_msg
        ALTER COLUMN status TYPE integer
        USING CASE WHEN status ~ '^-?[0-9]+$' THEN status::integer ELSE NULL END;
    END IF;
  END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_ding_task_status ON public.ding_task(status);
CREATE INDEX IF NOT EXISTS idx_ding_task_send_read ON public.ding_task(send_status, read_status);
CREATE INDEX IF NOT EXISTS idx_wechat_msg_status ON public.wechat_msg(status);
CREATE INDEX IF NOT EXISTS idx_wechat_msg_send_read ON public.wechat_msg(send_status, read_status);
CREATE INDEX IF NOT EXISTS idx_printer_register_source_type
  ON public.printer_register(source, service_type);

CREATE TABLE IF NOT EXISTS public.file_server_document (
  id bigint PRIMARY KEY,
  file_path varchar(1024) NOT NULL,
  file_name varchar(255) NOT NULL,
  file_org_type varchar(1024),
  file_size bigint NOT NULL,
  link_id bigint NOT NULL,
  file_type varchar(510),
  main_model_id bigint,
  size_dis varchar(256),
  memo varchar(510),
  property_code varchar(510),
  show_type varchar(510),
  opener varchar(510),
  open_time timestamp,
  deployment_id bigint,
  activity_name varchar(510),
  task_description varchar(510),
  file_icon varchar(510),
  is_file_view boolean DEFAULT false,
  doc_content varchar(510),
  doc_summary varchar(510),
  convert_status varchar(510),
  reason varchar(510),
  convert_path varchar(510),
  download_times bigint DEFAULT 0,
  preview_times bigint DEFAULT 0,
  valid varchar(16) DEFAULT '1',
  version bigint DEFAULT 0,
  creator varchar(32) DEFAULT 'system',
  modifier varchar(32),
  create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  modify_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_staff_id bigint DEFAULT 0,
  modify_staff_id bigint,
  delete_staff_id bigint,
  delete_time timestamp,
  tenant_id varchar(256)
);

CREATE TABLE IF NOT EXISTS public.file_server_document_down_info (
  id bigint PRIMARY KEY,
  document_id bigint NOT NULL,
  download_staff_id varchar(64),
  download_time timestamp,
  ip_addr varchar(255),
  record_type varchar(255) NOT NULL,
  valid varchar(16) DEFAULT '1',
  version bigint DEFAULT 0,
  creator varchar(32) DEFAULT 'system',
  modifier varchar(32),
  create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  modify_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_staff_id bigint DEFAULT 0,
  modify_staff_id bigint,
  delete_time timestamp
);

CREATE INDEX IF NOT EXISTS idx_file_server_document_link_id
  ON public.file_server_document(link_id);
CREATE INDEX IF NOT EXISTS idx_file_server_document_tenant_id
  ON public.file_server_document(tenant_id);
CREATE INDEX IF NOT EXISTS idx_file_server_down_info_document_id
  ON public.file_server_document_down_info(document_id);

CREATE TABLE IF NOT EXISTS public.sys_scripts_version (
  id bigint PRIMARY KEY,
  application_name varchar(128) NOT NULL,
  current_version varchar(64),
  create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS udx_sys_scripts_version_app
  ON public.sys_scripts_version(application_name);

INSERT INTO public.sys_scripts_version (id, application_name, current_version, create_time)
VALUES (100000000000001, 'notification-admin', '1.0.9', CURRENT_TIMESTAMP)
ON CONFLICT (id) DO UPDATE
SET current_version = EXCLUDED.current_version;

CREATE TABLE IF NOT EXISTS public.notice_protocol (
  id bigint PRIMARY KEY,
  protocol varchar(13) NOT NULL,
  name varchar(50) NOT NULL,
  app_name varchar(128) NOT NULL,
  vender_name varchar(256) NOT NULL,
  service_name varchar(256) NOT NULL,
  send_url varchar(256) NOT NULL,
  config_url varchar(256),
  system_config_app_code varchar(256),
  system_config_code varchar(256),
  default_template_code varchar(50) NOT NULL,
  content_type smallint NOT NULL DEFAULT 0,
  doc text,
  i18n_module varchar(100),
  i18n_key varchar(100),
  system smallint DEFAULT 0,
  valid smallint DEFAULT 1,
  creator varchar(32) NOT NULL DEFAULT 'system',
  create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_staff_id bigint NOT NULL DEFAULT 0,
  modify_staff_id bigint,
  modifier varchar(32),
  modify_time timestamp DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT udx_notice_protocol_protocol UNIQUE (protocol)
);

CREATE TABLE IF NOT EXISTS public.notice_protocol_config (
  id bigint PRIMARY KEY,
  protocol varchar(13) NOT NULL,
  config_value text NOT NULL,
  creator varchar(32) NOT NULL DEFAULT 'system',
  create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_staff_id bigint NOT NULL DEFAULT 0,
  modify_staff_id bigint,
  modifier varchar(32),
  modify_time timestamp,
  CONSTRAINT udx_notice_protocol_config_protocol UNIQUE (protocol)
);

CREATE TABLE IF NOT EXISTS public.notice_protocol_tmpl (
  id bigint PRIMARY KEY,
  code varchar(50) NOT NULL,
  name varchar(50) NOT NULL,
  i18n_key varchar(100),
  description varchar(256),
  template text NOT NULL,
  system smallint DEFAULT 0,
  notice_protocol_id bigint NOT NULL,
  creator varchar(32) NOT NULL DEFAULT 'system',
  create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_staff_id bigint NOT NULL DEFAULT 0,
  modify_staff_id bigint,
  modifier varchar(32),
  modify_time timestamp,
  CONSTRAINT udx_notice_protocol_tmpl_code UNIQUE (code),
  CONSTRAINT udx_notice_protocol_tmpl_name UNIQUE (notice_protocol_id, name)
);

CREATE TABLE IF NOT EXISTS public.notice_topic_type (
  id bigint PRIMARY KEY,
  code varchar(50) NOT NULL,
  name varchar(50) NOT NULL,
  i18n_key varchar(100),
  source varchar(50) NOT NULL DEFAULT 'system',
  modify_sign smallint DEFAULT 1,
  parent_id bigint,
  sort_value double precision NOT NULL DEFAULT 0,
  description varchar(256),
  creator varchar(32) NOT NULL DEFAULT 'system',
  create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_staff_id bigint NOT NULL DEFAULT 0,
  modify_staff_id bigint,
  modifier varchar(32),
  modify_time timestamp,
  version integer DEFAULT 0,
  valid smallint DEFAULT 0,
  lay_rec integer DEFAULT 0,
  CONSTRAINT udx_notice_topic_type_name UNIQUE (name)
);

CREATE TABLE IF NOT EXISTS public.notice_topic (
  id bigint PRIMARY KEY,
  code varchar(50) NOT NULL,
  name varchar(50) NOT NULL,
  source varchar(50) NOT NULL DEFAULT 'system',
  modify_sign smallint DEFAULT 1,
  cover_sign smallint DEFAULT 0,
  notice_topic_type_id bigint NOT NULL,
  description varchar(256),
  creator varchar(32) NOT NULL DEFAULT 'system',
  create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_staff_id bigint NOT NULL DEFAULT 0,
  modify_staff_id bigint,
  modifier varchar(32),
  modify_time timestamp,
  version integer DEFAULT 0,
  valid smallint DEFAULT 0,
  sort_value double precision NOT NULL DEFAULT 0,
  CONSTRAINT udx_notice_topic_code UNIQUE (code)
);

CREATE TABLE IF NOT EXISTS public.notice_tmpl (
  id bigint PRIMARY KEY,
  code varchar(50) NOT NULL,
  name varchar(50) NOT NULL,
  params varchar(50),
  description varchar(256),
  template text,
  source varchar(50) NOT NULL DEFAULT 'system',
  modify_sign smallint DEFAULT 1,
  cover_sign smallint DEFAULT 0,
  notice_protocol_id bigint,
  creator varchar(32) NOT NULL DEFAULT 'system',
  create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_staff_id bigint NOT NULL DEFAULT 0,
  modify_staff_id bigint,
  modifier varchar(32),
  modify_time timestamp,
  version integer DEFAULT 0,
  sort_value double precision NOT NULL DEFAULT 0,
  valid smallint DEFAULT 0,
  CONSTRAINT udx_notice_tmpl_code UNIQUE (code)
);

CREATE TABLE IF NOT EXISTS public.notice_topic_tmpl_rel (
  id bigint PRIMARY KEY,
  notice_topic_id varchar(32) NOT NULL,
  notice_tmpl_id varchar(32) NOT NULL,
  notice_protocol_id varchar(32) NOT NULL,
  creator varchar(32) NOT NULL DEFAULT 'system',
  create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_staff_id bigint NOT NULL DEFAULT 0,
  modify_staff_id bigint,
  modifier varchar(32),
  modify_time timestamp,
  CONSTRAINT udx_notice_topic_tmpl_rel UNIQUE (notice_topic_id, notice_tmpl_id, notice_protocol_id)
);

CREATE TABLE IF NOT EXISTS public.notice_topic_range (
  id bigint PRIMARY KEY,
  range_type smallint NOT NULL,
  bsmod_name varchar(50),
  bsmod_code varchar(50),
  bsmod_addr varchar(32),
  notice_topic_id bigint NOT NULL,
  creator varchar(32) NOT NULL DEFAULT 'system',
  create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_staff_id bigint NOT NULL DEFAULT 0,
  modify_staff_id bigint,
  modifier varchar(32),
  modify_time timestamp
);

CREATE TABLE IF NOT EXISTS public.notice_topic_range_ext (
  id bigint PRIMARY KEY,
  receiver_id bigint NOT NULL,
  receiver_code varchar(200) NOT NULL,
  contain_children smallint NOT NULL DEFAULT 0,
  notice_topic_range_id bigint NOT NULL,
  creator varchar(32) NOT NULL DEFAULT 'system',
  create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_staff_id bigint NOT NULL DEFAULT 0,
  modify_staff_id bigint,
  modifier varchar(32),
  modify_time timestamp
);

CREATE TABLE IF NOT EXISTS public.notice_task (
  id bigint PRIMARY KEY,
  code varchar(128) NOT NULL,
  bsmod_code varchar(200) NOT NULL,
  bsmod_name varchar(200) NOT NULL,
  task_type smallint NOT NULL,
  status smallint DEFAULT 0,
  sharding_time bigint NOT NULL,
  notice_topic_id bigint,
  creator varchar(32) NOT NULL DEFAULT 'system',
  create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_staff_id bigint NOT NULL DEFAULT 0,
  modify_staff_id bigint,
  modifier varchar(32),
  modify_time timestamp DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS public.notice_task_protocol (
  id bigint PRIMARY KEY,
  notice_protocol_id bigint NOT NULL,
  notice_task_id bigint NOT NULL,
  content text NOT NULL,
  creator varchar(32) NOT NULL DEFAULT 'system',
  create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_staff_id bigint NOT NULL DEFAULT 0,
  modify_staff_id bigint,
  modifier varchar(32),
  modify_time timestamp,
  CONSTRAINT udx_notice_task_protocol UNIQUE (notice_task_id, notice_protocol_id)
);

CREATE TABLE IF NOT EXISTS public.notice_msg (
  id bigint PRIMARY KEY,
  staff_code varchar(200) NOT NULL,
  staff_name varchar(200) NOT NULL,
  bsmod_code varchar(200),
  bsmod_name varchar(200),
  topic_name varchar(32),
  topic_id bigint,
  user_name varchar(200),
  send_status smallint NOT NULL,
  error_result varchar(200),
  param text,
  read_status smallint NOT NULL,
  retry smallint DEFAULT 0,
  sharding_time bigint NOT NULL,
  notice_task_id bigint NOT NULL,
  notice_protocol_id bigint NOT NULL,
  notice_task_protocol_id bigint NOT NULL,
  creator varchar(32) NOT NULL DEFAULT 'system',
  create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_staff_id bigint NOT NULL DEFAULT 0,
  modify_staff_id bigint,
  modifier varchar(32),
  modify_time timestamp DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_notice_msg_staff_protocol
  ON public.notice_msg(staff_code, notice_protocol_id, read_status);
CREATE INDEX IF NOT EXISTS idx_notice_msg_staff_topic
  ON public.notice_msg(staff_code, topic_id);
CREATE INDEX IF NOT EXISTS idx_notice_msg_task
  ON public.notice_msg(notice_task_id);

CREATE TABLE IF NOT EXISTS public.notice_message_unread_count (
  id bigint PRIMARY KEY,
  staff_code varchar(200) NOT NULL,
  notice_protocol_id bigint NOT NULL,
  topic_id bigint,
  unread_count bigint NOT NULL DEFAULT 0,
  creator varchar(32) NOT NULL DEFAULT 'system',
  create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_staff_id bigint NOT NULL DEFAULT 0,
  modify_staff_id bigint,
  modifier varchar(32),
  modify_time timestamp
);

CREATE UNIQUE INDEX IF NOT EXISTS udx_staff_protocol_topic
  ON public.notice_message_unread_count(staff_code, notice_protocol_id, topic_id);

INSERT INTO public.notice_protocol
  (id, protocol, name, i18n_key, app_name, vender_name, service_name, send_url, default_template_code, content_type, system, valid, creator, create_staff_id)
VALUES
  (1, 'email', 'notificationAdmin.src_common_mail', 'notificationAdmin.src_common_mail', 'email', 'supcon', 'supcon_email', 'sendUrl', '001', 1, 1, 1, 'default', 1),
  (2, 'stationLetter', 'notificationAdmin.src_common_information', 'notificationAdmin.src_common_information', 'stationLetter', 'supcon', 'supcon_stationLetter', 'sendUrl', '003', 0, 1, 1, 'default', 1)
ON CONFLICT (id) DO UPDATE
SET valid = EXCLUDED.valid,
    protocol = EXCLUDED.protocol,
    name = EXCLUDED.name,
    i18n_key = EXCLUDED.i18n_key;

INSERT INTO public.notice_protocol_tmpl
  (id, code, name, i18n_key, template, system, notice_protocol_id, creator, create_staff_id)
VALUES
  (1, '001', 'admin notice', 'notificationAdmin.protocol_basic_module_admin', '${name}$', 1, 1, 'default', 1),
  (2, '002', 'todo notice', 'notificationAdmin.protocol_basic_module_todo', '${username}$ ${name}$', 1, 1, 'default', 1),
  (3, '003', 'admin notice', 'notificationAdmin.protocol_basic_module_admin', '${name}$', 1, 2, 'default', 1),
  (4, '004', 'todo notice', 'notificationAdmin.protocol_basic_module_todo', '${username}$ ${name}$', 1, 2, 'default', 1)
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.notice_topic_type
  (id, parent_id, lay_rec, code, name, i18n_key, version, sort_value, description, valid, modify_sign, source, creator, create_staff_id, create_time)
VALUES
  (1000, 0, 0, 'defaultType', 'defaultType', 'notificationAdmin.type_default', 0, 0, 'system type', 0, 0, 'system', 'admin', 1, TIMESTAMP '2020-05-18 20:31:47.875'),
  (1001, 0, 0, 'defaultType002', 'todoType', 'notificationAdmin.type_todo', 0, 0, 'system type', 0, 0, 'system', 'admin', 1, TIMESTAMP '2020-05-18 20:31:47.875')
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.notice_tmpl
  (id, code, name, template, notice_protocol_id, creator, create_time, create_staff_id)
VALUES
  (1, 'defult_bap_pending', 'platform pending message template', '{"text":"new pending message: ${content}","subject":"pending"}', 1, 'default', TIMESTAMP '2020-10-26 20:31:47.875', 1),
  (2, 'defult_bap_reminding', 'platform reminder message template', '{"text":"new reminder message: ${content}","subject":"reminder"}', 1, 'default', TIMESTAMP '2020-10-26 20:31:47.875', 1),
  (3, 'defult_bap_over_pending', 'platform overdue pending template', '{"text":"overdue pending: ${content}","subject":"overdue"}', 1, 'default', TIMESTAMP '2020-10-26 20:31:47.875', 1),
  (4, 'stationLetter_pending', 'station letter pending template', '{"text":"${orderName} ${orderNumber} ${orderStatus}","url":"${relativeUrl}"}', 2, 'default', TIMESTAMP '2021-07-14 20:31:47.875', 1),
  (5, 'email_pending', 'email pending template', '{"text":"${receiver} ${orderName} ${orderNumber} ${orderStatus} ${orderUrl}","subject":"pending"}', 1, 'default', TIMESTAMP '2021-07-14 20:31:47.875', 1)
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.notice_topic
  (id, code, name, modify_sign, notice_topic_type_id, creator, create_time, create_staff_id)
VALUES
  (1, 'bap_pending', 'pending message', 1, 1001, 'default', TIMESTAMP '2020-10-26 20:31:47.875', 1),
  (2, 'bap_reminding', 'reminder message', 1, 1001, 'default', TIMESTAMP '2020-10-26 20:31:47.875', 1),
  (3, 'bap_over_pending', 'overdue pending message', 1, 1001, 'default', TIMESTAMP '2020-10-26 20:31:47.875', 1),
  (4, 'pending_topic', 'pending topic', 1, 1001, 'default', TIMESTAMP '2021-07-14 20:31:47.875', 1)
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.notice_topic_tmpl_rel
  (id, notice_topic_id, notice_tmpl_id, notice_protocol_id, creator, create_time, create_staff_id)
VALUES
  (1, '1', '1', '1', 'default', TIMESTAMP '2020-10-26 20:31:47.875', 1),
  (2, '2', '2', '1', 'default', TIMESTAMP '2020-10-26 20:31:47.875', 1),
  (3, '3', '3', '1', 'default', TIMESTAMP '2020-10-26 20:31:47.875', 1),
  (4, '4', '4', '2', 'default', TIMESTAMP '2021-07-14 20:31:47.875', 1),
  (5, '4', '5', '1', 'default', TIMESTAMP '2021-07-14 20:31:47.875', 1)
ON CONFLICT (id) DO NOTHING;

DO $$
DECLARE
  month_value text;
  month_start date;
  protocol_value text;
  task_table text;
  msg_table text;
  task_protocol_table text;
BEGIN
  FOR month_offset IN -1..1 LOOP
    month_start := (date_trunc('month', CURRENT_DATE) + (month_offset || ' months')::interval)::date;
    month_value := EXTRACT(YEAR FROM month_start)::integer::text ||
                   lpad(EXTRACT(MONTH FROM month_start)::integer::text, 2, '0');
    task_table := 'notice_task_' || month_value;
    task_protocol_table := 'notice_task_protocol_' || month_value;

    EXECUTE format(
      'CREATE TABLE IF NOT EXISTS public.%I (LIKE public.notice_task INCLUDING DEFAULTS INCLUDING CONSTRAINTS)',
      task_table
    );
    EXECUTE format(
      'CREATE TABLE IF NOT EXISTS public.%I (LIKE public.notice_task_protocol INCLUDING DEFAULTS INCLUDING CONSTRAINTS)',
      task_protocol_table
    );
    EXECUTE format(
      'CREATE UNIQUE INDEX IF NOT EXISTS %I ON public.%I (notice_task_id, notice_protocol_id)',
      'udx_ntp_' || month_value,
      task_protocol_table
    );

    FOREACH protocol_value IN ARRAY ARRAY['email', 'stationletter'] LOOP
      msg_table := 'notice_msg_' || protocol_value || month_value;
      EXECUTE format(
        'CREATE TABLE IF NOT EXISTS public.%I (LIKE public.notice_msg INCLUDING DEFAULTS INCLUDING CONSTRAINTS)',
        msg_table
      );
      EXECUTE format(
        'CREATE INDEX IF NOT EXISTS %I ON public.%I (staff_code, notice_protocol_id, read_status)',
        'idx_' || msg_table || '_staff_protocol',
        msg_table
      );
      EXECUTE format(
        'CREATE INDEX IF NOT EXISTS %I ON public.%I (staff_code, topic_id)',
        'idx_' || msg_table || '_staff_topic',
        msg_table
      );
      EXECUTE format(
        'CREATE INDEX IF NOT EXISTS %I ON public.%I (notice_task_id)',
        'idx_' || msg_table || '_task',
        msg_table
      );
    END LOOP;
  END LOOP;
END $$;
