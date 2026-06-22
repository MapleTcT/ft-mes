-- PostgreSQL compatibility table for RM_6.1.2.3 ProcessActive.
-- Source evidence: modules/rm/RM_6.1.2.3/service/src/main/resources/META-INF/init/init.xml.
-- WOM process start calls WOMProduceTaskServiceImpl.insertActiveWaitPut, which
-- filters task actives through RM_PROCESS_ACTIVES. This creates structure only;
-- marker and business rows must still be produced by the real front-end flow.

CREATE TABLE IF NOT EXISTS public.rm_process_actives (
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
                ('active_hidden_id', 'bigint'),
                ('active_type', 'character varying'),
                ('apply_check_dep_id', 'bigint'),
                ('apply_check_staff_id', 'bigint'),
                ('auto_start', 'boolean'),
                ('batch_phase_id', 'text'),
                ('batch_phase_name', 'text'),
                ('batch_site', 'character varying'),
                ('bigintparama', 'integer'),
                ('bigintparamb', 'integer'),
                ('bigintparamc', 'integer'),
                ('bigintparamd', 'integer'),
                ('bigintparame', 'integer'),
                ('cal_formula', 'text'),
                ('charparama', 'text'),
                ('charparamb', 'text'),
                ('charparamc', 'text'),
                ('charparamd', 'text'),
                ('charparame', 'text'),
                ('chcek_tip', 'text'),
                ('check_dep_id', 'bigint'),
                ('check_staff_id', 'bigint'),
                ('container', 'text'),
                ('copy_source_id', 'bigint'),
                ('dateparama', 'timestamp without time zone'),
                ('dateparamb', 'timestamp without time zone'),
                ('dateparamc', 'timestamp without time zone'),
                ('dateparamd', 'timestamp without time zone'),
                ('deal_type', 'character varying'),
                ('deley_time', 'integer'),
                ('dispatch_system', 'character varying'),
                ('exec_sort', 'text'),
                ('exe_system', 'character varying'),
                ('final_inspection', 'boolean'),
                ('fixed_quantity', 'numeric(19, 6)'),
                ('formula_hidden_id', 'bigint'),
                ('formula_id', 'bigint'),
                ('from_container', 'boolean'),
                ('hidden_sort', 'numeric(19, 6)'),
                ('ingredients_order', 'integer'),
                ('inspect_system', 'bigint'),
                ('is_agile', 'boolean'),
                ('is_analy', 'boolean'),
                ('is_auto', 'boolean'),
                ('is_back_mix', 'boolean'),
                ('is_copy', 'boolean'),
                ('is_fixed_quantity', 'boolean'),
                ('is_for_adjust', 'boolean'),
                ('is_for_temp', 'boolean'),
                ('is_more_other', 'boolean'),
                ('is_on_site_check', 'boolean'),
                ('is_pass_check', 'boolean'),
                ('is_release', 'boolean'),
                ('is_replace', 'boolean'),
                ('long_time', 'numeric(19, 6)'),
                ('loss_rate', 'numeric(19, 6)'),
                ('main_active', 'boolean'),
                ('material_id', 'bigint'),
                ('max_quantity', 'numeric(19, 6)'),
                ('min_quantity', 'numeric(19, 6)'),
                ('mixture_active_id', 'bigint'),
                ('mixture_id', 'bigint'),
                ('mobile_terminal', 'boolean'),
                ('name', 'text'),
                ('need_param_ana', 'boolean'),
                ('need_prepare', 'boolean'),
                ('node_id', 'text'),
                ('numberparama', 'numeric(19, 6)'),
                ('numberparamb', 'numeric(19, 6)'),
                ('numberparamc', 'numeric(19, 6)'),
                ('obj_formula_id', 'bigint'),
                ('objparama', 'bigint'),
                ('objparamb', 'bigint'),
                ('obj_process_id', 'bigint'),
                ('occur_turn', 'character varying'),
                ('process_id', 'bigint'),
                ('putin_order', 'text'),
                ('putin_type', 'character varying'),
                ('quality_std_id', 'bigint'),
                ('quantity', 'numeric(19, 6)'),
                ('reject_system', 'bigint'),
                ('release_conditions', 'text'),
                ('remark', 'text'),
                ('report_system', 'character varying'),
                ('scparama', 'text'),
                ('scparamb', 'text'),
                ('statistics_time', 'character varying'),
                ('sub_active_num', 'integer'),
                ('switch_response_item', 'text'),
                ('switch_set_item', 'text'),
                ('use_item', 'text')
        ) AS columns(name, definition)
    LOOP
        EXECUTE format(
            'ALTER TABLE public.rm_process_actives ADD COLUMN IF NOT EXISTS %I %s',
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
        WHERE conrelid = 'public.rm_process_actives'::regclass
          AND contype = 'p'
    ) THEN
        ALTER TABLE public.rm_process_actives
            ADD CONSTRAINT rm_process_actives_pkey PRIMARY KEY (id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_rm_process_actives_table_info
    ON public.rm_process_actives(table_info_id);

CREATE INDEX IF NOT EXISTS idx_rm_process_actives_formula
    ON public.rm_process_actives(formula_id, obj_formula_id);

CREATE INDEX IF NOT EXISTS idx_rm_process_actives_process
    ON public.rm_process_actives(process_id, obj_process_id);

CREATE INDEX IF NOT EXISTS idx_rm_process_actives_temp_adjust
    ON public.rm_process_actives(is_for_temp, is_for_adjust, valid);

CREATE INDEX IF NOT EXISTS idx_rm_process_actives_active_type
    ON public.rm_process_actives(active_type, valid);
