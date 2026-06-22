-- Runtime export compatibility for WTS work permit list.
--
-- SourceAudit target hook: WTS_1.0.0_workPermit_workPermitList exportExcel 导出 /msService/WTS/workPermit/workPermit/workPermitList-query.
--
-- The recovered WTS list can query/export through the backend when
-- exportFlag=true, but the runtime view/datagrid JSON still lacks a visible
-- export action. Keep this patch scoped to the work-permit list and append the
-- export capability without removing the add/edit buttons restored by earlier
-- runtime patches.

CREATE OR REPLACE FUNCTION public.adp_append_datagrid_export_button(
    target jsonb,
    grid_code text,
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
    IF jsonb_typeof(target) = 'object' THEN
        IF target->>'DataGridCode' = grid_code
           OR target->>'dataGridName' = grid_code
           OR target->>'datagridName' = grid_code
           OR target->>'code' = grid_code
           OR target->>'idPrefix' = 'compat_' || grid_code
           OR target#>>'{config,DataGridCode}' = grid_code THEN
            target := target || jsonb_build_object(
                'exportExcel', true,
                'isExportExcel', true,
                'downloadXls', '/msService/WTS/workPermit/workPermit/downloadXls',
                'dataUrl', '/msService/WTS/workPermit/workPermit/workPermitList-query'
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
                 OR COALESCE(button_value->>'buttonoperationcode', button_value->>'CODE', '') = 'workPermitList_export_exportExcel_WTS_1.0.0_workPermit_workPermitList'
                 OR COALESCE(button_value->>'showname', button_value->>'NAME', button_value->>'namekey', '') LIKE '%导出%'
             );

            target := jsonb_set(target, '{buttons}', kept_buttons || jsonb_build_array(export_button), true);
        END IF;

        patched := '{}'::jsonb;
        FOR item_key, item_value IN SELECT * FROM jsonb_each(target)
        LOOP
            patched := patched || jsonb_build_object(
                item_key,
                public.adp_append_datagrid_export_button(item_value, grid_code, export_button)
            );
        END LOOP;
        RETURN patched;
    END IF;

    IF jsonb_typeof(target) = 'array' THEN
        SELECT jsonb_agg(public.adp_append_datagrid_export_button(value, grid_code, export_button))
          INTO patched
          FROM jsonb_array_elements(target);
        RETURN COALESCE(patched, '[]'::jsonb);
    END IF;

    RETURN target;
END $$;

DO $do$
DECLARE
    target_view_code constant text := 'WTS_1.0.0_workPermit_workPermitList';
    export_func constant text := $js$
function(event) {
  if (event && event.preventDefault) {
    event.preventDefault();
  }
  var payload = {
    classifyCodes: "",
    customCondition: {},
    permissionCode: "WTS_1.0.0_workPermit_workPermitList",
    pageNo: 1,
    paging: true,
    pageSize: 20,
    crossCompanyFlag: "true",
    exportFlag: true,
    exportAuxiliaryModelFlag: false,
    useForImportFlag: false,
    properties: [],
    datagridCode: "WTS_1.0.0_workPermit_workPermitList",
    viewCode: "WTS_1.0.0_workPermit_workPermitList"
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
  xhr.open("POST", "/msService/WTS/workPermit/workPermit/workPermitList-query", true);
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
      link.download = "WTS_workPermitList.xls";
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
    console.error("WTS work permit export failed: HTTP " + xhr.status);
  };
  xhr.onerror = function() {
    console.error("WTS work permit export failed: network error");
  };
  xhr.send(JSON.stringify(payload));
}
$js$;
    export_button constant jsonb := jsonb_build_object(
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
        'cellCode', 'cell_adp_wts_workpermit_export',
        'buttonoperationcode', 'workPermitList_export_exportExcel_WTS_1.0.0_workPermit_workPermitList',
        'CODE', 'workPermitList_export_exportExcel_WTS_1.0.0_workPermit_workPermitList',
        'NAME', '导出',
        'ICONCLS', 'cui-btn-export',
        'USEINMORE', 'false',
        'SEPARATENUM', '0',
        'url', '/msService/WTS/workPermit/workPermit/workPermitList-query',
        'downloadXls', '/msService/WTS/workPermit/workPermit/downloadXls'
    );
    item record;
    current_payload text;
    patched_payload text;
BEGIN
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
            patched_payload := public.adp_append_datagrid_export_button(current_payload::jsonb, target_view_code, export_button)::text;
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
            patched_payload := public.adp_append_datagrid_export_button(current_payload::jsonb, target_view_code, export_button)::text;
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
    END LOOP;

    IF EXISTS (
        SELECT 1
          FROM information_schema.tables
         WHERE table_schema = 'public'
           AND table_name = 'ec_data_grid'
    ) THEN
        FOR item IN
            SELECT column_name
              FROM information_schema.columns
             WHERE table_schema = 'public'
               AND table_name = 'ec_data_grid'
               AND column_name IN ('data_grid_json', 'full_config', 'config')
        LOOP
            EXECUTE format(
                'SELECT %I::text FROM public.ec_data_grid WHERE code = $1 OR view_code = $1 LIMIT 1',
                item.column_name
            )
            INTO current_payload
            USING target_view_code;

            IF current_payload IS NOT NULL AND current_payload ~ '^\s*[\{\[]' THEN
                patched_payload := public.adp_append_datagrid_export_button(current_payload::jsonb, target_view_code, export_button)::text;
                EXECUTE format(
                    'UPDATE public.ec_data_grid SET %I = $1 WHERE code = $2 OR view_code = $2',
                    item.column_name
                )
                USING patched_payload, target_view_code;
            END IF;
        END LOOP;
    END IF;
END $do$;
