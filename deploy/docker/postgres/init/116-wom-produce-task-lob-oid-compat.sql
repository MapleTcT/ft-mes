-- PostgreSQL large-object compatibility for WOM production task remarks.
--
-- WOMProduceTask.remark and WOMProdTaskExelog.remark are both mapped as
-- @Lob String. Hibernate reads them through ResultSet.getClob(), which requires
-- PostgreSQL large-object OIDs rather than inline text.
--
-- Keep text backups so marker rows remain inspectable and the migration is
-- reversible during test-environment recovery.

DO $$
DECLARE
    lob_column record;
    current_udt text;
BEGIN
    FOR lob_column IN
        SELECT *
        FROM (
            VALUES
                ('wom_produce_tasks', 'remark', 'remark_text_backup'),
                ('wom_produce_task_exelog', 'remark', 'remark_text_backup')
        ) AS columns(table_name, column_name, backup_name)
    LOOP
        EXECUTE format(
            'ALTER TABLE public.%I ADD COLUMN IF NOT EXISTS %I text',
            lob_column.table_name,
            lob_column.backup_name
        );

        SELECT udt_name
          INTO current_udt
          FROM information_schema.columns
         WHERE table_schema = 'public'
           AND table_name = lob_column.table_name
           AND column_name = lob_column.column_name;

        IF current_udt IN ('text', 'varchar') THEN
            EXECUTE format(
                'UPDATE public.%1$I SET %3$I = %2$I WHERE %2$I IS NOT NULL AND %3$I IS NULL',
                lob_column.table_name,
                lob_column.column_name,
                lob_column.backup_name
            );

            EXECUTE format(
                'ALTER TABLE public.%1$I ALTER COLUMN %2$I TYPE oid USING CASE WHEN %2$I IS NULL OR %2$I = '''' THEN NULL ELSE lo_from_bytea(0, convert_to(%2$I::text, ''UTF8'')) END',
                lob_column.table_name,
                lob_column.column_name
            );
        END IF;
    END LOOP;
END $$;
