-- PostgreSQL compatibility for legacy WOM process sequencing SQL.
--
-- WOMProduceTaskServiceImpl.insertProcessWaitPut emits:
--   convert(proc_sort,int)
-- on the PostgreSQL/default path. PostgreSQL parses the second argument as a
-- column reference, so this adds the ignored compatibility column on
-- WOM_TASK_PROCESSES. The convert(text, integer) shim is created by
-- 098-wom-active-execsort-convert-compat.sql and is re-created here for
-- standalone/idempotent application.

ALTER TABLE IF EXISTS public.wom_task_processes
  ADD COLUMN IF NOT EXISTS int integer;

CREATE OR REPLACE FUNCTION public.convert(value text, ignored integer)
RETURNS integer
LANGUAGE sql
IMMUTABLE
AS $$
  SELECT NULLIF(regexp_replace(value, '[^0-9-]', '', 'g'), '')::integer;
$$;
