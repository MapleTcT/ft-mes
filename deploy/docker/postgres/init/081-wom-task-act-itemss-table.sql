-- PostgreSQL compatibility table for WOM_1.0.0_produceTask_TaskActItems.
-- Source evidence: WOM_6.1.3.4/service/src/main/resources/META-INF/init/init.xml.
-- WOMProduceTaskServiceImpl.addOutputByOutPutDetails updates this table when a
-- work order is finished. The table is optional for marker rows, but the native
-- UPDATE must be safe on PostgreSQL.

CREATE TABLE IF NOT EXISTS public.wom_task_act_itemss (
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
                ('can_be_used', 'boolean DEFAULT false'),
                ('obj_active_id', 'bigint'),
                ('obj_equipment_id', 'bigint'),
                ('obj_fac_item_id', 'bigint'),
                ('obj_process_id', 'bigint'),
                ('param_label', 'text'),
                ('produce_batch_num', 'character varying')
        ) AS columns(name, definition)
    LOOP
        EXECUTE format(
            'ALTER TABLE public.wom_task_act_itemss ADD COLUMN IF NOT EXISTS %I %s',
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
        WHERE conrelid = 'public.wom_task_act_itemss'::regclass
          AND contype = 'p'
    ) THEN
        ALTER TABLE public.wom_task_act_itemss
            ADD CONSTRAINT wom_task_act_itemss_pkey PRIMARY KEY (id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_wom_task_act_itemss_batch
    ON public.wom_task_act_itemss(produce_batch_num);

CREATE INDEX IF NOT EXISTS idx_wom_task_act_itemss_process
    ON public.wom_task_act_itemss(obj_process_id);

CREATE INDEX IF NOT EXISTS idx_wom_task_act_itemss_active
    ON public.wom_task_act_itemss(obj_active_id);

CREATE INDEX IF NOT EXISTS idx_wom_task_act_itemss_table_info
    ON public.wom_task_act_itemss(table_info_id);
