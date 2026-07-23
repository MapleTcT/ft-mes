# BPI Phase 3C-D 现场数据覆盖进度设计

## 1. 目标

本阶段回答一个运营问题：一条正在运行的 BPI 影子验收任务，距离后续训练资格所需的真实数据覆盖还差多少。

系统必须直接显示固定点位来源可信度、复核批次数、生产日数、START 接受标签和 START 拒绝标签。它不启动训练，不替代
`bpi-training-readiness/batch-start-boundary-v2`，也不能把受控 fixture、HTTP 200 或影子运行批准外推为模型可训练。

## 2. 方案选择

比较三个方案：

1. 扩展现有 `ShadowRun` 只读投影。复用已经固定的规则、拓扑、点位目录、观察周期、人工复核和数据质量状态。
2. 新建独立“采数任务”聚合。会复制 Shadow Run 生命周期，并引入两个任务状态不一致的问题。
3. 在前端根据复核列表计算。只能看到当前加载的数据，无法形成可信 API/PostgreSQL 验收证据。

采用方案 1。新增字段全部由后端 PostgreSQL 查询计算，不新增可编辑状态，不新增迁移，不改变影子运行审批门槛。

## 3. 来源覆盖投影

`ShadowRun` 新增 `sourceCoverage`：

| 字段 | 含义 |
| --- | --- |
| `pinnedPointCount` | 固定点位目录中的点位数 |
| `activeRegisteredPointCount` | 已注册、ACTIVE、属性存在且单位有效的点位数 |
| `physicalIdentityPointCount` | 要求来源序列且 origin 为 `DEVICE/GATEWAY`、binding fingerprint 有效的点位数 |
| `freshSequenceQualifiedPointCount` | 在固定快照后观察、当前仍有效且 binding 完全匹配的来源序列证据数 |
| `approvedCalibrationPointCount` | 固定快照时和当前时刻都有效的独立 MES 批准校准数 |
| `readyPointCount` | 同时满足上述全部条件的点位数 |
| `fullyReady` | `pinnedPointCount > 0` 且全部点位 READY |

这些计数只读取固定 snapshot 和权威证据表。来源自己声明的校准状态不能提高
`approvedCalibrationPointCount`，过期 sequence evidence 不能提高 `freshSequenceQualifiedPointCount`。

## 4. 训练数据覆盖投影

`ShadowRun` 新增 `trainingDataCoverage`，策略固定为
`bpi-training-data-coverage/batch-start-boundary-v1`：

| 门槛 | 固定值 | 实际值来源 |
| --- | ---: | --- |
| 复核批次 | 200 | ACTIVE 批次复核的 distinct batch |
| 生产日 | 7 | 自动 START 时刻按 UTC 生产日期去重，与训练就绪策略一致 |
| START 接受标签 | 100 | `start_boundary_accepted=true` |
| START 拒绝标签 | 10 | `start_boundary_accepted=false` |

返回 `thresholdsMet` 和以下 blocker：

- `TRAINING_REVIEWED_BATCHES_BELOW_MINIMUM`
- `TRAINING_PRODUCTION_DAYS_BELOW_MINIMUM`
- `TRAINING_ACCEPTED_START_LABELS_BELOW_MINIMUM`
- `TRAINING_REJECTED_START_LABELS_BELOW_MINIMUM`

该投影只说明数量覆盖。即使四项全部满足，也必须重新创建包含 READY 工艺窗口的数据集，并由 v2 policy 检查
point-in-time、过程窗口、排除率、现场周期、点位 READY 和 CRITICAL 数据质量事件。页面固定显示“训练仍未启动”。

## 5. API 与页面

现有 Shadow Run 列表和详情响应以兼容式新增字段：

- `sourceCoverage`
- `trainingDataCoverage`

影子运行列表保留验收指标，并新增现场覆盖摘要。详情抽屉新增两个不混淆的区块：

1. `固定来源可信度`：展示六类点位计数和失败关闭结论；
2. `现场数据覆盖`：展示四项真实进度、blocker code 和“仅覆盖进度，不代表可训练”提示。

页面不得提供手工修改计数、跳过门槛或启动训练的按钮。

## 6. 验收

1. Java 映射测试覆盖空数据、部分覆盖和全部数量门槛；
2. PostgreSQL 15 集成测试直接回查固定目录、校准、来源序列及 ACTIVE 复核投影；
3. 模拟器与 OpenAPI 保持同一响应；
4. Playwright 在桌面和 390px 页面验证进度、blocker、无 console/network/page error；
5. 目标环境只允许证明真实当前差距，不得批量制造 200 个 fixture 后宣称现场门槛通过；
6. 模型训练、注册、推断和生产激活继续保持关闭。

