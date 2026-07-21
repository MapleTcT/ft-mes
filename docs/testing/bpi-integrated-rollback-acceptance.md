# BPI 整栈回滚验收

## 结论

2026-07-21 在测试环境 `10.11.100.17` 完成 BPI Service、Java 8 Adapter 和 Flink
有状态作业的同窗回滚、真实业务写入与精确恢复，结论为
`PASS_INTEGRATED_ROLLBACK_CONTROLLED_MARKER`。

- marker：`ADP_BPI_INTEGRATED_ROLLBACK_20260721_2054`
- 产线：`1000 / PLANT-01 / LINE-IRB-_20260721_2054`
- 生产指令：`MO-ADP_BPI_INTEGRATED_ROLLBACK_20260721_2054`
- 候选：`71aa3c32-2505-577f-8f5d-8e726dd5fdc1`
- 影子批次：`28fa5580-eeaf-5461-b81c-21e6a982c5a0`
- 机器证据：[`metadata/bpi-integrated-rollback-acceptance.json`](../../metadata/bpi-integrated-rollback-acceptance.json)

该结果关闭 G-021 的“真实候选/批次负载下跨组件整体回切”软件缺口，不代表物理设备、正式校准、
连续 7-14 天现场影子运行、外部 ERP/WMS 或生产切换已经完成。

## 版本矩阵

| 组件 | 当前版本 | 回滚版本 | 恢复结果 |
| --- | --- | --- | --- |
| BPI Service | `ft-mes-bpi-service:20260721T092245-40a2c642ffdb` | `ft-mes-bpi-service:20260721T081611-8b3c1fa56207` | 精确恢复当前 tag/image ID，`healthy` |
| BPI Adapter | `ft-mes-bpi-adapter:20260721T125202Z-d63ea4b0ad70` | `ft-mes-bpi-adapter:20260720T212943Z-1962f599b3ea` | 精确恢复当前 tag/image ID，`healthy` |
| Flink | `bpi-stream-engine-308cca82-job.jar`，SHA-256 `7fbcacf8...efa11b` | `bpi-stream-engine-13b1296c-job.jar`，SHA-256 `dc0271cb...d9c7fe` | 从 savepoint 恢复当前 JAR，`RUNNING 36/36` |
| PostgreSQL | Flyway `V24` | 不降级 | schema 和业务事实保持不变 |

当前 Adapter 额外返回只读诊断头 `X-BPI-Adapter-Contract-Version: 1`，用于证明恢复后的
运行镜像确为 `d63ea4b0`。回滚 Adapter 选用 `1962f599`，因为它是包含当前批次页
`force-close` 读写路由的最近兼容版本；更早的 `bcf40e2f` 不满足当前前端契约，不能作为本次回滚基线。

## 验收链路

| 阶段 | 实际操作 | 页面/API/PostgreSQL 结果 | 状态 |
| --- | --- | --- | --- |
| 前置检查 | 读取当前镜像、JAR、Flink、Flyway、开关和 marker | Service/Adapter `healthy`，Flink `36/36`，Flyway `24`，marker 为零 | PASS |
| 同窗回滚 | 捕获当前 savepoint，切换旧 Flink JAR、旧 Service 和兼容旧 Adapter | 三组件同时运行；临时 Adapter scope 只开放本次动态产线 | PASS |
| Kafka/Flink 回放 | 发布目录、规则、生产上下文和 3 条遥测 | 规则先到 `READY`；恰好 1 个 START candidate；数据质量事件 0；退役事件为 `APPLIED` | PASS |
| PostgreSQL 首次落库 | 读取候选 | `PENDING/r1`，批次为 0 | PASS |
| 真实浏览器确认 | `/bpi/#/candidates` 点击确认 | `POST /bpi-api/candidates/{id}/confirm` 为 200；候选 `CONFIRMED/r2` | PASS |
| 批次落库 | 打开批次档案、时间线、证据、release 和 force-close 读取 | 影子批次 `ACTIVE/r1`；边界证据、状态事件、审计各 1；全部 API 200 | PASS |
| 恢复当前栈 | 旧栈 savepoint 后恢复当前 Flink，再恢复当前 Service/Adapter | 当前三组件健康；同一候选、批次 ID、状态、revision 和计数未变 | PASS |
| 恢复后浏览器复验 | `/bpi/#/batches` 精确读取同一批次 | 批次档案、START 证据、时间线和 release 全部 200；浏览器错误为 0 | PASS |
| 清理 | 只按唯一 marker 清理 | 拓扑、规则、候选、批次、inbox、开关、审计、目录快照均为 0 | PASS |
| 最终核验 | 恢复原 Adapter scope 和全部默认关闭开关 | scope=`admin=1000\|PLANT-01\|LINE-S07-01`；六开关 false；watchdog 已停止且未触发 | PASS |

旧栈确认页和恢复后的批次页均无 console、page 或 request error：

- [`metadata/bpi-integrated-rollback-old-stack.png`](../../metadata/bpi-integrated-rollback-old-stack.png)
- [`metadata/bpi-integrated-rollback-restored-stack.png`](../../metadata/bpi-integrated-rollback-restored-stack.png)
- [`metadata/bpi-integrated-rollback-cleanup.png`](../../metadata/bpi-integrated-rollback-cleanup.png)

## 数据不变性

回滚栈确认后和当前栈恢复后的 PostgreSQL 摘要完全一致：

| 对象 | 回滚栈 | 恢复栈 |
| --- | ---: | ---: |
| topology/rule | `1 / 1` | `1 / 1` |
| candidate/batch | `1 / 1` | `1 / 1` |
| boundary evidence/state event/audit | `1 / 1 / 1` | `1 / 1 / 1` |
| candidate | `CONFIRMED/r2` | `CONFIRMED/r2` |
| batch | `ACTIVE/r1/SHADOW` | `ACTIVE/r1/SHADOW` |

清理后的独立 precheck 又确认 Flyway `24`、marker 五类核心记录为 `0`，Flink checkpoint
继续从 `9010` 推进到 `9013`。数据库未执行 downgrade 或破坏性 schema 操作。

## 复验入口

运行前必须提供已审阅的旧镜像、旧 JAR、当前回放客户端 JAR和测试凭据：

```bash
ADP_SSH_HOST=10.11.100.17 \
ADP_BASE_URL=http://10.11.100.17:18080 \
ADP_USERNAME='<test-user>' \
ADP_PASSWORD='<secret>' \
BPI_ROLLBACK_SERVICE_IMAGE='<reviewed-service-image>' \
BPI_ROLLBACK_ADAPTER_IMAGE='<contract-compatible-adapter-image>' \
BPI_ROLLBACK_JOB_JAR='<absolute-reviewed-old-jar>' \
BPI_LOAD_CLIENT_JOB_JAR='<absolute-reviewed-load-client-jar>' \
make bpi-integrated-rollback-rehearsal
```

脚本使用唯一动态产线、受控 published fixture、双 savepoint、20 分钟 watchdog、精确镜像/JAR
校验和 marker 定向清理。不得用固定共享产线、数据库重置、忽略浏览器 4xx/5xx 或不兼容 Adapter
来换取通过结果。
