-- PostgreSQL compatibility table for WOM_1.0.0_produceTask_Source.
--
-- QCS asks WOM for the deployed industry code while opening an inspection
-- report. The vendor initialization seeds wom_source on Oracle/MySQL/SQL
-- Server, but the recovered PostgreSQL path did not create the table.

CREATE TABLE IF NOT EXISTS public.wom_source (
    id bigint PRIMARY KEY
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
                ('charparama', 'varchar(2000)'),
                ('charparamb', 'varchar(2000)'),
                ('charparamc', 'varchar(2000)'),
                ('charparamd', 'varchar(2000)'),
                ('charparame', 'varchar(2000)'),
                ('code', 'varchar(2000)'),
                ('dateparama', 'timestamp without time zone'),
                ('dateparamb', 'timestamp without time zone'),
                ('dateparamc', 'timestamp without time zone'),
                ('dateparamd', 'timestamp without time zone'),
                ('name', 'varchar(2000)'),
                ('numberparama', 'numeric'),
                ('numberparamb', 'numeric'),
                ('numberparamc', 'numeric'),
                ('objparama', 'bigint'),
                ('objparamb', 'bigint'),
                ('scparama', 'varchar(2000)'),
                ('scparamb', 'varchar(2000)')
          ) AS columns(name, definition)
    LOOP
        EXECUTE format(
            'ALTER TABLE public.wom_source ADD COLUMN IF NOT EXISTS %I %s',
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
         WHERE conrelid = 'public.wom_source'::regclass
           AND contype = 'p'
    ) THEN
        ALTER TABLE public.wom_source
            ADD CONSTRAINT wom_source_pkey PRIMARY KEY (id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_wom_source_table_info
    ON public.wom_source(table_info_id);

CREATE INDEX IF NOT EXISTS idx_wom_source_code
    ON public.wom_source(code);

INSERT INTO public.wom_source (
    id,
    version,
    create_time,
    valid,
    cid,
    code,
    name
)
SELECT
    1000,
    0,
    CURRENT_TIMESTAMP,
    true,
    1000,
    'chemical industry',
    U&'\7CBE\7EC6\5316\5DE5'
WHERE NOT EXISTS (
    SELECT 1
      FROM public.wom_source
     WHERE id = 1000
        OR code = 'chemical industry'
);
