# BPI 工艺窗口 Parquet v2 与 Iceberg 验收

## 结论

2026-07-23 在唯一测试环境 `10.11.100.17 / adp-mes-newbase` 完成 Phase 3C-G，
状态为 **`PASS_TARGET_BROWSER_API_POSTGRES_MINIO_ICEBERG_CLEANED`**。

源码 `007616b290efa366f97e99cb27efb68c8186268c` 把冻结的
`process.window.*` 事实从 PostgreSQL manifest 交付到
`bpi.dataset-parquet.v2 / bpi-dataset-materializer/0.2.0`，并由现有
`bpi-dataset-catalog-publisher/0.1.0` 发布到 Apache Polaris/Iceberg。
真实页面、API、PostgreSQL、MinIO exact version、指定 Iceberg snapshot scan 和精确清理均已闭合。

该结论只关闭“过程窗口能否进入受治理训练制品”的软件断点。受控 fixture 不是物理设备或正式计量
证据，训练资格仍为 `BLOCKED`；模型训练、注册、审批、推断和生产激活均未启动。

## 验收矩阵

| 业务动作 | 前端/API | PostgreSQL | MinIO/Iceberg | 实际结果 | 状态 |
|---|---|---|---|---|---|
| 创建含过程窗口的数据集 | `/bpi/#/datasets`；`POST /bpi-api/datasets` | definition 保存两个原始 featureRef | 不适用 | 页面创建成功，原始 key 未改名 | PASS |
| 冻结 manifest | `POST /bpi-api/datasets/{id}/snapshots` | `3 total / 1 included / 2 excluded`；6 条窗口事实，2 READY/4 BLOCKED | 不适用 | flow mean=`20.000000`，pump true ratio=`0.500000` | PASS |
| 物化 Parquet v2 | `POST /bpi-api/dataset-snapshots/{id}/materializations` | `READY/r3/attempt1`，审计 `QUEUED -> WRITING -> READY` | exact version、SHA、12761 bytes、1 row/27 fields 均匹配 | map 精确保留两个 featureRef 和六位小数 | PASS |
| 发布 Iceberg | `POST /bpi-api/dataset-materializations/{id}/catalog-publications` | `READY/r4/attempt1`，4 条审计，1 条 `COMPLETED/202` 幂等 | snapshot `6747414530221740825`，1 row/33 fields，semantic checksum 匹配 | 页面显示 Manifest/Parquet/Iceberg 三段 READY | PASS |
| 精确退场 | marker cleanup SQL | 20 类 marker 投影全部为 0 | 1 个源对象版本、6 个 warehouse 对象版本、表和专用 namespace 全部为 0 | 可选容器为 0、五个开关 false、核心服务 healthy、页面 200 | PASS |

## 关键身份与事实

marker：`ADP_E2E_BPI_PARQUET_V2_20260723_233000_A1`。

| 标识 | 值 |
|---|---|
| dataset definition | `520419c7-b648-4109-9757-a4f295e0edde` |
| dataset snapshot | `b199531e-451f-44c8-a55c-17848217dd4d` |
| materialization | `28b117f9-9976-4699-b9d1-7d7f8054ade3` |
| catalog publication | `2a9d7a45-f068-4f15-bd2e-33a2398eef2b` |
| source versionId | `fbb8bfae-41fa-4d42-9629-b2d80da7b6e9` |
| source SHA-256 | `2f9930cd46ca1942b93b87cef404757c888eefca308c20473ceef18644412f98` |
| source rows / fields | `1 / 27` |
| Iceberg snapshot | `6747414530221740825` |
| Iceberg rows / fields | `1 / 33` |
| semantic checksum | `a7e1ac899c21d9684d7a0b06625d6da1d140fb96167c155ac0c41208b901b810` |
| table | `ft_mes_bpi.bpi_training.tenant_40510175845988f1.dataset_520419c7b64841099757a4f295e0edde` |

Parquet exact-version verifier 和独立 Iceberg time-travel scan 均读取到：

```json
{
  "process.window.flow_instant.mean_60s": "20.000000",
  "process.window.pump_running.true_ratio_30s": "0.500000"
}
```

Iceberg scan 指定 snapshot ID，不依赖“当前表”推断。publication 的
`sourceContentSha256/sourceObjectVersionId` 与 Parquet 完全一致，
`catalogSnapshotVerified/sourceVersionVerified/manifestChecksumVerified` 均为 `true`。

## 页面与数据库

桌面 `1440x900` 和移动端 `390x844` 均通过；console error、page error、request failure 为 0，
移动端 viewport/body/document 为 `390/390/390`。页面先显示
`MANIFEST_READY -> Parquet READY`，发布后显示
`MANIFEST_READY -> Parquet READY -> Iceberg READY`，后续 Object Lock、MLflow、训练资格和模型阶段
保持 `NOT_STARTED`。

PostgreSQL catalog publication 审计顺序为：

```text
QUEUED -> COMMITTING -> VERIFYING -> READY
```

publication 创建幂等记录为 `1 row / COMPLETED / 202`。Flyway 保持 V35，本阶段不新增迁移，
因为变更是显式制品契约升级，不改写历史 v1 行或对象。

## 清理与默认关闭

取证后使用 recovery 管理身份先核对表的 dataset ID 和精确 warehouse location，再删除唯一测试表和
两个空 namespace。随后按已盘点的 exact version ID 删除 6 个 warehouse 对象版本，并只删除本次
Parquet 的 exact source version。marker cleanup SQL 删除 definition、snapshot、窗口事实、
materialization、publication、audit、idempotency 和 fixture 数据；最终 20 类投影全部为 0。

环境最终只保留健康的 `bpi-service`、`bpi-adapter` 和 `bpi-wms-adapter`。materializer、publisher、
Polaris 和 Polaris PostgreSQL 容器均为 0；以下开关均为 `false`：

- `BPI_DATASET_MATERIALIZER_ENABLED`
- `BPI_DATASET_CATALOG_PUBLISHER_ENABLED`
- `BPI_ICEBERG_WAREHOUSE_BOOTSTRAP_ENABLED`
- `BPI_POLARIS_CATALOG_BOOTSTRAP_ENABLED`
- `BPI_POLARIS_ENABLED`

## 证据

- 聚合账本：`metadata/bpi-dataset-parquet-v2-acceptance.json`
- 浏览器 Parquet：`metadata/bpi-dataset-parquet-v2-browser-target.json`
- exact Parquet：`metadata/bpi-dataset-parquet-v2-object-target.json`
- 浏览器 Iceberg：`metadata/bpi-dataset-parquet-v2-iceberg-browser-target.json`
- exact Iceberg：`metadata/bpi-dataset-parquet-v2-iceberg-target.json`
- 截图：
  - `metadata/bpi-dataset-parquet-v2-manifest-target.png`
  - `metadata/bpi-dataset-parquet-v2-ready-target.png`
  - `metadata/bpi-dataset-parquet-v2-ready-mobile-target.png`
  - `metadata/bpi-dataset-parquet-v2-iceberg-ready-target.png`

## 下一门槛

Phase 3D-A 只能先实现训练任务控制面和失败关闭契约。当前受控数据仍缺物理来源、正式校准、
至少 200 个真实复核批次、7 个生产日、至少 100 个 accepted START 和 10 个 rejected START，
因此训练创建必须拒绝或保持受阻，不能生成演示模型来绕过门槛。
