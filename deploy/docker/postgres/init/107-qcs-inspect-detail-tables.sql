-- QCS inspect/detail table compatibility for PostgreSQL runtime.
-- Source models:
--   QCS_INSPECTS_DI, QCS_INSPECT_STDS, QCS_INSPECT_COMS,
--   QCS_INSPECT_REPORTS_DI, QCS_INSPECT_REPORTS_SV, QCS_REPORT_COMS,
--   QCS_INSPECT_RELEASES_DI, QCS_UN_QLF_DEALS_DI
--
-- The recovered runtime can create QCS main tables from view metadata, but the
-- PostgreSQL test database is missing these detail/deal tables. WOM -> QCS
-- inspection creation persists QCSInspectStd/QCSInspectCom records through
-- Hibernate, so these tables must exist before a real production inspection
-- request can be accepted as persistence-verified.

DO $$
DECLARE
  table_name text;
BEGIN
  FOREACH table_name IN ARRAY ARRAY[
    'qcs_inspects_di',
    'qcs_inspect_releases_di',
    'qcs_inspect_reports_di',
    'qcs_un_qlf_deals_di'
  ] LOOP
    EXECUTE format('CREATE TABLE IF NOT EXISTS public.%I (id int8 PRIMARY KEY)', table_name);
    EXECUTE format('ALTER TABLE public.%I ADD COLUMN IF NOT EXISTS version int4', table_name);
    EXECUTE format('ALTER TABLE public.%I ADD COLUMN IF NOT EXISTS create_staff_id int8', table_name);
    EXECUTE format('ALTER TABLE public.%I ADD COLUMN IF NOT EXISTS create_time timestamp', table_name);
    EXECUTE format('ALTER TABLE public.%I ADD COLUMN IF NOT EXISTS modify_staff_id int8', table_name);
    EXECUTE format('ALTER TABLE public.%I ADD COLUMN IF NOT EXISTS modify_time timestamp', table_name);
    EXECUTE format('ALTER TABLE public.%I ADD COLUMN IF NOT EXISTS delete_staff_id int8', table_name);
    EXECUTE format('ALTER TABLE public.%I ADD COLUMN IF NOT EXISTS delete_time timestamp', table_name);
    EXECUTE format('ALTER TABLE public.%I ADD COLUMN IF NOT EXISTS valid boolean', table_name);
    EXECUTE format('ALTER TABLE public.%I ADD COLUMN IF NOT EXISTS cid int8', table_name);
    EXECUTE format('ALTER TABLE public.%I ADD COLUMN IF NOT EXISTS sort int4', table_name);
    EXECUTE format('ALTER TABLE public.%I ADD COLUMN IF NOT EXISTS main_obj int8', table_name);
    EXECUTE format('ALTER TABLE public.%I ADD COLUMN IF NOT EXISTS staff int8', table_name);
    EXECUTE format('ALTER TABLE public.%I ADD COLUMN IF NOT EXISTS recalled_flag boolean', table_name);
    EXECUTE format('ALTER TABLE public.%I ADD COLUMN IF NOT EXISTS user_agent varchar(2000)', table_name);
    EXECUTE format('ALTER TABLE public.%I ADD COLUMN IF NOT EXISTS table_info_id int8', table_name);
    EXECUTE format('CREATE INDEX IF NOT EXISTS idx_%s_table_info_id ON public.%I (table_info_id)', table_name, table_name);
    EXECUTE format('CREATE INDEX IF NOT EXISTS idx_%s_main_obj ON public.%I (main_obj)', table_name, table_name);
    EXECUTE format('CREATE INDEX IF NOT EXISTS idx_%s_valid ON public.%I (valid)', table_name, table_name);
  END LOOP;
END $$;

CREATE OR REPLACE VIEW public.qcs_inspect_reports_sv AS
SELECT
  table_info_id,
  owner_staff_id AS staff,
  id,
  id AS main_obj,
  valid,
  version,
  create_staff_id,
  create_time,
  modify_staff_id,
  modify_time,
  delete_staff_id,
  delete_time,
  cid,
  sort
FROM public.qcs_inspect_reports
WHERE table_info_id IS NOT NULL
  AND owner_staff_id IS NOT NULL;

CREATE TABLE IF NOT EXISTS public.qcs_inspect_stds (id int8 PRIMARY KEY);
ALTER TABLE public.qcs_inspect_stds
  ADD COLUMN IF NOT EXISTS version int4,
  ADD COLUMN IF NOT EXISTS create_staff_id int8,
  ADD COLUMN IF NOT EXISTS create_time timestamp,
  ADD COLUMN IF NOT EXISTS modify_staff_id int8,
  ADD COLUMN IF NOT EXISTS modify_time timestamp,
  ADD COLUMN IF NOT EXISTS delete_staff_id int8,
  ADD COLUMN IF NOT EXISTS delete_time timestamp,
  ADD COLUMN IF NOT EXISTS valid boolean,
  ADD COLUMN IF NOT EXISTS cid int8,
  ADD COLUMN IF NOT EXISTS sort int4,
  ADD COLUMN IF NOT EXISTS table_info_id int8,
  ADD COLUMN IF NOT EXISTS inspect_id int8,
  ADD COLUMN IF NOT EXISTS inspect_proj_id int8,
  ADD COLUMN IF NOT EXISTS insp_std_ver_com varchar(2000),
  ADD COLUMN IF NOT EXISTS memo_field varchar(2000),
  ADD COLUMN IF NOT EXISTS std_ver_id int8,
  ADD COLUMN IF NOT EXISTS skip_type varchar(255),
  ADD COLUMN IF NOT EXISTS bigintparama int4,
  ADD COLUMN IF NOT EXISTS bigintparamb int4,
  ADD COLUMN IF NOT EXISTS bigintparamc int4,
  ADD COLUMN IF NOT EXISTS bigintparamd int4,
  ADD COLUMN IF NOT EXISTS bigintparame int4,
  ADD COLUMN IF NOT EXISTS bigintparamf int4,
  ADD COLUMN IF NOT EXISTS bigintparamg int4,
  ADD COLUMN IF NOT EXISTS bigintparamh int4,
  ADD COLUMN IF NOT EXISTS bigintparami int4,
  ADD COLUMN IF NOT EXISTS bigintparamj int4,
  ADD COLUMN IF NOT EXISTS charparama varchar(2000),
  ADD COLUMN IF NOT EXISTS charparamb varchar(2000),
  ADD COLUMN IF NOT EXISTS charparamc varchar(2000),
  ADD COLUMN IF NOT EXISTS charparamd varchar(2000),
  ADD COLUMN IF NOT EXISTS charparame varchar(2000),
  ADD COLUMN IF NOT EXISTS charparamf varchar(2000),
  ADD COLUMN IF NOT EXISTS charparamg varchar(2000),
  ADD COLUMN IF NOT EXISTS charparamh varchar(2000),
  ADD COLUMN IF NOT EXISTS charparami varchar(2000),
  ADD COLUMN IF NOT EXISTS charparamj varchar(2000),
  ADD COLUMN IF NOT EXISTS dateparama timestamp,
  ADD COLUMN IF NOT EXISTS dateparamb timestamp,
  ADD COLUMN IF NOT EXISTS dateparamc timestamp,
  ADD COLUMN IF NOT EXISTS dateparamd timestamp,
  ADD COLUMN IF NOT EXISTS dateparame timestamp,
  ADD COLUMN IF NOT EXISTS dateparamf timestamp,
  ADD COLUMN IF NOT EXISTS dateparamg timestamp,
  ADD COLUMN IF NOT EXISTS dateparamh timestamp,
  ADD COLUMN IF NOT EXISTS numberparama numeric(38,6),
  ADD COLUMN IF NOT EXISTS numberparamb numeric(38,6),
  ADD COLUMN IF NOT EXISTS numberparamc numeric(38,6),
  ADD COLUMN IF NOT EXISTS numberparamd numeric(38,6),
  ADD COLUMN IF NOT EXISTS numberparame numeric(38,6),
  ADD COLUMN IF NOT EXISTS numberparamf numeric(38,6),
  ADD COLUMN IF NOT EXISTS objparama int8,
  ADD COLUMN IF NOT EXISTS objparamb int8,
  ADD COLUMN IF NOT EXISTS objparamc int8,
  ADD COLUMN IF NOT EXISTS objparamd int8,
  ADD COLUMN IF NOT EXISTS scparama varchar(2000),
  ADD COLUMN IF NOT EXISTS scparamb varchar(2000),
  ADD COLUMN IF NOT EXISTS scparamc varchar(2000),
  ADD COLUMN IF NOT EXISTS scparamd varchar(2000);

CREATE INDEX IF NOT EXISTS idx_qcs_inspect_stds_table_info_id ON public.qcs_inspect_stds (table_info_id);
CREATE INDEX IF NOT EXISTS idx_qcs_inspect_stds_inspect_id ON public.qcs_inspect_stds (inspect_id);
CREATE INDEX IF NOT EXISTS idx_qcs_inspect_stds_std_ver_id ON public.qcs_inspect_stds (std_ver_id);
CREATE INDEX IF NOT EXISTS idx_qcs_inspect_stds_inspect_proj_id ON public.qcs_inspect_stds (inspect_proj_id);
CREATE INDEX IF NOT EXISTS idx_qcs_inspect_stds_valid ON public.qcs_inspect_stds (valid);
CREATE INDEX IF NOT EXISTS idx_qcs_inspect_stds_cid ON public.qcs_inspect_stds (cid);

CREATE TABLE IF NOT EXISTS public.qcs_inspect_coms (id int8 PRIMARY KEY);
ALTER TABLE public.qcs_inspect_coms
  ADD COLUMN IF NOT EXISTS version int4,
  ADD COLUMN IF NOT EXISTS create_staff_id int8,
  ADD COLUMN IF NOT EXISTS create_time timestamp,
  ADD COLUMN IF NOT EXISTS modify_staff_id int8,
  ADD COLUMN IF NOT EXISTS modify_time timestamp,
  ADD COLUMN IF NOT EXISTS delete_staff_id int8,
  ADD COLUMN IF NOT EXISTS delete_time timestamp,
  ADD COLUMN IF NOT EXISTS valid boolean,
  ADD COLUMN IF NOT EXISTS cid int8,
  ADD COLUMN IF NOT EXISTS sort int4,
  ADD COLUMN IF NOT EXISTS table_info_id int8,
  ADD COLUMN IF NOT EXISTS count_num int4,
  ADD COLUMN IF NOT EXISTS inspect_id int8,
  ADD COLUMN IF NOT EXISTS inspect_std_id int8,
  ADD COLUMN IF NOT EXISTS last_test_date timestamp,
  ADD COLUMN IF NOT EXISTS std_ver_com_id int8,
  ADD COLUMN IF NOT EXISTS test_frequency int8,
  ADD COLUMN IF NOT EXISTS bigintparama int4,
  ADD COLUMN IF NOT EXISTS bigintparamb int4,
  ADD COLUMN IF NOT EXISTS bigintparamc int4,
  ADD COLUMN IF NOT EXISTS bigintparamd int4,
  ADD COLUMN IF NOT EXISTS bigintparame int4,
  ADD COLUMN IF NOT EXISTS bigintparamf int4,
  ADD COLUMN IF NOT EXISTS bigintparamg int4,
  ADD COLUMN IF NOT EXISTS bigintparamh int4,
  ADD COLUMN IF NOT EXISTS bigintparami int4,
  ADD COLUMN IF NOT EXISTS bigintparamj int4,
  ADD COLUMN IF NOT EXISTS charparama varchar(2000),
  ADD COLUMN IF NOT EXISTS charparamb varchar(2000),
  ADD COLUMN IF NOT EXISTS charparamc varchar(2000),
  ADD COLUMN IF NOT EXISTS charparamd varchar(2000),
  ADD COLUMN IF NOT EXISTS charparame varchar(2000),
  ADD COLUMN IF NOT EXISTS charparamf varchar(2000),
  ADD COLUMN IF NOT EXISTS charparamg varchar(2000),
  ADD COLUMN IF NOT EXISTS charparamh varchar(2000),
  ADD COLUMN IF NOT EXISTS charparami varchar(2000),
  ADD COLUMN IF NOT EXISTS charparamj varchar(2000),
  ADD COLUMN IF NOT EXISTS dateparama timestamp,
  ADD COLUMN IF NOT EXISTS dateparamb timestamp,
  ADD COLUMN IF NOT EXISTS dateparamc timestamp,
  ADD COLUMN IF NOT EXISTS dateparamd timestamp,
  ADD COLUMN IF NOT EXISTS dateparame timestamp,
  ADD COLUMN IF NOT EXISTS dateparamf timestamp,
  ADD COLUMN IF NOT EXISTS dateparamg timestamp,
  ADD COLUMN IF NOT EXISTS dateparamh timestamp,
  ADD COLUMN IF NOT EXISTS numberparama numeric(38,6),
  ADD COLUMN IF NOT EXISTS numberparamb numeric(38,6),
  ADD COLUMN IF NOT EXISTS numberparamc numeric(38,6),
  ADD COLUMN IF NOT EXISTS numberparamd numeric(38,6),
  ADD COLUMN IF NOT EXISTS numberparame numeric(38,6),
  ADD COLUMN IF NOT EXISTS numberparamf numeric(38,6),
  ADD COLUMN IF NOT EXISTS objparama int8,
  ADD COLUMN IF NOT EXISTS objparamb int8,
  ADD COLUMN IF NOT EXISTS objparamc int8,
  ADD COLUMN IF NOT EXISTS objparamd int8,
  ADD COLUMN IF NOT EXISTS scparama varchar(2000),
  ADD COLUMN IF NOT EXISTS scparamb varchar(2000),
  ADD COLUMN IF NOT EXISTS scparamc varchar(2000),
  ADD COLUMN IF NOT EXISTS scparamd varchar(2000);

CREATE INDEX IF NOT EXISTS idx_qcs_inspect_coms_table_info_id ON public.qcs_inspect_coms (table_info_id);
CREATE INDEX IF NOT EXISTS idx_qcs_inspect_coms_inspect_id ON public.qcs_inspect_coms (inspect_id);
CREATE INDEX IF NOT EXISTS idx_qcs_inspect_coms_inspect_std_id ON public.qcs_inspect_coms (inspect_std_id);
CREATE INDEX IF NOT EXISTS idx_qcs_inspect_coms_std_ver_com_id ON public.qcs_inspect_coms (std_ver_com_id);
CREATE INDEX IF NOT EXISTS idx_qcs_inspect_coms_valid ON public.qcs_inspect_coms (valid);
CREATE INDEX IF NOT EXISTS idx_qcs_inspect_coms_cid ON public.qcs_inspect_coms (cid);

CREATE TABLE IF NOT EXISTS public.qcs_report_coms (id int8 PRIMARY KEY);
ALTER TABLE public.qcs_report_coms
  ADD COLUMN IF NOT EXISTS version int4,
  ADD COLUMN IF NOT EXISTS create_staff_id int8,
  ADD COLUMN IF NOT EXISTS create_time timestamp,
  ADD COLUMN IF NOT EXISTS modify_staff_id int8,
  ADD COLUMN IF NOT EXISTS modify_time timestamp,
  ADD COLUMN IF NOT EXISTS delete_staff_id int8,
  ADD COLUMN IF NOT EXISTS delete_time timestamp,
  ADD COLUMN IF NOT EXISTS valid boolean,
  ADD COLUMN IF NOT EXISTS cid int8,
  ADD COLUMN IF NOT EXISTS sort int4,
  ADD COLUMN IF NOT EXISTS table_info_id int8,
  ADD COLUMN IF NOT EXISTS disp_value varchar(2000),
  ADD COLUMN IF NOT EXISTS index_range varchar(2000),
  ADD COLUMN IF NOT EXISTS limit_type varchar(255),
  ADD COLUMN IF NOT EXISTS max_value varchar(2000),
  ADD COLUMN IF NOT EXISTS memo_field varchar(2000),
  ADD COLUMN IF NOT EXISTS min_value varchar(2000),
  ADD COLUMN IF NOT EXISTS name_eng varchar(2000),
  ADD COLUMN IF NOT EXISTS procedure_no varchar(2000),
  ADD COLUMN IF NOT EXISTS report_id int8,
  ADD COLUMN IF NOT EXISTS report_name varchar(2000),
  ADD COLUMN IF NOT EXISTS show_values_bak varchar(2000),
  ADD COLUMN IF NOT EXISTS std_ver_com int8,
  ADD COLUMN IF NOT EXISTS unit_name varchar(2000),
  ADD COLUMN IF NOT EXISTS value_kind varchar(255),
  ADD COLUMN IF NOT EXISTS value_src_id int8,
  ADD COLUMN IF NOT EXISTS check_result varchar(255),
  ADD COLUMN IF NOT EXISTS bigintparama int4,
  ADD COLUMN IF NOT EXISTS bigintparamb int4,
  ADD COLUMN IF NOT EXISTS bigintparamc int4,
  ADD COLUMN IF NOT EXISTS bigintparamd int4,
  ADD COLUMN IF NOT EXISTS bigintparame int4,
  ADD COLUMN IF NOT EXISTS bigintparamf int4,
  ADD COLUMN IF NOT EXISTS bigintparamg int4,
  ADD COLUMN IF NOT EXISTS bigintparamh int4,
  ADD COLUMN IF NOT EXISTS bigintparami int4,
  ADD COLUMN IF NOT EXISTS bigintparamj int4,
  ADD COLUMN IF NOT EXISTS charparama varchar(2000),
  ADD COLUMN IF NOT EXISTS charparamb varchar(2000),
  ADD COLUMN IF NOT EXISTS charparamc varchar(2000),
  ADD COLUMN IF NOT EXISTS charparamd varchar(2000),
  ADD COLUMN IF NOT EXISTS charparame varchar(2000),
  ADD COLUMN IF NOT EXISTS charparamf varchar(2000),
  ADD COLUMN IF NOT EXISTS charparamg varchar(2000),
  ADD COLUMN IF NOT EXISTS charparamh varchar(2000),
  ADD COLUMN IF NOT EXISTS charparami varchar(2000),
  ADD COLUMN IF NOT EXISTS charparamj varchar(2000),
  ADD COLUMN IF NOT EXISTS dateparama timestamp,
  ADD COLUMN IF NOT EXISTS dateparamb timestamp,
  ADD COLUMN IF NOT EXISTS dateparamc timestamp,
  ADD COLUMN IF NOT EXISTS dateparamd timestamp,
  ADD COLUMN IF NOT EXISTS dateparame timestamp,
  ADD COLUMN IF NOT EXISTS dateparamf timestamp,
  ADD COLUMN IF NOT EXISTS dateparamg timestamp,
  ADD COLUMN IF NOT EXISTS dateparamh timestamp,
  ADD COLUMN IF NOT EXISTS numberparama numeric(38,6),
  ADD COLUMN IF NOT EXISTS numberparamb numeric(38,6),
  ADD COLUMN IF NOT EXISTS numberparamc numeric(38,6),
  ADD COLUMN IF NOT EXISTS numberparamd numeric(38,6),
  ADD COLUMN IF NOT EXISTS numberparame numeric(38,6),
  ADD COLUMN IF NOT EXISTS numberparamf numeric(38,6),
  ADD COLUMN IF NOT EXISTS objparama int8,
  ADD COLUMN IF NOT EXISTS objparamb int8,
  ADD COLUMN IF NOT EXISTS objparamc int8,
  ADD COLUMN IF NOT EXISTS objparamd int8,
  ADD COLUMN IF NOT EXISTS scparama varchar(2000),
  ADD COLUMN IF NOT EXISTS scparamb varchar(2000),
  ADD COLUMN IF NOT EXISTS scparamc varchar(2000),
  ADD COLUMN IF NOT EXISTS scparamd varchar(2000);

CREATE INDEX IF NOT EXISTS idx_qcs_report_coms_table_info_id ON public.qcs_report_coms (table_info_id);
CREATE INDEX IF NOT EXISTS idx_qcs_report_coms_report_id ON public.qcs_report_coms (report_id);
CREATE INDEX IF NOT EXISTS idx_qcs_report_coms_report_name ON public.qcs_report_coms (report_name);
CREATE INDEX IF NOT EXISTS idx_qcs_report_coms_std_ver_com ON public.qcs_report_coms (std_ver_com);
CREATE INDEX IF NOT EXISTS idx_qcs_report_coms_valid ON public.qcs_report_coms (valid);
CREATE INDEX IF NOT EXISTS idx_qcs_report_coms_cid ON public.qcs_report_coms (cid);
