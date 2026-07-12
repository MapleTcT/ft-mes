# BPI 遥测事实接入落库验收

## 结论

2026-07-12 在 Java 17、Spring Boot 3.4.7、本地 PostgreSQL 16 数据库 `ft_mes_bpi` 上，
`POST /internal/bpi/v1/telemetry` 的真实 API 和 PostgreSQL marker 验收通过。该纵切完成了 envelope 幂等、
源 epoch/sequence 去重、序列缺口与乱序分类、点级质量隔离、envelope 级隔离以及 tenant/plant/line 权限校验。

本结论只覆盖 BPI 短期 replay staging。该入口默认关闭，测试通过
`bpi.telemetry.http-ingress-enabled=true` 显式开启。JetLinks exporter、Kafka、Flink event-time 规则和批次候选生成
仍是后续门禁，不能把本次 HTTP 回放验收描述为完整实时链路，也不能把这些 staging 表作为全量测点权威存储。

## API 与表映射

| 业务动作 | 前端/来源入口 | API endpoint | 后端入口 | 目标表 | 实际结果 | 状态 |
|---|---|---|---|---|---|---|
| 部分有效 envelope 接入 | 受控 IoT replay | `POST /internal/bpi/v1/telemetry` | `InternalTelemetryController` -> `TelemetryIngestionService` -> `TelemetryPostgresRepository` | `bpi_telemetry_source_state`、`bpi_telemetry_events`、`bpi_telemetry_points`、`bpi_telemetry_point_rejects` | `201 PARTIAL`；2 个点接受、1 个点隔离 | PASS |
| 完整重放 | 同一 replay payload | 同上 | 同上 | event/point/reject | `200 replay=true`；行数不增加 | PASS |
| eventId 复用但 payload 变化 | replay 变体 | 同上 | 同上 | 无新增 | `409` | PASS |
| epoch/sequence 被其他事件占用 | replay 变体 | 同上 | 同上 | 无新增 | `409` | PASS |
| sequence 从 1 跳到 3 | replay | 同上 | 同上 | source state、event、point | `201 GAP`；source state 推进到 3 | PASS |
| sequence 2 迟到 | replay | 同上 | 同上 | event、point | `201 OUT_OF_ORDER`；source state 保持 3 | PASS |
| source epoch 回退 | replay | 同上 | 同上 | quarantine | `202 QUARANTINED`；事实表无该 event | PASS |
| envelope 缺少事件时间 | replay | 同上 | 同上 | quarantine | `202 QUARANTINED` | PASS |
| JSON `null` envelope | replay | 同上 | 同上 | quarantine | `202 QUARANTINED`，不泄漏空指针 `500` | PASS |
| 空点集 | replay | 同上 | 同上 | source state、event | `201 EMPTY`；不写点、不把缺失值当零 | PASS |
| 缺少接入角色 | replay | 同上 | security/service | 无 | `403` | PASS |
| line scope 不匹配 | replay | 同上 | security/service | 无 | `403` | PASS |
| 两个相同 envelope 并发到达 | 双线程 MockMvc | 同上 | event identity advisory lock + 同上 | source state、event、point | 一个 `201`、一个 `200 replay=true`；event/point 均仅 1 行 | PASS |

## Marker 与复验 SQL

测试类 `BpiTelemetryPostgresAcceptanceTest` 每次生成
`ADP_E2E_TELEMETRY_<UUID>` 作为 tenant 和事件前缀。断言发生在定向清理之前：

| 表 | 断言行数 |
|---|---:|
| `bpi.bpi_telemetry_source_state` | 1 |
| `bpi.bpi_telemetry_events` | 4 |
| `bpi.bpi_telemetry_points` | 4 |
| `bpi.bpi_telemetry_point_rejects` | 1 |
| `bpi.bpi_telemetry_quarantine` | 3 |

```sql
SELECT 'source_state' AS item, count(*) FROM bpi.bpi_telemetry_source_state WHERE tenant_id = :tenant_marker
UNION ALL
SELECT 'events', count(*) FROM bpi.bpi_telemetry_events WHERE tenant_id = :tenant_marker
UNION ALL
SELECT 'points', count(*) FROM bpi.bpi_telemetry_points WHERE tenant_id = :tenant_marker
UNION ALL
SELECT 'point_rejects', count(*) FROM bpi.bpi_telemetry_point_rejects WHERE tenant_id = :tenant_marker
UNION ALL
SELECT 'quarantine', count(*) FROM bpi.bpi_telemetry_quarantine WHERE tenant_id = :tenant_marker;

SELECT source_epoch, last_sequence, last_event_id, revision
  FROM bpi.bpi_telemetry_source_state
 WHERE tenant_id = :tenant_marker
   AND gateway_id = 'GW-S07-01'
   AND device_id = 'FLOW-S07-01';
```

实际 source state 最终 `last_sequence = 4`；sequence 2 的迟到事实保留，但不会倒退 source state。
`@AfterEach` 只删除动态 tenant marker 对应的数据，清理后残留为 0。

并发重放测试使用另一动态 marker，同时释放两个请求；事务级 event identity advisory lock 将相同事件串行化，
断言状态码恰为 `200 + 201`，且 event、point、source state 各只有 1 行，避免唯一键冲突泄漏为 `500`。

## 未覆盖范围

- JetLinks EventBus exporter 和 Protobuf 序列化尚未接入。
- Kafka topic、partition key、积压重放和 consumer recovery 尚未验收。
- Flink watermark、迟到修正、状态快照和候选生成尚未实现。
- 当前接入是受信 HTTP replay，不是最终的高吞吐 Kafka 消费入口。
- HTTP 入口生产默认关闭；原始测点权威仍在 JetLinks/Timescale，不能长期复制到 PostgreSQL staging 表。
- 尚未执行并发竞争、持续负载、容器运行和生产部署验收。
