# BPI Phase 3C-C 工艺信号窗口设计

## 1. 目标与边界

本阶段把已经治理的遥测点转换为可追溯、可复现、无未来泄漏的 Dataset
过程窗口事实，供 `BATCH_START_BOUNDARY_REVIEW_RISK` 离线训练就绪评估使用。

本阶段只构建 PostgreSQL Dataset Input，不训练模型、不注册模型、不执行在线推理，
也不改变批次、PLC、QCS 或 WMS。缺少拓扑绑定、点位目录、单位、校准、样本覆盖或
事件时序证据时必须失败关闭。

## 2. 窗口规格

每个不可变 Dataset Definition 可声明最多 20 个 `process.window.*` 窗口。规格固定：

| 字段 | 约束 | 说明 |
| --- | --- | --- |
| `featureRef` | 唯一，前缀 `process.window.` | Manifest 特征键 |
| `signal` | 拓扑语义信号 | 通过固定 topology version 解析物理点 |
| `valueType` | `NUMERIC/BOOLEAN` | 遥测值类型 |
| `metric` | `MEAN/MIN/MAX/LAST/DELTA/SLOPE/TRUE_RATIO` | 窗口聚合 |
| `startOffsetSeconds` | `-3600..-1` | 相对自动 START 的窗口起点 |
| `endOffsetSeconds` | 大于起点且 `<= 0` | 相对自动 START 的窗口终点 |
| `minimumSamples` | `2..900` | 可接受样本下限 |
| `maximumGapSeconds` | `1..600` | 包含窗口两端空档的最大间隔 |
| `expectedUnit` | 非空 | 与拓扑、点位目录和遥测严格比对 |
| `requireCalibration` | 布尔值 | 是否要求窗口内有效的批准校准 |
| `acceptedQualityCodes` | `GOOD/SUBSTITUTED` 的非空子集 | 可进入聚合的质量码 |

数值信号支持除 `TRUE_RATIO` 外的指标；布尔信号只支持 `TRUE_RATIO`。窗口规格进入
Dataset Definition checksum，定义创建后不可变；调整任何阈值必须创建新版本。

## 3. 时点安全

预测时间固定为 `automatic_start_time`，窗口为左闭右闭区间：

```text
[prediction_time + startOffsetSeconds, prediction_time + endOffsetSeconds]
```

只有同时满足以下条件的遥测点才可进入聚合：

1. `sample_time` 位于窗口内；
2. `sample_time <= prediction_time`；
3. `ingest_time <= prediction_time`，即预测时真实可见；
4. 物理点与固定 topology binding、point catalog snapshot 完全一致；
5. 值类型、单位、质量码和校准满足不可变窗口规格。

预测之后才到达的数据只计入 `lateAvailabilityCount`，绝不进入特征值。窗口 source
fingerprint 覆盖原始点身份、值、单位、质量、校准、sample/ingest time；fact checksum
再覆盖规格、版本血缘、统计量、阻断原因和 fingerprint。

## 4. PostgreSQL 不可变事实

Flyway V32：

- 给 `bpi_dataset_definitions` 增加不可变 `process_signal_windows`；
- 新增 `bpi_dataset_process_signal_window_facts`，主键为
  `(snapshot_id, review_id, feature_ref)`；
- 每条事实固定 Dataset Snapshot、review、batch、rule、topology、point catalog、
  物理点、窗口边界、质量统计、聚合值、fingerprint 和 checksum；
- 状态仅为 `READY/BLOCKED`；
- 运行角色只有 `SELECT/INSERT`，触发器禁止 UPDATE。

快照样本和窗口事实在一个事务中写入。任一必需窗口为 `BLOCKED` 时，样本写入但标记
为排除；窗口估计值只保留为证据，不进入 `featurePayload`。

## 5. 失败关闭规则

以下任一事实都会阻断对应窗口：

- `WINDOW_BINDING_MISSING`
- `WINDOW_BINDING_AMBIGUOUS`
- `WINDOW_BINDING_UNIT_MISMATCH`
- `WINDOW_POINT_CATALOG_MISSING`
- `WINDOW_POINT_NOT_READY`
- `WINDOW_POINT_CATALOG_UNIT_MISMATCH`
- `WINDOW_SAMPLE_COUNT_BELOW_MINIMUM`
- `WINDOW_MAX_GAP_EXCEEDED`
- `WINDOW_UNIT_MISMATCH`
- `WINDOW_VALUE_TYPE_MISMATCH`
- `WINDOW_CALIBRATION_MISMATCH`
- `WINDOW_METRIC_UNAVAILABLE`

不合格质量码和预测后到达点不会直接阻断；它们被排除并计数。如果因此达不到样本数或
最大间隔要求，窗口仍会被上述覆盖门槛阻断。

## 6. API 与页面

`POST /bpi/v1/datasets` 在原有字段之外接受 `processSignalWindows`。列表和详情回读
完整规格；Manifest 的 definition、样本 source evidence 和 PostgreSQL fact 均保留同一
checksum 血缘。

数据集工作台提供受控窗口模板和显式质量门槛，不允许自由 SQL。快照抽屉展示：

- READY/BLOCKED 窗口数量；
- 每个 feature 的时间范围、物理点、单位、样本数、最大间隔和聚合值；
- 被质量、校准、单位或摄取截止排除的点数；
- source fingerprint、fact checksum 和阻断原因。

## 7. 训练就绪策略升级

训练就绪策略升级为
`bpi-training-readiness/batch-start-boundary-v2`。除了至少两个过程窗口 featureRef，
它还要求每个被纳入样本的每个过程窗口都存在且为 `READY`。仅添加特征名不能通过门槛。
历史 v1 评估保持不可变。

## 8. 验收与回滚

本地验收必须覆盖：

- 规格校验与 checksum 确定性；
- 事件时间和 ingest time 截止；
- 数值及布尔聚合；
- 单位、质量、校准、样本数和最大间隔失败关闭；
- PostgreSQL 快照样本与事实同事务、不可变和幂等；
- v2 训练就绪不能被 featureRef 字符串绕过；
- 桌面和 390px 页面证据。

目标环境使用唯一 `ADP_E2E_*` marker，从真实页面创建 Dataset Definition/Snapshot，
通过 API 和 PostgreSQL 复验窗口事实，重启后回读，最后定向清理。

V32 为 expand-only。回滚只隐藏窗口配置和停止新建含窗口的数据集；不删除事实表、
不修改历史快照、不降级 Flyway。
