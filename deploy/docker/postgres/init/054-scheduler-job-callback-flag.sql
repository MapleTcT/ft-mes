-- Newer task-scheduler builds persist callback_flag when module scheduler
-- jobs are registered. The original PostgreSQL compatibility schema missed
-- this column, so module imports succeeded but scheduler job registration
-- failed with "column callback_flag does not exist".
ALTER TABLE IF EXISTS public.scheduler_job_info
  ADD COLUMN IF NOT EXISTS callback_flag boolean DEFAULT false;

UPDATE public.scheduler_job_info
SET callback_flag = false
WHERE callback_flag IS NULL;
