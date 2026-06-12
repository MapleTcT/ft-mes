-- Task scheduler runtime compatibility for PostgreSQL.
--
-- The bundled Quartz DDL came from MySQL/Oracle-style scripts where boolean
-- flags were varchar(1). The PostgreSQL driver receives "true"/"false" from
-- Quartz, so varchar(1) rejects job registration. Newer scheduler builds also
-- write operation logs and callback fields that were absent from the original
-- recovered PostgreSQL schema.
ALTER TABLE IF EXISTS public.qrtz_job_details
  ALTER COLUMN is_durable TYPE varchar(8),
  ALTER COLUMN is_nonconcurrent TYPE varchar(8),
  ALTER COLUMN is_update_data TYPE varchar(8),
  ALTER COLUMN requests_recovery TYPE varchar(8);

ALTER TABLE IF EXISTS public.qrtz_fired_triggers
  ALTER COLUMN is_nonconcurrent TYPE varchar(8),
  ALTER COLUMN requests_recovery TYPE varchar(8);

ALTER TABLE IF EXISTS public.qrtz_simprop_triggers
  ALTER COLUMN bool_prop_1 TYPE varchar(8),
  ALTER COLUMN bool_prop_2 TYPE varchar(8);

ALTER TABLE IF EXISTS public.scheduler_job_log_info
  ADD COLUMN IF NOT EXISTS callback_time timestamp DEFAULT NULL,
  ADD COLUMN IF NOT EXISTS callback_data varchar(510);

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
