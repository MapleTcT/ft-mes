-- PostgreSQL compatibility for WOM produce task supervision extension rows.
--
-- WOMProduceTaskController.data/editStates calls WOMProduceTaskDao.findSupervision,
-- which maps WOMProduceTaskSupervision to WOM_PRODUCE_TASKS_SV. The recovered
-- PostgreSQL view originally exposed only TABLE_INFO_ID and STAFF. Hibernate
-- also selects inherited/id/mainObj columns, so expose those fields from the
-- base produce task table while preserving the existing first two view columns.

DO $$
BEGIN
  IF to_regclass('public.wom_produce_tasks') IS NOT NULL
     AND (to_regclass('public.wom_produce_tasks_sv') IS NULL OR EXISTS (
       SELECT 1
       FROM pg_class c
       JOIN pg_namespace n ON n.oid = c.relnamespace
       WHERE n.nspname = 'public'
         AND c.relname = 'wom_produce_tasks_sv'
         AND c.relkind = 'v'
     )) THEN
    EXECUTE $view$
CREATE OR REPLACE VIEW public.wom_produce_tasks_sv AS
SELECT
  table_info_id,
  owner_staff_id AS staff,
  id,
  version,
  true::boolean AS valid,
  id AS main_obj,
  create_staff_id,
  modify_staff_id,
  delete_staff_id,
  create_time,
  modify_time,
  delete_time
FROM public.wom_produce_tasks
WHERE table_info_id IS NOT NULL
  AND owner_staff_id IS NOT NULL
$view$;
  END IF;
END $$;
