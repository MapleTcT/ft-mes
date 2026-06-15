# 生产模块落表排查

## 结论

当前生产模块已经有真实浏览器页面 smoke 证据，但还没有完成动作级落库验收。后续专门线程应以 [生产模块功能测试用例矩阵](../production-module-functional-test-cases.md) 为入口，逐条补页面/API/Controller/Service/Mapper/表/字段链路。

## 已验证

| 能力 | 证据 | 状态 |
| --- | --- | --- |
| WOM 制造任务列表打开 | `/msService/WOM/produceTask/produceTask/makeTaskList` 浏览器 smoke | PASS |
| WOM 退料 PDA 列表打开 | `/msService/WOM/batchMaterial/batMaterilPart/baRetireMentPDAList` 浏览器 smoke | PASS |
| RM 批量配方列表打开 | `/msService/RM/formula/formula/batchFormulaList` 浏览器 smoke | PASS |
| craftGraph 工艺基础信息打开 | `/msService/craftGraph/basicInfo/basicInfo/basicInfoList` 浏览器 smoke | PASS |
| craftGraph 按钮配置打开 | `/msService/craftGraph/operationButton/buttonConfig/buttonConfList` 浏览器 smoke | PASS |
| WTS 作业许可列表打开 | `/msService/WTS/workPermit/workPermit/workPermitList` 浏览器 smoke | PASS |
| workAppointment 作业计划列表打开 | `/msService/workAppointment/workPlan/workTicketPlan/workPlanList` 浏览器 smoke | PASS |
| QCS 制造检验列表打开 | `/msService/QCS/inspect/inspect/manuInspectList` 浏览器 smoke | PASS |

证据文件：

- API/layout smoke：`/tmp/adp-business-module-smoke-20260615184508.json`
- 页面 smoke：`/tmp/adp-business-page-smoke-final-20260615204003/business-page-smoke-results.json`
- WOM 制造任务动作发现：`/tmp/adp-production-action-discovery-final8-20260615204438/production-action-discovery.json`
- WOM 生产动作候选页探测：`/tmp/adp-production-action-discovery-candidates-20260615193213/production-action-discovery.json`
- WOM runtime JSON 修复后复验：`/tmp/adp-production-action-discovery-final8-20260615204438/production-action-discovery.json`
- WOM layoutJson 直连复验：5 个动作 viewCode 均 `200`，pageType 为 `EDIT/VIEW`
- WOM 生产动作源地图：[`business-production-action-map.md`](business-production-action-map.md)
- WOM 生产动作机器记录：`metadata/production-module-source-action-map.json`
- 运行时视图来源：`deploy/docker/postgres/init/065-business-view-runtime-json.sql` 中 `WOM_1.0.0_produceTask_makeTaskList` 的 `layoutDatagrid.buttons` 为 `[]`

## 未验证

| 业务动作 | 当前状态 | 后续验收要求 |
| --- | --- | --- |
| 新建生产工单或制造任务 | BLOCKED | 当前 WOM 制造任务列表只发现“查询 / 仅查待办 / 清空”，未发现新增入口；需定位真正创建页面或按钮权限来源 |
| 工单下发、暂停、恢复、关闭 | BLOCKED | 源事件和 `updateTaskState` 后端入口已定位，但当前列表按钮未暴露；恢复按钮后查询 `WOM_PRODUCE_TASKS`、`WOM_TASK_PROCESSES`、`WOM_TASK_ACTIVES` 字段变化 |
| 制造指令单生成或维护 | NOT_RUN | 先定位准确页面和目标表 |
| 工序、人员、班组派工 | NOT_RUN | 准备人员/班组数据，确认派工表 |
| 退料申请提交、审核或回写库存 | NOT_RUN | 查询退料单据、库存和状态字段 |
| 批量配方导入、编辑、删除 | NOT_RUN | 使用真实模板和 marker 行查库 |
| 工艺路线新增、编辑、删除 | NOT_RUN | 查询工艺主表和明细表 |
| 作业许可新建、提交、审批、关闭 | NOT_RUN | 查询许可表、流程表和待办状态 |
| 作业计划新建、调整、取消 | NOT_RUN | 查询计划表和状态字段 |
| 制造检验结果录入和生产状态回写 | NOT_RUN | 查询 QCS 表和生产状态字段 |
| 批次/物料/工单追溯 | BLOCKED | 当前未定位完整追溯页面和表链路 |
| 工序报工 | BLOCKED | 源事件已定位 `addOutputByOutPutDetails`、`endEasyActive` 和 `WOM_PROC_REPORTS`；`makeTaskView/makeTaskBatchView/easyTaskOperateView` 的 `layoutJson` 已修复为 200，但真实浏览器仍 React #130，未渲染出动作表单或按钮 |
| 完工入库或库存回写 | BLOCKED | 需要仓储/库存模块包和表结构 |

## 初始页面/API 入口

| 领域 | 页面 | 已知入口 | 备注 |
| --- | --- | --- | --- |
| 工单/任务 | 制造任务列表 | `GET /msService/WOM/produceTask/produceTask/makeTaskList`；`POST /msService/WOM/produceTask/produceTask/makeTaskList-pending` | 已捕获列表查询；运行时视图按钮为空；读模型识别到 `WOM_PRODUCE_TASKS`，待办筛选关联 `wfm_task_pending`；当前页面未发现创建入口 |
| 工单/任务 | 备料列表 | `GET /msService/WOM/produceTask/produceTask/prepareMakeTaskList`；`POST /msService/WOM/produceTask/produceTask/prepareMakeTaskList-query` | 已捕获列表查询；源码存在 `generatePrepareNeed` 写动作，但当前页面只暴露查询/清空 |
| 工单/任务 | 状态流转/报工动作视图 | `makeTaskEdit`、`makeTaskSubmitView`、`makeTaskView`、`makeTaskBatchView`、`easyTaskOperateView` | `layoutJson` 已返回 200，并带 `DataGridCode` 与真实 `produceTask` 字段键；真实浏览器仍 React #130，当前不能做 marker 落库 |
| 退料 | 退料 PDA | `GET /msService/WOM/batchMaterial/batMaterilPart/baRetireMentPDAList` | 需要确认提交/审核接口 |
| 配方 | 批量配方 | `GET /msService/RM/formula/formula/batchFormulaList` | 静态 patch 暴露 `downloadXls`、`importMainXls`、`delete` |
| 工艺 | 基础信息 | `GET /msService/craftGraph/basicInfo/basicInfo/basicInfoList` | 需要确认主表/明细表 |
| 作业许可 | 作业许可 | `GET /msService/WTS/workPermit/workPermit/workPermitList` | 需要确认流程表/待办表 |
| 作业计划 | 作业计划 | `GET /msService/workAppointment/workPlan/workTicketPlan/workPlanList` | 需要确认计划表和状态字段 |
| 质量联动 | 制造检验 | `GET /msService/QCS/inspect/inspect/manuInspectList` | 需要确认 QCS 与生产状态联动 |

## PostgreSQL backlog 规则

排查过程中每发现一个缺口，必须进入下面任一路径：

- 幂等 SQL：`deploy/docker/postgres/init/0xx-*.sql`
- 模块报告 backlog：本文件新增 `## Backlog` 明细
- 全局 Oracle backlog：`docs/oracle-migration-backlog.md`

禁止用清库重建绕过缺表、缺列、类型不兼容或 Oracle 方言残留。

## Backlog

| ID | 类型 | 问题 | 当前处理 |
| --- | --- | --- | --- |
| PROD-DB-001 | 缺入口 | WOM 制造任务当前列表页未发现新增入口；运行时视图 `buttons` 为空 | 读模型已定位 `WOM_PRODUCE_TASKS` 和 `wfm_task_pending`；继续排查菜单、按钮权限、原始创建视图或真实创建页面 |
| PROD-DB-002 | runtime render 阻断 | `makeTaskEdit`、`makeTaskSubmitView`、`makeTaskView`、`makeTaskBatchView`、`easyTaskOperateView` 的 `layoutJson` 已返回 200，但生成组件树仍触发 React #130 | 从原始 runtime/static/template 中恢复真实 edit/view component metadata，避免继续用 list-style datagrid JSON 冒充动作表单 |
| PROD-DB-005 | 缺按钮/权限 | WOM 源码存在 `updateTaskState`、`addOutputByOutPutDetails`、`generatePrepareNeed`、`createManuInspect` 等动作，但当前页面按钮未暴露 | 对齐旧 BAP 按钮元数据、RBAC 按钮权限和 runtime view 生成逻辑 |
| PROD-DB-006 | 缺映射 | QCS 检验单目标表未追到底 | 从 `createManuInspect`、`checkoutBill/generate` 继续追 QCS 源包和 PostgreSQL 元数据 |
| PROD-DB-003 | 缺映射 | 完工入库或库存回写链路未定位 | 等待仓储/库存模块包 |
| PROD-DB-004 | 缺映射 | 追溯页面和目标表未定位 | 等待菜单和运行包排查 |
