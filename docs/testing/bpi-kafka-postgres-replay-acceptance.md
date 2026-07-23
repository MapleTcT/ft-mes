# BPI Kafka/Flink/PostgreSQL 联合回放验收

## 当前结论

状态为 **LEGACY_HARNESS_SUPERSEDED / TARGET_ACCEPTED**。旧
`make bpi-stream-postgres-replay` 没有在当前目标环境重跑；当前能力由后续真实页面、Kafka/Flink 和
PostgreSQL 同 marker 联合链验收。

容量与集群基础已在 2026-07-23 复验：

| 项目 | 实际结果 | 状态 |
|---|---|---|
| 根分区可用空间 | `73.38 GiB` | PASS |
| Docker root 可用空间 | `/data/docker/docker`，`1552 GiB` | PASS |
| Kafka | 3 broker、24 topic、副本 3、minISR 2 | PASS |
| Flink | job `40f36698aeee4aaae17eac52608c7939`、2 TM、checkpoint `14905` | PASS |
| 破坏性操作 | 未执行 prune、删除、volume 清理或数据库重置 | PASS |

机器证据见
[`metadata/bpi-test-host-capacity-preflight.json`](../../metadata/bpi-test-host-capacity-preflight.json)。

## 当前落库验收

替代验收 marker：`ADP_E2E_20260714_091536_BPI_JOINT`。

| 证据 | 实际结果 | 状态 |
|---|---|---|
| 浏览器认证与规则模拟 | HTTP 200/202，页面无 console/page/request error | PASS |
| 规则发布 | PostgreSQL outbox + Kafka publication | PASS |
| Flink 应用 | `APPLIED` 回执并由浏览器显示 | PASS |
| Candidate | Kafka 只有 1 条匹配候选 | PASS |
| PostgreSQL | inbox=1、candidate=1、batch=1、evidence=2，并有 state/audit/idempotency | PASS |
| 浏览器确认 | candidate `CONFIRMED`，shadow batch `ACTIVE` | PASS |
| 规则退役 | typed `INACTIVE` + 新 `APPLIED` 回执 | PASS |
| 清理 | topology/rule/candidate/batch marker 行均为 0，consumer 恢复 deny-all | PASS |

验收 SQL：

- `deploy/bpi-runtime/sql/joint-acceptance-verify.sql`
- `deploy/bpi-runtime/sql/joint-acceptance-cleanup.sql`

机器记录：
[`metadata/bpi-browser-kafka-postgres-joint-acceptance.json`](../../metadata/bpi-browser-kafka-postgres-joint-acceptance.json)。

## 为什么不直接重跑旧脚本

旧 PostgreSQL replay 调用旧 cluster fixture。该 fixture 不符合当前 canonical point-catalog 的 revision、
event ID、Kafka key 和 source headers 契约；在当前 point-catalog/telemetry consumers 旁运行会产生
DLQ，并且它的清理范围没有覆盖这些新消费者产生的所有表。接口 200 或 candidate 出现都不能抵消这种污染。

脚本因此默认 fail-closed。仅在 point-catalog 与 telemetry consumers 已隔离的专用兼容环境，允许一次性
设置：

```bash
BPI_LEGACY_POSTGRES_REPLAY_COMPATIBILITY_ACK=ISOLATED_POINT_CATALOG_AND_TELEMETRY_CONSUMERS \
  make bpi-stream-postgres-replay
```

该确认值不得持久化。当前目标复验应使用真实浏览器联合验收，不使用旧兼容夹具。

## 边界

- `SUPERSEDED` 不表示旧脚本已在当前目标 PASS。
- 当前同 marker 链使用确定性 context/telemetry，不是物理 IoT/JetLinks 来源。
- 生产 HA、持续负载、7-14 天现场影子运行和外部 QCS/ERP-WMS 仍由各自发布门禁控制。
