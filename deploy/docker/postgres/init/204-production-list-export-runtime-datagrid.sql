-- Runtime export metadata for production list pages.
--
-- SourceAudit target hooks:
--   WOM_1.0.0_produceTask_makeTaskList exportExcel 导出 /msService/WOM/produceTask/produceTask/makeTaskList-query
--   RM_1.0.0_formula_batchFormulaList exportExcel 导出 /msService/RM/formula/formula/batchFormulaList-query
--   QCS_5.0.0.0_inspectReport_manuInspReportList exportExcel 导出 /msService/QCS/inspectReport/inspectReport/manuInspReportList-query
--   QCS_5.0.0.0_unQlfDeal_manuUnQlfDealList exportExcel 导出 /msService/QCS/unQlfDeal/unQlfDeal/manuUnQlfDealList-query
--   QCS_5.0.0.0_inspectRelease_manuInspReleaseList exportExcel 导出 /msService/QCS/inspectRelease/inspectRelease/manuInspReleaseList-query
--
-- The recovered runtime_extra_view rows contain layoutDatagrid definitions, but
-- the corresponding runtime_data_grid/ec_data_grid rows are missing. Export
-- requests with exportFlag=true look up the datagrid by code inside the module
-- service and currently fail with NullPointerException after
-- getDatagridByCode/getViewByDataGridCode misses. This patch rebuilds the
-- missing datagrid rows from the existing layout JSON and appends a normal
-- visible export button that posts to the target list query endpoint.

CREATE OR REPLACE FUNCTION public.adp_find_layout_datagrid(
    target jsonb,
    grid_code text
) RETURNS jsonb
LANGUAGE plpgsql
AS $$
DECLARE
    item_key text;
    item_value jsonb;
    found jsonb;
BEGIN
    IF target IS NULL THEN
        RETURN NULL;
    END IF;

    IF jsonb_typeof(target) = 'object' THEN
        IF target->>'DataGridCode' = grid_code
           OR target->>'dataGridName' = grid_code
           OR target->>'datagridName' = grid_code
           OR target->>'code' = grid_code
           OR target->>'viewCode' = grid_code
           OR target->>'idPrefix' = 'compat_' || grid_code
           OR target#>>'{config,DataGridCode}' = grid_code THEN
            RETURN target;
        END IF;

        FOR item_key, item_value IN SELECT * FROM jsonb_each(target)
        LOOP
            found := public.adp_find_layout_datagrid(item_value, grid_code);
            IF found IS NOT NULL THEN
                RETURN found;
            END IF;
        END LOOP;
        RETURN NULL;
    END IF;

    IF jsonb_typeof(target) = 'array' THEN
        FOR item_value IN SELECT value FROM jsonb_array_elements(target)
        LOOP
            found := public.adp_find_layout_datagrid(item_value, grid_code);
            IF found IS NOT NULL THEN
                RETURN found;
            END IF;
        END LOOP;
    END IF;

    RETURN NULL;
END $$;

CREATE OR REPLACE FUNCTION public.adp_append_target_export_button(
    target jsonb,
    grid_code text,
    query_path text,
    download_path text,
    export_button jsonb
) RETURNS jsonb
LANGUAGE plpgsql
AS $$
DECLARE
    item_key text;
    item_value jsonb;
    patched jsonb;
    existing_buttons jsonb;
    kept_buttons jsonb;
BEGIN
    IF target IS NULL THEN
        RETURN target;
    END IF;

    IF jsonb_typeof(target) = 'object' THEN
        IF target->>'DataGridCode' = grid_code
           OR target->>'dataGridName' = grid_code
           OR target->>'datagridName' = grid_code
           OR target->>'code' = grid_code
           OR target->>'viewCode' = grid_code
           OR target->>'idPrefix' = 'compat_' || grid_code
           OR target#>>'{config,DataGridCode}' = grid_code THEN
            target := target || jsonb_build_object(
                'exportExcel', true,
                'isExportExcel', true,
                'downloadXls', download_path,
                'dataUrl', query_path
            );

            IF jsonb_typeof(target->'listProperty') = 'object' THEN
                target := jsonb_set(
                    target,
                    '{listProperty}',
                    (target->'listProperty') || jsonb_build_object('exportExcel', true, 'isExportExcel', true),
                    true
                );
            ELSE
                target := jsonb_set(
                    target,
                    '{listProperty}',
                    jsonb_build_object('exportExcel', true, 'isExportExcel', true),
                    true
                );
            END IF;

            existing_buttons := CASE
                WHEN jsonb_typeof(target->'buttons') = 'array' THEN target->'buttons'
                ELSE '[]'::jsonb
            END;

            SELECT COALESCE(jsonb_agg(button_value), '[]'::jsonb)
              INTO kept_buttons
              FROM jsonb_array_elements(existing_buttons) AS existing(button_value)
             WHERE NOT (
                 lower(COALESCE(button_value->>'id', '')) IN ('export', 'exportexcel')
                 OR lower(COALESCE(button_value->>'buttonstyle', '')) IN ('export', 'exportexcel')
                 OR lower(COALESCE(button_value->>'operateType', button_value->>'operatetype', '')) = 'export'
                 OR COALESCE(button_value->>'showname', button_value->>'NAME', button_value->>'namekey', '') LIKE '%导出%'
             );

            target := jsonb_set(target, '{buttons}', kept_buttons || jsonb_build_array(export_button), true);
        END IF;

        patched := '{}'::jsonb;
        FOR item_key, item_value IN SELECT * FROM jsonb_each(target)
        LOOP
            patched := patched || jsonb_build_object(
                item_key,
                public.adp_append_target_export_button(item_value, grid_code, query_path, download_path, export_button)
            );
        END LOOP;
        RETURN patched;
    END IF;

    IF jsonb_typeof(target) = 'array' THEN
        SELECT jsonb_agg(public.adp_append_target_export_button(value, grid_code, query_path, download_path, export_button))
          INTO patched
          FROM jsonb_array_elements(target);
        RETURN COALESCE(patched, '[]'::jsonb);
    END IF;

    RETURN target;
END $$;

DO $do$
DECLARE
    target record;
    view_payload text;
    patched_view_payload text;
    grid_payload jsonb;
    grid_payload_text text;
    export_func text;
    export_button jsonb;
    view_meta record;
BEGIN
    FOR target IN
        SELECT *
          FROM (VALUES
              (
                'WOM_1.0.0_produceTask_makeTaskList',
                '/msService/WOM/produceTask/produceTask/makeTaskList-query',
                '/msService/WOM/produceTask/produceTask/downloadXls',
                'WOM_makeTaskList.xls'
              ),
              (
                'RM_1.0.0_formula_batchFormulaList',
                '/msService/RM/formula/formula/batchFormulaList-query',
                '/msService/RM/formula/formula/downloadXls',
                'RM_batchFormulaList.xls'
              ),
              (
                'QCS_5.0.0.0_inspectReport_manuInspReportList',
                '/msService/QCS/inspectReport/inspectReport/manuInspReportList-query',
                '/msService/QCS/inspectReport/inspectReport/downloadXls',
                'QCS_manuInspReportList.xls'
              ),
              (
                'QCS_5.0.0.0_unQlfDeal_manuUnQlfDealList',
                '/msService/QCS/unQlfDeal/unQlfDeal/manuUnQlfDealList-query',
                '/msService/QCS/unQlfDeal/unQlfDeal/downloadXls',
                'QCS_manuUnQlfDealList.xls'
              ),
              (
                'QCS_5.0.0.0_inspectRelease_manuInspReleaseList',
                '/msService/QCS/inspectRelease/inspectRelease/manuInspReleaseList-query',
                '/msService/QCS/inspectRelease/inspectRelease/downloadXls',
                'QCS_manuInspReleaseList.xls'
              )
          ) AS t(view_code, query_path, download_path, file_name)
    LOOP
        SELECT rv.module_code,
               rv.entity_code,
               rv.ec_env,
               rv.data_grid_type,
               rv.name,
               rv.title,
               COALESCE(NULLIF(rv.permission_code, ''), target.view_code) AS permission_code
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
            RAISE NOTICE 'skip %, runtime_extra_view payload is missing or not JSON', target.view_code;
            CONTINUE;
        END IF;

        export_func := format($js$
function(event) {
  if (event && event.preventDefault) {
    event.preventDefault();
  }
  var payload = {
    classifyCodes: "",
    customCondition: {},
    permissionCode: %L,
    pageNo: 1,
    paging: true,
    pageSize: 20,
    crossCompanyFlag: "true",
    exportFlag: true,
    exportAuxiliaryModelFlag: false,
    useForImportFlag: false,
    properties: [],
    datagridCode: %L,
    viewCode: %L
  };
  var token = "";
  try {
    token =
      window.localStorage.getItem("suposTicket") ||
      window.localStorage.getItem("SUPOS_TICKET") ||
      window.localStorage.getItem("token") ||
      window.sessionStorage.getItem("suposTicket") ||
      window.sessionStorage.getItem("SUPOS_TICKET") ||
      window.sessionStorage.getItem("token") ||
      "";
  } catch (ignore) {}
  var xhr = new XMLHttpRequest();
  xhr.open("POST", %L, true);
  xhr.responseType = "blob";
  xhr.setRequestHeader("Accept", "*/*");
  xhr.setRequestHeader("Content-Type", "application/json;charset=UTF-8");
  if (token) {
    xhr.setRequestHeader("Authorization", token.indexOf("Bearer ") === 0 ? token : "Bearer " + token);
  }
  xhr.onload = function() {
    if (xhr.status >= 200 && xhr.status < 300 && xhr.response && xhr.response.size > 0) {
      var blobUrl = window.URL.createObjectURL(xhr.response);
      var link = document.createElement("a");
      link.href = blobUrl;
      link.download = %L;
      link.style.display = "none";
      document.body.appendChild(link);
      link.click();
      window.setTimeout(function() {
        window.URL.revokeObjectURL(blobUrl);
        if (link.parentNode) {
          link.parentNode.removeChild(link);
        }
      }, 1000);
      return;
    }
    console.error("Production list export failed: HTTP " + xhr.status);
  };
  xhr.onerror = function() {
    console.error("Production list export failed: network error");
  };
  xhr.send(JSON.stringify(payload));
}
$js$, target.view_code, target.view_code, target.view_code, target.query_path, target.file_name);

        export_button := jsonb_build_object(
            'id', 'exportExcel',
            'showname', '导出',
            'namekey', '导出',
            'i18nKey', 'rbac.src_common_export',
            'buttonstyle', 'export',
            'operatetype', 'CUSTOM',
            'operateType', 'CUSTOM',
            'isHide', false,
            'ispermission', false,
            'isPublished', true,
            'iscallback', true,
            'iscustomfunc', true,
            'isCustomFunc', true,
            'useInMore', false,
            'isconfirm', false,
            'funcname', 'onclick=function(event)',
            'funcbody', export_func,
            'funcbody_es5', export_func,
            'scriptCode', export_func,
            'events', jsonb_build_array(jsonb_build_object(
                'name', 'onclick',
                'function', export_func,
                'function_es5', export_func
            )),
            'regionType', 'BUTTON',
            'cellCode', 'cell_adp_production_list_export_' || regexp_replace(target.view_code, '[^A-Za-z0-9]+', '_', 'g'),
            'buttonoperationcode', target.view_code || '_export_exportExcel',
            'CODE', target.view_code || '_export_exportExcel',
            'NAME', '导出',
            'ICONCLS', 'cui-btn-export',
            'USEINMORE', 'false',
            'SEPARATENUM', '0',
            'url', target.query_path,
            'downloadXls', target.download_path
        );

        patched_view_payload := public.adp_append_target_export_button(
            view_payload::jsonb,
            target.view_code,
            target.query_path,
            target.download_path,
            export_button
        )::text;

        UPDATE public.runtime_extra_view
           SET view_json = lo_from_bytea(0, convert_to(patched_view_payload, 'UTF8')),
               full_config = lo_from_bytea(0, convert_to(patched_view_payload, 'UTF8')),
               config = lo_from_bytea(0, convert_to(patched_view_payload, 'UTF8'))
         WHERE code = target.view_code OR view_code = target.view_code;

        grid_payload := public.adp_find_layout_datagrid(patched_view_payload::jsonb, target.view_code);
        IF grid_payload IS NULL THEN
            RAISE NOTICE 'skip %, layout datagrid not found after patch', target.view_code;
            CONTINUE;
        END IF;

        grid_payload := grid_payload || jsonb_build_object(
            'code', target.view_code,
            'DataGridCode', target.view_code,
            'dataGridName', target.view_code,
            'datagridName', target.view_code,
            'viewCode', target.view_code,
            'exportExcel', true,
            'isExportExcel', true,
            'downloadXls', target.download_path,
            'dataUrl', target.query_path
        );
        grid_payload := jsonb_set(
            grid_payload,
            '{listProperty}',
            COALESCE(grid_payload->'listProperty', '{}'::jsonb) || jsonb_build_object('exportExcel', true, 'isExportExcel', true),
            true
        );
        grid_payload_text := grid_payload::text;

        IF EXISTS (SELECT 1 FROM public.runtime_data_grid WHERE code = target.view_code OR view_code = target.view_code) THEN
            UPDATE public.runtime_data_grid
               SET ec_env = COALESCE(NULLIF(view_meta.ec_env, ''), 'product'),
                   version = COALESCE(version, 0) + 1,
                   modify_time = CURRENT_TIMESTAMP,
                   valid = true,
                   entity_code = COALESCE(view_meta.entity_code, entity_code),
                   module_code = COALESCE(view_meta.module_code, module_code),
                   data_grid_json = lo_from_bytea(0, convert_to(grid_payload_text, 'UTF8')),
                   full_config = lo_from_bytea(0, convert_to(grid_payload_text, 'UTF8')),
                   config = lo_from_bytea(0, convert_to(grid_payload_text, 'UTF8')),
                   proj_flag = false,
                   operate_name = COALESCE(view_meta.name, target.view_code),
                   permission_code = target.view_code,
                   is_permission = false,
                   data_grid_type = COALESCE(view_meta.data_grid_type, 0),
                   data_grid_name = target.view_code,
                   ex = false,
                   view_code = target.view_code,
                   name = COALESCE(view_meta.title, target.view_code)
             WHERE code = target.view_code OR view_code = target.view_code;
        ELSE
            INSERT INTO public.runtime_data_grid (
                code, ec_env, version, delete_time, modify_time, create_time,
                delete_staff_id, modify_staff_id, create_staff_id, valid,
                entity_code, module_code, data_grid_json, proj_flag, operate_name,
                permission_code, is_permission, data_grid_type, full_config,
                data_grid_name, ex, orgproperty_code, targetmodel_code, config,
                view_code, name
            ) VALUES (
                target.view_code, COALESCE(NULLIF(view_meta.ec_env, ''), 'product'), 0, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
                NULL, NULL, NULL, true,
                view_meta.entity_code, view_meta.module_code,
                lo_from_bytea(0, convert_to(grid_payload_text, 'UTF8')),
                false, COALESCE(view_meta.name, target.view_code),
                target.view_code, false, COALESCE(view_meta.data_grid_type, 0),
                lo_from_bytea(0, convert_to(grid_payload_text, 'UTF8')),
                target.view_code, false, NULL, NULL,
                lo_from_bytea(0, convert_to(grid_payload_text, 'UTF8')),
                target.view_code, COALESCE(view_meta.title, target.view_code)
            );
        END IF;

        IF EXISTS (SELECT 1 FROM public.ec_data_grid WHERE code = target.view_code OR view_code = target.view_code) THEN
            UPDATE public.ec_data_grid
               SET ec_env = COALESCE(NULLIF(view_meta.ec_env, ''), 'product'),
                   version = COALESCE(version, 0) + 1,
                   modify_time = CURRENT_TIMESTAMP,
                   valid = 1,
                   entity_code = COALESCE(view_meta.entity_code, entity_code),
                   module_code = COALESCE(view_meta.module_code, module_code),
                   data_grid_json = grid_payload_text,
                   full_config = grid_payload_text,
                   config = grid_payload_text,
                   proj_flag = 0,
                   operate_name = COALESCE(view_meta.name, target.view_code),
                   permission_code = target.view_code,
                   is_permission = 0,
                   data_grid_type = COALESCE(view_meta.data_grid_type, 0),
                   data_grid_name = target.view_code,
                   ex = 0,
                   view_code = target.view_code,
                   name = COALESCE(view_meta.title, target.view_code)
             WHERE code = target.view_code OR view_code = target.view_code;
        ELSE
            INSERT INTO public.ec_data_grid (
                code, ec_env, version, delete_time, modify_time, create_time,
                delete_staff_id, modify_staff_id, create_staff_id, valid,
                entity_code, module_code, data_grid_json, proj_flag, operate_name,
                permission_code, is_permission, data_grid_type, full_config,
                data_grid_name, ex, orgproperty_code, targetmodel_code, config,
                view_code, name
            ) VALUES (
                target.view_code, COALESCE(NULLIF(view_meta.ec_env, ''), 'product'), 0, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
                NULL, NULL, NULL, 1,
                view_meta.entity_code, view_meta.module_code,
                grid_payload_text, 0, COALESCE(view_meta.name, target.view_code),
                target.view_code, 0, COALESCE(view_meta.data_grid_type, 0),
                grid_payload_text, target.view_code, 0, NULL, NULL,
                grid_payload_text, target.view_code, COALESCE(view_meta.title, target.view_code)
            );
        END IF;
    END LOOP;
END $do$;
