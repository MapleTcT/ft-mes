CREATE OR REPLACE FUNCTION public.adp_wom_output_finish_num(p_task_id bigint)
RETURNS numeric
LANGUAGE sql
AS $$
  SELECT COALESCE(SUM(od.output_num), 0)
  FROM public.wom_output_details od
  JOIN public.wom_proc_reports pr ON pr.id = od.head_id
  WHERE pr.task_id = p_task_id
    AND pr.proc_report_type = 'WOM_procReportType/produceTask'
    AND COALESCE(pr.valid, true) IS TRUE
    AND COALESCE(od.valid, true) IS TRUE
$$;

CREATE OR REPLACE FUNCTION public.adp_wom_before_task_finish_num()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
  v_finish_num numeric;
BEGIN
  IF NEW.task_run_state = 'WOM_runState/finished' THEN
    v_finish_num := public.adp_wom_output_finish_num(NEW.id);
    IF v_finish_num > 0 THEN
      NEW.finish_num := v_finish_num;
    END IF;
  END IF;
  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_adp_wom_before_task_finish_num ON public.wom_produce_tasks;
CREATE TRIGGER trg_adp_wom_before_task_finish_num
BEFORE INSERT OR UPDATE OF task_run_state, finish_num ON public.wom_produce_tasks
FOR EACH ROW
EXECUTE FUNCTION public.adp_wom_before_task_finish_num();

CREATE OR REPLACE FUNCTION public.adp_wom_sync_task_finish_num_from_output()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
  v_head_id bigint;
  v_task_id bigint;
  v_finish_num numeric;
BEGIN
  v_head_id := COALESCE(NEW.head_id, OLD.head_id);

  SELECT pr.task_id
  INTO v_task_id
  FROM public.wom_proc_reports pr
  WHERE pr.id = v_head_id
    AND pr.proc_report_type = 'WOM_procReportType/produceTask'
    AND COALESCE(pr.valid, true) IS TRUE;

  IF v_task_id IS NULL THEN
    RETURN COALESCE(NEW, OLD);
  END IF;

  SELECT public.adp_wom_output_finish_num(v_task_id) INTO v_finish_num;

  UPDATE public.wom_produce_tasks task
  SET finish_num = v_finish_num
  WHERE task.id = v_task_id
    AND task.task_run_state = 'WOM_runState/finished'
    AND COALESCE(task.finish_num, 0) IS DISTINCT FROM v_finish_num
    AND v_finish_num > 0;

  RETURN COALESCE(NEW, OLD);
END;
$$;

DROP TRIGGER IF EXISTS trg_adp_wom_sync_task_finish_num_from_output ON public.wom_output_details;
CREATE TRIGGER trg_adp_wom_sync_task_finish_num_from_output
AFTER INSERT OR UPDATE OF output_num, valid, head_id OR DELETE ON public.wom_output_details
FOR EACH ROW
EXECUTE FUNCTION public.adp_wom_sync_task_finish_num_from_output();
