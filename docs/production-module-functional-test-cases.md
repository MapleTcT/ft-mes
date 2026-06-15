# 生产模块功能测试用例矩阵

## 当前结论

本文件把生产模块从“页面能打开”提升为动作级验收清单。当前已经完成真实浏览器页面 smoke，但生产主流程的新增、编辑、提交、审批、状态流转、导入导出和落库验收仍未完成。

机器可读记录见 `metadata/production-module-test-cases.json`，结构校验命令为：

```bash
make production-testcase-check
```

## 已有真实前端证据

| 证据 | 结果 | 路径 |
| --- | --- | --- |
| 业务模块 API/layout smoke | `53/53` PASS | `/tmp/adp-business-module-smoke-20260615184508.json` |
| 业务模块真实浏览器页面 smoke | `53/53` PASS | `/tmp/adp-business-page-smoke-final-20260615204003/business-page-smoke-results.json` |
| WOM 制造任务动作发现 | 未发现新增入口 | `/tmp/adp-production-action-discovery-202606151930/production-action-discovery.json` |
| WOM 生产动作候选页探测 | 2 个只读入口可达；5 个动作视图最初 `layoutJson` 500；1 个图形入口 404 | `/tmp/adp-production-action-discovery-candidates-20260615193213/production-action-discovery.json` |
| WOM runtime JSON 修复后复验 | 5 个动作视图 `layoutJson` 已返回 200，但真实浏览器仍显示错误页并在 console 报 React #130；未渲染出表单、按钮或写请求；图形入口 404 | `/tmp/adp-production-action-discovery-final8-20260615204438/production-action-discovery.json` |
| WOM layoutJson 直连复验 | `makeTaskEdit/makeTaskSubmitView/makeTaskView/makeTaskBatchView/easyTaskOperateView` 均返回 `200`，pageType 为 `EDIT/VIEW`，含 `DataGridCode` 和真实 `produceTask` 字段 key | 命令输出已记录在本轮验收日志 |
| WOM 生产动作源地图 | 源码入口和目标表已初步对齐，但写动作仍 BLOCKED | `metadata/production-module-source-action-map.json` |

生产相关已打开页面包括：

| 模块 | 页面/路由 | 当前状态 | 注意 |
| --- | --- | --- | --- |
| WOM | `/msService/WOM/produceTask/produceTask/makeTaskList` | 页面 smoke PASS | 只证明制造任务列表入口可打开 |
| WOM | `/msService/WOM/produceTask/produceTask/prepareMakeTaskList` | 页面 smoke PASS | 只证明备料候选列表入口可打开，当前未暴露生成备料按钮 |
| WOM | `/msService/WOM/produceTask/produceTask/makeTaskView` | 页面外层 200 / `layoutJson` 200 / React #130 | 工序开始/结束、活动开始/结束动作 UI 当前不可用 |
| WOM | `/msService/WOM/produceTask/produceTask/makeTaskBatchView` | 页面外层 200 / `layoutJson` 200 / React #130 | 批量工序/活动动作 UI 当前不可用 |
| WOM | `/msService/WOM/produceTask/produceTask/easyTaskOperateView` | 页面外层 200 / `layoutJson` 200 / React #130 | 简易活动报工 UI 当前不可用 |
| WOM | `/msService/WOM/batchMaterial/batMaterilPart/baRetireMentPDAList` | 页面 smoke PASS | 只证明退料 PDA 入口可打开 |
| RM | `/msService/RM/formula/formula/batchFormulaList` | 页面 smoke PASS | 只证明批量配方入口可打开 |
| craftGraph | `/msService/craftGraph/basicInfo/basicInfo/basicInfoList` | 页面 smoke PASS | 只证明工艺基础信息入口可打开 |
| craftGraph | `/msService/craftGraph/operationButton/buttonConfig/buttonConfList` | 页面 smoke PASS | 只证明工艺按钮配置入口可打开 |
| WTS | `/msService/WTS/workPermit/workPermit/workPermitList` | 页面 smoke PASS | 只证明作业许可入口可打开 |
| workAppointment | `/msService/workAppointment/workPlan/workTicketPlan/workPlanList` | 页面 smoke PASS | 只证明作业计划入口可打开 |
| QCS | `/msService/QCS/inspect/inspect/manuInspectList` | 页面 smoke PASS | 只证明制造检验入口可打开 |

## 动作级用例矩阵

| ID | 领域 | 用例 | 前端入口 | 是否落库 | 当前状态 | 阻断/下一步 |
| --- | --- | --- | --- | --- | --- | --- |
| PROD-001 | 工单/任务 | 制造任务列表打开和基础查询 | WOM 制造任务列表 | 否 | PASS | 已捕获列表查询接口 `POST /msService/WOM/produceTask/produceTask/makeTaskList-pending`；读模型识别到 `WOM_PRODUCE_TASKS` 和 `wfm_task_pending` |
| PROD-002 | 工单/任务 | 新建生产工单或制造任务 | WOM 制造任务列表 | 是 | BLOCKED | 当前页面只发现“查询 / 仅查待办 / 清空”，运行时视图 `buttons` 为空，没有新增入口，需要定位真正创建页面或按钮权限来源 |
| PROD-003 | 状态流转 | 工单下发、暂停、恢复、关闭 | WOM 制造任务列表 | 是 | BLOCKED | 源码已定位 `updateTaskState`，但当前运行时列表页按钮未暴露，不能执行 marker 状态流转 |
| PROD-004 | 指令 | 制造指令单生成或维护 | WOM/制造指令入口待确认 | 是 | NOT_RUN | 需要定位实际指令单页面和表 |
| PROD-005 | 派工 | 工序/人员/班组派工 | WOM/TeamInfo 相关入口 | 是 | NOT_RUN | 需要班组、岗位、人员测试数据 |
| PROD-006 | 退料 | 退料 PDA 页面打开 | WOM 退料 PDA 列表 | 否 | PASS | 已有页面/API smoke 证据 |
| PROD-007 | 退料 | 退料申请提交、审核或回写库存 | WOM 退料 PDA 列表 | 是 | NOT_RUN | 需要退料单据表、库存表和状态字段确认 |
| PROD-008 | 配方 | 批量配方列表打开 | RM 批量配方列表 | 否 | PASS | 已有页面/API smoke 证据 |
| PROD-009 | 配方 | 批量配方导入模板下载和 Excel 导入 | RM 批量配方列表 | 是 | NOT_RUN | 需要真实模板、上传接口、目标表确认 |
| PROD-010 | 配方 | 批量配方编辑和删除 | RM 批量配方列表 | 是 | NOT_RUN | 需要创建测试配方 marker 后查库 |
| PROD-011 | 工艺 | 工艺基础信息列表打开 | craftGraph 工艺基础信息 | 否 | PASS | 已有页面/API smoke 证据 |
| PROD-012 | 工艺 | 工艺路线或基础信息新增、编辑、删除 | craftGraph 工艺基础信息 | 是 | NOT_RUN | 需要目标表、字段含义和唯一编码规则 |
| PROD-013 | 工艺 | 工艺按钮配置列表打开 | craftGraph 按钮配置 | 否 | PASS | 已有页面/API smoke 证据 |
| PROD-014 | 作业许可 | 作业许可列表打开 | WTS 作业许可列表 | 否 | PASS | 已有页面/API smoke 证据 |
| PROD-015 | 作业许可 | 作业许可新建、提交、审批、关闭 | WTS 作业许可列表 | 是 | NOT_RUN | 需要流程、审批人、状态表和待办联动确认 |
| PROD-016 | 作业计划 | 作业计划列表打开 | workAppointment 作业计划列表 | 否 | PASS | 已有页面/API smoke 证据 |
| PROD-017 | 作业计划 | 作业计划新建、调整、取消 | workAppointment 作业计划列表 | 是 | NOT_RUN | 需要计划对象、时间字段和状态字段确认 |
| PROD-018 | 质量联动 | 制造检验列表打开 | QCS 制造检验列表 | 否 | PASS | 已有页面/API smoke 证据 |
| PROD-019 | 质量联动 | 制造检验结果录入、判定和回写生产状态 | QCS 制造检验列表 | 是 | NOT_RUN | 需要 QA/QC 边界、检验表和生产状态联动确认 |
| PROD-020 | 追溯 | 批次、物料、工单追溯查询 | 待定位 | 可能 | BLOCKED | 当前运行包未定位完整追溯页面和后端链路 |
| PROD-021 | 报工 | 工序报工、产量、不良数登记 | WOM `makeTaskView/makeTaskBatchView/easyTaskOperateView` | 是 | BLOCKED | 源码已定位 `addOutputByOutPutDetails/endEasyActive/startActive/endActive` 和 `WOM_PROC_REPORTS` 等表；动作视图 `layoutJson` 已修复为 200，但真实浏览器仍 React #130，未渲染表单或按钮 |
| PROD-022 | 入库 | 完工入库或库存回写 | 待定位 | 是 | BLOCKED | 需要仓储/库存模块包、接口和表结构 |
| PROD-023 | 导出 | 生产相关列表导出 | WOM/RM/WTS/QCS 相关列表 | 否 | NOT_RUN | 需要逐页捕获导出接口、文件名和权限结果 |

## 验收执行规则

动作级用例从 `NOT_RUN` 或 `BLOCKED` 变成 `PASS` 前，必须满足：

1. 使用真实浏览器或等效 E2E 操作。
2. 写操作必须带唯一 marker，例如 `ADP_E2E_YYYYMMDD_HHMMSS_PROD_TASK`。
3. 捕获 method、URL、payload、response status 和 response body。
4. 追踪 Controller/Service/Mapper/DAO 或反编译后端入口。
5. 查询 PostgreSQL 目标表证明新增、编辑、删除、状态流转或文件导入确实落库。
6. 如果发现缺表、缺列、类型不兼容或 Oracle 方言残留，必须补幂等 SQL 或记录到模块 backlog。

## 后续优先级

1. 先恢复 WOM 生产动作相关 desktop edit/view component tree：`makeTaskEdit`、`makeTaskSubmitView`、`makeTaskView`、`makeTaskBatchView`、`easyTaskOperateView`。当前 `layoutJson` 已返回 200，但生成组件树仍让前端 React #130，无法渲染动作 UI。
2. 同步恢复 `makeTaskList` 和 `prepareMakeTaskList` 的动作按钮/按钮权限，因为源码存在动作但当前页面只显示查询类按钮。
3. 恢复后优先做 `PROD-003` 状态流转和 `PROD-021` 报工，确认生产执行闭环。
4. 再补 `PROD-009` 配方导入、`PROD-015` 作业许可审批、`PROD-019` 质量判定回写。
4. 最后补追溯和完工入库，需要业务模块包和表级地图更完整后推进。
