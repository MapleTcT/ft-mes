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
| 业务模块真实浏览器页面 smoke | `53/53` PASS | `/tmp/adp-business-page-smoke-make-202606151849/business-page-smoke-results.json` |

生产相关已打开页面包括：

| 模块 | 页面/路由 | 当前状态 | 注意 |
| --- | --- | --- | --- |
| WOM | `/msService/WOM/produceTask/produceTask/makeTaskList` | 页面 smoke PASS | 只证明制造任务列表入口可打开 |
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
| PROD-001 | 工单/任务 | 制造任务列表打开和基础查询 | WOM 制造任务列表 | 否 | PASS | 已有页面/API smoke 证据 |
| PROD-002 | 工单/任务 | 新建生产工单或制造任务 | WOM 制造任务列表 | 是 | NOT_RUN | 需要业务前置数据、目标表和创建接口确认 |
| PROD-003 | 状态流转 | 工单下发、暂停、恢复、关闭 | WOM 制造任务列表 | 是 | NOT_RUN | 需要状态枚举、按钮权限和 Mapper 链路 |
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
| PROD-021 | 报工 | 工序报工、产量、不良数登记 | 待定位 | 是 | BLOCKED | 当前运行包未定位明确报工页面和目标表 |
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

1. 先补 `PROD-002` 工单/制造任务创建，因为它是生产主流程的上游。
2. 再补 `PROD-003` 状态流转和 `PROD-021` 报工，确认生产执行闭环。
3. 同步补 `PROD-009` 配方导入、`PROD-015` 作业许可审批、`PROD-019` 质量判定回写。
4. 最后补追溯和完工入库，需要业务模块包和表级地图更完整后推进。
