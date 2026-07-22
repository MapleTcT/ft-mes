# BPI Parquet 版本化物化目标验收

## 结论

2026-07-22 在唯一测试环境 `10.11.100.17 / adp-mes-newbase` 完成 BPI Phase 3B-A
Parquet 物化的真实页面、API、PostgreSQL、MinIO、失败恢复、服务重启和清理验收，状态为
**`PASS_TARGET_BROWSER_API_POSTGRES_MINIO_FAILURE_RETRY_RESTART_CLEANED`**。

目标运行镜像源码 revision 为 `7410320a45687968e6bd788cba7ebda4ec1cd7d9`，目标验收脚本
revision 为 `43e45ff33934cee83752c025be861c94f5b4938d`。受保护 expand-only 升级报告位于
`/home/v6/adp-mes-docker-newbase-20260611-181921/backups/bpi-v27/`
`bpi-integrated-upgrade-20260722T024558Z.json`，结果为 `PASS/COMPLETE`，目标数据库为
PostgreSQL `15.18`、Flyway `27`。

验收 marker 为 `ADP_E2E_BPI_PARQUET_20260722_105844_A1`。真实 ADP 页面先创建数据集定义
`64155718-d458-480e-9031-cbdb867896a8` 和 `MANIFEST_READY` 快照
`117f0045-cf03-4177-8010-dc730c566f13`；随后同一个物化任务
`02a0c765-c0f9-4c1b-a6cc-1e2dc2ca5983` 经历
`QUEUED -> WRITING -> FAILED -> QUEUED -> WRITING -> READY`。失败、重试和成功均通过页面操作，
没有直接修改物化状态。

本轮只证明版本锁定 Parquet 制品纵切。不可变 manifest 仍保持 `MANIFEST_ONLY / NOT_STARTED`；
Iceberg、MLflow、模型训练与生产推断均保持 `NOT_STARTED`，不在本轮完成声明内。

## 功能验收

| 模块 | 页面/路由 | 操作 | API | 前端结果 | 后端/落库结果 | 状态 |
|---|---|---|---|---|---|---|
| 数据集清单 | `/bpi/#/datasets` | 真实登录、创建定义、冻结快照并等待清单 | `POST /bpi/v1/datasets`；`POST /bpi/v1/datasets/{id}/snapshots` | definition 200、snapshot 202、页面到 `MANIFEST_READY`，桌面/移动错误为 0 | PostgreSQL 写入 definition、snapshot、3 个样本和审计/幂等记录 | PASS |
| Parquet 请求 | 清单详情抽屉 | 点击“生成 Parquet”并填写依据 | `POST /bpi/v1/dataset-snapshots/{snapshotId}/materializations` | 202，页面显示排队和轮询状态 | 单一 `QUEUED/r1` 任务、幂等响应 202、`DATASET_MATERIALIZATION_QUEUED` 审计 | PASS |
| 受控失败 | 同一详情抽屉 | 正式 Worker 镜像以不可写工作目录处理任务 | materialization GET | 页面显示 `FAILED/r3` 和 `MATERIALIZATION_ERROR` | `attempt_count=1`；URI、SHA 为空；审计含 `WRITING`、`FAILED` | PASS |
| 页面重试 | 同一失败详情 | 点击“重新排队”并填写恢复依据 | `POST /bpi/v1/dataset-materializations/{id}/retry` | 202，页面从 FAILED 到 `READY/r6`，显示 `VERIFIED` | 同一任务 `attempt_count=2`；审计含 `RETRIED`、第二条 `WRITING`、`READY` | PASS |
| 版本锁定对象 | READY 详情 | 检查 URI、SHA、大小、行数、schema 和对象版本 | materialization GET | 显示精确 URI、SHA、11.1 KiB、1 行、26 字段、对象版本 | PostgreSQL 保存 bucket/key/versionId/SHA；MinIO 精确版本重新下载校验通过 | PASS |
| Manifest 边界 | READY 详情 | 对比 manifest 与物化投影 | snapshot GET | manifest 区仍显示 `MANIFEST_ONLY / NOT_STARTED / -`；Parquet 区显示 READY | 快照 manifest JSON 未被物化 Worker 改写，checksum 与任务一致 | PASS |
| 响应式交互 | 桌面 `1440x900`；移动 `390x844` | FAILED、READY 和重启后读取 | 页面 GET/轮询 | console/page/request error 均为 0；移动宽度 `390/390/390`，无横向溢出 | 不适用 | PASS |
| 重启持久化 | 同一 READY 任务 | 重启 `bpi-service` 后重新登录和打开页面 | list/snapshot/materialization GET | 服务恢复 healthy，页面仍为 `READY / VERIFIED` | PostgreSQL 仍返回同一 ID、`r6`、attempt 2 和对象事实 | PASS |
| 最小权限 | 目标 Worker | 尝试删除精确对象版本 | MinIO S3 API | 不适用 | Worker 专用用户返回 `AccessDenied`；未获得删除版本权限 | PASS |
| 精确清理 | 管理员一次性容器 + SQL | 删除该 versionId，再清理 marker | MinIO admin + PostgreSQL SQL | 不适用 | MinIO 版本 `1 -> 0`；11 类 marker 投影和 4 条关联幂等记录归零 | PASS |

## API 与后端链路

```text
ADP 页面
  -> /bpi-api Java 8 adapter 精确路由
  -> DatasetController
  -> DatasetMaterializationService
  -> DatasetMaterializationPostgresRepository / DatasetPostgresRepository
  -> bpi_dataset_materializations / bpi_audit_events / bpi_api_idempotency

Python 3.12 materializer
  -> FOR UPDATE SKIP LOCKED 认领任务
  -> 从 bpi_dataset_snapshot_samples 读取 included 样本
  -> PyArrow 固定 schema、稳定排序、zstd:3 写 Parquet
  -> 私有版本化 MinIO bucket
  -> 按 versionId 重新下载并复算 SHA-256
  -> PostgreSQL READY + 审计
```

首次页面请求 payload：

```json
{
  "artifactFormat": "PARQUET",
  "reason": "ADP_E2E_BPI_PARQUET_20260722_105844_A1 目标对象失败恢复验收"
}
```

重试 payload：

```json
{
  "reason": "ADP_E2E_BPI_PARQUET_20260722_105844_A1 对象存储恢复后页面重试"
}
```

两次 POST 均返回 HTTP `202`，`bpi_api_idempotency` 中均为 `COMPLETED/202`。写操作要求
`Idempotency-Key` 和 `If-Match`，重试复用同一物化 ID，不创建第二条业务任务。

## PostgreSQL 证据

关键查询：

```sql
SELECT state, revision, attempt_count, artifact_format,
       artifact_schema_version, materializer_version,
       manifest_checksum, artifact_uri, object_bucket, object_key,
       content_sha256, byte_size, row_count,
       artifact_metadata ->> 'objectVersionId' AS object_version_id
FROM bpi.bpi_dataset_materializations
WHERE tenant_id = '1000' AND id = :materialization_id;

SELECT action, count(*)
FROM bpi.bpi_audit_events
WHERE tenant_id = '1000' AND object_id = :materialization_id
GROUP BY action ORDER BY action;
```

最终结果：

- materialization：`READY/r6`，`attempt_count=2`，`PARQUET`，schema
  `bpi.dataset-parquet.v1`，materializer `bpi-dataset-materializer/0.1.0`
- 行与 schema：`1 row / 26 fields`
- 字节数：`11341`
- manifest checksum：`efd6658e89ee0c6f2931956d56a35ce27cce64ab846e065e2669a27eb19ab255`
- content SHA-256：`8837c21dd9a5ac181bd86d09e22e43421e0c9420fca943a1f765b614059df126`
- 审计：`QUEUED=1`、`WRITING=2`、`FAILED=1`、`RETRIED=1`、`READY=1`
- 幂等：`2 rows / 2 COMPLETED / [202, 202]`

快照数据库 manifest 边界仍为 `MANIFEST_READY`、`MANIFEST_ONLY`、
`materializationState=NOT_STARTED`、`artifactUri=null`、`icebergReady=false`、
`mlflowRegistered=false`、`modelTrained=false`。API 的 latest materialization 投影独立显示 READY，
没有篡改冻结 manifest。

## MinIO 证据

目标 bucket `bpi-datasets` 为 private 且 versioning enabled。READY 时精确 URI 为：

```text
s3://bpi-datasets/datasets/117f0045-cf03-4177-8010-dc730c566f13/efd6658e89ee0c6f2931956d56a35ce27cce64ab846e065e2669a27eb19ab255/bpi-dataset-materializer-0.1.0/8837c21dd9a5ac181bd86d09e22e43421e0c9420fca943a1f765b614059df126.parquet?versionId=28b5a178-d972-4e5b-9c6f-c3ece7bb0838
```

独立验收按数据库 versionId 下载对象后得到：

| 校验项 | PostgreSQL | MinIO / PyArrow | 结果 |
|---|---:|---:|---|
| SHA-256 | `8837c2...f126` | `8837c2...f126` | 一致 |
| 字节数 | 11341 | 11341 | 一致 |
| 行数 | 1 | 1 | 一致 |
| schema 字段 | 26 | 26 | 一致 |
| Content-Type | 预期 Parquet | `application/vnd.apache.parquet` | 一致 |

Worker 专用凭据删除该 versionId 时返回 `AccessDenied`，证明业务账号没有删除历史版本权限。一次性
管理员清理容器随后只删除 `28b5a178-d972-4e5b-9c6f-c3ece7bb0838`；该快照前缀版本数从 1
变为 0，精确版本不可再读取。最终 bucket 仍为 private/versioning enabled，整个 bucket 对象版本数为 0。

## 清理与运行态

数据库清理采用 `deploy/docker/scripts/bpi-dataset-manifest-target-cleanup.sql`。脚本按定义、快照和
物化 UUID 建立关联集合，删除 4 条随机 UUID 幂等记录，而不是假设幂等键含 marker。清理后：

```text
definitions=0 snapshots=0 samples=0 materializations=0
shadowRuns=0 reviews=0 batches=0 rules=0 topologies=0 catalogs=0
idempotency=0
```

最终目标状态：Flyway `27`；`bpi-service`、`bpi-adapter`、`bpi-wms-adapter` 均 healthy；
materializer 容器数 `0`；`.env` 中 `BPI_DATASET_MATERIALIZER_ENABLED=false` 和
`BPI_DATASET_BUCKET_BOOTSTRAP_ENABLED=false`。其他 Phase 2/Kafka/WMS 写开关也保持关闭。

## 浏览器证据

- [受控失败态](../../metadata/bpi-dataset-materialization-failed-target.png)，SHA-256
  `3278f1760ea3813adbf2bac54e4ef6f061746480c0bf178ee69094c220080811`
- [READY 桌面态](../../metadata/bpi-dataset-materialization-ready-target.png)，SHA-256
  `ace2e3bc217dc2b0bb995ced861e81bf9f49ff508a7707daae00f9d2f9a3f84f`
- [READY 移动态](../../metadata/bpi-dataset-materialization-ready-mobile-target.png)，SHA-256
  `24ffd19725d63c75bfec59afeda825fc9a93034d92aaafe9e44bdd8731356e28`
- 原始浏览器报告：`metadata/bpi-dataset-materialization-{manifest,failed,ready,post-restart}-target.json`
- 机器可读总账：`metadata/bpi-dataset-materialization-acceptance.json`

## 未关闭门槛

1. 本轮不是 WORM/对象锁验收；versioning 和最小权限不能等同于法规级不可删除保留。
2. Iceberg 表、MLflow 注册、模型训练、模型审批、在线推断和漂移监控均未开始。
3. 物理 DEVICE/GATEWAY 来源、正式校准证书和目标产线连续 7-14 天影子运行仍需现场证据。
4. 外部 ERP/WMS 的消费、查单、拒绝、响应丢失、冲销和补偿仍未验收。
5. Worker 默认关闭；只有形成生产容量、监控、备份恢复和业务签字后才能提出持续启用申请。
