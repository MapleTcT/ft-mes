# BPI MLflow Dataset Input 落表审计

## 边界

Phase 3C-A 使用 BPI PostgreSQL 保存受控状态机和权威血缘，使用独立 MLflow PostgreSQL 保存 experiment、run、dataset 和 input。MLflow 不是 MES 生产事实主库，`REGISTERED` 也不是模型可用状态。

## 前端到落表链

| 前端动作 | API | Java 入口 | Worker 入口 | BPI PostgreSQL | 外部事实 |
| --- | --- | --- | --- | --- | --- |
| 从 LOCKED 恢复包申请登记 | `POST /bpi/v1/dataset-retention-archives/{archiveId}/mlflow-registrations` | `DatasetMlflowRegistrationController` -> `DatasetMlflowRegistrationService` | 无，先写 `QUEUED` | `bpi_dataset_mlflow_registrations`、`bpi_audit_events`、`bpi_api_idempotency` | 无 |
| registrar 领取并登记 | 内部 claim/complete repository 合同 | 无 | `MlflowRegistrarWorker` -> `MlflowClient` | `REGISTERING`，完成后 `REGISTERED` 或失败后 `FAILED` | MLflow experiment/run/dataset/input/tags |
| 页面失败重试 | `POST /bpi/v1/dataset-mlflow-registrations/{registrationId}/retry` | service optimistic revision/idempotency | 重新领取同一 ID | `RETRIED -> REGISTERING -> REGISTERED/FAILED` | 复用或校验同一 run，不重复创建 |
| 页面刷新/重启重发现 | registration GET 和上游关联 GET | controller/service/repository read | 无 | 读取同一 registration revision | 按 run ID 复核 source/input/tags |

## 关键表

| 表 | 职责 | 关键字段/事实 |
| --- | --- | --- |
| `bpi.bpi_dataset_mlflow_registrations` | BPI 登记权威状态机 | archive/tenant/plant/line、state、revision、attempt、experiment/run、versioned source、digest、checksum、registration metadata、failure |
| `bpi.bpi_audit_events` | 追加式状态审计 | `DATASET_MLFLOW_REGISTRATION_*` 六态、before/after revision、actor、payload |
| `bpi.bpi_api_idempotency` | 请求和重试幂等 | operation、idempotency key、request hash、`COMPLETED/202` response |
| MLflow `experiments` / `runs` | 外部实验与 run | experiment `1`、run `c84549b0748d413291c9018096da9a80`、`FINISHED` |
| MLflow dataset/input 关联表 | metadata-only Dataset Input | digest、schema/profile/source、context `training_candidate` |
| MLflow model 表 | 模型注册反证 | 本轮 `registered_models/model_versions/logged_models=0` |

## 不变量

- 只有上游 archive 为 `LOCKED` 且 Object Lock、恢复、行数、checksum 和精确版本全部复验通过，才能创建任务。
- `sourceFactsVerified=false` 必须在任何 MLflow side effect 之前拒绝。
- 失败重试复用同一 BPI registration；已存在的合法 run 必须读回对账，不能重复创建。
- `REGISTERED` 必须同时满足 `sourceFactsVerified`、`datasetInputVerified`、`lineageVerified`。
- `modelTrained`、`modelRegistered`、`onlineInferenceEnabled`、`productionActivationAllowed` 在本阶段必须为 `false`。
- registrar 只能访问 V30 所需 BPI 表；MLflow artifact identity 不得列举或删除 recovery bucket。

## 目标验收

marker `ADP_E2E_BPI_MLFLOW_20260723_022000_A1` 的真实目标结果：

```text
QUEUED/r1
REGISTERING/r2
FAILED/r3/MLFLOW_TRANSPORT_ERROR
RETRIED/r4
REGISTERING/r5
REGISTERED/r6
```

PostgreSQL 两条写请求均为 `COMPLETED/202`。MLflow 停机失败后为 0 run/0 input；恢复并重试后为 1 FINISHED run、1 dataset、1 input，registrar 重启后仍为 1。完整验收见 [V30 MLflow Dataset Input 目标验收](../testing/bpi-dataset-mlflow-registration-acceptance.md) 和 `metadata/bpi-dataset-mlflow-registration-acceptance.json`。

## 未关闭项

- MLflow 生产 RBAC/SSO、TLS、secret rotation、HA、备份恢复和容量。
- Phase 4 模型训练、模型版本注册、四眼审批、建议型推断和漂移监控。
- 物理设备、正式校准、连续 7-14 天现场数据和外部 ERP/WMS。
