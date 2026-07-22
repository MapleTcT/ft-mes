# BPI Phase 3B-C Object Lock 恢复包设计

## 目标

Phase 3B-C 为已完成 Iceberg 发布的训练数据集建立可恢复、不可静默覆盖的证据副本。完成事实必须同时满足：

1. 只有 `READY` 的 catalog publication 才能申请恢复包；
2. archiver 从 Phase 3B-A 的精确 `versionId` 下载源 Parquet，并重新校验 SHA-256、字节数、行数、schema 和发布语义 checksum；
3. 源 Parquet 与 canonical evidence manifest 写入独立、私有、启用 Object Lock 的 bucket；
4. 每个对象按精确 version 读取并验证 retention mode、retain-until、legal hold、SHA 和大小；
5. 从锁定副本重新构造发布数据后，行数和语义 checksum 必须与原 Iceberg snapshot 一致；
6. PostgreSQL 只在上述事实全部成立后进入 `LOCKED`，页面不能由“上传成功”推导不可变恢复已就绪；
7. 目标验收必须从锁定副本在隔离 namespace/table 中重建 Iceberg 数据、time-travel 回读、对账并清理恢复表。

本阶段不包含 MLflow、模型训练、在线推断、生产容量结论或整个 BPI 平台的灾备声明。

## 为什么使用独立恢复 bucket

在线 Iceberg warehouse 需要执行 snapshot expiration、compaction、orphan-file 清理和表维护。直接对 warehouse 设置默认 WORM 会让正常维护积累不可删除对象，并可能使事务提交或回滚失效。

因此边界固定为：

```text
versioned source Parquet + READY Iceberg publication
                     |
                     | explicit scoped command
                     v
retention archive task (PostgreSQL)
                     |
                     v
exact source version -> re-verify -> canonical evidence manifest
                     |
                     v
private Object Lock recovery bucket
  source.parquet (retained exact version)
  evidence.json  (retained exact version)
                     |
                     v
download exact archive versions -> reconstruct -> checksum/row verification
                     |
                     v
LOCKED
```

- live source bucket 和 Iceberg warehouse 保持 versioning，不把它们冒充 WORM；
- `bpi-dataset-recovery` bucket 在创建时使用 `--with-lock`，不能在普通 bucket 上事后伪造；
- archiver 身份不持有 `DeleteObject`、retention bypass、bucket policy 或管理员权限；
- root/admin 的 governance bypass 只用于受控测试清理，必须记录在验收报告；
- production 可选择 `COMPLIANCE`，但启用后在到期前连管理员也不能删除，必须先确认法规保留期和容量。

## 数据库与状态机

Flyway V29 新增 `bpi_dataset_retention_archives`：

```text
QUEUED -> ARCHIVING -> VERIFYING -> LOCKED
              |             |
              +-----> FAILED <----+
                         |
                         +-> QUEUED (same task retry)
```

任务唯一键为 `(tenant_id, catalog_publication_id, archiver_version)`。记录以下不可变锚点：

- dataset/snapshot/materialization/publication ID；
- manifest checksum、源 SHA/versionId/bytes/rows/schema；
- table identifier、Iceberg snapshot/metadata/schema/spec；
- catalog verified rows 与 semantic checksum；
- archive profile 和 archiver contract version。

worker 首次领取时锁定 retention mode、retain-until 与 legal-hold 策略；重试不能缩短保留期。`LOCKED` 行禁止后续 UPDATE，运行角色无 DELETE 权限。

## API

- `POST /bpi/v1/dataset-catalog-publications/{publicationId}/retention-archives`
- `GET /bpi/v1/dataset-catalog-publications/{publicationId}/retention-archives`
- `GET /bpi/v1/dataset-retention-archives/{archiveId}`
- `POST /bpi/v1/dataset-retention-archives/{archiveId}/retry`

写请求要求 `BPI_ENGINEER` 或 `BPI_ADMIN`、`Idempotency-Key`、`If-Match` 和原因。客户端不能提交 bucket、key、retention mode、保留期、catalog URI 或凭据。

## 恢复包

对象前缀由服务端固定：

```text
archives/tenant_<tenant-sha16>/<publication-id>/<archive-id>/
```

包含：

- `source.parquet`：原 Phase 3B-A 精确对象版本的字节副本；
- `evidence.json`：canonical JSON，记录所有来源锚点、归档对象 version/SHA/bytes、原 Iceberg snapshot 事实、retention 策略和产品边界。

manifest schema 为 `bpi.dataset-recovery-archive.v1`。manifest SHA 存入 PostgreSQL，manifest 本身最后上传，防止半成品恢复包进入 `LOCKED`。

## 幂等与故障恢复

- 稳定 prefix 与 object key 由 archive ID 决定；
- 重试先读取当前 exact object version，metadata/SHA/size/retention 全部一致时复用，不再写新版本；
- 同 key 已存在但内容或 archive identity 不一致时返回 `ARCHIVE_OBJECT_CONFLICT`；
- 对象已锁定但 PostgreSQL fencing 失败时，下次领取复用相同 version 并继续 VERIFYING；
- 任一对象缺失、锁定不足、checksum 不符或 restore checksum 不符都进入 `FAILED`；
- 错误详情必须脱敏，不能暴露 endpoint、credential 或内部绝对路径。

## 配置与默认关闭

- `BPI_DATASET_RETENTION_ARCHIVER_ENABLED=false`
- `BPI_DATASET_RECOVERY_BUCKET_BOOTSTRAP_ENABLED=false`
- `BPI_DATASET_RECOVERY_MODE` 必须为 `GOVERNANCE` 或 `COMPLIANCE`
- `BPI_DATASET_RECOVERY_RETENTION_DAYS` 必须为 1-36500
- `BPI_DATASET_RECOVERY_LEGAL_HOLD_ENABLED=false`

测试环境使用 `GOVERNANCE`，证明普通 archiver 身份不可删除；验收后由管理员带 `--bypass` 精确删除 marker prefix。生产只有在合规保留期、容量和管理员流程签字后才能切换 `COMPLIANCE`。

## 页面状态

数据集详情增加第五层：

1. Manifest；
2. Parquet；
3. Iceberg catalog；
4. Object Lock 恢复包：`NOT_STARTED/QUEUED/ARCHIVING/VERIFYING/LOCKED/FAILED`；
5. MLflow/模型：继续 `NOT_STARTED`。

`LOCKED` 显示 archive profile、retention mode、retain-until、legal hold、对象数量/字节、manifest SHA 和恢复校验行数/checksum。内部 bucket/key 只在受权详情中返回，页面不展示凭据或 endpoint。

## 验收门槛

1. V29 migration、scope、RBAC、revision、幂等、终态不可变和角色最小权限通过 PostgreSQL 测试；
2. archiver 单测覆盖 exact source version、canonical manifest、Object Lock bucket gate、保留期、legal hold、部分写入 reconcile、冲突、失败重试和脱敏；
3. 真实 MinIO 创建 Object Lock bucket，普通 archiver 身份对锁定 object version 删除得到拒绝；
4. 从 locked source version 在隔离 Iceberg table 重建，行数和 semantic checksum 与原 publication 完全一致；
5. API/UI 完成 request、FAILED/retry、LOCKED、重启读取，桌面/移动 console/page/network error 为零；
6. PostgreSQL、MinIO archive objects、Iceberg recovery table 和审计/幂等都能用同一 marker 定位；
7. 测试使用 governance bypass 精确清理，最终 marker 为零；所有 archiver/bootstrap/publisher/materializer/Polaris 开关恢复 false；
8. `README`、API 目录、目标账本和机器报告必须与实际完成边界一致。

## 仍未关闭的边界

- Polaris metastore、BPI PostgreSQL 全库和 Kafka/Flink checkpoint 的整站灾备；
- 多副本/异地对象存储、RPO/RTO 容量压测；
- Iceberg snapshot expiration、compaction 与 orphan-file 策略；
- MLflow、模型训练/审批、在线或 shadow inference；
- 物理设备、正式校准、连续 7-14 天和外部 ERP/WMS。
