-- Keep the recovered WOM makeTaskList toolbar usable in the PostgreSQL test runtime.
--
-- The legacy imported button JSON marks the production task toolbar actions as
-- electronic-signature buttons and leaves them unpublished. In the recovered
-- Linux/PostgreSQL test environment that wrapper opens an empty confirm dialog
-- before the real custom event, which blocks normal browser clicks. The imported
-- JSON also stores isconfirm as the string "false"; the front-end treats that
-- string as truthy and opens the same empty confirm dialog. The business event
-- functions still perform their own state checks, so disable only those wrappers
-- and mark these restored toolbar buttons as published.

WITH wom_toolbar_i18n_seed(i18n_key, i18n_value) AS (
    VALUES
        ('SupDatagrid.button.error', '请选择一条记录进行操作！'),
        ('SupDatagrid.button.tit', '提示'),
        ('ec.common.tableNo', '单据编号'),
        ('ec.engine.view.dealsuccess', '操作成功'),
        ('WOM.custom.random1623129307428', '指令单已保持！'),
        ('WOM.custom.random1623129401596', '指令单已重启！'),
        ('WOM.custom.randon1575958091725', '只有非批控的工单才可以操作！'),
        ('WOM.custom.randon1575958171058', '请先将指令单生效再进行操作！'),
        ('WOM.custom.randon1575958246066', '只有【待执行】的指令单可以开始！'),
        ('WOM.custom.randon1575958861853', '只有【执行中】或者【已暂停】的指令单可以结束！'),
        ('WOM.custom.randon1575959595767', '只有【执行中】的指令单可以保持！'),
        ('WOM.custom.randon1575968753369', '只有【已保持】的指令单可以重启！'),
        ('WOM.custom.randon1591583966456', '该批次不能提前放料！'),
        ('WOM.custom.randon1591599292328', '提前投料失败!'),
        ('WOM.custom.randon1591755924139', '操作成功！'),
        ('WOM.custom.randon1592209666643', '【{0}】，是否继续放料？'),
        ('WOM.custom.randon1596434273127', '是'),
        ('WOM.custom.randon1596434330969', '否'),
        ('WOM.custom.randon1597044938864', '只有【执行中】的指令单允许提前放料！'),
        ('WOM.custom.randon1597055078126', '是否提前放料？'),
        ('WOM.custom.randon1597211462498', '所选指令单未生效！'),
        ('WOM.custom.randon1597226087560', '该指令单检验状态为【{0}】'),
        ('WOM.custom.randon1597227115284', ',检验结论为【{}】'),
        ('WOM.custom.randon1597231220469', '处理成功！'),
        ('WOM.custom.randon1597309436125', '是否发起检验申请？'),
        ('WOM.custom.randon1597996619261', '只有【执行中】的指令单允许请检！'),
        ('WOM.custom.randon1598424040495', '指令单已生成产品检验申请单，检验状态【待检】，是否发起检验申请？'),
        ('WOM.custom.randon1598676953828', '该指令单产品无需质检！'),
        ('WOM.custom.randon1602465957459', '当前指令单不允许该操作！'),
        ('WOM.custom.randon1602655303748', '指令单已开始！')
),
wom_toolbar_i18n_languages(langu_code) AS (
    VALUES ('zh_CN'), ('zh_HK')
),
wom_toolbar_i18n_rows AS (
    SELECT
        6579000000168000::bigint + row_number() OVER (ORDER BY seed.i18n_key, lang.langu_code) AS id,
        seed.i18n_key,
        seed.i18n_value,
        lang.langu_code
    FROM wom_toolbar_i18n_seed seed
    CROSS JOIN wom_toolbar_i18n_languages lang
)
UPDATE public.supfusion_i18n_resource existing
SET i18n_value = seed.i18n_value,
    valid = '1',
    modifier = 'system',
    modify_time = CURRENT_TIMESTAMP,
    modify_staff_id = 1
FROM wom_toolbar_i18n_rows seed
WHERE existing.i18n_key = seed.i18n_key
  AND existing.langu_code = seed.langu_code
  AND COALESCE(existing.tenant_id, 'dt') = 'dt'
  AND (
      existing.i18n_value IS NULL
      OR existing.i18n_value = ''
      OR existing.i18n_value = existing.i18n_key
  );

WITH wom_toolbar_i18n_seed(i18n_key, i18n_value) AS (
    VALUES
        ('SupDatagrid.button.error', '请选择一条记录进行操作！'),
        ('SupDatagrid.button.tit', '提示'),
        ('ec.common.tableNo', '单据编号'),
        ('ec.engine.view.dealsuccess', '操作成功'),
        ('WOM.custom.random1623129307428', '指令单已保持！'),
        ('WOM.custom.random1623129401596', '指令单已重启！'),
        ('WOM.custom.randon1575958091725', '只有非批控的工单才可以操作！'),
        ('WOM.custom.randon1575958171058', '请先将指令单生效再进行操作！'),
        ('WOM.custom.randon1575958246066', '只有【待执行】的指令单可以开始！'),
        ('WOM.custom.randon1575958861853', '只有【执行中】或者【已暂停】的指令单可以结束！'),
        ('WOM.custom.randon1575959595767', '只有【执行中】的指令单可以保持！'),
        ('WOM.custom.randon1575968753369', '只有【已保持】的指令单可以重启！'),
        ('WOM.custom.randon1591583966456', '该批次不能提前放料！'),
        ('WOM.custom.randon1591599292328', '提前投料失败!'),
        ('WOM.custom.randon1591755924139', '操作成功！'),
        ('WOM.custom.randon1592209666643', '【{0}】，是否继续放料？'),
        ('WOM.custom.randon1596434273127', '是'),
        ('WOM.custom.randon1596434330969', '否'),
        ('WOM.custom.randon1597044938864', '只有【执行中】的指令单允许提前放料！'),
        ('WOM.custom.randon1597055078126', '是否提前放料？'),
        ('WOM.custom.randon1597211462498', '所选指令单未生效！'),
        ('WOM.custom.randon1597226087560', '该指令单检验状态为【{0}】'),
        ('WOM.custom.randon1597227115284', ',检验结论为【{}】'),
        ('WOM.custom.randon1597231220469', '处理成功！'),
        ('WOM.custom.randon1597309436125', '是否发起检验申请？'),
        ('WOM.custom.randon1597996619261', '只有【执行中】的指令单允许请检！'),
        ('WOM.custom.randon1598424040495', '指令单已生成产品检验申请单，检验状态【待检】，是否发起检验申请？'),
        ('WOM.custom.randon1598676953828', '该指令单产品无需质检！'),
        ('WOM.custom.randon1602465957459', '当前指令单不允许该操作！'),
        ('WOM.custom.randon1602655303748', '指令单已开始！')
),
wom_toolbar_i18n_languages(langu_code) AS (
    VALUES ('zh_CN'), ('zh_HK')
),
wom_toolbar_i18n_rows AS (
    SELECT
        6579000000168000::bigint + row_number() OVER (ORDER BY seed.i18n_key, lang.langu_code) AS id,
        seed.i18n_key,
        seed.i18n_value,
        lang.langu_code
    FROM wom_toolbar_i18n_seed seed
    CROSS JOIN wom_toolbar_i18n_languages lang
)
INSERT INTO public.supfusion_i18n_resource (
  id, i18n_key, i18n_value, langu_code, module_code, module_version_code, valid,
  tenant_id, creator, create_time, create_staff_id, modifier, modify_time, modify_staff_id
)
SELECT
    seed.id,
    seed.i18n_key,
    seed.i18n_value,
    seed.langu_code,
    'WOM',
    NULL,
    '1',
    'dt',
    'system',
    CURRENT_TIMESTAMP,
    1,
    'system',
    CURRENT_TIMESTAMP,
    1
FROM wom_toolbar_i18n_rows seed
WHERE NOT EXISTS (
  SELECT 1
  FROM public.supfusion_i18n_resource existing
  WHERE existing.i18n_key = seed.i18n_key
    AND existing.langu_code = seed.langu_code
    AND COALESCE(existing.tenant_id, 'dt') = 'dt'
);

CREATE OR REPLACE FUNCTION public.adp_patch_wom_maketasklist_toolbar_buttons(target jsonb)
RETURNS jsonb
LANGUAGE plpgsql
AS $$
DECLARE
    item_key text;
    item_value jsonb;
    patched jsonb;
    target_button_ids text[] := ARRAY[
        'startTask',
        'pauseTask',
        'recoveryTask',
        'stopTask',
        'earlyPutIn',
        'manuInspect',
        'prodprocessView',
        'generateCode'
    ];
BEGIN
    IF jsonb_typeof(target) = 'object' THEN
        IF target->>'key' = 'tableNo'
           AND target->>'namekey' = 'ec.common.tableNo' THEN
            target := jsonb_set(target, '{namekey}', to_jsonb('单据编号'::text), true);
        END IF;

        IF target->>'id' = ANY (target_button_ids)
           AND COALESCE(target->>'operateType', target->>'operatetype') = 'CUSTOM'
           AND COALESCE(target->>'CODE', target->>'buttonoperationcode', '') LIKE 'makeTaskList_%_WOM_1.0.0_produceTask_makeTaskList' THEN
            target := jsonb_set(target, '{isSignatureConfig}', 'false'::jsonb, true);
            target := jsonb_set(target, '{isPublished}', 'true'::jsonb, true);
            target := jsonb_set(target, '{isconfirm}', 'false'::jsonb, true);
        END IF;

        patched := '{}'::jsonb;
        FOR item_key, item_value IN SELECT * FROM jsonb_each(target)
        LOOP
            patched := patched || jsonb_build_object(
                item_key,
                public.adp_patch_wom_maketasklist_toolbar_buttons(item_value)
            );
        END LOOP;
        RETURN patched;
    END IF;

    IF jsonb_typeof(target) = 'array' THEN
        SELECT jsonb_agg(public.adp_patch_wom_maketasklist_toolbar_buttons(value))
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
        RAISE NOTICE 'runtime_extra_view WOM_1.0.0_produceTask_makeTaskList is missing; skip toolbar patch';
        RETURN;
    END IF;

    patched_payload := public.adp_patch_wom_maketasklist_toolbar_buttons(current_payload::jsonb)::text;

    IF COALESCE(view_json_is_oid, false) THEN
        UPDATE public.runtime_extra_view
        SET view_json = lo_from_bytea(0, convert_to(patched_payload, 'UTF8'))
        WHERE code = 'WOM_1.0.0_produceTask_makeTaskList';
    ELSE
        UPDATE public.runtime_extra_view
        SET view_json = patched_payload
        WHERE code = 'WOM_1.0.0_produceTask_makeTaskList';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = 'public'
          AND table_name = 'ec_extra_view'
    ) THEN
        UPDATE public.ec_extra_view
        SET view_json = patched_payload
        WHERE code = 'WOM_1.0.0_produceTask_makeTaskList';
    END IF;
END $do$;
