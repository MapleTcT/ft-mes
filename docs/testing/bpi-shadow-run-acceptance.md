# BPI 影子运行与人工验收闭环

## 结论

2026-07-20 在目标环境 `10.11.100.17` 完成影子运行软件闭环验收，结论为
**`PASS_CONTROLLED_TARGET_TIME_COMPRESSED_CLEANED`**。marker
`ADP_E2E_SHADOW_20260720_0152_V20` 通过真实 ADP 页面、Java 8 adapter、Java 17
service 和 PostgreSQL 15.18 闭合：

```text
固定规则/拓扑/点位目录 -> DRAFT/r1 -> 页面启动 RUNNING/r2
  -> 10 个 CLOSED_RAW 批次人工复核 -> 边界认同率 95% + 累计量偏差 0%
  -> 页面结束观察 EVALUATING/r13
  -> 未解决 CRITICAL 事件阻断批准 422
  -> 数据质量页面认领并解决事件
  -> 独立管理员批准 APPROVED/r14
  -> PostgreSQL 状态/指标/审计/幂等/外部写隔离直查
  -> marker 定向清理为 0
```

本次把运行时间在数据库中受控压缩为 8 天，只用于确定性验证 `7-14` 天门槛代码，
**不等于现场已经连续运行 8 天**。因此 BPI 的软件验收机制已经闭合，但 G-021 仍为
`PARTIAL_FIELD_DURATION_PENDING`，不能写成生产 READY。机器记录见
[`metadata/bpi-shadow-run-acceptance.json`](../../metadata/bpi-shadow-run-acceptance.json)。

## 页面与 API

| 页面/阶段 | 操作 | API | 实际结果 | 状态 |
|---|---|---|---|---|
| Java 17 service | 独立工程师创建验收任务 | `POST /bpi/v1/shadow-runs` | `200`，`DRAFT/r1`，9 项启动准入全部通过 | PASS |
| `/bpi/#/shadowRuns` | 页面启动 | `POST /bpi-api/shadow-runs/{id}/start` | `200`，`RUNNING/r2` | PASS |
| 批次复核弹窗 | 首批故意制造 61 秒结束偏差 | `POST /bpi-api/shadow-runs/{id}/batch-reviews` | `200`，首批边界 1/2 通过 | PASS |
| Adapter 合同 | 继续复核 9 批 | 同上 | 10 个有效样本，19/20 边界通过，认同率 `0.95` | PASS |
| `/bpi/#/shadowRuns` | 结束观察 | `POST /bpi-api/shadow-runs/{id}/complete` | `200`，`EVALUATING/r13` | PASS |
| 验收抽屉 | CRITICAL 未解决时尝试批准 | `POST /bpi-api/shadow-runs/{id}/approve` | `422 UNRESOLVED_CRITICAL_DATA_QUALITY`，revision 未改变 | PASS |
| `/bpi/#/dataQuality` | 认领、指派并解决事件 | `acknowledge`、`resolve` | 事件 `RESOLVED/r3`，原始事实与审计保留 | PASS |
| `/bpi/#/shadowRuns` | 独立管理员批准 | `POST /bpi-api/shadow-runs/{id}/approve` | `200`，`APPROVED/r14` | PASS |

创建人是 marker 专用工程师主体，决定人是经旧 ADP 会话验证后规范化的
`legacy-ticket:admin`，二者不同，四眼控制生效。浏览器捕获 57 个 BPI 响应，其中
51 个 GET、6 个 POST，均为 2xx；`consoleErrors=0`、`pageErrors=0`、
`requestFailures=0`。

![影子运行最终批准页面](../../metadata/bpi-shadow-run-acceptance.png)

## PostgreSQL 落库

Flyway V20 新增：

- `bpi.bpi_shadow_runs`：固定验收 scope、规则、拓扑、点位目录、门槛、状态和责任人。
- `bpi.bpi_shadow_run_batch_reviews`：保存每次人工复核，重复复核以 supersede 留痕而非删除。
- 单产线仅允许一个 `RUNNING` 任务，同一批次仅允许一个 `ACTIVE` 复核。
- 全部写动作要求 `Idempotency-Key` 与 `If-Match`，批准要求独立 `BPI_ADMIN`。

最终证据采集前执行的核心直查：

```sql
SELECT id, state, revision, created_by, started_by,
       completed_by, decided_by,
       floor(extract(epoch FROM (completed_at - started_at)))::bigint
FROM bpi.bpi_shadow_runs
WHERE tenant_id = '1000'
  AND run_code = 'ADP_E2E_SHADOW_20260720_0152_V20';

SELECT count(*) FILTER (WHERE state = 'ACTIVE') AS active_reviews,
       sum(start_boundary_accepted::integer + end_boundary_accepted::integer)
         FILTER (WHERE state = 'ACTIVE') AS accepted_boundaries,
       count(*) FILTER (WHERE state = 'ACTIVE') * 2 AS total_boundaries,
       abs(sum(automatic_quantity) - sum(reference_quantity))
         / nullif(sum(reference_quantity), 0) * 100 AS cumulative_deviation
FROM bpi.bpi_shadow_run_batch_reviews
WHERE tenant_id = '1000'
  AND shadow_run_id = '82194684-b67e-4307-a285-fabcd6def41e';

SELECT action, actor_id, before_revision, after_revision
FROM bpi.bpi_audit_events
WHERE tenant_id = '1000'
  AND object_id = '82194684-b67e-4307-a285-fabcd6def41e'
ORDER BY created_at, id;

SELECT state, response_status, count(*)
FROM bpi.bpi_api_idempotency
WHERE tenant_id = '1000'
  AND idempotency_key LIKE 'ADP_E2E_SHADOW_20260720_0152_V20%'
GROUP BY state, response_status;
```

实际结果：

- 运行最终为 `APPROVED/r14`，受控观察时长 `691202` 秒。
- 10 个有效复核，19/20 个边界通过，认同率 `0.95`。
- 自动量与参考量均为 `1000 t`，累计、平均和最大偏差均为 `0%`。
- CRITICAL 事件最终为 `RESOLVED/r3`，未解决关键事件数为 0。
- 16 条关联审计记录，10 条 `COMPLETED/200` 幂等记录；预期 `422` 未伪装成成功写入。
- 10 个来源批次仍全部为 `CLOSED_RAW`、`qualityGate=NOT_APPLICABLE`、
  `wmsStatus=NOT_REQUESTED`，没有 WOM、QCS、WMS、PLC 或 DCS 生产写回。

证据固化后运行 marker 范围清理脚本，影子运行、复核、事件、批次、规则、拓扑、
目录、校准、outbox 和幂等记录全部剩余 0。

## 目标环境修复

真实验收发现并修复了两个“源码正确、部署制品过旧”的阻断问题：

1. 首个 Java 17 镜像仍引用 V17 已改名的 `snapshot.ready_point_count`，创建请求返回
   `500 SQLGrammarException`。重新编译当前源码后，真实 PostgreSQL 专测通过，字节码不再
   包含旧字段；部署镜像为 `ft-mes-bpi-service:20260720-shadow-run-v20-r2`。
2. 首个 Java 8 adapter 镜像不含影子运行路由，页面 `GET /bpi-api/shadow-runs`
   返回 `403 BPI route is not allowlisted`。20 条 adapter 测试通过并重新打包后，部署镜像为
   `ft-mes-bpi-adapter:20260720-shadow-run-v20-r2`。

两个容器最终均为 `healthy`，验收后 15 分钟范围内 `ERROR` 日志均为 0。V20 迁移前备份为
`/home/v6/adp-backups/ft_mes_bpi-pre-v20-20260720-014559.dump`，SHA-256 为
`5c0b75291db7ff6a9beb1bfba3cdb8465709401b1980f11a0d629ca585d8bffc`。

## 本地回归

- Java 17：事件契约 17、规则运行时 9、service 66，共 92 条；0 失败、0 错误、1 条需目标
  Kafka marker 的生产器测试按设计跳过。
- Java 8 adapter：20/20。
- 模拟器：10/10。
- Chromium 确定性页面 E2E：14/14。
- 前端生产构建和 BPI API/service/adapter/UI 静态契约门禁：PASS。

全量 Java 17 首次并跑曾因多个缓存 Spring/Kafka 上下文各自使用 10 个 Hikari 连接，耗尽
本地 PostgreSQL `max_connections=100`。Surefire 现仅在测试进程把池限制为 2；生产配置默认池
没有改变，第二次全量回归通过。

## 未闭合边界

- 本次 8 天是数据库时间压缩，不是选定产线真实连续 7-14 天运行证据。
- 规则、拓扑、目录、事件和 10 个批次是受控 fixture，不是实时 IoT 与 WOM 同 marker 事实。
- 当前试点仍缺精确匹配的现场标定、连续单调 DEVICE/GATEWAY 来源序列和真实 END 边界。
- QCS/WMS 幂等写回、异常补偿、完工入库和批次谱系仍属于后续阶段。

因此这次关闭的是“影子运行产品交互、API、门槛和 PostgreSQL 审计是否可用”，没有关闭
“现场生产验证是否完成”。
