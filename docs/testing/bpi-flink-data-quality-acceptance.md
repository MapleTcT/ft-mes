# BPI Flink 自动数据质量全链验收

验收时间：2026-07-19

状态：`PASS_TARGET_FLINK_KAFKA_POSTGRES_BROWSER_CLEANUP`

最终 marker：`ADP_E2E_DQ_FLINK_20260719_2344_3ca0fff3`

机器记录：[`metadata/bpi-flink-data-quality-acceptance.json`](../../metadata/bpi-flink-data-quality-acceptance.json)

## 结论

目标测试环境 `10.11.100.17` 已真实闭合：

`selected telemetry -> Flink telemetry-data-quality operator -> bpi.data-quality.v1 ->`
`Java 17 consumer -> PostgreSQL V19 -> Java 17 API -> Java 8 adapter -> 真实 BPI 页面`。

Flink 对唯一 marker 自动产生且只产生以下四类事件：

| issue code | severity | Kafka partition/offset | PostgreSQL |
|---|---|---:|---|
| `SOURCE_SEQUENCE_GAP` | `WARNING` | `3/4` | `OPEN/r1/event_count=1` |
| `POINT_QUALITY_BAD` | `ERROR` | `1/8` | `OPEN/r1/event_count=1` |
| `CLOCK_DRIFT` | `ERROR` | `4/14` | `OPEN/r1/event_count=1` |
| `SOURCE_SEQUENCE_DUPLICATE` | `INFO` | `2/10` | `OPEN/r1/event_count=1` |

该结果关闭“真实 Flink 作业尚未作为数据质量自动 producer 验收”的缺口。它不证明现场点位已
READY，也不替代真实设备连续来源序列、真实校准、同 scope candidate/batch、7-14 天影子运行和
QCS/WMS 写回，因此 G-021 继续保持 `PARTIAL`。

## 源码与有状态升级

| 项目 | 实际结果 |
|---|---|
| 自动检测实现 | `ddec7709aae01c1f1217fdf8d372d320a8e03a7f` |
| 最终回放修复 | `3ca0fff33a3b88b15d27f1f18c6c6b4679715440` |
| 回放运行隔离 | `a680844e`，一次性 JAR 不再改写生产 Compose 期望状态 |
| 原运行 JAR | `993b99ff`，SHA-256 `10ddc3dc...e32f` |
| 升级 JAR | `ddec7709`，SHA-256 `e39002ce...e06f` |
| canonical savepoint | `s3://ft-mes-bpi-checkpoints/savepoints/savepoint-990e6d-74b13f8ce600` |
| 有状态恢复 | `allowNonRestoredState=false`，`33 -> 36` tasks，恢复后 checkpoint 成功 |
| 当前作业 | `1e981b842f4693e49f3c3def0fb98cb6`，`RUNNING 36/36` |
| 当前 checkpoint | `3937`，completed `53`，failed `0` |

生产作业当前 JobManager 使用 `1c5a2d02` JAR，SHA-256
`fc451b4dd287ef1eedbec369b5f098ea24d95cd7848c8f79ddb8444bb6efc6bb`。最终回放容器使用
`3ca0fff3` JAR，SHA-256
`e384ad2e303c8708c0e88d36c86877298e611029e7ca3332b071a712ee9015d1`；两者的生产拓扑和检测器
相同，差异只在验收 helper 的共享产线上下文修订逻辑。

## 回放与作业连续性

最终范围为 `tenant=1000 / plant=PLANT-01 / line=LINE-S07-01`。回放只写入一个活动上下文、
四条遥测和一个关闭上下文，不直接写数据质量 topic。报告证明：

- 输入遥测位于 partition `5` offsets `0..3`；
- 四条输出均带 `stage=telemetry-data-quality`；
- 输出 consumer 使用 `read_committed`；
- 执行前后 job ID 均为 `1e981b842f4693e49f3c3def0fb98cb6`；
- checkpoint 从 `3906` 推进到 `3908`，未重启或回退作业；
- 本地 Java 17 全量构建为 `120 tests / 0 failures / 0 errors / 1 env-gated skip`。

验收过程中真实发现并修复了三项编排缺陷：

1. scope 环境变量未传入容器，导致一次诊断 marker 使用默认 `TENANT-E2E`；脚本现显式传入并反查报告 scope。
2. Compose replay 依赖会因 JAR 路径变化重建 JobManager；脚本现强制 `--no-deps`，并比较前后 job ID/checkpoint。
3. 共享产线已有较高 revision 的关闭上下文，固定 revision `1/2` 会产生 `CONTEXT_WAIT_EXPIRED`；
   回放现沿用 epoch-second revision，并测试旧关闭上下文、新活动上下文和验收关闭顺序。

## PostgreSQL 落库

清理前直接查询 `ft_mes_bpi`，结果为：incident `4`、raw event `4`、inbox `4`、`CREATED`
action `4`、audit `4`。四条 incident 均为 marker 设备、`OPEN/r1/event_count=1`，点质量事件的
`property_id=flow`，四条 raw event 的 `headers.stage` 均为 `telemetry-data-quality`。

核心复验 SQL：

```sql
SELECT issue_code, severity, state, revision, event_count,
       device_id, property_id, last_source_event_id
FROM bpi.bpi_data_quality_incidents
WHERE tenant_id = '1000'
  AND line_id = 'LINE-S07-01'
  AND last_source_event_id LIKE
      'ADP_E2E_DQ_FLINK_20260719_2344_3ca0fff3-%'
ORDER BY issue_code;

SELECT event_id, source_event_id, severity, headers ->> 'stage'
FROM bpi.bpi_data_quality_incident_events
WHERE tenant_id = '1000'
  AND source_event_id LIKE
      'ADP_E2E_DQ_FLINK_20260719_2344_3ca0fff3-%';
```

## 真实页面

浏览器访问 `http://10.11.100.17:18080/bpi/`，真实登录 HTTP `200`。脚本通过 API 先证明四条
incident 和四条 raw fact，再进入“数据质量”页面，以 marker 筛选后精确得到 4 行并打开
`POINT_QUALITY_BAD` 详情抽屉。

| 检查 | 结果 |
|---|---|
| 数据质量 API | 5 个请求全部 `2xx` |
| 页面 marker 行 | 4 |
| 详情 raw event | 1 |
| console errors | 0 |
| page errors | 0 |
| request failures | 0 |

浏览器报告 SHA-256：`79f9993e5cc13638e4e6be9da08d1305754322f4e11f8e65285cd58bbdaea6d8`。

截图 SHA-256：`f650763c919aab5dc57f761dfdcc30f1117c69ce80fa29b7e052f2a9d135df54`。

运行报告和截图保存在验收机 `/tmp`，仓库不提交 ticket、密码、数据库 dump 或运行 JAR。

## 退场与 DLQ

验收结束后，BPI data-quality consumer 已恢复：

```text
BPI_DATA_QUALITY_KAFKA_ENABLED=false
BPI_DATA_QUALITY_KAFKA_ALLOWED_TENANT_IDS=_DENY_ALL_
BPI_DATA_QUALITY_KAFKA_ALLOWED_PLANT_IDS=_DENY_ALL_
BPI_DATA_QUALITY_KAFKA_ALLOWED_LINE_IDS=_DENY_ALL_
consumer group state=Empty, members=0
```

三个实际入库诊断/最终 marker 共 `13` 组 incident/raw/action/audit/inbox 已在一个事务内按
`source_event_id` 和 incident UUID 定向删除；复查 incident、raw、inbox、audit marker、API
idempotency 和 orphan action 均为 `0`。Kafka 源消息按追加日志语义保留。

DLQ 当前保留 4 条诊断记录，全部来自 scope 透传修复前的 marker
`ADP_E2E_DQ_FLINK_20260719_2332_1c5a2d02`，拒绝原因均为
`outside the configured tenant/plant/line scope`。最终 marker 在 DLQ 中为 `0`。未删除 topic 或截断
分区来美化数字；这 4 条记录保留为 fail-closed 诊断证据，并按 30 天 retention 自然过期。

Kafka consumer group 显示每分区 lag `1`，对应 Flink exactly-once 事务控制批次；各分区 committed
offset 已位于最后一条业务记录之后，`read_committed` 没有待处理业务事件。

## 剩余边界

- 用现场真实 DEVICE/GATEWAY `source_epoch + sequence` 和校准证据把试点点位提升为 READY。
- 让真实 IoT 遥测与 MES production context 在同一 scope 形成 candidate/batch。
- 连续运行 7-14 天，验收告警准确率、边界人工认同率、累计量偏差、RocksDB 状态增长和运维值守。
- 完成生产等价跨组件回切与业务签字后，才进入 QCS/WMS 幂等写回。
