# BPI Phase 3B-A 可复现训练数据制品设计

## 目标

把已经进入 `MANIFEST_READY` 的 BPI 数据集快照物化为一个私有、版本固定、可校验的
Parquet 制品，并把对象引用、内容 checksum、行数、schema 和完整审计写回 PostgreSQL。
同一 manifest 在相同物化器版本下必须得到相同的逻辑数据集；任何页面或 API 都只能展示
数据库和对象存储中已经验证的事实。

本纵切是 Phase 3B-A，不等同于完整 Phase 3B。Apache Iceberg catalog/table、Trino、MLflow、
训练、推断和生产激活均不在本轮范围内，相关状态必须继续保持 `NOT_STARTED`。

## 方案选择

采用独立 Python 3.12 materializer worker：

- Java 17 BPI Service 继续拥有 REST 授权、租户/工厂/产线范围、幂等、任务创建和查询投影。
- PostgreSQL Flyway V27 新增物化任务事实表；成功制品不可变，失败任务通过显式命令重试。
- Python worker 只获得数据集定义/快照/样本的读取权限，以及物化任务和对应审计行的最小写权限。
- worker 使用固定版本 PyArrow 写 Parquet，使用 MinIO SDK 写私有 bucket，不提供公网 HTTP 入口。
- Compose 开关 `BPI_DATASET_MATERIALIZER_ENABLED` 默认 `false`；未启用时不会领取任务或写对象。

没有选择把 PyArrow/Parquet 依赖装进在线 Java 服务，因为数据制品依赖、内存峰值和对象存储故障
不应扩大 MES 事务服务的故障域。也不在本轮直接引入 Spark/Iceberg/Trino，因为在制品契约、幂等、
泄漏控制和恢复机制尚未验收前，引入 catalog 和查询集群只会增加无法证明的运行面。

## 数据模型

`bpi.bpi_dataset_materializations` 每个 `(tenant_id, snapshot_id, format,
artifact_schema_version, materializer_version)` 只有一条逻辑任务：

- `state`: `QUEUED -> WRITING -> READY`，或 `WRITING -> QUEUED/FAILED`。
- `revision`: 每次状态变化递增，用于 `If-Match` 和并发冲突检测。
- `claim_token/claimed_at/attempt_count`: `FOR UPDATE SKIP LOCKED` 领取及过期恢复。
- `manifest_checksum`: 创建任务时固定引用快照 checksum，后续不得漂移。
- `artifact_uri/object_bucket/object_key`: 仅 `READY` 时存在。
- `content_sha256/byte_size/row_count/schema_json`: 对象下载后可独立复验。
- `failure_code/failure_detail`: 失败事实，不把异常吞成成功。
- `requested_by/request_reason/created_at/completed_at`: 操作与审计边界。

成功数据库行不可修改；成功行通过 `versionId` 固定引用一个经过内容校验的对象版本。worker 身份
没有删除权限，但本轮没有启用 MinIO Object Lock/WORM，管理员保留操作仍需后续独立治理。重跑相同
manifest 使用幂等重放原任务。失败重试复用同一任务，保留 attempt 计数和审计，不创建含义相同的
第二个“成功”制品。

## Parquet 契约

制品只包含 `included=true` 的样本，并按 `line_id, prediction_time, batch_id, review_id` 稳定排序。
固定列包括快照/批次标识、产线、prediction time、feature cutoff、label available time、split、
confidence，以及受控 feature/label 列。声明未选择的 feature/label 列写 null；不会把
`source_payload`、排除样本或未来标签字段混入训练特征。

Parquet schema metadata 固定保存：

- `bpi.artifact_schema_version`
- `bpi.materializer_version`
- `bpi.snapshot_id`
- `bpi.manifest_checksum`
- `bpi.definition_checksum`
- `bpi.feature_refs`
- `bpi.label_refs`
- `bpi.row_order`

内容 checksum 对最终 Parquet 字节计算 SHA-256。对象键采用内容寻址：
`datasets/{snapshotId}/{manifestChecksum}/{materializerVersion}/{contentSha256}.parquet`。
bucket 必须开启版本控制。数据库只在上传后取得确切 `versionId`，重新 stat 该版本，并下载该版本
逐字节复算 SHA-256；byte size、内容 SHA、SHA metadata 和 manifest metadata 全部一致时才进入
`READY`。`artifact_uri` 带 `?versionId=`，不能只保存可漂移的 latest-object URI。bucket 必须已存在
且保持私有，worker 不自动创建生产 bucket。

## API 与交互

- `POST /bpi/v1/dataset-snapshots/{snapshotId}/materializations`
  - 角色：`BPI_ENGINEER` 或 `BPI_ADMIN`
  - 必需：`Idempotency-Key`、`If-Match`、reason
  - 只允许 `MANIFEST_READY` 且 manifest checksum 非空的快照
  - 返回 `202` 和 `QUEUED` 任务
- `GET /bpi/v1/dataset-materializations/{materializationId}`
  - 与快照相同的 tenant/plant/line scope
- `POST /bpi/v1/dataset-materializations/{materializationId}/retry`
  - 只允许 `FAILED`，要求当前 revision，并返回 `202`

数据集详情抽屉显示 manifest 和制品两个独立事实层。只有 `READY` 才显示 URI、checksum、行数、
字节数和 schema；`QUEUED/WRITING/FAILED` 显示真实进度或失败原因。Iceberg、MLflow 和模型状态
继续显示 `NOT_STARTED`，不能由 Parquet `READY` 推导为已完成。

## 失败与恢复

- PostgreSQL 在领取前不可用：worker 不领取任务、不写对象。上传后数据库不可用时任务可能保留
  `WRITING`，恢复后由 claim timeout 重排队；达到最大过期次数进入 `FAILED`。
- MinIO 不可用且 PostgreSQL 仍可写：本次领取立即记录为 `FAILED`，由工程师通过带 revision 的显式
  retry 再次排队，不把对象存储故障伪装成成功。
- Parquet schema/泄漏校验失败：立即 `FAILED`，错误码可检索，禁止上传或发布。
- 上传成功但数据库提交失败：内容寻址对象可能成为未引用对象；reconciliation 只删除超过保留期且
  不被任何 `READY` 行引用的对象，不能按前缀盲删。
- worker 崩溃：claim token 和 timeout 防止永久卡死；旧 token 不能提交新的状态。
- 同一任务并发：唯一约束、幂等表和 `SKIP LOCKED` 共同保证一个逻辑任务。
- Phase 3B-A 仅允许一个 worker 副本。当前没有长任务租约心跳，多副本和水平扩容必须等心跳/fencing
  专项验收通过后才能开放。
- 关闭或回滚：先把开关设为 false，等待当前 claim 释放，再回滚 worker 镜像；V27 为 expand-only，
  不删除已生成制品或表。

## 验收门槛

1. Java 单元/API/PostgreSQL 验证权限、scope、幂等、revision、状态机和审计。
2. Python 单元测试验证固定 schema、稳定排序、仅 included、未来标签不进入 feature、相同输入字节一致。
3. PostgreSQL + MinIO 集成测试真实生成 Parquet，下载后由 PyArrow 回读并核对行、schema、metadata、
   `versionId`、byte size 和 SHA-256。
4. 故障测试覆盖 bucket 缺失、MinIO 停机、claim 过期、最大重试和上传后数据库 fencing 失败。
5. 桌面和移动真实页面记录 console/page/network error，并捕获 POST/GET。
6. 目标环境使用唯一 marker 查 PostgreSQL 与 MinIO；清理后任务、审计、幂等、fixture 和对象均为 0。
7. 所有现有 Phase 2/WMS 开关以及新 materializer 开关在验收结束后恢复为 false。

## 后续边界

V27 通过后，Phase 3B-B 才评估租约心跳/多副本、Object Lock 保留策略、Iceberg REST catalog、表分区
和 compaction；只有真实 catalog snapshot
可查询且与 Parquet/manifest checksum 对账后，`icebergReady` 才能变为 true。MLflow 注册、离线实验
和建议型模型分别使用独立状态、开关和验收，不复用 Parquet `READY` 冒充完成。
