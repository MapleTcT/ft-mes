# QCS 质检交互首轮修复与复验

## 验收范围

- 时间：2026-07-29
- 环境：`http://10.11.100.17:18080`
- 数据库：PostgreSQL
- 代码基线：`094c50dbfb725802f11fe9342695e839f45b2ad7` 加本轮工作树
- 原则：通过真实工作台页面操作；只读和防误操作测试不写业务表。

## 结果

| 模块 | 页面/路由 | 操作 | 关键 API | 实际结果 | 落库要求 | 状态 |
|---|---|---|---|---|---|---|
| 产品检验申请 | `/msService/QCS/inspect/inspect/manuInspectList` | 打开列表、检查 13 条记录、点击未选行的“批量提交” | `GET /msService/baseService/view/layoutJson`；列表 pending/query | “打开、关闭、批量提交、删除”四个动作恢复；任务描述表头和未选行提示均为中文；console/page/request/HTTP 错误为 0 | 防误操作不应落库 | PASS |
| 产品检验申请编辑 | `/msService/QCS/inspect/inspect/manuInspectEdit?...` | 双击既有待检记录并重载编辑页 | `GET /msService/QCS/inspect/inspect/data/{id}`；`GET /msService/baseService/view/layoutJson` | 业务类型、请检人/部门、物料、批号、质量标准、检验项目及保存/提交/作废完整显示；错误数组为 0 | 只读打开不应落库 | PASS |
| 产品检验报告 | `/msService/QCS/inspectReport/inspectReport/manuInspReportEdit?...` | 双击既有报告并重载 | 报告 data/layout 请求 | 申请单、物料、批号、质量标准、检验结论和不良数量区域正常显示；错误数组为 0 | 只读打开不应落库 | PASS |
| 产品不合格品处理 | `/msService/QCS/unQlfDeal/unQlfDeal/manuUnQlfDealEdit?...` | 双击既有处理单并重载 | 处理单 data/layout 请求 | 报告单、申请单、物料、批号、处理方式和原因区域正常显示；错误数组为 0 | 只读打开不应落库 | PASS |
| 产品紧急放行 | `/msService/QCS/inspectRelease/inspectRelease/manuInspReleaseView?...` | 双击既有放行单并重载 | 放行单 data/layout 请求 | 申请人、部门、时间、原因和明细区域正常显示；错误数组为 0 | 只读打开不应落库 | PASS |
| 产品检验记录 | `/msService/QCS/testPlan/inspectPlan/manuInspPlanList` | 打开空列表并点击未选行的“设置检测日期” | `GET /msService/baseService/view/layoutJson`；列表 query | 页面显示“设置检测日期、设置已跳批、删除”；未选行提示为“请选择一条记录进行操作！”；错误数组为 0 | 防误操作不应落库 | PASS |

## 修复内容

1. 从 QCS 原始模块恢复 `manuInspectEdit`、`manuInspectView`、`manuInspectList` 和 `manuInspPlanList` 的运行时布局。
2. 申请列表补回原包的打开、关闭、批量提交和删除动作，并修复损坏的 `bulkSubmitManu()` 绑定。
3. 为申请列表和检验记录页补齐跨模块中文资源，避免直接显示 `SupDatagrid.button.error`、`ec.list.taskDescription` 等资源键。
4. 为检验记录页增加精确、禁缓存的 Nginx 多语言资源映射。
5. 原包把“设置检测频率”标记为隐藏，并注明“废弃，去检验基础设置检验频率”；本轮保持该产品规则，没有错误恢复。

## PostgreSQL 证据

运行时布局直接查询：

```sql
SELECT code,
       view_json IS NOT NULL AS populated,
       octet_length(convert_from(lo_get(view_json), 'UTF8')) AS payload_bytes
FROM public.runtime_extra_view
WHERE code IN (
  'QCS_5.0.0.0_inspect_manuInspectEdit',
  'QCS_5.0.0.0_inspect_manuInspectView',
  'QCS_5.0.0.0_inspect_manuInspectList',
  'QCS_5.0.0.0_testPlan_manuInspPlanList'
)
ORDER BY code;
```

结果为四行全部 `populated=true`，payload 分别为：

- `manuInspectEdit`: 115879 bytes
- `manuInspectList`: 45400 bytes
- `manuInspectView`: 75315 bytes
- `manuInspPlanList`: 45594 bytes

同四个编码在 `ec_extra_view` 也均为非空。复验时间窗内 `baseService`
没有新增相关 `NullPointerException` 或 `SQLGrammarException`。

本轮只点击未选行保护和读取既有单据，没有发出业务写请求，因此不应产生
`qcs_inspects`、`qcs_inspect_reports`、`qcs_un_qlf_deals` 等业务表变化。

## 证据与剩余范围

- 截图：
  - `metadata/qcs-interaction-round1-application-list.png`
  - `metadata/qcs-interaction-round1-application-edit.png`
  - `metadata/qcs-interaction-round1-inspection-plan.png`
- 自动检查：
  - `python3 -m unittest deploy/docker/scripts/test_generate_business_view_runtime_sql.py deploy/docker/scripts/test_generate_module_i18n_js.py`
  - `node deploy/docker/scripts/test-qcs-display-bindings.js`
- 产品检验记录当前为 0 条。设置检测日期、设置已跳批和删除的真实写入仍需先建立一条受控检验计划，再做 marker、API 和 PostgreSQL 字段变化验收。
- 申请打开/关闭/批量提交及报告合格/不合格的真实业务落库已有历史专项证据；本轮没有重复改变保留中的真实业务单据。
