-- Restore the recovered WOM easyTaskOperateView "结束报工" runtime button.
-- The source module.xml defines reportEvent(event), but the recovered
-- runtime_extra_view JSON had buttons: [] for dg1577337007020.

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
    report_buttons jsonb;
BEGIN
    SELECT udt_name = 'oid' INTO view_json_is_oid
    FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name = 'runtime_extra_view'
      AND column_name = 'view_json';

    report_buttons := jsonb_build_array(
        jsonb_build_object(
            'id', 'btn-report',
            'showname', '结束报工',
            'namekey', 'WOM.buttonPropertyshowName.randon1577356159364.flag',
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
            'cellCode', 'cell_1577356111376_6635',
            'buttonoperationcode', 'easyTaskOperateView_reportEvent_add_WOM_1.0.0_produceTask_easyTaskOperateView',
            'funcname', $$onclick='reportEvent(event)'$$,
            'onclick', 'reportEvent(event)',
            'ONCLICK', 'reportEvent(event)',
            'CODE', 'easyTaskOperateView_reportEvent_add_WOM_1.0.0_produceTask_easyTaskOperateView',
            'NAME', '结束报工',
            'ICONCLS', 'cui-btn-add',
            'USEINMORE', 'false',
            'SEPARATENUM', '0',
            'funcbody', $$function reportEvent(event) {
    var dataGrid = ReactAPI.getComponentAPI('SupDataGrid').APIs('WOM_1.0.0_produceTask_easyTaskOperateViewdg1577337007020');
    var actives = dataGrid.getSelecteds();
    if (!actives.length) {
        ReactAPI.showMessage("f", ReactAPI.international.getText('SupDatagrid.button.error'));
        return false;
    }
    var active = actives[0];
    if (active.isFinish) {
        ReactAPI.showMessage("f", ReactAPI.international.getText('WOM.custom.randon1577356176633'));
        return false;
    }
    ReactAPI.openLoading(ReactAPI.international.getText('EditView.notice.processing'));
    ReactAPI.request({
        type: "post",
        url: "/msService/WOM/produceTask/produceTask/endEasyActive/" + active.id
    }, function(res) {
        ReactAPI.closeLoading();
        if (res.code == 200) {
            var data = res.data;
            if (data.dealSuccessFlag) {
                ReactAPI.showMessage("s", ReactAPI.international.getText('EditView.notice.operate.success'));
                dataGrid.refreshDataByRequst({
                    type: "post",
                    url: "/msService/WOM/produceTask/produceTask/data-dg1577337007020?datagridCode=WOM_1.0.0_produceTask_easyTaskOperateViewdg1577337007020&id=" + ReactAPI.getFormData().id,
                    param: { pageSize: 65535 }
                });
            } else {
                ReactAPI.showMessage("f", data.errorMessage);
            }
        } else {
            ReactAPI.showMessage("f", res.message);
        }
    });
}$$,
            'funcbody_es5', $$function reportEvent(event) {
    var dataGrid = ReactAPI.getComponentAPI('SupDataGrid').APIs('WOM_1.0.0_produceTask_easyTaskOperateViewdg1577337007020');
    var actives = dataGrid.getSelecteds();
    if (!actives.length) {
        ReactAPI.showMessage("f", ReactAPI.international.getText('SupDatagrid.button.error'));
        return false;
    }
    var active = actives[0];
    if (active.isFinish) {
        ReactAPI.showMessage("f", ReactAPI.international.getText('WOM.custom.randon1577356176633'));
        return false;
    }
    ReactAPI.openLoading(ReactAPI.international.getText('EditView.notice.processing'));
    ReactAPI.request({
        type: "post",
        url: "/msService/WOM/produceTask/produceTask/endEasyActive/" + active.id
    }, function(res) {
        ReactAPI.closeLoading();
        if (res.code == 200) {
            var data = res.data;
            if (data.dealSuccessFlag) {
                ReactAPI.showMessage("s", ReactAPI.international.getText('EditView.notice.operate.success'));
                dataGrid.refreshDataByRequst({
                    type: "post",
                    url: "/msService/WOM/produceTask/produceTask/data-dg1577337007020?datagridCode=WOM_1.0.0_produceTask_easyTaskOperateViewdg1577337007020&id=" + ReactAPI.getFormData().id,
                    param: { pageSize: 65535 }
                });
            } else {
                ReactAPI.showMessage("f", data.errorMessage);
            }
        } else {
            ReactAPI.showMessage("f", res.message);
        }
    });
}$$
        )
    );

    IF COALESCE(view_json_is_oid, false) THEN
        SELECT convert_from(lo_get(view_json), 'UTF8')
        INTO current_payload
        FROM public.runtime_extra_view
        WHERE code = 'WOM_1.0.0_produceTask_easyTaskOperateView';
    ELSE
        SELECT view_json::text
        INTO current_payload
        FROM public.runtime_extra_view
        WHERE code = 'WOM_1.0.0_produceTask_easyTaskOperateView';
    END IF;

    IF current_payload IS NULL OR current_payload = '' THEN
        RAISE NOTICE 'runtime_extra_view WOM easyTaskOperateView is missing; skip report button patch';
        RETURN;
    END IF;

    patched_payload := public.adp_patch_datagrid_buttons(
        current_payload::jsonb,
        'WOM_1.0.0_produceTask_easyTaskOperateViewdg1577337007020',
        report_buttons
    )::text;

    IF COALESCE(view_json_is_oid, false) THEN
        UPDATE public.runtime_extra_view
        SET view_json = lo_from_bytea(0, convert_to(patched_payload, 'UTF8'))
        WHERE code = 'WOM_1.0.0_produceTask_easyTaskOperateView';
    ELSE
        UPDATE public.runtime_extra_view
        SET view_json = patched_payload
        WHERE code = 'WOM_1.0.0_produceTask_easyTaskOperateView';
    END IF;
END $do$;
