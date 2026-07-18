# BPI Kafka 候选落库验收

## 结论

2026-07-13 在 Java 17、Spring Boot 3.4.7、Spring Kafka 嵌入式真实 broker 和临时隔离 PostgreSQL
16.13 上，`@KafkaListener` 已真实经过 `CandidateKafkaRecordProcessor -> CandidateEventMapper ->
CandidateIngestionService -> BpiPostgresRepository`。同一记录连续发布两次后，inbox 和 candidate 各保持
1 行；坏 Protobuf 实际进入相同 partition 的 candidate DLQ。

本段记录 2026-07-13 的单 broker 验收快照；当时测试机仍受磁盘门禁阻断。后续目标环境已完成
三 broker、Flink、远端 PostgreSQL、DLQ、真实候选页面和延迟候选恰好一次落库，当前证据见
`metadata/bpi-rule-retirement-acceptance.json`。本报告中的本地断言仍保留为底层回归证据。

## 消费与失败语义

| 场景 | 行为 | offset / DLQ | 状态 |
|---|---|---|---|
| 合法 candidate | 事务写 `bpi_inbox_events`、`bpi_batch_candidates` 后 ACK | `MANUAL_IMMEDIATE` | PASS |
| 相同记录重放 | checksum、event ID、candidate key 一致，返回原候选 | ACK，表行数保持 1/1 | PASS |
| 坏 Protobuf、错 topic/key/header、越过 allowlist | 作为永久坏消息拒绝 | 不重试，发 candidate DLQ | PASS（单元 + broker） |
| PostgreSQL/网络临时错误或版本引用未就绪 | 不 ACK | 最多 4 次，耗尽后发 DLQ | PASS（配置/单元） |
| DLQ 发布失败 | error handler 继续失败 | 不提交 recovered offset | 配置 PASS，故障注入待实机 |

## Marker 与 SQL

测试 tenant 使用 `ADP_E2E_BPI_<UUID>`，Kafka 证据和订单分别使用
`ADP_E2E_BPI_KAFKA_FLOW_<UUID>`、`ADP_E2E_BPI_KAFKA_ORDER_<UUID>`。

```sql
SELECT count(*)
  FROM bpi.bpi_inbox_events
 WHERE tenant_id = :tenant_marker;

SELECT count(*), min(evidence->0->>'source') AS source
  FROM bpi.bpi_batch_candidates
 WHERE tenant_id = :tenant_marker;
```

两次投递后的结果摘要为 inbox `1`、candidate `1`、source `bpi-stream-engine`。测试通过后只删除该动态
tenant marker，不操作其他 schema、容器或数据。

## 运行配置

- `BPI_CANDIDATE_KAFKA_ENABLED=false`：生产默认关闭。
- tenant、plant、line allowlist 默认均为 `_DENY_ALL_`；启用前必须显式替换。
- consumer 使用 `read_committed`、关闭 auto commit、禁止自动建 topic。
- 主 topic 与 DLQ 必须拥有相同 partition 数；测试编排当前均为 6 partition、RF=3、min ISR=2。
- 主 topic 与 DLQ 名称必须不同，二者保留期均为 30 天。
- 远端实机结论必须引用后续目标验收记录，不能只凭本报告的单 broker 测试提升状态。
