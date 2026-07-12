# BPI Protobuf 候选落库验收

## 结论

2026-07-12 在 Java 17、Spring Boot 3.4.7、本地 PostgreSQL 16 数据库 `ft_mes_bpi` 上，
`BatchCandidateV1` Protobuf wire bytes 已通过受信内部入口真实写入候选/inbox，并通过与前端相同的
候选确认 API 创建唯一影子批次。服务测试 **6/6 PASS**，其中 PostgreSQL 验收 2 项、mapper 单测 2 项、
遥测 PostgreSQL 验收 2 项；事件契约另有 14/14 PASS。

本结论证明的是 Flink 输出契约到 BPI/PostgreSQL 的受控 bridge，不声明 Kafka consumer、远端测试环境或
浏览器 E2E 已完成。HTTP Protobuf 入口默认关闭。

## 动作与落表

| 业务动作 | API endpoint | 后端入口 | 目标表 | 实际结果 | 状态 |
|---|---|---|---|---|---|
| Protobuf 候选接入 | `POST /internal/bpi/v1/candidate-events` | `InternalCandidateEventController` -> `CandidateEventMapper` -> `CandidateIngestionService` | `bpi_inbox_events`、`bpi_batch_candidates` | `201 PENDING`，完整 evidence/missing signals 可直接查库 | PASS |
| 候选确认 | `POST /bpi/v1/candidates/{id}/confirm` | `CandidateController` -> `CandidateService` | `bpi_batch_candidates`、`bpi_batch_instances`、`bpi_boundary_evidence`、`bpi_batch_state_events`、`bpi_audit_events`、`bpi_api_idempotency` | `200 CONFIRMED`，`shadow=true` | PASS |
| 跨 tenant event | mapper/受信 token | `CandidateEventMapper` | 无 | `403` | PASS |
| 无详细证据 event | mapper/受信 token | `CandidateEventMapper` | 无 | `422` | PASS |

## Marker 与 SQL

测试使用 `ADP_E2E_BPI_<UUID>` tenant、`ADP_E2E_BPI_PROTO_*` event/order marker。关键查询：

```sql
SELECT candidate_key, state, evidence->0->>'signal' AS signal,
       evidence->0->>'source' AS source, missing_signals->>0 AS missing_signal
  FROM bpi.bpi_batch_candidates
 WHERE tenant_id = :tenant_marker;

SELECT count(*) FROM bpi.bpi_inbox_events WHERE tenant_id = :tenant_marker;
SELECT count(*) FROM bpi.bpi_batch_instances WHERE tenant_id = :tenant_marker;
SELECT count(*) FROM bpi.bpi_boundary_evidence WHERE tenant_id = :tenant_marker;
```

确认前 `source=bpi-stream-engine`、`missing_signal=column.level`；确认后候选 revision=2、批次 1 行、
边界证据 1 行。`@AfterEach` 仅清理动态 tenant marker。

## 边界

- `BPI_CANDIDATE_PROTOBUF_HTTP_INGRESS_ENABLED=false` 是生产默认值。
- 原字段号 1-17 未变化；详细证据使用兼容新增字段 18，missing signals 使用字段 19。
- binary payload 上限 1 MiB，仍需 JWT issuer/audience/TTL、角色和 tenant/plant/line scope。
- Kafka topic、transactional sink/consumer、远端 Docker 部署和浏览器候选确认尚未验收。
