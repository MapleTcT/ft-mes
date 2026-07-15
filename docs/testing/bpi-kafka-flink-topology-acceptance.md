# BPI Kafka/Flink 作业拓扑验收

## 结论

`streaming/bpi-stream-engine` 已组装 Java 17 / Flink 2.2.1 的可部署 Kafka 作业入口
`BpiKafkaJob`。本里程碑状态为 **LOCAL_MINICLUSTER_ACCEPTED / TARGET_CLUSTER_PENDING**：生产数据面、
控制面、候选输出、数据质量输出和规则应用回执已经在作业图中闭合；规则应用回执已通过真实本地 Kafka 4.2
与 Flink 2.2.1 MiniCluster 的 checkpoint/TaskManager 重启验收，但尚未在目标三 broker、Flink、MinIO
测试集群完成联合运行验收。

常规 Java 17 streaming reactor 测试继续覆盖 wire、拓扑、Harness、checkpoint state 和 replay；
需要真实运行时的 `BpiRuleApplicationFlinkKafkaAcceptanceTest` 为显式启用测试，已连续通过本地独立 broker/job
运行，并输出机器可读证据。测试数量以当前 Surefire 报告为准，文档不固定易失真的累计数字。

## 作业链路

```text
iot.telemetry.selected.v1 -> BPKR/v1 -> TelemetryEnvelopeV1 validation -> BPTE/v1
iot.point-catalog.snapshot.v1 -> BPKR/v1 -> PointCatalogSnapshotV1 validation
mes.production.context.v1 -> BPKR/v1 -> ProductionContextEventV1 validation
    -> tenant|plant|line event-time join -> BPCT/v1 -> timestamp reassignment
bpi.boundary.rule-publication.v1 -> immutable lifecycle
point catalog + rule publication -> BRTC/v1 runtime READY gate -> product/device/property indexed routes
    -> scoped rule update BPRU/v1 -> keyed boundary evaluator
    -> bpi.batch.candidate.v1 (Kafka exactly-once sink)
    -> bpi.boundary.rule-application.v1 (Kafka exactly-once checkpoint sink)

decode/join/routing/evaluation issues
    -> DataQualityEventV1 -> bpi.data-quality.v1 (Kafka exactly-once sink)
```

## 已验收约束

- Kafka source 保留 topic、partition、offset、timestamp、key 和 tombstone，解码失败不会丢失定位证据。
- telemetry envelope 级错误整体隔离；单个非法 point 只隔离该 point，其余合法点继续处理。
- 四条 source 使用按 lane 区分且跨重启稳定的 consumer group、`read_committed` 和
  committed-offset/earliest fallback；`deployment-id` 只参与事务 producer 身份，不改变消费位点。
- telemetry、context、rule publication 配置 event-time watermark 和 source idleness；点位目录是控制流，
  按 scope 保留最新 `observed_at_ms` 快照并拒绝倒序或同时间不同内容。
- 上下文迟到补齐后，拓扑从 `BPCT/v1` 中重新赋 telemetry point 时间，禁止继承 context 时间。
- 规则生命周期使用 checkpointed keyed state；同版本只允许 `ACTIVE -> INACTIVE`，重新启用必须新版本。
- 规则引用使用 `tenant|plant|line|ruleCode|ruleVersion`，同名规则不会跨租户或产线冲突。
- 路由使用 tenant/plant/line/product/device/property broadcast index；单点只访问命中的规则列表，不扫描全部规则。
- 规则绑定同时固化 `productId` 与 `calibrationVersion`。目录缺失、设备停用、属性缺失、单位不符、
  校准版本漂移或来源序列未启用时，规则不会进入 evaluator；READY 降级会发送 DELETE 并清空该规则的
  全部待决窗口，旧 event-time timer 不会再产生候选，恢复后必须从新观测重新累积。
- `bpi.boundary.rule-application.v1` 的 `APPLIED` 当前表示规则控制事件通过不可变生命周期检查，
  不等价于点位目录运行时 READY；运行时阻断原因进入 `bpi.data-quality.v1`。独立运行时准入回执仍列为后续缺口。
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
| `BPI_POINT_CATALOG_TOPIC` | `point-catalog-topic` | `iot.point-catalog.snapshot.v1` |
| `BPI_POINT_CATALOG_MAX_MESSAGE_BYTES` | `point-catalog-max-message-bytes` | `6291456`；允许范围 1-8 MiB |
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
| 四类 Kafka payload 解码和 side output | `KafkaDecodeFunctionsHarnessTest` | PASS |
| point-in-time context join/checkpoint | `ProductionContextJoinHarnessTest` | PASS |
| 规则生命周期/checkpoint | `BoundaryRulePublicationLifecycleHarnessTest` | PASS |
| point catalog READY/降级/校准恢复、scoped indexed route/fan-out/deactivate | `BoundaryRuleRoutingBroadcastHarnessTest` | PASS |
| event-time evaluator/late replay/checkpoint/运行时 DELETE 清窗 | `BoundaryKeyedBroadcastHarnessTest` | PASS |
| candidate/data-quality Kafka record | `CandidateKafkaSerializationSchemaTest`、`DataQualityKafkaSerializationSchemaTest` | PASS |
| checkpoint、稳定 UID、candidate/data-quality/rule-application 三个事务 sink 作业图 | `BpiKafkaJobTopologyTest` | PASS |
| checkpoint 提交可见性、取消回滚、TaskManager 重启恢复和终态规则保护 | `BpiRuleApplicationFlinkKafkaAcceptanceTest` | PASS_LOCAL_FLINK_MINICLUSTER_KAFKA |

本地运行时验收入口：

```bash
JAVA_HOME=/path/to/jdk17 make bpi-rule-application-flink-acceptance
```

机器记录：`metadata/bpi-rule-application-flink-kafka-acceptance.json`。默认 broker 为测试进程内的一次性
Kafka 4.2 KRaft server，checkpoint 存储为测试拥有的本地目录；可通过
`BPI_TEST_KAFKA_BOOTSTRAP_SERVERS` 使用专用外部测试 broker。

## 尚未完成

- 未在目标环境连接 3 broker Kafka、Schema Registry 或 Flink HA 集群。
- 目标 checkpoint storage、RocksDB state backend、MinIO/S3 凭据和保留策略尚未实机验证。
- 本地 TaskManager 重启恢复已经通过；目标 broker/TaskManager 重启、savepoint 升级、事务超时、consumer lag、backpressure 和 10 万点压测仍未执行。
- Flink 到 BPI 业务语义仍是 Kafka at-least-once + BPI inbox 幂等；本次 exactly-once 只描述 Kafka sink
  与 Flink checkpoint 的事务边界。PostgreSQL 消费另有独立真实落库证据，但两份分离测试不替代浏览器到数据库的联合 marker 验收。
- `LATE_EVENT_REVISION_REQUIRED` 已进入数据质量 topic，但人工修订消费者和页面尚未实现。
- 规则运行时 READY/DEGRADED 状态尚未形成独立回执和前端状态列；当前必须联合规则应用回执与数据质量事件判断。
- 真实 JetLinks -> Kafka -> Flink -> BPI -> PostgreSQL -> 浏览器候选确认链必须单独验收后，状态才能提升为
  `CLUSTER_ACCEPTED`。
