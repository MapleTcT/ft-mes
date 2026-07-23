# BPI V30 MLflow Dataset Input 目标验收

## 结论

| 项目 | 结果 |
| --- | --- |
| 组合状态 | `PASS_TARGET_BROWSER_API_POSTGRES_MLFLOW_MINIO_RESTART_CLEANED` |
| 目标环境 | `10.11.100.17`，唯一 Compose project `adp-mes-newbase` |
| 数据库 | PostgreSQL `15.18`，Flyway `V30` |
| 运行版本 | `27ab9a5260c197b30747f405741dcdb6105187b1` |
| marker | `ADP_E2E_BPI_MLFLOW_20260723_022000_A1` |
| 浏览器 | 真实 ADP 页面，桌面、重启后重发现和 `390x844` 移动视口均通过 |
| 最终运行态 | 主 BPI 三服务健康，`/bpi/` 为 `200`；MLflow、registrar 及其他可选 sidecar 全部停止，相关开关全部关闭 |

本次结论只关闭 Phase 3C-A 的“不可变训练数据能够作为 MLflow Dataset Input 登记并保持完整来源血缘”门槛。它不证明模型已经训练、注册、审批或上线，也不改变 BPI 总目标 `PARTIAL`。

## 验收链

同一 marker 从真实页面完成六阶段交付链：

1. V26 不可变 manifest；
2. V27 精确版本 Parquet；
3. V28 Polaris/Iceberg snapshot；
4. V29 Object Lock 恢复包；
5. V30 MLflow Dataset Input 登记；
6. PostgreSQL、MLflow、MinIO、Polaris、对象和临时运行资源定向清理。

关键身份如下：

| 对象 | 标识 |
| --- | --- |
| dataset | `3873cd7e-656b-42bd-9a47-10ea2b9e3b94` |
| snapshot | `d19ca215-06b7-40e6-a83e-440240e22320` |
| materialization | `63398485-9bb4-4fb4-ae7b-caec3bd28bee` |
| catalog publication | `eafaa002-45ee-476e-8298-63d8cc755b0e` |
| retention archive | `3184209e-f81a-4c54-a23a-10e63755fbbd` |
| MLflow registration | `df8653ea-1aac-43c6-bba5-cd7f8c6a5ead` |
| MLflow experiment | `1` |
| MLflow run | `c84549b0748d413291c9018096da9a80` |
| Iceberg snapshot | `4859061201334947183` |
| semantic checksum | `17a8dde12ed964b0c3cfe4dcf023b5aedf8ecaf0b7615909787119aa79b34142` |

## 真实页面与 API

| 操作 | 请求 | 页面/API 结果 | PostgreSQL/MLflow 结果 | 状态 |
| --- | --- | --- | --- | --- |
| MLflow 不可用时请求登记 | `POST /bpi-api/dataset-retention-archives/3184209e-f81a-4c54-a23a-10e63755fbbd/mlflow-registrations` | `202`；真实页面显示 `FAILED/r3/attempt1` 和 `MLFLOW_TRANSPORT_ERROR` | PostgreSQL 持久化失败；空 MLflow backend 保持 `0 run / 0 input` | PASS |
| 恢复后页面重试 | `POST /bpi-api/dataset-mlflow-registrations/df8653ea-1aac-43c6-bba5-cd7f8c6a5ead/retry` | `202`；复用同一 registration ID，显示 `REGISTERED/r6/attempt2` | MLflow 恰有 `1 run / 1 dataset / 1 input`，模型相关表仍为 `0` | PASS |
| 重启后重新发现 | registration GET 和六阶段关联 GET | 页面在 registrar 重启后仍读取同一 experiment/run/source；桌面和移动端错误均为 0 | run 数保持 `1`，未重复创建外部事实 | PASS |

首次失败不是浏览器模拟：验收先启动全新的临时 MLflow backend，再真实停止 Tracking Server，保留 registrar 运行。registrar 无法连接后只写入 BPI 失败事实，不产生 MLflow 副作用。恢复 Tracking Server 后，页面按 revision 重试同一任务并完成登记。

## PostgreSQL 落库

`bpi.bpi_dataset_mlflow_registrations` 最终为：

- `REGISTERED/r6/attempt2`；
- experiment `1`、run `c84549b0748d413291c9018096da9a80`；
- dataset digest `17a8dde12ed964b0`；
- 精确来源为带 `versionId` 的 Object Lock 对象；
- `sourceFactsVerified=true`；
- `datasetInputVerified=true`；
- `lineageVerified=true`；
- `modelTrained=false`、`modelRegistered=false`、`onlineInferenceEnabled=false`、`productionActivationAllowed=false`。

审计顺序严格为：

```text
DATASET_MLFLOW_REGISTRATION_QUEUED/r1
DATASET_MLFLOW_REGISTRATION_REGISTERING/r2
DATASET_MLFLOW_REGISTRATION_FAILED/r3
DATASET_MLFLOW_REGISTRATION_RETRIED/r4
DATASET_MLFLOW_REGISTRATION_REGISTERING/r5
DATASET_MLFLOW_REGISTRATION_REGISTERED/r6
```

请求和重试两条写路径均持久化为 `COMPLETED/202`，没有创建第二个 registration。

## MLflow 与血缘

独立 MLflow PostgreSQL backend 的最终事实为：

- `runs=1`、`inputs=1`、`datasets=1`；
- `registered_models=0`、`model_versions=0`、`logged_models=0`；
- run 状态为 `FINISHED`；
- input context 为 `training_candidate`；
- tag `bpi.registration_id=df8653ea-1aac-43c6-bba5-cd7f8c6a5ead`；
- 数据源精确为：

```text
s3://bpi-dataset-recovery/archives/tenant_40510175845988f1/eafaa002-45ee-476e-8298-63d8cc755b0e/3184209e-f81a-4c54-a23a-10e63755fbbd/source.parquet?versionId=ea4c2797-b60e-4a22-bafe-3742f182a9d3
```

registrar 在调用 MLflow 前重新校验恢复包冻结事实。`sourceFactsVerified=false` 会在任何 MLflow side effect 之前失败；通过后，完整 checksum、Iceberg snapshot、Object Lock version、archive manifest 和 BPI registration ID 一并进入 run/dataset tags 与 BPI 登记 metadata。

## MinIO 最小权限

MLflow artifact bucket 保持私有。验收使用 scoped credential 证明：

| 权限事实 | 结果 |
| --- | --- |
| 列举自身 artifact bucket | 允许 |
| 管理员权限 | 无 |
| 列举 recovery bucket | 拒绝 |
| 删除 recovery exact version | 拒绝 |

登记为 metadata-only，MLflow artifact run prefix 在验收前后均为 `0`；registrar 不复制受 Object Lock 保护的训练源数据。

## 重启、幂等与清理

- registrar 重启后从 BPI PostgreSQL 重新发现同一 `REGISTERED/r6` 任务；
- MLflow run 数始终为 `1`，没有因重启或页面刷新重复登记；
- 测试 Polaris table/tenant namespace 和 training warehouse `6` 个版本已清除；
- V27 source exact version、V29 两个 GOVERNANCE exact versions 已由管理员定向清除；
- 两个测试专用 MLflow 临时卷已精确移除；
- definition、snapshot、sample、materialization、publication、archive、registration、audit、idempotency 和 fixture marker 均为 `0`；
- 所有可选 sidecar 停止，MLflow/catalog 相关开关全部为 `false`；
- 最终 `/bpi/` 为 `200`，主 BPI service、adapter、WMS adapter 均健康且镜像 revision 与 `27ab9a52` 一致。

## 证据资产

- `metadata/bpi-dataset-mlflow-registration-acceptance.json`
- `metadata/bpi-integrated-upgrade-v30-redeploy-target.json`
- `metadata/bpi-dataset-mlflow-failed-target.png`
- `metadata/bpi-dataset-mlflow-registered-target.png`
- `metadata/bpi-dataset-mlflow-registered-mobile-target.png`
- `deploy/docker/scripts/adp-bpi-dataset-mlflow-target-acceptance.js`
- `deploy/docker/scripts/bpi-dataset-mlflow-target-verification.sql`
- `deploy/docker/scripts/bpi-dataset-manifest-target-cleanup.sql`

截图 SHA-256：

| 文件 | SHA-256 |
| --- | --- |
| failed | `d0468be9210048aff8258303207975ed94db75d99dfafdcaf315397a96ec0073` |
| registered desktop | `7711dbcba07507c6e668c774896c8f9875d4cd24b4c3d834e8247bac3559b31a` |
| registered mobile | `4bc0fd7120e1a16fa358f130e448fc8e41cfc7de73a81ecb70d73afea263c524` |

V30 受控重部署报告为 `metadata/bpi-integrated-upgrade-v30-redeploy-target.json`，状态 `PASS/COMPLETE`，采用 `VALIDATE_EXISTING_SCHEMA`，Flyway 保持 `30 -> 30`，不降级 schema。

## 未关闭边界

- 没有训练模型、注册模型版本、审批模型或启用在线推断。
- 没有打开生产激活，全部模型/推断/激活标志保持 `false`。
- 没有完成 MLflow 生产 RBAC/SSO、TLS、secret rotation、HA、容量和全站灾备。
- 没有完成物理设备、正式校准、连续 7-14 天现场运行和外部 ERP/WMS。
- `G-021` 必须继续保持 `PARTIAL`。
