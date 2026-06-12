-- Module batch import uses the design-time configuration tables. The recovered
-- PostgreSQL profile already has runtime_entity for page rendering, but formal
-- module import still queries EC_ENTITY while sorting uploaded modules.

DO $$
BEGIN
  IF to_regclass('public.runtime_entity') IS NULL THEN
    RAISE EXCEPTION 'runtime_entity must exist before ec_entity compatibility is created';
  END IF;

  IF to_regclass('public.ec_entity') IS NULL THEN
    CREATE TABLE public.ec_entity
      (LIKE public.runtime_entity INCLUDING DEFAULTS INCLUDING CONSTRAINTS);
  END IF;
END $$;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_constraint
    WHERE conrelid = 'public.ec_entity'::regclass
      AND contype = 'p'
  ) THEN
    ALTER TABLE public.ec_entity
      ADD CONSTRAINT ec_entity_pkey PRIMARY KEY (code);
  END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_ec_entity_valid
  ON public.ec_entity(valid);

CREATE INDEX IF NOT EXISTS idx_ec_entity_module_code
  ON public.ec_entity(module_code);

INSERT INTO public.ec_entity (
  code, id, ec_env, version, delete_time, modify_time, create_time,
  delete_staff_id, modify_staff_id, create_staff_id, valid,
  enable_fields_permission_conf, enable_ws, enable_rest, proj_flag, type,
  enable_audit, enable_acl_restrict, mobile, cross_company_flag, is_control,
  is_inherented_base, inherent_common_flag, is_base, module_code,
  pay_close_attention, group_enabled, prefix, description, workflow_enabled,
  entity_name, value_zh_cn, name
)
SELECT
  code, id, ec_env, version, delete_time, modify_time, create_time,
  delete_staff_id, modify_staff_id, create_staff_id, valid,
  enable_fields_permission_conf, enable_ws, enable_rest, proj_flag, type,
  enable_audit, enable_acl_restrict, mobile, cross_company_flag, is_control,
  is_inherented_base, inherent_common_flag, is_base, module_code,
  pay_close_attention, group_enabled, prefix, description, workflow_enabled,
  entity_name, value_zh_cn, name
FROM public.runtime_entity
ON CONFLICT (code) DO NOTHING;

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
  upload_staff bigint,
  upload_state varchar(510),
  upload_date timestamp,
  CONSTRAINT ec_upload_info_batch_pkey PRIMARY KEY (id)
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
  cur_version varchar(1000),
  old_version varchar(1000),
  upload_staff bigint,
  upload_state varchar(510),
  upload_date timestamp,
  upload_filename varchar(1000),
  module_name varchar(1000),
  module_code varchar(1000),
  CONSTRAINT ec_upload_info_pkey PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_ec_upload_info_batchid
  ON public.ec_upload_info(batchid);
