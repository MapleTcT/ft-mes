-- PostgreSQL compatibility for RM formula audit/supervision tables used by
-- RMFormulaService.saveFormula during batch formula sync.
-- Source evidence:
--   RMFormulaSupervision.TABLE_NAME = RM_FORMULAS_SV with MAIN_OBJ
--   RMFormulaDealInfo.TABLE_NAME = RM_FORMULAS_DI with MAIN_OBJ

DO $$
DECLARE
    rel_kind char;
BEGIN
    SELECT c.relkind INTO rel_kind
    FROM pg_class c
    JOIN pg_namespace n ON n.oid = c.relnamespace
    WHERE n.nspname = 'public'
      AND c.relname = 'rm_formulas_sv';

    IF rel_kind IS NULL THEN
        CREATE TABLE public.rm_formulas_sv (
            table_info_id bigint,
            staff bigint
        );
        rel_kind := 'r';
    END IF;

    IF rel_kind = 'v' THEN
        EXECUTE $view$
CREATE OR REPLACE VIEW public.rm_formulas_sv AS
SELECT table_info_id,
       owner_staff_id AS staff,
       id,
       0::integer AS version,
       true::boolean AS valid,
       id AS main_obj,
       create_staff_id,
       modify_staff_id,
       delete_staff_id,
       create_time,
       modify_time,
       delete_time
FROM public.rm_formulas
WHERE table_info_id IS NOT NULL AND owner_staff_id IS NOT NULL
$view$;
    ELSE
        ALTER TABLE public.rm_formulas_sv ADD COLUMN IF NOT EXISTS id bigint;
        ALTER TABLE public.rm_formulas_sv ADD COLUMN IF NOT EXISTS version integer DEFAULT 0;
        ALTER TABLE public.rm_formulas_sv ADD COLUMN IF NOT EXISTS valid boolean DEFAULT true;
        ALTER TABLE public.rm_formulas_sv ADD COLUMN IF NOT EXISTS main_obj bigint;
        ALTER TABLE public.rm_formulas_sv ADD COLUMN IF NOT EXISTS create_staff_id bigint;
        ALTER TABLE public.rm_formulas_sv ADD COLUMN IF NOT EXISTS modify_staff_id bigint;
        ALTER TABLE public.rm_formulas_sv ADD COLUMN IF NOT EXISTS delete_staff_id bigint;
        ALTER TABLE public.rm_formulas_sv ADD COLUMN IF NOT EXISTS create_time timestamp without time zone;
        ALTER TABLE public.rm_formulas_sv ADD COLUMN IF NOT EXISTS modify_time timestamp without time zone;
        ALTER TABLE public.rm_formulas_sv ADD COLUMN IF NOT EXISTS delete_time timestamp without time zone;

        CREATE INDEX IF NOT EXISTS idx_rm_formulas_sv_main_obj
            ON public.rm_formulas_sv(main_obj, valid);

        CREATE INDEX IF NOT EXISTS idx_rm_formulas_sv_table_info
            ON public.rm_formulas_sv(table_info_id);
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS public.rm_formulas_di (
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
                ('valid', 'boolean DEFAULT true'),
                ('table_info_id', 'bigint'),
                ('main_obj', 'bigint'),
                ('staff', 'bigint'),
                ('sort', 'integer'),
                ('recalled_flag', 'boolean DEFAULT false'),
                ('user_agent', 'text'),
                ('create_staff_id', 'bigint'),
                ('create_time', 'timestamp without time zone'),
                ('modify_staff_id', 'bigint'),
                ('modify_time', 'timestamp without time zone'),
                ('delete_staff_id', 'bigint'),
                ('delete_time', 'timestamp without time zone')
        ) AS columns(name, definition)
    LOOP
        EXECUTE format(
            'ALTER TABLE public.rm_formulas_di ADD COLUMN IF NOT EXISTS %I %s',
            column_def.name,
            column_def.definition
        );
    END LOOP;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'public.rm_formulas_di'::regclass AND contype = 'p'
    ) THEN
        ALTER TABLE public.rm_formulas_di
            ADD CONSTRAINT rm_formulas_di_pkey PRIMARY KEY (id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_rm_formulas_di_main_obj
    ON public.rm_formulas_di(main_obj);

CREATE INDEX IF NOT EXISTS idx_rm_formulas_di_table_info
    ON public.rm_formulas_di(table_info_id);
