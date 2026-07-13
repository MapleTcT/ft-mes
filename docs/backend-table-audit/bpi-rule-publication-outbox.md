# BPI 规则发布 Outbox 与 Kafka 验收

状态：`PASS`（本地 PostgreSQL 16.14 + Spring Kafka 嵌入式 broker）

范围：规则发布事务、Protobuf 事件、outbox 领取/重试、Kafka broker 确认、API/操作台发布状态

## 实现边界

- `POST /bpi/v1/rules/{id}/publish` 在同一 PostgreSQL 事务内更新规则版本、写审计、写幂等结果并插入 `bpi_outbox_events`。
- outbox 默认关闭；只有显式设置 `BPI_RULE_PUBLICATION_OUTBOX_ENABLED=true` 才创建 Kafka producer 和定时 dispatcher。
- dispatcher 使用 `FOR UPDATE SKIP LOCKED` 领取事件，并通过 claim token 防止并发 worker 重复完成同一行。
- Kafka producer 使用 `acks=all` 和 idempotence；部署 broker 关闭自动建 topic，并由受控脚本预建 topic。broker 确认后才把 outbox 标记为 `PUBLISHED`。
- 发送失败按有界退避回到 `PENDING`，达到最大次数进入 `FAILED`；过期的 `DISPATCHING` claim 会被恢复。
- 稳定 `event_id`、partition key 和 `outbox_event_id` header 允许消费者去重。发送成功但数据库完成标记前进程崩溃时仍可能重投，因此接收端必须保持幂等。
- API 返回 `publicationStatus`、`publicationAttemptCount`、`publicationPublishedAt` 和 `publicationLastError`。
- `publicationStatus=PUBLISHED` 只表示 Kafka broker 已确认，不表示 Flink Broadcast State 已应用；操作台明确显示这一边界。

## 验收结果

| 场景 | 数据库 / Kafka 结果 | 状态 |
|---|---|---|
| checksum 错误发布 | `422`，outbox 0 行 | PASS |
| 合法规则发布 | 规则 `PUBLISHED/r3` 与 outbox `PENDING/0` 同事务提交 | PASS |
| Protobuf 内容 | tenant、plant、line、locality、rule/topology、条件、绑定和 checksum 可解析 | PASS |
| 幂等发布重放 | 返回原响应，outbox 保持 1 行 | PASS |
| 并发领取 | 第一 worker 领取后，第二 worker返回 0 行 | PASS |
| stale claim 恢复 | 超时 `DISPATCHING` 重新进入领取，attempt 递增 | PASS |
| 失败终态 | 达到 maxAttempts 后进入 `FAILED`，不再被领取 | PASS |
| PostgreSQL 到 Kafka | 唯一 marker 事件到达真实 broker，outbox 变为 `PUBLISHED|1` | PASS |
| 重复轮询 | 已发布行不再发送第二条 marker 事件 | PASS |

## 复验 SQL

```sql
SELECT status, attempt_count, topic, partition_key,
       published_at, last_error, created_at, updated_at
  FROM bpi.bpi_outbox_events
 WHERE tenant_id = :tenant_marker
   AND aggregate_type = 'RULE_VERSION'
   AND aggregate_id = :rule_version_id;

SELECT r.state, r.revision, o.status AS publication_status,
       o.attempt_count, o.published_at, o.last_error
  FROM bpi.bpi_rule_versions r
  LEFT JOIN bpi.bpi_outbox_events o
    ON o.tenant_id = r.tenant_id
   AND o.aggregate_id = r.id
   AND o.event_type = 'BOUNDARY_RULE_PUBLISHED'
 WHERE r.tenant_id = :tenant_marker
   AND r.id = :rule_version_id;
```

联合用例 marker 为 `ADP_E2E_BPI_OUTBOX_<UUID>`。用例退出后只删除该 tenant 的 outbox 行，不操作其他 schema 或数据。

## 证据与剩余门禁

- `BpiRulePostgresAcceptanceTest`：3/3 PASS。
- `RulePublicationOutboxDispatcherTest`：2/2 PASS。
- `BpiRuleOutboxKafkaPostgresAcceptanceTest`：1/1 PASS。
- BPI 模拟 API：6/6 PASS；BPI 浏览器交互：6/6 PASS。
- 目标机 Kafka/Flink/PostgreSQL 联合部署仍受磁盘容量门禁限制，不能用本地 broker 结果替代。
- Flink 已有规则 publication 解码、生命周期和 Broadcast State 测试；流作业向 BPI 回传“已应用”的运行时回执尚未实现，所以 UI 不宣称规则已在线生效。
- `FAILED` 事件当前没有管理端重新入队 API，需在下一里程碑补齐带权限和审计的运维重试动作。
