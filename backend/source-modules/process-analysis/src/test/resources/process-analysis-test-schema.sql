CREATE TABLE wom_produce_tasks (
  id BIGINT PRIMARY KEY, table_no VARCHAR(128), produce_batch_num VARCHAR(128), product_id BIGINT,
  cid BIGINT, line_id BIGINT,
  task_run_state VARCHAR(128), check_state VARCHAR(128), check_result VARCHAR(128),
  plan_num NUMERIC(20,6), finish_num NUMERIC(20,6), act_start_time TIMESTAMP, act_end_time TIMESTAMP,
  create_time TIMESTAMP, modify_time TIMESTAMP, valid BOOLEAN
);
CREATE TABLE baseset_materials (
  id BIGINT PRIMARY KEY, code VARCHAR(128), name VARCHAR(255), valid BOOLEAN
);
CREATE TABLE hm_factory_models (
  id BIGINT PRIMARY KEY, code VARCHAR(128), name VARCHAR(255), valid BOOLEAN
);
CREATE TABLE wom_bpi_production_context_bindings (
  id BIGINT PRIMARY KEY, wom_cid BIGINT, wom_line_id BIGINT, tenant_id VARCHAR(64),
  plant_id VARCHAR(128), line_id VARCHAR(128), enabled BOOLEAN
);
CREATE TABLE wom_produce_task_exelog (
  id BIGINT PRIMARY KEY, task_id BIGINT, table_no VARCHAR(128), produce_batch_num VARCHAR(128),
  product_id BIGINT, task_run_state VARCHAR(128), check_state VARCHAR(128), check_result VARCHAR(128),
  finish_num NUMERIC(20,6), analysis_flag BOOLEAN, act_start_time TIMESTAMP, act_end_time TIMESTAMP,
  create_time TIMESTAMP, modify_time TIMESTAMP
);
CREATE TABLE wom_task_processes (
  id BIGINT PRIMARY KEY, task_id BIGINT, table_no VARCHAR(128), name VARCHAR(255), exe_order INTEGER,
  process_run_state VARCHAR(128), act_start_time TIMESTAMP, act_end_time TIMESTAMP, plan_end_time TIMESTAMP,
  create_time TIMESTAMP, modify_time TIMESTAMP, hidden_sort NUMERIC(20,6), valid BOOLEAN
);
CREATE TABLE wom_process_exelogs (
  id BIGINT PRIMARY KEY, task_id BIGINT, task_process_id BIGINT, table_no VARCHAR(128), name VARCHAR(255),
  exe_order INTEGER,
  produce_batch_num VARCHAR(128), process_run_state VARCHAR(128), analysis_flag BOOLEAN,
  act_start_time TIMESTAMP, act_end_time TIMESTAMP, long_time NUMERIC(20,6),
  create_time TIMESTAMP, modify_time TIMESTAMP, valid BOOLEAN
);
CREATE TABLE wom_task_actives (
  id BIGINT PRIMARY KEY, task_id BIGINT, task_process_id BIGINT, table_no VARCHAR(128), name VARCHAR(255),
  material_batch_num VARCHAR(128), material_id BIGINT, run_state VARCHAR(128), check_state VARCHAR(128),
  check_result VARCHAR(128), plan_quantity NUMERIC(20,6), sum_num NUMERIC(20,6), hidden_sort NUMERIC(20,6),
  act_start_time TIMESTAMP, act_end_time TIMESTAMP, create_time TIMESTAMP, modify_time TIMESTAMP, valid BOOLEAN
);
CREATE TABLE wom_acti_exelogs (
  id BIGINT PRIMARY KEY, task_id BIGINT, task_process_id BIGINT, task_active_id BIGINT,
  table_no VARCHAR(128), name VARCHAR(255), produce_batch_num VARCHAR(128), material_batch_num VARCHAR(128),
  material_id BIGINT, run_state VARCHAR(128), analysis_flag BOOLEAN, actual_num NUMERIC(20,6),
  use_num NUMERIC(20,6), putin_detail_id BIGINT, output_detail_id BIGINT, act_start_time TIMESTAMP,
  act_end_time TIMESTAMP, create_time TIMESTAMP, modify_time TIMESTAMP, valid BOOLEAN
);
CREATE TABLE wom_putin_details (
  id BIGINT PRIMARY KEY, table_no VARCHAR(128), material_id BIGINT, material_batch_num VARCHAR(128),
  putin_num NUMERIC(20,6), use_num NUMERIC(20,6), putin_time TIMESTAMP, putin_end_time TIMESTAMP,
  ware_id BIGINT, store_id BIGINT, is_finish BOOLEAN, create_time TIMESTAMP
);
CREATE TABLE wom_output_details (
  id BIGINT PRIMARY KEY, table_no VARCHAR(128), material_batch_num VARCHAR(128), product BIGINT,
  output_num NUMERIC(20,6), report_num NUMERIC(20,6), putin_time TIMESTAMP, putin_end_time TIMESTAMP,
  ware_id BIGINT, store_id BIGINT, create_time TIMESTAMP
);
CREATE TABLE wom_mat_outpt_records (
  id BIGINT PRIMARY KEY, table_no VARCHAR(128), task_exelog_id BIGINT, proc_exelog_id BIGINT,
  act_exelog_id BIGINT, material_id BIGINT, mat_batch_num VARCHAR(128), produce_batch_num VARCHAR(128),
  output_num NUMERIC(20,6), output_time TIMESTAMP, output_end_time TIMESTAMP, ware_id BIGINT,
  store_id BIGINT, create_time TIMESTAMP, valid BOOLEAN
);
CREATE TABLE baseset_batch_infos (
  id BIGINT PRIMARY KEY, table_no VARCHAR(128), batch_num VARCHAR(128), material_id BIGINT,
  source_type VARCHAR(128), production_date TIMESTAMP, in_store_date TIMESTAMP, check_state VARCHAR(128),
  check_result VARCHAR(128), pass_state VARCHAR(128), is_available BOOLEAN, create_time TIMESTAMP,
  modify_time TIMESTAMP, valid BOOLEAN
);
CREATE TABLE qcs_inspects (
  id BIGINT PRIMARY KEY, table_no VARCHAR(128), source_id BIGINT, source_type VARCHAR(128),
  sourc_table_no VARCHAR(128), batch_code VARCHAR(128), prod_id BIGINT, quantity NUMERIC(20,6),
  check_state VARCHAR(128), closed BOOLEAN, apply_time TIMESTAMP, create_time TIMESTAMP,
  modify_time TIMESTAMP, valid BOOLEAN
);
CREATE TABLE qcs_inspect_reports (
  id BIGINT PRIMARY KEY, table_no VARCHAR(128), inspect_id BIGINT, batch_code VARCHAR(128), prod_id BIGINT,
  check_result VARCHAR(128), check_res_code VARCHAR(128), un_qlf_deal_flag BOOLEAN, check_time TIMESTAMP,
  create_time TIMESTAMP, modify_time TIMESTAMP, valid BOOLEAN
);
CREATE TABLE qcs_report_coms (
  id BIGINT PRIMARY KEY, report_id BIGINT, report_name VARCHAR(255), disp_value VARCHAR(255),
  check_result VARCHAR(128), unit_name VARCHAR(64), min_value NUMERIC(20,6), max_value NUMERIC(20,6),
  index_range VARCHAR(255), sort INTEGER, create_time TIMESTAMP, modify_time TIMESTAMP, valid BOOLEAN
);
CREATE TABLE qcs_un_qlf_deals (
  id BIGINT PRIMARY KEY, table_no VARCHAR(128), report_id BIGINT, batch_code VARCHAR(128), prod_id BIGINT,
  un_qlf_reason VARCHAR(255), deal_time TIMESTAMP, status INTEGER, effect_time TIMESTAMP,
  create_time TIMESTAMP, modify_time TIMESTAMP, valid BOOLEAN
);
CREATE TABLE wms_stock_documents (
  id BIGINT PRIMARY KEY, tenant_id VARCHAR(64), document_no VARCHAR(128), document_type VARCHAR(32), source_document_id VARCHAR(128),
  source_document_no VARCHAR(128), directive_no VARCHAR(128), warehouse_code VARCHAR(64), storage_date DATE,
  status VARCHAR(32), quality_status VARCHAR(32), created_at TIMESTAMP, updated_at TIMESTAMP
);
CREATE TABLE wms_stock_document_lines (
  id BIGINT PRIMARY KEY, tenant_id VARCHAR(64), document_id BIGINT, source_line_id VARCHAR(128), material_code VARCHAR(128),
  batch_no VARCHAR(128), production_batch_no VARCHAR(128), warehouse_code VARCHAR(64),
  location_code VARCHAR(64), quantity NUMERIC(20,6), quality_status VARCHAR(32),
  created_at TIMESTAMP, updated_at TIMESTAMP
);
CREATE TABLE wms_inventory_transactions (
  id BIGINT PRIMARY KEY, tenant_id VARCHAR(64), event_key VARCHAR(255), transaction_type VARCHAR(32), source_document_id VARCHAR(128),
  source_line_id VARCHAR(128), material_code VARCHAR(128), batch_no VARCHAR(128),
  production_batch_no VARCHAR(128), on_hand_delta NUMERIC(20,6), available_delta NUMERIC(20,6),
  hold_delta NUMERIC(20,6), balance_on_hand NUMERIC(20,6), balance_available NUMERIC(20,6),
  balance_hold NUMERIC(20,6), created_at TIMESTAMP
);
CREATE TABLE pa_trace_snapshots (
  id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, tenant_id VARCHAR(64) NOT NULL,
  source_type VARCHAR(16) NOT NULL, source_id BIGINT NOT NULL, task_id BIGINT NOT NULL,
  batch_no VARCHAR(128) NOT NULL, source_state VARCHAR(128) NOT NULL, metrics_json TEXT NOT NULL,
  source_updated_at TIMESTAMP, revision BIGINT NOT NULL DEFAULT 1, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (tenant_id, source_type, source_id)
);
