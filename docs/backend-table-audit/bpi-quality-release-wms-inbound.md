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

目标增量已在 `10.11.100.17` 先完成 V23 受控合同基线，再以实现提交
`1ce3cb996ff81556763283a5401f7c19554099c2` 完成
**`PASS_TARGET_CONTROLLED_QCS_EVENT_KAFKA_WMS_POSTGRES_BROWSER_CLEANED`**。受控认证 QCS Protobuf
marker 进入 BPI 后，经事务 outbox、目标三 broker Kafka 和 query-first WMS adapter，在同一目标栈
`material-wms` 中创建真实完工入库单、明细、库存事务和批次库存；durable receipt 使批次进入
`INBOUNDED/r4`。相同 QCS 重放及强制 Kafka command 重放均无重复增行，真实页面五个 API 为 200、
浏览器错误为 0。两个全链 marker 在取证后定向清理，BPI 和 material 表残留均为 0。

该结果仍未连接外部 QCS 实例主动事件，也没有执行外部 ERP/WMS 业务冲销、宕机恢复和补偿演练，
所以生产联合验收继续为 `BLOCKED`；Phase 2 全部开关最终关闭且 allowlist/route 恢复 deny-all。
机器记录为 `metadata/bpi-quality-release-wms-target-acceptance.json`。

V25 已在本地干净 PostgreSQL 16.13 增加完工入库冲销工作流，并通过 10/10 既有质量/WMS 回归、
其中 2 个冲销专项用例，以及 19/19 浏览器和 14/14 simulator 测试。该增量覆盖四眼申请/审批、
独立红字命令、发布前回执阻断、accepted/rejected 回执、失败后重试和原蓝单不可变，结论为
`PASS_LOCAL_BROWSER_API_POSTGRES_PROTOCOL_ONLY`。它尚未部署到目标 ADP，也没有调用外部 ERP/WMS，
不能替代上述生产联合验收。机器记录为 `metadata/bpi-wms-inbound-reversal-acceptance.json`。

## 表与所有权

| 表 | 所有者 | 写入时机 | 关键约束 |
|---|---|---|---|
| `bpi.bpi_quality_gates` | BPI service | 收到 QCS revisioned snapshot | tenant+batch 唯一，external gate/event 唯一，revision > 0 |
| `bpi.bpi_quality_links` | BPI service | 替换同一 gate 的检验快照 | tenant+gate+inspection code 唯一；required/final/disposition 明确 |
| `bpi.bpi_batch_instances` | BPI service | 质量门和 WMS 回执状态迁移 | revision 乐观并发；`CLOSED_RAW -> WAIT_QA -> RELEASED/REJECTED -> INBOUNDED` |
| `bpi.bpi_outbox_events` | BPI service | 非影子批次 RELEASED 的同一事务 | `BATCH_INSTANCE/WMS_COMPLETION_INBOUND_COMMAND` 专用过滤；确定性 event id |
| `bpi.bpi_wms_inbound_links` | BPI service | 与 WMS command 同事务，回执后更新 | tenant+batch、tenant+idempotency、tenant+receipt event 唯一；tenant 复合 FK 指向 outbox |
| `bpi.bpi_wms_inbound_reversal_tasks` | BPI service | durable 入库冲销申请、独立审批及红字回执 | 一个批次只允许一个活动任务；申请人与审批人必须不同；冻结原蓝单事实和独立红字 identity |
| `bpi.bpi_inbox_events` | BPI service | QCS/WMS 入站事件处理前 | source+idempotency/event/checksum 精确重放；冲突 payload 返回 409 |
| `bpi.bpi_batch_state_events` | BPI service | 每次合法状态迁移 | batch+revision 唯一，append-only |
| `bpi.bpi_audit_events` | BPI service | 每次质量/WMS 业务动作 | before/after revision、actor、trace、detail 完整留痕 |
| `bpi.bpi_api_idempotency` | BPI service | 冲销 REQUEST/APPROVE 命令进入事务时 | tenant+idempotency key 唯一；checksum 冲突关闭；完成响应可精确重放 |
| `public.wms_stock_documents` | material-wms | query-first 未查到精确幂等单据后创建 | tenant+document type+source system+idempotency key 唯一；BPI API key 保护 |
| `public.wms_stock_document_lines` | material-wms | 完工入库单明细创建 | source system/source line 精确唯一；保留 `quantity_unit` |
| `public.wms_inventory_transactions` | material-wms | 完工入库库存事务 | 事件键唯一；保存 source system、document/line 和数量单位 |
| `public.wms_batch_stocks` | material-wms | 入库后批次库存增量 | tenant+warehouse+location+material+batch 唯一；事务内更新 held/available 数量 |

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
| 受控 QCS -> 内部 WMS 全链 | 目标 `/bpi/#/batches` | QCS ingress + command/receipt topics + material REST | `InternalPhase2IntegrationController -> BatchReleaseService -> outbox publisher -> WmsCommandConsumer -> MaterialWmsClient -> material controller/service/repository` | BPI 8 表 + `wms_stock_documents`、`wms_stock_document_lines`、`wms_inventory_transactions`、`wms_batch_stocks` | 按 tenant/batch/event/idempotency/source system 查 BPI 和 material 两库 | 唯一单据、明细、事务、库存各 1，`12.345 kg`；durable receipt 后 `INBOUNDED/r4` | PASS_TARGET_CONTROLLED_CLEANED |
| QCS 与 Kafka 精确重放 | 同上 | 同一 QCS POST；重置 command consumer offset | inbox checksum + query-first material lookup | 同上 | 重放前后逐表计数和库存数量比较 | QCS 重放仍为 201；offset `1 -> 0 -> 1` 后单据/明细/事务/库存各 1、数量不变 | PASS_TARGET_CONTROLLED_CLEANED |
| V25 冲销申请与同人审批反证 | 本地 `/bpi/#/batches` | `POST /bpi/v1/batches/{batchId}/wms/reversal` | `BatchController -> WmsInboundReversalService -> WmsInboundReversalPostgresRepository` | batch、reversal task、state、audit、API idempotency | 查 task actor/state、batch state/revision、red outbox count | REQUEST 后 batch 保持 `INBOUNDED/r5`、task `PENDING_APPROVAL`、red outbox 0；同人 APPROVE 返回 403 | PASS_LOCAL_POSTGRES |
| V25 独立批准、红字回执与拒绝重试 | 同上 | 同一 public POST；`POST /internal/bpi/v1/wms-inbound-reversal-receipts` | reversal service/repository + internal integration controller | reversal task、red outbox、inbox、batch、state、audit | 对比蓝/红 identity 和业务事实；查 task/batch 终态、重放和第二次 red outbox 计数 | 独立管理员生成唯一红命令；accepted 后 `INBOUND_REVERSED/r7/REVERSED`；rejected 后恢复 `INBOUNDED/r7/REVERSAL_FAILED`，第二次申请/独立批准生成第二条不同红命令并进入 `INBOUND_REVERSING/r9`；原蓝单不变 | PASS_LOCAL_POSTGRES_CLEANED |
| 外部 QCS/ERP-WMS 联合链 | 目标环境页面 | 外部 adapters/API | external QCS/WMS systems | 外部检验与库存单据 | 需要外部 marker、所有权联接、冲销和宕机恢复证据 | 未执行 | BLOCKED |

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

SELECT state, requested_by, decided_by, original_command_event_id,
       original_idempotency_key, original_document_id,
       reversal_command_event_id, reversal_idempotency_key,
       reversal_document_id, error_code, revision
FROM bpi.bpi_wms_inbound_reversal_tasks
WHERE tenant_id = :tenant_marker AND batch_id = :batch_id
ORDER BY requested_at;

SELECT id, status, event_type, topic, payload, published_at
FROM bpi.bpi_outbox_events
WHERE tenant_id = :tenant_marker
  AND aggregate_type = 'BATCH_INSTANCE'
  AND event_type = 'WMS_COMPLETION_INBOUND_REVERSAL_COMMAND';

SELECT revision, action, from_state, to_state, trace_id
FROM bpi.bpi_batch_state_events
WHERE tenant_id = :tenant_marker AND batch_id = :batch_id
ORDER BY revision;

SELECT id, document_no, source_system, idempotency_key, status
FROM public.wms_stock_documents
WHERE tenant_id = :tenant_marker
  AND document_type = 'COMPLETION_INBOUND'
  AND source_system = 'BPI'
  AND idempotency_key = :command_idempotency_key;

SELECT document_id, source_line_id, quantity, quantity_unit
FROM public.wms_stock_document_lines
WHERE tenant_id = :tenant_marker AND source_system = 'BPI';

SELECT event_key, transaction_type, quantity, quantity_unit
FROM public.wms_inventory_transactions
WHERE tenant_id = :tenant_marker AND source_system = 'BPI';

SELECT warehouse_code, location_code, material_code, batch_no,
       held_quantity, available_quantity, quantity_unit
FROM public.wms_batch_stocks
WHERE tenant_id = :tenant_marker AND batch_no = :batch_no;
```

最终目标库证据：PostgreSQL 15.18/Flyway V23；全局 `bpi.auto-confirm=false`、
`bpi.qcs-link=false`、`bpi.shadow-only=true`、`bpi.wms-link=false`，Phase 2 HTTP/Kafka、WMS
outbox/adapter 均为 false，allowlist/route 均为 deny-all。历史 4 个
`ADP_E2E_20260720_BPI_QW_*` 合同 marker 残留为 0；全链 marker
`ADP_E2E_20260720_215500_BPI_WMS` 与 `ADP_E2E_UI_20260720_222600_BPI_WMS` 在 material
document/line/transaction/stock 和 BPI batch/gate/link/WMS/outbox 表中的残留也全部为 0。

## 已知边界

- 批次详情已在受控 marker 上真实显示 `ACCEPTED/INBOUNDED`、material 单据和时间线；这仍不代表
  外部 QCS/ERP-WMS 已联调。
- 六个 Phase 2 topic 已以 3 分区/RF3/minISR2 在目标 broker 建立并完成受控 command/receipt 重放；
  验收后 producer/consumer 和 adapter 均恢复关闭。
- 真实 WMS 必须返回 durable `document_id` 才能进入 `INBOUNDED`；HTTP/Kafka 成功本身不算入库。
- WMS receipt 状态仅接受合同内的 `ACCEPTED/REJECTED`；未知 Protobuf 枚举不会降级成业务拒绝。
- V25 红字回执同样要求红 outbox 已 durable `PUBLISHED`；accepted/rejected 都保留原蓝字 link/document，
  不允许以更新或删除原入库记录的方式伪造冲销。
- 旧本地测试库的 V19 checksum 与当前仓库不一致，本轮未执行 `flyway repair`，而是使用新数据库从零迁移。
- query-first 精确查单和 command 重放已通过；V25 本地冲销软件合同也已通过。激活前仍必须完成外部
  QCS 主动事件、外部 WMS 超时与真实红字单、目标 V25 部署、生产等价重启和业务所有者签字。
