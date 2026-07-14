# MES Production Context Outbox

该模块把 WOM 制造任务的有效上下文发布到 BPI Kafka topic
`mes.production.context.v1`。它运行在 Java 8，不侵入现有反编译服务，也不把 Oracle 恢复为默认路径。

## 数据链路

1. WOM 在 PostgreSQL 中新增或更新 `wom_produce_tasks`。
2. `176-wom-bpi-production-context-outbox.sql` 的触发器在同一事务内捕获完整业务快照；
   `177-wom-bpi-context-revision-clock-floor.sql` 防止适配器重建或共享 Topic 验收造成低版本复用。
3. 没有产线绑定、任务状态语义或关键字段时，快照进入 `BLOCKED_MAPPING`、
   `BLOCKED_STATE` 或 `BLOCKED_DATA`，不会被发布。
4. Java 8 发布器使用 `FOR UPDATE SKIP LOCKED` 抢占 `READY/RETRY` 行，构造并校验
   `ProductionContextEventV1`，按 `tenant|plant|line` 分区键发送 Kafka。
5. Kafka 已确认后标记 `SENT`。发送成功但数据库确认失败时可能重复投递；稳定 `event_id`
   让 Flink 时间线按内容幂等。

## 上线顺序

先对现有 ADP PostgreSQL 显式执行迁移，新建环境会由 Docker init 自动执行：

```bash
psql "$ADP_DATABASE_URL" -v ON_ERROR_STOP=1 \
  -f deploy/docker/postgres/init/176-wom-bpi-production-context-outbox.sql
psql "$ADP_DATABASE_URL" -v ON_ERROR_STOP=1 \
  -f deploy/docker/postgres/init/177-wom-bpi-context-revision-clock-floor.sql
```

确认现场 `cid`、WOM `line_id` 和 BPI scope 后再插入绑定。下面仅为格式示例，不是生产默认值：

```sql
INSERT INTO wom_bpi_production_context_bindings (
  wom_cid, wom_line_id, tenant_id, plant_id, line_id, enabled
) VALUES (1000, 77, '1000', 'PLANT-01', 'LINE-S07-01', true);
```

任务状态必须由业务负责人确认后逐项配置。数据库迁移不会猜测 `runing`、`iskeep`、
`finished` 或 `waitForRun` 的含义：

```sql
INSERT INTO wom_bpi_task_state_mappings (
  wom_state_code, active, enabled, description
) VALUES
  ('wom_runstate/runing', true, true, '现场确认：任务正在生产'),
  ('wom_runstate/finished', false, true, '现场确认：任务已经结束');
```

若旧值实际只保存 `runing`，映射键也必须保存 `runing`。触发器比较时会做 `lower(trim())`，
但不会做别名推断。

发布器数据库账号只需 outbox 读写权限。由数据库管理员创建账号后执行：

```sql
GRANT USAGE ON SCHEMA public TO mes_context_outbox;
GRANT SELECT, UPDATE ON public.wom_bpi_production_context_outbox TO mes_context_outbox;
```

不要给发布器 `wom_produce_tasks`、绑定表或状态映射表写权限。

## 启用与回滚

先构建，再把 `deploy/bpi-streaming/.env` 中 `MES_CONTEXT_OUTBOX_ENABLED` 改为 `true`：

```bash
mvn -pl backend/source-modules/mes-production-context-outbox -am package
docker compose --env-file deploy/bpi-streaming/.env \
  -f deploy/bpi-streaming/docker-compose.yml --profile mes-context \
  up -d --build mes-production-context-outbox
```

触发器异常会回滚同一笔 WOM 写事务；启用前必须在测试环境完成 marker 演练，
若出现业务写入阻断，应先停用发布器，并在审批后停止捕获：

```sql
DROP TRIGGER IF EXISTS trg_wom_bpi_production_context ON wom_produce_tasks;
```

只需暂停 Kafka 发布时，把 `MES_CONTEXT_OUTBOX_ENABLED=false` 并重建容器即可，
此操作不会停止数据库侧捕获。

不要删除 outbox 行或 revision 表；它们是重复投递判定和审计证据。

## Marker 验收

从真实 WOM 页面对一条单产线测试任务执行带 marker 的状态变化，例如
`ADP_E2E_YYYYMMDD_HHMMSS_CONTEXT`。记录浏览器请求后查询：

```sql
SELECT id, event_id, wom_task_id, tenant_id, plant_id, line_id,
       order_id, batch_id, material_code, recipe_version,
       source_state, context_revision, active, publication_state,
       attempt_count, block_reason, last_error, created_at, sent_at
FROM wom_bpi_production_context_outbox
WHERE order_id LIKE '%ADP_E2E_%' OR batch_id LIKE '%ADP_E2E_%'
ORDER BY id DESC;
```

通过条件：

- WOM 写动作和 outbox 行处于同一提交结果；WOM 回滚时不得留下 outbox 行。
- 活动上下文包含订单或批次、任务、物料、配方和唯一 scope revision。
- outbox 最终为 `SENT`，Kafka 上同 `event_id` 的 protobuf 可解码且合同校验通过。
- Flink 接收相同 scope 后，后续遥测能解析到该生产上下文。
- Kafka 暂停时行进入 `RETRY`；恢复后同一 `event_id` 发送成功。
- `BLOCKED_*`、`DEAD`、长时间 `SENDING` 均为验收失败，必须定位后再扩大产线。

常用巡检：

```sql
SELECT publication_state, count(*)
FROM wom_bpi_production_context_outbox
GROUP BY publication_state
ORDER BY publication_state;

SELECT id, wom_task_id, publication_state, block_reason, last_error, created_at
FROM wom_bpi_production_context_outbox
WHERE publication_state IN (
  'BLOCKED_MAPPING', 'BLOCKED_STATE', 'BLOCKED_DATA', 'DEAD'
)
ORDER BY id DESC
LIMIT 100;
```
