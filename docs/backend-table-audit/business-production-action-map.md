# 生产模块源动作地图

## 结论

本文件把 WOM 生产执行相关动作从旧源包、当前运行时页面和后端源码三个来源做了交叉对齐。当前不是动作验收通过状态：能真实打开的生产页面主要还是查询页，核心写动作被 runtime layout 和按钮缺失阻断，不能伪造为已落库。

机器可读记录见 `metadata/production-module-source-action-map.json`。

## 证据来源

| 来源 | 证据 |
| --- | --- |
| 旧源包 | `/Users/zhangchu/Documents/ADP/mes-modules-source-repo/modules/wom/WOM_6.1.3.4` |
| 前端事件 | `service/src/main/resources/custom/WOM/produceTask/produceTask/**/eventJs/customEvent.js` |
| 模型表 | `service/src/main/resources/META-INF/init/init.xml` |
| 后端入口 | `WOMProduceTaskController`、`WOMProduceTaskServiceImpl` |
| 真实浏览器探测 | `/tmp/adp-production-action-discovery-final8-20260615204438/production-action-discovery.json` |
| runtime JSON / pageType 直连复验 | 5 个动作 viewCode 均 `layoutJson` 200，pageType 为 `EDIT/VIEW`，但真实浏览器仍 React #130 |

## 真实页面探测

| 页面/路由 | 前端结果 | API / network | 状态 |
| --- | --- | --- | --- |
| `/msService/WOM/produceTask/produceTask/makeTaskList` | 页面 200；按钮只有“查询 / 仅查待办 / 清空” | `POST /msService/WOM/produceTask/produceTask/makeTaskList-pending` 200 | PASS_READ_ONLY |
| `/msService/WOM/produceTask/produceTask/prepareMakeTaskList` | 页面 200；按钮只有“查询 / 清空” | `POST /msService/WOM/produceTask/produceTask/prepareMakeTaskList-query` 200 | PASS_READ_ONLY |
| `/msService/WOM/produceTask/produceTask/makeTaskEdit` | 外层页面 200；`layoutJson` 200；真实浏览器 React #130 | `layoutJson?viewCode=WOM_1.0.0_produceTask_makeTaskEdit&isEs5=true` 200，但无表单、按钮或写请求 | FAIL_RUNTIME_LAYOUT |
| `/msService/WOM/produceTask/produceTask/makeTaskSubmitView` | 外层页面 200；`layoutJson` 200；真实浏览器 React #130 | `layoutJson?viewCode=WOM_1.0.0_produceTask_makeTaskSubmitView&isEs5=true` 200，但无表单、按钮或写请求 | FAIL_RUNTIME_LAYOUT |
| `/msService/WOM/produceTask/produceTask/makeTaskView` | 外层页面 200；`layoutJson` 200；真实浏览器 React #130 | `layoutJson?viewCode=WOM_1.0.0_produceTask_makeTaskView&isEs5=true` 200；pageType 热补丁为 `VIEW` 后仍 React #130 | FAIL_RUNTIME_LAYOUT |
| `/msService/WOM/produceTask/produceTask/makeTaskBatchView` | 外层页面 200；`layoutJson` 200；真实浏览器 React #130 | `layoutJson?viewCode=WOM_1.0.0_produceTask_makeTaskBatchView&isEs5=true` 200，但无表单、按钮或写请求 | FAIL_RUNTIME_LAYOUT |
| `/msService/WOM/produceTask/produceTask/easyTaskOperateView` | 外层页面 200；`layoutJson` 200；真实浏览器 React #130 | `layoutJson?viewCode=WOM_1.0.0_produceTask_easyTaskOperateView&isEs5=true` 200，但无表单、按钮或写请求 | FAIL_RUNTIME_LAYOUT |
| `/msService/WOM/produceTask/produceTask/makeTaskGraphList` | 页面 404 | document 404 | FAIL_NOT_FOUND |

## 动作级地图

| 动作 | 前端源事件 | API endpoint | 后端入口 | 目标表 | 当前状态 |
| --- | --- | --- | --- | --- | --- |
| 制造任务待办列表查询 | `makeTaskList` 查询 | `POST /msService/WOM/produceTask/produceTask/makeTaskList-pending` | runtime list query | `WOM_PRODUCE_TASKS`, `wfm_task_pending` | PASS_READ_ONLY |
| 备料候选列表查询 | `prepareMakeTaskList` 查询 | `POST /msService/WOM/produceTask/produceTask/prepareMakeTaskList-query` | runtime list query | `WOM_PRODUCE_TASKS` | PASS_READ_ONLY |
| 生成备料需求 | `prePareNeed(event)` | `POST /msService/WOM/produceTask/produceTask/generatePrepareNeed` | `WOMProduceTaskController.generatePrepareNeed -> WOMProduceTaskServiceImpl.generatePrepareNeed` | `WOM_PRODUCE_TASKS`, `WOM_TASK_MATERIALS` | BLOCKED |
| 制造任务开始/暂停/恢复 | `startTaskEvent / pauseTaskEvent / recoveryTaskEvent` | `POST /msService/WOM/produceTask/produceTask/updateTaskState` | `WOMProduceTaskController.updateTaskState -> WOMProduceTaskServiceImpl.updateTaskState` | `WOM_PRODUCE_TASKS`, `WOM_TASK_PROCESSES`, `WOM_TASK_ACTIVES` | BLOCKED |
| 制造任务完工报工 | `stopTaskEvent(event)` | `POST /msService/WOM/produceTask/produceTask/addOutputByOutPutDetails` | `WOMProduceTaskController.addOutputByOutPutDetails -> WOMProduceTaskServiceImpl.addOutputByOutPutDetails` | `WOM_PROC_REPORTS`, `WOM_PRODUCE_TASKS`, `WOM_TASK_ACTIVES`, `WOM_TASK_PROCESSES` | BLOCKED |
| 提前放料确认 | `earlyPutInFunc(event)` | `GET /msService/WOM/produceTask/produceTask/setAdvanceTrue/{orderId}` | `WOMProduceTaskController.setAdvanceTrue` | `WOM_PRODUCE_TASKS` | BLOCKED |
| 制造请检下推 | `manuInspect(event)` | `POST /msService/WOM/produceTask/produceTask/createManuInspect` | `WOMProduceTaskController.createManuInspect -> WOMProduceTaskServiceImpl.createManuInspect` | `WOM_PRODUCE_TASKS`, QCS 检验表 | BLOCKED |
| 工序开始/结束 | `startProcessEvent / endProcessEvent` | `/msService/WOM/produceTask/taskProcess/start/{id}`、`/end/{id}` | WOM taskProcess controller/service | `WOM_TASK_PROCESSES` | BLOCKED |
| 工序工作单元设置 | `setUnitEvent(event)` | `/msService/WOM/produceTask/taskProcess/processUnitEdit?id={processId}` + `submitFormData('save')` | runtime edit form save | `WOM_TASK_PROCESSES` | BLOCKED |
| 活动开始/结束 | `startActive(event) / endActiveEvent(event)` | `GET /msService/WOM/produceTask/produceTask/startActive`、`/endActive` | `WOMProduceTaskController.startActive/endActive -> WOMProduceTaskServiceImpl.startActive/endActive` | `WOM_TASK_ACTIVES`, `WOM_TASK_PROCESSES`, `WOM_PRODUCE_TASKS` | BLOCKED |
| 简易活动报工 | `reportEvent(event)` | `POST /msService/WOM/produceTask/produceTask/endEasyActive/{id}` | `WOMProduceTaskController.endEasyActive -> WOMProduceTaskServiceImpl.endEasyActive` | `WOM_TASK_ACTIVES`, `WOM_PROC_REPORTS` | BLOCKED |
| 检验单生成 | `generateCheckoutBillFunc(event)` | `GET /msService/WOM/produceTask/produceTask/checkoutBill/generate/{activeId}` | WOM/QCS checkout generation | `WOM_TASK_ACTIVES`, QCS 检验单表 | BLOCKED |

## 落库验收前置条件

这些动作从 `BLOCKED` 变为 `PASS` 前，必须先完成：

1. 恢复或补齐 `makeTaskEdit`、`makeTaskSubmitView`、`makeTaskView`、`makeTaskBatchView`、`easyTaskOperateView` 的真实 desktop edit/view 组件树，解决 `layoutJson` 200 后仍 React #130 的前端渲染阻断。
2. 恢复 `makeTaskList` 和 `prepareMakeTaskList` 的动作按钮或按钮权限，不能只靠调用接口绕过前端。
3. 准备一条带 marker 的生产任务、工序、活动、物料和检验前置数据。
4. 在真实前端执行动作，捕获 method、URL、payload、response。
5. 直接查询 PostgreSQL 表确认字段变化，例如 `WOM_PRODUCE_TASKS.TASK_RUN_STATE`、`WOM_TASK_PROCESSES.PROCESS_RUN_STATE`、`WOM_TASK_ACTIVES.RUN_STATE/IS_FINISH`、`WOM_PROC_REPORTS` 新增记录。

## Backlog

| ID | 问题 | 处理方向 |
| --- | --- | --- |
| PROD-ACTION-001 | `makeTaskEdit`、`makeTaskSubmitView`、`makeTaskView`、`makeTaskBatchView`、`easyTaskOperateView` 的 `layoutJson` 已返回 200，但生成组件树仍触发 React #130 | 从原始 runtime/static/template 中恢复这些 viewCode 的真实 edit/view component metadata，避免继续用 list-style datagrid JSON 冒充动作表单 |
| PROD-ACTION-002 | `makeTaskList` 当前运行时 `layoutDatagrid.buttons=[]`，源码里的状态/报工/请检动作没有显示 | 对齐旧 `module.xml` 按钮元数据、RBAC 按钮权限和 runtime view 生成逻辑 |
| PROD-ACTION-003 | `makeTaskGraphList` 当前路由 404 | 判断该图形化入口是否应上架菜单；如果应上架，需要补页面路由和 runtime view |
| PROD-ACTION-004 | QCS 目标表还没有从 `createManuInspect` / `checkoutBill/generate` 追到底 | 继续进入 QCS 源包和 PostgreSQL 元数据排查 |
