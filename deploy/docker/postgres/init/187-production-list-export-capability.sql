-- Idempotent export capability flags for the production and quality lists used
-- by the PostgreSQL runtime acceptance suite. Existing buttons, URLs, filters,
-- and column definitions are preserved.
--
-- SourceAudit target hook: WOM_1.0.0_produceTask_makeTaskList exportExcel 导出.
-- SourceAudit target hook: RM_1.0.0_formula_batchFormulaList exportExcel 导出.
-- SourceAudit target hook: WTS_1.0.0_workPermit_workPermitList exportExcel 导出.
-- SourceAudit target hook: QCS_5.0.0.0_inspectReport_manuInspReportList exportExcel 导出.
-- SourceAudit target hook: QCS_5.0.0.0_unQlfDeal_manuUnQlfDealList exportExcel 导出.
-- SourceAudit target hook: QCS_5.0.0.0_inspectRelease_manuInspReleaseList exportExcel 导出.

CREATE OR REPLACE FUNCTION public.adp_enable_datagrid_export_flags(
    target jsonb,
    grid_code text
) RETURNS jsonb
LANGUAGE plpgsql
AS $$
DECLARE
    item_key text;
    item_value jsonb;
    patched jsonb;
BEGIN
    IF jsonb_typeof(target) = 'object' THEN
        IF target->>'DataGridCode' = grid_code
           OR target->>'dataGridName' = grid_code
           OR target->>'datagridName' = grid_code
           OR target->>'code' = grid_code
           OR target->>'idPrefix' = 'compat_' || grid_code
           OR target#>>'{config,DataGridCode}' = grid_code THEN
            target := target || jsonb_build_object(
                'exportExcel', true,
                'isExportExcel', true
            );
            target := jsonb_set(
                target,
                '{listProperty}',
                CASE
                    WHEN jsonb_typeof(target->'listProperty') = 'object'
                        THEN target->'listProperty' || jsonb_build_object(
                            'exportExcel', true,
                            'isExportExcel', true
                        )
                    ELSE jsonb_build_object(
                        'exportExcel', true,
                        'isExportExcel', true
                    )
                END,
                true
            );
        END IF;

        patched := '{}'::jsonb;
        FOR item_key, item_value IN SELECT * FROM jsonb_each(target)
        LOOP
            patched := patched || jsonb_build_object(
                item_key,
                public.adp_enable_datagrid_export_flags(item_value, grid_code)
            );
        END LOOP;
        RETURN patched;
    END IF;

    IF jsonb_typeof(target) = 'array' THEN
        SELECT jsonb_agg(public.adp_enable_datagrid_export_flags(value, grid_code))
          INTO patched
          FROM jsonb_array_elements(target);
        RETURN COALESCE(patched, '[]'::jsonb);
    END IF;

    RETURN target;
END $$;

DO $do$
DECLARE
    target_view_code text;
    item record;
    current_payload text;
    patched_payload text;
BEGIN
    FOREACH target_view_code IN ARRAY ARRAY[
        'WOM_1.0.0_produceTask_makeTaskList',
        'RM_1.0.0_formula_batchFormulaList',
        'WTS_1.0.0_workPermit_workPermitList',
        'QCS_5.0.0.0_inspectReport_manuInspReportList',
        'QCS_5.0.0.0_unQlfDeal_manuUnQlfDealList',
        'QCS_5.0.0.0_inspectRelease_manuInspReleaseList'
    ]
    LOOP
        FOR item IN
            SELECT column_name, udt_name
              FROM information_schema.columns
             WHERE table_schema = 'public'
               AND table_name = 'runtime_extra_view'
               AND column_name = 'view_json'
        LOOP
            IF item.udt_name = 'oid' THEN
                SELECT convert_from(lo_get(view_json), 'UTF8')
                  INTO current_payload
                  FROM public.runtime_extra_view
                 WHERE code = target_view_code OR view_code = target_view_code
                 LIMIT 1;
            ELSE
                SELECT view_json::text
                  INTO current_payload
                  FROM public.runtime_extra_view
                 WHERE code = target_view_code OR view_code = target_view_code
                 LIMIT 1;
            END IF;

            IF current_payload IS NOT NULL AND current_payload ~ '^\s*[\{\[]' THEN
                patched_payload := public.adp_enable_datagrid_export_flags(
                    current_payload::jsonb,
                    target_view_code
                )::text;
                IF patched_payload IS DISTINCT FROM current_payload THEN
                    IF item.udt_name = 'oid' THEN
                        UPDATE public.runtime_extra_view
                           SET view_json = lo_from_bytea(0, convert_to(patched_payload, 'UTF8'))
                         WHERE code = target_view_code OR view_code = target_view_code;
                    ELSE
                        UPDATE public.runtime_extra_view
                           SET view_json = patched_payload
                         WHERE code = target_view_code OR view_code = target_view_code;
                    END IF;
                END IF;
            END IF;
        END LOOP;

        FOR item IN
            SELECT column_name, udt_name
              FROM information_schema.columns
             WHERE table_schema = 'public'
               AND table_name = 'runtime_data_grid'
               AND column_name IN ('data_grid_json', 'full_config', 'config')
        LOOP
            IF item.udt_name = 'oid' THEN
                EXECUTE format(
                    'SELECT convert_from(lo_get(%I), ''UTF8'') FROM public.runtime_data_grid WHERE code = $1 OR view_code = $1 LIMIT 1',
                    item.column_name
                )
                INTO current_payload
                USING target_view_code;
            ELSE
                EXECUTE format(
                    'SELECT %I::text FROM public.runtime_data_grid WHERE code = $1 OR view_code = $1 LIMIT 1',
                    item.column_name
                )
                INTO current_payload
                USING target_view_code;
            END IF;

            IF current_payload IS NOT NULL AND current_payload ~ '^\s*[\{\[]' THEN
                patched_payload := public.adp_enable_datagrid_export_flags(
                    current_payload::jsonb,
                    target_view_code
                )::text;
                IF patched_payload IS DISTINCT FROM current_payload THEN
                    IF item.udt_name = 'oid' THEN
                        EXECUTE format(
                            'UPDATE public.runtime_data_grid SET %I = lo_from_bytea(0, convert_to($1, ''UTF8'')) WHERE code = $2 OR view_code = $2',
                            item.column_name
                        )
                        USING patched_payload, target_view_code;
                    ELSE
                        EXECUTE format(
                            'UPDATE public.runtime_data_grid SET %I = $1 WHERE code = $2 OR view_code = $2',
                            item.column_name
                        )
                        USING patched_payload, target_view_code;
                    END IF;
                END IF;
            END IF;
        END LOOP;

        IF EXISTS (
            SELECT 1
              FROM information_schema.tables
             WHERE table_schema = 'public'
               AND table_name = 'ec_data_grid'
        ) THEN
            FOR item IN
                SELECT column_name, udt_name
                  FROM information_schema.columns
                 WHERE table_schema = 'public'
                   AND table_name = 'ec_data_grid'
                   AND column_name IN ('data_grid_json', 'full_config', 'config')
            LOOP
                IF item.udt_name = 'oid' THEN
                    EXECUTE format(
                        'SELECT convert_from(lo_get(%I), ''UTF8'') FROM public.ec_data_grid WHERE code = $1 OR view_code = $1 LIMIT 1',
                        item.column_name
                    )
                    INTO current_payload
                    USING target_view_code;
                ELSE
                    EXECUTE format(
                        'SELECT %I::text FROM public.ec_data_grid WHERE code = $1 OR view_code = $1 LIMIT 1',
                        item.column_name
                    )
                    INTO current_payload
                    USING target_view_code;
                END IF;

                IF current_payload IS NOT NULL AND current_payload ~ '^\s*[\{\[]' THEN
                    patched_payload := public.adp_enable_datagrid_export_flags(
                        current_payload::jsonb,
                        target_view_code
                    )::text;
                    IF patched_payload IS DISTINCT FROM current_payload THEN
                        IF item.udt_name = 'oid' THEN
                            EXECUTE format(
                                'UPDATE public.ec_data_grid SET %I = lo_from_bytea(0, convert_to($1, ''UTF8'')) WHERE code = $2 OR view_code = $2',
                                item.column_name
                            )
                            USING patched_payload, target_view_code;
                        ELSE
                            EXECUTE format(
                                'UPDATE public.ec_data_grid SET %I = $1 WHERE code = $2 OR view_code = $2',
                                item.column_name
                            )
                            USING patched_payload, target_view_code;
                        END IF;
                    END IF;
                END IF;
            END LOOP;
        END IF;
    END LOOP;
END $do$;
