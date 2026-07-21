# Material WMS 完工入库冲销软件持久化验收

## 结论

2026-07-21 在本地 Java 8、H2 PostgreSQL compatibility mode 环境执行：

```bash
make material-wms-test
```

`material-wms` 共 `12/12 PASS`，其中集成测试 `11/11`、服务测试 `1/1`。
状态为 `PASS_LOCAL_PERSISTENCE_CONTRACT_ONLY`：内部库存服务已经具备追加式完工入库冲销、
精确幂等、原单关联和库存不足整事务回滚。后续 BPI 四眼审批、红单 outbox/inbox、真实
PostgreSQL 16.13 和产品页已经在独立验收中闭合，但本合同本身仍没有调用外部 ERP/WMS，不能据此
开启生产集成。后续证据见 `docs/testing/bpi-wms-inbound-reversal-acceptance.md`。

## 不变量

- 原 `COMPLETION_INBOUND` 蓝字单、明细和库存流水永久保留，不执行覆盖或删除。
- 冲销创建独立 `COMPLETION_INBOUND_REVERSAL` 红字单，通过
  `reversal_of_document_id` 关联原单；同一原单最多一个红字单。
- 红单必须使用新的 BPI command event 和 idempotency key，且物料、批次、生产批次、仓库、
  库位、数量、单位与原单完全一致。
- 红单扣减可用量与现存量，任何维度库存不足都会回滚红单、明细、流水和原单状态更新。
- 原单仅在红单完整记账后从 `POSTED` 变为 `REVERSED`；红单保持 `POSTED`。

## 验收矩阵

| 场景 | API/动作 | 数据库预期 | 实际结果 | 状态 |
|---|---|---|---|---|
| 缺少内部密钥 | POST 冲销但不传 `X-BPI-WMS-Key` | 不生成红单，不改变库存 | `code=403`，原单与库存不变 | PASS |
| 业务事实不一致 | 数量 `10 -> 9` | 写库前拒绝 | `code=409`，红单数为 0 | PASS |
| 正常追加式冲销 | 原入库 10 后提交等事实红单 | 2 单、2 明细、2 流水，库存归零 | 原单 `REVERSED`，红单 `POSTED`，流水 `-10/-10/0` | PASS |
| 精确查询 | 按 BPI idempotency key 查红单 | 返回红单、原单、明细和流水 | 原单号、事件、单位和关系一致 | PASS |
| 同命令重放 | 原 event/key/payload 再提交 | 不增加任何行或库存变化 | `idempotent=true` | PASS |
| 同 key 事实冲突 | 原 key 改数量后重放 | 失败关闭 | `code=409`，计数不变 | PASS |
| 同原单新 key 再冲销 | 第二个 event/key | 唯一约束前稳定拒绝 | `code=409`，仍只有一个红单 | PASS |
| 库存已被消耗 | 入库 10、领料 3、再全额冲销 10 | 红单整事务回滚，原单仍有效 | 仅蓝单和领料单；库存保持 7，原单 `POSTED` | PASS |

## HTTP 合同

- 创建：`POST /material/wms/completion-inbound-reversals`
- 精确查询：`GET /material/wms/completion-inbound-reversals/by-idempotency`
- 隔离头：`X-Tenant-Id`、`X-BPI-WMS-Key`
- 请求：`sourceSystem=BPI`、独立 `idempotencyKey`、独立 `srcID`、
  `originalDocumentNo`、`redBlue=red` 和一条与原单完全一致的库存明细
- 查询结果：`document`、`originalDocument`、`lines`、`transactions`

## 验收 SQL

```sql
SELECT id, document_no, document_type, status, idempotency_key,
       reversal_of_document_id
FROM wms_stock_documents
WHERE tenant_id = :tenant_id
ORDER BY id;

SELECT transaction_type, on_hand_delta, available_delta, hold_delta
FROM wms_inventory_transactions
WHERE tenant_id = :tenant_id
ORDER BY id;

SELECT on_hand_quantity, available_quantity, hold_quantity
FROM wms_batch_stocks
WHERE tenant_id = :tenant_id;
```

## 未关闭门槛

1. V25、四个红单 topic/ACL、前端静态包和 adapter 尚未部署到目标 ADP 环境。
2. 目标正式双身份、真实 Kafka 和 BPI/material 双库 marker 清理尚未联跑。
3. 外部 ERP/WMS 实例没有参与，`G-021` 继续保持 `PARTIAL`，所有 Phase 2/WMS 开关保持关闭。
