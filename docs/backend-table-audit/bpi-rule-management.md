# BPI 规则回放与发布落库验收

状态：`PASS`（本地隔离 PostgreSQL 16.13）

范围：工艺拓扑只读、规则只读、真实遥测回放、金标准比对、模拟结果和 checksum 门控发布

验收类：`BpiRulePostgresAcceptanceTest`

## 验收边界

- Flyway 从空库顺序应用 V1-V5，默认数据库仅为 PostgreSQL。
- 规则回放读取 `bpi_telemetry_points`，不会使用 UI mock 或固定成功结果。
- 回放调用与在线候选相同的 `batch-rule-runtime`，最多读取 100,001 行并在超过 100,000 个观测值时 fail closed。
- 发布要求最近模拟属于当前规则、状态为 `PASSED`、checksum 完全一致且 revision 未过期。
- `bpi.rule-management` 默认 `false`；验收只为动态 tenant/line 打开。
- 本阶段不实现规则草稿编辑和拓扑发布，也不向 WOM、QCS、WMS、PLC 或 DCS 写数据。

## API 与落表结果

| 业务动作 | API | 目标表 | 实际结果 | 状态 |
|---|---|---|---|---|
| 查询作用域拓扑 | `GET /bpi/v1/topologies`、`/{id}` | `bpi_topology_versions` | 只返回 JWT tenant/plant/line 范围内版本 | PASS |
| 查询作用域规则 | `GET /bpi/v1/rules`、`/{id}` | `bpi_rule_versions` | 返回受控 AST、拓扑版本、revision 和 checksum | PASS |
| 缺少命令头模拟 | `POST /bpi/v1/rules/{id}/simulate` | 无 | `428`，不写幂等行 | PASS |
| 空遥测窗口模拟 | 同上 | 无 | `422`，事务回滚，不写幂等行 | PASS |
| 历史回放 | 同上 | `bpi_rule_simulations`、`bpi_rule_versions`、`bpi_audit_events`、`bpi_api_idempotency` | 2 个校准观测值产生 1 个 START 边界，匹配 1 个人工金标准 | PASS |
| 模拟幂等重放 | 同上 | `bpi_api_idempotency` | 返回相同 simulationId/checksum，不重复落表 | PASS |
| 错误 checksum 发布 | `POST /bpi/v1/rules/{id}/publish` | 无新增 | `422`，规则保持 `SIMULATION_PASSED/r2` | PASS |
| 正确 checksum 发布 | 同上 | `bpi_rule_versions`、`bpi_audit_events`、`bpi_api_idempotency` | 规则进入 `PUBLISHED/r3` | PASS |
| 发布幂等重放 | 同上 | `bpi_api_idempotency` | 返回原发布结果，不重复 revision | PASS |
| 跨作用域读取 | `GET /bpi/v1/rules/{id}` | 无 | `404`，不泄露对象存在性 | PASS |

## 直接 SQL 复验

验收在 marker 清理前执行以下等价查询：

```sql
SELECT state, revision, latest_simulation_id
  FROM bpi.bpi_rule_versions
 WHERE tenant_id = :tenant_marker AND id = :rule_id;

SELECT state, checksum, metrics, input_manifest, emitted_boundaries
  FROM bpi.bpi_rule_simulations
 WHERE tenant_id = :tenant_marker AND rule_version_id = :rule_id;

SELECT action, before_revision, after_revision
  FROM bpi.bpi_audit_events
 WHERE tenant_id = :tenant_marker AND object_id = :rule_id
 ORDER BY created_at;
```

模拟后断言为 `SIMULATION_PASSED|2`，指标为 `matched=1, missed=0, falsePositive=0`，
`observationCount=2`，发射边界等于金标准时间。发布后断言为 `PUBLISHED|3`，审计顺序为
`RULE_SIMULATED|1|2`、`RULE_PUBLISHED|2|3`。

第二个真实 PostgreSQL 用例把两个 quorum 条件的 `holdSeconds` 设为 15。观测时间为
`2026-07-12T08:15:00Z`，窗口结束为 `08:15:30Z`；回放结果必须且实际在 `08:15:15Z` 发射，证明历史回放
按事件时间触发 hold timer，而不是把边界错误记在窗口结束时间。

## 未覆盖范围

- 浏览器到 Java 8 适配器、Java 17 服务和 PostgreSQL 的目标机联合验收尚未执行。
- 生产规则发布所需双人审批、规则草稿编辑、拓扑草稿/校验/发布尚未实现。
- 100,000 观测值上限已有代码边界，尚未做上限附近的性能和内存压测。
- 目标 Kafka/Flink 集群、真实 JetLinks 历史窗口和故障恢复仍待目标机容量释放后验收。
