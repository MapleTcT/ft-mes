# WOM/QCS 独立不良数量实现与落库验收

生成时间：2026-07-17 22:10（Asia/Shanghai）

测试环境：`http://10.11.100.17:18080`，Compose 项目 `adp-mes-newbase`

数据库：PostgreSQL

## 结论

原恢复包确实没有独立不良数量字段、入口、接口或数值表；这一历史审计结论没有被改写。当前仓库通过可维护源码模块 `backend/source-modules/wom-quality-reporting`、迁移 `191-wom-quality-quantity-reporting.sql` 和 Material WMS 质量分配扩展补齐了该产品能力。

`PROD-019`、`PROD-021` 已通过真实 WOM/QCS 页面、API、PostgreSQL marker、幂等重试和冲销演练，状态改为 `PASS`。

## 产品规则

1. 每条不良数量必须绑定确切的 `wom_produce_tasks.id` 和 `wom_mat_outpt_records.id`。
2. 报工总数由 WOM 产出记录提供，客户端不能重定义。
3. `合格数量 = 报工数量 - 不良数量`，不良数量必须大于 0 且不超过报工数量。
4. `requestId + requestHash` 保证相同请求幂等，并拒绝同 ID 不同载荷。
5. 登记和冲销均保留不可变事件，不通过删除历史记录回滚。
6. QCS 通过 `qcs_inspects.source_id -> wom_produce_tasks.id` 打开同一任务上下文。
7. 质检合格回调只释放合格数量；不良数量继续冻结。
8. 冲销恢复 WMS 分配，且在没有其他不良分配时恢复整行可用数量。

## 页面与 API

| 入口 | 实际行为 |
|---|---|
| WOM 制造任务列表 `#btn-badQuantityReport` | 选择任务后打开 `/msService/WOM/quality-quantity/page?taskId=...` |
| QCS 产品检验报告编辑 `#btn-badQuantityReportQcs` | 按检验单打开 `/msService/WOM/quality-quantity/page?inspectId=...` |
| `POST /msService/WOM/quality-quantity/reports` | 登记数量并同步 WMS 分配 |
| `POST /msService/WOM/quality-quantity/reports/{id}/retry` | 重试失败的 WMS 同步 |
| `POST /msService/WOM/quality-quantity/reports/{id}/reverse` | 乐观锁冲销并恢复 WMS 分配 |
| `POST /msService/WOM/quality-quantity/reports/{id}/link-quality` | 关联后续 QCS 报告或处置记录 |

## PostgreSQL 映射

| 表/字段 | 责任 |
|---|---|
| `wom_quality_quantity_reports` | 当前登记状态、10/2/8 数量、QCS 关联、WMS 同步状态、版本和冲销信息 |
| `wom_quality_quantity_events` | 登记、同步、冲销等不可变 WOM 事件 |
| `wms_quality_allocations` | 以 WOM 产出行为键的合格/不良库存分配 |
| `wms_quality_allocation_events` | 分配应用与冲销事件 |
| `wms_stock_document_lines.reported_quantity/good_quantity/bad_quantity` | 完工入库行的数量拆分 |
| `wms_batch_stocks` | `on_hand_quantity/available_quantity/hold_quantity` 最终库存状态 |
| `wms_inventory_transactions` | 质检释放、冻结和冲销库存流水 |

## marker 验收

证据：`metadata/wom-quality-quantity-persistence-acceptance.json`

marker：`ADP_E2E_20260717141017_WOM_BAD_QTY`

| 阶段 | PostgreSQL/WMS 实际结果 |
|---|---|
| 登记后 | `reports=1`、`reportEvents=2`、`allocationStatus=ACTIVE`、报工 10 / 合格 8 / 不良 2 |
| 入库后 | `qualityStatus=PENDING`、`onHand=10`、`available=0`、`hold=10` |
| 质检后 | `qualityStatus=PARTIAL`、`available=8`、`hold=2` |
| 同请求重试后 | 报告、分配及事件计数不增加 |
| 冲销后 | 报告/分配均 `REVERSED`、库存行 `QUALIFIED`、`available=10`、`hold=0` |
| 清理后 | WOM 任务、数量报告和 WMS 单据 marker 计数均为 0 |

浏览器同时验证 WOM/QCS 两个旧页面均显示 `不良数量`，操作页和 QCS 上下文返回 `200`，无 console error、page error、request failure 或 4xx/5xx 响应。

## 边界

本轮验收覆盖数量登记、QCS 上下文、WMS 可用/冻结数量、幂等、失败重试、冲销与清理。实物报废、返工工单或成本结转属于后续处置流程，不能从“不良数量已登记”直接推导为已完成。
