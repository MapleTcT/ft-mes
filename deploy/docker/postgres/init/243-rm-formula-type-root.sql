-- Bootstrap the RM formula-type tree for a clean PostgreSQL deployment.
--
-- The packaged tree view exposes a synthetic "配方类型" navigation root, but
-- the standard add action requires a persisted node to be selected as parent.
-- Without one, a clean database can never create its first business category.
-- This neutral root is product master data, not an acceptance-test record.

INSERT INTO public.rm_formula_types (
    id,
    version,
    valid,
    cid,
    full_path_name,
    parent_id,
    lay_no,
    lay_rec,
    leaf,
    name,
    code
)
VALUES (
    1000,
    0,
    true,
    1000,
    '1000',
    -1,
    1,
    '1000',
    0,
    '默认配方分类',
    'defaultFormulaType'
)
ON CONFLICT (id) DO UPDATE SET
    valid = true,
    cid = EXCLUDED.cid,
    full_path_name = EXCLUDED.full_path_name,
    parent_id = EXCLUDED.parent_id,
    lay_no = EXCLUDED.lay_no,
    lay_rec = EXCLUDED.lay_rec,
    leaf = EXCLUDED.leaf,
    name = EXCLUDED.name,
    code = EXCLUDED.code;

DO $$
DECLARE
    root_count integer;
BEGIN
    SELECT count(*)
      INTO root_count
      FROM public.rm_formula_types
     WHERE id = 1000
       AND valid = true
       AND cid = 1000
       AND parent_id = -1
       AND code = 'defaultFormulaType'
       AND name = '默认配方分类';

    IF root_count <> 1 THEN
        RAISE EXCEPTION
            'RM formula-type root bootstrap failed: expected 1 valid root, got %',
            root_count;
    END IF;
END $$;
