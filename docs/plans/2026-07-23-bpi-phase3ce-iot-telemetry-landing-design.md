# BPI Phase 3C-E IoT 遥测落表与现场试点设计

## 1. 目标

本阶段关闭 `MapleTcT/iot` 已发布遥测与 MES PostgreSQL 现场证据之间的空档：

```text
MQTT -> JetLinks -> durable spool -> iot.telemetry.selected.v1
     -> Flink boundary/data-quality
     -> BPI telemetry Kafka consumer -> PostgreSQL
     -> Shadow Run 现场落表覆盖
```

系统必须区分以下三类事实：

1. 点位目录、物理身份、来源序列和校准是否具备准入证据；
2. 对应遥测是否在影子运行时间窗内真实进入 PostgreSQL；
3. 复核批次、生产日和标签数量是否达到后续训练覆盖门槛。

任何一类事实都不能由另一类事实替代。Kafka offset、HTTP 200、来源序列 `QUALIFIED` 或受控
fixture 都不能单独证明物理现场已经达到生产或训练资格。

## 2. 方案选择

比较三个方案：

1. 在 BPI service 增加独立、幂等、受控范围的 Kafka 落库消费者，并扩展现有 Shadow Run
   只读投影；
2. 让 Flink 直接写 PostgreSQL。这样会把业务审计事务和 Flink checkpoint 耦合，增加双写及恢复复杂度；
3. 新建“现场采数任务”生命周期。它会复制 Shadow Run 已有的规则、拓扑、目录快照、观察时间窗和审批状态。

采用方案 1。`iot.telemetry.selected.v1` 仍可被 Flink 和 BPI service 使用不同 consumer group
独立消费：Flink 负责事件时间计算，BPI service 负责不可变事实、幂等和审计查询。现场试点继续使用
Shadow Run，不新增第二个任务状态机。

## 3. 遥测 Kafka 落库入口

新增 `bpi.telemetry-kafka` 配置，必须满足：

- 默认 `enabled=false`；
- tenant、plant、line allowlist 默认 `_DENY_ALL_`；
- 只接受配置的 source topic；
- source topic 与 DLQ topic 必须不同；
- 只接受 `TelemetryEnvelopeV1` Protobuf；
- Kafka key 必须等于 `plant_id|device_id`；
- 使用 `read_committed`、手工同步提交和独立 consumer group；
- PostgreSQL 事务提交后才确认 offset；
- poison record 进入专用 DLQ；
- event ID 及 `(tenant, gateway, device, source_epoch, sequence)` 继续由既有唯一约束去重；
- HTTP 遥测入口仍默认关闭，Kafka 消费器不绕过 scope、合同、单位、质量码、时钟和序列校验。

首次部署可配置 `auto-offset-reset=latest`，避免未评审历史数据自动回灌。consumer group 建立后，
重启继续使用已提交 offset。需要回放历史时必须使用新的受控 group 和单独验收，不能静默改 offset。

## 4. 现场落表覆盖投影

`ShadowRun` 新增 `telemetryCoverage`，只读取本次运行固定的点位目录和
`startedAt..completedAt/now` 时间窗：

| 字段 | 含义 |
| --- | --- |
| `windowStarted` | 影子运行是否已经开始 |
| `windowStart` / `windowEnd` | 当前证据窗口 |
| `pinnedPointCount` | 固定目录点位数 |
| `observedPointCount` | 至少有一条 accepted PostgreSQL 点值的固定点位数 |
| `authoritativeSequencePointCount` | 至少有一条 DEVICE/GATEWAY origin 与固定绑定一致的点位数 |
| `calibratedPointCount` | 至少有一条 calibration version 与固定目录一致的点位数 |
| `goodQualityPointCount` | 至少有一条 `GOOD` 点值的点位数 |
| `acceptedEventCount` | 时间窗内匹配固定设备的去重遥测事件数 |
| `acceptedObservationCount` | 匹配固定点位的 accepted 点值数 |
| `rejectedObservationCount` | 匹配固定点位的 rejected 点值数 |
| `gapEventCount` / `outOfOrderEventCount` | 来源顺序异常事件数 |
| `firstObservedAt` / `lastObservedAt` | PostgreSQL 中匹配点值的首末采样时间 |
| `fullyCovered` | 全部固定点位具备落表、权威序列、匹配校准、GOOD 质量且没有顺序异常 |
| `blockers` | 由后端确定的失败关闭原因 |

阻断码固定为：

- `TELEMETRY_WINDOW_NOT_STARTED`
- `TELEMETRY_POINTS_NOT_OBSERVED`
- `TELEMETRY_AUTHORITATIVE_SEQUENCE_INCOMPLETE`
- `TELEMETRY_CALIBRATION_INCOMPLETE`
- `TELEMETRY_GOOD_QUALITY_INCOMPLETE`
- `TELEMETRY_SEQUENCE_GAP_DETECTED`
- `TELEMETRY_OUT_OF_ORDER_DETECTED`

`telemetryCoverage` 不改变 Shadow Run 的启动门槛，避免尚未开始就要求窗口数据；但运行结束进入
`EVALUATING` 后，落表覆盖不完整必须阻止批准。训练数据覆盖和模型门禁保持独立。

## 5. 页面

影子运行详情在“固定来源可信度”之后增加“现场遥测落表”区块：

- 展示四类点位覆盖、事件/点值数量、顺序异常和首末采样时间；
- 明确标注证据来自 PostgreSQL，而不是 Kafka offset 或浏览器缓存；
- 显示 blocker code，不能在页面手工改数；
- 即使 `fullyCovered=true`，仍显示“仅证明本次窗口落表，不等于物理设备、连续 7-14 天或训练资格”。

## 6. 目标验收边界

受控目标验收必须完成：

1. 真实 MQTT QoS1 发布到隔离 JetLinks 试点；
2. JetLinks exporter received/enqueued/published 与 Kafka offset 增长；
3. BPI Kafka consumer offset 追平且 DLQ 为 0；
4. marker 遥测写入 `bpi_telemetry_events` 和 `bpi_telemetry_points`；
5. API 与真实页面显示相同覆盖；
6. 重放同一事件不增加行，顺序 gap/poison 使用独立负向样本验证；
7. 浏览器 console、page、network error 为 0；
8. marker 数据精确清理，模型训练、注册、推断和激活继续为 0/false。

这组验收只能证明软件链和受控 MQTT 试点。没有现场提供的真实设备身份、正式校准证书和连续观察周期时，
G-021 必须保持 `PARTIAL`，不得把模拟 MQTT 样本写成物理现场证据。

