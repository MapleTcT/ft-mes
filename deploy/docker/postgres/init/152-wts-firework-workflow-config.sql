-- Register the recovered WTS fire-work workflow with the legacy JBPM runtime
-- and restore its low-code workflow graph metadata on PostgreSQL.
--
-- Source evidence:
-- - WorkPermitViewService.saveWorkTicket calls checkRequired before saving a
--   child work ticket. For new tickets, checkRequired calls
--   WorkFlowService.getFirstTransformerLineCode(deploymentId), which reads
--   wf_transition by deployment_id. The recovered PostgreSQL runtime had
--   fireWorkWF in wf_deployment, but no wf_transition rows, causing a
--   NullPointerException before any WTS child ticket could persist.
-- - wf_deployment.process_xml_text_backup contains the original fireWorkWF
--   JPDL XML, but process_definition_id/deployment_id/is_current_version were
--   not populated for runtime startup.

WITH workflow_seed(process_key, deployment_dbid) AS (
    VALUES
        ('fireWorkWF', 7401000000100000::bigint)
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
     WHERE coalesce(deployment.valid, 1) = 1
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
       modify_time = CURRENT_TIMESTAMP
  FROM current_flow
 WHERE deployment.process_key = current_flow.process_key;

WITH workflow_seed(process_key, deployment_dbid) AS (
    VALUES
        ('fireWorkWF', 7401000000100000::bigint)
),
current_flow AS (
    SELECT DISTINCT ON (deployment.process_key)
           deployment.process_key,
           workflow_seed.deployment_dbid
      FROM public.wf_deployment deployment
      JOIN workflow_seed
        ON workflow_seed.process_key = deployment.process_key
     WHERE coalesce(deployment.valid, 1) = 1
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
        ('fireWorkWF', 7401000000100000::bigint)
),
current_flow AS (
    SELECT DISTINCT ON (deployment.process_key)
           deployment.process_key,
           workflow_seed.deployment_dbid,
           greatest(coalesce(deployment.process_version, 0), 1) AS process_version
      FROM public.wf_deployment deployment
      JOIN workflow_seed
        ON workflow_seed.process_key = deployment.process_key
     WHERE coalesce(deployment.valid, 1) = 1
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
        ('fireWorkWF', 7401000000100000::bigint)
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
     WHERE coalesce(deployment.valid, 1) = 1
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

WITH current_flow AS (
    SELECT id AS deployment_id,
           coalesce(process_version, 1) AS process_version
      FROM public.wf_deployment
     WHERE process_key = 'fireWorkWF'
       AND coalesce(valid, 1) = 1
       AND coalesce(is_current_version, 0) = 1
     ORDER BY coalesce(process_version, 0) DESC, id DESC
     LIMIT 1
),
task_seed AS (
    SELECT *
      FROM (
        VALUES
            (101, 'start_l4ybb6w', 'WTS_1.0.0.workflow.randon1574148918584.flag', '开始活动', 1, NULL, NULL, NULL, 0, 0),
            (102, 'end_3pz5gzw', 'WTS_1.0.0.workflow.randon1574149109104.flag', '结束活动', 2, NULL, NULL, NULL, 0, 0),
            (103, 'TaskEvent_15w67f5', 'WTS_1.0.0.workflow.randon1574148384979.flag', '开票申请', 4, 'WTS_1.0.0_workTicket_fireworkEdit', NULL, '_blank', 1, 1),
            (105, 'TaskEvent_129vu5v', 'WTS_1.0.0.workflow.randon1574148406462.flag', '气体分析', 4, 'WTS_1.0.0_workTicket_fireworkGas', NULL, '_blank', 1, 1),
            (106, 'TaskEvent_01x4zva', 'WTS_1.0.0.workflow.randon1574148414589.flag', '风险与安全措施确认', 4, 'WTS_1.0.0_workTicket_fireworkRisk', NULL, '_blank', 1, 1),
            (107, 'TaskEvent_0ak83bj', 'WTS_1.0.0.workflow.randon1574148421720.flag', '生产单位安全部门确认', 4, 'WTS_1.0.0_workTicket_fireworkApproval', NULL, '_blank', 1, 1),
            (108, 'TaskEvent_032m1nx', 'WTS_1.0.0.workflow.randon1574148430941.flag', '作业单位安全部门确认', 4, 'WTS_1.0.0_workTicket_fireworkApproval', NULL, '_blank', 1, 1),
            (110, 'TaskEvent_06kzbg6', 'WTS_1.0.0.workflow.randon1574148161950.flag', '作业验票', 4, 'WTS_1.0.0_workTicket_fireworkApproval', NULL, '_blank', 1, 1),
            (111, 'TaskEvent_1y6xnvi', 'WTS_1.0.0.workflow.randon1574148444069.flag', '作业执行', 4, 'WTS_1.0.0_workTicket_fireworkDeal', NULL, '_blank', 1, 1),
            (113, 'TaskEvent_02aitou', 'WTS_1.0.0.workflow.randon1574236822405.flag', '生产单位安全部门确认（终止）', 4, 'WTS_1.0.0_workTicket_fireworkDeal', NULL, '_blank', 1, 1),
            (114, 'TaskEvent_16rrw0a', 'WTS_1.0.0.workflow.randon1574236827217.flag', '作业单位安全部门确认（终止）', 4, 'WTS_1.0.0_workTicket_fireworkDeal', NULL, '_blank', 1, 1),
            (115, 'TaskEvent_0r1w2fv', 'WTS_1.0.0.workflow.randon1574817918354.flag', '作业封票', 4, 'WTS_1.0.0_workTicket_fireworkClose', NULL, '_blank', 1, 1),
            (116, 'TaskEvent_06ya98w', 'WTS.workflow.randon1637749198432.flag', '作业暂停', 4, 'WTS_1.0.0_workTicket_fireworkDeal', NULL, '_blank', 1, 1),
            (117, 'ForkEvent_0r75jj3', 'WTS_1.0.0.workflow.randon1574148403840.flag', '分发路由', 3, NULL, NULL, NULL, 0, 0),
            (118, 'JoinEvent_0ghsw7w', 'WTS_1.0.0.workflow.randon1574149109116.flag', '聚合路由', 3, NULL, NULL, NULL, 0, 0),
            (119, 'DecisionEvent_0a25y40', 'WTS_1.0.0.workflow.randon1574148218784.flag', '选择路由', 3, NULL, NULL, NULL, 0, 0),
            (120, 'AutoEvent_103nes6', 'WTS.workflow.randon1655380669243.flag', '删除安全措施', 3, NULL, 'deleteMeasure', NULL, 0, 0),
            (121, 'AutoEvent_11389b7', 'WTS.workflow.randon1645077896224.flag', '进入执行', 3, NULL, 'executeJob', NULL, 0, 0),
            (122, 'AutoEvent_1l8x87t', 'WTS.workflow.randon1638610207839.flag', '提交气体分析', 3, NULL, 'submitGasAnalysis', NULL, 0, 0),
            (123, 'AutoEvent_0ah9dxe', 'WTS.workflow.randon1640056094982.flag', '暂停作业', 3, NULL, 'pauseJob', NULL, 0, 0),
            (124, 'AutoEvent_061ij9k', 'WTS.workflow.randon1640056111954.flag', '恢复作业', 3, NULL, 'resumeJob', NULL, 0, 0)
      ) AS seed(offset_id, code, name, name_zh_cn, type, view_code, script, open_mode, mobile_approve, recall_able)
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
    script,
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
       'fireWorkWF',
       task_seed.open_mode,
       task_seed.view_code,
       task_seed.script,
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
    script = EXCLUDED.script,
    open_mode = EXCLUDED.open_mode,
    mobile_approve = EXCLUDED.mobile_approve,
    recall_able = EXCLUDED.recall_able;

WITH current_flow AS (
    SELECT id AS deployment_id
      FROM public.wf_deployment
     WHERE process_key = 'fireWorkWF'
       AND coalesce(valid, 1) = 1
       AND coalesce(is_current_version, 0) = 1
     ORDER BY coalesce(process_version, 0) DESC, id DESC
     LIMIT 1
),
transition_seed AS (
    SELECT *
      FROM (
        VALUES
            (201, 'SequenceFlow_08qrkd6', 'WTS_1.0.0.workflow.randon1574148476056.flag', '制定', 'start_l4ybb6w', 'TaskEvent_15w67f5', 1, '1'),
            (202, 'SequenceFlow_0e9sj8r', 'WTS_1.0.0.workflow.randon1574148627791.flag', '提交', 'TaskEvent_15w67f5', 'AutoEvent_103nes6', 1, '0'),
            (203, 'SequenceFlow_11ah620', 'WTS_1.0.0.workflow.randon1574148701371.flag', '提交', 'ForkEvent_0r75jj3', 'TaskEvent_129vu5v', 1, '0'),
            (204, 'SequenceFlow_036u59h', 'WTS_1.0.0.workflow.randon1574148648393.flag', '提交', 'ForkEvent_0r75jj3', 'TaskEvent_01x4zva', 1, '0'),
            (205, 'SequenceFlow_03vvptt', 'WTS_1.0.0.workflow.randon1574148712106.flag', '提交', 'TaskEvent_129vu5v', 'JoinEvent_0ghsw7w', 1, '0'),
            (206, 'SequenceFlow_1g8eg7t', 'WTS_1.0.0.workflow.randon1574148665354.flag', '审批', 'TaskEvent_01x4zva', 'TaskEvent_0ak83bj', 1, '1'),
            (207, 'SequenceFlow_07dgcpk', 'WTS_1.0.0.workflow.randon1574148685597.flag', '审批', 'TaskEvent_0ak83bj', 'TaskEvent_032m1nx', 1, '0'),
            (208, 'SequenceFlow_1ulmibf', 'WTS_1.0.0.workflow.randon1574148716673.flag', '通过', 'TaskEvent_032m1nx', 'TaskEvent_06kzbg6', 1, '1'),
            (209, 'SequenceFlow_15pbnk4', 'WTS.workflow.randon1637749266205.flag', '提交', 'JoinEvent_0ghsw7w', 'DecisionEvent_0a25y40', 1, '0'),
            (210, 'SequenceFlow_1g2rlaw', 'WTS.workflow.randon1645077794530.flag', '作业执行审批', 'TaskEvent_06kzbg6', 'AutoEvent_11389b7', 1, '1'),
            (211, 'SequenceFlow_1lv8x6k', 'WTS_1.0.0.workflow.randon1574148803523.flag', '提交', 'TaskEvent_1y6xnvi', 'AutoEvent_1l8x87t', 1, '0'),
            (212, 'SequenceFlow_1pb7887', 'WTS.workflow.randon1637749173577.flag', '暂停作业', 'TaskEvent_1y6xnvi', 'AutoEvent_0ah9dxe', 1, '1'),
            (213, 'SequenceFlow_0hjryi8', 'WTS_1.0.0.workflow.randon1574236693124.flag', '终止', 'DecisionEvent_0a25y40', 'TaskEvent_02aitou', 1, '1'),
            (214, 'SequenceFlow_1aaofna', 'WTS.workflow.randon1653548173200.flag', '封票', 'DecisionEvent_0a25y40', 'TaskEvent_0r1w2fv', 1, '1'),
            (215, 'SequenceFlow_0ars0ox', 'WTS_1.0.0.workflow.randon1574236865529.flag', '终止通知', 'TaskEvent_02aitou', 'TaskEvent_16rrw0a', 1, '0'),
            (216, 'SequenceFlow_1b61jor', 'WTS_1.0.0.workflow.randon1574236871185.flag', '终止通知', 'TaskEvent_16rrw0a', 'end_3pz5gzw', 1, '0'),
            (217, 'SequenceFlow_13nnr1b', 'WTS_1.0.0.workflow.randon1574817917031.flag', '生效', 'TaskEvent_0r1w2fv', 'end_3pz5gzw', 1, '0'),
            (218, 'SequenceFlow_0iwbgon', 'WTS.workflow.randon1637749194825.flag', '恢复作业', 'TaskEvent_06ya98w', 'AutoEvent_061ij9k', 1, '0'),
            (219, 'SequenceFlow_06pyjkj', 'WTS.workflow.randon1638610301284.flag', '提交', 'AutoEvent_1l8x87t', 'JoinEvent_0ghsw7w', 1, '0'),
            (220, 'SequenceFlow_0x8drai', 'WTS.workflow.randon1640056140076.flag', '提交', 'AutoEvent_0ah9dxe', 'TaskEvent_06ya98w', 1, '0'),
            (221, 'SequenceFlow_0yi2g8l', 'WTS.workflow.randon1640056140105.flag', '提交', 'AutoEvent_061ij9k', 'TaskEvent_1y6xnvi', 1, '0'),
            (222, 'SequenceFlow_1ab2urt', 'WTS.workflow.randon1645077773047.flag', '作业执行审批', 'AutoEvent_11389b7', 'TaskEvent_1y6xnvi', 1, '0'),
            (223, 'SequenceFlow_0vx038o', 'WTS.workflow.randon1655380634861.flag', '分发', 'AutoEvent_103nes6', 'ForkEvent_0r75jj3', 1, '0')
      ) AS seed(offset_id, code, name, name_zh_cn, from_node_code, to_node_code, type, select_staff)
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
       transition_seed.select_staff,
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
           coalesce(name, 'WTS_1.0.0.workflowname.randon1574147897430.flag') AS flow_name
      FROM public.wf_deployment
     WHERE process_key = 'fireWorkWF'
       AND coalesce(valid, 1) = 1
       AND coalesce(is_current_version, 0) = 1
     ORDER BY coalesce(process_version, 0) DESC, id DESC
     LIMIT 1
),
permission_seed AS (
    SELECT *
      FROM (
        VALUES
            (301, 'TaskEvent_15w67f5'),
            (302, 'TaskEvent_129vu5v'),
            (303, 'TaskEvent_01x4zva'),
            (304, 'TaskEvent_0ak83bj'),
            (305, 'TaskEvent_032m1nx'),
            (306, 'TaskEvent_06kzbg6'),
            (307, 'TaskEvent_1y6xnvi'),
            (308, 'TaskEvent_02aitou'),
            (309, 'TaskEvent_16rrw0a'),
            (310, 'TaskEvent_0r1w2fv'),
            (311, 'TaskEvent_06ya98w')
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
       'WTS_1.0.0_workTicket',
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
       'fireWorkWF',
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

CREATE INDEX IF NOT EXISTS idx_wf_task_wts_firework_deployment_code
    ON public.wf_task(deployment_id, code);

CREATE INDEX IF NOT EXISTS idx_wf_transition_wts_firework_deployment_from
    ON public.wf_transition(deployment_id, from_node_code);

CREATE INDEX IF NOT EXISTS idx_rbac_flow_permission_wts_firework_activity
    ON public.rbac_flow_permission(flow_key, activity_code, purview_state);
