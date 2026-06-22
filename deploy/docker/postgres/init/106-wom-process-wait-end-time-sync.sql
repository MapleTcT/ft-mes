-- PostgreSQL compatibility guard for WOM process wait-record finish time.
--
-- WOMTaskProcessServiceImpl.endProcess first marks the process wait row as
-- finished by TASK_PROCESS_ID, but then updates ACTUAL_END_TIME by task_id =
-- processId. In the recovered PostgreSQL schema task_id stores the parent
-- produce-task id, so the process wait row keeps an empty actual_end_time even
-- though the API returns success. Keep only started process wait rows aligned
-- with the finished task-process/process-exelog timestamp.

CREATE OR REPLACE FUNCTION public.adp_wom_sync_process_wait_end_time()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    process_id bigint;
    end_time timestamp without time zone;
BEGIN
    process_id := NEW.id;
    end_time := NEW.act_end_time;

    IF TG_TABLE_NAME = 'wom_process_exelogs' THEN
        process_id := NEW.task_process_id;
    END IF;

    IF process_id IS NULL
       OR end_time IS NULL
       OR COALESCE(NEW.valid, true) IS NOT TRUE
       OR COALESCE(NEW.process_run_state, '') <> 'WOM_runState/finished' THEN
        RETURN NEW;
    END IF;

    UPDATE public.wom_wait_put_records
       SET actual_end_time = end_time,
           exe_state = 'WOM_runState/finished'
     WHERE task_process_id = process_id
       AND record_type = 'WOM_recordType/process'
       AND actual_start_time IS NOT NULL
       AND actual_end_time IS NULL
       AND COALESCE(valid, true) IS TRUE;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_adp_wom_sync_process_wait_end_time_from_process
    ON public.wom_task_processes;

CREATE TRIGGER trg_adp_wom_sync_process_wait_end_time_from_process
AFTER INSERT OR UPDATE OF process_run_state, act_end_time, valid
ON public.wom_task_processes
FOR EACH ROW
EXECUTE FUNCTION public.adp_wom_sync_process_wait_end_time();

DROP TRIGGER IF EXISTS trg_adp_wom_sync_process_wait_end_time_from_exelog
    ON public.wom_process_exelogs;

CREATE TRIGGER trg_adp_wom_sync_process_wait_end_time_from_exelog
AFTER INSERT OR UPDATE OF process_run_state, act_end_time, valid
ON public.wom_process_exelogs
FOR EACH ROW
EXECUTE FUNCTION public.adp_wom_sync_process_wait_end_time();
