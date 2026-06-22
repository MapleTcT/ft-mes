-- Restore the visible modify button for craftGraph basic information.
--
-- The list view already supports add/delete, and the edit page can load an
-- existing row by id. Without the modify button, users cannot reach that edit
-- flow from the list, so create/update/delete acceptance remains blocked.

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
    current_payload text;
    patched_payload text;
    craft_buttons jsonb := jsonb_build_array(
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
            'id', 'edit',
            'showname', '修改',
            'namekey', '修改',
            'i18nKey', 'craftGraph.buttonPropertyshowName.modify.flag',
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
            'cellCode', 'cell_adp_craft_basic_edit',
            'buttonoperationcode', 'basicInfoList_edit_modify_craftGraph_1.0_basicInfo_basicInfoList',
            'viewselect', public.adp_runtime_view_ref('craftGraph_1.0_basicInfo_basicInfoEdit'),
            'CODE', 'basicInfoList_edit_modify_craftGraph_1.0_basicInfo_basicInfoList',
            'NAME', '修改',
            'ICONCLS', 'cui-btn-modify',
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
    );
BEGIN
    SELECT udt_name = 'oid' INTO view_json_is_oid
    FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name = 'runtime_extra_view'
      AND column_name = 'view_json';

    SELECT CASE
        WHEN view_json_is_oid THEN convert_from(lo_get(view_json::oid), 'UTF8')
        ELSE view_json::text
    END
    INTO current_payload
    FROM public.runtime_extra_view
    WHERE code = 'craftGraph_1.0_basicInfo_basicInfoList';

    IF current_payload IS NULL OR btrim(current_payload) = '' THEN
        RAISE NOTICE 'runtime_extra_view craftGraph_1.0_basicInfo_basicInfoList is empty; skip button patch';
        RETURN;
    END IF;

    patched_payload := public.adp_patch_datagrid_buttons(
        current_payload::jsonb,
        'craftGraph_1.0_basicInfo_basicInfoList',
        craft_buttons
    )::text;

    IF view_json_is_oid THEN
        UPDATE public.runtime_extra_view
        SET view_json = lo_from_bytea(0, convert_to(patched_payload, 'UTF8'))
        WHERE code = 'craftGraph_1.0_basicInfo_basicInfoList';
    ELSE
        UPDATE public.runtime_extra_view
        SET view_json = patched_payload
        WHERE code = 'craftGraph_1.0_basicInfo_basicInfoList';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = 'public'
          AND table_name = 'ec_extra_view'
    ) THEN
        IF view_json_is_oid THEN
            UPDATE public.ec_extra_view
            SET view_json = lo_from_bytea(0, convert_to(patched_payload, 'UTF8'))
            WHERE code = 'craftGraph_1.0_basicInfo_basicInfoList';
        ELSE
            UPDATE public.ec_extra_view
            SET view_json = patched_payload
            WHERE code = 'craftGraph_1.0_basicInfo_basicInfoList';
        END IF;
    END IF;
END $do$;
