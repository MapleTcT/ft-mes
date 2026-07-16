-- PATROL 6.0.4.0 PostgreSQL runtime compatibility and required seed data.
-- Map-layer and mobile-app rows from the legacy package remain conditional on
-- SESGISConfig and BASE_APP_BUSINFO; neither table set exists in the current
-- PostgreSQL baseline, so this migration does not manufacture those products.

DO $$
DECLARE
    column_kind text;
BEGIN
    SELECT data_type INTO column_kind
    FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name = 'mp_public_item_trees'
      AND column_name = 'oa';
    IF column_kind = 'boolean' THEN
        ALTER TABLE public.mp_public_item_trees
            ALTER COLUMN oa TYPE text USING oa::text;
    END IF;

    SELECT data_type INTO column_kind
    FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name = 'mp_work_groups'
      AND column_name = 'oa';
    IF column_kind = 'boolean' THEN
        ALTER TABLE public.mp_work_groups
            ALTER COLUMN oa TYPE text USING oa::text;
    END IF;

    SELECT data_type INTO column_kind
    FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name = 'mp_public_item_trees'
      AND column_name = 'sort';
    IF column_kind = 'integer' THEN
        ALTER TABLE public.mp_public_item_trees
            ALTER COLUMN sort TYPE bigint USING sort::bigint;
    END IF;

    SELECT data_type INTO column_kind
    FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name = 'mp_work_groups'
      AND column_name = 'sort';
    IF column_kind = 'integer' THEN
        ALTER TABLE public.mp_work_groups
            ALTER COLUMN sort TYPE bigint USING sort::bigint;
    END IF;
END $$;

WITH standards(code, name, val_type, edit_type, decimal_place, candidate_value, sort) AS (
    VALUES
        ('M-RJY-1-001', '字符录入', 'PATROL_valueType/char', 'PATROL_editType/input', NULL::integer, NULL::varchar, 0),
        ('M-RJY-1-002', '是否', 'PATROL_valueType/char', 'PATROL_editType/whether', NULL::integer, '是,否', 1),
        ('M-RJY-1-003', '数字录入', 'PATROL_valueType/number', 'PATROL_editType/input', 2, NULL::varchar, 2)
), missing AS (
    SELECT standards.*, row_number() OVER (ORDER BY standards.sort) AS row_no
    FROM standards
    WHERE NOT EXISTS (
        SELECT 1 FROM public.mp_input_standards current_row
        WHERE current_row.code = standards.code
    )
), id_base AS (
    SELECT COALESCE(MAX(id), 999) AS max_id FROM public.mp_input_standards
)
INSERT INTO public.mp_input_standards (
    id, version, create_time, valid, cid, sort, effective_state,
    val_type, state, name, edit_type, decimal_place, code, candidate_value
)
SELECT
    id_base.max_id + missing.row_no,
    0,
    CURRENT_TIMESTAMP,
    true,
    1000,
    missing.sort,
    0,
    missing.val_type,
    true,
    missing.name,
    missing.edit_type,
    missing.decimal_place,
    missing.code,
    missing.candidate_value
FROM missing CROSS JOIN id_base;

WITH candidate_values(standard_code, value_name, sort) AS (
    VALUES
        ('M-RJY-1-002', '是', 1),
        ('M-RJY-1-002', '否', 2),
        ('M-RJY-1-002', 'YES', 3),
        ('M-RJY-1-002', 'NO', 4)
), resolved AS (
    SELECT
        standards.id AS input_standard_id,
        candidate_values.value_name,
        candidate_values.sort,
        row_number() OVER (ORDER BY candidate_values.sort) AS row_no
    FROM candidate_values
    JOIN public.mp_input_standards standards
      ON standards.code = candidate_values.standard_code
    WHERE NOT EXISTS (
        SELECT 1
        FROM public.mp_candidate_values current_row
        WHERE current_row.input_standard_id = standards.id
          AND current_row.value_name = candidate_values.value_name
    )
), id_base AS (
    SELECT COALESCE(MAX(id), 999) AS max_id FROM public.mp_candidate_values
)
INSERT INTO public.mp_candidate_values (
    id, version, create_time, valid, cid, sort, effective_state,
    input_standard_id, value_name
)
SELECT
    id_base.max_id + resolved.row_no,
    0,
    CURRENT_TIMESTAMP,
    true,
    1000,
    resolved.sort,
    0,
    resolved.input_standard_id,
    resolved.value_name
FROM resolved CROSS JOIN id_base;

WITH mnemonic_values(standard_code, mne_code) AS (
    VALUES
        ('M-RJY-1-001', 'character'),
        ('M-RJY-1-001', 'character input'),
        ('M-RJY-1-001', 'm-rjy-1-00'),
        ('M-RJY-1-001', 'm-rjy-1-001'),
        ('M-RJY-1-001', 'zflr'),
        ('M-RJY-1-001', 'zifuluru'),
        ('M-RJY-1-001', '字符录入'),
        ('M-RJY-1-002', 'yes or no'),
        ('M-RJY-1-002', 'm-rjy-1-00'),
        ('M-RJY-1-002', 'm-rjy-1-002'),
        ('M-RJY-1-002', 'sf'),
        ('M-RJY-1-002', 'shifou'),
        ('M-RJY-1-002', 'shipi'),
        ('M-RJY-1-002', 'sp'),
        ('M-RJY-1-002', '是否'),
        ('M-RJY-1-003', 'digital'),
        ('M-RJY-1-003', 'digital input'),
        ('M-RJY-1-003', 'm-rjy-1-00'),
        ('M-RJY-1-003', 'm-rjy-1-003'),
        ('M-RJY-1-003', 'shuziluru'),
        ('M-RJY-1-003', 'shuoziluru'),
        ('M-RJY-1-003', 'szlr'),
        ('M-RJY-1-003', '数字录入')
), resolved AS (
    SELECT
        standards.id AS input_standard_id,
        mnemonic_values.mne_code,
        row_number() OVER (ORDER BY mnemonic_values.standard_code, mnemonic_values.mne_code) AS row_no
    FROM mnemonic_values
    JOIN public.mp_input_standards standards
      ON standards.code = mnemonic_values.standard_code
    WHERE NOT EXISTS (
        SELECT 1
        FROM public.mp_input_standards_mc current_row
        WHERE COALESCE(current_row.input_standard_id, current_row.input_standard) = standards.id
          AND current_row.mne_code = mnemonic_values.mne_code
    )
), id_base AS (
    SELECT COALESCE(MAX(id), 999) AS max_id FROM public.mp_input_standards_mc
)
INSERT INTO public.mp_input_standards_mc (
    id, version, input_standard_id, input_standard, mne_code
)
SELECT
    id_base.max_id + resolved.row_no,
    0,
    resolved.input_standard_id,
    resolved.input_standard_id,
    resolved.mne_code
FROM resolved CROSS JOIN id_base;

CREATE INDEX IF NOT EXISTS idx_mp_input_standards_code
    ON public.mp_input_standards(code);
CREATE INDEX IF NOT EXISTS idx_mp_candidate_values_standard
    ON public.mp_candidate_values(input_standard_id);

DO $$
BEGIN
    IF to_regclass('public.base_app_businfo') IS NULL THEN
        RAISE NOTICE 'PATROL mobile app registration skipped: BASE_APP_BUSINFO is unavailable';
    END IF;
    IF to_regclass('public.sesgis_module_configs') IS NULL THEN
        RAISE NOTICE 'PATROL map seed skipped: SESGISConfig tables are unavailable';
    END IF;
END $$;
