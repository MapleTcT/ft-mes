-- Design-time configuration tables required by module package import.
-- Generated from configuration-services-dao META-INF/mariadb/configuration-init.sql.
-- Existing hand-tuned PostgreSQL tables are left untouched via IF NOT EXISTS.

-- Keep generated index creation compatible with recovered databases where an
-- already existing table may not have every vendor-era column.
CREATE OR REPLACE FUNCTION public.adp_create_index_if_columns_exist(
  index_name text,
  table_name text,
  column_names text[]
) RETURNS void
LANGUAGE plpgsql
AS $$
DECLARE
  missing_column text;
  columns_sql text;
BEGIN
  IF to_regclass(format('public.%I', table_name)) IS NULL THEN
    RETURN;
  END IF;

  SELECT column_name
  INTO missing_column
  FROM unnest(column_names) AS candidate(column_name)
  WHERE NOT EXISTS (
    SELECT 1
    FROM information_schema.columns AS c
    WHERE c.table_schema = 'public'
      AND c.table_name = adp_create_index_if_columns_exist.table_name
      AND c.column_name = candidate.column_name
  )
  LIMIT 1;

  IF missing_column IS NOT NULL THEN
    RAISE NOTICE 'skip index %, missing %.%', index_name, table_name, missing_column;
    RETURN;
  END IF;

  SELECT string_agg(format('%I', column_name), ', ')
  INTO columns_sql
  FROM unnest(column_names) AS candidate(column_name);

  EXECUTE format('CREATE INDEX IF NOT EXISTS %I ON public.%I (%s)', index_name, table_name, columns_sql);
END;
$$;

CREATE TABLE IF NOT EXISTS public.ec_adv_query_condition (
  id bigint NOT NULL,
  version integer DEFAULT 0,
  delete_time timestamp,
  modify_time timestamp,
  create_time timestamp,
  delete_staff_id bigint,
  modify_staff_id bigint,
  create_staff_id bigint,
  valid integer DEFAULT 1,
  layout_name varchar(510),
  admin_flag integer,
  view_code varchar(510),
  remark text,
  cond_name varchar(400),
  model_alias varchar(400),
  owner_id bigint,
  CONSTRAINT ec_adv_query_condition_pkey PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS public.ec_adv_query_condition_item (
  id bigint NOT NULL,
  version integer DEFAULT 0,
  delete_time timestamp,
  modify_time timestamp,
  create_time timestamp,
  delete_staff_id bigint,
  modify_staff_id bigint,
  create_staff_id bigint,
  valid integer DEFAULT 1,
  name varchar(510),
  valuebak varchar(2),
  adv_type integer,
  model_alias varchar(400),
  condition_id bigint,
  parent_id bigint,
  model_code varchar(510),
  join_info varchar(200),
  logic varchar(20),
  value text,
  operator varchar(60),
  param_str varchar(80),
  db_column_type varchar(510),
  column_name varchar(400),
  type varchar(2),
  CONSTRAINT ec_adv_query_condition_item_pkey PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS public.ec_adv_query_json (
  code varchar(1024) NOT NULL,
  ec_env varchar(510),
  version integer DEFAULT 0,
  targetmodel_code varchar(510),
  proj_flag integer,
  name varchar(510),
  layout_name varchar(510),
  query_config text,
  view_code varchar(510),
  create_time timestamp,
  CONSTRAINT ec_adv_query_json_pkey PRIMARY KEY (code)
);

CREATE TABLE IF NOT EXISTS public.ec_backup_data_grid (
  code varchar(1024) NOT NULL,
  ec_env varchar(510),
  version integer DEFAULT 0,
  delete_time timestamp,
  modify_time timestamp,
  create_time timestamp,
  delete_staff_id bigint,
  modify_staff_id bigint,
  create_staff_id bigint,
  valid integer DEFAULT 1,
  ex integer,
  orgproperty_code varchar(510),
  targetmodel_code varchar(510),
  view_code varchar(510),
  name varchar(510),
  dg_field_config text,
  config text,
  backupview_code varchar(510),
  CONSTRAINT ec_backup_data_grid_pkey PRIMARY KEY (code)
);

CREATE TABLE IF NOT EXISTS public.ec_backup_view (
  code varchar(1024) NOT NULL,
  ec_env varchar(510),
  version integer DEFAULT 0,
  delete_time timestamp,
  modify_time timestamp,
  create_time timestamp,
  delete_staff_id bigint,
  modify_staff_id bigint,
  create_staff_id bigint,
  valid integer DEFAULT 1,
  field_config text,
  config text,
  view_code varchar(510),
  CONSTRAINT ec_backup_view_pkey PRIMARY KEY (code)
);

CREATE TABLE IF NOT EXISTS public.ec_button (
  code varchar(1024) NOT NULL,
  ec_env varchar(510),
  version integer DEFAULT 0,
  delete_time timestamp,
  modify_time timestamp,
  create_time timestamp,
  delete_staff_id bigint,
  modify_staff_id bigint,
  create_staff_id bigint,
  valid integer DEFAULT 1,
  entity_code varchar(510),
  module_code varchar(510),
  button_operation_code varchar(510),
  release_felid varchar(510),
  is_signature_config integer,
  proj_flag integer,
  power_type varchar(510),
  role_id varchar(510),
  position_id varchar(510),
  signer_id varchar(510),
  signature_type varchar(510),
  signature_enabled integer,
  is_published integer,
  button_align varchar(510),
  permission_code varchar(510),
  config text,
  script_code varchar(510),
  region_type varchar(510),
  datagrid_code varchar(510),
  view_code varchar(510),
  cell_code varchar(510),
  display_name varchar(510),
  operate_url varchar(510),
  is_hide integer,
  is_custom_func integer,
  is_callback integer,
  is_permission integer,
  is_use_more integer,
  button_style varchar(510),
  confirm_content varchar(510),
  is_confirm integer,
  viewselect_code varchar(510),
  operate_type varchar(510),
  name varchar(510),
  signature_describle varchar(510),
  CONSTRAINT ec_button_pkey PRIMARY KEY (code)
);

CREATE TABLE IF NOT EXISTS public.ec_buttoninfo (
  code varchar(1024) NOT NULL,
  ec_env varchar(510),
  version integer DEFAULT 0,
  view_code varchar(510),
  entity_code varchar(510),
  action varchar(510),
  name_space varchar(510),
  icon_cls varchar(510),
  url varchar(510),
  name varchar(510),
  CONSTRAINT ec_buttoninfo_pkey PRIMARY KEY (code)
);

CREATE TABLE IF NOT EXISTS public.ec_customer_condition (
  code varchar(1024) NOT NULL,
  ec_env varchar(510),
  version integer DEFAULT 0,
  delete_time timestamp,
  modify_time timestamp,
  create_time timestamp,
  delete_staff_id bigint,
  modify_staff_id bigint,
  create_staff_id bigint,
  valid integer DEFAULT 1,
  entity_code varchar(510),
  module_code varchar(510),
  proj_flag integer,
  condition_sql text,
  json_condition text,
  dataclassific_code varchar(510),
  datagrid_code varchar(510),
  view_code varchar(510),
  CONSTRAINT ec_customer_condition_pkey PRIMARY KEY (code)
);

CREATE TABLE IF NOT EXISTS public.ec_custom_code (
  code varchar(1024) NOT NULL,
  ec_env varchar(510),
  version integer DEFAULT 0,
  custom_code text,
  sub_type varchar(510),
  type varchar(510),
  model_code varchar(510),
  entity_code varchar(510),
  module_code varchar(510),
  CONSTRAINT ec_custom_code_pkey PRIMARY KEY (code)
);

CREATE TABLE IF NOT EXISTS public.ec_data_classific (
  code varchar(1024) NOT NULL,
  ec_env varchar(510),
  version integer DEFAULT 0,
  delete_time timestamp,
  modify_time timestamp,
  create_time timestamp,
  delete_staff_id bigint,
  modify_staff_id bigint,
  create_staff_id bigint,
  valid integer DEFAULT 1,
  entity_code varchar(510),
  module_code varchar(510),
  proj_flag integer,
  is_default integer,
  sort bigint,
  data_group_code varchar(510),
  dc_condition varchar(510),
  display_name varchar(510),
  name varchar(510),
  CONSTRAINT ec_data_classific_pkey PRIMARY KEY (code)
);

CREATE TABLE IF NOT EXISTS public.ec_data_grid (
  code varchar(1024) NOT NULL,
  ec_env varchar(510),
  version integer DEFAULT 0,
  delete_time timestamp,
  modify_time timestamp,
  create_time timestamp,
  delete_staff_id bigint,
  modify_staff_id bigint,
  create_staff_id bigint,
  valid integer DEFAULT 1,
  entity_code varchar(510),
  module_code varchar(510),
  data_grid_json text,
  proj_flag integer,
  operate_name varchar(510),
  permission_code varchar(510),
  is_permission integer,
  data_grid_type integer,
  full_config text,
  data_grid_name varchar(510),
  ex integer,
  orgproperty_code varchar(510),
  targetmodel_code varchar(510),
  config text,
  view_code varchar(510),
  name varchar(510),
  CONSTRAINT ec_data_grid_pkey PRIMARY KEY (code)
);

CREATE TABLE IF NOT EXISTS public.ec_data_group (
  code varchar(1024) NOT NULL,
  ec_env varchar(510),
  version integer DEFAULT 0,
  delete_time timestamp,
  modify_time timestamp,
  create_time timestamp,
  delete_staff_id bigint,
  modify_staff_id bigint,
  create_staff_id bigint,
  valid integer DEFAULT 1,
  entity_code varchar(510),
  module_code varchar(510),
  targetmodel_code varchar(510),
  proj_flag integer,
  sort bigint,
  layout_name varchar(510),
  is_multiple integer,
  display_name varchar(510),
  name varchar(510),
  view_code varchar(510),
  CONSTRAINT ec_data_group_pkey PRIMARY KEY (code)
);

CREATE TABLE IF NOT EXISTS public.ec_default_adv_cond (
  code varchar(1024) NOT NULL,
  ec_env varchar(510),
  version integer DEFAULT 0,
  view_code text,
  content text,
  CONSTRAINT ec_default_adv_cond_pkey PRIMARY KEY (code)
);

CREATE TABLE IF NOT EXISTS public.ec_echarts (
  code varchar(1024) NOT NULL,
  ec_env varchar(510),
  version integer DEFAULT 0,
  delete_time timestamp,
  modify_time timestamp,
  create_time timestamp,
  delete_staff_id bigint,
  modify_staff_id bigint,
  create_staff_id bigint,
  valid integer DEFAULT 1,
  proj_flag integer,
  is_show_magic_type integer,
  legend_position varchar(510),
  is_show_legend integer,
  is_first_load integer,
  title varchar(510),
  CONSTRAINT ec_echarts_pkey PRIMARY KEY (code)
);

CREATE TABLE IF NOT EXISTS public.ec_echarts_model (
  code varchar(1024) NOT NULL,
  ec_env varchar(510),
  version integer DEFAULT 0,
  delete_time timestamp,
  modify_time timestamp,
  create_time timestamp,
  delete_staff_id bigint,
  modify_staff_id bigint,
  create_staff_id bigint,
  valid integer DEFAULT 1,
  proj_flag integer,
  model_sql text,
  custom_conditions_confjson text,
  custom_conditions text,
  is_custom_conditions integer,
  series_column varchar(510),
  value_column varchar(510),
  classification_column varchar(510),
  yaxis_str varchar(510),
  xaxis_str varchar(510),
  type varchar(510),
  model_code varchar(510),
  echarts_code varchar(510),
  CONSTRAINT ec_echarts_model_pkey PRIMARY KEY (code)
);

CREATE TABLE IF NOT EXISTS public.ec_entity (
  code varchar(1024) NOT NULL,
  ec_env varchar(510),
  version integer DEFAULT 0,
  delete_time timestamp,
  modify_time timestamp,
  create_time timestamp,
  delete_staff_id bigint,
  modify_staff_id bigint,
  create_staff_id bigint,
  valid integer DEFAULT 1,
  enable_fields_permission_conf integer,
  enable_ws integer,
  enable_rest integer,
  proj_flag integer,
  type varchar(510),
  enable_audit integer,
  enable_acl_restrict integer,
  mobile integer,
  cross_company_flag integer,
  is_control integer,
  is_inherented_base integer,
  inherent_common_flag integer,
  is_base integer,
  module_code varchar(510),
  pay_close_attention integer,
  group_enabled integer,
  prefix varchar(510),
  description text,
  workflow_enabled integer,
  entity_name varchar(510),
  value_zh_cn varchar(510),
  name varchar(510),
  id varchar(100),
  CONSTRAINT ec_entity_pkey PRIMARY KEY (code)
);

CREATE TABLE IF NOT EXISTS public.ec_entity_modify_info (
  id bigint NOT NULL,
  version integer DEFAULT 0,
  last_modify_time timestamp,
  module_code varchar(510),
  entity_code varchar(510),
  CONSTRAINT ec_entity_modify_info_pkey PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS public.ec_event (
  code varchar(1024) NOT NULL,
  ec_env varchar(510),
  version integer DEFAULT 0,
  delete_time timestamp,
  modify_time timestamp,
  create_time timestamp,
  delete_staff_id bigint,
  modify_staff_id bigint,
  create_staff_id bigint,
  valid integer DEFAULT 1,
  entity_code varchar(510),
  module_code varchar(510),
  event_function_es5 text,
  proj_flag integer,
  section_code varchar(510),
  tab_code varchar(510),
  layout_code varchar(510),
  button_code varchar(510),
  field_code varchar(510),
  event_function text,
  name varchar(510),
  CONSTRAINT ec_event_pkey PRIMARY KEY (code)
);

CREATE TABLE IF NOT EXISTS public.ec_extra_query_json (
  code varchar(1024) NOT NULL,
  ec_env varchar(510),
  version integer DEFAULT 0,
  proj_flag integer,
  query_config text,
  view_code varchar(510),
  CONSTRAINT ec_extra_query_json_pkey PRIMARY KEY (code)
);

CREATE TABLE IF NOT EXISTS public.ec_extra_view (
  code varchar(1024) NOT NULL,
  ec_env varchar(510),
  version integer DEFAULT 0,
  view_json text,
  proj_flag integer,
  full_config text,
  config text,
  view_code varchar(510),
  CONSTRAINT ec_extra_view_pkey PRIMARY KEY (code)
);

CREATE TABLE IF NOT EXISTS public.ec_fast_query_json (
  code varchar(1024) NOT NULL,
  ec_env varchar(510),
  version integer DEFAULT 0,
  targetmodel_code varchar(510),
  proj_flag integer,
  layout_name varchar(510),
  query_config text,
  view_code varchar(510),
  CONSTRAINT ec_fast_query_json_pkey PRIMARY KEY (code)
);

CREATE TABLE IF NOT EXISTS public.ec_field (
  code varchar(1024) NOT NULL,
  ec_env varchar(510),
  version integer DEFAULT 0,
  delete_time timestamp,
  modify_time timestamp,
  create_time timestamp,
  delete_staff_id bigint,
  modify_staff_id bigint,
  create_staff_id bigint,
  valid integer DEFAULT 1,
  entity_code varchar(510),
  module_code varchar(510),
  layer_type varchar(510),
  proj_flag integer,
  column_type varchar(510),
  full_property_code varchar(510),
  region_type varchar(510),
  advqueryjson_code varchar(510),
  fastqueryjson_code varchar(510),
  datagrid_code varchar(510),
  config text,
  cell_code varchar(510),
  none varchar(510),
  is_hidden integer,
  lay_rec text,
  show_format varchar(510),
  show_type varchar(510),
  view_code varchar(510),
  property_code varchar(510),
  display_name varchar(510),
  name varchar(510),
  field_key varchar(510),
  CONSTRAINT ec_field_pkey PRIMARY KEY (code)
);

CREATE TABLE IF NOT EXISTS public.ec_import_template (
  code varchar(1024) NOT NULL,
  ec_env varchar(510),
  version integer DEFAULT 0,
  proj_flag integer,
  value text,
  CONSTRAINT ec_import_template_pkey PRIMARY KEY (code)
);

CREATE TABLE IF NOT EXISTS public.ec_layout (
  code varchar(1024) NOT NULL,
  ec_env varchar(510),
  version integer DEFAULT 0,
  delete_time timestamp,
  modify_time timestamp,
  create_time timestamp,
  delete_staff_id bigint,
  modify_staff_id bigint,
  create_staff_id bigint,
  valid integer DEFAULT 1,
  content text,
  description varchar(510),
  image varchar(510),
  name varchar(510),
  CONSTRAINT ec_layout_pkey PRIMARY KEY (code)
);

CREATE TABLE IF NOT EXISTS public.ec_model (
  code varchar(1024) NOT NULL,
  ec_env varchar(510),
  version integer DEFAULT 0,
  delete_time timestamp,
  modify_time timestamp,
  create_time timestamp,
  delete_staff_id bigint,
  modify_staff_id bigint,
  create_staff_id bigint,
  valid integer DEFAULT 1,
  is_error_sql integer,
  view_sql text,
  model_sql text,
  proj_flag integer,
  specialper_template_sql text,
  is_config_special integer,
  special_auth_isandrel integer,
  is_mne_code integer,
  is_control integer,
  is_cache integer,
  entity_class varchar(510),
  is_extra_col integer,
  enable_data_audit integer,
  enable_operation_audit integer,
  enable_sync integer,
  table_name varchar(510),
  inherent_common_flag integer,
  ec_version varchar(510),
  jpa_name varchar(510),
  module_code varchar(510),
  extends_model_code varchar(510),
  is_extends integer,
  type integer,
  data_type integer,
  is_main integer,
  entity_code varchar(510),
  description text,
  model_name varchar(510),
  value_zh_cn varchar(510),
  name varchar(510),
  CONSTRAINT ec_model_pkey PRIMARY KEY (code)
);

CREATE TABLE IF NOT EXISTS public.ec_module (
  code varchar(1024) NOT NULL,
  ec_env varchar(510),
  version integer DEFAULT 0,
  delete_time timestamp,
  modify_time timestamp,
  create_time timestamp,
  delete_staff_id bigint,
  modify_staff_id bigint,
  create_staff_id bigint,
  valid integer DEFAULT 1,
  is_proto integer,
  main_module integer,
  acronym varchar(510),
  copy_module_code varchar(510),
  type varchar(510),
  publish_time timestamp,
  category varchar(510),
  is_hide integer,
  is_read_only integer,
  proj_flag integer,
  is_new_generate integer,
  is_inherented_base integer,
  deploy_order varchar(510),
  description text,
  initial_version varchar(510),
  project_version varchar(510),
  artifact varchar(510),
  value_zh_cn varchar(510),
  name varchar(510),
  CONSTRAINT ec_module_pkey PRIMARY KEY (code)
);

CREATE TABLE IF NOT EXISTS public.ec_module_generate_info (
  id bigint NOT NULL,
  version integer DEFAULT 0,
  last_modify_time timestamp,
  module_code varchar(510),
  CONSTRAINT ec_module_generate_info_pkey PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS public.ec_module_reference (
  code varchar(1024) NOT NULL,
  ec_env varchar(510),
  version integer DEFAULT 0,
  delete_time timestamp,
  modify_time timestamp,
  create_time timestamp,
  delete_staff_id bigint,
  modify_staff_id bigint,
  create_staff_id bigint,
  valid integer DEFAULT 1,
  proj_flag integer,
  module_code varchar(510),
  target_module_code varchar(510),
  CONSTRAINT ec_module_reference_pkey PRIMARY KEY (code)
);

CREATE TABLE IF NOT EXISTS public.ec_module_relation (
  code varchar(1024) NOT NULL,
  ec_env varchar(510),
  version integer DEFAULT 0,
  delete_time timestamp,
  modify_time timestamp,
  create_time timestamp,
  delete_staff_id bigint,
  modify_staff_id bigint,
  create_staff_id bigint,
  valid integer DEFAULT 1,
  proj_flag integer,
  module_code varchar(510),
  target_module_code varchar(510),
  CONSTRAINT ec_module_relation_pkey PRIMARY KEY (code)
);

CREATE TABLE IF NOT EXISTS public.ec_msmodule (
  code varchar(1024) NOT NULL,
  ec_env varchar(510),
  version integer DEFAULT 0,
  delete_time timestamp,
  modify_time timestamp,
  create_time timestamp,
  delete_staff_id bigint,
  modify_staff_id bigint,
  create_staff_id bigint,
  valid integer DEFAULT 1,
  status integer,
  colony integer,
  ramnum varchar(510),
  cpunum integer,
  category varchar(510),
  is_hide integer,
  is_read_only integer,
  proj_flag integer,
  is_new_generate integer,
  is_inherented_base integer,
  deploy_order varchar(510),
  description text,
  value_zh_cn varchar(510),
  artifact varchar(510),
  showname varchar(510),
  name varchar(510),
  CONSTRAINT ec_msmodule_pkey PRIMARY KEY (code)
);

CREATE TABLE IF NOT EXISTS public.ec_msmodule_ipadress (
  code varchar(1024) NOT NULL,
  ec_env varchar(510),
  version integer DEFAULT 0,
  delete_time timestamp,
  modify_time timestamp,
  create_time timestamp,
  delete_staff_id bigint,
  modify_staff_id bigint,
  create_staff_id bigint,
  valid integer DEFAULT 1,
  status integer,
  publish_staff_id varchar(510),
  publish_time timestamp,
  description text,
  msmodule_code varchar(510),
  ipadress varchar(510),
  CONSTRAINT ec_msmodule_ipadress_pkey PRIMARY KEY (code)
);

CREATE TABLE IF NOT EXISTS public.ec_msmodule_relation (
  code varchar(1024) NOT NULL,
  ec_env varchar(510),
  version integer DEFAULT 0,
  delete_time timestamp,
  modify_time timestamp,
  create_time timestamp,
  delete_staff_id bigint,
  modify_staff_id bigint,
  create_staff_id bigint,
  valid integer DEFAULT 1,
  msmodule_code varchar(510),
  CONSTRAINT ec_msmodule_relation_pkey PRIMARY KEY (code)
);

CREATE TABLE IF NOT EXISTS public.ec_my_portlet (
  id bigint NOT NULL,
  version integer DEFAULT 0,
  config text,
  user_id bigint,
  CONSTRAINT ec_my_portlet_pkey PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS public.ec_other_restrict (
  code varchar(1024) NOT NULL,
  ec_env varchar(510),
  version integer DEFAULT 0,
  delete_time timestamp,
  modify_time timestamp,
  create_time timestamp,
  delete_staff_id bigint,
  modify_staff_id bigint,
  create_staff_id bigint,
  valid integer DEFAULT 1,
  entity_code varchar(510),
  module_code varchar(510),
  memo varchar(510),
  hand_writing_flag integer,
  title varchar(510),
  condition_sql text,
  json_condition text,
  view_code varchar(510),
  CONSTRAINT ec_other_restrict_pkey PRIMARY KEY (code)
);

CREATE TABLE IF NOT EXISTS public.ec_portlet (
  code varchar(1024) NOT NULL,
  memo text,
  height integer,
  resize_func text,
  onload_func text,
  iframe_flag integer,
  menu_info_id bigint,
  menu_operate_id bigint,
  menu_code varchar(510),
  operate_code varchar(510),
  cid bigint,
  module_code varchar(510),
  is_hidden integer,
  power_flag integer,
  scope_num integer,
  version integer DEFAULT 0,
  is_default integer,
  title_color varchar(510),
  title_key varchar(510),
  title varchar(510),
  size_num integer,
  more_target varchar(510),
  more_url varchar(510),
  url varchar(510),
  CONSTRAINT ec_portlet_pkey PRIMARY KEY (code)
);

CREATE TABLE IF NOT EXISTS public.ec_print_template (
  code varchar(1024) NOT NULL,
  ec_env varchar(510),
  version integer DEFAULT 0,
  delete_time timestamp,
  modify_time timestamp,
  create_time timestamp,
  delete_staff_id bigint,
  modify_staff_id bigint,
  create_staff_id bigint,
  extra_param_script text,
  template_businesscode varchar(100),
  extra_pic_param_count integer,
  extra_param_count integer,
  extra_param integer,
  template_enabled integer,
  valid integer DEFAULT 1,
  proj_flag integer,
  is_publish integer,
  process_key varchar(510),
  process_version integer,
  description varchar(510),
  template text,
  template_remark text,
  template_script text,
  view_code text,
  model_code text,
  template_name text,
  template_code varchar(100),
  entity_code varchar(510),
  CONSTRAINT ec_print_template_pkey PRIMARY KEY (code)
);

CREATE TABLE IF NOT EXISTS public.ec_property (
  code varchar(1024) NOT NULL,
  ec_env varchar(510),
  version integer DEFAULT 0,
  delete_time timestamp,
  modify_time timestamp,
  create_time timestamp,
  delete_staff_id bigint,
  modify_staff_id bigint,
  create_staff_id bigint,
  valid integer DEFAULT 1,
  entity_code varchar(510),
  module_code varchar(510),
  only_leaf integer,
  is_group_object integer,
  proj_custom_in_use integer,
  proj_flag integer,
  sort integer,
  is_hidden integer,
  is_engine integer,
  fetch_mode varchar(510),
  is_tree_system_code integer,
  show_width integer,
  associated_property_code varchar(510),
  associated_type integer,
  is_custom integer,
  is_mne_whole_like_query integer,
  column_name varchar(510),
  org_column_name varchar(510),
  senior_system_code integer,
  no_analyzer integer,
  is_main_associated integer,
  is_used_for_search integer,
  stretch integer,
  is_pic_support_multi_select integer,
  max_pic_num integer,
  pic_height varchar(510),
  pic_width varchar(510),
  is_bussiness_key integer,
  is_control integer,
  is_used_mne_code integer,
  default_value varchar(510),
  is_sensitive integer,
  is_main_display integer,
  is_used_for_list integer,
  model_code varchar(510),
  description text,
  attributes text,
  fillcontent text,
  is_pk integer,
  is_support_sup_and_sub integer,
  is_inherent integer,
  is_ignore_audit integer,
  is_unique integer,
  multable integer,
  decimal_num integer,
  max_length integer,
  nullable integer,
  is_index integer,
  field_type varchar(510),
  format varchar(510),
  type varchar(510),
  display_name varchar(510),
  name varchar(510),
  counter_rule_id bigint,
  CONSTRAINT ec_property_pkey PRIMARY KEY (code)
);

CREATE TABLE IF NOT EXISTS public.ec_remind_record (
  id bigint NOT NULL,
  version integer DEFAULT 0,
  remind_content text,
  process_version integer,
  process_key varchar(510),
  task_description varchar(510),
  activity_name varchar(510),
  remind_time timestamp,
  table_info_id bigint,
  entity_code varchar(510),
  send_user_id bigint,
  remind_staff_name text,
  table_no varchar(510),
  CONSTRAINT ec_remind_record_pkey PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS public.ec_scheduler_job (
  code varchar(1024) NOT NULL,
  ec_env varchar(510),
  version integer DEFAULT 0,
  delete_time timestamp,
  modify_time timestamp,
  create_time timestamp,
  delete_staff_id bigint,
  modify_staff_id bigint,
  create_staff_id bigint,
  valid integer DEFAULT 1,
  short_code varchar(510),
  inherit_flag integer,
  proj_flag integer,
  error_msg varchar(510),
  next_run_time timestamp,
  has_run_times integer,
  status varchar(510),
  procedure_name varchar(510),
  datasouce_code varchar(510),
  cron varchar(510),
  interval_unit varchar(510),
  interval_num integer,
  repeat_count integer,
  trigger_type varchar(510),
  end_time timestamp,
  start_time timestamp,
  is_local integer,
  job_content varchar(510),
  job_type integer,
  description varchar(510),
  is_schedule_imm integer,
  name varchar(510),
  module_code varchar(510),
  CONSTRAINT ec_scheduler_job_pkey PRIMARY KEY (code)
);

CREATE TABLE IF NOT EXISTS public.ec_special_permission (
  code varchar(1024) NOT NULL,
  ec_env varchar(510),
  version integer DEFAULT 0,
  delete_time timestamp,
  modify_time timestamp,
  create_time timestamp,
  delete_staff_id bigint,
  modify_staff_id bigint,
  create_staff_id bigint,
  valid integer DEFAULT 1,
  entity_code varchar(510),
  module_code varchar(510),
  order_no integer,
  property_code varchar(510),
  ref_view_code varchar(510),
  is_tree integer,
  target_model_code varchar(510),
  type varchar(510),
  relation varchar(510),
  grade integer,
  model_code varchar(510),
  CONSTRAINT ec_special_permission_pkey PRIMARY KEY (code)
);

CREATE TABLE IF NOT EXISTS public.ec_special_permission_rshow (
  id bigint NOT NULL,
  version integer DEFAULT 0,
  delete_time timestamp,
  modify_time timestamp,
  create_time timestamp,
  delete_staff_id bigint,
  modify_staff_id bigint,
  create_staff_id bigint,
  valid integer DEFAULT 1,
  is_assigned integer,
  lay_rec varchar(510),
  is_include_sub varchar(510),
  operate_id bigint,
  value_code varchar(510),
  value_title varchar(510),
  value_id varchar(510),
  special_permission_code varchar(510),
  role_id bigint,
  CONSTRAINT ec_special_permission_rshow_pkey PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS public.ec_special_permission_ushow (
  id bigint NOT NULL,
  version integer DEFAULT 0,
  delete_time timestamp,
  modify_time timestamp,
  create_time timestamp,
  delete_staff_id bigint,
  modify_staff_id bigint,
  create_staff_id bigint,
  valid integer DEFAULT 1,
  is_assigned integer,
  lay_rec varchar(510),
  is_include_sub varchar(510),
  operate_id bigint,
  value_code varchar(510),
  value_title varchar(510),
  value_id varchar(510),
  special_permission_code varchar(510),
  user_id bigint,
  CONSTRAINT ec_special_permission_ushow_pkey PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS public.ec_sql (
  code varchar(1024) NOT NULL,
  ec_env varchar(510),
  version integer DEFAULT 0,
  proj_flag integer,
  data_grid_code varchar(510),
  view_code varchar(510),
  type integer,
  query_sql text,
  CONSTRAINT ec_sql_pkey PRIMARY KEY (code)
);

CREATE TABLE IF NOT EXISTS public.ec_table_info (
  id bigint NOT NULL,
  version integer DEFAULT 0,
  delete_time timestamp,
  modify_time timestamp,
  create_time timestamp,
  delete_staff_id bigint,
  modify_staff_id bigint,
  create_staff_id bigint,
  valid integer DEFAULT 1,
  effective_state integer,
  process_version integer,
  process_key varchar(510),
  deployment_id bigint,
  summary text,
  target_table_name varchar(510),
  status integer,
  owner_depaetment_id bigint,
  owner_department_id bigint,
  owner_position_id bigint,
  owner_staff_id bigint,
  target_entity_code varchar(510),
  create_position_id bigint,
  position_lay_rec varchar(510),
  effect_time timestamp,
  effect_staff_id bigint,
  create_department_id bigint,
  table_no varchar(510),
  CONSTRAINT ec_table_info_pkey PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS public.ec_upload_info (
  id bigint NOT NULL,
  uploadn varchar(510),
  uploadm varchar(510),
  uploadl varchar(510),
  uploadk varchar(510),
  uploadj varchar(510),
  uploadi varchar(510),
  uploadh varchar(510),
  uploadg varchar(510),
  uploadf varchar(510),
  uploade varchar(510),
  uploadd varchar(510),
  uploadc varchar(510),
  uploadb varchar(510),
  uploada varchar(510),
  batchid bigint,
  isuploadschedulerjob boolean,
  isimporttemplate boolean,
  isfiltermethod boolean,
  isflow boolean,
  ismetadata boolean,
  iscustomcode boolean,
  isall boolean,
  total_time varchar(510),
  cur_version varchar(510),
  old_version varchar(510),
  upload_staff varchar(510),
  upload_state varchar(510),
  upload_date timestamp,
  upload_filename varchar(510),
  module_name varchar(510),
  module_code varchar(510),
  CONSTRAINT ec_upload_info_pkey PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS public.ec_upload_info_batch (
  id bigint NOT NULL,
  uploade varchar(510),
  uploadd varchar(510),
  uploadc varchar(510),
  uploadb varchar(510),
  uploada varchar(510),
  des varchar(510),
  module_size integer,
  total_time varchar(510),
  upload_staff varchar(510),
  upload_state varchar(510),
  upload_date timestamp,
  CONSTRAINT ec_upload_info_batch_pkey PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS public.ec_validate (
  code varchar(1024) NOT NULL,
  ec_env varchar(510),
  version integer DEFAULT 0,
  delete_time timestamp,
  modify_time timestamp,
  create_time timestamp,
  delete_staff_id bigint,
  modify_staff_id bigint,
  create_staff_id bigint,
  valid integer DEFAULT 1,
  entity_code varchar(510),
  module_code varchar(510),
  proj_flag integer,
  field_code varchar(510),
  params text,
  type varchar(510),
  CONSTRAINT ec_validate_pkey PRIMARY KEY (code)
);

CREATE TABLE IF NOT EXISTS public.ec_view (
  code varchar(1024) NOT NULL,
  ec_env varchar(510),
  version integer DEFAULT 0,
  delete_time timestamp,
  modify_time timestamp,
  create_time timestamp,
  delete_staff_id bigint,
  modify_staff_id bigint,
  create_staff_id bigint,
  valid integer DEFAULT 1,
  parent_menu_code varchar(510),
  proj_enabled integer,
  menu_name varchar(510),
  parent_menu_id bigint,
  publish_time timestamp,
  inherit_type integer,
  proj_flag integer,
  edit_view_type integer,
  has_custom_section integer,
  import_flag integer,
  enable_simple_deal_info integer,
  batch_control_print_view_code varchar(510),
  is_batch_control_print integer,
  mobile_enable_flag integer,
  mobile_version integer,
  mobile integer,
  only_for_query integer,
  ref_operate_name varchar(510),
  operate_url varchar(510),
  permission_code varchar(510),
  module_code varchar(510),
  control_seting_name varchar(510),
  control_code varchar(510),
  control_name varchar(510),
  control_print integer,
  is_permission integer,
  is_audit integer,
  is_print integer,
  is_hand_sign integer,
  is_sign integer,
  is_shadow integer,
  shadow_view_code varchar(510),
  extra_query_json varchar(510),
  data_grid_type integer,
  ass_tree_path varchar(510),
  ass_tree_lay_rec varchar(510),
  ass_tree_model_code varchar(510),
  reference_view_code varchar(510),
  is_reference integer,
  include_children integer,
  used_for_tree integer,
  deal_info_group varchar(510),
  deal_info_show integer,
  move_flag integer,
  has_attachment integer,
  is_control integer,
  close_page_after_save integer,
  layout_code varchar(510),
  show_type varchar(510),
  width integer,
  height integer,
  dialog_type varchar(510),
  custom_flag integer,
  url varchar(510),
  script_code varchar(510),
  retrial_flag integer,
  adv_query_json varchar(510),
  fast_query_json varchar(510),
  extra_view varchar(510),
  ass_view_code varchar(510),
  ass_model_code varchar(510),
  entity_code varchar(510),
  description text,
  main_ref integer,
  main_view integer,
  used_for_work_flow integer,
  open_type varchar(20),
  type varchar(510),
  title varchar(510),
  display_name varchar(510),
  name varchar(510),
  attachment_flag integer,
  CONSTRAINT ec_view_pkey PRIMARY KEY (code)
);

CREATE TABLE IF NOT EXISTS public.runtime_adv_query_json (
  code varchar(1024) NOT NULL,
  ec_env varchar(510),
  version integer DEFAULT 0,
  targetmodel_code varchar(510),
  proj_flag boolean,
  name varchar(510),
  layout_name varchar(510),
  query_config text,
  view_code varchar(510),
  CONSTRAINT runtime_adv_query_json_pkey PRIMARY KEY (code)
);

CREATE TABLE IF NOT EXISTS public.runtime_backup_data_grid (
  code varchar(1024) NOT NULL,
  ec_env varchar(510),
  version integer DEFAULT 0,
  delete_time timestamp,
  modify_time timestamp,
  create_time timestamp,
  delete_staff_id bigint,
  modify_staff_id bigint,
  create_staff_id bigint,
  valid boolean DEFAULT true,
  ex boolean,
  orgproperty_code varchar(510),
  targetmodel_code varchar(510),
  view_code varchar(510),
  name varchar(510),
  dg_field_config text,
  config text,
  backupview_code varchar(510),
  CONSTRAINT runtime_backup_data_grid_pkey PRIMARY KEY (code)
);

CREATE TABLE IF NOT EXISTS public.runtime_backup_view (
  code varchar(1024) NOT NULL,
  ec_env varchar(510),
  version integer DEFAULT 0,
  delete_time timestamp,
  modify_time timestamp,
  create_time timestamp,
  delete_staff_id bigint,
  modify_staff_id bigint,
  create_staff_id bigint,
  valid boolean DEFAULT true,
  field_config text,
  config text,
  view_code varchar(510),
  CONSTRAINT runtime_backup_view_pkey PRIMARY KEY (code)
);

CREATE TABLE IF NOT EXISTS public.runtime_button (
  code varchar(1024) NOT NULL,
  ec_env varchar(510),
  version integer DEFAULT 0,
  delete_time timestamp,
  modify_time timestamp,
  create_time timestamp,
  delete_staff_id bigint,
  modify_staff_id bigint,
  create_staff_id bigint,
  valid boolean DEFAULT true,
  entity_code varchar(510),
  module_code varchar(510),
  button_operation_code varchar(510),
  release_felid varchar(510),
  is_signature_config boolean,
  power_type varchar(510),
  role_id varchar(510),
  position_id varchar(510),
  signer_id varchar(510),
  signature_type varchar(510),
  signature_enabled boolean,
  proj_flag boolean,
  is_published boolean,
  button_align varchar(510),
  permission_code varchar(510),
  config text,
  script_code varchar(510),
  region_type varchar(510),
  datagrid_code varchar(510),
  view_code varchar(510),
  cell_code varchar(510),
  display_name varchar(510),
  operate_url varchar(510),
  is_hide boolean,
  is_custom_func boolean,
  is_callback boolean,
  is_permission boolean,
  is_use_more boolean,
  button_style varchar(510),
  confirm_content varchar(510),
  is_confirm boolean,
  viewselect_code varchar(510),
  operate_type varchar(510),
  name varchar(510),
  signature_describle varchar(510),
  CONSTRAINT runtime_button_pkey PRIMARY KEY (code)
);

CREATE TABLE IF NOT EXISTS public.runtime_customer_condition (
  code varchar(1024) NOT NULL,
  ec_env varchar(510),
  version integer DEFAULT 0,
  delete_time timestamp,
  modify_time timestamp,
  create_time timestamp,
  delete_staff_id bigint,
  modify_staff_id bigint,
  create_staff_id bigint,
  valid boolean DEFAULT true,
  entity_code varchar(510),
  module_code varchar(510),
  proj_flag boolean,
  condition_sql text,
  json_condition text,
  dataclassific_code varchar(510),
  datagrid_code varchar(510),
  view_code varchar(510),
  CONSTRAINT runtime_customer_condition_pkey PRIMARY KEY (code)
);

CREATE TABLE IF NOT EXISTS public.runtime_data_classific (
  code varchar(1024) NOT NULL,
  ec_env varchar(510),
  version integer DEFAULT 0,
  delete_time timestamp,
  modify_time timestamp,
  create_time timestamp,
  delete_staff_id bigint,
  modify_staff_id bigint,
  create_staff_id bigint,
  valid boolean DEFAULT true,
  entity_code varchar(510),
  module_code varchar(510),
  proj_flag boolean,
  is_default boolean,
  sort bigint,
  data_group_code varchar(510),
  dc_condition varchar(510),
  display_name varchar(510),
  name varchar(510),
  CONSTRAINT runtime_data_classific_pkey PRIMARY KEY (code)
);

CREATE TABLE IF NOT EXISTS public.runtime_data_grid (
  code varchar(1024) NOT NULL,
  ec_env varchar(510),
  version integer DEFAULT 0,
  delete_time timestamp,
  modify_time timestamp,
  create_time timestamp,
  delete_staff_id bigint,
  modify_staff_id bigint,
  create_staff_id bigint,
  valid boolean DEFAULT true,
  entity_code varchar(510),
  module_code varchar(510),
  data_grid_json text,
  proj_flag boolean,
  operate_name varchar(510),
  permission_code varchar(510),
  is_permission boolean,
  data_grid_type integer,
  full_config text,
  data_grid_name varchar(510),
  ex boolean,
  orgproperty_code varchar(510),
  targetmodel_code varchar(510),
  config text,
  view_code varchar(510),
  name varchar(510),
  CONSTRAINT runtime_data_grid_pkey PRIMARY KEY (code)
);

CREATE TABLE IF NOT EXISTS public.runtime_data_group (
  code varchar(1024) NOT NULL,
  ec_env varchar(510),
  version integer DEFAULT 0,
  delete_time timestamp,
  modify_time timestamp,
  create_time timestamp,
  delete_staff_id bigint,
  modify_staff_id bigint,
  create_staff_id bigint,
  valid boolean DEFAULT true,
  entity_code varchar(510),
  module_code varchar(510),
  targetmodel_code varchar(510),
  proj_flag boolean,
  sort bigint,
  layout_name varchar(510),
  is_multiple boolean,
  display_name varchar(510),
  name varchar(510),
  view_code varchar(510),
  CONSTRAINT runtime_data_group_pkey PRIMARY KEY (code)
);

CREATE TABLE IF NOT EXISTS public.runtime_echarts (
  code varchar(1024) NOT NULL,
  ec_env varchar(510),
  version integer DEFAULT 0,
  delete_time timestamp,
  modify_time timestamp,
  create_time timestamp,
  delete_staff_id bigint,
  modify_staff_id bigint,
  create_staff_id bigint,
  valid boolean DEFAULT true,
  proj_flag boolean,
  is_show_magic_type boolean,
  legend_position varchar(510),
  is_show_legend boolean,
  is_first_load boolean,
  title varchar(510),
  CONSTRAINT runtime_echarts_pkey PRIMARY KEY (code)
);

CREATE TABLE IF NOT EXISTS public.runtime_echarts_model (
  code varchar(1024) NOT NULL,
  ec_env varchar(510),
  version integer DEFAULT 0,
  delete_time timestamp,
  modify_time timestamp,
  create_time timestamp,
  delete_staff_id bigint,
  modify_staff_id bigint,
  create_staff_id bigint,
  valid boolean DEFAULT true,
  proj_flag boolean,
  model_sql text,
  custom_conditions_confjson text,
  custom_conditions text,
  is_custom_conditions boolean,
  series_column varchar(510),
  value_column varchar(510),
  classification_column varchar(510),
  yaxis_str varchar(510),
  xaxis_str varchar(510),
  type varchar(510),
  model_code varchar(510),
  echarts_code varchar(510),
  CONSTRAINT runtime_echarts_model_pkey PRIMARY KEY (code)
);

CREATE TABLE IF NOT EXISTS public.runtime_entity (
  code varchar(1024) NOT NULL,
  ec_env varchar(510),
  version integer DEFAULT 0,
  delete_time timestamp,
  modify_time timestamp,
  create_time timestamp,
  delete_staff_id bigint,
  modify_staff_id bigint,
  create_staff_id bigint,
  valid boolean DEFAULT true,
  enable_fields_permission_conf boolean,
  enable_ws boolean,
  enable_rest boolean,
  proj_flag boolean,
  type varchar(510),
  enable_audit boolean,
  enable_acl_restrict boolean,
  mobile boolean,
  cross_company_flag boolean,
  is_control boolean,
  is_inherented_base boolean,
  inherent_common_flag boolean,
  is_base boolean,
  module_code varchar(510),
  pay_close_attention boolean,
  group_enabled boolean,
  prefix varchar(510),
  description text,
  workflow_enabled boolean,
  entity_name varchar(510),
  value_zh_cn varchar(510),
  name varchar(510),
  CONSTRAINT runtime_entity_pkey PRIMARY KEY (code)
);

CREATE TABLE IF NOT EXISTS public.runtime_event (
  code varchar(1024) NOT NULL,
  ec_env varchar(510),
  version integer DEFAULT 0,
  delete_time timestamp,
  modify_time timestamp,
  create_time timestamp,
  delete_staff_id bigint,
  modify_staff_id bigint,
  create_staff_id bigint,
  valid boolean DEFAULT true,
  entity_code varchar(510),
  module_code varchar(510),
  event_function_es5 text,
  proj_flag boolean,
  section_code varchar(510),
  tab_code varchar(510),
  layout_code varchar(510),
  button_code varchar(510),
  field_code varchar(510),
  event_function text,
  name varchar(510),
  CONSTRAINT runtime_event_pkey PRIMARY KEY (code)
);

CREATE TABLE IF NOT EXISTS public.runtime_extra_query_json (
  code varchar(1024) NOT NULL,
  ec_env varchar(510),
  version integer DEFAULT 0,
  proj_flag boolean,
  query_config text,
  view_code varchar(510),
  CONSTRAINT runtime_extra_query_json_pkey PRIMARY KEY (code)
);

CREATE TABLE IF NOT EXISTS public.runtime_extra_view (
  code varchar(1024) NOT NULL,
  ec_env varchar(510),
  version integer DEFAULT 0,
  view_json text,
  proj_flag boolean,
  full_config text,
  config text,
  view_code varchar(510),
  CONSTRAINT runtime_extra_view_pkey PRIMARY KEY (code)
);

CREATE TABLE IF NOT EXISTS public.runtime_fast_query_json (
  code varchar(1024) NOT NULL,
  ec_env varchar(510),
  version integer DEFAULT 0,
  targetmodel_code varchar(510),
  proj_flag boolean,
  layout_name varchar(510),
  query_config text,
  view_code varchar(510),
  CONSTRAINT runtime_fast_query_json_pkey PRIMARY KEY (code)
);

CREATE TABLE IF NOT EXISTS public.runtime_field (
  code varchar(1024) NOT NULL,
  ec_env varchar(510),
  version integer DEFAULT 0,
  delete_time timestamp,
  modify_time timestamp,
  create_time timestamp,
  delete_staff_id bigint,
  modify_staff_id bigint,
  create_staff_id bigint,
  valid boolean DEFAULT true,
  entity_code varchar(510),
  module_code varchar(510),
  layer_type varchar(510),
  proj_flag boolean,
  column_type varchar(510),
  full_property_code varchar(510),
  region_type varchar(510),
  advqueryjson_code varchar(510),
  fastqueryjson_code varchar(510),
  datagrid_code varchar(510),
  config text,
  cell_code varchar(510),
  none varchar(510),
  is_hidden boolean,
  lay_rec text,
  show_format varchar(510),
  show_type varchar(510),
  view_code varchar(510),
  property_code varchar(510),
  display_name varchar(510),
  name varchar(510),
  field_key varchar(510),
  CONSTRAINT runtime_field_pkey PRIMARY KEY (code)
);

CREATE TABLE IF NOT EXISTS public.runtime_import_template (
  code varchar(1024) NOT NULL,
  ec_env varchar(510),
  version integer DEFAULT 0,
  proj_flag boolean,
  value text,
  CONSTRAINT runtime_import_template_pkey PRIMARY KEY (code)
);

CREATE TABLE IF NOT EXISTS public.runtime_model (
  code varchar(1024) NOT NULL,
  ec_env varchar(510),
  version integer DEFAULT 0,
  delete_time timestamp,
  modify_time timestamp,
  create_time timestamp,
  delete_staff_id bigint,
  modify_staff_id bigint,
  create_staff_id bigint,
  valid boolean DEFAULT true,
  is_error_sql integer,
  view_sql text,
  model_sql text,
  proj_flag boolean,
  specialper_template_sql text,
  is_config_special boolean,
  special_auth_isandrel boolean,
  is_mne_code boolean,
  is_control boolean,
  is_cache boolean,
  entity_class varchar(510),
  is_extra_col boolean,
  enable_data_audit boolean,
  enable_operation_audit boolean,
  enable_sync boolean,
  table_name varchar(510),
  inherent_common_flag boolean,
  ec_version varchar(510),
  jpa_name varchar(510),
  module_code varchar(510),
  extends_model_code varchar(510),
  is_extends boolean,
  type integer,
  data_type integer,
  is_main boolean,
  entity_code varchar(510),
  description text,
  model_name varchar(510),
  value_zh_cn varchar(510),
  name varchar(510),
  CONSTRAINT runtime_model_pkey PRIMARY KEY (code)
);

CREATE TABLE IF NOT EXISTS public.runtime_module (
  code varchar(1024) NOT NULL,
  ec_env varchar(510),
  version integer DEFAULT 0,
  delete_time timestamp,
  modify_time timestamp,
  create_time timestamp,
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

CREATE TABLE IF NOT EXISTS public.runtime_module_reference (
  code varchar(1024) NOT NULL,
  ec_env varchar(510),
  version integer DEFAULT 0,
  delete_time timestamp,
  modify_time timestamp,
  create_time timestamp,
  delete_staff_id bigint,
  modify_staff_id bigint,
  create_staff_id bigint,
  valid boolean DEFAULT true,
  proj_flag integer,
  module_code varchar(510),
  target_module_code varchar(510),
  CONSTRAINT runtime_module_reference_pkey PRIMARY KEY (code)
);

CREATE TABLE IF NOT EXISTS public.runtime_module_relation (
  code varchar(1024) NOT NULL,
  ec_env varchar(510),
  version integer DEFAULT 0,
  delete_time timestamp,
  modify_time timestamp,
  create_time timestamp,
  delete_staff_id bigint,
  modify_staff_id bigint,
  create_staff_id bigint,
  valid boolean DEFAULT true,
  proj_flag boolean,
  module_code varchar(510),
  target_module_code varchar(510),
  CONSTRAINT runtime_module_relation_pkey PRIMARY KEY (code)
);

CREATE TABLE IF NOT EXISTS public.runtime_print_template (
  code varchar(1024) NOT NULL,
  ec_env varchar(510),
  version integer DEFAULT 0,
  delete_time timestamp,
  modify_time timestamp,
  create_time timestamp,
  delete_staff_id bigint,
  modify_staff_id bigint,
  create_staff_id bigint,
  template_businesscode varchar(100),
  extra_param_script text,
  extra_pic_param_count integer,
  extra_param_count integer,
  extra_param boolean,
  template_enabled boolean,
  valid boolean DEFAULT true,
  proj_flag boolean,
  is_publish integer,
  process_key varchar(510),
  process_version integer,
  description varchar(510),
  template text,
  template_remark text,
  template_script text,
  view_code text,
  model_code text,
  template_name text,
  template_code varchar(100),
  entity_code varchar(510),
  CONSTRAINT runtime_print_template_pkey PRIMARY KEY (code)
);

CREATE TABLE IF NOT EXISTS public.runtime_property (
  code varchar(1024) NOT NULL,
  ec_env varchar(510),
  version integer DEFAULT 0,
  delete_time timestamp,
  modify_time timestamp,
  create_time timestamp,
  delete_staff_id bigint,
  modify_staff_id bigint,
  create_staff_id bigint,
  valid boolean DEFAULT true,
  entity_code varchar(510),
  module_code varchar(510),
  only_leaf boolean,
  is_group_object boolean,
  proj_custom_in_use boolean,
  proj_flag boolean,
  sort integer,
  is_hidden boolean,
  is_engine boolean,
  fetch_mode varchar(510),
  is_tree_system_code boolean,
  show_width integer,
  associated_property_code varchar(510),
  associated_type integer,
  is_custom boolean,
  is_mne_whole_like_query boolean,
  column_name varchar(510),
  org_column_name varchar(510),
  senior_system_code boolean,
  no_analyzer boolean,
  is_main_associated boolean,
  is_used_for_search boolean,
  stretch boolean,
  is_pic_support_multi_select boolean,
  max_pic_num integer,
  pic_height varchar(510),
  pic_width varchar(510),
  is_bussiness_key boolean,
  is_control boolean,
  is_used_mne_code boolean,
  default_value varchar(510),
  is_sensitive boolean,
  is_main_display boolean,
  is_used_for_list boolean,
  model_code varchar(510),
  description text,
  attributes text,
  fillcontent text,
  is_pk boolean,
  is_support_sup_and_sub boolean,
  is_ignore_audit boolean,
  is_inherent boolean,
  is_unique boolean,
  multable boolean,
  decimal_num integer,
  max_length integer,
  nullable boolean,
  is_index boolean,
  field_type varchar(510),
  format varchar(510),
  type varchar(510),
  display_name varchar(510),
  name varchar(510),
  counter_rule_id bigint,
  CONSTRAINT runtime_property_pkey PRIMARY KEY (code)
);

CREATE TABLE IF NOT EXISTS public.runtime_sql (
  code varchar(1024) NOT NULL,
  ec_env varchar(510),
  version integer DEFAULT 0,
  proj_flag boolean,
  data_grid_code varchar(510),
  view_code varchar(510),
  type integer,
  query_sql text,
  CONSTRAINT runtime_sql_pkey PRIMARY KEY (code)
);

CREATE TABLE IF NOT EXISTS public.runtime_validate (
  code varchar(1024) NOT NULL,
  ec_env varchar(510),
  version integer DEFAULT 0,
  delete_time timestamp,
  modify_time timestamp,
  create_time timestamp,
  delete_staff_id bigint,
  modify_staff_id bigint,
  create_staff_id bigint,
  valid boolean DEFAULT true,
  entity_code varchar(510),
  module_code varchar(510),
  proj_flag boolean,
  field_code varchar(510),
  params text,
  type varchar(510),
  CONSTRAINT runtime_validate_pkey PRIMARY KEY (code)
);

CREATE TABLE IF NOT EXISTS public.runtime_view (
  code varchar(1024) NOT NULL,
  ec_env varchar(510),
  version integer DEFAULT 0,
  delete_time timestamp,
  modify_time timestamp,
  create_time timestamp,
  delete_staff_id bigint,
  modify_staff_id bigint,
  create_staff_id bigint,
  valid boolean DEFAULT true,
  parent_menu_code varchar(510),
  proj_enabled boolean,
  menu_name varchar(510),
  parent_menu_id bigint,
  publish_time timestamp,
  inherit_type integer,
  proj_flag boolean,
  has_custom_section boolean,
  edit_view_type integer,
  import_flag boolean,
  enable_simple_deal_info boolean,
  batch_control_print_view_code varchar(510),
  is_batch_control_print boolean,
  mobile_enable_flag boolean,
  mobile_version integer,
  mobile boolean,
  only_for_query boolean,
  ref_operate_name varchar(510),
  operate_url varchar(510),
  permission_code varchar(510),
  module_code varchar(510),
  control_seting_name varchar(510),
  control_code varchar(510),
  control_name varchar(510),
  control_print boolean,
  is_permission boolean,
  is_audit boolean,
  is_print boolean,
  is_hand_sign boolean,
  is_sign boolean,
  is_shadow boolean,
  shadow_view_code varchar(510),
  extra_query_json varchar(510),
  data_grid_type integer,
  ass_tree_path varchar(510),
  ass_tree_lay_rec varchar(510),
  ass_tree_model_code varchar(510),
  reference_view_code varchar(510),
  is_reference boolean,
  include_children boolean,
  used_for_tree boolean,
  deal_info_group varchar(510),
  deal_info_show boolean,
  move_flag boolean,
  has_attachment boolean,
  is_control boolean,
  close_page_after_save boolean,
  layout_code varchar(510),
  show_type varchar(510),
  width integer,
  height integer,
  dialog_type varchar(510),
  custom_flag boolean,
  url varchar(510),
  script_code varchar(510),
  retrial_flag boolean,
  adv_query_json varchar(510),
  fast_query_json varchar(510),
  extra_view varchar(510),
  ass_view_code varchar(510),
  ass_model_code varchar(510),
  entity_code varchar(510),
  description text,
  main_ref boolean,
  main_view boolean,
  used_for_work_flow boolean,
  open_type varchar(20),
  type varchar(510),
  title varchar(510),
  display_name varchar(510),
  name varchar(510),
  attachment_flag boolean,
  CONSTRAINT runtime_view_pkey PRIMARY KEY (code)
);

CREATE TABLE IF NOT EXISTS public.wf_countersign_assign_staff (
  id bigint NOT NULL,
  version integer DEFAULT 0,
  delete_time timestamp,
  modify_time timestamp,
  create_time timestamp,
  delete_staff_id bigint,
  modify_staff_id bigint,
  create_staff_id bigint,
  valid integer DEFAULT 1,
  table_info_id bigint,
  assign_staff text,
  outcome varchar(510),
  deployment_id bigint,
  CONSTRAINT wf_countersign_assign_staff_pkey PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS public.wf_deal_info (
  id bigint NOT NULL,
  version integer DEFAULT 0,
  signature text,
  pending_create_time timestamp,
  dealinfo_type varchar(510),
  proxy_staff_ids varchar(510),
  proxy_staff varchar(510),
  assign_staff_id varchar(510),
  assign_staff text,
  process_version integer,
  process_key varchar(510),
  task_description_zh_cn varchar(510),
  task_description varchar(510),
  activity_name varchar(510),
  outcome_des_zh_cn varchar(510),
  outcome_des varchar(510),
  outcome varchar(510),
  create_time timestamp,
  table_info_id bigint,
  entity_code varchar(510),
  instance_id varchar(510),
  user_id bigint,
  comments text,
  cid bigint,
  user_agent varchar(510),
  staff bigint,
  sort integer,
  main_obj bigint,
  CONSTRAINT wf_deal_info_pkey PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS public.wf_deployment (
  id bigint NOT NULL,
  version integer DEFAULT 0,
  delete_time timestamp,
  modify_time timestamp,
  create_time timestamp,
  delete_staff_id bigint,
  modify_staff_id bigint,
  create_staff_id bigint,
  valid integer DEFAULT 1,
  signature_enable integer,
  publish_time timestamp,
  main_view_view_code varchar(510),
  recall_remain_time bigint,
  recall_able integer,
  gradually_reject integer,
  allow_invalid integer,
  mobileapprove integer,
  mobileinitiate integer,
  mobilequery integer,
  required_time double precision,
  flow_edit_flag integer,
  temp_process_xml text,
  entry_url varchar(510),
  process_xml text,
  operate_powers text,
  publish_flag integer,
  entity_code varchar(510),
  menu_code varchar(510),
  menu_info_id bigint,
  process_definition_id varchar(510),
  is_current_version integer,
  is_suspended integer,
  deployment_id varchar(510),
  process_name varchar(510),
  description varchar(510),
  process_version integer,
  process_key varchar(510),
  name_zh_cn varchar(510),
  name varchar(510),
  cid bigint,
  cross_company_flag integer,
  CONSTRAINT wf_deployment_pkey PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS public.wf_expected_consign (
  id bigint NOT NULL,
  recall_flag integer,
  type varchar(510),
  active_code varchar(510),
  flow_key varchar(510),
  flow_version varchar(510),
  memo varchar(510),
  valid integer DEFAULT 1,
  end_date timestamp,
  start_date timestamp,
  create_date timestamp,
  consignor_staff_id bigint,
  consignor_name varchar(510),
  consignor_id bigint,
  user_id bigint,
  CONSTRAINT wf_expected_consign_pkey PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS public.wf_flow_current_status (
  id bigint NOT NULL,
  version integer DEFAULT 0,
  deployment_id bigint,
  table_info_id bigint,
  create_time timestamp,
  auto_create_deal_infos text,
  activity_type varchar(510),
  source_staff text,
  dealer bigint,
  in_transition varchar(510),
  curr_activity_name varchar(510),
  last_activity_name varchar(510),
  CONSTRAINT wf_flow_current_status_pkey PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS public.wf_flow_history (
  id bigint NOT NULL,
  publish_type varchar(510),
  staff_id bigint,
  deployment_id bigint,
  publish_time timestamp,
  flow_xml text,
  process_version integer,
  process_key varchar(510),
  CONSTRAINT wf_flow_history_pkey PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS public.wf_pay_close_attention (
  id bigint NOT NULL,
  version integer DEFAULT 0,
  delete_time timestamp,
  modify_time timestamp,
  create_time timestamp,
  delete_staff_id bigint,
  modify_staff_id bigint,
  create_staff_id bigint,
  table_info_id bigint,
  staff bigint,
  CONSTRAINT wf_pay_close_attention_pkey PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS public.wf_pending (
  id bigint NOT NULL,
  version integer DEFAULT 0,
  mobile_approve integer,
  main_loop integer,
  loops integer,
  description_zh_cn varchar(510),
  description varchar(510),
  source_staff varchar(510),
  proxy_source varchar(510),
  task_type integer,
  deployment_id bigint,
  statistics_date timestamp,
  overdue_time integer,
  overdue integer,
  system_calendar_id varchar(510),
  cid bigint,
  entity_code varchar(510),
  table_no varchar(510),
  table_info_id bigint,
  model_id bigint,
  task_description_zh_cn varchar(510),
  process_description_zh_cn varchar(510),
  process_description varchar(510),
  process_id varchar(510),
  process_name varchar(510),
  process_version integer,
  process_key varchar(510),
  instance_id varchar(510),
  open_url varchar(510),
  create_time timestamp,
  status integer,
  user_id bigint,
  execution_id varchar(510),
  activity_name varchar(510),
  activity_type varchar(510),
  task_description varchar(510),
  CONSTRAINT wf_pending_pkey PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS public.wf_supervise (
  id bigint NOT NULL,
  version integer DEFAULT 0,
  delete_time timestamp,
  modify_time timestamp,
  create_time timestamp,
  delete_staff_id bigint,
  modify_staff_id bigint,
  create_staff_id bigint,
  valid integer DEFAULT 1,
  staff bigint,
  deployment_id bigint,
  CONSTRAINT wf_supervise_pkey PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS public.wf_supervision (
  id bigint NOT NULL,
  version integer DEFAULT 0,
  delete_time timestamp,
  modify_time timestamp,
  create_time timestamp,
  delete_staff_id bigint,
  modify_staff_id bigint,
  create_staff_id bigint,
  table_info_id bigint,
  staff bigint,
  CONSTRAINT wf_supervision_pkey PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS public.wf_task (
  id bigint NOT NULL,
  version integer DEFAULT 0,
  delete_time timestamp,
  modify_time timestamp,
  create_time timestamp,
  delete_staff_id bigint,
  modify_staff_id bigint,
  create_staff_id bigint,
  valid integer DEFAULT 1,
  ignore_permission integer,
  web_signet_flag integer,
  deal_set integer,
  is_allow_proxy integer,
  show_in_simple_dealinfo integer,
  mobile_approve integer,
  recall_able integer,
  process_version integer,
  process_key varchar(510),
  custom_param varchar(510),
  overdue_reminders integer,
  required_time double precision,
  cross_company integer,
  route_sequence integer,
  sub_process_key varchar(510),
  join_count integer,
  expression varchar(510),
  external_component varchar(510),
  loop_countersign integer,
  reminder_type varchar(510),
  forbidden_comment integer,
  batch_process integer,
  candidate varchar(510),
  open_mode varchar(510),
  script varchar(510),
  view_code varchar(510),
  type integer,
  deployment_id bigint,
  code varchar(1024),
  name_zh_cn varchar(510),
  name varchar(510),
  CONSTRAINT wf_task_pkey PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS public.wf_transition (
  id bigint NOT NULL,
  version integer DEFAULT 0,
  delete_time timestamp,
  modify_time timestamp,
  create_time timestamp,
  delete_staff_id bigint,
  modify_staff_id bigint,
  create_staff_id bigint,
  valid integer DEFAULT 1,
  default_staff integer,
  route_sequence integer,
  expression varchar(510),
  required_staff integer,
  select_staff varchar(510),
  deployment_id bigint,
  to_node_code varchar(510),
  from_node_code varchar(510),
  type integer,
  code varchar(1024),
  name_zh_cn varchar(510),
  name varchar(510),
  CONSTRAINT wf_transition_pkey PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS public.wf_transition_staff (
  id bigint NOT NULL,
  version integer DEFAULT 0,
  delete_time timestamp,
  modify_time timestamp,
  create_time timestamp,
  delete_staff_id bigint,
  modify_staff_id bigint,
  create_staff_id bigint,
  valid integer DEFAULT 1,
  sort bigint,
  group_name varchar(510),
  type_id bigint,
  type varchar(510),
  outcome varchar(510),
  deployment_id bigint,
  CONSTRAINT wf_transition_staff_pkey PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS public.supos_app (
  code varchar(1024) NOT NULL,
  ec_env varchar(510),
  version integer DEFAULT 0,
  delete_time timestamp,
  modify_time timestamp,
  create_time timestamp,
  delete_staff_id bigint,
  modify_staff_id bigint,
  create_staff_id bigint,
  valid integer DEFAULT 1,
  menus text,
  main_app_code text,
  modules text,
  memory bigint,
  app_type integer,
  name text,
  CONSTRAINT supos_app_pkey PRIMARY KEY (code)
);

CREATE TABLE IF NOT EXISTS public.action_view (
  action_url varchar(255) NOT NULL,
  view_name varchar(510),
  view_code varchar(510),
  CONSTRAINT action_view_pkey PRIMARY KEY (action_url)
);

CREATE TABLE IF NOT EXISTS public.sc_script (
  id bigint NOT NULL,
  version integer DEFAULT 0,
  script_code varchar(510),
  description varchar(510),
  name varchar(510),
  entity_code varchar(510),
  CONSTRAINT sc_script_pkey PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS public.base_cp_model_mapping (
  id bigint NOT NULL,
  version integer DEFAULT 0,
  precision integer,
  related_key varchar(510),
  sort integer,
  description text,
  model_code varchar(510),
  property_code varchar(510),
  enable_custom boolean,
  nullable boolean,
  reference_view_code varchar(510),
  associated_type integer,
  associated_property_code varchar(510),
  senior_system_code boolean,
  multable boolean,
  fill_content text,
  format varchar(510),
  field_type varchar(510),
  display_name varchar(510),
  CONSTRAINT base_cp_model_mapping_pkey PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS public.base_cp_view_mapping (
  id bigint NOT NULL,
  version integer DEFAULT 0,
  length integer,
  precision integer,
  align varchar(510),
  readonly boolean,
  associated_code varchar(510),
  property_layrec varchar(510),
  property_code varchar(510),
  custom_script text,
  custom_style text,
  textarea_row integer,
  colspan integer,
  sort integer,
  show_custom boolean,
  nullable boolean,
  format varchar(510),
  field_type varchar(510),
  display_name varchar(510),
  CONSTRAINT base_cp_view_mapping_pkey PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS public.base_resourceservice (
  res_code varchar(255) NOT NULL,
  charparamd text,
  charparamc text,
  charparamb text,
  charparama text,
  cid bigint,
  res_url text,
  res_name text,
  module_name varchar(100),
  CONSTRAINT base_resourceservice_pkey PRIMARY KEY (res_code)
);

CREATE TABLE IF NOT EXISTS public.base_role_respermission (
  id bigint NOT NULL,
  version integer DEFAULT 0,
  role_id varchar(510),
  res_type text,
  res_key text,
  res_service_code text,
  valid integer DEFAULT 1,
  CONSTRAINT base_role_respermission_pkey PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS public.base_user_respermission (
  id bigint NOT NULL,
  version integer DEFAULT 0,
  delete_time timestamp,
  modify_time timestamp,
  create_time timestamp,
  delete_staff_id bigint,
  modify_staff_id bigint,
  create_staff_id bigint,
  valid integer DEFAULT 1,
  cid bigint,
  role_id varchar(510),
  purview_type integer,
  res_type text,
  res_key text,
  res_service_code text,
  user_id bigint,
  CONSTRAINT base_user_respermission_pkey PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS public.selection_ranges (
  id bigint NOT NULL,
  version integer DEFAULT 0,
  delete_time timestamp,
  modify_time timestamp,
  create_time timestamp,
  delete_staff_id bigint,
  modify_staff_id bigint,
  create_staff_id bigint,
  valid boolean DEFAULT true,
  range_name varchar(510),
  sort double precision,
  type varchar(510),
  range_id bigint,
  group_name varchar(510),
  range_ids varchar(510),
  field_code varchar(510),
  CONSTRAINT selection_ranges_pkey PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS public.sys_deployment_log (
  id bigint NOT NULL,
  bundle_symbolicname varchar(255),
  bundle_version varchar(255),
  deploy_time timestamp,
  CONSTRAINT sys_deployment_log_pkey PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS public.jbpm4_deployment (
  dbid_ bigint NOT NULL,
  name_ text,
  timestamp_ bigint,
  state_ varchar(255),
  CONSTRAINT jbpm4_deployment_pkey PRIMARY KEY (dbid_)
);

CREATE TABLE IF NOT EXISTS public.jbpm4_hist_procinst (
  dbid_ bigint NOT NULL,
  dbversion_ integer NOT NULL,
  id_ varchar(255),
  procdefid_ varchar(255),
  key_ varchar(255),
  start_ timestamp,
  end_ timestamp,
  duration_ bigint,
  state_ varchar(255),
  endactivity_ varchar(255),
  nextidx_ integer,
  tableinfoid_ bigint,
  CONSTRAINT jbpm4_hist_procinst_pkey PRIMARY KEY (dbid_)
);

CREATE TABLE IF NOT EXISTS public.jbpm4_id_user (
  dbid_ bigint NOT NULL,
  dbversion_ integer NOT NULL,
  id_ varchar(255),
  password_ varchar(255),
  givenname_ varchar(255),
  familyname_ varchar(255),
  businessemail_ varchar(255),
  CONSTRAINT jbpm4_id_user_pkey PRIMARY KEY (dbid_)
);

CREATE TABLE IF NOT EXISTS public.jbpm4_property (
  key_ varchar(255) NOT NULL,
  version_ integer NOT NULL,
  value_ varchar(255),
  CONSTRAINT jbpm4_property_pkey PRIMARY KEY (key_)
);

CREATE TABLE IF NOT EXISTS public.jbpm4_deployprop (
  dbid_ bigint NOT NULL,
  deployment_ bigint,
  objname_ varchar(255),
  key_ varchar(255),
  stringval_ varchar(255),
  longval_ bigint,
  CONSTRAINT jbpm4_deployprop_pkey PRIMARY KEY (dbid_)
);

CREATE TABLE IF NOT EXISTS public.jbpm4_execution (
  dbid_ bigint NOT NULL,
  class_ varchar(255) NOT NULL,
  dbversion_ integer NOT NULL,
  activityname_ varchar(255),
  procdefid_ varchar(255),
  hasvars_ bigint,
  name_ varchar(255),
  key_ varchar(255),
  id_ varchar(255),
  state_ varchar(255),
  susphiststate_ varchar(255),
  priority_ integer,
  hisactinst_ bigint,
  parent_ bigint,
  instance_ bigint,
  superexec_ bigint,
  subprocinst_ bigint,
  parent_idx_ integer,
  process_initiator_ bigint,
  tableinfo_id_ bigint,
  deployment_id_ bigint,
  entity_code_ varchar(255),
  owner_id_ bigint,
  owner_position_id_ bigint,
  initiator_position_id_ bigint,
  model_id_ bigint,
  table_no_ varchar(255),
  table_name_ varchar(255),
  group_enabled_ bigint,
  cross_company_flag_ bigint,
  script_excute_bean_name_ varchar(255),
  CONSTRAINT jbpm4_execution_pkey PRIMARY KEY (dbid_)
);

CREATE TABLE IF NOT EXISTS public.jbpm4_hist_task (
  dbid_ bigint NOT NULL,
  dbversion_ integer NOT NULL,
  execution_ varchar(255),
  outcome_ varchar(255),
  assignee_ varchar(255),
  priority_ integer,
  state_ varchar(255),
  create_ timestamp,
  end_ timestamp,
  duration_ bigint,
  nextidx_ integer,
  supertask_ bigint,
  CONSTRAINT jbpm4_hist_task_pkey PRIMARY KEY (dbid_)
);

CREATE TABLE IF NOT EXISTS public.jbpm4_hist_var (
  dbid_ bigint NOT NULL,
  dbversion_ integer NOT NULL,
  procinstid_ varchar(255),
  executionid_ varchar(255),
  varname_ varchar(255),
  value_ varchar(255),
  hproci_ bigint,
  htask_ bigint,
  CONSTRAINT jbpm4_hist_var_pkey PRIMARY KEY (dbid_)
);

CREATE TABLE IF NOT EXISTS public.jbpm4_id_group (
  dbid_ bigint NOT NULL,
  dbversion_ integer NOT NULL,
  id_ varchar(255),
  name_ varchar(255),
  type_ varchar(255),
  parent_ bigint,
  CONSTRAINT jbpm4_id_group_pkey PRIMARY KEY (dbid_)
);

CREATE TABLE IF NOT EXISTS public.jbpm4_id_membership (
  dbid_ bigint NOT NULL,
  dbversion_ integer NOT NULL,
  user_ bigint,
  group_ bigint,
  name_ varchar(255),
  CONSTRAINT jbpm4_id_membership_pkey PRIMARY KEY (dbid_)
);

CREATE TABLE IF NOT EXISTS public.jbpm4_lob (
  dbid_ bigint NOT NULL,
  dbversion_ integer NOT NULL,
  blob_value_ bytea,
  deployment_ bigint,
  name_ text,
  CONSTRAINT jbpm4_lob_pkey PRIMARY KEY (dbid_)
);

CREATE TABLE IF NOT EXISTS public.jbpm4_swimlane (
  dbid_ bigint NOT NULL,
  dbversion_ integer NOT NULL,
  name_ varchar(255),
  assignee_ varchar(255),
  execution_ bigint,
  CONSTRAINT jbpm4_swimlane_pkey PRIMARY KEY (dbid_)
);

CREATE TABLE IF NOT EXISTS public.jbpm4_task (
  dbid_ bigint NOT NULL,
  class_ char(1) NOT NULL,
  dbversion_ integer NOT NULL,
  name_ varchar(255),
  descr_ text,
  state_ varchar(255),
  susphiststate_ varchar(255),
  assignee_ varchar(255),
  form_ varchar(255),
  priority_ integer,
  create_ timestamp,
  duedate_ timestamp,
  progress_ integer,
  signalling_ bigint,
  execution_id_ varchar(255),
  activity_name_ varchar(255),
  hasvars_ bigint,
  supertask_ bigint,
  execution_ bigint,
  procinst_ bigint,
  swimlane_ bigint,
  taskdefname_ varchar(255),
  CONSTRAINT jbpm4_task_pkey PRIMARY KEY (dbid_)
);

CREATE TABLE IF NOT EXISTS public.jbpm4_variable (
  dbid_ bigint NOT NULL,
  class_ varchar(255) NOT NULL,
  dbversion_ integer NOT NULL,
  key_ varchar(255),
  converter_ varchar(255),
  hist_ bigint,
  execution_ bigint,
  task_ bigint,
  lob_ bigint,
  date_value_ timestamp,
  double_value_ double precision,
  classname_ varchar(255),
  long_value_ bigint,
  string_value_ varchar(255),
  text_value_ text,
  exesys_ bigint,
  CONSTRAINT jbpm4_variable_pkey PRIMARY KEY (dbid_)
);

CREATE TABLE IF NOT EXISTS public.jbpm4_hist_actinst (
  dbid_ bigint NOT NULL,
  class_ varchar(255) NOT NULL,
  dbversion_ integer NOT NULL,
  hproci_ bigint,
  type_ varchar(255),
  execution_ varchar(255),
  activity_name_ varchar(255),
  start_ timestamp,
  end_ timestamp,
  duration_ bigint,
  transition_ varchar(255),
  nextidx_ integer,
  htask_ bigint,
  creatorid_ bigint,
  dealerid_ bigint,
  departmentid_ bigint,
  positionid_ bigint,
  tableinfoid_ bigint,
  CONSTRAINT jbpm4_hist_actinst_pkey PRIMARY KEY (dbid_)
);

CREATE TABLE IF NOT EXISTS public.jbpm4_hist_detail (
  dbid_ bigint NOT NULL,
  class_ varchar(255) NOT NULL,
  dbversion_ integer NOT NULL,
  userid_ varchar(255),
  time_ timestamp,
  hproci_ bigint,
  hprociidx_ integer,
  hacti_ bigint,
  hactiidx_ integer,
  htask_ bigint,
  htaskidx_ integer,
  hvar_ bigint,
  hvaridx_ integer,
  message_ text,
  old_str_ varchar(255),
  new_str_ varchar(255),
  old_int_ integer,
  new_int_ integer,
  old_time_ timestamp,
  new_time_ timestamp,
  parent_ bigint,
  parent_idx_ integer,
  CONSTRAINT jbpm4_hist_detail_pkey PRIMARY KEY (dbid_)
);

CREATE TABLE IF NOT EXISTS public.jbpm4_job (
  dbid_ bigint NOT NULL,
  class_ varchar(255) NOT NULL,
  dbversion_ integer NOT NULL,
  duedate_ timestamp,
  state_ varchar(255),
  isexclusive_ bigint,
  lockowner_ varchar(255),
  lockexptime_ timestamp,
  exception_ text,
  retries_ integer,
  processinstance_ bigint,
  execution_ bigint,
  cfg_ bigint,
  signal_ varchar(255),
  event_ varchar(255),
  repeat_ varchar(255),
  CONSTRAINT jbpm4_job_pkey PRIMARY KEY (dbid_)
);

CREATE TABLE IF NOT EXISTS public.jbpm4_participation (
  dbid_ bigint NOT NULL,
  dbversion_ integer NOT NULL,
  groupid_ varchar(255),
  userid_ varchar(255),
  type_ varchar(255),
  task_ bigint,
  swimlane_ bigint,
  CONSTRAINT jbpm4_participation_pkey PRIMARY KEY (dbid_)
);

CREATE TABLE IF NOT EXISTS public.sys_counter (
  id char(32) NOT NULL,
  name varchar(400) NOT NULL,
  typecode varchar(400),
  dealtime timestamp,
  category varchar(400),
  pattern varchar(400),
  value bigint NOT NULL
);

CREATE TABLE IF NOT EXISTS public.bap_signature_logs (
  uuid varchar(255) NOT NULL,
  operate_log_uuid text,
  second_sign_time timestamp,
  first_sign_time timestamp,
  transition_name varchar(510),
  task_name varchar(510),
  process_name varchar(510),
  business_key text,
  status integer,
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

CREATE TABLE IF NOT EXISTS public.base_cookie (
  id bigint NOT NULL,
  version integer DEFAULT 0,
  global integer,
  menu_info_json varchar(4000),
  cid bigint,
  user_id bigint,
  value text,
  type varchar(510),
  model_code varchar(255) NOT NULL,
  module_code varchar(510) NOT NULL,
  company_id bigint,
  CONSTRAINT base_cookie_pkey PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS public.seq_table (
  tablename varchar(255) NOT NULL,
  maxid bigint NOT NULL,
  CONSTRAINT seq_table_pkey PRIMARY KEY (tablename)
);

CREATE TABLE IF NOT EXISTS public.ec_sql_model (
  code varchar(1024),
  version integer,
  model_sql text,
  oracle_sql text,
  sqlserver_sql text,
  mariadb_sql text,
  dm_sql text
);

CREATE TABLE IF NOT EXISTS public.app_project_static (
  id bigint NOT NULL,
  url varchar(510),
  tenant_id varchar(510),
  html text,
  CONSTRAINT app_project_static_pkey PRIMARY KEY (id)
);

SELECT public.adp_create_index_if_columns_exist('idx_ec_adv_query_condition_valid', 'ec_adv_query_condition', ARRAY['valid']);

SELECT public.adp_create_index_if_columns_exist('idx_ec_adv_query_condition_item_valid', 'ec_adv_query_condition_item', ARRAY['valid']);

SELECT public.adp_create_index_if_columns_exist('idx_ec_backup_data_grid_valid', 'ec_backup_data_grid', ARRAY['valid']);

SELECT public.adp_create_index_if_columns_exist('idx_ec_backup_view_valid', 'ec_backup_view', ARRAY['valid']);

SELECT public.adp_create_index_if_columns_exist('idx_ec_button_valid', 'ec_button', ARRAY['valid']);

SELECT public.adp_create_index_if_columns_exist('idx_ec_button_cell_code', 'ec_button', ARRAY['cell_code']);

SELECT public.adp_create_index_if_columns_exist('idx_ec_button_entity_code', 'ec_button', ARRAY['entity_code']);

SELECT public.adp_create_index_if_columns_exist('idx_ec_button_module_code', 'ec_button', ARRAY['module_code']);

SELECT public.adp_create_index_if_columns_exist('idx_ec_button_viewselect_code', 'ec_button', ARRAY['viewselect_code']);

SELECT public.adp_create_index_if_columns_exist('idx_ec_customer_condition_valid', 'ec_customer_condition', ARRAY['valid']);

SELECT public.adp_create_index_if_columns_exist('idx_ec_data_classific_valid', 'ec_data_classific', ARRAY['valid']);

SELECT public.adp_create_index_if_columns_exist('idx_ec_data_grid_valid', 'ec_data_grid', ARRAY['valid']);

SELECT public.adp_create_index_if_columns_exist('idx_ec_data_grid_valid_module_code', 'ec_data_grid', ARRAY['valid', 'module_code']);

SELECT public.adp_create_index_if_columns_exist('idx_ec_data_group_valid', 'ec_data_group', ARRAY['valid']);

SELECT public.adp_create_index_if_columns_exist('idx_ec_echarts_valid', 'ec_echarts', ARRAY['valid']);

SELECT public.adp_create_index_if_columns_exist('idx_ec_echarts_model_valid', 'ec_echarts_model', ARRAY['valid']);

SELECT public.adp_create_index_if_columns_exist('idx_ec_entity_valid', 'ec_entity', ARRAY['valid']);

SELECT public.adp_create_index_if_columns_exist('idx_ec_event_valid', 'ec_event', ARRAY['valid']);

SELECT public.adp_create_index_if_columns_exist('idx_ec_extra_view_view_code', 'ec_extra_view', ARRAY['view_code']);

SELECT public.adp_create_index_if_columns_exist('idx_ec_field_valid', 'ec_field', ARRAY['valid']);

SELECT public.adp_create_index_if_columns_exist('idx_ec_field_cell_code', 'ec_field', ARRAY['cell_code']);

SELECT public.adp_create_index_if_columns_exist('idx_ec_field_datagrid_code', 'ec_field', ARRAY['datagrid_code']);

SELECT public.adp_create_index_if_columns_exist('idx_ec_field_entity_code', 'ec_field', ARRAY['entity_code']);

SELECT public.adp_create_index_if_columns_exist('idx_ec_field_module_code', 'ec_field', ARRAY['module_code']);

SELECT public.adp_create_index_if_columns_exist('idx_ec_field_view_code', 'ec_field', ARRAY['view_code']);

SELECT public.adp_create_index_if_columns_exist('idx_ec_layout_valid', 'ec_layout', ARRAY['valid']);

SELECT public.adp_create_index_if_columns_exist('idx_ec_model_valid', 'ec_model', ARRAY['valid']);

SELECT public.adp_create_index_if_columns_exist('idx_ec_module_valid', 'ec_module', ARRAY['valid']);

SELECT public.adp_create_index_if_columns_exist('idx_ec_module_reference_valid', 'ec_module_reference', ARRAY['valid']);

SELECT public.adp_create_index_if_columns_exist('idx_ec_module_relation_valid', 'ec_module_relation', ARRAY['valid']);

SELECT public.adp_create_index_if_columns_exist('idx_ec_msmodule_valid', 'ec_msmodule', ARRAY['valid']);

SELECT public.adp_create_index_if_columns_exist('idx_ec_msmodule_ipadress_valid', 'ec_msmodule_ipadress', ARRAY['valid']);

SELECT public.adp_create_index_if_columns_exist('idx_ec_msmodule_relation_valid', 'ec_msmodule_relation', ARRAY['valid']);

SELECT public.adp_create_index_if_columns_exist('idx_ec_other_restrict_valid', 'ec_other_restrict', ARRAY['valid']);

SELECT public.adp_create_index_if_columns_exist('idx_ec_portlet_menu_info_id', 'ec_portlet', ARRAY['menu_info_id']);

SELECT public.adp_create_index_if_columns_exist('idx_ec_portlet_menu_operate_id', 'ec_portlet', ARRAY['menu_operate_id']);

SELECT public.adp_create_index_if_columns_exist('idx_ec_print_template_valid', 'ec_print_template', ARRAY['valid']);

SELECT public.adp_create_index_if_columns_exist('idx_ec_property_valid', 'ec_property', ARRAY['valid']);

SELECT public.adp_create_index_if_columns_exist('idx_ec_property_entity_code', 'ec_property', ARRAY['entity_code']);

SELECT public.adp_create_index_if_columns_exist('idx_ec_property_model_code', 'ec_property', ARRAY['model_code']);

SELECT public.adp_create_index_if_columns_exist('idx_ec_property_module_code', 'ec_property', ARRAY['module_code']);

SELECT public.adp_create_index_if_columns_exist('idx_ec_scheduler_job_valid', 'ec_scheduler_job', ARRAY['valid']);

SELECT public.adp_create_index_if_columns_exist('idx_ec_special_permission_valid', 'ec_special_permission', ARRAY['valid']);

SELECT public.adp_create_index_if_columns_exist('idx_ec_special_permission_rshow_valid', 'ec_special_permission_rshow', ARRAY['valid']);

SELECT public.adp_create_index_if_columns_exist('idx_ec_special_permission_ushow_valid', 'ec_special_permission_ushow', ARRAY['valid']);

SELECT public.adp_create_index_if_columns_exist('idx_ec_table_info_valid', 'ec_table_info', ARRAY['valid']);

SELECT public.adp_create_index_if_columns_exist('idx_ec_table_info_owner_staff_id', 'ec_table_info', ARRAY['owner_staff_id']);

SELECT public.adp_create_index_if_columns_exist('idx_ec_validate_valid', 'ec_validate', ARRAY['valid']);

SELECT public.adp_create_index_if_columns_exist('idx_ec_view_valid', 'ec_view', ARRAY['valid']);

SELECT public.adp_create_index_if_columns_exist('idx_ec_view_valid_module_code', 'ec_view', ARRAY['valid', 'module_code']);

SELECT public.adp_create_index_if_columns_exist('idx_runtime_adv_query_json_view_code', 'runtime_adv_query_json', ARRAY['view_code']);

SELECT public.adp_create_index_if_columns_exist('idx_runtime_backup_data_grid_valid', 'runtime_backup_data_grid', ARRAY['valid']);

SELECT public.adp_create_index_if_columns_exist('idx_runtime_backup_data_grid_view_code', 'runtime_backup_data_grid', ARRAY['view_code']);

SELECT public.adp_create_index_if_columns_exist('idx_runtime_backup_view_valid', 'runtime_backup_view', ARRAY['valid']);

SELECT public.adp_create_index_if_columns_exist('idx_runtime_button_valid', 'runtime_button', ARRAY['valid']);

SELECT public.adp_create_index_if_columns_exist('idx_runtime_button_datagrid_code', 'runtime_button', ARRAY['datagrid_code']);

SELECT public.adp_create_index_if_columns_exist('idx_runtime_button_view_code', 'runtime_button', ARRAY['view_code']);

SELECT public.adp_create_index_if_columns_exist('idx_runtime_button_viewselect_code', 'runtime_button', ARRAY['viewselect_code']);

SELECT public.adp_create_index_if_columns_exist('idx_runtime_customer_condition_valid', 'runtime_customer_condition', ARRAY['valid']);

SELECT public.adp_create_index_if_columns_exist('idx_runtime_data_classific_valid', 'runtime_data_classific', ARRAY['valid']);

SELECT public.adp_create_index_if_columns_exist('idx_runtime_data_classific_data_group_code', 'runtime_data_classific', ARRAY['data_group_code']);

SELECT public.adp_create_index_if_columns_exist('idx_runtime_data_grid_valid', 'runtime_data_grid', ARRAY['valid']);

SELECT public.adp_create_index_if_columns_exist('idx_runtime_data_grid_view_code', 'runtime_data_grid', ARRAY['view_code']);

SELECT public.adp_create_index_if_columns_exist('idx_runtime_data_group_valid', 'runtime_data_group', ARRAY['valid']);

SELECT public.adp_create_index_if_columns_exist('idx_runtime_data_group_view_code', 'runtime_data_group', ARRAY['view_code']);

SELECT public.adp_create_index_if_columns_exist('idx_runtime_echarts_valid', 'runtime_echarts', ARRAY['valid']);

SELECT public.adp_create_index_if_columns_exist('idx_runtime_echarts_model_valid', 'runtime_echarts_model', ARRAY['valid']);

SELECT public.adp_create_index_if_columns_exist('idx_runtime_entity_valid', 'runtime_entity', ARRAY['valid']);

SELECT public.adp_create_index_if_columns_exist('idx_runtime_entity_module_code', 'runtime_entity', ARRAY['module_code']);

SELECT public.adp_create_index_if_columns_exist('idx_runtime_event_valid', 'runtime_event', ARRAY['valid']);

SELECT public.adp_create_index_if_columns_exist('idx_runtime_event_button_code', 'runtime_event', ARRAY['button_code']);

SELECT public.adp_create_index_if_columns_exist('idx_runtime_event_field_code', 'runtime_event', ARRAY['field_code']);

SELECT public.adp_create_index_if_columns_exist('idx_runtime_extra_query_json_view_code', 'runtime_extra_query_json', ARRAY['view_code']);

SELECT public.adp_create_index_if_columns_exist('idx_runtime_extra_view_view_code', 'runtime_extra_view', ARRAY['view_code']);

SELECT public.adp_create_index_if_columns_exist('idx_runtime_fast_query_json_view_code', 'runtime_fast_query_json', ARRAY['view_code']);

SELECT public.adp_create_index_if_columns_exist('idx_runtime_field_advqueryjson_code', 'runtime_field', ARRAY['advqueryjson_code']);

SELECT public.adp_create_index_if_columns_exist('idx_runtime_field_datagrid_code', 'runtime_field', ARRAY['datagrid_code']);

SELECT public.adp_create_index_if_columns_exist('idx_runtime_field_fastqueryjson_code', 'runtime_field', ARRAY['fastqueryjson_code']);

SELECT public.adp_create_index_if_columns_exist('idx_runtime_field_property_code', 'runtime_field', ARRAY['property_code']);

SELECT public.adp_create_index_if_columns_exist('idx_runtime_field_view_code', 'runtime_field', ARRAY['view_code']);

SELECT public.adp_create_index_if_columns_exist('idx_runtime_field_cell_code', 'runtime_field', ARRAY['cell_code']);

SELECT public.adp_create_index_if_columns_exist('idx_runtime_field_entity_code', 'runtime_field', ARRAY['entity_code']);

SELECT public.adp_create_index_if_columns_exist('idx_runtime_field_module_code', 'runtime_field', ARRAY['module_code']);

SELECT public.adp_create_index_if_columns_exist('idx_runtime_field_valid', 'runtime_field', ARRAY['valid']);

SELECT public.adp_create_index_if_columns_exist('idx_runtime_model_valid', 'runtime_model', ARRAY['valid']);

SELECT public.adp_create_index_if_columns_exist('idx_runtime_module_valid', 'runtime_module', ARRAY['valid']);

SELECT public.adp_create_index_if_columns_exist('idx_runtime_module_reference_valid', 'runtime_module_reference', ARRAY['valid']);

SELECT public.adp_create_index_if_columns_exist('idx_runtime_module_relation_valid', 'runtime_module_relation', ARRAY['valid']);

SELECT public.adp_create_index_if_columns_exist('idx_runtime_print_template_valid', 'runtime_print_template', ARRAY['valid']);

SELECT public.adp_create_index_if_columns_exist('idx_runtime_print_template_entity_code', 'runtime_print_template', ARRAY['entity_code']);

SELECT public.adp_create_index_if_columns_exist('idx_runtime_property_valid', 'runtime_property', ARRAY['valid']);

SELECT public.adp_create_index_if_columns_exist('idx_runtime_property_model_code', 'runtime_property', ARRAY['model_code']);

SELECT public.adp_create_index_if_columns_exist('idx_runtime_property_is_sensitive', 'runtime_property', ARRAY['is_sensitive']);

SELECT public.adp_create_index_if_columns_exist('idx_runtime_sql_type', 'runtime_sql', ARRAY['type']);

SELECT public.adp_create_index_if_columns_exist('idx_runtime_sql_view_code', 'runtime_sql', ARRAY['view_code']);

SELECT public.adp_create_index_if_columns_exist('idx_runtime_validate_field_code', 'runtime_validate', ARRAY['field_code']);

SELECT public.adp_create_index_if_columns_exist('idx_runtime_validate_valid', 'runtime_validate', ARRAY['valid']);

SELECT public.adp_create_index_if_columns_exist('idx_runtime_view_valid', 'runtime_view', ARRAY['valid']);

SELECT public.adp_create_index_if_columns_exist('idx_runtime_view_entity_code', 'runtime_view', ARRAY['entity_code']);

SELECT public.adp_create_index_if_columns_exist('idx_wf_countersign_assign_staff_valid', 'wf_countersign_assign_staff', ARRAY['valid']);

SELECT public.adp_create_index_if_columns_exist('idx_wf_deployment_valid', 'wf_deployment', ARRAY['valid']);

SELECT public.adp_create_index_if_columns_exist('idx_wf_deployment_menu_info_id', 'wf_deployment', ARRAY['menu_info_id']);

SELECT public.adp_create_index_if_columns_exist('idx_wf_expected_consign_active_code', 'wf_expected_consign', ARRAY['active_code']);

SELECT public.adp_create_index_if_columns_exist('idx_wf_expected_consign_flow_key', 'wf_expected_consign', ARRAY['flow_key']);

SELECT public.adp_create_index_if_columns_exist('idx_wf_pay_close_attention_table_info_id', 'wf_pay_close_attention', ARRAY['table_info_id']);

SELECT public.adp_create_index_if_columns_exist('idx_wf_pending_entity_code', 'wf_pending', ARRAY['entity_code']);

SELECT public.adp_create_index_if_columns_exist('idx_wf_pending_execution_id', 'wf_pending', ARRAY['execution_id']);

SELECT public.adp_create_index_if_columns_exist('idx_wf_pending_table_info_id', 'wf_pending', ARRAY['table_info_id']);

SELECT public.adp_create_index_if_columns_exist('idx_wf_pending_user_id', 'wf_pending', ARRAY['user_id']);

SELECT public.adp_create_index_if_columns_exist('idx_wf_supervise_valid', 'wf_supervise', ARRAY['valid']);

SELECT public.adp_create_index_if_columns_exist('idx_wf_supervision_table_info_id', 'wf_supervision', ARRAY['table_info_id']);

SELECT public.adp_create_index_if_columns_exist('idx_wf_task_valid', 'wf_task', ARRAY['valid']);

SELECT public.adp_create_index_if_columns_exist('idx_wf_transition_valid', 'wf_transition', ARRAY['valid']);

SELECT public.adp_create_index_if_columns_exist('idx_wf_transition_staff_valid', 'wf_transition_staff', ARRAY['valid']);

SELECT public.adp_create_index_if_columns_exist('idx_supos_app_valid', 'supos_app', ARRAY['valid']);

SELECT public.adp_create_index_if_columns_exist('idx_base_cp_model_mapping_model_code', 'base_cp_model_mapping', ARRAY['model_code']);

SELECT public.adp_create_index_if_columns_exist('idx_base_cp_model_mapping_property_code', 'base_cp_model_mapping', ARRAY['property_code']);

SELECT public.adp_create_index_if_columns_exist('idx_base_cp_view_mapping_property_code', 'base_cp_view_mapping', ARRAY['property_code']);

SELECT public.adp_create_index_if_columns_exist('idx_base_cp_view_mapping_associated_code', 'base_cp_view_mapping', ARRAY['associated_code']);

SELECT public.adp_create_index_if_columns_exist('idx_base_cp_view_mapping_property_layrec', 'base_cp_view_mapping', ARRAY['property_layrec']);

SELECT public.adp_create_index_if_columns_exist('idx_base_user_respermission_valid', 'base_user_respermission', ARRAY['valid']);

SELECT public.adp_create_index_if_columns_exist('idx_selection_ranges_valid', 'selection_ranges', ARRAY['valid']);

SELECT public.adp_create_index_if_columns_exist('idx_jbpm4_deployprop_deployment_', 'jbpm4_deployprop', ARRAY['deployment_']);

SELECT public.adp_create_index_if_columns_exist('idx_jbpm4_execution_instance_', 'jbpm4_execution', ARRAY['instance_']);

SELECT public.adp_create_index_if_columns_exist('idx_jbpm4_execution_parent_', 'jbpm4_execution', ARRAY['parent_']);

SELECT public.adp_create_index_if_columns_exist('idx_jbpm4_execution_subprocinst_', 'jbpm4_execution', ARRAY['subprocinst_']);

SELECT public.adp_create_index_if_columns_exist('idx_jbpm4_execution_superexec_', 'jbpm4_execution', ARRAY['superexec_']);

SELECT public.adp_create_index_if_columns_exist('idx_jbpm4_execution_tableinfo_id_', 'jbpm4_execution', ARRAY['tableinfo_id_']);

SELECT public.adp_create_index_if_columns_exist('idx_jbpm4_hist_task_supertask_', 'jbpm4_hist_task', ARRAY['supertask_']);

SELECT public.adp_create_index_if_columns_exist('idx_jbpm4_hist_var_hproci_', 'jbpm4_hist_var', ARRAY['hproci_']);

SELECT public.adp_create_index_if_columns_exist('idx_jbpm4_hist_var_htask_', 'jbpm4_hist_var', ARRAY['htask_']);

SELECT public.adp_create_index_if_columns_exist('idx_jbpm4_id_group_parent_', 'jbpm4_id_group', ARRAY['parent_']);

SELECT public.adp_create_index_if_columns_exist('idx_jbpm4_id_membership_group_', 'jbpm4_id_membership', ARRAY['group_']);

SELECT public.adp_create_index_if_columns_exist('idx_jbpm4_id_membership_user_', 'jbpm4_id_membership', ARRAY['user_']);

SELECT public.adp_create_index_if_columns_exist('idx_jbpm4_lob_deployment_', 'jbpm4_lob', ARRAY['deployment_']);

SELECT public.adp_create_index_if_columns_exist('idx_jbpm4_swimlane_execution_', 'jbpm4_swimlane', ARRAY['execution_']);

SELECT public.adp_create_index_if_columns_exist('idx_jbpm4_task_supertask_', 'jbpm4_task', ARRAY['supertask_']);

SELECT public.adp_create_index_if_columns_exist('idx_jbpm4_variable_execution_', 'jbpm4_variable', ARRAY['execution_']);

SELECT public.adp_create_index_if_columns_exist('idx_jbpm4_variable_exesys_', 'jbpm4_variable', ARRAY['exesys_']);

SELECT public.adp_create_index_if_columns_exist('idx_jbpm4_variable_lob_', 'jbpm4_variable', ARRAY['lob_']);

SELECT public.adp_create_index_if_columns_exist('idx_jbpm4_variable_task_', 'jbpm4_variable', ARRAY['task_']);

SELECT public.adp_create_index_if_columns_exist('idx_jbpm4_hist_actinst_hproci_', 'jbpm4_hist_actinst', ARRAY['hproci_']);

SELECT public.adp_create_index_if_columns_exist('idx_jbpm4_hist_actinst_htask_', 'jbpm4_hist_actinst', ARRAY['htask_']);

SELECT public.adp_create_index_if_columns_exist('idx_jbpm4_hist_detail_hacti_', 'jbpm4_hist_detail', ARRAY['hacti_']);

SELECT public.adp_create_index_if_columns_exist('idx_jbpm4_hist_detail_hproci_', 'jbpm4_hist_detail', ARRAY['hproci_']);

SELECT public.adp_create_index_if_columns_exist('idx_jbpm4_hist_detail_htask_', 'jbpm4_hist_detail', ARRAY['htask_']);

SELECT public.adp_create_index_if_columns_exist('idx_jbpm4_hist_detail_hvar_', 'jbpm4_hist_detail', ARRAY['hvar_']);

SELECT public.adp_create_index_if_columns_exist('idx_jbpm4_job_duedate_', 'jbpm4_job', ARRAY['duedate_']);

SELECT public.adp_create_index_if_columns_exist('idx_jbpm4_job_lockexptime_', 'jbpm4_job', ARRAY['lockexptime_']);

SELECT public.adp_create_index_if_columns_exist('idx_jbpm4_job_retries_', 'jbpm4_job', ARRAY['retries_']);

SELECT public.adp_create_index_if_columns_exist('idx_jbpm4_job_cfg_', 'jbpm4_job', ARRAY['cfg_']);

SELECT public.adp_create_index_if_columns_exist('idx_jbpm4_job_processinstance_', 'jbpm4_job', ARRAY['processinstance_']);

SELECT public.adp_create_index_if_columns_exist('idx_jbpm4_participation_task_', 'jbpm4_participation', ARRAY['task_']);
