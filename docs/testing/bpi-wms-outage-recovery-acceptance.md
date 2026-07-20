# BPI WMS 停机恢复验收

## 结论

2026-07-21 在唯一测试环境 `10.11.100.17` 完成 `material-wms` 受控停机、真实 Kafka
重试/DLQ、服务恢复、真实批次页面“重新核对原单”、BPI/物料双 PostgreSQL 落库与定向清理。
最终结论为 `PASS_TARGET_OUTAGE_RECOVERY_CLEANED`。

本轮证明的是仓库内目标 `material-wms` 故障恢复合同：第一次投递失败后，系统保留原 command event
和原 WMS 幂等键；服务恢复后由管理员核对原单，再次发布同一业务命令并接收 durable receipt。整个过程
只创建一套完工入库单、明细、库存事务和批次库存，批次最终进入 `INBOUNDED`。

## 真实操作

| 模块 | 页面/路由 | 操作 | API | 前端结果 | 后端结果 | 数据库表 | 状态 | 问题 |
|---|---|---|---|---|---|---|---|---|
| BPI 完工入库故障 | `/bpi/#/batches` | 停止 `material-wms` 后发布完工入库命令 | Kafka `bpi.wms.completion-inbound-command.v1` | 页面操作前置数据可读取；故障阶段不伪造成功 | outbox 为 `PUBLISHED/r3`，首次投递 1 次；命令 DLQ 增加 1，DLT 保留原 event header；物料库单据为 0 | `bpi_outbox_events`、`bpi_wms_inbound_links`；物料四表 | PASS_FAIL_CLOSED | 受控停机，仅覆盖仓库内 `material-wms` |
| BPI 原单核对 | `/bpi/#/batches` | 恢复 `material-wms`，打开 marker 批次详情，点击“重新核对原单”并确认 | `POST /bpi-api/batches/37278a8e-33fd-4993-8876-6df5e9721cad/wms/reconcile` | 登录 200、POST 200；页面最终显示 `ACCEPTED/INBOUNDED` 和 durable 单号；console/page/request/BPI HTTP error 均为 0 | 同一 command event 和 WMS key 被重新发布；delivery attempt 为 2、manual retry 为 1，receipt 消费后 batch 为 `INBOUNDED/r4` | BPI batch/link/outbox/inbox/idempotency/audit；物料 document/line/transaction/stock | PASS_TARGET_OUTAGE_RECOVERY | 外部 ERP/WMS 查单协议未参与 |
| marker 清理 | 不适用 | 恢复 `.env`、Compose 服务和 deny-all 路由，定向删除双库 marker | 双库 cleanup SQL | 正常页面和服务恢复 | BPI/物料 marker 总残留 0；五个 Phase 2/WMS 开关均恢复 false；WMS adapter route 恢复 `_DENY_ALL_` | 上述表及临时 feature flags | PASS_CLEANED | 无 |

## 唯一标识与请求

- marker：`ADP_E2E_20260720193226_WMS_OUTAGE`
- batch：`37278a8e-33fd-4993-8876-6df5e9721cad`
- command event：`f3e9ccc0-5792-4cb3-801f-b6b2b9cef731`
- WMS 幂等键：`ADP_E2E_20260720193226_WMS_OUTAGE|WMS|1`
- 页面请求幂等键：`8e636204-9f73-47c9-ab36-f3c3616bb790`
- 页面请求 revision：`If-Match: 1`
- durable receipt：`817982c9-a6a4-5c44-88d2-58688095b7b1`
- durable document：`CIN-f3e9ccc0-5792-4cb3-801f-b6b2b9cef731-WARE-E2E`

请求链路：

```text
BpiProxyController
  -> BatchController.reconcileWmsInbound
  -> BatchReleaseService.reconcileWmsInbound
  -> BatchReleasePostgresRepository
  -> WMS transactional outbox
  -> Kafka command
  -> WmsCommandKafkaListener / WmsCommandProcessor
  -> MaterialWmsController / MaterialInventoryService / MaterialWmsRepository
  -> Kafka durable receipt
  -> WmsReceiptKafkaListener / BatchReleaseService.applyWmsReceipt
```

## Kafka 证据

| topic | 验收前 offset 总量 | 验收后 offset 总量 | 增量 | 结论 |
|---|---:|---:|---:|---|
| completion inbound command | 2 | 4 | 2 | 首次投递和恢复后原命令重排各 1 次 |
| command DLQ | 0 | 1 | 1 | 停机阶段真实失败进入 DLQ |
| completion inbound receipt | 3 | 4 | 1 | 恢复后仅产生 1 个 durable receipt |

验收结束时 WMS command consumer 与 receipt consumer lag 均为 0。

## PostgreSQL 证据

BPI 库 `ft_mes_bpi`、schema `bpi`：

| 字段 | 结果 |
|---|---|
| batch state / revision / WMS status | `INBOUNDED / 4 / INBOUNDED` |
| WMS link status / revision | `ACCEPTED / 3` |
| outbox status / revision | `PUBLISHED / 6` |
| manual retry / total attempt | `1 / 2` |
| reconciliation / accepted audit | `1 / 1` |
| receipt inbox / API idempotency / original outbox | `1 / 1 / 1` |

物料 PostgreSQL：

| 表 | marker 行数/结果 |
|---|---|
| `wms_stock_documents` | 1 个 `POSTED/QUALIFIED` 完工入库单 |
| `wms_stock_document_lines` | 1 行，`12.345 kg` |
| `wms_inventory_transactions` | 1 行 |
| `wms_batch_stocks` | 1 行，on-hand/available 均为 `12.345`，hold 为 `0` |

可复验 SQL：

- `deploy/docker/scripts/bpi-wms-outage-recovery-verification.sql`
- `deploy/docker/scripts/bpi-wms-outage-recovery-material-verification.sql`
- `deploy/docker/scripts/bpi-wms-outage-recovery-cleanup.sql`
- `deploy/docker/scripts/bpi-wms-outage-recovery-material-cleanup.sql`

## 安全编排与复验

执行入口要求显式确认目标停机：

```bash
BPI_CONFIRM_TARGET_OUTAGE=YES \
ADP_PASSWORD='<test-password>' \
make rehearse-bpi-wms-outage-recovery-target
```

编排器在变更前验证五个集成开关关闭、route 为 `_DENY_ALL_`、活跃 WMS outbox 为 0、冲突
feature flag 为 0、两个 consumer lag 为 0；`finally` 无条件恢复 `.env` 和服务，并执行双库 marker 清理。

本次结束状态：

- `environmentRestored=true`
- `servicesRestored=true`
- `residualRows=0`
- `cleanup.errors=[]`
- Phase 2、Protobuf ingress、Kafka、WMS outbox、WMS adapter 均为 `false`
- WMS adapter route 为 `_DENY_ALL_`

## 边界

本轮关闭了**目标环境内部 material-wms 宕机后的原命令恢复**缺口。外部 QCS 主动事件、外部
ERP/WMS 查单协议、响应丢失、拒绝、冲销、补偿和生产等价负载仍未验收，因此 `G-021` 继续保持
`PARTIAL`，所有生产写回开关继续默认关闭。

机器证据：`metadata/bpi-wms-outage-recovery-target-acceptance.json`；截图：
`metadata/bpi-wms-outage-recovery-target.png`。
