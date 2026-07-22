# BPI V29 Object Lock 恢复包目标验收

## 结论

| 项目 | 结果 |
| --- | --- |
| 组合状态 | `PASS_TARGET_BROWSER_API_POSTGRES_OBJECT_LOCK_POLARIS_RECOVERY_CLEANED` |
| 目标环境 | `10.11.100.17`，唯一 Compose project `adp-mes-newbase` |
| 数据库 | PostgreSQL `15.18`，Flyway `V29` |
| 运行版本 | `ef8036b8b71718f3bb4f65ede3e9ba9cca093a82` |
| marker | `ADP_E2E_BPI_ARCHIVE_20260722_215300_A1` |
| 浏览器 | 真实 ADP 页面，桌面、刷新和 `390x844` 移动视口均通过 |
| 最终运行态 | 主 BPI 三服务健康，`/bpi/` 为 `200`；materializer、publisher、archiver、Polaris 侧车全部停止，相关开关全部关闭 |

本次结论关闭的是“单个已验证训练数据集能够形成受 Object Lock 保护、可独立恢复且可销毁恢复副本的恢复包”这一软件门槛。它不等于整站备份恢复、生产容量、现场连续运行、MLflow 或模型投产已完成。

## 验收链

同一 marker 从真实页面依次完成：

1. V26 不可变 manifest；
2. V27 精确版本 Parquet；
3. V28 Polaris/Iceberg snapshot；
4. V29 Object Lock 恢复包；
5. 隔离 recovery namespace 恢复、time-travel 校验、物理清除；
6. 精确 marker、对象、table、namespace 和临时 sidecar 清理。

关键身份如下：

| 对象 | 标识 |
| --- | --- |
| dataset | `a67c078b-0b2f-434d-9b66-7fe8de9d21ac` |
| snapshot | `d97dd81f-04d0-4b7e-a7ee-5ea9bb3f2943` |
| materialization | `65571868-f6ed-42dc-bdae-ff9c99315541` |
| catalog publication | `ae56f35f-013e-4973-bd5a-98b1ab4ef8e4` |
| retention archive | `c2d585f4-5793-4f17-a230-aa98440d3293` |
| 原 Iceberg snapshot | `2413939455193407789` |
| 恢复演练 snapshot | `4888963949559974798` |

## 真实页面与 API

| 操作 | 请求 | 页面/API 结果 | PostgreSQL 结果 | 状态 |
| --- | --- | --- | --- | --- |
| 请求恢复包 | `POST /bpi-api/dataset-catalog-publications/ae56f35f-013e-4973-bd5a-98b1ab4ef8e4/retention-archives` | `202`，创建同一 archive ID | `QUEUED/r1`，幂等记录 `COMPLETED/202` | PASS |
| 持久化受控失败 | archiver 使用不可写工作目录 | 页面显示明确失败，不伪造已锁定 | `ARCHIVING/r2 -> FAILED/r3`，`RETENTION_ARCHIVE_ERROR`，attempt `1` | PASS |
| 页面重试 | `POST /bpi-api/dataset-retention-archives/c2d585f4-5793-4f17-a230-aa98440d3293/retry` | `202`，复用同一 archive ID | `RETRIED/r4 -> ARCHIVING/r5 -> VERIFYING/r6 -> LOCKED/r7`，attempt `2` | PASS |
| 重新发现 | GET dataset/snapshot/publication/archive | 刷新和移动视口读取同一 `LOCKED` 事实 | 数据库 ID、revision、对象版本和校验和不变 | PASS |

浏览器非预期 `console error`、`page error`、`request failure` 均为 `0`。移动视口为 `390/390/390`，drawer 为 `389/389`，无横向溢出。

## PostgreSQL 落库

`bpi.bpi_dataset_retention_archives` 最终为：

- `LOCKED/r7/attempt2`；
- `retention_mode=GOVERNANCE`；
- `retain_until=2026-08-21T14:00:55.331898Z`；
- `archive_object_count=2`，`archive_total_bytes=15301`；
- `verified_row_count=1`；
- `verified_semantic_checksum=8b4dacce8c19b25e93ef632cf0f30451fded82f300385ec8d22a3649ea258a28`；
- `archive_metadata.objectLockVerified=true`；
- `archive_metadata.recoveryVerified=true`。

审计顺序严格为：

```text
DATASET_RETENTION_ARCHIVE_QUEUED/r1
DATASET_RETENTION_ARCHIVE_ARCHIVING/r2
DATASET_RETENTION_ARCHIVE_FAILED/r3
DATASET_RETENTION_ARCHIVE_RETRIED/r4
DATASET_RETENTION_ARCHIVE_ARCHIVING/r5
DATASET_RETENTION_ARCHIVE_VERIFYING/r6
DATASET_RETENTION_ARCHIVE_LOCKED/r7
```

请求和重试两条写路径均持久化为 `COMPLETED/202`，重复请求不创建第二个业务任务。

## Object Lock 与权限

恢复 bucket 为 `bpi-dataset-recovery`，归档前缀为：

```text
archives/tenant_40510175845988f1/ae56f35f-013e-4973-bd5a-98b1ab4ef8e4/c2d585f4-5793-4f17-a230-aa98440d3293
```

| 对象 | versionId | SHA-256 / 事实 |
| --- | --- | --- |
| `source.parquet` | `dfa24784-8270-4e83-a032-3679964730c0` | `43a840dd341b9f667a571c0971a319640c7a57a66f429c1d500db3653f109dbb`，1 row |
| `recovery-manifest.json` | `7a96696b-3f27-48fb-9103-3d72bb5964b6` | `9d01266b9b0bea65e0346d0a030c0948ea2678c4d79aa7b9d9d5b0c0def725c2` |

两个精确版本都带 `GOVERNANCE` retention，retain-until 与数据库一致。archiver 的 exact-version 删除返回 `AccessDenied`；管理员未携带 governance bypass 的删除返回 `InvalidRequest`。恢复操作员只能读取精确恢复包并写入专用 recovery warehouse，列举业务 training warehouse 返回 `AccessDenied`，删除 retained archive 也返回 `AccessDenied`。

## 恢复演练

恢复工具以独立 MinIO/Polaris 恢复身份执行，未复用 publisher 或管理身份：

1. 按 archive manifest 读取两个精确版本并复算 SHA；
2. 在 `ft_mes_bpi.bpi_recovery.archive_c2d585f457934f17a230aa98440d3293.dataset` 创建隔离 Iceberg 表；
3. time-travel 扫描 snapshot `4888963949559974798`，得到 `1` row；
4. semantic checksum 与原 publication 完全一致；
5. purge 恢复表并精确删除该 recovery prefix 的 `6` 个对象版本；
6. 验证 recovery namespace 消失、warehouse 版本为 `0`；
7. 再次读取原 training table，原 snapshot `2413939455193407789`、1 row 和 checksum 均未改变。

恢复报告状态为 `PASS`，同时记录 `physicalPurgeVerified=true`、`namespaceCleanupVerified=true`、`timeTravelVerified=true`。原始机器报告为 `metadata/bpi-dataset-retention-recovery-rehearsal-target.json`。

## 精确清理与退场

- 原测试 training table 和其 `6` 个 warehouse 版本已删除；
- V27 源对象精确版本已删除；
- 两个 retained archive 版本仅由管理员显式 governance bypass 定向删除；
- definition、snapshot、sample、materialization、publication、archive、audit、idempotency 及依赖 fixture marker 均为 `0`；
- recovery table、namespace、warehouse versions 均为 `0`；
- materializer、publisher、archiver、Polaris、Polaris PostgreSQL 运行容器均为 `0`；
- `BPI_DATASET_MATERIALIZER_ENABLED`、`BPI_POLARIS_ENABLED`、`BPI_POLARIS_DROP_WITH_PURGE_ENABLED`、`BPI_POLARIS_CATALOG_BOOTSTRAP_ENABLED`、`BPI_ICEBERG_WAREHOUSE_BOOTSTRAP_ENABLED`、`BPI_DATASET_CATALOG_PUBLISHER_ENABLED`、`BPI_DATASET_RECOVERY_BUCKET_BOOTSTRAP_ENABLED`、`BPI_DATASET_RETENTION_ARCHIVER_ENABLED`、`BPI_DATASET_RECOVERY_RECONCILE_STALE` 均为 `false`。

## 证据资产

- `metadata/bpi-dataset-retention-archive-acceptance.json`
- `metadata/bpi-dataset-retention-recovery-rehearsal-target.json`
- `metadata/bpi-dataset-retention-failed-target.png`
- `metadata/bpi-dataset-retention-locked-target.png`
- `metadata/bpi-dataset-retention-locked-mobile-target.png`
- `deploy/docker/scripts/adp-bpi-dataset-retention-target-acceptance.js`
- `deploy/docker/scripts/bpi-dataset-manifest-target-cleanup.sql`

截图 SHA-256：

| 文件 | SHA-256 |
| --- | --- |
| failed | `8779bf23909a9bbab7a545325dfcaeeeea3c36de0da78a0bddaa32f51b4a948a` |
| locked desktop | `f5e92df44ed7b0d283800543936567655a854509db149f955f8b1ce4ad263439` |
| locked mobile | `f7e55836ff0ba5f71bbe43da26fe7e15ba1ffcd59478022e83038513f800b356` |

## 未关闭边界

- Object Lock 恢复包只覆盖已验证的 BPI 单数据集，不替代 PostgreSQL、Kafka、Keycloak、Nacos、MinIO 全站灾备。
- `GOVERNANCE/30 天` 是测试策略，不是生产法规留存周期决策。
- 未完成容量压测、生产 RPO/RTO、跨故障域复制和恢复值班演练。
- 未完成物理设备、正式校准、连续 7-14 天现场运行和外部 ERP/WMS。
- MLflow、模型训练、审批、在线推断均保持 `false/NOT_STARTED`。
