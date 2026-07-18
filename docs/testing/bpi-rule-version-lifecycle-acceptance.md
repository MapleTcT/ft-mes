# BPI 规则版本比较与审批生命周期验收

## 结论

本轮状态为 **`PASS_CONTROLLED_TARGET`**。2026-07-18 在目标环境
`10.11.100.17` 使用真实 ADP 登录、真实 BPI 页面、正式 `/bpi-api` 适配链和
PostgreSQL 15.18/Flyway V14，闭合了拓扑/规则版本比较、规则回放、提交审批、
职责分离拒绝、独立管理员批准发布和独立管理员驳回。

- 页面：`http://10.11.100.17:18080/bpi/#/rules`
- BPI 服务：`http://10.11.100.17:19091`
- 数据库：`ft_mes_bpi`
- marker：`ADP_E2E_20260718_023214_BPI_LIFECYCLE`
- 测试代码基线：`032ca832ced0ad66d0d2bc3a990b5a8cc5eadabe+worktree`
- 机器记录：`metadata/bpi-rule-version-lifecycle-acceptance.json`

本轮没有把接口 `200` 当成完成依据：每个写动作均核对规则、仿真、审批、幂等、审计
和 outbox 表。测试数据在证据固化后使用 marker 定向清理，所有残留计数为 0。

## 页面与 API

| 页面/路由 | 操作 | Method/API | 请求关键字段 | 实际结果 | 状态 |
|---|---|---|---|---|---|
| `/bpi/#/rules` 拓扑详情 | 对比同 code/同 scope 的 `1.0.0 -> 2.0.0` | `GET /bpi-api/topologies/{id}/compare` | `baseVersionId` | 页面显示稳定的 11 项 JSON Pointer 差异，包含 `/localityGroup`、节点和路径变化 | PASS |
| `/bpi/#/rules` 规则详情 | 对比已发布规则和候选规则 | `GET /bpi-api/rules/{id}/compare` | `baseVersionId` | 页面显示拓扑引用及 `/conditions/0/threshold` 差异 | PASS |
| `/bpi/#/rules` 规则详情 | 对发布候选和驳回候选执行历史回放 | `POST /bpi-api/rules/{id}/simulate` | `lineId/from/to/topologyVersion/calibrationVersion/goldenSetId` | 两次均 `202`；页面显示 matched=1、missed=0、falsePositive=0、平均偏差 0 | PASS |
| `/bpi/#/rules` 规则详情 | 使用最近一次通过的 simulation 提交审批 | `POST /bpi-api/rules/{id}/submit-approval` | `reason/simulationId/simulationChecksum`，并带 `If-Match`、`Idempotency-Key` | 两次均 `200`，页面进入 `PENDING_APPROVAL` | PASS |
| 同一提交人反证 | 提交人尝试批准和驳回自己的申请 | `POST .../publish`、`POST .../reject-approval` | 同上 | 两次均为预期 `422`；分别提示必须由不同管理员决定 | PASS |
| 独立管理员决策后刷新真实页面 | 一条批准发布，一条驳回草稿 | `POST /bpi/v1/rules/{id}/publish`、`POST .../reject-approval` | 批准携带 simulation 证明；驳回携带原因 | 两次均 `200`；页面分别显示 `PUBLISHED` 和 `DRAFT` | PASS |

独立管理员决策使用受控 BPI_ADMIN 身份直接调用 Java 17 服务，签名材料未写入仓库；
原因是当前 ADP `admin` 的旧会话在适配器内映射为同一提交 actor，不能拿同一 actor
伪装职责分离。批准/驳回后重新加载真实页面核对最终状态。

浏览器共记录 40 个 BPI 响应：`36 x 200`、`2 x 202`、`2 x 422`。两条 console
resource error 正好对应预期的 422 反证，非预期 console error、page error 和
request failure 均为 0。

## PostgreSQL 落库

| 业务动作 | 后端入口 | 目标表 | 验收 SQL/结果摘要 | 状态 |
|---|---|---|---|---|
| 历史回放 | `RuleController.simulateRule -> RuleService -> RulePostgresRepository` | `bpi_rule_simulations`、`bpi_rule_versions`、`bpi_api_idempotency`、`bpi_audit_events` | 2 条 simulation 均为 `PASSED`，指标均为 `1/0/0/0`；规则 `r1 -> r2` | PASS |
| 提交审批 | `RuleController.submitRuleApproval -> RuleService -> RulePostgresRepository` | `bpi_rule_approval_requests`、规则/幂等/审计表 | 2 条申请均固定 simulation ID/checksum，申请 `PENDING/r1`，规则 `PENDING_APPROVAL/r3` | PASS |
| 独立批准 | `RuleController.publishRuleVersion -> RuleService -> RulePublicationOutboxRepository` | 审批、规则、outbox、幂等、审计表 | 规则 `PUBLISHED/r4`，审批 `APPROVED/r2`，1 条 `BOUNDARY_RULE_PUBLISHED` outbox | PASS |
| 独立驳回 | `RuleController.rejectRuleApproval -> RuleService -> RulePostgresRepository` | 审批、规则、幂等、审计表 | 规则 `DRAFT/r4`，审批 `REJECTED/r2`，未创建发布 outbox | PASS |
| 审计/幂等 | 同上 | `bpi_audit_events`、`bpi_api_idempotency` | 审计顺序精确为模拟、提交、发布、模拟、提交、驳回；6 条幂等记录均 `COMPLETED` 且响应为 202/200 | PASS |

核心复验 SQL：

```sql
SELECT rule_code, version, state, revision, latest_simulation_id
FROM bpi.bpi_rule_versions
WHERE rule_code LIKE 'ADP_E2E_20260718_023214_BPI_LIFECYCLE%';

SELECT rule_version_id, state, revision, simulation_id, submitted_by, decided_by
FROM bpi.bpi_rule_approval_requests
WHERE rule_version_id IN (
  'b54aedc7-6dd2-47cd-fa8e-c95b1a32fefa',
  'c1e5ea8e-7d1b-54ef-3a76-d3ab09a2e3bb'
);

SELECT aggregate_id, event_type, topic, status, attempt_count
FROM bpi.bpi_outbox_events
WHERE aggregate_id = 'b54aedc7-6dd2-47cd-fa8e-c95b1a32fefa';
```

发布 outbox 为 `PENDING/attempts=0` 是本轮控制面 marker 的预期：目标保持 Phase 1
shadow-only，执行该 marker 时 dispatcher 关闭。它本身不等于 Kafka/Flink 已应用；随后
独立 marker 已闭合规则退役、typed inactive、savepoint 和延迟候选落库，见
[`bpi-rule-retirement-acceptance.md`](bpi-rule-retirement-acceptance.md)。

## 清理与边界

`deploy/docker/scripts/bpi-version-lifecycle-cleanup.sql` 已清理本轮 1 个成功 marker 和
3 个诊断 marker。最终回查 topology、rule、idempotency、telemetry、catalog snapshot
和 golden boundary 均为 0；共享 `bpi.rule-management` 功能开关保留，只归一化测试 actor。

本轮证明的是 **目标环境控制面版本治理与审批落库**，不扩大为以下结论：

- 不声明真实点位已经 READY。
- 不声明规则已由 broker/Flink 应用或完成运行时 READY 回执。
- 不写 WOM、QCS 或 WMS 生产状态。
- 本 marker 不替代规则退役和 typed inactive；二者已由独立 V15 marker 闭合。broker 故障、
  应用镜像回退和产品级生产回滚仍未完成。

原始浏览器报告、PostgreSQL 报告和两张截图保存在 `/tmp`，SHA-256 已写入机器记录，
仓库不提交凭证、ticket、数据库 dump 或运行包二进制。
