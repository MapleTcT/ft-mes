-- Restore missing runtime buttons on WOM makeTaskBatchView activity grid.
-- The recovered runtime_extra_view JSON in 076 has buttons: [] for this grid,
-- while the source module.xml contains startActive/endActive button handlers.

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
        IF target->>'DataGridCode' = grid_code OR target->>'dataGridName' = grid_code OR target->>'code' = grid_code THEN
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

DO $do$
DECLARE
    view_json_is_oid boolean;
    current_payload text;
    patched_payload text;
    active_buttons jsonb;
BEGIN
    SELECT udt_name = 'oid' INTO view_json_is_oid
    FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name = 'runtime_extra_view'
      AND column_name = 'view_json';

    active_buttons := jsonb_build_array(
        jsonb_build_object(
            'id', 'startActive',
            'showname', '开始',
            'namekey', 'WOM.buttonPropertyshowName.randon1576044613163.flag',
            'buttonstyle', 'add',
            'operatetype', 'CUSTOM',
            'operateType', 'CUSTOM',
            'isHide', false,
            'ispermission', false,
            'isPublished', false,
            'isconfirm', 'false',
            'iscallback', 'false',
            'iscustomfunc', 'false',
            'useInMore', 'false',
            'regionType', 'BUTTON',
            'cellCode', 'cell_1586416570062_1',
            'buttonoperationcode', 'makeTaskBatchView_startActive_add_WOM_1.0.0_produceTask_makeTaskBatchView',
            'funcname', $$onclick='startActive(event)'$$,
            'onclick', 'startActive(event)',
            'ONCLICK', 'startActive(event)',
            'CODE', 'makeTaskBatchView_startActive_add_WOM_1.0.0_produceTask_makeTaskBatchView',
            'NAME', '开始',
            'ICONCLS', 'cui-btn-add',
            'USEINMORE', 'false',
            'SEPARATENUM', '0',
            'funcbody', $$function startActive(event) {
    console.log('startActive');
    var activeDataGrid = ReactAPI.getComponentAPI('SupDataGrid').APIs('WOM_1.0.0_produceTask_makeTaskBatchViewdg1586416570027');
    var activeData = activeDataGrid.getSelecteds();
    var activeId = activeData[0].id;
    var processDataGrid = ReactAPI.getComponentAPI('SupDataGrid').APIs('WOM_1.0.0_produceTask_makeTaskBatchViewdg1586416569990');
    var processId = activeData[0].taskProcessId.id;
    var formData = ReactAPI.getFormData();
    var taskId = formData.id;
    $.ajax({
        url: "/msService/WOM/produceTask/produceTask/startActive",
        type: 'get',
        async: false,
        data: { activeId: activeId },
        success: function success(res) {
            if (res.success) {
                var data = res.data;
                if (data.success) {
                    ReactAPI.showMessage("s", "活动已开始！");
                    activeDataGrid.refreshDataByRequst({
                        type: "post",
                        url: "/msService/WOM/produceTask/taskActive/queryByProcess?processId=" + processId + "&showBatch=false",
                        param: {}
                    });
                    processDataGrid.refreshDataByRequst({
                        type: "post",
                        url: "/msService/WOM/produceTask/produceTask/data-dg1576028988483?datagridCode=WOM_1.0.0_produceTask_makeTaskBatchViewdg1586416569990&id=" + taskId,
                        param: {}
                    });
                    if (opener) {
                        opener.ReactAPI.getComponentAPI('ListView').SearchList.submitEditDialogCallback();
                    }
                } else {
                    ReactAPI.showMessage("f", data.msg);
                }
            } else {
                ReactAPI.showMessage("f", res.msg);
            }
        }
    });
}$$,
            'funcbody_es5', $$function startActive(event) {
    console.log('startActive');
    var activeDataGrid = ReactAPI.getComponentAPI('SupDataGrid').APIs('WOM_1.0.0_produceTask_makeTaskBatchViewdg1586416570027');
    var activeData = activeDataGrid.getSelecteds();
    var activeId = activeData[0].id;
    var processDataGrid = ReactAPI.getComponentAPI('SupDataGrid').APIs('WOM_1.0.0_produceTask_makeTaskBatchViewdg1586416569990');
    var processId = activeData[0].taskProcessId.id;
    var formData = ReactAPI.getFormData();
    var taskId = formData.id;
    $.ajax({
        url: "/msService/WOM/produceTask/produceTask/startActive",
        type: 'get',
        async: false,
        data: { activeId: activeId },
        success: function success(res) {
            if (res.success) {
                var data = res.data;
                if (data.success) {
                    ReactAPI.showMessage("s", "活动已开始！");
                    activeDataGrid.refreshDataByRequst({
                        type: "post",
                        url: "/msService/WOM/produceTask/taskActive/queryByProcess?processId=" + processId + "&showBatch=false",
                        param: {}
                    });
                    processDataGrid.refreshDataByRequst({
                        type: "post",
                        url: "/msService/WOM/produceTask/produceTask/data-dg1576028988483?datagridCode=WOM_1.0.0_produceTask_makeTaskBatchViewdg1586416569990&id=" + taskId,
                        param: {}
                    });
                    if (opener) {
                        opener.ReactAPI.getComponentAPI('ListView').SearchList.submitEditDialogCallback();
                    }
                } else {
                    ReactAPI.showMessage("f", data.msg);
                }
            } else {
                ReactAPI.showMessage("f", res.msg);
            }
        }
    });
}$$
        ),
        jsonb_build_object(
            'id', 'endActive',
            'showname', '结束',
            'namekey', 'WOM.buttonPropertyshowName.randon1576044689853.flag',
            'buttonstyle', 'add',
            'operatetype', 'CUSTOM',
            'operateType', 'CUSTOM',
            'isHide', false,
            'ispermission', false,
            'isPublished', false,
            'isconfirm', 'false',
            'iscallback', 'false',
            'iscustomfunc', 'false',
            'useInMore', 'false',
            'regionType', 'BUTTON',
            'cellCode', 'cell_1586416570062_2',
            'buttonoperationcode', 'makeTaskBatchView_endActive_add_WOM_1.0.0_produceTask_makeTaskBatchView',
            'funcname', $$onclick='endActiveEvent(event)'$$,
            'onclick', 'endActiveEvent(event)',
            'ONCLICK', 'endActiveEvent(event)',
            'CODE', 'makeTaskBatchView_endActive_add_WOM_1.0.0_produceTask_makeTaskBatchView',
            'NAME', '结束',
            'ICONCLS', 'cui-btn-add',
            'USEINMORE', 'false',
            'SEPARATENUM', '0',
            'funcbody', $$function endActiveEvent(event) {
    var activeDataGrid = ReactAPI.getComponentAPI('SupDataGrid').APIs('WOM_1.0.0_produceTask_makeTaskBatchViewdg1586416570027');
    var activeData = activeDataGrid.getSelecteds();
    var activeId = activeData[0].id;
    $.ajax({
        url: "/msService/WOM/produceTask/produceTask/endActive",
        type: 'get',
        async: false,
        data: { activeId: activeId },
        success: function success(res) {
            if (res.success && res.data && res.data.success) {
                ReactAPI.showMessage("s", "活动已结束！");
            } else {
                ReactAPI.showMessage("f", (res.data && res.data.msg) || res.msg);
            }
        }
    });
}$$,
            'funcbody_es5', $$function endActiveEvent(event) {
    var activeDataGrid = ReactAPI.getComponentAPI('SupDataGrid').APIs('WOM_1.0.0_produceTask_makeTaskBatchViewdg1586416570027');
    var activeData = activeDataGrid.getSelecteds();
    var activeId = activeData[0].id;
    $.ajax({
        url: "/msService/WOM/produceTask/produceTask/endActive",
        type: 'get',
        async: false,
        data: { activeId: activeId },
        success: function success(res) {
            if (res.success && res.data && res.data.success) {
                ReactAPI.showMessage("s", "活动已结束！");
            } else {
                ReactAPI.showMessage("f", (res.data && res.data.msg) || res.msg);
            }
        }
    });
}$$
        )
    );

    IF COALESCE(view_json_is_oid, false) THEN
        SELECT convert_from(lo_get(view_json), 'UTF8')
        INTO current_payload
        FROM public.runtime_extra_view
        WHERE code = 'WOM_1.0.0_produceTask_makeTaskBatchView';
    ELSE
        SELECT view_json::text
        INTO current_payload
        FROM public.runtime_extra_view
        WHERE code = 'WOM_1.0.0_produceTask_makeTaskBatchView';
    END IF;

    IF current_payload IS NULL OR current_payload = '' THEN
        RAISE NOTICE 'runtime_extra_view WOM makeTaskBatchView is missing; skip button patch';
        RETURN;
    END IF;

    patched_payload := public.adp_patch_datagrid_buttons(
        current_payload::jsonb,
        'WOM_1.0.0_produceTask_makeTaskBatchViewdg1586416570027',
        active_buttons
    )::text;

    IF COALESCE(view_json_is_oid, false) THEN
        UPDATE public.runtime_extra_view
        SET view_json = lo_from_bytea(0, convert_to(patched_payload, 'UTF8'))
        WHERE code = 'WOM_1.0.0_produceTask_makeTaskBatchView';
    ELSE
        UPDATE public.runtime_extra_view
        SET view_json = patched_payload
        WHERE code = 'WOM_1.0.0_produceTask_makeTaskBatchView';
    END IF;
END $do$;
