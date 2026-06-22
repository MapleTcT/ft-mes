-- Enable QCS product emergency release actions on PostgreSQL.
--
-- Source evidence:
-- - QCSInspectReleaseServiceImpl.save starts the workflow with deploymentId
--   and then persists DealInfo.
-- - The recovered QCS package defines manuInspReleaseWorkflow in module.xml,
--   but the PostgreSQL runtime was missing wf_task/wf_transition rows.
-- - QCSInspectRelease datagrids read QCSInspReleaseMat, whose table was also
--   absent in the recovered PostgreSQL database.

CREATE TABLE IF NOT EXISTS public.qcs_insp_release_mats (id int8 PRIMARY KEY);

ALTER TABLE public.qcs_insp_release_mats
  ADD COLUMN IF NOT EXISTS version int4,
  ADD COLUMN IF NOT EXISTS create_staff_id int8,
  ADD COLUMN IF NOT EXISTS create_time timestamp,
  ADD COLUMN IF NOT EXISTS modify_staff_id int8,
  ADD COLUMN IF NOT EXISTS modify_time timestamp,
  ADD COLUMN IF NOT EXISTS delete_staff_id int8,
  ADD COLUMN IF NOT EXISTS delete_time timestamp,
  ADD COLUMN IF NOT EXISTS valid boolean,
  ADD COLUMN IF NOT EXISTS cid int8,
  ADD COLUMN IF NOT EXISTS sort int4,
  ADD COLUMN IF NOT EXISTS create_department_id int8,
  ADD COLUMN IF NOT EXISTS create_position_id int8,
  ADD COLUMN IF NOT EXISTS deployment_id int8,
  ADD COLUMN IF NOT EXISTS effect_staff_id int8,
  ADD COLUMN IF NOT EXISTS effect_time timestamp,
  ADD COLUMN IF NOT EXISTS effective_state int4,
  ADD COLUMN IF NOT EXISTS oa boolean,
  ADD COLUMN IF NOT EXISTS group_id int8,
  ADD COLUMN IF NOT EXISTS owner_department_id int8,
  ADD COLUMN IF NOT EXISTS owner_position_id int8,
  ADD COLUMN IF NOT EXISTS owner_staff_id int8,
  ADD COLUMN IF NOT EXISTS position_lay_rec varchar(255),
  ADD COLUMN IF NOT EXISTS process_key varchar(255),
  ADD COLUMN IF NOT EXISTS process_version int4,
  ADD COLUMN IF NOT EXISTS status int4,
  ADD COLUMN IF NOT EXISTS table_no varchar(255),
  ADD COLUMN IF NOT EXISTS table_info_id int8,
  ADD COLUMN IF NOT EXISTS extra_col text,
  ADD COLUMN IF NOT EXISTS parent_id int8,
  ADD COLUMN IF NOT EXISTS lay_rec varchar(1000),
  ADD COLUMN IF NOT EXISTS lay_no int4,
  ADD COLUMN IF NOT EXISTS leaf int4,
  ADD COLUMN IF NOT EXISTS full_path_name varchar(2000),
  ADD COLUMN IF NOT EXISTS batch_code varchar(200),
  ADD COLUMN IF NOT EXISTS inspect_id int8,
  ADD COLUMN IF NOT EXISTS inspect_release_id int8,
  ADD COLUMN IF NOT EXISTS memo_field varchar(2000),
  ADD COLUMN IF NOT EXISTS product_id int8,
  ADD COLUMN IF NOT EXISTS scparama varchar(2000),
  ADD COLUMN IF NOT EXISTS scparamb varchar(2000),
  ADD COLUMN IF NOT EXISTS scparamc varchar(2000),
  ADD COLUMN IF NOT EXISTS scparamd varchar(2000),
  ADD COLUMN IF NOT EXISTS charparama varchar(2000),
  ADD COLUMN IF NOT EXISTS charparamb varchar(2000),
  ADD COLUMN IF NOT EXISTS charparamc varchar(2000),
  ADD COLUMN IF NOT EXISTS charparamd varchar(2000),
  ADD COLUMN IF NOT EXISTS bigintparama int4,
  ADD COLUMN IF NOT EXISTS bigintparamb int4,
  ADD COLUMN IF NOT EXISTS bigintparamc int4,
  ADD COLUMN IF NOT EXISTS bigintparamd int4,
  ADD COLUMN IF NOT EXISTS dateparama timestamp,
  ADD COLUMN IF NOT EXISTS dateparamb timestamp,
  ADD COLUMN IF NOT EXISTS dateparamc timestamp,
  ADD COLUMN IF NOT EXISTS dateparamd timestamp,
  ADD COLUMN IF NOT EXISTS dateparame timestamp,
  ADD COLUMN IF NOT EXISTS dateparamf timestamp,
  ADD COLUMN IF NOT EXISTS dateparamg timestamp,
  ADD COLUMN IF NOT EXISTS dateparamh timestamp,
  ADD COLUMN IF NOT EXISTS numberparama numeric(38,6),
  ADD COLUMN IF NOT EXISTS numberparamb numeric(38,6),
  ADD COLUMN IF NOT EXISTS numberparamc numeric(38,6),
  ADD COLUMN IF NOT EXISTS numberparamd numeric(38,6),
  ADD COLUMN IF NOT EXISTS objparama int8,
  ADD COLUMN IF NOT EXISTS objparamb int8,
  ADD COLUMN IF NOT EXISTS objparamc int8,
  ADD COLUMN IF NOT EXISTS objparamd int8;

CREATE INDEX IF NOT EXISTS idx_qcs_insp_release_mats_table_info_id
    ON public.qcs_insp_release_mats(table_info_id);
CREATE INDEX IF NOT EXISTS idx_qcs_insp_release_mats_batch_code
    ON public.qcs_insp_release_mats(batch_code);
CREATE INDEX IF NOT EXISTS idx_qcs_insp_release_mats_inspect_id
    ON public.qcs_insp_release_mats(inspect_id);
CREATE INDEX IF NOT EXISTS idx_qcs_insp_release_mats_release_id
    ON public.qcs_insp_release_mats(inspect_release_id);
CREATE INDEX IF NOT EXISTS idx_qcs_insp_release_mats_product_id
    ON public.qcs_insp_release_mats(product_id);
CREATE INDEX IF NOT EXISTS idx_qcs_insp_release_mats_valid
    ON public.qcs_insp_release_mats(valid);
CREATE INDEX IF NOT EXISTS idx_qcs_insp_release_mats_cid
    ON public.qcs_insp_release_mats(cid);

DO $$
BEGIN
  IF to_regclass('public.qcs_inspect_releases') IS NOT NULL
     AND (to_regclass('public.qcs_inspect_releases_sv') IS NULL OR EXISTS (
       SELECT 1
       FROM pg_class c
       JOIN pg_namespace n ON n.oid = c.relnamespace
       WHERE n.nspname = 'public'
         AND c.relname = 'qcs_inspect_releases_sv'
         AND c.relkind = 'v'
     )) THEN
    EXECUTE $view$
CREATE OR REPLACE VIEW public.qcs_inspect_releases_sv AS
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
FROM public.qcs_inspect_releases
WHERE table_info_id IS NOT NULL
  AND owner_staff_id IS NOT NULL
$view$;
  END IF;
END $$;

WITH current_flow AS (
    SELECT id AS deployment_id,
           coalesce(process_version, 1) AS process_version
      FROM public.wf_deployment
     WHERE process_key = 'manuInspReleaseWorkflow'
       AND coalesce(valid, 1) = 1
     ORDER BY coalesce(is_current_version, 0) DESC,
              coalesce(process_version, 0) DESC,
              id DESC
     LIMIT 1
)
UPDATE public.wf_deployment deployment
   SET is_current_version = CASE
       WHEN deployment.id = current_flow.deployment_id THEN 1
       ELSE 0
   END,
       valid = 1,
       process_version = current_flow.process_version,
       process_definition_id = 'manuInspReleaseWorkflow-' || current_flow.process_version::text,
       deployment_id = '7401000000010000',
       modify_time = CURRENT_TIMESTAMP
  FROM current_flow
 WHERE deployment.process_key = 'manuInspReleaseWorkflow';

CREATE INDEX IF NOT EXISTS idx_baseset_wf_custom_configs_code_valid
    ON public.baseset_wf_custom_configs(code, valid);

INSERT INTO public.baseset_wf_custom_configs (
    id,
    version,
    valid,
    cid,
    create_staff_id,
    create_time,
    create_department_id,
    create_position_id,
    group_id,
    owner_staff_id,
    owner_department_id,
    owner_position_id,
    position_lay_rec,
    status,
    table_no,
    code,
    name,
    deployment_key,
    transaction_key,
    flow_config_type,
    cus_create_staff_id,
    cus_create_dept_id,
    cus_create_pos_id,
    effective_state
)
SELECT
    1250000000000001::bigint,
    0,
    true,
    1000,
    1,
    CURRENT_TIMESTAMP,
    1,
    1,
    1000,
    1,
    1,
    1,
    '1',
    99,
    'QCS_MANU_INSP_RELEASE_WORKFLOW_CONFIG',
    'manuInspRelease',
    '产品紧急放行',
    'manuInspReleaseWorkflow',
    'manuInspRelease',
    'BaseSet_flowConfigType/save',
    1,
    1,
    1,
    0
WHERE NOT EXISTS (
    SELECT 1
      FROM public.baseset_wf_custom_configs existing
     WHERE existing.code = 'manuInspRelease'
);

UPDATE public.baseset_wf_custom_configs
   SET valid = true,
       status = 99,
       name = '产品紧急放行',
       deployment_key = 'manuInspReleaseWorkflow',
       transaction_key = 'manuInspRelease',
       flow_config_type = 'BaseSet_flowConfigType/save',
       cus_create_staff_id = coalesce(cus_create_staff_id, 1),
       cus_create_dept_id = coalesce(cus_create_dept_id, 1),
       cus_create_pos_id = coalesce(cus_create_pos_id, 1),
       effective_state = coalesce(effective_state, 0),
       modify_time = CURRENT_TIMESTAMP
 WHERE code = 'manuInspRelease';

WITH current_flow AS (
    SELECT id AS deployment_id,
           coalesce(process_version, 1) AS process_version
      FROM public.wf_deployment
     WHERE process_key = 'manuInspReleaseWorkflow'
       AND coalesce(valid, 1) = 1
       AND coalesce(is_current_version, 0) = 1
     ORDER BY coalesce(process_version, 0) DESC, id DESC
     LIMIT 1
),
task_seed AS (
    SELECT *
      FROM (
        VALUES
            (101, 'start_75elcfv', 'QCS.workflow.randon1619489335671.flag', '开始活动', 1, 'QCS_5.0.0.0_inspectRelease_manuInspReleaseEdit', NULL, 0, 0),
            (102, 'TaskEvent_1g5tdqv', 'QCS_5.0.0.0.workflow.randon1592221382676.flag', '编辑', 4, 'QCS_5.0.0.0_inspectRelease_manuInspReleaseEdit', '_blank', 1, 1),
            (103, 'TaskEvent_0fkkhcq', 'QCS_5.0.0.0.workflow.randon1592221629652.flag', '审核', 4, 'QCS_5.0.0.0_inspectRelease_manuInspReleaseAudit', '_blank', 1, 1),
            (104, 'end_87b94gk', 'QCS.workflow.randon1619489382710.flag', '结束活动', 2, NULL, NULL, 0, 0),
            (105, 'EndCancelEvent_0hs4p3l', 'QCS.workflow.randon1619489382722.flag', '作废', 3, NULL, NULL, 0, 0)
      ) AS seed(offset_id, code, name, name_zh_cn, type, view_code, open_mode, mobile_approve, recall_able)
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
SELECT current_flow.deployment_id + task_seed.offset_id,
       0,
       1,
       0,
       0,
       0,
       1,
       1,
       task_seed.mobile_approve,
       task_seed.recall_able,
       current_flow.process_version,
       'manuInspReleaseWorkflow',
       task_seed.open_mode,
       task_seed.view_code,
       task_seed.type,
       current_flow.deployment_id,
       task_seed.code,
       task_seed.name_zh_cn,
       task_seed.name,
       1000
  FROM current_flow
 CROSS JOIN task_seed
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

WITH current_flow AS (
    SELECT id AS deployment_id
      FROM public.wf_deployment
     WHERE process_key = 'manuInspReleaseWorkflow'
       AND coalesce(valid, 1) = 1
       AND coalesce(is_current_version, 0) = 1
     ORDER BY coalesce(process_version, 0) DESC, id DESC
     LIMIT 1
),
transition_seed AS (
    SELECT *
      FROM (
        VALUES
            (201, 'SequenceFlow_0zuxpdn', 'QCS_5.0.0.0.workflow.randon1619489335671.flag', '制定', 'start_75elcfv', 'TaskEvent_1g5tdqv', 1),
            (202, 'SequenceFlow_0ecewt8', 'QCS_5.0.0.0.workflow.randon1592221382676.flag', '审核', 'TaskEvent_1g5tdqv', 'TaskEvent_0fkkhcq', 1),
            (203, 'SequenceFlow_03u9f4j', 'QCS_5.0.0.0.workflow.randon1619489382722.flag', '作废', 'TaskEvent_1g5tdqv', 'EndCancelEvent_0hs4p3l', 3),
            (204, 'SequenceFlow_0xc8eo1', 'QCS_5.0.0.0.workflow.randon1619489382710.flag', '生效', 'TaskEvent_0fkkhcq', 'end_87b94gk', 1),
            (205, 'SequenceFlow_0sa9877', 'QCS_5.0.0.0.workflow.randon1592221901944.flag', '驳回', 'TaskEvent_0fkkhcq', 'TaskEvent_1g5tdqv', 2)
      ) AS seed(offset_id, code, name, name_zh_cn, from_node_code, to_node_code, type)
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
SELECT current_flow.deployment_id + transition_seed.offset_id,
       0,
       1,
       0,
       0,
       '0',
       current_flow.deployment_id,
       transition_seed.to_node_code,
       transition_seed.from_node_code,
       transition_seed.type,
       transition_seed.code,
       transition_seed.name_zh_cn,
       transition_seed.name
  FROM current_flow
 CROSS JOIN transition_seed
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

WITH current_flow AS (
    SELECT id AS deployment_id,
           coalesce(process_version, 1)::text AS flow_version,
           coalesce(name, 'QCS.workflowname.randon1619488976111.flag') AS flow_name
      FROM public.wf_deployment
     WHERE process_key = 'manuInspReleaseWorkflow'
       AND coalesce(valid, 1) = 1
       AND coalesce(is_current_version, 0) = 1
     ORDER BY coalesce(process_version, 0) DESC, id DESC
     LIMIT 1
),
permission_seed AS (
    SELECT *
      FROM (
        VALUES
            (301, 'TaskEvent_1g5tdqv'),
            (302, 'TaskEvent_0fkkhcq')
      ) AS seed(offset_id, activity_code)
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
SELECT current_flow.deployment_id + permission_seed.offset_id,
       0,
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP,
       1,
       1000,
       'QCS_5.0.0.0_inspectRelease',
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
       current_flow.flow_version,
       'manuInspReleaseWorkflow',
       current_flow.flow_name
  FROM current_flow
 CROSS JOIN permission_seed
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

CREATE INDEX IF NOT EXISTS idx_wf_task_qcs_insp_release_deployment_code
    ON public.wf_task(deployment_id, code);

CREATE INDEX IF NOT EXISTS idx_wf_transition_qcs_insp_release_deployment_from
    ON public.wf_transition(deployment_id, from_node_code);
