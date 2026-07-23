# BPI 现场数据覆盖目标验收

## 结论

2026-07-23 在唯一测试栈 `10.11.100.17 / adp-mes-newbase` 完成 BPI Phase 3C-D
现场数据覆盖投影的真实页面、API、PostgreSQL 和定向清理验收。状态为
`PASS_TARGET_BROWSER_API_POSTGRES_CLEANED`。

本次证明系统能够从影子运行固定版本中分别计算：

- 固定点位来源是否具备物理身份、平台注册、严格来源序列和已批准校准。
- 当前已经积累多少复核批次、生产日、START 接受标签和 START 拒绝标签。
- 数据量不足时是否返回明确 blocker，并保持训练、模型、推断和生产激活关闭。

本次没有制造训练样本，也不证明现场数据已经达到训练门槛。目标结果仍为
`0/200` 个复核批次、`0/7` 个生产日、`0/100` 个接受标签和 `0/10` 个拒绝标签。

## 运行身份

| 项目 | 值 |
|---|---|
| marker | `ADP_E2E_BPI_FIELD_COVERAGE_20260723_1525_A3` |
| run | `299d513a-6c9b-4f0a-9d1b-6f61eddbff1e` |
| release | `a73a53a0f4f278d70bd85db4b21acb545d14eabf` |
| 验收脚本修订 | `1a0f3a5144b9a82442fa6b0f937982165fa7ed15` |
| 数据库 | PostgreSQL 15 / `ft_mes_bpi` |
| Flyway | V33，`VALIDATE_EXISTING_SCHEMA` |
| 页面 | `http://10.11.100.17:18080/bpi/#/shadowRuns` |
| 工厂 / 产线 | `PLANT-01 / LINE-S07-01` |

升级报告为
`metadata/bpi-integrated-upgrade-field-data-coverage-target.json`，SHA-256 为
`5968840c310163ac007cd1351626989f80a2da32bd9ac7d88638dc67b1075127`。

## 页面动作

真实 `admin` 会话完成以下动作：

```http
POST /bpi-api/shadow-runs
GET  /bpi-api/shadow-runs/299d513a-6c9b-4f0a-9d1b-6f61eddbff1e
POST /bpi-api/shadow-runs/299d513a-6c9b-4f0a-9d1b-6f61eddbff1e/cancel
```

创建接口返回 `DRAFT/r1`，读取接口返回固定来源和训练数据覆盖，取消接口返回
`CANCELLED/r2`。真实页面共观察到 20 个 BPI 响应，全部为 2xx；console error、
page error 和 request failure 均为 0。

页面明确显示“仅表示现场数据覆盖进度，不代表允许训练”。训练覆盖不参与篡改既有影子运行
审批语义，当前 `readyForApproval=false`。

## 来源覆盖

fixture 固定两个受控点位：瞬时流量和泵运行状态。两个点位分别具备平台 ACTIVE/registered、
物理 product/device/property identity、严格递增来源序列和已批准校准。

| 指标 | 实际 |
|---|---:|
| 固定点位 | 2 |
| ACTIVE 且平台注册 | 2 |
| 物理身份完整 | 2 |
| 来源序列新鲜且 QUALIFIED | 2 |
| 校准已批准 | 2 |
| 完全就绪点位 | 2 |
| `fullyReady` | true |

这些值只描述本轮固定快照的来源可信度，后续目录变化不会倒写这次影子验收。

## 训练数据覆盖

策略版本为 `bpi-training-data-coverage/batch-start-boundary-v1`：

| 指标 | 实际 / 门槛 | blocker |
|---|---:|---|
| 不同复核批次 | 0 / 200 | `TRAINING_REVIEWED_BATCHES_BELOW_MINIMUM` |
| UTC 生产日 | 0 / 7 | `TRAINING_PRODUCTION_DAYS_BELOW_MINIMUM` |
| 接受的 START 标签 | 0 / 100 | `TRAINING_ACCEPTED_START_LABELS_BELOW_MINIMUM` |
| 拒绝的 START 标签 | 0 / 10 | `TRAINING_REJECTED_START_LABELS_BELOW_MINIMUM` |

`thresholdsMet=false`。本次没有通过降低阈值、复制批次、伪造日期或生成标签来制造通过结果。

## 后端与 PostgreSQL

读取链路为：

```text
ShadowRunController
  -> ShadowRunService
  -> ShadowRunPostgresRepository
  -> ShadowRunSourceCoverage
  -> ShadowRunTrainingDataCoverage
  -> ShadowRunView
```

`ShadowRunPostgresRepository` 直接从固定目录、点位、来源序列、校准、批次和复核事实聚合。
创建和取消仍通过既有事务写入影子运行、审计和幂等表。

取证前 PostgreSQL 结果：

| 事实 | 实际 |
|---|---|
| shadow run | `CANCELLED/r2` |
| createdBy / cancelledBy | `legacy-ticket:admin` / `legacy-ticket:admin` |
| audit actions | `SHADOW_RUN_CREATED`、`SHADOW_RUN_CANCELLED` |
| idempotency rows | 2 |
| batch rows | 0 |
| review rows | 0 |
| source coverage | 六项均为 2 |
| training coverage | 四项均为 0 |

验收 SQL：

- `deploy/docker/scripts/bpi-field-data-coverage-acceptance-fixture.sql`
- `deploy/docker/scripts/bpi-field-data-coverage-acceptance-verification.sql`
- `deploy/docker/scripts/bpi-field-data-coverage-acceptance-cleanup.sql`

## 浏览器

- 桌面：`1440x900`，抽屉宽 680px，三个动作按钮均在抽屉内。
- 移动：viewport/body/document 为 `390/390/390`，抽屉宽 390px。
- 移动底部“关闭 / 取消任务 / 启动影子运行”三个按钮均完整可见。
- 验收脚本等待抽屉滑入动画完成并检查几何边界后才截图，避免把过渡帧误判为显示缺陷。

截图：

- `metadata/bpi-field-data-coverage-desktop-target.png`
- `metadata/bpi-field-data-coverage-mobile-target.png`

## 精确清理与运行态

取证后按 marker 清理，以下 12 类投影全部为 0：

```text
audit, rules, outbox, batches, reviews, catalogs,
shadowRuns, topologies, idempotency, calibrations,
pointEntries, sourceSequenceEvidence
```

当前三项核心服务均为 HTTP 200 且 Docker healthy：

- `ft-mes-bpi-service:20260723t063556z-a73a53a0f4f2`
- `ft-mes-bpi-adapter:20260723t063556z-a73a53a0f4f2`
- `ft-mes-bpi-wms-adapter:20260723t063556z-a73a53a0f4f2`

一个不属于 Compose、`restart=no` 且日志显示 disabled 的旧 catalog publisher 已停止并由
Docker 自动删除。正式 Compose 的 materializer、catalog、retention 和 MLflow 可选服务仍为
0 个运行实例。

## 边界

- 本轮关闭的是 Phase 3C-D 的软件投影、页面可见性和真实落库验收。
- fixture 只证明计算逻辑，不替代 `MapleTcT/iot` 的物理 DEVICE/GATEWAY、真实时间跨度和正式证书。
- 仍需至少 200 个不同真实复核批次、7 个真实生产日和足量 accepted/rejected START 标签。
- 模型训练、模型注册、在线推断和生产激活均保持 false。
- 因此 `G-021` 继续保持 `PARTIAL`，不能标记为 `READY`。

完整机器证据为 `metadata/bpi-field-data-coverage-acceptance.json`。
