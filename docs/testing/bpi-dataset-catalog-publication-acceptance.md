# BPI Iceberg 目录发布目标后端验收

## 结论

2026-07-22 在唯一测试环境 `10.11.100.17 / adp-mes-newbase` 完成 BPI Phase 3B-B
Iceberg 目录发布的**目标后端检查点**，状态为
**`PASS_TARGET_BACKEND_CHECKPOINT_BROWSER_AND_POST_COMMIT_FENCING_PENDING_CLEANED`**。

本轮使用 PostgreSQL `15.18 / Flyway V28`、独立 PostgreSQL `16.14` Polaris metastore、
Apache Polaris `1.4.1`、PyIceberg `0.11.1` 和现有 MinIO。成功 marker
`ADP_E2E_BPI_DATASET_961b1001a363487bb5e68a0419c7a23d` 从 V26 manifest、V27 精确
Parquet 对象进入 V28 发布任务，最终生成真实 Iceberg v2 table/snapshot，并由独立 PyIceberg
time-travel scan 对账为 `1 row / 1 data file`。任务、对象、Iceberg table/namespace 和 PostgreSQL
marker 数据随后均已定向清理。

该结论不是 Phase 3B-B 全量完成：当前面向业务的 BPI service、Java 8 adapter 和 Web 仍是 V27
发布态，尚未通过真实 ADP 页面执行 V28 POST/GET/retry；“Polaris commit 已成功但 BPI PostgreSQL
fencing 回写失败”的真实故障窗口也尚未注入。MLflow、模型训练/推断、WORM/Object Lock、生产容量和
生产激活继续保持未完成。

## 验收矩阵

| 业务动作 | 入口 | API / 后端入口 | 目标存储 | 实际结果 | 状态 |
|---|---|---|---|---|---|
| 创建目录发布任务 | Java V28 真实应用验收 | `POST /bpi/v1/dataset-materializations/{id}/catalog-publications` -> `DatasetCatalogPublicationService` -> PostgreSQL repository | `bpi_dataset_catalog_publications`、audit、idempotency | `202`；publication `ccf2abb1-d0e0-4e23-8df2-ed63604799b6` 从 `QUEUED/r1` 开始 | PASS |
| 精确读取源制品 | Python publisher | `SourceObjectReader` 按 bucket/key/versionId 获取并校验 | MinIO `bpi-datasets` | versionId、9589 bytes、1 row、SHA-256 和冻结 schema 全部匹配 | PASS |
| 提交 Iceberg table/snapshot | PyIceberg -> Polaris REST | `CatalogPublisher.ensure_table_and_snapshot` | Polaris metastore + `bpi-iceberg-warehouse` | table、schema ID 0、partition spec ID 1 和 snapshot `9198617437104218826` 真实存在 | PASS |
| time-travel 对账并 READY | 同一 publisher 的 reconcile/verify | 指定 snapshot scan + 语义 checksum | Iceberg metadata/manifest/data file + BPI PostgreSQL | `VERIFYING -> READY/r4`；source/verified rows `1/1`，语义 checksum 一致 | PASS |
| 重启持久化 | 重启 Polaris PostgreSQL、Polaris、publisher | metastore gate + catalog lookup | Polaris PostgreSQL | auth/entities/grants/version 为 `2/13/24/1`，重启前后不变；同一 snapshot 仍为 1 row，没有重复 commit | PASS |
| bootstrap 幂等 | 重跑两个 MinIO bootstrap、metastore gate、catalog bootstrap | shell/Python bootstrap | MinIO、Polaris PostgreSQL、Polaris catalog | 所有脚本 PASS；publisher credential SHA 前后相同 | PASS |
| 失败与重试 | 真实 V28 HTTP API + publisher | retry API，带 `Idempotency-Key` 与 `If-Match` | publication/audit/idempotency | 缺失精确源对象使同一任务两次落为 `SOURCE_OBJECT_ERROR`；attempt `1 -> 2`，重复 retry key 返回 `Idempotent-Replay: true`，未创建 snapshot | PASS |
| 定向清理 | 管理员 one-shot + SQL | drop exact tables/namespaces、remove exact object versions/prefixes、marker SQL | Polaris、MinIO、BPI PostgreSQL | 两个测试 table、两个 tenant namespace、源对象版本、warehouse prefix 和 marker 关联行全部清零 | PASS |
| 真实 ADP 页面 V28 交互 | `/bpi/#/datasets` | Java 8 adapter -> V28 service | 页面/API/PostgreSQL/Polaris | V28 service/adapter/UI 尚未部署为业务发布态；没有目标页面截图或 console/network 证据 | BLOCKED |
| catalog commit 后 fencing 恢复 | publisher reconcile | catalog snapshot 已存在、BPI READY 回写首次失败 | Polaris + BPI PostgreSQL | 单元测试覆盖 reconcile 逻辑，但尚未做目标环境真实故障注入 | BLOCKED |

## 成功链证据

### 标识与状态

- marker：`ADP_E2E_BPI_DATASET_961b1001a363487bb5e68a0419c7a23d`
- definition：`9d769611-ba39-4d19-b3c6-99824712b7fd`
- dataset snapshot：`6aebfa3b-231e-4801-a04e-3c6a41bebf9d`
- materialization：`0af4ae11-d878-42eb-930b-3b8507cb3e7a`
- catalog publication：`ccf2abb1-d0e0-4e23-8df2-ed63604799b6`
- publication 终态：`READY/r4`，`attempt_count=1`
- 状态审计：`QUEUED -> COMMITTING -> VERIFYING -> READY`

### 源制品

```text
s3://bpi-datasets/datasets/6aebfa3b-231e-4801-a04e-3c6a41bebf9d/
c0b83245951ef05c005cac052e5a09354c34681e6123a9a7bc62e320e730d23d/
bpi-dataset-materializer-0.1.0/
56a1eeeaf651a47c1e397451832ec166474875068a5bebcef70d5a9dc8e2e7c0.parquet
```

| 字段 | 值 |
|---|---|
| object versionId | `78e62390-c039-4bbd-9381-f8ccf61852ad` |
| content SHA-256 | `56a1eeeaf651a47c1e397451832ec166474875068a5bebcef70d5a9dc8e2e7c0` |
| manifest checksum | `c0b83245951ef05c005cac052e5a09354c34681e6123a9a7bc62e320e730d23d` |
| byte size / row count | `9589 / 1` |
| objectContentVerified | `true` |

### Iceberg 事实

```text
catalog:    ft_mes_bpi
namespace:  bpi_training.tenant_0357920caac9023a
table:      dataset_9d769611ba394d19b3c699824712b7fd
snapshot:   9198617437104218826
metadata:   s3://bpi-iceberg-warehouse/warehouse/bpi_training/
            tenant_0357920caac9023a/dataset_9d769611ba394d19b3c699824712b7fd/
            metadata/00002-e2e574f2-a51a-4e89-b78b-421672c41fb6.metadata.json
```

- format version：Iceberg v2
- schema ID：`0`
- partition spec ID：`1`
- partition：`plant_id identity + prediction_time day`
- semantic checksum：`52b03feeac24bb3d38b8f154bea9a9009be0aadffd8c1c3056bafce79525a5d8`
- time-travel scan：`1 row / 1 data file`
- data file：`.../plant_id=PLANT-01/prediction_day=2026-07-22/00000-0-8d827613-c24b-4bd3-a60c-6932cfb1cf10.parquet`

关键 PostgreSQL 查询：

```sql
SELECT state, revision, attempt_count, iceberg_snapshot_id,
       iceberg_table_identifier, metadata_location,
       source_row_count, verified_row_count, semantic_checksum
FROM bpi.bpi_dataset_catalog_publications
WHERE tenant_id = :tenant_id AND id = :publication_id;

SELECT action, before_revision, after_revision,
       details ->> 'attemptCount' AS attempt_count
FROM bpi.bpi_audit_events
WHERE tenant_id = :tenant_id AND object_id = :publication_id
ORDER BY occurred_at, id;
```

## 失败、重试与幂等

受控缺失源对象 fixture 使用 materialization
`3cd57c7b-556d-42ca-ae35-2b952b7e97af` 和 publication
`6873b265-241f-49ba-8c9e-b13cd65b5304`。首次处理为
`QUEUED/r1 -> COMMITTING/r2 -> FAILED/r3`，失败码 `SOURCE_OBJECT_ERROR`、attempt 1；真实 retry API
返回 `202` 后，同一任务变为
`QUEUED/r4 -> COMMITTING/r5 -> FAILED/r6`、attempt 2。相同 retry key 在 r6 后重放返回
`202 + Idempotent-Replay: true` 和原 `QUEUED/r4` 响应。两个失败周期均没有 Iceberg snapshot。

Polaris 整体不可用时，publisher 的健康门在认领前失败，任务保持 `QUEUED`，没有制造一个虚假的
业务失败或增加 attempt；这属于预期 fail-closed 行为。

## 组件来源与运行边界

- Polaris 源码：官方 tag `1.4.1`，commit
  `9569f2d24c08f926cf768290fda7680cdb1e1611`
- Polaris server image：`sha256:e209d661faf4869d26f70da4ff5af9d6c8f635932111f9459c7fe5c699e82ab1`
- Polaris admin image：`sha256:49b527cb7506bbb8c8d1e067405db199bc108a7604cdb078665e0ddeab52a13e`
- publisher image：`sha256:d9b70afc244073e0b101880f36c812cc3df1bbd4bba056959d462e85ebda8ccd`
- 验收候选基线：`5754f161770cda5a268aa9bcc3afc351ae075767`
- 目标 staging：`/home/v6/ft-mes-bpi-phase3bb-acceptance-20260722-https`
- 受保护 `.env.phase3bb` 权限为 `0600`；仓库不保存凭据。

源码默认保持 `BPI_DATASET_CATALOG_PUBLISHER_ENABLED=false`、
`BPI_POLARIS_ENABLED=false`、`BPI_POLARIS_BOOTSTRAP_ENABLED=false` 和既有
materializer/bootstrap 默认关闭。目标验收 staging 曾显式启用 sidecar；在 V28 业务服务、adapter 和页面
正式部署并完成目标浏览器验收前，不得把该临时验收开关解释为生产激活。

## 清理结果

清理删除了首次 schema 契约失败测试表和成功表、对应两个 tenant namespace、精确源对象版本与 warehouse
prefix。BPI PostgreSQL 定向清理结果：

```text
catalogPublications=3 materializations=3 samples=12 snapshots=4 definitions=2
audits=29 idempotency=12 reviews=8 shadowRuns=4 batches=8
rules=2 topologies=2 pointCatalogs=2
```

以上是“本次删除数量”，不是残留数量。清理后 publication、materialization、definition marker 投影均为
`0`；源对象精确版本和两个 warehouse table prefix 均不可再读取。

## 未关闭门槛

1. 部署已提交的 V28 service、Java 8 adapter 和 BPI UI，使用真实 ADP 会话在桌面/移动页面执行
   POST、GET、FAILED/retry、READY，记录 console/network/截图与 PostgreSQL/Polaris 对账。
2. 注入一次“Polaris commit 成功、BPI PostgreSQL fencing 回写失败”，重启 publisher 后证明只 reconcile
   既有 snapshot，不产生第二次 commit。
3. 验收结束后恢复目标 sidecar/default-off 开关，保留一套 `adp-mes-newbase` 运行环境。
4. WORM/Object Lock、MLflow、模型训练/审批、在线推断、容量、备份恢复和连续现场运行另立验收，不从
   本检查点外推。
