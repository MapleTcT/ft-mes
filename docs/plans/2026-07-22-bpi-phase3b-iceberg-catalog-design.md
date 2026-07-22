# BPI Phase 3B-B Iceberg 目录发布设计

## 目标与完成事实

Phase 3B-B 把 Phase 3B-A 已验证的、带 MinIO `versionId` 的 Parquet 制品发布为真实
Apache Iceberg 表快照。完成事实不是“对象存在”或“接口返回 200”，而是同时满足：

1. BPI PostgreSQL 保存一条不可混淆的目录发布任务；
2. publisher 从精确对象版本下载源 Parquet，并重新校验 SHA-256、字节数、行数和 schema；
3. publisher 通过 Apache Polaris 的 Iceberg REST catalog 提交一个带 BPI 来源属性的快照；
4. 按提交得到的 `icebergSnapshotId` 做 time-travel 回读，逐行计算语义 checksum；
5. catalog snapshot、metadata location、manifest checksum、源制品 checksum、行数和回读 checksum
   全部对账后，任务才能进入 `READY`；
6. 页面/API 展示的是上述持久化事实，不能由 Parquet `READY` 推导 Iceberg `READY`。

本阶段不包含 MLflow、模型训练、在线推断、WORM/Object Lock 或生产激活。它们继续保持
`NOT_STARTED`。MinIO versioning 仍不等同于 WORM。

> **实施状态（2026-07-22）：** 真实 PostgreSQL/MinIO/Polaris/PyIceberg 后端检查点已通过，包含
> snapshot time-travel、失败重试、重启持久化、bootstrap 幂等和清理；真实 ADP 页面 V28 验收及
> catalog commit 后 PostgreSQL fencing 故障注入仍未关闭。证据见
> [`bpi-dataset-catalog-publication-acceptance.md`](../testing/bpi-dataset-catalog-publication-acceptance.md)。

## 组件与版本决策

- Catalog server：Apache Polaris `1.4.1`，固定版本，不使用 `latest`。选择已有补丁版本，
  暂不采用刚发布且尚无补丁的 `1.6.0`。
- Python client：PyIceberg `0.11.1`，通过 Iceberg REST 协议访问 Polaris。
- 表格式：Apache Iceberg format v2，数据文件 Parquet。
- 对象存储：沿用私有 MinIO，但为 Iceberg warehouse 使用独立 bucket/prefix、独立凭据和策略。
- Catalog metastore：独立 PostgreSQL database/schema/role；不能写入 BPI 领域 schema，也不能依赖
  Polaris 的内存 metastore 作为测试环境或生产事实。
- Query engine：本阶段由 PyIceberg 做按 snapshot 回读验收。Trino 保持后续只读诊断能力，
  不进入在线事务链路。

官方依据：

- Apache Polaris 1.4.1 production configuration 要求持久化 metastore、固定 OAuth2 key、realm
  header 校验并禁用 FILE storage。
- PyIceberg `append` 支持 snapshot properties，scan 支持指定 snapshot；这两项共同提供重试
  reconcile 和 time-travel 对账所需的稳定锚点。

## 权威边界

```text
MANIFEST_READY snapshot
        |
        v
READY Parquet materialization -- exact versionId/SHA verified
        |
        | explicit API command + Idempotency-Key + If-Match
        v
catalog publication (PostgreSQL) -- QUEUED/COMMITTING/VERIFYING
        |
        v
PyIceberg -> Polaris REST catalog -> Iceberg metadata/data in private MinIO warehouse
        |
        v
time-travel read by icebergSnapshotId -> semantic checksum/row/schema verification
        |
        v
catalog publication READY
```

- BPI Service 是命令、scope、幂等、状态投影和审计的唯一入口。
- Python publisher 只领取已排队任务，不提供公网 HTTP API。
- Polaris 拥有 catalog 元数据；BPI 只保存不可变引用和对账摘要。
- publisher 不修改 Phase 3A manifest，也不修改 Phase 3B-A materialization 成功事实。
- Java 8 adapter 只精确代理 BPI 路由，不持有 catalog 凭据和 Iceberg 状态。

## API 契约

### 创建发布任务

`POST /bpi/v1/dataset-materializations/{materializationId}/catalog-publications`

- 角色：`BPI_ENGINEER` 或 `BPI_ADMIN`；
- 必需 header：`Idempotency-Key`、`If-Match`；
- body 只包含 `reason`，不允许客户端提交 catalog URI、warehouse URI、namespace 或 table name；
- 只允许源 materialization 为 `READY`，拥有非空 `versionId`、SHA、schema，且已验证行数大于零；
- 返回 `202` 与 `QUEUED` 任务；相同幂等键重放同一结果；同一发布契约只允许一条逻辑任务。

### 查询发布任务

`GET /bpi/v1/dataset-catalog-publications/{publicationId}`

- 使用源 snapshot 的 tenant/plant/line scope；
- 只有 `READY` 才返回 `icebergSnapshotId`、metadata location、table identifier、schema ID、
  partition spec ID、source/semantic checksum 和 verified row count。

### 失败重试

`POST /bpi/v1/dataset-catalog-publications/{publicationId}/retry`

- 只允许 `FAILED`；
- 必需 `Idempotency-Key`、`If-Match` 和 `reason`；
- 复用同一任务并增加 attempt，不创建第二个逻辑发布；
- retry 首先按 snapshot properties 搜索既有 commit，解决 catalog 已提交而数据库回写失败的窗口。

## PostgreSQL 模型

Flyway V28 新增 `bpi_dataset_catalog_publications`：

- 唯一键：`(tenant_id, materialization_id, catalog_name, publisher_version)`；
- 状态：`QUEUED -> COMMITTING -> VERIFYING -> READY`，任一执行态可进入 `FAILED`；
- 并发：`claim_token`、`claimed_at`、`attempt_count` 和 `FOR UPDATE SKIP LOCKED`；
- 源锚点：snapshot/materialization ID、manifest checksum、source content SHA/versionId/rows/schema；
- catalog 锚点：catalog、namespace、table identifier、Iceberg snapshot ID、metadata location、
  schema ID、partition spec ID；
- 验证结果：verified rows、semantic checksum、验证时间；
- 失败事实：failure code/detail；
- 操作事实：requested by/reason、created/started/completed timestamp、revision。

沿用现有 `bpi_audit_events` 追加记录 `QUEUED/COMMITTING/VERIFYING/READY/FAILED/RETRIED`，
`object_type` 固定为 `DATASET_CATALOG_PUBLICATION`。进入 `READY` 后通过数据库 trigger 拒绝后续
UPDATE；运行角色均不持有 DELETE 权限，验收数据只能由受控管理员清理。同一 materialization 与
publisher contract 只能存在一个逻辑发布任务，失败后复用原任务重试。

`publisher_version` 是 Iceberg 表兼容契约 ID，不是镜像构建号。修补 worker、重新构建镜像或滚动重启时
必须保持 `bpi-dataset-catalog-publisher/0.1.0` 不变；只有 schema、分区或 snapshot properties
发生不兼容演进，并且已有独立 Flyway 迁移与新表迁移方案时，才能切换该值。普通的新数据集快照通过新的
`materialization_id` 形成发布任务，不通过抬高 publisher version 绕开幂等约束。

## 表身份、schema 与分区

表身份由服务端确定，不使用用户输入：

```text
catalog:   ft_mes_bpi
namespace: bpi_training.tenant_<sha256(tenant_id)[0:16]>
table:     dataset_<dataset_definition_uuid_without_dash>
```

同一数据集定义的多个不可变 snapshot 追加到同一表。每行在 Phase 3B-A 列之外增加：

- `tenant_id`
- `plant_id`
- `dataset_id`
- `source_snapshot_id`
- `source_materialization_id`
- `source_content_sha256`

表使用 Iceberg format v2，当前按 `plant_id` identity 和 `prediction_time` day 组织；只有生产量证明
day 分区过大时，才能通过独立 schema/spec 演进把时间粒度调整为 hour。测试数据允许
产生小文件，但表属性固定 128-512 MB 的目标文件范围；真正 compaction 必须是独立维护任务并有
前后 snapshot/行数/checksum 验收，不能在本阶段伪装为已经完成。

## 幂等与部分失败恢复

每次 Iceberg append 写入 snapshot properties：

- `bpi.publication-id`
- `bpi.materialization-id`
- `bpi.source-snapshot-id`
- `bpi.manifest-checksum`
- `bpi.source-content-sha256`
- `bpi.source-object-version-id`
- `bpi.publisher-version`

publisher 执行前扫描表 snapshot summary：

- 找到同一 publication ID 且来源 checksum 一致：不再次 append，直接进入 VERIFYING；
- 找到同一 publication ID 但 checksum 不同：`CATALOG_COMMIT_CONFLICT`，禁止继续；
- 未找到：执行 append，刷新 table，捕获新 snapshot ID；
- catalog 提交成功但 PostgreSQL fencing 失败：下次领取通过 snapshot properties reconcile；
- PostgreSQL 状态已 READY：不再访问 catalog 执行写入。

## 安全与运行开关

- `BPI_DATASET_CATALOG_PUBLISHER_ENABLED=false` 为默认值；
- `BPI_POLARIS_ENABLED=false` 和 bootstrap 开关默认关闭；
- BPI API 不接受任意 catalog/warehouse URL，避免 SSRF 和跨 bucket 写入；
- catalog OAuth client secret、Polaris root credential、MinIO warehouse credential 分离；
- publisher 不持有 BPI 业务表 DELETE 权限，不持有源 Parquet bucket DELETE 权限；
- Polaris 使用固定 realm header、持久化 metastore 和固定密钥，FILE catalog 类型被禁用；
- warehouse bucket 私有并启用 versioning；Object Lock/WORM 必须另行设计 retention 与管理员流程。

## 页面状态

数据集详情保留四个独立层级：

1. Manifest：`MANIFEST_READY`
2. Parquet：`QUEUED/WRITING/READY/FAILED`
3. Iceberg catalog：`NOT_STARTED/QUEUED/COMMITTING/VERIFYING/READY/FAILED`
4. MLflow/模型：继续 `NOT_STARTED`

`READY` 展示 catalog/table/snapshot/rows/checksum；`FAILED` 展示稳定 failure code 和经过脱敏的摘要，
并提供带 revision 的重试。内部路径、凭据和原始异常栈不得直接显示给用户。

## 验收门槛

1. V28 migration、API、scope、RBAC、revision、幂等和不可变约束通过 Java/PostgreSQL 测试。
2. publisher 单元测试覆盖表名派生、源版本/SHA 校验、schema enrich、snapshot property reconcile、
   semantic checksum 和脱敏错误。
3. 真实 PostgreSQL + MinIO + Polaris 集成创建表并 append；按捕获的 Iceberg snapshot time-travel
   回读，行数、schema、来源列和语义 checksum 全部一致。
4. catalog 停机、MinIO 停机、提交后数据库 fencing 失败、重复请求和服务重启均有失败/恢复证据。
5. 桌面和移动真实页面完成 POST/GET/retry，console/page/network error 为零且无横向溢出。
6. 唯一 marker 在 BPI PostgreSQL、Polaris metastore、Iceberg warehouse 和页面/API 均可定位；
   清理只删除 marker 对应表/对象/任务，最终残留为零。
7. 验收结束后 publisher、Polaris bootstrap、materializer 及所有 Phase 2 写回开关恢复为 false。

## 后续边界

Phase 3B-B 通过后仍不能声明完整训练平台完成。后续分别处理：

- Object Lock/WORM retention、legal hold 和管理员恢复；
- 多副本 publisher 的租约心跳和 fencing；
- Iceberg compaction/expire snapshots/orphan-file 管理及保留策略；
- 只读 Trino 查询与资源隔离；
- MLflow 数据集注册、离线实验、模型审批和建议型推断。
