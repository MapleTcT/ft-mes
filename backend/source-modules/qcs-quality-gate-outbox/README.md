# QCS Quality Gate Outbox

该模块把旧 ADP/QCS 的最终检验结论可靠发布为 BPI `QcsQualityGateV1` 事件。它运行在
Java 8 上，数据库只使用 PostgreSQL；Oracle 仍是 `legacy-template-only`，不参与默认运行路径。

## 事务边界

1. `193-qcs-bpi-quality-gate-outbox.sql` 为 `qcs_inspect_reports` 和
   `qcs_report_coms` 安装延迟约束触发器，在整笔 QCS 事务结束时读取最终快照。
2. 映射链固定为 QCS report -> QCS inspect -> WOM task ->
   `wom_bpi_production_context_bindings`，不猜测 tenant、工厂或产线。
3. 完整且语义明确的最终快照进入 `READY`；缺映射、缺明细或状态矛盾分别进入
   `BLOCKED_MAPPING`、`BLOCKED_DATA`、`BLOCKED_STATE`。
4. Java sidecar 使用 `FOR UPDATE SKIP LOCKED` 抢占行，通过受信 JWT 调用
   `GET /internal/bpi/v1/batches/resolve` 精确解析 BPI 批次，再发布 Kafka。
5. Broker 确认后才写入 `SENT`、`resolved_batch_id` 和 `payload_sha256`。发送成功但数据库
   确认失败时允许相同幂等键重复投递，由 BPI inbox 去重。

Kafka 发布 topic 只取部署配置 `QCS_BPI_OUTBOX_TOPIC`；数据库行中的 `topic` 仅作为捕获审计字段，
不能重定向发布目标。

`READY`、`SENDING`、`SENT`、`DEAD` revision 均不可变。业务结论改变时必须产生新的
QCS report revision；只有 `BLOCKED_*` 快照可在补齐映射或数据后原位恢复。迁移不会回填历史
质检结果，避免上线时意外批量放行。同一个 QCS inspection request 若出现多份有效终态报告，
系统保留 QCS 业务事务但将新增快照置为 `BLOCKED_DATA`，在产品语义确认前不自动选择其中一份。

## 构建与数据库验收

```bash
make qcs-quality-gate-outbox-test
make qcs-quality-gate-outbox-postgres-test
make qcs-quality-gate-outbox-package
```

现有测试库需由数据库管理员显式应用迁移；新建 Docker 数据卷会通过 init 自动应用：

```bash
psql "$ADP_DATABASE_URL" -v ON_ERROR_STOP=1 \
  -f deploy/docker/postgres/init/193-qcs-bpi-quality-gate-outbox.sql
```

发布账号最小权限为：

```sql
GRANT USAGE ON SCHEMA public TO qcs_bpi_outbox;
GRANT SELECT, UPDATE ON public.qcs_bpi_quality_gate_outbox TO qcs_bpi_outbox;
```

不要授予 sidecar 修改 QCS report、WOM task 或绑定表的权限。

## 启用顺序

默认配置同时关闭 sidecar 和 BPI Phase 2。启用前必须满足：

- `BPI_PHASE2_INTEGRATION_ENABLED=true`；
- `BPI_PHASE2_KAFKA_ENABLED=true`；
- `BPI_PHASE2_ALLOWED_TENANT_IDS/PLANT_IDS/LINE_IDS` 是精确试点范围；
- BPI 功能开关 `bpi.qcs-link=true` 已在相同 tenant/plant/line scope 生效；
- QCS 对应 WOM task 的 `line_id` 已映射到唯一启用的 BPI scope；
- 同一 `order_id` 在该 scope 内只有一个可接收质检结果的 BPI 批次；
- Kafka topic `qcs.batch.quality-gate.v1` 和 consumer 已就绪。

构建 JAR 后启动受控 sidecar：

```bash
make qcs-quality-gate-outbox-package
docker compose --env-file deploy/docker/.env \
  -f deploy/docker/docker-compose.yml --profile bpi \
  up -d --build qcs-quality-gate-outbox
```

先以 `QCS_BPI_OUTBOX_ENABLED=false` 验证健康检查，再为精确试点范围打开。暂停发布只需恢复
`false` 并重建该容器；数据库捕获仍保留审计证据。若触发器阻断真实 QCS 写入，应停止业务写入、
保留事务错误证据，并经审批后再决定是否移除触发器，不能删除 outbox 历史行。

## 运维查询

```sql
SELECT publication_state, count(*)
FROM qcs_bpi_quality_gate_outbox
GROUP BY publication_state
ORDER BY publication_state;

SELECT id, qcs_report_id, qcs_report_revision, wom_task_id,
       tenant_id, plant_id, line_id, order_id, publication_state,
       attempt_count, block_reason, last_error, resolved_batch_id,
       created_at, sent_at
FROM qcs_bpi_quality_gate_outbox
WHERE publication_state <> 'SENT'
ORDER BY id DESC
LIMIT 100;
```

现场旧 WOM task 若没有 `line_id` 或有效 binding，会保持 `BLOCKED_MAPPING`，不允许临时填默认产线。
补齐受控映射后，可由管理员执行 `qcs_bpi_enqueue_quality_gate(report_id)` 重新捕获一个可恢复快照。

## Marker 验收

从真实 QCS 页面提交带 `ADP_E2E_YYYYMMDD_HHMMSS_QCS_GATE` 标识的最终检验结果，并同时保存：

- 浏览器 route、操作步骤、console 和 network 证据；
- QCS report/component 与 outbox 同事务落库结果；
- resolver 的 HTTP 状态及唯一 BPI `batchId`；
- Kafka key `batchId|qualityGateId` 和四个必需 header；
- BPI inbox、quality gate、batch state 与 audit 行；
- 相同 revision 重放不重复改变业务状态。

任何 `BLOCKED_*`、`DEAD`、长时间 `SENDING`，或接口成功但 PostgreSQL 状态未改变，均为失败。
