-- PostgreSQL large-object compatibility for workflow XML CLOB fields.
--
-- Runtime evidence:
-- - WOM generatePrepareNeed now reaches TaskServiceImpl.getCurrentDeployment.
-- - Hibernate reads wf_deployment.process_xml, temp_process_xml, and
--   operate_powers through CLOB descriptors in the legacy workflow engine.
-- - PostgreSQL JDBC getClob expects a large-object OID, not inline text, and
--   raises "Bad value for type long" when the XML text is stored directly.
--
-- Keep text backups so the conversion remains reversible for migration work.

ALTER TABLE public.wf_deployment
    ADD COLUMN IF NOT EXISTS process_xml_text_backup text,
    ADD COLUMN IF NOT EXISTS temp_process_xml_text_backup text,
    ADD COLUMN IF NOT EXISTS operate_powers_text_backup text;

DO $$
DECLARE
    process_xml_type text;
    temp_process_xml_type text;
    operate_powers_type text;
BEGIN
    SELECT udt_name
      INTO process_xml_type
      FROM information_schema.columns
     WHERE table_schema = 'public'
       AND table_name = 'wf_deployment'
       AND column_name = 'process_xml';

    IF process_xml_type IN ('text', 'varchar') THEN
        UPDATE public.wf_deployment
           SET process_xml_text_backup = process_xml
         WHERE process_xml IS NOT NULL
           AND process_xml_text_backup IS NULL;

        ALTER TABLE public.wf_deployment
            ALTER COLUMN process_xml TYPE oid
            USING CASE
                WHEN process_xml IS NULL THEN NULL
                ELSE lo_from_bytea(0, convert_to(process_xml, 'UTF8'))
            END;
    END IF;

    SELECT udt_name
      INTO temp_process_xml_type
      FROM information_schema.columns
     WHERE table_schema = 'public'
       AND table_name = 'wf_deployment'
       AND column_name = 'temp_process_xml';

    IF temp_process_xml_type IN ('text', 'varchar') THEN
        UPDATE public.wf_deployment
           SET temp_process_xml_text_backup = temp_process_xml
         WHERE temp_process_xml IS NOT NULL
           AND temp_process_xml_text_backup IS NULL;

        ALTER TABLE public.wf_deployment
            ALTER COLUMN temp_process_xml TYPE oid
            USING CASE
                WHEN temp_process_xml IS NULL THEN NULL
                ELSE lo_from_bytea(0, convert_to(temp_process_xml, 'UTF8'))
            END;
    END IF;

    SELECT udt_name
      INTO operate_powers_type
      FROM information_schema.columns
     WHERE table_schema = 'public'
       AND table_name = 'wf_deployment'
       AND column_name = 'operate_powers';

    IF operate_powers_type IN ('text', 'varchar') THEN
        UPDATE public.wf_deployment
           SET operate_powers_text_backup = operate_powers
         WHERE operate_powers IS NOT NULL
           AND operate_powers_text_backup IS NULL;

        ALTER TABLE public.wf_deployment
            ALTER COLUMN operate_powers TYPE oid
            USING CASE
                WHEN operate_powers IS NULL OR operate_powers = '' THEN NULL
                ELSE lo_from_bytea(0, convert_to(operate_powers, 'UTF8'))
            END;
    END IF;
END $$;
