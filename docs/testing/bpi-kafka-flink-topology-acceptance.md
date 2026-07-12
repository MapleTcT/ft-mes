# BPI Kafka/Flink 作业拓扑验收

## 结论

`streaming/bpi-stream-engine` 已组装 Java 17 / Flink 2.2.1 的可部署 Kafka 作业入口
`BpiKafkaJob`。本里程碑状态为 **JOB_WIRED / HARNESS_PASS**：生产数据面、控制面、候选输出和
数据质量输出已经在作业图中闭合，但尚未在真实 Kafka/Flink 集群完成运行验收。

当前 Java 17 streaming module 为 **75/75 PASS**；连同事件契约 14 项和规则运行时 9 项，
本次 Reactor 共 **98/98 PASS**。其中新增 4 项覆盖唯一 marker 的集群 replay fixture、范围过滤、
fail-closed 配置和机器可读报告；实机 replay 仍受测试机磁盘阻断。

## 作业链路

```text
iot.telemetry.selected.v1 -> BPKR/v1 -> TelemetryEnvelopeV1 validation -> BPTE/v1
mes.production.context.v1 -> BPKR/v1 -> ProductionContextEventV1 validation
    -> tenant|plant|line event-time join -> BPCT/v1 -> timestamp reassignment
bpi.boundary.rule-publication.v1 -> immutable lifecycle -> indexed broadcast routes
    -> scoped rule update BPRU/v1 -> keyed boundary evaluator
    -> bpi.batch.candidate.v1 (Kafka exactly-once sink)

decode/join/routing/evaluation issues
    -> DataQualityEventV1 -> bpi.data-quality.v1 (Kafka exactly-once sink)
```

## 已验收约束

- Kafka source 保留 topic、partition、offset、timestamp、key 和 tombstone，解码失败不会丢失定位证据。
- telemetry envelope 级错误整体隔离；单个非法 point 只隔离该 point，其余合法点继续处理。
- 三条 source 使用按 lane 区分且跨重启稳定的 consumer group、`read_committed` 和
  committed-offset/earliest fallback；`deployment-id` 只参与事务 producer 身份，不改变消费位点。
- telemetry、context、rule publication 都配置 event-time watermark 和 source idleness。
- 上下文迟到补齐后，拓扑从 `BPCT/v1` 中重新赋 telemetry point 时间，禁止继承 context 时间。
- 规则生命周期使用 checkpointed keyed state；同版本只允许 `ACTIVE -> INACTIVE`，重新启用必须新版本。
- 规则引用使用 `tenant|plant|line|ruleCode|ruleVersion`，同名规则不会跨租户或产线冲突。
- device/property 路由使用 broadcast index；单点只访问命中的规则列表，不扫描全部规则。
- evaluator key 包含 tenant、plant、line、locality、boundary kind 和 scoped rule identity，多规则窗口不共享状态。
- context join state TTL 为 retention+wait；边界窗口 state TTL 可配置，规则发布时强制
  `evaluationTimeout + allowedLateness < TTL`，过期 state 不会被读取。
- 算子间数据使用 `BPKR/v1`、`BPTE/v1`、`BPCT/v1`、`BPRU/v1` 和 Protobuf bytes，不传生成的
  Protobuf Java 对象。
- checkpoint 模式为 `EXACTLY_ONCE`，候选和数据质量 sink 使用不同、带 deployment id 的事务前缀。
- source、watermark、join、rule lifecycle、routing、evaluator 和 sink 都有稳定 UID，支持后续 savepoint 映射。

## 配置入口

命令行使用 `--key=value` 或 `--key value`，环境变量与主要参数如下：

| 环境变量 | 参数 | 默认/要求 |
|---|---|---|
| `BPI_KAFKA_BOOTSTRAP_SERVERS` | `bootstrap-servers` | 必填 |
| `BPI_DEPLOYMENT_ID` | `deployment-id` | 必填；并行/蓝绿作业间唯一，恢复同一作业时保持稳定 |
| `BPI_KAFKA_GROUP_PREFIX` | `group-prefix` | `ft-mes-bpi` |
| `BPI_TELEMETRY_TOPIC` | `telemetry-topic` | `iot.telemetry.selected.v1` |
| `BPI_CONTEXT_TOPIC` | `context-topic` | `mes.production.context.v1` |
| `BPI_RULE_TOPIC` | `rule-topic` | `bpi.boundary.rule-publication.v1` |
| `BPI_CANDIDATE_TOPIC` | `candidate-topic` | `bpi.batch.candidate.v1` |
| `BPI_DATA_QUALITY_TOPIC` | `data-quality-topic` | `bpi.data-quality.v1` |
| `BPI_CHECKPOINT_INTERVAL_MS` | `checkpoint-interval-ms` | `30000` |
| `BPI_CHECKPOINT_TIMEOUT_MS` | `checkpoint-timeout-ms` | `120000` |
| `BPI_CONTEXT_WAIT_MS` | `context-wait-ms` | `120000` |
| `BPI_CONTEXT_RETENTION_MS` | `context-retention-ms` | `86400000` |
| `BPI_WATERMARK_DELAY_MS` | `watermark-delay-ms` | `30000` |
| `BPI_SOURCE_IDLENESS_MS` | `source-idleness-ms` | `60000` |
| `BPI_BOUNDARY_STATE_TTL_MS` | `boundary-state-ttl-ms` | `2592000000`（30 天） |
| `BPI_TRANSACTION_TIMEOUT_MS` | `transaction-timeout-ms` | `900000` |
| `BPI_PARALLELISM` | `parallelism` | `1`，部署前按基准压测调整 |

配置校验会拒绝 topic 复用、非法 deployment token、context wait 大于 retention、checkpoint timeout
不大于 interval，以及 transaction timeout 不大于 checkpoint timeout。凭据不属于该参数对象，不会被日志输出。

## 自动证据

```bash
JAVA_HOME=<jdk17> mvn -f streaming/pom.xml -pl bpi-stream-engine -am test
```

| 验收面 | 证据 | 状态 |
|---|---|---|
| Kafka record wire/metadata/tombstone | `KafkaIngressRecordCodecTest`、`KafkaIngressDeserializationSchemaTest` | PASS |
| 三类 Kafka payload 解码和 side output | `KafkaDecodeFunctionsHarnessTest` | PASS |
| point-in-time context join/checkpoint | `ProductionContextJoinHarnessTest` | PASS |
| 规则生命周期/checkpoint | `BoundaryRulePublicationLifecycleHarnessTest` | PASS |
| scoped indexed route/fan-out/deactivate | `BoundaryRuleRoutingBroadcastHarnessTest` | PASS |
| event-time evaluator/late replay/checkpoint | `BoundaryKeyedBroadcastHarnessTest` | PASS |
| candidate/data-quality Kafka record | `CandidateKafkaSerializationSchemaTest`、`DataQualityKafkaSerializationSchemaTest` | PASS |
| checkpoint、稳定 UID、双事务 sink 作业图 | `BpiKafkaJobTopologyTest` | PASS |

## 尚未完成

- 未连接真实 3 broker Kafka、Schema Registry 或 Flink HA 集群。
- checkpoint storage、RocksDB state backend、MinIO/S3 凭据和保留策略由目标集群配置，本次未实机验证。
- 未执行 broker/TaskManager 重启、savepoint 升级、事务超时、consumer lag、backpressure 和 10 万点压测。
- Flink 到 BPI 业务语义仍是 Kafka at-least-once + BPI inbox 幂等；本次 exactly-once 只描述 Kafka sink
  与 Flink checkpoint 的事务边界，不替代 BPI PostgreSQL marker 验收。
- `LATE_EVENT_REVISION_REQUIRED` 已进入数据质量 topic，但人工修订消费者和页面尚未实现。
- 真实 JetLinks -> Kafka -> Flink -> BPI -> PostgreSQL -> 浏览器候选确认链必须单独验收后，状态才能提升为
  `CLUSTER_ACCEPTED`。
