-- Restore missing runtime buttons on WOM makeTaskBatchView process grid.
-- Source evidence:
-- WOM_6.1.3.4/service/src/main/resources/custom/WOM/produceTask/produceTask/makeTaskBatchView/eventJs/customEvent.js
-- The recovered runtime_extra_view JSON can lose the process-grid buttons while
-- retaining only the activity-grid buttons patched by 100-wom-batch-active-runtime-buttons.sql.

DO $do$
DECLARE
    view_json_is_oid boolean;
    current_payload text;
    patched_payload text;
    process_buttons jsonb;
BEGIN
    SELECT udt_name = 'oid' INTO view_json_is_oid
    FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name = 'runtime_extra_view'
      AND column_name = 'view_json';

    process_buttons := jsonb_build_array(
        jsonb_build_object(
            'id', 'startProcess',
            'showname', '开始',
            'namekey', 'WOM.buttonPropertyshowName.randon1576045686052.flag',
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
            'cellCode', 'cell_1576045667879_5394',
            'buttonoperationcode', 'makeTaskBatchView_startProcess_add_WOM_1.0.0_produceTask_makeTaskBatchView',
            'funcname', $$onclick='startProcessEvent(event)'$$,
            'onclick', 'startProcessEvent(event)',
            'ONCLICK', 'startProcessEvent(event)',
            'CODE', 'makeTaskBatchView_startProcess_add_WOM_1.0.0_produceTask_makeTaskBatchView',
            'NAME', '开始',
            'ICONCLS', 'cui-btn-add',
            'USEINMORE', 'false',
            'SEPARATENUM', '0',
            'funcbody', $$function startProcessEvent(event) {
    var dataGrid = ReactAPI.getComponentAPI('SupDataGrid').APIs('WOM_1.0.0_produceTask_makeTaskBatchViewdg1586416569990');
    var task = ReactAPI.getFormData();
    var process = dataGrid.getSelecteds()[0];
    ReactAPI.request({
        type: "post",
        url: "/msService/WOM/produceTask/taskProcess/start/" + process.id
    }, function(res) {
        if (res.dealSuccessFlag) {
            dataGrid.refreshDataByRequst({
                type: "POST",
                url: "/msService/WOM/produceTask/produceTask/data-dg1576028988483?datagridCode=WOM_1.0.0_produceTask_makeTaskBatchViewdg1586416569990&id=" + task.id,
                param: { pageSize: 65535 }
            });
            ReactAPI.getComponentAPI('SupDataGrid').APIs('WOM_1.0.0_produceTask_makeTaskBatchViewdg1586416570027').deleteLine();
        } else {
            ReactAPI.showMessage("f", res.errorMessage);
        }
    });
}$$,
            'funcbody_es5', $$function startProcessEvent(event) {
    var dataGrid = ReactAPI.getComponentAPI('SupDataGrid').APIs('WOM_1.0.0_produceTask_makeTaskBatchViewdg1586416569990');
    var task = ReactAPI.getFormData();
    var process = dataGrid.getSelecteds()[0];
    ReactAPI.request({
        type: "post",
        url: "/msService/WOM/produceTask/taskProcess/start/" + process.id
    }, function(res) {
        if (res.dealSuccessFlag) {
            dataGrid.refreshDataByRequst({
                type: "POST",
                url: "/msService/WOM/produceTask/produceTask/data-dg1576028988483?datagridCode=WOM_1.0.0_produceTask_makeTaskBatchViewdg1586416569990&id=" + task.id,
                param: { pageSize: 65535 }
            });
            ReactAPI.getComponentAPI('SupDataGrid').APIs('WOM_1.0.0_produceTask_makeTaskBatchViewdg1586416570027').deleteLine();
        } else {
            ReactAPI.showMessage("f", res.errorMessage);
        }
    });
}$$
        ),
        jsonb_build_object(
            'id', 'endProcess',
            'showname', '结束',
            'namekey', 'WOM.buttonPropertyshowName.randon1576045773357.flag',
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
            'cellCode', 'cell_1576045760718_9667',
            'buttonoperationcode', 'makeTaskBatchView_endProcess_add_WOM_1.0.0_produceTask_makeTaskBatchView',
            'funcname', $$onclick='endProcessEvent(event)'$$,
            'onclick', 'endProcessEvent(event)',
            'ONCLICK', 'endProcessEvent(event)',
            'CODE', 'makeTaskBatchView_endProcess_add_WOM_1.0.0_produceTask_makeTaskBatchView',
            'NAME', '结束',
            'ICONCLS', 'cui-btn-add',
            'USEINMORE', 'false',
            'SEPARATENUM', '0',
            'funcbody', $$function endProcessEvent(event) {
    var dataGrid = ReactAPI.getComponentAPI('SupDataGrid').APIs('WOM_1.0.0_produceTask_makeTaskBatchViewdg1586416569990');
    var activeGrid = ReactAPI.getComponentAPI('SupDataGrid').APIs('WOM_1.0.0_produceTask_makeTaskBatchViewdg1586416570027');
    var task = ReactAPI.getFormData();
    var process = dataGrid.getSelecteds()[0];
    var submitEnd = function() {
        ReactAPI.request({
            type: "post",
            url: "/msService/WOM/produceTask/taskProcess/end/" + process.id
        }, function(res) {
            if (res.dealSuccessFlag) {
                dataGrid.refreshDataByRequst({
                    type: "POST",
                    url: "/msService/WOM/produceTask/produceTask/data-dg1576028988483?datagridCode=WOM_1.0.0_produceTask_makeTaskBatchViewdg1586416569990&id=" + task.id,
                    param: { pageSize: 65535 }
                });
                activeGrid.deleteLine();
            } else {
                ReactAPI.showMessage("f", res.errorMessage);
            }
        });
    };
    ReactAPI.request({
        type: "get",
        url: "/msService/WOM/produceTask/taskProcess/isUnFinishActives/" + process.id
    }, function(res) {
        if (res.dealSuccessFlag) {
            ReactAPI.openConfirm({
                message: "工序【" + process.name + "】中存在未完成的活动，是否确认结束?",
                buttons: [
                    { operatetype: "yes", text: "是", type: "primary", onClick: function() { ReactAPI.closeConfirm(); submitEnd(); } },
                    { operatetype: "no", text: "否", type: "primary", onClick: function() { ReactAPI.closeConfirm(); } }
                ]
            });
        } else {
            submitEnd();
        }
    });
}$$,
            'funcbody_es5', $$function endProcessEvent(event) {
    var dataGrid = ReactAPI.getComponentAPI('SupDataGrid').APIs('WOM_1.0.0_produceTask_makeTaskBatchViewdg1586416569990');
    var activeGrid = ReactAPI.getComponentAPI('SupDataGrid').APIs('WOM_1.0.0_produceTask_makeTaskBatchViewdg1586416570027');
    var task = ReactAPI.getFormData();
    var process = dataGrid.getSelecteds()[0];
    var submitEnd = function() {
        ReactAPI.request({
            type: "post",
            url: "/msService/WOM/produceTask/taskProcess/end/" + process.id
        }, function(res) {
            if (res.dealSuccessFlag) {
                dataGrid.refreshDataByRequst({
                    type: "POST",
                    url: "/msService/WOM/produceTask/produceTask/data-dg1576028988483?datagridCode=WOM_1.0.0_produceTask_makeTaskBatchViewdg1586416569990&id=" + task.id,
                    param: { pageSize: 65535 }
                });
                activeGrid.deleteLine();
            } else {
                ReactAPI.showMessage("f", res.errorMessage);
            }
        });
    };
    ReactAPI.request({
        type: "get",
        url: "/msService/WOM/produceTask/taskProcess/isUnFinishActives/" + process.id
    }, function(res) {
        if (res.dealSuccessFlag) {
            ReactAPI.openConfirm({
                message: "工序【" + process.name + "】中存在未完成的活动，是否确认结束?",
                buttons: [
                    { operatetype: "yes", text: "是", type: "primary", onClick: function() { ReactAPI.closeConfirm(); submitEnd(); } },
                    { operatetype: "no", text: "否", type: "primary", onClick: function() { ReactAPI.closeConfirm(); } }
                ]
            });
        } else {
            submitEnd();
        }
    });
}$$
        ),
        jsonb_build_object(
            'id', 'setUnit',
            'showname', '设置工作单元',
            'namekey', 'WOM.buttonPropertyshowName.randon1576045806034.flag',
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
            'cellCode', 'cell_1576045797089_9695',
            'buttonoperationcode', 'makeTaskBatchView_setUnit_add_WOM_1.0.0_produceTask_makeTaskBatchView',
            'funcname', $$onclick='setUnitEvent(event)'$$,
            'onclick', 'setUnitEvent(event)',
            'ONCLICK', 'setUnitEvent(event)',
            'CODE', 'makeTaskBatchView_setUnit_add_WOM_1.0.0_produceTask_makeTaskBatchView',
            'NAME', '设置工作单元',
            'ICONCLS', 'cui-btn-add',
            'USEINMORE', 'false',
            'SEPARATENUM', '0',
            'funcbody', $$function setUnitEvent(event) {
    var dataGrid = ReactAPI.getComponentAPI('SupDataGrid').APIs('WOM_1.0.0_produceTask_makeTaskBatchViewdg1586416569990');
    var task = ReactAPI.getFormData();
    var process = dataGrid.getSelecteds()[0];
    var powerCode = ReactAPI.getParamsInRequestUrl()['__pc__'];
    var dialogName = 'processUnitSet';
    ReactAPI.createDialog(dialogName, {
        title: '工序工作单元',
        url: '/msService/WOM/produceTask/taskProcess/processUnitEdit?id=' + process.id + '&__pc__=' + powerCode,
        isRef: false,
        size: 1,
        buttons: [
            {
                text: "保存",
                type: "primary",
                onClick: function(event) {
                    event.ReactAPI.submitFormData("save", function(res) {
                        if (res) {
                            ReactAPI.destroyDialog(dialogName);
                            dataGrid.refreshDataByRequst({
                                type: "POST",
                                url: "/msService/WOM/produceTask/produceTask/data-dg1576028988483?datagridCode=WOM_1.0.0_produceTask_makeTaskBatchViewdg1586416569990&id=" + task.id,
                                param: { pageSize: 65535 }
                            });
                            ReactAPI.getComponentAPI('SupDataGrid').APIs('WOM_1.0.0_produceTask_makeTaskBatchViewdg1586416570027').deleteLine();
                        }
                    });
                }
            },
            { text: "取消", onClick: function() { ReactAPI.destroyDialog(dialogName); } }
        ]
    });
}$$,
            'funcbody_es5', $$function setUnitEvent(event) {
    var dataGrid = ReactAPI.getComponentAPI('SupDataGrid').APIs('WOM_1.0.0_produceTask_makeTaskBatchViewdg1586416569990');
    var task = ReactAPI.getFormData();
    var process = dataGrid.getSelecteds()[0];
    var powerCode = ReactAPI.getParamsInRequestUrl()['__pc__'];
    var dialogName = 'processUnitSet';
    ReactAPI.createDialog(dialogName, {
        title: '工序工作单元',
        url: '/msService/WOM/produceTask/taskProcess/processUnitEdit?id=' + process.id + '&__pc__=' + powerCode,
        isRef: false,
        size: 1,
        buttons: [
            {
                text: "保存",
                type: "primary",
                onClick: function(event) {
                    event.ReactAPI.submitFormData("save", function(res) {
                        if (res) {
                            ReactAPI.destroyDialog(dialogName);
                            dataGrid.refreshDataByRequst({
                                type: "POST",
                                url: "/msService/WOM/produceTask/produceTask/data-dg1576028988483?datagridCode=WOM_1.0.0_produceTask_makeTaskBatchViewdg1586416569990&id=" + task.id,
                                param: { pageSize: 65535 }
                            });
                            ReactAPI.getComponentAPI('SupDataGrid').APIs('WOM_1.0.0_produceTask_makeTaskBatchViewdg1586416570027').deleteLine();
                        }
                    });
                }
            },
            { text: "取消", onClick: function() { ReactAPI.destroyDialog(dialogName); } }
        ]
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
        RAISE NOTICE 'runtime_extra_view WOM makeTaskBatchView is missing; skip process button patch';
        RETURN;
    END IF;

    patched_payload := public.adp_patch_datagrid_buttons(
        current_payload::jsonb,
        'WOM_1.0.0_produceTask_makeTaskBatchViewdg1586416569990',
        process_buttons
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
END
$do$;
