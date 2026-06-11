-- Workflow widgets and legacy BAP services expect ec_table_info to exist even
-- when the base package has no business workflow instances.
CREATE TABLE IF NOT EXISTS public.ec_table_info (
  id BIGINT PRIMARY KEY,
  version INTEGER DEFAULT 0,
  delete_time TIMESTAMP,
  modify_time TIMESTAMP,
  create_time TIMESTAMP,
  delete_staff_id BIGINT,
  modify_staff_id BIGINT,
  create_staff_id BIGINT,
  valid INTEGER DEFAULT 1,
  effective_state INTEGER,
  process_version INTEGER,
  process_key VARCHAR(510),
  deployment_id BIGINT,
  summary TEXT,
  target_table_name VARCHAR(510),
  status INTEGER,
  owner_depaetment_id BIGINT,
  owner_department_id BIGINT,
  owner_position_id BIGINT,
  owner_staff_id BIGINT,
  target_entity_code VARCHAR(510),
  create_position_id BIGINT,
  position_lay_rec VARCHAR(510),
  effect_time TIMESTAMP,
  effect_staff_id BIGINT,
  create_department_id BIGINT,
  table_no VARCHAR(510)
);

CREATE INDEX IF NOT EXISTS idx_ec_table_info_valid
  ON public.ec_table_info (valid);

CREATE INDEX IF NOT EXISTS idx_ec_table_info_owner_staff_id
  ON public.ec_table_info (owner_staff_id);

CREATE INDEX IF NOT EXISTS idx_ec_table_info_table_no
  ON public.ec_table_info (table_no);
