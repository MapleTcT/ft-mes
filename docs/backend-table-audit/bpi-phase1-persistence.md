# BPI Phase 1 持久化验收

## 结论

当前验收结论为 **PASS（仅 Phase 1 影子批次纵切）**。

本次在 Java 17、Spring Boot 3.4.7 和隔离 Docker 网络中的 PostgreSQL 16.13 数据库 `bpi_acceptance` 上，通过真实签名 JWT 调用 BPI HTTP API，并由 Flyway V1-V3 管理 `bpi` schema。验收证明候选接入、幂等重放、输入校验、功能开关、人工确认/拒绝、影子批次生成、证据/状态/审计持久化、乐观版本冲突和租户/产线隔离已形成最小闭环。

这不是 BPI Phase 1 全量上线结论。本地嵌入式 Kafka 和模拟浏览器已有独立自动化证据，但测试机上的 Kafka/Flink/PostgreSQL/浏览器联合验收仍受磁盘门禁阻断；本阶段也不包含 WOM、QCS、WMS 的任何写入。

## 验收基线

| 项目 | 实际值 |
|---|---|
| 仓库提交 | `ab1e506`，本轮拒绝候选代码位于未提交工作树 |
| Java / 框架 | Java 17 / Spring Boot 3.4.7 |
| 数据库 | 隔离 Docker 网络 PostgreSQL 16.13，数据库 `bpi_acceptance`，schema `bpi` |
| 数据库迁移 | Flyway `V1__bpi_phase1_baseline.sql` + `V2__bpi_tenant_and_runtime_hardening.sql` + `V3__bpi_telemetry_ingress.sql` |
| HTTP 验收 | `BpiPostgresAcceptanceTest`，真实 HMAC-SHA256 内部 JWT |
| 自动化结果 | BPI service `15/15`、PostgreSQL 类 `4/4`、规则运行时 `9/9`、模拟 API `4/4`、Playwright `3/3` 通过 |
| 验收时间 | 2026-07-13 01:31:36 +08:00 |
| 数据标识 | 每次测试使用动态租户 marker：`ADP_E2E_BPI_<UUID>` |

测试证据位于：

- `services/bpi-service/app/target/surefire-reports/TEST-com.mapletct.ftmes.bpi.BpiPostgresAcceptanceTest.xml`
- `services/bpi-service/batch-rule-runtime/target/surefire-reports/TEST-com.mapletct.ftmes.bpi.rules.BoundaryRuleRuntimeTest.xml`

## API 与持久化验收

| 业务动作 | API endpoint | 关键请求条件 | 实际结果 | 目标表 | 状态 |
|---|---|---|---|---|---|
| 非法证据拒绝 | `POST /internal/bpi/v1/candidates` | evidence 缺少 `eventTime` | `422`；进入 Controller 前拒绝，inbox 仍为 0 行 | 无 | PASS |
| 内部候选接入 | `POST /internal/bpi/v1/candidates` | 已签名 JWT，角色 `BPI_EVENT_INGEST`，tenant/plant/line claims | `201`；候选状态 `PENDING`、revision `1` | `bpi_inbox_events`、`bpi_batch_candidates` | PASS |
| 候选重放 | `POST /internal/bpi/v1/candidates` | 原请求完整重放 | `201`；返回同一 candidate ID；候选和 inbox 均仍为 1 行 | `bpi_inbox_events`、`bpi_batch_candidates` | PASS |
| 事件身份冲突 | `POST /internal/bpi/v1/candidates` | 保持同一 `eventId`，更换 `candidateKey`，因此 payload/checksum 也变化 | `409 application/problem+json`；未新增候选或 inbox | `bpi_inbox_events`、`bpi_batch_candidates` | PASS |
| 确认命令前置条件 | `POST /bpi/v1/candidates/{id}/confirm` | 缺少 `Idempotency-Key` 和 `If-Match` | `428`；无业务写入 | 无 | PASS |
| 确认候选并生成影子批次 | `POST /bpi/v1/candidates/{id}/confirm` | shift-lead JWT、`Idempotency-Key`、`If-Match: 1`、reason | `200`；候选变为 `CONFIRMED`、revision `2`；生成 `shadow=true` 且 `wmsStatus=NOT_REQUESTED` 的批次 | `bpi_batch_candidates`、`bpi_batch_instances`、`bpi_batch_state_events`、`bpi_boundary_evidence`、`bpi_audit_events`、`bpi_api_idempotency` | PASS |
| 确认命令幂等重放 | `POST /bpi/v1/candidates/{id}/confirm` | 相同 command 和相同 `Idempotency-Key` | `200`；响应头 `Idempotent-Replay: true`；返回同一 batch ID | `bpi_api_idempotency` | PASS |
| 拒绝命令前置条件 | `POST /bpi/v1/candidates/{id}/reject` | 缺少 `Idempotency-Key` 和 `If-Match` | `428`；无业务写入 | 无 | PASS |
| 拒绝误判候选 | `POST /bpi/v1/candidates/{id}/reject` | shift-lead JWT、`Idempotency-Key`、`If-Match: 1`、reason | `200`；候选变为 `REJECTED`、revision `2`；写入审核人、原因和审计 | `bpi_batch_candidates`、`bpi_audit_events`、`bpi_api_idempotency` | PASS |
| 拒绝命令幂等重放 | `POST /bpi/v1/candidates/{id}/reject` | 相同 command 和相同 `Idempotency-Key` | `200`；`Idempotent-Replay: true`；无第二条审计 | `bpi_api_idempotency` | PASS |
| 跨命令复用幂等键 | 先 `reject` 后用同 key 调 `confirm` | method/path 不同，reason/revision 相同 | `409`；拒绝响应不能按确认响应重放 | `bpi_api_idempotency` 仍为 1 行 | PASS |
| 拒绝后旧页面确认 | `POST /bpi/v1/candidates/{id}/confirm` | 新 key、过期 `If-Match: 1` | `409`、`currentRevision=2`；不创建批次 | 无新增 | PASS |
| 幂等回放权限复核 | `POST /bpi/v1/candidates/{id}/confirm` | 相同 tenant/key，但 token 不含目标 line | `403`；缓存响应返回前重新校验对象权限 | 无 | PASS |
| 过期版本拒绝 | `POST /bpi/v1/candidates/{id}/confirm` | 新 idempotency key，但仍使用 `If-Match: 1` | `409`；响应 `currentRevision=2`；事务回滚，无额外幂等行 | `bpi_api_idempotency` | PASS |
| 影子批次查询 | `GET /bpi/v1/batches/{id}`、`/evidence`、`/timeline` | viewer JWT，作用域包含目标 plant/line | `200`；批次为 shadow，起始证据 1 条，时间线动作为 `SHADOW_BATCH_CREATED` | `bpi_batch_instances`、`bpi_boundary_evidence`、`bpi_batch_state_events` | PASS |
| 产线隔离 | `GET /bpi/v1/candidates/{id}` | 同 tenant、错误 line claim | `403` | 无写入 | PASS |
| 租户隔离 | `GET /bpi/v1/candidates/{id}` | 其他 tenant，plant/line 为通配符 | `404`，不泄露跨租户对象存在性 | 无写入 | PASS |
| 命令开关 fail-closed | `POST /bpi/v1/candidates/{id}/confirm` | 删除测试租户 LINE 级 `bpi.commands=true` 覆盖 | 继承全局 `bpi.commands=false`，返回 `403` | 无写入 | PASS |

## PostgreSQL 落表结果

以下结果由验收测试通过 `JdbcTemplate` 直接查询本地 `ft_mes_bpi`，过滤同一次测试的 `tenant_id = :tenant_marker`，并在清理前断言：

| 表 | 实际行数 |
|---|---:|
| `bpi.bpi_batch_candidates` | 1 |
| `bpi.bpi_inbox_events` | 1 |
| `bpi.bpi_batch_instances` | 1 |
| `bpi.bpi_batch_state_events` | 1 |
| `bpi.bpi_boundary_evidence` | 1 |
| `bpi.bpi_audit_events` | 1 |
| `bpi.bpi_api_idempotency` | 1 |

复验 SQL：

```sql
SELECT 'bpi_batch_candidates' AS table_name, count(*) AS row_count
  FROM bpi.bpi_batch_candidates WHERE tenant_id = :tenant_marker
UNION ALL
SELECT 'bpi_inbox_events', count(*)
  FROM bpi.bpi_inbox_events WHERE tenant_id = :tenant_marker
UNION ALL
SELECT 'bpi_batch_instances', count(*)
  FROM bpi.bpi_batch_instances WHERE tenant_id = :tenant_marker
UNION ALL
SELECT 'bpi_batch_state_events', count(*)
  FROM bpi.bpi_batch_state_events WHERE tenant_id = :tenant_marker
UNION ALL
SELECT 'bpi_boundary_evidence', count(*)
  FROM bpi.bpi_boundary_evidence WHERE tenant_id = :tenant_marker
UNION ALL
SELECT 'bpi_audit_events', count(*)
  FROM bpi.bpi_audit_events WHERE tenant_id = :tenant_marker
UNION ALL
SELECT 'bpi_api_idempotency', count(*)
  FROM bpi.bpi_api_idempotency WHERE tenant_id = :tenant_marker;
```

### 幂等行数说明

当前真实断言是 `bpi_api_idempotency = 1`，不是 2：首次成功确认持久化 1 行；相同 key 的重复命令复用该行；过期 revision 使用的新 key 在 `409` 事务中整体回滚，不留下持久行。缺少请求头的 `428` 在进入命令事务前即被拒绝，也不会创建幂等记录。

### 拒绝候选零副作用

拒绝专项 marker 的候选行为 `REJECTED|2|acceptance-user|现场确认该边界为流量波动误判`，审计行为
`CANDIDATE_REJECTED|1|2`。清理前直接查库确认：候选、审计、幂等各 1 行；
`bpi_batch_instances`、`bpi_batch_state_events`、`bpi_boundary_evidence` 均为 0 行。

## 清理与可复验性

测试结束后，`@AfterEach` 按动态 tenant marker 定向删除本次验收数据，范围包括 audit、state event、evidence、API idempotency、inbox、candidate、batch，以及测试播种的 rule/topology version。清理不使用全表删除、不删除 schema，也不影响其他租户数据。

## 未覆盖范围

- Kafka listener、重复投递和 DLQ 已在本地嵌入式 broker 验证；目标三节点集群仍未联合验收。
- Flink 逻辑与确定性回放已有独立测试；目标集群 event-time、checkpoint 和恢复仍未联合验收。
- 候选输入来自验收回放 payload，不是真实 IoT/JetLinks 测点流。
- 已通过模拟浏览器确认和拒绝候选；尚未完成目标环境浏览器到 PostgreSQL 的联合落库验收。
- 影子批次不写 WOM、QCS、WMS；`wmsStatus=NOT_REQUESTED` 是本阶段的预期行为。
- 验收 JWT 使用短时 HS256 内部测试密钥；生产 JWKS/非对称签名和密钥轮换尚未完成。
- 本地 Docker 已完成隔离 PostgreSQL 16.13 验收；目标机因根分区约 0.51 GiB 可用而未启动新容器。
- 未验证生产级部署、并发压测、故障注入、备份恢复或长时间运行。
