# BPI Kafka Cluster Replay Acceptance

## 状态

当前状态为 **HARNESS_READY / CLUSTER_BLOCKED_DISK**。本地 Java 17 场景、契约和筛选测试
通过；测试机根文件系统仍为 100%，因此没有拉取镜像或启动 Kafka/Flink，不能声明实机 PASS。

## 回放链

```text
unique ADP_E2E marker
  -> bpi.boundary.rule-publication.v1
  -> mes.production.context.v1
  -> iot.telemetry.selected.v1 (3 events)
  -> Flink BpiKafkaJob
  -> checkpoint commits exactly-once Kafka transaction
  -> bpi.batch.candidate.v1 (exactly one matching candidate)
  -> bpi.data-quality.v1 (zero matching issues)
  -> same rule version ACTIVE -> INACTIVE cleanup
```

`BpiKafkaAcceptanceReplay` 使用 `read_committed` 独立 consumer group，只从执行开始时的 topic
尾部读取。结果按 tenant、plant、line、rule code 和 order marker 五重过滤，避免旧数据造成假
PASS。成功报告包含每个输入 event、`INACTIVE` 清理 event 和候选的 topic/partition/offset、
candidate key、证据 event ID、Flink job ID 和最近完成 checkpoint ID。

## 本地验收

| 项目 | 结果 |
|---|---|
| 规则、上下文、遥测 fixture 契约 | PASS |
| marker 候选与数据质量隔离 | PASS |
| 配置缺失 fail-closed | PASS |
| 新增 replay tests | 4/4 PASS |
| streaming module tests | 75/75 PASS |
| streaming reactor total | 98/98 PASS |

## 实机命令

```bash
make bpi-stream-cluster-replay
```

该命令先执行 cluster smoke，要求 Flink `RUNNING` 且已有成功 checkpoint；随后执行 replay，
并将两份证据合并。它证明 Kafka -> Flink -> Kafka 数据面，但 PostgreSQL marker 仍需候选
consumer/受控桥接、BPI inbox 和数据库直查闭合后才能验收。
