# 生产模块源动作地图

## 结论

本文件把 WOM 生产执行相关动作从旧源包、当前运行时页面和后端源码三个来源做了交叉对齐。当前 WOM 制造任务 `start/hold/restart`、无产出明细 `stop` 最小路径、完工报工产出明细、投入明细报工、提前放料确认、下推备料需求、工序开始/结束、活动开始/结束、简易活动报工、制造请检和质量活动检验单生成已经完成真实前端触发和 PostgreSQL 落库验收；质量判定、不良数、工作单元设置等其他动作仍缺少带 marker 的前置数据，不能由已通过动作推断为已落库。

机器可读记录见 `metadata/production-module-source-action-map.json`。

## 证据来源

| 来源 | 证据 |
| --- | --- |
| 旧源包 | `/Users/zhangchu/Documents/ADP/mes-modules-source-repo/modules/wom/WOM_6.1.3.4` |
| 前端事件 | `service/src/main/resources/custom/WOM/produceTask/produceTask/**/eventJs/customEvent.js` |
| 模型表 | `service/src/main/resources/META-INF/init/init.xml` |
| 后端入口 | `WOMProduceTaskController`、`WOMProduceTaskServiceImpl` |
| 真实浏览器探测 | `/tmp/adp-production-action-discovery-20260616014350/production-action-discovery.json`、`/tmp/adp-production-action-discovery-20260615180122/production-action-discovery.json` |
| runtime JSON / pageType 直连复验 | 5 个动作 viewCode 均 `layoutJson` 200，pageType 为 `EDIT/VIEW`，真实浏览器可渲染但未提交写请求 |
| 列表按钮复验 | `/tmp/adp-wom-button-visibility-20260615180122/wom-button-visibility.json`；`makeTaskList` 8 个动作按钮、`prepareMakeTaskList` 1 个动作按钮可见 |
| PostgreSQL 兼容表回读 | `deploy/docker/postgres/init/079-wom-wait-put-records-table.sql`；测试机 `public.wom_wait_put_records` 存在、112 列、主键和 5 个索引存在；`deploy/docker/postgres/init/080-wom-wait-record-state-sync-trigger.sql` 已修复 hold/restart 后待办状态同步 |
| 状态流转/报工/提前放料/备料/工序/活动/请检/检验单生成落库验收 | `/tmp/adp-wom-start-persistence-script-current.json`、`/tmp/adp-wom-hold-restart-persistence-current.json`、`/tmp/adp-wom-stop-persistence-current.json`、`/tmp/adp-wom-stop-output-persistence-current.json`、`/tmp/adp-wom-putin-active-persistence-current.json`、`/tmp/adp-wom-advance-release-persistence-current.json`、`/tmp/adp-wom-prepare-need-persistence-current.json`、`/tmp/adp-wom-process-end-persistence-current.json`、`/tmp/adp-wom-active-persistence-current.json`、`/tmp/adp-wom-active-end-persistence-current.json`、`/tmp/adp-wom-easy-active-persistence-current.json`、`/tmp/adp-wom-manu-inspect-persistence-acceptance-rerun.json`、`/tmp/adp-wom-checkoutbill-persistence-20260618200829/wom-checkoutbill-persistence-results.json` |
| WOM 投入明细/消耗记录专项分析 | [`wom-consumption-record-analysis.md`](wom-consumption-record-analysis.md)；`metadata/wom-consumption-record-analysis.json`；历史 `0/0` 已定位为源码执行记录缺口，最新 `...CLOSED_11` 回读执行/消耗 `1/1`、产出明细/产出台账 `2/2`，由服务层补丁关闭且未引入数据库触发器 |

## 真实页面探测

| 页面/路由 | 前端结果 | API / network | 状态 |
| --- | --- | --- | --- |
| `/msService/WOM/produceTask/produceTask/makeTaskList` | 页面 200；`开始/保持/重启/结束/提前放料/请检/生产过程追溯/生成二维码` 可见；2026-06-20 地址切换后复验无 visible/console/page/request/network error，仍未发现新增入口 | `POST /msService/WOM/produceTask/produceTask/makeTaskList-pending` 200；未产生写请求；证据 `/tmp/adp-production-action-discovery-public-current/production-action-discovery.json` | PASS_READ_ONLY |
| `/msService/WOM/produceTask/produceTask/prepareMakeTaskList` | 页面 200；`下推备料需求` 可见；列表 `共 0 项` | `POST /msService/WOM/produceTask/produceTask/prepareMakeTaskList-query` 200；未产生写请求 | PASS_READ_ONLY |
| `/msService/WOM/produceTask/produceTask/makeTaskEdit` | 外层页面 200；`layoutJson` 200；真实浏览器可渲染；2026-06-20 带 `entityCode/viewCode` 复验渲染 17 个表单控件但无可见保存/提交按钮；未提交写请求 | `editStates?viewCode=WOM_1.0.0_produceTask_makeTaskEdit` 200；`layoutJson?viewCode=WOM_1.0.0_produceTask_makeTaskEdit&isEs5=true` 200；证据 `/tmp/adp-production-action-discovery-public-edit-entity-current/production-action-discovery.json` | PASS_RENDER_ONLY |
| `/msService/WOM/produceTask/produceTask/makeTaskSubmitView` | 外层页面 200；`layoutJson` 200；真实浏览器可渲染；未提交写请求 | `layoutJson?viewCode=WOM_1.0.0_produceTask_makeTaskSubmitView&isEs5=true` 200 | PASS_RENDER_ONLY |
| `/msService/WOM/produceTask/produceTask/makeTaskView` | 外层页面 200；`layoutJson` 200；真实浏览器可渲染；未提交写请求 | `layoutJson?viewCode=WOM_1.0.0_produceTask_makeTaskView&isEs5=true` 200 | PASS_RENDER_ONLY |
| `/msService/WOM/produceTask/produceTask/makeTaskBatchView` | 外层页面 200；`layoutJson` 200；真实浏览器可渲染；未提交写请求 | `layoutJson?viewCode=WOM_1.0.0_produceTask_makeTaskBatchView&isEs5=true` 200 | PASS_RENDER_ONLY |
| `/msService/WOM/produceTask/produceTask/easyTaskOperateView` | 外层页面 200；`layoutJson` 200；真实浏览器可渲染；后续已通过该页提交 marker 简易活动报工 | `layoutJson?viewCode=WOM_1.0.0_produceTask_easyTaskOperateView&isEs5=true` 200；`POST /endEasyActive/8388157077549689` 200 | PASS_RENDER_ONLY |
| `/msService/WOM/produceTask/produceTask/makeTaskGraphList` | 页面 404 | document 404 | FAIL_NOT_FOUND |

## 动作级地图

| 动作 | 前端源事件 | API endpoint | 后端入口 | 目标表 | 当前状态 |
| --- | --- | --- | --- | --- | --- |
| 制造任务待办列表查询 | `makeTaskList` 查询 | `POST /msService/WOM/produceTask/produceTask/makeTaskList-pending` | runtime list query | `WOM_PRODUCE_TASKS`, `wfm_task_pending` | PASS_READ_ONLY |
| 备料候选列表查询 | `prepareMakeTaskList` 查询 | `POST /msService/WOM/produceTask/produceTask/prepareMakeTaskList-query` | runtime list query | `WOM_PRODUCE_TASKS` | PASS_READ_ONLY |
| 生成备料需求 | `prePareNeed(event)` | `POST /msService/WOM/produceTask/produceTask/generatePrepareNeed` | `WOMProduceTaskController.generatePrepareNeed -> WOMProduceTaskServiceImpl.generatePrepareNeed` | `WOM_PRODUCE_TASKS`, `WOM_PRE_PARE_NEEDS`, `WOM_PRE_PARE_NEED_REFS`, `WOM_PRE_PARE_NEEDS_DI`, `wfm_task_pending`, `jbpm4_execution` | PASS |
| 制造任务开始/暂停/恢复 | `startTaskEvent / pauseTaskEvent / recoveryTaskEvent` | `POST /msService/WOM/produceTask/produceTask/updateTaskState` | `WOMProduceTaskController.updateTaskState -> WOMProduceTaskServiceImpl.updateTaskState` | `WOM_PRODUCE_TASKS`, `WOM_WAIT_PUT_RECORDS`, `WOM_PROC_REPORTS`, `WOM_PRODUCE_TASK_EXELOG` | PASS |
| 制造任务完工报工产出明细 | `stopTaskEvent(event)` | `POST /msService/WOM/produceTask/produceTask/addOutputByOutPutDetails` | `WOMProduceTaskController.addOutputByOutPutDetails -> WOMProduceTaskServiceImpl.addOutputByOutPutDetails -> WOMMatOutptRecordServiceImpl.afterSaveMatOutptRecord` | `WOM_PROC_REPORTS`, `WOM_PRODUCE_TASKS`, `WOM_PRODUCE_TASK_EXELOG`, `WOM_OUTPUT_DETAILS`, `WOM_MAT_OUTPT_RECORDS` | PASS |
| 投入明细保存并结束投料活动 | `startActive(event) -> remainMaterialView/save -> endActiveEvent(event)` | `GET /msService/WOM/produceTask/produceTask/startActive`、`POST /msService/WOM/procReport/putinDetail/remainMaterialView/save`、`GET /msService/WOM/produceTask/produceTask/endActive` | `WOMProduceTaskController.startActive/endActive -> WOMProduceTaskServiceImpl.startActive/endActive/toProcfeedback`；缺失执行记录由服务层按明细补建并去重，再复用 `generateInOutRecordByAvtiveRecord` | `WOM_TASK_ACTIVES`, `WOM_PROC_REPORTS`, `WOM_ACTI_EXELOGS`, `WOM_WAIT_PUT_RECORDS`, `WOM_PUTIN_DETAILS`, `WOM_MAT_CONSUM_RECODS` | PASS：`...CLOSED_11` 精确证明明细/执行/消耗 `1/1/1`，页面无空白警告 |
| 提前放料确认 | `earlyPutInFunc(event)` | `GET /msService/WOM/produceTask/produceTask/setAdvanceTrue/{orderId}` | `WOMProduceTaskController.setAdvanceFlag -> WOMProduceTaskServiceImpl.setAdvaceTrue` | `WOM_PRODUCE_TASKS` | PASS |
| 制造请检下推 | `manuInspect(event)` | `POST /msService/WOM/produceTask/produceTask/createManuInspect` | `WOMProduceTaskController.createManuInspect -> WOMProduceTaskServiceImpl.createManuInspect` | `WOM_PRODUCE_TASKS`, `WOM_WAIT_PUT_RECORDS`, `WOM_PRODUCE_TASK_EXELOG`, `BASESET_BATCH_INFOS`, `QCS_INSPECTS`, `QCS_INSPECT_STDS`, `RM_FORMULA_QUALITIES`, `LIMSBA_QUALITY_STDS`, `LIMSBA_STD_VERSIONS`, `LIMSBA_ANALY_PROD_STDS`, `WF_CUSTOM_CONFIG`, `WF_DEPLOYMENT` | PASS |
| 工序开始/结束 | `startProcessEvent / endProcessEvent` | `/msService/WOM/produceTask/taskProcess/start/{id}`、`/end/{id}` | `WOMTaskProcessController.startProcess/endProcess -> WOMTaskProcessServiceImpl.startProcess/endProcess -> WOMProduceTaskServiceImpl.insertActiveWaitPut/insertProcessWaitPut` | `WOM_TASK_PROCESSES`, `WOM_PROC_REPORTS`, `WOM_PROCESS_EXELOGS`, `WOM_WAIT_PUT_RECORDS`, `RM_PROCESS_ACTIVES` | PASS |
| 工序工作单元设置 | `setUnitEvent(event)` | `POST /msService/WOM/produceTask/taskProcess/processUnitEdit/submit?id={processId}` | `WOMTaskProcessController.submit -> WOMTaskProcessServiceImpl.afterSaveTaskProcess -> WOMProduceTaskServiceImpl.modifyTaskWorkUnitByProcess` | `WOM_TASK_PROCESSES`, `WOM_WAIT_PUT_RECORDS`, `HM_FACTORY_MODELS` | PASS：marker `ADP_E2E_20260619174817_WOM_ACTIVE_WU` 经真实浏览器 `processUnitEdit` 提交后写入 `wom_task_processes.equipment_id`，并同步 `wom_wait_put_records` process/workOrder 行 `euq_id/equ_code/equ_name`；证据 `/tmp/adp-wom-process-unit-persistence-current.json` |
| 活动开始/结束 | `startActive(event) / endActiveEvent(event)` | `GET /msService/WOM/produceTask/produceTask/startActive`、`/endActive` | `WOMProduceTaskController.startActive/endActive -> WOMProduceTaskServiceImpl.startActive/endActive` | `WOM_TASK_ACTIVES`, `WOM_TASK_PROCESSES`, `WOM_PRODUCE_TASKS`, `WOM_PROC_REPORTS`, `WOM_ACTI_EXELOGS`, `WOM_WAIT_PUT_RECORDS` | PASS |
| 简易活动报工 | `reportEvent(event)` | `POST /msService/WOM/produceTask/produceTask/endEasyActive/{id}` | `WOMProduceTaskController.endEasyActive -> WOMProduceTaskServiceImpl.endEasyActive` | `WOM_TASK_ACTIVES`, `WOM_PROC_REPORTS`, `WOM_ACTI_EXELOGS`, `WOM_WAIT_PUT_RECORDS`, `WOM_OUTPUT_DETAILS`, `WOM_MAT_OUTPT_RECORDS` | PASS |
| 检验单生成 | `generateCheckoutBillFunc(event)` | `GET /msService/WOM/produceTask/produceTask/checkoutBill/generate/{activeId}` | `WOMProduceTaskController.generateCheckoutBill -> WOMProduceTaskServiceImpl.generateCheckoutBill -> WOMProcReportServiceImpl.getByEasyActive -> WOMQCSServiceImpl.createInspect -> QCSInspectServiceImpl.createInspect -> WOMProduceTaskServiceImpl.endEasyActive` | `WOM_TASK_ACTIVES`, `WOM_WAIT_PUT_RECORDS`, `WOM_PROC_REPORTS`, `WOM_ACTI_EXELOGS`, `QCS_INSPECTS`, `QCS_INSPECT_STDS`, `QCS_INSPECT_COMS`, `BASESET_BATCH_INFOS` | PASS |
| 制造任务结束最小路径（无产出明细） | `stopTaskEvent(event) -> outPutCommonTaskEdit -> addOutputByOutPutDetails` | `POST /msService/WOM/produceTask/produceTask/addOutputByOutPutDetails` | `WOMProduceTaskController.addOutputByOutPutDetails -> WOMProduceTaskServiceImpl.addOutputByOutPutDetails -> updateTaskState(stop)` | `WOM_PRODUCE_TASKS`, `WOM_WAIT_PUT_RECORDS`, `WOM_PROC_REPORTS`, `WOM_PRODUCE_TASK_EXELOG`, `WOM_OUTPUT_DETAILS`, `WOM_TASK_ACT_ITEMSS` | PASS |
| public 日计划生成制造指令单接口 | retired legacy callback | `POST /msService/public/WOM/produceTask/produceTask/produceTaskCreated` | `WOMProduceTaskController.produceTaskCreated -> explicit deprecated Result.fail` | 无 | NOT_APPLICABLE：旧源码创建主体被注释且会假成功；当前契约已正式废弃，真实验收返回 `HTTP 200/code=400/已废弃`，PostgreSQL marker `0 -> 0`。受支持入口为认证后的 `produceTaskCreated2` 和 `makeTaskList -> 新建指令单`；详见 [`wom-public-produce-task-created-analysis.md`](wom-public-produce-task-created-analysis.md) |

## 落库验收前置条件

这些动作从 `BLOCKED` 变为 `PASS` 前，必须先完成：

1. 为剩余动作准备带 marker 的生产任务、工序、活动、物料和检验前置数据；`updateTaskState(start/hold/restart)` 所需 `WOM_WAIT_PUT_RECORDS` 表结构和待办状态同步已由 `079`、`080` 两个 PostgreSQL 补丁处理，工序开始/结束所需 `RM_PROCESS_ACTIVES`、`convert(proc_sort,int)` 兼容和 process wait 结束时间同步已由 `104`、`105`、`106` 处理，提前放料确认已通过 `setAdvanceTrue` 真实前端验收，`startActive/endActive` 所需活动 runtime 按钮和 PostgreSQL 兼容已由 `098`、`099`、`100` 处理，简易活动报工按钮已由 `102` 恢复并复验；当前投料路径会补建缺失执行记录并生成 `WOM_MAT_CONSUM_RECODS`，运行按钮响应包兼容由 `207` 处理。
2. 在真实前端列表中选中 marker 行后执行工作单元设置、质量判定和不良数等动作，不能只靠直接调用接口绕过前端；提前放料确认、下推备料需求、工序开始/结束、活动开始/结束、简易活动报工、无产出明细 `stop` 最小路径、产出明细报工、投入明细报工、制造请检和检验单生成已通过，但不应替代质量判定和库存链路。
3. 如果要验收新建生产任务，需要先定位真实创建入口；当前 `makeTaskList` 是待办/查询页，不是新建入口。
4. 捕获 method、URL、payload、response，并保留浏览器 network 证据。
5. 直接查询 PostgreSQL 表确认字段变化，例如 `WOM_PRODUCE_TASKS.TASK_RUN_STATE`、`WOM_TASK_PROCESSES.PROCESS_RUN_STATE`、`WOM_TASK_ACTIVES.RUN_STATE/IS_FINISH`、`WOM_PROC_REPORTS` 新增记录。

## Backlog

| ID | 问题 | 处理方向 |
| --- | --- | --- |
| PROD-ACTION-001 | 动作页已能真实渲染，`startProcess/endProcess`、`startActive/endActive`、产出明细、投入明细、简易活动报工、制造请检和检验单生成已通过；但工作单元设置、质量判定和不良数路径仍缺可执行前置条件 | 准备 marker 质量数据，并补工作单元/工厂对象主数据后，重新通过真实前端执行动作并查 PostgreSQL |
| PROD-ACTION-002 | `makeTaskList` 和 `prepareMakeTaskList` 动作按钮已恢复；状态流转 start/hold/restart/无明细 stop、产出明细报工、投入明细报工、提前放料确认、下推备料需求、工序开始/结束、活动开始/结束、简易活动报工、制造请检和检验单生成已验收 | 补齐可测试业务数据和状态前置条件，再验收质量判定和库存回写等写接口 |
| PROD-ACTION-003 | `makeTaskGraphList` 当前路由 404 | 判断该图形化入口是否应上架菜单；如果应上架，需要补页面路由和 runtime view |
| PROD-ACTION-004 | QCS 目标表已从 `createManuInspect` 和 `checkoutBill/generate` 追到 `qcs_inspects/qcs_inspect_stds/qcs_inspect_coms` 并通过落库验收；质量判定和不良数目标表仍未完成 | 继续进入 QCS 源包和 PostgreSQL 元数据排查剩余质量动作 |
| PROD-ACTION-005 | `updateTaskState` 依赖的 `WOM_WAIT_PUT_RECORDS` 原先在 PostgreSQL 缺表，且 hold/restart 曾出现任务状态和待办状态不同步 | 已用 `079-wom-wait-put-records-table.sql` 补 `public.wom_wait_put_records`，并用 `080-wom-wait-record-state-sync-trigger.sql` 同步 workOrder 待办状态；start/hold/restart 已通过真实前端和 PostgreSQL 复验 |
| PROD-ACTION-006 | 历史 `remainMaterialView/save` 后只有 `wom_putin_details`，缺少活动执行和消耗记录 | 已由 WOM 服务层按明细补建缺失执行记录、保留已有记录并复用既有消耗生成器关闭；`...CLOSED_11` 真实页面/API/PostgreSQL 精确证明明细/执行/消耗 `1/1/1`，验收脚本已把缺失或重复设为硬失败 |
| PROD-ACTION-007 | 旧 public `produceTaskCreated` 已废弃；URL 仅保留明确拒绝兼容行为，不写入 `wom_produce_tasks` | 已按 `NOT_APPLICABLE` 关闭。用 `make acceptance-wom-public-produce-task-created-retirement` 复验 `HTTP 200/code=400/已废弃`、marker `0 -> 0` 和替代页面 0 错误；未来若要新增外部创建 API，必须新建认证、幂等、事务化 v2 契约，不能复活旧实现 |
