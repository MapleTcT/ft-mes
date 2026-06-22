-- PostgreSQL compatibility tables for RM batch formula persistence.
-- Source evidence:
--   modules/RM/RM_6.1.2.3/core/src/main/java/com/supcon/orchid/RM/entities/RMFormulaMateria.java
--   modules/RM/RM_6.1.2.3/core/src/main/java/com/supcon/orchid/RM/entities/RMFormulaProcess.java
-- Batch formula sync saves RM_FORMULAS, then RMFormulaService.autoProduce
-- queries rm_formula_materias and deleteFormula updates rm_formula_processes.

CREATE TABLE IF NOT EXISTS public.rm_formula_materias (
    id bigint
);

CREATE TABLE IF NOT EXISTS public.rm_formula_processes (
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
                ('formula_id', 'bigint'),
                ('material_id', 'bigint'),
                ('property', 'character varying'),
                ('fixed_quantity', 'numeric(19, 6)'),
                ('is_mix_quality', 'boolean DEFAULT false'),
                ('loss_rate', 'numeric(19, 6)'),
                ('max_quality', 'numeric(19, 6)'),
                ('min_quality', 'numeric(19, 6)'),
                ('need_prepare', 'boolean DEFAULT false'),
                ('remark', 'text'),
                ('unit_quality', 'numeric(19, 6)'),
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
                ('numberparama', 'numeric(19, 6)'),
                ('numberparamb', 'numeric(19, 6)'),
                ('numberparamc', 'numeric(19, 6)'),
                ('objparama', 'bigint'),
                ('objparamb', 'bigint'),
                ('scparama', 'text'),
                ('scparamb', 'text')
        ) AS columns(name, definition)
    LOOP
        EXECUTE format(
            'ALTER TABLE public.rm_formula_materias ADD COLUMN IF NOT EXISTS %I %s',
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
                ('auto_start', 'boolean'),
                ('batch_unit_id', 'character varying(256)'),
                ('copy_source_id', 'bigint'),
                ('exe_order', 'integer'),
                ('formula', 'bigint'),
                ('formula_id', 'bigint'),
                ('is_first_process', 'boolean'),
                ('is_last_process', 'boolean DEFAULT false'),
                ('long_time', 'numeric(19, 6)'),
                ('name', 'character varying(256)'),
                ('need_param_ana', 'boolean'),
                ('node_id', 'character varying(256)'),
                ('process_table_id', 'bigint'),
                ('process_type', 'bigint'),
                ('proc_sort', 'character varying(256)'),
                ('remark', 'text'),
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
                ('numberparama', 'numeric(19, 6)'),
                ('numberparamb', 'numeric(19, 6)'),
                ('numberparamc', 'numeric(19, 6)'),
                ('objparama', 'bigint'),
                ('objparamb', 'bigint'),
                ('scparama', 'text'),
                ('scparamb', 'text')
        ) AS columns(name, definition)
    LOOP
        EXECUTE format(
            'ALTER TABLE public.rm_formula_processes ADD COLUMN IF NOT EXISTS %I %s',
            column_def.name,
            column_def.definition
        );
    END LOOP;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'public.rm_formula_materias'::regclass AND contype = 'p'
    ) THEN
        ALTER TABLE public.rm_formula_materias
            ADD CONSTRAINT rm_formula_materias_pkey PRIMARY KEY (id);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'public.rm_formula_processes'::regclass AND contype = 'p'
    ) THEN
        ALTER TABLE public.rm_formula_processes
            ADD CONSTRAINT rm_formula_processes_pkey PRIMARY KEY (id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_rm_formula_materias_formula
    ON public.rm_formula_materias(formula_id, valid);

CREATE INDEX IF NOT EXISTS idx_rm_formula_materias_table_info
    ON public.rm_formula_materias(table_info_id);

CREATE INDEX IF NOT EXISTS idx_rm_formula_processes_formula
    ON public.rm_formula_processes(formula, formula_id, valid);

CREATE INDEX IF NOT EXISTS idx_rm_formula_processes_table_info
    ON public.rm_formula_processes(table_info_id);
