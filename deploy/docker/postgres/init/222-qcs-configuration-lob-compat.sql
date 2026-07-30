-- Make QCS entity configuration metadata readable by Hibernate on PostgreSQL.
--
-- ExtraView.fullConfig is mapped with @Lob, so the PostgreSQL JDBC driver
-- expects an OID. The recovered QCS design and project-draft rows contain an
-- empty text value instead, which intermittently breaks /ec/entity/wf while
-- opening the inspection workflow configuration page.

BEGIN;

CREATE OR REPLACE FUNCTION public.adp_qcs_is_large_object_ref(
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

DO $do$
DECLARE
    target_table text;
    item record;
    payload_oid oid;
    invalid_count bigint;
BEGIN
    FOREACH target_table IN ARRAY ARRAY['ec_extra_view', 'project_extra_view']
    LOOP
        IF to_regclass('public.' || target_table) IS NULL THEN
            CONTINUE;
        END IF;

        FOR item IN EXECUTE format(
            'SELECT ctid, full_config::text AS payload
               FROM public.%1$I
              WHERE (
                    position(''QCS_5.0.0.0_'' in code) = 1
                 OR position(''QCS_5.0.0.0_'' in view_code) = 1
              )
                AND full_config IS NOT NULL',
            target_table
        )
        LOOP
            IF public.adp_qcs_is_large_object_ref(item.payload) THEN
                CONTINUE;
            END IF;

            payload_oid := lo_from_bytea(0, convert_to(item.payload, 'UTF8'));
            EXECUTE format(
                'UPDATE public.%1$I
                    SET full_config = $1::text
                  WHERE ctid = $2',
                target_table
            )
            USING payload_oid, item.ctid;
        END LOOP;

        EXECUTE format(
            'SELECT count(*)
               FROM public.%1$I
              WHERE (
                    position(''QCS_5.0.0.0_'' in code) = 1
                 OR position(''QCS_5.0.0.0_'' in view_code) = 1
              )
                AND full_config IS NOT NULL
                AND NOT public.adp_qcs_is_large_object_ref(full_config::text)',
            target_table
        )
        INTO invalid_count;

        IF invalid_count > 0 THEN
            RAISE EXCEPTION
                'QCS full_config LOB conversion incomplete for %: % invalid values',
                target_table,
                invalid_count;
        END IF;
    END LOOP;
END $do$;

DROP FUNCTION IF EXISTS public.adp_qcs_is_large_object_ref(text);

COMMIT;
