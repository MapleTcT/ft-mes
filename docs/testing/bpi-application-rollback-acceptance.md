# BPI 应用组件回滚验收

## 结论

2026-07-19 在测试机 `10.11.100.17` 完成 BPI 应用组件真实回滚演练，结论为
`PASS_APPLICATION_COMPONENT_ROLLBACK`。验收覆盖 Java 17 service、Java 8 adapter 和有状态 Flink
job；三者均回退到上一版制品并恢复到演练前制品。PostgreSQL 保持 Flyway V16，不执行 schema
降级、删表或清库。

机器证据：

- [`metadata/bpi-runtime-image-rollback-acceptance.json`](../../metadata/bpi-runtime-image-rollback-acceptance.json)
- [`metadata/bpi-flink-job-rollback-acceptance.json`](../../metadata/bpi-flink-job-rollback-acceptance.json)

该结论关闭 G-021 的 service/adapter/Flink 应用组件回退缺口，不等于完整产品回切或生产投用完成。
跨组件同时回切、真实业务负载、现场 IoT 来源、7-14 天影子运行和 QCS/WMS 写回仍需单独验收。

演练退场后的独立复核确认：service/adapter 保持演练前精确 image ID 且均为 healthy，BPI 页面 200，
JobManager 挂载当前 `993b99ff` JAR，唯一 Flink job `990e6d0d610ebe623f6845706d13f383`
为 `RUNNING 33/33`，标准集群 smoke 再次 PASS 并把成功 checkpoint 推进到 `2589`。

## Runtime 镜像回滚

marker：`ADP_BPI_RUNTIME_ROLLBACK_20260719_120102`

| 阶段 | Service | Adapter | 健康 | Flyway/核心表摘要 | 页面/API | 状态 |
|---|---|---|---|---|---|---|
| 演练前 | `ft-mes-bpi-service:20260719-repeat-observation` | `ft-mes-bpi-adapter:20260718-v15b` | 均为 healthy | `16\|5\|5\|0\|0\|0\|0\|0\|0\|22` | `/bpi/` 200 | PASS |
| 回退 | `ft-mes-bpi-service:20260718-07b26131` | `ft-mes-bpi-adapter:20260718-v15` | 均为 healthy | 与演练前完全一致 | 真实登录 200；`/#/points` 显示“点位目录”；`GET /bpi-api/point-catalog/current` 200；console/page/request error 均为 0 | PASS |
| 恢复 | 演练前 tag 和 image ID | 演练前 tag 和 image ID | 均为 healthy | 与演练前完全一致 | 同一页面/API 再次 200；三类浏览器错误均为 0 | PASS |

数据库摘要依次表示 Flyway 版本，以及 point snapshot、point entry、topology、rule、approval、candidate、
batch、state event、audit 的行数。三阶段值完全一致，证明本次应用镜像往返没有冒充业务写入，也没有
通过 schema 降级绕过兼容问题。

## Flink 有状态回滚

marker：`ADP_BPI_FLINK_ROLLBACK_20260719_120659`

| 阶段 | JAR / SHA-256 | Savepoint | Job / checkpoint | 状态 |
|---|---|---|---|---|
| 当前版本抓取 | `bpi-stream-engine-993b99ff-job.jar` / `10ddc3dc...32f` | `savepoint-0a2dd0-6ea7eff0e1f0` | `0a2dd090...311f` / 2558 | PASS |
| 上一版恢复 | `bpi-stream-engine-ec850619-job.jar` / `b74f25af...1f41` | 从当前版本 savepoint 恢复 | `c866ffcd...b557` / 恢复后 2561 | PASS |
| 上一版抓取 | 同上一版 JAR | `savepoint-c866ff-a510d1adf61b` | checkpoint 2562 | PASS |
| 当前版本恢复 | 当前 JAR 和完整 SHA-256 | 从上一版 savepoint 恢复 | `990e6d0d...f383` / 恢复后 2564 | PASS |

两次恢复均保持 `allowNonRestoredState=false`，点位目录 source 和 runtime-readiness sink 都存在，
集群 smoke 通过。标准 smoke 覆盖的 12 个配置内 BPI 业务 topic 均无欠副本；savepoint 报告中的
`businessTopicCount=13` 是“全部非内部 topic”旧字段口径，额外包含保留的 broker-chaos 验收 topic。
演练只改变 Flink 应用制品和状态，数据库落库验收为 `NOT_APPLICABLE`。

## 保护与复验

两个入口都要求显式确认，拒绝相对备份目录或同一版本制品。Flink 脚本在异常、`HUP`、`INT`、
`TERM` 时使用退出守卫恢复当前 JAR；runtime 脚本无论回退阶段是否成功都会在 `finally` 中恢复演练前
service/adapter 镜像。环境备份权限为 `0600`，备份目录权限为 `0700`。

```bash
BPI_RUNTIME_ROLLBACK_CONFIRM=ROLLBACK_BPI_RUNTIME_IMAGES_AND_RESTORE \
BPI_ROLLBACK_SERVICE_IMAGE=<previous-service-image> \
BPI_ROLLBACK_ADAPTER_IMAGE=<previous-adapter-image> \
  make bpi-runtime-image-rollback-rehearsal

BPI_FLINK_ROLLBACK_CONFIRM=ROLLBACK_BPI_FLINK_JOB_AND_RESTORE \
BPI_FLINK_ROLLBACK_JAR=<absolute-previous-job-jar> \
BPI_FLINK_ROLLBACK_BACKUP_DIR=<absolute-protected-directory> \
  make bpi-stream-flink-rollback-rehearsal
```

生产发布前仍须在生产等价环境完成跨组件编排、真实业务 marker、故障中断恢复、流量切换和业务签字；
本报告不能替代这些门槛。
