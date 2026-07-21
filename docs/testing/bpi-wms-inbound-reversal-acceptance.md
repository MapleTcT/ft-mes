# BPI 完工入库冲销验收

## 结论

2026-07-22 完成 BPI 完工入库冲销的软件闭环，状态为
**`PASS_LOCAL_BROWSER_API_POSTGRES_PROTOCOL_ONLY`**。

本次实现并验证了：`INBOUNDED` 批次申请冲销、不同管理员四眼审批、追加独立 WMS 红单
outbox、红单发布前拒绝回执、accepted/rejected 两种回执、批次状态迁移、原蓝单不可变、
浏览器桌面/移动端交互和 marker 清理。PostgreSQL 16.13 从空库连续应用 Flyway V1-V25，
定向 API/落库测试 `10/10 PASS`，浏览器 E2E `19/19 PASS`，模拟 API `14/14 PASS`。
Java 8 Adapter 的精确 GET/POST 路由、内部 JWT 和并发/重放头透传测试 `32/32 PASS`。

浏览器测试使用确定性模拟器；Java API/DAO/SQL 使用真实 PostgreSQL。外部 ERP/WMS、目标环境
部署、正式 ADP 双身份和真实 Kafka broker 未参与本轮，因此 Phase 2、WMS outbox、adapter 和
scope feature flag 继续默认关闭，`G-021` 保持 `PARTIAL`。

## 状态机

| 动作 | 前置状态 | 结果状态 | 关键不变量 | 状态 |
|---|---|---|---|---|
| 申请冲销 | `INBOUNDED` + 蓝单 `ACCEPTED/PUBLISHED/documentId` | 批次仍为 `INBOUNDED`，任务 `PENDING_APPROVAL` | 只增加批次 revision，不生成红单 | PASS |
| 同人批准 | 任务 `PENDING_APPROVAL` | HTTP 403，状态不变 | `requestedBy != decidedBy` | PASS |
| 独立批准 | `INBOUNDED` + 待审批任务 | `INBOUND_REVERSING` / `PENDING_WMS` | 新 event/key、新 outbox，原蓝单不变 | PASS |
| 发布前回执 | 红单 outbox 非 `PUBLISHED` | HTTP 409，inbox 回滚 | 不能用提前回执伪造完成 | PASS |
| WMS 接受 | `INBOUND_REVERSING` + 红单已发布 | `INBOUND_REVERSED` / `COMPLETED` | 必须有持久化红单号 | PASS |
| WMS 拒绝 | `INBOUND_REVERSING` + 红单已发布 | 回到 `INBOUNDED` / `FAILED` | 保存 errorCode，可新建下一次申请 | PASS |

## 页面验收

页面入口为 `/bpi/#/batches`，使用接口：

- `GET /bpi/v1/batches/{batchId}/release`
- `GET /bpi/v1/batches/{batchId}/wms/reversal`
- `POST /bpi/v1/batches/{batchId}/wms/reversal`

浏览器从真实点击依次完成“申请入库冲销 -> 提交独立审批 -> 独立审批冲销 -> 批准并生成红单”，
两次 POST 的 `approvalMode` 分别为 `REQUEST`、`APPROVE`，`If-Match` 分别为 `7`、`8`，
两次响应 operationId 都为 `commandWmsInboundReversal`。页面终态同时显示：

- 原蓝单 `WMS-IN-ADP-E2E-0001`
- 冲销红单 `WMS-RED-ADP-E2E-0001`
- 申请人与审批人
- 申请依据、红单消息状态、命令 event、幂等键和回执 event
- `INBOUND_REVERSED` / `REVERSED`

桌面区块截图 `/tmp/bpi-console-wms-inbound-reversal.png` 为 `639x397`，SHA-256
`e55c9c085463ced1c9e247ea7f1c7a386392d04701664116644bcb89dc43e77c`；移动区块截图
`/tmp/bpi-console-wms-inbound-reversal-mobile.png` 为 `349x581`，SHA-256
`575906c919f19ff6b0c646ce8a9d0c8511f7fbc10462afac3298fed0568ee606`。截图是临时验收证据，
不作为部署制品提交。390x844 视口无页面级横向溢出，console/page/request failure 均为 0。

## PostgreSQL 落库

后端调用链：

```text
BatchController
  -> WmsInboundReversalService
  -> WmsInboundReversalPostgresRepository / BatchReleasePostgresRepository
  -> bpi_batch_instances / bpi_wms_inbound_reversal_tasks
     / bpi_outbox_events / bpi_inbox_events
     / bpi_batch_state_events / bpi_audit_events
```

定向测试 `approvedFourEyeReversalPersistsOneRedCommandAndOneDurableReceipt` 证明：

- REQUEST 幂等重放不增加任务或事件；同申请人 APPROVE 返回 403。
- 独立管理员批准后 outbox 从 1 个蓝单增加为 2 个，红单 payload 精确复制原批次号、指令号、
  物料、数量、单位和原 document/event/key，并保存不同 requester/approver。
- 红单发布前回执返回 409，inbox 行数保持不变；发布后 accepted 回执及其重放保持单一终态。
- 最终批次为 `INBOUND_REVERSED|r7|ACCEPTED|REVERSED`，任务、inbox、state event、audit
  分别为 `1/3/6/6`，原 WMS link 投影逐字段不变。

定向测试 `rejectedReversalRestoresInboundStateAndAllowsANewRequest` 证明 WMS 拒绝后批次回到
`INBOUNDED|r7|ACCEPTED|REVERSAL_FAILED`，错误码为 `WMS_REVERSAL_PERIOD_CLOSED`，修复外部
原因后可形成第二个 `PENDING_APPROVAL` 任务；再由独立管理员批准后，批次进入
`INBOUND_REVERSING|r9|ACCEPTED|REVERSAL_PENDING`，两次尝试对应两条不同红命令，且活动任务仍
只有一个。该回归同时证明 V15 rule-lifecycle 唯一索引在 V25 被收窄后，不再错误阻断同批次的合法
冲销重试。

验收 SQL：

```sql
SELECT state, revision, quality_gate, wms_status
FROM bpi.bpi_batch_instances
WHERE tenant_id = :tenant_id AND id = :batch_id;

SELECT state, revision, requested_by, decided_by,
       original_document_id, reversal_command_event_id,
       reversal_document_id, error_code
FROM bpi.bpi_wms_inbound_reversal_tasks
WHERE tenant_id = :tenant_id AND batch_id = :batch_id
ORDER BY requested_at;

SELECT id, event_type, topic, status, attempt_count, published_at
FROM bpi.bpi_outbox_events
WHERE tenant_id = :tenant_id AND aggregate_id = :batch_id
ORDER BY created_at;

SELECT action, from_state, to_state, revision
FROM bpi.bpi_batch_state_events
WHERE tenant_id = :tenant_id AND batch_id = :batch_id
ORDER BY revision;
```

每个动态 marker `ADP_E2E_20260720_BPI_QW_<UUID>` 在测试后清理。最终直查：
`batch=0`、`reversal_tasks=0`、`outbox=0`、`inbox=0`、`audit=0`；Flyway 非空版本为
`25/25`。

## 自动化结果

```text
npm run build
  PASS - TypeScript 5.7.2 + Vite 6.4.3

npm run test:e2e
  19 tests, 19 pass, 0 fail

node --test simulation/bpi/bpi-simulation.test.js
  14 tests, 14 pass, 0 fail

BpiQualityReleaseWmsPostgresAcceptanceTest
  10 tests, 10 pass, 0 fail

batch-intelligence-adapter
  32 tests, 32 pass, 0 fail

python3 scripts/verify-bpi-api-contracts.py
  PASS - operations=72, simulated=57, implemented=56, phase2Reads=3,
         phase2Commands=1, topics=20
```

## 未关闭门槛

1. 把 V25、前端静态包和四个红单 topic/ACL 部署到目标测试环境，并用正式 ADP 两个身份重跑。
2. 连接真实外部 ERP/WMS，执行 query-first、响应丢失、4xx/5xx、拒绝、accepted 红单和补偿演练。
3. 使用同一 marker 闭合目标浏览器、Kafka、BPI PostgreSQL、外部 WMS 数据库和清理证据。
4. 在真实物理来源、正式校准和连续 7-14 天影子验收完成前，不申请生产激活。
