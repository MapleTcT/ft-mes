-- Remove recovered WorkAppointment work-plan picture widgets from runtime JSON.
--
-- In the current Linux/PostgreSQL recovery, the WorkAppointment upload-list
-- controller still crashes while resolving the legacy PICTURE permission block
-- (Long cannot be cast to String).  The picture field is optional for the
-- plan-only workflow acceptance path, so keep the form usable by stripping the
-- PICTURE cells from WorkPlan edit/approve runtime metadata.  This leaves the
-- persistence model column in place for a later full attachment recovery.

CREATE OR REPLACE FUNCTION public.adp_strip_picture_cells(payload jsonb)
RETURNS jsonb
LANGUAGE plpgsql
IMMUTABLE
AS $$
DECLARE
    result jsonb;
    item jsonb;
    key text;
    member_value jsonb;
BEGIN
    IF payload IS NULL THEN
        RETURN NULL;
    END IF;

    IF jsonb_typeof(payload) = 'object' THEN
        IF payload #>> '{element,columnType}' = 'PICTURE' THEN
            RETURN 'null'::jsonb;
        END IF;

        result := '{}'::jsonb;
        FOR key, member_value IN SELECT * FROM jsonb_each(payload) LOOP
            result := result || jsonb_build_object(key, public.adp_strip_picture_cells(member_value));
        END LOOP;
        RETURN result;
    END IF;

    IF jsonb_typeof(payload) = 'array' THEN
        result := '[]'::jsonb;
        FOR item IN SELECT element_value FROM jsonb_array_elements(payload) AS elements(element_value) LOOP
            item := public.adp_strip_picture_cells(item);
            IF item <> 'null'::jsonb THEN
                result := result || jsonb_build_array(item);
            END IF;
        END LOOP;
        RETURN result;
    END IF;

    RETURN payload;
END;
$$;

DO $$
DECLARE
    runtime_extra_view_json_is_oid boolean;
    target_view_code text;
    current_payload text;
    patched_payload text;
BEGIN
    SELECT udt_name = 'oid' INTO runtime_extra_view_json_is_oid
      FROM information_schema.columns
     WHERE table_schema = 'public'
       AND table_name = 'runtime_extra_view'
       AND column_name = 'view_json';

    FOREACH target_view_code IN ARRAY ARRAY[
        'workAppointment_6.1.6.1_workPlan_workPlanEdit',
        'workAppointment_6.1.6.1_workPlan_workPlanApprove'
    ] LOOP
        SELECT CASE
                   WHEN COALESCE(runtime_extra_view_json_is_oid, false)
                       THEN convert_from(lo_get(view_json::oid), 'UTF8')
                   ELSE view_json::text
               END
          INTO current_payload
          FROM public.runtime_extra_view
         WHERE code = target_view_code;

        IF current_payload IS NULL OR btrim(current_payload) = '' THEN
            CONTINUE;
        END IF;

        patched_payload := public.adp_strip_picture_cells(current_payload::jsonb)::text;
        patched_payload := replace(patched_payload, '"isFileView": true', '"isFileView": false');

        IF COALESCE(runtime_extra_view_json_is_oid, false) THEN
            UPDATE public.runtime_extra_view
               SET view_json = lo_from_bytea(0, convert_to(patched_payload, 'UTF8'))
             WHERE code = target_view_code;
        ELSE
            UPDATE public.runtime_extra_view
               SET view_json = patched_payload
             WHERE code = target_view_code;
        END IF;

        UPDATE public.ec_extra_view
           SET view_json = patched_payload
         WHERE code = target_view_code;
    END LOOP;
END $$;
