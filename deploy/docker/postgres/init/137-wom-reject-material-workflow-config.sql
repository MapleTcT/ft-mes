-- Register recovered WOM reject-material workflows with the legacy JBPM
-- runtime on PostgreSQL.
--
-- Source evidence:
-- - RejectMaterialController saves / submits through
--   /WOM/rejectMaterilal/rejectMaterial/*/save.
-- - Remote wf_deployment contains recovered process_xml_text_backup for
--   batchRejectFlow, materiaReject, and prePareRejectFlow, but the JBPM
--   deployment/deployprop/lob rows were absent, causing
--   ec.workflow.noProcessDefinition during save.
-- - init.xml defines WOM_REJECT_MATERIALS_DI and WOM_REJECT_MATERIALS_SV, but
--   the recovered PostgreSQL schema missed the DI table and exposed only a
--   narrow SV view.

CREATE TABLE IF NOT EXISTS public.wom_reject_materials_di (
    id bigint PRIMARY KEY,
    version integer,
    create_staff_id bigint,
    create_time timestamp without time zone,
    modify_staff_id bigint,
    modify_time timestamp without time zone,
    delete_staff_id bigint,
    delete_time timestamp without time zone,
    valid boolean,
    cid bigint,
    sort integer,
    main_obj bigint,
    staff bigint,
    recalled_flag boolean,
    user_agent varchar(2000),
    table_info_id bigint,
    activity_name varchar(510),
    assign_staff varchar(4000),
    assign_staff_id varchar(2000),
    comments varchar(4000),
    dealinfo_type varchar(255),
    entity_code varchar(510),
    instance_id varchar(510),
    outcome varchar(510),
    outcome_des varchar(2000),
    outcome_des_zh_cn varchar(2000),
    pending_create_time timestamp without time zone,
    process_key varchar(510),
    process_version integer,
    proxy_staff varchar(2000),
    proxy_staff_ids varchar(2000),
    signature varchar(400),
    task_description varchar(2000),
    task_description_zh_cn varchar(2000),
    user_id bigint
);

DO $$
DECLARE
    column_def record;
BEGIN
    FOR column_def IN
        SELECT *
        FROM (
            VALUES
                ('id', 'bigint'),
                ('version', 'integer'),
                ('create_staff_id', 'bigint'),
                ('create_time', 'timestamp without time zone'),
                ('modify_staff_id', 'bigint'),
                ('modify_time', 'timestamp without time zone'),
                ('delete_staff_id', 'bigint'),
                ('delete_time', 'timestamp without time zone'),
                ('valid', 'boolean'),
                ('cid', 'bigint'),
                ('sort', 'integer'),
                ('main_obj', 'bigint'),
                ('staff', 'bigint'),
                ('recalled_flag', 'boolean'),
                ('user_agent', 'varchar(2000)'),
                ('table_info_id', 'bigint'),
                ('activity_name', 'varchar(510)'),
                ('assign_staff', 'varchar(4000)'),
                ('assign_staff_id', 'varchar(2000)'),
                ('comments', 'varchar(4000)'),
                ('dealinfo_type', 'varchar(255)'),
                ('entity_code', 'varchar(510)'),
                ('instance_id', 'varchar(510)'),
                ('outcome', 'varchar(510)'),
                ('outcome_des', 'varchar(2000)'),
                ('outcome_des_zh_cn', 'varchar(2000)'),
                ('pending_create_time', 'timestamp without time zone'),
                ('process_key', 'varchar(510)'),
                ('process_version', 'integer'),
                ('proxy_staff', 'varchar(2000)'),
                ('proxy_staff_ids', 'varchar(2000)'),
                ('signature', 'varchar(400)'),
                ('task_description', 'varchar(2000)'),
                ('task_description_zh_cn', 'varchar(2000)'),
                ('user_id', 'bigint')
        ) AS columns(name, definition)
    LOOP
        EXECUTE format(
            'ALTER TABLE public.wom_reject_materials_di ADD COLUMN IF NOT EXISTS %I %s',
            column_def.name,
            column_def.definition
        );
    END LOOP;
END $$;

ALTER TABLE public.wom_reject_materials_di
    ALTER COLUMN assign_staff TYPE varchar(4000),
    ALTER COLUMN comments TYPE varchar(4000),
    ALTER COLUMN signature TYPE varchar(400);

CREATE INDEX IF NOT EXISTS idx_wom_reject_materials_di_table_info
    ON public.wom_reject_materials_di(table_info_id);

CREATE INDEX IF NOT EXISTS idx_wom_reject_materials_di_main_obj
    ON public.wom_reject_materials_di(main_obj);

CREATE INDEX IF NOT EXISTS idx_wom_reject_materials_di_process_key
    ON public.wom_reject_materials_di(process_key);

CREATE INDEX IF NOT EXISTS idx_wfm_task_pending_reject_material_model
    ON public.wfm_task_pending(process_key, model_id);

CREATE INDEX IF NOT EXISTS idx_wfm_task_pending_reject_material_table_info
    ON public.wfm_task_pending(process_key, table_info_id);

CREATE OR REPLACE FUNCTION public.adp_cleanup_wom_reject_material_pending()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF coalesce(OLD.valid, true) = true
       AND coalesce(NEW.valid, false) = false THEN
        DELETE FROM public.wfm_task_pending pending
        WHERE pending.process_key IN ('batchRejectFlow', 'materiaReject', 'prePareRejectFlow')
          AND (
              pending.model_id = NEW.id
              OR pending.table_info_id = NEW.table_info_id
              OR pending.table_no = NEW.table_no
          );
    END IF;
    RETURN NEW;
END $$;

DROP TRIGGER IF EXISTS trg_wom_reject_material_pending_cleanup
    ON public.wom_reject_materials;

CREATE TRIGGER trg_wom_reject_material_pending_cleanup
AFTER UPDATE OF valid ON public.wom_reject_materials
FOR EACH ROW
EXECUTE FUNCTION public.adp_cleanup_wom_reject_material_pending();

DELETE FROM public.wfm_task_pending pending
USING public.wom_reject_materials reject_material
WHERE pending.process_key IN ('batchRejectFlow', 'materiaReject', 'prePareRejectFlow')
  AND coalesce(reject_material.valid, false) = false
  AND (
      pending.model_id = reject_material.id
      OR pending.table_info_id = reject_material.table_info_id
      OR pending.table_no = reject_material.table_no
  );

DO $$
DECLARE
    rel_kind char;
BEGIN
    SELECT c.relkind INTO rel_kind
    FROM pg_class c
    JOIN pg_namespace n ON n.oid = c.relnamespace
    WHERE n.nspname = 'public'
      AND c.relname = 'wom_reject_materials_sv';

    IF to_regclass('public.wom_reject_materials') IS NOT NULL
       AND (rel_kind IS NULL OR rel_kind = 'v') THEN
        EXECUTE $view$
CREATE OR REPLACE VIEW public.wom_reject_materials_sv AS
SELECT
  table_info_id,
  owner_staff_id AS staff,
  id,
  version,
  true::boolean AS valid,
  id AS main_obj,
  create_staff_id,
  modify_staff_id,
  delete_staff_id,
  create_time,
  modify_time,
  delete_time
FROM public.wom_reject_materials
WHERE table_info_id IS NOT NULL
  AND owner_staff_id IS NOT NULL
$view$;
    ELSIF rel_kind = 'r' THEN
        ALTER TABLE public.wom_reject_materials_sv ADD COLUMN IF NOT EXISTS id bigint;
        ALTER TABLE public.wom_reject_materials_sv ADD COLUMN IF NOT EXISTS version integer DEFAULT 0;
        ALTER TABLE public.wom_reject_materials_sv ADD COLUMN IF NOT EXISTS valid boolean DEFAULT true;
        ALTER TABLE public.wom_reject_materials_sv ADD COLUMN IF NOT EXISTS main_obj bigint;
        ALTER TABLE public.wom_reject_materials_sv ADD COLUMN IF NOT EXISTS create_staff_id bigint;
        ALTER TABLE public.wom_reject_materials_sv ADD COLUMN IF NOT EXISTS modify_staff_id bigint;
        ALTER TABLE public.wom_reject_materials_sv ADD COLUMN IF NOT EXISTS delete_staff_id bigint;
        ALTER TABLE public.wom_reject_materials_sv ADD COLUMN IF NOT EXISTS create_time timestamp without time zone;
        ALTER TABLE public.wom_reject_materials_sv ADD COLUMN IF NOT EXISTS modify_time timestamp without time zone;
        ALTER TABLE public.wom_reject_materials_sv ADD COLUMN IF NOT EXISTS delete_time timestamp without time zone;
        CREATE INDEX IF NOT EXISTS idx_wom_reject_materials_sv_main_obj
            ON public.wom_reject_materials_sv(main_obj, valid);
        CREATE INDEX IF NOT EXISTS idx_wom_reject_materials_sv_table_info
            ON public.wom_reject_materials_sv(table_info_id);
    END IF;
END $$;

WITH workflow_seed(process_key, deployment_dbid) AS (
    VALUES
        ('batchRejectFlow', 7401000000070000::bigint),
        ('materiaReject', 7401000000080000::bigint),
        ('prePareRejectFlow', 7401000000090000::bigint)
),
current_flow AS (
    SELECT DISTINCT ON (deployment.process_key)
           deployment.id,
           deployment.process_key,
           workflow_seed.deployment_dbid,
           greatest(coalesce(deployment.process_version, 0), 1) AS process_version
      FROM public.wf_deployment deployment
      JOIN workflow_seed
        ON workflow_seed.process_key = deployment.process_key
     WHERE deployment.entity_code = 'WOM_1.0.0_rejectMaterilal'
       AND coalesce(deployment.valid, 1) = 1
       AND (deployment.process_xml IS NOT NULL OR deployment.process_xml_text_backup IS NOT NULL)
     ORDER BY deployment.process_key,
              coalesce(deployment.is_current_version, 0) DESC,
              coalesce(deployment.process_version, 0) DESC,
              deployment.id DESC
)
UPDATE public.wf_deployment deployment
   SET process_version = current_flow.process_version,
       process_definition_id = current_flow.process_key || '-' || current_flow.process_version::text,
       deployment_id = current_flow.deployment_dbid::text,
       is_current_version = CASE WHEN deployment.id = current_flow.id THEN 1 ELSE 0 END,
       valid = 1,
       publish_flag = 1,
       modify_time = CURRENT_TIMESTAMP
  FROM current_flow
 WHERE deployment.process_key = current_flow.process_key;

WITH workflow_seed(process_key, deployment_dbid) AS (
    VALUES
        ('batchRejectFlow', 7401000000070000::bigint),
        ('materiaReject', 7401000000080000::bigint),
        ('prePareRejectFlow', 7401000000090000::bigint)
),
current_flow AS (
    SELECT DISTINCT ON (deployment.process_key)
           deployment.process_key,
           workflow_seed.deployment_dbid
      FROM public.wf_deployment deployment
      JOIN workflow_seed
        ON workflow_seed.process_key = deployment.process_key
     WHERE deployment.entity_code = 'WOM_1.0.0_rejectMaterilal'
       AND coalesce(deployment.valid, 1) = 1
       AND coalesce(deployment.is_current_version, 0) = 1
       AND (deployment.process_xml IS NOT NULL OR deployment.process_xml_text_backup IS NOT NULL)
     ORDER BY deployment.process_key,
              coalesce(deployment.process_version, 0) DESC,
              deployment.id DESC
)
INSERT INTO public.jbpm4_deployment (
    dbid_,
    name_,
    timestamp_,
    state_
)
SELECT current_flow.deployment_dbid,
       '',
       0,
       'active'
  FROM current_flow
ON CONFLICT (dbid_) DO UPDATE SET
    state_ = EXCLUDED.state_;

WITH workflow_seed(process_key, deployment_dbid) AS (
    VALUES
        ('batchRejectFlow', 7401000000070000::bigint),
        ('materiaReject', 7401000000080000::bigint),
        ('prePareRejectFlow', 7401000000090000::bigint)
),
current_flow AS (
    SELECT DISTINCT ON (deployment.process_key)
           deployment.process_key,
           workflow_seed.deployment_dbid,
           greatest(coalesce(deployment.process_version, 0), 1) AS process_version
      FROM public.wf_deployment deployment
      JOIN workflow_seed
        ON workflow_seed.process_key = deployment.process_key
     WHERE deployment.entity_code = 'WOM_1.0.0_rejectMaterilal'
       AND coalesce(deployment.valid, 1) = 1
       AND coalesce(deployment.is_current_version, 0) = 1
       AND (deployment.process_xml IS NOT NULL OR deployment.process_xml_text_backup IS NOT NULL)
     ORDER BY deployment.process_key,
              coalesce(deployment.process_version, 0) DESC,
              deployment.id DESC
),
deployprop_seed AS (
    SELECT current_flow.deployment_dbid + 1 AS dbid_,
           current_flow.deployment_dbid AS deployment_,
           current_flow.process_key AS objname_,
           'langid' AS key_,
           'jpdl-4.4' AS stringval_,
           NULL::bigint AS longval_
      FROM current_flow
    UNION ALL
    SELECT current_flow.deployment_dbid + 2,
           current_flow.deployment_dbid,
           current_flow.process_key,
           'pdid',
           current_flow.process_key || '-' || current_flow.process_version::text,
           NULL::bigint
      FROM current_flow
    UNION ALL
    SELECT current_flow.deployment_dbid + 3,
           current_flow.deployment_dbid,
           current_flow.process_key,
           'pdkey',
           current_flow.process_key,
           NULL::bigint
      FROM current_flow
    UNION ALL
    SELECT current_flow.deployment_dbid + 4,
           current_flow.deployment_dbid,
           current_flow.process_key,
           'pdversion',
           NULL::text,
           current_flow.process_version::bigint
      FROM current_flow
)
INSERT INTO public.jbpm4_deployprop (
    dbid_,
    deployment_,
    objname_,
    key_,
    stringval_,
    longval_
)
SELECT dbid_,
       deployment_,
       objname_,
       key_,
       stringval_,
       longval_
  FROM deployprop_seed
ON CONFLICT (dbid_) DO UPDATE SET
    deployment_ = EXCLUDED.deployment_,
    objname_ = EXCLUDED.objname_,
    key_ = EXCLUDED.key_,
    stringval_ = EXCLUDED.stringval_,
    longval_ = EXCLUDED.longval_;

WITH workflow_seed(process_key, deployment_dbid) AS (
    VALUES
        ('batchRejectFlow', 7401000000070000::bigint),
        ('materiaReject', 7401000000080000::bigint),
        ('prePareRejectFlow', 7401000000090000::bigint)
),
current_flow AS (
    SELECT DISTINCT ON (deployment.process_key)
           deployment.process_key,
           deployment.name,
           workflow_seed.deployment_dbid,
           CASE
               WHEN deployment.process_xml_text_backup IS NOT NULL THEN deployment.process_xml_text_backup
               WHEN deployment.process_xml IS NOT NULL THEN convert_from(lo_get(deployment.process_xml), 'UTF8')
               ELSE NULL
           END AS process_xml_text
      FROM public.wf_deployment deployment
      JOIN workflow_seed
        ON workflow_seed.process_key = deployment.process_key
     WHERE deployment.entity_code = 'WOM_1.0.0_rejectMaterilal'
       AND coalesce(deployment.valid, 1) = 1
       AND coalesce(deployment.is_current_version, 0) = 1
       AND (deployment.process_xml IS NOT NULL OR deployment.process_xml_text_backup IS NOT NULL)
     ORDER BY deployment.process_key,
              coalesce(deployment.process_version, 0) DESC,
              deployment.id DESC
)
INSERT INTO public.jbpm4_lob (
    dbid_,
    dbversion_,
    blob_value_,
    deployment_,
    name_
)
SELECT current_flow.deployment_dbid + 10,
       0,
       lo_from_bytea(0, convert_to(current_flow.process_xml_text, 'UTF8')),
       current_flow.deployment_dbid,
       coalesce(nullif(current_flow.name, ''), current_flow.process_key) || '.jpdl.xml'
  FROM current_flow
 WHERE current_flow.process_xml_text IS NOT NULL
ON CONFLICT (dbid_) DO UPDATE SET
    dbversion_ = EXCLUDED.dbversion_,
    blob_value_ = EXCLUDED.blob_value_,
    deployment_ = EXCLUDED.deployment_,
    name_ = EXCLUDED.name_;

WITH current_flows AS (
    SELECT process_key,
           id AS deployment_id,
           coalesce(process_version, 1) AS process_version
      FROM public.wf_deployment
     WHERE entity_code = 'WOM_1.0.0_rejectMaterilal'
       AND process_key IN ('batchRejectFlow', 'materiaReject', 'prePareRejectFlow')
       AND coalesce(valid, 1) = 1
       AND coalesce(is_current_version, 0) = 1
),
task_seed AS (
    SELECT *
      FROM (
        VALUES
            ('batchRejectFlow', 101, 'start_6964gxi', 'WOM_1.0.0.workflow.randon1582014237934.flag', '开始活动', 1, 'WOM_1.0.0_rejectMaterilal_batchRejectEdit', NULL, 0, 0),
            ('batchRejectFlow', 102, 'TaskEvent_0nsp5v4', 'WOM_1.0.0.workflow.randon1582014246686.flag', '编辑', 4, 'WOM_1.0.0_rejectMaterilal_batchRejectEdit', '_blank', 1, 1),
            ('batchRejectFlow', 103, 'end_uaf1y1d', 'WOM_1.0.0.workflow.randon1582014289329.flag', '结束活动', 2, NULL, NULL, 0, 0),
            ('batchRejectFlow', 104, 'EndCancelEvent_1x0g63x', 'WOM_1.0.0.workflow.randon1582014289342.flag', '作废', 3, NULL, NULL, 0, 0),
            ('materiaReject', 101, 'start_cqxg3gf', 'WOM_1.0.0.workflow.randon1597226957803.flag', '开始活动', 1, 'WOM_1.0.0_rejectMaterilal_materiaRejectEdit', NULL, 0, 0),
            ('materiaReject', 102, 'TaskEvent_0hf8fwb', 'WOM_1.0.0.workflow.randon1597227031104.flag', '退料发起', 4, 'WOM_1.0.0_rejectMaterilal_materiaRejectEdit', '_blank', 1, 1),
            ('materiaReject', 103, 'TaskEvent_0t6vfw8', 'WOM_1.0.0.workflow.randon1597227108723.flag', '退料接收', 4, 'WOM_1.0.0_rejectMaterilal_materiaEditableEdit', '_blank', 1, 1),
            ('materiaReject', 104, 'end_hj3h473', 'WOM_1.0.0.workflow.randon1597227394758.flag', '结束活动', 2, NULL, NULL, 0, 0),
            ('materiaReject', 105, 'EndCancelEvent_13tc39i', 'WOM_1.0.0.workflow.randon1597227394769.flag', '作废', 3, NULL, NULL, 0, 0),
            ('prePareRejectFlow', 101, 'start_duviwfq', 'WOM_1.0.0.workflow.randon1582014450964.flag', '开始活动', 1, 'WOM_1.0.0_rejectMaterilal_prePareRejectEdit', NULL, 0, 0),
            ('prePareRejectFlow', 102, 'TaskEvent_17rdgt4', 'WOM_1.0.0.workflow.randon1582014456951.flag', '编辑', 4, 'WOM_1.0.0_rejectMaterilal_prePareRejectEdit', '_blank', 1, 1),
            ('prePareRejectFlow', 103, 'end_xwzcjr1', 'WOM_1.0.0.workflow.randon1582014466807.flag', '结束活动', 2, NULL, NULL, 0, 0),
            ('prePareRejectFlow', 104, 'EndCancelEvent_1bs9k56', 'WOM_1.0.0.workflow.randon1582014466819.flag', '作废', 3, NULL, NULL, 0, 0)
      ) AS seed(process_key, offset_id, code, name, name_zh_cn, type, view_code, open_mode, mobile_approve, recall_able)
)
INSERT INTO public.wf_task (
    id,
    version,
    valid,
    ignore_permission,
    web_signet_flag,
    deal_set,
    is_allow_proxy,
    show_in_simple_dealinfo,
    mobile_approve,
    recall_able,
    process_version,
    process_key,
    open_mode,
    view_code,
    type,
    deployment_id,
    code,
    name_zh_cn,
    name,
    cid
)
SELECT current_flows.deployment_id + task_seed.offset_id,
       0,
       1,
       0,
       0,
       0,
       1,
       1,
       task_seed.mobile_approve,
       task_seed.recall_able,
       current_flows.process_version,
       task_seed.process_key,
       task_seed.open_mode,
       task_seed.view_code,
       task_seed.type,
       current_flows.deployment_id,
       task_seed.code,
       task_seed.name_zh_cn,
       task_seed.name,
       1000
  FROM current_flows
  JOIN task_seed
    ON task_seed.process_key = current_flows.process_key
ON CONFLICT (id) DO UPDATE SET
    valid = EXCLUDED.valid,
    deployment_id = EXCLUDED.deployment_id,
    process_key = EXCLUDED.process_key,
    process_version = EXCLUDED.process_version,
    code = EXCLUDED.code,
    name = EXCLUDED.name,
    name_zh_cn = EXCLUDED.name_zh_cn,
    type = EXCLUDED.type,
    view_code = EXCLUDED.view_code,
    open_mode = EXCLUDED.open_mode,
    mobile_approve = EXCLUDED.mobile_approve,
    recall_able = EXCLUDED.recall_able;

WITH current_flows AS (
    SELECT process_key,
           id AS deployment_id
      FROM public.wf_deployment
     WHERE entity_code = 'WOM_1.0.0_rejectMaterilal'
       AND process_key IN ('batchRejectFlow', 'materiaReject', 'prePareRejectFlow')
       AND coalesce(valid, 1) = 1
       AND coalesce(is_current_version, 0) = 1
),
transition_seed AS (
    SELECT *
      FROM (
        VALUES
            ('batchRejectFlow', 201, 'SequenceFlow_00wl0va', 'WOM_1.0.0.workflow.randon1582014229609.flag', '开始', 'start_6964gxi', 'TaskEvent_0nsp5v4', 1),
            ('batchRejectFlow', 202, 'SequenceFlow_1dghow0', 'WOM_1.0.0.workflow.randon1582014235866.flag', '生效', 'TaskEvent_0nsp5v4', 'end_uaf1y1d', 1),
            ('batchRejectFlow', 203, 'SequenceFlow_0qqo3nz', 'WOM_1.0.0.workflow.randon1582014273492.flag', '作废', 'TaskEvent_0nsp5v4', 'EndCancelEvent_1x0g63x', 3),
            ('materiaReject', 201, 'SequenceFlow_0ll1p1h', 'WOM_1.0.0.workflow.randon1597227354043.flag', '开始', 'start_cqxg3gf', 'TaskEvent_0hf8fwb', 1),
            ('materiaReject', 202, 'SequenceFlow_1ubtbie', 'WOM_1.0.0.workflow.randon1597227387979.flag', '提交', 'TaskEvent_0hf8fwb', 'TaskEvent_0t6vfw8', 1),
            ('materiaReject', 203, 'SequenceFlow_03kby02', 'WOM_1.0.0.workflow.randon1597227837191.flag', '作废', 'TaskEvent_0hf8fwb', 'EndCancelEvent_13tc39i', 3),
            ('materiaReject', 204, 'SequenceFlow_0igvfiq', 'WOM_1.0.0.workflow.randon1597227344699.flag', '生效', 'TaskEvent_0t6vfw8', 'end_hj3h473', 1),
            ('prePareRejectFlow', 201, 'SequenceFlow_09grns2', 'WOM_1.0.0.workflow.randon1582014421678.flag', '开始', 'start_duviwfq', 'TaskEvent_17rdgt4', 1),
            ('prePareRejectFlow', 202, 'SequenceFlow_1nu8kl7', 'WOM_1.0.0.workflow.randon1582014428385.flag', '生效', 'TaskEvent_17rdgt4', 'end_xwzcjr1', 1),
            ('prePareRejectFlow', 203, 'SequenceFlow_1dlhfro', 'WOM_1.0.0.workflow.randon1582014443760.flag', '作废', 'TaskEvent_17rdgt4', 'EndCancelEvent_1bs9k56', 3)
      ) AS seed(process_key, offset_id, code, name, name_zh_cn, from_node_code, to_node_code, type)
)
INSERT INTO public.wf_transition (
    id,
    version,
    valid,
    default_staff,
    required_staff,
    select_staff,
    deployment_id,
    to_node_code,
    from_node_code,
    type,
    code,
    name_zh_cn,
    name
)
SELECT current_flows.deployment_id + transition_seed.offset_id,
       0,
       1,
       0,
       0,
       '0',
       current_flows.deployment_id,
       transition_seed.to_node_code,
       transition_seed.from_node_code,
       transition_seed.type,
       transition_seed.code,
       transition_seed.name_zh_cn,
       transition_seed.name
  FROM current_flows
  JOIN transition_seed
    ON transition_seed.process_key = current_flows.process_key
ON CONFLICT (id) DO UPDATE SET
    valid = EXCLUDED.valid,
    deployment_id = EXCLUDED.deployment_id,
    to_node_code = EXCLUDED.to_node_code,
    from_node_code = EXCLUDED.from_node_code,
    type = EXCLUDED.type,
    code = EXCLUDED.code,
    name_zh_cn = EXCLUDED.name_zh_cn,
    name = EXCLUDED.name,
    default_staff = EXCLUDED.default_staff,
    required_staff = EXCLUDED.required_staff,
    select_staff = EXCLUDED.select_staff;

WITH current_flows AS (
    SELECT process_key,
           id AS deployment_id,
           coalesce(process_version, 1)::text AS flow_version,
           coalesce(name, process_key) AS flow_name
      FROM public.wf_deployment
     WHERE entity_code = 'WOM_1.0.0_rejectMaterilal'
       AND process_key IN ('batchRejectFlow', 'materiaReject', 'prePareRejectFlow')
       AND coalesce(valid, 1) = 1
       AND coalesce(is_current_version, 0) = 1
),
permission_seed AS (
    SELECT *
      FROM (
        VALUES
            ('batchRejectFlow', 301, 'TaskEvent_0nsp5v4'),
            ('materiaReject', 301, 'TaskEvent_0hf8fwb'),
            ('materiaReject', 302, 'TaskEvent_0t6vfw8'),
            ('prePareRejectFlow', 301, 'TaskEvent_17rdgt4')
      ) AS seed(process_key, offset_id, activity_code)
)
INSERT INTO public.rbac_flow_permission (
    id,
    version,
    modify_time,
    create_time,
    create_staff_id,
    cid,
    entity_code,
    purview_distribution,
    purview_state,
    memo,
    unlimited_power,
    group_power_flag,
    assign_staff_flag,
    assign_pos_flag,
    position_power_flag,
    flow_permission_type,
    type_id,
    activity_code,
    flow_version,
    flow_key,
    flow_name
)
SELECT current_flows.deployment_id + permission_seed.offset_id,
       0,
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP,
       1,
       1000,
       'WOM_1.0.0_rejectMaterilal',
       3,
       1,
       '',
       true,
       false,
       false,
       false,
       false,
       'USER',
       1,
       permission_seed.activity_code,
       current_flows.flow_version,
       current_flows.process_key,
       current_flows.flow_name
  FROM current_flows
  JOIN permission_seed
    ON permission_seed.process_key = current_flows.process_key
ON CONFLICT (id) DO UPDATE SET
    modify_time = CURRENT_TIMESTAMP,
    entity_code = EXCLUDED.entity_code,
    purview_distribution = EXCLUDED.purview_distribution,
    purview_state = EXCLUDED.purview_state,
    unlimited_power = EXCLUDED.unlimited_power,
    group_power_flag = EXCLUDED.group_power_flag,
    assign_staff_flag = EXCLUDED.assign_staff_flag,
    assign_pos_flag = EXCLUDED.assign_pos_flag,
    position_power_flag = EXCLUDED.position_power_flag,
    flow_permission_type = EXCLUDED.flow_permission_type,
    type_id = EXCLUDED.type_id,
    activity_code = EXCLUDED.activity_code,
    flow_version = EXCLUDED.flow_version,
    flow_key = EXCLUDED.flow_key,
    flow_name = EXCLUDED.flow_name;

CREATE INDEX IF NOT EXISTS idx_jbpm4_deployprop_reject_key_stringval
    ON public.jbpm4_deployprop(key_, stringval_);

CREATE INDEX IF NOT EXISTS idx_wf_task_reject_deployment_code
    ON public.wf_task(deployment_id, code);

CREATE INDEX IF NOT EXISTS idx_wf_transition_reject_deployment_from
    ON public.wf_transition(deployment_id, from_node_code);

CREATE INDEX IF NOT EXISTS idx_rbac_flow_permission_reject_activity
    ON public.rbac_flow_permission(flow_key, activity_code, purview_state);
