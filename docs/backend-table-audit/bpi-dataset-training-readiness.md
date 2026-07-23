# BPI 离线训练就绪评估落表审计

## 边界

Phase 3C-B 只把训练准入判断保存为 PostgreSQL 不可变事实。它不调用训练框架、不写 MLflow、不创建模型，也不允许在线推理或生产激活。

## 前端到落表链

| 前端动作 | API | Java 入口 | PostgreSQL | 外部副作用 |
| --- | --- | --- | --- | --- |
| 打开已登记数据集 | `GET .../training-readiness-assessments` | `DatasetController` -> `DatasetTrainingReadinessService` | 读取最近一次同策略 assessment | 无 |
| 执行训练资格评估 | `POST .../training-readiness-assessments` | controller -> service -> builder -> repository | 新增 assessment、audit、idempotency | 无 |
| 页面刷新或查看历史事实 | `GET /dataset-training-readiness-assessments/{id}` | controller -> service -> repository | 按 scope 读取不可变行 | 无 |

## 关键表

| 表 | 职责 | 关键事实 |
| --- | --- | --- |
| `bpi.bpi_dataset_training_readiness_assessments` | V31 训练准入权威事实 | registration/snapshot、策略、序号、19 gates、blockers、checksum、全 false phase boundary |
| `bpi.bpi_dataset_mlflow_registrations` | 锁定评估输入 | `REGISTERED` revision、row count、dataset digest、input/lineage/source 验证 |
| `bpi.bpi_dataset_snapshot_samples` | point-in-time 样本证据 | included/excluded、batch、prediction/cutoff/label time、split、标签值 |
| `bpi.bpi_shadow_runs` / `bpi.bpi_point_catalog_snapshots` | 现场周期与点位准入 | APPROVED、运行时间、固定目录与来源 READY 证据 |
| `bpi.bpi_data_quality_incidents` | 质量阻断 | 与来源运行窗口重叠的未解决 CRITICAL 事件 |
| `bpi.bpi_audit_events` | 追加式审计 | `DATASET_TRAINING_READINESS_ASSESSED` 及完整评估摘要 |
| `bpi.bpi_api_idempotency` | 命令幂等 | operation/key/request hash 和 `COMPLETED/200` 响应 |

## 不变量

- 只有 `REGISTERED` 且 revision 匹配的 Dataset Input 可以评估。
- 每次非幂等重放产生新的 sequence；既有 assessment 禁止 UPDATE/DELETE。
- assessment checksum 只由固定输入 revision、阈值、观测值、门槛结果和阶段边界计算。
- `ELIGIBLE` 不等于模型有效，更不等于生产可用。
- `trainingStarted/modelCreated/modelRegistered/onlineInferenceEnabled/productionActivationAllowed` 必须全为 false。
- 当前小样本缺过程窗口和类别覆盖时必须保存 `BLOCKED`，不能修改数据或阈值迎合结果。

## 当前证据

本地 PostgreSQL 15.18 已通过 V31 migration、scope、stale revision、幂等、两次不可变评估、19 门槛、审计和全 false 副作用边界；确定性 API 15/15、浏览器 8/8 通过。目标 `10.11.100.17` 的真实页面/API/PostgreSQL/MLflow 零副作用验收尚待执行，因此本报告当前不声明 TARGET_ACCEPTED。

## 未关闭项

- 目标 marker、重启回读、MLflow 模型表零变化和定向清理。
- 过程信号窗口特征工程、至少 200 个独立批次、7 个生产日及正反边界标签。
- 模型训练、时间切分评估、模型卡、四眼审批、shadow inference 和漂移监控。
