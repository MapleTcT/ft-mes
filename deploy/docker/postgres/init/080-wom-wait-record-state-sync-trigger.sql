-- PostgreSQL compatibility guard for WOM work-order wait records.
-- WOMProduceTaskServiceImpl.updateTaskState("hold"/"restart") updates
-- WOM_WAIT_PUT_RECORDS with native SQL, then later merges the wait-record
-- entity again. On the recovered PostgreSQL runtime that stale merge can
-- restore the previous EXE_STATE even though the API returns success. Keep the
-- work-order wait row aligned with the task state when that row is written.

CREATE OR REPLACE FUNCTION public.adp_wom_sync_work_order_wait_state()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    current_task_state text;
BEGIN
    IF NEW.task_id IS NULL
       OR NEW.record_type IS DISTINCT FROM 'WOM_recordType/workOrder'
       OR COALESCE(NEW.valid, true) IS NOT TRUE THEN
        RETURN NEW;
    END IF;

    SELECT task_run_state
      INTO current_task_state
      FROM public.wom_produce_tasks
     WHERE id = NEW.task_id
       AND COALESCE(valid, true) IS TRUE;

    IF current_task_state IN (
        'WOM_runState/waitForRun',
        'WOM_runState/runing',
        'WOM_runState/iskeep',
        'WOM_runState/paused',
        'WOM_runState/finished',
        'WOM_runState/stoped'
    ) THEN
        NEW.exe_state := current_task_state;
    END IF;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_adp_wom_sync_work_order_wait_state
    ON public.wom_wait_put_records;

CREATE TRIGGER trg_adp_wom_sync_work_order_wait_state
BEFORE INSERT OR UPDATE OF exe_state, batch_sync_status, task_id, record_type, valid
ON public.wom_wait_put_records
FOR EACH ROW
EXECUTE FUNCTION public.adp_wom_sync_work_order_wait_state();

CREATE OR REPLACE FUNCTION public.adp_wom_push_task_state_to_wait_records()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.task_run_state IN (
        'WOM_runState/waitForRun',
        'WOM_runState/runing',
        'WOM_runState/iskeep',
        'WOM_runState/paused',
        'WOM_runState/finished',
        'WOM_runState/stoped'
    ) THEN
        UPDATE public.wom_wait_put_records
           SET exe_state = NEW.task_run_state
         WHERE task_id = NEW.id
           AND record_type = 'WOM_recordType/workOrder'
           AND COALESCE(valid, true) IS TRUE
           AND exe_state IS DISTINCT FROM NEW.task_run_state;
    END IF;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_adp_wom_push_task_state_to_wait_records
    ON public.wom_produce_tasks;

CREATE TRIGGER trg_adp_wom_push_task_state_to_wait_records
AFTER UPDATE OF task_run_state
ON public.wom_produce_tasks
FOR EACH ROW
WHEN (OLD.task_run_state IS DISTINCT FROM NEW.task_run_state)
EXECUTE FUNCTION public.adp_wom_push_task_state_to_wait_records();
