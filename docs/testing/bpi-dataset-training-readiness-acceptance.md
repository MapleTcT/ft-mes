# BPI 离线训练就绪评估目标验收

## 结论

2026-07-23 在唯一目标 ADP 栈 `10.11.100.17 / adp-mes-newbase` 完成 BPI Phase 3C-B
“离线训练就绪评估”真实验收。功能验收状态为
`PASS_TARGET_BROWSER_API_POSTGRES_RESTART_NO_MODEL_SIDE_EFFECT_CLEANED`，但本次数据资格结果应当且确实为
`BLOCKED`。

这两个结论不冲突：

- 功能 PASS：真实页面、API、PostgreSQL、幂等、不可变、重启、移动布局、零模型副作用和清理均通过。
- 数据 BLOCKED：当前冻结数据只有 1 个 included batch、1 个生产日、0 个过程信号窗口，不能进入模型训练。

## 运行身份

| 项目 | 值 |
|---|---|
| marker | `ADP_E2E_BPI_READINESS_20260723_091500_A1` |
| 仓库 revision | `89044926c1335f8028c624b99fd7ecb57d771f2b` |
| 数据库 | PostgreSQL 15.18 / `ft_mes_bpi` |
| Flyway | `30 -> 31`，expand-only |
| 页面 | `http://10.11.100.17:18080/bpi/#/datasets` |
| 目标 | `BATCH_START_BOUNDARY_REVIEW_RISK` |
| policy | `bpi-training-readiness/batch-start-boundary-v1` |
| registration | `21edf8aa-b354-41f7-8703-0c42fc2984f1` |
| assessment 1 | `3b4bb80a-5a96-4fcb-81a3-0230340fb311` |
| assessment 2 | `f961a15f-ee22-42c6-9702-7136d9cab007` |
| checksum | `91d8726c265720758ed796a7dac297f6fd15b61329cef8464e2afe80f5d0ef98` |

升级报告：
`metadata/bpi-integrated-upgrade-v31-target.json`，SHA-256
`9016ef2d947af5cb8cae3ca2bf745e9817be553134b69c59c3bbbf82ab62fff1`。

## 页面与 API

真实 `admin` 会话打开数据集详情，在“离线训练就绪”抽屉填写验收原因并提交：

```http
POST /bpi-api/dataset-mlflow-registrations/21edf8aa-b354-41f7-8703-0c42fc2984f1/training-readiness-assessments
If-Match: 6
Idempotency-Key: <unique UUID>
Content-Type: application/json

{
  "objectiveCode": "BATCH_START_BOUNDARY_REVIEW_RISK",
  "reason": "ADP_E2E_BPI_READINESS_20260723_091500_A1 真实页面核对离线训练资格，不启动模型"
}
```

接口返回 HTTP 200 和 `BLOCKED/r1/sequence=1`。相同幂等键重放返回同一 assessment，
没有新增业务行；第二个幂等键在相同冻结事实下追加 `sequence=2`，checksum 保持一致。

页面随后通过以下只读接口恢复状态：

- `GET /bpi-api/dataset-mlflow-registrations/{registrationId}/training-readiness-assessments`
- `GET /bpi-api/dataset-training-readiness-assessments/{assessmentId}`

## 门槛结果

服务端固定执行 19 个门槛，其中 11 个通过、8 个阻断：

| 阻断码 | 要求 | 实际 |
|---|---:|---:|
| `PROCESS_SIGNAL_WINDOWS_MISSING` | 至少 2 类过程信号窗口 | 0 |
| `INCLUDED_SAMPLE_COUNT_BELOW_MINIMUM` | 200 个 included samples | 1 |
| `DISTINCT_BATCH_COUNT_BELOW_MINIMUM` | 200 个独立批次 | 1 |
| `PRODUCTION_DAY_COVERAGE_BELOW_MINIMUM` | 7 个生产日 | 1 |
| `PRODUCTION_SPLIT_GROUPS_BELOW_MINIMUM` | 2 个生产时间切分组 | 1 |
| `EXCLUDED_RATIO_ABOVE_MAXIMUM` | 排除率不高于 0.2 | 0.666667 |
| `START_ACCEPTED_LABEL_COUNT_BELOW_MINIMUM` | 100 个 accepted 标签 | 1 |
| `START_REJECTED_LABEL_COUNT_BELOW_MINIMUM` | 10 个 rejected 标签 | 0 |

`review.boundary_acceptance` 标签、MLflow Dataset Input、point-in-time 防泄漏、上下文特征、
影子运行周期、点位目录和关键数据质量门槛均被真实核对，不能用其中部分通过来推导模型可训练。

## PostgreSQL 落表

后端链路：

```text
DatasetController.assessTrainingReadiness
  -> DatasetTrainingReadinessService
  -> DatasetTrainingReadinessPostgresRepository
  -> bpi.bpi_dataset_training_readiness_assessments
  -> bpi.bpi_audit_events
  -> bpi.bpi_api_idempotency
```

验收 SQL：

```bash
psql -v marker=ADP_E2E_BPI_READINESS_20260723_091500_A1 \
  -f deploy/docker/scripts/bpi-dataset-training-readiness-target-verification.sql
```

关键结果：

- assessment：2 行，状态 `BLOCKED/BLOCKED`，序号 `1/2`，revision 均为 1。
- 每行 19 个 gate、8 个 blocker、1 个 included sample、0 个 signal-window feature。
- 两行 checksum 相同，证明相同冻结事实的确定性评估。
- audit：2 行 `DATASET_TRAINING_READINESS_ASSESSED`。
- idempotency：2 行 `COMPLETED/200`；相同键重放没有第三行。
- 直接 `UPDATE` 被 `trg_bpi_dataset_training_readiness_immutable` 拒绝。

## 零模型副作用

评估前后 MLflow 均为：

| 表/对象 | 评估前 | 评估后 |
|---|---:|---:|
| runs | 1 | 1 |
| datasets | 1 | 1 |
| inputs | 1 | 1 |
| registered_models | 0 | 0 |
| model_versions | 0 | 0 |
| logged_models | 0 | 0 |

`phaseBoundary` 始终为：

```json
{
  "assessmentOnly": true,
  "trainingStarted": false,
  "modelCreated": false,
  "modelRegistered": false,
  "onlineInferenceEnabled": false,
  "productionActivationAllowed": false
}
```

## 浏览器与重启

- 首次真实页面 POST、相同键重放、第二次评估均为 HTTP 200。
- console errors、page errors、request failures 均为 0。
- 移动端 viewport/body/document 为 `390/390/390`，抽屉 client/scroll width 为 `389/389`。
- 重启 `bpi-service` 和 `bpi-adapter` 后，页面仍读到 assessment 2、同 checksum 和 8 个 blocker。

截图：

- `metadata/bpi-dataset-training-readiness-blocked-target.png`
- `metadata/bpi-dataset-training-readiness-blocked-mobile-target.png`
- `metadata/bpi-dataset-training-readiness-restart-target.png`

原始浏览器记录：

- `metadata/bpi-dataset-training-readiness-assess-target.json`
- `metadata/bpi-dataset-training-readiness-restart-target.json`

## 精确清理

取证后执行 V31-aware marker 清理：

- V26-V31 definition、snapshot、sample、materialization、publication、archive、registration、
  readiness assessment、audit、idempotency 和 fixture 行均为 0。
- source Parquet、Object Lock archive 和 training warehouse 的精确对象版本均为 0。
- Polaris test table 和两个仅用于该 marker 的 namespace 已 purge。
- 本轮临时 MLflow 数据卷已移除，可选 worker/Polaris/MLflow 运行数为 0。
- `bpi-service`、`bpi-adapter`、`bpi-wms-adapter` 均 healthy，`/bpi/` 为 HTTP 200。
- Compose project 只引用正式
  `/home/v6/adp-mes-docker-newbase-20260611-181921/deploy/docker/docker-compose.yml`。

聚合机器证据：
`metadata/bpi-dataset-training-readiness-acceptance.json`。

## 下一门槛

下一阶段不是训练三行演示模型，而是补“过程信号窗口特征”：

1. 把流量、泵、阀、液位等预测时刻之前的窗口统计接入不可变数据集。
2. 用真实生产持续积累至少 200 个已复核独立批次和 7 个生产日。
3. 降低排除率并补足 accepted/rejected 标签覆盖。
4. 重新运行同一 policy；只有返回 `ELIGIBLE` 后才规划训练作业。
