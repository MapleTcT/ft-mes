-- Restore visible runtime buttons for recovered business list views.
--
-- The business runtime JSON generated during PostgreSQL recovery preserved the
-- list fields and endpoints, but several source buttons were filtered out as
-- hidden. Keep this as a narrow compatibility patch so the test environment can
-- exercise real edit/delete/export flows from the browser instead of calling
-- only backend APIs.

CREATE OR REPLACE FUNCTION public.adp_patch_datagrid_buttons(
    target jsonb,
    grid_code text,
    buttons jsonb
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
           OR target->>'code' = grid_code
           OR target->>'idPrefix' = 'compat_' || grid_code THEN
            target := jsonb_set(target, '{buttons}', buttons, true);
        END IF;

        patched := '{}'::jsonb;
        FOR item_key, item_value IN SELECT * FROM jsonb_each(target)
        LOOP
            patched := patched || jsonb_build_object(
                item_key,
                public.adp_patch_datagrid_buttons(item_value, grid_code, buttons)
            );
        END LOOP;
        RETURN patched;
    END IF;

    IF jsonb_typeof(target) = 'array' THEN
        SELECT jsonb_agg(public.adp_patch_datagrid_buttons(value, grid_code, buttons))
        INTO patched
        FROM jsonb_array_elements(target);
        RETURN COALESCE(patched, '[]'::jsonb);
    END IF;

    RETURN target;
END $$;

CREATE OR REPLACE FUNCTION public.adp_runtime_view_ref(target_code text)
RETURNS jsonb
LANGUAGE sql
AS $$
    SELECT jsonb_build_object(
        'title', rv.title,
        'code', rv.code,
        'name', rv.name,
        'openType', COALESCE(NULLIF(rv.open_type, ''), 'frame'),
        'url', rv.url,
        'iscrosscompany', 'false',
        'mneType', 'other'
    )
    FROM public.runtime_view rv
    WHERE rv.code = target_code
$$;

DO $do$
DECLARE
    view_json_is_oid boolean;
    item record;
    current_payload text;
    patched_payload text;
BEGIN
    SELECT udt_name = 'oid' INTO view_json_is_oid
    FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name = 'runtime_extra_view'
      AND column_name = 'view_json';

    FOR item IN
        SELECT *
        FROM (
            VALUES
            (
                'WTS_1.0.0_workPermit_workPermitList',
                'WTS_1.0.0_workPermit_workPermitList',
                jsonb_build_array(
                    jsonb_build_object(
                        'id', 'add',
                        'showname', '新增',
                        'namekey', '新增',
                        'i18nKey', 'WTS.buttonPropertyshowName.randon1574731372165.flag',
                        'buttonstyle', 'add',
                        'operatetype', 'ADD',
                        'operateType', 'ADD',
                        'isHide', false,
                        'ispermission', false,
                        'isPublished', true,
                        'iscallback', true,
                        'iscustomfunc', false,
                        'useInMore', false,
                        'isconfirm', false,
                        'regionType', 'BUTTON',
                        'cellCode', 'cell_adp_wts_workpermit_add',
                        'buttonoperationcode', 'workPermitList_add_add_WTS_1.0.0_workPermit_workPermitList',
                        'viewselect', public.adp_runtime_view_ref('WTS_1.0.0_workPermit_workPermitEdit'),
                        'CODE', 'workPermitList_add_add_WTS_1.0.0_workPermit_workPermitList',
                        'NAME', '新增',
                        'ICONCLS', 'cui-btn-add',
                        'USEINMORE', 'false',
                        'SEPARATENUM', '0'
                    ),
                    jsonb_build_object(
                        'id', 'edit',
                        'showname', '修改',
                        'namekey', '修改',
                        'i18nKey', 'WTS.buttonPropertyshowName.randon1574731383929.flag',
                        'buttonstyle', 'modify',
                        'operatetype', 'MODIFY',
                        'operateType', 'MODIFY',
                        'isHide', false,
                        'ispermission', false,
                        'isPublished', true,
                        'iscallback', true,
                        'iscustomfunc', false,
                        'useInMore', false,
                        'isconfirm', false,
                        'regionType', 'BUTTON',
                        'cellCode', 'cell_adp_wts_workpermit_edit',
                        'buttonoperationcode', 'workPermitList_edit_modify_WTS_1.0.0_workPermit_workPermitList',
                        'viewselect', public.adp_runtime_view_ref('WTS_1.0.0_workPermit_workPermitEdit'),
                        'CODE', 'workPermitList_edit_modify_WTS_1.0.0_workPermit_workPermitList',
                        'NAME', '修改',
                        'ICONCLS', 'cui-btn-modify',
                        'USEINMORE', 'false',
                        'SEPARATENUM', '0'
                    )
                )
            ),
            (
                'craftGraph_1.0_basicInfo_basicInfoList',
                'craftGraph_1.0_basicInfo_basicInfoList',
                jsonb_build_array(
                    jsonb_build_object(
                        'id', 'add',
                        'showname', '新增',
                        'namekey', '新增',
                        'i18nKey', 'craftGraph.buttonPropertyshowName.randon1623222803369.flag',
                        'buttonstyle', 'add',
                        'operatetype', 'ADD',
                        'operateType', 'ADD',
                        'isHide', false,
                        'ispermission', false,
                        'isPublished', true,
                        'iscallback', true,
                        'iscustomfunc', false,
                        'useInMore', false,
                        'isconfirm', false,
                        'regionType', 'BUTTON',
                        'cellCode', 'cell_adp_craft_basic_add',
                        'buttonoperationcode', 'basicInfoList_add_add_craftGraph_1.0_basicInfo_basicInfoList',
                        'viewselect', public.adp_runtime_view_ref('craftGraph_1.0_basicInfo_basicInfoEdit'),
                        'CODE', 'basicInfoList_add_add_craftGraph_1.0_basicInfo_basicInfoList',
                        'NAME', '新增',
                        'ICONCLS', 'cui-btn-add',
                        'USEINMORE', 'false',
                        'SEPARATENUM', '0'
                    ),
                    jsonb_build_object(
                        'id', 'delete1',
                        'showname', '删除',
                        'namekey', '删除',
                        'i18nKey', 'craftGraph.buttonPropertyshowName.randon1623328178961.flag',
                        'buttonstyle', 'del',
                        'operatetype', 'DELETE',
                        'operateType', 'DELETE',
                        'isHide', false,
                        'ispermission', false,
                        'isPublished', true,
                        'iscallback', true,
                        'iscustomfunc', false,
                        'useInMore', false,
                        'isconfirm', true,
                        'regionType', 'BUTTON',
                        'cellCode', 'cell_adp_craft_basic_delete',
                        'buttonoperationcode', 'basicInfoList_delete1_del_craftGraph_1.0_basicInfo_basicInfoList',
                        'CODE', 'basicInfoList_delete1_del_craftGraph_1.0_basicInfo_basicInfoList',
                        'NAME', '删除',
                        'ICONCLS', 'cui-btn-del',
                        'USEINMORE', 'false',
                        'SEPARATENUM', '0'
                    )
                )
            ),
            (
                'workAppointment_6.1.6.1_workPlan_workPlanList',
                'workAppointment_6.1.6.1_workPlan_workPlanList',
                jsonb_build_array(
                    jsonb_build_object(
                        'id', 'add',
                        'showname', '新增',
                        'namekey', '新增',
                        'i18nKey', 'workAppointment.buttonPropertyshowName.workPlan.add',
                        'buttonstyle', 'add',
                        'operatetype', 'ADD',
                        'operateType', 'ADD',
                        'isHide', false,
                        'ispermission', false,
                        'isPublished', true,
                        'iscallback', true,
                        'iscustomfunc', false,
                        'useInMore', false,
                        'isconfirm', false,
                        'regionType', 'BUTTON',
                        'cellCode', 'cell_adp_waps_workplan_add',
                        'buttonoperationcode', 'workPlanList_add_add_workAppointment_6.1.6.1_workPlan_workPlanList',
                        'viewselect', public.adp_runtime_view_ref('workAppointment_6.1.6.1_workPlan_workPlanEdit'),
                        'CODE', 'workPlanList_add_add_workAppointment_6.1.6.1_workPlan_workPlanList',
                        'NAME', '新增',
                        'ICONCLS', 'cui-btn-add',
                        'USEINMORE', 'false',
                        'SEPARATENUM', '0'
                    ),
                    jsonb_build_object(
                        'id', 'delete',
                        'showname', '删除',
                        'namekey', '删除',
                        'i18nKey', 'workAppointment.buttonPropertyshowName.workPlan.delete',
                        'buttonstyle', 'del',
                        'operatetype', 'DELETE',
                        'operateType', 'DELETE',
                        'isHide', false,
                        'ispermission', false,
                        'isPublished', true,
                        'iscallback', true,
                        'iscustomfunc', false,
                        'useInMore', false,
                        'isconfirm', true,
                        'regionType', 'BUTTON',
                        'cellCode', 'cell_adp_waps_workplan_delete',
                        'buttonoperationcode', 'workPlanList_delete_del_workAppointment_6.1.6.1_workPlan_workPlanList',
                        'CODE', 'workPlanList_delete_del_workAppointment_6.1.6.1_workPlan_workPlanList',
                        'NAME', '删除',
                        'ICONCLS', 'cui-btn-del',
                        'USEINMORE', 'false',
                        'SEPARATENUM', '0'
                    )
                )
            )
        ) AS target(view_code, grid_code, buttons)
    LOOP
        current_payload := NULL;

        IF COALESCE(view_json_is_oid, false) THEN
            SELECT convert_from(lo_get(view_json), 'UTF8')
            INTO current_payload
            FROM public.runtime_extra_view
            WHERE code = item.view_code;
        ELSE
            SELECT view_json::text
            INTO current_payload
            FROM public.runtime_extra_view
            WHERE code = item.view_code;
        END IF;

        IF current_payload IS NULL OR current_payload = '' THEN
            RAISE NOTICE 'runtime_extra_view % is missing; skip button patch', item.view_code;
            CONTINUE;
        END IF;

        patched_payload := public.adp_patch_datagrid_buttons(
            current_payload::jsonb,
            item.grid_code,
            item.buttons
        )::text;

        IF COALESCE(view_json_is_oid, false) THEN
            UPDATE public.runtime_extra_view
            SET view_json = lo_from_bytea(0, convert_to(patched_payload, 'UTF8'))
            WHERE code = item.view_code;
        ELSE
            UPDATE public.runtime_extra_view
            SET view_json = patched_payload
            WHERE code = item.view_code;
        END IF;

        IF EXISTS (
            SELECT 1
            FROM information_schema.tables
            WHERE table_schema = 'public'
              AND table_name = 'ec_extra_view'
        ) THEN
            UPDATE public.ec_extra_view
            SET view_json = patched_payload
            WHERE code = item.view_code;
        END IF;
    END LOOP;
END $do$;
