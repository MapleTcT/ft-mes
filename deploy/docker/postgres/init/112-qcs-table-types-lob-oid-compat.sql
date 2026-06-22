-- PostgreSQL large-object compatibility for QCS table-type code-rule fields.
--
-- QCS maps QCSTableType.*CodeRule as @Lob String. PostgreSQL/Hibernate reads
-- such fields through large-object OIDs; storing the JSON rule inline as text
-- makes the JDBC driver attempt to parse the JSON as a long OID:
--   Bad value for type long: {"config": ...}
--
-- Keep text backups so the migration remains inspectable and reversible.

ALTER TABLE public.qcs_table_types
    ADD COLUMN IF NOT EXISTS insp_code_rule_text_backup text,
    ADD COLUMN IF NOT EXISTS report_code_rule_text_backup text,
    ADD COLUMN IF NOT EXISTS un_qlf_code_rule_text_backup text,
    ADD COLUMN IF NOT EXISTS release_code_rule_text_backup text;

DO $$
DECLARE
    lob_column record;
    current_udt text;
BEGIN
    FOR lob_column IN
        SELECT *
        FROM (
            VALUES
                ('insp_code_rule', 'insp_code_rule_text_backup'),
                ('report_code_rule', 'report_code_rule_text_backup'),
                ('un_qlf_code_rule', 'un_qlf_code_rule_text_backup'),
                ('release_code_rule', 'release_code_rule_text_backup')
        ) AS columns(name, backup_name)
    LOOP
        SELECT udt_name
          INTO current_udt
          FROM information_schema.columns
         WHERE table_schema = 'public'
           AND table_name = 'qcs_table_types'
           AND column_name = lob_column.name;

        IF current_udt IN ('text', 'varchar') THEN
            EXECUTE format(
                'UPDATE public.qcs_table_types SET %1$I = %2$I WHERE %2$I IS NOT NULL AND %1$I IS NULL',
                lob_column.backup_name,
                lob_column.name
            );

            EXECUTE format(
                'ALTER TABLE public.qcs_table_types ALTER COLUMN %1$I TYPE oid USING CASE WHEN %1$I IS NULL OR %1$I = '''' THEN NULL ELSE lo_from_bytea(0, convert_to(%1$I::text, ''UTF8'')) END',
                lob_column.name
            );
        END IF;
    END LOOP;
END $$;
