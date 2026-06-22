-- Register the recovered WorkAppointment work-plan workflow with the legacy
-- JBPM runtime on PostgreSQL.
--
-- Source evidence:
-- - workPlanEdit custom JS submits through /workAppointment/workPlan/workPlan/workPlanUltraSubmit.
-- - WorkPlanViewService then calls WorkAppointmentWorkTicketPlanService.submit,
--   which starts the workflow with deploymentId.
-- - The recovered database has wf_deployment/process XML for process_key
--   ticketPlan, but it was not marked current and had no JBPM runtime
--   definition, causing ec.workflow.noProcessDefinition.
-- - init.xml defines WAPS_WORK_TICKET_PLANS_DI, but the recovered PostgreSQL
--   schema missed that table, causing WorkTicketPlanDealInfo persistence to
--   fail after workflow start.
-- - WorkTicketPlanSupervision maps to WAPS_WORK_TICKET_PLANS_SV. Earlier
--   compatibility SQL exposed only table_info_id/staff, while Hibernate also
--   selects id/version/valid/main_obj during delete/view loading.

CREATE TABLE IF NOT EXISTS public.waps_work_ticket_plans_di (
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
            'ALTER TABLE public.waps_work_ticket_plans_di ADD COLUMN IF NOT EXISTS %I %s',
            column_def.name,
            column_def.definition
        );
    END LOOP;
END $$;

ALTER TABLE public.waps_work_ticket_plans_di
    ALTER COLUMN assign_staff TYPE varchar(4000),
    ALTER COLUMN comments TYPE varchar(4000),
    ALTER COLUMN signature TYPE varchar(400);

CREATE INDEX IF NOT EXISTS idx_waps_work_ticket_plans_di_table_info
    ON public.waps_work_ticket_plans_di(table_info_id);

CREATE INDEX IF NOT EXISTS idx_waps_work_ticket_plans_di_main_obj
    ON public.waps_work_ticket_plans_di(main_obj);

CREATE INDEX IF NOT EXISTS idx_waps_work_ticket_plans_di_process_key
    ON public.waps_work_ticket_plans_di(process_key);

DO $$
DECLARE
    rel_kind char;
BEGIN
    SELECT c.relkind INTO rel_kind
    FROM pg_class c
    JOIN pg_namespace n ON n.oid = c.relnamespace
    WHERE n.nspname = 'public'
      AND c.relname = 'waps_work_ticket_plans_sv';

    IF to_regclass('public.waps_work_ticket_plans') IS NOT NULL
       AND (rel_kind IS NULL OR rel_kind = 'v') THEN
        EXECUTE $view$
CREATE OR REPLACE VIEW public.waps_work_ticket_plans_sv AS
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
FROM public.waps_work_ticket_plans
WHERE table_info_id IS NOT NULL
  AND owner_staff_id IS NOT NULL
$view$;
    ELSIF rel_kind = 'r' THEN
        ALTER TABLE public.waps_work_ticket_plans_sv ADD COLUMN IF NOT EXISTS id bigint;
        ALTER TABLE public.waps_work_ticket_plans_sv ADD COLUMN IF NOT EXISTS version integer DEFAULT 0;
        ALTER TABLE public.waps_work_ticket_plans_sv ADD COLUMN IF NOT EXISTS valid boolean DEFAULT true;
        ALTER TABLE public.waps_work_ticket_plans_sv ADD COLUMN IF NOT EXISTS main_obj bigint;
        ALTER TABLE public.waps_work_ticket_plans_sv ADD COLUMN IF NOT EXISTS create_staff_id bigint;
        ALTER TABLE public.waps_work_ticket_plans_sv ADD COLUMN IF NOT EXISTS modify_staff_id bigint;
        ALTER TABLE public.waps_work_ticket_plans_sv ADD COLUMN IF NOT EXISTS delete_staff_id bigint;
        ALTER TABLE public.waps_work_ticket_plans_sv ADD COLUMN IF NOT EXISTS create_time timestamp without time zone;
        ALTER TABLE public.waps_work_ticket_plans_sv ADD COLUMN IF NOT EXISTS modify_time timestamp without time zone;
        ALTER TABLE public.waps_work_ticket_plans_sv ADD COLUMN IF NOT EXISTS delete_time timestamp without time zone;
        CREATE INDEX IF NOT EXISTS idx_waps_work_ticket_plans_sv_main_obj
            ON public.waps_work_ticket_plans_sv(main_obj, valid);
        CREATE INDEX IF NOT EXISTS idx_waps_work_ticket_plans_sv_table_info
            ON public.waps_work_ticket_plans_sv(table_info_id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_wfm_task_pending_ticketplan_model
    ON public.wfm_task_pending(process_key, model_id);

CREATE INDEX IF NOT EXISTS idx_wfm_task_pending_ticketplan_table_info
    ON public.wfm_task_pending(process_key, table_info_id);

CREATE OR REPLACE FUNCTION public.adp_cleanup_waps_work_ticket_plan_pending()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF coalesce(OLD.valid, true) = true
       AND coalesce(NEW.valid, false) = false THEN
        DELETE FROM public.wfm_task_pending pending
        WHERE pending.process_key = 'ticketPlan'
          AND (
              pending.model_id = NEW.id
              OR pending.table_info_id = NEW.table_info_id
              OR pending.table_no = NEW.table_no
          );
    END IF;
    RETURN NEW;
END $$;

DROP TRIGGER IF EXISTS trg_waps_work_ticket_plan_pending_cleanup
    ON public.waps_work_ticket_plans;

CREATE TRIGGER trg_waps_work_ticket_plan_pending_cleanup
AFTER UPDATE OF valid ON public.waps_work_ticket_plans
FOR EACH ROW
EXECUTE FUNCTION public.adp_cleanup_waps_work_ticket_plan_pending();

DELETE FROM public.wfm_task_pending pending
USING public.waps_work_ticket_plans plan
WHERE pending.process_key = 'ticketPlan'
  AND coalesce(plan.valid, false) = false
  AND (
      pending.model_id = plan.id
      OR pending.table_info_id = plan.table_info_id
      OR pending.table_no = plan.table_no
  );

WITH current_flow AS (
    SELECT id AS deployment_id,
           'ticketPlan'::text AS process_key,
           7401000000060000::bigint AS deployment_dbid,
           1 AS process_version
      FROM public.wf_deployment
     WHERE process_key = 'ticketPlan'
       AND coalesce(valid, 1) = 1
     ORDER BY coalesce(process_version, 0) DESC, id DESC
     LIMIT 1
)
UPDATE public.wf_deployment deployment
   SET process_version = current_flow.process_version,
       process_definition_id = current_flow.process_key || '-' || current_flow.process_version::text,
       deployment_id = current_flow.deployment_dbid::text,
       is_current_version = CASE WHEN deployment.id = current_flow.deployment_id THEN 1 ELSE 0 END,
       valid = 1,
       publish_flag = 1,
       modify_time = CURRENT_TIMESTAMP
  FROM current_flow
 WHERE deployment.process_key = current_flow.process_key;

WITH current_flow AS (
    SELECT 7401000000060000::bigint AS deployment_dbid
      FROM public.wf_deployment
     WHERE process_key = 'ticketPlan'
       AND coalesce(valid, 1) = 1
       AND coalesce(is_current_version, 0) = 1
     LIMIT 1
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

WITH current_flow AS (
    SELECT 'ticketPlan'::text AS process_key,
           7401000000060000::bigint AS deployment_dbid,
           1 AS process_version
      FROM public.wf_deployment
     WHERE process_key = 'ticketPlan'
       AND coalesce(valid, 1) = 1
       AND coalesce(is_current_version, 0) = 1
     LIMIT 1
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

WITH current_flow AS (
    SELECT deployment.process_key,
           deployment.name,
           7401000000060000::bigint AS deployment_dbid,
           CASE
               WHEN deployment.process_xml_text_backup IS NOT NULL THEN deployment.process_xml_text_backup
               WHEN deployment.process_xml IS NOT NULL THEN convert_from(lo_get(deployment.process_xml), 'UTF8')
               ELSE NULL
           END AS process_xml_text
      FROM public.wf_deployment deployment
     WHERE deployment.process_key = 'ticketPlan'
       AND coalesce(deployment.valid, 1) = 1
       AND coalesce(deployment.is_current_version, 0) = 1
     LIMIT 1
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
     WHERE process_key = 'ticketPlan'
       AND coalesce(valid, 1) = 1
       AND coalesce(is_current_version, 0) = 1
     ORDER BY coalesce(process_version, 0) DESC, id DESC
     LIMIT 1
),
task_seed AS (
    SELECT *
      FROM (
        VALUES
            (101, 'start_glvw15o', 'workAppointment.workflow.randon1654496605228.flag', '开始活动', 1, 'workAppointment_6.1.6.1_workPlan_workPlanEdit', NULL, 0, 0),
            (102, 'TaskEvent_0tb6d76', 'workAppointment.workflow.randon1654496605555.flag', '申请', 4, 'workAppointment_6.1.6.1_workPlan_workPlanEdit', '_blank', 1, 1),
            (103, 'TaskEvent_01f7pag', 'workAppointment.workflow.randon1654496605716.flag', '作业人审批', 4, 'workAppointment_6.1.6.1_workPlan_workPlanApprove', '_blank', 1, 1),
            (104, 'TaskEvent_0uatdpl', 'workAppointment.workflow.randon1654686422334.flag', '分析人审批', 4, 'workAppointment_6.1.6.1_workPlan_workPlanApprove', '_blank', 1, 1),
            (105, 'TaskEvent_1p2qrpi', 'workAppointment.workflow.randon1654686422526.flag', '审批人', 4, 'workAppointment_6.1.6.1_workPlan_workPlanApprove', '_blank', 1, 1),
            (106, 'end_j4ipyt9', 'workAppointment.workflow.randon1654496605394.flag', '结束活动', 2, NULL, NULL, 0, 0),
            (107, 'EndCancelEvent_167sjzc', 'workAppointment.workflow.randon1654496605965.flag', '作废', 3, NULL, NULL, 0, 0)
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
       'ticketPlan',
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
     WHERE process_key = 'ticketPlan'
       AND coalesce(valid, 1) = 1
       AND coalesce(is_current_version, 0) = 1
     ORDER BY coalesce(process_version, 0) DESC, id DESC
     LIMIT 1
),
transition_seed AS (
    SELECT *
      FROM (
        VALUES
            (201, 'SequenceFlow_167j5aa', 'workAppointment.workflow.randon1654496605885.flag', '制定', 'start_glvw15o', 'TaskEvent_0tb6d76', 1),
            (202, 'SequenceFlow_15k1f4h', 'workAppointment.workflow.randon1654496605917.flag', '提交', 'TaskEvent_0tb6d76', 'TaskEvent_01f7pag', 1),
            (203, 'SequenceFlow_1wa5f0c', 'workAppointment.workflow.randon1654496606139.flag', '作废', 'TaskEvent_0tb6d76', 'EndCancelEvent_167sjzc', 3),
            (204, 'SequenceFlow_1arr2zz', 'workAppointment.workflow.randon1654496605941.flag', '提交', 'TaskEvent_01f7pag', 'TaskEvent_0uatdpl', 1),
            (205, 'SequenceFlow_1dcbf1x', 'workAppointment.workflow.randon1654686202028.flag', '驳回', 'TaskEvent_01f7pag', 'TaskEvent_0tb6d76', 2),
            (206, 'SequenceFlow_1wj6nyt', 'workAppointment.workflow.randon1654686177732.flag', '提交', 'TaskEvent_0uatdpl', 'TaskEvent_1p2qrpi', 1),
            (207, 'SequenceFlow_09ia91s', 'workAppointment.workflow.randon1654686228789.flag', '驳回', 'TaskEvent_0uatdpl', 'TaskEvent_0tb6d76', 2),
            (208, 'SequenceFlow_1qu55ey', 'workAppointment.workflow.randon1654686298294.flag', '生效', 'TaskEvent_1p2qrpi', 'end_j4ipyt9', 1),
            (209, 'SequenceFlow_0s1cc6j', 'workAppointment.workflow.randon1654686389941.flag', '驳回', 'TaskEvent_1p2qrpi', 'TaskEvent_0tb6d76', 2)
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
           coalesce(name, 'workAppointment.workflowname.randon1654496538009.flag') AS flow_name
      FROM public.wf_deployment
     WHERE process_key = 'ticketPlan'
       AND coalesce(valid, 1) = 1
       AND coalesce(is_current_version, 0) = 1
     ORDER BY coalesce(process_version, 0) DESC, id DESC
     LIMIT 1
),
permission_seed AS (
    SELECT *
      FROM (
        VALUES
            (301, 'TaskEvent_0tb6d76'),
            (302, 'TaskEvent_01f7pag'),
            (303, 'TaskEvent_0uatdpl'),
            (304, 'TaskEvent_1p2qrpi')
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
       'workAppointment_6.1.6.1_workPlan',
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
       'ticketPlan',
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

CREATE INDEX IF NOT EXISTS idx_wf_task_ticketplan_deployment_code
    ON public.wf_task(deployment_id, code);

CREATE INDEX IF NOT EXISTS idx_wf_transition_ticketplan_deployment_from
    ON public.wf_transition(deployment_id, from_node_code);

CREATE INDEX IF NOT EXISTS idx_rbac_flow_permission_ticketplan_activity
    ON public.rbac_flow_permission(flow_key, activity_code, purview_state);
