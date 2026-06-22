-- PostgreSQL large-object compatibility for WOM report check detail remarks.
--
-- WOMProCheckDetail.remark is mapped as @Lob String in the legacy Hibernate
-- entity. PostgreSQL JDBC reads that mapping through ResultSet.getClob(), so a
-- non-empty inline text value in remark is parsed as a large-object OID and
-- fails with "Bad value for type long" during endActive/check-report flows.
--
-- Keep a text backup so migrated test markers remain inspectable.

ALTER TABLE public.wom_pro_check_details
    ADD COLUMN IF NOT EXISTS remark_text_backup text;

DO $$
DECLARE
    current_udt text;
BEGIN
    SELECT udt_name
      INTO current_udt
      FROM information_schema.columns
     WHERE table_schema = 'public'
       AND table_name = 'wom_pro_check_details'
       AND column_name = 'remark';

    IF current_udt IN ('text', 'varchar') THEN
        UPDATE public.wom_pro_check_details
           SET remark_text_backup = remark
         WHERE remark IS NOT NULL
           AND remark_text_backup IS NULL;

        ALTER TABLE public.wom_pro_check_details
            ALTER COLUMN remark TYPE oid
            USING CASE
                WHEN remark IS NULL OR remark = '' THEN NULL
                ELSE lo_from_bytea(0, convert_to(remark::text, 'UTF8'))
            END;
    END IF;
END $$;
