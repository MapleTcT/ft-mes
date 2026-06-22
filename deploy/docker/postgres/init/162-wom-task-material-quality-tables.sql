-- PostgreSQL compatibility tables for manufacturing task detail generation.
--
-- Source evidence:
-- - WOMProduceTaskServiceImpl.clearProductTask deletes WOM_TASK_MATERIALS and
--   WOM_TASK_QUALITIES during produceTaskCreated2/save.
-- - WOMTaskMaterial and WOMTaskQuality are recovered generated entities under
--   WOM_6.1.3.4/core/src/main/java/com/supcon/orchid/WOM/entities.
--
-- This script creates structure only. It does not seed business data.

CREATE TABLE IF NOT EXISTS public.wom_task_materials (
    id bigint
);

CREATE TABLE IF NOT EXISTS public.wom_task_qualities (
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
                ('main_obj', 'bigint'),
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
                ('dateparama', 'timestamp without time zone'),
                ('dateparamb', 'timestamp without time zone'),
                ('dateparamc', 'timestamp without time zone'),
                ('dateparamd', 'timestamp without time zone'),
                ('loss_rate', 'numeric(19, 6)'),
                ('material_id', 'bigint'),
                ('max_quality', 'numeric(19, 6)'),
                ('min_quality', 'numeric(19, 6)'),
                ('need_prepare', 'boolean DEFAULT false'),
                ('need_weigh', 'boolean DEFAULT false'),
                ('numberparama', 'numeric(19, 6)'),
                ('numberparamb', 'numeric(19, 6)'),
                ('numberparamc', 'numeric(19, 6)'),
                ('objparama', 'bigint'),
                ('objparamb', 'bigint'),
                ('plan_quality', 'numeric(19, 6)'),
                ('property', 'character varying'),
                ('remark', 'text'),
                ('scparama', 'text'),
                ('scparamb', 'text'),
                ('standard_quality', 'numeric(19, 6)'),
                ('sum_num', 'numeric(19, 6)'),
                ('task_id', 'bigint')
          ) AS columns(name, definition)
    LOOP
        EXECUTE format(
            'ALTER TABLE public.wom_task_materials ADD COLUMN IF NOT EXISTS %I %s',
            column_def.name,
            column_def.definition
        );
    END LOOP;
END $$;

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
                ('main_obj', 'bigint'),
                ('apply_check_dep_id', 'bigint'),
                ('apply_check_staff_id', 'bigint'),
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
                ('check_dep_id', 'bigint'),
                ('check_staff_id', 'bigint'),
                ('dateparama', 'timestamp without time zone'),
                ('dateparamb', 'timestamp without time zone'),
                ('dateparamc', 'timestamp without time zone'),
                ('dateparamd', 'timestamp without time zone'),
                ('final_inspection', 'boolean DEFAULT false'),
                ('formula_id', 'bigint'),
                ('material_id', 'bigint'),
                ('numberparama', 'numeric(19, 6)'),
                ('numberparamb', 'numeric(19, 6)'),
                ('numberparamc', 'numeric(19, 6)'),
                ('objparama', 'bigint'),
                ('objparamb', 'bigint'),
                ('quality_code', 'character varying(256)'),
                ('quality_std_id', 'bigint'),
                ('remark', 'text'),
                ('scparama', 'text'),
                ('scparamb', 'text'),
                ('task_id', 'bigint')
          ) AS columns(name, definition)
    LOOP
        EXECUTE format(
            'ALTER TABLE public.wom_task_qualities ADD COLUMN IF NOT EXISTS %I %s',
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
         WHERE conrelid = 'public.wom_task_materials'::regclass
           AND contype = 'p'
    ) THEN
        ALTER TABLE public.wom_task_materials
            ADD CONSTRAINT wom_task_materials_pkey PRIMARY KEY (id);
    END IF;

    IF NOT EXISTS (
        SELECT 1
          FROM pg_constraint
         WHERE conrelid = 'public.wom_task_qualities'::regclass
           AND contype = 'p'
    ) THEN
        ALTER TABLE public.wom_task_qualities
            ADD CONSTRAINT wom_task_qualities_pkey PRIMARY KEY (id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_wom_task_materials_task
    ON public.wom_task_materials(task_id, valid);

CREATE INDEX IF NOT EXISTS idx_wom_task_materials_material
    ON public.wom_task_materials(material_id);

CREATE INDEX IF NOT EXISTS idx_wom_task_materials_table_info
    ON public.wom_task_materials(table_info_id);

CREATE INDEX IF NOT EXISTS idx_wom_task_qualities_task
    ON public.wom_task_qualities(task_id, valid);

CREATE INDEX IF NOT EXISTS idx_wom_task_qualities_quality_std
    ON public.wom_task_qualities(quality_std_id);

CREATE INDEX IF NOT EXISTS idx_wom_task_qualities_table_info
    ON public.wom_task_qualities(table_info_id);
