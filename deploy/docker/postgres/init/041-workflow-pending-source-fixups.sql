-- The workbench Todo tab calls baseService pending statistics that classify
-- workflow tasks by PENDING_SOURCE. Older recovered workflow schemas do not
-- include this column, which makes PostgreSQL fail the count query.

ALTER TABLE public.wfm_task_pending
  ADD COLUMN IF NOT EXISTS pending_source varchar(32);

ALTER TABLE public.wfm_task_pending
  ALTER COLUMN pending_source SET DEFAULT 'system';

UPDATE public.wfm_task_pending
SET pending_source = 'system'
WHERE pending_source IS NULL;

CREATE INDEX IF NOT EXISTS idx_wfm_task_pending_user_source
  ON public.wfm_task_pending(user_id, pending_source);

COMMENT ON COLUMN public.wfm_task_pending.pending_source
  IS 'Pending source: system or other.';
