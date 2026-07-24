-- PostgreSQL compatibility table for RM formula to production line mappings.
--
-- Runtime evidence:
-- - WOM make-task "生产线名称" opens HierarchicalMod factoryLineRef.
-- - When the source form passes customCondition.formulaId, the recovered
--   reference condition queries rm_line_formulas:
--     select LINE_ID from rm_line_formulas where FORMULA_ID = ? and VALID = 1
-- - The PostgreSQL dataset had rm_formulas but not rm_line_formulas, causing
--   SQLGrammarException / relation "rm_line_formulas" does not exist.
--
-- This script creates the missing RM_1.0.0_formula_LineFormula table structure
-- from the recovered RMLineFormula entity. It does not invent new mappings;
-- the backfill only preserves relationships already present on WOM tasks.

CREATE TABLE IF NOT EXISTS public.rm_line_formulas (
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
                ('default_capacity', 'numeric(19, 6)'),
                ('formula_id', 'bigint'),
                ('line_id', 'bigint'),
                ('long_time', 'numeric(19, 6)'),
                ('max_capacity', 'numeric(19, 6)'),
                ('min_capacity', 'numeric(19, 6)'),
                ('numberparama', 'numeric(19, 6)'),
                ('numberparamb', 'numeric(19, 6)'),
                ('numberparamc', 'numeric(19, 6)'),
                ('objparama', 'bigint'),
                ('objparamb', 'bigint'),
                ('process_unit_cache', 'text'),
                ('remark', 'text'),
                ('report_type', 'character varying(255)'),
                ('scparama', 'text'),
                ('scparamb', 'text'),
                ('theore_yield', 'numeric(19, 6)')
          ) AS columns(name, definition)
    LOOP
        EXECUTE format(
            'ALTER TABLE public.rm_line_formulas ADD COLUMN IF NOT EXISTS %I %s',
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
         WHERE conrelid = 'public.rm_line_formulas'::regclass
           AND contype = 'p'
    ) THEN
        ALTER TABLE public.rm_line_formulas
            ADD CONSTRAINT rm_line_formulas_pkey PRIMARY KEY (id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_rm_line_formulas_formula_valid
    ON public.rm_line_formulas(formula_id, valid);

CREATE INDEX IF NOT EXISTS idx_rm_line_formulas_line_valid
    ON public.rm_line_formulas(line_id, valid);

CREATE INDEX IF NOT EXISTS idx_rm_line_formulas_table_info
    ON public.rm_line_formulas(table_info_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_rm_line_formulas_formula_line_valid
    ON public.rm_line_formulas(formula_id, line_id)
    WHERE valid IS TRUE AND formula_id IS NOT NULL AND line_id IS NOT NULL;

DO $$
BEGIN
    IF to_regclass('public.wom_produce_tasks') IS NOT NULL THEN
        EXECUTE $backfill$
            INSERT INTO public.rm_line_formulas (
                id,
                version,
                create_time,
                valid,
                cid,
                formula_id,
                line_id,
                table_info_id
            )
            SELECT
                (9000000000000000::bigint + row_number() OVER (ORDER BY task.formula_id, task.line_id)) AS id,
                0 AS version,
                CURRENT_TIMESTAMP AS create_time,
                true AS valid,
                min(task.cid) AS cid,
                task.formula_id,
                task.line_id,
                min(task.table_info_id) AS table_info_id
            FROM public.wom_produce_tasks task
            WHERE task.formula_id IS NOT NULL
              AND task.line_id IS NOT NULL
              AND COALESCE(task.valid, true) IS TRUE
            GROUP BY task.formula_id, task.line_id
            ON CONFLICT DO NOTHING
        $backfill$;
    END IF;
END $$;
