# BPI Phase 3C-A MLflow 训练数据登记设计

## 1. 目标

Phase 3C-A 把 V29 已经达到 `LOCKED`、可从精确 Object Lock 版本恢复的数据集登记到
MLflow Tracking。完成事实必须同时满足：

1. 用户从 BPI 数据集清单真实页面发起登记；
2. BPI PostgreSQL 先保存可重试、可审计、幂等的登记任务；
3. 独立 registrar 在 MLflow 中创建或复用受控 experiment，并创建一个 dataset-registration run；
4. run 通过 MLflow Dataset Input 保存数据集名称、digest、schema、profile、来源和训练候选上下文；
5. MLflow 返回的 experiment、run 和 dataset input 可重新读取，并与 BPI 的 manifest、Iceberg、
   Object Lock 版本及 semantic checksum 对账；
6. 页面刷新后仍能从 PostgreSQL 重建登记状态；失败可以在同一任务上重试；
7. 模型训练、模型注册、在线推断和生产激活仍为 `NOT_STARTED`。

本阶段登记的是可复现训练数据产品，不是训练结果，也不把一行目标验收数据解释为模型有效。

## 2. 方案选择

考虑过三种路线：

1. 直接训练一个演示模型。表面完整，但会把模拟样本和真实模型质量混在一起，拒绝采用。
2. registrar 下载 Parquet 后用 Pandas 重新计算 MLflow dataset。会扩大对象存储权限，并重复
   V27-V29 已完成的校验，首个纵切不采用。
3. 使用 MLflow `MetaDataset`/Dataset Input 语义登记不可变数据源和完整血缘。该方式不会复制数据，
   也不会让 MLflow 成为生产事实主库，是本阶段采用的方案。

MLflow 官方文档明确支持 metadata-only dataset、`mlflow.log_input()`、远程 Tracking Server、
PostgreSQL backend 和 S3-compatible artifact store。本实现使用等价的官方 REST API，避免 registrar
引入完整训练依赖。

## 3. 组件和所有权

```text
BPI 页面
  -> Java 8 adapter
    -> Java 17 bpi-service
      -> ft_mes_bpi.bpi_dataset_mlflow_registrations
        -> bpi-dataset-mlflow-registrar (Python 3.12)
          -> MLflow 3.14 Tracking REST
            -> 独立 MLflow PostgreSQL backend
            -> 独立 MinIO artifact bucket
```

- `bpi-service`：鉴权、租户/工厂范围、前置条件、幂等、任务事实和审计。
- `bpi-dataset-mlflow-registrar`：唯一外部写入者，领取任务、调用 MLflow、读回验证和 fencing 写回。
- MLflow：实验、run 和 dataset input 的协作/检索面，不是 MES 生产事实主库。
- BPI PostgreSQL：保存 MLflow 外部身份、完整 checksum、状态机和审计，支持断点恢复。
- registrar 不读取 MinIO、Polaris、Kafka、WOM、QCS 或 WMS；它只能读必要的 BPI 数据集表并更新
  自己的任务表。

## 4. 前置条件和不可变血缘

只允许从 `LOCKED` 的 `bpi_dataset_retention_archives` 创建登记任务。服务端必须验证：

- `objectLockVerified=true`；
- `recoveryVerified=true`；
- retained source 和 manifest 均有精确 `versionId`；
- source row count、source SHA-256、catalog semantic checksum 与 V28/V29 事实一致；
- `modelTrained=false`；
- 同一租户、archive 和 registrar version 只能有一个任务。

MLflow dataset name 使用受控的 dataset code；digest 使用 semantic checksum 前 16 位，完整 64 位
checksum 作为 run tag 和 BPI 不可变字段保留。source 使用：

```text
s3://<recovery-bucket>/<source-object-key>?versionId=<exact-version-id>
```

schema 来自 V27 固化的 `source_schema_json`，profile 至少包含行数、字节数、Iceberg snapshot、
manifest checksum 和 label/feature cutoff 版本信息。

## 5. PostgreSQL 状态机

V30 新增 `bpi.bpi_dataset_mlflow_registrations`：

```text
QUEUED -> REGISTERING -> REGISTERED
   ^          |              terminal immutable
   |          +-> FAILED
   +-------------- retry ----+
```

关键字段包括：

- tenant、archive/publication/materialization/snapshot 身份；
- registrar version、tracking profile、experiment name、dataset name/digest；
- manifest/source/semantic checksum、exact source version、Iceberg snapshot；
- claim token、claimed_at、attempt_count、revision；
- MLflow experiment ID、run ID、artifact URI、dataset source、验证 metadata；
- failure code/detail、requested_by/reason 和时间戳。

触发器阻断非法状态跳转、revision 跳号、身份改写和 `REGISTERED` 后更新。外部调用成功但 BPI
写回前崩溃时，registrar 通过确定性 run tag 搜索既有 run，验证一致后完成 fencing 写回，不创建重复 run。

## 6. API

新增四个公开操作，全部沿用现有 JWT、RBAC、`Idempotency-Key` 和 `If-Match` 约束：

| Method | Path | 作用 |
| --- | --- | --- |
| POST | `/bpi/v1/dataset-retention-archives/{archiveId}/mlflow-registrations` | 创建登记任务 |
| GET | `/bpi/v1/dataset-retention-archives/{archiveId}/mlflow-registrations` | 按恢复包查询 |
| GET | `/bpi/v1/dataset-mlflow-registrations/{registrationId}` | 查询任务详情 |
| POST | `/bpi/v1/dataset-mlflow-registrations/{registrationId}/retry` | 同任务失败重试 |

写操作仅 `BPI_ENGINEER`、`BPI_ADMIN` 可执行。接口返回 `202`，不能把排队成功伪装成 MLflow
登记成功。

## 7. MLflow 写入契约

registrar 使用内部 Tracking REST 完成：

1. 按受控名称查询或创建 experiment；
2. 以确定性 tags 搜索同一 BPI registration，存在时先做血缘一致性校验；
3. 创建 run，写入 BPI identity/checksum/Object Lock/production boundary tags；
4. 调用 `runs/log-inputs` 写入 dataset input，context 为 `training_candidate`；
5. 将 run 标记 `FINISHED`；
6. 重新读取 run inputs，逐项核对 name、digest、source、schema、profile 和 tags；
7. 以 claim token 和 revision fencing 更新 BPI 为 `REGISTERED`。

任何 4xx/5xx、超时、响应缺字段或读回不一致都进入 `FAILED`，保存稳定 failure code；不得返回
假成功。模型相关 tags 固定为 `modelTrained=false`、`modelRegistered=false`、
`onlineInferenceEnabled=false`、`productionActivationAllowed=false`。

## 8. 部署和安全边界

- MLflow 固定为 `3.14.0`，使用独立 PostgreSQL backend；不用 SQLite 作为共享运行路径。
- MLflow artifact store 使用独立 MinIO bucket 和 server-only 凭据。
- MLflow 与 registrar 放在独立 `bpi-ml` Compose profile，不映射宿主机端口。
- registrar 和 MLflow 默认关闭；只有受控验收或批准环境显式开启。
- registrar 使用独立 `bpi_mlflow_registrar` PostgreSQL 角色，不得读取业务批次、人员或平台库。
- 首个纵切不把内部网络当成生产认证。正式发布前仍需 MLflow RBAC/SSO、TLS、secret rotation、
  备份恢复、容量和跨故障域演练。

## 9. 页面交互

数据交付链扩展为：

```text
Manifest -> Parquet -> Iceberg -> 恢复包 -> MLflow 数据集 -> 模型
```

恢复包为 `LOCKED` 后显示“登记训练数据”命令。详情卡显示：

- `QUEUED / REGISTERING / REGISTERED / FAILED`；
- experiment、run、dataset digest、exact source version；
- manifest/source/semantic checksum 对账；
- Dataset Input 和 Object Lock 验证状态；
- `模型未训练` 和 `生产未激活` 边界。

失败态提供同任务重试。桌面和 390x844 移动视图均须无横向溢出、console error、page error、
request failure 或非预期 HTTP 错误。

## 10. 验收和回滚

本地验收：

- V30 PostgreSQL 约束、状态机、幂等、租户隔离和 fencing；
- registrar 单元测试覆盖创建、既有 run 发现、读回不一致、超时、失败重试和 stale claim；
- 真实 MLflow/PostgreSQL 组件测试验证 experiment/run/dataset input；
- OpenAPI、Java 8 adapter、模拟器和浏览器 E2E。

目标验收使用唯一 marker，必须经过真实页面、API、BPI PostgreSQL 和真实 MLflow。受控关闭 MLflow
先形成 `FAILED`，恢复后从页面重试同一任务到 `REGISTERED`；随后验证服务重启读回、无重复 run、
默认开关恢复关闭和 marker 定向清理。验收 MLflow backend 使用测试专用临时卷，结束后停止并移除，
不得污染现有 ADP 数据。

V30 是 expand-only。回滚只关闭 registrar/MLflow profile 和前端入口，不删除迁移表、不降级 Flyway、
不修改 V26-V29 已完成事实。

## 11. 明确不在本阶段

- 模型训练、评估、审批、Model Registry 或在线推断；
- Feast/在线特征；
- 物理来源连续 7-14 天、正式校准证书和现场签字；
- 外部 ERP/WMS 生产实例；
- 整站灾备、生产 RPO/RTO、正式容量或生产留存策略结论；
- 对 PLC/DCS 的任何闭环控制。

## 12. 参考

- MLflow Dataset Tracking: <https://mlflow.org/docs/latest/dataset/>
- MLflow Tracking: <https://mlflow.org/docs/latest/ml/tracking/>
- MLflow Tracking Server: <https://mlflow.org/docs/latest/self-hosting/architecture/tracking-server/>
- MLflow REST API: <https://mlflow.org/docs/latest/api_reference/rest-api.html>
