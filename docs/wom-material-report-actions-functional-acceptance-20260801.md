# WOM 退料、用料、报工、尾料与完工入库操作验收

## 验收范围

- 环境：`http://10.11.100.17:18080`
- 账号：`admin`
- 数据库：PostgreSQL
- 目标：恢复用户反馈的 12 个页面操作入口，并通过真实页面、接口、详情、导出和数据库快照复验。
- 机器记录：`metadata/wom-material-report-actions-acceptance-20260801.json`

历史记录和明细页按审计语义只恢复查看、双击详情和导出，不为了填满工具栏增加新增或删除；
只有原包确实定义维护动作的“尾料记录”恢复新增、修改、删除。

## 页面验收

| 模块 | 页面/路由 | 恢复操作 | 页面查询 | 详情/表单 | 状态与边界 |
|---|---|---|---|---|---|
| 备料退料 | `/msService/WOM/rejectMaterilal/rejectMaterial/prePareRejectList` | 新增退料申请、查看详情、双击、导出 | HTTP 200，4/4 行渲染 | 新增、按钮详情、双击详情均 HTTP 200 | PASS |
| 配料退料 | `/msService/WOM/rejectMaterilal/rejectMaterial/batchRejectList` | 新增退料申请、查看详情、双击、导出 | 点击查询后 HTTP 200，2/2 行渲染 | 新增、按钮详情、双击详情均 HTTP 200 | PASS；原页面首次不自动查询 |
| 车间物料退料 | `/msService/WOM/rejectMaterilal/rejectMaterial/materiaRejectList` | 新增退料申请、查看详情、双击、导出 | HTTP 200，3/3 行渲染 | 新增、按钮详情、双击详情均 HTTP 200 | PASS |
| 退料记录 | `/msService/WOM/rejectMaterilal/rejctMatalPart/batchRejectPrtList` | 查看退料单、双击、导出 | HTTP 200，3/3 行渲染 | 按来源退料类型打开真实退料单，按钮和双击均 HTTP 200 | PASS |
| 生产用料单 | `/msService/WOM/putInMaterial/putInMaterial/putinList` | 查看详情、双击、导出 | HTTP 200，0 行 | 无选择提示通过；当前无记录可打开 | PASS；详情 NOT_APPLICABLE |
| 生产用料明细 | `/msService/WOM/putInMaterial/putMateiDetail/putInDetailList` | 查看详情、双击、导出 | HTTP 200，0 行 | 无选择提示通过；当前无记录可打开 | PASS；详情 NOT_APPLICABLE |
| 生产报工单 | `/msService/WOM/outputMaterial/outputMaterial/outputList` | 查看详情、双击、导出 | HTTP 200，0 行 | 无选择提示通过；当前无记录可打开 | PASS；详情 NOT_APPLICABLE |
| 生产报工明细 | `/msService/WOM/outputMaterial/outMateDetail/outputDetailList` | 查看报工单、双击、导出 | HTTP 200，0 行 | 无选择提示通过；当前无记录可打开 | PASS；详情 NOT_APPLICABLE |
| 尾料记录 | `/msService/WOM/remainMaterial/remainMaterial/remainMaterialList` | 新增、修改、删除、导出 | HTTP 200，0 行 | 新增完整表单 HTTP 200；修改/删除无记录可执行 | PASS；修改/删除 BLOCKED_BY_DATA |
| 尾料投料 | `/msService/WOM/procReport/putinDetail/putinDetailList` | 查看指令、双击、导出 | HTTP 200，0 行 | 无选择提示通过；当前无记录可打开 | PASS；详情 NOT_APPLICABLE |
| 尾料产出 | `/msService/WOM/procReport/outputDetail/outputDetailList` | 查看指令、双击、导出 | HTTP 200，0 行 | 无选择提示通过；当前无记录可打开 | PASS；详情 NOT_APPLICABLE |
| 完工入库台账 | `/msService/material/wms` | 查看详情、双击、导出当前结果 | `GET /material/wms/completion-inbounds` HTTP 200，1 行 | 入库明细和库存流水均显示；CSV 346 bytes | PASS |

## 修复内容

1. 从 WOM 模块 `META-INF/bap/module.xml` 恢复 11 个列表页及其 12 个支撑详情/编辑视图，
   由 `256-wom-material-report-actions-runtime.sql` 幂等写入运行时元数据。
2. 为三类退料申请恢复新增、查看和双击详情；退料记录根据来源退料类型跳到正确单据。
3. 为用料、报工及尾料投入/产出台账恢复查看或查看指令，并保留原生导出。
4. 为尾料记录恢复原模块已有的新增、修改、删除动作。
5. 完工入库台账增加显式查看详情、双击详情和 CSV 导出，导出内容进行公式注入转义。
6. 不覆盖旧 SupDataGrid 自己的身份字段，避免宽表双击监听失效。

## PostgreSQL 与浏览器证据

运行时元数据复验 SQL 摘要：

```sql
WITH targets(code) AS (VALUES /* 256 迁移中的 23 个 WOM view code */)
SELECT
  (SELECT count(*) FROM targets),
  (SELECT count(*) FROM runtime_extra_view r JOIN targets t ON t.code = r.code
   WHERE r.view_json IS NOT NULL),
  (SELECT count(*) FROM ec_extra_view e JOIN targets t ON t.code = e.code
   WHERE e.view_json IS NOT NULL);
```

结果：`23|23|23`。

整轮浏览器结果：console error `0`、page error `0`、request failure `0`、HTTP 4xx/5xx `0`。
只读验收前后以下业务表行数完全一致：

| 表 | 验收前 | 验收后 |
|---|---:|---:|
| `wom_reject_materials` | 13 | 13 |
| `wom_rejct_matal_parts` | 5 | 5 |
| `wom_put_in_materials` | 0 | 0 |
| `wom_put_matei_details` | 0 | 0 |
| `wom_output_materials` | 0 | 0 |
| `wom_output_details` | 34 | 34 |
| `wom_remain_materials` | 0 | 0 |
| `wms_stock_documents` | 13 | 13 |
| `wms_stock_document_lines` | 13 | 13 |
| `wms_inventory_transactions` | 26 | 26 |

本轮只打开新增空表单，没有保存、删除或修改业务记录，因此业务落库状态为
`NOT_APPLICABLE`。生产用料、生产报工和尾料投入/产出当前没有可操作业务行，后续建立
受控生产样例后仍需补做详情和写入级验收，不能由本轮页面 PASS 推断为业务闭环完成。
