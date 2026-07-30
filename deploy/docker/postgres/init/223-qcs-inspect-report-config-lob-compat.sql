-- Make the recovered QCS inspection-report designer metadata readable by
-- Hibernate on PostgreSQL.
--
-- The legacy configuration entities map the columns below with @Lob. The
-- PostgreSQL JDBC driver therefore expects a large-object OID, while the
-- recovered inspection-report rows still contain their original CLOB text.
-- Keep the conversion scoped to QCS_5.0.0.0_inspectReport and cover both the
-- published ec_* metadata and the project-draft metadata.

BEGIN;

CREATE OR REPLACE FUNCTION public.adp_qcs_inspect_report_is_lob_ref(
    candidate text
) RETURNS boolean
LANGUAGE plpgsql
STABLE
AS $$
BEGIN
    IF candidate IS NULL OR candidate !~ '^[0-9]+$' THEN
        RETURN false;
    END IF;

    RETURN EXISTS (
        SELECT 1
        FROM pg_largeobject_metadata
        WHERE oid = candidate::oid
    );
EXCEPTION
    WHEN invalid_text_representation OR numeric_value_out_of_range THEN
        RETURN false;
END $$;

CREATE OR REPLACE FUNCTION public.adp_qcs_inspect_report_convert_lob(
    target_table text,
    target_column text,
    target_predicate text
) RETURNS void
LANGUAGE plpgsql
AS $$
DECLARE
    current_udt text;
    item record;
    payload_oid oid;
BEGIN
    SELECT udt_name
    INTO current_udt
    FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name = target_table
      AND column_name = target_column;

    IF current_udt IS NULL OR current_udt NOT IN ('text', 'varchar') THEN
        RETURN;
    END IF;

    FOR item IN EXECUTE format(
        'SELECT ctid, %1$I::text AS payload
           FROM public.%2$I
          WHERE (%3$s)
            AND %1$I IS NOT NULL',
        target_column,
        target_table,
        target_predicate
    )
    LOOP
        IF public.adp_qcs_inspect_report_is_lob_ref(item.payload) THEN
            CONTINUE;
        END IF;

        payload_oid := lo_from_bytea(0, convert_to(item.payload, 'UTF8'));
        EXECUTE format(
            'UPDATE public.%1$I
                SET %2$I = $1::text
              WHERE ctid = $2',
            target_table,
            target_column
        )
        USING payload_oid, item.ctid;
    END LOOP;
END $$;

DO $do$
DECLARE
    item record;
    table_prefix text;
    target_table text;
    invalid_count bigint;
BEGIN
    FOR item IN
        SELECT *
        FROM (
            VALUES
                (
                    'fast_query_json',
                    'query_config',
                    $$position('QCS_5.0.0.0_inspectReport' in view_code) = 1$$
                ),
                (
                    'adv_query_json',
                    'query_config',
                    $$position('QCS_5.0.0.0_inspectReport' in view_code) = 1$$
                ),
                (
                    'extra_view',
                    'full_config',
                    $$position('QCS_5.0.0.0_inspectReport' in code) = 1
                      OR position('QCS_5.0.0.0_inspectReport' in view_code) = 1$$
                ),
                (
                    'data_grid',
                    'config',
                    $$position('QCS_5.0.0.0_inspectReport' in entity_code) = 1
                      OR position('QCS_5.0.0.0_inspectReport' in view_code) = 1$$
                ),
                (
                    'data_grid',
                    'full_config',
                    $$position('QCS_5.0.0.0_inspectReport' in entity_code) = 1
                      OR position('QCS_5.0.0.0_inspectReport' in view_code) = 1$$
                ),
                (
                    'data_grid',
                    'data_grid_json',
                    $$position('QCS_5.0.0.0_inspectReport' in entity_code) = 1
                      OR position('QCS_5.0.0.0_inspectReport' in view_code) = 1$$
                ),
                (
                    'field',
                    'config',
                    $$position('QCS_5.0.0.0_inspectReport' in entity_code) = 1
                      OR position('QCS_5.0.0.0_inspectReport' in view_code) = 1$$
                ),
                (
                    'button',
                    'config',
                    $$position('QCS_5.0.0.0_inspectReport' in entity_code) = 1
                      OR position('QCS_5.0.0.0_inspectReport' in view_code) = 1$$
                ),
                (
                    'event',
                    'event_function',
                    $$position('QCS_5.0.0.0_inspectReport' in entity_code) = 1
                      OR position('QCS_5.0.0.0_inspectReport' in code) = 1$$
                ),
                (
                    'event',
                    'event_function_es5',
                    $$position('QCS_5.0.0.0_inspectReport' in entity_code) = 1
                      OR position('QCS_5.0.0.0_inspectReport' in code) = 1$$
                ),
                (
                    'backup_view',
                    'config',
                    $$position('QCS_5.0.0.0_inspectReport' in view_code) = 1$$
                ),
                (
                    'backup_view',
                    'field_config',
                    $$position('QCS_5.0.0.0_inspectReport' in view_code) = 1$$
                ),
                (
                    'backup_data_grid',
                    'config',
                    $$position('QCS_5.0.0.0_inspectReport' in view_code) = 1
                      OR position('QCS_5.0.0.0_inspectReport' in targetmodel_code) = 1$$
                ),
                (
                    'backup_data_grid',
                    'dg_field_config',
                    $$position('QCS_5.0.0.0_inspectReport' in view_code) = 1
                      OR position('QCS_5.0.0.0_inspectReport' in targetmodel_code) = 1$$
                ),
                (
                    'import_template',
                    'value',
                    $$position('QCS_5.0.0.0_inspectReport' in code) = 1$$
                ),
                (
                    'custom_code',
                    'custom_code',
                    $$position('QCS_5.0.0.0_inspectReport' in code) = 1
                      OR position('QCS_5.0.0.0_inspectReport' in entity_code) = 1
                      OR position('QCS_5.0.0.0_inspectReport' in model_code) = 1$$
                )
        ) AS mappings(table_suffix, target_column, target_predicate)
    LOOP
        FOREACH table_prefix IN ARRAY ARRAY['ec_', 'project_']
        LOOP
            target_table := table_prefix || item.table_suffix;

            PERFORM public.adp_qcs_inspect_report_convert_lob(
                target_table,
                item.target_column,
                item.target_predicate
            );

            IF EXISTS (
                SELECT 1
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = target_table
                  AND column_name = item.target_column
            ) THEN
                EXECUTE format(
                    'SELECT count(*)
                       FROM public.%1$I
                      WHERE (%2$s)
                        AND %3$I IS NOT NULL
                        AND NOT public.adp_qcs_inspect_report_is_lob_ref(%3$I::text)',
                    target_table,
                    item.target_predicate,
                    item.target_column
                )
                INTO invalid_count;

                IF invalid_count > 0 THEN
                    RAISE EXCEPTION
                        'QCS inspection-report LOB conversion incomplete for %.%: % invalid values',
                        target_table,
                        item.target_column,
                        invalid_count;
                END IF;
            END IF;
        END LOOP;
    END LOOP;
END $do$;

DROP FUNCTION IF EXISTS public.adp_qcs_inspect_report_convert_lob(
    text,
    text,
    text
);
DROP FUNCTION IF EXISTS public.adp_qcs_inspect_report_is_lob_ref(text);

COMMIT;
