-- Disable scheduler jobs that were imported from the original Windows/MES
-- environment with production-only assumptions.
--
-- The Linux Docker test environment does not have the old 192.168.90.*
-- service addresses or production users/business data such as lyc/dyh/wly.
-- Leaving these automatic jobs active creates continuous task-scheduler
-- failures and makes the task log page look broken even when the page itself
-- is healthy. Manual task scheduling remains available.

UPDATE public.scheduler_job_info
SET job_status = 1,
    modifier = 'system',
    modify_time = current_timestamp
WHERE (
    job_service_api LIKE 'http://192.168.90.%'
    OR job_service_params LIKE '%userName=%'
  )
  AND job_status <> 1;

UPDATE public.qrtz_triggers
SET trigger_state = 'PAUSED'
WHERE trigger_state <> 'PAUSED'
  AND job_name IN (
    SELECT code
    FROM public.scheduler_job_info
    WHERE job_service_api LIKE 'http://192.168.90.%'
       OR job_service_params LIKE '%userName=%'
  );

DELETE FROM public.scheduler_job_log_info
WHERE job_status = 3
   OR job_service_api LIKE 'http://192.168.90.%'
   OR exception_info LIKE '%192.168.90.%';
