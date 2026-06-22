-- PostgreSQL compatibility for legacy WOM activity sequencing SQL.
--
-- WOMProduceTaskServiceImpl.insertActiveWaitPut emits:
--   convert(EXEC_SORT,int)
-- on the PostgreSQL/default path. PostgreSQL parses the second argument as a
-- column reference, so the compatibility shim below provides that ignored
-- column on WOM_TASK_ACTIVES and a matching convert(text, integer) function.
-- The function only emulates the integer cast use case required by EXEC_SORT.

ALTER TABLE IF EXISTS public.wom_task_actives
  ADD COLUMN IF NOT EXISTS int integer;

CREATE OR REPLACE FUNCTION public.convert(value text, ignored integer)
RETURNS integer
LANGUAGE sql
IMMUTABLE
AS $$
  SELECT NULLIF(regexp_replace(value, '[^0-9-]', '', 'g'), '')::integer;
$$;
