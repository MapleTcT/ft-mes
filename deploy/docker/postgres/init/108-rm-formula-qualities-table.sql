-- PostgreSQL compatibility table for RM_6.1.2.3 FormulaQuality.
-- Source evidence:
--   modules/rm/RM_6.1.2.3/service/src/main/resources/META-INF/init/init.xml
--   modules/rm/RM_6.1.2.3/core/src/main/java/.../RMFormulaQuality.java
--
-- WOM -> QCS inspection creation queries rm_formula_qualities for the
-- requester staff/department by formula, work unit, and optional formula
-- activity. The recovered PostgreSQL runtime did not have this table, so
-- create the source-aligned structure before real createManuInspect
-- persistence acceptance can continue.

CREATE TABLE IF NOT EXISTS public.rm_formula_qualities (
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
                ('table_info_id', 'bigint'),
                ('apply_check_dep_id', 'bigint'),
                ('apply_check_staff_id', 'bigint'),
                ('check_dep_id', 'bigint'),
                ('check_staff_id', 'bigint'),
                ('deal_type', 'character varying'),
                ('final_inspection', 'boolean DEFAULT false'),
                ('formula_active_id', 'bigint'),
                ('formula_id', 'bigint'),
                ('inspect_system', 'bigint'),
                ('is_on_site_check', 'boolean'),
                ('is_pass_check', 'boolean DEFAULT false'),
                ('material_id', 'bigint'),
                ('quality_code', 'character varying(256)'),
                ('quality_std_id', 'bigint'),
                ('reject_system', 'bigint'),
                ('remark', 'text'),
                ('unit_id', 'bigint'),
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
            'ALTER TABLE public.rm_formula_qualities ADD COLUMN IF NOT EXISTS %I %s',
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
        WHERE conrelid = 'public.rm_formula_qualities'::regclass
          AND contype = 'p'
    ) THEN
        ALTER TABLE public.rm_formula_qualities
            ADD CONSTRAINT rm_formula_qualities_pkey PRIMARY KEY (id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_rm_formula_qualities_table_info
    ON public.rm_formula_qualities(table_info_id);

CREATE INDEX IF NOT EXISTS idx_rm_formula_qualities_formula_unit
    ON public.rm_formula_qualities(formula_id, unit_id, valid);

CREATE INDEX IF NOT EXISTS idx_rm_formula_qualities_formula_active
    ON public.rm_formula_qualities(formula_id, formula_active_id, unit_id, valid);

CREATE INDEX IF NOT EXISTS idx_rm_formula_qualities_quality_std
    ON public.rm_formula_qualities(quality_std_id);

CREATE INDEX IF NOT EXISTS idx_rm_formula_qualities_apply_check
    ON public.rm_formula_qualities(apply_check_staff_id, apply_check_dep_id);
