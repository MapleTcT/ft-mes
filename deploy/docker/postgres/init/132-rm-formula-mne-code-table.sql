-- PostgreSQL compatibility for RM formula mnemonic-code persistence.
-- Source evidence:
--   RMFormulaMneCode.TABLE_NAME = RM_FORMULAS_MC
--   RMBatchFormulaServiceImpl.addFormula deletes RM_FORMULAS_MC by formula id
-- Batch formula sync fails on PostgreSQL when this Oracle-era support table is
-- missing, before rm_formulas persistence can commit.

CREATE TABLE IF NOT EXISTS public.rm_formulas_mc (
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
                ('mne_code', 'text'),
                ('formula', 'bigint')
        ) AS columns(name, definition)
    LOOP
        EXECUTE format(
            'ALTER TABLE public.rm_formulas_mc ADD COLUMN IF NOT EXISTS %I %s',
            column_def.name,
            column_def.definition
        );
    END LOOP;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'public.rm_formulas_mc'::regclass AND contype = 'p'
    ) THEN
        ALTER TABLE public.rm_formulas_mc
            ADD CONSTRAINT rm_formulas_mc_pkey PRIMARY KEY (id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_rm_formulas_mc_formula
    ON public.rm_formulas_mc(formula);

CREATE INDEX IF NOT EXISTS idx_rm_formulas_mc_mne_code
    ON public.rm_formulas_mc(mne_code);
