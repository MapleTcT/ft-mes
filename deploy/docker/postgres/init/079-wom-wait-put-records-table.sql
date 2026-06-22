-- PostgreSQL compatibility table for WOM_1.0.0_waitPutinRecord_WaitPutRecord.
-- Source evidence: WOM_6.1.3.4/service/src/main/resources/META-INF/init/init.xml.
-- The table is required by WOMProduceTaskServiceImpl.updateTaskState and related
-- production execution flows. This script creates structure only; it does not
-- insert marker or business data.

CREATE TABLE IF NOT EXISTS public.wom_wait_put_records (
    id bigint
);

DO $$
DECLARE
    column_def record;
BEGIN
    FOR column_def IN
        SELECT *
        FROM (
            VALUES
                ('id', 'bigint'),
                ('version', 'integer DEFAULT 0'),
                ('create_staff_id', 'bigint'),
                ('create_time', 'timestamp without time zone DEFAULT CURRENT_TIMESTAMP'),
                ('modify_staff_id', 'bigint'),
                ('modify_time', 'timestamp without time zone'),
                ('delete_staff_id', 'bigint'),
                ('delete_time', 'timestamp without time zone'),
                ('valid', 'boolean DEFAULT true'),
                ('cid', 'bigint'),
                ('sort', 'integer'),
                ('create_department_id', 'bigint'),
                ('create_position_id', 'bigint'),
                ('deployment_id', 'bigint'),
                ('effect_staff_id', 'bigint'),
                ('effect_time', 'timestamp without time zone'),
                ('effective_state', 'integer'),
                ('group_id', 'bigint'),
                ('owner_department_id', 'bigint'),
                ('owner_position_id', 'bigint'),
                ('owner_staff_id', 'bigint'),
                ('position_lay_rec', 'character varying'),
                ('process_key', 'character varying'),
                ('process_version', 'integer'),
                ('status', 'integer'),
                ('table_no', 'character varying'),
                ('table_info_id', 'bigint'),
                ('extra_col', 'text'),
                ('parent_id', 'bigint'),
                ('lay_rec', 'character varying'),
                ('lay_no', 'integer'),
                ('leaf', 'integer'),
                ('full_path_name', 'character varying'),
                ('oa', 'boolean'),
                ('acti_exelog', 'bigint'),
                ('active_batch_state', 'bigint'),
                ('active_name', 'character varying'),
                ('active_type', 'character varying'),
                ('act_staff_id', 'bigint'),
                ('act_staff_name', 'character varying'),
                ('actual_end_time', 'timestamp without time zone'),
                ('actual_num', 'numeric'),
                ('actual_start_time', 'timestamp without time zone'),
                ('advance_charge', 'boolean'),
                ('batch_dealed_ids', 'character varying'),
                ('batch_mq_ids', 'character varying'),
                ('batch_sync_status', 'character varying'),
                ('bigintparama', 'integer'),
                ('bigintparamb', 'integer'),
                ('bigintparamc', 'integer'),
                ('bigintparamd', 'integer'),
                ('bigintparame', 'integer'),
                ('charparama', 'character varying'),
                ('charparamb', 'character varying'),
                ('charparamc', 'character varying'),
                ('charparamd', 'character varying'),
                ('charparame', 'character varying'),
                ('check_result', 'character varying'),
                ('check_state', 'character varying'),
                ('check_times', 'integer'),
                ('dateparama', 'timestamp without time zone'),
                ('dateparamb', 'timestamp without time zone'),
                ('dateparamc', 'timestamp without time zone'),
                ('dateparamd', 'timestamp without time zone'),
                ('equ_code', 'character varying'),
                ('equ_name', 'character varying'),
                ('error_info', 'character varying'),
                ('euq_id', 'bigint'),
                ('exe_state', 'character varying'),
                ('exe_system', 'character varying'),
                ('final_inspection', 'boolean'),
                ('formula_code', 'character varying'),
                ('formula_id', 'bigint'),
                ('is_alige', 'boolean'),
                ('is_auto', 'boolean'),
                ('is_for_adjust', 'boolean'),
                ('is_for_temp', 'boolean'),
                ('is_pass_check', 'boolean'),
                ('line_code', 'character varying'),
                ('line_id', 'bigint'),
                ('line_name', 'character varying'),
                ('material_code', 'character varying'),
                ('material_id', 'bigint'),
                ('material_name', 'character varying'),
                ('material_unit', 'character varying'),
                ('notice_time', 'timestamp without time zone'),
                ('numberparama', 'numeric'),
                ('numberparamb', 'numeric'),
                ('numberparamc', 'numeric'),
                ('objparama', 'bigint'),
                ('objparamb', 'bigint'),
                ('plan_end_time', 'timestamp without time zone'),
                ('plan_num', 'numeric'),
                ('plan_start_time', 'timestamp without time zone'),
                ('process_name', 'character varying'),
                ('proc_report_id', 'bigint'),
                ('produce_batch_num', 'character varying'),
                ('product_code', 'character varying'),
                ('product_id', 'bigint'),
                ('product_name', 'character varying'),
                ('product_unit', 'character varying'),
                ('quality_std_id', 'bigint'),
                ('record_type', 'character varying'),
                ('remark', 'text'),
                ('rm_check_detail_id', 'bigint'),
                ('scan_material', 'boolean'),
                ('scparama', 'character varying'),
                ('scparamb', 'character varying'),
                ('sync_log_id', 'bigint'),
                ('task_active_id', 'bigint'),
                ('task_id', 'bigint'),
                ('task_process_id', 'bigint')
        ) AS columns(name, definition)
    LOOP
        EXECUTE format(
            'ALTER TABLE public.wom_wait_put_records ADD COLUMN IF NOT EXISTS %I %s',
            column_def.name,
            column_def.definition
        );
    END LOOP;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conrelid = 'public.wom_wait_put_records'::regclass
          AND contype = 'p'
    ) THEN
        ALTER TABLE public.wom_wait_put_records
            ADD CONSTRAINT wom_wait_put_records_pkey PRIMARY KEY (id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_wom_wait_put_records_task
    ON public.wom_wait_put_records(task_id);

CREATE INDEX IF NOT EXISTS idx_wom_wait_put_records_type_state
    ON public.wom_wait_put_records(record_type, exe_state, valid);

CREATE INDEX IF NOT EXISTS idx_wom_wait_put_records_process
    ON public.wom_wait_put_records(task_process_id);

CREATE INDEX IF NOT EXISTS idx_wom_wait_put_records_active
    ON public.wom_wait_put_records(task_active_id);

CREATE INDEX IF NOT EXISTS idx_wom_wait_put_records_proc_report
    ON public.wom_wait_put_records(proc_report_id);
