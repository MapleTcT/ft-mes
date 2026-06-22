-- PostgreSQL large-object compatibility for WOM wait-put records.
--
-- WOMWaitPutRecord.remark is mapped as @Lob String. Hibernate reads it through
-- ResultSet.getClob(), which PostgreSQL expects to contain a large-object OID.
-- Keeping inline text in this column makes the driver parse the text value as a
-- long and fail during business-flow reads, for example:
--   Bad value for type long : ADP_E2E_..._WOM_MANU_INSPECT
--
-- Keep a text backup so the migrated value remains inspectable.

ALTER TABLE public.wom_wait_put_records
    ADD COLUMN IF NOT EXISTS remark_text_backup text;

DO $$
DECLARE
    current_udt text;
BEGIN
    SELECT udt_name
      INTO current_udt
      FROM information_schema.columns
     WHERE table_schema = 'public'
       AND table_name = 'wom_wait_put_records'
       AND column_name = 'remark';

    IF current_udt IN ('text', 'varchar') THEN
        UPDATE public.wom_wait_put_records
           SET remark_text_backup = remark
         WHERE remark IS NOT NULL
           AND remark_text_backup IS NULL;

        ALTER TABLE public.wom_wait_put_records
            ALTER COLUMN remark TYPE oid
            USING CASE
                WHEN remark IS NULL OR remark = '' THEN NULL
                ELSE lo_from_bytea(0, convert_to(remark::text, 'UTF8'))
            END;
    END IF;
END $$;
