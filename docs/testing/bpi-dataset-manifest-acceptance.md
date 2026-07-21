# BPI 数据集清单工作台验收

## 结论

2026-07-22 完成 BPI Phase 3A 数据集清单工作台的本地回归和目标环境整链，状态为
**`PASS_TARGET_BROWSER_API_POSTGRES_MANIFEST_ONLY_CLEANED`**。

本轮实现并验证了受控数据集定义、时间点冻结、批准影子批次筛选、低置信度与标签延迟排除、
特征/标签隔离、确定性 checksum、异步 worker、幂等重放、租户/工厂/产线范围控制、终态不可变、
桌面与移动端清单检查。本地 PostgreSQL 16.13 从空库应用 Flyway V1-V26；浏览器 E2E `20/20 PASS`，
模拟 API `15/15 PASS`，Java 8 Adapter 路由测试通过，定向 Java API/worker/PostgreSQL 验收通过。
随后将源码 revision `e116580aa110fd9c6895a4bbd208b533d89e2dee` 以 expand-only 方式部署到
`10.11.100.17` 唯一 `adp-mes-newbase`，再以真实 ADP 会话从页面创建 marker、捕获 API、直查
PostgreSQL 15.18/Flyway V26 并完成定向清理。

交付边界是 **manifest-only**：数据库明确保持 `materializationState=NOT_STARTED`、
`artifactUri=null`、`icebergReady=false`、`mlflowRegistered=false`、`modelTrained=false`。
本轮不声明训练数据制品、Iceberg 表、MLflow 注册、模型训练或生产预测已经完成。

## 功能验收

| 模块 | 页面/路由 | 操作 | API | 前端结果 | 后端结果 | 数据库表 | 状态 |
|---|---|---|---|---|---|---|---|
| 数据集定义 | `/bpi/#/datasets` | 选择受控特征/标签、产线、置信度和标签延迟，创建不可变版本 | `POST /bpi/v1/datasets` | 对话框校验、保存、列表刷新和详情抽屉正常 | `BPI_ENGINEER/BPI_ADMIN` 可写；同 key 重放原响应；越权角色/产线返回 403 | `bpi_dataset_definitions`、`bpi_audit_events`、`bpi_api_idempotency` | PASS |
| 清单快照 | `/bpi/#/datasets` | 选择 freezeAt、产线和规则版本，排队并轮询清单 | `POST /bpi/v1/datasets/{id}/snapshots`；`GET /bpi/v1/dataset-snapshots/{id}` | 页面从 `QUEUED` 到 `MANIFEST_READY`，显示 checksum、计数和排除原因 | worker 以 `FOR UPDATE SKIP LOCKED` 领取，持久化 3 个时间点样本：1 included、2 excluded | `bpi_dataset_snapshots`、`bpi_dataset_snapshot_samples`、审计表 | PASS |
| 时间点与泄漏控制 | 清单详情抽屉 | 检查 prediction time、feature cutoff、标签可用时间及 payload | 同上 GET | 抽屉显示样本边界和排除原因 | 3/3 样本 `feature_cutoff=prediction_time`；feature payload 中标签字段 0 个 | `bpi_dataset_snapshot_samples` | PASS |
| 确定性与不可变 | 同一数据集 | 对同一 freezeAt 再生成快照并尝试修改终态 | 同上 POST/GET | 两次清单 checksum 一致 | 两个独立 snapshot 得到相同 64 位 checksum；终态 UPDATE 被触发器拒绝 | `bpi_dataset_snapshots` | PASS |
| 阶段边界 | 清单详情抽屉 | 检查交付阶段和下游状态 | 同上 GET | 明确显示 `MANIFEST ONLY` | artifact 为空，物化/注册/训练均未开始 | `bpi_dataset_snapshots.manifest` | PASS |
| 响应式交互 | 桌面 `1440x900`；移动 `390x844` | 创建、打开定义和清单详情、检查底部导航与抽屉 | 确定性模拟器 | 无非预期 console/page/request error，无页面级横向溢出 | 浏览器套件不声明 PostgreSQL 落库 | 不适用 | PASS |
| 目标页面/API/落库 | `http://10.11.100.17:18080/bpi/#/datasets` | 真实登录后创建定义、冻结快照、等待清单完成并打开详情 | `POST /bpi-api/datasets`；`POST /bpi-api/datasets/{id}/snapshots`；snapshot GET | 定义 200、快照 202、详情 `MANIFEST_READY/r3`；桌面/移动均显示 3 行；console/page/request error 为 0 | 目标库 `3 total / 1 included / 2 excluded / 3 cutoff-safe / 0 leaked / 0 cross-plant`；幂等 2/2 完成；十类 marker 残留为 0 | 定义、快照、样本、审计、幂等及 fixture 依赖表 | PASS_TARGET_CLEANED |

## 后端链路

```text
DatasetController
  -> DatasetService
  -> DatasetPostgresRepository
  -> bpi_dataset_definitions / bpi_dataset_snapshots
     / bpi_dataset_snapshot_samples / bpi_audit_events

DatasetManifestProcessor
  -> DatasetPostgresRepository.claimPending
  -> DatasetManifestBuilder
  -> DatasetPostgresRepository.completeManifest
```

定义和快照写操作要求 `Idempotency-Key` 与 `If-Match`。定义只能使用 `If-Match: 0`；快照必须
使用不可变定义当前 revision。快照只能选择定义内的产线，并且 freezeAt 时每条产线都必须存在
已批准、可用的影子运行与人工复核。未来 freezeAt、越权范围、未批准来源和泄漏风险字段均失败关闭。

## PostgreSQL 验收

本地数据库为 `ft_mes_bpi_v26_dataset_20260722_0414`（PostgreSQL `16.13`）；目标数据库为
`ft_mes_bpi`（PostgreSQL `15.18`）。两者当前 Flyway 版本均为 `26`。目标 marker 为
`ADP_E2E_BPI_DATASET_TARGET_20260722_055000_A7C4`，definition id
`adc587a0-117e-4b4b-834c-94ae4b4df39d`，snapshot id
`522f11a1-9b51-4b38-99ce-d0f49aca6139`。

关键验收 SQL：

```sql
SELECT state, snapshot_version, manifest_checksum, included_count, excluded_count,
       materialization_state, artifact_uri
FROM bpi.bpi_dataset_snapshots
WHERE tenant_id = :tenant_id AND id = :snapshot_id;

SELECT count(*)::integer AS total,
       count(*) FILTER (WHERE included)::integer AS included,
       count(*) FILTER (WHERE NOT included)::integer AS excluded,
       count(*) FILTER (WHERE feature_cutoff = prediction_time)::integer AS cutoff_safe,
       count(*) FILTER (
           WHERE jsonb_exists(feature_payload, 'review.manual_start_time')
              OR jsonb_exists(feature_payload, 'review.reference_quantity'))::integer AS leaked
FROM bpi.bpi_dataset_snapshot_samples
WHERE tenant_id = :tenant_id AND snapshot_id = :snapshot_id;

SELECT count(*)
FROM bpi.bpi_audit_events
WHERE tenant_id = :tenant_id
  AND object_id IN (:snapshot_id_1, :snapshot_id_2)
  AND action = 'DATASET_MANIFEST_READY';
```

目标实际结果：definition `ACTIVE/r1`、snapshot `MANIFEST_READY/r3`，manifest checksum 为
`4877203f7a9f3b306c0c65d2cc396b548ed13ef85cb275874aa74d4ca227e043`；`1 included`、
`2 excluded`、`3 cutoff_safe`、`0 leaked`，`CONFIDENCE_BELOW_THRESHOLD=1`、
`LABEL_DELAY_EXCEEDED=1`、`START_BOUNDARY_OUTSIDE_TOLERANCE=1`；最后一项与低置信度为同一样本
上的两个原因，不是第三个排除样本。额外插入的 `PLANT-02 / LINE-S07-01` 已审批干扰事实未进入
`PLANT-01` 清单，`cross_plant_rows=0`。审计 3 条；两个幂等请求均为 `COMPLETED`，保存的响应状态
为 `200/202`。本地确定性回归仍额外证明两个独立 snapshot 的 checksum 相同和终态 UPDATE 被拒绝。

清理后 definitions、snapshots、samples、shadow runs、reviews、batches、rules、topologies、catalogs、
idempotency 十类 marker 投影均为 0。

## 浏览器证据

- 桌面：`/tmp/ADP_E2E_BPI_DATASET_TARGET_20260722_055000_A7C4-desktop.png`，`1440x900`，
  SHA-256 `0cd9c37f1fd98d93f4d4a83ed20e23c154f6f1e1d8be889f51097aed1b8b4fcb`
- 移动：`/tmp/ADP_E2E_BPI_DATASET_TARGET_20260722_055000_A7C4-mobile.png`，`390x844`，
  SHA-256 `2b8842c9ffa3eab823481a158bc7252efd3d3b93db290333d07d643bbdd93f47`

截图和原始输出为本机临时证据，不作为部署制品提交。浏览器报告 SHA-256 为
`51e36569fddf33ded99a31b904739231ba73f567b0860aabb1e9db13e0fc2111`，数据库回查为
`1dd8e3dede2c92470167bf41daedeb0af5614a328520b5f8e1661ec13d58ff70`，清理回查为
`f45ec3d18d8841eeef157193dc6c49dc06384e721afae5aa9c24cfdb17dd2a25`。机器可读摘要保存在
`metadata/bpi-dataset-manifest-acceptance.json`。

## 部署与安全边界

- expand-only 报告：`/home/v6/adp-backups/bpi-v26-e116580a-20260721T213644Z/bpi-integrated-upgrade-20260721T213645Z.json`，结果 `PASS/COMPLETE`。
- service、adapter、WMS adapter 均为 exact revision `e116580a` 镜像且健康；`/bpi/` 返回 200。
- `BPI_PHASE2_INTEGRATION_ENABLED`、Protobuf ingress、Kafka、WMS outbox、WMS adapter 和 QCS outbox
  六个写开关在验收前后均为 false。
- 静态包移动端长标题换行修复只同步 BPI Web 目录，更新前备份位于
  `/home/v6/adp-backups/bpi-v26-ui-before-title-wrap-20260721T220112Z.tar.gz`。

## 未关闭门槛

1. Phase 3B 另行实现可版本化物化制品；未实现前不得出现 Iceberg/MLflow/模型 ready 状态。
2. 模型训练、在线推断、漂移监控和生产激活仍需后续独立目标与验收。
3. 物理设备来源序列、正式计量校准和连续 7-14 天现场运行仍需现场证据。
4. 本轮是一组受控 marker 的功能验收，不等于生产容量、故障注入或生产迁移签字。
