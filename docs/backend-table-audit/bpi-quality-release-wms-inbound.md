# BPI 质量放行与 WMS 完工入库落表验收

## 结论

代码提交 `22ddadebd20ed9ed5d7efd19c3c0ed49967b9c90` 已在干净 PostgreSQL 16.13 数据库
`ft_mes_bpi_v23_quality_wms_final` 从 Flyway V1 完整迁移到 V23，并通过 4/4 个真实
HTTP/Protobuf/PostgreSQL 验收用例。以下链路已形成可执行的本地软件闭环：

```text
CLOSED_RAW
  -> QCS required inspection snapshot
  -> WAIT_QA
  -> ACCEPTED: RELEASED + one deterministic WMS outbox command
  -> durable outbox PUBLISHED
  -> WMS receipt with document_id
  -> INBOUNDED
```

该本地结论是 **`PASS_LOCAL_POSTGRES_CONTRACTS_NOT_TARGET_ACTIVATED`**，不是生产上线结论。
`BPI_PHASE2_INTEGRATION_ENABLED`、`BPI_WMS_OUTBOX_ENABLED`、`bpi.qcs-link` 和
`bpi.wms-link` 默认均关闭；Phase 1 仍为 `SHADOW_ONLY`。

目标增量已在 `10.11.100.17` 以
**`PASS_TARGET_V23_CONTROLLED_CONTRACTS_EXTERNAL_QCS_WMS_BLOCKED`** 完成：PostgreSQL 15.18
expand-only 升级至 V23，Java 8 release 路由和批次质量/库存产品页已部署；真实页面/API 在
service/adapter 重启前后通过。4 个唯一 marker 在目标库执行同一测试类 4/4，随后独立复查 12 张表
残留均为 0。该结果仍未连接真实 QCS/WMS broker 或创建真实业务单据，所以外部系统联合验收继续
为 `BLOCKED`。机器记录为 `metadata/bpi-quality-release-wms-target-acceptance.json`。

## 表与所有权

| 表 | 所有者 | 写入时机 | 关键约束 |
|---|---|---|---|
| `bpi.bpi_quality_gates` | BPI service | 收到 QCS revisioned snapshot | tenant+batch 唯一，external gate/event 唯一，revision > 0 |
| `bpi.bpi_quality_links` | BPI service | 替换同一 gate 的检验快照 | tenant+gate+inspection code 唯一；required/final/disposition 明确 |
| `bpi.bpi_batch_instances` | BPI service | 质量门和 WMS 回执状态迁移 | revision 乐观并发；`CLOSED_RAW -> WAIT_QA -> RELEASED/REJECTED -> INBOUNDED` |
| `bpi.bpi_outbox_events` | BPI service | 非影子批次 RELEASED 的同一事务 | `BATCH_INSTANCE/WMS_COMPLETION_INBOUND_COMMAND` 专用过滤；确定性 event id |
| `bpi.bpi_wms_inbound_links` | BPI service | 与 WMS command 同事务，回执后更新 | tenant+batch、tenant+idempotency、tenant+receipt event 唯一；tenant 复合 FK 指向 outbox |
| `bpi.bpi_inbox_events` | BPI service | QCS/WMS 入站事件处理前 | source+idempotency/event/checksum 精确重放；冲突 payload 返回 409 |
| `bpi.bpi_batch_state_events` | BPI service | 每次合法状态迁移 | batch+revision 唯一，append-only |
| `bpi.bpi_audit_events` | BPI service | 每次质量/WMS 业务动作 | before/after revision、actor、trace、detail 完整留痕 |

V23 还创建数据库触发器 `trg_bpi_reject_shadow_wms_command`。即使绕过 Java 服务直接插入
outbox，只要目标批次为 `is_shadow=true`，PostgreSQL 也会拒绝 WMS 完工入库命令。

## 业务动作与 SQL 验收

| 业务动作 | 前端入口 | API endpoint | 后端入口 | 目标表 | 验收 SQL | 实际结果 | 状态 |
|---|---|---|---|---|---|---|---|
| QCS 待定快照 | `/bpi/#/batches` 批次质量与库存区 | `POST /internal/bpi/v1/qcs-quality-gates` | `InternalPhase2IntegrationController -> BatchReleaseService.applyQualityGate` | quality gate/link、batch、state、audit、inbox | 按 tenant+batch 查 state/revision/gate/link | `CLOSED_RAW/r1 -> WAIT_QA/r2`，1 个 required PENDING | PASS_TARGET_CONTROLLED |
| QCS 精确重放 | 同上 | 同上 | inbox checksum | inbox、quality gate/link | 按 source/event/idempotency 计数 | 所有业务表仍各 1 份 | PASS_TARGET_CONTROLLED |
| QCS 冲突重放 | 同上 | 同上 | inbox checksum | 无新增 | 比较冲突前后计数 | 返回 409，事务回滚 | PASS_TARGET_CONTROLLED |
| QCS 全部必检合格 | 同上 | 同上 | quality evaluator + transactional outbox | quality gate/link、batch、state、audit、outbox、WMS link | 解码 outbox Protobuf 并查状态 | `WAIT_QA -> RELEASED`，恰好 1 个 WMS command | PASS_TARGET_CONTROLLED |
| WMS 提前回执 | `/bpi/#/batches` 批次质量与库存区 | `POST /internal/bpi/v1/wms-inbound-receipts` | `BatchReleaseService.applyWmsReceipt` | 无新增 | 查 outbox status 与 inbox 计数 | outbox 未 PUBLISHED 时 409，inbox 写入回滚 | PASS_TARGET_CONTROLLED |
| WMS 入库成功 | 同上 | 同上 | WMS receipt transaction | WMS link、batch、state、audit、inbox | 查 document_id/state/wms_status/revision | accepted receipt + document id 后 `INBOUNDED/r4` | PASS_TARGET_CONTROLLED |
| 必检不合格 | `/bpi/#/batches` 批次质量与库存区 | QCS endpoint | quality evaluator | quality gate/link、batch、state、audit | 查 batch state 和 outbox 计数 | `REJECTED/r3`，WMS outbox 0 | PASS_TARGET_CONTROLLED |
| 影子批次阻断 | 无生产入口 | QCS endpoint + DB trigger | Java invariant + PostgreSQL trigger | batch，禁止 outbox | 直接尝试插入 WMS outbox | Java 不发命令，数据库直插也拒绝 | PASS_TARGET_CONTROLLED |
| WMS 未知/拒绝状态 | `/bpi/#/batches` 批次质量与库存区 | WMS receipt endpoint | receipt validation + transaction | WMS link、batch、state、audit、inbox | 查错误响应及拒绝前后 projection | 未知枚举值 `99` 返回 422 且事务无残留；合法 rejected 回执使 batch 保持 `RELEASED/r4`、`wmsStatus=FAILED` | PASS_TARGET_CONTROLLED |
| 真实 QCS/WMS 联合链 | 目标环境页面 | Kafka topics + 目标系统 API | adapters/broker/target systems | 目标 QCS/WMS 单据 | 需要目标 marker 和单据直查 | 未激活、未执行 | BLOCKED |

## 复验 SQL

```sql
SELECT state, revision, quality_gate, wms_status
FROM bpi.bpi_batch_instances
WHERE tenant_id = :tenant_marker AND id = :batch_id;

SELECT external_gate_id, external_revision, state, release_quantity,
       quantity_unit, material_code
FROM bpi.bpi_quality_gates
WHERE tenant_id = :tenant_marker AND batch_id = :batch_id;

SELECT inspection_code, inspection_record_id, required, disposition, final_result
FROM bpi.bpi_quality_links
WHERE tenant_id = :tenant_marker AND batch_id = :batch_id
ORDER BY inspection_code;

SELECT id, status, aggregate_type, event_type, topic, attempt_count, published_at
FROM bpi.bpi_outbox_events
WHERE tenant_id = :tenant_marker
  AND aggregate_type = 'BATCH_INSTANCE'
  AND event_type = 'WMS_COMPLETION_INBOUND_COMMAND';

SELECT status, command_event_id, receipt_event_id, document_id, error_code, revision
FROM bpi.bpi_wms_inbound_links
WHERE tenant_id = :tenant_marker AND batch_id = :batch_id;

SELECT revision, action, from_state, to_state, trace_id
FROM bpi.bpi_batch_state_events
WHERE tenant_id = :tenant_marker AND batch_id = :batch_id
ORDER BY revision;
```

最终目标库证据：PostgreSQL 15.18/Flyway V23；全局 `bpi.auto-confirm=false`、
`bpi.qcs-link=false`、`bpi.shadow-only=true`、`bpi.wms-link=false`；4 个精确
`ADP_E2E_20260720_BPI_QW_*` marker 在 batch、quality、WMS、outbox、inbox、audit、state、
idempotency、feature、rule 和 topology 共 12 张表中的剩余行均为 0。既有真实 batch
`52427282-eb88-5645-a246-b76fe6547038` 仍保持 `CLOSED_RAW/r2/SHADOW`。

## 已知边界

- 批次详情质量/库存区与目标 release 读取链已部署并通过；真实外部状态仍为空态，不能用模拟六态冒充目标单据。
- Kafka producer/consumer 配置和 DLQ 已接线但默认关闭，本轮没有冒充真实 broker 联合验收。
- 真实 WMS 必须返回 durable `document_id` 才能进入 `INBOUNDED`；HTTP/Kafka 成功本身不算入库。
- WMS receipt 状态仅接受合同内的 `ACCEPTED/REJECTED`；未知 Protobuf 枚举不会降级成业务拒绝。
- 旧本地测试库的 V19 checksum 与当前仓库不一致，本轮未执行 `flyway repair`，而是使用新数据库从零迁移。
- 激活前必须完成真实 QCS/WMS adapter、topic ACL、消费积压、重放、超时、查单和补偿演练。
