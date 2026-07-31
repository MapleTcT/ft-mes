-- PostgreSQL compatibility for the RM formula quality remark CLOB.
--
-- RMFormulaQuality.remark is mapped with @Lob. The recovered PostgreSQL
-- schema stored it as plain text, so Hibernate attempted to interpret the
-- business value as a large-object OID while enabling a formula.

DO $$
DECLARE
    remark_type text;
    invalid_lob_count bigint;
BEGIN
    IF to_regclass('public.rm_formula_qualities') IS NULL THEN
        RAISE EXCEPTION 'public.rm_formula_qualities is required';
    END IF;

    SELECT c.udt_name
      INTO remark_type
      FROM information_schema.columns c
     WHERE c.table_schema = 'public'
       AND c.table_name = 'rm_formula_qualities'
       AND c.column_name = 'remark';

    IF remark_type IS NULL THEN
        RAISE EXCEPTION 'public.rm_formula_qualities.remark is required';
    END IF;

    IF remark_type IS DISTINCT FROM 'oid' THEN
        ALTER TABLE public.rm_formula_qualities
            ALTER COLUMN remark TYPE oid
            USING CASE
                WHEN remark IS NULL OR remark::text = '' THEN NULL
                ELSE lo_from_bytea(0, convert_to(remark::text, 'UTF8'))
            END;
    END IF;

    SELECT count(*)
      INTO invalid_lob_count
      FROM public.rm_formula_qualities q
     WHERE q.remark IS NOT NULL
       AND NOT EXISTS (
            SELECT 1
              FROM pg_largeobject_metadata lom
             WHERE lom.oid = q.remark
       );

    IF invalid_lob_count <> 0 THEN
        RAISE EXCEPTION
            'rm_formula_qualities.remark contains % invalid large-object references',
            invalid_lob_count;
    END IF;
END
$$;
