-- PostgreSQL compatibility table for WOM_1.0.0_prepareMaterialNeed_PrePareNeedRef.
-- Source evidence: WOM_6.1.3.4/service/src/main/resources/META-INF/init/init.xml.
-- WOMProduceTaskServiceImpl.generatePrepareNeed persists this child table when
-- a manufacturing task is pushed to material-preparation demand.

CREATE TABLE IF NOT EXISTS public.wom_pre_pare_need_refs (
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
                ('charparama', 'character varying(2000)'),
                ('charparamb', 'character varying(2000)'),
                ('charparamc', 'character varying(2000)'),
                ('charparamd', 'character varying(2000)'),
                ('charparame', 'character varying(2000)'),
                ('dateparama', 'timestamp without time zone'),
                ('dateparamb', 'timestamp without time zone'),
                ('dateparamc', 'timestamp without time zone'),
                ('dateparamd', 'timestamp without time zone'),
                ('head_id', 'bigint'),
                ('numberparama', 'numeric(38,6)'),
                ('numberparamb', 'numeric(38,6)'),
                ('numberparamc', 'numeric(38,6)'),
                ('objparama', 'bigint'),
                ('objparamb', 'bigint'),
                ('origins', 'text'),
                ('plan_num', 'numeric(38,6)'),
                ('produce_batch_num', 'text'),
                ('product_id', 'bigint'),
                ('remark', 'text'),
                ('scparama', 'character varying(2000)'),
                ('scparamb', 'character varying(2000)')
        ) AS columns(name, definition)
    LOOP
        EXECUTE format(
            'ALTER TABLE public.wom_pre_pare_need_refs ADD COLUMN IF NOT EXISTS %I %s',
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
        WHERE conrelid = 'public.wom_pre_pare_need_refs'::regclass
          AND contype = 'p'
    ) THEN
        ALTER TABLE public.wom_pre_pare_need_refs
            ADD CONSTRAINT wom_pre_pare_need_refs_pkey PRIMARY KEY (id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_wom_pre_pare_need_refs_table_info
    ON public.wom_pre_pare_need_refs(table_info_id);

CREATE INDEX IF NOT EXISTS idx_wom_pre_pare_need_refs_head
    ON public.wom_pre_pare_need_refs(head_id);

CREATE INDEX IF NOT EXISTS idx_wom_pre_pare_need_refs_origins
    ON public.wom_pre_pare_need_refs(origins);

CREATE INDEX IF NOT EXISTS idx_wom_pre_pare_need_refs_product
    ON public.wom_pre_pare_need_refs(product_id);
