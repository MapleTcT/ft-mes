-- Source-backed WOM manual manufacturing instruction entry.
-- PostgreSQL remains the default runtime; the legacy WOM service still owns workflow persistence.

CREATE TABLE IF NOT EXISTS public.wom_manual_task_requests (
    id bigserial PRIMARY KEY,
    tenant_id varchar(64) NOT NULL DEFAULT 'default',
    request_id varchar(80) NOT NULL,
    request_hash char(64) NOT NULL,
    batch_code varchar(128) NOT NULL,
    payload_json jsonb NOT NULL,
    upstream_response jsonb,
    task_id bigint,
    status varchar(16) NOT NULL,
    error_message varchar(1000),
    created_at timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT wom_manual_task_requests_status_chk
        CHECK (status IN ('PROCESSING', 'SUCCESS', 'FAILED')),
    CONSTRAINT wom_manual_task_requests_tenant_request_uq UNIQUE (tenant_id, request_id)
);

CREATE INDEX IF NOT EXISTS idx_wom_manual_task_requests_batch
    ON public.wom_manual_task_requests (tenant_id, LOWER(TRIM(batch_code)));

-- WOM production batches are global in the recovered schema (the task table has no tenant column).
-- This closes the race where two different requestIds could create the same batch concurrently.
CREATE UNIQUE INDEX IF NOT EXISTS uq_wom_manual_task_requests_active_batch
    ON public.wom_manual_task_requests (LOWER(TRIM(batch_code)))
    WHERE status IN ('PROCESSING', 'SUCCESS');

CREATE INDEX IF NOT EXISTS idx_wom_manual_task_requests_task
    ON public.wom_manual_task_requests (task_id);

CREATE INDEX IF NOT EXISTS idx_wom_manual_task_requests_status_updated
    ON public.wom_manual_task_requests (status, updated_at DESC);

-- The recovered test baseline has one valid product/formula/line combination but no unit master.
-- Seed only that deterministic recovery fixture; real products remain governed by BaseSet master data.
INSERT INTO public.baseset_units (
    id, version, valid, cid, create_staff_id, create_time, create_department_id,
    create_position_id, group_id, owner_staff_id, owner_department_id,
    owner_position_id, position_lay_rec, status, table_no, table_info_id,
    code, name, symbol, accuracy
) VALUES (
    8991071330917399, 0, true, 1000, 1, CURRENT_TIMESTAMP, 1, 1,
    1000, 1, 1, 1, '1', 99, 'ADP_RECOVERY_UNIT_PIECE', 8991071330917399,
    'ADP_RECOVERY_PIECE', '件', '件', 0
)
ON CONFLICT (id) DO UPDATE SET
    valid = true,
    code = EXCLUDED.code,
    name = EXCLUDED.name,
    symbol = EXCLUDED.symbol,
    modify_time = CURRENT_TIMESTAMP;

UPDATE public.baseset_materials
SET produce_unit = 8991071330917399,
    modify_time = CURRENT_TIMESTAMP
WHERE code = 'ADP_E2E_20260618200829_WOM_CHECKOUTBILL_MAT'
  AND COALESCE(valid, true)
  AND produce_unit IS NULL;

CREATE OR REPLACE FUNCTION public.adp_add_wom_manual_task_button(target jsonb)
RETURNS jsonb
LANGUAGE plpgsql
AS $$
DECLARE
    item_key text;
    item_value jsonb;
    patched jsonb;
    current_buttons jsonb;
    manual_button jsonb := jsonb_build_object(
        'id', 'manualCreateTask',
        'showname', '新建指令单',
        'namekey', '新建指令单',
        'buttonstyle', 'add',
        'operatetype', 'CUSTOM',
        'isHide', false,
        'ispermission', false,
        'isPublished', true,
        'buttonoperationcode', 'makeTaskList_manualCreateTask_add_WOM_1.0.0_produceTask_makeTaskList',
        'funcname', 'onclick=''manualCreateTask(event)''',
        'funcbody', 'function manualCreateTask(event) { if (window.adpOpenWomManualTaskCreate) { window.adpOpenWomManualTaskCreate(event); } }',
        'funcbody_es5', 'function manualCreateTask(event) { if (window.adpOpenWomManualTaskCreate) { window.adpOpenWomManualTaskCreate(event); } }',
        'iscallback', false,
        'iscustomfunc', false,
        'useInMore', false,
        'isconfirm', false,
        'isSignatureConfig', false,
        'cellCode', 'cell_adp_wom_manual_task_create',
        'ecEnv', 'product',
        'regionType', 'BUTTON',
        'name', '新建指令单',
        'operateType', 'CUSTOM',
        'onclick', 'manualCreateTask(event)',
        'ONCLICK', 'manualCreateTask(event)',
        'CODE', 'makeTaskList_manualCreateTask_add_WOM_1.0.0_produceTask_makeTaskList',
        'NAME', '新建指令单',
        'ICONCLS', 'cui-btn-add',
        'USEINMORE', 'false',
        'SEPARATENUM', '0'
    );
BEGIN
    IF jsonb_typeof(target) = 'object' THEN
        patched := '{}'::jsonb;
        FOR item_key, item_value IN SELECT * FROM jsonb_each(target)
        LOOP
            patched := patched || jsonb_build_object(
                item_key,
                public.adp_add_wom_manual_task_button(item_value)
            );
        END LOOP;

        IF patched->>'DataGridCode' = 'WOM_1.0.0_produceTask_makeTaskList' THEN
            current_buttons := COALESCE(patched->'buttons', '[]'::jsonb);
            IF jsonb_typeof(current_buttons) <> 'array' THEN
                current_buttons := '[]'::jsonb;
            END IF;
            IF NOT EXISTS (
                SELECT 1
                FROM jsonb_array_elements(current_buttons) button
                WHERE button->>'id' = 'manualCreateTask'
            ) THEN
                patched := jsonb_set(
                    patched,
                    '{buttons}',
                    jsonb_build_array(manual_button) || current_buttons,
                    true
                );
            END IF;
        END IF;
        RETURN patched;
    END IF;

    IF jsonb_typeof(target) = 'array' THEN
        SELECT jsonb_agg(public.adp_add_wom_manual_task_button(value))
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
BEGIN
    SELECT udt_name = 'oid'
    INTO view_json_is_oid
    FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name = 'runtime_extra_view'
      AND column_name = 'view_json';

    IF COALESCE(view_json_is_oid, false) THEN
        SELECT convert_from(lo_get(view_json), 'UTF8')
        INTO current_payload
        FROM public.runtime_extra_view
        WHERE code = 'WOM_1.0.0_produceTask_makeTaskList';
    ELSE
        SELECT view_json::text
        INTO current_payload
        FROM public.runtime_extra_view
        WHERE code = 'WOM_1.0.0_produceTask_makeTaskList';
    END IF;

    IF current_payload IS NULL OR current_payload = '' THEN
        RAISE EXCEPTION 'runtime_extra_view WOM makeTaskList is missing';
    END IF;

    patched_payload := public.adp_add_wom_manual_task_button(current_payload::jsonb)::text;

    IF COALESCE(view_json_is_oid, false) THEN
        UPDATE public.runtime_extra_view
        SET view_json = lo_from_bytea(0, convert_to(patched_payload, 'UTF8'))
        WHERE code = 'WOM_1.0.0_produceTask_makeTaskList';
    ELSE
        UPDATE public.runtime_extra_view
        SET view_json = patched_payload
        WHERE code = 'WOM_1.0.0_produceTask_makeTaskList';
    END IF;
END $do$;

DO $do$
DECLARE
    button_config text := '<?xml version="1.0" encoding="UTF-8"?><config><button><isSignatureConfig><![CDATA[false]]></isSignatureConfig><isCustomFunc><![CDATA[false]]></isCustomFunc><code><![CDATA[WOM_1.0.0_produceTask_makeTaskList_BUTTON_manualCreateTask]]></code><moduleCode><![CDATA[WOM_1.0.0]]></moduleCode><entityCode><![CDATA[WOM_1.0.0_produceTask]]></entityCode><displayName><![CDATA[新建指令单]]></displayName><buttonStyle><![CDATA[add]]></buttonStyle><cellCode><![CDATA[cell_adp_wom_manual_task_create]]></cellCode><operateType><![CDATA[CUSTOM]]></operateType><isUseMore><![CDATA[false]]></isUseMore><isPermission><![CDATA[false]]></isPermission><isHide><![CDATA[false]]></isHide><ecEnv><![CDATA[product]]></ecEnv><isConfirm><![CDATA[false]]></isConfirm><regionType><![CDATA[BUTTON]]></regionType><name><![CDATA[manualCreateTask]]></name><isCallback><![CDATA[false]]></isCallback><functionName><![CDATA[onclick=''manualCreateTask(event)'']]></functionName><functionBody><![CDATA[function manualCreateTask(event) { if (window.adpOpenWomManualTaskCreate) { window.adpOpenWomManualTaskCreate(event); } }]]></functionBody></button></config>';
BEGIN
    INSERT INTO public.runtime_button (
        code, ec_env, version, valid, entity_code, module_code,
        button_operation_code, is_signature_config, proj_flag, is_published,
        button_align, permission_code, config, region_type, datagrid_code,
        view_code, cell_code, display_name, is_hide, is_custom_func,
        is_callback, is_permission, is_use_more, button_style, is_confirm,
        operate_type, name, create_time, modify_time
    )
    SELECT
        'WOM_1.0.0_produceTask_makeTaskList_BUTTON_manualCreateTask',
        'product', 0, true, 'WOM_1.0.0_produceTask', 'WOM_1.0.0',
        'makeTaskList_manualCreateTask_add_WOM_1.0.0_produceTask_makeTaskList',
        false, false, true, 'LEFT', NULL,
        lo_from_bytea(0, convert_to(button_config, 'UTF8')),
        'BUTTON', 'WOM_1.0.0_produceTask_makeTaskList',
        'WOM_1.0.0_produceTask_makeTaskList', 'cell_adp_wom_manual_task_create',
        '新建指令单', false, false, false, false, false, 'add', false,
        'CUSTOM', 'manualCreateTask', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    WHERE NOT EXISTS (
        SELECT 1 FROM public.runtime_button
        WHERE code = 'WOM_1.0.0_produceTask_makeTaskList_BUTTON_manualCreateTask'
    );

    UPDATE public.runtime_button
    SET valid = true,
        is_signature_config = false,
        is_published = true,
        is_hide = false,
        is_permission = false,
        is_confirm = false,
        display_name = '新建指令单',
        operate_type = 'CUSTOM',
        modify_time = CURRENT_TIMESTAMP
    WHERE code = 'WOM_1.0.0_produceTask_makeTaskList_BUTTON_manualCreateTask';
END $do$;

DO $do$
DECLARE
    button_count integer;
BEGIN
    SELECT COUNT(*)
    INTO button_count
    FROM public.runtime_button
    WHERE code = 'WOM_1.0.0_produceTask_makeTaskList_BUTTON_manualCreateTask'
      AND COALESCE(valid, true)
      AND COALESCE(is_published, false);

    IF button_count <> 1 THEN
        RAISE EXCEPTION 'WOM manual task button metadata was not installed';
    END IF;
END $do$;

DROP FUNCTION IF EXISTS public.adp_add_wom_manual_task_button(jsonb);
