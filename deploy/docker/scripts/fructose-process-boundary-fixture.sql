\set ON_ERROR_STOP on

BEGIN;

-- This fixture intentionally reuses the dedicated BPI_MIN test order. It must
-- never be applied to a production database or to a non-marker business order.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
          FROM public.wom_produce_tasks
         WHERE id = 9007190231280101
           AND table_no = 'BPI_MIN_20260727_110711_TASK_TN'
    ) THEN
        RAISE EXCEPTION 'Dedicated BPI_MIN WOM task is missing; refusing to seed.';
    END IF;
END
$$;

UPDATE public.wom_produce_tasks
   SET task_run_state = 'WOM_runState/finished',
       act_start_time = timestamp '2026-07-27 16:00:20',
       act_end_time = timestamp '2026-07-27 16:01:32',
       modify_time = CURRENT_TIMESTAMP
 WHERE id = 9007190231280101
   AND table_no = 'BPI_MIN_20260727_110711_TASK_TN';

UPDATE public.wom_task_processes
   SET name = '喷射',
       table_no = 'BPI_MIN_20260727_110711_JET_PROCESS_TN',
       exe_order = 1,
       process_run_state = 'WOM_runState/finished',
       act_start_time = timestamp '2026-07-27 16:00:20',
       act_end_time = timestamp '2026-07-27 16:00:44',
       plan_end_time = timestamp '2026-07-27 16:00:44',
       modify_time = CURRENT_TIMESTAMP
 WHERE id = 9007190231281105
   AND task_id = 9007190231280101;

INSERT INTO public.wom_task_processes
SELECT (
    jsonb_populate_record(
        NULL::public.wom_task_processes,
        to_jsonb(template) || jsonb_build_object(
            'id', 9007190231281106,
            'table_no', 'BPI_MIN_20260727_110711_SACCH_PROCESS_TN',
            'name', '糖化',
            'exe_order', 2,
            'process_run_state', 'WOM_runState/finished',
            'act_start_time', '2026-07-27 16:00:56',
            'act_end_time', '2026-07-27 16:01:32',
            'plan_end_time', '2026-07-27 16:01:32',
            'create_time', CURRENT_TIMESTAMP,
            'modify_time', CURRENT_TIMESTAMP
        )
    )
).*
  FROM public.wom_task_processes template
 WHERE template.id = 9007190231281105
ON CONFLICT (id) DO UPDATE SET
    table_no = EXCLUDED.table_no,
    name = EXCLUDED.name,
    exe_order = EXCLUDED.exe_order,
    process_run_state = EXCLUDED.process_run_state,
    act_start_time = EXCLUDED.act_start_time,
    act_end_time = EXCLUDED.act_end_time,
    plan_end_time = EXCLUDED.plan_end_time,
    modify_time = EXCLUDED.modify_time,
    valid = true;

INSERT INTO public.wom_proc_reports
SELECT (
    jsonb_populate_record(
        NULL::public.wom_proc_reports,
        to_jsonb(template) || jsonb_build_object(
            'id', 9007190231283110,
            'table_info_id', 9007190231283110,
            'table_no', 'BPI_MIN_20260727_110711_SACCH_REPORT_TN',
            'task_process_id', 9007190231281106,
            'is_finish', true,
            'create_time', CURRENT_TIMESTAMP,
            'modify_time', CURRENT_TIMESTAMP
        )
    )
).*
  FROM public.wom_proc_reports template
 WHERE template.id = 9007190231282110
ON CONFLICT (id) DO UPDATE SET
    table_info_id = EXCLUDED.table_info_id,
    table_no = EXCLUDED.table_no,
    task_id = EXCLUDED.task_id,
    task_process_id = EXCLUDED.task_process_id,
    is_finish = EXCLUDED.is_finish,
    modify_time = EXCLUDED.modify_time,
    valid = true;

UPDATE public.wom_process_exelogs
   SET name = '喷射',
       table_no = NULL,
       table_info_id = 9007190231280101,
       task_process_id = 9007190231281105,
       equipment_id = 9007190231282114,
       head_id = 770314208122112,
       proc_report_id = 9007190231282110,
       exe_order = 1,
       process_run_state = 'WOM_runState/finished',
       act_start_time = timestamp '2026-07-27 16:00:20',
       act_end_time = timestamp '2026-07-27 16:00:44',
       long_time = 24,
       need_param_ana = false,
       analysis_flag = false,
       group_id = NULL,
       status = NULL,
       create_department_id = NULL,
       create_position_id = NULL,
       owner_department_id = NULL,
       owner_position_id = NULL,
       owner_staff_id = NULL,
       position_lay_rec = NULL,
       modify_staff_id = 1,
       modify_time = CURRENT_TIMESTAMP,
       valid = true
 WHERE id = 9007190231281109
   AND task_id = 9007190231280101;

UPDATE public.wom_process_exelogs
   SET name = '糖化',
       table_no = NULL,
       table_info_id = 9007190231280101,
       task_process_id = 9007190231281106,
       equipment_id = 9007190231282114,
       head_id = 770314208122112,
       proc_report_id = 9007190231283110,
       exe_order = 2,
       process_run_state = 'WOM_runState/finished',
       act_start_time = timestamp '2026-07-27 16:00:56',
       act_end_time = timestamp '2026-07-27 16:01:32',
       long_time = 36,
       need_param_ana = false,
       analysis_flag = false,
       group_id = NULL,
       status = NULL,
       create_department_id = NULL,
       create_position_id = NULL,
       owner_department_id = NULL,
       owner_position_id = NULL,
       owner_staff_id = NULL,
       position_lay_rec = NULL,
       modify_staff_id = 1,
       modify_time = CURRENT_TIMESTAMP,
       valid = true
 WHERE id = 9007190231282109
   AND task_id = 9007190231280101;

UPDATE public.wom_bpi_production_context_bindings
   SET wom_cid = 1000,
       wom_line_id = 9007190231280105,
       tenant_id = '1000',
       plant_id = 'PLANT-01',
       line_id = 'LINE-S07-01',
       enabled = true,
       updated_at = CURRENT_TIMESTAMP
 WHERE tenant_id = '1000'
   AND plant_id = 'PLANT-01'
   AND line_id = 'LINE-S07-01';

COMMIT;

SELECT jsonb_build_object(
    'taskId', task.id,
    'taskNo', task.table_no,
    'batchNo', task.produce_batch_num,
    'womLineId', task.line_id,
    'processExecutions', (
        SELECT jsonb_agg(
            jsonb_build_object(
                'id', execution.id,
                'name', execution.name,
                'order', execution.exe_order,
                'start', execution.act_start_time,
                'end', execution.act_end_time
            )
            ORDER BY execution.exe_order
        )
          FROM public.wom_process_exelogs execution
         WHERE execution.task_id = task.id
           AND execution.id IN (9007190231281109, 9007190231282109)
    ),
    'bpiContext', (
        SELECT jsonb_build_object(
            'tenantId', binding.tenant_id,
            'plantId', binding.plant_id,
            'lineId', binding.line_id
        )
          FROM public.wom_bpi_production_context_bindings binding
         WHERE binding.wom_cid = task.cid
           AND binding.wom_line_id = task.line_id
           AND binding.enabled
    )
)
  FROM public.wom_produce_tasks task
 WHERE task.id = 9007190231280101;
