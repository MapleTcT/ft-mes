CREATE TABLE IF NOT EXISTS pa_trace_snapshots (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
    source_type VARCHAR(16) NOT NULL,
    source_id BIGINT NOT NULL,
    task_id BIGINT NOT NULL,
    batch_no VARCHAR(128) NOT NULL DEFAULT '',
    source_state VARCHAR(128) NOT NULL DEFAULT '',
    metrics_json TEXT NOT NULL,
    source_updated_at TIMESTAMP,
    revision BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_pa_trace_snapshots_type CHECK (source_type IN ('TASK', 'PROCESS', 'ACTIVITY')),
    CONSTRAINT ck_pa_trace_snapshots_revision CHECK (revision > 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_pa_trace_snapshots_source
    ON pa_trace_snapshots (tenant_id, source_type, source_id);
CREATE INDEX IF NOT EXISTS idx_pa_trace_snapshots_batch
    ON pa_trace_snapshots (tenant_id, batch_no, updated_at);
CREATE INDEX IF NOT EXISTS idx_pa_trace_snapshots_task
    ON pa_trace_snapshots (tenant_id, task_id, source_type);

CREATE INDEX IF NOT EXISTS idx_wom_produce_tasks_trace_batch
    ON wom_produce_tasks (produce_batch_num, valid, id);
CREATE INDEX IF NOT EXISTS idx_wom_produce_task_exelog_trace_task
    ON wom_produce_task_exelog (task_id, create_time, id);
CREATE INDEX IF NOT EXISTS idx_wom_process_exelogs_trace_task
    ON wom_process_exelogs (task_id, act_start_time, id);
CREATE INDEX IF NOT EXISTS idx_wom_acti_exelogs_trace_task
    ON wom_acti_exelogs (task_id, act_start_time, id);
CREATE INDEX IF NOT EXISTS idx_qcs_inspects_trace_batch
    ON qcs_inspects (batch_code, create_time, id);
CREATE INDEX IF NOT EXISTS idx_qcs_inspect_reports_trace_batch
    ON qcs_inspect_reports (batch_code, create_time, id);
CREATE INDEX IF NOT EXISTS idx_qcs_un_qlf_deals_trace_batch
    ON qcs_un_qlf_deals (batch_code, create_time, id);

INSERT INTO public.runtime_view (
    code, ec_env, version, create_time, modify_time, valid, type, show_type,
    title, display_name, name, url, module_code, entity_code, has_attachment,
    only_for_query, main_view, main_ref, mobile, mobile_enable_flag, move_flag,
    extra_view, is_shadow, open_type, is_permission
)
VALUES (
    'ProcessAnalysis_1.0.0_processAnalysis_processBatchViewOut', 'product', 0,
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, true, 'LIST', 'SINGLE',
    '生产过程追溯', '生产过程追溯', 'processBatchViewOut',
    '/msService/ProcessAnalysis/processAnalysis/exelogSecond/processBatchViewOut',
    'ProcessAnalysis_1.0.0', 'ProcessAnalysis_1.0.0_processAnalysis', false,
    true, false, false, false, false, false,
    'ProcessAnalysis_1.0.0_processAnalysis_processBatchViewOut', false, 'page', false
)
ON CONFLICT (code) DO UPDATE SET
    modify_time = CURRENT_TIMESTAMP,
    valid = true,
    title = EXCLUDED.title,
    display_name = EXCLUDED.display_name,
    name = EXCLUDED.name,
    url = EXCLUDED.url,
    module_code = EXCLUDED.module_code,
    entity_code = EXCLUDED.entity_code,
    only_for_query = true,
    open_type = 'page',
    is_permission = false;

INSERT INTO public.rbac_menuinfo (
    id, version, create_time, modify_time, valid, cid, leaf, parent_id, lay_no,
    sort, code, name, name_display, module_code, namespace, url, route, target,
    menu_type, show_type, request_type, is_hide, system_default, enable, edited,
    no_restrict, status, source
)
SELECT
    9175000000000001, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, true, 1, true,
    6579610968539376, 3, 99,
    'ProcessAnalysis_1.0.0_processAnalysis_processBatchViewOut',
    '生产过程追溯', '生产过程追溯', 'ProcessAnalysis_1.0.0',
    'ProcessAnalysis',
    '/msService/ProcessAnalysis/processAnalysis/exelogSecond/processBatchViewOut',
    '/msService/ProcessAnalysis/processAnalysis/exelogSecond/processBatchViewOut',
    '_self', 1, 1, 1, false, false, true, true, 1, 0, 'ADP_POSTGRES_RECOVERY'
WHERE NOT EXISTS (
    SELECT 1 FROM public.rbac_menuinfo
    WHERE code = 'ProcessAnalysis_1.0.0_processAnalysis_processBatchViewOut'
);

UPDATE public.rbac_menuinfo
SET modify_time = CURRENT_TIMESTAMP,
    valid = true,
    enable = true,
    name = '生产过程追溯',
    name_display = '生产过程追溯',
    module_code = 'ProcessAnalysis_1.0.0',
    namespace = 'ProcessAnalysis',
    url = '/msService/ProcessAnalysis/processAnalysis/exelogSecond/processBatchViewOut',
    route = '/msService/ProcessAnalysis/processAnalysis/exelogSecond/processBatchViewOut'
WHERE code = 'ProcessAnalysis_1.0.0_processAnalysis_processBatchViewOut';
