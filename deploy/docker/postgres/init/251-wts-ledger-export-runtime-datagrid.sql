-- Restore the exact datagrid metadata used by WTS ledger exports.
--
-- Migration 248 publishes the visible export actions. The list query works
-- without runtime_data_grid, but exportFlag=true resolves the submitted
-- datagridCode again inside WTS. These two recovered pages were missing that
-- metadata and therefore returned HTTP 500 even though their lists rendered.

DO $do$
DECLARE
    target record;
    view_payload text;
    grid_payload jsonb;
    grid_payload_text text;
    view_meta record;
BEGIN
    FOR target IN
        SELECT *
          FROM (VALUES
              (
                'WTS_1.0.0_workTicket_workList',
                'WTS_1.0.0_workTicket_workList_workTicket_sdg',
                '/msService/WTS/workTicket/workTicket/workList-query',
                '/msService/WTS/workTicket/workTicket/downloadXls'
              ),
              (
                'WTS_1.0.0_blindPlateAccount_plateAccountList',
                'WTS_1.0.0_blindPlateAccount_plateAccountList',
                '/msService/WTS/blindPlateAccount/plateAccount/plateAccountList-query',
                '/msService/WTS/blindPlateAccount/plateAccount/downloadXls'
              )
          ) AS t(view_code, grid_code, query_path, download_path)
    LOOP
        SELECT rv.module_code,
               rv.entity_code,
               COALESCE(NULLIF(rv.ec_env, ''), 'product') AS ec_env,
               COALESCE(rv.data_grid_type, 0) AS data_grid_type,
               COALESCE(rv.name, target.grid_code) AS operate_name,
               COALESCE(rv.title, target.grid_code) AS title
          INTO view_meta
          FROM public.runtime_view rv
         WHERE rv.code = target.view_code
         LIMIT 1;

        SELECT convert_from(lo_get(rev.view_json), 'UTF8')
          INTO view_payload
          FROM public.runtime_extra_view rev
         WHERE rev.code = target.view_code OR rev.view_code = target.view_code
         LIMIT 1;

        IF view_payload IS NULL OR view_payload !~ '^\s*[\{\[]' THEN
            RAISE EXCEPTION 'WTS export view payload is missing or invalid: %', target.view_code;
        END IF;

        grid_payload := public.adp_find_layout_datagrid(view_payload::jsonb, target.grid_code);
        IF grid_payload IS NULL THEN
            RAISE EXCEPTION 'WTS export datagrid is missing from view %: %', target.view_code, target.grid_code;
        END IF;

        grid_payload := grid_payload || jsonb_build_object(
            'code', target.grid_code,
            'DataGridCode', target.grid_code,
            'dataGridName', target.grid_code,
            'datagridName', target.grid_code,
            'viewCode', target.view_code,
            'exportExcel', true,
            'isExportExcel', true,
            'downloadXls', target.download_path,
            'dataUrl', target.query_path
        );
        grid_payload := jsonb_set(
            grid_payload,
            '{listProperty}',
            COALESCE(grid_payload->'listProperty', '{}'::jsonb)
                || jsonb_build_object('exportExcel', true, 'isExportExcel', true),
            true
        );
        grid_payload_text := grid_payload::text;

        INSERT INTO public.runtime_data_grid (
            code, ec_env, version, delete_time, modify_time, create_time,
            delete_staff_id, modify_staff_id, create_staff_id, valid,
            entity_code, module_code, data_grid_json, proj_flag, operate_name,
            permission_code, is_permission, data_grid_type, full_config,
            data_grid_name, ex, orgproperty_code, targetmodel_code, config,
            view_code, name
        ) VALUES (
            target.grid_code, COALESCE(view_meta.ec_env, 'product'), 0,
            NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
            NULL, NULL, NULL, true,
            view_meta.entity_code, view_meta.module_code,
            lo_from_bytea(0, convert_to(grid_payload_text, 'UTF8')),
            false, view_meta.operate_name, target.view_code, false,
            COALESCE(view_meta.data_grid_type, 0),
            lo_from_bytea(0, convert_to(grid_payload_text, 'UTF8')),
            target.grid_code, false, NULL, NULL,
            lo_from_bytea(0, convert_to(grid_payload_text, 'UTF8')),
            target.view_code, view_meta.title
        )
        ON CONFLICT (code) DO UPDATE SET
            ec_env = EXCLUDED.ec_env,
            version = public.runtime_data_grid.version + 1,
            modify_time = CURRENT_TIMESTAMP,
            valid = true,
            entity_code = EXCLUDED.entity_code,
            module_code = EXCLUDED.module_code,
            data_grid_json = EXCLUDED.data_grid_json,
            proj_flag = false,
            operate_name = EXCLUDED.operate_name,
            permission_code = EXCLUDED.permission_code,
            is_permission = false,
            data_grid_type = EXCLUDED.data_grid_type,
            full_config = EXCLUDED.full_config,
            data_grid_name = EXCLUDED.data_grid_name,
            ex = false,
            config = EXCLUDED.config,
            view_code = EXCLUDED.view_code,
            name = EXCLUDED.name;

        INSERT INTO public.ec_data_grid (
            code, ec_env, version, delete_time, modify_time, create_time,
            delete_staff_id, modify_staff_id, create_staff_id, valid,
            entity_code, module_code, data_grid_json, proj_flag, operate_name,
            permission_code, is_permission, data_grid_type, full_config,
            data_grid_name, ex, orgproperty_code, targetmodel_code, config,
            view_code, name
        ) VALUES (
            target.grid_code, COALESCE(view_meta.ec_env, 'product'), 0,
            NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
            NULL, NULL, NULL, 1,
            view_meta.entity_code, view_meta.module_code, grid_payload_text,
            0, view_meta.operate_name, target.view_code, 0,
            COALESCE(view_meta.data_grid_type, 0), grid_payload_text,
            target.grid_code, 0, NULL, NULL, grid_payload_text,
            target.view_code, view_meta.title
        )
        ON CONFLICT (code) DO UPDATE SET
            ec_env = EXCLUDED.ec_env,
            version = public.ec_data_grid.version + 1,
            modify_time = CURRENT_TIMESTAMP,
            valid = 1,
            entity_code = EXCLUDED.entity_code,
            module_code = EXCLUDED.module_code,
            data_grid_json = EXCLUDED.data_grid_json,
            proj_flag = 0,
            operate_name = EXCLUDED.operate_name,
            permission_code = EXCLUDED.permission_code,
            is_permission = 0,
            data_grid_type = EXCLUDED.data_grid_type,
            full_config = EXCLUDED.full_config,
            data_grid_name = EXCLUDED.data_grid_name,
            ex = 0,
            config = EXCLUDED.config,
            view_code = EXCLUDED.view_code,
            name = EXCLUDED.name;
    END LOOP;
END $do$;
