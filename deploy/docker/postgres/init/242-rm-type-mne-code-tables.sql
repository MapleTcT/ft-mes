-- PostgreSQL compatibility for RM formula-type and process-type mnemonic codes.
--
-- Source evidence:
--   RMFormulaTypeMneCode.TABLE_NAME = RM_FORMULA_TYPES_MC
--   RMProcessTypeMneCode.TABLE_NAME = RM_PROCESS_TYPES_MC
-- Both legacy save services delete and rebuild mnemonic-code rows in the same
-- transaction as the master record. Missing support tables therefore roll back
-- otherwise valid formula-type and process-type creates or updates.

CREATE TABLE IF NOT EXISTS public.rm_formula_types_mc (
    id bigint
);

CREATE TABLE IF NOT EXISTS public.rm_process_types_mc (
    id bigint
);

DO $$
DECLARE
    target_table text;
    relation_column text;
    column_def record;
BEGIN
    FOR target_table, relation_column IN
        VALUES
            ('rm_formula_types_mc', 'formula_type'),
            ('rm_process_types_mc', 'process_type')
    LOOP
        FOR column_def IN
            SELECT *
            FROM (
                VALUES
                    ('id', 'bigint'),
                    ('version', 'integer DEFAULT 0'),
                    ('valid', 'boolean DEFAULT true'),
                    ('mne_code', 'text'),
                    (relation_column, 'bigint')
            ) AS columns(name, definition)
        LOOP
            EXECUTE format(
                'ALTER TABLE public.%I ADD COLUMN IF NOT EXISTS %I %s',
                target_table,
                column_def.name,
                column_def.definition
            );
        END LOOP;
    END LOOP;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
          FROM pg_constraint
         WHERE conrelid = 'public.rm_formula_types_mc'::regclass
           AND contype = 'p'
    ) THEN
        ALTER TABLE public.rm_formula_types_mc
            ADD CONSTRAINT rm_formula_types_mc_pkey PRIMARY KEY (id);
    END IF;

    IF NOT EXISTS (
        SELECT 1
          FROM pg_constraint
         WHERE conrelid = 'public.rm_process_types_mc'::regclass
           AND contype = 'p'
    ) THEN
        ALTER TABLE public.rm_process_types_mc
            ADD CONSTRAINT rm_process_types_mc_pkey PRIMARY KEY (id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_rm_formula_types_mc_formula_type
    ON public.rm_formula_types_mc(formula_type);

CREATE INDEX IF NOT EXISTS idx_rm_formula_types_mc_mne_code
    ON public.rm_formula_types_mc(mne_code);

CREATE INDEX IF NOT EXISTS idx_rm_process_types_mc_process_type
    ON public.rm_process_types_mc(process_type);

CREATE INDEX IF NOT EXISTS idx_rm_process_types_mc_mne_code
    ON public.rm_process_types_mc(mne_code);

DO $$
DECLARE
    missing_columns text[];
BEGIN
    SELECT array_agg(required_column ORDER BY required_column)
      INTO missing_columns
      FROM (
          VALUES
              ('rm_formula_types_mc', 'id'),
              ('rm_formula_types_mc', 'mne_code'),
              ('rm_formula_types_mc', 'formula_type'),
              ('rm_process_types_mc', 'id'),
              ('rm_process_types_mc', 'mne_code'),
              ('rm_process_types_mc', 'process_type')
      ) AS required(table_name, required_column)
     WHERE NOT EXISTS (
         SELECT 1
           FROM information_schema.columns
          WHERE table_schema = 'public'
            AND information_schema.columns.table_name = required.table_name
            AND column_name = required.required_column
     );

    IF missing_columns IS NOT NULL THEN
        RAISE EXCEPTION
            'RM mnemonic-code compatibility failed; missing columns: %',
            missing_columns;
    END IF;
END $$;
