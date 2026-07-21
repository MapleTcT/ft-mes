-- Hide the vendor layout's unnamed, data-less custom column. Without a label,
-- GreenDill renders the generated UUID key as a visible table header.

CREATE OR REPLACE FUNCTION public.adp_qcs_hide_empty_manu_inspect_column(target jsonb)
RETURNS jsonb
LANGUAGE plpgsql
IMMUTABLE
AS $$
DECLARE
    patched jsonb;
BEGIN
    IF target IS NULL THEN
        RETURN NULL;
    END IF;

    IF jsonb_typeof(target) = 'object' THEN
        IF target ->> 'code' = 'QCS_5.0.0.0_inspect_manuInspectList_LISTPT_QCS_5_0_0_0_inspect_manuInspectList_LISTPT_CUSTOM_3aaefcb6_6d96_4d9d_b2c6_bde20b55ed9d'
           OR target ->> 'key' = 'QCS_5_0_0_0_inspect_manuInspectList_LISTPT_CUSTOM_3aaefcb6_6d96_4d9d_b2c6_bde20b55ed9d' THEN
            RETURN target || jsonb_build_object('isHidden', true, 'hide', true);
        END IF;

        SELECT jsonb_object_agg(key, public.adp_qcs_hide_empty_manu_inspect_column(value))
          INTO patched
          FROM jsonb_each(target);
        RETURN COALESCE(patched, '{}'::jsonb);
    END IF;

    IF jsonb_typeof(target) = 'array' THEN
        SELECT jsonb_agg(public.adp_qcs_hide_empty_manu_inspect_column(value))
          INTO patched
          FROM jsonb_array_elements(target);
        RETURN COALESCE(patched, '[]'::jsonb);
    END IF;

    RETURN target;
END $$;

DO $do$
DECLARE
    target_code constant text := 'QCS_5.0.0.0_inspect_manuInspectList';
    runtime_json_is_oid boolean;
    ec_json_is_oid boolean;
    current_payload text;
    patched_payload text;
BEGIN
    SELECT udt_name = 'oid'
      INTO runtime_json_is_oid
      FROM information_schema.columns
     WHERE table_schema = 'public'
       AND table_name = 'runtime_extra_view'
       AND column_name = 'view_json';

    SELECT CASE
               WHEN COALESCE(runtime_json_is_oid, false)
                   THEN convert_from(lo_get(view_json::oid), 'UTF8')
               ELSE view_json::text
           END
      INTO current_payload
      FROM public.runtime_extra_view
     WHERE code = target_code;

    IF current_payload IS NULL OR btrim(current_payload) = '' THEN
        RAISE NOTICE 'runtime_extra_view % is missing; skip empty-column patch', target_code;
    ELSE
        patched_payload := public.adp_qcs_hide_empty_manu_inspect_column(current_payload::jsonb)::text;
        IF COALESCE(runtime_json_is_oid, false) THEN
            UPDATE public.runtime_extra_view
               SET view_json = lo_from_bytea(0, convert_to(patched_payload, 'UTF8'))
             WHERE code = target_code;
        ELSE
            UPDATE public.runtime_extra_view
               SET view_json = patched_payload
             WHERE code = target_code;
        END IF;
    END IF;

    IF EXISTS (
        SELECT 1
          FROM information_schema.tables
         WHERE table_schema = 'public'
           AND table_name = 'ec_extra_view'
    ) THEN
        SELECT udt_name = 'oid'
          INTO ec_json_is_oid
          FROM information_schema.columns
         WHERE table_schema = 'public'
           AND table_name = 'ec_extra_view'
           AND column_name = 'view_json';

        SELECT CASE
                   WHEN COALESCE(ec_json_is_oid, false)
                       THEN convert_from(lo_get(view_json::oid), 'UTF8')
                   ELSE view_json::text
               END
          INTO current_payload
          FROM public.ec_extra_view
         WHERE code = target_code;

        IF current_payload IS NOT NULL AND btrim(current_payload) <> '' THEN
            patched_payload := public.adp_qcs_hide_empty_manu_inspect_column(current_payload::jsonb)::text;
            IF COALESCE(ec_json_is_oid, false) THEN
                UPDATE public.ec_extra_view
                   SET view_json = lo_from_bytea(0, convert_to(patched_payload, 'UTF8'))
                 WHERE code = target_code;
            ELSE
                UPDATE public.ec_extra_view
                   SET view_json = patched_payload
                 WHERE code = target_code;
            END IF;
        END IF;
    END IF;
END $do$;

DROP FUNCTION IF EXISTS public.adp_qcs_hide_empty_manu_inspect_column(jsonb);
