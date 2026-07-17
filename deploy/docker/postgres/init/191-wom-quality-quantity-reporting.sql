-- Standalone bad-quantity reporting and WMS quantity allocation.
-- PostgreSQL remains the default runtime path; legacy WOM/QCS tables are read-only inputs.

CREATE TABLE IF NOT EXISTS wom_quality_quantity_reports (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
    request_id VARCHAR(128) NOT NULL,
    request_hash VARCHAR(128) NOT NULL,
    task_id BIGINT NOT NULL,
    task_no VARCHAR(128),
    source_output_id BIGINT NOT NULL,
    source_output_no VARCHAR(128),
    batch_no VARCHAR(128),
    reported_quantity NUMERIC(20, 6) NOT NULL,
    good_quantity NUMERIC(20, 6) NOT NULL,
    bad_quantity NUMERIC(20, 6) NOT NULL,
    unit_code VARCHAR(64),
    reason_code VARCHAR(64) NOT NULL,
    reason_text VARCHAR(1000),
    qcs_inspect_id BIGINT,
    qcs_report_id BIGINT,
    qcs_deal_id BIGINT,
    status VARCHAR(32) NOT NULL DEFAULT 'CONFIRMED',
    version BIGINT NOT NULL DEFAULT 0,
    wms_sync_state VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    wms_sync_message VARCHAR(1000),
    wms_synced_at TIMESTAMP WITH TIME ZONE,
    confirmed_by VARCHAR(128) NOT NULL,
    confirmed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reversal_reason VARCHAR(1000),
    reversed_by VARCHAR(128),
    reversed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_wom_quality_quantity_positive
        CHECK (
            reported_quantity > 0
            AND good_quantity >= 0
            AND bad_quantity > 0
            AND reported_quantity = good_quantity + bad_quantity
        ),
    CONSTRAINT ck_wom_quality_quantity_status
        CHECK (status IN ('CONFIRMED', 'REVERSAL_PENDING', 'REVERSED')),
    CONSTRAINT ck_wom_quality_quantity_wms_sync
        CHECK (wms_sync_state IN ('PENDING', 'APPLIED', 'FAILED'))
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_wom_quality_quantity_request
    ON wom_quality_quantity_reports (tenant_id, request_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_wom_quality_quantity_active_output
    ON wom_quality_quantity_reports (tenant_id, source_output_id)
    WHERE status IN ('CONFIRMED', 'REVERSAL_PENDING');
CREATE INDEX IF NOT EXISTS idx_wom_quality_quantity_task
    ON wom_quality_quantity_reports (tenant_id, task_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_wom_quality_quantity_sync
    ON wom_quality_quantity_reports (wms_sync_state, status, updated_at);

CREATE TABLE IF NOT EXISTS wom_quality_quantity_events (
    id BIGSERIAL PRIMARY KEY,
    report_id BIGINT NOT NULL REFERENCES wom_quality_quantity_reports(id) ON DELETE CASCADE,
    event_no BIGINT NOT NULL,
    event_type VARCHAR(32) NOT NULL,
    actor VARCHAR(128) NOT NULL,
    reason VARCHAR(1000),
    payload TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_wom_quality_quantity_event_type
        CHECK (event_type IN (
            'CONFIRMED', 'WMS_SYNC_APPLIED', 'WMS_SYNC_FAILED',
            'QUALITY_LINKED', 'REVERSAL_REQUESTED', 'REVERSED'
        ))
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_wom_quality_quantity_event_no
    ON wom_quality_quantity_events (report_id, event_no);
CREATE INDEX IF NOT EXISTS idx_wom_quality_quantity_event_created
    ON wom_quality_quantity_events (report_id, created_at);

ALTER TABLE wms_stock_documents
    ADD COLUMN IF NOT EXISTS quantity_allocation_state VARCHAR(32) NOT NULL DEFAULT 'NONE';

ALTER TABLE wms_stock_document_lines
    ADD COLUMN IF NOT EXISTS reported_quantity NUMERIC(20, 6);
ALTER TABLE wms_stock_document_lines
    ADD COLUMN IF NOT EXISTS good_quantity NUMERIC(20, 6);
ALTER TABLE wms_stock_document_lines
    ADD COLUMN IF NOT EXISTS bad_quantity NUMERIC(20, 6);

UPDATE wms_stock_document_lines
SET reported_quantity = COALESCE(reported_quantity, quantity),
    good_quantity = COALESCE(good_quantity, quantity),
    bad_quantity = COALESCE(bad_quantity, 0)
WHERE reported_quantity IS NULL
   OR good_quantity IS NULL
   OR bad_quantity IS NULL;

ALTER TABLE wms_stock_document_lines
    ALTER COLUMN reported_quantity SET NOT NULL,
    ALTER COLUMN good_quantity SET NOT NULL,
    ALTER COLUMN bad_quantity SET NOT NULL;
ALTER TABLE wms_stock_document_lines
    ALTER COLUMN reported_quantity SET DEFAULT 0,
    ALTER COLUMN good_quantity SET DEFAULT 0,
    ALTER COLUMN bad_quantity SET DEFAULT 0;

ALTER TABLE wms_stock_document_lines
    DROP CONSTRAINT IF EXISTS ck_wms_stock_document_lines_allocation;
ALTER TABLE wms_stock_document_lines
    ADD CONSTRAINT ck_wms_stock_document_lines_allocation
    CHECK (
        reported_quantity = quantity
        AND reported_quantity = good_quantity + bad_quantity
        AND good_quantity >= 0
        AND bad_quantity >= 0
    );

ALTER TABLE wms_stock_document_lines
    DROP CONSTRAINT IF EXISTS ck_wms_stock_document_lines_quality;
ALTER TABLE wms_stock_document_lines
    ADD CONSTRAINT ck_wms_stock_document_lines_quality
    CHECK (quality_status IN ('PENDING', 'QUALIFIED', 'PARTIAL', 'UNQUALIFIED'));

ALTER TABLE wms_stock_documents
    DROP CONSTRAINT IF EXISTS ck_wms_stock_documents_quality;
ALTER TABLE wms_stock_documents
    ADD CONSTRAINT ck_wms_stock_documents_quality
    CHECK (quality_status IN ('PENDING', 'QUALIFIED', 'PARTIAL', 'UNQUALIFIED'));

ALTER TABLE wms_stock_documents
    DROP CONSTRAINT IF EXISTS ck_wms_stock_documents_allocation;
ALTER TABLE wms_stock_documents
    ADD CONSTRAINT ck_wms_stock_documents_allocation
    CHECK (quantity_allocation_state IN ('NONE', 'ACTIVE'));

ALTER TABLE wms_inventory_transactions
    DROP CONSTRAINT IF EXISTS ck_wms_inventory_transactions_type;
ALTER TABLE wms_inventory_transactions
    ADD CONSTRAINT ck_wms_inventory_transactions_type
    CHECK (transaction_type IN (
        'COMPLETION_INBOUND', 'PRODUCTION_ISSUE', 'QUALITY_RELEASE', 'QUALITY_HOLD',
        'QUALITY_ALLOCATION_HOLD', 'QUALITY_ALLOCATION_RELEASE'
    ));

CREATE TABLE IF NOT EXISTS wms_quality_allocations (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
    source_system VARCHAR(32) NOT NULL DEFAULT 'WOM',
    source_line_id VARCHAR(128) NOT NULL,
    task_id VARCHAR(128) NOT NULL,
    quality_report_id VARCHAR(128) NOT NULL,
    total_quantity NUMERIC(20, 6) NOT NULL,
    good_quantity NUMERIC(20, 6) NOT NULL,
    bad_quantity NUMERIC(20, 6) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_wms_quality_allocations_quantity
        CHECK (
            total_quantity > 0
            AND good_quantity >= 0
            AND bad_quantity > 0
            AND total_quantity = good_quantity + bad_quantity
        ),
    CONSTRAINT ck_wms_quality_allocations_status
        CHECK (status IN ('ACTIVE', 'REVERSED'))
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_wms_quality_allocations_source
    ON wms_quality_allocations (tenant_id, source_system, source_line_id);
CREATE INDEX IF NOT EXISTS idx_wms_quality_allocations_report
    ON wms_quality_allocations (tenant_id, quality_report_id);

CREATE TABLE IF NOT EXISTS wms_quality_allocation_events (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
    event_key VARCHAR(256) NOT NULL,
    allocation_id BIGINT NOT NULL REFERENCES wms_quality_allocations(id) ON DELETE CASCADE,
    event_type VARCHAR(32) NOT NULL,
    quality_report_id VARCHAR(128) NOT NULL,
    payload TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_wms_quality_allocation_event_type
        CHECK (event_type IN ('APPLY', 'REVERSE'))
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_wms_quality_allocation_events_key
    ON wms_quality_allocation_events (tenant_id, event_key);

CREATE OR REPLACE FUNCTION public.adp_add_quality_quantity_button(
    target jsonb,
    target_grid_code text,
    button_id text,
    show_name text,
    operation_code text,
    cell_code text,
    window_handler text
)
RETURNS jsonb
LANGUAGE plpgsql
AS $$
DECLARE
    item_key text;
    item_value jsonb;
    patched jsonb;
    current_buttons jsonb;
    handler_body text := format(
        'function %s(event) { if (window.%s) { window.%s(event); } }',
        button_id, window_handler, window_handler
    );
    button_config jsonb := jsonb_build_object(
        'id', button_id,
        'showname', show_name,
        'namekey', show_name,
        'buttonstyle', 'edit',
        'operatetype', 'CUSTOM',
        'isHide', false,
        'ispermission', false,
        'isPublished', true,
        'buttonoperationcode', operation_code,
        'funcname', format('onclick=''%s(event)''', button_id),
        'funcbody', handler_body,
        'funcbody_es5', handler_body,
        'iscallback', false,
        'iscustomfunc', false,
        'useInMore', false,
        'isconfirm', false,
        'isSignatureConfig', false,
        'cellCode', cell_code,
        'ecEnv', 'product',
        'regionType', 'BUTTON',
        'name', show_name,
        'operateType', 'CUSTOM',
        'onclick', format('%s(event)', button_id),
        'ONCLICK', format('%s(event)', button_id),
        'CODE', operation_code,
        'NAME', show_name,
        'ICONCLS', 'cui-btn-edit',
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
                public.adp_add_quality_quantity_button(
                    item_value,
                    target_grid_code,
                    button_id,
                    show_name,
                    operation_code,
                    cell_code,
                    window_handler
                )
            );
        END LOOP;

        IF patched->>'DataGridCode' = target_grid_code THEN
            current_buttons := COALESCE(patched->'buttons', '[]'::jsonb);
            IF jsonb_typeof(current_buttons) <> 'array' THEN
                current_buttons := '[]'::jsonb;
            END IF;
            IF NOT EXISTS (
                SELECT 1
                FROM jsonb_array_elements(current_buttons) button
                WHERE button->>'id' = button_id
            ) THEN
                patched := jsonb_set(
                    patched,
                    '{buttons}',
                    current_buttons || jsonb_build_array(button_config),
                    true
                );
            END IF;
        END IF;
        RETURN patched;
    END IF;

    IF jsonb_typeof(target) = 'array' THEN
        SELECT jsonb_agg(
            public.adp_add_quality_quantity_button(
                value,
                target_grid_code,
                button_id,
                show_name,
                operation_code,
                cell_code,
                window_handler
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
    target record;
    current_payload text;
    patched_payload text;
BEGIN
    SELECT udt_name = 'oid'
    INTO view_json_is_oid
    FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name = 'runtime_extra_view'
      AND column_name = 'view_json';

    FOR target IN
        SELECT * FROM (VALUES
            (
                'WOM_1.0.0_produceTask_makeTaskList',
                'WOM_1.0.0_produceTask_makeTaskList',
                'badQuantityReport',
                '不良数量',
                'makeTaskList_badQuantityReport_edit_WOM_1.0.0_produceTask_makeTaskList',
                'cell_adp_wom_bad_quantity',
                'adpOpenWomBadQuantityReport'
            ),
            (
                'QCS_5.0.0.0_inspectReport_manuInspReportEdit',
                'QCS_5.0.0.0_inspectReport_manuInspReportEditdg1591145511105',
                'badQuantityReportQcs',
                '不良数量',
                'manuInspReportEdit_badQuantityReportQcs_edit_QCS_5.0.0.0_inspectReport',
                'cell_adp_qcs_bad_quantity',
                'adpOpenQcsBadQuantityReport'
            )
        ) AS targets(
            view_code,
            grid_code,
            button_id,
            show_name,
            operation_code,
            cell_code,
            window_handler
        )
    LOOP
        IF COALESCE(view_json_is_oid, false) THEN
            SELECT convert_from(lo_get(view_json), 'UTF8')
            INTO current_payload
            FROM public.runtime_extra_view
            WHERE code = target.view_code;
        ELSE
            SELECT view_json::text
            INTO current_payload
            FROM public.runtime_extra_view
            WHERE code = target.view_code;
        END IF;

        IF current_payload IS NULL OR current_payload = '' THEN
            RAISE EXCEPTION 'runtime_extra_view % is missing', target.view_code;
        END IF;

        patched_payload := public.adp_add_quality_quantity_button(
            current_payload::jsonb,
            target.grid_code,
            target.button_id,
            target.show_name,
            target.operation_code,
            target.cell_code,
            target.window_handler
        )::text;

        IF COALESCE(view_json_is_oid, false) THEN
            UPDATE public.runtime_extra_view
            SET view_json = lo_from_bytea(0, convert_to(patched_payload, 'UTF8'))
            WHERE code = target.view_code;
        ELSE
            UPDATE public.runtime_extra_view
            SET view_json = patched_payload
            WHERE code = target.view_code;
        END IF;
    END LOOP;
END $do$;

DO $do$
DECLARE
    target record;
    button_config text;
BEGIN
    FOR target IN
        SELECT * FROM (VALUES
            (
                'WOM_1.0.0_produceTask_makeTaskList_BUTTON_badQuantityReport',
                'WOM_1.0.0_produceTask',
                'WOM_1.0.0',
                'makeTaskList_badQuantityReport_edit_WOM_1.0.0_produceTask_makeTaskList',
                'WOM_1.0.0_produceTask_makeTaskList',
                'WOM_1.0.0_produceTask_makeTaskList',
                'cell_adp_wom_bad_quantity',
                'badQuantityReport',
                'adpOpenWomBadQuantityReport'
            ),
            (
                'QCS_5.0.0.0_inspectReport_manuInspReportEdit_BUTTON_badQuantityReportQcs',
                'QCS_5.0.0.0_inspectReport',
                'QCS_5.0.0.0',
                'manuInspReportEdit_badQuantityReportQcs_edit_QCS_5.0.0.0_inspectReport',
                'QCS_5.0.0.0_inspectReport_manuInspReportEditdg1591145511105',
                'QCS_5.0.0.0_inspectReport_manuInspReportEdit',
                'cell_adp_qcs_bad_quantity',
                'badQuantityReportQcs',
                'adpOpenQcsBadQuantityReport'
            )
        ) AS targets(
            code,
            entity_code,
            module_code,
            operation_code,
            datagrid_code,
            view_code,
            cell_code,
            button_name,
            window_handler
        )
    LOOP
        button_config := format(
            '<?xml version="1.0" encoding="UTF-8"?><config><button>'
            || '<isSignatureConfig><![CDATA[false]]></isSignatureConfig>'
            || '<isCustomFunc><![CDATA[false]]></isCustomFunc>'
            || '<code><![CDATA[%s]]></code><moduleCode><![CDATA[%s]]></moduleCode>'
            || '<entityCode><![CDATA[%s]]></entityCode><displayName><![CDATA[不良数量]]></displayName>'
            || '<buttonStyle><![CDATA[edit]]></buttonStyle><cellCode><![CDATA[%s]]></cellCode>'
            || '<operateType><![CDATA[CUSTOM]]></operateType><isUseMore><![CDATA[false]]></isUseMore>'
            || '<isPermission><![CDATA[false]]></isPermission><isHide><![CDATA[false]]></isHide>'
            || '<ecEnv><![CDATA[product]]></ecEnv><isConfirm><![CDATA[false]]></isConfirm>'
            || '<regionType><![CDATA[BUTTON]]></regionType><name><![CDATA[%s]]></name>'
            || '<isCallback><![CDATA[false]]></isCallback>'
            || '<functionName><![CDATA[onclick=''%s(event)'']]></functionName>'
            || '<functionBody><![CDATA[function %s(event) { if (window.%s) { window.%s(event); } }]]></functionBody>'
            || '</button></config>',
            target.code,
            target.module_code,
            target.entity_code,
            target.cell_code,
            target.button_name,
            target.button_name,
            target.button_name,
            target.window_handler,
            target.window_handler
        );

        INSERT INTO public.runtime_button (
            code, ec_env, version, valid, entity_code, module_code,
            button_operation_code, is_signature_config, proj_flag, is_published,
            button_align, permission_code, config, region_type, datagrid_code,
            view_code, cell_code, display_name, is_hide, is_custom_func,
            is_callback, is_permission, is_use_more, button_style, is_confirm,
            operate_type, name, create_time, modify_time
        )
        SELECT
            target.code, 'product', 0, true, target.entity_code, target.module_code,
            target.operation_code, false, false, true, 'LEFT', NULL,
            lo_from_bytea(0, convert_to(button_config, 'UTF8')),
            'BUTTON', target.datagrid_code, target.view_code, target.cell_code,
            '不良数量', false, false, false, false, false, 'edit', false,
            'CUSTOM', target.button_name, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
        WHERE NOT EXISTS (
            SELECT 1 FROM public.runtime_button WHERE code = target.code
        );

        UPDATE public.runtime_button
        SET valid = true,
            is_signature_config = false,
            is_published = true,
            is_hide = false,
            is_permission = false,
            is_confirm = false,
            display_name = '不良数量',
            operate_type = 'CUSTOM',
            modify_time = CURRENT_TIMESTAMP
        WHERE code = target.code;
    END LOOP;
END $do$;
