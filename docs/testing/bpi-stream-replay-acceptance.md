# BPI 流式回放候选验收

## 结论

`streaming/bpi-stream-engine` 已建立 Java 17/Flink 2.2.1 流式计算工程骨架，并复用
`batch-rule-runtime` 的事件时间判定公式和 `bpi-events` 的 Protobuf 契约。当前里程碑实现的是
单执行上下文确定性回放和 START/END 候选投影，不是生产 Kafka/Flink 作业。

自动测试结果为 **3/3 PASS**；连同依赖模块，本次 Reactor 共验证事件契约 13 项、规则运行时
8 项和流式回放 3 项。

## 已验收能力

| 场景 | 预期 | 结果 |
|---|---|---|
| 乱序输入列表 | 按 event time 和 event id 确定性排序 | PASS |
| START 候选 | 使用生产指令和 first-quorum evidence 生成稳定 candidate key | PASS |
| END 候选 | 使用现有 batch id 和 first-quorum evidence 生成稳定 candidate key | PASS |
| 重放同一输入 | 只产生一个完全相同的 `BatchCandidateV1` | PASS |
| 证据投影 | 只包含 TRUE 证据并稳定排序、去重 | PASS |
| 事件时间 | boundary/emitted 时间均来自事件时间，不依赖系统时钟 | PASS |
| 水位线倒退 | final watermark 早于最后事件时拒绝执行 | PASS |
| 契约校验 | START/END 输出均通过 `BpiContractValidator` | PASS |

## 运行方式

```sh
JAVA_HOME=/path/to/jdk17 make bpi-stream-test
```

该目标只在 Java 17 CI 作业中编译。仓库 Java 8 主门禁执行 `bpi-stream-static-check`，因此不会
把 Flink 或 Java 17 字节码引入旧 MES Maven Reactor。

## 尚未覆盖

- Flink `KeyedBroadcastProcessFunction`、版本化 `ValueState<byte[]>` 和 event-time timer。
- Kafka Protobuf source/sink、分区键、exactly-once checkpoint 与 savepoint 恢复。
- allowed lateness 内的迟到修正和已确认批次的冲突处置。
- IoT MQTT/Kafka 真实回放、BPI inbox、PostgreSQL marker 和浏览器确认闭环。
