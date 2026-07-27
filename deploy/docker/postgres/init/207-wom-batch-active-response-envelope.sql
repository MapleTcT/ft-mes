-- Make WOM activity runtime buttons understand both legacy and current API envelopes.
--
-- Current controllers return {code, data, message}. The recovered button code
-- checked only {success, data, msg}, so a successful action displayed an empty
-- warning toast. Patch both handler variants without replacing unrelated grid
-- metadata.

BEGIN;

CREATE OR REPLACE FUNCTION public.adp_patch_wom_active_button_handlers(
    target jsonb,
    start_handler text,
    end_handler text
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
        IF target->>'id' = 'startActive' THEN
            target := target || jsonb_build_object(
                'funcbody', start_handler,
                'funcbody_es5', start_handler
            );
        ELSIF target->>'id' = 'endActive' THEN
            target := target || jsonb_build_object(
                'funcbody', end_handler,
                'funcbody_es5', end_handler
            );
        END IF;

        patched := '{}'::jsonb;
        FOR item_key, item_value IN SELECT * FROM jsonb_each(target)
        LOOP
            patched := patched || jsonb_build_object(
                item_key,
                public.adp_patch_wom_active_button_handlers(
                    item_value,
                    start_handler,
                    end_handler
                )
            );
        END LOOP;
        RETURN patched;
    END IF;

    IF jsonb_typeof(target) = 'array' THEN
        SELECT jsonb_agg(
            public.adp_patch_wom_active_button_handlers(
                value,
                start_handler,
                end_handler
            )
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
    start_handler text;
    end_handler text;
    old_view_json_oid oid;
    new_view_json_oid oid;
BEGIN
    start_handler := $handler$function startActive(event) {
    var activeDataGrid = ReactAPI.getComponentAPI('SupDataGrid').APIs('WOM_1.0.0_produceTask_makeTaskBatchViewdg1586416570027');
    var activeData = activeDataGrid.getSelecteds();
    if (!activeData || activeData.length !== 1) {
        ReactAPI.showMessage("f", "请选择一条活动记录");
        return;
    }
    var activeId = activeData[0].id;
    var processDataGrid = ReactAPI.getComponentAPI('SupDataGrid').APIs('WOM_1.0.0_produceTask_makeTaskBatchViewdg1586416569990');
    var processId = activeData[0].taskProcessId.id;
    var taskId = ReactAPI.getFormData().id;
    $.ajax({
        url: "/msService/WOM/produceTask/produceTask/startActive",
        type: "get",
        async: false,
        data: { activeId: activeId },
        success: function success(res) {
            var data = res && res.data ? res.data : {};
            if (res && (res.success === true || res.code === 200) && data.success === true) {
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
                ReactAPI.showMessage("f", data.msg || (res && (res.message || res.msg)) || "活动开始失败");
            }
        },
        error: function error(xhr) {
            ReactAPI.showMessage("f", "活动开始请求失败（HTTP " + (xhr && xhr.status ? xhr.status : "未知") + "）");
        }
    });
}$handler$;

    end_handler := $handler$function endActiveEvent(event) {
    var activeDataGrid = ReactAPI.getComponentAPI('SupDataGrid').APIs('WOM_1.0.0_produceTask_makeTaskBatchViewdg1586416570027');
    var activeData = activeDataGrid.getSelecteds();
    if (!activeData || activeData.length !== 1) {
        ReactAPI.showMessage("f", "请选择一条活动记录");
        return;
    }
    var activeId = activeData[0].id;
    $.ajax({
        url: "/msService/WOM/produceTask/produceTask/endActive",
        type: "get",
        async: false,
        data: { activeId: activeId },
        success: function success(res) {
            var data = res && res.data ? res.data : {};
            if (res && (res.success === true || res.code === 200) && data.success === true) {
                ReactAPI.showMessage("s", "活动已结束！");
            } else {
                ReactAPI.showMessage("f", data.msg || (res && (res.message || res.msg)) || "活动结束失败");
            }
        },
        error: function error(xhr) {
            ReactAPI.showMessage("f", "活动结束请求失败（HTTP " + (xhr && xhr.status ? xhr.status : "未知") + "）");
        }
    });
}$handler$;

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
        WHERE code = 'WOM_1.0.0_produceTask_makeTaskBatchView'
        FOR UPDATE;
    ELSE
        SELECT view_json::text
        INTO current_payload
        FROM public.runtime_extra_view
        WHERE code = 'WOM_1.0.0_produceTask_makeTaskBatchView'
        FOR UPDATE;
    END IF;

    IF current_payload IS NULL OR current_payload = '' THEN
        RAISE EXCEPTION 'runtime_extra_view WOM makeTaskBatchView is missing';
    END IF;

    patched_payload := public.adp_patch_wom_active_button_handlers(
        current_payload::jsonb,
        start_handler,
        end_handler
    )::text;

    IF patched_payload NOT LIKE '%res.code === 200%' THEN
        RAISE EXCEPTION 'WOM activity response-envelope handlers were not patched';
    END IF;

    IF COALESCE(view_json_is_oid, false) THEN
        new_view_json_oid := lo_from_bytea(0, convert_to(patched_payload, 'UTF8'));
        UPDATE public.runtime_extra_view
        SET view_json = new_view_json_oid
        WHERE code = 'WOM_1.0.0_produceTask_makeTaskBatchView';
        IF FOUND THEN
            IF old_view_json_oid IS NOT NULL AND old_view_json_oid <> new_view_json_oid THEN
                PERFORM lo_unlink(old_view_json_oid);
            END IF;
        ELSE
            PERFORM lo_unlink(new_view_json_oid);
            RAISE EXCEPTION 'runtime_extra_view WOM makeTaskBatchView disappeared during patch';
        END IF;
    ELSE
        UPDATE public.runtime_extra_view
        SET view_json = patched_payload
        WHERE code = 'WOM_1.0.0_produceTask_makeTaskBatchView';
        IF NOT FOUND THEN
            RAISE EXCEPTION 'runtime_extra_view WOM makeTaskBatchView disappeared during patch';
        END IF;
    END IF;
END $do$;

COMMIT;
