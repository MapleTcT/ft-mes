# BPI 规则退役、回滚与延迟候选落库验收

## 结论

2026-07-18 在目标环境 `10.11.100.17` 完成受控 Phase 1 影子验收，结论为
**`PASS_CONTROLLED_TARGET_SHADOW`**。同一 marker
`ADP_E2E_20260718_065300_BPI_RETIRE_V15B` 闭合了：

```text
真实规则页面退役
  -> PostgreSQL RETIRED/r5 + typed RETIRE outbox
  -> Kafka active=false/RETIRE
  -> Flink APPLIED + runtime INACTIVE
  -> savepoint 有状态升级
  -> 受控 2.0.1 回滚草稿
  -> 退役前已生成候选的延迟消费
  -> BPI inbox + candidate PostgreSQL 恰好一次
  -> 真实候选页面只读复显
  -> marker 定向清理与页面空态复验
```

本轮测试源码分别为 service `07b26131a319857429ae2e8f5803d5a4af3f910d`、
stream `993b99ffa07eaa4bf8e4d5c8783533186ae7cd1e`。目标运行镜像为
`ft-mes-bpi-service:20260718-07b26131`，Flink job 为
`0a2dd090eb290f82d052fc7c0465311f`。机器记录见
[`metadata/bpi-rule-retirement-acceptance.json`](../../metadata/bpi-rule-retirement-acceptance.json)。

## 页面与生命周期

| 页面/阶段 | 动作 | API/事件 | 实际结果 | 状态 |
|---|---|---|---|---|
| `/bpi/#/rules` | 从真实详情抽屉退役 `2.0.0` | `POST /bpi-api/rules/{id}/retire` | `200`；`PUBLISHED/r4 -> RETIRED/r5` | PASS |
| Kafka publication | 核对激活和退役消息 | `bpi.boundary.rule-publication.v1` | partition 1 offset `0/1`；`ACTIVATE -> RETIRE` | PASS |
| Flink lifecycle | 核对双应用回执和运行时回执 | application/readiness topics | 两次 `APPLIED`；`READY -> INACTIVE` | PASS |
| savepoint 升级 | 停止旧 job 并恢复精确 JAR | Flink REST/MinIO | savepoint `savepoint-4fe197-969006fb3dbb` 完成；恢复后 33/33 task RUNNING | PASS |
| `/bpi/#/rules` | 从退役版本创建 `2.0.1` 回滚草稿 | `POST /bpi-api/rules/drafts` | 新 UUID 为 `DRAFT/r1/NOT_PUBLISHED`；旧 UUID 仍 `RETIRED/INACTIVE` | PASS |
| `/bpi/#/candidates` | 只读打开延迟落库候选 | 两次 `GET /bpi-api/candidates...` | 列表/详情均 `200`；`PENDING/r1`、2/2 证据完整 | PASS |
| 清理后候选页 | 刷新候选列表 | `GET /bpi-api/candidates...` | `200`；计数 0，显示“没有待审核候选”，错误为 0 | PASS |

退役脚本和候选只读脚本记录的非预期 console error、page error、request failure 均为 0。
候选抽屉在 `1280x720` 视口完整进入屏幕，边界约为 `left=660/right=1280`；脚本会
等待滑入动画完成后再截图，避免把动画中间帧误判为布局故障。

## 延迟候选落库

候选 `dacdef34-3b29-5753-81da-441ac736d015` 在规则仍有效时由 Flink 产生，随后规则
完成退役。BPI candidate listener 后启用时，旧实现只允许当前 `PUBLISHED` 版本，会把
这个合法在途消息误送 DLQ。service `07b26131` 将版本解析收紧为“历史上已发布且当前为
`PUBLISHED` 或 `RETIRED`”，同时仍拒绝从未发布的 `DRAFT` 规则。

| 核对项 | PostgreSQL/Kafka 结果 |
|---|---|
| candidate topic | partition 0 offset 0，匹配记录恰好 1 条 |
| `bpi.bpi_batch_candidates` | 1 条，`PENDING/r1`，rule=`RETIRED`，topology=`PUBLISHED` |
| `bpi.bpi_inbox_events` | 1 条，source=`bpi.batch.candidate.v1`，event/idempotency identity 精确匹配 |
| `bpi.bpi_batch_instances` | 0 条；本轮没有确认候选 |
| candidate DLQ | 六分区 end offset 全为 0 |

Kafka consumer group 在 partition 0/1 显示 `current=1, end=2, lag=1`。日志段深度检查证明
offset 0 是唯一业务记录，offset 1 是事务 `COMMIT` 控制记录（`isControl=true`）。因此
当前是 **0 条未读业务消息**，不能把 Kafka CLI 的控制记录差值误报为消费积压。

核心复验 SQL：

```sql
SELECT c.id, c.candidate_key, c.state, c.revision, c.order_id,
       r.rule_code, r.version, r.state AS rule_state,
       t.topology_code, t.version, t.state AS topology_state
FROM bpi.bpi_batch_candidates c
JOIN bpi.bpi_rule_versions r ON r.id = c.rule_version_id
JOIN bpi.bpi_topology_versions t ON t.id = c.topology_version_id
WHERE c.candidate_key = 'dacdef34-3b29-5753-81da-441ac736d015';

SELECT source, event_id, idempotency_key, received_at, processed_at
FROM bpi.bpi_inbox_events
WHERE idempotency_key = 'dacdef34-3b29-5753-81da-441ac736d015';
```

## 清理与边界

`bpi-version-lifecycle-cleanup.sql` 现在先冻结候选 ID、candidate key 和 batch ID，再清理
candidate、inbox、batch、audit、API idempotency、outbox、规则/拓扑及测试遥测。它先在
本地 PostgreSQL 16/Flyway V15 使用 `ADP_SQL_20260718_CLEANUP` 播种回归，然后在目标
环境清理本轮 marker 和旧诊断 marker；11 类残留计数全部为 0，共享功能开关保留。

本轮仍不等于正式生产可用：

- candidate 保持 `PENDING`，没有生成批次，也没有写 WOM、QCS 或 WMS。
- 输入是受控上下文与遥测回放，不是现场设备连续运行。
- 真实来源 READY、END 边界、7-14 天影子运行、broker 故障恢复和生产写回仍待闭合。
- 仓库只提交摘要和 SHA-256，不提交凭证、数据库 dump 或运行包二进制。
