# BPI Iceberg 目录发布目标验收

## 结论

2026-07-22 在唯一测试环境 `10.11.100.17 / adp-mes-newbase` 完成 BPI Phase 3B-B
Iceberg 目录发布纵切，状态为
**`PASS_TARGET_BROWSER_API_POSTGRES_MINIO_POLARIS_FENCING_CLEANED`**。

候选提交 `b7356aa0749600a436df84f39fbff3851c89ed60` 已通过受保护的 expand-only 流程部署到
PostgreSQL `15.18 / Flyway V28`。真实 `/bpi/#/datasets` 页面经 Java 8 adapter 调用 V28 Java 17
service，完成发布、轮询、受控失败、同任务重试、刷新后重发现和桌面/移动端读取。独立 Python publisher
使用 MinIO 精确 `versionId`、Apache Polaris `1.4.1`、独立 PostgreSQL `16.14` metastore 和
PyIceberg `0.11.1` 生成并复验真实 Iceberg v2 snapshot。

本轮还真实注入了“Polaris commit 已成功，但 BPI PostgreSQL fencing 尚未写入”的进程退出窗口。
恢复前，Polaris 中恰好存在一个 snapshot，BPI PostgreSQL 仍为 `COMMITTING/r2` 且 snapshot 字段为空；
claim 超时后原版 publisher 将同一任务恢复到 `READY/r6/attempt2`，仍引用同一 snapshot、同一 metadata
location，独立扫描保持 `1 row / 1 data file`，没有第二次 append。

该结论只关闭 Phase 3B-B 软件纵切，不代表 G-021 或整个 BPI 产品生产就绪。WORM/Object Lock、
MLflow、模型训练/审批/在线推断、容量与备份恢复、物理来源连续 7-14 天和外部 ERP/WMS 仍未完成。

## 验收矩阵

| 业务动作 | 前端/API | 后端入口 | 目标存储 | 实际结果 | 状态 |
|---|---|---|---|---|---|
| V28 expand-only 部署 | upgrade script；`/bpi/` | Flyway/service/adapter/Web | PostgreSQL、Docker runtime | release `b7356aa0`，Flyway 28，三个 BPI 主服务健康，页面 200 | PASS |
| 页面发布并 READY | `POST /bpi-api/dataset-materializations/{id}/catalog-publications`；GET publication | adapter -> service -> publisher | publication/audit/idempotency、MinIO、Polaris | `41a1826f...` 为 `READY/r4/attempt1`；1/1 行，snapshot `1863646729883880222` | PASS |
| 页面刷新与移动端 | `/bpi/#/datasets`，`1440x900`、`390x844` | snapshot/publication GET | PostgreSQL、Polaris | 刷新后同一 snapshot/table；viewport/body/document=`390/390/390`；drawer `389/389` | PASS |
| 受控失败 | 页面“发布 Iceberg”；错误 source-reader secret | publisher source GET | publication/audit；MinIO 不变 | `ddd76d7d...` 为 `FAILED/r3/attempt1`、`SOURCE_OBJECT_ERROR`，snapshot null | PASS |
| 页面重试 | `POST /bpi-api/dataset-catalog-publications/{id}/retry` | service retry -> publisher | publication/audit/idempotency、Polaris | 同一 ID 到 `READY/r7/attempt2`；审计 7 条；两个 `COMPLETED/202` | PASS |
| post-commit fencing 注入 | 页面先创建 QUEUED；受保护 one-shot 注入 | repository claim -> real commit -> exit 86 | Polaris 已提交；BPI fence 未写 | PostgreSQL `COMMITTING/r2/attempt1` 且 snapshot null；Polaris snapshot 恰好 1 个 | PASS |
| claim 恢复与 reconcile | 原版 publisher，timeout 30 秒 | recover/reclaim -> ensure existing commit -> verify/complete | PostgreSQL、Polaris | `READY/r6/attempt2`；同一 snapshot `3771508441673321637`、同一 metadata location | PASS |
| 独立 time-travel 对账 | PyIceberg 指定 snapshot/materialization scan | 不经过 BPI API | Iceberg metadata/manifest/data file | snapshot count 1、publication snapshot count 1、1 row、1 data file | PASS |
| 最小权限 | publisher 尝试 drop table | Polaris authorization | Polaris | `DROP_TABLE_WITHOUT_PURGE` 返回 403；清理改用 bootstrap 管理身份 | PASS |
| 定向清理与退场 | exact table/version/prefix + marker SQL | bootstrap admin、MinIO admin、PostgreSQL | 三类存储 | 测试表、源版本、warehouse prefix、definition/materialization/publication 均为 0；sidecar 停止 | PASS |

## 页面成功链

成功 marker：`ADP_E2E_BPI_ICEBERG_20260722_175829_A1`。

| 标识 | 值 |
|---|---|
| definition | `204bdfa3-de0f-4bf9-a78c-85cd24f0e110` |
| dataset snapshot | `596ed479-2ae2-401f-8737-cec0ed84171f` |
| materialization | `628ee8a0-bc8e-470e-b8be-46e0c8eb0f00` |
| catalog publication | `41a1826f-d872-483d-b710-b4f24c7ca4b9` |
| source versionId | `9f6028b5-c320-4e13-b111-d6f74ba98557` |
| source SHA-256 | `fc090779c1d355846cbe9a961a9d1bc8282cb71e57ebb0adfb8cc893841b854f` |
| source rows / fields | `1 / 26` |
| Iceberg snapshot | `1863646729883880222` |
| table | `ft_mes_bpi.bpi_training.tenant_40510175845988f1.dataset_204bdfa3de0f4bf9a78c85cd24f0e110` |
| schema / partition spec | `0 / 1` |
| semantic checksum | `68e6221babb2ab947f804e3dc120e259c846c9428222601e5d13c8a85eaea07a` |

页面捕获的写请求为 `202`，随后 GET publication 到 `READY/r4`。PostgreSQL 审计为
`QUEUED -> COMMITTING -> VERIFYING -> READY`，publication 创建幂等记录为 `COMPLETED/202`。
独立 PyIceberg scan 验证 1 row、1 data file，并核对 snapshot properties 中 publication、materialization
和 source versionId。

桌面、移动端、刷新重发现各轮的 console error、page error 和 request failure 均为 0。移动端页面与抽屉
均无横向溢出。证据截图：

- `metadata/bpi-dataset-catalog-ready-target.png`
- `metadata/bpi-dataset-catalog-ready-mobile-target.png`

## 页面失败与重试

重试 marker：`ADP_E2E_BPI_ICEBERG_RETRY_20260722_181106_A1`，publication
`ddd76d7d-3073-42a3-ba56-7dbad3327c23`。

publisher 使用只在该容器内生效的错误 source-reader secret，Polaris 和共享 MinIO 未停止。页面发起发布后，
任务真实进入 `FAILED/r3/attempt1`，失败码为 `SOURCE_OBJECT_ERROR`，Iceberg snapshot 字段保持 null。
恢复正确 secret 后，页面点击“重试 Iceberg”，POST `202` 复用同一 publication ID，最终为
`READY/r7/attempt2`、2/2 行。数据库审计顺序为：

```text
QUEUED -> COMMITTING -> FAILED -> RETRIED -> COMMITTING -> VERIFYING -> READY
```

两次写请求均对应 `COMPLETED/202` 幂等记录；重试前后浏览器没有非预期错误。证据截图：

- `metadata/bpi-dataset-catalog-failed-target.png`
- `metadata/bpi-dataset-catalog-retry-ready-target.png`
- `metadata/bpi-dataset-catalog-retry-ready-mobile-target.png`

该 marker 建立时上一组 fixture 尚未清理，快照按真实 scope 合法冻结了两组同产线 review，因此旧 manifest
runner 的“恰好 3 个样本”测试隔离断言失败。产品查询、目录失败/重试和 2/2 行对账均正常；后续 fencing
marker 在清场后重新以单一 fixture 执行。清理时 PostgreSQL 外键也阻止了先删除仍被第二个快照引用的 review，
事务完整回滚；按依赖顺序清理后两组均为零。

## Fencing 恢复

fencing marker：`ADP_E2E_BPI_ICEBERG_FENCE_20260722_181629_A1`。

| 标识 | 值 |
|---|---|
| definition | `90d0b27e-d0e5-4a51-ae24-809bb04f92b0` |
| dataset snapshot | `8608bf03-7e04-43e1-b694-a657bf3d5306` |
| materialization | `2b37efc6-1688-42af-b716-59bc4e836c99` |
| catalog publication | `c6cd596b-b389-46b0-aaef-292ebf61bf21` |
| source versionId | `88d7173b-567f-44d7-9bc9-91b6c6ba4c56` |
| source SHA-256 | `22241ae47cdabf315e95a79fc024cd1595d3e6a16ae4d7fb4ef10d5df926a1da` |
| Iceberg snapshot | `3771508441673321637` |
| semantic checksum | `a74b96f0c5d3c89658b565711bcaaf566dcd3a54823f5481de5f4ab86fdf5696` |

页面先创建 `QUEUED/r1`。`bpi-dataset-catalog-post-commit-failure-injection.py` 只在明确 confirmation、
整个 publisher scope 内唯一 active 且为 queued publication、匹配 publication ID 和 dataset code 时运行；它调用正式 repository、source reader
和 catalog publisher，真实 commit 后以预期 exit code `86` 退出，不执行 `mark_verifying` 或 `fail`。

故障时双侧事实：

```text
PostgreSQL: COMMITTING/r2, attempt=1, claim present, snapshot=null, metadata=null
Polaris:    snapshot=3771508441673321637, publication matches=1, table snapshots=1
```

30 秒 claim timeout 后启动未修改的正式 publisher，审计 revision 顺序为：

```text
QUEUED/r1 -> COMMITTING/r2 -> CLAIM_RECOVERED/r3
          -> COMMITTING/r4 -> VERIFYING/r5 -> READY/r6
```

恢复后的 snapshot ID 和 metadata location 与故障时完全一致；独立 PyIceberg 再次得到 1 snapshot、1 row、
1 data file。截图：

- `metadata/bpi-dataset-catalog-fencing-queued-target.png`
- `metadata/bpi-dataset-catalog-fencing-recovered-target.png`
- `metadata/bpi-dataset-catalog-fencing-recovered-mobile-target.png`

## 部署与退场

- clean release：`/home/v6/ft-mes-bpi-release-b7356aa0`
- runtime：`/home/v6/adp-mes-docker-newbase-20260611-181921`
- upgrade report：`backups/bpi-v28-b7356aa0/bpi-integrated-upgrade-20260722T094016Z.json`
- service image：`ft-mes-bpi-service:20260722t094016z-b7356aa07496`
- adapter image：`ft-mes-bpi-adapter:20260722t094016z-b7356aa07496`
- publisher image：`ft-mes-bpi-dataset-catalog-publisher:20260722t094016z-b7356aa07496`

验收结束后：

- `bpi-dataset-materializer`、`bpi-dataset-catalog-publisher`、`bpi-polaris`、
  `bpi-polaris-postgres` 均停止；
- materializer、publisher、Polaris、catalog bootstrap、bucket/source-reader/warehouse bootstrap 七个开关均为
  `false`；
- V28 service、adapter 和 WMS adapter 保持健康，`/bpi/` 返回 200；
- `ADP_E2E_BPI_ICEBERG_%` definition、全部 materialization 和 catalog publication 数量为 `0|0|0`；
- 三张测试表、精确源对象版本和三个 warehouse table prefix 均已删除。

publisher 的 `DROP_TABLE_WITHOUT_PURGE` 得到预期 403；验收清理使用独立 bootstrap 管理身份，没有扩大
publisher 的运行权限。

## 可复验入口

- 浏览器 runner：`deploy/docker/scripts/adp-bpi-dataset-catalog-target-acceptance.js`
- PostgreSQL 对账：`deploy/docker/scripts/bpi-dataset-catalog-target-verification.sql`
- post-commit 注入：`deploy/docker/scripts/bpi-dataset-catalog-post-commit-failure-injection.py`
- V28-aware cleanup：`deploy/docker/scripts/bpi-dataset-manifest-target-cleanup.sql`
- 机器证据：`metadata/bpi-dataset-catalog-publication-acceptance.json`

运行口令和 catalog credential 不写入仓库。默认编排仍保持 fail-closed 和 disabled-by-default。
