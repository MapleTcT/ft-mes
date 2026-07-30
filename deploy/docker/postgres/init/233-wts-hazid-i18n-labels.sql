-- Align the recovered WTS hazard-library labels with the split add/modify UI.
-- The vendor package reused an "编辑" resource for the ADD button and omitted
-- the misspelled legacy "ec.property.dafult" resource used by the default flag.

BEGIN;

WITH label_values(i18n_key, langu_code, i18n_value) AS (
    VALUES
        (
            'WTS.buttonPropertyshowName.randon1573731918669.flag',
            'zh_CN',
            '新增'
        ),
        (
            'WTS.buttonPropertyshowName.randon1573731918669.flag',
            'en_US',
            'Add'
        ),
        ('ec.property.dafult', 'zh_CN', '默认'),
        ('ec.property.dafult', 'zh_HK', '預設'),
        ('ec.property.dafult', 'en_US', 'Default')
)
UPDATE public.supfusion_i18n_resource target
SET i18n_value = source.i18n_value,
    valid = '1',
    modifier = now(),
    modify_time = now(),
    modify_staff_id = 1
FROM label_values source
WHERE target.i18n_key = source.i18n_key
  AND target.langu_code = source.langu_code
  AND COALESCE(target.tenant_id, 'dt') = 'dt';

WITH label_values(i18n_key, langu_code, i18n_value) AS (
    VALUES
        (
            'WTS.buttonPropertyshowName.randon1573731918669.flag',
            'zh_CN',
            '新增'
        ),
        (
            'WTS.buttonPropertyshowName.randon1573731918669.flag',
            'en_US',
            'Add'
        ),
        ('ec.property.dafult', 'zh_CN', '默认'),
        ('ec.property.dafult', 'zh_HK', '預設'),
        ('ec.property.dafult', 'en_US', 'Default')
),
missing_values AS (
    SELECT
        source.*,
        row_number() OVER (
            ORDER BY source.i18n_key, source.langu_code
        ) AS rn
    FROM label_values source
    WHERE NOT EXISTS (
        SELECT 1
        FROM public.supfusion_i18n_resource current_value
        WHERE current_value.i18n_key = source.i18n_key
          AND current_value.langu_code = source.langu_code
          AND COALESCE(current_value.tenant_id, 'dt') = 'dt'
    )
),
id_base AS (
    SELECT COALESCE(max(id), 6579648279134488) AS base_id
    FROM public.supfusion_i18n_resource
)
INSERT INTO public.supfusion_i18n_resource (
    id,
    i18n_key,
    i18n_value,
    langu_code,
    module_code,
    module_version_code,
    valid,
    tenant_id,
    creator,
    create_time,
    create_staff_id,
    modifier,
    modify_time,
    modify_staff_id
)
SELECT
    id_base.base_id + missing.rn,
    missing.i18n_key,
    missing.i18n_value,
    missing.langu_code,
    'WTS',
    'WTS202606130022',
    '1',
    'dt',
    'codex_wts_basic_20260730',
    now(),
    1,
    now(),
    now(),
    1
FROM missing_values missing
CROSS JOIN id_base;

DO $runtime_labels$
DECLARE
    item record;
    payload text;
    patched_payload text;
    payload_oid oid;
BEGIN
    FOR item IN
        SELECT ctid, code, view_json
        FROM public.runtime_extra_view
        WHERE code LIKE 'WTS_1.0.0_hazidLib_%'
          AND view_json IS NOT NULL
    LOOP
        payload := convert_from(lo_get(item.view_json), 'UTF8');
        patched_payload := replace(
            replace(
                replace(
                    payload,
                    '"namekey":"WTS.buttonPropertyshowName.randon1573731918669.flag"',
                    '"namekey":"新增"'
                ),
                '"i18nKey":"WTS.buttonPropertyshowName.randon1573731918669.flag"',
                '"i18nKey":"新增"'
            ),
            '"displayName":"ec.property.dafult"',
            '"displayName":"默认"'
        );
        patched_payload := replace(
            patched_payload,
            '"namekey":"ec.property.dafult"',
            '"namekey":"默认"'
        );

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
        WHERE code LIKE 'WTS_1.0.0_hazidLib_%'
          AND view_json ~ '^[0-9]+$'
          AND EXISTS (
              SELECT 1
              FROM pg_largeobject_metadata
              WHERE oid = view_json::oid
          )
    LOOP
        payload := convert_from(lo_get(item.view_json::oid), 'UTF8');
        patched_payload := replace(
            replace(
                replace(
                    payload,
                    '"namekey":"WTS.buttonPropertyshowName.randon1573731918669.flag"',
                    '"namekey":"新增"'
                ),
                '"i18nKey":"WTS.buttonPropertyshowName.randon1573731918669.flag"',
                '"i18nKey":"新增"'
            ),
            '"displayName":"ec.property.dafult"',
            '"displayName":"默认"'
        );
        patched_payload := replace(
            patched_payload,
            '"namekey":"ec.property.dafult"',
            '"namekey":"默认"'
        );

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
END $runtime_labels$;

DO $validation$
DECLARE
    invalid_count integer;
    invalid_runtime_count integer;
BEGIN
    SELECT count(*)
    INTO invalid_count
    FROM (
        VALUES
            (
                'WTS.buttonPropertyshowName.randon1573731918669.flag',
                'zh_CN',
                '新增'
            ),
            (
                'WTS.buttonPropertyshowName.randon1573731918669.flag',
                'en_US',
                'Add'
            ),
            ('ec.property.dafult', 'zh_CN', '默认'),
            ('ec.property.dafult', 'zh_HK', '預設'),
            ('ec.property.dafult', 'en_US', 'Default')
    ) expected(i18n_key, langu_code, i18n_value)
    WHERE NOT EXISTS (
        SELECT 1
        FROM public.supfusion_i18n_resource actual
        WHERE actual.i18n_key = expected.i18n_key
          AND actual.langu_code = expected.langu_code
          AND actual.i18n_value = expected.i18n_value
          AND COALESCE(actual.tenant_id, 'dt') = 'dt'
          AND actual.valid = '1'
    );

    IF invalid_count > 0 THEN
        RAISE EXCEPTION
            'WTS hazard-library i18n repair incomplete: % labels missing',
            invalid_count;
    END IF;

    SELECT count(*)
    INTO invalid_runtime_count
    FROM public.runtime_extra_view
    WHERE code IN (
        'WTS_1.0.0_hazidLib_hazidLibList',
        'WTS_1.0.0_hazidLib_hazidLibEdit'
    )
      AND (
          strpos(
              convert_from(lo_get(view_json), 'UTF8'),
              'ec.property.dafult'
          ) > 0
          OR (
              code = 'WTS_1.0.0_hazidLib_hazidLibList'
              AND strpos(
                  convert_from(lo_get(view_json), 'UTF8'),
                  '"namekey":"新增"'
              ) = 0
          )
      );

    IF invalid_runtime_count > 0 THEN
        RAISE EXCEPTION
            'WTS hazard-library runtime label repair incomplete: % views invalid',
            invalid_runtime_count;
    END IF;
END $validation$;

COMMIT;
