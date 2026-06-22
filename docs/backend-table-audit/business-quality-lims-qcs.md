# 质量域 LIMS/QCS 落表排查

## 结论

当前恢复包覆盖的是 QC/LIMS/QCS 和 Qualify 相关能力，不是完整 QA/QMS 套件。已经有 QCS 制造检验列表页面 smoke 证据，也已经确认 PostgreSQL 默认测试库存在 QCS 主表；本轮进一步确认并修复了 QCS 请检/报告明细表、报告生成前置和报告数据接口兼容缺口，并通过真实前端/同款前端会话完成 WOM 制造请检生成 QCS 请检单、QCS 审核生效后生成检验报告的 PostgreSQL 落库验收。

`createManuInspect` 已经是 PASS：marker `ADP_E2E_20260616050127_WOM_MANU_INSPECT` 通过真实前端触发 `POST /msService/WOM/produceTask/produceTask/createManuInspect`，并查库确认 `qcs_inspects`、`qcs_inspect_stds` 以及 WOM 状态字段落库。最新复验 marker `ADP_E2E_20260616084434_WOM_MANU_INSPECT` 继续从 QCS 列表执行 bulkSubmit 到生效，两次接口均返回 `200`，并查库确认 `qcs_inspect_reports/qcs_report_coms` 以及报告编辑待办已生成，因此报告生成落库为 PASS。当前剩余阻断是报告编辑页 `layoutJson` 仍返回 `500/服务器异常`，根因定位到 `runtime_extra_view.view_json` 缺失；检验结果录入、判定回写、不良数登记和完工入库仍必须在报告编辑页恢复后逐项真实验收。

## 已验证

| 项目 | 证据 | 状态 |
| --- | --- | --- |
| QCS 制造检验列表入口 | `/msService/QCS/inspect/inspect/manuInspectList` 真实浏览器页面 smoke PASS | PASS |
| QCS 单据类型/业务类型基础字典 | `067-qcs-table-types-postgres.sql` 已补 `qcs_table_types` 并回读 `manu/purch/other/quality`；`qcs_busi_types` 回读 `product/productRetest/addItem/material/...` | PASS |
| QCS 请检/报告明细表缺口 | `107-qcs-inspect-detail-tables.sql` 已在测试机执行，`information_schema.columns` 回读 7 张表存在 | PASS |
| WOM 生产请检生成 QCS 请检单 | `/tmp/adp-wom-manu-inspect-persistence-acceptance-rerun.json`；marker `ADP_E2E_20260616050127_WOM_MANU_INSPECT`；`qcs_inspects.id=755756625425664`、`qcs_inspect_stds` 计数 `1` | PASS |
| QCS 请检单提交到生效并生成报告 | `/tmp/adp-wom-manu-inspect-persistence-acceptance-after-qcs-report-fix.json`；marker `ADP_E2E_20260616084434_WOM_MANU_INSPECT`；两次 `POST /msService/QCS/inspect/inspect/bulkSubmit` 均 `200`，`qcs_inspects.status=99/refable=true/check_state=QCS_checkState/reporting`，`qcs_inspect_reports.id=755811962860800`，`qcs_report_coms` 计数 `1`，报告待办 `755811963663616` | PASS |
| QCS 检验报告编辑页 | `editStates` 和 `data/755811962860800` 均为 `200/操作成功`；`layoutJson?viewCode=QCS_5.0.0.0_inspectReport_manuInspReportEdit` 仍为 `500/服务器异常`；`runtime_extra_view.view_json` 为空 | FAIL |

## 源码定位与本轮补齐

本轮按源码继续追 QCS 报告生成链路，确认 `QCSInspectServiceImpl.bulkSubmit -> afterSubmitInspect -> QCSInspectReportServiceImpl.createQcsReportByInspet` 在生效且 `needLab=false` 时才会生成报告。报告生成依赖的前置和当前处理如下：

| 前置 | 源码证据 | 仓库处理 | 状态 |
| --- | --- | --- | --- |
| 自动报告配置 | `createQcsReportByInspet` 读取 `QCS.autoReport`，且必须包含当前表类型 `manu` | `deploy/docker/postgres/init/117-qcs-report-generation-support.sql` 补 `QCS.autoReport=manuCheck`、`QCS.autoReportStaff=currentUser`、`QCS.reportShowIndexRange=qualityStd` | 已远端应用并通过报告生成复验 |
| 质量标准报表项 | `getReportComList` 在 `reportShowIndexRange=qualityStd` 时查询 `LIMSBasicStdVerCom where valid=1 and isReport=1 and stdVerId.id=?` | `adp-wom-manu-inspect-persistence-acceptance.js` 新增 marker `limsba_std_ver_coms` 报表项种子和查库断言 | 已重新生成 marker 并复验 PASS |
| 检测分项实体表 | `QCSReportCom` 生成时读取 `stdVerCom.getComId().getValueKind()/getLimitType()/getMaxValue()/getMinValue()` | `117-qcs-report-generation-support.sql` 按 LIMSBasic `TestComponent` 模型补 `limsba_test_components` 表及索引 | 已远端应用并通过报告明细生成复验 |
| 报告工作流配置 | `createQcsReportByInspet` 对 `manu` 查询 `BaseSetWfCustomConfig.code=manuReport` 并启动 `manuInspectReportWorkFlow` | `113-qcs-manu-inspect-jbpm-definition.sql` 补 `wf_task/wf_transition/rbac_flow_permission`，`117` 调整 `manuReport -> manuInspectReportWorkFlow` | 已远端应用并生成报告编辑待办 |
| 报告数据接口兼容 | 报告页 `editStates/data` 依赖监督视图和原生 SQL grade 查询 | `107-qcs-inspect-detail-tables.sql` 将 `qcs_inspect_reports_sv` 改为兼容视图，`118-postgres-legacy-text-bytea-operator.sql` 补 `varchar/text = bytea` legacy operator | 已远端应用，`editStates/data` 复验 200 |
| 报告编辑页布局 JSON | `layoutJson` 需要 `runtime_extra_view.view_json` | 当前只确认 `runtime_extra_view/config/full_config` 存在，`view_json` 为空 | 未修复，页面仍 FAIL |

注意：报告生成已经通过新 marker 重新执行真实前端/API 和 PostgreSQL 查询复验；历史 marker `ADP_E2E_20260616050127_WOM_MANU_INSPECT` 的 `qcs_inspect_reports=0` 仍作为旧失败样本保留，不再代表当前链路。LIMS 日志中的 `syncEntity:2618` 空指针仍会出现，但最新复验事务未回滚、报告已落库，应作为残余风险继续定位。

## 本轮 PostgreSQL 修复

| 表 | 来源模型 | 远端回读 | 用途 |
| --- | --- | --- | --- |
| `qcs_inspect_stds` | QCS `init.xml` `InspectStd` / `QCSInspectStd` | 60 列 | 检验申请质量标准 |
| `qcs_inspect_coms` | QCS `init.xml` `InspectCom` / `QCSInspectCom` | 60 列 | 检验申请指定分项 |
| `qcs_report_coms` | QCS `init.xml` `ReportCom` / `QCSReportCom` | 70 列 | 检验报告明细 |
| `qcs_inspects_di` | QCS `init.xml` `InspectDealInfo` | 16 列 | 检验申请流程处理信息 |
| `qcs_inspect_reports_di` | QCS `init.xml` `InspectReportDealInfo` | 16 列 | 检验报告流程处理信息 |
| `qcs_inspect_releases_di` | QCS `init.xml` `InspectReleaseDealInfo` | 16 列 | 检验放行流程处理信息 |
| `qcs_un_qlf_deals_di` | QCS `init.xml` `UnQlfDealDealInfo` | 16 列 | 不合格品处理流程处理信息 |

远端验证 SQL：

```sql
SELECT table_name, count(*)
FROM information_schema.columns
WHERE table_schema = 'public'
  AND table_name IN (
    'qcs_inspects_di',
    'qcs_inspect_releases_di',
    'qcs_inspect_reports_di',
    'qcs_un_qlf_deals_di',
    'qcs_inspect_stds',
    'qcs_inspect_coms',
    'qcs_report_coms'
  )
GROUP BY table_name
ORDER BY table_name;
```

结果摘要：

```text
qcs_inspect_coms|60
qcs_inspect_releases_di|16
qcs_inspect_reports_di|16
qcs_inspect_stds|60
qcs_inspects_di|16
qcs_report_coms|70
qcs_un_qlf_deals_di|16
```

## 生产请检链路

| 业务动作 | 前端入口 | API | 后端链路 | 目标表 | 当前状态 |
| --- | --- | --- | --- | --- | --- |
| WOM 制造任务请检 | WOM 制造任务列表“请检”按钮 | `POST /msService/WOM/produceTask/produceTask/createManuInspect` | `WOMProduceTaskController.createManuInspect -> WOMProduceTaskServiceImpl.createManuInspect/changeTaskStateAndInitiateCheck -> WOMQCSServiceImpl.createInspect -> QCSInspectController.createInspect -> QCSInspectServiceImpl.createInspect` | `qcs_inspects`、`qcs_inspect_stds`、`qcs_inspect_coms`、`wom_produce_tasks`、`wom_wait_put_records`、`wom_produce_task_exelog`、`baseset_batch_infos` | PASS |
| QCS 制造检验申请提交到生效并生成报告 | QCS 制造检验列表待办行 | `POST /msService/QCS/inspect/inspect/bulkSubmit` | `QCSInspectController.bulkSubmit -> QCSInspectServiceImpl.bulkSubmit -> TaskServiceImpl.take -> QCSInspectReportServiceImpl.autoReport/createQcsReportByInspet -> TaskServiceImpl.createPendings` | `qcs_inspects`、`qcs_inspects_di`、`wfm_task_pending`、`qcs_inspect_reports`、`qcs_report_coms`、`limsba_std_ver_coms`、`limsba_std_ver_grades`、`limsba_spec_limits` | PASS |
| QCS 检验报告编辑页打开 | 生成的报告待办 URL | `GET /msService/baseService/view/layoutJson?...QCS_5.0.0.0_inspectReport_manuInspReportEdit...` | `ViewController.getLayoutJson -> ViewServiceFoundationImpl.findLayoutJsonByViewCode -> runtime_extra_view.view_json` | `runtime_extra_view`、`runtime_view`、`runtime_data_grid` | FAIL |

当前阻断/失败原因：

- 制造请检前置数据已经补齐并验收通过，不能再把 `createManuInspect` 当作当前 blocker。
- 当前恢复的 QCS 最小工作流已能推进到 `qcs_inspects.status=99` 并生成检验报告；bulkSubmit 第二步仍会在 LIMS 日志中触发 `NullPointerException at QCSInspectServiceImpl.syncEntity(QCSInspectServiceImpl.java:2618)`，但最新复验未导致事务回滚。
- 历史 marker 质量标准版本 `8990058608766751` 的 `limsba_std_ver_coms` 报告分项计数为 `0`，因此旧失败不再用于当前结论；新 marker `8990059947475305` 已种报表项、等级和规格限制。
- 报告编辑页 `layoutJson` 仍失败：`runtime_extra_view.code=QCS_5.0.0.0_inspectReport_manuInspReportEdit` 存在，但 `view_json` 为空。下一步应把 QCS edit view 的 XML `config/full_config` 转为运行时 JSON。

## 未验证

| 能力 | 状态 | 下一步 |
| --- | --- | --- |
| QCS 制造检验申请真实落库 | PASS | 已通过 `/tmp/adp-wom-manu-inspect-persistence-acceptance-rerun.json` 证明 `qcs_inspects/qcs_inspect_stds` 与 WOM 状态字段落库 |
| QCS 检验报告生成 | PASS | 已用 `ADP_E2E_20260616084434_WOM_MANU_INSPECT` 复验 `qcs_inspect_reports/qcs_report_coms/wfm_task_pending` 落库 |
| QCS 检验报告编辑页 | FAIL | 补齐 `QCS_5.0.0.0_inspectReport_manuInspReportEdit` 的 `runtime_extra_view.view_json`，复验 `layoutJson` 200 后再继续 |
| 检验结果录入/判定 | BLOCKED | 报告编辑页 layoutJson 失败前不应继续推断结果录入；修复页面后，从 QCS 页面录入结果并查询 `qcs_inspect_reports/qcs_report_coms` 与生产状态字段 |
| 不合格品处理 | BLOCKED | 基于不合格检验报告执行处理，查询 `qcs_un_qlf_deals` 和相关 DI 表 |
| 完整 QA/QMS 管理能力 | NOT_APPLICABLE | 当前证据只覆盖 QC/LIMS/QCS/Qualify，不代表 QA 文件、审计、CAPA、变更、偏差等完整质量管理套件 |

## 后续验收要求

1. 每个写动作必须使用 `ADP_E2E_YYYYMMDD_HHMMSS_QCS_*` marker。
2. 前端必须捕获 method、URL、payload、response status 和 response body。
3. 后端必须追到 Controller、Service、DAO/Hibernate 实体和目标表。
4. PostgreSQL 必须直接查询 `qcs_inspects`、`qcs_inspect_stds`、`qcs_inspect_coms`、`qcs_inspects_di`、`qcs_inspect_reports`、`qcs_report_coms` 以及 WOM 状态回写表。
5. 接口返回成功但 QCS 或 WOM 表未变化时，必须标记 `FAIL`。
