-- Restore the child TaskActive grid used by WOM easyTaskOperateView.
--
-- The recovered extra-view converter retained the parent ProduceTask model and
-- emitted fields/elements as empty arrays. SupDataGrid then collapses multiple
-- activity rows because they have no configured identity or child columns.

BEGIN;

CREATE OR REPLACE FUNCTION public.adp_patch_wom_easy_activity_grid(
    target jsonb,
    grid_code text,
    grid_fields jsonb
) RETURNS jsonb
LANGUAGE plpgsql
AS $$
DECLARE
    item_key text;
    item_value jsonb;
    patched jsonb;
BEGIN
    IF target IS NULL THEN
        RETURN target;
    END IF;

    IF jsonb_typeof(target) = 'object' THEN
        IF (
            target->>'DataGridCode' = grid_code
            OR target->>'dataGridName' = grid_code
            OR target->>'datagridName' = grid_code
            OR target->>'code' = grid_code
        ) AND (
            target->>'type' = 'layoutDatagrid'
            OR target ? 'fields'
            OR target ? 'elements'
        ) THEN
            target := target || jsonb_build_object(
                'modelCode', 'WOM_1.0.0_produceTask_TaskActive',
                'targetModelCode', 'WOM_1.0.0_produceTask_TaskActive',
                'mainDisplayName', 'name',
                'idPrefix', 'TaskActive_dg1577337007020',
                'fields', grid_fields,
                'elements', grid_fields
            );
        END IF;

        patched := '{}'::jsonb;
        FOR item_key, item_value IN SELECT * FROM jsonb_each(target)
        LOOP
            patched := patched || jsonb_build_object(
                item_key,
                public.adp_patch_wom_easy_activity_grid(item_value, grid_code, grid_fields)
            );
        END LOOP;
        RETURN patched;
    END IF;

    IF jsonb_typeof(target) = 'array' THEN
        SELECT jsonb_agg(
            public.adp_patch_wom_easy_activity_grid(value, grid_code, grid_fields)
        )
        INTO patched
        FROM jsonb_array_elements(target);
        RETURN COALESCE(patched, '[]'::jsonb);
    END IF;

    RETURN target;
END $$;

DO $do$
DECLARE
    view_json_is_oid boolean;
    current_payload text;
    patched_payload text;
    activity_fields jsonb;
    old_view_json_oid oid;
    new_view_json_oid oid;
BEGIN
    activity_fields := jsonb_build_array(
        jsonb_build_object(
            'key', 'id',
            'namekey', 'ec.common.ID',
            'showType', 'TEXTFIELD',
            'showFormat', 'TEXT',
            'width', 120,
            'isHidden', true,
            'columnType', 'LONG'
        ),
        jsonb_build_object(
            'key', 'name',
            'namekey', 'WOM.produceTask.TaskActive.name',
            'showType', 'TEXTFIELD',
            'showFormat', 'TEXT',
            'width', 160,
            'isHidden', false,
            'columnType', 'TEXT'
        ),
        jsonb_build_object(
            'key', 'materialId.code',
            'namekey', 'BaseSet.material.Material.code',
            'showType', 'SELECTCOMP',
            'showFormat', 'SELECTCOMP',
            'width', 120,
            'isHidden', false,
            'columnType', 'TEXT'
        ),
        jsonb_build_object(
            'key', 'materialId.name',
            'namekey', 'BaseSet.material.Material.name',
            'showType', 'TEXTFIELD',
            'showFormat', 'TEXT',
            'width', 140,
            'isHidden', false,
            'columnType', 'TEXT'
        ),
        jsonb_build_object(
            'key', 'activeType',
            'namekey', 'WOM.produceTask.TaskActive.activeType',
            'showType', 'SELECTCOMP',
            'showFormat', 'SELECTCOMP',
            'width', 110,
            'isHidden', false,
            'columnType', 'SYSTEMCODE',
            'fill', jsonb_build_object(
                'fillName', '系统编码',
                'fillType', '3',
                'fillContent', 'RM_activeType'
            )
        ),
        jsonb_build_object(
            'key', 'property',
            'namekey', 'WOM.produceTask.TaskActive.property',
            'showType', 'SELECTCOMP',
            'showFormat', 'SELECTCOMP',
            'width', 100,
            'isHidden', false,
            'columnType', 'SYSTEMCODE',
            'fill', jsonb_build_object(
                'fillName', '系统编码',
                'fillType', '3',
                'fillContent', 'RM_RMproperty'
            )
        ),
        jsonb_build_object(
            'key', 'planQuantity',
            'namekey', 'WOM.produceTask.TaskActive.planQuantity',
            'showType', 'TEXTFIELD',
            'showFormat', 'TEXT',
            'width', 110,
            'isHidden', false,
            'columnType', 'DECIMAL'
        ),
        jsonb_build_object(
            'key', 'standardQuantity',
            'namekey', 'WOM.produceTask.TaskActive.standardQuantity',
            'showType', 'TEXTFIELD',
            'showFormat', 'TEXT',
            'width', 110,
            'isHidden', false,
            'columnType', 'DECIMAL'
        ),
        jsonb_build_object(
            'key', 'sumNum',
            'namekey', 'WOM.produceTask.TaskActive.sumNum',
            'showType', 'TEXTFIELD',
            'showFormat', 'TEXT',
            'width', 110,
            'isHidden', false,
            'columnType', 'DECIMAL'
        ),
        jsonb_build_object(
            'key', 'isFinish',
            'namekey', 'WOM.propertyshowName.randon1593770587695.flag',
            'showType', 'CHECKBOX',
            'showFormat', 'CHECKBOX',
            'width', 90,
            'isHidden', false,
            'columnType', 'BOOLEAN'
        ),
        jsonb_build_object(
            'key', 'remark',
            'namekey', 'WOM.produceTask.TaskActive.remark',
            'showType', 'TEXTFIELD',
            'showFormat', 'TEXT',
            'width', 180,
            'isHidden', false,
            'columnType', 'LONGTEXT'
        )
    );

    SELECT udt_name = 'oid'
    INTO view_json_is_oid
    FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name = 'runtime_extra_view'
      AND column_name = 'view_json';

    IF COALESCE(view_json_is_oid, false) THEN
        SELECT view_json, convert_from(lo_get(view_json), 'UTF8')
        INTO old_view_json_oid, current_payload
        FROM public.runtime_extra_view
        WHERE code = 'WOM_1.0.0_produceTask_easyTaskOperateView'
        FOR UPDATE;
    ELSE
        SELECT view_json::text
        INTO current_payload
        FROM public.runtime_extra_view
        WHERE code = 'WOM_1.0.0_produceTask_easyTaskOperateView'
        FOR UPDATE;
    END IF;

    IF current_payload IS NULL OR current_payload = '' THEN
        RAISE NOTICE 'runtime_extra_view WOM easyTaskOperateView is missing; skip activity grid patch';
        RETURN;
    END IF;

    patched_payload := public.adp_patch_wom_easy_activity_grid(
        current_payload::jsonb,
        'WOM_1.0.0_produceTask_easyTaskOperateViewdg1577337007020',
        activity_fields
    )::text;

    IF COALESCE(view_json_is_oid, false) THEN
        new_view_json_oid := lo_from_bytea(0, convert_to(patched_payload, 'UTF8'));
        UPDATE public.runtime_extra_view
        SET view_json = new_view_json_oid
        WHERE code = 'WOM_1.0.0_produceTask_easyTaskOperateView';
        IF FOUND THEN
            IF old_view_json_oid IS NOT NULL AND old_view_json_oid <> new_view_json_oid THEN
                PERFORM lo_unlink(old_view_json_oid);
            END IF;
        ELSE
            PERFORM lo_unlink(new_view_json_oid);
            RAISE EXCEPTION 'runtime_extra_view WOM easyTaskOperateView disappeared during patch';
        END IF;
    ELSE
        UPDATE public.runtime_extra_view
        SET view_json = patched_payload
        WHERE code = 'WOM_1.0.0_produceTask_easyTaskOperateView';
        IF NOT FOUND THEN
            RAISE EXCEPTION 'runtime_extra_view WOM easyTaskOperateView disappeared during patch';
        END IF;
    END IF;
END $do$;

INSERT INTO public.runtime_customer_condition (
    code,
    ec_env,
    version,
    create_time,
    modify_time,
    valid,
    entity_code,
    module_code,
    proj_flag,
    condition_sql,
    json_condition,
    dataclassific_code,
    datagrid_code,
    view_code
) VALUES (
    'WOM_1.0.0_produceTask_easyTaskOperateView_WOM_1.0.0_produceTask_easyTaskOperateViewdg1577337007020',
    'product',
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    true,
    'WOM_1.0.0_produceTask',
    'WOM_1.0.0',
    false,
    NULL,
    '{"condName":"undefined","remark":"undefined","viewCode":"undefined","modelAlias":"taskActive","subconds":[{"type":"1","logic":"or","subconds":[{"type":"0","columnName":"ACTIVE_TYPE","dbColumnType":"SYSTEMCODE","operator":"=","paramStr":"?","value":"RM_activeType/putin"},{"type":"0","columnName":"ACTIVE_TYPE","dbColumnType":"SYSTEMCODE","operator":"=","paramStr":"?","value":"RM_activeType/output"}]}]}',
    NULL,
    'WOM_1.0.0_produceTask_easyTaskOperateViewdg1577337007020',
    'WOM_1.0.0_produceTask_easyTaskOperateView'
)
ON CONFLICT (code) DO UPDATE SET
    ec_env = EXCLUDED.ec_env,
    modify_time = CURRENT_TIMESTAMP,
    valid = true,
    entity_code = EXCLUDED.entity_code,
    module_code = EXCLUDED.module_code,
    proj_flag = EXCLUDED.proj_flag,
    json_condition = EXCLUDED.json_condition,
    datagrid_code = EXCLUDED.datagrid_code,
    view_code = EXCLUDED.view_code;

INSERT INTO public.ec_customer_condition (
    code,
    ec_env,
    version,
    create_time,
    modify_time,
    valid,
    entity_code,
    module_code,
    proj_flag,
    condition_sql,
    json_condition,
    dataclassific_code,
    datagrid_code,
    view_code
) VALUES (
    'WOM_1.0.0_produceTask_easyTaskOperateView_WOM_1.0.0_produceTask_easyTaskOperateViewdg1577337007020',
    'product',
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    1,
    'WOM_1.0.0_produceTask',
    'WOM_1.0.0',
    0,
    NULL,
    '{"condName":"undefined","remark":"undefined","viewCode":"undefined","modelAlias":"taskActive","subconds":[{"type":"1","logic":"or","subconds":[{"type":"0","columnName":"ACTIVE_TYPE","dbColumnType":"SYSTEMCODE","operator":"=","paramStr":"?","value":"RM_activeType/putin"},{"type":"0","columnName":"ACTIVE_TYPE","dbColumnType":"SYSTEMCODE","operator":"=","paramStr":"?","value":"RM_activeType/output"}]}]}',
    NULL,
    'WOM_1.0.0_produceTask_easyTaskOperateViewdg1577337007020',
    'WOM_1.0.0_produceTask_easyTaskOperateView'
)
ON CONFLICT (code) DO UPDATE SET
    ec_env = EXCLUDED.ec_env,
    modify_time = CURRENT_TIMESTAMP,
    valid = 1,
    entity_code = EXCLUDED.entity_code,
    module_code = EXCLUDED.module_code,
    proj_flag = EXCLUDED.proj_flag,
    json_condition = EXCLUDED.json_condition,
    datagrid_code = EXCLUDED.datagrid_code,
    view_code = EXCLUDED.view_code;

COMMIT;
