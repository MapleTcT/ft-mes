-- Align JBPM4 repository LOB storage with the legacy Hibernate Blob mapping.
--
-- The old workflow engine inserts PostgreSQL large-object OIDs into
-- jbpm4_lob.blob_value_.  When the recovered schema has this column as bytea,
-- workflow publication fails with:
--   column "blob_value_" is of type bytea but expression is of type bigint
--
-- Keep the change idempotent and preserve any existing bytea payloads by
-- importing them into PostgreSQL large objects.
DO $$
DECLARE
    blob_type text;
BEGIN
    SELECT data_type
      INTO blob_type
      FROM information_schema.columns
     WHERE table_schema = 'public'
       AND table_name = 'jbpm4_lob'
       AND column_name = 'blob_value_';

    IF blob_type = 'bytea' THEN
        ALTER TABLE public.jbpm4_lob
            ALTER COLUMN blob_value_ TYPE oid
            USING CASE
                WHEN blob_value_ IS NULL THEN NULL
                ELSE lo_from_bytea(0, blob_value_)
            END;
    END IF;
END $$;
