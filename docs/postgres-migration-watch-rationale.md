# PostgreSQL Watch 语句说明

本文件由 `scripts/generate-postgres-migration-inventory.py` 生成，用于解释 PostgreSQL init SQL 中允许但需要关注的 watch 语句。

## 口径

- `DROP DATABASE`、`DROP SCHEMA`、`DROP TABLE`、`TRUNCATE` 仍是阻断级高风险语句。
- `DROP VIEW`、`DROP FUNCTION`、`DROP TRIGGER`、`DROP OPERATOR`、`DROP AGGREGATE` 只允许带 `IF EXISTS`，用于幂等重建兼容对象。
- `DELETE FROM` 只允许带 `WHERE`，用于有范围的测试环境兼容数据清理或 pending 清理。
- 这里的 PASS 只证明语句具备最低保护；业务影响仍需要在对应 SQL、验收报告或 PR 中解释。

## 摘要

- Watch 语句：`60`。
- Watch 安全问题：`0`。

## 语句清单

| File | Line | Pattern | Safety | Rationale | Statement |
| --- | --- | --- | --- | --- | --- |
| 001-adp-postgres-compat.sql | 1 | drop-operator | guarded-if-exists | Allowed only with IF EXISTS when replacing PostgreSQL compatibility operators idempotently. | DROP OPERATOR IF EXISTS public.= (varchar, smallint); |
| 001-adp-postgres-compat.sql | 2 | drop-operator | guarded-if-exists | Allowed only with IF EXISTS when replacing PostgreSQL compatibility operators idempotently. | DROP OPERATOR IF EXISTS public.= (varchar, integer); |
| 001-adp-postgres-compat.sql | 3 | drop-operator | guarded-if-exists | Allowed only with IF EXISTS when replacing PostgreSQL compatibility operators idempotently. | DROP OPERATOR IF EXISTS public.= (varchar, bigint); |
| 001-adp-postgres-compat.sql | 4 | drop-function | guarded-if-exists | Allowed only with IF EXISTS when replacing compatibility helper functions idempotently. | DROP FUNCTION IF EXISTS public.varchar_eq_smallint(varchar, smallint); |
| 001-adp-postgres-compat.sql | 5 | drop-function | guarded-if-exists | Allowed only with IF EXISTS when replacing compatibility helper functions idempotently. | DROP FUNCTION IF EXISTS public.varchar_eq_integer(varchar, integer); |
| 001-adp-postgres-compat.sql | 6 | drop-function | guarded-if-exists | Allowed only with IF EXISTS when replacing compatibility helper functions idempotently. | DROP FUNCTION IF EXISTS public.varchar_eq_bigint(varchar, bigint); |
| 007-rbac-boolean-columns.sql | 5 | drop-view | guarded-if-exists | Allowed only with IF EXISTS when recreating compatibility views idempotently. | DROP VIEW IF EXISTS base_rolepermission; |
| 007-rbac-boolean-columns.sql | 6 | drop-view | guarded-if-exists | Allowed only with IF EXISTS when recreating compatibility views idempotently. | DROP VIEW IF EXISTS base_menuoperate; |
| 007-rbac-boolean-columns.sql | 7 | drop-view | guarded-if-exists | Allowed only with IF EXISTS when recreating compatibility views idempotently. | DROP VIEW IF EXISTS base_menuinfo; |
| 008-rbac-boolean-aggregate.sql | 16 | drop-aggregate | guarded-if-exists | Allowed only with IF EXISTS when replacing PostgreSQL compatibility aggregates idempotently. | DROP AGGREGATE IF EXISTS public.sum(boolean); |
| 009-rbac-boolean-integer-operators.sql | 66 | drop-operator | guarded-if-exists | Allowed only with IF EXISTS when replacing PostgreSQL compatibility operators idempotently. | DROP OPERATOR IF EXISTS public.= (boolean, integer); |
| 009-rbac-boolean-integer-operators.sql | 67 | drop-operator | guarded-if-exists | Allowed only with IF EXISTS when replacing PostgreSQL compatibility operators idempotently. | DROP OPERATOR IF EXISTS public.= (integer, boolean); |
| 009-rbac-boolean-integer-operators.sql | 68 | drop-operator | guarded-if-exists | Allowed only with IF EXISTS when replacing PostgreSQL compatibility operators idempotently. | DROP OPERATOR IF EXISTS public.<> (boolean, integer); |
| 009-rbac-boolean-integer-operators.sql | 69 | drop-operator | guarded-if-exists | Allowed only with IF EXISTS when replacing PostgreSQL compatibility operators idempotently. | DROP OPERATOR IF EXISTS public.<> (integer, boolean); |
| 010-rbac-boolean-minmax-aggregates.sql | 34 | drop-aggregate | guarded-if-exists | Allowed only with IF EXISTS when replacing PostgreSQL compatibility aggregates idempotently. | DROP AGGREGATE IF EXISTS public.max(boolean); |
| 010-rbac-boolean-minmax-aggregates.sql | 35 | drop-aggregate | guarded-if-exists | Allowed only with IF EXISTS when replacing PostgreSQL compatibility aggregates idempotently. | DROP AGGREGATE IF EXISTS public.min(boolean); |
| 011-rbac-userpermission-boolean-fixups.sql | 5 | drop-view | guarded-if-exists | Allowed only with IF EXISTS when recreating compatibility views idempotently. | DROP VIEW IF EXISTS base_userpermission; |
| 014-ui-smoke-runtime-fixups.sql | 169 | drop-view | guarded-if-exists | Allowed only with IF EXISTS when recreating compatibility views idempotently. | DROP VIEW IF EXISTS public.base_menuoperate; |
| 014-ui-smoke-runtime-fixups.sql | 243 | drop-view | guarded-if-exists | Allowed only with IF EXISTS when recreating compatibility views idempotently. | DROP VIEW IF EXISTS public.base_datapermission; |
| 014-ui-smoke-runtime-fixups.sql | 384 | drop-view | guarded-if-exists | Allowed only with IF EXISTS when recreating compatibility views idempotently. | DROP VIEW IF EXISTS public.base_positionwork; |
| 017-rbac-menuoperate-url-ref-fixups.sql | 195 | drop-view | guarded-if-exists | Allowed only with IF EXISTS when recreating compatibility views idempotently. | DROP VIEW IF EXISTS public.base_datapmsposition; |
| 017-rbac-menuoperate-url-ref-fixups.sql | 196 | drop-view | guarded-if-exists | Allowed only with IF EXISTS when recreating compatibility views idempotently. | DROP VIEW IF EXISTS public.base_datapermissionstaff; |
| 017-rbac-menuoperate-url-ref-fixups.sql | 197 | drop-view | guarded-if-exists | Allowed only with IF EXISTS when recreating compatibility views idempotently. | DROP VIEW IF EXISTS public.base_datapermission; |
| 018-org-mnecode-fixups.sql | 61 | drop-view | guarded-if-exists | Allowed only with IF EXISTS when recreating compatibility views idempotently. | DROP VIEW IF EXISTS public.base_staff_mnecode; |
| 018-org-mnecode-fixups.sql | 62 | drop-view | guarded-if-exists | Allowed only with IF EXISTS when recreating compatibility views idempotently. | DROP VIEW IF EXISTS public.base_department_mnecode; |
| 018-org-mnecode-fixups.sql | 63 | drop-view | guarded-if-exists | Allowed only with IF EXISTS when recreating compatibility views idempotently. | DROP VIEW IF EXISTS public.base_position_mnecode; |
| 018-org-mnecode-fixups.sql | 64 | drop-view | guarded-if-exists | Allowed only with IF EXISTS when recreating compatibility views idempotently. | DROP VIEW IF EXISTS public.base_custom_group_mnecode; |
| 021-notification-postgres-idempotent-fixups.sql | 38 | drop-trigger | guarded-if-exists | Allowed only with IF EXISTS when replacing trigger definitions idempotently. | DROP TRIGGER IF EXISTS trg_notice_protocol_before_insert_idempotent ON public.notice_protocol; |
| 021-notification-postgres-idempotent-fixups.sql | 91 | drop-trigger | guarded-if-exists | Allowed only with IF EXISTS when replacing trigger definitions idempotently. | DROP TRIGGER IF EXISTS trg_notice_protocol_tmpl_before_insert_idempotent ON public.notice_protocol_tmpl; |
| 021-notification-postgres-idempotent-fixups.sql | 130 | drop-trigger | guarded-if-exists | Allowed only with IF EXISTS when replacing trigger definitions idempotently. | DROP TRIGGER IF EXISTS trg_notice_tmpl_before_insert_idempotent ON public.notice_tmpl; |
| 021-notification-postgres-idempotent-fixups.sql | 167 | drop-trigger | guarded-if-exists | Allowed only with IF EXISTS when replacing trigger definitions idempotently. | DROP TRIGGER IF EXISTS trg_notice_topic_before_insert_idempotent ON public.notice_topic; |
| 021-notification-postgres-idempotent-fixups.sql | 199 | drop-trigger | guarded-if-exists | Allowed only with IF EXISTS when replacing trigger definitions idempotently. | DROP TRIGGER IF EXISTS trg_notice_topic_tmpl_rel_before_insert_idempotent ON public.notice_topic_tmpl_rel; |
| 037-platform-page-smoke-fixups.sql | 304 | drop-view | guarded-if-exists | Allowed only with IF EXISTS when recreating compatibility views idempotently. | DROP VIEW IF EXISTS public.base_menuoperate; |
| 039-platform-menu-full-smoke-fixups.sql | 414 | drop-view | guarded-if-exists | Allowed only with IF EXISTS when recreating compatibility views idempotently. | DROP VIEW IF EXISTS public.base_menuinfo; |
| 039-platform-menu-full-smoke-fixups.sql | 469 | drop-view | guarded-if-exists | Allowed only with IF EXISTS when recreating compatibility views idempotently. | DROP VIEW IF EXISTS public.base_menuoperate; |
| 039-platform-menu-full-smoke-fixups.sql | 538 | drop-view | guarded-if-exists | Allowed only with IF EXISTS when recreating compatibility views idempotently. | DROP VIEW IF EXISTS public.base_roleuser; |
| 039-platform-menu-full-smoke-fixups.sql | 551 | drop-view | guarded-if-exists | Allowed only with IF EXISTS when recreating compatibility views idempotently. | DROP VIEW IF EXISTS public.base_custom_group; |
| 039-platform-menu-full-smoke-fixups.sql | 570 | drop-view | guarded-if-exists | Allowed only with IF EXISTS when recreating compatibility views idempotently. | DROP VIEW IF EXISTS public.base_custom_groupmember; |
| 049-rbac-menuoperate-valid-integer.sql | 14 | drop-view | guarded-if-exists | Allowed only with IF EXISTS when recreating compatibility views idempotently. | DROP VIEW IF EXISTS public.base_menuoperate; |
| 063-disable-legacy-external-scheduler-jobs.sql | 30 | delete | scoped-delete | Allowed only for scoped cleanup statements with an explicit WHERE clause; never for full-table cleanup. | DELETE FROM public.scheduler_job_log_info WHERE job_status = 3 OR job_service_api LIKE 'http://192.168.90.%' OR exception_info LIKE '%192.168.90.%'; |
| 065-business-view-runtime-json.sql | 48 | drop-operator | guarded-if-exists | Allowed only with IF EXISTS when replacing PostgreSQL compatibility operators idempotently. | DROP OPERATOR IF EXISTS public.= (text, bigint); |
| 065-business-view-runtime-json.sql | 51 | drop-operator | guarded-if-exists | Allowed only with IF EXISTS when replacing PostgreSQL compatibility operators idempotently. | DROP OPERATOR IF EXISTS public.= (bigint, text); |
| 065-business-view-runtime-json.sql | 54 | drop-operator | guarded-if-exists | Allowed only with IF EXISTS when replacing PostgreSQL compatibility operators idempotently. | DROP OPERATOR IF EXISTS public.= (boolean, text); |
| 065-business-view-runtime-json.sql | 57 | drop-operator | guarded-if-exists | Allowed only with IF EXISTS when replacing PostgreSQL compatibility operators idempotently. | DROP OPERATOR IF EXISTS public.= (text, boolean); |
| 075-rbac-data-resource-permission-tables.sql | 191 | drop-trigger | guarded-if-exists | Allowed only with IF EXISTS when replacing trigger definitions idempotently. | DROP TRIGGER IF EXISTS trg_rbac_role_data_permission_sync_users ON public.rbac_role_data_permission; |
| 080-wom-wait-record-state-sync-trigger.sql | 42 | drop-trigger | guarded-if-exists | Allowed only with IF EXISTS when replacing trigger definitions idempotently. | DROP TRIGGER IF EXISTS trg_adp_wom_sync_work_order_wait_state ON public.wom_wait_put_records; |
| 080-wom-wait-record-state-sync-trigger.sql | 76 | drop-trigger | guarded-if-exists | Allowed only with IF EXISTS when replacing trigger definitions idempotently. | DROP TRIGGER IF EXISTS trg_adp_wom_push_task_state_to_wait_records ON public.wom_produce_tasks; |
| 094-rbac-menuoperate-foundation-boolean-casts.sql | 12 | drop-view | guarded-if-exists | Allowed only with IF EXISTS when recreating compatibility views idempotently. | DROP VIEW IF EXISTS public.base_menuoperate; |
| 095-rbac-flow-permission-foundation-boolean-casts.sql | 8 | drop-view | guarded-if-exists | Allowed only with IF EXISTS when recreating compatibility views idempotently. | DROP VIEW IF EXISTS public.base_datapermission; |
| 095-rbac-flow-permission-foundation-boolean-casts.sql | 42 | drop-view | guarded-if-exists | Allowed only with IF EXISTS when recreating compatibility views idempotently. | DROP VIEW IF EXISTS public.base_datapmsposition; |
| 101-wom-output-finish-num-sync.sql | 31 | drop-trigger | guarded-if-exists | Allowed only with IF EXISTS when replacing trigger definitions idempotently. | DROP TRIGGER IF EXISTS trg_adp_wom_before_task_finish_num ON public.wom_produce_tasks; |
| 101-wom-output-finish-num-sync.sql | 72 | drop-trigger | guarded-if-exists | Allowed only with IF EXISTS when replacing trigger definitions idempotently. | DROP TRIGGER IF EXISTS trg_adp_wom_sync_task_finish_num_from_output ON public.wom_output_details; |
| 106-wom-process-wait-end-time-sync.sql | 45 | drop-trigger | guarded-if-exists | Allowed only with IF EXISTS when replacing trigger definitions idempotently. | DROP TRIGGER IF EXISTS trg_adp_wom_sync_process_wait_end_time_from_process ON public.wom_task_processes; |
| 106-wom-process-wait-end-time-sync.sql | 54 | drop-trigger | guarded-if-exists | Allowed only with IF EXISTS when replacing trigger definitions idempotently. | DROP TRIGGER IF EXISTS trg_adp_wom_sync_process_wait_end_time_from_exelog ON public.wom_process_exelogs; |
| 136-workappointment-workplan-workflow-config.sql | 185 | delete | scoped-delete | Allowed only for scoped cleanup statements with an explicit WHERE clause; never for full-table cleanup. | DELETE FROM public.wfm_task_pending pending WHERE pending.process_key = 'ticketPlan' AND ( pending.model_id = NEW.id OR pending.table_info_id = NEW.table_info_id OR pending.table_no = NEW.table_no ); |
| 136-workappointment-workplan-workflow-config.sql | 196 | drop-trigger | guarded-if-exists | Allowed only with IF EXISTS when replacing trigger definitions idempotently. | DROP TRIGGER IF EXISTS trg_waps_work_ticket_plan_pending_cleanup ON public.waps_work_ticket_plans; |
| 136-workappointment-workplan-workflow-config.sql | 204 | delete | scoped-delete | Allowed only for scoped cleanup statements with an explicit WHERE clause; never for full-table cleanup. | DELETE FROM public.wfm_task_pending pending USING public.waps_work_ticket_plans plan WHERE pending.process_key = 'ticketPlan' AND coalesce(plan.valid, false) = false AND ( pending.model_id = plan.id OR pending.table_info |
| 137-wom-reject-material-workflow-config.sql | 133 | delete | scoped-delete | Allowed only for scoped cleanup statements with an explicit WHERE clause; never for full-table cleanup. | DELETE FROM public.wfm_task_pending pending WHERE pending.process_key IN ('batchRejectFlow', 'materiaReject', 'prePareRejectFlow') AND ( pending.model_id = NEW.id OR pending.table_info_id = NEW.table_info_id OR pending.t |
| 137-wom-reject-material-workflow-config.sql | 144 | drop-trigger | guarded-if-exists | Allowed only with IF EXISTS when replacing trigger definitions idempotently. | DROP TRIGGER IF EXISTS trg_wom_reject_material_pending_cleanup ON public.wom_reject_materials; |
| 137-wom-reject-material-workflow-config.sql | 152 | delete | scoped-delete | Allowed only for scoped cleanup statements with an explicit WHERE clause; never for full-table cleanup. | DELETE FROM public.wfm_task_pending pending USING public.wom_reject_materials reject_material WHERE pending.process_key IN ('batchRejectFlow', 'materiaReject', 'prePareRejectFlow') AND coalesce(reject_material.valid, fal |
