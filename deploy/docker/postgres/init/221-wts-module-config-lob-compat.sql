-- Convert recovered WTS design metadata to PostgreSQL large-object references.
--
-- The legacy configuration service maps these columns with Hibernate @Lob.
-- PostgreSQL's JDBC driver therefore expects an OID, while the recovered
-- ec_* and project_* tables still contain the original Oracle CLOB text. The
-- mismatch makes entity configuration pages fail with "Bad value for type
-- long", most visibly when opening Menu Information for approval and safety
-- entities.
--
-- Migration 220 fixed the workPermit entity while restoring the first WTS
-- configuration path. This migration applies the same compatibility rule to
-- the complete WTS module and to both design and project-draft metadata.

BEGIN;

CREATE OR REPLACE FUNCTION public.adp_is_large_object_ref(
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

CREATE OR REPLACE FUNCTION public.adp_convert_module_text_lob_to_oid_ref(
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
        IF public.adp_is_large_object_ref(item.payload) THEN
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
                    'extra_query_json',
                    'query_config',
                    $$position('WTS_1.0.0_' in view_code) = 1$$
                ),
                (
                    'fast_query_json',
                    'query_config',
                    $$position('WTS_1.0.0_' in view_code) = 1$$
                ),
                (
                    'adv_query_json',
                    'query_config',
                    $$position('WTS_1.0.0_' in view_code) = 1$$
                ),
                (
                    'extra_view',
                    'config',
                    $$position('WTS_1.0.0_' in code) = 1
                      OR position('WTS_1.0.0_' in view_code) = 1$$
                ),
                (
                    'extra_view',
                    'full_config',
                    $$position('WTS_1.0.0_' in code) = 1
                      OR position('WTS_1.0.0_' in view_code) = 1$$
                ),
                (
                    'extra_view',
                    'view_json',
                    $$position('WTS_1.0.0_' in code) = 1
                      OR position('WTS_1.0.0_' in view_code) = 1$$
                ),
                (
                    'data_grid',
                    'config',
                    $$position('WTS_1.0.0_' in entity_code) = 1
                      OR position('WTS_1.0.0_' in view_code) = 1$$
                ),
                (
                    'data_grid',
                    'full_config',
                    $$position('WTS_1.0.0_' in entity_code) = 1
                      OR position('WTS_1.0.0_' in view_code) = 1$$
                ),
                (
                    'data_grid',
                    'data_grid_json',
                    $$position('WTS_1.0.0_' in entity_code) = 1
                      OR position('WTS_1.0.0_' in view_code) = 1$$
                ),
                (
                    'field',
                    'config',
                    $$position('WTS_1.0.0_' in entity_code) = 1
                      OR position('WTS_1.0.0_' in view_code) = 1$$
                ),
                (
                    'button',
                    'config',
                    $$position('WTS_1.0.0_' in entity_code) = 1
                      OR position('WTS_1.0.0_' in view_code) = 1$$
                ),
                (
                    'event',
                    'event_function',
                    $$position('WTS_1.0.0_' in entity_code) = 1
                      OR position('WTS_1.0.0_' in code) = 1$$
                ),
                (
                    'event',
                    'event_function_es5',
                    $$position('WTS_1.0.0_' in entity_code) = 1
                      OR position('WTS_1.0.0_' in code) = 1$$
                ),
                (
                    'backup_view',
                    'config',
                    $$position('WTS_1.0.0_' in view_code) = 1$$
                ),
                (
                    'backup_view',
                    'field_config',
                    $$position('WTS_1.0.0_' in view_code) = 1$$
                ),
                (
                    'backup_data_grid',
                    'config',
                    $$position('WTS_1.0.0_' in view_code) = 1
                      OR position('WTS_1.0.0_' in targetmodel_code) = 1$$
                ),
                (
                    'backup_data_grid',
                    'dg_field_config',
                    $$position('WTS_1.0.0_' in view_code) = 1
                      OR position('WTS_1.0.0_' in targetmodel_code) = 1$$
                ),
                (
                    'property',
                    'attributes',
                    $$position('WTS_1.0.0_' in entity_code) = 1$$
                ),
                (
                    'property',
                    'fillcontent',
                    $$position('WTS_1.0.0_' in entity_code) = 1$$
                ),
                (
                    'model',
                    'specialper_template_sql',
                    $$position('WTS_1.0.0_' in entity_code) = 1$$
                ),
                (
                    'model',
                    'model_sql',
                    $$position('WTS_1.0.0_' in entity_code) = 1$$
                ),
                (
                    'model',
                    'view_sql',
                    $$position('WTS_1.0.0_' in entity_code) = 1$$
                ),
                (
                    'validate',
                    'params',
                    $$position('WTS_1.0.0_' in entity_code) = 1$$
                ),
                (
                    'print_template',
                    'template',
                    $$position('WTS_1.0.0_' in entity_code) = 1
                      OR position('WTS_1.0.0_' in view_code) = 1$$
                )
        ) AS mappings(table_suffix, target_column, target_predicate)
    LOOP
        FOREACH table_prefix IN ARRAY ARRAY['ec_', 'project_']
        LOOP
            target_table := table_prefix || item.table_suffix;

            PERFORM public.adp_convert_module_text_lob_to_oid_ref(
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
                        AND NOT public.adp_is_large_object_ref(%3$I::text)',
                    target_table,
                    item.target_predicate,
                    item.target_column
                )
                INTO invalid_count;

                IF invalid_count > 0 THEN
                    RAISE EXCEPTION
                        'WTS LOB conversion incomplete for %.%: % invalid values',
                        target_table,
                        item.target_column,
                        invalid_count;
                END IF;
            END IF;
        END LOOP;
    END LOOP;
END $do$;

DROP FUNCTION IF EXISTS public.adp_convert_module_text_lob_to_oid_ref(
    text,
    text,
    text
);
DROP FUNCTION IF EXISTS public.adp_is_large_object_ref(text);

COMMIT;
