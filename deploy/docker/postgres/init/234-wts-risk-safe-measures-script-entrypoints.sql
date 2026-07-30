-- Align the recovered risk/safety-measures action views with the JavaScript
-- entry points delivered by the WTS web package. The package exposes onLoad
-- and pageOnsave; older metadata still calls onLoad_InitParam and onSave.

BEGIN;

DO $patch_action_views$
DECLARE
    item record;
    payload text;
    patched_payload text;
    payload_oid oid;
BEGIN
    FOR item IN
        SELECT ctid, code, view_json
        FROM public.runtime_extra_view
        WHERE code IN (
            'WTS_1.0.0_riskSafeMeasures_riskSafeyEdit',
            'WTS_1.0.0_riskSafeMeasures_riskSafeyView'
        )
          AND view_json IS NOT NULL
    LOOP
        payload := convert_from(lo_get(item.view_json), 'UTF8');
        patched_payload := payload;

        IF item.code = 'WTS_1.0.0_riskSafeMeasures_riskSafeyEdit' THEN
            patched_payload := replace(
                patched_payload,
                'onLoad_InitParam();',
                'if (typeof onLoad === \"function\") { onLoad(); }'
            );
            patched_payload := replace(
                patched_payload,
                'onSave()',
                'pageOnsave()'
            );
        ELSE
            patched_payload := replace(
                patched_payload,
                'onLoad_InitParam();',
                'if (typeof onLoad_InitParam === \"function\") { onLoad_InitParam(); }'
            );
            patched_payload := replace(
                patched_payload,
                'if(onSave() === false)',
                'if(typeof onSave === \"function\" && onSave() === false)'
            );
            patched_payload := replace(
                patched_payload,
                'if (onSave() === false)',
                'if (typeof onSave === \"function\" && onSave() === false)'
            );
        END IF;

        IF patched_payload IS DISTINCT FROM payload THEN
            payload_oid := lo_from_bytea(
                0,
                convert_to(patched_payload, 'UTF8')
            );
            UPDATE public.runtime_extra_view
            SET view_json = payload_oid
            WHERE ctid = item.ctid;
        END IF;
    END LOOP;

    FOR item IN
        SELECT ctid, code, view_json
        FROM public.ec_extra_view
        WHERE code IN (
            'WTS_1.0.0_riskSafeMeasures_riskSafeyEdit',
            'WTS_1.0.0_riskSafeMeasures_riskSafeyView'
        )
          AND view_json ~ '^[0-9]+$'
          AND EXISTS (
              SELECT 1
              FROM pg_largeobject_metadata
              WHERE oid = view_json::oid
          )
    LOOP
        payload := convert_from(lo_get(item.view_json::oid), 'UTF8');
        patched_payload := payload;

        IF item.code = 'WTS_1.0.0_riskSafeMeasures_riskSafeyEdit' THEN
            patched_payload := replace(
                patched_payload,
                'onLoad_InitParam();',
                'if (typeof onLoad === \"function\") { onLoad(); }'
            );
            patched_payload := replace(
                patched_payload,
                'onSave()',
                'pageOnsave()'
            );
        ELSE
            patched_payload := replace(
                patched_payload,
                'onLoad_InitParam();',
                'if (typeof onLoad_InitParam === \"function\") { onLoad_InitParam(); }'
            );
            patched_payload := replace(
                patched_payload,
                'if(onSave() === false)',
                'if(typeof onSave === \"function\" && onSave() === false)'
            );
            patched_payload := replace(
                patched_payload,
                'if (onSave() === false)',
                'if (typeof onSave === \"function\" && onSave() === false)'
            );
        END IF;

        IF patched_payload IS DISTINCT FROM payload THEN
            payload_oid := lo_from_bytea(
                0,
                convert_to(patched_payload, 'UTF8')
            );
            UPDATE public.ec_extra_view
            SET view_json = payload_oid::text
            WHERE ctid = item.ctid;
        END IF;
    END LOOP;
END $patch_action_views$;

DO $validate_entrypoints$
DECLARE
    runtime_valid_count integer;
    product_valid_count integer;
BEGIN
    SELECT count(*)
    INTO runtime_valid_count
    FROM public.runtime_extra_view target
    CROSS JOIN LATERAL (
        SELECT convert_from(lo_get(target.view_json), 'UTF8') AS payload
    ) decoded
    CROSS JOIN LATERAL (
        SELECT decoded.payload::jsonb AS payload_json
    ) parsed
    WHERE (
        target.code = 'WTS_1.0.0_riskSafeMeasures_riskSafeyEdit'
        AND jsonb_typeof(parsed.payload_json) = 'object'
        AND strpos(
            decoded.payload,
            'typeof onLoad === \"function\"'
        ) > 0
        AND strpos(decoded.payload, 'pageOnsave()') > 0
        AND decoded.payload NOT LIKE '%onLoad_InitParam();%'
    )
       OR (
        target.code = 'WTS_1.0.0_riskSafeMeasures_riskSafeyView'
        AND jsonb_typeof(parsed.payload_json) = 'object'
        AND strpos(
            decoded.payload,
            'typeof onLoad_InitParam === \"function\"'
        ) > 0
        AND strpos(
            decoded.payload,
            'typeof onSave === \"function\"'
        ) > 0
    );

    SELECT count(*)
    INTO product_valid_count
    FROM public.ec_extra_view target
    CROSS JOIN LATERAL (
        SELECT convert_from(lo_get(target.view_json::oid), 'UTF8') AS payload
    ) decoded
    CROSS JOIN LATERAL (
        SELECT decoded.payload::jsonb AS payload_json
    ) parsed
    WHERE target.view_json ~ '^[0-9]+$'
      AND EXISTS (
          SELECT 1
          FROM pg_largeobject_metadata
          WHERE oid = target.view_json::oid
      )
      AND (
          (
              target.code = 'WTS_1.0.0_riskSafeMeasures_riskSafeyEdit'
              AND jsonb_typeof(parsed.payload_json) = 'object'
              AND strpos(
                  decoded.payload,
                  'typeof onLoad === \"function\"'
              ) > 0
              AND strpos(decoded.payload, 'pageOnsave()') > 0
              AND decoded.payload NOT LIKE '%onLoad_InitParam();%'
          )
          OR (
              target.code = 'WTS_1.0.0_riskSafeMeasures_riskSafeyView'
              AND jsonb_typeof(parsed.payload_json) = 'object'
              AND strpos(
                  decoded.payload,
                  'typeof onLoad_InitParam === \"function\"'
              ) > 0
              AND strpos(
                  decoded.payload,
                  'typeof onSave === \"function\"'
              ) > 0
          )
      );

    IF runtime_valid_count <> 2 OR product_valid_count <> 2 THEN
        RAISE EXCEPTION
            'WTS risk/safety entry-point repair incomplete: runtime %, product %',
            runtime_valid_count,
            product_valid_count;
    END IF;
END $validate_entrypoints$;

COMMIT;
