-- PostgreSQL compatibility table for WOM_1.0.0_procReport_ProCheckDetail.
-- Source evidence: WOM_6.1.3.4/service/src/main/resources/META-INF/init/init.xml.
-- The report-work dialog reads this table while loading an existing
-- WOM_PROC_REPORTS row. Missing structure produces a frontend network 500.

CREATE TABLE IF NOT EXISTS public.wom_pro_check_details (
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
                ('bigintparama', 'integer'),
                ('bigintparamb', 'integer'),
                ('bigintparamc', 'integer'),
                ('bigintparamd', 'integer'),
                ('bigintparame', 'integer'),
                ('charparama', 'text'),
                ('charparamb', 'text'),
                ('charparamc', 'text'),
                ('charparamd', 'text'),
                ('charparame', 'text'),
                ('check_items', 'text'),
                ('dateparama', 'timestamp without time zone'),
                ('dateparamb', 'timestamp without time zone'),
                ('dateparamc', 'timestamp without time zone'),
                ('dateparamd', 'timestamp without time zone'),
                ('head_id', 'bigint'),
                ('is_pass', 'boolean'),
                ('numberparama', 'numeric(38,6)'),
                ('numberparamb', 'numeric(38,6)'),
                ('numberparamc', 'numeric(38,6)'),
                ('objparama', 'bigint'),
                ('objparamb', 'bigint'),
                ('remark', 'text'),
                ('report_num', 'numeric(38,6)'),
                ('report_value', 'text'),
                ('rm_check_detail_id', 'bigint'),
                ('scparama', 'text'),
                ('scparamb', 'text'),
                ('standrad', 'text')
        ) AS columns(name, definition)
    LOOP
        EXECUTE format(
            'ALTER TABLE public.wom_pro_check_details ADD COLUMN IF NOT EXISTS %I %s',
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
        WHERE conrelid = 'public.wom_pro_check_details'::regclass
          AND contype = 'p'
    ) THEN
        ALTER TABLE public.wom_pro_check_details
            ADD CONSTRAINT wom_pro_check_details_pkey PRIMARY KEY (id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_wom_pro_check_details_head
    ON public.wom_pro_check_details(head_id);

CREATE INDEX IF NOT EXISTS idx_wom_pro_check_details_rm_check
    ON public.wom_pro_check_details(rm_check_detail_id);

CREATE INDEX IF NOT EXISTS idx_wom_pro_check_details_table_info
    ON public.wom_pro_check_details(table_info_id);
