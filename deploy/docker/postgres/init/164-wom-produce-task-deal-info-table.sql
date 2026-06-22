-- PostgreSQL compatibility table for WOM_1.0.0_produceTask_ProduceTask DI.
--
-- Source evidence:
-- - WOMProduceTaskDealInfo maps to WOM_PRODUCE_TASKS_DI and extends
--   AbstractDealInfoEntity.
-- - WOMProduceTaskServiceImpl.save starts makeTaskFlow and then copies the
--   workflow DealInfo into WOMProduceTaskDealInfo. Missing this table rolls
--   back produceTaskCreated2 after the workflow starts.

CREATE TABLE IF NOT EXISTS public.wom_produce_tasks_di (
    id bigint PRIMARY KEY,
    version integer,
    create_staff_id bigint,
    create_time timestamp without time zone,
    modify_staff_id bigint,
    modify_time timestamp without time zone,
    delete_staff_id bigint,
    delete_time timestamp without time zone,
    valid boolean,
    cid bigint,
    sort integer,
    main_obj bigint,
    staff bigint,
    recalled_flag boolean,
    user_agent varchar(2000),
    table_info_id bigint,
    activity_name varchar(510),
    assign_staff varchar(4000),
    assign_staff_id varchar(2000),
    comments varchar(4000),
    dealinfo_type varchar(255),
    entity_code varchar(510),
    instance_id varchar(510),
    outcome varchar(510),
    outcome_des varchar(2000),
    outcome_des_zh_cn varchar(2000),
    pending_create_time timestamp without time zone,
    process_key varchar(510),
    process_version integer,
    proxy_staff varchar(2000),
    proxy_staff_ids varchar(2000),
    signature varchar(400),
    task_description varchar(2000),
    task_description_zh_cn varchar(2000),
    user_id bigint
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
                ('version', 'integer'),
                ('create_staff_id', 'bigint'),
                ('create_time', 'timestamp without time zone'),
                ('modify_staff_id', 'bigint'),
                ('modify_time', 'timestamp without time zone'),
                ('delete_staff_id', 'bigint'),
                ('delete_time', 'timestamp without time zone'),
                ('valid', 'boolean'),
                ('cid', 'bigint'),
                ('sort', 'integer'),
                ('main_obj', 'bigint'),
                ('staff', 'bigint'),
                ('recalled_flag', 'boolean'),
                ('user_agent', 'varchar(2000)'),
                ('table_info_id', 'bigint'),
                ('activity_name', 'varchar(510)'),
                ('assign_staff', 'varchar(4000)'),
                ('assign_staff_id', 'varchar(2000)'),
                ('comments', 'varchar(4000)'),
                ('dealinfo_type', 'varchar(255)'),
                ('entity_code', 'varchar(510)'),
                ('instance_id', 'varchar(510)'),
                ('outcome', 'varchar(510)'),
                ('outcome_des', 'varchar(2000)'),
                ('outcome_des_zh_cn', 'varchar(2000)'),
                ('pending_create_time', 'timestamp without time zone'),
                ('process_key', 'varchar(510)'),
                ('process_version', 'integer'),
                ('proxy_staff', 'varchar(2000)'),
                ('proxy_staff_ids', 'varchar(2000)'),
                ('signature', 'varchar(400)'),
                ('task_description', 'varchar(2000)'),
                ('task_description_zh_cn', 'varchar(2000)'),
                ('user_id', 'bigint')
          ) AS columns(name, definition)
    LOOP
        EXECUTE format(
            'ALTER TABLE public.wom_produce_tasks_di ADD COLUMN IF NOT EXISTS %I %s',
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
         WHERE conrelid = 'public.wom_produce_tasks_di'::regclass
           AND contype = 'p'
    ) THEN
        ALTER TABLE public.wom_produce_tasks_di
            ADD CONSTRAINT wom_produce_tasks_di_pkey PRIMARY KEY (id);
    END IF;
END $$;

ALTER TABLE public.wom_produce_tasks_di
    ALTER COLUMN assign_staff TYPE varchar(4000),
    ALTER COLUMN comments TYPE varchar(4000),
    ALTER COLUMN signature TYPE varchar(400);

CREATE INDEX IF NOT EXISTS idx_wom_produce_tasks_di_table_info
    ON public.wom_produce_tasks_di(table_info_id);

CREATE INDEX IF NOT EXISTS idx_wom_produce_tasks_di_main_obj
    ON public.wom_produce_tasks_di(main_obj);

CREATE INDEX IF NOT EXISTS idx_wom_produce_tasks_di_process_key
    ON public.wom_produce_tasks_di(process_key);

CREATE INDEX IF NOT EXISTS idx_wom_produce_tasks_di_staff
    ON public.wom_produce_tasks_di(staff);
