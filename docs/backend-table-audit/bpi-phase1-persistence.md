# BPI Phase 1 持久化验收

## 结论

当前验收结论为 **PASS（仅 Phase 1 影子批次纵切）**。

本次在 Java 17、Spring Boot 3.4.7 和本地 PostgreSQL 16 数据库 `ft_mes_bpi` 上，通过真实签名 JWT 调用 BPI HTTP API，并由 Flyway V1+V2 管理 `bpi` schema。验收证明候选接入、幂等重放、输入校验、功能开关、人工确认、影子批次生成、证据/状态/审计持久化、乐观版本冲突和租户/产线隔离已形成最小闭环。

这不是 BPI Phase 1 全量上线结论，不包含 Kafka、Flink、真实 IoT 数据源、浏览器 UI，也不包含 WOM、QCS、WMS 的任何写入。

## 验收基线

| 项目 | 实际值 |
|---|---|
| 仓库提交 | `65ee6e1`，验收代码位于未提交工作树 |
| Java / 框架 | Java 17 / Spring Boot 3.4.7 |
| 数据库 | 本地 PostgreSQL 16，数据库 `ft_mes_bpi`，schema `bpi` |
| 数据库迁移 | Flyway `V1__bpi_phase1_baseline.sql` + `V2__bpi_tenant_and_runtime_hardening.sql` |
| HTTP 验收 | `BpiPostgresAcceptanceTest`，真实 HMAC-SHA256 内部 JWT |
| 自动化结果 | PostgreSQL 验收 `1/1` 通过；规则运行时 `2/2` 通过；0 failure、0 error、0 skipped |
| 验收时间 | 2026-07-12 17:01:20 +08:00 |
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

## 清理与可复验性

测试结束后，`@AfterEach` 按动态 tenant marker 定向删除本次验收数据，范围包括 audit、state event、evidence、API idempotency、inbox、candidate、batch，以及测试播种的 rule/topology version。清理不使用全表删除、不删除 schema，也不影响其他租户数据。

## 未覆盖范围

- 未接入 Kafka，未验证消息分区、积压、重放和消费端恢复。
- 未接入 Flink，未验证 event-time、水位线、迟到事件和状态恢复。
- 候选输入来自验收回放 payload，不是真实 IoT/JetLinks 测点流。
- 未通过浏览器 UI 执行操作，不构成前端功能验收。
- 影子批次不写 WOM、QCS、WMS；`wmsStatus=NOT_REQUESTED` 是本阶段的预期行为。
- 验收 JWT 使用短时 HS256 内部测试密钥；生产 JWKS/非对称签名和密钥轮换尚未完成。
- Docker Compose 已通过渲染校验，但本机 Docker daemon 未运行，尚未执行容器启动验收。
- 未验证生产级部署、并发压测、故障注入、备份恢复或长时间运行。
