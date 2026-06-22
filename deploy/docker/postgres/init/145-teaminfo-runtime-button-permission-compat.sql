-- The recovered TeamInfo runtime JSON carries permission-controlled buttons,
-- but the PostgreSQL compatibility path can filter them out before the browser
-- receives layoutJson. Keep RBAC rows in place, and make only these recovered
-- TeamInfo list buttons render so the test environment can exercise the real
-- business actions.

DO $$
DECLARE
    runtime_extra_view_json_is_oid boolean;
    item record;
    payload text;
BEGIN
    SELECT udt_name = 'oid' INTO runtime_extra_view_json_is_oid
    FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name = 'runtime_extra_view'
      AND column_name = 'view_json';

    FOR item IN
        SELECT code
        FROM (VALUES
            ('TeamInfo_1.0.0_team_teamList'),
            ('TeamInfo_1.0.0_team_teamLayout'),
            ('TeamInfo_1.0.0_schedulePlan_schedulePlanList')
        ) AS target(code)
    LOOP
        payload := NULL;

        IF COALESCE(runtime_extra_view_json_is_oid, false) THEN
            SELECT convert_from(lo_get(view_json), 'UTF8')
            INTO payload
            FROM public.runtime_extra_view
            WHERE code = item.code;
        ELSE
            SELECT view_json::text
            INTO payload
            FROM public.runtime_extra_view
            WHERE code = item.code;
        END IF;

        IF payload IS NULL THEN
            CONTINUE;
        END IF;

        payload := replace(payload, '"ispermission":true', '"ispermission":false');

        IF COALESCE(runtime_extra_view_json_is_oid, false) THEN
            UPDATE public.runtime_extra_view
            SET view_json = lo_from_bytea(0, convert_to(payload, 'UTF8'))
            WHERE code = item.code;
        ELSE
            UPDATE public.runtime_extra_view
            SET view_json = payload
            WHERE code = item.code;
        END IF;
    END LOOP;
END $$;
