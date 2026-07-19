# BPI 单 Broker 故障恢复验收

## 结论

2026-07-19，目标测试环境 `10.11.100.17` 的 `ft-mes-bpi-streaming` 完成单 broker
故障恢复演练，状态为 `PASS`。

验收 marker：`ADP_BPI_BROKER_CHAOS_20260719_1129`。

本次真实停止 `kafka-2`，不是仅检查配置或模拟异常。故障期间 Kafka 仍保留两个 ISR，唯一 marker
能够以 `acks=all` 写入并只消费一次，Flink 作业始终为 `RUNNING` 且 checkpoint 成功推进；恢复后
全部分区回到三 ISR，标准集群 smoke 再次通过。

机器记录见
[`metadata/bpi-broker-failure-recovery-acceptance.json`](../../metadata/bpi-broker-failure-recovery-acceptance.json)。

## 验收结果

| 验收项 | 实际结果 | 状态 |
|---|---|---|
| 演练前集群 smoke | 3 broker、12 个核心 topic、RF=3、minISR=2；Flink checkpoint `2476` | PASS |
| Broker 故障 | `docker compose stop kafka-2`，真实容器停止 | PASS |
| 分区可用性 | 151 个用户分区中 unavailable=`0`、低于 minISR=`0` | PASS |
| 预期降级 | 故障期间 151 个分区均由 ISR=3 降为 ISR=2 | PASS |
| 故障期间写入 | `bpi.acceptance.broker-chaos.v1` offset `1 -> 2`，`acks=all` | PASS |
| 故障期间读取 | marker 精确出现 1 次 | PASS |
| Flink 连续性 | job `0a2dd090eb290f82d052fc7c0465311f` 始终 `RUNNING` | PASS |
| Checkpoint | `2481 -> 2482 -> 2483`，failed checkpoint 始终为 `0` | PASS |
| Broker 恢复 | `kafka-2` 恢复 healthy，151 个分区 under-replicated=`0` | PASS |
| 演练后集群 smoke | 3 broker 和 12 个核心 topic 全部通过，checkpoint 推进到 `2485` | PASS |
| 页面/数据库 | 本演练只验证基础设施可用性，不触发业务页面和 PostgreSQL 写入 | NOT_APPLICABLE |

总恢复演练耗时 52 秒。这里的耗时包含故障探测、marker 写读、故障期间 checkpoint、broker 启动、
ISR 补齐和恢复后 checkpoint，并不等价于生产 RTO 承诺。

## 自动恢复保护

新增脚本：

```text
deploy/bpi-streaming/scripts/run-broker-failure-recovery.sh
```

脚本只允许 `kafka-1`、`kafka-2` 或 `kafka-3`，并在任何错误、信号或中断时通过 trap 启动被停止的
broker 并等待健康。演练前必须满足所有分区 ISR=3；故障期间任何 leader 丢失或 ISR 低于 2 都立即
失败；恢复后必须重新达到 ISR=3。

第一次受控预演没有生成 PASS 报告，恢复保护仍将 `kafka-2` 拉回 healthy，Flink 保持 RUNNING；
调整 KRaft ISR 收敛等待后，使用上述唯一 marker 完成正式验收。只有正式 PASS 报告进入仓库证据。

复验入口：

```bash
BPI_CHAOS_BROKER_SERVICE=kafka-2 \
BPI_BROKER_CHAOS_MARKER=ADP_BPI_BROKER_CHAOS_YYYYMMDD_HHMMSS \
make bpi-stream-broker-failure-recovery
```

验收 topic `bpi.acceptance.broker-chaos.v1` 使用 RF=3、minISR=2、禁止 unclean leader election 和
24 小时 retention；它不接入 BPI 业务消费者。

## 原始证据与剩余边界

目标机证据目录：

```text
/data/docker/bpi-upgrade-backups/20260719-bpi-broker-chaos
```

其中保留 baseline smoke、正式 marker JSON/log 和 post smoke。报告不包含密码、token 或数据库连接串。

本次只闭合单 broker 故障，不证明：

- 两台 broker 同时故障仍可写；minISR=2 下本就应失败关闭；
- JobManager HA、跨主机 Kafka/Flink 容灾或机房级故障恢复；
- service、adapter、Flink 应用镜像回退和 BPI 整体产品回退；
- 真实现场点位 READY、同 scope candidate/batch 或 7-14 天影子运行。
