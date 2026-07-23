# BPI 离线训练就绪评估落表审计

## 边界

Phase 3C-B 只评估一个已登记 MLflow Dataset Input 是否具备离线训练条件。它不启动训练、
不创建模型、不注册模型、不启用在线推理，也不允许生产激活。

目标环境已在 PostgreSQL 15.18 / Flyway V31 完成真实页面、API、落表、重启与清理验收。
功能验收 PASS，当前数据资格结果为 `BLOCKED`。

## 入口与链路

| 动作 | 前端/API | 后端入口 | 落表 |
|---|---|---|---|
| 读取最新评估 | `GET /bpi/v1/dataset-mlflow-registrations/{id}/training-readiness-assessments` | `DatasetController.getLatestTrainingReadinessAssessment` | 只读 |
| 创建评估 | `POST /bpi/v1/dataset-mlflow-registrations/{id}/training-readiness-assessments` | `DatasetController.assessTrainingReadiness -> DatasetTrainingReadinessService.assess` | assessment + audit + idempotency |
| 按 ID 读取 | `GET /bpi/v1/dataset-training-readiness-assessments/{id}` | `DatasetController.getTrainingReadinessAssessment` | 只读 |

Java 8 adapter 只转发这三条精确路由，不提供通配写入口。OpenAPI 目标固定为
`BATCH_START_BOUNDARY_REVIEW_RISK`，policy 固定为
`bpi-training-readiness/batch-start-boundary-v1`。

## 主表

`bpi.bpi_dataset_training_readiness_assessments` 由
`V31__bpi_dataset_training_readiness.sql` 创建。

关键约束：

- `(tenant_id, mlflow_registration_id, objective_code, policy_version, assessment_sequence)`
  唯一。
- registration 和 source snapshot 均使用 tenant 复合外键。
- state 只允许 `ELIGIBLE/BLOCKED`，且 blocker 数量必须与 state 一致。
- revision 固定为 1；每次重新评估追加 sequence，不更新旧行。
- `phase_boundary` 必须包含 assessment-only 和五个 false 模型边界。
- `trg_bpi_dataset_training_readiness_immutable` 拒绝所有 UPDATE。
- 服务角色只获得 SELECT/INSERT，不获得 UPDATE/DELETE。

## 事务事实

创建评估在同一 PostgreSQL 事务内写入：

1. `bpi_dataset_training_readiness_assessments`：不可变评估事实。
2. `bpi_audit_events`：`DATASET_TRAINING_READINESS_ASSESSED`。
3. `bpi_api_idempotency`：请求指纹、状态、HTTP 200 和响应体。

相同 `Idempotency-Key` + 相同 payload 返回原响应；同 key 不同 payload 冲突。新的 key 会追加新的
assessment sequence，但相同冻结事实必须生成相同 `assessment_checksum`。

## 目标落表结果

marker：`ADP_E2E_BPI_READINESS_20260723_091500_A1`。

| 事实 | 实际结果 |
|---|---|
| assessment rows | 2 |
| sequences | 1, 2 |
| states | BLOCKED, BLOCKED |
| revisions | 1, 1 |
| gate count | 19, 19 |
| blocker count | 8, 8 |
| checksum distinct count | 1 |
| audit rows | 2 |
| idempotency rows | 2 个 COMPLETED/200 |
| immutable update | 被 trigger 拒绝 |

主查询：

```sql
SELECT assessment_sequence,
       state,
       revision,
       jsonb_array_length(gate_results) AS gate_count,
       jsonb_array_length(blocker_codes) AS blocker_count,
       assessment_checksum,
       phase_boundary
  FROM bpi.bpi_dataset_training_readiness_assessments
 WHERE tenant_id = '1000'
   AND mlflow_registration_id = '21edf8aa-b354-41f7-8703-0c42fc2984f1'
 ORDER BY assessment_sequence;
```

完整断言：
`deploy/docker/scripts/bpi-dataset-training-readiness-target-verification.sql`。

## 模型副作用反证

本动作只读 MLflow registration 的冻结事实。目标验收前后 MLflow 均为 1 run / 1 dataset / 1 input，
`registered_models`、`model_versions`、`logged_models` 均为 0。assessment 表也没有 model URI、
artifact URI 或 activation 字段，避免把资格评估扩展成隐式训练。

## 清理顺序

`deploy/docker/scripts/bpi-dataset-manifest-target-cleanup.sql` 已升级到 V31：

1. 捕获 registration 关联的 readiness assessment ID。
2. 删除 assessment 关联 audit 和 idempotency。
3. 删除 readiness assessment。
4. 再按 registration -> archive -> publication -> materialization -> snapshot -> definition 顺序清理。

目标清理后 readiness assessment、audit、idempotency 和所有上游 marker 投影均为 0。Polaris 表、
MinIO 精确版本和临时 MLflow 卷也已独立清理。

## 验收状态

`已恢复并验收`。该状态只说明评估机制真实可用，不代表数据已达到训练条件。下一项后端工作应为
预测时刻之前的过程信号窗口特征落表和不可变数据集投影，而不是降低 19 门槛。
