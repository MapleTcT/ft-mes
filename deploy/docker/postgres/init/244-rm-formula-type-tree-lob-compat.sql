-- Align the restored RM formula-type tree with the legacy entity mapping.
--
-- AbstractEcTreeFullEntity binds the inherited OA property as a large-object
-- value. Other restored tree tables (for example hm_factory_models and
-- qlf_certificate_types) therefore use text for this compatibility column.
-- A boolean column rejects even the typed null sent by Hibernate and rolls back
-- formula-type inserts before mnemonic-code persistence runs.

ALTER TABLE public.rm_formula_types
    ALTER COLUMN oa TYPE text
    USING CASE
        WHEN oa IS NULL THEN NULL
        ELSE oa::text
    END;

DO $$
DECLARE
    oa_type text;
BEGIN
    SELECT data_type
      INTO oa_type
      FROM information_schema.columns
     WHERE table_schema = 'public'
       AND table_name = 'rm_formula_types'
       AND column_name = 'oa';

    IF oa_type IS DISTINCT FROM 'text' THEN
        RAISE EXCEPTION
            'RM formula-type OA compatibility failed: expected text, got %',
            oa_type;
    END IF;
END $$;
