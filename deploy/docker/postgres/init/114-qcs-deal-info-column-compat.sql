-- Complete QCS workflow deal-info tables for PostgreSQL.
--
-- Source evidence:
-- - QCSInspectDealInfo extends AbstractDealInfoEntity and Hibernate inserts
--   columns such as activity_name, instance_id, outcome_des, process_key, and
--   user_id into QCS_INSPECTS_DI.
-- - The initial recovered compatibility table only contained the QCS local
--   fields (main_obj/staff/recalled_flag/user_agent/table_info_id), so WOM ->
--   QCS manufacturing inspection failed at qcs_inspects_di persistence.

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
    EXECUTE format('ALTER TABLE public.%I ADD COLUMN IF NOT EXISTS activity_name varchar(510)', table_name);
    EXECUTE format('ALTER TABLE public.%I ADD COLUMN IF NOT EXISTS assign_staff varchar(2000)', table_name);
    EXECUTE format('ALTER TABLE public.%I ADD COLUMN IF NOT EXISTS assign_staff_id varchar(2000)', table_name);
    EXECUTE format('ALTER TABLE public.%I ADD COLUMN IF NOT EXISTS comments varchar(2000)', table_name);
    EXECUTE format('ALTER TABLE public.%I ADD COLUMN IF NOT EXISTS dealinfo_type varchar(255)', table_name);
    EXECUTE format('ALTER TABLE public.%I ADD COLUMN IF NOT EXISTS entity_code varchar(510)', table_name);
    EXECUTE format('ALTER TABLE public.%I ADD COLUMN IF NOT EXISTS instance_id varchar(510)', table_name);
    EXECUTE format('ALTER TABLE public.%I ADD COLUMN IF NOT EXISTS outcome varchar(510)', table_name);
    EXECUTE format('ALTER TABLE public.%I ADD COLUMN IF NOT EXISTS outcome_des varchar(2000)', table_name);
    EXECUTE format('ALTER TABLE public.%I ADD COLUMN IF NOT EXISTS outcome_des_zh_cn varchar(2000)', table_name);
    EXECUTE format('ALTER TABLE public.%I ADD COLUMN IF NOT EXISTS pending_create_time timestamp', table_name);
    EXECUTE format('ALTER TABLE public.%I ADD COLUMN IF NOT EXISTS process_key varchar(510)', table_name);
    EXECUTE format('ALTER TABLE public.%I ADD COLUMN IF NOT EXISTS process_version int4', table_name);
    EXECUTE format('ALTER TABLE public.%I ADD COLUMN IF NOT EXISTS proxy_staff varchar(2000)', table_name);
    EXECUTE format('ALTER TABLE public.%I ADD COLUMN IF NOT EXISTS proxy_staff_ids varchar(2000)', table_name);
    EXECUTE format('ALTER TABLE public.%I ADD COLUMN IF NOT EXISTS signature varchar(2000)', table_name);
    EXECUTE format('ALTER TABLE public.%I ADD COLUMN IF NOT EXISTS task_description varchar(2000)', table_name);
    EXECUTE format('ALTER TABLE public.%I ADD COLUMN IF NOT EXISTS task_description_zh_cn varchar(2000)', table_name);
    EXECUTE format('ALTER TABLE public.%I ADD COLUMN IF NOT EXISTS user_id int8', table_name);

    EXECUTE format('CREATE INDEX IF NOT EXISTS idx_%s_activity_name ON public.%I (activity_name)', table_name, table_name);
    EXECUTE format('CREATE INDEX IF NOT EXISTS idx_%s_process_key ON public.%I (process_key)', table_name, table_name);
    EXECUTE format('CREATE INDEX IF NOT EXISTS idx_%s_user_id ON public.%I (user_id)', table_name, table_name);
  END LOOP;
END $$;
